package excelemfixes.witherskulls;

/**
 * The skull probability math — pure, no Bukkit, so the numbers are
 * unit-testable deterministically (the suites pass explicit rolls;
 * nobody Monte-Carlos a drop rate).
 *
 * <p>Vanilla (26.2 loot table, extracted and read in the Oct 21
 * feedback audit): a player-killed wither skeleton rolls a skull at
 * 2.5% + 1.0% per Looting level on the killer's weapon (3.5 / 4.5 /
 * 5.5% at Looting I / II / III).
 *
 * <p>This plugin TOPS UP the difference toward the configured
 * target: when vanilla's roll left no skull in the drops, the
 * plugin rolls (target − vanillaEffectiveChance) once and adds
 * exactly one skull on success. The realized total chance is
 * therefore v + (1 − v) × (target − v) — 24.44% at the 25% target
 * with no Looting, 24.72% at Looting III — which the spec states
 * plainly rather than dressing up as exactly 25.
 */
public final class SkullMath {

    /** Vanilla base skull chance, percent (2.5%). */
    public static final double VANILLA_BASE_PERCENT = 2.5;

    /** Vanilla skull chance gained per Looting level, percent. */
    public static final double VANILLA_PER_LOOTING_PERCENT = 1.0;

    private SkullMath() {
    }

    /** The chance vanilla already rolled for this kill, in percent. */
    public static double vanillaChancePercent(int lootingLevel) {
        int level = Math.max(0, lootingLevel);
        return VANILLA_BASE_PERCENT + VANILLA_PER_LOOTING_PERCENT * level;
    }

    /**
     * The plugin's own roll chance for the top-up, in percent:
     * target minus what vanilla already covered, never below zero
     * (the plugin only ever ADDS — a target under the vanilla
     * effective chance means no roll at all), never above 100.
     */
    public static double topUpChancePercent(double targetPercent, int lootingLevel) {
        double diff = clampPercent(targetPercent) - vanillaChancePercent(lootingLevel);
        if (diff <= 0.0) return 0.0;
        return Math.min(100.0, diff);
    }

    /** The realized total skull chance for a kill, in percent. */
    public static double realizedTotalPercent(double targetPercent, int lootingLevel) {
        double v = vanillaChancePercent(lootingLevel) / 100.0;
        double t = topUpChancePercent(targetPercent, lootingLevel) / 100.0;
        return (v + (1.0 - v) * t) * 100.0;
    }

    /** A roll succeeds when the rolled percent falls under the chance. */
    public static boolean rollSucceeds(double chancePercent, double rollPercent) {
        return rollPercent < chancePercent;
    }

    static double clampPercent(double percent) {
        if (Double.isNaN(percent)) return 0.0;
        if (percent < 0.0) return 0.0;
        if (percent > 100.0) return 100.0;
        return percent;
    }
}
