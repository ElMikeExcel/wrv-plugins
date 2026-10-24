package com.winkreyes.leaderboard;

import com.winkreyes.leaderboard.core.ClaimBlocksMath;
import com.winkreyes.leaderboard.core.ClaimBlocksMath.ClaimFigure;
import com.winkreyes.leaderboard.core.ClaimBlocksMath.Totals;

import java.util.List;

/** Fixture suite for the Land Claimed board's math, over fake
 * GriefPrevention data: every shape of player GP can hand the adapter
 * gets a fixture and an expected claimed / total / available triple. */
public final class ClaimBlocksMathTests {

    private static int checks = 0;

    private static void eq(long actual, long expected, String label) {
        checks++;
        if (actual != expected) {
            throw new AssertionError(label + ": expected <" + expected + "> but was <" + actual + ">");
        }
    }

    private static ClaimFigure claim(int area) {
        return new ClaimFigure(area, true, false);
    }

    public static void main(String[] args) {
        // One player, claims in several claims-enabled worlds (on this
        // server that is the survival world and any world GP enables):
        // every top-level area counts.
        Totals multi = ClaimBlocksMath.summarize(2_000, 0,
                List.of(claim(400), claim(250), claim(100)));
        eq(multi.claimed(), 750, "multi-world claimed");
        eq(multi.total(), 2_000, "multi-world total");
        eq(multi.available(), 1_250, "multi-world available");

        // A claim standing in a claims-DISABLED world (nether/end here)
        // does not count: GP cannot create or honour it, so the land is
        // not claimed.
        Totals disabledWorld = ClaimBlocksMath.summarize(2_000, 0,
                List.of(claim(400), new ClaimFigure(900, false, false)));
        eq(disabledWorld.claimed(), 400, "disabled-world claim excluded");
        eq(disabledWorld.available(), 1_600, "disabled-world available");

        // A subdivision carves up its parent claim's area; it consumes
        // no blocks of its own, enabled world or not.
        Totals subdivided = ClaimBlocksMath.summarize(2_000, 0,
                List.of(claim(400), new ClaimFigure(150, true, true),
                        new ClaimFigure(50, false, true)));
        eq(subdivided.claimed(), 400, "subdivisions excluded");
        eq(subdivided.available(), 1_600, "subdivision available");

        // Bonus blocks add to the total on top of accrued blocks.
        Totals bonus = ClaimBlocksMath.summarize(1_000, 250, List.of(claim(300)));
        eq(bonus.total(), 1_250, "bonus total");
        eq(bonus.claimed(), 300, "bonus claimed");
        eq(bonus.available(), 950, "bonus available");

        // Zero claims: nothing used, everything still available.
        Totals zero = ClaimBlocksMath.summarize(1_100, 0, List.of());
        eq(zero.claimed(), 0, "zero-claims claimed");
        eq(zero.available(), 1_100, "zero-claims available");

        // Brand-new player, absent from GP's data entirely: the adapter
        // reads a fresh GP record (accrued = initial blocks, bonus 0, no
        // claims), which must surface as a full initial allowance.
        Totals fresh = ClaimBlocksMath.summarize(100, 0, List.of());
        eq(fresh.claimed(), 0, "new player claimed");
        eq(fresh.total(), 100, "new player total");
        eq(fresh.available(), 100, "new player available");

        // Over-drawn (accrued later lowered below what is claimed):
        // claimed still reports the real footprint; available goes
        // negative in the math and the display layer clamps it to 0.
        Totals over = ClaimBlocksMath.summarize(500, 0, List.of(claim(800)));
        eq(over.claimed(), 800, "over-drawn claimed");
        eq(over.available(), -300, "over-drawn available is negative in math");

        // Junk in, nothing phantom out: negative areas never subtract.
        Totals junk = ClaimBlocksMath.summarize(1_000, 0,
                List.of(new ClaimFigure(-50, true, false), claim(100)));
        eq(junk.claimed(), 100, "negative area ignored");

        System.out.println("ClaimBlocksMathTests: " + checks + " assertions passed");
    }
}
