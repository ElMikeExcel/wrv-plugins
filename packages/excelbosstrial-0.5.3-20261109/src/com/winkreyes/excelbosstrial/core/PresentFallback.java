/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The auto-present Bedrock fallback decision (ExcelBossTrial
 * 0.4.0, the Wither-night fix train, PM-11). The retry window
 * exhausted on a Java player ends with one informational chat
 * line. A Bedrock (Floodgate) player gets one step more: the
 * inventory route that kept failing is exactly the route the
 * wand bar blocks on Bedrock, so at window end the present
 * falls back to a Floodgate form prompt (accept -> the claims
 * menu opens through the form answer, not through the blocked
 * inventory state).
 *
 * This class owns ONLY the decision (the adapter owns the
 * reflective Floodgate seam and the form itself): the form is
 * offered exactly when the player is a Floodgate player, the
 * retry window has exhausted, and there is still an unpresented
 * batch waiting - never mid-window (the retry loop's whole
 * point is that the normal route usually recovers), never for
 * a Java player, never with nothing to present.
 *
 * Pure logic, unit-tested directly.
 */
public final class PresentFallback {

    private PresentFallback() {
    }

    /** True when the exhausted-window present should offer the
     *  Bedrock form fallback for this player and batch. */
    public static boolean offerForm(boolean floodgatePlayer, boolean windowExhausted,
                                    boolean batchWaiting) {
        return floodgatePlayer && windowExhausted && batchWaiting;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
