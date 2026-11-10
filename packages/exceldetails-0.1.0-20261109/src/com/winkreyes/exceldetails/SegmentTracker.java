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

    private Map<UUID, CombatStats> lastSegment = Map.of();
    private long lastSegmentDurationMs;

    private CombatStats statsFor(Map<UUID, CombatStats> map, UUID id, String name) {
        if (name != null) names.put(id, name);
        return map.computeIfAbsent(id, k -> new CombatStats());
    }

    private void touch(long nowMs) {
        if (!active) {
            active = true;
            segmentStartMs = nowMs;
            current.clear();
        }
        lastEventMs = nowMs;
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

    /** Close the segment if the quiet timeout has passed. Returns true when a segment ended. */
    public synchronized boolean tick(long nowMs, long timeoutMs) {
        if (active && nowMs - lastEventMs >= timeoutMs) {
            endSegment(nowMs);
            return true;
        }
        return false;
    }

    private void endSegment(long nowMs) {
        Map<UUID, CombatStats> snapshot = new LinkedHashMap<>();
        current.forEach((id, s) -> {
            snapshot.put(id, s.copy());
            statsFor(overall, id, names.get(id)).absorb(s);
        });
        lastSegment = snapshot;
        lastSegmentDurationMs = Math.max(0, lastEventMs - segmentStartMs);
        current.clear();
        active = false;
    }

    /** Manual clear: discard the current segment; if it was live, a fresh one starts now. */
    public synchronized void clearCurrent(long nowMs) {
        current.clear();
        if (active) {
            segmentStartMs = nowMs;
            lastEventMs = nowMs;
        }
    }

    /** Manual reset: current segment + overall totals wiped. */
    public synchronized void resetAll(long nowMs) {
        overall.clear();
        lastSegment = Map.of();
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
