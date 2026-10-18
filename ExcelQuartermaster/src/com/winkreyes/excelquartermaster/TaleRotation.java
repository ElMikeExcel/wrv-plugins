package com.winkreyes.excelquartermaster;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Pure per-player tale rotation behind the Old Quartermaster's voice.
 *
 * No Bukkit dependencies, so the whole rule set is unit-testable on a plain
 * JVM (see tests/com/winkreyes/excelquartermaster/TaleRotationTest.java).
 *
 * Each tracked player has a record: the index of the NEXT tale they should
 * hear and the wall-clock time (ms) they last heard one. The stored index
 * is raw (not reduced); it is taken modulo the current tale count at
 * decision time, so editing the tales list in config (adding or removing
 * tales) never strands a saved index out of range.
 *
 * Rules (decide):
 * - Sneaking is ALWAYS a passthrough: sneaking is the Shopkeepers editor
 *   gesture, and this plugin never interferes with it. A sneak click does
 *   not tell a tale, does not advance the rotation, and does not start or
 *   refresh the cooldown.
 * - An untrackable caller (null UUID) is cancelled silently: the trade
 *   window still never opens (he is not a shop), but no tale is told,
 *   because there is nobody to rotate for. The Bukkit listener always has
 *   a real player, so this is a defensive path only.
 * - With zero tales configured there is nothing to say: cancel silently,
 *   change nothing.
 * - Inside the cooldown (now - lastTold &lt; cooldownMillis): cancel
 *   silently. The index does not advance and the cooldown is NOT extended
 *   by clicking again.
 * - Otherwise: TELL the tale at (stored next index mod tale count),
 *   advance the stored index by one past the told tale, stamp the time,
 *   and mark the outcome changed so the caller persists.
 */
public final class TaleRotation {

    /** What the listener should do with one matched interaction. */
    public enum Kind {
        /** Leave the event alone entirely (sneak / editor path). */
        PASSTHROUGH,
        /** Cancel the event (no trade window) but say nothing. */
        CANCEL_SILENT,
        /** Cancel the event and tell the tale at {@code taleIndex}. */
        TELL
    }

    /**
     * @param kind      what to do
     * @param taleIndex tale to tell; only meaningful for Kind.TELL, else -1
     * @param changed   true when the rotation mutated, so the caller should
     *                  persist
     */
    public record Outcome(Kind kind, int taleIndex, boolean changed) {}

    /** One player's saved place in the rotation. */
    public record PlayerTale(int nextIndex, long lastTaleMillis) {}

    private final Map<UUID, PlayerTale> players = new LinkedHashMap<>();

    /** A player's saved record, or null if they have never been told a tale. */
    public synchronized PlayerTale record(UUID playerId) {
        return playerId == null ? null : players.get(playerId);
    }

    /** How many players carry a rotation record. */
    public synchronized int trackedCount() {
        return players.size();
    }

    /** Snapshot of every record (UUID to saved place). */
    public synchronized Map<UUID, PlayerTale> snapshot() {
        return new LinkedHashMap<>(players);
    }

    /**
     * Restore persisted records. Null keys/values are skipped; negative
     * fields clamp to 0.
     */
    public synchronized void restore(Map<UUID, PlayerTale> records) {
        players.clear();
        if (records != null) {
            for (Map.Entry<UUID, PlayerTale> e : records.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    players.put(e.getKey(), new PlayerTale(
                            Math.max(0, e.getValue().nextIndex()),
                            Math.max(0L, e.getValue().lastTaleMillis())));
                }
            }
        }
    }

    /**
     * The one decision entry point. See the class docs for the rules.
     * {@code cooldownMillis} of 0 or less disables the cooldown.
     */
    public synchronized Outcome decide(UUID playerId, boolean sneaking, long nowMillis,
                                       int taleCount, long cooldownMillis) {
        if (sneaking) {
            return new Outcome(Kind.PASSTHROUGH, -1, false);
        }
        if (playerId == null || taleCount <= 0) {
            return new Outcome(Kind.CANCEL_SILENT, -1, false);
        }
        PlayerTale prev = players.get(playerId);
        long last = prev == null ? 0L : prev.lastTaleMillis();
        if (prev != null && cooldownMillis > 0 && nowMillis - last < cooldownMillis) {
            return new Outcome(Kind.CANCEL_SILENT, -1, false);
        }
        int stored = prev == null ? 0 : prev.nextIndex();
        int index = Math.floorMod(stored, taleCount);
        players.put(playerId, new PlayerTale(index + 1, nowMillis));
        return new Outcome(Kind.TELL, index, true);
    }
}
