package com.winkreyes.excelmenu.bukkit;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

/**
 * The Java purchase-confirm screen (0.1.4). One-shot: the stored
 * action lines run at most once, only from the green Confirm slot,
 * only for the player it was opened for, and only within
 * EXPIRY_MILLIS. Cancel re-opens the parent menu; closing the
 * window runs nothing.
 */
final class ConfirmHolder implements InventoryHolder {
    static final int SIZE = 27;
    static final int SLOT_PREVIEW = 13;
    static final int SLOT_CONFIRM = 11;
    static final int SLOT_CANCEL = 15;
    static final long EXPIRY_MILLIS = 30_000L;

    private final UUID player;
    private final String menuId;
    private final String itemId;
    private final List<String> lines;
    private final long createdMillis;
    private boolean used;
    private Inventory inventory;

    ConfirmHolder(UUID player, String menuId, String itemId, List<String> lines, long now) {
        this.player = player;
        this.menuId = menuId;
        this.itemId = itemId;
        this.lines = List.copyOf(lines);
        this.createdMillis = now;
    }

    UUID player() { return player; }
    String menuId() { return menuId; }
    String itemId() { return itemId; }
    List<String> lines() { return lines; }
    boolean expired(long now) { return now - createdMillis > EXPIRY_MILLIS; }

    /** True exactly once. */
    boolean consume() {
        if (used) return false;
        used = true;
        return true;
    }

    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
