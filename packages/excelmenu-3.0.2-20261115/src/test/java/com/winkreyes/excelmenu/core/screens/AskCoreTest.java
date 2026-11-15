// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Ask text box's sanitizer, pinned to the 1.5.28 port: a typed
 * command never runs as itself (leading slashes dropped), codes and
 * control characters are removed, whitespace collapses, and the
 * question caps at ASK_MAX before the "ask " prefix is added.
 */
public class AskCoreTest {

    @Test
    public void plainQuestionPassesThrough() {
        assertEquals("ask How do I claim land?",
                AskCore.askCommandFor("How do I claim land?"));
    }

    @Test
    public void nothingUsableIsNull() {
        assertNull(AskCore.askCommandFor(null));
        assertNull(AskCore.askCommandFor(""));
        assertNull(AskCore.askCommandFor("   "));
        assertNull(AskCore.askCommandFor("///"));
        assertNull(AskCore.askCommandFor("§a§b"));
    }

    @Test
    public void leadingSlashesAreDropped() {
        assertEquals("ask spawn", AskCore.askCommandFor("/spawn"));
        assertEquals("ask spawn", AskCore.askCommandFor("  //  spawn"));
    }

    @Test
    public void codesAndControlsAreRemovedAndSpacesCollapse() {
        assertEquals("ask what is this?",
                AskCore.askCommandFor("§ewhat   is\n\tthis?§7"));
    }

    @Test
    public void lengthCapsAtAskMax() {
        String typed = "x".repeat(AskCore.ASK_MAX + 50);
        String line = AskCore.askCommandFor(typed);
        assertTrue(line.startsWith("ask "));
        assertEquals(AskCore.ASK_MAX, line.length() - "ask ".length());
    }

    @Test
    public void guardOutcomeMatrix() {
        assertEquals(0, AskCore.guardOutcome(false, false));
        assertEquals(0, AskCore.guardOutcome(false, true));
        assertEquals(1, AskCore.guardOutcome(true, false));
        assertEquals(2, AskCore.guardOutcome(true, true));
    }

    @Test
    public void askCopyIsThePortedCopy() {
        assertEquals("Ask the Guide", AskCore.ASK_TITLE);
        assertEquals("Type your question. The Guide answers from the server's own pages.",
                AskCore.ASK_PROMPT);
        assertEquals("How do I claim land?", AskCore.ASK_PLACEHOLDER);
        assertEquals(200, AskCore.ASK_MAX);
        assertEquals("excelguide.ask", AskCore.ASK_PERMISSION);
    }
}
