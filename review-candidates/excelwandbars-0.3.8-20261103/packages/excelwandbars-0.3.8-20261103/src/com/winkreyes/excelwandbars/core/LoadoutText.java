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
    /** 0.3.5: appended under the announce lines when the sync also badged new spells. */
    public static final String NEW_POINTER = "Find them under New spells in the Spell Book.";

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

    /** 0.3.7: positions are named by bar and slot (BarPosition), never the flattened index. */
    public static String slotButton(int slot, String name, int perPage) { return fit(BarPosition.format(slot, perPage) + ": " + (name == null ? EMPTY : name)); }

    public static String slotTitle(int slot, String name, int perPage) { return fit(BarPosition.format(slot, perPage) + ": " + (name == null ? EMPTY : name)); }

    public static String moveButton(String name) { return fit("Move " + name + " to another slot"); }
    public static String offButton(String name) { return fit("Take " + name + " off the bar"); }

    public static String pickSpellButton(String name, int slotOnBar, int perPage) {
        return fit(slotOnBar >= 0 ? name + " (" + BarPosition.format(slotOnBar, perPage) + ")" : name);
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

    /** "Bar 1 has slots 1 to 7, bar 2 slots 1 to 7." (0.3.7: bars and slots, no flattened ranges). */
    public static String pagesLine(int perPage, int size) {
        int pp = Math.max(1, perPage);
        int pages = (size + pp - 1) / pp;
        if (pages <= 1) return "All " + size + " slots are on bar 1.";
        if (pages > 3) return pp + " slots per bar, " + pages + " bars.";
        StringBuilder sb = new StringBuilder("Bar 1 has slots 1 to " + Math.min(pp, size));
        for (int p = 1; p < pages; p++) sb.append(", bar ").append(p + 1).append(" slots 1 to ").append(Math.min((p + 1) * pp, size) - p * pp);
        return sb.append(".").toString();
    }

    public static String pickSlotContent(String name) {
        return "Pick a slot for " + name + ". A spell already there goes back to your Spellbook. If " + name + " is on your bar now, the two swap.";
    }

    public static String pickSpellContent(int slot, int perPage) {
        return "Pick the spell for " + BarPosition.format(slot, perPage) + ". A spell already on your bar swaps places.";
    }

    // ---------------------------------------------------------------- results

    public static String changed(BarLoadout.Change c, Map<String, String> names, int perPage) {
        String a = c.spell == null ? "" : names.getOrDefault(c.spell, c.spell);
        String b = c.other == null ? "" : names.getOrDefault(c.other, c.other);
        switch (c.kind) {
            case "put": return c.other == null ? a + " is in " + BarPosition.format(c.slot, perPage) + " now."
                    : a + " is in " + BarPosition.format(c.slot, perPage) + " now. " + b + " went back to your Spellbook.";
            case "move": return a + " moved to " + BarPosition.format(c.slot, perPage) + ".";
            case "swap": return a + " is in " + BarPosition.format(c.slot, perPage) + ", " + b + " is in " + BarPosition.format(c.from, perPage) + ".";
            case "remove": return a + " is off your bar. It stays in your Spellbook.";
            case "same": return "No change.";
            default: return "That did not work. Open /spellbar and try again.";
        }
    }

    /** 0.3.7 drag-write action-bar confirmations (short by design: they ride the action bar). */
    public static String dragPutLine(String name, int slot, int perPage) {
        return BarPosition.format(slot, perPage) + ": " + name;
    }

    public static String dragOffLine(String name) {
        return name + " off your bar";
    }

    public static final String NOT_KNOWN = "Your wand does not know that spell.";
    public static final String NO_WAND = "No wand found. Keep your wand in your inventory, then try /spellbar again.";
    public static final String NOT_BAR_STYLE = "Your wand style has no spell bar. Pick Tap bar with /wandstyle to use one.";
    public static final String OFF = "Spell bars are not turned on here.";

    // ---------------------------------------------------------------- 0.3.5 arrange + new tab

    public static final String FIRST_FREE = "First free slot";
    public static final String ARRANGE_BUTTON = "Arrange by dragging";

    /** The arrange mode's one instruction line (wording per platform, design 2.2). */
    public static String arrangeHint(boolean bedrock) {
        return bedrock ? "Arrange is on. Tap a spell on the bottom row, then tap below it to take it off your bar. It stays in your Spellbook."
                : "Arrange is on. Drag spells off the bottom row to take them off your bar. They stay in your Spellbook.";
    }

    /** The acknowledgement result line. */
    public static String gotIt(String name) { return name + " marked as seen."; }

    // ---------------------------------------------------------------- 0.3.6 honest drags + exit sweep

    /** An unarmed drag on a spell icon does not save; this points at the path that does. */
    public static final String DRAG_SIGNPOST = "That will not stick. To set your spell order: /spellbar, then Arrange by dragging.";

    /**
     * The exit sweep's one chat line after a bar open (null when nothing moved).
     * fullNames = one entry per stack moved whole; partialName = a stack only
     * part of which fit (may be null); leftBehind = anything is still in the bar.
     */
    public static String sweptLine(java.util.List<String> fullNames, String partialName, boolean leftBehind) {
        boolean anyFull = fullNames != null && !fullNames.isEmpty();
        if (!anyFull && partialName == null) return null;
        if (!anyFull) return "Moved part of the " + partialName + " out of your spell bar into your inventory. The rest stayed in the bar.";
        String base = fullNames.size() == 1
                ? "Moved " + fullNames.get(0) + " out of your spell bar. It is in your inventory."
                : "Moved " + fullNames.size() + " items out of your spell bar. They are in your inventory.";
        return leftBehind ? base + " Some would not fit and stayed in the bar." : base;
    }

    /** Crouch on a one-page bar (replaces 0.2.8's "all N of your spells are on the bar"). */
    public static String onePage(int onBar, int total) {
        return onBar >= total ? "Page 1/1: all " + total + " of your spells are on the bar"
                : "Page 1/1: " + onBar + " of your " + total + " spells. More: /spellbar";
    }
}

