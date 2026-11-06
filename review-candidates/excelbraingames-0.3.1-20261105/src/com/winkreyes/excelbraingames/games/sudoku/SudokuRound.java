package com.winkreyes.excelbraingames.games.sudoku;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * One Sudoku round: generator (randomized backtracking fill + uniqueness-
 * preserving dig), solver, placement/check logic, save/resume (SPEC §4).
 * Pure logic — no Bukkit.
 *
 * Sizes: 4x4 (boxes 2x2), 6x6 (boxes 2 rows x 3 cols), 9x9 (boxes 3x3).
 * Every generated puzzle has EXACTLY ONE solution, proven by the digger:
 * a cell is only removed while a solution counter still finds exactly one
 * completion. The counter is a bitmask MRV backtracker (fast enough to dig
 * a 9x9 to the mid-20s givens in tens of milliseconds).
 *
 * Check button (SPEC §4.4): checkCells() lists the player-placed cells that
 * disagree with the solution. Adult tiers pay for checks out of the payout
 * (afterCheckDeduction); the kid tier's checks are free and unlimited —
 * the round itself just counts uses, GameService applies the money rules.
 */
public final class SudokuRound {

    public enum PlaceResult { PLACED, GIVEN_LOCKED, OUT_OF_RANGE, IGNORED }

    private final String tierId;
    private final int size;      // 4, 6, or 9
    private final int boxRows;
    private final int boxCols;
    private final int[] solution; // size*size, values 1..size
    private final boolean[] given;
    private final int[] board;    // 0 = empty
    private final boolean picture;
    private int checksUsed;
    private final long startedMillis;
    private boolean paid;

    private SudokuRound(String tierId, int size, int boxRows, int boxCols,
                        int[] solution, boolean[] given, int[] board, boolean picture,
                        int checksUsed, long startedMillis, boolean paid) {
        this.tierId = tierId; this.size = size;
        this.boxRows = boxRows; this.boxCols = boxCols;
        this.solution = solution; this.given = given; this.board = board;
        this.picture = picture; this.checksUsed = checksUsed;
        this.startedMillis = startedMillis; this.paid = paid;
    }

    // ------------------------------------------------------------ generation

    /**
     * Deal a fresh puzzle. A target givens count is drawn inside
     * [givensMin, givensMax]; the digger removes cells (singly, then in
     * 180-degree pairs when singles stall) while uniqueness survives. If a
     * dig stalls above givensMax, the whole deal retries with a fresh grid
     * (bounded attempts) and the best (fewest-givens) result is kept.
     */
    public static SudokuRound generate(String tierId, int size, int boxRows, int boxCols,
                                       int givensMin, int givensMax, boolean picture,
                                       Random rng, long startedMillis) {
        SudokuRound best = null;
        for (int attempt = 0; attempt < 8; attempt++) {
            int target = givensMin + rng.nextInt(Math.max(1, givensMax - givensMin + 1));
            int[] solution = fullGrid(size, boxRows, boxCols, rng);
            int[] puzzle = solution.clone();
            int givens = dig(puzzle, size, boxRows, boxCols, target, rng);
            boolean[] givenMask = new boolean[puzzle.length];
            for (int i = 0; i < puzzle.length; i++) givenMask[i] = puzzle[i] != 0;
            SudokuRound r = new SudokuRound(tierId, size, boxRows, boxCols, solution,
                    givenMask, puzzle, picture, 0, startedMillis, false);
            if (givens <= givensMax) return r;
            if (best == null || givens < best.givensCount()) best = r;
        }
        return best;
    }

    /** Randomized backtracking fill of a complete valid grid. */
    static int[] fullGrid(int size, int boxRows, int boxCols, Random rng) {
        int[] grid = new int[size * size];
        fill(grid, size, boxRows, boxCols, rng);
        return grid;
    }

    private static boolean fill(int[] g, int size, int br, int bc, Random rng) {
        int idx = -1;
        for (int i = 0; i < g.length; i++) if (g[i] == 0) { idx = i; break; }
        if (idx < 0) return true;
        List<Integer> values = new ArrayList<>();
        for (int v = 1; v <= size; v++) values.add(v);
        Collections.shuffle(values, rng);
        for (int v : values) {
            if (legal(g, size, br, bc, idx, v)) {
                g[idx] = v;
                if (fill(g, size, br, bc, rng)) return true;
                g[idx] = 0;
            }
        }
        return false;
    }

    private static boolean legal(int[] g, int size, int br, int bc, int idx, int v) {
        int r = idx / size, c = idx % size;
        for (int k = 0; k < size; k++) {
            if (g[r * size + k] == v || g[k * size + c] == v) return false;
        }
        int r0 = (r / br) * br, c0 = (c / bc) * bc;
        for (int i = 0; i < br; i++) for (int j = 0; j < bc; j++) {
            if (g[(r0 + i) * size + (c0 + j)] == v) return false;
        }
        return true;
    }

