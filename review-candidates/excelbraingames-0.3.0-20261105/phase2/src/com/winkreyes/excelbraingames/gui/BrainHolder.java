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

    public enum View { PICKER, THEME_PICK, TIER_PICK, RESUME, WS_BOARD, WS_WORDS, MEMORY_BOARD, SUDOKU_BOARD }

    public final View view;
    public final Map<Integer, String> actions = new HashMap<>();
    public String game;      // "wordsearch" | "memory" | "sudoku" where relevant
    public boolean armed;    // discard confirmation armed
    public boolean done;     // board completed; cell clicks ignored
    // Sudoku transient GUI state (never persisted; the round model is):
    public int sudokuSel = 0;          // 0 = nothing picked, -1 = eraser, 1..9 = digit
    public boolean sudokuBankHigh;     // 9x9 palette bank: false = digits 1-5, true = 6-9
    public int sudokuPage = 0;         // 9x9 view: 0 = rows 1-5, 1 = rows 5-9
    public java.util.Set<Integer> sudokuWrong = java.util.Set.of();
    public long sudokuWrongUntil;      // millis; wrong-cell highlight expiry
    private Inventory inventory;

    public BrainHolder(View view) { this.view = view; }

    public void bind(Inventory inventory) { this.inventory = inventory; }

    @Override
    public Inventory getInventory() { return inventory; }
}
