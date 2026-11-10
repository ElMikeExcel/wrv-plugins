package com.winkreyes.exceldetails;

/**
 * 0.2.2 boss scaling — the pure half (unit-tested, no Bukkit).
 *
 * Mike's ruling (Nov 9, 2026): boss segments scale by
 * BossTrial's display-scale so meter numbers match the boss
 * pop-ups. The arithmetic mirrors ExcelBossTrial 0.5.3's shipped
 * wiring (records package excelbosstrial-0.5.3-20261109):
 *
 *   - BossTrial's ledger stores GOVERNED RAW damage (caps only;
 *     core/BossGovernor is pure cap math) — the GovernorStash
 *     value its MONITOR record handler consumes.
 *   - Its governor (HIGH) then scales the EVENT by the intake
 *     factor f = min(1, 1024 / pool), so a MONITOR listener like
 *     this meter sees finalDamage = ledger x f.
 *   - Every pop-up renders DisplayScale.apply(displayScale,
 *     ledgerValue), displayScale from BossTrial's config key
 *     feedback.display-scale (settled 1000).
 *
 * So the pop-up space is recovered exactly as
 * finalDamage / f x displayScale. The sanitizers below mirror
 * BossTrial's own (core/DisplayScale.sanitized,
 * core/IntakeFactor.factor) value for value, so a junk number
 * degrades identically on both sides of the seam. The seam half
 * (reading BossTrial's live config + encounter state) lives in
 * BossScaleLookup and fails soft to scale-default / f = 1.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class BossScale {

    /** BossTrial's settled default (DisplayScale.DEFAULT_SCALE). */
    public static final double DEFAULT_SCALE = 1000.0;
    /** DisplayScale.MIN_SCALE: a scale never shrinks numbers. */
    public static final double MIN_SCALE = 1.0;
    /** DisplayScale.MAX_SCALE. */
    public static final double MAX_SCALE = 100_000.0;

    private BossScale() {}

    /**
     * The seal-boss type set — the exact test BossTrial's own
     * Attribution.kindOf performs (Wither / ElderGuardian /
     * Warden / EnderDragon), on Bukkit EntityType names so this
     * class stays Bukkit-free and unit-testable.
     */
    public static boolean isSealBossType(String entityTypeName) {
        if (entityTypeName == null) return false;
        return switch (entityTypeName) {
            case "WITHER", "ELDER_GUARDIAN", "WARDEN", "ENDER_DRAGON" -> true;
            default -> false;
        };
    }

    /** Mirror of DisplayScale.sanitized: NaN / infinite /
     *  non-positive reads as the settled default; a finite
     *  positive value clamps to [MIN_SCALE, MAX_SCALE]. */
    public static double sanitizedScale(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw <= 0.0) {
            return DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, raw));
    }

    /** Mirror of IntakeFactor.factor: f = cap / pool when the
     *  pool overflows the entity cap, else exactly 1. Junk
     *  (non-positive / NaN pool or cap) reads as 1 — identity is
     *  the only safe failure value. */
    public static double intakeFactor(double entityCap, double pool) {
        if (Double.isNaN(entityCap) || Double.isNaN(pool)
                || entityCap <= 0 || pool <= entityCap) {
            return 1.0;
        }
        return entityCap / pool;
    }

    /**
     * One boss hit as the pop-ups show it: the MONITOR-visible
     * final damage (ledger x f) recovered to ledger space and
     * rendered at the display scale. A factor outside (0, 1]
     * can never come from intakeFactor; if one arrives anyway
     * it reads as 1 (identity) rather than amplifying — the
     * x21-amplification failure shape BossTrial's PM-9 closed
     * on its side must never open on ours.
     */
    public static double forDisplay(double finalDamage, double displayScale, double intakeFactor) {
        if (Double.isNaN(finalDamage) || Double.isInfinite(finalDamage) || finalDamage <= 0) {
            return 0.0;
        }
        double f = (Double.isNaN(intakeFactor) || intakeFactor <= 0.0 || intakeFactor > 1.0)
                ? 1.0 : intakeFactor;
        return finalDamage / f * sanitizedScale(displayScale);
    }
}
