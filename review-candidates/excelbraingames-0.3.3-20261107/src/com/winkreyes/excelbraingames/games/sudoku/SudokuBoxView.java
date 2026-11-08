package com.winkreyes.excelbraingames.games.sudoku;

import com.winkreyes.excelbraingames.core.WsGeometry;

/**
 * Sudoku box view (0.3.3) — the Bedrock presentation geometry for
 * Sudoku, as pure slot math (no Bukkit; the view layer renders through
 * these maps and the listener routes taps back through them).
 *
 * The problem (Mike's play-tests, Nov 5–7, 2026): a chest GUI is a
 * 9-wide grid, Geyser reflows it to ~5 columns on a phone, and a 9x9
 * board cannot survive that wrap — the 0.3.1 chat square made the
 * puzzle readable but left taps as counting exercises ("helpful but
 * still very confusing, borderline unplayable"). A 9x9 square cannot
 * be rendered at wrap width 5; that is arithmetic, not effort.
 *
 * The approved design (Mike, Nov 7, 2026, after rejecting a
 * non-visual form-based variant): page the presentation by BOX. A
 * Sudoku box (3x3 on the 9x9 tiers, 2x2 on the kid 4x4, 2 rows x 3
 * cols on the 6x6) is at most 3 wide, so it fits the wrap: the box's
 * cells sit at stream offsets {@code localRow * 5 + localCol}, and
 * the phone's ~5-wide wrap renders them as a true visual square —
 * the same stream trick as the 0.3.2 Word Search board, at box
 * scale. The box picker is itself laid out one entry per box on the
 * same stride, and the digit picker lays digits 1-9 in rows of three
 * on the same stride, so every Bedrock Sudoku surface is visual and
 * every tap lands where the eye put it — no counting anywhere.
 *
 * KEY DIFFERENCE from Word Search geometry: Sudoku's board is the
 * SAME board on both platforms (the box view is a paging of the
 * presentation, not a different board shape), so nothing is recorded
 * on the round and the save format is untouched. Presentation keys
 * off the VIEWER's platform, decided once when the board opens
 * ({@link #useBoxView}); a round dealt on one platform opens with the
 * other platform's presentation and the identical board —
 * cross-platform resume is unscrambled by construction, and the
 * tests prove the maps depend only on round state.
 */
public final class SudokuBoxView {

    /** The phone wrap width every Bedrock Sudoku surface is laid out for. */
    public static final int DISPLAY_WIDTH = 5;

    private SudokuBoxView() {}

    /**
     * The one presentation decision: a Bedrock viewer gets the box
     * view; anything else (Java, and the unknown-platform default that
     * {@code ExcelBrainGames.platformOf} answers JAVA for) gets the
     * full-board view, exactly as 0.3.2 and earlier rendered it.
     */
    public static boolean useBoxView(WsGeometry viewerGeometry) {
        return viewerGeometry == WsGeometry.BEDROCK;
    }

    // ---------------------------------------------------------- box structure

    public static int boxesPerRow(SudokuRound r) { return r.size() / r.boxCols(); }

    public static int boxRowsCount(SudokuRound r) { return r.size() / r.boxRows(); }

    public static int boxCount(SudokuRound r) { return boxesPerRow(r) * boxRowsCount(r); }

    /** Which box a board cell belongs to (boxes numbered row-major). */
    public static int boxOf(SudokuRound r, int idx) {
        int n = r.size();
        return (idx / n / r.boxRows()) * boxesPerRow(r) + (idx % n / r.boxCols());
    }

    /** Board row of the box's top-left cell. */
    public static int boxOriginRow(SudokuRound r, int box) {
        return (box / boxesPerRow(r)) * r.boxRows();
    }

    /** Board column of the box's top-left cell. */
    public static int boxOriginCol(SudokuRound r, int box) {
        return (box % boxesPerRow(r)) * r.boxCols();
    }

    /** The box's board indices, in local row-major order. */
    public static int[] boxCells(SudokuRound r, int box) {
        int n = r.size(), br = r.boxRows(), bc = r.boxCols();
        int[] out = new int[br * bc];
        int r0 = boxOriginRow(r, box), c0 = boxOriginCol(r, box);
        for (int lr = 0; lr < br; lr++)
            for (int lc = 0; lc < bc; lc++)
                out[lr * bc + lc] = (r0 + lr) * n + (c0 + lc);
        return out;
    }

