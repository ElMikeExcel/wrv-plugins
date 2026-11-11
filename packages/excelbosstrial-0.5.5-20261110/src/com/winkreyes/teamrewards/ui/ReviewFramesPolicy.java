/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.teamrewards.ui;

/**
 * 0.2.11 (B-2): the immunity-frame state machine behind an AUTO
 * rewards review. Pure tick math; ReviewFrames owns the Bukkit
 * events and the view cross-check.
 *
 * Frames bind to AUTO sessions ONLY. A manual open (the /bossrewards
 * command, a chest marker, the compass) ends frames outright from
 * any state — that asymmetry is the entire anti-exploit boundary:
 * protection exists so a champion can read what they earned in the
 * seconds after a kill or a login, and nobody can summon it on
 * demand, mid-fight, by opening their own menu.
 *
 * States:
 *   IDLE   — nothing.
 *   FRAMED — the AUTO view is open: damage in AND out and interact
 *            are all blocked, until the absolute cap measured from
 *            the session's first open. Paging or refreshing inside
 *            the session never extends the window.
 *   GRACE  — the framed view closed while still inside its window:
 *            damage-IN stays blocked for the grace ticks only;
 *            damage-out is free, and dealing damage ends the grace
 *            instantly (attacking from safety is the one exploit the
 *            grace must never fund).
 *
 * The window/grace lengths are constructor parameters because they
 * are balance values: TeamClaims reads them from config
 * (team-rewards.review-frames.*) and the DEFAULT_* constants are the
 * settled values those keys fall back to.
 */
public final class ReviewFramesPolicy {
    /** Settled default: 60 seconds of framed review. */
    public static final int DEFAULT_MAX_TICKS = 1200;
    /** Settled default: 1 second of close grace. */
    public static final int DEFAULT_GRACE_TICKS = 20;

    public enum State { IDLE, FRAMED, GRACE }

    private final int maxTicks;
    private final int graceTicks;
    private State state = State.IDLE;
    private long windowStart;
    private long graceStart;

    public ReviewFramesPolicy() {
        this(DEFAULT_MAX_TICKS, DEFAULT_GRACE_TICKS);
    }

    public ReviewFramesPolicy(int maxTicks, int graceTicks) {
        this.maxTicks = Math.max(0, maxTicks);
        this.graceTicks = Math.max(0, graceTicks);
    }

    public int maxTicks() {
        return maxTicks;
    }

    public int graceTicks() {
        return graceTicks;
    }

    /**
     * A rewards view opened. AUTO starts frames (from IDLE or GRACE)
     * or continues them (already FRAMED — the original window stands,
     * so paging can never push the cap out). MANUAL ends frames
     * outright, from any state.
     */
    public synchronized void sessionOpened(boolean auto, long tick) {
        if (!auto) {
            state = State.IDLE;
            return;
        }
        if (state == State.FRAMED) {
            return;
        }
        state = State.FRAMED;
        windowStart = tick;
    }

    /**
     * The live view closed. Only a session closed while still inside
     * its window earns the close grace; a session that already ran
     * to the cap closes to nothing.
     */
    public synchronized void sessionClosed(long tick) {
        if (state != State.FRAMED) {
            return;
        }
        if (tick - windowStart < maxTicks) {
            state = State.GRACE;
            graceStart = tick;
        } else {
            state = State.IDLE;
        }
    }

    /** Dealing damage ends the grace instantly. No-op otherwise. */
    public synchronized void damageDealt(long tick) {
        if (state == State.GRACE) {
            state = State.IDLE;
        }
    }

    /** The state at a tick, with cap/grace expiry applied. */
    public synchronized State state(long tick) {
        if (state == State.FRAMED && tick - windowStart >= maxTicks) {
            return State.IDLE;
        }
        if (state == State.GRACE && tick - graceStart >= graceTicks) {
            return State.IDLE;
        }
        return state;
    }

    public synchronized boolean framedAt(long tick) {
        return state(tick) == State.FRAMED;
    }

    public synchronized boolean inGrace(long tick) {
        return state(tick) == State.GRACE;
    }

    /** Damage-in is blocked while framed and during the close grace. */
    public synchronized boolean damageInBlocked(long tick) {
        return state(tick) != State.IDLE;
    }

    /** Damage-out and interact are blocked only while framed. */
    public synchronized boolean actionsBlocked(long tick) {
        return state(tick) == State.FRAMED;
    }
}
