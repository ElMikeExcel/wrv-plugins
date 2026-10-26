package com.winkreyes.excelwandbars.core;

import java.util.List;

/**
 * 0.2.2: how-to lines for each wand style, worded per platform. Bedrock players
 * (phone, tablet, console) read tap and press and hold, never "click".
 * Java wording is the same as 0.2.1.
 * 0.2.4: Tap to cycle (original) and Tap bar; one-line button descriptions.
 */
public final class ControlsText {
    private ControlsText() {}

    public static List<String> howTo(WandStyle s, boolean bedrock) {
        // 0.2.4: the two hotbar-icon styles, worded the same way everywhere.
        if (s == WandStyle.TAPBAR) return bedrock ? List.of(
                "Tap your wand in the hotbar and all your spells fill the bar.",
                "Tap a spell icon to cast it right away. Each spell keeps its own icon and slot.",
                "More spells than fit? Crouch flips to the next page (Page 1/2 shows on screen).",
                "Press and hold the screen (not an icon) to read your spells: what each does, mana, cooldown and rank. It never casts.",
                "Tap the empty slot at the end of the bar to get your items back.")
                : List.of(
                "Hold your wand and all your spells fill the hotbar.",
                "Press a hotbar key (1-9) or scroll to cast that spell right away.",
                "More spells than fit? Sneak flips to the next page (Page 1/2 shows on screen).",
                "Left click reads your spells: what each does, mana, cooldown and rank. It never casts. Hover an icon for the same.",
                "Q, right click or the empty slot at the end of the bar puts your items back. Right click again brings the bar back.");
        if (s == WandStyle.ORIGINAL) return bedrock ? List.of(
                "Tap your wand in the hotbar and all your spells fill the bar.",
                "Tap a spell icon to pick it, then press and hold the screen (controller: RT) to cast.",
                "More spells than fit? Crouch flips to the next page.",
                "Press and hold your wand, or tap the empty slot at the end of the bar, to get your items back.")
                : List.of(
                "Hold your wand and all your spells fill the hotbar.",
                "Press a hotbar key (1-9) or scroll to pick a spell. Left click casts it.",
                "More spells than fit? Sneak flips to the next page.",
                "Q or right click puts your items back. Right click again brings the bar back.");
        if (bedrock) {
            if (s == WandStyle.CYCLE) return List.of(
                    "Press and hold (controller: LT) to go to the next spell. Its name shows on screen.",
                    "Crouch and press and hold (controller: crouch + LT) to go back one spell.",
                    "Tap (controller: RT) to cast. No spell list pops up.");
            if (s == WandStyle.BARS) return List.of(
                    "Press and hold (controller: LT) to open your spell bar.",
                    "Tap a spell on the bar to cast it.",
                    "Have more than one bar? Crouch once to flip to the next one. Press and hold again to close it.");
            return List.of(
                    "Press and hold (controller: LT) to pick the next spell.",
                    "Crouch and press and hold to open your Spell Book.",
                    "Tap (controller: RT) to cast.");
        }
        if (s == WandStyle.CYCLE) return List.of(
                "Right click (Bedrock: press and hold, or LT) goes to the next spell. Its name shows on screen.",
                "Sneak + right click or Q goes back one spell. No spell list pops up (/spellbook shows them).",
                "Left click casts.");
        if (s == WandStyle.BARS) return List.of(
                "Right click (press and hold, or LT) opens your spell bar.",
                "Tap a spell on the bar to cast it.",
                "Have more than one bar? Sneak once to flip to the next one. Right click again to close it.");
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
        return controlsButton(bars ? "Sneak bars" : "Classic", bedrock);
    }
    /** 0.2.3: with the style's own label (Classic, Sneak bars or Cycle). */
    public static String controlsButton(String label, boolean bedrock) {
        return " [Wand controls: " + label + (bedrock ? " - tap to change]" : " - click to change]");
    }

    /**
     * 0.2.4: the order of the Wand controls buttons (form and chat). Sneak bars only
     * when allowed (Bedrock). Classic first (the default), Tap bar last, marked for phones.
     */
    public static List<WandStyle> chooserOrder(boolean barsAllowed) {
        List<WandStyle> out = new java.util.ArrayList<>();
        out.add(WandStyle.CLASSIC);
        if (barsAllowed) out.add(WandStyle.BARS);
        out.add(WandStyle.CYCLE);
        out.add(WandStyle.ORIGINAL);
        out.add(WandStyle.TAPBAR);
        return out;
    }

    /** 0.2.4: the name shown on a chooser button. */
    public static String buttonName(WandStyle s, boolean bedrock) {
        if (s == WandStyle.CYCLE) return "Cycle (next spell)";
        if (s == WandStyle.TAPBAR) return bedrock ? "Tap bar (best on phone)" : "Tap bar";
        return s.label();
    }

    /** 0.2.4: one plain line per style, short enough for one line on a phone form button. */
    public static String oneLine(WandStyle s, boolean bedrock) {
        if (bedrock) {
            switch (s) {
                case CLASSIC: return "Hold: next. Tap: cast. Crouch + hold: list";
                case BARS: return "Hold opens your bars, crouch flips";
                case CYCLE: return "Hold: next spell. Crouch + hold: back";
                case ORIGINAL: return "Tap: pick. Hold: cast. Crouch: page";
                default: return "Tap: cast. Hold: read. Crouch: page";
            }
        }
        switch (s) {
            case CLASSIC: return "Right click next, sneak + right click list";
            case BARS: return "Right click opens your bars, sneak flips";
            case CYCLE: return "Right click next, sneak + right click back";
            case ORIGINAL: return "Key picks, left click casts, sneak pages";
            default: return "Key casts, left click reads, sneak pages";
        }
    }

    /** 0.2.4: full button text: name (and "now") on line 1, the one-line description on line 2. */
    public static String button(WandStyle s, WandStyle current, boolean bedrock) {
        return buttonName(s, bedrock) + (s == current ? " (now)" : "") + "\n" + oneLine(s, bedrock);
    }

    /** 0.2.4: the form's top text is ONE short line so nothing is cut off on an iPad or phone. */
    public static String chooserContent(WandStyle current, boolean bedrock, boolean barsAllowed) {
        return "Now: " + buttonName(current, bedrock) + ". Pick one. Switch any time.";
    }

    /** Chat chooser heading when no form can be sent. */
    public static String chooserHeading(String current, boolean bedrock) {
        return "Wand controls (now: " + current + "). " + (bedrock ? "Tap one" : "Click one") + ", or type /wandstyle and its name:";
    }
}
