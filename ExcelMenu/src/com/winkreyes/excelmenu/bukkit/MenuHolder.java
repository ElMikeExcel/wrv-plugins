package com.winkreyes.excelmenu.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

/**
 * Marks an inventory as an ExcelMenu view and remembers, per slot,
 * which definition item is rendered there — the click listener
 * trusts this map, never the ItemStack, so a renamed or copied item
 * can never smuggle a different action into a menu.
 */
final class MenuHolder implements InventoryHolder {

    private final String menuId;
    private Map<Integer, String> slotItems;
    private Inventory inventory;
    private long lastRenderMillis;

    MenuHolder(String menuId, Map<Integer, String> slotItems) {
        this.menuId = menuId;
        this.slotItems = slotItems;
    }

    String menuId() { return menuId; }

    String itemAt(int slot) { return slotItems.get(slot); }

    void replaceSlotItems(Map<Integer, com.winkreyes.excelmenu.core.MenuDef.ItemDef> visible) {
        Map<Integer, String> next = new java.util.LinkedHashMap<>();
        for (Map.Entry<Integer, com.winkreyes.excelmenu.core.MenuDef.ItemDef> e
                : visible.entrySet()) {
            next.put(e.getKey(), e.getValue().id());
        }
        this.slotItems = next;
    }

    void markRendered() { this.lastRenderMillis = System.currentTimeMillis(); }

    long lastRenderMillis() { return lastRenderMillis; }

    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
