package com.winkreyes.acrobatics.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Remembers where each player's water landings paid out, so repeat jumps
 * at the same spot inside the window pay less (Mike's rule: reduced, never
 * zero; new spots pay best). In-memory only, by design. */
public final class SpotMemory {

    private record Landing(String world, int x, int z, long atMillis) {}

    private static final long PRUNE_AFTER_MS = 86_400_000L; // 24h

    private final Map<UUID, List<Landing>> landings = new HashMap<>();

    public synchronized void record(UUID player, String world, int x, int z, long nowMillis) {
        List<Landing> list = landings.computeIfAbsent(player, k -> new ArrayList<>());
        prune(list, nowMillis);
        list.add(new Landing(world, x, z, nowMillis));
    }

    public synchronized int countRecent(UUID player, String world, int x, int z,
                                        int radiusBlocks, long windowMillis, long nowMillis) {
        List<Landing> list = landings.get(player);
        if (list == null) {
            return 0;
        }
        prune(list, nowMillis);
        long radiusSquared = (long) radiusBlocks * radiusBlocks;
        int count = 0;
        for (Landing landing : list) {
            if (!landing.world().equals(world)) {
                continue;
            }
            if (nowMillis - landing.atMillis() > windowMillis) {
                continue;
            }
            long dx = landing.x() - x;
            long dz = landing.z() - z;
            if (dx * dx + dz * dz <= radiusSquared) {
                count++;
            }
        }
        return count;
    }

    private void prune(List<Landing> list, long nowMillis) {
        list.removeIf(l -> nowMillis - l.atMillis() > PRUNE_AFTER_MS);
    }
}

