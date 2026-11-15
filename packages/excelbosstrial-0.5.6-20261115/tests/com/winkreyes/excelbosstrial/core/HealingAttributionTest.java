/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.UUID;

/**
 * Support recognition (0.3.0, SPEC 4.6): the presence proxy -
 * weight = presence fraction x (1 - dealt share). Recognition
 * only: these checks pin the proxy's shape, including the solo
 * case that must name nobody.
 */
public final class HealingAttributionTest {

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
        check(PresenceHealingAttribution.weight(1.0, 0.0) == 1.0,
                "full presence, no damage: weight 1");
        check(PresenceHealingAttribution.weight(1.0, 1.0) == 0.0,
                "full presence, all the damage: weight 0 (a carry is not support)");
        check(Math.abs(PresenceHealingAttribution.weight(0.5, 0.25) - 0.375) < 1e-9,
                "half presence, quarter share: 0.375");
        check(PresenceHealingAttribution.weight(2.0, 0.0) == 1.0,
                "presence clamps at 1");
        check(PresenceHealingAttribution.weight(0.5, 2.0) == 0.0,
                "share clamps at 1");
        check(PresenceHealingAttribution.weight(Double.NaN, 0.5) == 0.0,
                "NaN presence reads as absent");

        // Ledger level: the low-damage full-presence champion is
        // named; a solo clear names nobody.
        UUID boss = UUID.randomUUID();
        UUID carry = UUID.randomUUID();
        UUID support = UUID.randomUUID();
        EncounterLedger ledger = new EncounterLedger(BossKind.WARDEN, boss, 1000, 1_000L);
        ledger.recordDealt(carry, 900, 2_000L);
        ledger.recordDealt(support, 100, 2_000L);
        ledger.recordDealt(carry, 0.0001, 60_000L);
        ledger.recordDealt(support, 0.0001, 60_000L);
        check(HealingAttribution.presenceProxy().supportChampion(ledger, 61_000L)
                        .map(support::equals).orElse(false),
                "the low-share full-presence champion is the support pick");

        EncounterLedger solo = new EncounterLedger(BossKind.WARDEN, UUID.randomUUID(), 1000, 1_000L);
        solo.recordDealt(carry, 1000, 2_000L);
        check(HealingAttribution.presenceProxy().supportChampion(solo, 3_000L).isEmpty(),
                "a solo clear names no support champion");
        check(HealingAttribution.presenceProxy().supportChampion(null, 0L).isEmpty(),
                "no ledger: nobody named, no crash");

        System.out.println("HealingAttributionTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
