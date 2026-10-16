package com.winkreyes.excelwaypoints;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.LongSupplier;

/** Short-lived one-use actions (delete confirm, "type a name in chat"). Expire by clock. */
public final class Pending<T> {
    private final Map<UUID, Entry<T>> map = new HashMap<>();
    private final LongSupplier clock;
    private final long ttlMs;
    private static final class Entry<T> { T v; long until; String token; }

    public Pending(long ttlMs, LongSupplier clock) { this.ttlMs = ttlMs; this.clock = clock; }

    /** Stores a value for the player (replacing any older one) and returns its one-use token. */
    public synchronized String put(UUID p, T v) {
        Entry<T> e = new Entry<>();
        e.v = v; e.until = clock.getAsLong() + ttlMs;
        e.token = Long.toString(Math.abs(new java.util.Random().nextLong()) % 1_000_000_000L, 36);
        map.put(p, e);
        return e.token;
    }
    /** Takes the value if present, not expired and (when token != null) the token matches. Always one use. */
    public synchronized T take(UUID p, String token) {
        Entry<T> e = map.get(p);
        if (e == null) return null;
        if (clock.getAsLong() > e.until) { map.remove(p); return null; }
        if (token != null && !token.equals(e.token)) return null;
        map.remove(p);
        return e.v;
    }
    public synchronized boolean has(UUID p) {
        Entry<T> e = map.get(p);
        if (e == null) return false;
        if (clock.getAsLong() > e.until) { map.remove(p); return false; }
        return true;
    }
    public synchronized void clear(UUID p) { map.remove(p); }
}