    // ---------------------------------------------------------- box view map

    /**
     * A cell's slot inside its box's view: local position on the
     * display stride, so the wrap shows the box as dealt.
     */
    public static int slotOfCell(SudokuRound r, int idx) {
        int n = r.size();
        int lr = (idx / n) % r.boxRows();
        int lc = (idx % n) % r.boxCols();
        return lr * DISPLAY_WIDTH + lc;
    }

    /** A box-view slot → board index, or -1 when the slot is not a cell of this box. */
    public static int cellAtSlot(SudokuRound r, int box, int slot) {
        if (slot < 0 || slot >= 54) return -1;
        int lr = slot / DISPLAY_WIDTH, lc = slot % DISPLAY_WIDTH;
        if (lr >= r.boxRows() || lc >= r.boxCols()) return -1;
        return (boxOriginRow(r, box) + lr) * r.size() + (boxOriginCol(r, box) + lc);
    }

    // ---------------------------------------------------------- box picker map

    /** A box's slot in the picker: box position on the display stride. */
    public static int slotOfBox(SudokuRound r, int box) {
        return (box / boxesPerRow(r)) * DISPLAY_WIDTH + (box % boxesPerRow(r));
    }

    /** A picker slot → box, or -1 when the slot is not a box entry. */
    public static int boxAtSlot(SudokuRound r, int slot) {
        if (slot < 0 || slot >= 54) return -1;
        int br = slot / DISPLAY_WIDTH, bc = slot % DISPLAY_WIDTH;
        if (br >= boxRowsCount(r) || bc >= boxesPerRow(r)) return -1;
        return br * boxesPerRow(r) + bc;
    }

    // ---------------------------------------------------------- digit picker map

    /** A digit's slot in the picker: digits in rows of three on the display stride. */
    public static int slotOfDigit(int digit) {
        int z = digit - 1;
        return (z / 3) * DISPLAY_WIDTH + (z % 3);
    }

    /** A digit-picker slot → digit for this round's size, or -1 when not a digit tile. */
    public static int digitAtSlot(SudokuRound r, int slot) {
        if (slot < 0 || slot >= 54) return -1;
        int row = slot / DISPLAY_WIDTH, col = slot % DISPLAY_WIDTH;
        if (col >= 3) return -1;
        int d = row * 3 + col + 1;
        return d <= r.size() ? d : -1;
    }

    // ---------------------------------------------------------- box state

    /** Cells of the box holding any value (given or placed). */
    public static int boxFilled(SudokuRound r, int box) {
        int n = 0;
        for (int idx : boxCells(r, box)) if (r.valueAt(idx) != 0) n++;
        return n;
    }

    /**
     * Finished = every cell of the box is filled AND matches the
     * solution — the picker's finished mark means exactly this.
     */
    public static boolean boxFinished(SudokuRound r, int box) {
        for (int idx : boxCells(r, box)) {
            if (r.valueAt(idx) == 0 || r.valueAt(idx) != r.solutionAt(idx)) return false;
        }
        return true;
    }

    // ---------------------------------------------------------- context lines

    /**
     * The cell's full row, formatted EXACTLY as {@link SudokuTextGrid}
     * renders that row inside the whole-board square (same cell
     * formatting, same box separators, same leading row number) — the
     * tests pin the two equal, so the context line can never drift
     * from the board print.
     */
    public static String rowLine(SudokuRound r, int idx) {
        int n = r.size(), bc = r.boxCols();
        int row = idx / n;
        StringBuilder sb = new StringBuilder();
        sb.append(row + 1).append(' ');
        for (int c = 0; c < n; c++) {
            if (c % bc == 0) sb.append('|');
            sb.append(SudokuTextGrid.cell(r, row * n + c));
        }
        sb.append('|');
        return sb.toString();
    }

    /**
     * The cell's full column in the same cell formatting: the column's
     * values top to bottom, listed on one chat line with a box
     * separator at each box boundary.
     */
    public static String colLine(SudokuRound r, int idx) {
        int n = r.size(), br = r.boxRows();
        int col = idx % n;
        StringBuilder sb = new StringBuilder("Col ").append(col + 1).append(": ");
        for (int row = 0; row < n; row++) {
            if (row > 0) sb.append(row % br == 0 ? " | " : " ");
            sb.append(SudokuTextGrid.cell(r, row * n + col));
        }
        return sb.toString();
    }
}
