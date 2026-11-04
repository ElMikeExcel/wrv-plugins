package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** ExcelMagicLevel — weight lookup chain + reward line + share copy math. */
public class TablesTest {
    @Test
    public void weightLookupFallsBackFullThenBaseThenDefault() {
        WeightTable weights = new WeightTable(Map.of("heal", 2, "greaterheal|3", 6, "mute", 0), 4);
        assertEquals(2, weights.weightFor("heal"));
        assertEquals(2, weights.weightFor("heal|2"));       // base key fallback
        assertEquals(6, weights.weightFor("greaterheal|3")); // full key wins
        assertEquals(4, weights.weightFor("greaterheal"));   // unlisted -> default
        assertEquals(4, weights.weightFor("brandnewspell|7"));
        assertEquals(0, weights.weightFor("mute"));          // zeroed by config
    }

    /** The shipping per-level increments: settled cap totals divided by 999. */
    private static final Boost PER_LEVEL_1000 =
            new Boost(0.00023773773773773774, 0.00023773773773773774, 0.0000950950950950951);

    @Test
    public void rewardLineIsStraight() {
        RewardTable rewards = new RewardTable(PER_LEVEL_1000,
                Set.of(25, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000));
        assertEquals(Boost.NONE, rewards.boostAt(1));
        // Even spread (SPEC §10 as amended): level L carries (L-1)/999 of
        // the totals — level 2 is one step, 500 is 499 steps, 1000 is all.
        assertBoost(rewards.boostAt(2), 0.2375 / 999, 0.2375 / 999, 0.095 / 999);
        assertBoost(rewards.boostAt(500), 0.2375 * 499 / 999, 0.2375 * 499 / 999, 0.095 * 499 / 999);
        assertBoost(rewards.boostAt(999), 0.2375 * 998 / 999, 0.2375 * 998 / 999, 0.095 * 998 / 999);
        // Mike's settled cap totals at level 1000: +23.75 / +23.75 / -9.5
        assertBoost(rewards.boostAt(1000), 0.2375, 0.2375, 0.095);
        assertTrue(rewards.isMilestone(25));
        assertTrue(rewards.isMilestone(1000));
        assertFalse(rewards.isMilestone(999));
        assertFalse(rewards.isMilestone(7));
    }

    @Test
    public void legacyRewardLineKeptItsCap() {
        // The 20-level line the totals came from: the same settled cap
        // totals (+23.75 / +23.75 / -9.5) reached in 19 steps instead of
        // 999 — the totals are the invariant, the slicing is the rescale.
        RewardTable legacy = new RewardTable(new Boost(0.0125, 0.0125, 0.005), Set.of(5, 10, 15, 20));
        assertBoost(legacy.boostAt(20), 0.2375, 0.2375, 0.095);
    }

    private static void assertBoost(Boost boost, double pool, double regen, double cost) {
        assertEquals(pool, boost.manaMaxBoost(), 1e-9);
        assertEquals(regen, boost.manaRegenerationBoost(), 1e-9);
        assertEquals(cost, boost.costReduction(), 1e-9);
    }

    @Test
    public void shareCopiesFromFinalAmount() {
        assertEquals(20, ShareMath.copyAmount(20, 100)); // settled slate: full copy
        assertEquals(10, ShareMath.copyAmount(20, 50));
        assertEquals(3, ShareMath.copyAmount(7, 50));    // floor
        assertEquals(0, ShareMath.copyAmount(0, 100));
        assertEquals(0, ShareMath.copyAmount(20, 0));
        // Copies derive from the earner's FINAL amount: a weekend-boosted
        // 24 (20 x 1.2, floored in the award path) copies as 24 — the
        // multiplier is inside the final amount, never applied twice
        // (SPEC §4.5/§4.6).
        assertEquals(24, ShareMath.copyAmount(24, 100));
        // No int overflow at absurd finals.
        assertEquals(Integer.MAX_VALUE, ShareMath.copyAmount(Integer.MAX_VALUE, 100));
    }

    @Test
    public void percentFormatting() {
        assertEquals("1.25", Percents.format(0.0125));
        assertEquals("9.5", Percents.format(0.095));
        assertEquals("23.75", Percents.format(0.2375));
        assertEquals("5", Percents.format(0.05));
        assertEquals("0", Percents.format(0.0));
    }
}
