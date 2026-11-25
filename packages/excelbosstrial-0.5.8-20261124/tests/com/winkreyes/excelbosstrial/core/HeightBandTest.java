/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * The warden height rule (Mike, Oct 19, 2026 evening amendment -
 * "tower hits don't feed the Warden seal"): a hit counts toward
 * the warden seal's share only when the responsible player stood
 * within +/-12 blocks vertically of the warden when the hit
 * landed. The damage itself is never reduced - out-of-band hits
 * land in the ledger's total exactly like unattributed damage,
 * they add nothing to the share and no participation.
 *
 * The adapter resolves the eligibility input from the RESPONSIBLE
 * PLAYER's position at hit time; for a projectile that is the
 * SHOOTER's position (the arrow itself is already at the boss
 * when it lands, so its own Y would always read in-band). These
 * tests therefore feed the predicate the shooter's Y, per that
 * contract.
 */
public final class HeightBandTest {

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

    private static void eq(double got, double want, String label) {
        check(Math.abs(got - want) < 1e-9, label + " (got " + got + ", want " + want + ")");
    }

    public static void main(String[] args) {
        double bossY = 64.0;
        double band = 12.0;

        // --- band edges, both directions (symmetric) -------------
        check(CreditJudge.withinHeightBand(bossY + 12.0, bossY, band),
                "edge: +12 counts (inclusive)");
        check(!CreditJudge.withinHeightBand(bossY + 13.0, bossY, band),
                "edge: +13 does not count");
        check(CreditJudge.withinHeightBand(bossY - 12.0, bossY, band),
                "edge: -12 counts (the underground stab is symmetric, in-band at -12)");
        check(!CreditJudge.withinHeightBand(bossY - 13.0, bossY, band),
                "edge: -13 does not count (band is symmetric)");
        check(CreditJudge.withinHeightBand(bossY, bossY, band),
                "same level always counts");
        check(CreditJudge.withinHeightBand(bossY + 500.0, bossY, 0.0),
                "band 0 disables the rule (every hit counts)");
        check(CreditJudge.withinHeightBand(bossY + 500.0, bossY, -3.0),
                "negative band also disables the rule");

        // --- kind gate: the band is WARDEN ONLY -------------------
        check(CreditJudge.countsTowardShare(BossKind.WARDEN, bossY + 25.0, bossY, band) == false,
                "warden: sky-tower hit (+25) excluded");
        check(CreditJudge.countsTowardShare(BossKind.WARDEN, bossY + 5.0, bossY, band),
                "warden: ground-level hit (+5) counts");
        check(CreditJudge.countsTowardShare(BossKind.WITHER, bossY + 25.0, bossY, band),
                "wither: unaffected by the band (it flies to its attackers)");
        check(CreditJudge.countsTowardShare(BossKind.ELDER_GUARDIAN, bossY - 40.0, bossY, band),
                "elder guardian: unaffected by the band (underwater fight)");
        check(CreditJudge.countsTowardShare(BossKind.ENDER_DRAGON, bossY + 90.0, bossY, band),
                "dragon: unaffected by the band (no share gate at all)");

        // --- config override widens the band ----------------------
        check(CreditJudge.countsTowardShare(BossKind.WARDEN, bossY + 15.0, bossY, 20.0),
                "override band 20: +15 counts");
        check(!CreditJudge.countsTowardShare(BossKind.WARDEN, bossY + 21.0, bossY, 20.0),
                "override band 20: +21 excluded");

        // --- ledger: out-of-band = unattributed for share --------
        UUID boss = UUID.randomUUID();
        UUID sniper = UUID.randomUUID();
        UUID fighter = UUID.randomUUID();
        EncounterLedger l = new EncounterLedger(BossKind.WARDEN, boss, 500.0, 0L);

        // Projectile case, judged by the SHOOTER's position: the
        // shooter stands on a +25 tower (their Y is what the
        // adapter passes), the arrows land for 400 of the 500 HP.
        boolean towerCounts = CreditJudge.countsTowardShare(
                BossKind.WARDEN, bossY + 25.0, bossY, band);
        l.recordDealt(sniper, 400.0, 100L, towerCounts);
        eq(l.totalDamage(), 400.0, "tower arrows still land: total damage 400");
        eq(l.playerDamage(), 0.0, "tower arrows feed the share 0");
        eq(l.playerSharePercent(), 0.0, "counted share is 0% after a near-kill from the tower");
        check(!l.isParticipant(sniper), "a pure tower sniper is not a participant");
        check(!l.shareMet(0.5), "pure tower kill cannot meet the share gate -> DENY_SHARE");

        // One honest ground swing lands in-band and is all that counts.
        boolean groundCounts = CreditJudge.countsTowardShare(
                BossKind.WARDEN, bossY + 1.0, bossY, band);
        l.recordDealt(fighter, 8.0, 200L, groundCounts);
        eq(l.totalDamage(), 408.0, "total includes both tower and ground damage");
        eq(l.playerDamage(), 8.0, "only the ground swing counts toward the share");
        eq(l.playerSharePercent(), 1.6, "verdict share names ONLY the ground damage (1.6%)");
        check(l.isParticipant(fighter), "the ground fighter is a participant");
        check(!l.shareMet(0.5), "1.6% still denies the seal (no near-miss mercy)");

        // A real ground fight still credits exactly as before.
        EncounterLedger g = new EncounterLedger(BossKind.WARDEN, boss, 500.0, 0L);
        g.recordDealt(fighter, 250.0, 100L,
                CreditJudge.countsTowardShare(BossKind.WARDEN, bossY, bossY, band));
        check(g.shareMet(0.5), "ground fight at exactly 50% still credits (CREDIT path intact)");
        eq(g.playerSharePercent(), 50.0, "ground fight share reads 50%");

        // The 3-arg recordDealt (every other boss) is untouched:
        // it always counts toward the share.
        EncounterLedger w = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        w.recordDealt(fighter, 150.0, 100L);
        eq(w.playerDamage(), 150.0, "legacy recordDealt still counts in full");

        System.out.println("HeightBandTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

