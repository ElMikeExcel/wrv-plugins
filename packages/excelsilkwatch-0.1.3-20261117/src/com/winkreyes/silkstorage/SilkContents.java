// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import org.bukkit.block.ShulkerBox;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

/** Bounded read-only inspection. Never changes or applies ItemMeta/BlockState snapshots. */
public final class SilkContents {
    // Root + at most four levels of contained stacks, and at most 256 visited stacks.
    // Ordinary vanilla shulkers require only depth 1 and at most 28 nodes.
    public static final int MAX_DEPTH = 4;
    public static final int MAX_NODES = 256;
    private SilkContents() { }

    public static boolean containsSilkTouch(ItemStack item) {
        return inspect(item, 0, new int[] { MAX_NODES });
    }

    private static boolean inspect(ItemStack item, int depth, int[] budget) {
        if (item == null || depth > MAX_DEPTH || budget[0] <= 0) return false;
        budget[0]--;
        try {
            if (item.getAmount() <= 0 || item.getType().isAir()) return false;
            if (item.containsEnchantment(Enchantment.SILK_TOUCH)) return true;
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof EnchantmentStorageMeta book && book.hasStoredEnchant(Enchantment.SILK_TOUCH))
                return true;
            if (depth == MAX_DEPTH || !(meta instanceof BlockStateMeta blockMeta) ||
                    !(blockMeta.getBlockState() instanceof ShulkerBox box)) return false;
            for (ItemStack child : box.getInventory().getContents()) {
                if (budget[0] <= 0) break;
                if (inspect(child, depth + 1, budget)) return true;
            }
        } catch (RuntimeException malformedMetadata) {
            // A broken/custom item must not disrupt the player's inventory transaction.
            // Treat unreadable data as unknown/nonmatching, never speculate or mutate it.
            return false;
        }
        return false;
    }
}
