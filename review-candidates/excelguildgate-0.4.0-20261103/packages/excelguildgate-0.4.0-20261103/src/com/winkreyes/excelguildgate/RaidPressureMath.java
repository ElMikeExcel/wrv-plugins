package com.winkreyes.excelguildgate;

/**
 * ExcelGuildGate 0.4.0 (RAID-COMPANION-SPEC section 2.1): the pure
 * arithmetic of raid pressure - per-wave attribute scaling and the
 * attrition pulse's hot-window test. The Bukkit side owns sessions
 * and events; every number the pressure system applies is decided
 * here so the whole curve is unit-testable without a server.
 *
 * Scaling shape (Mike's settled pattern, the same one the boss
 * chapters use): the per-raider health multiplier applies to every
 * raider; the wave ramp multiplies wave N (0-based) by
 * {@code 1 + ramp-per-wave * N}, so later waves hit harder than
 * early ones across the whole fight - pressure that SUSTAINS and
 * grows over a long raid instead of spiking at the horn and going
 * soft. The attack multiplier is flat (a raider's blow is a
 * raider's blow; the ramp lives on the health side and the pulse).
 *
 * The pulse is attrition: a flat amount of magic damage on an
 * interval, owed only while the raid is HOT - a boss (here, the
 * raid itself) that has seen fight activity inside the hot window.
 * A raid that has gone quiet (everyone dead, gone, or the fight
 * abandoned) stops pulsing: the pulse pressures a fight, never
 * punishes an absence.
 */
public final class RaidPressureMath {

    private RaidPressureMath() {
    }

    /**
     * The health scale for one raider of wave {@code waveIndex}
     * (0-based): the chapter multiplier times the wave ramp.
     * Non-positive or NaN inputs degrade to the unscaled 1.0 - a
     * bad config row must never zero a raider's health or throw
     * inside a spawn event.
     */
    public static double waveHealthScale(int waveIndex, double healthMultiplier,
                                         double rampPerWave) {
        double base = saneMultiplier(healthMultiplier);
        double ramp = Double.isNaN(rampPerWave) ? 0.0 : Math.max(0.0, rampPerWave);
        return base * (1.0 + ramp * Math.max(0, waveIndex));
    }

    /** The (flat) attack scale for every raider. */
    public static double attackScale(double attackMultiplier) {
        return saneMultiplier(attackMultiplier);
    }

    private static double saneMultiplier(double multiplier) {
        return Double.isNaN(multiplier) || multiplier <= 0.0 ? 1.0 : multiplier;
    }

    /**
     * True when the fight is hot at {@code nowMs}: the last fight
     * activity (a participant's dealt or taken blow) landed within
     * {@code hotMs}. No activity ever (0) is never hot.
     */
    public static boolean hot(long lastActivityMs, long nowMs, long hotMs) {
        return lastActivityMs > 0L && nowMs - lastActivityMs <= Math.max(0L, hotMs);
    }

    /** True when a pulse is due: hot, and the interval has elapsed since the last one. */
    public static boolean pulseDue(long lastActivityMs, long lastPulseMs,
                                   long nowMs, long intervalMs, long hotMs) {
        if (!hot(lastActivityMs, nowMs, hotMs)) {
            return false;
        }
        return lastPulseMs <= 0L || nowMs - lastPulseMs >= Math.max(0L, intervalMs);
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.
