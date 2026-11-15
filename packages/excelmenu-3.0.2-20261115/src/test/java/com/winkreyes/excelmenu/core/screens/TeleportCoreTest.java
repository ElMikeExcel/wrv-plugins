// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * The teleport picker's direction mapping and prompt bytes, pinned
 * (the 1.5.29 AdapterLogicTest's role, carried into the unified
 * plugin): Mike's labels byte for byte, the Essentials command per
 * direction, and the target-facing sentence per direction.
 */
public class TeleportCoreTest {

    @Test
    public void settledLabelsAreByteExact() {
        assertEquals("Teleport to player", TeleportCore.LABEL_TELEPORT_TO);
        assertEquals("Ask player to come to me", TeleportCore.LABEL_COME_TO_ME);
        assertEquals("What do you want to do?", TeleportCore.CHOICE_BODY);
    }

    @Test
    public void backNavigationLabels() {
        // BB-01 (3.0.2): step 1 Back -> Server Menu (FIX-2); step 2
        // Back -> the step-1 picker (FIX-1). One Back label source.
        assertEquals("Back", TeleportCore.BACK_LABEL);
        assertEquals("Back to the player list", TeleportCore.BACK_TO_PICKER_LORE);
        assertEquals("Back to the Server Menu", TeleportCore.BACK_TO_SERVER_MENU_LORE);
    }

    @Test
    public void directionMapsToEssentialsCommand() {
        assertEquals("tpa Alex",
                TeleportCore.requestCommand(TeleportCore.Direction.TELEPORT_TO, "Alex"));
        assertEquals("tpahere Alex",
                TeleportCore.requestCommand(TeleportCore.Direction.COME_TO_ME, "Alex"));
    }

    @Test
    public void promptTextIsDirectionAware() {
        assertEquals("Sam wants to teleport to you.",
                TeleportCore.promptText(TeleportCore.Direction.TELEPORT_TO, "Sam"));
        assertEquals("Sam is asking you to teleport to them.",
                TeleportCore.promptText(TeleportCore.Direction.COME_TO_ME, "Sam"));
    }

    @Test
    public void choiceLoreNamesTheTarget() {
        assertEquals("You go to Alex. They can accept or decline.",
                TeleportCore.choiceLoreTo("Alex"));
        assertEquals("Alex comes to you. They can accept or decline.",
                TeleportCore.choiceLoreHere("Alex"));
    }

    @Test
    public void acceptHoverMatchesDirection() {
        assertEquals("Let them come to you",
                TeleportCore.acceptHover(TeleportCore.Direction.TELEPORT_TO));
        assertEquals("Teleport to them",
                TeleportCore.acceptHover(TeleportCore.Direction.COME_TO_ME));
        assertEquals("Say no thanks", TeleportCore.DECLINE_HOVER);
    }

    @Test
    public void staticCopyIsThePortedCopy() {
        assertEquals("Teleport to a player", TeleportCore.LIST_TITLE);
        assertEquals("Tap who you want to visit. They'll get a request to accept.",
                TeleportCore.LIST_BODY);
        assertEquals("Click to ask to teleport", TeleportCore.HEAD_LORE);
        assertEquals("Nobody else is online right now. Try again when family joins!",
                TeleportCore.NOBODY_MESSAGE);
        assertEquals("Looks like they just left the game. Try someone else!",
                TeleportCore.LEFT_MESSAGE);
        assertEquals("Teleport request", TeleportCore.PROMPT_TITLE);
        assertEquals("Accept", TeleportCore.ACCEPT_LABEL);
        assertEquals("Decline", TeleportCore.DECLINE_LABEL);
        assertEquals("Alex left the game, so there's nothing to answer.",
                TeleportCore.answererLeftMessage("Alex"));
    }
}
