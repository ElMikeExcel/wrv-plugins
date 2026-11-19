package com.winkreyes.excelitems.core;

import java.util.List;

/**
 * The first registry entries: the four existing special products,
 * migrated as DEFINITIONS ONLY (P-ITEM-REGISTRY-1, 0.1.0). Nothing
 * about ExcelShops, ExcelMobVeil, ExcelDeathProtection or
 * ExcelDowned changes in this unit — those plugins keep minting and
 * recognizing their items exactly as today; the registry records the
 * canonical construction of each item and stamps both the primary
 * mark (excelitems:id) and the legacy mark on anything IT mints, so
 * a registry-minted item is recognized by the owning plugin too.
 *
 * <p>Every construction fact below was read from the owning plugin's
 * mint code on Nov 18, 2026 (paths in BUILD-REPORT.md):
 * <ul>
 *   <li>id 1 veil_potion — ExcelMobVeil createPotion: POTION, base
 *       WATER, colour 0x63A8AA, glint, PDC excelmobveil:potion BYTE 1.
 *       Shop id "veil_potion" (MobileCatalog Route.VEIL, console
 *       command "mobveil buy"); shop price $2,000 is a SHOP fact and
 *       deliberately NOT part of the item definition.</li>
 *   <li>id 2 deathprotection_basic — NO physical item exists: buying
 *       basic activates the protection immediately (ExcelDeathProtection
 *       0.2.0 deliverBasic -> Tier.BASIC; the shop menu's CHEST is a
 *       display icon only). Kind ACTIVATION; the CHEST definition is
 *       the display/representative stack. Shop price $15,000.</li>
 *   <li>id 3 deathpotion — ExcelDeathProtection createPotion (the
 *       premium tier): POTION, colour 0x7D3C98, glint, display
 *       "+Death Protection Potion", PDC exceldeathprotection:potion
 *       BYTE 1 + exceldeathprotection:lore_version INTEGER 2.
 *       Shop price $30,000.</li>
 *   <li>id 4 revive_potion — ExcelDowned RevivePotionItem.create:
 *       POTION, colour 0xFF4D6D, display "Revive Potion", PDC
 *       exceldowned:revive_potion BYTE 1 +
 *       exceldowned:revive_potion_lore INTEGER 1. The last lore line
 *       carries the price in the original mint (formatted from the
 *       ExcelDowned config, default 5000); the definition pins the
 *       default "$5,000" text. Shop price $5,000.</li>
 * </ul>
 * IDs are assigned in shop-menu order and are PERMANENT: never
 * renumber, never reuse.
 */
public final class BuiltinItems {

    private BuiltinItems() {}

    public static final ItemDefinition VEIL_POTION = new ItemDefinition(
            1, "veil_potion",
            "&b&lVeil Potion",
            "POTION", 0x63A8AA, "WATER", true,
            List.of(
                    "&7Drink: invisible body + standard mobs ignore you.",
                    "&7Adds 8 real-time minutes, even while offline.",
                    "&bDrink more to stack time; attacking ends it all.",
                    "&eWorks in armor. Armor stays visible to players.",
                    "&7Farming is safe. Milk and death end all time.",
                    "&8Boss scripts and area attacks can still hurt you."),
            ItemDefinition.Kind.ITEM,
            "excelmobveil", "potion", null, 0,
            "excelmobveil:veil-drink");

    public static final ItemDefinition DEATH_PROTECTION_BASIC = new ItemDefinition(
            2, "deathprotection_basic",
            "&a&lDeath Protection",
            "CHEST", null, null, false,
            List.of(
                    "&7Buy to activate protection immediately.",
                    "&7Your next death keeps items &fonce&7.",
                    "&7You still die and respawn normally.",
                    "&7No extra abilities. One active protection.",
                    "&cDoes NOT save your XP levels."),
            ItemDefinition.Kind.ACTIVATION,
            null, null, null, 0,
            "exceldeathprotection:basic-activation");

    public static final ItemDefinition DEATH_POTION = new ItemDefinition(
            3, "deathpotion",
            "&d&l+Death Protection Potion",
            "POTION", 0x7D3C98, null, true,
            List.of(
                    "&7Drink to protect your items for one death.",
                    "&7You still die and respawn normally.",
                    "&cDoes NOT save your XP levels.",
                    "&bWhile active: Conduit Power gives",
                    "&bunderwater breathing and clearer vision,",
                    "&bplus faster mining (including on land).",
                    "&8One active protection. A second drink is blocked.",
                    "&8Existing potions keep their premium benefits."),
            ItemDefinition.Kind.ITEM,
            "exceldeathprotection", "potion", "lore_version", 2,
            "exceldeathprotection:premium-drink");

    public static final ItemDefinition REVIVE_POTION = new ItemDefinition(
            4, "revive_potion",
            "&d&lRevive Potion",
            "POTION", 0xFF4D6D, null, false,
            List.of(
                    "&7Carry it. Walk up to a downed party",
                    "&7member and tap Revive.",
                    "&7Works everywhere \u2014 even where wands don't.",
                    "&7One use revives one teammate at half health.",
                    "&7Excel Shop \u2014 $5,000"),
            ItemDefinition.Kind.ITEM,
            "exceldowned", "revive_potion", "revive_potion_lore", 1,
            "exceldowned:revive-potion");

    /** The four first entries, in permanent-ID order. */
    public static final List<ItemDefinition> ALL =
            List.of(VEIL_POTION, DEATH_PROTECTION_BASIC, DEATH_POTION, REVIVE_POTION);

    /** A fresh registry pre-loaded with the four builtins. */
    public static ItemRegistry registry() {
        ItemRegistry registry = new ItemRegistry();
        for (ItemDefinition def : ALL) registry.register(def);
        return registry;
    }
}
