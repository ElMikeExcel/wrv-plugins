/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.Map;

/**
 * Every numeric/behavioural knob of the trial, with the shipped
 * defaults from SPEC.md. Pure data: the Bukkit adapter fills it from
 * config.yml (via {@link #fromMap}); tests fill it directly.
 *
 * Validation clamps instead of throwing: a typo'd config should
 * degrade the fight toward vanilla, never crash the plugin.
 */
public final class TrialConfig {

    public final boolean enabled;
    public final boolean applyToResummons;
    public final double phase1MaxHealth;
    public final double phase2MaxHealth;
    public final double phase2DamageMultiplier;
    public final int invulnerableSeconds;
    public final int aggressionIntervalSeconds;
    public final double aggressionChance;
    public final boolean addsEnabled;
    public final int addsPerWave;
    public final int addsMaxAlive;
    // 0.1.1 anti-nuke caps (DRAGON-CHEESE-RESEARCH.md section 2).
    // A cap value of 0 means DISABLED (fail-soft off, per rule).
    public final double perEventDamageCap;
    public final double explosionTickBudget;
    public final boolean blockCrystalPlacement;
    /** 0.3.0 (SPEC section 0.3): the platform's MAX_HEALTH clamp.
     *  Phase bars carry min(phase pool, entityCap) and governed
     *  damage scales by the matching intake factor; at the default
     *  1024 (or any cap above both pools) the chapter is exactly
     *  its pre-0.3.0 self. Sourced from the host's top-level
     *  intake.entity-cap key. */
    public final double entityCap;
    /** 0.3.2: the phase-2 add-wave thresholds as health fractions,
     *  high to low (config key dragon.adds.wave-thresholds). Before
     *  0.3.2 these were the TrialStateMachine.WAVE_THRESHOLDS code
     *  constants; at the defaults the behavior is byte-for-byte the
     *  old one. Sanitized by TrialStateMachine.sanitizeThresholds. */
    public final double[] addsWaveThresholds;

    public TrialConfig(boolean enabled, boolean applyToResummons,
                       double phase1MaxHealth, double phase2MaxHealth,
                       double phase2DamageMultiplier, int invulnerableSeconds,
                       int aggressionIntervalSeconds, double aggressionChance,
                       boolean addsEnabled, int addsPerWave, int addsMaxAlive,
                       double perEventDamageCap, double explosionTickBudget,
                       boolean blockCrystalPlacement) {
        this(enabled, applyToResummons, phase1MaxHealth, phase2MaxHealth,
                phase2DamageMultiplier, invulnerableSeconds, aggressionIntervalSeconds,
                aggressionChance, addsEnabled, addsPerWave, addsMaxAlive,
                perEventDamageCap, explosionTickBudget, blockCrystalPlacement, 1024.0);
    }

    public TrialConfig(boolean enabled, boolean applyToResummons,
                       double phase1MaxHealth, double phase2MaxHealth,
                       double phase2DamageMultiplier, int invulnerableSeconds,
                       int aggressionIntervalSeconds, double aggressionChance,
                       boolean addsEnabled, int addsPerWave, int addsMaxAlive,
                       double perEventDamageCap, double explosionTickBudget,
                       boolean blockCrystalPlacement, double entityCap) {
        this(enabled, applyToResummons, phase1MaxHealth, phase2MaxHealth,
                phase2DamageMultiplier, invulnerableSeconds, aggressionIntervalSeconds,
                aggressionChance, addsEnabled, addsPerWave, addsMaxAlive,
                perEventDamageCap, explosionTickBudget, blockCrystalPlacement,
                entityCap, TrialStateMachine.WAVE_THRESHOLDS);
    }

    public TrialConfig(boolean enabled, boolean applyToResummons,
                       double phase1MaxHealth, double phase2MaxHealth,
                       double phase2DamageMultiplier, int invulnerableSeconds,
                       int aggressionIntervalSeconds, double aggressionChance,
                       boolean addsEnabled, int addsPerWave, int addsMaxAlive,
                       double perEventDamageCap, double explosionTickBudget,
                       boolean blockCrystalPlacement, double entityCap,
                       double[] addsWaveThresholds) {
        this.enabled = enabled;
        this.applyToResummons = applyToResummons;
        this.phase1MaxHealth = clamp(phase1MaxHealth, 1.0, 100000.0);
        this.phase2MaxHealth = clamp(phase2MaxHealth, 1.0, 100000.0);
        this.phase2DamageMultiplier = clamp(phase2DamageMultiplier, 0.1, 10.0);
        this.invulnerableSeconds = (int) clamp(invulnerableSeconds, 0, 30);
        this.aggressionIntervalSeconds = (int) clamp(aggressionIntervalSeconds, 1, 300);
        this.aggressionChance = clamp(aggressionChance, 0.0, 1.0);
        this.addsEnabled = addsEnabled;
        this.addsPerWave = (int) clamp(addsPerWave, 0, 10);
        this.addsMaxAlive = (int) clamp(addsMaxAlive, 0, 30);
        this.perEventDamageCap = capValue(perEventDamageCap);
        this.explosionTickBudget = capValue(explosionTickBudget);
        this.blockCrystalPlacement = blockCrystalPlacement;
        this.entityCap = clamp(entityCap, 1.0, 100000.0);
        this.addsWaveThresholds = TrialStateMachine.sanitizeThresholds(addsWaveThresholds);
    }

