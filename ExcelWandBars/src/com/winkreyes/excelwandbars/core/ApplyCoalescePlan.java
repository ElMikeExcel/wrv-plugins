package com.winkreyes.excelwandbars.core;

/**
 * 0.3.16: coalescing for self-inflicted wand-style applies (diagnosis
 * round 2, Nov 12, 2026, fix item F4). Pure (no Bukkit or Magic types),
 * unit-tested on the plain JVM; StyleHook supplies the live state.
 *
 * The defect: every apply ends with deactivate + checkWand, which fires
 * WandActivatedEvent again. On Nov 12 that re-entry burned ApplyGuard
 * tokens (4 per 10 s), re-ran the full configure each time, and — once a
 * layout failure started scheduling verify retries — produced
 * overlapping chains ("retry already pending") and a THROTTLED flood
 * that also starved genuine activations (the player's own tap, the
 * join-settle apply).
 *
 * The rules here:
 *  - A fresh (non-retry) apply that arrives while a verify retry is
 *    already pending for the player, or while an apply for the player
 *    is in flight, is coalesced: Result.ALREADY, no guard token spent.
 *    The pending retry re-reads the then-current wand, so nothing is
 *    lost by skipping the duplicate.
 *  - An activation within {@link #SELF_APPLY_WINDOW_TICKS} ticks of our
 *    own apply, with no fresh player-input stamp (hotbar pick / icon
 *    tap) after that apply, is our own deactivate + checkWand echo and
 *    is not scheduled at all. A genuine pick or tap in between defeats
 *    the skip.
 *  - Retries stay forced and token-free (0.3.15, unchanged), and the
 *    ApplyGuard itself is unchanged for genuine external bursts.
 */
public final class ApplyCoalescePlan {
    private ApplyCoalescePlan() {}

    /** An activation this many ticks (or fewer) after our own apply can be that apply's echo. */
    public static final long SELF_APPLY_WINDOW_TICKS = 2;

    /**
     * True when a fresh apply should be coalesced into work already in
     * flight for the player instead of spending an ApplyGuard token.
     * Retries are never coalesced: the pending flag is theirs, and the
     * retry pass is the one that clears it.
     */
    public static boolean coalesce(boolean isRetry, boolean retryPending, boolean applyInFlight) {
        return !isRetry && (retryPending || applyInFlight);
    }

    /**
     * True when a WandActivatedEvent is the echo of our own apply: it
     * falls within the window after the apply, and no player-input
     * stamp (pick/tap) is newer than the apply itself.
     *
     * @param activationTick   server tick of the activation event
     * @param lastSelfApplyTick server tick of our last apply (-1 = none)
     * @param lastInputMs      newest player pick/tap stamp, wall-clock ms (0 = none)
     * @param selfApplyMs      wall-clock ms of our last apply (0 = none)
     */
    public static boolean selfEcho(long activationTick, long lastSelfApplyTick, long lastInputMs, long selfApplyMs) {
        if (lastSelfApplyTick < 0) return false;
        long dt = activationTick - lastSelfApplyTick;
        if (dt < 0 || dt > SELF_APPLY_WINDOW_TICKS) return false;
        return lastInputMs <= selfApplyMs;
    }
}
