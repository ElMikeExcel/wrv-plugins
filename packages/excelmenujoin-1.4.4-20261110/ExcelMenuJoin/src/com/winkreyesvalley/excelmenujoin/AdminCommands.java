package com.winkreyesvalley.excelmenujoin;

import java.util.UUID;

/**
 * The console-admin surface for one player's ExcelMenuJoin state
 * (1.4.4, Mike's Nov 3, 2026 console-admin rule; the console-admin
 * coverage audit of Nov 9, 2026 found ExcelMenuJoin a FAIL —
 * audited at 1.4.1, re-verified still true at 1.4.3 —
 * because /excelmenujoin and /compass were both self-only and
 * player-gated, so an operator at the console could neither
 * inspect nor fix another player's join-menu state, nor hand
 * them a replacement compass):
 *
 *   /excelmenujoin admin status <player>
 *   /excelmenujoin admin toggle <player>
 *   /excelmenujoin admin compass <player>
 *
 * All three forms are gated by the plugin.yml permission
 * {@link #PERMISSION} (default: op). The server console is
 * explicitly allowed ({@link #allowed}); any other sender needs
 * the permission. The check lives in code, on the admin branch
 * only, because /excelmenujoin itself must stay open to every
 * player for the self-service toggle form.
 *
 * What status reports — and what toggle flips — is exactly the
 * plugin's real state: the player's join auto-open flag, which
 * lives in the player's own PersistentDataContainer
 * (excelmenujoin:autojoin_disabled, with the legacy
 * menujoin:autojoin_disabled fallback — see ExcelMenuJoin
 * isDisabled/setDisabled, the same calls the self toggle and
 * %menujoin_status% use), plus, for status, whether a menu
 * compass is currently in the player's inventory
 * (ExcelMenuJoin.hasMenuCompass). Compass gives through the
 * plugin's single give path (giveMenuCompass), so the 1.4.0
 * Utility Pouch rule applies to an admin give exactly as it
 * does to /compass: a player who already holds a compass — in
 * hand or parked in the pouch — is never handed a duplicate.
 *
 * STATE-MODEL LIMIT, stated plainly: the auto-open flag lives
 * ON the player (PDC), not in any plugin-owned file or map, and
 * this plugin keeps no name-to-UUID record of its own. Bukkit
 * can only read or write a player's PDC while that player is
 * online, so all three admin forms take an ONLINE player by
 * exact name; there is no offline form and none is invented.
 * (Compass needs the target online by definition in any case —
 * the item goes into a live inventory.)
 *
 * No Bukkit dependencies: the gate decision and the exact reply
 * texts live here and are unit-tested on a plain JVM; the
 * plugin class only resolves the online target, checks the
 * sender, performs the read/flip/give through its existing
 * methods, and delivers the message. Status is read-only by
 * construction: it formats state the caller already read and
 * mutates nothing.
 */
public final class AdminCommands {

    /** The admin permission node (plugin.yml, default: op). */
    public static final String PERMISSION = "excelmenujoin.admin";

    /** Usage line for the admin status form. */
    public static final String USAGE_STATUS = "Usage: /excelmenujoin admin status <player>";

    /** Usage line for the admin toggle form. */
    public static final String USAGE_TOGGLE = "Usage: /excelmenujoin admin toggle <player>";

    /** Usage line for the admin compass form. */
    public static final String USAGE_COMPASS = "Usage: /excelmenujoin admin compass <player>";

    /** Reply when a non-console sender lacks the admin permission. */
    public static final String DENIED =
            "You do not have permission to do that. (excelmenujoin.admin required)";

    private AdminCommands() { }

    /**
     * The gate decision: the console is always allowed (the
     * explicit console path, the ExcelCosmetics pattern); any
     * other sender needs the admin permission. In Bukkit/Paper
     * the console also satisfies hasPermission for an op-default
     * node — the explicit branch keeps the intent testable and
     * independent of that behaviour.
     */
    public static boolean allowed(boolean isConsole, boolean hasAdminPermission) {
        return isConsole || hasAdminPermission;
    }

    private static String who(String displayName, UUID playerId) {
        String name = (displayName == null || displayName.isBlank())
                ? "that player" : displayName;
        String id = playerId == null ? "unknown uuid" : playerId.toString();
        return name + " (" + id + ")";
    }

    /**
     * The one-line status reply for one ONLINE player, from the
     * plugin's real state: the join auto-open flag (the inverse
     * of the stored disabled flag) and whether a menu compass is
     * currently in their inventory. Read-only.
     */
    public static String statusMessage(String displayName, UUID playerId,
                                       boolean autoOpen, boolean compassHeld) {
        return "MenuJoin status for " + who(displayName, playerId)
                + ": join auto-open " + (autoOpen ? "ON" : "OFF")
                + "; menu compass " + (compassHeld ? "held." : "not held.");
    }

    /**
     * The one-line confirmation after an admin toggle flip,
     * naming the auto-open state before and after so the log
     * shows what actually changed. The caller performs the flip
     * itself (ExcelMenuJoin.setDisabled) — this only formats.
     *
     * @param wasAutoOpen the target's auto-open state before the flip
     */
    public static String toggleMessage(String displayName, UUID playerId, boolean wasAutoOpen) {
        return "MenuJoin auto-open for " + who(displayName, playerId)
                + " set to " + (wasAutoOpen ? "OFF" : "ON")
                + " (was " + (wasAutoOpen ? "ON" : "OFF") + ").";
    }

    /** Confirmation when the give path handed over a compass. */
    public static String compassGivenMessage(String displayName, UUID playerId) {
        return "Gave a Server Menu compass to " + who(displayName, playerId) + ".";
    }

    /**
     * Reply when the give path gave nothing: the single give
     * path refuses exactly when the player already holds a
     * compass, in the inventory or parked in the Utility Pouch
     * (the 1.4.0 rule) — the reply says so rather than guessing
     * which of the two it was.
     */
    public static String compassNotGivenMessage(String displayName, UUID playerId) {
        return "No compass given to " + who(displayName, playerId)
                + ": they already hold one (inventory or Utility Pouch).";
    }

    /** Rejection when a status target is not an online player. */
    public static String notFoundStatus(String target) {
        return "No online player named '" + target
                + "'. MenuJoin state lives on the player, so admin commands need the player online. Nothing was looked up.";
    }

    /** Rejection when a toggle target is not an online player. */
    public static String notFoundToggle(String target) {
        return "No online player named '" + target
                + "'. MenuJoin state lives on the player, so admin commands need the player online. Nothing was changed.";
    }

    /** Rejection when a compass target is not an online player. */
    public static String notFoundCompass(String target) {
        return "No online player named '" + target
                + "'. A compass can only be given to a player who is online. Nothing was given.";
    }
}
