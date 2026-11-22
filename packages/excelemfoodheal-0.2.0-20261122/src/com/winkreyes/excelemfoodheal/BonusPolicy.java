package com.winkreyes.excelemfoodheal;

/**
 * Pure D184 out-of-combat bonus decision (no Bukkit): the percent
 * of max health the base regen bonus delivers on one 1-second tick.
 * The bonus exists to raise EliteMobs worlds' total base
 * out-of-combat heal to 2%/s — EliteMobs' own ACS trickle is a
 * hardcoded 1%/s with no config, so this plugin adds the second
 * 1%/s under the same conditions ACS uses (EliteMobs world, out of
 * combat). The food budget is a separate, stacking layer and is
 * not decided here.
 */
public final class BonusPolicy {

    private BonusPolicy() {}

    /**
     * @param enabled                the config on/off gate
     * @param ratePercentPerSecond   the configured bonus rate
     * @param inEmWorld              the plugin's world gate result
     * @param inCombat               the plugin's combat-state result
     *                               (20 s window, damage dealt or taken)
     * @return the % of max health to deliver this tick: the
     *         configured rate when every condition holds, else 0
     */
    public static double tickPercent(boolean enabled, double ratePercentPerSecond,
                                     boolean inEmWorld, boolean inCombat) {
        if (!enabled || ratePercentPerSecond <= 0) return 0.0;
        if (!inEmWorld || inCombat) return 0.0;
        return ratePercentPerSecond;
    }
}
