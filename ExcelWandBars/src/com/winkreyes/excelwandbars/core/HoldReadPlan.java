package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.3.5: hold-to-read inside the inventory screen (design 3.3). Pure logic, no Bukkit.
 *
 * Pinning the native Bedrock tooltip is not possible (its duration is chosen
 * client-side; there is no server-set tooltip duration to extend), so an
 * in-screen lift that is still uncommitted after the hold window becomes a
 * read: the hook opens the spell's detail form, which never fades. Any commit
 * (a place, a second tap that lands, the arrange intent firing, the screen or
 * the bar closing) cancels the pending read. A second lift replaces the first.
 */
public final class HoldReadPlan {
    private static final class State { String spell; long at; boolean fired; }
    private final Map<UUID, State> states = new HashMap<>();

    /** A bar icon was lifted (tapped) in the screen at this time. */
    public synchronized void lift(UUID id, String spell, long now) {
        if (id == null || spell == null) return;
        State s = new State();
        s.spell = spell; s.at = now;
        states.put(id, s);
    }

    /** Any commit cancels the pending read. */
    public synchronized void commit(UUID id) { if (id != null) states.remove(id); }

    /** The inventory screen closed: no read. */
    public synchronized void screenClosed(UUID id) { commit(id); }

    /** The spell bar closed: no read. */
    public synchronized void barClosed(UUID id) { commit(id); }

    /**
     * At the deadline: the spell to read when the lift is still uncommitted and
     * the full hold window has passed; null otherwise. Fires exactly once.
     */
    public synchronized String decide(UUID id, long now, long holdMs) {
        State s = id == null ? null : states.get(id);
        if (s == null || s.fired) return null;
        if (now - s.at >= Math.max(1, holdMs)) { s.fired = true; return s.spell; }
        return null;
    }

    /** A lift is waiting out its hold window (not yet fired). */
    public synchronized boolean pending(UUID id) {
        State s = id == null ? null : states.get(id);
        return s != null && !s.fired;
    }

    public synchronized void forget(UUID id) { if (id != null) states.remove(id); }
    public synchronized void clear() { states.clear(); }
}
