package com.winkreyes.excelwhatsnew.core;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Paging between news entries (0.1.1) and By-date navigation (0.1.2).
 * Entry index 0 is the newest; the player types /whatsnew N where N = index + 1.
 * Pure logic so the tests can check the button order and the callback mapping
 * without a server.
 *
 * Two browsing modes share the entry view:
 * - GLOBAL: Older/Newer walk the show-count newest entries; the view carries a
 *   By date button that opens the months list.
 * - DAY: the walk runs OLDEST to NEWEST inside one day (0.1.3, Mike Nov 14,
 *   2026): position 1 is the day's oldest entry, position M its newest.
 *   "Newer news" advances the walk (toward the newest), "Older news" steps
 *   back. Every day page shows its position as N/M. The view carries a Back
 *   button that returns to the days list of that entry's month.
 */
public final class Nav {
    private Nav() { }

    /** What a button does. */
    public enum Act { OK, NEWER, OLDER, CLOSE, BYDATE, BACK }

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
     * for OK), Newer news, Older news, By date, Close (only when there is no OK button,
     * same as 0.1.0 where the first button was OK or Close). The clicked button id is
     * the index into this list. 0.1.2 adds By date after Older news.
     */
    public static List<Act> formButtons(boolean pending, int idx, int shown) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (hasNewer(idx)) l.add(Act.NEWER);
        if (hasOlder(idx, shown)) l.add(Act.OLDER);
        l.add(Act.BYDATE);
        if (!pending) l.add(Act.CLOSE);
        return l;
    }

    /** Java clickable chat row, left to right: [OK] [Newer news] [Older news] [By date]. Chat needs no Close. */
    public static List<Act> javaButtons(boolean pending, int idx, int shown) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (hasNewer(idx)) l.add(Act.NEWER);
        if (hasOlder(idx, shown)) l.add(Act.OLDER);
        l.add(Act.BYDATE);
        return l;
    }

    // ------------------------------------------------ day walk (0.1.3)
    // Inside a day, entries walk OLDEST first: idxInDay 0 is the day's oldest
    // entry (position 1) and idxInDay daySize-1 its newest (position daySize).
    // These are NOT the global hasNewer/hasOlder: in the day walk, "newer"
    // means later in the walk.

    /** In the day walk there is a newer entry after this one. */
    public static boolean dayHasNewer(int idxInDay, int daySize) { return idxInDay + 1 < daySize; }

    /** In the day walk there is an older entry before this one. */
    public static boolean dayHasOlder(int idxInDay) { return idxInDay > 0; }

    /** The /whatsnew bydate <day> N of the next newer entry in the walk (walk numbers are 1-based). */
    public static int dayNewerWalkNumber(int idxInDay) { return idxInDay + 2; }

    /** The /whatsnew bydate <day> N of the next older entry in the walk. */
    public static int dayOlderWalkNumber(int idxInDay) { return idxInDay; }

    /** The day-walk position label, e.g. "1/10" for the first (oldest) of ten. */
    public static String dayPosition(int idxInDay, int daySize) { return (idxInDay + 1) + "/" + daySize; }

    /**
     * Bedrock form buttons for an entry opened from a day: OK, Newer news, Older news
     * (both bounded to the day walk), Back (to the days list), Close (only when there is
     * no OK button). idx is the position inside the day (0 = oldest), daySize the day's entries.
     */
    public static List<Act> formDayButtons(boolean pending, int idx, int daySize) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (dayHasNewer(idx, daySize)) l.add(Act.NEWER);
        if (dayHasOlder(idx)) l.add(Act.OLDER);
        l.add(Act.BACK);
        if (!pending) l.add(Act.CLOSE);
        return l;
    }

    /** Java clickable chat row for a day entry: [OK] [Newer news] [Older news] [Back]. */
    public static List<Act> javaDayButtons(boolean pending, int idx, int daySize) {
        List<Act> l = new ArrayList<>();
        if (pending) l.add(Act.OK);
        if (dayHasNewer(idx, daySize)) l.add(Act.NEWER);
        if (dayHasOlder(idx)) l.add(Act.OLDER);
        l.add(Act.BACK);
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

    /**
     * Typed fallback line for a day entry (Bedrock when the form could not open).
     * Leads with the day-walk position N/M (0.1.3: every day page shows it).
     * Newer/Older use the entry's global /whatsnew number, so the form and the chat
     * never disagree about which entry is which; the last part is the way back to
     * the day. Always non-empty: the position and the Back part are always there.
     */
    public static String typedDayHint(boolean pending, int globalIdx, int idxInDay, int daySize, LocalDate day) {
        List<String> parts = new ArrayList<>();
        if (pending) parts.add("Type &e/whatsnew ok &7to hide this.");
        if (dayHasNewer(idxInDay, daySize)) parts.add("Newer news: &e/whatsnew " + newerNumber(globalIdx) + "&7.");
        if (dayHasOlder(idxInDay)) parts.add("Older news: &e/whatsnew " + olderNumber(globalIdx) + "&7.");
        parts.add("Back to " + ByDate.dayLabel(day) + ": &e/whatsnew bydate " + day + "&7.");
        return "&7" + ByDate.dayLabel(day) + " " + dayPosition(idxInDay, daySize) + ". " + String.join(" ", parts);
    }

    /** Bedrock hint under /whatsnew list ('&' colors), from the real number of entries shown. Empty = no line. */
    public static String listHint(int shown) {
        if (shown <= 1) return "";
        if (shown == 2) return "&7Type &e/whatsnew 2 &7to read the older one.";
        return "&7Type &e/whatsnew 2 &7to &e/whatsnew " + shown + " &7to read an older one.";
    }
}
