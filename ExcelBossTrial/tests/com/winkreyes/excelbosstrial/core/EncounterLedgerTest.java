package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Attribution invariants of the encounter ledger: totals always
 * split cleanly into player vs other damage, the share is measured
 * against MAX HP (not damage dealt), and participation means
 * dealt-anything OR took-anything.
 */
public final class EncounterLedgerTest {

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
        UUID boss = UUID.randomUUID();
        UUID ana = UUID.randomUUID();
        UUID ben = UUID.randomUUID();

        // Wither bar: 300 max HP. 150 player damage = exactly 50%.
        EncounterLedger l = new EncounterLedger(BossKind.WITHER, boss, 300.0, 1_000L);
        l.recordDealt(ana, 100.0, 1_100L);
        l.recordDealt(null, 40.0, 1_200L); // golem fists / environment
        l.recordDealt(ana, 50.0, 1_300L);
        eq(l.totalDamage(), 190.0, "total = every recorded hit");
        eq(l.playerDamage(), 150.0, "player damage excludes unattributed");
        eq(l.otherDamage(), 40.0, "other damage is the unattributed rest");
        eq(l.playerSharePercent(), 50.0, "share is vs max HP, not vs total");
        check(l.shareMet(0.5), "exactly 50% meets the gate (no near-miss mercy either way)");
        check(!l.shareMet(0.5000001), "a hair above 50% is not met by exactly 50%");

        // 49.9% must fail.
        EncounterLedger l2 = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        l2.recordDealt(ana, 149.7, 1L);
        eq(l2.playerSharePercent(), 49.9, "149.7 of 300 is 49.9%");
        check(!l2.shareMet(0.5), "49.9% is denied - no near-miss mercy");

        // Overkill and overheal-era totals: share can exceed 100% of
        // the bar only through recorded damage; gate still just a gate.
        EncounterLedger l3 = new EncounterLedger(BossKind.WARDEN, boss, 500.0, 0L);
        l3.recordDealt(ana, 600.0, 1L);
        eq(l3.playerSharePercent(), 120.0, "overkill share reads 120%");
        check(l3.shareMet(0.5), "overkill passes");

        // Participation: dealt OR taken. Spectators and zero hits don't count.
        EncounterLedger l4 = new EncounterLedger(BossKind.ELDER_GUARDIAN, boss, 80.0, 0L);
        l4.recordDealt(ana, 10.0, 1L);
        l4.recordTaken(ben, 5.0, 2L); // ben only ever got spiked - still a participant
        check(l4.isParticipant(ana), "dealer is a participant");
        check(l4.isParticipant(ben), "damage-taker is a participant");
        check(l4.participants().size() == 2, "two participants");
        l4.recordDealt(UUID.randomUUID(), 0.0, 3L);
        check(l4.participants().size() == 2, "a zero-damage event creates no participant");
        eq(l4.takenBy(ben), 5.0, "taken tracked per player");
        eq(l4.dealtBy(ben), 0.0, "ben dealt nothing");

        // Non-positive and NaN records are dropped entirely.
        EncounterLedger l5 = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        l5.recordDealt(ana, -5.0, 1L);
        l5.recordDealt(ana, Double.NaN, 2L);
        l5.recordDealt(null, 0.0, 3L);
        eq(l5.totalDamage(), 0.0, "junk records ignored");
        check(l5.participants().isEmpty(), "no participants from junk");

        // An untouched ledger (boss died to /kill before any event):
        EncounterLedger l6 = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        eq(l6.playerSharePercent(), 0.0, "no damage = 0%");
        check(!l6.shareMet(0.5), "no damage never meets the gate");

        // Max-health zero guard.
        EncounterLedger l7 = new EncounterLedger(BossKind.WITHER, boss, 0.0, 0L);
        l7.recordDealt(ana, 10.0, 1L);
        check(!l7.shareMet(0.5), "zero max health never meets the gate (no div-by-zero credit)");

        System.out.println("EncounterLedgerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

