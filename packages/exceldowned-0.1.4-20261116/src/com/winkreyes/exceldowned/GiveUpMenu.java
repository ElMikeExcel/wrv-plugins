package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The Java give-up confirm menu (0.1.1): one row, two unmistakable
 * buttons — a red "✝ LET GO — die now" and a green "Keep fighting" —
 * on a dark filler field. Built the ExcelMenu way: the inventory's
 * holder carries the slot→action truth, items are only pictures.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GiveUpMenu {

    public static final int SIZE = 9;
    public static final int SLOT_LET_GO = 2;
    public static final int SLOT_KEEP_FIGHTING = 6;
    public static final String TITLE = ChatColor.DARK_RED + "✝ Let go?";

    private GiveUpMenu() {}

    public static Inventory build() {
        Map<Integer, String> actions = new LinkedHashMap<>();
        actions.put(SLOT_LET_GO, GiveUpHolder.ACTION_LET_GO);
        actions.put(SLOT_KEEP_FIGHTING, GiveUpHolder.ACTION_KEEP_FIGHTING);
        GiveUpHolder holder = new GiveUpHolder(actions);
        Inventory inv = Bukkit.createInventory(holder, SIZE, TITLE);
        holder.bind(inv);

        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < SIZE; i++) inv.setItem(i, filler);

        inv.setItem(SLOT_LET_GO, named(Material.RED_CONCRETE,
                ChatColor.RED + "" + ChatColor.BOLD + "✝ LET GO — die now",
                ChatColor.GRAY + "You die where you lie.",
                ChatColor.GRAY + "Your items drop. No rescue, no undo."));
        inv.setItem(SLOT_KEEP_FIGHTING, named(Material.GREEN_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "✚ Keep fighting",
                ChatColor.GRAY + "Stay down and hold on —",
                ChatColor.GRAY + "a party member can still revive you."));
        return inv;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }
}
