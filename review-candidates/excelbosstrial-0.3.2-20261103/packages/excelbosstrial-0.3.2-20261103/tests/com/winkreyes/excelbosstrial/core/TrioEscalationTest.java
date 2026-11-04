/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import java.util.UUID;

/**
 * Elder trio-position escalation (0.3.1, Mike Nov 1, 2026): every
 * elder is an epic fight of its own (the retune: base 7,800 +
 * 3,100 per extra, pool at 4 champions = 17,100), and the trio's
 * third, seal-completing elder carries x1.3 - at every party
 * size, under custom config, and again in repeat generations.
 */
public final class TrioEscalationTest {

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
        check(Math.abs(actual - expected) < 0.001,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // The 0.3.1 retune slate (elder-guardian.scaling).
        ScalingRule slate = new ScalingRule(true, 7800, 3100, 5);

        // ---- The retune ladder -----------------------------------
        eq(slate.poolFor(1), 7800, "retune: solo pool");
        eq(slate.poolFor(2), 10_900, "retune: duo pool");
        eq(slate.poolFor(3), 14_000, "retune: three-champion pool");
        eq(slate.poolFor(4), 17_100, "retune: pool at 4 champions is ~17,000");
        eq(slate.poolFor(5), 20_200, "retune: five-champion pool");
        eq(slate.poolFor(9), 20_200, "retune: the counted cap holds the pool");

        // ---- Positions and the settled defaults ------------------
        check(TrioEscalation.positionFor(0) == 1, "position: no counted kills = elder #1");
        check(TrioEscalation.positionFor(1) == 2, "position: one counted = elder #2");
        check(TrioEscalation.positionFor(2) == 3, "position: two counted = the completer");
        eq(TrioEscalation.multiplierFor(0, TrioEscalation.DEFAULTS), 1.0,
                "defaults: position 1 at x1.0");
        eq(TrioEscalation.multiplierFor(1, TrioEscalation.DEFAULTS), 1.0,
                "defaults: position 2 at x1.0");
        eq(TrioEscalation.multiplierFor(2, TrioEscalation.DEFAULTS), 1.3,
                "defaults: position 3 at x1.3");
        eq(TrioEscalation.multiplierFor(7, TrioEscalation.DEFAULTS), 1.3,
                "defaults: a position past the list reuses its last entry");

        // ---- Config is honored; junk config is safe --------------
        eq(TrioEscalation.multiplierFor(2, List.of(1.0, 1.0, 1.5)), 1.5,
                "config: a custom third multiplier is honored");
        eq(TrioEscalation.multiplierFor(0, null), 1.0,
                "config: a missing list falls back to the defaults");
        eq(TrioEscalation.multiplierFor(2, List.of()), 1.3,
                "config: an empty list falls back to the defaults");
        eq(TrioEscalation.multiplierFor(1, List.of(1.0, Double.NaN, 1.3)), 1.0,
                "config: a NaN entry reads as 1.0, never a broken pool");
        eq(TrioEscalation.multiplierFor(0, List.of(-4.0)), 0.0,
                "config: entries clamp at 0 (a disabled elder is a config choice)");
        eq(TrioEscalation.multiplierFor(0, List.of(99.0)), 10.0,
                "config: entries clamp at 10 (a typo cannot blow a pool up)");

        // ---- The multiplier scales the whole pool shape ----------
        ScalingRule third = TrioEscalation.scaled(slate, 1.3);
        check(TrioEscalation.scaled(slate, 1.0) == slate,
                "scale: x1.0 returns the rule untouched (positions 1-2 price as 0.3.0)");
        for (int n = 1; n <= 5; n++) {
            eq(third.poolFor(n), slate.poolFor(n) * 1.3,
                    "scale: position 3 pool = 1.3 x position 1 at " + n + " champion(s)");
        }
        eq(third.poolFor(1), 10_140, "scale: solo completer = 7,800 x 1.3 before growth");
        eq(third.poolFor(2), 14_170, "scale: duo completer = 10,900 x 1.3");
        eq(third.poolFor(4), 22_230, "scale: the 4-champion completer is ~22,000");
        check(third.enabled() == slate.enabled()
                && third.maxDamagersCounted() == slate.maxDamagersCounted(),
                "scale: enabled and the counted cap carry over untouched");

        // ---- The monument flow: generations escalate alike -------
        {
            MonumentTracker t = new MonumentTracker();
            UUID e1 = UUID.randomUUID();
            UUID e2 = UUID.randomUUID();
            UUID e3 = UUID.randomUUID();
            t.register(e1, "world", 100, 40, 100);
            t.register(e2, "world", 104, 40, 100);
            t.register(e3, "world", 100, 40, 104);
            String mid = t.monumentOf(e1);
            List<Double> mults = TrioEscalation.DEFAULTS;
            eq(TrioEscalation.multiplierFor(t.progress(mid), mults), 1.0,
                    "generation 1: the first elder prices at x1.0");
            t.markCounted(e1);
            eq(TrioEscalation.multiplierFor(t.progress(mid), mults), 1.0,
                    "generation 1: the second elder prices at x1.0");
            t.markCounted(e2);
            eq(TrioEscalation.multiplierFor(t.progress(mid), mults), 1.3,
                    "generation 1: the completing elder prices at x1.3");
            t.markCounted(e3);
            check(t.trioComplete(mid), "generation 1: the trio completes");
            t.completeTrioRespawn(mid, 999_999L);
            check(t.progress(mid) == 0, "the completed trio turns over: progress restarts");
            // The repeat generation: fresh elders, same monument.
            UUID e4 = UUID.randomUUID();
            UUID e5 = UUID.randomUUID();
            UUID e6 = UUID.randomUUID();
            t.register(e4, "world", 102, 40, 102);
            t.register(e5, "world", 98, 40, 100);
            t.register(e6, "world", 100, 40, 98);
            eq(TrioEscalation.multiplierFor(t.progress(mid), mults), 1.0,
                    "repeat generation: the first elder prices at x1.0 again");
            t.markCounted(e4);
            t.markCounted(e5);
            eq(TrioEscalation.multiplierFor(t.progress(mid), mults), 1.3,
                    "repeat generation: the completing elder escalates again");
        }

        System.out.println("TrioEscalationTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
