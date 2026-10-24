package com.winkreyes.excelraidbuff.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The raid lifetime caps (0.1.2, Mike's order Oct 23, 2026) —
 * Bukkit-free so the clock rules are unit-testable. Time is
 * injected by the adapter (epoch milliseconds).
 *
 * Every tracked raid has exactly one fact here: WHEN IT
 * STARTED. The cap is a hard lifetime limit — nothing resets
 * it. There is deliberately no progress input: a raid that
 * fights non-stop for the full cap still dies down (that is
 * what separates this clock from the reveal's quiet clock,
 * which any landed blow resets).
 *
 * The starts are persisted by the adapter (snapshot/restore)
 * so a server restart does NOT reset the clock — the server
 * restarts every morning, and a clock that forgot overnight
 * would make the cap meaningless. A raid first sighted
 * mid-progress starts its clock at first sight: the honest
 * bound of what can be known.
 */
public final class ExpiryTracker {

    /** Raid key -> start, epoch milliseconds. */
    private final Map<Integer, Long> starts = new HashMap<>();

    /**
     * A raid was sighted (trigger, wave, sweep). Its first
     * sighting starts its clock; later sightings never move it.
     *
     * @return true when a NEW clock started (the adapter persists)
     */
    public boolean onRaidSeen(int raidKey, long nowMs) {
        if (starts.containsKey(raidKey)) return false;
        starts.put(raidKey, nowMs);
        return true;
    }

    /** The raid's start (epoch ms), or -1 when untracked. */
    public long startOf(int raidKey) {
        Long s = starts.get(raidKey);
        return s == null ? -1L : s;
    }

    /** The raid's age at {@code nowMs}, or -1 when untracked. */
    public long ageMs(int raidKey, long nowMs) {
        long s = startOf(raidKey);
        return s < 0 ? -1L : Math.max(0L, nowMs - s);
    }

    /** At or past the cap? Fires exactly at it, never before. */
    public boolean isExpired(int raidKey, long nowMs, long capMs) {
        long s = startOf(raidKey);
        return s >= 0 && nowMs - s >= capMs;
    }

    /** The oldest tracked start (epoch ms), or -1 when none. */
    public long oldestStartMs() {
        long oldest = -1L;
        for (long s : starts.values()) {
            if (oldest < 0 || s < oldest) oldest = s;
        }
        return oldest;
    }

    /**
     * The raid ended (victory, defeat, expiry itself): drop
     * its clock.
     *
     * @return true when it was tracked
     */
    public boolean onRaidEnd(int raidKey) {
        return starts.remove(raidKey) != null;
    }

    /**
     * Drop every raid not in the active set (raids that ended
     * without reaching the finish/stop handlers, or whose
     * chunks are no longer loaded — they start fresh clocks
     * at their next sighting, per the first-sight rule).
     *
     * @return the dropped keys
     */
    public Set<Integer> retainOnly(Set<Integer> activeKeys) {
        Set<Integer> dropped = new HashSet<>();
        starts.keySet().removeIf(key -> {
            if (activeKeys.contains(key)) return false;
            dropped.add(key);
            return true;
        });
        return dropped;
    }

    /**
     * Full reset (the feature toggled off in a reload): drop
     * every clock.
     *
     * @return the keys that were tracked
     */
    public Set<Integer> reset() {
        Set<Integer> keys = new HashSet<>(starts.keySet());
        starts.clear();
        return keys;
    }

    /** Raids currently tracked (diagnostics / tests). */
    public int trackedRaids() {
        return starts.size();
    }

    /** The persisted shape: raid key -> start epoch ms (a copy). */
    public Map<Integer, Long> snapshot() {
        return new HashMap<>(starts);
    }

    /**
     * Adopt saved starts (the adapter's on-enable load). A
     * clock already running always wins — a restored start
     * never moves a live raid's clock.
     */
    public void restore(Map<Integer, Long> saved) {
        if (saved == null) return;
        for (Map.Entry<Integer, Long> e : saved.entrySet()) {
            if (e.getKey() != null && e.getValue() != null) {
                starts.putIfAbsent(e.getKey(), e.getValue());
            }
        }
    }
}
