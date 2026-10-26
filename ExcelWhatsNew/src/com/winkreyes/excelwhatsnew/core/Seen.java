package com.winkreyes.excelwhatsnew.core;

import java.time.LocalDate;
import java.util.List;

/** Which entry a player should get on join. Pure logic, no Bukkit. */
public final class Seen {
    private Seen() { }

    /**
     * The newest entry the player has not said OK to, or null.
     * okId = the id of the newest entry they pressed OK on (null = never).
     * Entries are newest first; anything above okId in the list is unseen.
     * If okId is no longer in the file, only entries posted after okDate count
     * (okDate null = all entries count). maxAgeDays > 0 skips entries older than that.
     */
    public static Entry pending(List<Entry> entries, String okId, LocalDate okDate, LocalDate today, int maxAgeDays) {
        if (entries.isEmpty()) return null;
        Entry top = entries.get(0);
        if (okId != null) {
            int idx = -1;
            for (int i = 0; i < entries.size(); i++) if (entries.get(i).id.equalsIgnoreCase(okId)) { idx = i; break; }
            if (idx == 0) return null;
            if (idx < 0 && okDate != null && top.posted != null && !top.posted.isAfter(okDate)) return null;
        }
        if (maxAgeDays > 0 && top.posted != null && today != null && top.posted.plusDays(maxAgeDays).isBefore(today)) return null;
        return top;
    }

    /** How many entries above okId (unseen), for the "and N more" line. */
    public static int unseenCount(List<Entry> entries, String okId) {
        if (okId == null) return entries.size();
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).id.equalsIgnoreCase(okId)) return i;
        return entries.size();
    }
}
