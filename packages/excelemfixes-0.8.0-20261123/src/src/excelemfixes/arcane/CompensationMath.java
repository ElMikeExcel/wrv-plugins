package excelemfixes.arcane;

import java.util.List;

/**
 * Pure math for the compensation layer. No Bukkit or EliteMobs types, so
 * the whole class is unit-testable on a plain JVM.
 */
public final class CompensationMath {

    private CompensationMath() {
    }

    /** One timed outgoing-damage modifier, mirrored out of EliteMobs. */
    public record BuffEntry(double multiplier, long expiresAtNanos, String domain) {
    }

    /**
     * Extra damage owed on a measured ability hit.
     *
     * @param dealt      damage EliteMobs actually dealt (effective HP drop)
     * @param scale      configured ability-damage scale (1.0 = no scale)
     * @param buffFactor caster's strongest active outgoing modifier (1.0 = none)
     * @return the top-up amount, never negative, 0 when the combined
     * multiplier does not exceed 1
     */
    public static double extraDamage(double dealt, double scale, double buffFactor) {
        if (!Double.isFinite(dealt) || dealt <= 0.0) return 0.0;
        if (!Double.isFinite(scale) || !Double.isFinite(buffFactor)) return 0.0;
        double combined = scale * buffFactor;
        if (combined <= 1.0) return 0.0;
        return dealt * (combined - 1.0);
    }

    /**
     * The buff factor for a caster: the strongest unexpired modifier in
     * the ANY or SPELL domain, mirroring EliteMobs' own strongest-only
     * semantics in TimedCombatModifiers.strongestOutgoing. Returns 1.0
     * when nothing applies.
     */
    public static double strongestBuff(List<BuffEntry> entries, long nowNanos) {
        if (entries == null || entries.isEmpty()) return 1.0;
        double best = 1.0;
        for (BuffEntry entry : entries) {
            if (entry == null) continue;
            if (entry.expiresAtNanos() <= nowNanos) continue;
            if (!"ANY".equals(entry.domain()) && !"SPELL".equals(entry.domain())) continue;
            if (Double.isFinite(entry.multiplier()) && entry.multiplier() > best) {
                best = entry.multiplier();
            }
        }
        return best;
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
