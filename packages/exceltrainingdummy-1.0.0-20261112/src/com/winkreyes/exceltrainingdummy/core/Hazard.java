package com.winkreyes.exceltrainingdummy.core;

/**
 * Hazardous mode levels (DESIGN §5). Damage-per-pulse defaults ship
 * in config; the level ladder (Light / Medium / Intense) is fixed.
 * The non-lethal clamp is the default posture: a pulse may take the
 * tester to 1 HP but never below it. The Lethal toggle is a separate
 * explicit switch (Downed / resurrection drills with a volunteer).
 */
public enum Hazard {
    OFF("Off", 0),
    LIGHT("Light", 2),
    MEDIUM("Medium", 4),
    INTENSE("Intense", 8);

    public final String displayName;
    public final int defaultDamagePerPulse;

    Hazard(String displayName, int defaultDamagePerPulse) {
        this.displayName = displayName;
        this.defaultDamagePerPulse = defaultDamagePerPulse;
    }

    public Hazard next() {
        Hazard[] v = values();
        return v[(ordinal() + 1) % v.length];
    }

    public static Hazard parse(String s, Hazard fallback) {
        if (s == null) return fallback;
        try {
            return valueOf(s.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * Damage actually applied by one pulse. Non-lethal: clamped so
     * the tester keeps at least 1 HP (returns 0 when already at 1).
     * Lethal: the full configured amount.
     */
    public static double pulseDamage(double configured, double currentHealth, boolean lethal) {
        if (configured <= 0) return 0;
        if (lethal) return configured;
        double maxAllowed = Math.max(0, currentHealth - 1.0);
        return Math.min(configured, maxAllowed);
    }
}
