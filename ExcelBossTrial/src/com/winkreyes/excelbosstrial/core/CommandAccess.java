package com.winkreyes.excelbosstrial.core;

/**
 * Who may do what with /bosstrial (0.2.6 PERMISSION SPLIT).
 *
 * The status view is PUBLIC: a bare /bosstrial or /bosstrial
 * status is the check the seal system itself points players
 * at (seal state, credit cooldowns), so any player may run
 * it. Everything else the command exposes - reload, reset,
 * and any unknown subcommand, which for an admin falls
 * through to the usage line - is an admin action and needs
 * the excelbosstrial.admin node, the node that gated the
 * WHOLE command in plugin.yml through 0.2.5 (which is why
 * players could not run the status view at all).
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

    /**
     * True when the call may proceed: the status view for
     * anyone, anything else only for an admin.
     */
    public static boolean allowed(String[] args, boolean admin) {
        return isStatusCall(args) || admin;
    }
}

