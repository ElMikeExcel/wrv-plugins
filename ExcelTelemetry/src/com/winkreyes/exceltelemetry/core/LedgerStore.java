package com.winkreyes.exceltelemetry.core;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bounded store of per-victim damage ledgers. Entries are created on
 * first damage, removed on death, and pruned when the victim is gone
 * or the entry is older than the TTL - so a long-running server never
 * accumulates ledgers for mobs that despawned, burned in lava off in
 * a cave, or were removed by other plugins.
 */
public final class LedgerStore {
    private final Map<UUID, DamageLedger> ledgers = new ConcurrentHashMap<>();
    private final long ttlMs;

    public LedgerStore(long ttlMs) {
        this.ttlMs = ttlMs;
    }

    public DamageLedger getOrCreate(UUID victim, long nowMs) {
        return ledgers.computeIfAbsent(victim, k -> new DamageLedger(nowMs));
    }

    public DamageLedger get(UUID victim) {
        return ledgers.get(victim);
    }

    public DamageLedger remove(UUID victim) {
        return ledgers.remove(victim);
    }

    public int size() {
        return ledgers.size();
    }

    /**
     * Drop entries whose victim is not in aliveIds, plus any entry
     * untouched for longer than the TTL (even if the victim still
     * exists - a mob nobody has hit in 10 minutes starts a fresh
     * ledger on its next fight). Returns how many were pruned.
     */
    public int prune(Set<UUID> aliveIds, long nowMs) {
        int dropped = 0;
        Iterator<Map.Entry<UUID, DamageLedger>> it = ledgers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, DamageLedger> e = it.next();
            boolean dead = !aliveIds.contains(e.getKey());
            boolean stale = nowMs - e.getValue().lastTouchMs() > ttlMs;
            if (dead || stale) {
                it.remove();
                dropped++;
            }
        }
        return dropped;
    }

    public Set<UUID> victimIds() {
        return new HashSet<>(ledgers.keySet());
    }
}
