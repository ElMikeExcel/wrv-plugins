/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * The seal-cycle mechanism (0.5.6; reset law Nov 10, 2026 +
 * D7; design section 3.2): a broken seal stays broken, the
 * count resets ONLY at 5/5 into a new cycle, and the Elite
 * door latch, once set at the first 5/5, never clears.
 */
public final class SealCycleTest {

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

    private static final String[] SEALS = {
            "ender_dragon", "wither", "elder_guardian", "warden", "raid"};

    public static void main(String[] args) {
        SealCycle cycle = new SealCycle(5);
        check(cycle.cycleNumber() == 1, "starts at cycle 1");
        check(!cycle.guildOpened(), "latch unset at start");
        check(!cycle.eliteDoorOpen(), "door closed at 0 of 5");
        check(cycle.cycleCount() == 0, "cycle count 0 at start");

        // Breaks accumulate; nothing resets before 5/5.
        for (int i = 0; i < 4; i++) {
            check(cycle.credit(SEALS[i], 1000L + i) == SealCycle.CreditResult.CREDITED,
                    "credit " + SEALS[i] + " credited");
            check(cycle.cycleCount() == i + 1, "cycle count " + (i + 1));
            check(!cycle.guildOpened(), "latch still unset at " + (i + 1) + " of 5");
            check(cycle.cycleNumber() == 1, "still cycle 1 at " + (i + 1) + " of 5");
        }
        // A broken seal stays broken: re-crediting changes nothing.
        check(cycle.credit(SEALS[0], 2000L) == SealCycle.CreditResult.ALREADY_IN_CYCLE,
                "re-credit of a broken seal is a no-op");
        check(cycle.cycleCount() == 4, "count still 4 after re-credit");
        check(cycle.isBrokenLifetime(SEALS[0]), "lifetime broken reads true");

        // The 5th break completes the cycle: latch + archive +
        // new cycle at 0 of 5, lifetime records retained.
        check(cycle.credit(SEALS[4], 3000L) == SealCycle.CreditResult.CYCLE_COMPLETED,
                "5th credit completes the cycle");
        check(cycle.guildOpened(), "latch set at 5/5");
        check(cycle.openedTimestampMs() == 3000L, "latch timestamp recorded");
        check(cycle.eliteDoorOpen(), "door open at the boundary");
        check(cycle.cycleNumber() == 2, "cycle increments to 2");
        check(cycle.cycleCount() == 0, "new cycle starts at 0 of 5");
        check(cycle.history().size() == 1, "completed cycle archived");
        check(cycle.history().get(0).contains("cycle 1"), "archive names cycle 1");
        for (String seal : SEALS) {
            check(cycle.isBrokenLifetime(seal), "lifetime still broken: " + seal);
            check(!cycle.isBrokenInCycle(seal), "cycle 2 starts unbroken: " + seal);
        }
        check(cycle.lifetimeCount() == 5, "lifetime count stays 5");

        // The door stays open through the whole new cycle.
        check(cycle.eliteDoorOpen(), "door stays open at cycle 2, 0 of 5");
        check(cycle.credit(SEALS[4], 4000L) == SealCycle.CreditResult.CREDITED,
                "the completing seal can be cycle 2's first break");
        check(cycle.cycleCount() == 1, "cycle 2 count 1");
        check(cycle.eliteDoorOpen(), "door still open at cycle 2, 1 of 5");

        // Completing cycle 2 keeps the ORIGINAL latch timestamp.
        for (int i = 0; i < 4; i++) {
            cycle.credit(SEALS[i], 5000L + i);
        }
        check(cycle.cycleNumber() == 3, "cycle 2 completion opens cycle 3");
        check(cycle.openedTimestampMs() == 3000L, "latch timestamp is the FIRST opening");
        check(cycle.history().size() == 2, "two cycles archived");

        // Snapshot round-trip.
        SealCycle restored = new SealCycle(5);
        restored.restore(cycle.snapshot());
        check(restored.cycleNumber() == 3, "round-trip cycle number");
        check(restored.guildOpened(), "round-trip latch");
        check(restored.openedTimestampMs() == 3000L, "round-trip latch timestamp");
        check(restored.cycleCount() == 0, "round-trip cycle count");
        check(restored.lifetimeCount() == 5, "round-trip lifetime count");
        check(restored.history().equals(cycle.history()), "round-trip history");

        // A pre-cycle state (no snapshot) migrates to cycle 1.
        SealCycle fresh = new SealCycle(5);
        fresh.restore(null);
        check(fresh.cycleNumber() == 1 && !fresh.guildOpened(), "null snapshot migrates to cycle 1");

        // A smaller required count (a disabled seal) completes
        // at its own total.
        SealCycle four = new SealCycle(4);
        four.credit("a", 1L);
        four.credit("b", 2L);
        four.credit("c", 3L);
        check(four.credit("d", 4L) == SealCycle.CreditResult.CYCLE_COMPLETED,
                "required count 4 completes at 4");
        check(four.guildOpened(), "required-4 latch set");

        System.out.println("SealCycleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
