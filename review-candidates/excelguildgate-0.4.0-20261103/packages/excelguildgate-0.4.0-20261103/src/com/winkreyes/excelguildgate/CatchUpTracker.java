package com.winkreyes.excelguildgate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pure per-player "seen-stage" tracker behind the join catch-up (v0.2.0).
 *
 * No Bukkit dependencies, so the whole rule set is unit-testable on a plain
 * JVM (see tests/com/winkreyes/excelguildgate/CatchUpTrackerTest.java).
 *
 * The campaign stage is the fallen-seal count (SealState.brokenCount(),
 * 0..5 with every seal enabled). The five beats are keyed by that count,
 * not by which boss fell: the Nth fallen seal, in any order, carries the
 * Nth beat (1 Healer, 2 Pathfinder, 3 Wonder, 4 Masons, 5 the Guild
 * Opens). A player's seen-stage is the highest stage they have been told
 * about, live at the moment of a fall or via a catch-up message.
 *
 * Rules:
 * - evaluateJoin, no record: the player is brand-new (or predates 0.2.0
 *   data). They start AT the current stage, silently: no "while you were
 *   away" for someone who was never here. A record is created, so the
 *   outcome is marked changed.
 * - evaluateJoin, seen &lt; current: the missed beats are seen+1..current
 *   (ascending); the mark advances to current. The caller sends ONE
 *   combined catch-up message for exactly those beats, so a second join
 *   at the same stage reports nothing.
 * - evaluateJoin, seen &gt; current (REVOKE RULE): the stage dropped under
 *   the player (a seal was revoked, or the campaign was reset). Nothing
 *   is ever sent for a drop: no negative or repeat message. The mark is
 *   silently clamped down to the current stage. From the clamped mark,
 *   only a genuinely new fall that pushes the stage back above the mark
 *   is ever reported, exactly once, like any other missed beat.
 * - advanceOnline: when a seal falls, everyone online is standing there
 *   for the broadcast, so their marks advance to the new stage (a player
 *   with no record yet is treated as told: they were here). A mark above
 *   the new stage (possible after a reset) is never lowered here; the
 *   join-time clamp handles drops.
 * - setSeen: the admin path (/guildgate catchup &lt;player&gt; reset sets
 *   0, so the player's next join replays every beat up to the then-current
 *   stage).
 */
public final class CatchUpTracker {

    /**
     * Outcome of a join evaluation.
     *
     * @param missedBeats beat numbers (1-based, ascending) the player has
     *                    not been told about and should be caught up on;
     *                    empty when there is nothing to say
     * @param changed     true when the tracker mutated, so the caller
     *                    should persist
     */
    public record JoinResult(List<Integer> missedBeats, boolean changed) {}

    private final Map<UUID, Integer> seen = new LinkedHashMap<>();

    /** The stage a player has been told through, or null if never seen. */
    public synchronized Integer seenStage(UUID playerId) {
        return playerId == null ? null : seen.get(playerId);
    }

    /** How many players carry a seen-stage record. */
    public synchronized int trackedCount() {
        return seen.size();
    }

    /** Snapshot of every record (UUID to seen-stage). */
    public synchronized Map<UUID, Integer> snapshot() {
        return new LinkedHashMap<>(seen);
    }

    /**
     * Restore persisted records. Null keys/values are skipped and negative
     * stages clamp to 0.
     */
    public synchronized void restore(Map<UUID, Integer> records) {
        seen.clear();
        if (records != null) {
            for (Map.Entry<UUID, Integer> e : records.entrySet()) {
                if (e.getKey() != null && e.getValue() != null) {
                    seen.put(e.getKey(), Math.max(0, e.getValue()));
                }
            }
        }
    }

    /**
     * The join path. See the class docs for the exact rules, including the
     * silent first-sighting start and the revoke clamp.
     */
    public synchronized JoinResult evaluateJoin(UUID playerId, int currentStage) {
        if (playerId == null) {
            return new JoinResult(List.of(), false);
        }
        int current = Math.max(0, currentStage);
        Integer prev = seen.get(playerId);
        if (prev == null) {
            seen.put(playerId, current);
            return new JoinResult(List.of(), true);
        }
        if (prev < current) {
            List<Integer> missed = new ArrayList<>();
            for (int beat = prev + 1; beat <= current; beat++) {
                missed.add(beat);
            }
            seen.put(playerId, current);
            return new JoinResult(Collections.unmodifiableList(missed), true);
        }
        if (prev > current) {
            seen.put(playerId, current);
            return new JoinResult(List.of(), true);
        }
        return new JoinResult(List.of(), false);
    }

    /**
     * The seal-fall path: mark every online player as told through the new
     * stage. Returns true when any record changed (caller persists).
     */
    public synchronized boolean advanceOnline(Collection<UUID> onlineIds, int newStage) {
        if (onlineIds == null) {
            return false;
        }
        int stage = Math.max(0, newStage);
        boolean changed = false;
        for (UUID id : onlineIds) {
            if (id == null) {
                continue;
            }
            Integer prev = seen.get(id);
            if (prev == null || prev < stage) {
                seen.put(id, stage);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Admin path: set a player's mark directly (creating a record if none
     * exists). Negative stages clamp to 0. Returns true on change.
     */
    public synchronized boolean setSeen(UUID playerId, int stage) {
        if (playerId == null) {
            return false;
        }
        int clamped = Math.max(0, stage);
        Integer prev = seen.get(playerId);
        if (prev != null && prev == clamped) {
            return false;
        }
        seen.put(playerId, clamped);
        return true;
    }
}
