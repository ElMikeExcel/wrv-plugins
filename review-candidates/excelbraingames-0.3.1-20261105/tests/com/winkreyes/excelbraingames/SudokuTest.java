package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Payouts;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;

import java.util.List;
import java.util.Random;

/**
 * Sudoku unit/logic tests (SPEC §4): generator shapes at unit scale,
 * placement and given-lock rules, the Check button (wrong-cell detection,
 * adult cap, deduction math), the kid-tier half rule on Sudoku values,
 * completion detection, solver sanity, and save/resume serialization.
 * The 500-seed uniqueness proof lives in SudokuPropertyTest.
 */
public final class SudokuTest {

    public static void main(String[] args) {
        Check ck = new Check("SudokuTest");
        generatedShapes(ck);
        placementRules(ck);
        checkButton(ck);
        deductionMath(ck);
        completionDetection(ck);
        solverSanity(ck);
        serialization(ck);
        ck.finish();
    }

    static SudokuRound fresh(String tier, int size, int br, int bc, int min, int max, boolean pic, long seed) {
        return SudokuRound.generate(tier, size, br, bc, min, max, pic, new Random(seed), 1_000L);
    }

    /** Independent validity check of a completed grid (rows/cols/boxes are permutations). */
    static boolean validComplete(SudokuRound r) {
        int n = r.size();
        for (int i = 0; i < n; i++) {
            boolean[] row = new boolean[n + 1], col = new boolean[n + 1], box = new boolean[n + 1];
            for (int j = 0; j < n; j++) {
                int rv = r.solutionAt(i * n + j);
                int cv = r.solutionAt(j * n + i);
                int br = (i / r.boxRows()) * r.boxRows() + j / r.boxCols();
                int bc = (i % r.boxRows()) * r.boxCols() + j % r.boxCols();
                int bv = r.solutionAt(br * n + bc);
                if (rv < 1 || rv > n || row[rv]) return false;
                if (cv < 1 || cv > n || col[cv]) return false;
                if (bv < 1 || bv > n || box[bv]) return false;
                row[rv] = col[cv] = box[bv] = true;
            }
        }
        return true;
    }

    static void generatedShapes(Check ck) {
        int[][] shapes = {{4, 2, 2, 8, 10}, {6, 2, 3, 18, 22}, {9, 3, 3, 38, 44}, {9, 3, 3, 24, 29}};
        String[] tiers = {"kid4", "six", "easy", "hard"};
        for (int s = 0; s < shapes.length; s++) {
            for (long seed = 0; seed < 25; seed++) {
                SudokuRound r = fresh(tiers[s], shapes[s][0], shapes[s][1], shapes[s][2],
                        shapes[s][3], shapes[s][4], false, seed * 7919 + s);
                String tag = tiers[s] + " seed " + seed;
                ck.that(validComplete(r), tag + ": solution is a valid complete grid");
                ck.that(r.givensCount() >= shapes[s][3] && r.givensCount() <= shapes[s][4],
                        tag + ": givens " + r.givensCount() + " in band");
                boolean givensMatch = true, boardIsGivens = true;
                for (int i = 0; i < r.size() * r.size(); i++) {
                    if (r.isGiven(i) && r.valueAt(i) != r.solutionAt(i)) givensMatch = false;
                    if (!r.isGiven(i) && r.valueAt(i) != 0) boardIsGivens = false;
                }
                ck.that(givensMatch, tag + ": givens match the solution");
                ck.that(boardIsGivens, tag + ": board starts as exactly the givens");
                ck.eq(1, SudokuRound.countSolutions(boardOf(r), r.size(), r.boxRows(), r.boxCols(), 2),
                        tag + ": unique solution (production counter)");
            }
        }
    }

    static int[] boardOf(SudokuRound r) {
        int[] b = new int[r.size() * r.size()];
        for (int i = 0; i < b.length; i++) b[i] = r.valueAt(i);
        return b;
    }

    static int firstEmpty(SudokuRound r) {
        for (int i = 0; i < r.size() * r.size(); i++) if (!r.isGiven(i)) return i;
        return -1;
    }

