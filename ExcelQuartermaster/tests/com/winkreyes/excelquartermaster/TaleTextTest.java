package com.winkreyes.excelquartermaster;

import java.util.List;

/**
 * Plain main-method test harness for TaleText (same style as the
 * ExcelGuildGate harnesses: no test framework, every check counted, any
 * failure prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class TaleTextTest {

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        visibleLengthIgnoresCodes();
        shortLineStaysWhole();
        longLineSplitsInTwo();
        splitPreservesAllWords();
        splitSecondLineInheritsColor();
        splitIsBalanced();
        giantWordStaysWhole();
        nullAndBlank();
        defaultTalesShape();
        System.out.println("TaleTextTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void visibleLengthIgnoresCodes() {
        checkEq("visible: plain", 5, TaleText.visibleLength("hello"));
        checkEq("visible: & codes free", 5, TaleText.visibleLength("&7hello"));
        checkEq("visible: section codes free", 5, TaleText.visibleLength("§6hello"));
        checkEq("visible: mid codes free", 4, TaleText.visibleLength("ab&rcd"));
        checkEq("visible: ampersand word counts", 12, TaleText.visibleLength("fish & chips"));
        checkEq("visible: null", 0, TaleText.visibleLength(null));
    }

    private static void shortLineStaysWhole() {
        String line = "&7A short tale.";
        checkEq("short: one line, unchanged", List.of(line), TaleText.splitForChat(line));
        String exactlyAtLimit = "x".repeat(TaleText.MAX_SINGLE_LINE_VISIBLE);
        checkEq("short: exactly at limit stays whole", 1, TaleText.splitForChat(exactlyAtLimit).size());
    }

    private static void longLineSplitsInTwo() {
        String line = "&7" + "word ".repeat(40).trim(); // 199 visible chars
        List<String> parts = TaleText.splitForChat(line);
        checkEq("long: exactly two lines", 2, parts.size());
        check("long: neither half blank", !parts.get(0).isBlank() && !parts.get(1).isBlank());
    }

    private static void splitPreservesAllWords() {
        String line = "&7" + "alpha bravo charlie delta echo foxtrot golf hotel india juliet "
                + "kilo lima mike november oscar papa quebec romeo sierra tango";
        List<String> parts = TaleText.splitForChat(line);
        checkEq("words: two lines", 2, parts.size());
        String rejoined = (parts.get(0) + " " + parts.get(1)).replace("&7", "");
        checkEq("words: nothing lost or reordered", line.replace("&7", ""), rejoined);
    }

    private static void splitSecondLineInheritsColor() {
        String line = "&6" + "word ".repeat(40).trim();
        List<String> parts = TaleText.splitForChat(line);
        check("inherit: second line re-colored", parts.get(1).startsWith("&6"));
        // A second line that already carries its own code is left alone.
        String own = "&7" + "word ".repeat(20).trim() + " &c" + "word ".repeat(20).trim();
        List<String> ownParts = TaleText.splitForChat(own);
        checkEq("inherit: still two lines", 2, ownParts.size());
    }

    private static void splitIsBalanced() {
        String line = "w ".repeat(60).trim(); // 119 visible chars, just over the limit
        List<String> parts = TaleText.splitForChat(line);
        checkEq("balanced: two lines", 2, parts.size());
        int a = TaleText.visibleLength(parts.get(0));
        int b = TaleText.visibleLength(parts.get(1));
        check("balanced: halves within 2 chars", Math.abs(a - b) <= 2);
    }

    private static void giantWordStaysWhole() {
        String line = "x".repeat(300);
        checkEq("giant word: unsplit rather than cut", List.of(line), TaleText.splitForChat(line));
    }

    private static void nullAndBlank() {
        check("null: empty list", TaleText.splitForChat(null).isEmpty());
        check("blank: empty list", TaleText.splitForChat("   ").isEmpty());
    }

    /** The shipped default tales must each land as 1 or 2 chat lines. */
    private static void defaultTalesShape() {
        String[] defaults = {
            "Sit a moment. I kept the stores for the Adventurers Guild, back when its doors stood open and the road was never quiet.",
            "Five seals hold that guild now. A dragon, a wither, an old guardian of the deep, the thing that hunts by sound, and a village that must be defended. Break them all, and the doors open.",
            "A wand is not a toy and not a treasure. It is a promise you make to the valley. I only hand them to those the valley has already called. The seals will do the calling.",
        };
        for (int i = 0; i < defaults.length; i++) {
            int lines = TaleText.splitForChat("&7" + defaults[i]).size();
            check("default tale " + (i + 1) + ": 1-2 lines (got " + lines + ")", lines >= 1 && lines <= 2);
        }
    }
}
