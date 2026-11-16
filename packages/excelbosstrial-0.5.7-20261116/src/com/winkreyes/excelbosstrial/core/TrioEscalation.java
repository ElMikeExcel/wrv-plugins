/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Elder trio-position escalation (ExcelBossTrial 0.3.1).
 *
 * Mike's ruling (Nov 1, 2026): every elder of the trio is an epic
 * fight on its own (15+ minutes at the family's strongest pace),
 * and the LAST elder of the trio - the seal-completing kill -
 * should be even harder. The per-elder pool retune rides in
 * config (elder-guardian.scaling); the position escalation is
 * this class: the trio's elders are priced at position
 * multipliers, settled defaults 1.0 / 1.0 / 1.3, read from the
 * config key elder-guardian.trio.position-multipliers (the
 * adjustability law: balance lives in config, the mechanism in
 * code).
 *
 * The position of an elder is the monument's counted kills so
 * far + 1: position 1 for the first elder of a generation, 3 for
 * the seal-completing one. Because the count restarts when a
 * completed trio flips to respawn, repeat generations escalate
 * exactly like the first break - no generation branch anywhere.
 *
 * Pure math, no Bukkit: the adapter reads the monument's progress
 * when an encounter opens, caches the multiplier against the
 * boss, and prices the pool (and mid-fight rally growth) through
 * {@link #scaled}.
 */
public final class TrioEscalation {

    /** Settled defaults (Mike, Nov 1, 2026): positions 1 and 2
     *  at the slate pool, the seal-completing elder at x1.3. */
    public static final List<Double> DEFAULTS = List.of(1.0, 1.0, 1.3);

    private TrioEscalation() {
    }

    /** Trio position (1-based) of the next elder at a monument
     *  that has {@code countedSoFar} counted kills. */
    public static int positionFor(int countedSoFar) {
        return Math.max(0, countedSoFar) + 1;
    }

    /**
     * The multiplier for the next elder at a monument with
     * {@code countedSoFar} counted kills. Positions beyond the
     * configured list reuse its last entry (a list shorter than
     * the trio still escalates the completing elder); a null or
     * empty list falls back to the settled defaults.
     */
    public static double multiplierFor(int countedSoFar, List<Double> multipliers) {
        List<Double> list = sanitized(multipliers);
        int index = Math.min(positionFor(countedSoFar) - 1, list.size() - 1);
        return list.get(index);
    }

    /**
     * Clean a configured multiplier list: null/empty becomes the
     * settled defaults; each entry is clamped to [0, 10] with
     * NaN read as 1.0 (a typo'd config must never zero a pool or
     * blow one up).
     */
    public static List<Double> sanitized(List<Double> multipliers) {
        if (multipliers == null || multipliers.isEmpty()) {
            return DEFAULTS;
        }
        List<Double> out = new ArrayList<>(multipliers.size());
        for (Double d : multipliers) {
            double v = d == null || Double.isNaN(d) ? 1.0 : d;
            out.add(Math.max(0.0, Math.min(10.0, v)));
        }
        return List.copyOf(out);
    }

    /**
     * A scaling rule with the trio multiplier applied to both the
     * base and the per-extra slope, so the pool shape (solo through
     * full party) is preserved and only its scale moves. A
     * multiplier of exactly 1.0 returns the rule untouched (the
     * same instance - positions 1 and 2 price byte-identically to
     * an unescalated build).
     */
    public static ScalingRule scaled(ScalingRule rule, double multiplier) {
        if (rule == null || multiplier == 1.0) {
            return rule;
        }
        return new ScalingRule(rule.enabled(),
                rule.baseHealth() * multiplier,
                rule.perExtraDamager() * multiplier,
                rule.maxDamagersCounted());
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
