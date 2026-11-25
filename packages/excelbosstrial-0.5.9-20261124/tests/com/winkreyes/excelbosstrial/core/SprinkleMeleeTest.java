/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The sprinkle melee rules (0.5.4, the elder sprinkle melee
 * fix): the knockback set/unset/clamp/applied table and the
 * stagger delay table - first add immediate, last add exactly
 * at the window's end, adds spread evenly between. Pure
 * logic; the adapter's half (attribute write after conversion,
 * tick-scheduled spawns) is rig- and player-proven.
 */
public final class SprinkleMeleeTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eqD(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // --- the unset state
        check(!SprinkleMelee.knockbackSet(SprinkleMelee.KNOCKBACK_UNSET),
                "NaN reads unset (the chapter keeps the conversion's attribute)");
        check(SprinkleMelee.knockbackSet(-1.0) && SprinkleMelee.knockbackSet(0.0)
                        && SprinkleMelee.knockbackSet(1.0),
                "every finite value reads set, including an explicit 0.0");
        check(Double.isNaN(SprinkleMelee.sanitizedKnockback(Double.NaN)),
                "sanitize keeps NaN unset (unset is a state, not an error)");
        check(Double.isNaN(SprinkleMelee.sanitizedKnockback(Double.POSITIVE_INFINITY))
                        && Double.isNaN(SprinkleMelee.sanitizedKnockback(
                                Double.NEGATIVE_INFINITY)),
                "sanitize fails an infinite knockback to unset");

        // --- the configured clamp [-1.0, 1.0]
        eqD(SprinkleMelee.sanitizedKnockback(-1.0), -1.0, "the -1.0 floor loads exact");
        eqD(SprinkleMelee.sanitizedKnockback(1.0), 1.0, "the 1.0 ceiling loads exact");
        eqD(SprinkleMelee.sanitizedKnockback(-5.0), -1.0, "below the floor clamps to -1.0");
        eqD(SprinkleMelee.sanitizedKnockback(5.0), 1.0, "above the ceiling clamps to 1.0");
        eqD(SprinkleMelee.sanitizedKnockback(0.35), 0.35, "an in-range value loads untouched");

        // --- the applied value: negative asks land at the
        // vanilla attribute floor 0.0 (full vanilla knockback)
        eqD(SprinkleMelee.appliedKnockback(-1.0), 0.0,
                "the elder's -1.0 applies at 0.0 - maximum knockback the game gives");
        eqD(SprinkleMelee.appliedKnockback(-0.25), 0.0,
                "any negative ask applies at the 0.0 floor");
        eqD(SprinkleMelee.appliedKnockback(0.0), 0.0, "an explicit 0.0 applies at 0.0");
        eqD(SprinkleMelee.appliedKnockback(0.35), 0.35, "a positive ask applies as written");
        eqD(SprinkleMelee.appliedKnockback(1.0), 1.0, "full immunity applies as written");

        // --- the stagger delay table (window 40 ticks = 2 s,
        // the elder's shipped value)
        check(SprinkleMelee.delayTicks(0, 6, 40) == 0, "the first add is immediate");
        check(SprinkleMelee.delayTicks(1, 6, 40) == 8
                        && SprinkleMelee.delayTicks(2, 6, 40) == 16
                        && SprinkleMelee.delayTicks(3, 6, 40) == 24
                        && SprinkleMelee.delayTicks(4, 6, 40) == 32
                        && SprinkleMelee.delayTicks(5, 6, 40) == 40,
                "a 6-add wave spreads 0/8/16/24/32/40 across the 2 s window");
        check(SprinkleMelee.delayTicks(0, 4, 40) == 0
                        && SprinkleMelee.delayTicks(1, 4, 40) == 13
                        && SprinkleMelee.delayTicks(2, 4, 40) == 27
                        && SprinkleMelee.delayTicks(3, 4, 40) == 40,
                "a 4-add wave spreads 0/13/27/40 (rounded, last exact)");
        check(SprinkleMelee.delayTicks(0, 5, 40) == 0
                        && SprinkleMelee.delayTicks(1, 5, 40) == 10
                        && SprinkleMelee.delayTicks(4, 5, 40) == 40,
                "a 5-add wave spreads by tens, last exact");
        check(SprinkleMelee.delayTicks(0, 2, 40) == 0
                        && SprinkleMelee.delayTicks(1, 2, 40) == 40,
                "a 2-add wave is first-immediate, second at the window's end");

        // --- the instant-ring retirements: 0 window, one-add
        // waves, and bad indexes all read 0
        check(SprinkleMelee.delayTicks(3, 6, 0) == 0, "a 0 window is the instant ring");
        check(SprinkleMelee.delayTicks(0, 1, 40) == 0
                        && SprinkleMelee.delayTicks(1, 1, 40) == 0,
                "a one-add wave never staggers");
        check(SprinkleMelee.delayTicks(-1, 6, 40) == 0, "a negative index reads 0");
        check(SprinkleMelee.delayTicks(9, 6, 40) == 40,
                "an index past the end reads the window's end, never beyond");

        System.out.println("SprinkleMeleeTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
