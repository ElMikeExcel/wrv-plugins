package com.winkreyes.excelbosstrial.dragon;

/**
 * The phase state machine for ONE End world's dragon battle. Pure
 * logic, no Bukkit: the adapter feeds it damage numbers and entity
 * facts, and acts on its verdicts.
 *
 * THE SEAL RULE (SPEC Step 0): GuildGate 0.3.0 credits the
 * ender_dragon seal from EntityDeathEvent alone. So a death may be
 * permitted only in PHASE2, and only once. In PHASE1 a would-be
 * lethal blow returns FLIP and the adapter cancels the damage event
 * and heals the dragon synchronously — health never reaches 0, the
 * death event never exists, the seal cannot credit.
 */
public final class TrialStateMachine {

    /** What the adapter should do with a damage event against the dragon. */
    public enum Verdict {
        /** Let the damage through untouched. */
        PASS,
        /** Phase-1 lethal blow: cancel it and run the phase flip. */
        FLIP,
        /** Phase-2 lethal blow: let it through; this is the one real death. */
        ALLOW_DEATH
    }

    /** Phase-2 add-wave thresholds, as health fractions, high to low. */
    public static final double[] WAVE_THRESHOLDS = {0.75, 0.50, 0.25};

    private BattlePhase phase = BattlePhase.NONE;
    private String dragonUuid;
    private int wavesFiredMask;
    private boolean deathConsumed;
    private boolean invulnerableByUs;

    // ---------------------------------------------------------------
    // Battle lifecycle
    // ---------------------------------------------------------------

    public boolean hasBattle() {
        return phase != BattlePhase.NONE && phase != BattlePhase.DONE && dragonUuid != null;
    }

    public boolean tracks(String uuid) {
        return hasBattle() && dragonUuid.equals(uuid);
    }

    /** Register a brand-new battle (fresh dragon): phase 1, clean slate. */
    public void startBattle(String uuid) {
        this.dragonUuid = uuid;
        this.phase = BattlePhase.PHASE1;
        this.wavesFiredMask = 0;
        this.deathConsumed = false;
        this.invulnerableByUs = false;
    }

    /** The tracked dragon is gone and the battle is over. */
    public void endBattle() {
        this.phase = BattlePhase.DONE;
        this.dragonUuid = null;
        this.invulnerableByUs = false;
    }

    // ---------------------------------------------------------------
    // Damage verdicts — the heart of the seal guarantee
    // ---------------------------------------------------------------

    /**
     * Decide a damage event against the tracked dragon.
     *
     * @param currentHealth the dragon's health BEFORE this blow
     * @param finalDamage   the event's final damage
     */
    public Verdict onDamage(double currentHealth, double finalDamage) {
        if (!hasBattle()) {
            return Verdict.PASS;
        }
        boolean lethal = finalDamage >= currentHealth && currentHealth > 0;
        switch (phase) {
            case PHASE1:
                if (lethal) {
                    phase = BattlePhase.TRANSITION;
                    invulnerableByUs = true;
                    return Verdict.FLIP;
                }
                return Verdict.PASS;
            case TRANSITION:
                // The adapter cancels all damage during the transition
                // (invulnerable flag + explicit cancel); nothing here
                // may ever be lethal.
                return Verdict.PASS;
            case PHASE2:
                if (lethal && !deathConsumed) {
                    deathConsumed = true;
                    return Verdict.ALLOW_DEATH;
                }
                return Verdict.PASS;
            default:
                return Verdict.PASS;
        }
    }

    /** Transition finished: the dragon is fighting on its phase-2 bar. */
    public void completeTransition() {
        if (phase == BattlePhase.TRANSITION) {
            phase = BattlePhase.PHASE2;
            invulnerableByUs = false;
        }
    }

    // ---------------------------------------------------------------
    // Phase-2 add waves
    // ---------------------------------------------------------------

    /** Bit i is set when the fraction has crossed WAVE_THRESHOLDS[i]. */
    public static int crossedMask(double healthFraction) {
        int mask = 0;
        for (int i = 0; i < WAVE_THRESHOLDS.length; i++) {
            if (healthFraction <= WAVE_THRESHOLDS[i]) {
                mask |= (1 << i);
            }
        }
        return mask;
    }

    /** Thresholds crossed at this fraction that have not yet fired. */
    public int pendingWaveMask(double healthFraction) {
        if (phase != BattlePhase.PHASE2) {
            return 0;
        }
        return crossedMask(healthFraction) & ~wavesFiredMask;
    }

    /** Record waves as actually spawned (the adapter may spawn fewer). */
    public void commitWaves(int mask) {
        wavesFiredMask |= mask;
    }

    public int wavesFiredMask() {
        return wavesFiredMask;
    }

    // ---------------------------------------------------------------
    // Outgoing damage (phase-2 buff) and aggression
    // ---------------------------------------------------------------

    /** True when damage dealt BY this battle's dragon should be scaled. */
    public boolean buffApplies(BattlePhase inPhase, DamagerKind kind) {
        return inPhase == BattlePhase.PHASE2 && kind != null && kind != DamagerKind.OTHER;
    }

    public boolean buffApplies(DamagerKind kind) {
        return buffApplies(phase, kind);
    }

    /** Scale a damage amount by the config multiplier (core-side so it is tested). */
    public static double scaledDamage(double damage, TrialConfig cfg) {
        return damage * cfg.phase2DamageMultiplier;
    }

    // ---------------------------------------------------------------
    // Persistence
    // ---------------------------------------------------------------

    public BattlePhase phase() {
        return phase;
    }

    public String dragonUuid() {
        return dragonUuid;
    }

    public boolean deathConsumed() {
        return deathConsumed;
    }

    public boolean invulnerableByUs() {
        return invulnerableByUs;
    }

    public void setInvulnerableByUs(boolean v) {
        this.invulnerableByUs = v;
    }

    /**
     * Restore from persisted fields. A battle saved mid-TRANSITION
     * resolves to PHASE2 (its dragon was already healed onto the
     * phase-2 bar before the save; the invulnerable window simply
     * ends early — the adapter's reconcile clears the stray flag).
     */
    public void restore(BattlePhase savedPhase, String uuid, int wavesMask,
                        boolean deathConsumed, boolean invulnerableByUs) {
        this.phase = savedPhase == BattlePhase.TRANSITION ? BattlePhase.PHASE2 : savedPhase;
        this.dragonUuid = uuid;
        this.wavesFiredMask = wavesMask;
        this.deathConsumed = deathConsumed;
        // Kept as persisted even though the phase resolved to PHASE2:
        // the entity may still be wearing the invulnerable flag we set,
        // and the adapter's reconcile uses this to clear it exactly once.
        this.invulnerableByUs = invulnerableByUs;
    }
}

