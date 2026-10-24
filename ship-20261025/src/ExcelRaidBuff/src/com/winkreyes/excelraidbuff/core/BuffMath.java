package com.winkreyes.excelraidbuff.core;

/**
 * The health arithmetic of applying, re-scaling and stripping a
 * raid buff. The rule everywhere is PROPORTIONAL: a raider keeps
 * the fraction of its health it had, so buffing a wounded joiner
 * never heals it and stripping never overheals past the vanilla
 * max. The glue caps at the live attribute value; this class owns
 * the raw transforms so they are unit-testable.
 */
public final class BuffMath {

    private BuffMath() {}

    /** First application: current health scaled by the HP factor. */
    public static double healthOnApply(double currentHealth, double healthFactor) {
        if (currentHealth <= 0.0) return currentHealth;
        return currentHealth * healthFactor;
    }

    /** Re-scale (config retune / repair): health follows the factor ratio. */
    public static double healthOnRescale(double currentHealth, double oldFactor, double newFactor) {
        if (currentHealth <= 0.0 || oldFactor <= 0.0) return currentHealth;
        return currentHealth * (newFactor / oldFactor);
    }

    /** Strip: the health stays, clamped to the post-strip max. */
    public static double healthOnStrip(double currentHealth, double maxAfterStrip) {
        if (currentHealth <= 0.0) return currentHealth;
        return Math.min(currentHealth, Math.max(1.0, maxAfterStrip));
    }

    /** Attribute modifier amount for a MULTIPLY_SCALAR_1 modifier. */
    public static double modifierAmount(double factor) {
        return factor - 1.0;
    }
}
