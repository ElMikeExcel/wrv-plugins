package com.winkreyes.exceldowned;

/**
 * Pure policy for the 0.1.5 boot spell-template self-check
 * (P-DOWNED-REVIVE-1 Defect-2 template leg; diagnosis of record:
 * board-r427/downed-template-diagnosis/DIAGNOSIS.md §5).
 *
 * The defect: Magic 11.2.5 loads its spell registry in a DEFERRED
 * phase that completes ~7 s AFTER ExcelDowned's onEnable. The
 * pre-0.1.5 check probed synchronously inside onEnable, when the
 * registry was still empty, so getSpellTemplate returned null for
 * ANY key and the check emitted a false template WARN for both
 * probed keys on every boot.
 *
 * The policy: probe a known-good builtin CONTROL key ("heal")
 * alongside the target keys.
 *  - Control probe unavailable (null)  -> PROBE_UNAVAILABLE: the
 *    reflective probe itself could not run; distinct from a
 *    missing template (the UNPROVEN branch, kept distinct).
 *  - Control returns false             -> REGISTRY_NOT_LOADED:
 *    the registry is simply not populated yet; defer and retry,
 *    never a template WARN.
 *  - Control true, every target true   -> ALL_RESOLVE.
 *  - Control true, some target false   -> MISSING_KEYS: the
 *    registry IS populated, so a false target is genuinely absent
 *    — the fail-loud WARN fires for exactly those keys, once.
 *
 * Attempts are bounded: attempt delays land at +10 s, +20 s and
 * +40 s after enable (delays between attempts: 200, 200, 400
 * ticks). Only the final exhausted attempt may emit a WARNING,
 * and only MISSING_KEYS emits the per-key template WARN; a final
 * REGISTRY_NOT_LOADED / PROBE_UNAVAILABLE emits the distinct
 * UNPROVEN warning instead — never a false per-key WARN.
 *
 * No Bukkit: the plugin feeds probe results in and acts on the
 * verdict, so the regression tests exercise the production logic.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class TemplateCheckPolicy {

    private TemplateCheckPolicy() {}

    /** Known-good builtin spell probed as the registry-loaded control. */
    public static final String CONTROL_KEY = "heal";

    /** Bounded attempts: first at +10 s, retries at +20 s and +40 s. */
    public static final int MAX_ATTEMPTS = 3;

    /** Ticks to wait BEFORE each attempt (attempt 0, 1, 2). */
    public static final long[] ATTEMPT_DELAY_TICKS = {200L, 200L, 400L};

    public enum Verdict {
        PROBE_UNAVAILABLE,
        REGISTRY_NOT_LOADED,
        ALL_RESOLVE,
        MISSING_KEYS
    }

    public static boolean isFinalAttempt(int attempt) {
        return attempt >= MAX_ATTEMPTS - 1;
    }

    /** Ticks to wait before the given attempt; -1 when out of range. */
    public static long delayTicksForAttempt(int attempt) {
        if (attempt < 0 || attempt >= ATTEMPT_DELAY_TICKS.length) return -1;
        return ATTEMPT_DELAY_TICKS[attempt];
    }

    /**
     * Decide one attempt's outcome.
     *
     * @param controlResolves probe of the control key: TRUE resolves,
     *                        FALSE answered-but-absent, null = the
     *                        probe could not run at all
     * @param targetResolves  one probe per target key, same encoding
     *                        (a null target also means the probe
     *                        could not run — PROBE_UNAVAILABLE)
     */
    public static Verdict verdict(Boolean controlResolves, Boolean... targetResolves) {
        if (controlResolves == null) return Verdict.PROBE_UNAVAILABLE;
        for (Boolean t : targetResolves) {
            if (t == null) return Verdict.PROBE_UNAVAILABLE;
        }
        if (!controlResolves) return Verdict.REGISTRY_NOT_LOADED;
        for (Boolean t : targetResolves) {
            if (!t) return Verdict.MISSING_KEYS;
        }
        return Verdict.ALL_RESOLVE;
    }

    /** True when this verdict is settled now (no retry may follow). */
    public static boolean isSettled(Verdict v) {
        return v == Verdict.ALL_RESOLVE || v == Verdict.MISSING_KEYS;
    }

    /**
     * True when a WARNING may be emitted for this attempt: only the
     * final attempt of an unsettled verdict, or a settled
     * MISSING_KEYS verdict (which is final by construction — the
     * populated registry has given its answer).
     */
    public static boolean mayWarn(Verdict v, int attempt) {
        if (v == Verdict.MISSING_KEYS) return true;
        if (isSettled(v)) return false;
        return isFinalAttempt(attempt);
    }

    /** True when another attempt should be scheduled after this one. */
    public static boolean shouldRetry(Verdict v, int attempt) {
        return !isSettled(v) && !isFinalAttempt(attempt);
    }
}
