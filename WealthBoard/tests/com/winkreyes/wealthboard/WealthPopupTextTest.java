package com.winkreyes.wealthboard;

/** Runs without server API dependencies; this is text-layout verification only. */
public final class WealthPopupTextTest {
    public static void main(String[] args) {
        equals("Welcome!", plain(WealthPopupText.TITLE));
        equals("Wealth in chat", plain(WealthPopupText.SUBTITLE));
        equals("Go earn!", plain(WealthPopupText.EMPTY_SUBTITLE));
        String[] names = {".SixteenCharName12", ".MobilePlayerTwo", "玩家名字", "VeryLongBedrockGamertag_123456789"};
        for (int i = 0; i < names.length; i++) {
            equals((i + 1) + ". " + names[i], plain(WealthPopupText.nameLine(i + 1, names[i])));
        }
        equals("  $6,481.24", plain(WealthPopupText.balanceLine("6,481.24")));
        equals("  $4,706.23", plain(WealthPopupText.balanceLine("4,706.23")));
        equals("  $3,000,000,000.25", plain(WealthPopupText.balanceLine("3,000,000,000.25")));
        System.out.println("PASS: 10 text-layout assertions; full names preserved, compact fixed titles, separate balances.");
    }

    private static String plain(String text) {
        return text.replaceAll("\u00a7[0-9a-fk-or]", "");
    }

    private static void equals(String expected, String actual) {
        if (!expected.equals(actual)) throw new AssertionError("Expected: " + expected + "; actual: " + actual);
    }
}
