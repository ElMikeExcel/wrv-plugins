package com.winkreyes.buildmode.core;

/**
 * Console admin surface (ExcelBuildMode 1.3.3, Mike's Nov 3, 2026
 * console-admin rule; console-admin audit Nov 9, 2026, AUDIT.md §4).
 *
 * Through 1.3.2 the sole command /buildmode [on|off|status] rejected
 * every non-player sender ("Use /buildmode in game. It only changes
 * your own mode.") and declared no admin permission node — so a stuck
 * Build Mode session (the durable state this plugin exists to guard)
 * had NO console rescue: an operator could neither inspect a builder's
 * session nor force its clean Survival restore.
 *
 * 1.3.3 adds exactly the audit's packet, shaped on the AutoSort 0.2.1
 * precedent (one minor bump, admin subcommands on the existing
 * command, harness tests for the new subcommands):
 *
 *   /buildmode admin status <player>  — inspect that player's mode /
 *                                       durable session state.
 *   /buildmode admin off <player>     — force a clean Survival restore
 *                                       through the EXISTING recovery
 *                                       path (ExcelBuildMode.recover ->
 *                                       SessionCoordinator.recover). No
 *                                       second restore implementation
 *                                       exists; this class only decides
 *                                       whether the admin act may run.
 *
 * Both are gated by the NEW node excelbuildmode.admin, declared in
 * plugin.yml with default op — deliberately NOT default false like the
 * use/bigpaste nodes: an operator (or the console) holds it by the
 * ordinary op rule, a non-op builder does not. The admin branch is the
 * ONLY branch a non-player sender may use, and the console is allowed
 * explicitly (the ExcelCosmetics / ExcelBrainGames shape:
 * console bypass, everyone else needs the node) rather than relying on
 * console permission resolution alone.
 *
 * This class is the pure decision layer — routing, the gate, target
 * classification, and the force-off decision — with no Bukkit types,
 * so the harness (AdminTests) exercises every branch. The Bukkit
 * mechanics (player lookup, the recover() call, messages) live in the
 * paper class and stay thin.
 */
public final class AdminPolicy {
    private AdminPolicy() { }

    /** Our own admin node. Registered in plugin.yml, default op. */
    public static final String PERMISSION = "excelbuildmode.admin";

    /** What a /buildmode invocation routes to. */
    public enum Route {
        /** Not an admin invocation: the pre-1.3.3 player paths, unchanged. */
        PLAYER_PATH,
        /** admin status <player>, gate passed. */
        ADMIN_STATUS,
        /** admin off <player>, gate passed. */
        ADMIN_OFF,
        /** admin invocation with a missing/unknown subcommand or target. */
        ADMIN_USAGE,
        /** admin invocation from a sender without the admin gate. */
        ADMIN_DENIED
    }

    /** A routed invocation; target is non-null only for ADMIN_STATUS / ADMIN_OFF. */
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

    /**
     * Route one invocation. Anything whose first argument is not
     * "admin" is the player path, byte-for-byte the pre-1.3.3 surface
     * (the paper class still rejects non-player senders there). An
     * "admin" invocation is decided here completely: gate first (a
     * sender without the gate learns nothing about the subcommand
     * shapes beyond the denial), then the exact two forms.
     */
    public static Parsed route(String[] args, boolean consoleSender, boolean holdsAdmin) {
        if (args == null || args.length == 0 || !"admin".equalsIgnoreCase(args[0]))
            return Parsed.of(Route.PLAYER_PATH);
        if (!mayUseAdmin(consoleSender, holdsAdmin)) return Parsed.of(Route.ADMIN_DENIED);
        if (args.length == 3 && "status".equalsIgnoreCase(args[1]) && !args[2].isBlank())
            return new Parsed(Route.ADMIN_STATUS, args[2]);
        if (args.length == 3 && "off".equalsIgnoreCase(args[1]) && !args[2].isBlank())
            return new Parsed(Route.ADMIN_OFF, args[2]);
        return Parsed.of(Route.ADMIN_USAGE);
    }

    /** Where a named admin target was found. */
    public enum TargetKind {
        /** Online right now: status reads live state; off runs the live restore. */
        ONLINE,
        /** Not online, but a durable Build Mode record exists for the UUID
         *  (active journal session or review hold): status reports it; off
         *  reports that the live restore runs at the next join, which is
         *  the existing recovery path for an offline session. */
        DURABLE_ONLY,
        /** No online player and no record under that name at all. */
        UNKNOWN
    }

    public static TargetKind targetKind(boolean online, boolean durableRecord) {
        if (online) return TargetKind.ONLINE;
        return durableRecord ? TargetKind.DURABLE_ONLY : TargetKind.UNKNOWN;
    }

    /** Whether an admin off for an ONLINE target may proceed, and why not. */
    public enum OffDecision {
        /** Run the existing recovery path now. */
        PROCEED,
        /** Target holds no guarded session: a clean no-op, reported as such. */
        NOT_GUARDED,
        /** Target is under a persistent review hold. recover() itself
         *  refuses these (fail-closed); the admin surface reports the
         *  hold instead of forcing past it — a hold clears only by the
         *  administrator verification that created it, never by the
         *  rescue command. */
        REVIEW_HOLD,
        /** Recovery storage is not verified ready: no restore is attempted. */
        STORAGE_UNAVAILABLE
    }

    /**
     * The force-off decision for an online target, in the same guard
     * order recover() applies: nothing to do beats everything; a review
     * hold beats a storage report (the hold is the fact an operator
     * must act on); storage readiness gates the attempt itself.
     */
    public static OffDecision decideOff(boolean guarded, boolean reviewHold, boolean recoveryReady) {
        if (!guarded) return OffDecision.NOT_GUARDED;
        if (reviewHold) return OffDecision.REVIEW_HOLD;
        if (!recoveryReady) return OffDecision.STORAGE_UNAVAILABLE;
        return OffDecision.PROCEED;
    }
}
