package com.winkreyes.excelbosstrial.core;

/**
 * PARTICIPANT GROWTH - "the boss rallies" (ExcelBossTrial 0.2,
 * spec BOSSTRIAL-0.2-SPEC-2026-10-20.md section 1; Mike's
 * first-attempt doctrine, decisions settled Oct 20, 2026).
 *
 * A governed boss opens its encounter at {@code baseHealth}. Every
 * time a NEW player registers attributed dealt damage (the
 * EncounterLedger's dealt record - the same record the share gate
 * reads), the health pool grows by {@code perExtraDamager}, up to
 * {@code maxDamagersCounted} damagers. Growth is a ratchet: it
 * never reverses when a damager dies, leaves, or logs off. No
 * boss's outgoing damage changes anywhere in 0.2 - duration is
 * the dial, because the family's armor math leaves no other
 * honest one (spec section 0).
 *
 * The pool for n damagers: base + perExtra * clamp(n - 1, 0,
 * maxDamagersCounted - 1). One damager faces the base pool; the
 * first damager never triggers a step (the base already prices
 * them in).
 *
 * Pure data + math, Bukkit-free like the rest of core. Values are
 * clamped instead of throwing - a typo'd config degrades toward
 * vanilla, never crashes the plugin (the BossTrialConfig pattern).
 */
public record ScalingRule(boolean enabled, double baseHealth,
                          double perExtraDamager, int maxDamagersCounted) {

    /** Scaling off: the encounter keeps the boss's vanilla max HP
     *  (the 0.1.0 fight). */
    public static final ScalingRule OFF = new ScalingRule(false, 0, 0, 5);

    public ScalingRule {
        baseHealth = clamp(baseHealth, 0, 1_000_000);
        perExtraDamager = clamp(perExtraDamager, 0, 1_000_000);
        maxDamagersCounted = (int) clamp(maxDamagersCounted, 1, 100);
    }

    /** Settled defaults (spec section 2). */
    public static ScalingRule settledDefault(BossKind kind) {
        return switch (kind) {
            case WITHER -> new ScalingRule(true, 750, 200, 5);
            case ELDER_GUARDIAN -> new ScalingRule(true, 400, 50, 5);
            case WARDEN -> new ScalingRule(true, 800, 150, 5);
            // The dragon's two-phase machine is its scaler (spec
            // section 2.4); growth is never applied to it.
            case ENDER_DRAGON -> OFF;
        };
    }

    /** True when this rule actually scales an encounter open. */
    public boolean active() {
        return enabled && baseHealth > 0;
    }

    /** The pool a fight with this many registered damagers faces. */
    public double poolFor(int damagers) {
        if (!active()) {
            return baseHealth;
        }
        int steps = Math.max(0, Math.min(damagers - 1, maxDamagersCounted - 1));
        return baseHealth + perExtraDamager * steps;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, v));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

