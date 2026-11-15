/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

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

        // 0.4.0 (PM-1): healing lives in the ledger - net damage
        // is gross minus healed, and healing never touches the
        // share inputs (share stays gross-based by design).
        EncounterLedger lh = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        lh.recordDealt(ana, 200.0, 1L);
        lh.recordDealt(null, 50.0, 2L);
        eq(lh.netDamage(), 250.0, "net equals gross before any healing");
        lh.recordHealed(80.0);
        eq(lh.healedPool(), 80.0, "healed pool accumulates");
        eq(lh.netDamage(), 170.0, "net = gross - healed");
        eq(lh.totalDamage(), 250.0, "gross total is untouched by healing");
        eq(lh.playerDamage(), 200.0, "player damage is untouched by healing");
        eq(lh.otherDamage(), 50.0, "other damage is untouched by healing");
        eq(lh.playerSharePercent(), 200.0 / 300.0 * 100.0, "the share still judges gross player damage");
        lh.recordHealed(10_000.0);
        eq(lh.netDamage(), 0.0, "net floors at zero under absurd healing");
        lh.recordHealed(-5.0);
        lh.recordHealed(Double.NaN);
        eq(lh.healedPool(), 10_080.0, "negative and NaN heals are refused");

        // 0.4.0 (PM-2): the verdict blow's dealer is the top
        // contributor; ties go to the earliest damager.
        EncounterLedger lt = new EncounterLedger(BossKind.WITHER, boss, 300.0, 0L);
        check(lt.topDealer() == null, "no damager yet: no top dealer");
        lt.recordDealt(ana, 100.0, 1L);
        lt.recordDealt(ben, 150.0, 2L);
        check(ben.equals(lt.topDealer()), "the bigger total deals the verdict blow");
        lt.recordDealt(ana, 50.0, 3L);
        check(ana.equals(lt.topDealer()), "a tie goes to the earliest damager");
        lt.recordDealt(null, 500.0, 4L);
        check(ana.equals(lt.topDealer()), "unattributed damage never becomes the dealer");

        System.out.println("EncounterLedgerTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

