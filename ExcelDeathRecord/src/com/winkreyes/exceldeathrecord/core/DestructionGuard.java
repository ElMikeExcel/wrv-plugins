package com.winkreyes.exceldeathrecord.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Exactly-once guard for destruction records. Three capture
 * paths watch the same death of an item (the remove event, the
 * lethal-damage follow-up check, the void sweep); whichever
 * fires first writes the record and the others stand down.
 * Entries expire after a TTL so the map stays bounded; an item
 * entity UUID is never reused while it lives, so an expired
 * entry can never mask a different item.
 */
public final class DestructionGuard {

    private final Map<UUID, Long> recordedAt = new HashMap<>();
    private final long ttlMillis;

    public DestructionGuard(long ttlMillis) {
        this.ttlMillis = ttlMillis;
    }

    /** True (and remembers) the first time an entity is claimed;
     *  false for every later claim inside the TTL. */
    public synchronized boolean claim(UUID entityId, long nowMillis) {
        evict(nowMillis);
        if (recordedAt.containsKey(entityId)) return false;
        recordedAt.put(entityId, nowMillis);
        return true;
    }

    public synchronized boolean isClaimed(UUID entityId, long nowMillis) {
        evict(nowMillis);
        return recordedAt.containsKey(entityId);
    }

    public synchronized int size() {
        return recordedAt.size();
    }

    private void evict(long nowMillis) {
        Iterator<Map.Entry<UUID, Long>> it = recordedAt.entrySet().iterator();
        while (it.hasNext()) {
            if (nowMillis - it.next().getValue() > ttlMillis) it.remove();
        }
    }
}
