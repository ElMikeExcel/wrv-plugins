package com.winkreyes.excelraidbuff.core;

/**
 * The one player-facing line: fires exactly once per raid, only
 * when the buff is meaningfully on, only with both switches on —
 * and never for the near-vanilla low-omen raids.
 */
public final class AnnounceRuleTest {

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
        check(AnnounceRule.shouldAnnounce(true, true, 3, 3, false), "omen III at the threshold: announce");
        check(AnnounceRule.shouldAnnounce(true, true, 5, 3, false), "omen V: announce");
        check(AnnounceRule.shouldAnnounce(true, true, 4, 3, false), "omen IV: announce");
        check(!AnnounceRule.shouldAnnounce(true, true, 2, 3, false), "omen II below threshold: silent");
        check(!AnnounceRule.shouldAnnounce(true, true, 1, 3, false), "omen I: silent");
        check(!AnnounceRule.shouldAnnounce(false, true, 5, 3, false), "plugin disabled: silent");
        check(!AnnounceRule.shouldAnnounce(true, false, 5, 3, false), "announce toggled off: silent");
        check(!AnnounceRule.shouldAnnounce(true, true, 5, 3, true), "already announced this raid: silent");
        check(!AnnounceRule.shouldAnnounce(true, true, 6, 3, false), "omen above the vanilla cap: silent");
        check(!AnnounceRule.shouldAnnounce(true, true, 0, 3, false), "omen 0: silent");
        check(AnnounceRule.shouldAnnounce(true, true, 1, 1, false), "min omen 1 config: omen I announces");
        check(AnnounceRule.shouldAnnounce(true, true, 5, 0, false), "min omen 0 clamps to 1: omen V announces");
        check(!AnnounceRule.shouldAnnounce(true, true, 1, 0, true), "min omen 0, already announced: silent");

        System.out.println("AnnounceRuleTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
