package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Seal feedback doctrine (Mike, Oct 19, 2026): silent during the
 * fight, but every failed seal names its specific reason
 * afterward. The one gap was the warden height denial - a tower
 * sniper denied under the generic "% dealt by players" text reads
 * it as a bug, because his arrows DID land. These checks pin the
 * selection (warden + share denial + at least one height-excluded
 * hit -> the height text; everything else unchanged) and the
 * ledger fact the selection reads.
 */
public final class DenialMessageTest {

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

    private static final CreditJudge.Verdict CREDIT = CreditJudge.Verdict.CREDIT;
    private static final CreditJudge.Verdict DENY_SHARE = CreditJudge.Verdict.DENY_SHARE;
    private static final CreditJudge.Verdict DENY_PRESENCE = CreditJudge.Verdict.DENY_PRESENCE;
    private static final CreditJudge.Verdict DENY_COOLDOWN = CreditJudge.Verdict.DENY_COOLDOWN;
    private static final DenialMessage.Slot SHARE = DenialMessage.Slot.SHARE;
    private static final DenialMessage.Slot SHARE_HEIGHT = DenialMessage.Slot.SHARE_HEIGHT;
    private static final DenialMessage.Slot PRESENCE = DenialMessage.Slot.PRESENCE;
    private static final DenialMessage.Slot COOLDOWN = DenialMessage.Slot.COOLDOWN;
    private static final DenialMessage.Slot NONE = DenialMessage.Slot.NONE;

    public static void main(String[] args) {
        // The height variant: warden + share denial + exclusions.
        check(DenialMessage.select(BossKind.WARDEN, DENY_SHARE, 1) == SHARE_HEIGHT,
                "warden denied on share with ONE excluded hit: height text");
        check(DenialMessage.select(BossKind.WARDEN, DENY_SHARE, 92) == SHARE_HEIGHT,
                "warden denied on share with 92 excluded hits (the tower run): height text");

        // Warden share denial with ZERO exclusions stays generic -
        // a golem-army warden kill must not blame the height rule.
        check(DenialMessage.select(BossKind.WARDEN, DENY_SHARE, 0) == SHARE,
                "warden denied on share with zero exclusions: generic share text");

        // The band is warden-only in the selection too: any other
        // boss keeps the generic text even if a count were passed.
        check(DenialMessage.select(BossKind.WITHER, DENY_SHARE, 5) == SHARE,
                "wither share denial unaffected by an exclusion count: generic text");
        check(DenialMessage.select(BossKind.ELDER_GUARDIAN, DENY_SHARE, 3) == SHARE,
                "elder share denial unaffected: generic text (elder denials ride the shared messages)");
        check(DenialMessage.select(BossKind.ENDER_DRAGON, DENY_SHARE, 3) == SHARE,
                "dragon (no share gate in play) unaffected: generic text");

        // Other verdicts are never rerouted by exclusions.
        check(DenialMessage.select(BossKind.WARDEN, DENY_PRESENCE, 5) == PRESENCE,
                "warden presence denial with exclusions: presence text, not height");
        check(DenialMessage.select(BossKind.WARDEN, DENY_COOLDOWN, 5) == COOLDOWN,
                "warden cooldown denial with exclusions: cooldown text, not height");
        check(DenialMessage.select(BossKind.WITHER, DENY_PRESENCE, 0) == PRESENCE,
                "wither presence denial: presence text");
        check(DenialMessage.select(BossKind.ELDER_GUARDIAN, DENY_COOLDOWN, 0) == COOLDOWN,
                "elder cooldown denial: cooldown text");

        // Credits and junk get no denial text at all.
        check(DenialMessage.select(BossKind.WARDEN, CREDIT, 12) == NONE,
                "a credited warden kill has no denial text");
        check(DenialMessage.select(BossKind.WITHER, CREDIT, 0) == NONE,
                "a credited wither kill has no denial text");
        check(DenialMessage.select(BossKind.WARDEN, null, 3) == NONE,
                "a null verdict selects nothing (defensive)");

        ledgerFact();
        towerScenario();

        System.out.println("DenialMessageTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }

    /** The fact the selection reads: the ledger's exclusion count. */
    private static void ledgerFact() {
        UUID boss = UUID.randomUUID();
        UUID sniper = UUID.randomUUID();
        UUID fighter = UUID.randomUUID();
        EncounterLedger ledger = new EncounterLedger(BossKind.WARDEN, boss, 500, 0);
        check(ledger.shareExcludedHits() == 0 && ledger.shareExcludedDamage() == 0,
                "a fresh encounter has no exclusions");
        ledger.recordDealt(sniper, 40, 1, false);
        check(ledger.shareExcludedHits() == 1 && ledger.shareExcludedDamage() == 40,
                "one out-of-band hit: count 1, damage 40");
        check(ledger.playerDamage() == 0 && ledger.totalDamage() == 40,
                "the excluded hit still dealt its damage but feeds the share 0");
        check(!ledger.isParticipant(sniper),
                "the tower sniper is not a participant off excluded hits alone");
        ledger.recordDealt(fighter, 25, 2, true);
        check(ledger.shareExcludedHits() == 1,
                "a counted in-band hit does not move the exclusion count");
        ledger.recordDealt(null, 60, 3);
        check(ledger.shareExcludedHits() == 1,
                "unattributed damage (3-arg path) is not a height exclusion");
        ledger.recordDealt(sniper, 0, 4, false);
        ledger.recordDealt(sniper, Double.NaN, 5, false);
        check(ledger.shareExcludedHits() == 1,
                "zero / NaN hits never count as exclusions");
    }

    /** The evening's tower run, replayed as ledger arithmetic. */
    private static void towerScenario() {
        UUID boss = UUID.randomUUID();
        UUID sniper = UUID.randomUUID();
        EncounterLedger ledger = new EncounterLedger(BossKind.WARDEN, boss, 500, 0);
        for (int i = 0; i < 92; i++) {
            ledger.recordDealt(sniper, 5.4, i + 1L, false);
        }
        check(ledger.shareExcludedHits() == 92,
                "tower run: all 92 landed arrows recorded as exclusions");
        check(!ledger.shareMet(0.5),
                "tower run: the share gate fails on excluded damage alone");
        check(DenialMessage.select(BossKind.WARDEN, DENY_SHARE,
                ledger.shareExcludedHits()) == SHARE_HEIGHT,
                "tower run: the denial this kill earns is the height text");
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