    static int firstGiven(SudokuRound r) {
        for (int i = 0; i < r.size() * r.size(); i++) if (r.isGiven(i)) return i;
        return -1;
    }

    static void placementRules(Check ck) {
        SudokuRound r = fresh("six", 6, 2, 3, 18, 22, false, 42L);
        int given = firstGiven(r), empty = firstEmpty(r);
        ck.that(r.place(given, 1) == SudokuRound.PlaceResult.GIVEN_LOCKED, "place: given cell is locked");
        ck.eq(r.solutionAt(given), r.valueAt(given), "place: given value unchanged");
        ck.that(!r.erase(given), "erase: given cell refuses");
        ck.that(r.place(empty, r.solutionAt(empty)) == SudokuRound.PlaceResult.PLACED, "place: empty cell accepts");
        ck.eq(r.solutionAt(empty), r.valueAt(empty), "place: value stored");
        ck.that(r.erase(empty), "erase: player cell clears");
        ck.eq(0, r.valueAt(empty), "erase: cell empty again");
        ck.that(!r.erase(empty), "erase: already-empty is a no-op");
        ck.that(r.place(-1, 1) == SudokuRound.PlaceResult.OUT_OF_RANGE, "place: negative index rejected");
        ck.that(r.place(0, 7) == SudokuRound.PlaceResult.OUT_OF_RANGE, "place: value above size rejected");
        // overwrite a placed value with another placed value
        r.place(empty, 1);
        r.place(empty, 2);
        ck.eq(2, r.valueAt(empty), "place: player cell can be overwritten");
    }

    static void checkButton(Check ck) {
        SudokuRound r = fresh("medium", 9, 3, 3, 30, 35, false, 7L);
        int empty = firstEmpty(r);
        int wrongValue = r.solutionAt(empty) % r.size() + 1;
        r.place(empty, wrongValue);
        List<Integer> wrong = r.wrongCells();
        ck.eq(1, wrong.size(), "check: exactly the planted error is flagged");
        ck.eq(empty, wrong.get(0), "check: flagged cell is the planted one");
        r.erase(empty);
        ck.eq(0, r.wrongCells().size(), "check: clean board flags nothing");
        // adult cap: 3 uses (config max), the 4th is refused
        ck.that(r.useCheck(3), "check: use 1 allowed");
        ck.that(r.useCheck(3), "check: use 2 allowed");
        ck.that(r.useCheck(3), "check: use 3 allowed");
        ck.that(!r.useCheck(3), "check: use 4 refused at the cap");
        ck.eq(3, r.checksUsed(), "check: counter stops at the cap");
        // kid tier: free + unlimited uses
        SudokuRound kid = fresh("kid4", 4, 2, 2, 8, 10, true, 9L);
        for (int i = 0; i < 6; i++) ck.that(kid.useCheck(Integer.MAX_VALUE), "check: kid use " + (i + 1) + " allowed");
        ck.eq(6, kid.checksUsed(), "check: kid counter keeps counting (cost is waived in settlement)");
    }

    static void deductionMath(Check ck) {
        // 10% of payout per use, floored at zero (SPEC §4.4).
        ck.eq(25_000, SudokuRound.afterCheckDeduction(25_000, 0, 10), "deduct: no checks, full $250");
        ck.eq(20_000, SudokuRound.afterCheckDeduction(25_000, 2, 10), "deduct: 2 checks on $250 -> $200");
        ck.eq(12_600, SudokuRound.afterCheckDeduction(18_000, 3, 10), "deduct: 3 checks on $180 -> $126");
        ck.eq(5_400, SudokuRound.afterCheckDeduction(6_000, 1, 10), "deduct: 1 check on $60 -> $54");
        ck.eq(0, SudokuRound.afterCheckDeduction(1_000, 3, 50), "deduct: deductions floor at zero");
        // Kid-tier rule on Sudoku values (Mike's Q6 amendment, via the shared path):
        ck.eq(1_250, Payouts.adjust(2_500, 0, true, false), "kid 4x4: adult earns half of $25 = $12.50");
        ck.eq(2_500, Payouts.adjust(2_500, 0, true, true), "kid 4x4: kid-flagged earns full $25");
        ck.eq(6_000, Payouts.adjust(6_000, 0, false, false), "6x6 is not a kid tier: full $60 for everyone");
        // Speed bonus applies to the post-deduction amount on Medium/Hard:
        long after = SudokuRound.afterCheckDeduction(18_000, 1, 10);
        ck.eq(19_440, Payouts.adjust(after, 20, false, false), "medium: ($180 - 10%) + 20% speed = $194.40");
    }

