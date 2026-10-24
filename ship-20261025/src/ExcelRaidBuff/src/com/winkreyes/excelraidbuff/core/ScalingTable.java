package com.winkreyes.excelraidbuff.core;

/**
 * The raid-buff scaling rule (Mike, Oct 20, 2026 late): a raid's
 * raiders get tougher with the Bad Omen level that raised the
 * raid, so the low-omen raids the family runs for Hero of the
 * Village stay vanilla and the Bad Omen V seal raid is the wall.
 * Pure math: the Bukkit glue reads the raid's omen level, each
 * raider's wave, and the raid's total waves, and asks here.
 *
 * The late-wave ramp exists so the wall is the END of the raid:
 * the final waves already carry the most vindicators, evokers and
 * ravager jockeys in the vanilla table, and the ramp leans on
 * exactly those waves. It only exists on a raid that is buffed at
 * all (omen HP factor above 1.0) — an Omen I raid is identity
 * end to end, final wave included.
 */
public final class ScalingTable {

    /** The factors for one raider. active=false means: touch nothing. */
    public record Factors(double health, double damage, boolean lateWave, boolean active) {
        public static final Factors IDENTITY = new Factors(1.0, 1.0, false, false);
    }

    private ScalingTable() {}

    /**
     * @param cfg         the live config
     * @param omenLevel   the raid's Bad Omen level (1..5; anything
     *                    else degrades to identity)
     * @param wave        the raider's wave, 1-based (values below 1
     *                    are treated as wave 1)
     * @param totalWaves  the raid's total waves including the bonus
     *                    wave (values below 1 disable the ramp)
     */
    public static Factors factors(RaidBuffConfig cfg, int omenLevel, int wave, int totalWaves) {
        if (cfg == null || !cfg.enabled) return Factors.IDENTITY;
        if (omenLevel < 1 || omenLevel > 5) return Factors.IDENTITY;
        double hp = cfg.healthMultiplier(omenLevel);
        double dmg = cfg.damageMultiplier(omenLevel);
        boolean late = false;
        if (cfg.lateWaveCount > 0 && totalWaves > 0 && hp > 1.0) {
            int w = Math.max(1, wave);
            late = w > totalWaves - cfg.lateWaveCount;
            if (late) hp *= cfg.lateWaveHealthRamp;
        }
        boolean active = hp != 1.0 || dmg != 1.0;
        return new Factors(hp, dmg, late, active);
    }
}
