package com.winkreyes.excelbraingames.core;

/**
 * Word Search board geometry (0.3.2) — which slot map a round renders
 * through. Mike's Bedrock layout investigation (Nov 7, 2026) proved the
 * 0.3.1 failure is pure geometry: a chest GUI is a 9-wide grid, Geyser
 * reflows it to ~5 columns on a phone, and the 2D adjacency the game is
 * built on dies in the wrap — while taps still map back to the correct
 * Java slots. The fix deals Bedrock starters a board shaped for the
 * phone: the slot order IS the board, so the Geyser wrap displays the
 * board exactly as dealt.
 *
 * <p>The geometry is recorded ON THE ROUND (the save carries it), never
 * re-derived from whoever is looking: a round renders through its own
 * map on every client, so a round started on one platform resumes
 * unscrambled on the other. Rounds saved before 0.3.2 carry no geometry
 * key and load as {@link #JAVA} — the only geometry that existed.
 *
 * <p>JAVA: the original map, cell (r,c) at slot {@code r*9 + c}; the
 * Words / New Game / Close controls at slots 51/52/53 sit over the
 * bottom row of a 9-wide board, so the cells under them are void.
 * BEDROCK: the board is a contiguous stream, cell (r,c) at slot
 * {@code r*5 + c} — with the shipped 5-column shapes the stream and the
 * logical grid are the same numbering; the controls still render at
 * 51/52/53, past the end of every shipped Bedrock board (max 50 cells),
 * so Bedrock boards have no voids.
 */
public enum WsGeometry {

    JAVA(9),
    BEDROCK(5);

    /** Columns of the inventory grid this geometry renders into. */
    public final int guiCols;

    WsGeometry(int guiCols) {
        this.guiCols = guiCols;
    }

    /**
     * Save-file value → geometry. Absent, null, or unrecognized values
     * are {@link #JAVA}: the safe default (it is also the only geometry
     * a pre-0.3.2 save can mean).
     */
    public static WsGeometry parse(String s) {
        if (s != null) {
            String t = s.trim();
            for (WsGeometry g : values()) {
                if (g.name().equalsIgnoreCase(t)) return g;
            }
        }
        return JAVA;
    }

    /** Cell index (row-major over the logical board) → inventory slot. */
    public int slotOf(int idx, int cols) {
        if (this == BEDROCK) return idx;
        return (idx / cols) * guiCols + (idx % cols);
    }

    /** Inventory slot → cell index, or -1 when the slot is not a board cell. */
    public int cellAt(int slot, int cols, int rows) {
        if (slot < 0 || slot >= 54) return -1;
        if (this == BEDROCK) return slot < cols * rows ? slot : -1;
        int r = slot / guiCols, c = slot % guiCols;
        return (r < rows && c < cols) ? r * cols + c : -1;
    }

    /**
     * Cells removed from play because the Words / New Game / Close
     * controls (slots 51/52/53) render over them. Computed through the
     * slot map itself, so it is the legacy rule stated once: under JAVA
     * it marks exactly the cells the 0.3.1 code marked (the bottom-row
     * cells whose 9-wide slot is a control slot); under BEDROCK with
     * the shipped shapes it marks none.
     */
    public boolean[] controlVoids(int cols, int rows) {
        boolean[] v = new boolean[cols * rows];
        for (int slot = 51; slot <= 53; slot++) {
            int idx = cellAt(slot, cols, rows);
            if (idx >= 0) v[idx] = true;
        }
        return v;
    }
}
