package com.winkreyesvalley.excelmagiclevel.core;

import java.util.Map;

/**
 * ExcelMagicLevel — per-spell XP weights (SPEC §4.1). Lookup order: full
 * spell key (e.g. {@code greaterheal|3}) → base key ({@code greaterheal}) →
 * the configured default. The table lives in OUR config, seeded once by hand
 * from spells.yml's {@code earns:} values — a Magic update or a MagicGate
 * stage swap can never silently change XP values.
 */
public final class WeightTable {
    private final Map<String, Integer> weights;
    private final int defaultWeight;

    public WeightTable(Map<String, Integer> weights, int defaultWeight) {
        this.weights = Map.copyOf(weights);
        this.defaultWeight = defaultWeight;
    }

    public int weightFor(String spellKey) {
        Integer exact = weights.get(spellKey);
        if (exact != null) return exact;
        String base = baseKey(spellKey);
        if (!base.equals(spellKey)) {
            Integer byBase = weights.get(base);
            if (byBase != null) return byBase;
        }
        return defaultWeight;
    }

    /** Magic spell keys are {@code base} or {@code base|rank} (SpellKey). */
    public static String baseKey(String spellKey) {
        int bar = spellKey.indexOf('|');
        return bar < 0 ? spellKey : spellKey.substring(0, bar);
    }
}
