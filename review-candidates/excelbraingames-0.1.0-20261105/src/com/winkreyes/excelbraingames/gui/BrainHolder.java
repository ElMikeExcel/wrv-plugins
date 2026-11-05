package com.winkreyes.excelbraingames.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marks every Brain Games inventory and carries the click routing table the
 * view builder filled in: slot -> action string ("cell:12", "theme:animals",
 * "tier:wordsearch:standard", "resume:memory", "ctl:words", ...). The
 * listener dispatches actions; it never guesses meaning from items.
 */
public final class BrainHolder implements InventoryHolder {

    public enum View { PICKER, THEME_PICK, TIER_PICK, RESUME, WS_BOARD, WS_WORDS, MEMORY_BOARD }

    public final View view;
    public final Map<Integer, String> actions = new HashMap<>();
    public String game;      // "wordsearch" | "memory" where relevant
    public boolean armed;    // discard confirmation armed
    public boolean done;     // board completed; cell clicks ignored
    private Inventory inventory;

    public BrainHolder(View view) { this.view = view; }

    public void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
