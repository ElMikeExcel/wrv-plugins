package com.winkreyes.excelraidbuff.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Which raiders in a wave become EliteMobs elites (Mike's
 * addition, Oct 20, 2026 late: "include elites in there too").
 * Deterministic by design — a fixed quota per wave and an evenly
 * spaced pick — so a wave's shape is reproducible on the rig and
 * in the packet, with no RNG streaks (a wave that randomly rolled
 * five elites, or none, would be a tuning accident, not a rule).
 *
 * The share ramps across the raid: the first wave is the softest
 * (ramp-from), the final wave the densest (ramp-to), linear in
 * between, and the raid-wide average stays the omen's base share.
 * Within a wave, conversion prefers the raiders that make a fight
 * (the glue passes a priority per raider: vindicators, then
 * witches, then evokers, then pillagers, ravagers last) and
 * spreads the picks evenly through that order.
 */
public final class EliteComposition {

    private EliteComposition() {}

    /** The elite share for one wave, after the across-the-raid ramp. */
    public static double waveShare(RaidBuffConfig cfg, int omenLevel, int wave, int totalWaves) {
        if (cfg == null || !cfg.elitesEnabled) return 0.0;
        double base = cfg.eliteShare(omenLevel);
        if (base <= 0.0) return 0.0;
        if (totalWaves <= 1) return base * cfg.eliteWaveRampTo;
        int w = Math.max(1, Math.min(wave, totalWaves));
        double t = (w - 1) / (double) (totalWaves - 1);
        double ramp = cfg.eliteWaveRampFrom + (cfg.eliteWaveRampTo - cfg.eliteWaveRampFrom) * t;
        return Math.min(1.0, base * ramp);
    }

    /** How many of a wave of this size convert at this share (rounds half up). */
    public static int quota(int waveSize, double share) {
        if (waveSize <= 0 || share <= 0.0) return 0;
        int q = (int) Math.floor(waveSize * share + 0.5);
        return Math.min(q, waveSize);
    }

    /**
     * Pick which raiders convert. {@code priorities} is one entry
     * per raider (lower converts earlier). Returns indices into
     * the input list, ascending. The picks spread evenly through
     * the priority order so a quota of 2 in a 6-raider wave takes
     * the 1st and 4th of the order, never a clump.
     */
    public static List<Integer> select(int quota, List<Integer> priorities) {
        List<Integer> out = new ArrayList<>();
        if (priorities == null || quota <= 0) return out;
        int n = priorities.size();
        int q = Math.min(quota, n);
        if (q <= 0) return out;
        List<Integer> order = new ArrayList<>(n);
        for (int i = 0; i < n; i++) order.add(i);
        order.sort((a, b) -> {
            int c = Integer.compare(priorities.get(a), priorities.get(b));
            return c != 0 ? c : Integer.compare(a, b);
        });
        List<Integer> picked = new ArrayList<>(q);
        for (int i = 0; i < q; i++) {
            int pos = (int) Math.floor(i * (double) n / q);
            picked.add(order.get(pos));
        }
        picked.sort(Integer::compareTo);
        return picked;
    }
}
