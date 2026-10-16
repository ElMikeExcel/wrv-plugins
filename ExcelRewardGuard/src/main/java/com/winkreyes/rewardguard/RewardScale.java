package com.winkreyes.rewardguard;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The reduced-reward policy math (ExcelRewardGuard 1.1, Mike's decision of
 * Oct 15, 2026: farmed kills earn a reduced share instead of nothing).
 *
 * A single config value, farm-reward-percent (0-100, default 65), drives
 * every reward path: Jobs money, Jobs points, Jobs XP and mcMMO XP for a
 * DENY-classified kill are multiplied by percent/100. Two endpoints are
 * special by design:
 *   - 0 reproduces ExcelRewardGuard 1.0 exactly (full veto / zeroing);
 *   - 100 is a true no-op: values pass through untouched, with no rounding.
 * In between, scaled doubles are rounded half-up to 2 decimal places. Jobs
 * money and points are currency-like, so a scaled reward never carries
 * floating-point dust like 6.500000000000001 into a balance, and sub-cent
 * results round the way money does (65 percent of 0.01 is 0.0065, which
 * rounds to 0.01, not silently to zero).
 *
 * Pure logic on purpose: no Bukkit types, so the policy is unit-testable
 * without a server, same as XpSuppression.
 */
public final class RewardScale {
    public static final int DEFAULT_PERCENT = 65;

    private RewardScale() { }

    /** Result of parsing the raw config value: the effective percent, and whether the raw value had to be defaulted or clamped to get there. */
    public record ParsedPercent(int percent, boolean adjusted) { }

    /**
     * Parses farm-reward-percent from whatever the config held:
     *   - missing (null)          -> the shipped default, 65, not "adjusted" (it is the normal path);
     *   - an integer in [0, 100]   -> itself (a numeric string like "65" counts);
     *   - an integer out of range  -> clamped to 0 or 100, adjusted;
     *   - a fractional number      -> truncated to an integer, then clamped, adjusted;
     *   - anything unparseable     -> the default, 65, adjusted.
     */
    public static ParsedPercent parsePercent(Object raw) {
        if (raw == null) return new ParsedPercent(DEFAULT_PERCENT, false);
        Long value = null;
        if (raw instanceof Number number) {
            double d = number.doubleValue();
            if (Double.isNaN(d) || Double.isInfinite(d)) return new ParsedPercent(DEFAULT_PERCENT, true);
            value = (long) d;
            if (d != value) return new ParsedPercent(clamp(value), true);
        } else if (raw instanceof String text) {
            try {
                value = Long.parseLong(text.trim());
            } catch (NumberFormatException notANumber) {
                return new ParsedPercent(DEFAULT_PERCENT, true);
            }
        } else {
            return new ParsedPercent(DEFAULT_PERCENT, true);
        }
        int clamped = clamp(value);
        return new ParsedPercent(clamped, clamped != value);
    }

    private static int clamp(long value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return (int) value;
    }

    /** Scales a double reward (Jobs money, points or XP) to the configured percent. */
    public static double scale(double value, int percent) {
        if (percent >= 100) return value;
        if (percent <= 0) return 0.0;
        return BigDecimal.valueOf(value)
            .multiply(BigDecimal.valueOf(percent))
            .movePointLeft(2)
            .setScale(2, RoundingMode.HALF_UP)
            .doubleValue();
    }

    /** Scales an mcMMO XP gain (a float) to the configured percent, same rounding as scale(). */
    public static float scaleFloat(float value, int percent) {
        if (percent >= 100) return value;
        if (percent <= 0) return 0.0f;
        return (float) scale(value, percent);
    }
}
