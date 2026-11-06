package com.winkreyes.excelcosmetics;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/** Click/drag guard for the picker GUI: the GUI is a menu, never
 *  a container — nothing goes in, nothing comes out. */
final class PickerListener implements Listener {

    private final ExcelCosmetics plugin;

    PickerListener(ExcelCosmetics plugin) { this.plugin = plugin; }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getView().getTopInventory().getHolder();
        if (!(holder instanceof PickerGui.Holder picker)) return;
        if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
            event.setCancelled(true);
            if (event.getWhoClicked() instanceof Player player) {
                PickerGui.handleClick(plugin, player, picker, event.getRawSlot());
            }
        } else if (event.isShiftClick()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PickerGui.Holder) {
            event.setCancelled(true);
        }
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
