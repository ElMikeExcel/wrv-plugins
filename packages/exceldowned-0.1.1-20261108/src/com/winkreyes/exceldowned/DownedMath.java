package com.winkreyes.exceldowned;

/**
 * Pure damage math for the downed state (unit-tested, no Bukkit).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedMath {

    private DownedMath() {}

    /** Damage that reaches a downed player, scaled by the downed factor. */
    public static double scaleDamage(double baseDamage, double factor) {
        if (baseDamage <= 0) return 0;
        return baseDamage * factor;
    }

    /**
     * Two multiplicative scalings of the same event compose regardless of
     * listener order: TrainingWheels' factor and the downed factor multiply.
     * Rylie downed = 0.25 x 0.25 = 0.0625 of the raw hit reaching the window.
     */
    public static double composeFactor(double first, double second) {
        return first * second;
    }

    /** Bleed-out seconds eaten by a scaled damage amount. */
    public static long windowMillisEaten(double scaledDamage, double secondsPerPoint) {
        if (scaledDamage <= 0 || secondsPerPoint <= 0) return 0;
        return Math.round(scaledDamage * secondsPerPoint * 1000.0);
    }
}
