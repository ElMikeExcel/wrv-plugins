package com.winkreyes.exceldowned;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

/**
 * Marks an inventory as the ExcelDowned give-up confirm menu and
 * remembers, per slot, which action is rendered there — the click
 * listener trusts this map, never the ItemStack, so a renamed or
 * copied item can never smuggle a different action into the menu.
 * (The ExcelMenu MenuHolder pattern.)
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GiveUpHolder implements InventoryHolder {

    public static final String ACTION_LET_GO = "letgo";
    public static final String ACTION_KEEP_FIGHTING = "keepfighting";

    private final Map<Integer, String> slotActions;
    private Inventory inventory;

    public GiveUpHolder(Map<Integer, String> slotActions) {
        this.slotActions = slotActions;
    }

    public String actionAt(int slot) { return slotActions.get(slot); }

    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
