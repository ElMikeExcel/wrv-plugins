package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 0.3.0: the Spell Book groups (Healing, Defense, ...), read from plugins/ExcelWandBars/spellbook.yml.
 * Pure (no Bukkit): the plugin hands in the parsed YAML as plain maps and lists.
 * A spell in no group lands in "Other" (shown last) and is listed once in a boot warning.
 * A spell listed in two groups stays in the first one (also warned).
 */
public final class SpellbookGroups {
    public static final String OTHER_KEY = "other";
    public static final String DEFAULT_OTHER_NAME = "Other";

    public static final class Group {
        public final String key, name;
        public final List<String> spells;
        Group(String key, String name, List<String> spells) { this.key = key; this.name = name; this.spells = Collections.unmodifiableList(spells); }
    }

    private final List<Group> groups = new ArrayList<>();
    private final Map<String, String> spellToGroup = new LinkedHashMap<>();
    private final List<String> duplicates = new ArrayList<>();
    private final List<String> problems = new ArrayList<>();
    private String otherName = DEFAULT_OTHER_NAME;

    public List<Group> groups() { return Collections.unmodifiableList(groups); }
    public String otherName() { return otherName; }
    /** Spells listed in more than one group ("key (group, ignored in other)"). */
    public List<String> duplicates() { return Collections.unmodifiableList(duplicates); }
    /** File problems (bad group, empty file). Empty when fine. */
    public List<String> problems() { return Collections.unmodifiableList(problems); }
    public int spellCount() { return spellToGroup.size(); }

    static String norm(String s) {
        if (s == null) return null;
        String k = s.trim().toLowerCase(Locale.ROOT);
        int bar = k.indexOf('|');
        if (bar >= 0) k = k.substring(0, bar);
        return k.isEmpty() ? null : k;
    }

    /** The group key of a spell (base key, any "|level" dropped), or OTHER_KEY. */
    public String groupOf(String spell) {
        String k = norm(spell);
        String g = k == null ? null : spellToGroup.get(k);
        return g == null ? OTHER_KEY : g;
    }

    /** Display name of a group key ("Other" for OTHER_KEY or an unknown key). */
    public String nameOf(String groupKey) {
        if (groupKey != null) for (Group g : groups) if (g.key.equalsIgnoreCase(groupKey)) return g.name;
        return otherName;
    }

    /** A group key from what a player typed ("healing", "Healing", "other"); null when none matches. */
    public String find(String typed) {
        if (typed == null) return null;
        String t = typed.trim();
        for (Group g : groups) if (g.key.equalsIgnoreCase(t) || g.name.equalsIgnoreCase(t)) return g.key;
        if (t.equalsIgnoreCase(OTHER_KEY) || t.equalsIgnoreCase(otherName)) return OTHER_KEY;
        return null;
    }

    /** Group keys in display order, Other last. */
    public List<String> order() {
        List<String> out = new ArrayList<>();
        for (Group g : groups) out.add(g.key);
        out.add(OTHER_KEY);
        return out;
    }

    /** Spells (base keys) that are not in any group, in the order given, no repeats. */
    public List<String> unlisted(Collection<String> spells) {
        List<String> out = new ArrayList<>();
        if (spells == null) return out;
        for (String s : spells) {
            String k = norm(s);
            if (k != null && !spellToGroup.containsKey(k) && !out.contains(k)) out.add(k);
        }
        return out;
    }

    /**
     * Parse spellbook.yml (as plain maps). Shape:
     * <pre>
     * categories:
     *   healing: { name: Healing, spells: [heal, mend] }
     * other-name: Other
     * </pre>
     * Never throws; a broken file gives no groups (every spell in Other) plus a line in problems().
     */
    @SuppressWarnings("unchecked")
    public static SpellbookGroups parse(Map<String, Object> root) {
        SpellbookGroups g = new SpellbookGroups();
        if (root == null) { g.problems.add("spellbook.yml is empty"); return g; }
        Object on = root.get("other-name");
        if (on instanceof String s && !s.isBlank()) g.otherName = s.trim();
        Object cats = root.get("categories");
        if (!(cats instanceof Map<?, ?> cm)) { g.problems.add("spellbook.yml has no categories section"); return g; }
        for (Map.Entry<?, ?> e : cm.entrySet()) {
            String key = String.valueOf(e.getKey()).trim().toLowerCase(Locale.ROOT);
            if (key.isEmpty() || key.equals(OTHER_KEY) || key.equals("all") || key.equals("group") || key.equals("locked")) { g.problems.add("group key '" + key + "' is not allowed"); continue; }
            String name = key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
            List<String> spells = new ArrayList<>();
            Object v = e.getValue();
            if (v instanceof Map<?, ?> vm) {
                Object n = vm.get("name");
                if (n instanceof String s && !s.isBlank()) name = s.trim();
                Object sl = vm.get("spells");
                if (sl instanceof List<?> l) for (Object o : l) { String k = norm(o == null ? null : String.valueOf(o)); if (k != null) spells.add(k); }
                else if (sl != null) g.problems.add("group '" + key + "': spells is not a list");
            } else { g.problems.add("group '" + key + "' is not a section"); continue; }
            List<String> kept = new ArrayList<>();
            for (String k : spells) {
                String had = g.spellToGroup.get(k);
                if (had != null) { if (!had.equals(key) || kept.contains(k)) g.duplicates.add(k + " (" + had + ", ignored in " + key + ")"); continue; }
                g.spellToGroup.put(k, key);
                kept.add(k);
            }
            g.groups.add(new Group(key, name, kept));
        }
        if (g.groups.isEmpty()) g.problems.add("spellbook.yml has no groups");
        return g;
    }
}

