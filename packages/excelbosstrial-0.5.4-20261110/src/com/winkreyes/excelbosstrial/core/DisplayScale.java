/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The display scale (Item 35 Packet B, seal-fight DESIGN section
 * 0.4 as amended Nov 8, 2026, ~6:43 PM CST - Mike's ruling): the
 * damage display keeps ONE number space (pool space, so hits
 * still sum to the pool) and applies a uniform DISPLAY SCALE to
 * every player-facing pool-space number - per-hit feedback,
 * running totals, the stats pop-up, and pool figures on the boss
 * bar title. Internal ledger / governor math is UNCHANGED: this
 * class is only ever consulted at render time, by the display
 * surfaces, with the true ledger value in hand.
 *
 * Why it exists: the vanilla gear ladder is small numbers
 * (plain swords: wooden 4, diamond 7, netherite 8 HP - ratios
 * 1 : 1.75 : 2) and a governed display multiplies each hit by
 * the chapter's intake factor before rounding (wither ~0.0468,
 * elder quad ~0.0599, dragon phase 2 0.064). Unscaled, the three
 * tiers render as ~0.2 / ~0.4 / ~0.5 - three digits huddled under
 * 1 that read as "about nothing" (the Nov 7 party complaint).
 * At the settled scale of 1000 the same wither hits render as
 * 187 / 328 / 374: three visibly different values whose spacing
 * IS the ladder. The requirement is on what is seen - wooden
 * smallest, diamond between, netherite largest, ratios
 * recognizable after the display's own rounding - in EVERY
 * chapter, at every intake factor in the settled slate.
 *
 * Pure and pinned: sanitization mirrors the house feedback
 * shape (junk reads as the settled default, never as a silent
 * zero or a NaN on a player's screen).
 */
public final class DisplayScale {

    /** The settled default (Mike's amendment, Nov 8, 2026). */
    public static final double DEFAULT_SCALE = 1000.0;
    /** A scale below 1 would shrink numbers toward the failure
     *  this class exists to retire; the floor is 1 (unscaled). */
    public static final double MIN_SCALE = 1.0;
    /** Ceiling: far above any sane tuning; keeps a junk config
     *  value from printing pool figures in the billions. */
    public static final double MAX_SCALE = 100_000.0;

    private DisplayScale() {
    }

    /**
     * Sanitize a raw config value: NaN / infinite / non-positive
     * reads as the settled default; a finite positive value
     * clamps to [MIN_SCALE, MAX_SCALE].
     */
    public static double sanitized(double raw) {
        if (Double.isNaN(raw) || Double.isInfinite(raw) || raw <= 0.0) {
            return DEFAULT_SCALE;
        }
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, raw));
    }

    /** One pool-space value, display-scaled. NaN degrades to 0
     *  (a title never prints "NaN"); the scale is sanitized on
     *  the way in so a raw caller value can never leak junk. */
    public static double apply(double scale, double poolValue) {
        if (Double.isNaN(poolValue) || Double.isInfinite(poolValue)) {
            return 0.0;
        }
        return poolValue * sanitized(scale);
    }

    /**
     * The pool-space value one hit enters the ledger with: the
     * raw (entity-space) damage after the chapter's intake
     * factor. This is the number the ledger credits, the bar
     * loses, and - scaled - the action bar shows as {last}.
     */
    public static double poolHit(double rawDamage, double intakeFactor) {
        if (Double.isNaN(rawDamage) || Double.isInfinite(rawDamage)
                || Double.isNaN(intakeFactor) || Double.isInfinite(intakeFactor)) {
            return 0.0;
        }
        return Math.max(0.0, rawDamage) * Math.max(0.0, intakeFactor);
    }

    /** A hit as the player sees it: pool hit x display scale. */
    public static double displayedHit(double scale, double rawDamage,
                                      double intakeFactor) {
        return apply(scale, poolHit(rawDamage, intakeFactor));
    }

    /**
     * The gear-ladder verdict for one chapter (the design's
     * section 0.4 requirement, machine-checked): given the
     * three plain-sword rendered values, they must be strictly
     * ordered wooden < diamond < netherite AND their spacing
     * must keep the ladder's shape after rounding - the
     * diamond/wooden and netherite/wooden ratios within the
     * given tolerance of vanilla's 1.75 and 2.0. A display that
     * collapses the tiers to one number, or huddles them so the
     * ratios wash out, fails here.
     */
    public static boolean ladderVisible(double wooden, double diamond,
                                        double netherite, double ratioTolerance) {
        if (!(wooden > 0.0) || !(diamond > wooden) || !(netherite > diamond)) {
            return false;
        }
        double tol = Math.max(0.0, ratioTolerance);
        return Math.abs(diamond / wooden - 1.75) <= tol
                && Math.abs(netherite / wooden - 2.0) <= tol;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
