/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Spoils follow the verdict (Mike, Oct 19, 2026, from the seal
 * review): the participant multiplier and the dragon's XP scaling
 * apply ONLY on kills that CREDIT. Every denial pays vanilla 1x,
 * however many champions stood in range - the pre-rule behavior
 * (multiplier keyed to presence alone) is pinned here as the
 * contrast case via the bare SpoilsMath.multiplier.
 */
public final class SpoilsVerdictTest {

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

    public static void main(String[] args) {
        // CREDIT pays the champion-count multiplier, cap respected.
        check(SpoilsMath.multiplierFor(true, CREDIT, 0, 5) == 1,
                "credit, nobody standing: x1");
        check(SpoilsMath.multiplierFor(true, CREDIT, 1, 5) == 1,
                "credit, solo: x1");
        check(SpoilsMath.multiplierFor(true, CREDIT, 2, 5) == 2,
                "credit, 2 champions: x2");
        check(SpoilsMath.multiplierFor(true, CREDIT, 3, 5) == 3,
                "credit, 3 champions: x3");
        check(SpoilsMath.multiplierFor(true, CREDIT, 5, 5) == 5,
                "credit, 5 champions: x5");
        check(SpoilsMath.multiplierFor(true, CREDIT, 6, 5) == 5,
                "credit, 6 champions: capped at x5");
        check(SpoilsMath.multiplierFor(true, CREDIT, 4, 3) == 3,
                "credit, 4 champions under a cap of 3: x3 (configured cap honored)");

        // Every denial kind pays vanilla, even with a crowd standing by.
        for (CreditJudge.Verdict denial : new CreditJudge.Verdict[]{DENY_SHARE, DENY_PRESENCE, DENY_COOLDOWN}) {
            check(SpoilsMath.multiplierFor(true, denial, 2, 5) == 1,
                    denial + " with 2 champions standing: x1 (no star farming off a refused kill)");
            check(SpoilsMath.multiplierFor(true, denial, 5, 5) == 1,
                    denial + " with 5 champions standing: x1, not the old x5");
            check(SpoilsMath.multiplierFor(true, denial, 1, 5) == 1,
                    denial + " solo: x1");
        }

        // The contrast the rule closes: presence alone used to pay.
        check(SpoilsMath.multiplier(5, 5) == 5,
                "bare champion math still says x5 for 5 standing (the pre-rule payout)");
        check(SpoilsMath.multiplierFor(true, DENY_SHARE, 5, 5) == 1,
                "the verdict gate is what turns that x5 into x1 on a denied kill");

        // Spoils switched off behaves exactly as before: 1x always.
        check(SpoilsMath.multiplierFor(false, CREDIT, 3, 5) == 1,
                "spoils disabled + credit + 3 champions: x1");
        check(SpoilsMath.multiplierFor(false, CREDIT, 5, 5) == 1,
                "spoils disabled + credit + 5 champions: x1");
        check(SpoilsMath.multiplierFor(false, DENY_SHARE, 3, 5) == 1,
                "spoils disabled + denial: x1");

        // Dragon XP rides the same gate: scaled on credit, base on denial.
        check(SpoilsMath.dragonXp(true, SpoilsMath.multiplierFor(true, CREDIT, 3, 5)) == 36_000,
                "first dragon credited with 3 champions: 12,000 x3 = 36,000 XP");
        check(SpoilsMath.dragonXp(true, SpoilsMath.multiplierFor(true, DENY_PRESENCE, 3, 5)) == 12_000,
                "first dragon denied (nobody standing at the fall): unscaled 12,000 XP");
        check(SpoilsMath.dragonXp(false, SpoilsMath.multiplierFor(true, CREDIT, 2, 5)) == 1_000,
                "repeat dragon credited with 2 champions: 500 x2 = 1,000 XP");
        check(SpoilsMath.dragonXp(false, SpoilsMath.multiplierFor(true, DENY_COOLDOWN, 2, 5)) == 500,
                "repeat dragon denied on cooldown: unscaled 500 XP");

        // Elder mapping: each elder's own verdict governs its own drops.
        check(SpoilsMath.multiplierFor(true, CREDIT, 2, 5) == 2,
                "elder trio-progress kill (its verdict is CREDIT) with 2 standing: x2");
        check(SpoilsMath.multiplierFor(true, CREDIT, 4, 5) == 4,
                "elder trio-completing kill (CREDIT) with 4 standing: x4");
        check(SpoilsMath.multiplierFor(true, DENY_SHARE, 2, 5) == 1,
                "elder kill denied on share with 2 standing: x1 (its own verdict, not the trio's)");
        check(SpoilsMath.multiplierFor(true, DENY_COOLDOWN, 3, 5) == 1,
                "elder kill denied on cooldown with 3 standing: x1");

        System.out.println("SpoilsVerdictTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

