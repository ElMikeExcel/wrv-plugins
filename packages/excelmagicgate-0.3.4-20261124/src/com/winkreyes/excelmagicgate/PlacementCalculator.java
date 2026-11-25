package com.winkreyes.excelmagicgate;

import java.util.Map;

/**
 * The offline placement calculation (DESIGN.md §2/§8): given one
 * mage's lifetime per-spell cast counts (Magic's .dat `spells:`
 * section, tier keys -&gt; cast_count), compute the CREDITED total
 * under the packet's frozen classification — the credited definition
 * is "every family not in the no-credit set", because placement is a
 * historical sum: effect gates cannot be observed retroactively, so
 * placement credits every historical cast in a credited family at
 * face value (DESIGN.md §8 step 2; the live effect gates bind only
 * casts made after switch-over).
 *
 * Pure logic: the caller (install-window tooling / the unit suite)
 * parses the YAML; this class owns the arithmetic, so the Nov 9
 * fixtures pin the credited definition as a regression net.
 */
public final class PlacementCalculator {

    private PlacementCalculator() {}

    /**
     * Credited total from a spells-section map: tier key -&gt; record
     * map carrying a numeric "cast_count". Malformed records count 0.
     */
    public static long creditedTotal(Map<String, ?> spellsSection, CreditPolicy policy) {
        if (spellsSection == null || policy == null) {
            return 0;
        }
        long total = 0;
        for (Map.Entry<String, ?> e : spellsSection.entrySet()) {
            String family = CreditPolicy.familyOf(e.getKey());
            if (policy.modeFor(family) == CreditPolicy.GateMode.NO_CREDIT) {
                continue;
            }
            total += castCountOf(e.getValue());
        }
        return total;
    }

    /**
     * Credited total from a flattened tier key -&gt; cast count map
     * (the shape the live-API fallback sum produces, DESIGN.md §2).
     */
    public static long creditedTotalFromCounts(Map<String, Long> counts, CreditPolicy policy) {
        if (counts == null || policy == null) {
            return 0;
        }
        long total = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            String family = CreditPolicy.familyOf(e.getKey());
            if (policy.modeFor(family) == CreditPolicy.GateMode.NO_CREDIT) {
                continue;
            }
            if (e.getValue() != null) {
                total += Math.max(0, e.getValue());
            }
        }
        return total;
    }

    /** Raw (unclassified) lifetime total — every cast, credited or not. */
    public static long rawTotal(Map<String, ?> spellsSection) {
        if (spellsSection == null) {
            return 0;
        }
        long total = 0;
        for (Object v : spellsSection.values()) {
            total += castCountOf(v);
        }
        return total;
    }

    private static long castCountOf(Object record) {
        if (record instanceof Map<?, ?> m) {
            Object cc = m.get("cast_count");
            if (cc instanceof Number n) {
                return Math.max(0, n.longValue());
            }
            if (cc instanceof String s) {
                try {
                    return Math.max(0, Long.parseLong(s.trim()));
                } catch (NumberFormatException e) {
                    return 0;
                }
            }
        }
        return 0;
    }
}
