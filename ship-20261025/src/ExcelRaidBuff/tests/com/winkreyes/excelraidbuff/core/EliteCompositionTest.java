package com.winkreyes.excelraidbuff.core;

import java.util.List;

/**
 * The elite composition (Mike's "include elites in there too"):
 * share by omen, the across-the-raid ramp that averages back to
 * the share, deterministic half-up quotas, evenly-spaced picks
 * through the type-priority order — and the level rule (EM's
 * natural level + the omen bonus, hard-clamped to EM's cap).
 */
public final class EliteCompositionTest {

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
        RaidBuffConfig cfg = RaidBuffConfig.defaults();

        // Share by omen, wave 1 of 8 (ramp-from 0.6).
        eq(EliteComposition.waveShare(cfg, 1, 1, 8), 0.0, "omen I never converts");
        eq(EliteComposition.waveShare(cfg, 2, 1, 8), 0.05 * 0.6, "omen II wave 1 share");
        eq(EliteComposition.waveShare(cfg, 5, 1, 8), 0.18 * 0.6, "omen V wave 1 share");
        // Final wave (ramp-to 1.4).
        eq(EliteComposition.waveShare(cfg, 5, 8, 8), 0.18 * 1.4, "omen V final wave share");
        eq(EliteComposition.waveShare(cfg, 3, 6, 6), 0.10 * 1.4, "omen III final wave share");
        // Mid-raid sits between, and the raid-wide average is the base share.
        double mid = EliteComposition.waveShare(cfg, 5, 4, 8);
        check(mid > 0.18 * 0.6 && mid < 0.18 * 1.4, "omen V mid wave share between the ramps");
        double sum = 0;
        for (int w = 1; w <= 8; w++) sum += EliteComposition.waveShare(cfg, 5, w, 8);
        eq(sum / 8, 0.18, "omen V raid-wide average share is the base share");
        // A one-wave raid converts at the top of the ramp.
        eq(EliteComposition.waveShare(cfg, 5, 1, 1), 0.18 * 1.4, "single-wave raid uses ramp-to");
        // Out-of-range waves clamp to the ends.
        eq(EliteComposition.waveShare(cfg, 5, 0, 8), 0.18 * 0.6, "wave 0 clamps to wave 1");
        eq(EliteComposition.waveShare(cfg, 5, 99, 8), 0.18 * 1.4, "wave 99 clamps to the final wave");
        // Elites off: nothing, whatever the omen.
        RaidBuffConfig off = RaidBuffConfig.fromMap(
                java.util.Map.of("elites", java.util.Map.of("enabled", false)));
        eq(EliteComposition.waveShare(off, 5, 8, 8), 0.0, "elites disabled: share 0");

        // Quotas round half up and never exceed the wave.
        check(EliteComposition.quota(10, 0.15) == 2, "quota 10 @ 15% rounds half up to 2");
        check(EliteComposition.quota(10, 0.14) == 1, "quota 10 @ 14% is 1");
        check(EliteComposition.quota(10, 0.252) == 3, "quota 10 @ 25.2% is 3");
        check(EliteComposition.quota(7, 0.108) == 1, "quota 7 @ 10.8% is 1");
        check(EliteComposition.quota(5, 0.0) == 0, "quota at share 0 is 0");
        check(EliteComposition.quota(0, 0.5) == 0, "quota of an empty wave is 0");
        check(EliteComposition.quota(3, 1.0) == 3, "quota never exceeds the wave size");
        check(EliteComposition.quota(12, 0.05) == 1, "quota 12 @ 5% is 1 (no zero-elite mid raids by rounding)");

        // Picks: priority order first (vindicator 0 ... ravager 4),
        // spread evenly through it, returned ascending.
        List<Integer> allPillager = List.of(3, 3, 3, 3, 3, 3);
        check(EliteComposition.select(2, allPillager).equals(List.of(0, 3)),
                "even spread: quota 2 of 6 takes positions 0 and 3");
        check(EliteComposition.select(1, allPillager).equals(List.of(0)),
                "quota 1 takes the head of the order");
        check(EliteComposition.select(6, allPillager).equals(List.of(0, 1, 2, 3, 4, 5)),
                "quota = size takes everyone");
        check(EliteComposition.select(0, allPillager).isEmpty(), "quota 0 takes nobody");
        check(EliteComposition.select(9, allPillager).size() == 6, "quota above size clamps to the wave");
        // Priority beats position: the vindicator at index 2 goes first.
        List<Integer> mixed = List.of(3, 3, 0, 3, 1); // pillager, pillager, vindicator, pillager, witch
        check(EliteComposition.select(1, mixed).equals(List.of(2)),
                "priority first: the lone vindicator converts first");
        List<Integer> two = EliteComposition.select(2, mixed);
        check(two.contains(2) && two.size() == 2, "quota 2 includes the vindicator");
        // Ravagers convert last: with one slot and a ravager + pillager, the pillager goes.
        check(EliteComposition.select(1, List.of(4, 3)).equals(List.of(1)),
                "ravager is the last priority");

        // The level rule: natural + bonus, clamped to the cap, floored at 1.
        check(EliteLevelRule.levelFor(10, 15, 150) == 25, "level: natural 10 + omen V bonus 15");
        check(EliteLevelRule.levelFor(10, 0, 150) == 10, "level: no bonus at omen I/II");
        check(EliteLevelRule.levelFor(148, 15, 150) == 150, "level: clamped at the cap");
        check(EliteLevelRule.levelFor(200, 15, 150) == 150, "level: over-cap natural clamped");
        check(EliteLevelRule.levelFor(0, 0, 150) == 1, "level: natural 0 floors to 1");
        check(EliteLevelRule.levelFor(-3, 5, 150) == 6, "level: negative natural floors to 1 before the bonus");
        check(EliteLevelRule.levelFor(10, -5, 150) == 10, "level: negative bonus ignored");
        check(EliteLevelRule.levelFor(10, 5, 0) == 1, "level: degenerate cap 0 clamps everything to 1");

        System.out.println("EliteCompositionTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
