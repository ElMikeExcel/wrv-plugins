package com.winkreyes.exceltogether;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * WRV-068 party range indicator: the state machine behind the action bar line and the
 * transition cues. Pure (no Bukkit types, clock passed in) so it can be unit tested.
 *
 * The plugin feeds one sample per partied player every couple of seconds: whether they are
 * in a party and how many party members are in bonus range right now, counted by
 * PartyMemberFinder with the same near() test that drives the payout, so the display can
 * never disagree with the money. The action bar line shows the raw sample. Cues (a title
 * flash when the bonus flips, plus a soft sound when it turns on) go through the debounce
 * here: a flip only registers after the new state has held for HYSTERESIS_MS, so standing
 * on the 50-block edge does not strobe titles and sounds.
 *
 * The per-player cue toggle lives in this class as an in-memory map, the same way the rest
 * of the plugin keeps per-player state (parties themselves live in memory only).
 */
public final class RangeIndicator {
    public static final long HYSTERESIS_MS = 3_000L;
    public static final String OUT_LINE = "Party: nobody in range";

    public enum Cue { NONE, TURNED_ON, TURNED_OFF }

    private static final class State {
        Boolean stable;          // null = no sample yet (or not in a party): stay silent
        Boolean pending;         // candidate state waiting out the hysteresis window
        long pendingSince;
        boolean cues = true;     // /excelparty cue toggle, default on
    }

    private final Map<UUID, State> states = new HashMap<>();

    /** Action bar line while the bonus is on, e.g. "Party bonus ACTIVE: 2 in range (x1.25)". */
    public static String activeLine(int membersInRange, double multiplier) {
        return "Party bonus ACTIVE: " + membersInRange + " in range (x" + multText(multiplier) + ")";
    }
    static String multText(double m) {
        if (Double.isFinite(m) && m == Math.rint(m)) return Long.toString(Math.round(m));
        return java.math.BigDecimal.valueOf(m).stripTrailingZeros().toPlainString();
    }

    /**
     * Feed one sample and get back the cue to fire (already debounced). The caller still
     * checks cuesEnabled before flashing or sounding. A not-in-party sample resets the
     * machine silently: the party ending is announced in chat by the party logic, and a
     * player who rejoins starts fresh instead of getting a stale OFF flash. The first
     * sample after joining is also silent, so logging in next to your party does not flash.
     */
    public synchronized Cue sample(UUID id, boolean inParty, boolean inRange, long now) {
        State s = states.computeIfAbsent(id, k -> new State());
        if (!inParty) { s.stable = null; s.pending = null; return Cue.NONE; }
        if (s.stable == null) { s.stable = inRange; s.pending = null; return Cue.NONE; }
        if (inRange == s.stable) { s.pending = null; return Cue.NONE; }
        if (s.pending == null || s.pending != inRange) { s.pending = inRange; s.pendingSince = now; return Cue.NONE; }
        if (now - s.pendingSince < HYSTERESIS_MS) return Cue.NONE;
        s.stable = inRange; s.pending = null;
        return inRange ? Cue.TURNED_ON : Cue.TURNED_OFF;
    }

    /** Drop all state for a player (quit). The cue toggle goes too: settings are memory-only, like parties. */
    public synchronized void forget(UUID id) { states.remove(id); }

    public synchronized boolean cuesEnabled(UUID id) {
        State s = states.get(id);
        return s == null || s.cues;
    }
    /** Sets the cue toggle and returns the new value. */
    public synchronized boolean setCues(UUID id, boolean on) {
        states.computeIfAbsent(id, k -> new State()).cues = on;
        return on;
    }
    public synchronized boolean toggleCues(UUID id) { return setCues(id, !cuesEnabled(id)); }
}
