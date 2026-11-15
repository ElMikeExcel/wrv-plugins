package com.winkreyes.excelbraingames.core;

import java.util.Locale;

/** Compass directions on the Word Search grid, in (row, col) deltas. Pure logic. */
public enum Dir {
    N(-1, 0), S(1, 0), E(0, 1), W(0, -1),
    NE(-1, 1), NW(-1, -1), SE(1, 1), SW(1, -1);

    public final int dr, dc;

    Dir(int dr, int dc) { this.dr = dr; this.dc = dc; }

    public static Dir ofDelta(int dr, int dc) {
        for (Dir d : values()) if (d.dr == dr && d.dc == dc) return d;
        return null;
    }

    public static Dir parse(String s) {
        try { return valueOf(s.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException e) { return null; }
    }
}
