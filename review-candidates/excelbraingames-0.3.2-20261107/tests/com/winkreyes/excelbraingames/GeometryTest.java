package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Dir;
import com.winkreyes.excelbraingames.core.WsGeometry;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Word Search geometry (0.3.2): the slot maps, Mike's Bedrock length
 * ruling, cross-geometry resume, and the unknown-platform default chain.
 *
 * The display-traceability section is the behavior gate for the defect
 * Mike hit on Nov 7, 2026: it renders a round into a simulated display
 * (the inventory's slot sequence wrapped at a display width) and
 * verifies every placed word is traceable in the display in its placed
 * direction — and, as a negative control, that a 0.3.1-style Java round
 * wrapped at phone width breaks that property, which is Mike's failure
 * reproduced in the harness.
 */
public class GeometryTest {

    static final List<String> POOL = List.of(
            "CAT", "DOG", "PIG", "HEN", "COW", "BEE",
            "HORSE", "SHEEP", "TIGER", "ZEBRA", "PANDA", "CAMEL",
            "DONKEY", "TURKEY", "WALRUS", "DOLPHIN", "PENGUIN", "GIRAFFE",
            "OCTOPUS", "BUFFALO", "RACCOON", "HAMSTER", "LEOPARD", "SPARROW");

    static final WordSearchRound.Spec JAVA_KID =
            new WordSearchRound.Spec(6, 6, 5, 3, 5, EnumSet.of(Dir.E, Dir.S));
    static final WordSearchRound.Spec JAVA_CHALLENGE =
            new WordSearchRound.Spec(9, 6, 10, 5, 10, EnumSet.allOf(Dir.class));
    static final WordSearchRound.Spec BEDROCK_KID =
            new WordSearchRound.Spec(5, 8, 5, 3, 5, EnumSet.of(Dir.E, Dir.S));
    static final WordSearchRound.Spec BEDROCK_STANDARD =
            new WordSearchRound.Spec(5, 10, 8, 4, 8, EnumSet.of(Dir.E, Dir.W, Dir.S, Dir.N));
    static final WordSearchRound.Spec BEDROCK_CHALLENGE =
            new WordSearchRound.Spec(5, 10, 10, 5, 10, EnumSet.allOf(Dir.class));

    public static void main(String[] args) {
        Check ck = new Check("GeometryTest");
        javaMapIsTheLegacyMap(ck);
        bedrockStreamMap(ck);
        displayTraceability(ck);
        lengthRuling(ck);
        crossGeometryResume(ck);
        unknownPlatformDefaults(ck);
        ck.finish();
    }

    // ------------------------------------------------------------ slot maps

    /** The legacy 0.3.1 rule, restated: cell (r,c) at slot r*9+c; controls void their cells. */
    static boolean[] legacyVoids(int cols, int rows) {
        boolean[] v = new boolean[cols * rows];
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++) {
                int slot = r * 9 + c;
                if (slot == 51 || slot == 52 || slot == 53) v[r * cols + c] = true;
            }
        return v;
    }

    static void javaMapIsTheLegacyMap(Check ck) {
        int[][] shapes = {{6, 6}, {9, 6}};
        for (int[] s : shapes) {
            int cols = s[0], rows = s[1];
            for (int idx = 0; idx < cols * rows; idx++) {
                int legacySlot = (idx / cols) * 9 + (idx % cols);
                ck.eq(legacySlot, WsGeometry.JAVA.slotOf(idx, cols),
                        "java slotOf == r*9+c at " + cols + "x" + rows + " idx " + idx);
                ck.eq(idx, WsGeometry.JAVA.cellAt(legacySlot, cols, rows),
                        "java cellAt inverts slotOf at idx " + idx);
            }
            for (int slot = 0; slot < 54; slot++) {
                int r = slot / 9, c = slot % 9;
                int legacyIdx = (r < rows && c < cols) ? r * cols + c : -1;
                ck.eq(legacyIdx, WsGeometry.JAVA.cellAt(slot, cols, rows),
                        "java cellAt == legacy inverse at slot " + slot);
            }
            boolean[] legacy = legacyVoids(cols, rows);
            boolean[] geo = WsGeometry.JAVA.controlVoids(cols, rows);
            ck.eq(legacy.length, geo.length, "java voids length");
            for (int i = 0; i < legacy.length; i++)
                ck.that(legacy[i] == geo[i], "java controlVoids == legacy rule at cell " + i);
        }
    }

    static void bedrockStreamMap(Check ck) {
        int[][] shapes = {{5, 8}, {5, 10}};
        for (int[] s : shapes) {
            int cols = s[0], rows = s[1], cells = cols * rows;
            for (int idx = 0; idx < cells; idx++) {
                ck.eq(idx, WsGeometry.BEDROCK.slotOf(idx, cols),
                        "bedrock slotOf is the stream itself at idx " + idx);
                ck.eq(idx, WsGeometry.BEDROCK.cellAt(idx, cols, rows),
                        "bedrock cellAt inverts at idx " + idx);
            }
            for (int slot = cells; slot < 54; slot++)
                ck.eq(-1, WsGeometry.BEDROCK.cellAt(slot, cols, rows),
                        "bedrock tail slot " + slot + " is not a cell (controls/filler zone)");
            boolean[] v = WsGeometry.BEDROCK.controlVoids(cols, rows);
            for (int i = 0; i < cells; i++)
                ck.that(!v[i], "bedrock board has no control voids at cell " + i);
        }
    }

    // --------------------------------------------- display traceability gate

    /**
     * Check every placement of a round against a display: the inventory's
     * slot sequence (cells placed by {@code map}) wrapped at
     * {@code displayWidth} columns — how a client actually shows the GUI.
     * A placement is traceable when its letters occupy consecutive
     * display positions stepping by the placement's own direction and
     * spell the word. Returns the number of placements that break it.
     */
    static int displayBreaks(WordSearchRound r, WsGeometry map, int displayWidth) {
        int cols = r.cols();
        int breaks = 0;
        for (WordSearchRound.Placement p : r.placements()) {
            int slot0 = map.slotOf(p.startIdx(cols), cols);
            int dr0 = slot0 / displayWidth, dc0 = slot0 % displayWidth;
            boolean ok = slot0 < 54;
            for (int k = 0; ok && k < p.len(); k++) {
                int idx = p.idxAt(cols, k);
                int slot = map.slotOf(idx, cols);
                if (slot >= 54
                        || slot / displayWidth != dr0 + p.dr * k
                        || slot % displayWidth != dc0 + p.dc * k
                        || r.letterAt(idx) != p.word.charAt(k)) ok = false;
            }
            if (!ok) breaks++;
        }
        return breaks;
    }

    static void displayTraceability(Check ck) {
        // Bedrock-dealt rounds on the phone display (wrap at 5): traceable
        // under their own stream map, every seed.
        WordSearchRound.Spec[] bedrockSpecs = {BEDROCK_KID, BEDROCK_STANDARD, BEDROCK_CHALLENGE};
        for (WordSearchRound.Spec spec : bedrockSpecs) {
            for (int seed = 1; seed <= 120; seed++) {
                WordSearchRound r = WordSearchRound.generate("t", "animals", spec, POOL,
                        WsGeometry.BEDROCK.controlVoids(spec.cols(), spec.rows()),
                        WsGeometry.BEDROCK, new Random(seed * 7919L), seed);
                ck.eq(0, displayBreaks(r, WsGeometry.BEDROCK, 5),
                        "bedrock round traceable in the 5-wide stream (seed " + seed + ")");
            }
        }
        // Java-dealt rounds on the Java display (wrap at 9): traceable
        // (unchanged 0.3.1 behavior); the SAME rounds wrapped at phone
        // width break — the negative control reproducing Mike's defect.
        WordSearchRound.Spec[] javaSpecs = {JAVA_KID, JAVA_CHALLENGE};
        int phoneBreaksTotal = 0, javaRounds = 0;
        for (WordSearchRound.Spec spec : javaSpecs) {
            for (int seed = 1; seed <= 120; seed++) {
                WordSearchRound r = WordSearchRound.generate("t", "animals", spec, POOL,
                        WsGeometry.JAVA.controlVoids(spec.cols(), spec.rows()),
                        WsGeometry.JAVA, new Random(seed * 104729L), seed);
                ck.eq(0, displayBreaks(r, WsGeometry.JAVA, 9),
                        "java round traceable in the 9-wide grid (seed " + seed + ")");
                phoneBreaksTotal += displayBreaks(r, WsGeometry.JAVA, 5);
                javaRounds++;
            }
        }
        ck.that(phoneBreaksTotal > 0,
                "negative control: a 0.3.1-style java round wrapped at phone width breaks traceability ("
                        + phoneBreaksTotal + " broken placements over " + javaRounds + " rounds)");
    }

    // --------------------------------------------------------- length ruling

    static void lengthRuling(Check ck) {
        // Bedrock: no horizontal (dr == 0) placement longer than the stream
        // width, across tiers and seeds; long words still place, off-axis.
        // Saturation is asserted where this synthetic pool is calibrated
        // for it (standard: 300/300). At the challenge shape the synthetic
        // pool is letter-heavy (nine of its 18 band words are 7 letters,
        // all ruling-restricted) and deals land at 8-9 of 10 — the
        // generator's documented shortfall shape for an undersized pool;
        // saturation at the shipped challenge shape over the SHIPPED
        // pools is BedrockPropertyTest's proof (500/500 per theme).
        WordSearchRound.Spec[] specs = {BEDROCK_STANDARD, BEDROCK_CHALLENGE};
        long longOffAxis = 0;
        for (WordSearchRound.Spec spec : specs) {
            int seeds = spec == BEDROCK_CHALLENGE ? 150 : 300;
            for (int seed = 1; seed <= seeds; seed++) {
                WordSearchRound r = WordSearchRound.generate("t", "animals", spec, POOL,
                        WsGeometry.BEDROCK.controlVoids(spec.cols(), spec.rows()),
                        WsGeometry.BEDROCK, new Random(seed * 31L + 7), seed);
                if (spec == BEDROCK_STANDARD) {
                    ck.eq(spec.wordCount(), r.placements().size(),
                            "bedrock standard saturates under the ruling (seed " + seed + ")");
                } else {
                    ck.that(r.placements().size() >= spec.wordCount() - 2,
                            "bedrock challenge deals near-full on the synthetic pool (seed " + seed
                                    + ", got " + r.placements().size() + ")");
                }
                for (int i = 0; i < r.placements().size(); i++) {
                    WordSearchRound.Placement p = r.placements().get(i);
                    ck.that(!(p.dr == 0 && p.len() > WsGeometry.BEDROCK.guiCols),
                            "no bedrock E/W word longer than 5: " + p.word + " len " + p.len());
                    if (p.len() > 5 && p.dr != 0) longOffAxis++;
                    ck.that(p.word.equals(r.readPlacement(i)), "bedrock placement readable: " + p.word);
                }
            }
        }
        ck.that(longOffAxis > 0, "long bedrock words place off the horizontal axis (" + longOffAxis + " seen)");
        // Java control: the ruling is geometry-gated — long E/W placements
        // still occur on Java boards.
        long javaLongHorizontal = 0;
        for (int seed = 1; seed <= 300; seed++) {
            WordSearchRound r = WordSearchRound.generate("t", "animals", JAVA_CHALLENGE, POOL,
                    WsGeometry.JAVA.controlVoids(JAVA_CHALLENGE.cols(), JAVA_CHALLENGE.rows()),
                    WsGeometry.JAVA, new Random(seed * 31L + 7), seed);
            for (WordSearchRound.Placement p : r.placements())
                if (p.dr == 0 && p.len() > 5) javaLongHorizontal++;
        }
        ck.that(javaLongHorizontal > 0,
                "java boards still place long horizontal words (" + javaLongHorizontal + " seen)");
        // Kid bedrock: every word fits the width anyway; geometry is recorded.
        WordSearchRound kid = WordSearchRound.generate("kid", "animals", BEDROCK_KID, POOL,
                WsGeometry.BEDROCK.controlVoids(5, 8), WsGeometry.BEDROCK, new Random(42), 42);
        ck.that(kid.geometry() == WsGeometry.BEDROCK, "kid bedrock round records BEDROCK geometry");
        ck.eq(5, kid.placements().size(), "kid bedrock round saturates");
    }

    // ------------------------------------------------------ resume & default

    static void crossGeometryResume(Check ck) {
        // A Bedrock round survives a save/load with its geometry and board.
        WordSearchRound r = WordSearchRound.generate("challenge", "animals", BEDROCK_CHALLENGE, POOL,
                WsGeometry.BEDROCK.controlVoids(5, 10), WsGeometry.BEDROCK, new Random(99), 1234);
        // Find one word first, so the found-set is part of the round-trip.
        WordSearchRound.Placement p0 = r.placements().get(0);
        ck.that(r.tap(p0.startIdx(5)) == WordSearchRound.TapResult.PENDING_SET, "resume setup: first tap");
        ck.that(r.tap(p0.endIdx(5)) == WordSearchRound.TapResult.FOUND, "resume setup: word found");
        Map<String, Object> map = r.toMap();
        ck.that("BEDROCK".equals(String.valueOf(map.get("geometry"))), "save carries the geometry key");
        WordSearchRound back = WordSearchRound.fromMap(map);
        ck.that(back.geometry() == WsGeometry.BEDROCK, "bedrock round reloads as BEDROCK");
        ck.eq(r.cols(), back.cols(), "resume keeps cols");
        ck.eq(r.rows(), back.rows(), "resume keeps rows");
        ck.eq(r.placements().size(), back.placements().size(), "resume keeps placement count");
        for (int i = 0; i < r.placements().size(); i++) {
            WordSearchRound.Placement a = r.placements().get(i), b = back.placements().get(i);
            ck.that(a.word.equals(b.word), "resume keeps word " + i);
            ck.that(a.r == b.r && a.c == b.c && a.dr == b.dr && a.dc == b.dc,
                    "resume keeps placement geometry " + i);
            ck.that(a.word.equals(back.readPlacement(i)), "resumed placement readable: " + a.word);
        }
        ck.that(r.found().equals(back.found()), "resume keeps the found set");
        ck.eq(0, displayBreaks(back, back.geometry(), back.geometry().guiCols),
                "resumed bedrock round renders traceable under its own map");
        // It also SOLVES under the resumed copy (the other-platform resume).
        for (int i = 1; i < back.placements().size(); i++) {
            WordSearchRound.Placement p = back.placements().get(i);
            back.tap(p.startIdx(back.cols()));
            ck.that(back.tap(p.endIdx(back.cols())) == WordSearchRound.TapResult.FOUND,
                    "resumed round solves word " + p.word);
        }
        ck.that(back.isComplete(), "resumed bedrock round completes");

        // A Java round round-trips as JAVA.
        WordSearchRound j = WordSearchRound.generate("challenge", "animals", JAVA_CHALLENGE, POOL,
                WsGeometry.JAVA.controlVoids(9, 6), WsGeometry.JAVA, new Random(7), 55);
        WordSearchRound jBack = WordSearchRound.fromMap(j.toMap());
        ck.that(jBack.geometry() == WsGeometry.JAVA, "java round reloads as JAVA");
        ck.eq(0, displayBreaks(jBack, jBack.geometry(), jBack.geometry().guiCols), "resumed java round traceable");

        // A pre-0.3.2 save (no geometry key) loads as JAVA, board intact.
        Map<String, Object> legacy = new java.util.LinkedHashMap<>(j.toMap());
        legacy.remove("geometry");
        WordSearchRound lBack = WordSearchRound.fromMap(legacy);
        ck.that(lBack.geometry() == WsGeometry.JAVA, "pre-0.3.2 save defaults to JAVA geometry");
        ck.eq(j.placements().size(), lBack.placements().size(), "legacy save keeps its board");
        for (int i = 0; i < j.placements().size(); i++)
            ck.that(j.placements().get(i).word.equals(lBack.readPlacement(i)), "legacy placement readable");
    }

    static void unknownPlatformDefaults(Check ck) {
        // The parse-level half of the unknown-platform default: anything
        // unrecognized is JAVA. (The plugin half — no Floodgate binding
        // means platformOf answers JAVA for everyone — is proven by the
        // rig boot without floodgate; see TEST-REPORT.md.)
        ck.that(WsGeometry.parse(null) == WsGeometry.JAVA, "parse(null) is JAVA");
        ck.that(WsGeometry.parse("") == WsGeometry.JAVA, "parse(empty) is JAVA");
        ck.that(WsGeometry.parse("sideways") == WsGeometry.JAVA, "parse(junk) is JAVA");
        ck.that(WsGeometry.parse("bedrock") == WsGeometry.BEDROCK, "parse(bedrock) is BEDROCK");
        ck.that(WsGeometry.parse(" BEDROCK ") == WsGeometry.BEDROCK, "parse trims and folds case");
        ck.that(WsGeometry.parse("Java") == WsGeometry.JAVA, "parse(Java) is JAVA");
        // A save carrying a garbage geometry value also lands on JAVA.
        WordSearchRound r = WordSearchRound.generate("kid", "animals", BEDROCK_KID, POOL,
                WsGeometry.BEDROCK.controlVoids(5, 8), WsGeometry.BEDROCK, new Random(3), 3);
        Map<String, Object> map = new java.util.LinkedHashMap<>(r.toMap());
        map.put("geometry", "SIDEWAYS");
        ck.that(WordSearchRound.fromMap(map).geometry() == WsGeometry.JAVA,
                "garbage geometry in a save defaults to JAVA");
    }
}
