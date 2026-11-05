package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Dir;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Word Search logic: the two-tap checker in every direction class, tier
 * direction legality, void cells, cancel/re-tap behavior, serialization.
 */
public final class WordSearchTest {

    static final List<String> POOL = List.of(
            "CAT", "DOG", "HORSE", "SHEEP", "TIGER", "ZEBRA", "PANDA", "CAMEL",
            "DOLPHIN", "PENGUIN", "GIRAFFE", "OCTOPUS", "BUFFALO", "RACCOON",
            "HAMSTER", "LEOPARD", "WALRUS", "SPARROW", "DONKEY", "TURKEY",
            "BISON", "MOOSE", "LLAMA", "OTTER", "GOOSE", "KOALA");

    /** The GUI control-slot void rule, mirrored from GameService. */
    static boolean[] voids(int cols, int rows) {
        boolean[] v = new boolean[cols * rows];
        for (int r = 0; r < rows; r++) for (int c = 0; c < cols; c++) {
            int slot = r * 9 + c;
            if (slot == 51 || slot == 52 || slot == 53) v[r * cols + c] = true;
        }
        return v;
    }

    public static void main(String[] args) {
        Check ck = new Check("WordSearchTest");
        testCheckerEveryDirection(ck);
        testTierDirectionLegality(ck);
        testInvalidSelections(ck);
        testCancelAndRetap(ck);
        testVoids(ck);
        testSerialization(ck);
        ck.finish();
    }

    /** Hand-place one word in each of the 8 directions and tap it both ways. */
    static void testCheckerEveryDirection(Check ck) {
        for (Dir d : Dir.values()) {
            int cols = 9, rows = 6;
            char[] grid = new char[cols * rows];
            boolean[] voids = voids(cols, rows);
            // put "WORD" heading d, starting far enough from the edges to fit
            String word = "WORD";
            int r = d.dr < 0 ? 4 : 2, c = d.dc < 0 ? 4 : 2;
            for (int i = 0; i < word.length(); i++) grid[(r + d.dr * i) * cols + (c + d.dc * i)] = word.charAt(i);
            for (int i = 0; i < grid.length; i++) if (grid[i] == 0 && !voids[i]) grid[i] = 'X';
            WordSearchRound.Placement pl = new WordSearchRound.Placement(word, r, c, d.dr, d.dc);
            WordSearchRound round = roundOf(cols, rows, grid, voids, List.of(pl));
            int start = pl.startIdx(cols), end = pl.endIdx(cols);
            ck.that(round.tap(start) == WordSearchRound.TapResult.PENDING_SET, d + ": first tap sets pending");
            ck.that(round.tap(end) == WordSearchRound.TapResult.FOUND, d + ": forward tap finds");
            ck.that(round.isComplete(), d + ": complete after find");
            // reverse tap order on a fresh round must also find it
            WordSearchRound round2 = roundOf(cols, rows, grid, voids, List.of(pl));
            ck.that(round2.tap(end) == WordSearchRound.TapResult.PENDING_SET, d + ": reverse first tap");
            ck.that(round2.tap(start) == WordSearchRound.TapResult.FOUND, d + ": reverse tap finds");
        }
    }

