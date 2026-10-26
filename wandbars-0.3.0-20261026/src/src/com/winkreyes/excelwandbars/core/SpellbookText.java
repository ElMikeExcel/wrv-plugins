package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 0.3.0: the organized Spell Book. Pure model and every player-facing line (no Bukkit), so the
 * grouping, sorting, paging and text limits are unit tested. Plain text, no em dashes. Bedrock
 * form titles and buttons stay at or under {@link #MAX} visible characters, and Bedrock text never
 * says "click". Java chat pages hold {@link #JAVA_PER_PAGE} spells, Bedrock forms {@link #BEDROCK_PER_PAGE}.
 */
public final class SpellbookText {
    private SpellbookText() {}

    public static final int MAX = 38;
    public static final int JAVA_PER_PAGE = 10;
    public static final int BEDROCK_PER_PAGE = 12;
    /** Band of a spell that is on no rank wand (sorts after every rank). */
    public static final int NO_BAND = 99;
    /** Pseudo group key for the A to Z list. */
    public static final String ALL = "all";
    /** Bedrock form grey (locked spells). Not counted in the 38 characters. */
    public static final String GREY = "\u00a77";

    /** One spell line in the book. */
    public static final class Entry {
        public String key = "", name = "";
        /** "Rank 2 of 3" (empty for a locked spell). */
        public String rankText = "";
        /** Rank band index (0 = Beginner) of the lowest rank wand that has it; NO_BAND if none. */
        public int band = NO_BAND;
        public String bandName = "";
        /** On a higher rank's wand (this seal), not on the player's wand yet. */
        public boolean locked = false;
        /** 0-based slot on the player's own bar, -1 = not on it (or no own bar). */
        public int barSlot = -1;
        /** 1-based Magic hotbar page when the own bar is off (0.2.8 paging), 0 = none. */
        public int page = 0;
        public boolean active = false;

        public Entry() {}
        public Entry(String key, String name, int band, String bandName, boolean locked) {
            this.key = key; this.name = name; this.band = band; this.bandName = bandName; this.locked = locked;
        }
        public Entry bar(int slot) { this.barSlot = slot; return this; }
        public Entry rank(String r) { this.rankText = r == null ? "" : r; return this; }
    }

    /** A group button: known spells plus locked ones. */
    public static final class GroupCount {
        public final String key, name;
        public final int known, locked;
        GroupCount(String key, String name, int known, int locked) { this.key = key; this.name = name; this.known = known; this.locked = locked; }
        public int total() { return known + locked; }
    }

    // ---------------------------------------------------------------- model

    public static final Comparator<Entry> BY_RANK_THEN_NAME = Comparator
            .comparingInt((Entry e) -> e.band)
            .thenComparing(e -> e.name.toLowerCase(Locale.ROOT))
            .thenComparing(e -> e.key);

    public static final Comparator<Entry> BY_NAME = Comparator
            .comparing((Entry e) -> e.name.toLowerCase(Locale.ROOT))
            .thenComparing(e -> e.key);

    /** Group buttons in display order (file order, Other last). Empty groups are left out. */
    public static List<GroupCount> counts(SpellbookGroups g, List<Entry> entries, boolean withLocked) {
        List<GroupCount> out = new ArrayList<>();
        for (String gk : g.order()) {
            int k = 0, l = 0;
            for (Entry e : entries) {
                if (!g.groupOf(e.key).equals(gk)) continue;
                if (e.locked) { if (withLocked) l++; } else k++;
            }
            if (k + l > 0) out.add(new GroupCount(gk, g.nameOf(gk), k, l));
        }
        return out;
    }

    /** One group's spells, sorted by rank band then name (locked ones by the same rule). ALL = every spell A to Z. */
    public static List<Entry> list(SpellbookGroups g, List<Entry> entries, String groupKey, boolean withLocked) {
        List<Entry> out = new ArrayList<>();
        boolean all = ALL.equals(groupKey);
        for (Entry e : entries) {
            if (e.locked && !withLocked) continue;
            if (all || g.groupOf(e.key).equals(groupKey)) out.add(e);
        }
        out.sort(all ? BY_NAME : BY_RANK_THEN_NAME);
        return out;
    }

    public static int known(List<Entry> entries) { int n = 0; for (Entry e : entries) if (!e.locked) n++; return n; }
    public static int locked(List<Entry> entries) { int n = 0; for (Entry e : entries) if (e.locked) n++; return n; }
    public static int onBar(List<Entry> entries) { int n = 0; for (Entry e : entries) if (!e.locked && e.barSlot >= 0) n++; return n; }

    public static int pages(int count, int perPage) { int pp = Math.max(1, perPage); return Math.max(1, (count + pp - 1) / pp); }

    /** 0-based page, clamped to the pages there are. */
    public static int clampPage(int page, int count, int perPage) { return Math.max(0, Math.min(page, pages(count, perPage) - 1)); }

    public static <T> List<T> page(List<T> all, int page, int perPage) {
        int pp = Math.max(1, perPage);
        int p = clampPage(page, all.size(), pp);
        int from = p * pp, to = Math.min(all.size(), from + pp);
        return from >= to ? new ArrayList<>() : new ArrayList<>(all.subList(from, to));
    }

    // ---------------------------------------------------------------- text helpers

    /** Visible length (formatting codes do not count). */
    public static int visible(String s) { return s == null ? 0 : s.replaceAll("\u00a7.", "").length(); }

    public static String fit(String s) { return LoadoutText.fit(s); }

    static String plural(int n, String one, String many) { return n + " " + (n == 1 ? one : many); }

    // ---------------------------------------------------------------- home

    public static final String HOME_TITLE = "Spell Book";
    public static final String ALL_BUTTON = "All spells A to Z";
    public static final String ALL_JAVA = "All A to Z";
    public static final String BAR_BUTTON = "Put spells on my bar";
    public static final String CLOSE = "Close";
    public static final String NEXT = "Next page";
    public static final String PREV = "Previous page";
    public static final String GROUPS = "Back to groups";
    public static final String JAVA_PREV = "< Prev";
    public static final String JAVA_NEXT = "Next >";
    public static final String JAVA_GROUPS = "Groups";

    /** "Master Wand: 17 spells" (fits a form line). */
    public static String wandLine(String wandName, int known) {
        String tail = ": " + plural(known, "spell", "spells");
        String w = wandName == null || wandName.isBlank() ? "Your wand" : wandName.trim();
        if (w.length() + tail.length() > MAX) w = w.substring(0, Math.max(1, MAX - tail.length() - 3)).trim() + "...";
        return w + tail;
    }

    /** "17 on your bar, 0 in the book." */
    public static String barLine(int onBar, int known) {
        return onBar + " on your bar, " + Math.max(0, known - onBar) + " in the book.";
    }

    /** "3 more unlock at a higher rank." (null when none). */
    public static String lockedLine(int locked) {
        if (locked <= 0) return null;
        return locked == 1 ? "1 more unlocks at a higher rank." : locked + " more unlock at a higher rank.";
    }

    public static final String HOME_HINT_BEDROCK = "Tap a group to see its spells.";
    public static final String HOME_HINT_JAVA = "Click a group to see its spells.";

    /** Bedrock group button: "Healing (7)" or "Healing (4, +3 locked)". */
    public static String groupButton(GroupCount c) {
        String s = c.locked <= 0 ? c.name + " (" + c.known + ")"
                : c.known == 0 ? c.name + " (" + c.locked + " locked)"
                : c.name + " (" + c.known + ", +" + c.locked + " locked)";
        if (s.length() > MAX) s = c.name + " (" + c.total() + ")";
        return fit(s);
    }

    /** Java group button text (inside [ ]): "Healing 7" or "Healing 4 +3". */
    public static String groupJava(GroupCount c) {
        if (c.locked <= 0) return c.name + " " + c.known;
        return c.known == 0 ? c.name + " +" + c.locked : c.name + " " + c.known + " +" + c.locked;
    }

    /** Java hover on a group button. */
    public static String groupHover(GroupCount c) {
        String s = plural(c.known, "spell", "spells") + " on your wand.";
        if (c.locked > 0) s += "\n" + c.locked + " more unlock" + (c.locked == 1 ? "s" : "") + " at a higher rank.";
        return s;
    }

    // ---------------------------------------------------------------- group pages

    /** Bedrock title "Healing (7) page 1/2" (page part only when there is more than one). */
    public static String groupTitle(String name, int count, int page, int pages) {
        String base = name + " (" + count + ")";
        String s = pages > 1 ? base + " page " + (page + 1) + "/" + pages : base;
        if (s.length() <= MAX) return s;
        String pg = pages > 1 ? " " + (page + 1) + "/" + pages : "";
        int room = MAX - pg.length();
        String n = name.length() <= room ? name : name.substring(0, Math.max(1, room - 3)).trim() + "...";
        return n + pg;
    }

    /** Java heading "Healing (7), page 1 of 2". */
    public static String groupHeading(String name, int count, int page, int pages) {
        return name + " (" + count + ")" + (pages > 1 ? ", page " + (page + 1) + " of " + pages : "");
    }

    public static String allName() { return "All spells A to Z"; }

    public static final String LOCKED_HINT = "Gray spells unlock at a higher rank.";

    public static String groupContent(boolean bedrock, boolean anyLocked) {
        String s = bedrock ? "Tap a spell to read it. Reading never casts." : "Hover to read a spell, click for the full page.";
        if (anyLocked) s += "\n" + LOCKED_HINT;
        return s;
    }

    /** " bar 4" / " page 2" / "" */
    static String mark(Entry e) {
        if (e.barSlot >= 0) return " bar " + (e.barSlot + 1);
        if (e.page > 0) return " page " + e.page;
        return "";
    }

    /** Java line text (no limit): "Storm Shackle (Rank 2 of 3) bar 4", locked "Tame: unlocks at Master". */
    public static String javaLine(Entry e) {
        if (e.locked) return e.name + ": unlocks at " + e.bandName;
        String r = e.rankText.isEmpty() ? "" : " (" + e.rankText + ")";
        return e.name + r + mark(e) + (e.active ? " (in use)" : "");
    }

    /**
     * Bedrock button (38 visible characters at most): "Storm Shackle (Rank 2 of 3) - bar 4".
     * Too long: the rank part goes first, then the name is shortened; the bar number always stays.
     * Locked: grey "Tame - unlocks at Master", or "Name (Master)" when that is too long.
     */
    public static String bedrockButton(Entry e) {
        if (e.locked) {
            String s = e.name + " - unlocks at " + e.bandName;
            if (s.length() > MAX) s = e.name + " (" + e.bandName + ")";
            return GREY + fit(s);
        }
        String m = e.barSlot >= 0 ? " - bar " + (e.barSlot + 1) : e.page > 0 ? " - page " + e.page : "";
        String use = e.active ? " (in use)" : "";
        String r = e.rankText.isEmpty() ? "" : " (" + e.rankText + ")";
        String s = e.name + r + m + use;
        if (s.length() <= MAX) return s;
        s = e.name + m + use;
        if (s.length() <= MAX) return s;
        String tail = m + use;
        int room = Math.max(4, MAX - tail.length());
        String n = e.name.length() <= room ? e.name : e.name.substring(0, room - 3).trim() + "...";
        return n + tail;
    }

    /** Hover / form text for a locked spell. */
    public static List<String> lockedLines(Entry e, String description) {
        List<String> out = new ArrayList<>();
        out.add(e.name);
        if (description != null && !description.isBlank()) out.add(description.trim());
        out.add("Unlocks at " + e.bandName + " rank.");
        out.add("Cast spells to earn SP and rank up your wand.");
        return out;
    }

    public static String lockedTitle(Entry e) { return fit(e.name + " (locked)"); }

    /** "Back to Healing" (form button, fits). */
    public static String backTo(String groupName) { return fit("Back to " + groupName); }

    // ---------------------------------------------------------------- bar picker (0.2.9 flow, by group)

    public static String pickGroupContent(int slot) {
        return "Pick the spell for slot " + (slot + 1) + ". Choose a group first.";
    }

    /** Picker list title: "Slot 4: Healing 1/2". */
    public static String pickListTitle(int slot, String groupName, int page, int pages) {
        String pg = pages > 1 ? " " + (page + 1) + "/" + pages : "";
        String head = "Slot " + (slot + 1) + ": ";
        int room = MAX - head.length() - pg.length();
        String n = groupName.length() <= room ? groupName : groupName.substring(0, Math.max(1, room - 3)).trim() + "...";
        return head + n + pg;
    }

    /** Picker group button: only spells the wand knows. */
    public static String pickGroupButton(GroupCount c) { return fit(c.name + " (" + c.known + ")"); }
}
