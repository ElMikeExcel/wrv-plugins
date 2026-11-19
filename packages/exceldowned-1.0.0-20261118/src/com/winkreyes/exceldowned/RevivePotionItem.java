package com.winkreyes.exceldowned;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The Revive Potion item (ExcelDowned 1.0.0, spec A section 2) — the
 * mint factory + identity. Follows the ExcelDeathProtection 0.2.0
 * house precedent: a Material.POTION whose PersistentDataContainer
 * carries the identity tag; the tag is the ONLY identity — display
 * name and lore are pictures, never trusted (GiveUpHolder principle).
 *
 * This is the ONLY place the PDC keys are named, and the ONLY code
 * that mints the item (the seam is /downed revivepotion give, spec
 * section 8). Counting and consuming scan the storage contents plus
 * the offhand (spec 2: main inventory + hotbar; NOT ender chest,
 * NOT shulker contents) and delegate the arithmetic to the pure
 * RevivePotionPolicy slot functions the harness tests.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePotionItem {

    /** Lore version stamped beside the identity tag (precedent). */
    public static final int LORE_VERSION = 1;

    private RevivePotionItem() {}

    static NamespacedKey tagKey(ExcelDowned plugin) {
        return new NamespacedKey(plugin, "revive_potion");
    }

    static NamespacedKey loreKey(ExcelDowned plugin) {
        return new NamespacedKey(plugin, "revive_potion_lore");
    }

    /** Mint one tagged Revive Potion (display per spec 2 / S-2). */
    public static ItemStack create(ExcelDowned plugin) {
        ItemStack stack = new ItemStack(Material.POTION, 1);
        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta) {
            PotionMeta potion = (PotionMeta) meta;
            potion.setColor(Color.fromRGB(0xFF, 0x4D, 0x6D)); // revive pink-red
            potion.setDisplayName(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Revive Potion");
            List<String> lore = new ArrayList<>(Arrays.asList(
                    ChatColor.GRAY + "Carry it. Walk up to a downed party",
                    ChatColor.GRAY + "member and tap Revive.",
                    ChatColor.GRAY + "Works everywhere — even where wands don't.",
                    ChatColor.GRAY + "One use revives one teammate at half health.",
                    ChatColor.GRAY + "Excel Shop — $" + String.format("%,d", plugin.revivePotionPrice)));
            potion.setLore(lore);
            potion.getPersistentDataContainer().set(tagKey(plugin), PersistentDataType.BYTE, (byte) 1);
            potion.getPersistentDataContainer().set(loreKey(plugin), PersistentDataType.INTEGER, LORE_VERSION);
            stack.setItemMeta(potion);
        }
        return stack;
    }

    /** Identity is the PDC tag alone — never the name or lore. */
    public static boolean isRevivePotion(ExcelDowned plugin, ItemStack stack) {
        if (stack == null || stack.getType() != Material.POTION || !stack.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(tagKey(plugin), PersistentDataType.BYTE);
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
