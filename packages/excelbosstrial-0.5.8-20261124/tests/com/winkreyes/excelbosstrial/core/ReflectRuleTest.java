/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Spike reflect (0.3.5): the multiplier math, the identification
 * predicate, and the compositions the fight depends on - the
 * attrition ramp must never resurrect reflect at multiplier 0,
 * and TrainingWheels' 0.25 child factor must compose to 0 in
 * either order. The vanilla spike on Hard is 3 HP; that is the
 * raw value used throughout.
 */
public final class ReflectRuleTest {

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

    private static void close(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // --- The settled multipliers on the vanilla Hard spike (3 HP).
        close(ReflectRule.scaledDamage(3.0, 0.0), 0.0,
                "multiplier 0.0: reflect retired, exactly 0");
        close(ReflectRule.scaledDamage(3.0, 0.25), 0.75,
                "multiplier 0.25: a quarter sting remains");
        close(ReflectRule.scaledDamage(3.0, 1.0), 3.0,
                "multiplier 1.0: exact vanilla");
        close(ReflectRule.scaledDamage(3.0, 2.0), 6.0,
                "multiplier above 1 allowed (a future chapter may turn reflect up)");

        // --- Multiplier clamps: never inverted, never NaN.
        close(ReflectRule.clampMultiplier(-0.5), 0.0,
                "negative multiplier reads as 0 (reflect never becomes healing)");
        close(ReflectRule.clampMultiplier(Double.NaN), 0.0,
                "NaN multiplier reads as 0 (a broken value retires reflect, never restores the tax)");
        close(ReflectRule.scaledDamage(3.0, -1.0), 0.0,
                "scaled damage under a negative multiplier is 0, not negative");
        close(ReflectRule.scaledDamage(3.0, Double.NaN), 0.0,
                "scaled damage under a NaN multiplier is 0, not NaN");

        // --- Raw-damage hygiene: the fight table stays clean.
        close(ReflectRule.scaledDamage(Double.NaN, 1.0), 0.0,
                "NaN raw damage reads as 0");
        close(ReflectRule.scaledDamage(-3.0, 1.0), 0.0,
                "negative raw damage reads as 0");
        close(ReflectRule.scaledDamage(0.0, 1.0), 0.0, "zero raw stays zero");
        boolean clean = true;
        for (double raw : new double[] {0.0, 0.5, 2.0, 3.0, 5.0, 100.0}) {
            for (double mult : new double[] {-1.0, 0.0, 0.25, 0.5, 1.0, 2.0, Double.NaN}) {
                double out = ReflectRule.scaledDamage(raw, mult);
                if (Double.isNaN(out) || Double.isInfinite(out) || out < 0.0) {
                    clean = false;
                }
            }
        }
        check(clean, "sweep: scaled damage is always finite and >= 0 (no negatives/NaN in the table)");

        // --- Cancel semantics.
        check(ReflectRule.cancels(0.0), "scaled 0 cancels the event outright");
        check(ReflectRule.cancels(ReflectRule.scaledDamage(3.0, 0.0)),
                "multiplier 0.0 composes to a cancel");
        check(!ReflectRule.cancels(0.75), "scaled 0.75 lands (no cancel)");
        check(!ReflectRule.cancels(3.0), "scaled 3.0 lands (no cancel)");

        // --- Ramp independence: reflect REPLACES the pressure scale.
        // The handler applies scaledDamage INSTEAD of outgoingScale;
        // simulate the wrong composition too, to pin why the order
        // matters: at minute 25 the elder ramp is x2.0.
        double rampAt25 = PressureMath.outgoingScale(1.0, 25, 0.06, 2.0);
        close(rampAt25, 2.0, "fixture: the elder pressure ramp at minute 25 is x2.0");
        close(ReflectRule.scaledDamage(3.0, 0.0), 0.0,
                "multiplier 0 at minute 25: reflect is 0 - the ramp never sees the event");
        check(ReflectRule.scaledDamage(3.0, 0.0) * rampAt25 == 0.0,
                "even if a future edit multiplied the ramp in afterwards, 0 x ramp stays 0");
        close(ReflectRule.scaledDamage(3.0, 1.0), 3.0,
                "multiplier 1.0 at minute 25: exact vanilla 3.0, NOT the ramped 6.0 of 0.3.4");

        // --- The identification predicate.
        check(ReflectRule.isSpikeReflect(true, true, BossKind.ELDER_GUARDIAN),
                "THORNS + tracked elder damager + elder kind: spike reflect");
        check(!ReflectRule.isSpikeReflect(false, true, BossKind.ELDER_GUARDIAN),
                "non-THORNS cause from a tracked elder (the laser): not reflect");
        check(!ReflectRule.isSpikeReflect(true, false, BossKind.ELDER_GUARDIAN),
                "THORNS from an untracked elder (no open encounter): not ours to scale");
        check(!ReflectRule.isSpikeReflect(true, true, BossKind.WITHER),
                "THORNS-shaped event from a wither: not reflect (withers have no spikes)");
        check(!ReflectRule.isSpikeReflect(true, true, BossKind.WARDEN),
                "THORNS-shaped event from a warden: not reflect");
        check(!ReflectRule.isSpikeReflect(true, true, BossKind.ENDER_DRAGON),
                "THORNS-shaped event from the dragon: not reflect");
        check(!ReflectRule.isSpikeReflect(true, true, null),
                "THORNS with no boss kind (a player damager - Thorns armor): not reflect");
        check(!ReflectRule.isSpikeReflect(false, false, null),
                "nothing matching: not reflect");

        // --- TrainingWheels composition (Rylie, 0.25 damage factor):
        // her melee reflect at multiplier 0 is 0 in either order -
        // no special-casing in the handler, proven arithmetically.
        double tw = 0.25;
        close(ReflectRule.scaledDamage(3.0, 0.0) * tw, 0.0,
                "Rylie: reflect scaled to 0 first, then TW 0.25: still 0");
        close(ReflectRule.scaledDamage(3.0 * tw, 0.0), 0.0,
                "Rylie: TW 0.25 applied to the raw spike first, then reflect 0: still 0");
        close(ReflectRule.scaledDamage(3.0, 0.25) * tw, 0.1875,
                "Rylie at multiplier 0.25: 3 x 0.25 (reflect) x 0.25 (TW) = 0.1875 - composition is plain multiplication");

        System.out.println("ReflectRuleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
