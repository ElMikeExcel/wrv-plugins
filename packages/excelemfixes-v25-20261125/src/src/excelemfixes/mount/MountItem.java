package excelemfixes.mount;

import com.winkreyes.excelitems.core.ItemDefinition;

import java.util.List;

/**
 * The Mount Charm's registry definition — the item as DATA
 * (CUSTOM ITEM REGISTRY design law, Mike, Nov 17, 2026): a
 * permanent ID + a stable key + everything needed to mint it.
 * Behavior lives in MountService; this class is the assignment of
 * record for ID 5.
 *
 * <p>ID 5 is the first ID after the four ExcelItems builtins
 * (1 veil_potion, 2 deathprotection_basic, 3 deathpotion,
 * 4 revive_potion) and is PERMANENT: never renumber, never reuse,
 * even after the item retires (D196 — the charm is interim until
 * EliteMobs ships mounts; the ID stays claimed forever).
 *
 * <p>Why the assignment is pinned here and NOT in ExcelItems'
 * config.yml: ExcelItems 0.1.0 validates its config assignments
 * at its own enable, when its registry holds only the builtins —
 * a {@code 5: mount_charm} line reads as "unknown id" there and
 * ExcelItems disables itself (fail closed) before any dependent
 * can register. The unit tests prove both directions against the
 * real registry classes. The config line is staged, flagged
 * do-not-apply, for the future ExcelItems validation fix; see
 * the build RECORD.
 *
 * <p>Material ECHO_SHARD: an inert item with no vanilla use
 * behavior (not placeable, doesn't leash, doesn't blow a horn with
 * a client-side use cooldown), so a right-click means exactly one
 * thing on Java and Bedrock alike. The glint marks it as a custom
 * item at a glance.
 */
public final class MountItem {

    private MountItem() {}

    public static final int ID = 5;
    public static final String KEY = "mount_charm";
    public static final String HANDLER_ID = "excelemfixes:summon-mount";

    public static final ItemDefinition DEFINITION = new ItemDefinition(
            ID, KEY,
            "&6&lMount Charm",
            "ECHO_SHARD", null, null, true,
            List.of(
                    "&7Right-click: call your horse.",
                    "&7Right-click again: send it away.",
                    "&7Your own tamed horse, saddled",
                    "&7and ready. It can't fight and",
                    "&7can't be hurt.",
                    "&7Works in the open worlds and",
                    "&7Primis. Not in dungeons/arenas.",
                    "&8Yours forever — losing the horse",
                    "&8only ever costs a short wait."),
            ItemDefinition.Kind.ITEM,
            null, null, null, 0,
            HANDLER_ID);
}
