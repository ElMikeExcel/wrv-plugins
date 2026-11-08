package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Sudoku property tests (SPEC §9.1 pattern, the Sudoku contract of §4):
 * 500 generator seeds x every tier in the SHIPPED config. For each deal:
 * the givens fall inside the config band; an INDEPENDENT solver (written
 * here from scratch with set-based candidate logic, sharing no code with
 * the production bitmask solver) proves the puzzle has EXACTLY ONE
 * solution and that it equals the round's stored solution; and solving
 * the round by placing the solution completes it while one wrong value
 * does not.
 */
public final class SudokuPropertyTest {

    record TierBand(String id, int size, int br, int bc, int min, int max) {}

    public static void main(String[] args) throws Exception {
        Check ck = new Check("SudokuPropertyTest");
        List<TierBand> bands = readBands(Path.of(args.length > 0 ? args[0] : "config.yml"));
        ck.eq(5, bands.size(), "config carries exactly the five Sudoku tiers");
        String[] expected = {"kid4", "six", "easy", "medium", "hard"};
        for (int i = 0; i < expected.length; i++) {
            ck.that(bands.get(i).id().equals(expected[i]), "tier order: " + expected[i] + " present");
        }
        final int seeds = 500;
        for (TierBand t : bands) {
            long maxGenMs = 0;
            for (int s = 0; s < seeds; s++) {
                long seed = 1_000_003L * s + t.id().hashCode();
                long t0 = System.nanoTime();
                SudokuRound r = SudokuRound.generate(t.id(), t.size(), t.br(), t.bc(),
                        t.min(), t.max(), t.id().equals("kid4"), new Random(seed), 5_000L + s);
                maxGenMs = Math.max(maxGenMs, (System.nanoTime() - t0) / 1_000_000);
                String tag = t.id() + " seed " + s;
                ck.that(r.givensCount() >= t.min() && r.givensCount() <= t.max(),
                        tag + ": givens " + r.givensCount() + " in [" + t.min() + "," + t.max() + "]");
                int n = t.size();
                int[] puzzle = new int[n * n];
                boolean givensMatch = true;
                for (int i = 0; i < n * n; i++) {
                    puzzle[i] = r.valueAt(i);
                    if (r.isGiven(i) && puzzle[i] != r.solutionAt(i)) givensMatch = false;
                }
                ck.that(givensMatch, tag + ": givens match the stored solution");
                CountingSolver solver = new CountingSolver(n, t.br(), t.bc());
                int count = solver.count((int[]) puzzle.clone(), 2);
                ck.eq(1, count, tag + ": INDEPENDENT solver proves a unique solution");
                int[] solved = solver.solveOne((int[]) puzzle.clone());
                boolean sameSolution = solved != null;
                if (sameSolution) {
                    for (int i = 0; i < n * n; i++) if (solved[i] != r.solutionAt(i)) sameSolution = false;
                }
                ck.that(sameSolution, tag + ": the unique solution is the stored one");
                // Solve-by-placement completes; one wrong value does not.
                SudokuRound played = SudokuRound.fromMap(r.toMap());
                int firstEmpty = -1;
                for (int i = 0; i < n * n; i++) {
                    if (!played.isGiven(i)) {
                        if (firstEmpty < 0) firstEmpty = i;
                        played.place(i, played.solutionAt(i));
                    }
                }
                ck.that(played.isSolved(), tag + ": placing the solution completes the round");
                SudokuRound spoiled = SudokuRound.fromMap(r.toMap());
                for (int i = 0; i < n * n; i++) {
                    if (!spoiled.isGiven(i)) spoiled.place(i, spoiled.solutionAt(i));
                }
                spoiled.place(firstEmpty, spoiled.solutionAt(firstEmpty) % n + 1);
                ck.that(!spoiled.isSolved(), tag + ": one wrong value blocks completion");
            }
            System.out.println("    " + t.id() + ": 500 seeds OK, slowest generation " + maxGenMs + " ms");
        }
        ck.finish();
    }

