package com.winkreyes.excelmagicgate;

/**
 * The wand-loss safety net's decisions, pure and pinned by tests
 * (0.3.5).
 *
 * The defect family this guards: a Magic wand BOUND to its owner but
 * not physically carried, while the mage record still holds it. That
 * state is how both real wand losses presented (Nov 23 and Nov 24,
 * 2026 — the "✝ DOWNED" name-mark seam): Magic deleted the carried
 * item during login processing and never re-materializes a wand from
 * its own mage record, so without a net the wand is simply gone
 * until an operator restores it by hand. Magic DOES ship the cure —
 * Mage.restoreWand(), the backend of its own /wand restore command:
 * for every bound wand whose template is absent from the player's
 * inventory AND ender chest, it re-gives a copy from the record.
 * MagicGate's reconcile already detects exactly this state; 0.3.5
 * acts on it.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class WandLossNet {

    /**
     * Greppable log token for the loss signature. A bound wand that
     * is neither carried nor pouch-parked is the earliest observable
     * signal of a silent wand deletion — it is logged at WARNING,
     * never as a routine line.
     */
    public static final String SIGNATURE = "WAND-LOSS-SIGNATURE";

    private WandLossNet() {}

    /**
     * The loss signature: a bound-wand record exists (the mage record
     * or a bound list entry) AND the wand-access classification says
     * no live wand is carried and it is not parked in the Utility
     * Pouch. A player with no bound record at all is simply not a
     * wand user — never a signature.
     */
    public static boolean isLossSignature(boolean hasBoundRecord,
            FloorPlan.WandAccess access) {
        return hasBoundRecord && access == FloorPlan.WandAccess.NO_LIVE_WAND;
    }

    /**
     * Whether to attempt the re-issue on this detection. All must
     * hold: the signature is present; the net is enabled in config;
     * no attempt has been made yet this login session (restoreWand
     * is itself presence-checked, so a repeat could never duplicate
     * — the latch exists to keep attempts and logs to one per
     * session); and the player is alive (a dead player mid-respawn
     * is retried at a later reconcile leg instead of consuming the
     * session's attempt).
     */
    public static boolean shouldAttemptRestore(boolean signature, boolean enabled,
            boolean attemptedThisSession, boolean playerDead) {
        return signature && enabled && !attemptedThisSession && !playerDead;
    }
}