    static void completionDetection(Check ck) {
        SudokuRound r = fresh("kid4", 4, 2, 2, 8, 10, false, 3L);
        ck.that(!r.isSolved(), "complete: fresh puzzle is not solved");
        // Fill every non-given with a WRONG value first (rotate solution +1):
        for (int i = 0; i < 16; i++) {
            if (!r.isGiven(i)) r.place(i, r.solutionAt(i) % 4 + 1);
        }
        ck.eq(16, r.filledCount(), "complete: board is full");
        ck.that(!r.isSolved(), "complete: a full but wrong board does NOT complete");
        ck.that(r.wrongCells().size() > 0, "complete: wrong cells are visible to Check");
        // Now fix every cell to the solution:
        for (int i = 0; i < 16; i++) {
            if (!r.isGiven(i)) r.place(i, r.solutionAt(i));
        }
        ck.that(r.isSolved(), "complete: board equal to the solution completes");
        ck.eq(0, r.wrongCells().size(), "complete: no wrong cells on a solved board");
    }

    static void solverSanity(Check ck) {
        // Production solver solves a generated puzzle back to its solution.
        SudokuRound r = fresh("easy", 9, 3, 3, 38, 44, false, 11L);
        int[] puzzle = boardOf(r);
        ck.that(SudokuRound.solve(puzzle, 9, 3, 3), "solver: generated puzzle is solvable");
        boolean same = true;
        for (int i = 0; i < 81; i++) if (puzzle[i] != r.solutionAt(i)) same = false;
        ck.that(same, "solver: the solution it finds is THE solution (unique)");
        // An empty 4x4 has many solutions; counter must report >= 2 (limit).
        ck.eq(2, SudokuRound.countSolutions(new int[16], 4, 2, 2, 2), "solver: empty 4x4 is not unique");
        // A contradictory grid has none.
        int[] bad = new int[16];
        bad[0] = 1; bad[1] = 1;
        ck.eq(0, SudokuRound.countSolutions(bad, 4, 2, 2, 2), "solver: contradictory grid counts 0");
    }

    static void serialization(Check ck) {
        SudokuRound r = fresh("medium", 9, 3, 3, 30, 35, false, 5L);
        int a = firstEmpty(r);
        int b = -1;
        for (int i = a + 1; i < 81; i++) if (!r.isGiven(i)) { b = i; break; }
        r.place(a, r.solutionAt(a));
        r.place(b, r.solutionAt(b) % 9 + 1); // a deliberate error, mid-round state
        r.useCheck(3);
        SudokuRound back = SudokuRound.fromMap(r.toMap());
        ck.that(back.tierId().equals("medium") && back.size() == 9, "serde: tier + size survive");
        ck.eq(r.givensCount(), back.givensCount(), "serde: givens survive");
        ck.eq(1, back.checksUsed(), "serde: checks used survive");
        ck.that(!back.picture() && !back.paid(), "serde: flags survive");
        boolean sameBoard = true, sameSolution = true;
        for (int i = 0; i < 81; i++) {
            if (back.valueAt(i) != r.valueAt(i) || back.isGiven(i) != r.isGiven(i)) sameBoard = false;
            if (back.solutionAt(i) != r.solutionAt(i)) sameSolution = false;
        }
        ck.that(sameBoard, "serde: board + given mask survive a mid-round restart");
        ck.that(sameSolution, "serde: solution survives");
        ck.eq(1, back.wrongCells().size(), "serde: the planted error is still detectable after reload");
        // Picture-mode round keeps its mode:
        SudokuRound pic = fresh("kid4", 4, 2, 2, 8, 10, true, 6L);
        ck.that(SudokuRound.fromMap(pic.toMap()).picture(), "serde: picture mode survives");
    }
}
