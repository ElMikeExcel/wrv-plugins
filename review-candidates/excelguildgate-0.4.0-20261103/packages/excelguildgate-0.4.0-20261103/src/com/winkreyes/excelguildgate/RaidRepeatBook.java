package com.winkreyes.excelguildgate;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelGuildGate 0.4.0 (RAID-COMPANION-SPEC section 1.2): the raid
 * repeat book - the 24-hour repeat window and the per-player clear
 * counts of the raid seal. Pure state, Bukkit-free; the adapter
 * persists it as the additive {@code raidRepeat} section of
 * state.yml through the same checked write that carries the seals
 * themselves, so a rollback to 0.3.4 reads the file cleanly (the
 * section rides along unread, exactly like the 0.2.0 catch-up
 * section did under 0.1.0).
 *
 * Discipline:
 * - The window starts at the FIRST break and at every
 *   REPEAT_CREDIT, and the adapter persists it (through the
 *   checked state write) BEFORE the reward event fires: a crash
 *   between the ruling and the rewards can lose a payout, never
 *   double one.
 * - Clear counts are per PLAYER (not per winner list): each
 *   winner of a crediting raid - the break and every repeat -
 *   gains one. First-break winners are seeded at 1 by that same
 *   break. Counts survive restarts (persisted above) and are
 *   read by the reward layer for the clear-count titles.
 * - Denials touch nothing here: no window starts, no count moves.
 *   Only the adapter's two credit paths call {@link #startWindow}
 *   and {@link #recordClear}.
 */
public final class RaidRepeatBook {

    private long windowUntilMs;
    private final Map<UUID, Integer> clears = new LinkedHashMap<>();

    /** Start (or restart) the repeat window from {@code nowMs}. */
    public synchronized void startWindow(long nowMs, long windowMs) {
        windowUntilMs = nowMs + Math.max(0L, windowMs);
    }

    /** True while the repeat window is still running at {@code nowMs}. */
    public synchronized boolean windowActive(long nowMs) {
        return nowMs < windowUntilMs;
    }

    /** Milliseconds left on the window at {@code nowMs} (0 when expired). */
    public synchronized long remainingMs(long nowMs) {
        return Math.max(0L, windowUntilMs - nowMs);
    }

    /** The instant the current window ends (0 when none was ever started). */
    public synchronized long windowUntilMs() {
        return windowUntilMs;
    }

    /** One crediting clear for every winner: each count rises by one. */
    public synchronized void recordClear(Collection<UUID> winners) {
        if (winners == null) {
            return;
        }
        for (UUID winner : winners) {
            if (winner != null) {
                clears.merge(winner, 1, Integer::sum);
            }
        }
    }

    /** A player's clear count (0 when they have never won a crediting raid). */
    public synchronized int clears(UUID player) {
        return player == null ? 0 : clears.getOrDefault(player, 0);
    }

    /** Snapshot of every clear count, for persistence and the reward event. */
    public synchronized Map<UUID, Integer> snapshotClears() {
        return new LinkedHashMap<>(clears);
    }

    /** Restore persisted state (load path and tests). Null clears are ignored. */
    public synchronized void restore(long windowUntil, Map<UUID, Integer> restoredClears) {
        this.windowUntilMs = Math.max(0L, windowUntil);
        clears.clear();
        if (restoredClears != null) {
            for (Map.Entry<UUID, Integer> e : restoredClears.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && e.getValue() > 0) {
                    clears.put(e.getKey(), e.getValue());
                }
            }
        }
    }

    /**
     * A remaining window in player words ("23h 59m", "5m", "12s"),
     * the same rendering shape ExcelBossTrial's cooldown book uses
     * for its status lines, kept local so GuildGate owes BossTrial
     * nothing at compile time.
     */
    public static String formatRemaining(long ms) {
        if (ms <= 0L) {
            return "0s";
        }
        long totalSeconds = (ms + 999L) / 1000L;
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        if (minutes > 0) {
            return minutes + "m";
        }
        return seconds + "s";
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.
