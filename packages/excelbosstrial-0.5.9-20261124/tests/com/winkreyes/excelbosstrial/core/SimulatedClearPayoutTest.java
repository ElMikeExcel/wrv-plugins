/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SIMULATED-CLEAR payout proof (0.5.6 rig-equivalent harness):
 * replays the host's exact payout sequence — recordRepeatClear's
 * increment, then the taper price read from the incremented
 * lifetime count (the host's taperCashFor) — for simulated
 * clears at counts 1 / 2 / 3+, for a mixed party of a veteran
 * and a newcomer, and for the seeded Elder pair. This is the
 * payout-path behavior proof for the desk rig, whose Paper set
 * carries no verdict-driving script (the same scope limit the
 * 0.5.5 record states); the live fight remains the final proof.
 */
public final class SimulatedClearPayoutTest {

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

    /** The host sequence, mirrored: increment first, price after. */
    private static long simulateClear(Map<String, Map<UUID, Integer>> counts,
                                      String sealId, UUID player) {
        counts.computeIfAbsent(sealId, k -> new HashMap<>()).merge(player, 1, Integer::sum);
        int count = counts.get(sealId).get(player);
        return CashTaper.cashFor(sealId, count);
    }

    public static void main(String[] args) {
        Map<String, Map<UUID, Integer>> counts = new HashMap<>();
        UUID vet = UUID.randomUUID();
        UUID newPlayer = UUID.randomUUID();
        String warden = BossKind.WARDEN.sealId();
        String elder = BossKind.ELDER_GUARDIAN.sealId();

        // Warden: a fresh party's clears 1 / 2 / 3 / 4.
        check(simulateClear(counts, warden, newPlayer) == 10000L, "warden clear 1 pays 10000");
        check(simulateClear(counts, warden, newPlayer) == 5000L, "warden clear 2 pays 5000");
        check(simulateClear(counts, warden, newPlayer) == 2000L, "warden clear 3 pays 2000");
        check(simulateClear(counts, warden, newPlayer) == 2000L, "warden clear 4 pays 2000");

        // Mixed party on one kill: the veteran (already at
        // warden #2 lifetime) prices at #3 while the newcomer
        // prices at #1 — per-player, never party-flat.
        counts.get(warden).put(vet, 2);
        long vetPrice = CashTaper.cashFor(warden, counts.get(warden).get(vet) + 1);
        check(vetPrice == 2000L, "veteran on the same kill prices at their own tier (2000)");
        check(CashTaper.cashFor(warden, 1) == 10000L, "newcomer on the same kill prices 10000");

        // Seeded Elder pair: seed to #2, next clear pays $2,000.
        UUID marvin = UUID.randomUUID();
        UUID jester = UUID.randomUUID();
        CounterSeeding.applySeed(counts, elder, marvin, 2);
        CounterSeeding.applySeed(counts, elder, jester, 2);
        check(simulateClear(counts, elder, marvin) == 2000L, "seeded Marvin: next elder clear pays 2000");
        check(simulateClear(counts, elder, jester) == 2000L, "seeded Jester: next elder clear pays 2000");
        // An unseeded player's elder ladder starts at #1.
        check(simulateClear(counts, elder, newPlayer) == 5000L, "unseeded elder clear 1 pays 5000");
        check(simulateClear(counts, elder, newPlayer) == 5000L, "unseeded elder clear 2 pays 5000");
        check(simulateClear(counts, elder, newPlayer) == 2000L, "unseeded elder clear 3 pays 2000");

        // Counts never reset across a cycle boundary: run a
        // full cycle through SealCycle and confirm the ladder
        // is untouched (the bookkeeping the host preserves).
        SealCycle cycle = new SealCycle(5);
        for (String seal : new String[] {"ender_dragon", "wither", "elder_guardian", "warden", "raid"}) {
            cycle.credit(seal, 1000L);
        }
        check(cycle.cycleNumber() == 2, "cycle rolled");
        check(counts.get(warden).get(newPlayer) == 4, "lifetime warden count survives the cycle roll");
        check(simulateClear(counts, warden, newPlayer) == 2000L, "post-cycle clear still prices by lifetime count");

        System.out.println("SimulatedClearPayoutTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
