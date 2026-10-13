package com.winkreyes.wealthboard;

/** Presentation-only text: never truncate names or concatenate the top three. */
final class WealthPopupText {
    static final String TITLE = "\u00a76\u00a7lWelcome!";
    static final String SUBTITLE = "\u00a7eWealth in chat";
    static final String EMPTY_SUBTITLE = "\u00a77Go earn!";

    private WealthPopupText() {}

    static String nameLine(int rank, String name) {
        return "\u00a7e" + rank + ". \u00a7f" + name;
    }

    static String balanceLine(String formattedAmount) {
        return "\u00a7a  $" + formattedAmount;
    }
}
