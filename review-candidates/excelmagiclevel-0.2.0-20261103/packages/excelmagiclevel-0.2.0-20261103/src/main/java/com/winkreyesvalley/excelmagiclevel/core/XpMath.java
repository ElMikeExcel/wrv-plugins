package com.winkreyesvalley.excelmagiclevel.core;

/**
 * ExcelMagicLevel — the XP payment formula (SPEC §4.6, order is normative):
 * base weight → per-spell diminishing window (§4.2) → per-target repeat
 * scale (§4.3) → weekend multiplier → floor, once, at the end.
 *
 * <p>The window ramp mirrors Magic's own SP earn cooldown exactly:
 * {@code floor(weight × min(1, elapsed / window))} — the shape the family
 * already lives under for SP.</p>
 */
public final class XpMath {
    private XpMath() {
    }

    /**
     * @param weight            the spell's weight (base XP of a full-window cast)
     * @param elapsedMillis     ms since the last XP-paying cast of this spell by
     *                          this player, or {@code null} if there was none
     * @param earnWindowMillis  the per-spell diminishing window
     * @param targetRepeat      true if this cast's target was already paid
     *                          against inside the target window
     * @param targetRepeatScale scale applied on a target repeat (e.g. 0.5)
     * @param multiplier        earner-only multiplier (weekend rate); 1.0 = none
     * @return the XP to pay; 0 means "pays nothing" (and, per Magic's own
     *         earn code, a 0-pay cast does NOT reset the window timestamp)
     */
    public static int payment(int weight, Long elapsedMillis, long earnWindowMillis,
                              boolean targetRepeat, double targetRepeatScale, double multiplier) {
        if (weight <= 0) return 0;
        double scaled = weight;
        if (elapsedMillis != null && earnWindowMillis > 0 && elapsedMillis < earnWindowMillis) {
            scaled = weight * (elapsedMillis / (double) earnWindowMillis);
        }
        if (targetRepeat) {
            scaled *= targetRepeatScale;
        }
        scaled *= multiplier;
        return (int) Math.floor(scaled);
    }
}
