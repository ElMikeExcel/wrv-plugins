package com.winkreyes.exceltrainingwheels.core;

/**
 * Raid compensation for the damage shield (0.1.2 — Mike's rule,
 * Oct 21, 2026: "Keep it at exactly a quarter of vanilla —
 * compensate for the buff"): ExcelRaidBuff raises a raider's
 * damage through its GENERIC_ATTACK_DAMAGE attribute, so a flat
 * shield factor would let a protected player take factor x the
 * buffed damage (0.25 x 1.30 = 0.325 of vanilla at Bad Omen V).
 * The shield therefore divides its effective factor by the buff
 * factor stored on the damager — read through a reflective seam
 * on ExcelRaidBuffAPI.damageMultiplierOf, exactly the division
 * CompensationTable already performs against the EliteMobs
 * ladder:
 *
 *     take = vanilla x buffFactor x (baseFactor / buffFactor)
 *          = baseFactor x vanilla          (at every omen level)
 *
 * The division applies ONLY where the attack attribute actually
 * scaled the damage: the melee causes (ENTITY_ATTACK and
 * ENTITY_SWEEP_ATTACK). Projectile damage (a pillager's crossbow
 * bolt) and potion damage were never attribute-scaled, were never
 * raised, and are NEVER divided here, or the player would drop
 * BELOW the guaranteed quarter. Elites are never raid-buffed
 * (the buff loop skips EliteMobs-tagged raiders), so an elite
 * damager's read is exactly 1.0 and the elite path is numerically
 * unchanged; an unbuffed damager, and an unbound seam (plugin
 * absent), likewise read 1.0 — the only states where 1.0 is
 * correct, because there nothing is buffed.
 *
 * Pure logic; the Bukkit glue owns the reflective read and
 * passes the divisor in. A divisor below 1.0, NaN, or infinite
 * can never come from the API's contract (it returns the stored
 * factor, >= 1.0 by construction) and is sanitized to 1.0 here:
 * a bad read must fail toward today's behavior, never toward a
 * weaker OR stronger shield than the flat factor gives.
 */
public final class RaidDivision {

    private RaidDivision() {}

    /** True for the damage causes the attack attribute scales: melee only. */
    public static boolean isMelee(ShieldRule.Cause cause) {
        return cause == ShieldRule.Cause.ENTITY_ATTACK
                || cause == ShieldRule.Cause.ENTITY_SWEEP_ATTACK;
    }

    /**
     * A divisor the division may trust: finite and >= 1.0.
     * Anything else (absent plugin's 1.0 is fine and passes;
     * sub-1.0, NaN, infinite) collapses to 1.0 = no division.
     */
    public static double sanitize(double divisor) {
        return (divisor >= 1.0 && !Double.isInfinite(divisor)) ? divisor : 1.0;
    }

    /**
     * The shield factor after raid division: {@code factor /
     * divisor} on melee causes, the factor itself everywhere
     * else. The divisor is sanitized first, so a melee division
     * can never RAISE the factor. Factor must be in [0, 1] (the
     * effective factor the glue computed, post elite division).
     */
    public static double dividedFactor(double factor, ShieldRule.Cause cause, double divisor) {
        if (!ProtectedRegistry.validFactor(factor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + factor);
        }
        if (!isMelee(cause)) return factor;
        return factor / sanitize(divisor);
    }
}

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
