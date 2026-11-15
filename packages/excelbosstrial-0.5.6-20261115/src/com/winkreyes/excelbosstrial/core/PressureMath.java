/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The attrition axis (ExcelBossTrial 0.3.0, SPEC section 3): the
 * pure arithmetic of outgoing pressure - the ramp that scales a
 * boss's outgoing damage as the fight runs long, and the pulse's
 * hot/due tests. The Bukkit adapter owns the handlers and the
 * repeating task; every number they apply is decided here.
 *
 * Why this axis exists (the spec's statement of the gap): before
 * 0.3.0, incoming damage had a config dial on every chapter
 * (scaling) and outgoing damage had none outside the dragon's
 * phase-2 multiplier - a long fight pressured the players' DPS
 * but never their HEALING, so a group with a healer out-sustained
 * any pool given time. Pressure makes longevity itself cost: the
 * longer the fight, the harder the boss hits and the more the
 * pulse drains, until the group's sustain - not its damage -
 * becomes the binding constraint. That is what "barely make it
 * out alive" means mechanically at epic length: the ramp reaches
 * its cap partway into a 20-30 minute fight and HOLDS there, so
 * the final stretch is run at maximum pressure, never soft.
 */
public final class PressureMath {

    private PressureMath() {
    }

    /**
     * The ramp multiplier at {@code elapsedMinutes} into the
     * fight: 1 + per-minute * minutes, capped at rampMax. A
     * rampMax below 1 clamps to 1 (pressure never weakens a boss
     * below its chapter multiplier), and non-positive inputs read
     * as the fight's first instant.
     */
    public static double rampFactor(double elapsedMinutes, double rampPerMinute,
                                    double rampMax) {
        double minutes = Math.max(0.0, Double.isNaN(elapsedMinutes) ? 0.0 : elapsedMinutes);
        double per = Math.max(0.0, Double.isNaN(rampPerMinute) ? 0.0 : rampPerMinute);
        double cap = Math.max(1.0, Double.isNaN(rampMax) ? 1.0 : rampMax);
        return Math.min(cap, 1.0 + per * minutes);
    }

    /** Full outgoing scale: the chapter multiplier under the ramp. */
    public static double outgoingScale(double chapterMultiplier, double elapsedMinutes,
                                       double rampPerMinute, double rampMax) {
        double base = Double.isNaN(chapterMultiplier) || chapterMultiplier < 0.0
                ? 1.0 : chapterMultiplier;
        return base * rampFactor(elapsedMinutes, rampPerMinute, rampMax);
    }

    /** The pulse amount at this point in the fight: base under the ramp. */
    public static double pulseAmount(double baseDamage, double elapsedMinutes,
                                     double rampPerMinute, double rampMax) {
        double base = Math.max(0.0, Double.isNaN(baseDamage) ? 0.0 : baseDamage);
        return base * rampFactor(elapsedMinutes, rampPerMinute, rampMax);
    }

    /**
     * True when the fight is hot at {@code nowMs}: the most recent
     * fight evidence (a counted dealt blow or a landed taken blow)
     * is inside the hot window. No evidence ever (both stamps 0)
     * is never hot - an untouched boss does not pulse, and a fight
     * everyone died out of stops pulsing when its last evidence
     * ages past the window.
     */
    public static boolean hot(long lastCountedMs, long lastTakenMs, long nowMs, long hotMs) {
        long last = Math.max(lastCountedMs, lastTakenMs);
        return last > 0L && nowMs - last <= Math.max(0L, hotMs);
    }

    /** True when a pulse is due: hot, and the interval since the last pulse elapsed. */
    public static boolean pulseDue(long lastCountedMs, long lastTakenMs, long lastPulseMs,
                                   long nowMs, long intervalMs, long hotMs) {
        if (!hot(lastCountedMs, lastTakenMs, nowMs, hotMs)) {
            return false;
        }
        return lastPulseMs <= 0L || nowMs - lastPulseMs >= Math.max(0L, intervalMs);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
