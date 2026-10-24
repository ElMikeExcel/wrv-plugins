package com.winkreyes.excelraidbuff.core;

/**
 * The buff health arithmetic: always PROPORTIONAL — applying never
 * heals a wounded raider past its fraction, stripping never
 * overheals past the vanilla max, and the dead are never touched.
 */
public final class BuffMathTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eq(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // Apply: health scales with the factor.
        eq(BuffMath.healthOnApply(24, 2.20), 52.8, "full pillager at omen V hp");
        eq(BuffMath.healthOnApply(12, 2.20), 26.4, "half-health pillager keeps its fraction");
        eq(BuffMath.healthOnApply(20, 1.0), 20.0, "identity factor changes nothing");
        eq(BuffMath.healthOnApply(0, 2.20), 0.0, "dead stays dead on apply");
        eq(BuffMath.healthOnApply(-4, 2.20), -4.0, "negative health untouched on apply");

        // Rescale (config retune): health follows the factor ratio.
        eq(BuffMath.healthOnRescale(52.8, 2.20, 1.50), 36.0, "retune down scales health down");
        eq(BuffMath.healthOnRescale(36.0, 1.50, 2.20), 52.8, "retune up scales health up");
        eq(BuffMath.healthOnRescale(30, 2.20, 2.20), 30.0, "same factor is a no-op");
        eq(BuffMath.healthOnRescale(30, 0.0, 2.20), 30.0, "zero old factor is a no-op, not a div-by-zero");
        eq(BuffMath.healthOnRescale(0, 1.5, 2.2), 0.0, "dead stays dead on rescale");

        // Strip: health clamps to the post-strip vanilla max.
        eq(BuffMath.healthOnStrip(52.8, 24), 24.0, "strip clamps to vanilla max");
        eq(BuffMath.healthOnStrip(10, 24), 10.0, "strip leaves wounded health alone");
        eq(BuffMath.healthOnStrip(24, 24), 24.0, "strip at exactly max is stable");
        eq(BuffMath.healthOnStrip(0, 24), 0.0, "dead stays dead on strip");
        eq(BuffMath.healthOnStrip(10, 0), 1.0, "degenerate max still leaves a living floor of 1");

        // Modifier amounts for MULTIPLY_SCALAR_1.
        eq(BuffMath.modifierAmount(1.0), 0.0, "identity modifier amount");
        eq(BuffMath.modifierAmount(2.20), 1.20, "omen V hp modifier amount");
        eq(BuffMath.modifierAmount(1.30), 0.30, "omen V dmg modifier amount");

        System.out.println("BuffMathTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
