package com.winkreyesvalley.excelhalloweentoggles;

/**
 * ExcelHalloweenToggles — the fire gate, as pure logic.
 *
 * Every input is gathered live at fire time from the Halloween plugin's
 * own public surface (its global on/off, its isEligible() — which folds
 * in the player's /halloween disable, the world blacklist, vanish and
 * spectator filters and the WorldGuard flag) plus the companion's own
 * state (event window, scheduler switch, the player's toggle). Keeping
 * the decision pure makes the whole matrix unit-testable without a
 * server, which is where the "a toggled-off scare never fires, not even
 * in the Witching Hour" guarantee is actually proven.
 */
public final class ScareGate {

    public enum Decision {
        /** All gates pass: fire through the Halloween plugin now, then reschedule. */
        FIRE,
        /** Temporarily blocked (Halloween globally off, or the player is
         *  momentarily ineligible): do not fire, retry on the short cadence. */
        RETRY,
        /** Definitively off (event retired, scheduler disabled, or the
         *  player's toggle is off): do not fire, end the chain. */
        STOP
    }

    private ScareGate() { }

    public static Decision decide(boolean eventActive, boolean schedulerEnabled,
                                  boolean globalOn, boolean toggleOn, boolean eligible) {
        if (!eventActive || !schedulerEnabled) {
            return Decision.STOP;
        }
        if (!toggleOn) {
            return Decision.STOP;
        }
        if (!globalOn || !eligible) {
            return Decision.RETRY;
        }
        return Decision.FIRE;
    }

    /** The login-pumpkin gate: every condition must hold. */
    public static boolean shouldApplyLogin(boolean eventActive, boolean globalOn,
                                           boolean toggleOn, boolean eligible,
                                           boolean cooldownOk) {
        return eventActive && globalOn && toggleOn && eligible && cooldownOk;
    }
}
