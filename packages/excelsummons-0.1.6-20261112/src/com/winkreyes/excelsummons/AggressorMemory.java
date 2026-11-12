package com.winkreyes.excelsummons;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Retaliation memory (ExcelSummons 0.1.1, Mike's ruling of Nov 8,
 * 2026): per summon, the ONE non-party player who most recently hit
 * it — the only player that summon may ever target or damage.
 *
 * Lifetime (the listener enforces the event-driven half; this class
 * enforces the clock): an entry lives until the EARLIEST of —
 *   - {@code windowMs} pass with no further hit from that aggressor
 *     (every new hit refreshes the entry; default window 30 s);
 *   - the aggressor dies or logs off (listener: clearAggressor);
 *   - the aggressor leaves the summon's engage radius or its world,
 *     or joins the owner's party (listener: target-tick validation —
 *     live party membership always overrides this memory);
 *   - the summon dies, despawns, or leaves the registry (clear).
 * One aggressor per summon: a second stranger's hit replaces the
 * first (the most recent attacker owns the retaliation — the way a
 * vanilla mob switches to whoever hit it last).
 *
 * Pure and unit-tested; no Bukkit types.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class AggressorMemory {

    private final long windowMs;
    private final Map<UUID, Map.Entry<UUID, Long>> bySummon = new HashMap<>();

    public AggressorMemory(long windowMs) {
        this.windowMs = Math.max(1L, windowMs);
    }

    public long windowMs() { return windowMs; }

    /** Record (or refresh) a stranger's hit on a summon. */
    public synchronized void record(UUID summonId, UUID aggressorId, long nowMillis) {
        if (summonId == null || aggressorId == null) return;
        bySummon.put(summonId, Map.entry(aggressorId, nowMillis));
    }

    /** The summon's live aggressor, or null when none / expired.
     *  An expired entry is dropped as it is read. */
    public synchronized UUID aggressorOf(UUID summonId, long nowMillis) {
        Map.Entry<UUID, Long> e = bySummon.get(summonId);
        if (e == null) return null;
        if (nowMillis - e.getValue() > windowMs) {
            bySummon.remove(summonId);
            return null;
        }
        return e.getKey();
    }

    /** True when this player is the summon's live recorded aggressor. */
    public synchronized boolean isAggressor(UUID summonId, UUID playerId, long nowMillis) {
        return playerId != null && playerId.equals(aggressorOf(summonId, nowMillis));
    }

    /** Forget everything about one summon (it died / despawned). */
    public synchronized void clear(UUID summonId) {
        bySummon.remove(summonId);
    }

    /** Forget one player everywhere (they died / logged off). */
    public synchronized void clearAggressor(UUID playerId) {
        if (playerId == null) return;
        bySummon.values().removeIf(e -> e.getKey().equals(playerId));
    }

    public synchronized int size() { return bySummon.size(); }
}
