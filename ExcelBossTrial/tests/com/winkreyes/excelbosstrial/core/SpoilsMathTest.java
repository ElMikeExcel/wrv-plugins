package com.winkreyes.excelbosstrial.core;

/**
 * Spoils: the multiplier is the in-range participant count, floored
 * at 1 (a solo or even unattended kill never multiplies), capped at
 * the settled 5; dragon XP scales 12,000 first / 500 repeat.
 */
public final class SpoilsMathTest {

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

    public static void main(String[] args) {
        check(SpoilsMath.multiplier(0, 5) == 1, "0 champions in range: no spoils (x1)");
        check(SpoilsMath.multiplier(1, 5) == 1, "solo kill: x1 - your own drops, nothing more");
        check(SpoilsMath.multiplier(2, 5) == 2, "2 champions: x2");
        check(SpoilsMath.multiplier(3, 5) == 3, "3 champions: x3");
        check(SpoilsMath.multiplier(5, 5) == 5, "5 champions: x5");
        check(SpoilsMath.multiplier(6, 5) == 5, "6 champions: capped at x5");
        check(SpoilsMath.multiplier(40, 5) == 5, "a crowd still caps at x5");
        check(SpoilsMath.multiplier(3, 3) == 3, "a lower configured cap is honored");
        check(SpoilsMath.multiplier(3, 0) == 1, "a junk cap of 0 clamps to 1 - spoils growth off, never negative");
        check(SpoilsMath.extraCopies(1) == 0, "x1 adds no copies");
        check(SpoilsMath.extraCopies(4) == 3, "x4 adds 3 copies of each drop");
        check(SpoilsMath.extraCopies(0) == 0, "x0 adds none (defensive)");

        check(SpoilsMath.dragonXp(true, 1) == 12_000, "first dragon, solo: 12,000 XP");
        check(SpoilsMath.dragonXp(true, 4) == 48_000, "first dragon, 4 champions: 48,000 XP");
        check(SpoilsMath.dragonXp(false, 1) == 500, "repeat dragon, solo: 500 XP");
        check(SpoilsMath.dragonXp(false, 5) == 2_500, "repeat dragon, 5 champions: 2,500 XP");
        check(SpoilsMath.dragonXpTopUp(true, 4, 12_000) == 36_000,
                "top-up beyond a 12,000 vanilla first kill at x4");
        check(SpoilsMath.dragonXpTopUp(false, 2, 500) == 500,
                "top-up beyond a 500 vanilla repeat at x2");
        check(SpoilsMath.dragonXpTopUp(true, 1, 12_000) == 0, "no top-up at x1");
        check(SpoilsMath.dragonXpTopUp(true, 2, 99_999) == 0, "top-up never goes negative");

        System.out.println("SpoilsMathTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
