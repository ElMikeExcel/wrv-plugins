/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import com.winkreyes.excelbosstrial.dragon.DamageGovernor;
import com.winkreyes.excelbosstrial.dragon.TrialConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * The 0.1.1 anti-nuke math (DRAGON-CHEESE-RESEARCH.md section 2):
 * P1 per-event cap, P2 per-tick explosion pooling, fail-soft off
 * switches, and the tick boundary. Ticks are passed in, so the fake
 * clock is just the test choosing numbers.
 */
public final class DamageGovernorTest {

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

    private static TrialConfig cfgWith(double perEvent, double budget) {
        Map<String, Object> m = new HashMap<>();
        m.put("caps.per-event-damage", perEvent);
        m.put("caps.explosion-per-tick", budget);
        return TrialConfig.fromMap(m);
    }

    public static void main(String[] args) {
        TrialConfig d = TrialConfig.defaults();

        // --- P1: per-event cap --------------------------------------
        DamageGovernor g = new DamageGovernor();
        DamageGovernor.Result r = g.govern(25, false, 1, d);
        check(r.damage == 25 && !r.trimmed(), "legit 25 hit untouched");
        r = g.govern(60, false, 1, d);
        check(r.damage == 60 && !r.trimmed(), "hit exactly at the cap is not trimmed");
        r = g.govern(61, false, 1, d);
        check(r.damage == 60 && r.perEventTrimmed && !r.budgetTrimmed
                && r.ruleLabel().equals("P1"), "61 trimmed to 60 by P1 alone");
        r = g.govern(10000, false, 1, d);
        check(r.damage == 60 && r.perEventTrimmed, "mace one-shot (10000) lands as 60");
        r = g.govern(0, false, 1, d);
        check(r.damage == 0 && !r.trimmed(), "zero-damage event passes through");

        // --- P2: a single bed lands; the chain pools -----------------
        DamageGovernor chain = new DamageGovernor();
        r = chain.govern(70, true, 5, d);
        check(r.damage == 60 && r.ruleLabel().equals("P1"),
                "first bed: P1 judges it as one event (60), budget leaves it");
        check(chain.poolRemaining() == 15, "pool spent 60 of 75");
        r = chain.govern(70, true, 5, d);
        check(r.damage == 15 && r.ruleLabel().equals("P1+P2"),
                "second same-tick bed: capped, then budget trims to the last 15");
        check(chain.poolRemaining() == 0, "pool exhausted at exactly 75 total");
        r = chain.govern(70, true, 5, d);
        check(r.damage == 0 && r.budgetTrimmed, "third same-tick bed: nothing left in the tick");
        r = chain.govern(50, true, 5, d);
        check(r.damage == 0 && r.ruleLabel().equals("P2"),
                "sub-cap blast in an empty pool is trimmed by P2 alone");

        // --- P2: tick boundary refills --------------------------------
        r = chain.govern(70, true, 6, d);
        check(r.damage == 60 && chain.poolRemaining() == 15,
                "next tick: a fresh 75 budget (bed lands at its P1 value)");

        // --- P2: melee never pools and never consumes -----------------
        DamageGovernor mix = new DamageGovernor();
        mix.govern(70, true, 7, d); // pool now 15
        r = mix.govern(50, false, 7, d);
        check(r.damage == 50 && !r.trimmed(), "melee at full value while pool is low");
        check(mix.poolRemaining() == 15, "melee did not consume the explosion pool");
        r = mix.govern(50, true, 7, d);
        check(r.damage == 15 && r.ruleLabel().equals("P2"),
                "explosion after melee still finds only the pooled 15");

        // --- P2: partial budget trim under the per-event cap ----------
        DamageGovernor partial = new DamageGovernor();
        r = partial.govern(40, true, 9, d);
        check(r.damage == 40 && !r.trimmed(), "40 blast lands in full");
        r = partial.govern(40, true, 9, d);
        check(r.damage == 35 && r.ruleLabel().equals("P2"),
                "second 40 blast trimmed to the remaining 35 by the budget");

        // --- Fail-soft off switches ------------------------------------
        TrialConfig p1Off = cfgWith(0, 75);
        DamageGovernor gOff = new DamageGovernor();
        r = gOff.govern(10000, false, 1, p1Off);
        check(r.damage == 10000 && !r.trimmed(), "P1 at 0 = disabled: 10000 passes whole");
        r = gOff.govern(100, true, 1, p1Off);
        check(r.damage == 75 && r.ruleLabel().equals("P2"),
                "budget alone trims a 100 blast to 75");

        TrialConfig p2Off = cfgWith(60, 0);
        DamageGovernor gNoPool = new DamageGovernor();
        boolean allSixty = true;
        for (int i = 0; i < 3; i++) {
            DamageGovernor.Result x = gNoPool.govern(70, true, 3, p2Off);
            allSixty &= x.damage == 60 && !x.budgetTrimmed;
        }
        check(allSixty, "P2 at 0 = disabled: same-tick blasts each land at the P1 cap");

        TrialConfig allOff = cfgWith(0, 0);
        DamageGovernor gAllOff = new DamageGovernor();
        r = gAllOff.govern(10000, false, 1, allOff);
        check(r.damage == 10000 && !r.trimmed(), "all caps off: melee is 0.1.0 exactly");
        r = gAllOff.govern(10000, true, 1, allOff);
        check(r.damage == 10000 && !r.trimmed(), "all caps off: explosions are 0.1.0 exactly");

        DamageGovernor.Result neg = new DamageGovernor().govern(500, false, 1, cfgWith(-5, -1));
        check(neg.damage == 500 && !neg.trimmed(), "negative caps disable their rules too");

        System.out.println("DamageGovernorTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

