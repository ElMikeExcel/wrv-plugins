package com.winkreyes.exceldowned;

/**
 * Pure policy for the Cleric "Raise Ally" companion ability
 * (ExcelDowned 1.0.0, design B — board-r506/revive/B-CLERIC-REVIVE-
 * DESIGN.md sections 3 and 5; GRACE route D107; Mike's amended
 * baseline D108).
 *
 * No Bukkit types, no EliteMobs types: every branch the command /
 * service layers apply is decided here so the harness (DownedTest)
 * exercises production logic, in the FixPolicy / RevivePotionPolicy
 * house pattern. The Bukkit + EM mechanics (live reads through the
 * ClericGate seam, target enumeration, the revive call, the GRACE
 * spend) live in ClericRevive / EmClericGate / ExcelDowned and stay
 * thin.
 *
 * Rules pinned here:
 *  - Gate order = design section 5.1: every check is read-only and
 *    stops at the first failure; NOTHING is charged unless the
 *    revive itself lands (step 7). A refused attempt spends no
 *    GRACE and starts no cooldown.
 *  - Class gate: the active lineage ROOT must be Cleric (any Cleric
 *    form keeps the root-kit companion — D-B1) and the resource must
 *    be GRACE (the Cleric pool trySpend will deduct from).
 *  - Unlock gate: EM level >= config unlock-em-level (Mike/D108:
 *    10, the Cleric root unlock). An unreported level never unlocks.
 *  - Party rule (D-B3): NO Together-party requirement by default;
 *    the gate exists behind config require-party (default false) so
 *    the ruling is reversible in one edit. When required, membership
 *    must be positively PARTY — UNKNOWN never counts.
 *  - Cooldown (D108, supersedes design 120 s / D106): 600 s,
 *    success-only, per caster. The value is config-fed; nothing is
 *    hardcoded here.
 *  - GRACE affordability uses EM's own epsilon (the spike's guard
 *    analysis: trySpend refuses when amount + 1.0E-9 < cost).
 *  - Ordering invariant (design 5.1 step 7): GRACE is charged and
 *    the cooldown starts if and only if the revive landed —
 *    shouldCharge(reviveLanded) is the whole rule.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ClericRevivePolicy {

    private ClericRevivePolicy() {}

    /** The lineage root id of the Cleric tree (EM catalog id). */
    public static final String CLERIC_ROOT_ID = "cleric";

    /** The Cleric resource type name (EM ClassResourceType). */
    public static final String GRACE_TYPE = "GRACE";

    /** Internal ability id (design section 3 — house namespace,
     *  deliberately NOT an EM registry id). */
    public static final String ABILITY_ID = "excel.cleric.raise_ally";

    // ---------- class + unlock gates ----------

    /** True when the active lineage root is the Cleric root. */
    public static boolean isClericRoot(String rootId) {
        return rootId != null && CLERIC_ROOT_ID.equalsIgnoreCase(rootId.trim());
    }

    /** True when the active resource is the Cleric GRACE pool. */
    public static boolean isGrace(String resourceType) {
        return resourceType != null && GRACE_TYPE.equalsIgnoreCase(resourceType.trim());
    }

    /**
     * The unlock gate: the player's EM level (active lineage
     * effective level) must reach the config unlock level. An
     * unreported level (-1) never unlocks — a missing read is a
     * refusal, not a pass.
     */
    public static boolean levelUnlocked(int emLevel, int unlockLevel) {
        return emLevel >= 0 && unlockLevel >= 0 && emLevel >= unlockLevel;
    }

    /** GRACE affordability, EM's own math (amount + 1e-9 >= cost). */
    public static boolean graceAffordable(Double amount, double cost) {
        return amount != null && Double.isFinite(amount)
                && Double.isFinite(cost) && cost >= 0
                && amount + 1.0E-9 >= cost;
    }

    // ---------- the gate order (design 5.1) ----------

    /** Why a cast attempt was refused (the service maps each to one
     *  plain-words line). OK means every pre-check passed and the
     *  revive may be attempted. */
    public enum Verdict {
        OK,
        FEATURE_DISABLED,
        MODULE_UNAVAILABLE,
        MODULE_NOT_READY,
        NO_ACTIVE_CLASS,
        NOT_CLERIC,
        LEVEL_TOO_LOW,
        CASTER_DOWNED,
        CASTER_DEAD,
        ON_COOLDOWN,
        NO_TARGET_IN_RANGE,
        NOT_PARTY,
        GRACE_SHORT
    }

    /**
     * The section 5.1 decision table, in order. All inputs are
     * re-derived live at attempt time by the service (probe state,
     * fresh ClericView, caster state, cooldown, target existence at
     * config range, GRACE snapshot) — nothing rendered or cached
     * earlier is trusted.
     *
     * @param targetInRange    a downed ally exists in range (party
     *                         already applied when requireParty)
     * @param targetIsParty    the picked target is positively PARTY
     *                         (only consulted when requireParty)
     */
    public static Verdict verdict(boolean enabled, ClericGate.Probe probe,
                                  boolean hasActiveClass, boolean clericRoot,
                                  boolean gracePool, boolean levelOk,
                                  boolean casterDowned, boolean casterDead,
                                  boolean cooldownActive,
                                  boolean targetInRange, boolean requireParty,
                                  boolean targetIsParty,
                                  boolean graceAffordable) {
        if (!enabled) return Verdict.FEATURE_DISABLED;
        if (probe == null || probe == ClericGate.Probe.UNAVAILABLE) {
            return Verdict.MODULE_UNAVAILABLE;
        }
        if (probe == ClericGate.Probe.NOT_INITIALIZED) {
            return Verdict.MODULE_NOT_READY;
        }
        if (!hasActiveClass) return Verdict.NO_ACTIVE_CLASS;
        if (!clericRoot || !gracePool) return Verdict.NOT_CLERIC;
        if (!levelOk) return Verdict.LEVEL_TOO_LOW;
        if (casterDowned) return Verdict.CASTER_DOWNED;
        if (casterDead) return Verdict.CASTER_DEAD;
        if (cooldownActive) return Verdict.ON_COOLDOWN;
        if (!targetInRange) return Verdict.NO_TARGET_IN_RANGE;
        if (requireParty && !targetIsParty) return Verdict.NOT_PARTY;
        if (!graceAffordable) return Verdict.GRACE_SHORT;
        return Verdict.OK;
    }

    // ---------- target picking (design section 3 cast shape) ----------

    /**
     * Pick the target index: the aimed-at eligible candidate wins;
     * otherwise the NEAREST eligible candidate; -1 when none is
     * eligible. Eligibility (downed, same world, in range, party
     * when required, never the caster) is computed by the caller
     * into {@code eligible}; {@code distanceSquared} is only read
     * for eligible entries. A single eligible candidate is picked
     * whether aimed or not (the design's "single downed ally in
     * range" case falls out of nearest-of-one).
     */
    public static int pickTarget(int aimedIndex, boolean[] eligible,
                                 double[] distanceSquared) {
        if (eligible == null || distanceSquared == null) return -1;
        int n = Math.min(eligible.length, distanceSquared.length);
        if (aimedIndex >= 0 && aimedIndex < n && eligible[aimedIndex]) {
            return aimedIndex;
        }
        int best = -1;
        double bestSq = Double.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            if (eligible[i] && distanceSquared[i] < bestSq) {
                bestSq = distanceSquared[i];
                best = i;
            }
        }
        return best;
    }

    // ---------- ordering invariant + cooldown (design 5.1/5.4) ----------

    /**
     * THE no-burn rule: GRACE is charged and the cooldown starts if
     * and only if the revive landed. A lost race (potion / spell /
     * operator / bleed-out / give-up in the same instant) charges
     * nothing — the caster is told "already raised."
     */
    public static boolean shouldCharge(boolean reviveLanded) {
        return reviveLanded;
    }

    /** Per-caster cooldown: active only after a SUCCESS (D108:
     *  600 s default, config-fed; separate from the potion's). */
    public static boolean cooldownActive(long nowMs, long lastSuccessMs,
                                         long cooldownMs) {
        return lastSuccessMs > 0 && cooldownMs > 0
                && nowMs - lastSuccessMs < cooldownMs;
    }

    /** Remaining cooldown millis (0 when not cooling). */
    public static long cooldownRemainingMs(long nowMs, long lastSuccessMs,
                                           long cooldownMs) {
        if (lastSuccessMs <= 0 || cooldownMs <= 0) return 0;
        return Math.max(0, cooldownMs - (nowMs - lastSuccessMs));
    }
}
