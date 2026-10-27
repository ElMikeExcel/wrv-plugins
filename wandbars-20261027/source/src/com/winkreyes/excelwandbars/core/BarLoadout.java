package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 0.2.9: a player's own spell bar (their "loadout"). Pure (no Bukkit/Magic types),
 * unit-tested on the plain JVM.
 *
 * A loadout is a fixed number of bar slots (default 21 = 3 pages of 7), each holding
 * one spell key or nothing, plus the set of spells this player has already been
 * shown ("seen"). Every spell the wand knows that is not on the bar is in the
 * player's Spellbook (Magic keeps it on the wand, parked after the last bar page).
 *
 *  - First load: seeded from the wand's current spell order (the first N), and every
 *    spell the wand knows counts as seen, so nobody gets "new spell" lines for
 *    spells they already had.
 *  - New spells (known by the wand, not seen yet) go into the first EMPTY slots;
 *    when the bar is full they stay in the Spellbook. Either way they become seen.
 *  - A slot holding a spell the held wand does not know is skipped on that wand
 *    (shown empty, never cast) and keeps its place for a wand that does know it.
 *  - Manual changes: put (with swap), remove, swap.
 *
 * Keys are stored as Magic base keys, lower case ("mend", never "mend|3").
 */
public final class BarLoadout {
    public static final int MIN_SLOTS = 1, MAX_SLOTS = 64;
    /** Default bar size: 21 = 3 pages of 7 (Mike, Oct 26, 2026). Used when config.yml has no bar-loadout.slots. */
    public static final int DEFAULT_SLOTS = 21;

    private final List<String> slots = new ArrayList<>();
    private final Set<String> seen = new LinkedHashSet<>();

    public BarLoadout(int size) { resize(size); }

    public static String norm(String key) {
        if (key == null) return null;
        String k = key.trim();
        int i = k.indexOf('|');
        if (i >= 0) k = k.substring(0, i);
        k = k.toLowerCase(Locale.ROOT);
        return k.isEmpty() ? null : k;
    }

    public static int clampSize(int n) { return Math.max(MIN_SLOTS, Math.min(MAX_SLOTS, n)); }

    public int size() { return slots.size(); }
    public String get(int i) { return i >= 0 && i < slots.size() ? slots.get(i) : null; }
    public List<String> slots() { return java.util.Collections.unmodifiableList(slots); }
    public Set<String> seen() { return java.util.Collections.unmodifiableSet(seen); }
    public boolean seenAny() { return !seen.isEmpty(); }

    /** Grow with empty slots, or cut from the end (cut spells go back to the Spellbook; still known). */
    public void resize(int size) {
        int n = clampSize(size);
        while (slots.size() < n) slots.add(null);
        while (slots.size() > n) slots.remove(slots.size() - 1);
    }

    /** Slot of this spell on the bar, or -1. */
    public int indexOf(String key) {
        String k = norm(key);
        if (k == null) return -1;
        for (int i = 0; i < slots.size(); i++) if (k.equals(slots.get(i))) return i;
        return -1;
    }

    public int firstEmpty() {
        for (int i = 0; i < slots.size(); i++) if (slots.get(i) == null) return i;
        return -1;
    }

    public int filled() { int n = 0; for (String s : slots) if (s != null) n++; return n; }

    /** First load: the first N spells in the wand's current order; everything the wand knows is seen. */
    public static BarLoadout seed(List<String> wandOrder, int size) {
        BarLoadout l = new BarLoadout(size);
        int i = 0;
        for (String s : wandOrder) {
            String k = norm(s);
            if (k == null || !l.seen.add(k)) continue;
            if (i < l.slots.size()) l.slots.set(i++, k);
        }
        return l;
    }

    /** Restore from storage. Unknown junk (blank, duplicates) is dropped. */
    public static BarLoadout restore(List<String> storedSlots, Collection<String> storedSeen, int size) {
        BarLoadout l = new BarLoadout(size);
        Set<String> used = new java.util.HashSet<>();
        if (storedSlots != null) {
            for (int i = 0; i < storedSlots.size() && i < l.slots.size(); i++) {
                String k = norm(storedSlots.get(i));
                if (k != null && used.add(k)) l.slots.set(i, k);
            }
        }
        if (storedSeen != null) for (String s : storedSeen) { String k = norm(s); if (k != null) l.seen.add(k); }
        for (String s : l.slots) if (s != null) l.seen.add(s);
        return l;
    }

    /** Storage form: one entry per slot, "" for empty. */
    public List<String> storedSlots() {
        List<String> out = new ArrayList<>();
        for (String s : slots) out.add(s == null ? "" : s);
        return out;
    }

    /** What sync did: new spells put on the bar (slot index), and new spells left in the Spellbook. */
    public static final class Sync {
        public final Map<String, Integer> placed = new LinkedHashMap<>();
        public final List<String> toBook = new ArrayList<>();
        public boolean changed() { return !placed.isEmpty() || !toBook.isEmpty(); }
        public int count() { return placed.size() + toBook.size(); }
    }

