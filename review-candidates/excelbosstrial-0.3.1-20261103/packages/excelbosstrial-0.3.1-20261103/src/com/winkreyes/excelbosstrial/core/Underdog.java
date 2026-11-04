/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The underdog backbone (ExcelBossTrial 0.3.0, SPEC section 4.1):
 * on the REPEAT layer only, the rolled quantities scale by
 *
 *     U(n) = clamp(4 / n, 1, 2)        (n = champions standing)
 *
 * so a repeat cleared short-handed pays each standing champion a
 * bigger cut - a solo repeat pays double rolls, a pair 2x, a trio
 * 1.33x, a full four exactly the flat slate, and a crowd of five
 * or more never dilutes below the slate (the floor at 1). U
 * scales ONLY jackpot roll counts and the XP accelerants; the
 * signature set, its reforged form, the war-chest rows, the
 * cosmetics and the titles never scale - the spec's list is
 * exhaustive, and this class is deliberately the only place the
 * curve lives so the boundary stays testable.
 */
public final class Underdog {

    private Underdog() {
    }

    /** U(n) for n champions standing; n below 1 reads as a full four (U = 1). */
    public static double factor(int champions) {
        if (champions <= 0) {
            return 1.0;
        }
        double u = 4.0 / champions;
        if (u < 1.0) {
            return 1.0;
        }
        return Math.min(2.0, u);
    }

    /**
     * A rolled stack count under U: floor(base * U), never below
     * 1 for a positive base - the spec's floor rule (a scaled
     * count of rolled loot is at least one of the thing).
     */
    public static int scaledCount(int baseCount, int champions) {
        if (baseCount <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.floor(baseCount * factor(champions)));
    }

    /** A flat XP amount under U (accelerants only): floor(base * U). */
    public static long scaledXp(long baseAmount, int champions) {
        if (baseAmount <= 0) {
            return 0;
        }
        return (long) Math.floor(baseAmount * factor(champions));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
