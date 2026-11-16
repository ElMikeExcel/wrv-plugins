/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Who may do what with /bosstrial (0.2.6 PERMISSION SPLIT).
 *
 * The status view is PUBLIC: a bare /bosstrial or /bosstrial
 * status is the check the seal system itself points players
 * at (seal state, credit cooldowns), so any player may run
 * it. The 0.3.6 feedback toggle is public for the same
 * reason: /bosstrial feedback on|off is a player's OWN
 * display setting (their damage totals, their boss bar
 * audience) and touches nothing of anyone else's. The 0.4.1
 * stats view is public for the same reason again:
 * /bosstrial stats re-shows the CALLER's own most recent
 * fight record and no one else's - a player reading their
 * own numbers needs no admin node. Everything
 * else the command exposes - reload, reset, and any unknown
 * subcommand, which for an admin falls through to the usage
 * line - is an admin action and needs the
 * excelbosstrial.admin node, the node that gated the WHOLE
 * command in plugin.yml through 0.2.5 (which is why players
 * could not run the status view at all).
 *
 * Pure and Bukkit-free so the permission matrix is pinned
 * by the JVM suite; the adapter only supplies the sender's
 * admin flag and sends DENIAL_MESSAGE when this class
 * refuses. The denial wording is Bukkit's own default
 * permission message, word for word - the refusal a player
 * sees does not change with the split, only who can see
 * the status view does.
 */
public final class CommandAccess {

    /** The permission node every non-status subcommand needs. */
    public static final String ADMIN_PERMISSION = "excelbosstrial.admin";

    /**
     * The denial a non-admin sees for an admin subcommand:
     * Bukkit's default permission message, exactly what the
     * plugin.yml command gate sent through 0.2.5.
     */
    public static final String DENIAL_MESSAGE =
            "I'm sorry, but you do not have permission to perform this command. "
            + "Please contact the server administrators if you believe that this is in error.";

    private CommandAccess() {
    }

    /** True when the call is the public status view. */
    public static boolean isStatusCall(String[] args) {
        return args.length == 0 || "status".equalsIgnoreCase(args[0]);
    }

    /** True when the call is the public per-player feedback
     *  toggle (0.3.6): /bosstrial feedback [on|off]. */
    public static boolean isFeedbackCall(String[] args) {
        return args.length >= 1 && "feedback".equalsIgnoreCase(args[0]);
    }

    /** True when the call is the public per-player stats
     *  view (0.4.1, ㉚.9): /bosstrial stats - the caller's
     *  own most recent fight record. */
    public static boolean isStatsCall(String[] args) {
        return args.length >= 1 && "stats".equalsIgnoreCase(args[0]);
    }

    /**
     * True when the call may proceed: the status view, the
     * feedback toggle and the stats view for anyone, anything
     * else only for an admin.
     */
    public static boolean allowed(String[] args, boolean admin) {
        return isStatusCall(args) || isFeedbackCall(args) || isStatsCall(args) || admin;
    }
}

