/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The cash taper table (0.5.6, Mike's law Nov 10, 2026):
 * per-player per-seal LIFETIME counts; Warden #1 $10,000, every
 * other seal #1 $5,000, #2 $5,000, #3+ $2,000 — and the
 * cash-only pins: the non-cash slate (mcMMO 5,000 SWORDS,
 * MagicLevel 250, jackpot +2, war chest) is NOT tapered.
 */
public final class CashTaperTest {

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

    private static void eq(long expected, long actual, String label) {
        check(expected == actual, label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        String warden = BossKind.WARDEN.sealId();
        String wither = BossKind.WITHER.sealId();
        String elder = BossKind.ELDER_GUARDIAN.sealId();
        String dragon = BossKind.ENDER_DRAGON.sealId();
        String raid = CashTaper.RAID_SEAL_ID;

        // The taper table, every seal.
        eq(10000, CashTaper.cashFor(warden, 1), "warden clear #1 pays 10000");
        eq(5000, CashTaper.cashFor(warden, 2), "warden clear #2 pays 5000");
        eq(2000, CashTaper.cashFor(warden, 3), "warden clear #3 pays 2000");
        eq(2000, CashTaper.cashFor(warden, 4), "warden clear #4 pays 2000");
        eq(2000, CashTaper.cashFor(warden, 25), "warden clear #25 pays 2000");
        for (String seal : new String[] {wither, elder, dragon, raid}) {
            eq(5000, CashTaper.cashFor(seal, 1), seal + " clear #1 pays 5000");
            eq(5000, CashTaper.cashFor(seal, 2), seal + " clear #2 pays 5000");
            eq(2000, CashTaper.cashFor(seal, 3), seal + " clear #3 pays 2000");
            eq(2000, CashTaper.cashFor(seal, 9), seal + " clear #9 pays 2000");
        }
        // BossKind overload agrees with the seal-id form.
        eq(10000, CashTaper.cashFor(BossKind.WARDEN, 1), "BossKind warden #1");
        eq(5000, CashTaper.cashFor(BossKind.ELDER_GUARDIAN, 1), "BossKind elder #1");
        eq(2000, CashTaper.cashFor(BossKind.WITHER, 3), "BossKind wither #3");
        // A lost count (0 / negative) clamps to the first tier,
        // never below it and never to the floor.
        eq(10000, CashTaper.cashFor(warden, 0), "warden count 0 clamps to first tier");
        eq(5000, CashTaper.cashFor(elder, -3), "elder count -3 clamps to first tier");
        // An unknown seal prices as a non-Warden seal.
        eq(5000, CashTaper.cashFor("some_future_seal", 1), "unknown seal #1 pays 5000");
        eq(2000, CashTaper.cashFor("some_future_seal", 3), "unknown seal #3 pays 2000");
        check(CashTaper.isWarden(warden), "isWarden(warden)");
        check(!CashTaper.isWarden(elder), "isWarden(elder) is false");

        // CASH-ONLY pins: the rest of the slate pays in full at
        // every count. The settled specs are the source of the
        // numbers the host pays from; the taper class holds no
        // lever over them.
        FirstBreakRewards.Spec fb = FirstBreakRewards.Spec.settled();
        eq(5000, fb.mcMmoXp(), "first-break mcMMO XP stays 5000 (not tapered)");
        eq(250, fb.magicLevelXp(), "first-break MagicLevel XP stays 250 (not tapered)");
        eq(2, fb.jackpotExtraRolls(), "first-break jackpot stays +2 rolls (not tapered)");
        check(fb.claimflyHours() == 2.0, "first-break war chest claimfly stays 2.0h");
        check(fb.deathProtectionPremium(), "first-break war chest premium stays on");
        BossTrialConfig cfg = BossTrialConfig.defaults();
        eq(5000, cfg.repeatMcMmoXp, "repeat mcMMO XP default stays 5000");
        eq(250, cfg.repeatMagicLevelXp, "repeat MagicLevel XP default stays 250");
        eq(2, cfg.repeatJackpotExtraRolls, "repeat jackpot default stays +2");
        check(cfg.repeatClaimflyHours == 2.0, "repeat war chest claimfly default stays 2.0h");
        check(cfg.repeatDeathProtectionPremium, "repeat war chest premium default stays on");
        // Underdog scaling for the non-cash rows is unchanged.
        check(Underdog.scaledXp(5000, 1) == 10000, "underdog solo mcMMO still x2");
        check(Underdog.scaledXp(5000, 4) == 5000, "underdog full party mcMMO still x1");

        System.out.println("CashTaperTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
