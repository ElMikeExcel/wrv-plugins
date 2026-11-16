package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.10 (P-PROTECT-WATCH-1) — the audit watch's PURE mana-reserve
 * derivation. Protect does not own the reserve; ExcelSummons does. What the
 * watch records as a mana_reserve line is a DERIVATION replayed from Protect's
 * own observed summon registry (spawn/despawn observations) through the same
 * law ExcelSummons' ReservePolicy applies, so a reader can answer "did my
 * mana adjust correctly" from the log alone:
 *
 *   - only CAST-origin summons hold mana; ADOPTED (hand-tamed / bred) hold 0,
 *     permanently (Mike's ruling, Nov 12, 2026 — Summons 0.1.6);
 *   - holds are walked in registry order and a summon is included only while
 *     the running applied total stays within BOTH the ceiling and the
 *     floor-derived cap (100 − floor); a summon that would pass the cap holds
 *     nothing, and a smaller later summon that still fits is still included
 *     (Summons 0.1.6 clamp walk);
 *   - the percent table, ceiling and floor are read from the LIVE
 *     ExcelSummons config at derivation time, never baked in.
 *
 * Records derived this way carry source=derived; a mana_reserve line fired by
 * ExcelSummons itself through the GameplayEvent door carries the emitter's
 * own numbers and source=<emitter>. No Bukkit types — unit-tested off-server.
 * Excel — Wink Reyes Valley
 */
public final class AuditReserve {

    private AuditReserve() { }

    /** One observed living summon, in registry (spawn) order. */
    public record Summon(String spellKey, boolean castOrigin) { }

    /** The derivation outcome: applied total + per-summon inclusion flags. */
    public record Derivation(int heldTotalPercent, Map<String, Integer> appliedCounts,
                             List<Boolean> included) { }

    /** Fold a tiered spell key (archer|2) to its family base (archer). */
    public static String baseKey(String spellKey) {
        if (spellKey == null) return "";
        int bar = spellKey.indexOf('|');
        return bar >= 0 ? spellKey.substring(0, bar) : spellKey;
    }

    /** Percent of the pool one summon of this family holds; 0 when unconfigured. */
    public static int percentFor(Map<String, Integer> percents, String spellKey) {
        if (percents == null || spellKey == null) return 0;
        Integer p = percents.get(baseKey(spellKey));
        return p == null ? 0 : Math.max(0, p);
    }

    /** The most the applied total may reach under a ceiling + floor pair. */
    public static int appliedCap(int ceilingPercent, int floorPercent) {
        int byCeiling = Math.max(0, ceilingPercent);
        int byFloor = Math.max(0, 100 - Math.max(0, floorPercent));
        return Math.min(byCeiling, byFloor);
    }

    /**
     * Replay the clamp walk over one owner's observed living summons, in
     * order. ADOPTED records and unconfigured families never hold; a CAST
     * record whose hold would pass the applied cap is skipped (it stays
     * alive, it simply holds nothing).
     */
    public static Derivation derive(List<Summon> inOrder, Map<String, Integer> percents,
                                    int ceilingPercent, int floorPercent) {
        int cap = appliedCap(ceilingPercent, floorPercent);
        Map<String, Integer> counts = new LinkedHashMap<>();
        List<Boolean> included = new ArrayList<>();
        int running = 0;
        if (inOrder != null) {
            for (Summon s : inOrder) {
                boolean inc = false;
                if (s != null && s.castOrigin()) {
                    int pct = percentFor(percents, s.spellKey());
                    if (pct > 0 && running + pct <= cap) {
                        inc = true;
                        running += pct;
                        counts.merge(baseKey(s.spellKey()), 1, Integer::sum);
                    }
                }
                included.add(inc);
            }
        }
        return new Derivation(running, counts, included);
    }

    /** Held mana in pool units for an applied percent of a known base pool. */
    public static double heldUnits(int heldPercent, double basePool) {
        if (basePool <= 0) return 0;
        return Math.round(heldPercent * basePool) / 100.0;
    }
}
