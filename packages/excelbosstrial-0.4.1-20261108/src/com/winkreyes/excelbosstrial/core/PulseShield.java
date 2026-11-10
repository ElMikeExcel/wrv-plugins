/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The TrainingWheels shield applied to the pressure pulse
 * (ExcelBossTrial 0.3.7, TW-PRESSURE-CHECK fix shape 1): the pure
 * arithmetic of scaling a pulse amount by a protected player's
 * shield factor. The Bukkit adapter (TwShieldSeam) owns the
 * reflective factor read; every number applied to a pulse is
 * decided here.
 *
 * Why this exists: the pulse is source-less MAGIC damage, so
 * TrainingWheels' damage listener never sees it and the protected
 * player ate the full pulse. BossTrial applies the factor itself,
 * read through the seam - the exact pattern TrainingWheels uses
 * for its own compensation seams, run in the other direction.
 *
 * The hygiene rules mirror PressureMath: a factor is meaningful
 * only inside [0, 1] (ProtectedRegistry's own valid range). NaN,
 * negative or greater-than-1 factors read as 1.0 (no shield) - a
 * broken seam read must never amplify a pulse, and must never
 * zero one by accident either. A genuine factor of 0.0 is valid
 * and stays 0.0.
 */
public final class PulseShield {

    private PulseShield() {
    }

    /**
     * The factor a pulse may be scaled by: the input clamped to
     * the shield's valid range, with anything outside [0, 1] or
     * NaN reading as 1.0 (no shield).
     */
    public static double sanitizeFactor(double factor) {
        if (Double.isNaN(factor) || factor < 0.0 || factor > 1.0) {
            return 1.0;
        }
        return factor;
    }

    /**
     * The pulse amount for one victim: the (non-negative) amount
     * under the victim's sanitized shield factor. A NaN or
     * negative amount reads as 0 - no pulse - as in PressureMath.
     */
    public static double shieldedAmount(double amount, double factor) {
        double base = Math.max(0.0, Double.isNaN(amount) ? 0.0 : amount);
        return base * sanitizeFactor(factor);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
