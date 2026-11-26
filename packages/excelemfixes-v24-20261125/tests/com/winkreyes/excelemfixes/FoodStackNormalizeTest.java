import excelemfixes.foodstack.RestorePlanner;
import excelemfixes.foodstack.StampView;
import excelemfixes.foodstack.StackMerger;

/**
 * Suites for the food-stack-normalize module's pure core: the
 * restore planner (EliteMobs' own restore decisions, replicated)
 * and the stack merger arithmetic.
 */
public class FoodStackNormalizeTest {

    public static void run() {
        planner();
        merger();
    }

    private static void planner() {
        // No stamp -> no action, ever.
        RestorePlanner.Plan none = RestorePlanner.plan(StampView.absent());
        Check.ok(!none.stamped() && !none.removeFood() && !none.setFood()
                && none.consumptionSeconds() == null, "planner: absent stamp is a no-op");
        Check.ok(!RestorePlanner.plan(null).stamped(), "planner: null view is a no-op");

        // The Fresh Rat case: minted with no food of its own
        // (default flag set), backup holds the vanilla rabbit
        // values (3 / 1.8 / not always-edible / 1.6s).
        RestorePlanner.Plan rat = RestorePlanner.plan(
                new StampView(true, true, 3, 1.8f, false, 1.6f));
        Check.ok(rat.stamped(), "planner: rat stamp seen");
        Check.ok(rat.removeFood() && !rat.setFood(),
                "planner: default-flagged stamp removes the food component");
        Check.ok(rat.consumptionSeconds() != null && rat.consumptionSeconds() == 1.6f,
                "planner: rat consumption restored to the backed-up 1.6s");

        // An item with food of its own: component rebuilt from
        // the backup, including the backed-up always-edible
        // flag (NOT the combat form's forced true).
        RestorePlanner.Plan own = RestorePlanner.plan(
                new StampView(true, false, 8, 12.8f, false, 1.6f));
        Check.ok(own.setFood() && !own.removeFood(),
                "planner: own-food stamp rebuilds the component");
        Check.ok(own.nutrition() == 8 && own.saturation() == 12.8f && !own.alwaysEdible(),
                "planner: rebuilt from backup values, always-edible as backed up");
        RestorePlanner.Plan ownEdible = RestorePlanner.plan(
                new StampView(true, false, 4, 2.4f, true, 0.8f));
        Check.ok(ownEdible.setFood() && ownEdible.alwaysEdible()
                && ownEdible.consumptionSeconds() == 0.8f,
                "planner: backed-up always-edible true survives");

        // Incomplete backup (not default, a value missing):
        // food left exactly as-is, like EliteMobs' own restore.
        RestorePlanner.Plan partial = RestorePlanner.plan(
                new StampView(true, false, null, 1.0f, false, null));
        Check.ok(partial.stamped() && !partial.removeFood() && !partial.setFood()
                && partial.consumptionSeconds() == null,
                "planner: incomplete backup leaves food untouched");

        // Default flag wins over present values (EliteMobs
        // checks the flag first).
        RestorePlanner.Plan flagWins = RestorePlanner.plan(
                new StampView(true, true, 8, 12.8f, true, 1.6f));
        Check.ok(flagWins.removeFood() && !flagWins.setFood(),
                "planner: default flag takes precedence over backup values");
    }

    private static void merger() {
        // Jester's piles: 7 / 1 / 2 / 1 of one item, max 64.
        int[] merged = StackMerger.merge(
                new int[]{0, 0, 0, 0}, new int[]{7, 1, 2, 1}, new int[]{64, 64, 64, 64});
        Check.ok(merged[0] == 11 && merged[1] == 0 && merged[2] == 0 && merged[3] == 0,
                "merger: four split piles converge into the first slot");

        // Different groups never merge.
        int[] two = StackMerger.merge(
                new int[]{0, 1, 0}, new int[]{5, 5, 5}, new int[]{64, 64, 64});
        Check.ok(two[0] == 10 && two[1] == 5 && two[2] == 0,
                "merger: groups stay separate");

        // Max stack respected; overflow stays in later slots.
        int[] capped = StackMerger.merge(
                new int[]{0, 0, 0}, new int[]{60, 60, 60}, new int[]{64, 64, 64});
        Check.ok(capped[0] == 64 && capped[1] == 64 && capped[2] == 52,
                "merger: fills to max, remainder flows forward");

        // Small max (e.g. 16): same arithmetic.
        int[] small = StackMerger.merge(
                new int[]{0, 0}, new int[]{10, 10}, new int[]{16, 16});
        Check.ok(small[0] == 16 && small[1] == 4, "merger: respects a max of 16");

        // Empty slots (group -1) and full slots are untouched.
        int[] gaps = StackMerger.merge(
                new int[]{-1, 0, 0}, new int[]{0, 64, 3}, new int[]{64, 64, 64});
        Check.ok(gaps[0] == 0 && gaps[1] == 64 && gaps[2] == 3,
                "merger: empty and full slots untouched");

        // Nothing to merge: identity.
        int[] single = StackMerger.merge(
                new int[]{0}, new int[]{5}, new int[]{64});
        Check.ok(single[0] == 5, "merger: single stack unchanged");
    }
}