    /** Sudoku tiers read straight out of the shipped config.yml (text scrape, no YAML lib). */
    static List<TierBand> readBands(Path config) throws Exception {
        List<String> lines = Files.readAllLines(config);
        List<TierBand> out = new ArrayList<>();
        boolean inSudoku = false, inTiers = false;
        String cur = null;
        int size = 0, br = 0, bc = 0, min = 0, max = 0;
        for (String raw : lines) {
            String line = raw.strip();
            int indent = raw.length() - raw.stripLeading().length();
            if (indent == 2 && line.startsWith("sudoku:")) inSudoku = true;
            else if (indent == 2 && inSudoku && !line.startsWith("sudoku:")) inSudoku = false;
            if (!inSudoku) continue;
            if (indent == 4 && line.startsWith("tiers:")) { inTiers = true; continue; }
            if (!inTiers) continue;
            if (indent == 6 && line.endsWith(":")) {
                if (cur != null) out.add(new TierBand(cur, size, br, bc, min, max));
                cur = line.substring(0, line.length() - 1);
                size = br = bc = min = max = 0;
                continue;
            }
            if (indent == 4) { inTiers = false; continue; }
            if (cur == null) continue;
            if (line.startsWith("size:")) size = num(line);
            else if (line.startsWith("box-rows:")) br = num(line);
            else if (line.startsWith("box-cols:")) bc = num(line);
            else if (line.startsWith("givens-min:")) min = num(line);
            else if (line.startsWith("givens-max:")) max = num(line);
        }
        if (cur != null) out.add(new TierBand(cur, size, br, bc, min, max));
        return out;
    }

    static int num(String line) { return Integer.parseInt(line.split(":")[1].strip()); }

    /**
     * Independent solver: plain set-based candidate computation and
     * recursion over the most-constrained empty cell. Deliberately shares
     * no code with the production SudokuRound bitmask solver, so a bug in
     * one cannot hide in the other.
     */
    static final class CountingSolver {
        final int n, br, bc;

        CountingSolver(int n, int br, int bc) { this.n = n; this.br = br; this.bc = bc; }

        Set<Integer> candidates(int[] grid, int idx) {
            Set<Integer> used = new HashSet<>();
            int row = idx / n, col = idx % n;
            for (int j = 0; j < n; j++) {
                used.add(grid[row * n + j]);
                used.add(grid[j * n + col]);
            }
            int r0 = (row / br) * br, c0 = (col / bc) * bc;
            for (int rr = r0; rr < r0 + br; rr++) {
                for (int cc = c0; cc < c0 + bc; cc++) used.add(grid[rr * n + cc]);
            }
            Set<Integer> out = new HashSet<>();
            for (int v = 1; v <= n; v++) if (!used.contains(v)) out.add(v);
            return out;
        }

        int pickCell(int[] grid, int[] outCandsHolder) {
            int best = -1, bestSize = Integer.MAX_VALUE;
            Set<Integer> bestSet = null;
            for (int i = 0; i < grid.length; i++) {
                if (grid[i] != 0) continue;
                Set<Integer> c = candidates(grid, i);
                if (c.size() < bestSize) { bestSize = c.size(); best = i; bestSet = c; if (c.isEmpty()) break; }
            }
            if (best >= 0) {
                int k = 0;
                for (int v : bestSet) outCandsHolder[k++] = v;
                outCandsHolder[bestSize] = Integer.MIN_VALUE; // length marker via count below
            }
            return best;
        }

        int count(int[] grid, int limit) {
            int[] holder = new int[n + 1];
            int idx = pickCell(grid, holder);
            if (idx < 0) return 1;
            Set<Integer> cands = candidates(grid, idx);
            if (cands.isEmpty()) return 0;
            int total = 0;
            for (int v : cands) {
                grid[idx] = v;
                total += count(grid, limit - total);
                grid[idx] = 0;
                if (total >= limit) return total;
            }
            return total;
        }

        int[] solveOne(int[] grid) {
            int idx = pickCell(grid, new int[n + 1]);
            if (idx < 0) return grid;
            for (int v : candidates(grid, idx)) {
                grid[idx] = v;
                if (solveOne(grid) != null) return grid;
                grid[idx] = 0;
            }
            return null;
        }
    }
}
