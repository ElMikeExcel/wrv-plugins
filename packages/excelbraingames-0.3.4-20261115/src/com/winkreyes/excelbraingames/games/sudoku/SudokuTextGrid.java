package com.winkreyes.excelbraingames.games.sudoku;

import java.util.ArrayList;
import java.util.List;

/**
 * Sudoku board as bordered chat text (0.3.1, Extension 2). On Bedrock the
 * Geyser chest GUI reflows the 54-slot board to ~5 columns, so the Java
 * 9-wide layout wraps mid-row and the puzzle cannot be read as a square
 * (Mike's play-test screenshot, Nov 5, 2026). This renders the SAME round
 * as a true square in chat: boxes delineated, givens bracketed, placed
 * digits plain, blanks dotted, rows and columns numbered so a chat cell
 * maps straight back to the GUI tap surface. Pure logic — the view layer
 * only sends the lines. Picture-mode rounds print their underlying digits;
 * the view adds a legend tying digits to palette pictures.
 */
public final class SudokuTextGrid {

    private SudokuTextGrid() {}

    /** One cell, always exactly 3 characters: "[d]" given, " d " placed, " . " blank. */
    static String cell(SudokuRound round, int idx) {
        int v = round.valueAt(idx);
        if (v == 0) return " . ";
        return round.isGiven(idx) ? "[" + v + "]" : " " + v + " ";
    }

    /**
     * The full grid: a column-number header, then border/row lines with a
     * border at the top, at every box boundary, and at the bottom. Every
     * line has the same length, so the square holds its shape in chat.
     */
    public static List<String> lines(SudokuRound round) {
        int n = round.size();
        int br = round.boxRows(), bc = round.boxCols();
        List<String> out = new ArrayList<>();
        // Column header: digit centered over each 3-wide cell, boxes split by a space.
        StringBuilder header = new StringBuilder("   ");
        for (int c = 0; c < n; c++) {
            if (c > 0 && c % bc == 0) header.append(' ');
            header.append(' ').append(c + 1).append(' ');
        }
        out.add(header + " "); // pad so every line shares one length: the square holds
        String border = border(n, bc);
        for (int r = 0; r < n; r++) {
            if (r % br == 0) out.add(border);
            StringBuilder row = new StringBuilder();
            row.append(r + 1).append(' ');
            for (int c = 0; c < n; c++) {
                if (c % bc == 0) row.append('|');
                row.append(cell(round, r * n + c));
            }
            row.append('|');
            out.add(row.toString());
        }
        out.add(border);
        return out;
    }

    private static String border(int n, int bc) {
        StringBuilder b = new StringBuilder("  +");
        for (int c = 0; c < n; c++) {
            b.append("---");
            if ((c + 1) % bc == 0) b.append('+');
        }
        return b.toString();
    }
}
