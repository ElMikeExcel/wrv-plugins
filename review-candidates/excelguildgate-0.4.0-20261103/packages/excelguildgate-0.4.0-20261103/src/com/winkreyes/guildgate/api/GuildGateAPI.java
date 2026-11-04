package com.winkreyes.guildgate.api;

import com.winkreyes.excelguildgate.SealState;

/**
 * Public, read-only state API for ExcelGuildGate ("The Road to the Guild").
 *
 * Other plugins (for example ExcelBedrockMenu's dynamic button label) read
 * the gate state through here, typically by reflection so they keep no hard
 * dependency on this plugin. Every method is null-safe: before the plugin
 * has loaded its state (or after it disables), the gate reads as locked
 * with zero progress.
 *
 * The bind/unbind methods are internal wiring used by the plugin itself;
 * consumers should only call the getters.
 */
public final class GuildGateAPI {

    private static volatile SealState state;

    private GuildGateAPI() {}

    /** Internal: called by ExcelGuildGate once its state is loaded. */
    public static void bind(SealState loadedState) {
        state = loadedState;
    }

    /** Internal: called by ExcelGuildGate on disable. */
    public static void unbind() {
        state = null;
    }

    /** True when every enabled seal is broken and the guild is open. */
    public static boolean isUnlocked() {
        SealState s = state;
        return s != null && s.isUnlocked();
    }

    /** Enabled seals broken so far (the progress numerator). */
    public static int sealsBrokenCount() {
        SealState s = state;
        return s == null ? 0 : s.brokenCount();
    }

    /** Enabled seals required for the unlock (the progress denominator). */
    public static int sealsTotalCount() {
        SealState s = state;
        return s == null ? 0 : s.requiredCount();
    }

    /**
     * True when the named seal is currently broken (v0.3.3).
     * Unknown or null ids read as unbroken. This is the live
     * cross-check for consumers that keep their own seal-side
     * state (for example ExcelBossTrial's elder approval latch):
     * a /guildgate revoke lands here immediately, so a consumer
     * can notice its cached approval no longer matches the gate
     * instead of waving the next credit through on stale state.
     */
    public static boolean isBroken(String sealId) {
        SealState s = state;
        return s != null && s.isBroken(sealId);
    }
}
