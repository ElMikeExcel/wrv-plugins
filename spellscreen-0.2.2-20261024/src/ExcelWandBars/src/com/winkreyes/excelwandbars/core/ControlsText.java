package com.winkreyes.excelwandbars.core;

import java.util.List;

/**
 * 0.2.2: how-to lines for each wand style, worded per platform. Bedrock players
 * (phone, tablet, console) read tap and press and hold, never "click".
 * Java wording is the same as 0.2.1.
 */
public final class ControlsText {
    private ControlsText() {}

    public static List<String> howTo(WandStyle s, boolean bedrock) {
        if (bedrock) {
            if (s == WandStyle.BARS) return List.of(
                    "Press and hold (controller: LT) to open your spell bar.",
                    "Tap a spell on the bar to cast it.",
                    "Sneak once to flip to the next bar (1, 2, 3, then back to 1). Press and hold again to close it.");
            return List.of(
                    "Press and hold (controller: LT) to pick the next spell.",
                    "Crouch and press and hold to open your Spell Book.",
                    "Tap (controller: RT) to cast.");
        }
        if (s == WandStyle.BARS) return List.of(
                "Right click (press and hold, or LT) opens your spell bar.",
                "Tap a spell on the bar to cast it.",
                "Sneak once to flip to the next bar (1, 2, 3, then back to 1). Right click again to close it.");
        return List.of(
                "Right click (Bedrock: press and hold, or LT) picks the next spell.",
                "Sneak + right click opens your spell list. Q goes back one spell.",
                "Left click casts.");
    }

    /** Chat Spell Book title line (used when no form can be sent). */
    public static String bookTitle(boolean bedrock) {
        return bedrock ? "Spell Book (tap a spell)" : "Spell Book (hover or click a spell)";
    }

    /** Chat Spell Book controls button. */
    public static String controlsButton(boolean bars, boolean bedrock) {
        return " [Wand controls: " + (bars ? "Sneak bars" : "Classic") + (bedrock ? " - tap to change]" : " - click to change]");
    }

    /** Chat chooser heading when no form can be sent. */
    public static String chooserHeading(String current, boolean bedrock) {
        return "Wand controls (now: " + current + "). " + (bedrock ? "Tap one" : "Click one") + ", or type /wandstyle classic or /wandstyle sneakbars:";
    }
}
