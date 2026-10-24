package com.winkreyes.excelbosstrial.core;

/**
 * The per-boss caps in action: the elder guardian's 35/40 pair
 * (chosen so a point-blank Impaling V trident crit at ~31 lands
 * WHOLE), wither/warden 60/75, the per-tick explosion pool
 * (same-tick chains starve, next tick refills), non-explosion hits
 * never touching the pool, and caps off = vanilla.
 */
public final class BossGovernorTest {

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
        BossCaps elder = new BossCaps(35, 40);
        BossCaps wither = new BossCaps(60, 75);

        // The trident case: the monument's own best legit hit (~31,
        // Impaling V crit) must pass the elder cap untouched.
        BossGovernor g = new BossGovernor();
        BossGovernor.Result r = g.govern(31.0, false, 100L, elder);
        eq(r.damage, 31.0, "Impaling V trident crit vs elder lands whole");
        check(!r.trimmed(), "legit hit is not flagged trimmed");

        // A stacked 50-damage burst against an elder trims to the cap.
        r = g.govern(50.0, false, 101L, elder);
        eq(r.damage, 35.0, "elder per-event cap trims 50 to 35");
        check(r.perEventTrimmed && !r.budgetTrimmed, "P1 label for a melee trim");

        // Sword hits at the wither cap boundary.
        BossGovernor g2 = new BossGovernor();
        eq(g2.govern(60.0, false, 1L, wither).damage, 60.0, "exactly at the cap passes whole");
        eq(g2.govern(60.5, false, 2L, wither).damage, 60.0, "just over the cap trims to it");

        // Explosion pool: two 50-blasts the same tick vs the wither
        // (75 budget): first lands whole, second gets only 25.
        BossGovernor g3 = new BossGovernor();
        BossGovernor.Result b1 = g3.govern(50.0, true, 7L, wither);
        eq(b1.damage, 50.0, "first blast lands whole");
        BossGovernor.Result b2 = g3.govern(50.0, true, 7L, wither);
        eq(b2.damage, 25.0, "same-tick second blast starves to the pool remainder");
        check(b2.budgetTrimmed, "P2 label for a pool trim");
        BossGovernor.Result b3 = g3.govern(50.0, true, 7L, wither);
        eq(b3.damage, 0.0, "a drained pool zeroes further same-tick blasts");
        BossGovernor.Result b4 = g3.govern(50.0, true, 8L, wither);
        eq(b4.damage, 50.0, "the next tick refills the pool");

        // Per-event cap applies to explosions BEFORE the pool sees
        // them (a 100-blast vs the wither: P1 to 60, pool 75 -> 60 lands).
        BossGovernor g4 = new BossGovernor();
        BossGovernor.Result big = g4.govern(100.0, true, 1L, wither);
        eq(big.damage, 60.0, "P1 trims the mega-blast before the pool");
        check(big.perEventTrimmed && !big.budgetTrimmed, "only P1 fired");
        eq(g4.poolRemaining(), 15.0, "the pool spent the governed 60, not the raw 100");

        // Elder pool (40): one 40-blast exactly drains it.
        BossGovernor g5 = new BossGovernor();
        eq(g5.govern(40.0, true, 1L, elder).damage, 35.0,
                "elder blast is P1-trimmed to 35 first");
        eq(g5.poolRemaining(), 5.0, "pool keeps 40 - 35");
        eq(g5.govern(40.0, true, 1L, elder).damage, 5.0,
                "second same-tick elder blast gets the 5 left");

        // Melee never touches the pool.
        BossGovernor g6 = new BossGovernor();
        g6.govern(20.0, false, 3L, wither);
        eq(g6.poolRemaining(), 0.0, "pool untouched before any explosion tick");
        eq(g6.govern(70.0, true, 3L, wither).damage, 60.0,
                "explosion after melee still sees a full pool (P1 trims to 60)");

        // Caps off: everything passes, DragonTrial OFF semantics.
        BossGovernor g7 = new BossGovernor();
        BossGovernor.Result off = g7.govern(1000.0, true, 1L, BossCaps.OFF);
        eq(off.damage, 1000.0, "caps off = vanilla damage");
        check(!off.trimmed(), "caps off never trims");
        BossGovernor.Result nul = g7.govern(1000.0, true, 1L, null);
        eq(nul.damage, 1000.0, "null caps = vanilla damage");

        // Junk inputs degrade, never explode.
        eq(g7.govern(-5.0, false, 1L, wither).damage, 0.0, "negative damage clamps to 0");
        eq(g7.govern(Double.NaN, false, 1L, wither).damage, 0.0, "NaN damage clamps to 0");

        // BossCaps clamps junk config values to off.
        check(!new BossCaps(-1, -1).anyEnabled(), "negative caps are off");
        check(new BossCaps(60, 0).perEventEnabled() && !new BossCaps(60, 0).explosionEnabled(),
                "each cap switches independently");

        System.out.println("BossGovernorTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
