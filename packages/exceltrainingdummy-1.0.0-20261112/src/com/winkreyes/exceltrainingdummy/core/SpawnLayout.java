package com.winkreyes.exceltrainingdummy.core;

/**
 * Spawn geometry (DESIGN §1): dummies form up in front of the
 * spawner; 3 and 5 make the cleave cluster for AoE tests. Offsets
 * are (forward, side) in blocks relative to the spawner's facing;
 * the Bukkit layer rotates them by yaw and ground-checks each spot.
 * Cluster side spacing is 1.6 blocks — close enough that one wave /
 * cleave covers the line, far enough that single-target casts pick
 * one dummy (the spell-audit cluster tests rely on both facts).
 */
public final class SpawnLayout {
    private SpawnLayout() {}

    public static final double FORWARD = 2.5;
    public static final double SIDE_SPACING = 1.6;

    /** (forward, side) pairs for a spawn count of 1, 3 or 5. */
    public static double[][] offsets(int count) {
        switch (count) {
            case 1:
                return new double[][]{{FORWARD, 0}};
            case 3:
                return new double[][]{
                        {FORWARD, 0},
                        {FORWARD, -SIDE_SPACING},
                        {FORWARD, SIDE_SPACING}};
            case 5:
                return new double[][]{
                        {FORWARD, 0},
                        {FORWARD, -SIDE_SPACING},
                        {FORWARD, SIDE_SPACING},
                        {FORWARD, -2 * SIDE_SPACING},
                        {FORWARD, 2 * SIDE_SPACING}};
            default:
                throw new IllegalArgumentException("dummy count must be 1, 3 or 5, got " + count);
        }
    }

    public static boolean isSupportedCount(int count) {
        return count == 1 || count == 3 || count == 5;
    }
}
