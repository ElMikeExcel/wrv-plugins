package com.winkreyes.excelskillspanel.bukkit;

import com.winkreyes.excelskillspanel.ExcelSkillsPanel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryHolder;

/**
 * Menu click wiring for the Skills panel: the menus are displays,
 * never item sources — every click in one of our views is
 * cancelled, and clicks inside the view itself delegate to
 * PanelMenus.handleClick (the house menu-listener pattern).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PanelMenuListener implements Listener {

    private final ExcelSkillsPanel plugin;

    public PanelMenuListener(ExcelSkillsPanel plugin) {
        this.plugin = plugin;
    }

    private static boolean ours(InventoryHolder holder) {
        return holder instanceof PanelMenus.MainMenu
                || holder instanceof PanelMenus.SkillDetailMenu
                || holder instanceof PanelMenus.MagicDetailMenu;
    }

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        InventoryHolder holder = event.getInventory().getHolder();
        if (ours(holder)) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                PanelMenus.handleClick(plugin, p, holder, event.getSlot());
            }
        }
    }

    @EventHandler
    public void onMenuDrag(InventoryDragEvent event) {
        if (ours(event.getInventory().getHolder())) {
            event.setCancelled(true);
        }
    }
}
