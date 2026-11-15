import com.winkreyes.excelattunement.core.WeaponKind;
import com.winkreyes.excelattunement.core.Weapons;

/** The full martial weapon set + the shield/tool non-triggers. */
public class WeaponsTest {
    static void run() {
        System.out.println("== WeaponsTest ==");
        String[] tiers = {"WOODEN", "STONE", "IRON", "GOLDEN", "DIAMOND", "NETHERITE"};
        for (String t : tiers) {
            Check.eq(WeaponKind.MELEE, Weapons.kindOfItemName(t + "_SWORD"), t + "_SWORD is martial melee");
            Check.eq(WeaponKind.MELEE, Weapons.kindOfItemName(t + "_AXE"), t + "_AXE is martial melee");
            Check.eq(WeaponKind.NONE, Weapons.kindOfItemName(t + "_PICKAXE"), t + "_PICKAXE is a tool, never martial");
            Check.eq(WeaponKind.NONE, Weapons.kindOfItemName(t + "_SHOVEL"), t + "_SHOVEL is a tool, never martial");
            Check.eq(WeaponKind.NONE, Weapons.kindOfItemName(t + "_HOE"), t + "_HOE is a tool, never martial");
        }
        Check.eq(WeaponKind.MELEE, Weapons.kindOfItemName("MACE"), "MACE is martial melee");
        Check.eq(WeaponKind.TRIDENT, Weapons.kindOfItemName("TRIDENT"), "TRIDENT in hand is martial");
        Check.eq(WeaponKind.BOW, Weapons.kindOfItemName("BOW"), "BOW item kind");
        Check.eq(WeaponKind.CROSSBOW, Weapons.kindOfItemName("CROSSBOW"), "CROSSBOW item kind");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("SHIELD"), "SHIELD never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("STICK"), "plain stick (wand lookalike) never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("BLAZE_ROD"), "blaze rod (wand material) never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("AIR"), "empty hand never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName(null), "null item never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("FISHING_ROD"), "fishing rod never triggers");
        Check.eq(WeaponKind.NONE, Weapons.kindOfItemName("SHEARS"), "shears never trigger");
        Check.ok(Weapons.isMartialItemName("netherite_sword"), "item names are case-insensitive");
    }
}
