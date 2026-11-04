/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Command permission matrix (0.2.6 split): the status view
 * is public, reload/reset/anything else is admin-only.
 * Pins every cell of the matrix, the case-insensitivity of
 * the subcommand match, and the denial wording the adapter
 * sends when this gate refuses (Bukkit's default permission
 * message - what the plugin.yml gate sent through 0.2.5).
 */
public final class CommandAccessTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    private static String[] a(String... args) {
        return args;
    }

    public static void main(String[] args) {
        // --- The status view is a status call, for anyone. ---
        check(CommandAccess.isStatusCall(a()), "bare call is the status view");
        check(CommandAccess.isStatusCall(a("status")), "'status' is the status view");
        check(CommandAccess.isStatusCall(a("STATUS")), "'STATUS' matches case-insensitively");
        check(CommandAccess.isStatusCall(a("Status")), "'Status' matches case-insensitively");
        check(CommandAccess.isStatusCall(a("status", "extra")),
                "status with a stray extra arg is still the status view");

        // --- Non-status subcommands are not status calls. ---
        check(!CommandAccess.isStatusCall(a("reload")), "'reload' is not the status view");
        check(!CommandAccess.isStatusCall(a("reset")), "'reset' is not the status view");
        check(!CommandAccess.isStatusCall(a("reset", "all")), "'reset all' is not the status view");
        check(!CommandAccess.isStatusCall(a("bogus")), "an unknown subcommand is not the status view");
        check(!CommandAccess.isStatusCall(a("statusx")), "'statusx' is not the status view");

        // --- NON-ADMIN matrix: status OK, everything else denied. ---
        check(CommandAccess.allowed(a(), false), "non-op: bare status allowed");
        check(CommandAccess.allowed(a("status"), false), "non-op: status allowed");
        check(CommandAccess.allowed(a("STATUS"), false), "non-op: STATUS allowed");
        check(!CommandAccess.allowed(a("reload"), false), "non-op: reload DENIED");
        check(!CommandAccess.allowed(a("reset"), false), "non-op: reset DENIED");
        check(!CommandAccess.allowed(a("reset", "all"), false), "non-op: reset all DENIED");
        check(!CommandAccess.allowed(a("bogus"), false),
                "non-op: unknown subcommand DENIED (never reaches the usage line, as under the old gate)");

        // --- 0.3.6: the feedback toggle is a player's own display
        // setting - public like the status view, and NOT status. ---
        check(CommandAccess.isFeedbackCall(a("feedback")), "'feedback' is the feedback toggle");
        check(CommandAccess.isFeedbackCall(a("feedback", "off")), "'feedback off' is the feedback toggle");
        check(CommandAccess.isFeedbackCall(a("FEEDBACK", "ON")), "'FEEDBACK ON' matches case-insensitively");
        check(!CommandAccess.isFeedbackCall(a("feedbackx")), "'feedbackx' is not the feedback toggle");
        check(!CommandAccess.isFeedbackCall(a()), "a bare call is not the feedback toggle");
        check(!CommandAccess.isStatusCall(a("feedback", "off")), "'feedback off' is not the status view");
        check(CommandAccess.allowed(a("feedback"), false), "non-op: feedback allowed (own screen only)");
        check(CommandAccess.allowed(a("feedback", "off"), false), "non-op: feedback off allowed");
        check(CommandAccess.allowed(a("feedback", "on"), true), "op: feedback on allowed");

        // --- ADMIN matrix: everything allowed, byte-for-byte the old op surface. ---
        check(CommandAccess.allowed(a(), true), "op: bare status allowed");
        check(CommandAccess.allowed(a("status"), true), "op: status allowed");
        check(CommandAccess.allowed(a("reload"), true), "op: reload allowed");
        check(CommandAccess.allowed(a("reset"), true), "op: reset allowed");
        check(CommandAccess.allowed(a("reset", "all"), true), "op: reset all allowed");
        check(CommandAccess.allowed(a("bogus"), true),
                "op: unknown subcommand allowed through to the usage line");

        // --- The node + the denial wording are pinned. ---
        check(CommandAccess.ADMIN_PERMISSION.equals("excelbosstrial.admin"),
                "the admin node is excelbosstrial.admin (unchanged from plugin.yml)");
        check(CommandAccess.DENIAL_MESSAGE.equals(
                        "I'm sorry, but you do not have permission to perform this command. "
                        + "Please contact the server administrators if you believe that this is in error."),
                "the denial is Bukkit's default permission message, word for word");

        System.out.println("CommandAccessTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

