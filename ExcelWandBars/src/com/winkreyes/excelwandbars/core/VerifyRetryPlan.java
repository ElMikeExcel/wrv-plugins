package com.winkreyes.excelwandbars.core;

import java.util.Locale;
import java.util.Set;

/**
 * 0.3.15: the wand-style verify race (diagnosis Nov 12, 2026). Pure
 * (no Bukkit or Magic types), unit-tested on the plain JVM; StyleHook
 * supplies the live readings and does the scheduling.
 *
 * The defect: StyleHook.apply writes the style's control keys (and, when
 * needed, the hotbar layout), then deactivates and re-checks the wand.
 * Magic rebuilds the wand from the item as the server sees it at that
 * instant, and on a Bedrock (Geyser) session the server-side item stack
 * churns, so the rebuilt wand can still show the PRE-write values even
 * though the write landed. The old code returned FAILED on that first
 * reading; the failure re-fired Magic's activation event, which applied
 * again, failed again, and throttled — and every attempt closed the
 * player's spell bar first, so the bar would not stay up.
 *
 * The rule here: a verify reading whose ONLY differences are the keys
 * (or the layout) this very apply just wrote is not a failure, it is
 * the race — schedule ONE delayed re-check/re-apply instead (at most
 * {@link #MAX_RETRIES} per chain, {@link #RETRY_DELAY_TICKS} later).
 * A difference this apply did NOT write is a real mismatch and fails
 * at once. Retries are forced past the ApplyGuard on purpose (the
 * original apply already paid a guard token, and a throttled retry
 * would silently lose the fix); the retry budget itself — capped,
 * one pending per player, StyleHook-enforced — is the storm guard
 * for this path, and every chain terminates.
 *
 * The close rule ({@link #shouldCloseInventory}) is the other half:
 * Magic's configure sequence closes the wand inventory before writing,
 * which is correct for a deliberate switch but destructive when the
 * trigger is the player opening the bar, and on any retry — a FAILED
 * verdict must never be what tore down the bar the player just opened.
 */
public final class VerifyRetryPlan {
    private VerifyRetryPlan() {}

    /** Delay before a verify retry re-checks the wand (half a second). */
    public static final long RETRY_DELAY_TICKS = 10L;

    /** Retries allowed after the first apply in one chain; then FAILED. */
    public static final int MAX_RETRIES = 2;

    public enum Verdict { APPLIED, RETRY, FAILED }

    /** Which verify branch(es) still differ after the write. */
    public enum Branch { NONE, PROPS, LAYOUT, PROPS_AND_LAYOUT }

    public static Branch branch(boolean propsBad, boolean layoutBad) {
        if (propsBad && layoutBad) return Branch.PROPS_AND_LAYOUT;
        if (propsBad) return Branch.PROPS;
        if (layoutBad) return Branch.LAYOUT;
        return Branch.NONE;
    }

    /**
     * True when every residual control-property difference is a key this
     * apply itself just wrote (the rebuilt wand read the pre-write item).
     * An empty residual is not a failure at all and returns false here;
     * {@link #verdict} handles it as APPLIED before consulting this.
     */
    public static boolean propsRetryable(Set<String> justWritten, Set<String> residual) {
        return residual != null && !residual.isEmpty()
                && justWritten != null && justWritten.containsAll(residual);
    }

    /**
     * The post-write verify decision.
     *
     * @param justWritten  control keys this apply wrote (its diff)
     * @param residualProps control keys still differing after the write
     * @param layoutWritten this apply wrote the hotbar layout
     * @param layoutBad    the layout still does not match after the write
     * @param retriesUsed  retries already spent in this chain (0 = first apply)
     */
    public static Verdict verdict(Set<String> justWritten, Set<String> residualProps,
                                  boolean layoutWritten, boolean layoutBad, int retriesUsed) {
        boolean propsBad = residualProps != null && !residualProps.isEmpty();
        if (!propsBad && !layoutBad) return Verdict.APPLIED;
        boolean retryable = true;
        if (propsBad && !propsRetryable(justWritten, residualProps)) retryable = false;
        if (layoutBad && !layoutWritten) retryable = false;
        if (retryable && retriesUsed < MAX_RETRIES) return Verdict.RETRY;
        return Verdict.FAILED;
    }

    /**
     * Triggers that mean "the player is opening / has just opened the
     * spell bar": a WandActivatedEvent apply ("activate") and the
     * TapBarHook open path's layout repair ("bar open"). A verify retry
     * ("verify retry") is a continuation of such an apply and counts
     * too — retries never close (see {@link #shouldCloseInventory}).
     */
    public static boolean isBarOpenTrigger(String why) {
        if (why == null) return false;
        String w = why.trim().toLowerCase(Locale.ROOT);
        return w.equals("activate") || w.equals("bar open") || w.equals("verify retry");
    }

    /**
     * May this apply close the wand inventory before configuring?
     * Only when the apply will actually change something, the trigger
     * is not a bar-open activation, and this pass is not a verify
     * retry. A verify-only failure path (a retry, or any pass in a
     * bar-open chain) therefore never closes the bar.
     */
    public static boolean shouldCloseInventory(boolean willChange, String why, boolean retry) {
        if (!willChange) return false;
        if (retry) return false;
        return !isBarOpenTrigger(why);
    }
}
