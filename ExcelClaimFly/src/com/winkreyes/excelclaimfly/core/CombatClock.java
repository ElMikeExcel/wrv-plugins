package com.winkreyes.excelclaimfly.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Combat tags with a quiet period. Pure logic, unit tested. */
public final class CombatClock {
    private final Map<UUID, Long> last = new HashMap<>();
    private long quietMs;
    public CombatClock(long quietMs) { this.quietMs = quietMs; }
    public void setQuietMs(long ms) { this.quietMs = ms; }
    public void tag(UUID id, long now) { last.put(id, now); }
    public void clear(UUID id) { last.remove(id); }
    public boolean tagged(UUID id, long now) {
        Long t = last.get(id);
        if (t == null) return false;
        if (now - t >= quietMs || now < t) { last.remove(id); return false; }
        return true;
    }
    public long leftMs(UUID id, long now) {
        Long t = last.get(id);
        return t == null ? 0 : Math.max(0, quietMs - (now - t));
    }
}
