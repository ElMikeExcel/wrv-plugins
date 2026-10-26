package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 0.2.9: every player-facing line for the spell bar loadout. Plain text, no em dashes.
 * Form buttons and titles stay at or under {@link #MAX} characters (Bedrock forms).
 */
public final class LoadoutText {
    private LoadoutText() {}

    public static final int MAX = 38;

    public static String fit(String s) {
        if (s == null) return "";
        return s.length() <= MAX ? s : s.substring(0, MAX - 3).trim() + "...";
    }

    // ---------------------------------------------------------------- new spell lines

    public static String newOnBar(String name) { return "New spell: " + name + " added to your bar"; }
    public static String newInBook(String name) { return "New spell: " + name + " is in your Spellbook"; }
    public static String more(int n) { return n + " more new spell" + (n == 1 ? "" : "s") + ". See /spellbook"; }
    public static final String HOW_TO_CHANGE = "Change your bar: /spellbar";

    /** The chat lines for one sync: at most max named lines, then one "N more" line. */
    public static List<String> announce(List<String> placedNames, List<String> bookNames, int max) {
        List<String> out = new ArrayList<>();
        int cap = Math.max(1, max), shown = 0, total = placedNames.size() + bookNames.size();
        for (String n : placedNames) { if (shown >= cap) break; out.add(newOnBar(n)); shown++; }
        for (String n : bookNames) { if (shown >= cap) break; out.add(newInBook(n)); shown++; }
        if (total > shown) out.add(more(total - shown));
        if (!bookNames.isEmpty()) out.add(HOW_TO_CHANGE);
        return out;
    }

    // ---------------------------------------------------------------- menus

    public static final String BAR_TITLE = "Your spell bar";
    public static final String BOOK_BUTTON = "My spell bar (change it)";
    public static final String PUT_BUTTON = "Put on my bar";
    public static final String MOVE_BUTTON = "Move on my bar";
    public static final String OFF_BUTTON = "Take off my bar";
    public static final String PICK_BUTTON = "Pick a spell for this slot";
    public static final String BACK_BAR = "Back to my bar";
    public static final String BACK_BOOK = "Back to Spell Book";
    public static final String CLOSE = "Close";
    public static final String EMPTY = "(empty)";

    public static String barTitleCount(int used, int size) { return fit("Your spell bar (" + used + " of " + size + ")"); }

    public static String slotButton(int slot, String name) { return fit((slot + 1) + ": " + (name == null ? EMPTY : name)); }

    public static String slotTitle(int slot, String name) { return fit("Slot " + (slot + 1) + ": " + (name == null ? EMPTY : name)); }

    public static String moveButton(String name) { return fit("Move " + name + " to another slot"); }
    public static String offButton(String name) { return fit("Take " + name + " off the bar"); }

    public static String pickSpellButton(String name, int slotOnBar) {
        return fit(slotOnBar >= 0 ? name + " (slot " + (slotOnBar + 1) + ")" : name);
    }

    public static String pickTitle(String name) { return fit("Where should " + name + " go?"); }

    public static String barContent(boolean bedrock, int perPage, int size, int inBook) {
        StringBuilder sb = new StringBuilder();
        sb.append(bedrock ? "Tap a slot to change it." : "Click a slot to change it.");
        sb.append(" ").append(pagesLine(perPage, size));
        sb.append("\nNew spells go into empty slots by themselves.");
        if (inBook > 0) sb.append("\n").append(inBook).append(inBook == 1 ? " spell is" : " spells are").append(" only in your Spellbook.");
        return sb.toString();
    }

    /** "Slots 1 to 7 are page 1, 8 to 14 page 2." (up to 3 pages named, then "7 slots per page"). */
    public static String pagesLine(int perPage, int size) {
        int pp = Math.max(1, perPage);
        int pages = (size + pp - 1) / pp;
        if (pages <= 1) return "All " + size + " slots are on page 1.";
        if (pages > 3) return pp + " slots per page, " + pages + " pages.";
        StringBuilder sb = new StringBuilder("Slots 1 to " + Math.min(pp, size) + " are page 1");
        for (int p = 1; p < pages; p++) sb.append(", ").append(p * pp + 1).append(" to ").append(Math.min((p + 1) * pp, size)).append(" page ").append(p + 1);
        return sb.append(".").toString();
    }

    public static String pickSlotContent(String name) {
        return "Pick a slot for " + name + ". A spell already there goes back to your Spellbook. If " + name + " is on your bar now, the two swap.";
    }

    public static String pickSpellContent(int slot) {
        return "Pick the spell for slot " + (slot + 1) + ". A spell already on your bar swaps places.";
    }

    // ---------------------------------------------------------------- results

    public static String changed(BarLoadout.Change c, Map<String, String> names) {
        String a = c.spell == null ? "" : names.getOrDefault(c.spell, c.spell);
        String b = c.other == null ? "" : names.getOrDefault(c.other, c.other);
        switch (c.kind) {
            case "put": return c.other == null ? a + " is in slot " + (c.slot + 1) + " now."
                    : a + " is in slot " + (c.slot + 1) + " now. " + b + " went back to your Spellbook.";
            case "move": return a + " moved to slot " + (c.slot + 1) + ".";
            case "swap": return a + " is in slot " + (c.slot + 1) + ", " + b + " is in slot " + (c.from + 1) + ".";
            case "remove": return a + " is off your bar. It stays in your Spellbook.";
            case "same": return "No change.";
            default: return "That did not work. Open /spellbar and try again.";
        }
    }

    public static final String NOT_KNOWN = "Your wand does not know that spell.";
    public static final String NO_WAND = "No wand found. Keep your wand in your inventory, then try /spellbar again.";
    public static final String NOT_BAR_STYLE = "Your wand style has no spell bar. Pick Tap bar with /wandstyle to use one.";
    public static final String OFF = "Spell bars are not turned on here.";

    /** Crouch on a one-page bar (replaces 0.2.8's "all N of your spells are on the bar"). */
    public static String onePage(int onBar, int total) {
        return onBar >= total ? "Page 1/1: all " + total + " of your spells are on the bar"
                : "Page 1/1: " + onBar + " of your " + total + " spells. More: /spellbar";
    }
}
