// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * The childproof picker's pure text logic, ported verbatim from
 * ExcelBedrockMenu 1.5.28's ChildproofMenu (its Bukkit/GriefPrevention
 * reads stay in the Bukkit screen; the words live here so the tests
 * pin them). Nothing here changes trust itself: the screen runs
 * "/childproof <name>" AS the player, so GriefPrevention does all
 * permission checks and prints its own confirmations.
 */
public final class ChildproofCore {

    public static final String TITLE = "Childproof a player";
    public static final String YES = "Yes, childproof";
    public static final String NO = "Cancel";
    public static final String LIST_BODY =
            "Tap who should only use doors and buttons in this claim (no building, breaking or chests).";
    public static final String NOT_SET_UP_MESSAGE =
            "§eChildproof isn't set up on this server yet. Ask an adult to check the server.";
    public static final String OUTSIDE_CLAIM_MESSAGE =
            "§eStand inside your claim first, then try again (or type it, like /childproof Alex). It only changes the claim you're standing in.";
    public static final String NOBODY_MESSAGE =
            "§eNobody else is online right now. You can also type the name, like /childproof Alex (example).";
    public static final String TARGET_LEFT_MESSAGE = "§eThat player left. Nothing was changed.";
    public static final String JAVA_LIST_HEADER =
            "§6Childproof: click a name, then press Enter. Doors and buttons only, in this claim.";

    /** Help text, always with a concrete example. */
    public static String[] helpLines() {
        return new String[]{
            "§6Childproof §7- let little ones use doors, buttons and beds in your claim without breaking stuff.",
            "§eStand in your claim and type §a/childproof Alex §7(example - type the player's name, not a nickname)",
            "§eUndo it: §a/unchildproof Alex§7. Full trust again: §a/trust Alex",
            "§eOn a phone? Tap §a/childproofmenu§e and pick the player from a list.",
        };
    }

    /**
     * The one confirmation line after the alias ran, given the
     * verified state: "access", "none", "other:<level>", "outside"
     * or "unknown" (the Bukkit screen's GriefPrevention read).
     */
    public static String resultLine(boolean childproof, String name, String state) {
        if (state.equals("outside"))
            return "§eYou weren't standing in a claim, so this changed ALL your claims. To change just one, stand in it and type §a/" + (childproof ? "childproof " : "unchildproof ") + name;
        if (childproof && state.equals("access"))
            return "§aDone! " + name + " can open doors, press buttons and use beds in this claim, but can't build, break things or open chests. Undo: §e/unchildproof " + name;
        if (!childproof && state.equals("none"))
            return "§aDone! " + name + " has no access to this claim now. Childproof again: §e/childproof " + name;
        if (state.startsWith("other:"))
            return "§e" + name + " still has " + state.substring(6) + " access here (maybe from public or a group trust). Check §a/trustlist§e.";
        return "§eCheck the messages above. Type the player's exact name, like §a/" + (childproof ? "childproof " : "unchildproof ") + "Alex§e, while standing in your claim.";
    }

    /** Body text of the confirm form for a picked name. */
    public static String confirmContent(String name) {
        return name + " will only be able to use doors, buttons, levers and beds in this claim. "
                + "They can't build, break things or open chests here. "
                + "Undo: /unchildproof " + name + ". Full trust again: /trust " + name + ".";
    }

    /** The modal title for a picked name. */
    public static String confirmTitle(String name) {
        return "Childproof " + name + "?";
    }

    /** The fallback line when the confirm form cannot be sent. */
    public static String typeItMessage(String name) {
        return "§eType /childproof " + name + " to do it.";
    }

    /** The exact command run as the player for a picked name. */
    public static String childproofCommand(String name) {
        return "childproof " + name;
    }

    /**
     * Button label: nickname plus the real name when they differ
     * (".KidPlayer" shows as "Alex (.KidPlayer)"). The display name
     * arrives already colour-stripped.
     */
    public static String label(String strippedDisplay, String name) {
        String d = strippedDisplay == null ? "" : strippedDisplay.trim();
        String core = d.startsWith("~") ? d.substring(1) : d; // Essentials nick prefix
        if (core.isEmpty() || core.equalsIgnoreCase(name)) return name;
        return core + " (" + name + ")";
    }

    private ChildproofCore() {}
}
