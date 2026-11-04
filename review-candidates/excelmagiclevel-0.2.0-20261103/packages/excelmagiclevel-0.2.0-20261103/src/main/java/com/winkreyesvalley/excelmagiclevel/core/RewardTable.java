package com.winkreyesvalley.excelmagiclevel.core;

import java.util.Set;

/**
 * ExcelMagicLevel — the reward table (SPEC §5.2). Per-level increments are
 * config data; the boost AT a level is the increment × (level − 1), a clean
 * straight line anyone can sanity-check. Level 1 has no boost and no
 * modifier. Milestone levels are presentation only (title + sound) — no
 * stat bumps, so the line never kinks.
 */
public final class RewardTable {
    private final Boost perLevel;
    private final Set<Integer> milestones;

    public RewardTable(Boost perLevel, Set<Integer> milestones) {
        this.perLevel = perLevel;
        this.milestones = Set.copyOf(milestones);
    }

    public Boost perLevel() {
        return perLevel;
    }

    /** Cumulative boost totals AT {@code level}; {@link Boost#NONE} at level 1. */
    public Boost boostAt(int level) {
        if (level <= 1) return Boost.NONE;
        return perLevel.scale(level - 1);
    }

    public boolean isMilestone(int level) {
        return milestones.contains(level);
    }

    public Set<Integer> milestones() {
        return milestones;
    }
}
