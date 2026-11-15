package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.WsGeometry;
import com.winkreyes.excelbraingames.games.sudoku.SudokuBoxView;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuTextGrid;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Sudoku box view (0.3.3): the slot maps, the display-shape behavior
 * gate, digit entry, box completion states, context lines,
 * cross-platform resume, and the viewer-platform decision.
 *
 * The display gate is the behavior proof for the defect Mike hit on
 * Nov 5–7, 2026: it renders a box (or picker, or digit picker) into
 * a simulated 54-slot stream wrapped at phone width 5 and verifies the
 * 3x3 (or box-shaped) surface holds as a true visual square — and, as
 * a negative control, that the SAME cells laid out on the 0.3.1-style
 * Java 9-wide map wrapped at phone width do NOT hold that shape, which
 * is why the old board was "borderline unplayable" on a phone.
 *
 * No carried test file was modified; this suite only adds checks.
 */
public class SudokuBoxViewTest {

    /** Shipped tier shapes: {size, boxRows, boxCols} (kid 4x4, 6x6, 9x9). */
    static final int[][] SHAPES = {{4, 2, 2}, {6, 2, 3}, {9, 3, 3}};

    static final int[] GIVENS_MIN = {8, 18, 38};
    static final int[] GIVENS_MAX = {10, 22, 44};

    public static void main(String[] args) {
        Check ck = new Check("SudokuBoxViewTest");
        boxStructure(ck);
        boxViewSlotMap(ck);
        pickerSlotMap(ck);
        digitPickerMap(ck);
        displayShapeGate(ck);
        digitPlacement(ck);
        boxFinishedStates(ck);
        contextLines(ck);
        crossPlatformResume(ck);
        viewerPlatformDecision(ck);
        ck.finish();
    }

    // ------------------------------------------------------------ helpers

    static SudokuRound gen(int size, int br, int bc, long seed) {
        int i = size == 4 ? 0 : size == 6 ? 1 : 2;
        return SudokuRound.generate("t", size, br, bc, GIVENS_MIN[i], GIVENS_MAX[i],
                false, new Random(seed), seed);
    }

    static String digits(int[] a) {
        StringBuilder sb = new StringBuilder(a.length);
        for (int v : a) sb.append((char) ('0' + v));
        return sb.toString();
    }

    /** A round with the source's solution but a chosen board/given mask. */
    static SudokuRound craftedFrom(SudokuRound src, int[] board, boolean[] given) {
        Map<String, Object> m = new LinkedHashMap<>(src.toMap());
        m.put("board", digits(board));
        StringBuilder g = new StringBuilder();
        for (boolean b : given) g.append(b ? '1' : '0');
        m.put("given", g.toString());
        return SudokuRound.fromMap(m);
    }

    static int[] solutionOf(SudokuRound r) {
        int[] s = new int[r.size() * r.size()];
        for (int i = 0; i < s.length; i++) s[i] = r.solutionAt(i);
        return s;
    }

    /** One cell's 3-char rendering, restated independently (cell is package-private). */
    static String fmtCell(SudokuRound r, int idx) {
        int v = r.valueAt(idx);
        if (v == 0) return " . ";
        return r.isGiven(idx) ? "[" + v + "]" : " " + v + " ";
    }

    // ------------------------------------------------------------ structure

