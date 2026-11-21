package com.winkreyes.exceltrainingwheels.core;

/**
 * BossTrial compensation for the kid shield (0.1.3 — Mike's
 * ABSOLUTE LINE, restated Nov 2, 2026: a protected player takes
 * their factor of a mob's NORMAL, UNSCALED damage in every world
 * at every seal state). ExcelBossTrial's pressure handler scales
 * a seal boss's outgoing damage in an open encounter — chapter
 * multiplier x attrition ramp (PressureMath: +6%/min, cap x2.0;
 * Wither/Warden run a x1.25 chapter multiplier, so 2.5x vanilla
 * at ramp cap) — so a flat shield factor would let a protected
 * player take factor x chapter x ramp of vanilla (0.25 x 2.5 =
 * 0.625 on the Wither/Warden at cap) instead of the guaranteed
 * quarter. On the events that handler scales, the shield
 * therefore divides its effective factor by the boss's CURRENT
 * outgoing scale, read through a reflective seam on
 * ExcelBossTrialAPI.outgoingScaleFor(LivingEntity) — the third
 * compensation seam, beside the EliteMobs ladder division
 * (CompensationTable) and the raid-buff division (RaidDivision),
 * whose shape this class mirrors:
 *
 *     take = vanilla x scale x (baseFactor / scale)
 *          = baseFactor x vanilla        (at every ramp point)
 *
 * THE DIVISION APPLIES ONLY WHERE THE API'S NUMBER IS THE
 * SCALER THAT ACTUALLY TOUCHED THE EVENT. The 0.3.7 contract
 * (its DESIGN section 3.1 + the API's own scope note) is
 * explicit: outgoingScaleFor reports the PRESSURE scale —
 * chapter outgoing x ramp — and the dragon chapter's native
 * phase-2 factor is a separate axis it does NOT report. That
 * fixes exactly which shapes may divide:
 *
 *  - DIRECT_BOSS (the damager IS the boss LivingEntity: melee,
 *    sweep, the elder's laser, the warden's sonic boom — the
 *    pressure handler has no cause filter): the event was
 *    multiplied by exactly the API's number. DIVIDE.
 *  - The dragon's part / fireball / breath shapes are scaled
 *    ONLY on the phase-2 axis (DragonChapter's x1.5, phase 2
 *    only); the pressure scale never touches them. Dividing
 *    them by the pressure number would be wrong in both
 *    directions — in phase 1 it would push the take BELOW the
 *    guaranteed quarter (the failure RaidDivision's design
 *    exists to avoid), in phase 2 it would coincidentally match
 *    only where ramp happened to equal 1.5. So these shapes are
 *    ADMITTED by the shield (ShieldRule / the glue's parent and
 *    cloud-source attribution) at the FLAT factor and do NOT
 *    divide: exact quarter outside phase 2, 0.375x vanilla
 *    during phase 2 — a bounded, named residual whose fix
 *    belongs to a future phase-aware BossTrial getter, not to
 *    dividing by the wrong axis. (The dragon's own direct hits
 *    divide on the pressure axis; the phase-2 CONTACT buff
 *    stacks on them too, so the same x1.5 phase-2 residual
 *    applies there.)
 *  - WITHER_PROJECTILE (skull direct hit / skull blast): NO
 *    BossTrial scaler touches these on any path — the pressure
 *    handler requires a LivingEntity damager and no dragon
 *    chapter is involved. NEVER divided, or the protected
 *    player would drop below the guaranteed quarter.
 *
 * The glue maps each admitted event to a Shape from the Bukkit
 * entities; this class owns which shapes divide, so the rule
 * is unit-testable.
 *
 * A scale below 1.0, NaN, or infinite can never come from the
 * API's contract (it returns chapter x ramp, both >= 1.0 by
 * construction, and 1.0 when no encounter is open or on any
 * failure) and is sanitized to 1.0 here: a bad read — or an
 * absent plugin, whose read IS 1.0 — must fail toward today's
 * behavior, the flat factor, never toward a weaker OR stronger
 * shield.
 *
 * Pure logic; the Bukkit glue owns the reflective read and the
 * shape mapping, and passes the scale in.
 */
public final class BossDivision {

    private BossDivision() {}

    /**
     * How an admitted event's damage was produced, as the glue
     * resolves it from the Bukkit entities. Only DIRECT_BOSS
     * divides — see the class doc for the axis reasoning.
     */
    public enum Shape {
        /** Damager IS the seal boss itself (melee / sweep / laser / sonic boom): pressure-scaled by exactly the API's number. */
        DIRECT_BOSS,
        /** Projectile whose shooter is the Ender Dragon: scaled only on the phase-2 axis — admitted, never divided by the pressure scale. */
        DRAGON_PROJECTILE,
        /** AreaEffectCloud whose source is the Ender Dragon: phase-2 axis only — admitted, never divided. */
        DRAGON_BREATH,
        /** EnderDragonPart (parent is the dragon): phase-2 axis only — admitted, never divided. */
        DRAGON_PART,
        /** Projectile whose shooter is the Wither (skull direct hit / skull blast): NO BossTrial scaler exists. */
        WITHER_PROJECTILE,
        /** Anything else (non-boss source, creeper blast, ...): no BossTrial scale applies. */
        NON_BOSS
    }

    /** True only where the API's pressure scale is the scaler that touched the event. */
    public static boolean divides(Shape shape) {
        return shape == Shape.DIRECT_BOSS;
    }

    /**
     * A scale the division may trust: finite and >= 1.0.
     * Anything else (absent plugin's 1.0 is fine and passes;
     * sub-1.0, NaN, infinite) collapses to 1.0 = no division.
     */
    public static double sanitize(double scale) {
        return (scale >= 1.0 && !Double.isInfinite(scale)) ? scale : 1.0;
    }

    /**
     * The shield factor after boss division: {@code factor /
     * scale}, the scale sanitized first, so the division can
     * never RAISE the factor. Factor must be in [0, 1] (the
     * effective factor the glue computed, post elite and raid
     * divisions).
     */
    public static double dividedFactor(double factor, double scale) {
        if (!ProtectedRegistry.validFactor(factor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + factor);
        }
        return factor / sanitize(scale);
    }
}

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
