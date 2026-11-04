package com.winkreyesvalley.excelmagiclevel.core;

import java.util.Map;
import java.util.Objects;

/**
 * ExcelMagicLevel — the level curve. Level is DERIVED from lifetime XP on
 * every read (SPEC §7.1): the store keeps one number (total XP), this class
 * turns it into a level. Retuning the curve in config re-derives everyone's
 * level consistently; nothing here is hardcoded per level — the level count,
 * the formula, and any per-level overrides are all config data, so a future
 * rescale (e.g. 20 → 100 levels with identical totals) is a config change,
 * not a rebuild.
 *
 * <p>Formula (SPEC §5.1): xp-to-next(L) = round-to-N(base × L^exponent),
 * with an explicit per-level override winning where present.</p>
 *
 * <p>Pure Java: no Bukkit, no Magic, no mcMMO imports (SPEC §9.1).</p>
 */
public final class Curve {
    private final int maxLevel;
    private final double base;
    private final double exponent;
    private final long roundTo;
    private final Map<Integer, Long> overrides;
    /** cumulative[i] = total lifetime XP required to REACH level i+1. */
    private final long[] cumulative;

    public Curve(int maxLevel, double base, double exponent, long roundTo, Map<Integer, Long> overrides) {
        if (maxLevel < 2) throw new IllegalArgumentException("max-level must be >= 2, got " + maxLevel);
        if (!(base > 0)) throw new IllegalArgumentException("curve base must be > 0, got " + base);
        if (!(exponent > 0)) throw new IllegalArgumentException("curve exponent must be > 0, got " + exponent);
        this.maxLevel = maxLevel;
        this.base = base;
        this.exponent = exponent;
        this.roundTo = roundTo;
        this.overrides = Map.copyOf(Objects.requireNonNull(overrides, "overrides"));
        this.cumulative = new long[maxLevel + 1];
        cumulative[0] = 0; // index 0 unused; level 1 sits at index 1
        cumulative[1] = 0;
        for (int level = 2; level <= maxLevel; level++) {
            cumulative[level] = cumulative[level - 1] + xpToNext(level - 1);
        }
    }

    public int maxLevel() {
        return maxLevel;
    }

    /** XP needed to go from {@code fromLevel} to {@code fromLevel + 1}. */
    public long xpToNext(int fromLevel) {
        if (fromLevel < 1 || fromLevel >= maxLevel) {
            throw new IllegalArgumentException("xpToNext undefined for level " + fromLevel + " (max " + maxLevel + ")");
        }
        Long override = overrides.get(fromLevel);
        if (override != null) {
            if (override <= 0) throw new IllegalArgumentException("curve override for level " + fromLevel + " must be > 0");
            return override;
        }
        double raw = base * Math.pow(fromLevel, exponent);
        if (roundTo > 1) {
            return Math.round(raw / (double) roundTo) * roundTo;
        }
        return Math.round(raw);
    }

    /** Total lifetime XP required to reach {@code level} (level 1 = 0). */
    public long cumulativeFor(int level) {
        if (level < 1 || level > maxLevel) throw new IllegalArgumentException("level out of range: " + level);
        return cumulative[level];
    }

    /** Total lifetime XP required to reach the cap. */
    public long totalToCap() {
        return cumulative[maxLevel];
    }

    /** The level a lifetime XP total derives to, clamped to [1, maxLevel]. */
    public int levelFor(long totalXp) {
        if (totalXp <= 0) return 1;
        if (totalXp >= cumulative[maxLevel]) return maxLevel;
        int lo = 1, hi = maxLevel;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (cumulative[mid] <= totalXp) lo = mid; else hi = mid - 1;
        }
        return lo;
    }

    /** XP earned inside the current level. */
    public long xpIntoLevel(long totalXp) {
        return totalXp - cumulative[levelFor(totalXp)];
    }

    /** XP still needed to reach the next level; 0 at cap. */
    public long xpToNextLevel(long totalXp) {
        int level = levelFor(totalXp);
        if (level >= maxLevel) return 0;
        return cumulative[level + 1] - totalXp;
    }
}
