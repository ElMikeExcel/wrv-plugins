package com.winkreyes.exceldowned;

import com.winkreyes.excelitems.api.ExcelItemsAPI;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;

/**
 * The Revive Potion item (ExcelDowned 1.0.0, spec A section 2) — the
 * mint factory + identity. Follows the ExcelDeathProtection 0.2.0
 * house precedent: a Material.POTION whose PersistentDataContainer
 * carries the identity tag; the tag is the ONLY identity — display
 * name and lore are pictures, never trusted (GiveUpHolder principle).
 *
 * 1.0.1 (M1 migration, P-ITEM-REGISTRY-1, D124): the construction of
 * record lives ONCE in the ExcelItems registry as permanent ID 4
 * (key "revive_potion"). Minting FULLY delegates to
 * ExcelItemsAPI.create(4) — the registry mint dual-stamps the
 * primary excelitems:id mark plus this plugin's legacy marks
 * (exceldowned:revive_potion BYTE 1 + exceldowned:revive_potion_lore
 * INTEGER 1), so pre-migration recognition paths keep working.
 * Identification goes through the API (primary mark first, legacy
 * fallback — the registry's own rule); if the API cannot be reached
 * the legacy PDC check below remains as the fail-soft fallback, so
 * a registry hiccup never makes a held potion unrecognizable.
 * This class no longer constructs the item itself.
 *
 * Counting and consuming scan the storage contents plus the offhand
 * (spec 2: main inventory + hotbar; NOT ender chest, NOT shulker
 * contents) and delegate the arithmetic to the pure
 * RevivePotionPolicy slot functions the harness tests.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePotionItem {

    /** Lore version stamped beside the identity tag (precedent). */
    public static final int LORE_VERSION = 1;

    /** Permanent ExcelItems registry ID for the Revive Potion. */
    public static final int REGISTRY_ID = 4;

    private RevivePotionItem() {}

    static NamespacedKey tagKey(ExcelDowned plugin) {
        return new NamespacedKey(plugin, "revive_potion");
    }

    static NamespacedKey loreKey(ExcelDowned plugin) {
        return new NamespacedKey(plugin, "revive_potion_lore");
    }

    /**
     * Mint one Revive Potion via the registry (D124 full delegation).
     * The returned stack carries BOTH the primary excelitems:id=4
     * mark and the legacy exceldowned marks (registry dual-stamp).
     * Display (name, colour, lore) is the registry definition's —
     * its last lore line pins the default "$5,000" text (D123 D3);
     * the CHARGE price stays ExcelDowned's config fact and is never
     * read from the item.
     */
    public static ItemStack create(ExcelDowned plugin) {
        return ExcelItemsAPI.get().create(REGISTRY_ID);
    }

    /**
     * Pure identify rule (the registry's rule, harness-tested):
     * a primary mark decides alone (id == 4 is a revive potion, any
     * other primary id is NOT — a forged/stale primary never falls
     * through to legacy); only with NO primary mark does the legacy
     * tag decide.
     */
    public static boolean isReviveFromMarks(Integer primaryId, boolean legacyTagged) {
        if (primaryId != null) return primaryId == REGISTRY_ID;
        return legacyTagged;
    }

    /** Legacy-only identity check (pre-migration marks). */
    static boolean hasLegacyTag(ExcelDowned plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.POTION || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(tagKey(plugin), PersistentDataType.BYTE);
    }

    /** Identity is the item's own data alone — never name or lore. */
    public static boolean isRevivePotion(ExcelDowned plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.POTION || !stack.hasItemMeta()) {
            return false;
        }
        try {
            ExcelItemsAPI api = ExcelItemsAPI.get();
            java.util.OptionalInt primary = api.identify(stack);
            if (primary.isPresent()) return primary.getAsInt() == REGISTRY_ID;
            // No primary mark: the API already applies the legacy
            // fallback internally; re-checking the legacy tag here
            // keeps the answer identical if the registry's legacy
            // table ever narrows.
            return hasLegacyTag(plugin, stack);
        } catch (Throwable apiUnavailable) {
            // Fail-soft: ExcelItems is a hard depend, so this should
            // never happen while Downed is enabled — but recognition
            // of already-held potions must not die with a lookup.
            return hasLegacyTag(plugin, stack);
        }
    }

    /**
     * The scanned slots, in scan order: storage contents (hotbar +
     * main rows) followed by the offhand. Returns live references —
     * callers mutate amounts through the inventory, not the array.
     */
    private static ItemStack[] scannedSlots(PlayerInventory inv) {
        ItemStack[] storage = inv.getStorageContents();
        ItemStack[] all = Arrays.copyOf(storage, storage.length + 1);
        all[storage.length] = inv.getItemInOffHand();
        return all;
    }

    private static void extract(ExcelDowned plugin, ItemStack[] slots,
                                boolean[] tagged, int[] amounts) {
        for (int i = 0; i < slots.length; i++) {
            tagged[i] = isRevivePotion(plugin, slots[i]);
            amounts[i] = slots[i] == null ? 0 : slots[i].getAmount();
        }
    }

    /** How many Revive Potions the inventory holds (spec 2 count). */
    public static int countPotions(ExcelDowned plugin, PlayerInventory inv) {
        ItemStack[] slots = scannedSlots(inv);
        boolean[] tagged = new boolean[slots.length];
        int[] amounts = new int[slots.length];
        extract(plugin, slots, tagged, amounts);
        return RevivePotionPolicy.countFromSlots(tagged, amounts);
    }

    /**
     * Remove exactly one potion (the first tagged stack, per the
     * policy's slot choice). Returns true only when one was removed.
     * Callers invoke this ONLY after a revive has landed (spec 3.5
     * ordering invariant, RevivePotionPolicy.shouldConsume).
     */
    public static boolean consumeOne(ExcelDowned plugin, PlayerInventory inv) {
        ItemStack[] slots = scannedSlots(inv);
        boolean[] tagged = new boolean[slots.length];
        int[] amounts = new int[slots.length];
        extract(plugin, slots, tagged, amounts);
        int idx = RevivePotionPolicy.consumeSlotIndex(tagged, amounts);
        if (idx < 0) return false;
        ItemStack stack = slots[idx];
        if (stack.getAmount() <= 1) {
            if (idx == slots.length - 1) inv.setItemInOffHand(null);
            else inv.setItem(idx, null);
        } else {
            stack.setAmount(stack.getAmount() - 1);
            if (idx == slots.length - 1) inv.setItemInOffHand(stack);
            else inv.setItem(idx, stack);
        }
        return true;
    }
}
