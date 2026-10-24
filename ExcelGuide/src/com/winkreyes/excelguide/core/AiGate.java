package com.winkreyes.excelguide.core;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The AI fallback's own manners (0.2.0), separate from the chat
 * RateLimiter on purpose: AI answers are private, cost a third
 * party's free quota, and arrive seconds late, so they get a
 * stricter bucket - a per-player cooldown (default 60 s), a daily
 * call budget (default 50, counted at engagement so a failing
 * provider cannot be retry-stormed), and at most 2 calls in flight.
 * A reservation is taken BEFORE the provider call and is not
 * refunded on failure: the budget guards the quota, not the outcome.
 * The day rolls on the configured zone (the server's local day).
 */
public final class AiGate {

    private static final int MAX_IN_FLIGHT = 2;

    private final int cooldownSeconds;
    private final int dailyBudget;
    private final ZoneId zone;
    private final Map<UUID, Long> lastByPlayer = new HashMap<>();
    private LocalDate day;
    private int usedToday;
    private int inFlight;

    public AiGate(int cooldownSeconds, int dailyBudget, ZoneId zone) {
        this.cooldownSeconds = Math.max(0, cooldownSeconds);
        this.dailyBudget = Math.max(0, dailyBudget);
        this.zone = zone == null ? ZoneId.systemDefault() : zone;
    }

    /**
     * Try to reserve one AI call for this player at {@code nowMillis}.
     * On true the caller MUST call {@link #release()} when the call
     * settles, answered or not.
     */
    public synchronized boolean tryReserve(UUID player, long nowMillis) {
        rollDay(nowMillis);
        if (usedToday >= dailyBudget || inFlight >= MAX_IN_FLIGHT) {
            return false;
        }
        Long last = lastByPlayer.get(player);
        if (last != null && nowMillis - last < cooldownSeconds * 1000L) {
            return false;
        }
        usedToday++;
        inFlight++;
        lastByPlayer.put(player, nowMillis);
        return true;
    }

    public synchronized void release() {
        inFlight = Math.max(0, inFlight - 1);
    }

    /**
     * Milliseconds left on this player's per-player AI cooldown
     * (0.2.2), 0 when none is running. The cooldown notice quotes
     * this when the gate turns a question away on the cooldown; the
     * daily-budget and in-flight blocks have no per-player wait to
     * quote (the budget resets at midnight, a call in flight lands
     * in seconds), so this returns 0 for them and the engine serves
     * the plain non-answer instead of a misleading timer.
     */
    public synchronized long cooldownRemainingMillis(UUID player, long nowMillis) {
        Long last = lastByPlayer.get(player);
        if (last == null) {
            return 0;
        }
        return Math.max(0, last + cooldownSeconds * 1000L - nowMillis);
    }

    /** AI calls engaged so far today (the budget's spent count). */
    public synchronized int usedToday(long nowMillis) {
        rollDay(nowMillis);
        return usedToday;
    }

    public synchronized int inFlight() {
        return inFlight;
    }

    private void rollDay(long nowMillis) {
        LocalDate today = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowMillis), zone);
        if (day == null || !today.equals(day)) {
            day = today;
            usedToday = 0;
        }
    }
}
