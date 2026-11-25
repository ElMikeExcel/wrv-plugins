package com.winkreyes.exceldowned;

import org.bukkit.ChatColor;

/**
 * The DOWNED name mark, in one place (1.0.4).
 *
 * While a player is downed, ExcelDowned shows "✝ DOWNED <name>" as
 * their display and player-list name. That mark is presentation only —
 * it must never be mistaken for the player's identity. Magic wands
 * proved the point the hard way (Nov 23, 2026): a display-name-bound
 * wand derives its owner string from the player's display name, so a
 * wand saved while its owner was downed came back with the owner
 * "✝ DOWNED <name>", failed Magic's owner check at the next login,
 * and was silently deleted.
 *
 * STRIPPED_PREFIX is the mark exactly as other plugins end up storing
 * it after color codes are stripped. ExcelMagicGate's wand-owner
 * repair matches owner strings against this same literal; unit tests
 * on both sides pin the two to the incident string so they can never
 * drift apart.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DownedMark {

    /** The mark with no color codes: "✝ DOWNED " (trailing space). */
    public static final String STRIPPED_PREFIX = "✝ DOWNED ";

    private DownedMark() {}

    /** The marked display/list name for a downed player. */
    public static String markedDisplay(String realName) {
        return ChatColor.RED + STRIPPED_PREFIX + ChatColor.RESET + realName;
    }

    /** The marked name as other plugins store it (colors stripped). */
    public static String strippedMarked(String realName) {
        return STRIPPED_PREFIX + realName;
    }
}
