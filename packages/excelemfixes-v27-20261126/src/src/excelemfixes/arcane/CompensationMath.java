package excelemfixes.arcane;

/**
 * Pure math for the compensation layer. No Bukkit or EliteMobs types, so
 * the whole class is unit-testable on a plain JVM.
 */
public final class CompensationMath {

    private CompensationMath() {
    }

    /**
     * Extra damage owed on a measured ability hit.
     *
     * The measured damage already includes everything EliteMobs itself
     * applied to the hit - including timed outgoing-damage buffs such
     * as Spell Echo, which EliteMobs applies inside its own damage
     * event (measured on EliteMobs 10.9.14; see the module header).
     * This module therefore adds ONLY the configured scale's share:
     *
     *     extra = dealt x (scale - 1)
     *
     * There is deliberately no buff term: re-applying the caster's
     * buff here would count it twice (upstream correction, EliteMobs
     * issue #243).
     *
     * @param dealt damage EliteMobs actually dealt (its damage event's
     *              final amount for the hit)
     * @param scale configured ability-damage scale (1.0 = no scale)
     * @return the top-up amount, never negative, 0 when the scale does
     * not exceed 1
     */
    public static double extraDamage(double dealt, double scale) {
        if (!Double.isFinite(dealt) || dealt <= 0.0) return 0.0;
        if (!Double.isFinite(scale)) return 0.0;
        if (scale <= 1.0) return 0.0;
        return dealt * (scale - 1.0);
    }

    /**
     * Splits an extra-damage amount across absorption then health, in
     * vanilla order.
     *
     * @return {newAbsorption, newHealth}
     */
    public static double[] applySplit(double absorption, double health, double extra) {
        double safeAbsorption = Math.max(0.0, absorption);
        double safeHealth = Math.max(0.0, health);
        double safeExtra = Math.max(0.0, extra);
        double absorbed = Math.min(safeAbsorption, safeExtra);
        double newAbsorption = safeAbsorption - absorbed;
        double newHealth = Math.max(0.0, safeHealth - (safeExtra - absorbed));
        return new double[]{newAbsorption, newHealth};
    }
}
