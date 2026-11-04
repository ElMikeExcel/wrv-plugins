package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 0.3.5: the crouch + hold Spell Book gesture, qualified on Bukkit signals.
 *
 * The protocol gate (CrouchHoldGate) starves on real Bedrock clients: it needs a
 * PLAYER_INPUT start followed by repeated SIMULATION_TICK transactions, and the
 * shipped Geyser never synthesizes that feed (investigation, Oct 30 2026). This
 * gate qualifies the same gesture on the signal family the plugin already proves
 * daily on real clients (arm swings and right-click-air interacts, observed at
 * MONITOR): crouch FIRST, then press and hold use on air for the hold window.
 *
 * Pure logic, no Bukkit types: server timestamps and tick arithmetic only, in
 * the same shape as CrouchHoldGate / WandSlotPlan. Freshness vocabulary is
 * shared with CrouchHoldGate (400 ms hold, 450 ms max gap) so the two gates
 * cannot drift apart silently.
 *
 * Rules (design section 6.1):
 *  - A burst starts only on a sneak-first use signal: the crouch was already
 *    down before this PRESS began (the caller judges it against the press
 *    chain, so press-first-then-crouch belongs to hold-to-read for the whole
 *    press, and a same-moment crouch + press is not sneak-first either).
 *  - Continuation is a freshness rule only (gap &lt;= 450 ms); no cadence is
 *    prescribed, so sparse client rhythms still qualify.
 *  - The gate fires once per burst, at the threshold, while the finger is
 *    still down: either on a signal arrival past the threshold, or from the
 *    scheduled check (see {@link #scheduledCheck}), with one grace re-check.
 *  - A use-on-block signal latches the burst disqualified (aiming at a block
 *    mid-hold kills that hold; tilting to the sky starts a fresh one).
 *  - After a fire or a disqualification the gate re-arms only when the burst
 *    fully ends: sneak released, or use-signal silence over 500 ms.
 */
public final class BookHoldPlan {
    /** Signal kinds fed by the hook. */
    public static final int USE_AIR = 0, USE_BLOCK = 1;
    /** CrouchHoldGate's own constants, shared on purpose. */
    public static final long DEFAULT_HOLD_MS = 400, MAX_GAP_MS = 450;
    /** A fired / disqualified burst re-arms after this much signal silence. */
    public static final long REARM_SILENCE_MS = 500;
    /** The scheduled check gets exactly one grace re-check this much later. */
    public static final long GRACE_MS = 150;
    private static final int MAX_PLAYERS = 2048;

    /** Scheduled threshold check outcomes. */
    public enum Check { FIRE, GRACE, DEAD }

    private static final class State {
        long start, last;
        boolean claimed, disqualified, fired, graceUsed;
    }

    private final Map<UUID, State> states = new HashMap<>();

    private void restart(State s, long now) {
        s.start = now; s.last = now;
        s.claimed = true; s.disqualified = false; s.fired = false; s.graceUsed = false;
    }

    /**
     * One use signal. sneakFirst: the crouch was already down before this press
     * began. Returns true when this signal fires the gate.
     */
    public synchronized boolean signal(UUID id, long now, int kind, boolean sneaking, boolean sneakFirst, long holdMs) {
        if (id == null) return false;
        State s = states.get(id);
        if (kind == USE_BLOCK) {
            if (s != null) s.disqualified = true; // latch: this hold aimed at a block
            return false;
        }
        if (!sneaking) { states.remove(id); return false; }
        if (s == null) {
            if (!sneakFirst) return false; // press-first bursts belong to hold-to-read
            if (states.size() >= MAX_PLAYERS) return false;
            s = new State();
            restart(s, now);
            states.put(id, s);
            return false;
        }
        if (s.fired || s.disqualified) {
            // Latched until the burst fully ends (sneak up, or silence over the re-arm window).
            if (now - s.last > REARM_SILENCE_MS && sneakFirst) { restart(s, now); return false; }
            s.last = now;
            return false;
        }
        if (now - s.last > MAX_GAP_MS) {
            // The burst died of silence; this signal starts a fresh one.
            if (sneakFirst) { restart(s, now); return false; }
            states.remove(id);
            return false;
        }
        s.last = now;
        if (now - s.start >= Math.max(1, holdMs)) { s.fired = true; return true; }
        return false;
    }

    /**
     * The scheduled threshold check (the hook schedules it for burst start +
     * holdMs): fires when the last signal is still fresh; one GRACE re-check
     * when freshness fails by a hair; DEAD otherwise (burst gone, fired,
     * disqualified, sneak released, or threshold not yet reached).
     */
    public synchronized Check scheduledCheck(UUID id, long now, boolean sneaking, long holdMs) {
        State s = states.get(id);
        if (s == null || s.fired || s.disqualified) return Check.DEAD;
        if (!sneaking) { states.remove(id); return Check.DEAD; }
        if (now - s.start < Math.max(1, holdMs)) return Check.DEAD;
        if (now - s.last <= MAX_GAP_MS) { s.fired = true; return Check.FIRE; }
        if (!s.graceUsed) { s.graceUsed = true; return Check.GRACE; }
        states.remove(id);
        return Check.DEAD;
    }

    /** True while a sneak-first burst is live and has not fired: hold-to-read stands down for it. */
    public synchronized boolean claimed(UUID id) {
        State s = states.get(id);
        return s != null && s.claimed && !s.fired;
    }

    /** Start time of the live burst (0 = none), so the hook can schedule per burst. */
    public synchronized long burstStart(UUID id) {
        State s = states.get(id);
        return s == null ? 0L : s.start;
    }

    /** Sneak released: the burst is over, fired or not. */
    public synchronized void sneakUp(UUID id) { states.remove(id); }

    /** Cancellation feeds (wand tap, other inventory, quit, wand reconfigure). */
    public synchronized void cancel(UUID id) { states.remove(id); }

    public synchronized void clear() { states.clear(); }
}
