/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * The 0.2 growth interplay in the ledger: growTo is a ratchet,
 * the share denominator tracks the scaled maximum (the share
 * boundary moves as the boss rallies), only DEALT damage
 * registers a damager (the took-damage-only clause, spec section 3), and
 * recordDealt reports new damagers exactly once each.
 */
public final class LedgerGrowthTest {

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
        UUID cy = UUID.randomUUID();
        UUID tookOnly = UUID.randomUUID();

        // Opening pool (wither base 750): 375 dealt is exactly 50%.
        EncounterLedger l = new EncounterLedger(BossKind.WITHER, boss, 750.0, 0L);
        check(l.recordDealt(ana, 375.0, 1L), "first dealt record registers a new damager");
        check(l.shareMet(0.5), "375 of 750 meets the gate before growth");
        check(l.damagerCount() == 1, "one damager so far");

        // A second champion lands a blow: the pool rallies to 950
        // and the SAME 375+10 no longer meets the gate - the
        // denominator tracks the pool actually being fought.
        check(l.recordDealt(ben, 10.0, 2L), "second player registers as a new damager");
        check(l.damagerCount() == 2, "two damagers");
        eq(l.growTo(950.0), 200.0, "growth applies the full step delta");
        eq(l.maxHealth(), 950.0, "denominator is the scaled pool");
        check(!l.shareMet(0.5), "385 of 950 no longer meets the gate after the rally");
        l.recordDealt(ana, 90.0, 3L);
        check(l.shareMet(0.5), "475 of 950 meets the gate again exactly");

        // recordDealt reports a new damager exactly once per player.
        check(!l.recordDealt(ana, 5.0, 4L), "a repeat hit is not a new damager");
        check(!l.recordDealt(ben, 5.0, 5L), "the second champion's repeat is not new either");
        check(l.recordDealt(cy, 5.0, 6L), "a third champion is new");
        check(l.damagerCount() == 3, "three damagers");

        // The ratchet: growth never reverses, even if a damager
        // somehow "unregisters" - there is no unregister, and a
        // smaller growTo target is a no-op returning 0.
        eq(l.growTo(1150.0), 200.0, "third step grows to the trio pool");
        eq(l.growTo(950.0), 0.0, "growTo below the current pool is a no-op");
        eq(l.growTo(1150.0), 0.0, "growTo at the current pool is a no-op");
        eq(l.growTo(Double.NaN), 0.0, "growTo NaN is a no-op");
        eq(l.maxHealth(), 1150.0, "pool stands at the trio size");

        // The took-damage-only clause: taking damage from the boss makes a
        // participant but NEVER a damager - she cannot grow the pool.
        EncounterLedger l2 = new EncounterLedger(BossKind.WARDEN, boss, 800.0, 0L);
        l2.recordTaken(tookOnly, 30.0, 1L);
        l2.recordTaken(tookOnly, 15.0, 2L);
        check(l2.damagerCount() == 0, "took-only player is not a damager");
        check(l2.isParticipant(tookOnly), "took-only player is still a participant");
        eq(l2.maxHealth(), 800.0, "no growth from standing in the arena");
        // Unattributed damage never registers a damager either.
        check(!l2.recordDealt(null, 100.0, 3L), "environment damage registers nobody");
        check(l2.damagerCount() == 0, "still zero damagers");
        // If she lands a hit herself she counts exactly once.
        check(l2.recordDealt(tookOnly, 3.0, 4L), "her own first hit registers her");
        check(l2.damagerCount() == 1, "exactly one damager after her hit");

        // Share-excluded hits (the warden height band) behave like
        // unattributed damage for growth too: no damager, no share.
        EncounterLedger l3 = new EncounterLedger(BossKind.WARDEN, boss, 800.0, 0L);
        check(!l3.recordDealt(ana, 400.0, 1L, false),
                "a height-excluded hit registers no damager");
        check(l3.damagerCount() == 0, "excluded hits never grow the pool");
        eq(l3.playerDamage(), 0.0, "excluded hits add no share");
        eq(l3.totalDamage(), 400.0, "excluded hits still really damaged the boss");

        // Zero / NaN hits never register anyone.
        EncounterLedger l4 = new EncounterLedger(BossKind.WITHER, boss, 750.0, 0L);
        check(!l4.recordDealt(ana, 0.0, 1L), "a zero hit registers nobody");
        check(!l4.recordDealt(ana, Double.NaN, 2L), "a NaN hit registers nobody");
        check(l4.damagerCount() == 0, "no damagers from empty hits");

        // The ledger invariant survives growth untouched.
        eq(l.playerDamage() + l.otherDamage(), l.totalDamage(),
                "player + other == total after growth");

        System.out.println("LedgerGrowthTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

