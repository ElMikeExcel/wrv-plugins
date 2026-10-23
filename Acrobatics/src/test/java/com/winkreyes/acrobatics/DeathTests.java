package com.winkreyes.acrobatics;

import com.winkreyes.acrobatics.core.DeathMath;

public final class DeathTests {
    private static int checks = 0;

    private static void eq(Object actual, Object expected, String label) {
        checks++;
        if (!actual.equals(expected)) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    public static void main(String[] args) {
        // Vanilla XP curve spot values (known constants of the game).
        eq(DeathMath.totalXpForLevel(0), 0, "level 0");
        eq(DeathMath.totalXpForLevel(10), 160, "level 10 total");
        eq(DeathMath.totalXpForLevel(15), 315, "level 15 total");
        eq(DeathMath.totalXpForLevel(20), 550, "level 20 total");
        eq(DeathMath.totalXpForLevel(30), 1395, "level 30 total");
        eq(DeathMath.xpBarCapacity(0), 7, "bar at 0");
        eq(DeathMath.xpBarCapacity(15), 37, "bar at 15");
        eq(DeathMath.xpBarCapacity(30), 112, "bar at 30");

        // Death cost: level 20, empty bar, drops the vanilla cap of 100 orbs.
        eq(DeathMath.xpLostOnDeath(20, 0f, 100), 450, "lost = total - dropped");
        // Level 5 drops 35 orbs (7 per level): loses almost nothing beyond them.
        eq(DeathMath.xpLostOnDeath(5, 0f, 35), 20, "small level loses little");
        // keepInventory-style: nothing dropped and nothing lost cannot pass.
        eq(DeathMath.xpLostOnDeath(20, 0f, 550), 0, "full recovery loses zero");

        // The gate (Mike's rule): XP lost is the ONLY condition, 200 points.
        eq(DeathMath.gatePasses(250, 200), true, "lost 250: pays");
        eq(DeathMath.gatePasses(200, 200), true, "lost exactly 200: pays");
        eq(DeathMath.gatePasses(199, 200), false, "lost 199: blocked");
        eq(DeathMath.gatePasses(0, 200), false, "empty bar suicide: blocked");
        // A level-14 death with a nearly full bar loses ~228 and passes on loss alone.
        eq(DeathMath.xpLostOnDeath(14, 0.95f, 98) >= 200, true, "level 14 full bar loses enough");

        // Award math: 20 final damage at 600 = 12,000; FF compensation doubles.
        eq(DeathMath.deathAwardXp(20, 0, 600, 2.0, 25_000), 12_000, "plain death award");
        eq(DeathMath.deathAwardXp(20, 4, 600, 2.0, 25_000), 24_000, "FF compensated death award");
        eq(DeathMath.deathAwardXp(100, 0, 600, 2.0, 25_000), 25_000, "death award capped");
        eq(DeathMath.deathAwardXp(0, 0, 600, 2.0, 25_000), 0, "no damage no award");

        System.out.println("DeathTests: " + checks + " assertions passed");
    }
}