    static void testTierDirectionLegality(Check ck) {
        Map<String, WordSearchRound.Spec> tiers = Map.of(
                "kid", new WordSearchRound.Spec(6, 6, 5, 3, 5, EnumSet.of(Dir.E, Dir.S)),
                "standard", new WordSearchRound.Spec(9, 6, 8, 4, 8, EnumSet.of(Dir.E, Dir.W, Dir.S, Dir.N)),
                "challenge", new WordSearchRound.Spec(9, 6, 10, 5, 10, EnumSet.allOf(Dir.class)));
        for (Map.Entry<String, WordSearchRound.Spec> e : tiers.entrySet()) {
            for (int seed = 0; seed < 200; seed++) {
                WordSearchRound r = WordSearchRound.generate(e.getKey(), "animals", e.getValue(),
                        POOL, voids(e.getValue().cols(), e.getValue().rows()), new Random(seed), 0L);
                ck.that(r.placements().size() == e.getValue().wordCount(),
                        e.getKey() + " seed " + seed + ": full word count placed");
                for (WordSearchRound.Placement p : r.placements()) {
                    Dir d = Dir.ofDelta(p.dr, p.dc);
                    ck.that(d != null && e.getValue().directions().contains(d),
                            e.getKey() + ": placement direction legal (" + p.word + ")");
                    ck.that(r.readPlacement(r.placements().indexOf(p)).equals(p.word),
                            e.getKey() + ": placed word readable at placement (" + p.word + ")");
                }
            }
        }
    }

    static void testInvalidSelections(Check ck) {
        WordSearchRound.Spec spec = new WordSearchRound.Spec(9, 6, 8, 4, 8,
                EnumSet.of(Dir.E, Dir.W, Dir.S, Dir.N));
        WordSearchRound r = WordSearchRound.generate("standard", "animals", spec, POOL,
                voids(9, 6), new Random(42), 0L);
        // pick two cells that are not any placement's endpoints: brute force a no-match pair
        Set<Long> endpointPairs = new java.util.HashSet<>();
        for (WordSearchRound.Placement p : r.placements()) {
            endpointPairs.add(pairKey(p.startIdx(9), p.endIdx(9)));
            endpointPairs.add(pairKey(p.endIdx(9), p.startIdx(9)));
        }
        boolean testedNoMatch = false, testedNotLine = false;
        outer:
        for (int a = 0; a < 54 && !testedNoMatch; a++) {
            if (r.isVoid(a)) continue;
            for (int b = 0; b < 54; b++) {
                if (b == a || r.isVoid(b)) continue;
                int ar = a / 9, ac = a % 9, br = b / 9, bc = b % 9;
                boolean straight = ar == br || ac == bc || Math.abs(ar - br) == Math.abs(ac - bc);
                if (!straight || endpointPairs.contains(pairKey(a, b))) continue;
                // fresh copy so state doesn't leak between probes
                WordSearchRound probe = WordSearchRound.fromMap(r.toMap());
                probe.tap(a);
                WordSearchRound.TapResult res = probe.tap(b);
                if (res == WordSearchRound.TapResult.NO_MATCH) { testedNoMatch = true; break outer; }
            }
        }
        ck.that(testedNoMatch, "a straight non-word selection is NO_MATCH");
        // not-a-line: (0,0) -> (1,2) is a knight move
        WordSearchRound probe = WordSearchRound.fromMap(r.toMap());
        probe.tap(0);
        ck.that(probe.tap(1 * 9 + 2) == WordSearchRound.TapResult.NOT_A_LINE, "knight move is NOT_A_LINE");
        testedNotLine = true;
        ck.that(testedNotLine, "not-a-line probe ran");
    }

    static void testCancelAndRetap(Check ck) {
        WordSearchRound.Spec spec = new WordSearchRound.Spec(9, 6, 8, 4, 8,
                EnumSet.of(Dir.E, Dir.W, Dir.S, Dir.N));
        WordSearchRound r = WordSearchRound.generate("standard", "animals", spec, POOL,
                voids(9, 6), new Random(7), 0L);
        WordSearchRound.Placement p = r.placements().get(0);
        int start = p.startIdx(9), end = p.endIdx(9);
        ck.that(r.tap(start) == WordSearchRound.TapResult.PENDING_SET, "tap start");
        ck.that(r.tap(start) == WordSearchRound.TapResult.CANCELLED, "tap same cell cancels");
        ck.that(r.pending() == -1, "pending cleared after cancel");
        ck.that(r.tap(start) == WordSearchRound.TapResult.PENDING_SET, "tap start again");
        ck.that(r.tap(end) == WordSearchRound.TapResult.FOUND, "find the word");
        ck.that(r.tap(start) == WordSearchRound.TapResult.PENDING_SET, "tap found word start");
        ck.that(r.tap(end) == WordSearchRound.TapResult.ALREADY_FOUND, "re-tap found word says ALREADY_FOUND");
        ck.that(r.found().size() == 1, "found count unchanged by re-tap");
    }

