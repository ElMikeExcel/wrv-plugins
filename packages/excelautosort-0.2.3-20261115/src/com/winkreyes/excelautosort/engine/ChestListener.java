package com.winkreyes.excelautosort.engine;

import com.winkreyes.excelautosort.ExcelAutoSort;
import com.winkreyes.excelautosort.store.ChestRecord;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * All Bukkit event wiring for the sort + filter engines. Order of
 * operations per SPEC §4: on close, the filter pass runs FIRST, then
 * the sort pass tidies what remains; on open, sort only — opening a
 * chest must never delete anything.
 */
public final class ChestListener implements Listener {

    private final ExcelAutoSort plugin;

    public ChestListener(ExcelAutoSort plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onOpen(InventoryOpenEvent e) {
        plugin.sorter().sortOnOpen(e.getInventory(), e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onClose(InventoryCloseEvent e) {
        plugin.filter().closeSweep(e.getInventory(),
                e.getPlayer() instanceof Player p ? p : null);
        plugin.sorter().sortOnClose(e.getInventory(), e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onClick(InventoryClickEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.filter().noteInteraction(e.getView().getTopInventory(), p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrag(InventoryDragEvent e) {
        if (e.getWhoClicked() instanceof Player p) {
            plugin.filter().noteInteraction(e.getView().getTopInventory(), p);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHopperMove(InventoryMoveItemEvent e) {
        plugin.filter().noteHopper(e.getDestination());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (isContainerBlock(e.getBlock())) plugin.filter().onBlockGone(e.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent e) {
        for (Block b : e.blockList()) if (isContainerBlock(b)) plugin.filter().onBlockGone(b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent e) {
        for (Block b : e.blockList()) if (isContainerBlock(b)) plugin.filter().onBlockGone(b);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlock().getType() == Material.CHEST) {
            plugin.filter().onChestPlaced(e.getBlock(), e.getPlayer());
        }
    }

    /** §7.1: sneak + right-click with an empty hand opens the selector — configure-rights holders only. */
    @EventHandler
    public void onSneakOpen(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND) return;
        Player p = e.getPlayer();
        if (!p.isSneaking()) return;
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand != null && hand.getType() != Material.AIR) return;
        Block b = e.getClickedBlock();
        if (b == null || !isContainerBlock(b)) return;
        if (!p.hasPermission("excelautosort.chest")) return;
        List<String> keys = FilterEngine.keysOfBlock(b);
        if (keys.isEmpty()) return;
        ChestRecord r = plugin.flow().recordOf(keys);
        if (!plugin.rights().canConfigure(p, b.getLocation(), r == null ? null : r.owner)) return;
        e.setCancelled(true);
        plugin.openSelector(p, keys);
    }

    /**
     * §7.1 compass entry (Mike, Nov 6, 2026, 11:51 PM CST): hitting or
     * tapping a chest/barrel with a compass in the main hand opens that
     * chest's filter menu — the same targeted resolution and the same
     * §6.4 configure-rights check as the command path. A chest the
     * player may not configure gets the plain no-permission line,
     * never the menu.
     */
    @EventHandler
    public void onCompassTap(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() != Action.LEFT_CLICK_BLOCK && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        if (hand == null || hand.getType() != Material.COMPASS) return;
        Block b = e.getClickedBlock();
        if (b == null || !isContainerBlock(b)) return;
        if (!p.hasPermission("excelautosort.chest")) return;
        List<String> keys = FilterEngine.keysOfBlock(b);
        if (keys.isEmpty()) return;
        e.setCancelled(true);
        ChestRecord r = plugin.flow().recordOf(keys);
        if (!plugin.rights().canConfigure(p, b.getLocation(), r == null ? null : r.owner)) {
            plugin.messages().send(p, "no-permission", "&cYou don't have permission for that.");
            return;
        }
        plugin.openSelector(p, keys);
    }

    private static boolean isContainerBlock(Block b) {
        return b.getType() == Material.CHEST || b.getType() == Material.BARREL
                || b.getType() == Material.TRAPPED_CHEST;
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
