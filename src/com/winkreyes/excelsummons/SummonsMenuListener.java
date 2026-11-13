package com.winkreyes.excelsummons;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Menu click wiring for the /summons menu (0.1.7): the menus are
 * control panels, never item sources — every click in one of our
 * views is cancelled, and clicks inside the view itself delegate
 * to SummonsMenus.handleClick (the DummyListener pattern).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class SummonsMenuListener implements Listener {

    private final ExcelSummons plugin;

    public SummonsMenuListener(ExcelSummons plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getInventory().getHolder() instanceof SummonsMenus.MainMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.AdoptedMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.ConfirmDismissMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                SummonsMenus.handleClick(plugin, p, event.getInventory().getHolder(),
                        event.getSlot());
            }
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof SummonsMenus.MainMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.AdoptedMenu
                || event.getInventory().getHolder() instanceof SummonsMenus.ConfirmDismissMenu) {
            event.setCancelled(true);
        }
    }
}
