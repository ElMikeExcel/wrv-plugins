// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * Ask the Guide's pure logic, ported verbatim from ExcelBedrockMenu
 * 1.5.28. Java's button prints a hint; Bedrock gets the same thing as
 * a form: one text box, and Send runs /ask <question> as the player
 * (the exact command Java players type).
 */
public final class AskCore {

    public static final String ASK_TITLE = "Ask the Guide";
    public static final String ASK_PROMPT =
            "Type your question. The Guide answers from the server's own pages.";
    public static final String ASK_PLACEHOLDER = "How do I claim land?";
    public static final String ASK_HINT =
            "§7Type §e/ask §7and your question in chat, or start a message with §eGuide,";
    public static final String ASK_EMPTY =
            "§eNo question typed. Tap Ask the Guide again, or type /ask and your question in chat.";
    public static final int ASK_MAX = 200;
    public static final String ASK_PERMISSION = "excelguide.ask";
    public static final String GUIDE_MISSING_MESSAGE =
            "§eThe Guide isn't available right now. Ask an adult to check the server.";
    public static final String GUIDE_DENIED_MESSAGE =
            "§eYou can't ask the Guide right now. Ask an adult to check the server.";

    /**
     * The command line Send runs, or null when nothing usable was
     * typed. Section codes and control characters are removed,
     * spaces collapsed, a leading slash dropped (so a typed command
     * never runs as itself; it is only ever asked as a question),
     * and the length capped at ASK_MAX.
     */
    public static String askCommandFor(String typed) {
        if (typed == null) return null;
        String q = stripCodes(typed).replaceAll("[\\p{Cntrl}]", " ")
                .replaceAll("\\s+", " ").trim();
        while (q.startsWith("/")) q = q.substring(1).trim();
        if (q.isEmpty()) return null;
        if (q.length() > ASK_MAX) q = q.substring(0, ASK_MAX).trim();
        return "ask " + q;
    }

    /** ExcelBedrockMenu's stripCodes: section-sign pairs removed. */
    public static String stripCodes(String s) {
        return s == null ? null : s.replaceAll("§.", "");
    }

    /**
     * The guarded-run outcome (ExcelBedrockMenu's guardOutcome):
     * 0 = the command doesn't exist, 1 = the player may not run it,
     * 2 = run it.
     */
    public static int guardOutcome(boolean commandExists, boolean hasPermission) {
        if (!commandExists) return 0;
        if (!hasPermission) return 1;
        return 2;
    }

    private AskCore() {}
}