    /** Pre-0.1.1 constructor shape: caps at their shipped defaults. */
    public TrialConfig(boolean enabled, boolean applyToResummons,
                       double phase1MaxHealth, double phase2MaxHealth,
                       double phase2DamageMultiplier, int invulnerableSeconds,
                       int aggressionIntervalSeconds, double aggressionChance,
                       boolean addsEnabled, int addsPerWave, int addsMaxAlive) {
        this(enabled, applyToResummons, phase1MaxHealth, phase2MaxHealth,
                phase2DamageMultiplier, invulnerableSeconds, aggressionIntervalSeconds,
                aggressionChance, addsEnabled, addsPerWave, addsMaxAlive,
                60.0, 75.0, true);
    }

    public static TrialConfig defaults() {
        // Phase 2 HP 500 (Mike, Oct 19, 2026 evening amendment: the
        // dragon is the campaign's first wall - 250 + 500 = 750 total;
        // the value reloads live with the chapter, so a slog can be
        // tuned down in place). Everything else stays at the 0.1.2
        // port values.
        return new TrialConfig(true, true, 250.0, 500.0, 1.5, 3, 7, 0.6, true, 3, 9,
                60.0, 75.0, true);
    }

    /** True when the P1 per-event cap is armed (value > 0). */
    public boolean perEventCapEnabled() {
        return perEventDamageCap > 0;
    }

    /** True when the P2 per-tick explosion budget is armed (value > 0). */
    public boolean explosionBudgetEnabled() {
        return explosionTickBudget > 0;
    }

    /**
     * Cap values are fail-soft OFF switches: zero, negative or NaN
     * disables the rule (the fight degrades toward vanilla, per the
     * class contract); positive values clamp to a sane ceiling.
     */
    private static double capValue(double v) {
        if (Double.isNaN(v) || v <= 0) {
            return 0.0;
        }
        return Math.min(v, 100000.0);
    }

    /** Build from flat config keys (the adapter passes config.yml values). */
    public static TrialConfig fromMap(Map<String, Object> m) {
        TrialConfig d = defaults();
        if (m == null) {
            return d;
        }
        return new TrialConfig(
                bool(m, "enabled", d.enabled),
                bool(m, "apply-to-resummons", d.applyToResummons),
                num(m, "phase1.max-health", d.phase1MaxHealth),
                num(m, "phase2.max-health", d.phase2MaxHealth),
                num(m, "phase2.damage-multiplier", d.phase2DamageMultiplier),
                (int) num(m, "transition.invulnerable-seconds", d.invulnerableSeconds),
                (int) num(m, "phase2.aggression-interval-seconds", d.aggressionIntervalSeconds),
                num(m, "phase2.aggression-chance", d.aggressionChance),
                bool(m, "adds.enabled", d.addsEnabled),
                (int) num(m, "adds.per-wave", d.addsPerWave),
                (int) num(m, "adds.max-alive", d.addsMaxAlive),
                num(m, "caps.per-event-damage", d.perEventDamageCap),
                num(m, "caps.explosion-per-tick", d.explosionTickBudget),
                bool(m, "caps.block-crystal-placement", d.blockCrystalPlacement),
                num(m, "entity-cap", d.entityCap),
                thresholds(m, "adds.wave-thresholds", d.addsWaveThresholds));
    }

    /** The add-wave threshold list (0.3.2): a YAML list of numbers,
     *  or a lone number for a single-wave fight; anything else (or
     *  an absent key) keeps the fallback. Sanitization happens in
     *  the constructor, so raw values pass through here. */
    private static double[] thresholds(Map<String, Object> m, String key, double[] fallback) {
        Object o = m.get(key);
        if (o instanceof java.util.List<?> list) {
            java.util.List<Double> nums = new java.util.ArrayList<>();
            for (Object e : list) {
                if (e instanceof Number n) {
                    nums.add(n.doubleValue());
                }
            }
            if (!nums.isEmpty()) {
                double[] out = new double[nums.size()];
                for (int i = 0; i < out.length; i++) {
                    out[i] = nums.get(i);
                }
                return out;
            }
            return fallback;
        }
        if (o instanceof Number n) {
            return new double[] {n.doubleValue()};
        }
        return fallback;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return lo;
        }
        return Math.max(lo, Math.min(hi, v));
    }

    private static boolean bool(Map<String, Object> m, String key, boolean fallback) {
        Object o = m.get(key);
        return o instanceof Boolean b ? b : fallback;
    }

    private static double num(Map<String, Object> m, String key, double fallback) {
        Object o = m.get(key);
        return o instanceof Number n ? n.doubleValue() : fallback;
    }
}

