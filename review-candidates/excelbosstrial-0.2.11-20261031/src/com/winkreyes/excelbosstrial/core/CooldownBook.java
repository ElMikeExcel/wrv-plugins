/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The per-seal credit cooldowns (settled consequence of a
 * non-crediting kill, Mike, Oct 18, 2026): after an unearned boss
 * kill, that boss's seal cannot credit for 6 hours, server-wide.
 * The cooldown survives restarts (the adapter persists this book in
 * state.yml) and is shown in guild status (GuildGate reads it back
 * through BossTrialAPI).
 *
 * Refresh rule: a kill that FAILS the share or presence gate starts
 * the cooldown - or restarts it at a full duration if one is already
 * running. A kill that WOULD have credited but lands during an
 * active cooldown neither credits nor extends it (see CreditJudge).
 * Escapes and despawns never touch this book: there is no kill.
 */
public final class CooldownBook {

    public static final long DEFAULT_DURATION_MS = 6L * 60L * 60L * 1000L;

    private final Map<String, Long> untilMsBySeal = new LinkedHashMap<>();
    private long durationMs = DEFAULT_DURATION_MS;

    public CooldownBook() {
    }

    public CooldownBook(long durationMs) {
        setDurationMs(durationMs);
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = Math.max(0L, durationMs);
    }

    public long durationMs() {
        return durationMs;
    }

    /** Start (or restart) the cooldown for a seal from now. */
    public synchronized void start(String sealId, long nowMs) {
        untilMsBySeal.put(sealId, nowMs + durationMs);
    }

    public synchronized boolean isActive(String sealId, long nowMs) {
        Long until = untilMsBySeal.get(sealId);
        return until != null && until > nowMs;
    }

    /** Milliseconds left on a seal's cooldown (0 when inactive/expired). */
    public synchronized long remainingMs(String sealId, long nowMs) {
        Long until = untilMsBySeal.get(sealId);
        if (until == null) {
            return 0;
        }
        return Math.max(0L, until - nowMs);
    }

    /** The instant a seal's cooldown ends (0 when none recorded). */
    public synchronized long untilMs(String sealId) {
        return untilMsBySeal.getOrDefault(sealId, 0L);
    }

    /** Admin clear (rig/repair use); returns true when one was running. */
    public synchronized boolean clear(String sealId) {
        return untilMsBySeal.remove(sealId) != null;
    }

    public synchronized void clearAll() {
        untilMsBySeal.clear();
    }

    /** Drop expired entries (housekeeping; keeps the state file small). */
    public synchronized void prune(long nowMs) {
        untilMsBySeal.entrySet().removeIf(e -> e.getValue() <= nowMs);
    }

    /** Plain-map snapshot for the adapter's YAML persistence. */
    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        for (Map.Entry<String, Long> e : untilMsBySeal.entrySet()) {
            m.put(e.getKey(), e.getValue());
        }
        return m;
    }

    /** Restore from a snapshot map (bad entries are skipped, never fatal). */
    public synchronized void restore(Map<String, ?> snapshot) {
        untilMsBySeal.clear();
        if (snapshot == null) {
            return;
        }
        for (Map.Entry<String, ?> e : snapshot.entrySet()) {
            if (e.getValue() instanceof Number n) {
                untilMsBySeal.put(e.getKey(), n.longValue());
            }
        }
    }

    /** Compact human duration for broadcasts/status: "5h 42m", "12m", "38s". */
    public static String formatRemaining(long ms) {
        long totalSeconds = Math.max(0L, ms / 1000L);
        long h = totalSeconds / 3600L;
        long m = (totalSeconds % 3600L) / 60L;
        long s = totalSeconds % 60L;
        if (h > 0) {
            return h + "h " + m + "m";
        }
        if (m > 0) {
            return m + "m";
        }
        return s + "s";
    }
}

