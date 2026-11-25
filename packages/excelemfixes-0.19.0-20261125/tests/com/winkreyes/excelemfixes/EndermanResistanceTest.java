import excelemfixes.enderman.CastTargeting;
import excelemfixes.enderman.EndermanScope;
import excelemfixes.enderman.LearnedAmounts;
import excelemfixes.enderman.ResistanceMath;

import java.util.List;
import java.util.UUID;

/**
 * Plain-JVM suite for the enderman-resistance module's pure
 * classes: ResistanceMath (landed/restored shares, settle
 * decisions, restore splitting, seed estimates), EndermanScope
 * (the scope truth table), CastTargeting (cone selection) and
 * LearnedAmounts (seed memory). Uses the shared Check harness.
 */
public final class EndermanResistanceTest {

    public static void run() {
        System.out.println("EndermanResistanceTest:");

        // ---- fraction sanitize: in-range passes through, junk -> 0.5
        Check.close(0.5, ResistanceMath.sanitizeFraction(0.5), "enderman fraction 0.5 kept");
        Check.close(0.0, ResistanceMath.sanitizeFraction(0.0), "enderman fraction 0 kept (deliberate)");
        Check.close(1.0, ResistanceMath.sanitizeFraction(1.0), "enderman fraction 1 kept (deliberate)");
        Check.close(0.5, ResistanceMath.sanitizeFraction(1.7), "enderman fraction >1 -> default");
        Check.close(0.5, ResistanceMath.sanitizeFraction(-0.2), "enderman fraction <0 -> default");
        Check.close(0.5, ResistanceMath.sanitizeFraction(Double.NaN), "enderman fraction NaN -> default");

        // ---- landed / restore shares at the shipped 0.5
        Check.close(1.1425, ResistanceMath.landedAmount(0.5, 2.285), "enderman landed half of 2.285");
        Check.close(1.1425, ResistanceMath.restoreAmount(0.5, 2.285), "enderman restore half of 2.285");
        Check.close(2.285, ResistanceMath.landedAmount(1.0, 2.285), "enderman fraction 1 lands all");
        Check.close(0.0, ResistanceMath.restoreAmount(1.0, 2.285), "enderman fraction 1 restores none");
        Check.close(0.0, ResistanceMath.landedAmount(0.5, 0.0), "enderman zero dealt lands zero");
        Check.close(0.0, ResistanceMath.restoreAmount(0.5, Double.NaN), "enderman NaN dealt restores zero");
        double dealt = 7.31;
        Check.close(dealt, ResistanceMath.landedAmount(0.35, dealt)
                + ResistanceMath.restoreAmount(0.35, dealt), "enderman landed + restored = dealt");

        // ---- settle decisions
        Check.eq(ResistanceMath.Decision.LANDED, ResistanceMath.decision(2.0, true),
                "enderman decision: damage measured -> LANDED");
        Check.eq(ResistanceMath.Decision.REROUTE, ResistanceMath.decision(0.0, true),
                "enderman decision: no damage, reroutable -> REROUTE");
        Check.eq(ResistanceMath.Decision.NONE, ResistanceMath.decision(0.0, false),
                "enderman decision: no damage, not reroutable -> NONE");
        Check.eq(ResistanceMath.Decision.REROUTE, ResistanceMath.decision(-3.0, true),
                "enderman decision: healed during window counts as no damage");
        Check.eq(ResistanceMath.Decision.REROUTE, ResistanceMath.decision(Double.NaN, true),
                "enderman decision: NaN delta -> REROUTE path, never LANDED");

        // ---- restoreSplit: health restored first, then absorption,
        //      never more than was lost
        split("health only", 0.0, 40.0, 0.0, 37.715, 1.1425, 0.0, 38.8575);
        split("absorption soaked all", 5.0, 40.0, 3.0, 40.0, 2.0, 5.0, 40.0);
        split("split across both", 5.0, 40.0, 0.0, 38.0, 4.0, 2.0, 40.0);
        split("restore capped at loss", 0.0, 40.0, 0.0, 39.0, 99.0, 0.0, 40.0);
        split("zero restore", 2.0, 40.0, 2.0, 39.0, 0.0, 2.0, 39.0);
        split("no loss no restore", 2.0, 40.0, 2.0, 40.0, 5.0, 2.0, 40.0);

        // ---- arrow seed: vanilla ceil(base x speed), clamped
        Check.close(6.0, ResistanceMath.arrowSeed(2.0, 2.6), "enderman arrow seed full draw");
        Check.close(2.0, ResistanceMath.arrowSeed(2.0, 0.9), "enderman arrow seed weak draw");
        Check.close(0.0, ResistanceMath.arrowSeed(0.0, 3.0), "enderman arrow seed zero base");
        Check.close(0.0, ResistanceMath.arrowSeed(2.0, 0.0), "enderman arrow seed zero speed");
        Check.close(30.0, ResistanceMath.arrowSeed(10.0, 10.0), "enderman arrow seed clamped");

        // ---- seed sanitize
        Check.close(6.0, ResistanceMath.sanitizeSeed(6.0, 2.0), "enderman seed kept");
        Check.close(2.0, ResistanceMath.sanitizeSeed(-1.0, 2.0), "enderman negative seed -> fallback");
        Check.close(2.0, ResistanceMath.sanitizeSeed(Double.NaN, 2.0), "enderman NaN seed -> fallback");

        // ---- scope truth table
        Check.ok(EndermanScope.isInScope(true, false, false, "world", "em_"),
                "enderman scope: elite anywhere");
        Check.ok(EndermanScope.isInScope(false, true, false, "world", "em_"),
                "enderman scope: registry world");
        Check.ok(EndermanScope.isInScope(false, false, true, "world", "em_"),
                "enderman scope: listed world");
        Check.ok(EndermanScope.isInScope(false, false, false, "em_primis", "em_"),
                "enderman scope: prefix world");
        Check.ok(EndermanScope.isInScope(false, false, false, "EM_ARENA", "em_"),
                "enderman scope: prefix is case-insensitive");
        Check.ok(!EndermanScope.isInScope(false, false, false, "world_the_end", "em_"),
                "enderman scope: plain world out");
        Check.ok(!EndermanScope.isInScope(false, false, false, "em_primis", ""),
                "enderman scope: empty prefix never matches");
        Check.ok(!EndermanScope.isInScope(false, false, false, null, "em_"),
                "enderman scope: null world out");

        // ---- cast targeting: cone selection
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        List<CastTargeting.Candidate> candidates = List.of(
                new CastTargeting.Candidate(a, 10, 0, 0),      // dead ahead
                new CastTargeting.Candidate(b, 10, 0, 10),     // 45 degrees off
                new CastTargeting.Candidate(c, -10, 0, 0));    // behind
        Check.eq(a, CastTargeting.select(1, 0, 0, candidates, 40, 0.90),
                "enderman targeting: dead-ahead candidate wins");
        Check.eq(null, CastTargeting.select(1, 0, 0,
                        List.of(new CastTargeting.Candidate(b, 10, 0, 10)), 40, 0.90),
                "enderman targeting: 45-degree candidate outside a 0.90 cone");
        Check.eq(b, CastTargeting.select(1, 0, 0,
                        List.of(new CastTargeting.Candidate(b, 10, 0, 10)), 40, 0.70),
                "enderman targeting: wider cone admits the 45-degree candidate");
        Check.eq(null, CastTargeting.select(1, 0, 0, candidates, 5, 0.90),
                "enderman targeting: beyond max distance ignored");
        Check.eq(null, CastTargeting.select(0, 0, 0, candidates, 40, 0.90),
                "enderman targeting: zero direction selects nothing");
        Check.eq(null, CastTargeting.select(1, 0, 0, List.of(), 40, 0.90),
                "enderman targeting: no candidates selects nothing");
        UUID near = UUID.randomUUID(), far = UUID.randomUUID();
        Check.eq(near, CastTargeting.select(1, 0, 0, List.of(
                        new CastTargeting.Candidate(far, 30, 0, 3),
                        new CastTargeting.Candidate(near, 10, 0, 0.5)), 40, 0.90),
                "enderman targeting: straighter candidate beats farther one");
        Check.eq(near, CastTargeting.select(1, 0, 0, List.of(
                        new CastTargeting.Candidate(far, 20, 0, 0),
                        new CastTargeting.Candidate(near, 10, 0, 0)), 40, 0.90),
                "enderman targeting: equal angle breaks to the nearer one");

        // ---- learned amounts
        LearnedAmounts learned = new LearnedAmounts();
        UUID player = UUID.randomUUID();
        Check.close(2.0, learned.get(player, "WANDS", 2.0), "enderman learned: fallback before any");
        learned.remember(player, "WANDS", 2.285);
        Check.close(2.285, learned.get(player, "WANDS", 2.0), "enderman learned: remembered amount");
        Check.close(6.0, learned.get(player, "BOWS", 6.0), "enderman learned: categories are separate");
        learned.remember(player, "WANDS", -5.0);
        Check.close(2.285, learned.get(player, "WANDS", 2.0), "enderman learned: negative ignored");
        learned.remember(player, "WANDS", Double.NaN);
        Check.close(2.285, learned.get(player, "WANDS", 2.0), "enderman learned: NaN ignored");
        Check.ok(learned.knows(player, "WANDS"), "enderman learned: knows WANDS");
        Check.ok(!learned.knows(player, "STAVES"), "enderman learned: does not know STAVES");
        learned.clear();
        Check.close(2.0, learned.get(player, "WANDS", 2.0), "enderman learned: clear resets");
    }

    private static void split(String name, double absBefore, double healthBefore,
                              double absNow, double healthNow, double amount,
                              double expectedAbs, double expectedHealth) {
        double[] result = ResistanceMath.restoreSplit(absBefore, healthBefore, absNow, healthNow, amount);
        boolean same = Math.abs(expectedAbs - result[0]) < 1e-9
                && Math.abs(expectedHealth - result[1]) < 1e-9;
        if (!same) {
            System.out.println("    expected=[" + expectedAbs + ", " + expectedHealth
                    + "] actual=[" + result[0] + ", " + result[1] + "]");
        }
        Check.ok(same, "enderman restoreSplit " + name);
    }
}
