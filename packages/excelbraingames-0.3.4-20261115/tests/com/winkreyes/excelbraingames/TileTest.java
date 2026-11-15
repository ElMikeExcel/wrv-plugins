package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.ChoicePrintTracker;
import com.winkreyes.excelbraingames.core.TileKeys;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuTextGrid;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Letter Tiles tests (0.3.1): the TileKeys key space is exactly the 205
 * tiles the packs ship (cross-checked against packs/dist/tiles-manifest.json
 * when -Dtiles=<path> is given, as build.sh does); every render mapping the
 * views use resolves; Quick Math kid choice values always fit the generated
 * number-tile range; the Scramble chat print fires once per prompt; the
 * Quick Math kid question print fires once per question (index-keyed);
 * and SudokuTextGrid renders a true square whose every cell matches the round.
 */
public final class TileTest {

    public static void main(String[] args) {
        Check ck = new Check("TileTest");
        keySpace(ck);
        numberLabels(ck);
        manifestCrossCheck(ck);
        kidMathBound(ck);
        choicePrint(ck);
        quickMathPrint(ck);
        sudokuGrid(ck);
        ck.finish();
    }

    static void keySpace(Check ck) {
        List<String> all = TileKeys.allModelKeys();
        ck.eq(205, all.size(), "key space is 205 tiles");
        ck.eq(205, new HashSet<>(all).size(), "key space has no duplicates");
        List<String> sorted = all.stream().sorted().toList();
        ck.that(all.equals(sorted), "key space is sorted");
        for (char c = 'a'; c <= 'z'; c++) {
            for (TileKeys.WsState s : TileKeys.WsState.values()) {
                ck.that(all.contains(TileKeys.wsModel(c, s)), "ws key for " + c + "/" + s);
            }
            ck.that(TileKeys.wsModel(Character.toUpperCase(c), TileKeys.WsState.NORMAL)
                    .equals(TileKeys.wsModel(c, TileKeys.WsState.NORMAL)), "ws key lowercases " + c);
        }
        for (int d = 1; d <= 9; d++) {
            for (TileKeys.SudokuState s : TileKeys.SudokuState.values()) {
                ck.that(all.contains(TileKeys.sudokuModel(d, s)), "sudoku key for " + d + "/" + s);
            }
        }
        for (int v = 0; v <= TileKeys.MAX_NUMBER; v++) {
            ck.that(all.contains(TileKeys.numberModel(v)), "number key for " + v);
        }
        ck.that(throwsIae(() -> TileKeys.wsModel('1', TileKeys.WsState.NORMAL)), "ws rejects a digit");
        ck.that(throwsIae(() -> TileKeys.sudokuModel(0, TileKeys.SudokuState.GIVEN)), "sudoku rejects 0");
        ck.that(throwsIae(() -> TileKeys.sudokuModel(10, TileKeys.SudokuState.GIVEN)), "sudoku rejects 10");
        ck.that(throwsIae(() -> TileKeys.numberModel(-1)), "number rejects -1");
        ck.that(throwsIae(() -> TileKeys.numberModel(100)), "number rejects 100");
    }

    static boolean throwsIae(Runnable r) {
        try { r.run(); return false; } catch (IllegalArgumentException e) { return true; }
    }

    static void numberLabels(Check ck) {
        ck.that("number/0".equals(TileKeys.numberModelFor("0")), "label 0 tiles");
        ck.that("number/7".equals(TileKeys.numberModelFor("7")), "label 7 tiles");
        ck.that("number/40".equals(TileKeys.numberModelFor("40")), "label 40 tiles");
        ck.that("number/99".equals(TileKeys.numberModelFor("99")), "label 99 tiles");
        ck.that(TileKeys.numberModelFor("100") == null, "label 100 has no tile (view leaves it plain)");
        ck.that(TileKeys.numberModelFor("-3") == null, "negative label has no tile");
        ck.that(TileKeys.numberModelFor("apple") == null, "word label has no tile");
        ck.that(TileKeys.numberModelFor("") == null, "blank label has no tile");
        ck.that(TileKeys.numberModelFor(null) == null, "null label has no tile");
        ck.that(TileKeys.numberModelFor(" 12 ") != null, "padded label still tiles");
    }

    /** The packs' manifest (from the generator) must name exactly TileKeys' space. */
    static void manifestCrossCheck(Check ck) {
        String path = System.getProperty("tiles");
        if (path == null) {
            System.out.println("  (manifest cross-check skipped: -Dtiles not set)");
            return;
        }
        String json;
        try {
            json = Files.readString(Path.of(path));
        } catch (IOException e) {
            ck.that(false, "tiles manifest unreadable: " + e.getMessage());
            return;
        }
        Set<String> manifestKeys = new HashSet<>();
        Matcher m = Pattern.compile("\"key\":\\s*\"([^\"]+)\"").matcher(json);
        while (m.find()) manifestKeys.add(m.group(1));
        ck.eq(205, manifestKeys.size(), "manifest names 205 tiles");
        ck.that(manifestKeys.equals(new HashSet<>(TileKeys.allModelKeys())),
                "manifest key set == TileKeys key space");
    }

