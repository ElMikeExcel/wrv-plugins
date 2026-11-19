import com.winkreyes.excelitems.core.BuiltinItems;
import com.winkreyes.excelitems.core.ItemDefinition;

import java.util.HashSet;
import java.util.Set;

/**
 * Pins the four first registry entries to the construction facts read
 * from the owning plugins' mint code (Nov 18, 2026; paths in
 * BUILD-REPORT.md). If an owning plugin's item changes, this suite is
 * where the drift shows up first.
 */
public final class BuiltinItemsTest {

    public static void run() {
        System.out.println("BuiltinItemsTest:");

        Check.eq(4, BuiltinItems.ALL.size(), "exactly four builtin entries");
        Set<Integer> ids = new HashSet<>();
        Set<String> keys = new HashSet<>();
        for (ItemDefinition def : BuiltinItems.ALL) {
            ids.add(def.id());
            keys.add(def.key());
        }
        Check.eq(Set.of(1, 2, 3, 4), ids, "builtin ids are 1..4");
        Check.eq(Set.of("veil_potion", "deathprotection_basic", "deathpotion", "revive_potion"),
                keys, "builtin keys are the four shop product ids");

        // id 1 — Veil Potion (ExcelMobVeil createPotion)
        ItemDefinition veil = BuiltinItems.VEIL_POTION;
        Check.eq("POTION", veil.material(), "veil material POTION");
        Check.eq(0x63A8AA, veil.potionColorRgb(), "veil colour 0x63A8AA");
        Check.eq("WATER", veil.basePotionType(), "veil base potion WATER");
        Check.ok(veil.glint(), "veil glint on");
        Check.eq("&b&lVeil Potion", veil.displayName(), "veil display name");
        Check.eq(6, veil.lore().size(), "veil lore is the 6 mint lines");
        Check.eq("excelmobveil", veil.legacyNamespace(), "veil legacy namespace");
        Check.eq("potion", veil.legacyKey(), "veil legacy key");
        Check.eq(ItemDefinition.Kind.ITEM, veil.kind(), "veil is a physical item");

        // id 2 — Death Protection basic (activation, no physical item)
        ItemDefinition basic = BuiltinItems.DEATH_PROTECTION_BASIC;
        Check.eq(ItemDefinition.Kind.ACTIVATION, basic.kind(), "basic is an activation, not an item");
        Check.eq("CHEST", basic.material(), "basic display material is the menu CHEST icon");
        Check.ok(!basic.hasLegacyMark(), "basic has no legacy item mark (no item ever existed)");
        Check.eq("&a&lDeath Protection", basic.displayName(), "basic display name");

        // id 3 — Death Protection Potion, premium (ExcelDeathProtection createPotion)
        ItemDefinition potion = BuiltinItems.DEATH_POTION;
        Check.eq("POTION", potion.material(), "death potion material POTION");
        Check.eq(0x7D3C98, potion.potionColorRgb(), "death potion colour 0x7D3C98");
        Check.ok(potion.glint(), "death potion glint on");
        Check.eq("&d&l+Death Protection Potion", potion.displayName(), "death potion display name");
        Check.eq(8, potion.lore().size(), "death potion lore is the 8 mint lines (0.2.0 text)");
        Check.eq("exceldeathprotection", potion.legacyNamespace(), "death potion legacy namespace");
        Check.eq("potion", potion.legacyKey(), "death potion legacy key");
        Check.eq("lore_version", potion.legacyLoreKey(), "death potion legacy lore key");
        Check.eq(2, potion.loreVersion(), "death potion lore version 2");

        // id 4 — Revive Potion (ExcelDowned RevivePotionItem.create)
        ItemDefinition revive = BuiltinItems.REVIVE_POTION;
        Check.eq("POTION", revive.material(), "revive material POTION");
        Check.eq(0xFF4D6D, revive.potionColorRgb(), "revive colour 0xFF4D6D");
        Check.eq("&d&lRevive Potion", revive.displayName(), "revive display name");
        Check.eq(5, revive.lore().size(), "revive lore is the 5 mint lines");
        Check.ok(revive.lore().get(4).contains("$5,000"), "revive lore carries the default $5,000 price line");
        Check.eq("exceldowned", revive.legacyNamespace(), "revive legacy namespace");
        Check.eq("revive_potion", revive.legacyKey(), "revive legacy key");
        Check.eq("revive_potion_lore", revive.legacyLoreKey(), "revive legacy lore key");
        Check.eq(1, revive.loreVersion(), "revive lore version 1");
    }
}
