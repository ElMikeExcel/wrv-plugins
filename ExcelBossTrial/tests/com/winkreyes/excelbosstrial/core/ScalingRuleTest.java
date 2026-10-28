package com.winkreyes.excelbosstrial.core;

/**
 * Participant-growth pool math (0.2 spec section 2): the settled
 * per-boss tables, the damager cap, the off switch, and the
 * fail-soft clamps.
 */
public final class ScalingRuleTest {

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
        // Settled wither table: 750 + 200 per extra damager, cap 5.
        ScalingRule wither = ScalingRule.settledDefault(BossKind.WITHER);
        eq(wither.poolFor(0), 750, "wither pool before anyone (base)");
        eq(wither.poolFor(1), 750, "wither solo pool is the base");
        eq(wither.poolFor(2), 950, "wither duo pool");
        eq(wither.poolFor(3), 1150, "wither trio pool");
        eq(wither.poolFor(4), 1350, "wither four pool");
        eq(wither.poolFor(5), 1550, "wither five pool (the cap)");
        eq(wither.poolFor(6), 1550, "wither pool never grows past the cap");
        eq(wither.poolFor(40), 1550, "wither pool at absurd counts still capped");

        // Settled elder table: 400 + 50, max 600.
        ScalingRule elder = ScalingRule.settledDefault(BossKind.ELDER_GUARDIAN);
        eq(elder.poolFor(1), 400, "elder solo pool");
        eq(elder.poolFor(2), 450, "elder duo pool");
        eq(elder.poolFor(3), 500, "elder trio pool");
        eq(elder.poolFor(5), 600, "elder five pool (max 600)");
        eq(elder.poolFor(9), 600, "elder pool capped");

        // Settled warden table: 800 + 150, max 1400.
        ScalingRule warden = ScalingRule.settledDefault(BossKind.WARDEN);
        eq(warden.poolFor(1), 800, "warden solo pool");
        eq(warden.poolFor(2), 950, "warden duo pool");
        eq(warden.poolFor(3), 1100, "warden trio pool");
        eq(warden.poolFor(5), 1400, "warden five pool (max 1400)");

        // The dragon never scales by growth (its phases are its scaler).
        check(ScalingRule.settledDefault(BossKind.ENDER_DRAGON) == ScalingRule.OFF,
                "dragon scaling default is OFF");
        check(!ScalingRule.OFF.active(), "OFF rule is not active");
        check(wither.active(), "settled wither rule is active");

        // A disabled rule with numbers is still off.
        ScalingRule disabled = new ScalingRule(false, 750, 200, 5);
        check(!disabled.active(), "enabled=false is off even with numbers");
        eq(disabled.poolFor(3), 750, "disabled rule poolFor still reports base");

        // An enabled rule with no base is off (degrades to vanilla).
        check(!new ScalingRule(true, 0, 200, 5).active(), "base 0 = inactive");

        // Fail-soft clamps: NaN / infinities / negatives degrade, never throw.
        ScalingRule junk = new ScalingRule(true, Double.NaN, -50, 0);
        eq(junk.baseHealth(), 0, "NaN base clamps to 0");
        eq(junk.perExtraDamager(), 0, "negative step clamps to 0");
        check(junk.maxDamagersCounted() == 1, "damager cap clamps to >= 1");
        ScalingRule huge = new ScalingRule(true, 1e12, 1e12, 5000);
        check(huge.baseHealth() == 1_000_000, "base clamps to the ceiling");
        check(huge.maxDamagersCounted() == 100, "damager cap clamps to 100");

        // A zero step is a flat pool (base only) - a legitimate tuning.
        ScalingRule flat = new ScalingRule(true, 500, 0, 5);
        eq(flat.poolFor(1), 500, "flat rule solo");
        eq(flat.poolFor(5), 500, "flat rule full party");

        System.out.println("ScalingRuleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