    /**
     * Every value a Quick Math KID choice card can show must have a tile.
     * Kid questions and distractors are sampled across 2,000 generated
     * rounds (first question of each; the generator path is identical for
     * every question in a round).
     */
    static void kidMathBound(Check ck) {
        int observedMax = -1;
        for (int seed = 0; seed < 2000; seed++) {
            QuickMathRound r = QuickMathRound.generate("kid", true, true, new Random(seed), 1_000L);
            List<Integer> choices = r.currentChoices();
            ck.eq(3, choices.size(), "kid card shows 3 choices");
            for (int v : choices) {
                observedMax = Math.max(observedMax, v);
                ck.that(v >= 0 && v <= TileKeys.MAX_NUMBER, "kid choice " + v + " fits the tile range");
                ck.that(TileKeys.numberModelFor(String.valueOf(v)) != null, "kid choice " + v + " tiles");
            }
        }
        System.out.println("  kid QM observed choice max over 2000 rounds: " + observedMax + " (design bound 40)");
        ck.that(observedMax <= 40, "observed kid max within the design bound");
    }

    static void choicePrint(Check ck) {
        ChoicePrintTracker t = new ChoicePrintTracker();
        ck.that(t.markIfNew("ELPPA|APPLE,PEPAL,PAPEL"), "first prompt announces");
        ck.that(!t.markIfNew("ELPPA|APPLE,PEPAL,PAPEL"), "same prompt stays silent on re-render");
        ck.that(!t.markIfNew("ELPPA|APPLE,PEPAL,PAPEL"), "and stays silent again");
        ck.that(t.markIfNew("ANANAB|BANANA,ANANAB,NABANA"), "next word announces");
        ck.that(!t.markIfNew(null), "null never announces");
    }

    /**
     * The Quick Math kid question print (0.3.1 add-on, SPEC-DELTA §8): the
     * view keys the shared tracker "qm|<index>|<question>|<choices>" — the
     * index makes each of the 10 questions announce exactly once per card
     * even when the same question text recurs in a round, while re-renders
     * of the SAME question stay silent. Typed rounds carry no choices at
     * all, which is what keeps the print (it lives in the choices branch)
     * out of modes already served by promptInChat.
     */
    static void quickMathPrint(Check ck) {
        ChoicePrintTracker t = new ChoicePrintTracker();
        QuickMathRound r = QuickMathRound.generate("kid", true, true, new Random(42), 1_000L);
        while (!r.finished()) {
            String key = "qm|" + r.index() + "|" + r.currentPrompt() + "|"
                    + String.join(",", r.currentChoices().stream().map(String::valueOf).toList());
            ck.that(t.markIfNew(key), "qm question " + r.index() + " announces on first render");
            ck.that(!t.markIfNew(key), "qm question " + r.index() + " stays silent on re-render");
            r.answer(r.currentAnswer());
        }
        // Identical question text at the next index is a new question.
        ck.that(t.markIfNew("qm|3|5 + 3 = ?|6,8,10"), "identical text, first index announces");
        ck.that(!t.markIfNew("qm|3|5 + 3 = ?|6,8,10"), "identical text, same index silent");
        ck.that(t.markIfNew("qm|4|5 + 3 = ?|6,8,10"), "identical text, next index announces");
        // Typed rounds (Standard/Challenge, kid choices off): no choices,
        // ever — the choices-branch print cannot fire there.
        for (String tier : new String[]{"standard", "challenge"}) {
            QuickMathRound typed = QuickMathRound.generate(tier, false, false, new Random(7), 1_000L);
            while (!typed.finished()) {
                ck.that(typed.currentChoices().isEmpty(), tier + " question " + typed.index() + " has no choice card");
                typed.answer(typed.currentAnswer());
            }
        }
        QuickMathRound kidTyped = QuickMathRound.generate("kid", true, false, new Random(7), 1_000L);
        ck.that(kidTyped.currentChoices().isEmpty(), "kid round with choices off has no choice card");
    }

    static void sudokuGrid(Check ck) {
        gridFor(ck, SudokuRound.generate("kid4", 4, 2, 2, 6, 8, false, new Random(7), 1_000L));
        gridFor(ck, SudokuRound.generate("std6", 6, 2, 3, 14, 18, false, new Random(8), 1_000L));
        gridFor(ck, SudokuRound.generate("std9", 9, 3, 3, 30, 35, false, new Random(9), 1_000L));
    }

    static void gridFor(Check ck, SudokuRound round) {
        int n = round.size(), br = round.boxRows(), bc = round.boxCols();
        List<String> lines = SudokuTextGrid.lines(round);
        ck.eq(1 + n + (n / br + 1), lines.size(), n + "x" + n + " grid line count");
        int width = lines.get(1).length();
        for (String line : lines) ck.eq(width, line.length(), n + "x" + n + " grid stays square (equal line lengths)");
        // Every cell of the round appears at its computed square position.
        for (int r = 0; r < n; r++) {
            String line = lines.get(1 + r + (r / br + 1));
            for (int c = 0; c < n; c++) {
                int idx = r * n + c;
                int start = 3 + 3 * c + c / bc;
                String cellText = line.substring(start, start + 3);
                int v = round.valueAt(idx);
                String want = v == 0 ? " . " : round.isGiven(idx) ? "[" + v + "]" : " " + v + " ";
                ck.that(want.equals(cellText), n + "x" + n + " cell " + idx + " renders '" + want + "' (got '" + cellText + "')");
            }
        }
        // A placed digit re-renders plain (unbracketed) at the same square slot.
        for (int idx = 0; idx < n * n; idx++) {
            if (!round.isGiven(idx) && round.valueAt(idx) == 0) {
                int sol = round.solutionAt(idx);
                round.place(idx, sol);
                int r = idx / n, c = idx % n;
                String line = SudokuTextGrid.lines(round).get(1 + r + (r / br + 1));
                int start = 3 + 3 * c + c / bc;
                ck.that((" " + sol + " ").equals(line.substring(start, start + 3)),
                        n + "x" + n + " placed digit renders plain at " + idx);
                break;
            }
        }
    }
}
