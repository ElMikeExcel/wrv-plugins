package com.winkreyes.exceldowned;

/**
 * Console admin surface (ExcelDowned 0.1.2, Mike's Nov 3, 2026
 * console-admin rule; console-admin coverage audit Nov 9, 2026,
 * AUDIT.md §4 — the ExcelDowned packet).
 *
 * Through 0.1.1 an operator could INSPECT a downed player from the
 * console (/downed <player>) and reload settings, but had NO way to
 * fix a stuck downed session: no revive, no clear, no force-resolve.
 * A player wedged in the downed state (a revive that never lands, a
 * session restored from disk for a player who cannot be reached)
 * could only bleed out or be waited out. The audit graded 0.1.1 FAIL
 * on exactly that gap.
 *
 * 0.1.2 adds exactly the audit's packet, shaped on the AutoSort 0.2.1
 * / BuildMode 1.3.3 precedent (one minor bump, admin subcommands on
 * the existing command, harness tests for the new subcommands):
 *
 *   /downed revive <player>  — force-resolve an ONLINE downed session
 *                              without death, through the EXISTING
 *                              revive path (ExcelDowned.revive — the
 *                              same exactly-once path the Resurrection
 *                              spell uses; target returns at the
 *                              configured revive fraction, 50%).
 *   /downed clear <player>   — remove a downed session outright
 *                              (online OR offline-durable): state
 *                              surgery, not a heal and not a death.
 *                              No health is restored, no death runs,
 *                              no drops. This is the tool for a stuck
 *                              or offline session.
 *
 * Both are gated by the EXISTING node exceldowned.admin (plugin.yml,
 * default op — the audit's explicit condition: "gated by the existing
 * exceldowned.admin"). The console is allowed explicitly for these
 * admin forms only (the ExcelCosmetics / BrainGames shape:
 * console || hasPermission); the player-facing paths — /giveup, the
 * self status, the gated <player> inspection and reload — are
 * behavior-unchanged.
 *
 * Deliberate limit: revive requires the target ONLINE, because the
 * revive restores live health/presentation through the spell's path.
 * An offline-durable session is reported as such and left untouched;
 * clear is the offline tool.
 *
 * This class is the pure decision layer — routing, the gate, target
 * classification, and the revive/clear decisions — with no Bukkit
 * types, so the harness (DownedTest) exercises every branch. The
 * Bukkit mechanics (player/session lookup, the revive/clear calls,
 * messages) live in DownedCommands / ExcelDowned and stay thin.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class AdminPolicy {
    private AdminPolicy() { }

    /** The existing admin node. Registered in plugin.yml, default op. */
    public static final String PERMISSION = "exceldowned.admin";

    /** What a /downed invocation routes to. */
    public enum Route {
        /** Not a revive/clear invocation: the pre-0.1.2 surface
         *  (self status, gated <player> inspection, gated reload),
         *  unchanged. */
        LEGACY,
        /** revive <player>, gate passed. */
        ADMIN_REVIVE,
        /** clear <player>, gate passed. */
        ADMIN_CLEAR,
        /** revive/clear invocation with a missing/extra target. */
        ADMIN_USAGE,
        /** revive/clear invocation from a sender without the gate. */
        ADMIN_DENIED
    }

    /** A routed invocation; target is non-null only for ADMIN_REVIVE / ADMIN_CLEAR. */
    public record Parsed(Route route, String target) {
        static Parsed of(Route route) { return new Parsed(route, null); }
    }

    /**
     * The admin gate: the console always may (explicit, per the rule's
     * reference implementations); any other sender needs the node.
     * Bukkit's console satisfies hasPermission for an op-default node
     * anyway — the explicit console term keeps the intent true even if
     * a permission plugin ever re-resolves console grants.
     */
    public static boolean mayUseAdmin(boolean consoleSender, boolean holdsAdmin) {
        return consoleSender || holdsAdmin;
    }

    /** Is this first argument one of the 0.1.2 admin verbs? */
    public static boolean isAdminVerb(String arg) {
        return arg != null && ("revive".equalsIgnoreCase(arg) || "clear".equalsIgnoreCase(arg));
    }

    /**
     * Route one /downed invocation. Anything whose first argument is
     * not "revive"/"clear" is the legacy surface, byte-for-byte the
     * pre-0.1.2 behavior. A revive/clear invocation is decided here
     * completely: gate first (a sender without the gate learns
     * nothing about targets), then the exact one-target form.
     */
    public static Parsed route(String[] args, boolean consoleSender, boolean holdsAdmin) {
        if (args == null || args.length == 0 || !isAdminVerb(args[0]))
            return Parsed.of(Route.LEGACY);
        if (!mayUseAdmin(consoleSender, holdsAdmin)) return Parsed.of(Route.ADMIN_DENIED);
        if (args.length == 2 && !args[1].isBlank()) {
            return new Parsed("revive".equalsIgnoreCase(args[0])
                    ? Route.ADMIN_REVIVE : Route.ADMIN_CLEAR, args[1]);
        }
        return Parsed.of(Route.ADMIN_USAGE);
    }

    /** Where a named admin target was found. */
    public enum TargetKind {
        /** Online with a live downed session: revive and clear both act. */
        ONLINE_DOWNED,
        /** Online but not downed: a clean no-op for both verbs. */
        ONLINE_NOT_DOWNED,
        /** Not online, but a durable downed session exists under that
         *  name (persisted in downed.yml): clear acts on the record;
         *  revive cannot (it restores live health) and changes nothing. */
        OFFLINE_DOWNED,
        /** No online player and no downed session under that name. */
        UNKNOWN
    }

    public static TargetKind targetKind(boolean online, boolean onlineDowned, boolean durableDowned) {
        if (online) return onlineDowned ? TargetKind.ONLINE_DOWNED : TargetKind.ONLINE_NOT_DOWNED;
        return durableDowned ? TargetKind.OFFLINE_DOWNED : TargetKind.UNKNOWN;
    }

    /** Whether an admin revive may proceed for a classified target. */
    public enum ReviveDecision {
        /** Run the existing revive path now. */
        PROCEED,
        /** Target is online but not downed: clean no-op. */
        NOT_DOWNED,
        /** Target's session is offline-durable: revive needs a live
         *  player; report it and change nothing (clear is the tool). */
        OFFLINE_ONLY,
        /** No such player or session: report unknown, change nothing. */
        UNKNOWN
    }

    public static ReviveDecision decideRevive(TargetKind kind) {
        return switch (kind) {
            case ONLINE_DOWNED -> ReviveDecision.PROCEED;
            case ONLINE_NOT_DOWNED -> ReviveDecision.NOT_DOWNED;
            case OFFLINE_DOWNED -> ReviveDecision.OFFLINE_ONLY;
            case UNKNOWN -> ReviveDecision.UNKNOWN;
        };
    }

    /** Whether an admin clear may proceed for a classified target. */
    public enum ClearDecision {
        /** Remove the session now (online session or offline record). */
        PROCEED,
        /** Target is online but not downed: clean no-op. */
        NOT_DOWNED,
        /** No such player or session: report unknown, change nothing. */
        UNKNOWN
    }

    public static ClearDecision decideClear(TargetKind kind) {
        return switch (kind) {
            case ONLINE_DOWNED, OFFLINE_DOWNED -> ClearDecision.PROCEED;
            case ONLINE_NOT_DOWNED -> ClearDecision.NOT_DOWNED;
            case UNKNOWN -> ClearDecision.UNKNOWN;
        };
    }
}
