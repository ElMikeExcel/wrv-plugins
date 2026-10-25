package com.winkreyes.exceltrainingwheels.core;

/**
 * Which EliteMobs multiplier an elite's hit was scaled by (0.1.6).
 *
 * EliteMobs 10.9.10 picks ONE config multiplier per hit on the
 * elite-to-player path (PlayerDamagedByEliteMobHandler / PDBEFilter):
 *  - SCALED      natural elites with scaled combat:
 *                MobCombatSettings scaledDamageToPlayerMultiplier
 *                (the seal ladder);
 *  - NORMALIZED  custom bosses with normalized combat (regional,
 *                dungeon and instanced bosses):
 *                normalizedDamageToPlayerMultiplier;
 *  - OTHER       any other custom boss: damageToPlayerMultiplier.
 * The damage shield must divide by THAT multiplier so a protected
 * player takes base x the mob's UNSCALED damage on every path.
 *
 * Fallbacks never guess weaker than 0.1.5: an unreadable live value
 * or an unclassified elite uses the seal table (the 0.1.5 divisor).
 * The divisor never drops below 1.0, so the shield is never weaker
 * than the flat base factor against the damage that actually lands.
 * Pure logic; the Bukkit glue reads the live values reflectively.
 */
public final class CombatDivisor {

    public enum Path { SCALED, NORMALIZED, OTHER, UNKNOWN }

    /** A live EliteMobs value outside [0.05, 10] is treated as unreadable. */
    public static final double MIN_LIVE = 0.05;
    public static final double MAX_LIVE = 10.0;

    private CombatDivisor() {}

    public static boolean validLive(Double v) {
        return v != null && !v.isNaN() && !v.isInfinite() && v >= MIN_LIVE && v <= MAX_LIVE;
    }

    /**
     * @param path        the combat path EliteMobs uses for this elite
     * @param useLive     config shield.compensation.source = live
     * @param liveScaled  live scaledDamageToPlayerMultiplier (null if unreadable)
     * @param liveNorm    live normalizedDamageToPlayerMultiplier (null if unreadable)
     * @param liveOther   live damageToPlayerMultiplier (null if unreadable)
     * @param tableValue  the seal table's multiplier for the current seal
     */
    public static double divisor(Path path, boolean useLive, Double liveScaled, Double liveNorm,
                                 Double liveOther, double tableValue) {
        double table = Math.max(1.0, tableValue);
        if (!useLive || path == null) return table;
        Double live = switch (path) {
            case SCALED -> liveScaled;
            case NORMALIZED -> liveNorm;
            case OTHER -> liveOther;
            case UNKNOWN -> null;
        };
        if (!validLive(live)) return table;
        return Math.max(1.0, live);
    }

    /** base / divisor, clamped to [0, 1]. */
    public static double factor(double baseFactor, double divisor) {
        if (!ProtectedRegistry.validFactor(baseFactor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + baseFactor);
        }
        double d = Math.max(1.0, divisor);
        return Math.max(0.0, Math.min(1.0, baseFactor / d));
    }

    /** True when the seal table row matches the live ladder value (within 0.005). */
    public static boolean tableMatches(double tableValue, double liveScaled) {
        return Math.abs(tableValue - liveScaled) < 0.005;
    }
}