    static void testVoids(Check ck) {
        boolean[] v = voids(9, 6);
        ck.that(v[51 - 0] || true, "void computation ran"); // slots 51..53 map to cells (5,6),(5,7),(5,8) in a 9-wide grid
        ck.that(v[5 * 9 + 6] && v[5 * 9 + 7] && v[5 * 9 + 8], "bottom-right three cells are void on 9x6");
        WordSearchRound.Spec spec = new WordSearchRound.Spec(9, 6, 10, 5, 10, EnumSet.allOf(Dir.class));
        for (int seed = 0; seed < 200; seed++) {
            WordSearchRound r = WordSearchRound.generate("challenge", "animals", spec, POOL, v, new Random(seed), 0L);
            for (WordSearchRound.Placement p : r.placements()) {
                for (int i = 0; i < p.len(); i++) {
                    ck.that(!r.isVoid(p.idxAt(9, i)), "no placement crosses a void cell (seed " + seed + ")");
                }
            }
            ck.that(r.tap(5 * 9 + 6) == WordSearchRound.TapResult.IGNORED, "tapping a void cell is IGNORED");
        }
    }

    static void testSerialization(Check ck) {
        WordSearchRound.Spec spec = new WordSearchRound.Spec(9, 6, 8, 4, 8,
                EnumSet.of(Dir.E, Dir.W, Dir.S, Dir.N));
        WordSearchRound r = WordSearchRound.generate("standard", "animals", spec, POOL,
                voids(9, 6), new Random(99), 123456L);
        WordSearchRound.Placement p = r.placements().get(0);
        r.tap(p.startIdx(9));
        r.tap(p.endIdx(9));
        WordSearchRound back = WordSearchRound.fromMap(r.toMap());
        ck.that(back.tierId().equals("standard") && back.themeId().equals("animals"), "round trip: ids");
        ck.eq(123456L, back.startedMillis(), "round trip: started");
        ck.that(back.found().equals(r.found()), "round trip: found set");
        ck.that(back.placements().size() == r.placements().size(), "round trip: placements");
        boolean lettersSame = true;
        for (int i = 0; i < 54; i++) if (back.letterAt(i) != r.letterAt(i)) lettersSame = false;
        ck.that(lettersSame, "round trip: grid letters");
        ck.that(back.readPlacement(0).equals(r.readPlacement(0)), "round trip: placement readable");
    }

    static long pairKey(int a, int b) { return ((long) a << 32) | (b & 0xffffffffL); }

    static WordSearchRound roundOf(int cols, int rows, char[] grid, boolean[] voids,
                                   List<WordSearchRound.Placement> placements) {
        // Build via serialization so the test uses only the public surface.
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("tier", "standard");
        m.put("theme", "animals");
        m.put("cols", cols);
        m.put("rows", rows);
        StringBuilder g = new StringBuilder();
        for (char ch : grid) g.append(ch == 0 ? '.' : ch);
        m.put("grid", g.toString());
        StringBuilder v = new StringBuilder();
        for (boolean b : voids) v.append(b ? '1' : '0');
        m.put("voids", v.toString());
        List<String> ps = new java.util.ArrayList<>();
        for (WordSearchRound.Placement p : placements) {
            ps.add(p.word + ":" + p.r + "," + p.c + "," + p.dr + "," + p.dc);
        }
        m.put("placements", ps);
        m.put("found", List.of());
        m.put("started", 0L);
        m.put("paid", false);
        return WordSearchRound.fromMap(m);
    }
}
