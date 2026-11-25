package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.core.ItemDefinition;
import java.util.List;

/**
 * The Strong Healing Potion's registry definition — the item
 * registered once, as data, under the CUSTOM ITEM REGISTRY law
 * (Mike, Nov 17, 2026): permanent ID, stable key, construction
 * pinned here in the owning module. The ExcelShops
 * special-products listing references the item by this ID in its
 * own config; the listing's price is a shop fact there, and the
 * prices this module charges live in this module's config.
 *
 * <p><b>ID 7, permanently.</b> IDs 1–4 are ExcelItems' builtins
 * (veil_potion, deathprotection_basic, deathpotion,
 * revive_potion), ID 5 is the sibling summon-mount module's
 * mount_charm, and ID 6 is this module's own field_bandage. The
 * potion takes the next free ID. This constant is never config:
 * a knob that could move an ID could silently re-point every
 * reference to it.
 *
 * <p>The construction is the whole point of the item: a REAL
 * Splash Potion of Strong Healing. The material is SPLASH_POTION
 * and the base potion type is STRONG_HEALING, which is what
 * carries Instant Health II (8 HP) — ExcelItems' factory applies
 * the base type at mint time. A plain SPLASH_POTION entry with
 * no base type would mint an effect-less dud under a healing
 * name; that staging was considered and rejected in the shop
 * catalog pass (Nov 22, 2026, finding F2). No custom potion
 * colour: the vanilla Strong Healing colour shows.
 *
 * <p>The definition is deliberately NOT mirrored into ExcelItems'
 * own config.yml assignments, for the same reason as the
 * bandage's: ExcelItems 0.1.0 validates those assignments at its
 * enable, when its registry holds only the four builtins, so a
 * "7: strong_healing_potion" line would read as an unknown ID
 * and ExcelItems would disable itself. Registration happens at
 * this module's enable through the API instead (see
 * {@link PotionItems}). The trap is pinned by a unit test against
 * the real registry classes.
 */
public final class PotionItem {

    /** Permanent ExcelItems registry ID of the Strong Healing Potion. */
    public static final int REGISTRY_ID = 7;

    /** Permanent registry key. */
    public static final String KEY = "strong_healing_potion";

    /** Behavior routing label registered against the ID. */
    public static final String HANDLER_ID = "excelemfixes:field-medic";

    public static final ItemDefinition DEFINITION = new ItemDefinition(
            REGISTRY_ID, KEY,
            "&c&lSplash Potion of Strong Healing",
            "SPLASH_POTION", null, "STRONG_HEALING", false,
            List.of(
                    "&7Throw it: &a8 HP&7 of healing, instantly \u2014",
                    "&7works mid-fight, and splashes nearby",
                    "&7allies too. One throw, then it's gone.",
                    "&7Brew your own for free, or buy the",
                    "&7convenience here.",
                    "&8Excel Shop \u2014 $150 \u00b7 Guild vendor \u2014 $112.50"),
            ItemDefinition.Kind.ITEM,
            null, null, null, 0,
            HANDLER_ID);

    private PotionItem() {}
}
