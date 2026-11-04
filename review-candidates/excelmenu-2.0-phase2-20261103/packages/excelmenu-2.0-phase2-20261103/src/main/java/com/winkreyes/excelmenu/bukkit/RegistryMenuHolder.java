// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.render.ChestModel;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.Map;
import java.util.UUID;

/**
 * Marks an inventory as a registry-menu view and remembers, per
 * slot, what the chest model placed there — the click listener
 * trusts this map, never the ItemStack (the MenuHolder rule, carried
 * over). The generation counter stale-proofs delayed actions: a
 * [delay] line from an older view of the menu finds its generation
 * superseded and does not run.
 */
final class RegistryMenuHolder implements InventoryHolder {

    private final UUID player;
    private final String menuId;
    private final int page; // 1-based
    private final long generation;
    private final Map<Integer, ChestModel.Placed> placed;
    private Inventory inventory;

    RegistryMenuHolder(UUID player, String menuId, int page, long generation,
                       Map<Integer, ChestModel.Placed> placed) {
        this.player = player;
        this.menuId = menuId;
        this.page = page;
        this.generation = generation;
        this.placed = placed;
    }

    UUID player() { return player; }

    String menuId() { return menuId; }

    int page() { return page; }

    long generation() { return generation; }

    ChestModel.Placed at(int slot) { return placed.get(slot); }

    void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
