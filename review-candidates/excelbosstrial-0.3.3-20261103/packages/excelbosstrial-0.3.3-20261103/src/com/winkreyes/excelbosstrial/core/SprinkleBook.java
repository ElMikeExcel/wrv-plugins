/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The sprinkle's per-encounter bookkeeping (ExcelBossTrial
 * 0.3.3, study section 6): which elite adds each open encounter
 * owns, and how many it has spawned across the whole fight (the
 * max-per-fight accounting). Pure logic, no Bukkit - the
 * adapter (SprinkleService) owns the entities and asks this
 * book who to remove when an encounter closes.
 *
 * Runtime-only by design: sprinkles never persist across an
 * encounter, so there is no state file and nothing to restore.
 * An orphan elite left by a crash is just a tagged elite with
 * its loot flags already set false (study section 6).
 *
 * Cleanup contract: EVERY encounter close - verdict, prune,
 * warden give-up, admin reset, plugin disable - drains the
 * boss's entry here and removes exactly the returned adds.
 * Closing an unknown boss returns an empty set.
 */
public final class SprinkleBook {

    private static final class Fight {
        final Set<UUID> adds = new LinkedHashSet<>();
        int spawnedTotal;
    }

    private final Map<UUID, Fight> fights = new LinkedHashMap<>();

    /** Record one spawned add under its boss. */
    public void recordSpawn(UUID bossUuid, UUID addUuid) {
        Fight f = fights.computeIfAbsent(bossUuid, k -> new Fight());
        if (f.adds.add(addUuid)) {
            f.spawnedTotal++;
        }
    }

    /** Elites this encounter has spawned across the whole fight. */
    public int spawnedTotal(UUID bossUuid) {
        Fight f = fights.get(bossUuid);
        return f == null ? 0 : f.spawnedTotal;
    }

    /** The adds currently registered to this encounter (a copy). */
    public Set<UUID> adds(UUID bossUuid) {
        Fight f = fights.get(bossUuid);
        return f == null ? Set.of() : Set.copyOf(f.adds);
    }

    /** Living registered adds, given the set of add UUIDs still
     *  alive in the world (the adapter prunes the dead first). */
    public int aliveCount(UUID bossUuid, Set<UUID> aliveInWorld) {
        Fight f = fights.get(bossUuid);
        if (f == null) {
            return 0;
        }
        int n = 0;
        for (UUID id : f.adds) {
            if (aliveInWorld.contains(id)) {
                n++;
            }
        }
        return n;
    }

    /** Forget one add (it died or was removed on its own). The
     *  spawned total is NOT decremented: it prices the fight. */
    public void forget(UUID bossUuid, UUID addUuid) {
        Fight f = fights.get(bossUuid);
        if (f != null) {
            f.adds.remove(addUuid);
        }
    }

    /** Forget every add no longer in the world; returns how many
     *  were pruned. The fight entry itself SURVIVES an empty add
     *  set: its spawned total is the max-per-fight accounting and
     *  must live until the encounter closes (entries drain only
     *  through close / closeAll, which every encounter ending
     *  calls). */
    public int pruneDead(UUID bossUuid, Set<UUID> aliveInWorld) {
        Fight f = fights.get(bossUuid);
        if (f == null) {
            return 0;
        }
        int before = f.adds.size();
        f.adds.retainAll(aliveInWorld);
        return before - f.adds.size();
    }

    /**
     * Close one encounter: returns exactly the adds to remove
     * and drains the entry. Unknown boss: empty set.
     */
    public Set<UUID> close(UUID bossUuid) {
        Fight f = fights.remove(bossUuid);
        return f == null ? Set.of() : Set.copyOf(f.adds);
    }

    /** Close every open encounter (disable / reset): boss -> adds. */
    public Map<UUID, Set<UUID>> closeAll() {
        Map<UUID, Set<UUID>> out = new LinkedHashMap<>();
        for (Map.Entry<UUID, Fight> e : fights.entrySet()) {
            out.put(e.getKey(), Set.copyOf(e.getValue().adds));
        }
        fights.clear();
        return out;
    }

    /** Bosses with sprinkle state on the book. */
    public Set<UUID> openBosses() {
        return Set.copyOf(fights.keySet());
    }

    public boolean isEmpty() {
        return fights.isEmpty();
    }
}
