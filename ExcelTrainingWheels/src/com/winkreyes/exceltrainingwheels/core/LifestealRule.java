package com.winkreyes.exceltrainingwheels.core;

import java.util.Locale;

/**
 * Lifesteal on kill (0.1.4 — Mike, Oct 24, 2026, 8:33 AM CDT: "some
 * lifesteal when they kill something so they have passive sustain.
 * Just a little bit. Maybe a kill gives 1 or 2 hearts").
 *
 * A PROTECTED player who kills a hostile mob regains mob-hp (default
 * 2 HP = 1 heart); killing an EliteMobs elite regains elite-hp
 * (default 4 HP = 2 hearts). Never from players, never from passive
 * mobs, never from spawner-cage mobs (farm guard). Capped at max
 * health. Pure math so the plain-JVM suite can pin it.
 */
public final class LifestealRule {

    /** Hard ceiling on any configured heal, so a typo cannot make a player immortal (10 hearts). */
    public static final double MAX_CONFIG_HP = 20.0;
    public static final double DEFAULT_MOB_HP = 2.0;
    public static final double DEFAULT_ELITE_HP = 4.0;

    private LifestealRule() {}

    /** Config value -> usable HP: NaN/infinite -> default; negative -> 0; above MAX_CONFIG_HP -> MAX_CONFIG_HP. */
    public static double sanitizeHp(double value, double fallback) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return fallback;
        if (value < 0.0) return 0.0;
        return Math.min(value, MAX_CONFIG_HP);
    }

    /**
     * HP the kill is worth before the cap. 0 when anything rules it out.
     * Elite wins over hostile (an elite is usually also a hostile type);
     * an elite counts even on a non-hostile base type (EM custom bosses).
     */
    public static double healFor(boolean enabled, boolean killerProtected, boolean victimIsPlayer,
                                 boolean victimHostile, boolean victimElite, boolean fromSpawner,
                                 double mobHp, double eliteHp) {
        if (!enabled || !killerProtected || victimIsPlayer || fromSpawner) return 0.0;
        if (victimElite) return Math.max(0.0, eliteHp);
        if (victimHostile) return Math.max(0.0, mobHp);
        return 0.0;
    }

    /** HP actually gained: never past max, never negative, 0 for a dead/invalid killer. */
    public static double cappedGain(double current, double max, double heal) {
        if (!(heal > 0.0) || !(current > 0.0) || !(max > 0.0)) return 0.0;
        if (Double.isInfinite(heal) || Double.isNaN(current) || Double.isNaN(max)) return 0.0;
        if (current >= max) return 0.0;
        return Math.min(heal, max - current);
    }

    /** The new health value to set (current + capped gain). */
    public static double newHealth(double current, double max, double heal) {
        return current + cappedGain(current, max, heal);
    }

    /** Action-bar text for a gain in HP: 2 -> "+1 heart", 4 -> "+2 hearts", 1 -> "+0.5 heart". */
    public static String heartsLabel(double gainHp) {
        double hearts = Math.round(gainHp / 2.0 * 10.0) / 10.0;
        String num = hearts == Math.rint(hearts)
                ? String.valueOf((long) hearts)
                : String.format(Locale.ROOT, "%.1f", hearts);
        return "+" + num + (hearts > 1.0 ? " hearts" : " heart");
    }
}
