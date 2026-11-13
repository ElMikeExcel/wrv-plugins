// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

/**
 * The teleport picker's pure logic, ported verbatim from
 * ExcelBedrockMenu 1.5.29's TeleportMenu: which Essentials request a
 * direction sends, and the exact words every step shows. The two
 * direction labels are Mike's (Nov 2, 2026), pinned byte-exact by
 * TeleportCoreTest.
 */
public final class TeleportCore {

    /** Which way the teleport goes once the target accepts. */
    public enum Direction {
        /** The picker travels to the picked player: Essentials /tpa. */
        TELEPORT_TO,
        /** The picked player travels to the picker: Essentials /tpahere. */
        COME_TO_ME
    }

    // Settled wording (Mike, Nov 2, 2026) — pinned byte-exact by tests.
    public static final String LABEL_TELEPORT_TO = "Teleport to player";
    public static final String LABEL_COME_TO_ME = "Ask player to come to me";
    public static final String CHOICE_BODY = "What do you want to do?";

    public static final String LIST_TITLE = "Teleport to a player";
    public static final String LIST_BODY =
            "Tap who you want to visit. They'll get a request to accept.";
    public static final String HEAD_LORE = "Click to ask to teleport";
    public static final String NOBODY_MESSAGE =
            "Nobody else is online right now. Try again when family joins!";
    public static final String LEFT_MESSAGE =
            "Looks like they just left the game. Try someone else!";
    public static final String ESSENTIALS_MISSING_MESSAGE =
            "Teleport requests aren't working right now. Ask an adult to check the server.";
    public static final String PROMPT_TITLE = "Teleport request";
    public static final String ACCEPT_LABEL = "Accept";
    public static final String DECLINE_LABEL = "Decline";
    public static final String DECLINE_HOVER = "Say no thanks";

    /** The Essentials request command for a direction, run AS the picker. */
    public static String requestCommand(Direction direction, String targetName) {
        return (direction == Direction.COME_TO_ME ? "tpahere " : "tpa ") + targetName;
    }

    /** The sentence the target reads in their answer prompt. */
    public static String promptText(Direction direction, String who) {
        return direction == Direction.COME_TO_ME
                ? who + " is asking you to teleport to them."
                : who + " wants to teleport to you.";
    }

    /** Java choice-chest lore (the name is the picked player's). */
    public static String choiceLoreTo(String name) {
        return "You go to " + name + ". They can accept or decline.";
    }

    public static String choiceLoreHere(String name) {
        return name + " comes to you. They can accept or decline.";
    }

    /** Hover text on the target's [Accept] button. */
    public static String acceptHover(Direction direction) {
        return direction == Direction.COME_TO_ME
                ? "Teleport to them" : "Let them come to you";
    }

    /** The target's answer when the picker has already left. */
    public static String answererLeftMessage(String fromName) {
        return fromName + " left the game, so there's nothing to answer.";
    }

    private TeleportCore() {}
}
