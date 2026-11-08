package com.winkreyes.excelbraingames.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Letter Tiles (0.3.1) — the ONE place that knows how a rendered game cell
 * maps to its tile model key. Pure logic, no Bukkit: the view layer stamps
 * the returned key onto the item as its {@code minecraft:item_model}
 * component ({@code braingames:<key>}), the Java resource pack resolves the
 * key to a letter/digit texture, and Geyser maps the same key to the
 * Bedrock tile through its v2 custom-item mappings. A client WITHOUT the
 * pack ignores the component and sees exactly the 0.3.0 items and names —
 * this class changes presentation only, never game state.
 *
 * Key space (kept in lockstep with packs/tiles-manifest.json, cross-checked
 * by TileTest against the generated packs):
 *   ws/&lt;letter&gt;_normal|selected|found   — Word Search cells (26 x 3)
 *   sudoku/&lt;digit&gt;_given|placed|wrong    — Sudoku cells + palette (9 x 3)
 *   number/&lt;0..99&gt;                       — Quick Math kid choice cards (100)
 */
public final class TileKeys {

    public static final String NAMESPACE = "braingames";
    public static final int MAX_NUMBER = 99;

    public enum WsState {
        NORMAL("normal"), SELECTED("selected"), FOUND("found");
        public final String id;
        WsState(String id) { this.id = id; }
    }

    public enum SudokuState {
        GIVEN("given"), PLACED("placed"), WRONG("wrong");
        public final String id;
        SudokuState(String id) { this.id = id; }
    }

    private TileKeys() {}

    /** Word Search cell tile for a letter in a render state. */
    public static String wsModel(char letter, WsState state) {
        char lower = Character.toLowerCase(letter);
        if (lower < 'a' || lower > 'z') throw new IllegalArgumentException("not a letter: " + letter);
        return "ws/" + lower + "_" + state.id;
    }

    /** Sudoku tile for a digit (1–9) in a render state. */
    public static String sudokuModel(int digit, SudokuState state) {
        if (digit < 1 || digit > 9) throw new IllegalArgumentException("not a sudoku digit: " + digit);
        return "sudoku/" + digit + "_" + state.id;
    }

    /** Quick Math number tile for a choice value in [0, MAX_NUMBER]. */
    public static String numberModel(int value) {
        if (value < 0 || value > MAX_NUMBER) throw new IllegalArgumentException("no number tile: " + value);
        return "number/" + value;
    }

    /**
     * Tile key for a Quick Math choice label, or null when the label is not
     * a tileable number (a word, blank, or out of range) — the view then
     * leaves the item unstamped, i.e. exactly the 0.3.0 rendering.
     */
    public static String numberModelFor(String choiceLabel) {
        if (choiceLabel == null) return null;
        String s = choiceLabel.trim();
        if (s.isEmpty() || s.length() > 2) return null;
        for (int i = 0; i < s.length(); i++) if (!Character.isDigit(s.charAt(i))) return null;
        int v = Integer.parseInt(s);
        return v <= MAX_NUMBER ? numberModel(v) : null;
    }

    /** The full expected key space, sorted — the contract the packs must satisfy. */
    public static List<String> allModelKeys() {
        List<String> out = new ArrayList<>(205);
        for (char c = 'a'; c <= 'z'; c++)
            for (WsState s : WsState.values()) out.add(wsModel(c, s));
        for (int d = 1; d <= 9; d++)
            for (SudokuState s : SudokuState.values()) out.add(sudokuModel(d, s));
        for (int v = 0; v <= MAX_NUMBER; v++) out.add(numberModel(v));
        out.sort(null);
        return List.copyOf(out);
    }
}
