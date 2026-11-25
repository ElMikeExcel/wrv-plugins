/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Spike reflect (ExcelBossTrial 0.3.5): the pure arithmetic and
 * the identification predicate for guardian spike reflect dealt
 * BY a tracked seal elder during an encounter.
 *
 * Why this rule exists (Mike, Nov 2, 2026 - playstyle parity):
 * vanilla elder guardians return 3 HP (Hard) on every melee hit
 * while their spikes are out. Against a normal 80 HP elder that
 * is nothing; against a governed seal pool in the tens of
 * thousands it is a melee-only tax that scales with OUR pool -
 * and until 0.3.5 the attrition pressure ramp scaled it too, so
 * the tax doubled by about minute 17 of a long elder fight.
 * Ranged and magic never pay it. A mechanic that taxes one
 * playstyle out of the fight is a defect even when the tuning
 * matches the spreadsheet, so the elder chapter carries a
 * multiplier for it ({@code elder-guardian.reflect.multiplier},
 * settled 0.0: reflect retired for seal elders). Melee, ranged
 * and magic all stay viable; preparation - not a per-hit tax -
 * is the fight.
 *
 * Identification: Paper gives guardian spikes and the Thorns
 * enchantment the SAME damage cause (THORNS) - by cause alone
 * they are indistinguishable. The distinguisher is the damager:
 * a spike hit's damager IS the guardian itself, so the pair
 * (cause == THORNS, damager resolves to a tracked elder with an
 * open encounter) can only be guardian spikes. Seal elders wear
 * no armor and carry no Thorns enchantment, and a player's own
 * Thorns armor fires in the opposite direction (victim = boss,
 * damager = player), which the boss-to-player handler never
 * sees. See REFLECT-FIX-FEASIBILITY-20261102.md sections 1-2.
 *
 * The Bukkit adapter owns the handler; every number it applies
 * is decided here, in the PressureMath / BossCaps style.
 */
public final class ReflectRule {

    private ReflectRule() {
    }

    /**
     * True when a boss-to-player damage event is spike reflect
     * from a tracked seal elder: the THORNS cause, a damager the
     * adapter resolved to a boss with an open encounter ledger,
     * and that boss an elder guardian. Every other combination -
     * a non-THORNS cause, an untracked damager, any other chapter
     * - is not spike reflect and is never scaled by this rule.
     */
    public static boolean isSpikeReflect(boolean thornsCause, boolean damagerIsTrackedBoss,
                                         BossKind damagerKind) {
        return thornsCause && damagerIsTrackedBoss && damagerKind == BossKind.ELDER_GUARDIAN;
    }

    /**
     * The multiplier as applied: a negative reads as 0 (reflect
     * can be retired or kept, never inverted into healing), and a
     * NaN reads as 0 too - a broken value retires reflect rather
     * than restoring the tax Mike ruled out. Values above 1 are
     * allowed deliberately: a future chapter could turn reflect
     * UP without a rebuild (the adjustability law).
     */
    public static double clampMultiplier(double multiplier) {
        if (Double.isNaN(multiplier) || multiplier < 0.0) {
            return 0.0;
        }
        return multiplier;
    }

    /**
     * The reflected damage after the chapter multiplier: the raw
     * event damage times the clamped multiplier, floored at 0.
     * This REPLACES the pressure scale for reflect events - the
     * result never passes through the attrition ramp, at any
     * fight length, so a multiplier of 0.0 yields exactly 0 no
     * matter how the caller would have ordered the two scales.
     * A NaN or negative raw damage reads as 0 (there is nothing
     * to reflect), and the result is never NaN or negative: the
     * fight table that later records it stays clean.
     */
    public static double scaledDamage(double rawDamage, double multiplier) {
        double raw = Double.isNaN(rawDamage) ? 0.0 : Math.max(0.0, rawDamage);
        double scaled = raw * clampMultiplier(multiplier);
        return Double.isNaN(scaled) ? 0.0 : Math.max(0.0, scaled);
    }

    /**
     * True when a scaled reflect amount cancels the event
     * outright: at 0 the event is cancelled (not merely zeroed),
     * so the MONITOR fight recorder skips it and reflect leaves
     * the damage-taken table entirely instead of stamping a
     * zero-damage "boss fought back" witness.
     */
    public static boolean cancels(double scaledDamage) {
        return !(scaledDamage > 0.0);
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
