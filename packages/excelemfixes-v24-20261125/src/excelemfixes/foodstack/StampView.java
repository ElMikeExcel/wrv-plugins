package excelemfixes.foodstack;

/**
 * A read-only view of EliteMobs' Advanced Combat food stamp on
 * one item stack: the six {@code elitemobs:advanced_combat_food*}
 * persistent-data keys EliteMobs writes when its Advanced Combat
 * system "adapts" a food item for combat (bytecode-verified
 * against EliteMobs 10.9.14's
 * {@code AdvancedFoodItems.adaptItem}).
 *
 * <p>The stamp is a backup, not an identity: when EliteMobs
 * adapts an item it stores the item's ORIGINAL food values in
 * these keys (nutrition, saturation, always-edible, the original
 * consumption time in seconds, and a flag saying whether the
 * item had a food component of its own at all), then rewrites
 * the live food component for combat (same values, but
 * always-edible, and double the consumption time). When combat
 * ends EliteMobs restores the item from this backup
 * ({@code AdvancedFoodItems.restoreItem}). Copies that miss the
 * restore pass keep the stamp forever — and because Minecraft
 * only stacks items whose data is identical, a stamped copy and
 * a resting copy of the same food refuse to merge.
 *
 * @param present         the marker key is on the stack at all
 * @param defaultFood     the {@code _default} backup flag: true
 *                        when the item had NO food component of
 *                        its own before adaptation (its food
 *                        values came from the material's vanilla
 *                        table)
 * @param nutrition       backed-up nutrition, or null
 * @param saturation      backed-up saturation, or null
 * @param alwaysEdible    backed-up always-edible flag
 * @param consumptionSeconds backed-up original consumption
 *                        time in seconds, or null
 */
public record StampView(boolean present,
                        boolean defaultFood,
                        Integer nutrition,
                        Float saturation,
                        boolean alwaysEdible,
                        Float consumptionSeconds) {

    /** The six EliteMobs PDC key names, in one place. */
    public static final String KEY_MARKER = "advanced_combat_food";
    public static final String KEY_DEFAULT = "advanced_combat_food_default";
    public static final String KEY_NUTRITION = "advanced_combat_food_nutrition";
    public static final String KEY_SATURATION = "advanced_combat_food_saturation";
    public static final String KEY_ALWAYS_EDIBLE = "advanced_combat_food_always_edible";
    public static final String KEY_CONSUMPTION_SECONDS = "advanced_combat_food_consumption_seconds";

    public static final String NAMESPACE = "elitemobs";

    /** A stack with no stamp on it. */
    public static StampView absent() {
        return new StampView(false, false, null, null, false, null);
    }
}
