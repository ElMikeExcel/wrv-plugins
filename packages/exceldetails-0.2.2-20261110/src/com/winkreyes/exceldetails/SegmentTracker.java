package com.winkreyes.exceldetails;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The shared encounter segment (Details' "current segment") plus the
 * overall totals. Pure logic with caller-supplied time so the segment
 * lifecycle is unit-testable without a server.
 *
 * Lifecycle: idle -> (first tracked event) active -> (timeout with no
 * tracked event) archived as lastSegment + merged into overall -> idle.
 * Manual clear discards the current segment and, if combat is live,
 * restarts the clock on the spot.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SegmentTracker {

    public record Row(UUID id, String name, double amount) {}

    private final Map<UUID, CombatStats> current = new LinkedHashMap<>();
    private final Map<UUID, CombatStats> overall = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();

    private boolean active;
    private long segmentStartMs;
    private long lastEventMs;

    // 0.2.0 churn fix: tracked-event count for the live segment + the
    // announce verdict of the most recent close (see lastCloseAnnounceable).
    private int eventCount;
    private boolean lastCloseAnnounceable;

    private Map<UUID, CombatStats> lastSegment = Map.of();
    private long lastSegmentDurationMs;

    // 0.2.2 EM bucket: EliteMobs-space damage (DESIGN §2) lives
    // apart from the ranked segment — it touches the segment
    // clock like any tracked event, but it is never ranked,
    // never rendered, and never merged into the overall totals
    // (overall stays true-HP forever). Archived per segment
    // like the main map; discarded on clear/reset with it.
    private final Map<UUID, CombatStats> emCurrent = new LinkedHashMap<>();
    private Map<UUID, CombatStats> lastEmSegment = Map.of();
    private int emEventCount;
    private int lastEmEventCount;

    private CombatStats statsFor(Map<UUID, CombatStats> map, UUID id, String name) {
        if (name != null) names.put(id, name);
        return map.computeIfAbsent(id, k -> new CombatStats());
    }

    private void touch(long nowMs) {
        if (!active) {
            active = true;
            segmentStartMs = nowMs;
            current.clear();
            eventCount = 0;
        }
        lastEventMs = nowMs;
        eventCount++;
    }

    public synchronized void recordDamageDone(UUID id, String name, double amount, String target, long nowMs) {
        if (amount <= 0) return;
        touch(nowMs);
        statsFor(current, id, name).addDamageDone(amount, target);
    }

    public synchronized void recordDamageTaken(UUID id, String name, double amount, String source, long nowMs) {
        if (amount <= 0) return;
        touch(nowMs);
        statsFor(current, id, name).addDamageTaken(amount, source);
    }

    public synchronized void recordHealing(UUID id, String name, double amount, long nowMs) {
        if (amount <= 0) return;
        touch(nowMs);
        statsFor(current, id, name).addHealing(amount);
    }

    // ----- 0.2.2: the EM bucket (kept apart, never ranked) -----

    public synchronized void recordEmDamageDone(UUID id, String name, double amount, String target, long nowMs) {
        if (amount <= 0) return;
        touch(nowMs);
        emEventCount++;
        statsFor(emCurrent, id, name).addDamageDone(amount, target);
    }

    public synchronized void recordEmDamageTaken(UUID id, String name, double amount, String source, long nowMs) {
        if (amount <= 0) return;
        touch(nowMs);
        emEventCount++;
        statsFor(emCurrent, id, name).addDamageTaken(amount, source);
    }

    /** The player's EM-bucket stats for the live segment (or the
     *  last segment's, while idle — the ranked() idiom). */
    public synchronized CombatStats emStats(UUID id) {
        Map<UUID, CombatStats> src = active || !emCurrent.isEmpty() ? emCurrent : lastEmSegment;
        CombatStats s = src.get(id);
        return s == null ? new CombatStats() : s;
    }

    /** EM-bucket events in the live segment (or the last one's). */
    public synchronized int emEventCount() {
        return active || !emCurrent.isEmpty() ? emEventCount : lastEmEventCount;
    }

    /** Close the segment if the quiet timeout has passed. Returns true when a segment ended. */
    public synchronized boolean tick(long nowMs, long timeoutMs) {
        if (active && nowMs - lastEventMs >= timeoutMs) {
            endSegment(nowMs);
            return true;
        }
        return false;
    }

    private void endSegment(long nowMs) {
        // Announce verdict (0.2.0 churn fix): a close is announce-worthy
        // only when the segment saw real combat — 2+ tracked events, or
        // a span of a second or more. A lone event followed by quiet
        // (a single regen tick, one stray hit) closes as a MICRO
        // segment: it still merges into overall and archives as the
        // last segment, exactly as 0.1.0 did, but it never announces.
        // A post-clear segment holds zero events, so its close can
        // never announce either.
        lastCloseAnnounceable = eventCount >= 2
                || Math.max(0, lastEventMs - segmentStartMs) >= 1000L;
        Map<UUID, CombatStats> snapshot = new LinkedHashMap<>();
        current.forEach((id, s) -> {
            snapshot.put(id, s.copy());
            statsFor(overall, id, names.get(id)).absorb(s);
        });
        lastSegment = snapshot;
        lastSegmentDurationMs = Math.max(0, lastEventMs - segmentStartMs);
        current.clear();
        // 0.2.2: the EM bucket archives alongside the segment but
        // is NOT absorbed into overall — EM-space numbers never
        // enter the true-HP totals.
        Map<UUID, CombatStats> emSnapshot = new LinkedHashMap<>();
        emCurrent.forEach((id, s) -> emSnapshot.put(id, s.copy()));
        lastEmSegment = emSnapshot;
        lastEmEventCount = emEventCount;
        emCurrent.clear();
        emEventCount = 0;
        active = false;
    }

    /** Manual clear: discard the current segment; if it was live, a fresh one starts now. */
    public synchronized void clearCurrent(long nowMs) {
        current.clear();
        emCurrent.clear();
        emEventCount = 0;
        eventCount = 0;
        if (active) {
            segmentStartMs = nowMs;
            lastEventMs = nowMs;
        }
    }

    /** Whether the most recent segment close is worth a console announce. */
    public synchronized boolean lastCloseAnnounceable() { return lastCloseAnnounceable; }

    /** Tracked events in the live segment (0 while idle / just cleared). */
    public synchronized int segmentEventCount() { return eventCount; }

    /** Manual reset: current segment + overall totals wiped. */
    public synchronized void resetAll(long nowMs) {
        overall.clear();
        lastSegment = Map.of();
        lastEmSegment = Map.of();
        lastEmEventCount = 0;
        lastSegmentDurationMs = 0;
        clearCurrent(nowMs);
    }

    public synchronized boolean isActive() { return active; }

    /** Elapsed time of the live segment, or the last segment's duration when idle. */
    public synchronized long elapsedMs(long nowMs) {
        return active ? Math.max(0, nowMs - segmentStartMs) : lastSegmentDurationMs;
    }

    public synchronized CombatStats currentStats(UUID id) {
        CombatStats s = current.get(id);
        return s == null ? new CombatStats() : s;
    }

    public synchronized CombatStats overallStats(UUID id) {
        CombatStats s = overall.get(id);
        return s == null ? new CombatStats() : s;
    }

    // ----- 0.2.0 overall-total persistence (DataFile round-trip) -----

    /** Deep-copy snapshot of the overall totals, for data.yml. */
    public synchronized Map<UUID, CombatStats> overallSnapshot() {
        Map<UUID, CombatStats> out = new LinkedHashMap<>();
        overall.forEach((id, s) -> out.put(id, s.copy()));
        return out;
    }

    /** Display names for every player holding overall totals. */
    public synchronized Map<UUID, String> overallNames() {
        Map<UUID, String> out = new LinkedHashMap<>();
        overall.keySet().forEach(id -> out.put(id, nameOf(id)));
        return out;
    }

    /** Restore persisted overall totals at startup (replaces, never merges). */
    public synchronized void restoreOverall(Map<UUID, CombatStats> totals, Map<UUID, String> restoredNames) {
        overall.clear();
        if (totals != null) totals.forEach((id, s) -> overall.put(id, s.copy()));
        if (restoredNames != null) restoredNames.forEach(names::putIfAbsent);
    }

    public synchronized String nameOf(UUID id) {
        return names.getOrDefault(id, "?");
    }

    /** Ranked rows for a window over the current segment (or the last segment when idle). */
    public synchronized List<Row> ranked(CombatStats.Window window) {
        Map<UUID, CombatStats> src = active || !current.isEmpty() ? current : lastSegment;
        List<Row> rows = new ArrayList<>();
        src.forEach((id, s) -> {
            double amt = s.forWindow(window);
            if (amt > 0) rows.add(new Row(id, nameOf(id), amt));
        });
        rows.sort(Comparator.comparingDouble(Row::amount).reversed());
        return rows;
    }

    public synchronized double windowTotal(CombatStats.Window window) {
        return ranked(window).stream().mapToDouble(Row::amount).sum();
    }
}
