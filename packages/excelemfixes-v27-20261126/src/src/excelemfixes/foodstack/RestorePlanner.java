package excelemfixes.foodstack;

/**
 * Decides how one stamped stack returns to its resting form.
 * The decision replicates EliteMobs' own restore
 * ({@code AdvancedFoodItems.restoreItem}, bytecode-verified
 * against EliteMobs 10.9.14) step for step, so a stack this
 * module normalizes is the same stack EliteMobs itself would
 * have produced at combat end:
 *
 * <ul>
 *   <li>no stamp → no action (the resting form already);</li>
 *   <li>all six stamp keys are removed;</li>
 *   <li>if the backup's {@code default} flag is set, the item
 *       had no food component of its own → the food component
 *       is removed (the material's vanilla defaults apply,
 *       exactly as when the item was minted);</li>
 *   <li>otherwise, when the backup carries both nutrition and
 *       saturation, the food component is rebuilt from the
 *       backed-up values (nutrition, saturation, and the
 *       backed-up always-edible flag — NOT the combat form's
 *       forced always-edible);</li>
 *   <li>when the backup is incomplete (not default, but a
 *       value missing), the food component is left exactly as
 *       it is — the same conservative choice EliteMobs makes;</li>
 *   <li>the consumption time is set back to the backed-up
 *       original seconds whenever the backup carries them.</li>
 * </ul>
 *
 * Direction note (why normalize DOWN to the resting form and
 * not UP to the stamped form): the stamp's nutrition and
 * saturation are copied from the item's own resting values
 * (for a component-less item, from the material's vanilla food
 * table), so eating a resting copy nourishes exactly as much
 * as eating a stamped one. The stamped form's only deltas are
 * transient combat conveniences EliteMobs applies and removes
 * itself — always-edible and a doubled consumption time.
 * Stamping everything would export those conveniences to the
 * whole world (ground drops, chests) and EliteMobs would still
 * strip them at every combat end; the resting form is the
 * fixed point both systems agree on.
 */
public final class RestorePlanner {

    private RestorePlanner() {}

    public record Plan(boolean stamped,
                       boolean removeFood,
                       boolean setFood,
                       int nutrition,
                       float saturation,
                       boolean alwaysEdible,
                       Float consumptionSeconds) {}

    /** The no-op plan for an unstamped stack. */
    public static Plan none() {
        return new Plan(false, false, false, 0, 0f, false, null);
    }

    public static Plan plan(StampView view) {
        if (view == null || !view.present()) {
            return none();
        }
        boolean removeFood = view.defaultFood();
        boolean setFood = !removeFood
                && view.nutrition() != null
                && view.saturation() != null;
        return new Plan(
                true,
                removeFood,
                setFood,
                setFood ? view.nutrition() : 0,
                setFood ? view.saturation() : 0f,
                setFood && view.alwaysEdible(),
                view.consumptionSeconds());
    }
}
