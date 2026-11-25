package excelemfixes.fieldmedic;

import com.winkreyes.excelitems.core.ItemDefinition;
import java.util.List;

/**
 * The Field Bandage's registry definition — the item registered
 * once, as data, under the CUSTOM ITEM REGISTRY law (Mike,
 * Nov 17, 2026): permanent ID, stable key, construction pinned
 * here in the owning module. Shops, menus and loot reference the
 * item by this ID in their own configs.
 *
 * <p><b>ID 6, permanently.</b> IDs 1–4 are ExcelItems' builtins
 * (veil_potion, deathprotection_basic, deathpotion, revive_potion).
 * ID 5 was claimed first by the sibling summon-mount module
 * (mount_charm, same D199 item-solution model, Nov 22, 2026), so
 * the bandage takes the next free ID. This constant is never
 * config: a knob that could move an ID could silently re-point
 * every reference to it.
 *
 * <p>The definition is deliberately NOT mirrored into ExcelItems'
 * own config.yml assignments: ExcelItems 0.1.0 validates those
 * assignments at its enable, when its registry holds only the four
 * builtins, so a "6: field_bandage" line would read as an unknown
 * ID and ExcelItems would disable itself. Registration happens at
 * this module's enable through the API instead (see
 * {@link BandageItems}). The trap is pinned by a unit test against
 * the real registry classes.
 */
public final class BandageItem {

    /** Permanent ExcelItems registry ID of the Field Bandage. */
    public static final int REGISTRY_ID = 6;

    /** Permanent registry key. */
    public static final String KEY = "field_bandage";

    /** Behavior routing label registered against the ID. */
    public static final String HANDLER_ID = "excelemfixes:field-medic";

    public static final ItemDefinition DEFINITION = new ItemDefinition(
            REGISTRY_ID, KEY,
            "&f&lField Bandage",
            "PAPER", null, null, false,
            List.of(
                    "&7Right-click: wrap up and heal &a35% of your max",
                    "&7health over 5 seconds \u2014 works mid-fight.",
                    "&7One bandage at a time; 60 s between bandages.",
                    "&8Excel Shop \u2014 $150"),
            ItemDefinition.Kind.ITEM,
            null, null, null, 0,
            HANDLER_ID);

    private BandageItem() {}
}
