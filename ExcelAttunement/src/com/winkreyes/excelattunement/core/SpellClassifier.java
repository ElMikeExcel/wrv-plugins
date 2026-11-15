package com.winkreyes.excelattunement.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Spell key -> SpellClass, from the generated table (spell-classes.txt).
 *
 * The table covers every spell the server can ever issue: Magic runs
 * with load_default_spells off and the MagicGate stage configs are the
 * complete spell universe (100 keys at build time). A key NOT in the
 * table can therefore only appear if a future stage ships without
 * regenerating the table. Default for an unknown key is COMBAT —
 * fail-closed for the anti-weave lock: an unclassified cast still
 * commits the caster, and an unclassified cast is still blocked while
 * martial (martial blocks every cast regardless of class). The plugin
 * logs a warning the first time it sees an unknown key so the table
 * gets regenerated.
 *
 * Leveled spell instances carry keys like "heal|3" in Magic's data;
 * the level suffix is stripped before lookup because a spell's class
 * does not change with its level.
 */
public final class SpellClassifier {
    private final Map<String, SpellClass> table;

    public SpellClassifier(Map<String, SpellClass> table) {
        this.table = Collections.unmodifiableMap(new LinkedHashMap<>(table));
    }

    /** Parse "key = CLASS" lines; '#' comments and blank lines are skipped. */
    public static SpellClassifier fromLines(List<String> lines) {
        Map<String, SpellClass> map = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            int eq = line.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("bad spell-classes line: " + raw);
            String key = line.substring(0, eq).trim().toLowerCase();
            String value = line.substring(eq + 1).trim().toUpperCase();
            if (key.isEmpty()) throw new IllegalArgumentException("empty spell key: " + raw);
            SpellClass cls;
            try {
                cls = SpellClass.valueOf(value);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("bad spell class in line: " + raw);
            }
            if (map.put(key, cls) != null) {
                throw new IllegalArgumentException("duplicate spell key: " + key);
            }
        }
        return new SpellClassifier(map);
    }

    /** Strip a Magic level suffix ("heal|3" -> "heal") and normalize case. */
    public static String normalizeKey(String key) {
        if (key == null) return "";
        String k = key.trim().toLowerCase();
        int bar = k.indexOf('|');
        if (bar >= 0) k = k.substring(0, bar);
        return k;
    }

    public boolean isKnown(String key) {
        return table.containsKey(normalizeKey(key));
    }

    /** Class of a spell key; unknown keys fail closed as COMBAT. */
    public SpellClass classify(String key) {
        SpellClass cls = table.get(normalizeKey(key));
        return cls != null ? cls : SpellClass.COMBAT;
    }

    public int size() {
        return table.size();
    }

    public Map<String, SpellClass> table() {
        return table;
    }
}
