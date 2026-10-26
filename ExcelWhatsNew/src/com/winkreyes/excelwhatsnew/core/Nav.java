package com.winkreyes.excelwhatsnew.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Paging between news entries (0.1.1). Entry index 0 is the newest; the player types
 * /whatsnew N where N = index + 1. Pure logic so the tests can check the button
 * order and the callback mapping without a server.
 */
public final class Nav {
    private Nav() { }

    /** What a button does. */
    public enum Act { OK, NEWER, OLDER, CLOSE }

    /** There is a newer entry to page back to. */
    public static boolean hasNewer(int idx) { return idx > 0; }

    /** There is an older entry inside the shown range. */
    public static boolean hasOlder(int idx, int shown) { return idx + 1 < shown; }

    /** The /whatsnew number of the next newer entry (entry idx - 1 is number idx). */
    public static int newerNumber(int idx) { return idx; }

    /** The /whatsnew number of the next older entry. */
    public static int olderNumber(int idx) { return idx + 2; }

    /**
     * Bedrock form buttons, top to bottom: OK (only when this entry is the one waiting
     * for OK), Newer news, Older news, Close (only when there is no OK button, same as
     * 0.1.0 where the first button was OK or Close). The clicked button id is the index
     * into this list.
     */
    public static List<Act> formButtons(boolean pending, int idx, int shown) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (hasNewer(idx)) l.add(Act.NEWER);
        if (hasOlder(idx, shown)) l.add(Act.OLDER);
        if (!pending) l.add(Act.CLOSE);
        return l;
    }

    /** Java clickable chat row, left to right: [OK] [Newer news] [Older news]. Chat needs no Close. */
    public static List<Act> javaButtons(boolean pending, int idx, int shown) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (hasNewer(idx)) l.add(Act.NEWER);
        if (hasOlder(idx, shown)) l.add(Act.OLDER);
        return l;
    }

    /** Clicked form button id to its action, or null when closed (-1) or out of range. */
    public static Act clicked(List<Act> buttons, int clicked) {
        return clicked >= 0 && clicked < buttons.size() ? buttons.get(clicked) : null;
    }

    /**
     * Typed fallback line ('&' colors) for players who cannot click (Bedrock when the form
     * could not open). Mentions both directions when both exist. Empty string = no line.
     */
    public static String typedHint(boolean pending, int idx, int shown) {
        List<String> parts = new ArrayList<>();
        if (pending) parts.add("Type &e/whatsnew ok &7to hide this.");
        if (hasNewer(idx)) parts.add("Newer news: &e/whatsnew " + newerNumber(idx) + "&7.");
        if (hasOlder(idx, shown)) parts.add("Older news: &e/whatsnew " + olderNumber(idx) + "&7.");
        return parts.isEmpty() ? "" : "&7" + String.join(" ", parts);
    }

    /** Bedrock hint under /whatsnew list ('&' colors), from the real number of entries shown. Empty = no line. */
    public static String listHint(int shown) {
        if (shown <= 1) return "";
        if (shown == 2) return "&7Type &e/whatsnew 2 &7to read the older one.";
        return "&7Type &e/whatsnew 2 &7to &e/whatsnew " + shown + " &7to read an older one.";
    }
}