    /**
     * Remove cells down toward target givens while the puzzle keeps a
     * unique solution. Returns the givens count actually reached (== target
     * unless the dig stalls, which the caller treats per its band).
     */
    private static int dig(int[] puzzle, int size, int br, int bc, int target, Random rng) {
        int givens = size * size;
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < puzzle.length; i++) order.add(i);
        while (givens > target) {
            Collections.shuffle(order, rng);
            boolean removedAny = false;
            for (int idx : order) {
                if (givens <= target) break;
                if (puzzle[idx] == 0) continue;
                int saved = puzzle[idx];
                puzzle[idx] = 0;
                if (countSolutions(puzzle, size, br, bc, 2) == 1) {
                    givens--;
                    removedAny = true;
                } else {
                    puzzle[idx] = saved;
                }
            }
            if (givens <= target || removedAny) continue;
            // Singles stalled: try 180-degree rotational pairs together —
            // symmetric removal breaks uniqueness less often per cell.
            boolean pairRemoved = false;
            for (int idx : order) {
                if (givens <= target + 1) break;
                int mirror = puzzle.length - 1 - idx;
                if (mirror == idx || puzzle[idx] == 0 || puzzle[mirror] == 0) continue;
                int a = puzzle[idx], b = puzzle[mirror];
                puzzle[idx] = 0;
                puzzle[mirror] = 0;
                if (countSolutions(puzzle, size, br, bc, 2) == 1) {
                    givens -= 2;
                    pairRemoved = true;
                } else {
                    puzzle[idx] = a;
                    puzzle[mirror] = b;
                }
            }
            if (!pairRemoved) break;
        }
        return givens;
    }

    // ------------------------------------------------------------ solver

    /**
     * Count solutions of a (partial) grid, stopping at limit. Bitmask MRV
     * backtracking — the generator's uniqueness oracle. Also used by tests
     * as the production solver; the property suite cross-checks with an
     * independent implementation.
     */
    public static int countSolutions(int[] grid, int size, int boxRows, int boxCols, int limit) {
        int[] g = grid.clone();
        int[] rowMask = new int[size], colMask = new int[size], boxMask = new int[size];
        int boxesPerRow = size / boxCols;
        for (int i = 0; i < g.length; i++) {
            int v = g[i];
            if (v == 0) continue;
            if (v < 1 || v > size) return 0;
            int bit = 1 << (v - 1);
            int r = i / size, c = i % size, b = (r / boxRows) * boxesPerRow + (c / boxCols);
            if ((rowMask[r] & bit) != 0 || (colMask[c] & bit) != 0 || (boxMask[b] & bit) != 0) return 0;
            rowMask[r] |= bit; colMask[c] |= bit; boxMask[b] |= bit;
        }
        int full = (1 << size) - 1;
        return countRec(g, size, boxRows, boxCols, boxesPerRow, rowMask, colMask, boxMask, full, limit);
    }

    private static int countRec(int[] g, int size, int br, int bc, int bpr,
                                int[] rowMask, int[] colMask, int[] boxMask, int full, int limit) {
        // Pick the empty cell with the fewest candidates (MRV).
        int best = -1, bestMask = 0, bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < g.length; i++) {
            if (g[i] != 0) continue;
            int r = i / size, c = i % size, b = (r / br) * bpr + (c / bc);
            int mask = full & ~(rowMask[r] | colMask[c] | boxMask[b]);
            int n = Integer.bitCount(mask);
            if (n == 0) return 0;
            if (n < bestCount) { bestCount = n; best = i; bestMask = mask; if (n == 1) break; }
        }
        if (best < 0) return 1; // no empty cells: one completion
        int r = best / size, c = best % size, b = (r / br) * bpr + (c / bc);
        int total = 0;
        int mask = bestMask;
        while (mask != 0) {
            int bit = mask & -mask;
            mask &= mask - 1;
            g[best] = Integer.numberOfTrailingZeros(bit) + 1;
            rowMask[r] |= bit; colMask[c] |= bit; boxMask[b] |= bit;
            total += countRec(g, size, br, bc, bpr, rowMask, colMask, boxMask, full, limit - total);
            rowMask[r] &= ~bit; colMask[c] &= ~bit; boxMask[b] &= ~bit;
            g[best] = 0;
            if (total >= limit) return total;
        }
        return total;
    }

    /** Solve a puzzle in place; returns false if it has no solution. */
    public static boolean solve(int[] grid, int size, int boxRows, int boxCols) {
        return solveRec(grid, size, boxRows, boxCols);
    }

    private static boolean solveRec(int[] g, int size, int br, int bc) {
        int idx = -1;
        for (int i = 0; i < g.length; i++) if (g[i] == 0) { idx = i; break; }
        if (idx < 0) return true;
        for (int v = 1; v <= size; v++) {
            if (legal(g, size, br, bc, idx, v)) {
                g[idx] = v;
                if (solveRec(g, size, br, bc)) return true;
                g[idx] = 0;
            }
        }
        return false;
    }

    // ------------------------------------------------------------ play

    public PlaceResult place(int idx, int value) {
        if (idx < 0 || idx >= board.length || value < 1 || value > size) return PlaceResult.OUT_OF_RANGE;
        if (given[idx]) return PlaceResult.GIVEN_LOCKED;
        board[idx] = value;
        return PlaceResult.PLACED;
    }

    /** Erase a player-placed cell. Returns true if anything changed. */
    public boolean erase(int idx) {
        if (idx < 0 || idx >= board.length || given[idx] || board[idx] == 0) return false;
        board[idx] = 0;
        return true;
    }

    /** Player-placed cells whose value disagrees with the solution (the Check button's answer). */
    public List<Integer> wrongCells() {
        List<Integer> wrong = new ArrayList<>();
        for (int i = 0; i < board.length; i++) {
            if (!given[i] && board[i] != 0 && board[i] != solution[i]) wrong.add(i);
        }
        return wrong;
    }

    /**
     * Record one Check-button use. maxAllowed is the adult cap (config
     * sudoku.max-checks); the kid tier passes Integer.MAX_VALUE (free +
     * unlimited). Returns false when the cap is already reached.
     */
    public boolean useCheck(int maxAllowed) {
        if (checksUsed >= maxAllowed) return false;
        checksUsed++;
        return true;
    }

    /** Payout after check deductions: percent of the tier payout per use, floored at zero. */
    public static long afterCheckDeduction(long payoutCents, int checksUsed, int percent) {
        long perCheck = payoutCents * percent / 100;
        return Math.max(0, payoutCents - perCheck * checksUsed);
    }

    /** Complete = every cell filled AND the board equals the solution (SPEC §4.5). */
    public boolean isSolved() {
        for (int i = 0; i < board.length; i++) {
            if (board[i] != solution[i]) return false;
        }
        return true;
    }

    public int givensCount() {
        int n = 0;
        for (boolean g : given) if (g) n++;
        return n;
    }

    public int filledCount() {
        int n = 0;
        for (int v : board) if (v != 0) n++;
        return n;
    }

    public String tierId() { return tierId; }
    public int size() { return size; }
    public int boxRows() { return boxRows; }
    public int boxCols() { return boxCols; }
    public boolean picture() { return picture; }
    public int checksUsed() { return checksUsed; }
    public long startedMillis() { return startedMillis; }
    public boolean paid() { return paid; }
    public void markPaid() { this.paid = true; }
    public boolean isGiven(int idx) { return given[idx]; }
    public int valueAt(int idx) { return board[idx]; }
    public int solutionAt(int idx) { return solution[idx]; }

    // ---------------------------------------------------------- save / resume

    public Map<String, Object> toMap() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tier", tierId);
        m.put("size", size);
        m.put("boxRows", boxRows);
        m.put("boxCols", boxCols);
        m.put("solution", digits(solution));
        StringBuilder giv = new StringBuilder();
        for (boolean g : given) giv.append(g ? '1' : '0');
        m.put("given", giv.toString());
        m.put("board", digits(board));
        m.put("picture", picture);
        m.put("checks", checksUsed);
        m.put("started", startedMillis);
        m.put("paid", paid);
        return m;
    }

    public static SudokuRound fromMap(Map<String, Object> m) {
        String tier = (String) m.get("tier");
        int size = ((Number) m.get("size")).intValue();
        int br = ((Number) m.get("boxRows")).intValue();
        int bc = ((Number) m.get("boxCols")).intValue();
        int[] solution = parseDigits((String) m.get("solution"), size * size);
        String giv = (String) m.getOrDefault("given", "");
        boolean[] given = new boolean[size * size];
        for (int i = 0; i < given.length && i < giv.length(); i++) given[i] = giv.charAt(i) == '1';
        int[] board = parseDigits((String) m.get("board"), size * size);
        SudokuRound r = new SudokuRound(tier, size, br, bc, solution, given, board,
                Boolean.TRUE.equals(m.get("picture")),
                ((Number) m.getOrDefault("checks", 0)).intValue(),
                ((Number) m.getOrDefault("started", 0L)).longValue(),
                Boolean.TRUE.equals(m.get("paid")));
        return r;
    }

    private static String digits(int[] a) {
        StringBuilder sb = new StringBuilder(a.length);
        for (int v : a) sb.append((char) ('0' + v));
        return sb.toString();
    }

    private static int[] parseDigits(String s, int len) {
        int[] out = new int[len];
        if (s != null) {
            for (int i = 0; i < len && i < s.length(); i++) out[i] = s.charAt(i) - '0';
        }
        return out;
    }
}