    /**
     * The held wand's spells, in the order new ones should be placed (template order first).
     * Spells not seen yet go into the first empty slots, the rest to the Spellbook.
     */
    public Sync sync(List<String> wandOrder) {
        Sync r = new Sync();
        for (String s : wandOrder) {
            String k = norm(s);
            if (k == null || seen.contains(k)) continue;
            seen.add(k);
            if (indexOf(k) >= 0) continue; // a manual pick before it was seen (cannot normally happen)
            int e = firstEmpty();
            if (e >= 0) { slots.set(e, k); r.placed.put(k, e); }
            else r.toBook.add(k);
        }
        return r;
    }

    /** Result of a manual change, for the chat line. */
    public static final class Change {
        public final String kind; // put, move, swap, remove, same, bad
        public final String spell, other; public final int slot, from;
        Change(String kind, String spell, String other, int slot, int from) { this.kind = kind; this.spell = spell; this.other = other; this.slot = slot; this.from = from; }
        public boolean ok() { return !kind.equals("bad") && !kind.equals("same"); }
        @Override public String toString() { return kind + " " + spell + " slot " + (slot + 1) + (from >= 0 ? " from " + (from + 1) : "") + (other != null ? " other " + other : ""); }
    }

    /**
     * Put a spell in a slot. On the bar already: the two slots swap. Off the bar: it takes
     * the slot and whatever was there goes back to the Spellbook.
     */
    public Change put(String key, int slot) {
        String k = norm(key);
        if (k == null || slot < 0 || slot >= slots.size()) return new Change("bad", k, null, slot, -1);
        seen.add(k);
        int from = indexOf(k);
        String there = slots.get(slot);
        if (from == slot) return new Change("same", k, null, slot, from);
        if (from >= 0) {
            slots.set(from, there);
            slots.set(slot, k);
            return there == null ? new Change("move", k, null, slot, from) : new Change("swap", k, there, slot, from);
        }
        slots.set(slot, k);
        return new Change("put", k, there, slot, -1);
    }

    /** Take whatever is in this slot off the bar (it stays in the Spellbook). */
    public Change remove(int slot) {
        if (slot < 0 || slot >= slots.size() || slots.get(slot) == null) return new Change("bad", null, null, slot, -1);
        String k = slots.get(slot);
        slots.set(slot, null);
        return new Change("remove", k, null, slot, -1);
    }

    /** Swap two slots (either may be empty). */
    public Change swap(int a, int b) {
        if (a < 0 || b < 0 || a >= slots.size() || b >= slots.size()) return new Change("bad", null, null, b, a);
        if (a == b) return new Change("same", slots.get(a), null, b, a);
        String x = slots.get(a), y = slots.get(b);
        if (x == null && y == null) return new Change("same", null, null, b, a);
        slots.set(a, y); slots.set(b, x);
        if (x == null) return new Change("move", y, null, a, b);
        return y == null ? new Change("move", x, null, b, a) : new Change("swap", x, y, b, a);
    }

    // ------------------------------------------------------------------ Magic layout

    /** The wand's layout: spell key to Magic's global slot (bar page p, slot i = p * 8 + i), and the page count. */
    public static final class Layout {
        public final Map<String, Integer> slots = new LinkedHashMap<>();
        public int pages = 1, onBar = 0, inBook = 0;
    }

    /**
     * The bar for the held wand. Slots whose spell this wand knows are placed at
     * page = i / perPage, column = i % perPage (the 8th column stays empty for Tap bar).
     * Every other spell the wand knows is parked after the last page, in wand order, so
     * Magic keeps it (Spellbook) but never shows it on the bar.
     */
    public Layout layout(List<String> wandOrder, int perPage) {
        int pp = PageLayout.clampPerPage(perPage);
        Set<String> known = new LinkedHashSet<>();
        for (String s : wandOrder) { String k = norm(s); if (k != null) known.add(k); }
        Layout out = new Layout();
        int last = -1;
        for (int i = 0; i < slots.size(); i++) { String k = slots.get(i); if (k != null && known.contains(k)) last = i; }
        out.pages = last < 0 ? 1 : last / pp + 1;
        for (int i = 0; i < slots.size(); i++) {
            String k = slots.get(i);
            if (k == null || !known.contains(k)) continue;
            out.slots.put(k, (i / pp) * PageLayout.SLOTS_PER_BAR + (i % pp));
            out.onBar++;
        }
        int next = out.pages * PageLayout.SLOTS_PER_BAR;
        for (String k : known) {
            if (out.slots.containsKey(k)) continue;
            out.slots.put(k, next++);
            out.inBook++;
        }
        return out;
    }

    /**
     * Placement order for new spells: the wand template's own list first (the curated order),
     * then anything else by its current slot (then name).
     */
    public static List<String> placementOrder(List<String> templateOrder, Map<String, Integer> current, Collection<String> wandSpells) {
        Set<String> known = new LinkedHashSet<>();
        for (String s : wandSpells) { String k = norm(s); if (k != null) known.add(k); }
        List<String> out = new ArrayList<>();
        Set<String> added = new java.util.HashSet<>();
        if (templateOrder != null) for (String s : templateOrder) { String k = norm(s); if (k != null && known.contains(k) && added.add(k)) out.add(k); }
        Map<String, Integer> cur = new java.util.HashMap<>();
        if (current != null) for (Map.Entry<String, Integer> e : current.entrySet()) { String k = norm(e.getKey()); if (k != null && e.getValue() != null) cur.put(k, e.getValue()); }
        for (String k : PageLayout.order(cur, known)) if (added.add(k)) out.add(k);
        return out;
    }
}