    static void boxStructure(Check ck) {
        int[][] expect = {{2, 2, 4}, {2, 3, 6}, {3, 3, 9}}; // boxesPerRow, boxRowsCount, count
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            SudokuRound r = gen(size, br, bc, 7);
            ck.eq(expect[s][0], SudokuBoxView.boxesPerRow(r), "boxesPerRow " + size);
            ck.eq(expect[s][1], SudokuBoxView.boxRowsCount(r), "boxRowsCount " + size);
            ck.eq(expect[s][2], SudokuBoxView.boxCount(r), "boxCount " + size);
            // Every cell belongs to exactly one box, and the box's cells
            // are exactly its rectangle.
            boolean[] seen = new boolean[size * size];
            for (int b = 0; b < SudokuBoxView.boxCount(r); b++) {
                int[] cells = SudokuBoxView.boxCells(r, b);
                ck.eq(br * bc, cells.length, "box " + b + " cell count " + size);
                int r0 = SudokuBoxView.boxOriginRow(r, b), c0 = SudokuBoxView.boxOriginCol(r, b);
                for (int i = 0; i < cells.length; i++) {
                    int idx = cells[i];
                    ck.that(!seen[idx], "cell " + idx + " in exactly one box");
                    seen[idx] = true;
                    ck.eq(b, SudokuBoxView.boxOf(r, idx), "boxOf cell " + idx);
                    ck.eq(r0 + i / bc, idx / size, "box cell row " + idx);
                    ck.eq(c0 + i % bc, idx % size, "box cell col " + idx);
                }
            }
            for (int i = 0; i < seen.length; i++) ck.that(seen[i], "cell " + i + " covered by some box");
        }
    }

    // ------------------------------------------------------------ slot maps

    static void boxViewSlotMap(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            for (int seed = 1; seed <= 20; seed++) {
                SudokuRound r = gen(size, br, bc, seed * 131L);
                for (int b = 0; b < SudokuBoxView.boxCount(r); b++) {
                    boolean[] used = new boolean[54];
                    for (int idx : SudokuBoxView.boxCells(r, b)) {
                        int slot = SudokuBoxView.slotOfCell(r, idx);
                        ck.that(slot >= 0 && slot < 54, "slotOfCell in range, box " + b + " idx " + idx);
                        ck.eq(idx, SudokuBoxView.cellAtSlot(r, b, slot),
                                "cellAtSlot inverts slotOfCell at box " + b + " idx " + idx);
                        // The display position IS the local position:
                        // local row = slot / 5, local col = slot % 5.
                        int lr = (idx / size) % br, lc = (idx % size) % bc;
                        ck.eq(lr, slot / SudokuBoxView.DISPLAY_WIDTH, "slot display row == local row");
                        ck.eq(lc, slot % SudokuBoxView.DISPLAY_WIDTH, "slot display col == local col");
                        ck.that(!used[slot], "no two cells share a slot in box " + b);
                        used[slot] = true;
                    }
                    for (int slot = 0; slot < 54; slot++) {
                        boolean occupied = false;
                        for (int idx : SudokuBoxView.boxCells(r, b))
                            if (SudokuBoxView.slotOfCell(r, idx) == slot) occupied = true;
                        ck.eq(occupied ? 0 : -1,
                                SudokuBoxView.cellAtSlot(r, b, slot) < 0 ? -1 : 0,
                                "cellAtSlot -1 exactly off the box cells, slot " + slot);
                    }
                    ck.eq(-1, SudokuBoxView.cellAtSlot(r, b, -1), "cellAtSlot(-1) is -1");
                    ck.eq(-1, SudokuBoxView.cellAtSlot(r, b, 54), "cellAtSlot(54) is -1");
                }
            }
        }
    }

    static void pickerSlotMap(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            for (int seed = 1; seed <= 20; seed++) {
                SudokuRound r = gen(size, br, bc, seed * 977L);
                boolean[] used = new boolean[54];
                for (int b = 0; b < SudokuBoxView.boxCount(r); b++) {
                    int slot = SudokuBoxView.slotOfBox(r, b);
                    ck.that(slot >= 0 && slot < 54, "slotOfBox in range, box " + b);
                    ck.eq(b, SudokuBoxView.boxAtSlot(r, slot), "boxAtSlot inverts slotOfBox at box " + b);
                    ck.eq(b / SudokuBoxView.boxesPerRow(r), slot / SudokuBoxView.DISPLAY_WIDTH,
                            "picker display row == box row");
                    ck.eq(b % SudokuBoxView.boxesPerRow(r), slot % SudokuBoxView.DISPLAY_WIDTH,
                            "picker display col == box col");
                    ck.that(!used[slot], "no two boxes share a picker slot, box " + b);
                    used[slot] = true;
                }
                for (int slot = 0; slot < 54; slot++) {
                    boolean occupied = false;
                    for (int b = 0; b < SudokuBoxView.boxCount(r); b++)
                        if (SudokuBoxView.slotOfBox(r, b) == slot) occupied = true;
                    ck.eq(occupied ? 0 : -1,
                            SudokuBoxView.boxAtSlot(r, slot) < 0 ? -1 : 0,
                            "boxAtSlot -1 exactly off the box entries, slot " + slot);
                }
                ck.eq(-1, SudokuBoxView.boxAtSlot(r, -1), "boxAtSlot(-1) is -1");
                ck.eq(-1, SudokuBoxView.boxAtSlot(r, 54), "boxAtSlot(54) is -1");
            }
        }
    }

    static void digitPickerMap(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            SudokuRound r = gen(size, br, bc, 3);
            boolean[] used = new boolean[54];
            for (int d = 1; d <= size; d++) {
                int slot = SudokuBoxView.slotOfDigit(d);
                ck.that(slot >= 0 && slot < 54, "slotOfDigit in range, digit " + d);
                ck.eq(d, SudokuBoxView.digitAtSlot(r, slot), "digitAtSlot inverts slotOfDigit at " + d);
                ck.eq((d - 1) / 3, slot / SudokuBoxView.DISPLAY_WIDTH, "digit display row is rows of three");
                ck.eq((d - 1) % 3, slot % SudokuBoxView.DISPLAY_WIDTH, "digit display col is rows of three");
                ck.that(!used[slot], "no two digits share a slot, digit " + d);
                used[slot] = true;
            }
            for (int slot = 0; slot < 54; slot++) {
                int d = SudokuBoxView.digitAtSlot(r, slot);
                if (d < 0) {
                    ck.that(d == -1, "digitAtSlot -1 off the digits, slot " + slot);
                } else {
                    ck.eq(slot, SudokuBoxView.slotOfDigit(d), "digitAtSlot slot round-trips, slot " + slot);
                }
            }
            ck.eq(-1, SudokuBoxView.digitAtSlot(r, -1), "digitAtSlot(-1) is -1");
            ck.eq(-1, SudokuBoxView.digitAtSlot(r, 54), "digitAtSlot(54) is -1");
        }
    }

    // ------------------------------------------------------- display shape gate

    /**
     * A 54-slot stream filled per the given per-cell slot function; '.'
     * elsewhere. A cell whose slot falls outside the 54-slot inventory
     * is absent from the stream (the box view never produces one —
     * proven in boxViewSlotMap; the legacy 9-wide map does for the
     * bottom boxes of a 9x9, whose cells live on the Java board's
     * second inventory page instead).
     */
    static char[] stream(SudokuRound r, int box, java.util.function.IntUnaryOperator slotOf, char code0) {
        char[] s = new char[54];
        Arrays.fill(s, '.');
        int i = 0;
        for (int idx : SudokuBoxView.boxCells(r, box)) {
            int slot = slotOf.applyAsInt(idx);
            if (slot >= 0 && slot < 54) s[slot] = (char) (code0 + i);
            i++;
        }
        return s;
    }

    /**
     * The square holds at phone width 5: the br*bc codes sit in local
     * row-major order at display rows 0..br-1, cols 0..bc-1, and no code
     * appears anywhere else in the stream.
     */
    static boolean rendersSquare(char[] stream, int br, int bc, char code0) {
        int seen = 0;
        for (int lr = 0; lr < br; lr++)
            for (int lc = 0; lc < bc; lc++) {
                if (stream[lr * SudokuBoxView.DISPLAY_WIDTH + lc] != (char) (code0 + seen)) return false;
                seen++;
            }
        for (int slot = 0; slot < 54; slot++) {
            int lr = slot / SudokuBoxView.DISPLAY_WIDTH, lc = slot % SudokuBoxView.DISPLAY_WIDTH;
            if (!(lr < br && lc < bc) && stream[slot] != '.') return false;
        }
        return seen == br * bc;
    }

    static void displayShapeGate(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            for (int seed = 1; seed <= 12; seed++) {
                SudokuRound r = gen(size, br, bc, seed * 7919L);
                for (int b = 0; b < SudokuBoxView.boxCount(r); b++) {
                    // Box view: the square holds.
                    char[] boxStream = stream(r, b, idx -> SudokuBoxView.slotOfCell(r, idx), 'A');
                    ck.that(rendersSquare(boxStream, br, bc, 'A'),
                            "box " + b + " renders as a true square at wrap 5 (" + size + " seed " + seed + ")");
                    // Negative control: the SAME cells on the 0.3.1-style
                    // Java map (global row*9 + col) do NOT hold the square
                    // (bottom-box cells fall off the 54-slot page entirely,
                    // which only makes the legacy layout worse, never better).
                    char[] legacyStream = stream(r, b, idx -> (idx / size) * 9 + (idx % size), 'A');
                    ck.that(!rendersSquare(legacyStream, br, bc, 'A'),
                            "negative control: box " + b + " on the legacy 9-wide map breaks at wrap 5 (" + size + ")");
                    // Picker: the box entries form their own rectangle.
                    char[] pickerStream = new char[54];
                    Arrays.fill(pickerStream, '.');
                    for (int b2 = 0; b2 < SudokuBoxView.boxCount(r); b2++)
                        pickerStream[SudokuBoxView.slotOfBox(r, b2)] = (char) ('a' + b2);
                    ck.that(rendersSquare(pickerStream, SudokuBoxView.boxRowsCount(r),
                                    SudokuBoxView.boxesPerRow(r), 'a'),
                            "picker renders its box rectangle at wrap 5 (" + size + " seed " + seed + ")");
                }
                // Digit picker: digits in rows of three on the stride.
                char[] digits = new char[54];
                Arrays.fill(digits, '.');
                for (int d = 1; d <= size; d++) digits[SudokuBoxView.slotOfDigit(d)] = (char) ('0' + d);
                int digitRows = (size + 2) / 3;
                boolean digitsOk = true;
                for (int d = 1; d <= size; d++) {
                    int slot = ((d - 1) / 3) * SudokuBoxView.DISPLAY_WIDTH + ((d - 1) % 3);
                    if (digits[slot] != (char) ('0' + d)) digitsOk = false;
                }
                for (int slot = 0; slot < 54; slot++) {
                    int lr = slot / SudokuBoxView.DISPLAY_WIDTH, lc = slot % SudokuBoxView.DISPLAY_WIDTH;
                    boolean isDigitPos = lr < digitRows && lc < 3 && (lr * 3 + lc + 1) <= size;
                    if (!isDigitPos && digits[slot] != '.') digitsOk = false;
                }
                ck.that(digitsOk, "digit picker renders rows of three at wrap 5 (" + size + " seed " + seed + ")");
            }
        }
    }

    // ------------------------------------------------------------ play entry

    static void digitPlacement(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            SudokuRound src = gen(size, br, bc, 11);
            int[] sol = solutionOf(src);
            // A board with one empty, one given, and the rest solved.
            int[] board = sol.clone();
            boolean[] given = new boolean[size * size];
            int emptyIdx = -1, givenIdx = -1;
            for (int i = 0; i < board.length && (emptyIdx < 0 || givenIdx < 0); i++) {
                if (emptyIdx < 0) { emptyIdx = i; board[i] = 0; }
                else if (givenIdx < 0) { givenIdx = i; given[i] = true; }
            }
            SudokuRound r = craftedFrom(src, board, given);
            // Every digit enters through the picker mapping and sticks.
            for (int d = 1; d <= size; d++) {
                int slot = SudokuBoxView.slotOfDigit(d);
                ck.eq(d, SudokuBoxView.digitAtSlot(r, slot), "picker digit " + d + " maps back");
                ck.that(r.place(emptyIdx, d) == SudokuRound.PlaceResult.PLACED,
                        "place digit " + d + " into the empty square");
                ck.eq(d, r.valueAt(emptyIdx), "square holds " + d + " after placement");
                ck.that(r.erase(emptyIdx), "erase clears the square");
                ck.eq(0, r.valueAt(emptyIdx), "square is empty after erase");
            }
            // A given square is locked; out-of-range input is refused.
            ck.that(r.place(givenIdx, 1) == SudokuRound.PlaceResult.GIVEN_LOCKED,
                    "given square refuses placement");
            ck.that(r.place(emptyIdx, 0) == SudokuRound.PlaceResult.OUT_OF_RANGE, "digit 0 refused");
            ck.that(r.place(emptyIdx, size + 1) == SudokuRound.PlaceResult.OUT_OF_RANGE,
                    "digit " + (size + 1) + " refused");
            ck.that(r.place(-1, 1) == SudokuRound.PlaceResult.OUT_OF_RANGE, "index -1 refused");
            ck.that(r.place(size * size, 1) == SudokuRound.PlaceResult.OUT_OF_RANGE, "index past end refused");
            ck.that(!r.erase(givenIdx), "erase refuses a given square");
            ck.that(!r.erase(emptyIdx), "erase on an empty square is a no-op");
            // The GUI flow's target: placing the solution digits in the
            // empty squares solves the board through the same calls.
            SudokuRound solve = craftedFrom(src, board, given);
            for (int i = 0; i < board.length; i++) {
                if (!given[i] && solve.valueAt(i) == 0) {
                    ck.that(solve.place(i, sol[i]) == SudokuRound.PlaceResult.PLACED,
                            "solution digit places at " + i);
                }
            }
            ck.that(solve.isSolved(), "solution digits solve the board (" + size + ")");
        }
    }

    // ---------------------------------------------------------- box completion

    static void boxFinishedStates(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            SudokuRound src = gen(size, br, bc, 21);
            int[] sol = solutionOf(src);
            boolean[] noGiven = new boolean[size * size];
            // Board == solution: every box finished, filled counts full.
            SudokuRound full = craftedFrom(src, sol.clone(), noGiven);
            for (int b = 0; b < SudokuBoxView.boxCount(full); b++) {
                ck.that(SudokuBoxView.boxFinished(full, b), "box " + b + " finished when solved (" + size + ")");
                ck.eq(br * bc, SudokuBoxView.boxFilled(full, b), "box " + b + " filled count full");
            }
            // One zeroed cell breaks only its own box; one wrong value
            // breaks only its own box.
            for (int b = 0; b < SudokuBoxView.boxCount(full); b++) {
                int[] cells = SudokuBoxView.boxCells(full, b);
                int[] board0 = sol.clone();
                board0[cells[0]] = 0;
                SudokuRound zeroed = craftedFrom(src, board0, noGiven);
                ck.that(!SudokuBoxView.boxFinished(zeroed, b), "box " + b + " not finished with a blank");
                ck.eq(br * bc - 1, SudokuBoxView.boxFilled(zeroed, b), "box " + b + " filled count minus one");
                int[] boardW = sol.clone();
                boardW[cells[0]] = sol[cells[0]] % size + 1; // a different digit, still 1..size
                SudokuRound wrong = craftedFrom(src, boardW, noGiven);
                ck.that(!SudokuBoxView.boxFinished(wrong, b), "box " + b + " not finished with a wrong digit");
                for (int b2 = 0; b2 < SudokuBoxView.boxCount(full); b2++) {
                    if (b2 == b) continue;
                    ck.that(SudokuBoxView.boxFinished(zeroed, b2), "box " + b2 + " stays finished (zeroed)");
                    ck.that(SudokuBoxView.boxFinished(wrong, b2), "box " + b2 + " stays finished (wrong)");
                }
            }
            // Only one box filled: exactly that box is finished.
            for (int b = 0; b < SudokuBoxView.boxCount(full); b++) {
                int[] board = new int[size * size];
                for (int idx : SudokuBoxView.boxCells(full, b)) board[idx] = sol[idx];
                SudokuRound one = craftedFrom(src, board, noGiven);
                for (int b2 = 0; b2 < SudokuBoxView.boxCount(full); b2++) {
                    ck.that(SudokuBoxView.boxFinished(one, b2) == (b2 == b),
                            "only box " + b + " finished when only it is filled");
                }
            }
            // A fresh generated round: finished iff every box cell matches
            // the solution — checked directly against the board state.
            SudokuRound fresh = gen(size, br, bc, 31);
            for (int b = 0; b < SudokuBoxView.boxCount(fresh); b++) {
                boolean expect = true;
                for (int idx : SudokuBoxView.boxCells(fresh, b)) {
                    if (fresh.valueAt(idx) == 0 || fresh.valueAt(idx) != fresh.solutionAt(idx)) expect = false;
                }
                ck.that(SudokuBoxView.boxFinished(fresh, b) == expect,
                        "boxFinished matches the board state for fresh box " + b);
            }
        }
    }

    // ------------------------------------------------------------ context lines

    /** The grid's own row line for row r, extracted from SudokuTextGrid.lines. */
    static String gridRowLine(SudokuRound r, int row) {
        List<String> lines = SudokuTextGrid.lines(r);
        int rowSeen = -1;
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("  +")) continue; // border
            rowSeen++;
            if (rowSeen == row) return line;
        }
        throw new IllegalStateException("no grid row " + row);
    }

    static void contextLines(Check ck) {
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            for (int seed = 1; seed <= 6; seed++) {
                SudokuRound r = gen(size, br, bc, seed * 61L);
                // Play a few moves so rows/cols hold placed digits too.
                for (int i = 0; i < r.size() * r.size() && r.filledCount() < r.givensCount() + 4; i++) {
                    if (!r.isGiven(i) && r.valueAt(i) == 0) r.place(i, r.solutionAt(i));
                }
                for (int idx = 0; idx < size * size; idx++) {
                    int row = idx / size, col = idx % size;
                    // The row line is byte-identical to the grid's row line.
                    ck.that(SudokuBoxView.rowLine(r, idx).equals(gridRowLine(r, row)),
                            "rowLine == grid row line at idx " + idx + " (" + size + ")");
                    // The column line, restated independently from the board.
                    StringBuilder expect = new StringBuilder("Col ").append(col + 1).append(": ");
                    for (int rr = 0; rr < size; rr++) {
                        if (rr > 0) expect.append(rr % br == 0 ? " | " : " ");
                        expect.append(fmtCell(r, rr * size + col));
                    }
                    ck.that(SudokuBoxView.colLine(r, idx).equals(expect.toString()),
                            "colLine == the board's column at idx " + idx + " (" + size + ")");
                    // The column line holds exactly the column's values, top to bottom.
                    String colLine = SudokuBoxView.colLine(r, idx);
                    for (int rr = 0; rr < size; rr++) {
                        ck.that(colLine.contains(fmtCell(r, rr * size + col).trim()),
                                "colLine holds row " + rr + " value");
                    }
                }
            }
        }
    }

    // ------------------------------------------------------ cross-platform resume

    static void crossPlatformResume(Check ck) {
        List<String> expectKeys = List.of("tier", "size", "boxRows", "boxCols",
                "solution", "given", "board", "picture", "checks", "started", "paid");
        for (int s = 0; s < SHAPES.length; s++) {
            int size = SHAPES[s][0], br = SHAPES[s][1], bc = SHAPES[s][2];
            for (int seed = 1; seed <= 5; seed++) {
                SudokuRound r = gen(size, br, bc, seed * 103L);
                // Play on "one platform": correct placements + an erased mistake.
                int played = 0;
                for (int i = 0; i < size * size && played < 3; i++) {
                    if (!r.isGiven(i) && r.valueAt(i) == 0) {
                        r.place(i, r.solutionAt(i));
                        played++;
                    }
                }
                for (int i = 0; i < size * size; i++) {
                    if (!r.isGiven(i) && r.valueAt(i) == 0) {
                        r.place(i, r.solutionAt(i) % size + 1);
                        r.erase(i);
                        break;
                    }
                }
                Map<String, Object> map = r.toMap();
                // The save carries NO presentation key: the format is
                // byte-for-byte the 0.3.2 key set, in order.
                ck.that(List.copyOf(map.keySet()).equals(expectKeys),
                        "save key set is the 0.3.2 set exactly (" + size + " seed " + seed + ")");
                SudokuRound back = SudokuRound.fromMap(map);
                ck.that(back.tierId().equals(r.tierId()), "resume keeps tier");
                ck.eq(r.size(), back.size(), "resume keeps size");
                ck.eq(r.boxRows(), back.boxRows(), "resume keeps boxRows");
                ck.eq(r.boxCols(), back.boxCols(), "resume keeps boxCols");
                ck.eq(r.checksUsed(), back.checksUsed(), "resume keeps checks");
                ck.eq(r.startedMillis(), back.startedMillis(), "resume keeps started");
                ck.that(back.picture() == r.picture(), "resume keeps picture");
                for (int i = 0; i < size * size; i++) {
                    ck.eq(r.solutionAt(i), back.solutionAt(i), "resume keeps solution at " + i);
                    ck.eq(r.valueAt(i), back.valueAt(i), "resume keeps board at " + i);
                    ck.that(r.isGiven(i) == back.isGiven(i), "resume keeps given at " + i);
                }
                // The presentation is a pure function of round state: the
                // reloaded round maps identically on every surface.
                for (int i = 0; i < size * size; i++) {
                    ck.eq(SudokuBoxView.boxOf(r, i), SudokuBoxView.boxOf(back, i),
                            "resume keeps boxOf at " + i);
                    ck.eq(SudokuBoxView.slotOfCell(r, i), SudokuBoxView.slotOfCell(back, i),
                            "resume keeps slotOfCell at " + i);
                    ck.that(SudokuBoxView.rowLine(r, i).equals(SudokuBoxView.rowLine(back, i)),
                            "resume keeps rowLine at " + i);
                    ck.that(SudokuBoxView.colLine(r, i).equals(SudokuBoxView.colLine(back, i)),
                            "resume keeps colLine at " + i);
                }
                for (int b = 0; b < SudokuBoxView.boxCount(r); b++) {
                    ck.eq(SudokuBoxView.slotOfBox(r, b), SudokuBoxView.slotOfBox(back, b),
                            "resume keeps slotOfBox at box " + b);
                    ck.that(SudokuBoxView.boxFinished(r, b) == SudokuBoxView.boxFinished(back, b),
                            "resume keeps boxFinished at box " + b);
                }
                // And it plays identically to completion on the other side.
                for (int i = 0; i < size * size; i++) {
                    if (!back.isGiven(i) && back.valueAt(i) == 0) {
                        ck.that(back.place(i, back.solutionAt(i)) == SudokuRound.PlaceResult.PLACED,
                                "resumed round places at " + i);
                    }
                }
                ck.that(back.isSolved(), "resumed round solves (" + size + " seed " + seed + ")");
            }
        }
    }

    // ------------------------------------------------------- platform decision

    static void viewerPlatformDecision(Check ck) {
        ck.that(SudokuBoxView.useBoxView(WsGeometry.BEDROCK), "BEDROCK viewer gets the box view");
        ck.that(!SudokuBoxView.useBoxView(WsGeometry.JAVA), "JAVA viewer gets the full-board view");
        ck.that(!SudokuBoxView.useBoxView(WsGeometry.parse(null)),
                "unknown platform (null) gets the full-board view");
        ck.that(!SudokuBoxView.useBoxView(WsGeometry.parse("")), "unknown platform (empty) gets the full-board view");
        ck.that(!SudokuBoxView.useBoxView(WsGeometry.parse("junk")), "unknown platform (junk) gets the full-board view");
        ck.that(SudokuBoxView.useBoxView(WsGeometry.parse("bedrock")), "parse(bedrock) gets the box view");
        ck.that(SudokuBoxView.useBoxView(WsGeometry.parse(" BEDROCK ")), "parse trims and folds case");
    }
}
