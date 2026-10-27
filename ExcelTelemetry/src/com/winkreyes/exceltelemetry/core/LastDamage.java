package com.winkreyes.exceltelemetry.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-player last-damage memory, so a death line can name its cause.
 * Vanilla's own killed_by stat undercounts deaths badly (design doc
 * §1a) - the death event itself carries no damager for environmental
 * finishes, so the last damage a player took (within a freshness
 * window) is the best evidence of what killed them.
 */
public final class LastDamage {
    public record Hit(String damageCause, String damagerKind, String damagerType,
                      String damagerUuid, long atMs) {}

    /** Deaths usually land within seconds of the fatal damage chain. */
    public static final long FRESH_MS = 30_000;

    private final Map<UUID, Hit> hits = new ConcurrentHashMap<>();

    public void record(UUID player, Hit hit) {
        hits.put(player, hit);
    }

    /** The recorded hit when fresh enough, else null. */
    public Hit fresh(UUID player, long nowMs) {
        Hit h = hits.get(player);
        if (h == null) return null;
        return (nowMs - h.atMs() <= FRESH_MS) ? h : null;
    }

    public void clear(UUID player) {
        hits.remove(player);
    }

    public int size() {
        return hits.size();
    }
}
