package com.winkreyes.exceldowned;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;

/**
 * Marks an inventory as the ExcelDowned Revive Potion confirm menu
 * and remembers, per slot, which action is rendered there — the
 * click listener trusts this map, never the ItemStack, so a renamed
 * or copied item can never smuggle a different action into the menu.
 * (The GiveUpHolder / ExcelMenu MenuHolder pattern, spec A 3.4.)
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePotionHolder implements InventoryHolder {

    public static final String ACTION_CONFIRM = "confirm";
    public static final String ACTION_BACK = "back";

    private final Map<Integer, String> slotActions;
    private Inventory inventory;

    public RevivePotionHolder(Map<Integer, String> slotActions) {
        this.slotActions = slotActions;
    }

    public String actionAt(int slot) { return slotActions.get(slot); }

    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
