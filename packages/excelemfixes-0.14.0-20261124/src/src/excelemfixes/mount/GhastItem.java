package excelemfixes.mount;

import com.winkreyes.excelitems.core.ItemDefinition;

import java.util.List;

/**
 * The Ghast Charm's registry definition — the item as DATA
 * (CUSTOM ITEM REGISTRY design law, Mike, Nov 17, 2026): a
 * permanent ID + a stable key + everything needed to mint it.
 * Behavior lives in GhastMountService; this class is the
 * assignment of record for ID 9.
 *
 * <p>ID 9 is PERMANENT: never renumber, never reuse, even after
 * the item retires. The registry lineage it lands behind:
 * 1–4 the ExcelItems builtins, 5 the Mount Charm (this module),
 * 6 the Field Bandage and 7 the Strong Healing Potion
 * (field-medic module), 8 the Atlas (ExcelAtlas, minted from
 * the registry's next free ID at its enable). 9 was the next
 * free ID when this item was assigned (Nov 23, 2026).
 *
 * <p>Like ID 5, the assignment is pinned here and NOT in
 * ExcelItems' config.yml: ExcelItems 0.1.0 validates its config
 * assignments at its own enable, when its registry holds only
 * the builtins — a {@code 9: ghast_charm} line reads as
 * "unknown id" there and ExcelItems disables itself (fail
 * closed) before any dependent can register. The unit tests
 * prove both directions against the real registry classes; the
 * config line is staged, flagged do-not-apply, for the future
 * ExcelItems validation fix (see the build RECORD).
 *
 * <p>Material GHAST_TEAR: an inert item with no vanilla use
 * behavior (a brewing ingredient — right-clicking it does
 * nothing in vanilla), so a right-click means exactly one
 * thing on Java and Bedrock alike, the same reasoning that
 * picked ECHO_SHARD for the Mount Charm. The glint marks it
 * as a custom item at a glance.
 */
public final class GhastItem {

    private GhastItem() {}

    public static final int ID = 9;
    public static final String KEY = "ghast_charm";
    public static final String HANDLER_ID = "excelemfixes:summon-mount";

    public static final ItemDefinition DEFINITION = new ItemDefinition(
            ID, KEY,
            "&b&lGhast Charm",
            "GHAST_TEAR", null, null, true,
            List.of(
                    "&7Right-click: call your ghast.",
                    "&7Right-click again: send it away.",
                    "&7A flying mount — ride it and",
                    "&7the sky is the road. It can't",
                    "&7fight and can't be hurt.",
                    "&7Works in the open worlds and",
                    "&7Primis. Not in dungeons/arenas.",
                    "&8Yours forever — losing the ghast",
                    "&8only ever costs a short wait."),
            ItemDefinition.Kind.ITEM,
            null, null, null, 0,
            HANDLER_ID);
}
