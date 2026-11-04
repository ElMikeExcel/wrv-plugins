// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The childproof picker's pure helpers, pinned to the 1.5.28 port:
 * the label shape (nick + real name), the confirm body, the exact
 * command, and every resultLine branch.
 */
public class ChildproofCoreTest {

    @Test
    public void labelShowsNickPlusRealName() {
        assertEquals("KidPlayer (.KidPlayer)",
                ChildproofCore.label("KidPlayer", ".KidPlayer"));
        assertEquals("Alex", ChildproofCore.label("Alex", "Alex"));
        assertEquals("Alex", ChildproofCore.label("~Alex", "Alex"));
        assertEquals("Alex", ChildproofCore.label("", "Alex"));
        assertEquals("Alex", ChildproofCore.label(null, "Alex"));
        assertEquals("Kid (Alex)", ChildproofCore.label("~Kid", "Alex"));
    }

    @Test
    public void commandAndConfirmCopy() {
        assertEquals("childproof Alex", ChildproofCore.childproofCommand("Alex"));
        assertEquals("Childproof Alex?", ChildproofCore.confirmTitle("Alex"));
        assertEquals("Alex will only be able to use doors, buttons, levers and beds "
                + "in this claim. They can't build, break things or open chests here. "
                + "Undo: /unchildproof Alex. Full trust again: /trust Alex.",
                ChildproofCore.confirmContent("Alex"));
        assertEquals("Childproof a player", ChildproofCore.TITLE);
        assertEquals("Yes, childproof", ChildproofCore.YES);
        assertEquals("Cancel", ChildproofCore.NO);
    }

    @Test
    public void resultLineBranches() {
        assertTrue(ChildproofCore.resultLine(true, "Alex", "outside")
                .contains("changed ALL your claims"));
        assertTrue(ChildproofCore.resultLine(true, "Alex", "access")
                .startsWith("§aDone! Alex can open doors"));
        assertTrue(ChildproofCore.resultLine(false, "Alex", "none")
                .startsWith("§aDone! Alex has no access"));
        assertTrue(ChildproofCore.resultLine(true, "Alex", "other:build")
                .contains("still has build access"));
        assertTrue(ChildproofCore.resultLine(true, "Alex", "unknown")
                .contains("Check the messages above"));
        // The childproof success line must not fire for an unchildproof.
        assertTrue(ChildproofCore.resultLine(false, "Alex", "access")
                .contains("Check the messages above"));
    }

    @Test
    public void helpLinesCarryTheNeutralExample() {
        String[] lines = ChildproofCore.helpLines();
        assertEquals(4, lines.length);
        assertTrue(lines[1].contains("/childproof Alex"));
        assertTrue(lines[3].contains("/childproofmenu"));
    }
}
