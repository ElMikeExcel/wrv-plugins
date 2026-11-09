package com.winkreyes.exceldowned;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * One player's downed session — the pure state machine (unit-tested, no
 * Bukkit). A session exists only while DOWNED. Every exit consumes it in
 * the registry; this class guarantees the internal invariants:
 *
 *  - give-up is two-step: first call arms, second inside the window dies;
 *    a call after the window simply re-arms (never dies).
 *  - the death transition fires at most once (deathConsumed latch).
 *  - damage while downed only ever shortens the deadline, never extends.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedSession {

    public enum Exit { REVIVE, GIVE_UP, BLEED_OUT, EXTERNAL }

    private final UUID playerId;
    private final String playerName;
    private final long downedAtMs;
    private long deadlineMs;
    private final String cause;
    private final String world;
    private final double x, y, z;
    private long giveUpArmedAtMs = -1;
    private boolean giveUpConfirmed;
    private boolean deathConsumed;
    private double totalWindowDamage;

    public DownedSession(UUID playerId, String playerName, long nowMs, long bleedOutMs,
                         String cause, String world, double x, double y, double z) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.downedAtMs = nowMs;
        this.deadlineMs = nowMs + bleedOutMs;
        this.cause = cause;
        this.world = world;
        this.x = x; this.y = y; this.z = z;
    }

    public UUID playerId() { return playerId; }
    public String playerName() { return playerName; }
    public long downedAtMs() { return downedAtMs; }
    public long deadlineMs() { return deadlineMs; }
    public String cause() { return cause; }
    public double totalWindowDamage() { return totalWindowDamage; }

    public boolean isExpired(long nowMs) { return nowMs >= deadlineMs; }
    public long remainingMs(long nowMs) { return Math.max(0, deadlineMs - nowMs); }

    /** Scaled damage eats the window. Returns the millis actually eaten. */
    public long eatDamage(double scaledDamage, double secondsPerPoint) {
        long eaten = DownedMath.windowMillisEaten(scaledDamage, secondsPerPoint);
        if (eaten <= 0) return 0;
        deadlineMs -= eaten;
        totalWindowDamage += scaledDamage;
        return eaten;
    }

    public enum GiveUpResult { NOT_DOWNED, ARMED, DIED }

    /**
     * Two-step give-up. First call arms (returns ARMED). A second call
     * within windowMs CONFIRMS the intent (DIED) — confirmation is not
     * the death itself: the caller runs the death path, whose
     * consumeDeath() latch is the single exactly-once guard (a confirm
     * that also consumed would make the death path refuse its own
     * death — the run-5 rig catch). A call after the window re-arms
     * instead of confirming — a stale arm can never kill.
     */
    public GiveUpResult giveUp(long nowMs, long windowMs) {
        if (deathConsumed) return GiveUpResult.NOT_DOWNED;
        if (giveUpConfirmed) return GiveUpResult.DIED;
        if (giveUpArmedAtMs >= 0 && nowMs - giveUpArmedAtMs <= windowMs) {
            giveUpConfirmed = true;
            return GiveUpResult.DIED;
        }
        giveUpArmedAtMs = nowMs;
        return GiveUpResult.ARMED;
    }

    /**
     * Arm the give-up WITHOUT confirming (0.1.1: opening the confirm UI
     * arms — the same arm state the typed two-step uses, so a typed
     * /giveup while the menu is open confirms, and vice versa). Arming
     * never kills and never confirms; a no-op once intent is confirmed
     * or the death is consumed.
     */
    public void armGiveUp(long nowMs) {
        if (deathConsumed || giveUpConfirmed) return;
        giveUpArmedAtMs = nowMs;
    }

    /** Cancel a pending arm (Keep fighting / UI closed). Never un-confirms. */
    public void disarmGiveUp() {
        if (giveUpConfirmed) return;
        giveUpArmedAtMs = -1;
    }

    /**
     * Confirm a give-up from the UI ("Let go"). True only when intent is
     * confirmed by this call or already stood confirmed. A STALE confirm
     * (never armed, or the arm window lapsed) returns false and changes
     * nothing — crucially it does NOT re-arm, so a late click on an old
     * menu can never kill or silently re-prime the typed path.
     */
    public boolean confirmGiveUp(long nowMs, long windowMs) {
        if (deathConsumed) return false;
        if (giveUpConfirmed) return true;
        if (giveUpArmedAtMs >= 0 && nowMs - giveUpArmedAtMs <= windowMs) {
            giveUpConfirmed = true;
            return true;
        }
        return false;
    }

    public boolean isGiveUpConfirmed() { return giveUpConfirmed; }

    public boolean isGiveUpArmed(long nowMs, long windowMs) {
        return giveUpArmedAtMs >= 0 && nowMs - giveUpArmedAtMs <= windowMs;
    }

    /** Consume the one real death. True only for the first caller. */
    public boolean consumeDeath() {
        if (deathConsumed) return false;
        deathConsumed = true;
        return true;
    }

    public boolean deathConsumed() { return deathConsumed; }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("uuid", playerId.toString());
        m.put("name", playerName);
        m.put("downedAtMs", downedAtMs);
        m.put("deadlineMs", deadlineMs);
        m.put("cause", cause);
        m.put("world", world);
        m.put("x", x); m.put("y", y); m.put("z", z);
        m.put("totalWindowDamage", totalWindowDamage);
        return m;
    }

    public static DownedSession fromMap(Map<String, Object> m) {
        DownedSession s = new DownedSession(
                UUID.fromString((String) m.get("uuid")),
                (String) m.get("name"),
                ((Number) m.get("downedAtMs")).longValue(),
                0L,
                (String) m.get("cause"),
                (String) m.get("world"),
                ((Number) m.get("x")).doubleValue(),
                ((Number) m.get("y")).doubleValue(),
                ((Number) m.get("z")).doubleValue());
        s.deadlineMs = ((Number) m.get("deadlineMs")).longValue();
        Object twd = m.get("totalWindowDamage");
        if (twd instanceof Number) s.totalWindowDamage = ((Number) twd).doubleValue();
        return s;
    }
}
