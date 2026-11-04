// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The Skills & Perks copy, pinned to the 1.5.28 port: the Magic line
 * is the sealed text only at zero broken seals, carries the live
 * count (capped at 5) after that, and the body keeps the spec's
 * numbers and commands.
 */
public class SkillsPerksCoreTest {

    @Test
    public void magicLineSealedAtZero() {
        assertEquals("Magic is sealed for now. It arrives as the Adventurers Guild "
                + "seals fall (0 of 5 today).", SkillsPerksCore.skillsPerksMagicLine(0));
        assertEquals(SkillsPerksCore.skillsPerksMagicLine(0),
                SkillsPerksCore.skillsPerksMagicLine(-3));
    }

    @Test
    public void magicLineCountsAndCaps() {
        assertTrue(SkillsPerksCore.skillsPerksMagicLine(1)
                .startsWith("Magic is awake: 1 of 5 seals broken."));
        assertTrue(SkillsPerksCore.skillsPerksMagicLine(3)
                .startsWith("Magic is awake: 3 of 5 seals broken."));
        assertTrue(SkillsPerksCore.skillsPerksMagicLine(9)
                .startsWith("Magic is awake: 5 of 5 seals broken."));
    }

    @Test
    public void contentCarriesTheSpecCopy() {
        String content = SkillsPerksCore.skillsPerksContent(2);
        assertTrue(content.contains("19 skills and 83 perks"));
        assertTrue(content.contains("1 to 1,000"));
        assertTrue(content.contains("/mcstats shows all your levels."));
        assertTrue(content.contains("/mctop shows the leaderboards."));
        assertTrue(content.endsWith(SkillsPerksCore.skillsPerksMagicLine(2)));
    }

    @Test
    public void buttonsAndFollowOn() {
        assertEquals("My mcMMO stats", SkillsPerksCore.BUTTONS[SkillsPerksCore.MCSTATS]);
        assertEquals("Skill Trees on the Website",
                SkillsPerksCore.BUTTONS[SkillsPerksCore.WEBSITE]);
        assertEquals("Close", SkillsPerksCore.BUTTONS[2]);
        assertTrue(SkillsPerksCore.skillsPerksWebsiteContent()
                .startsWith("elmikeexcel.github.io/winkreyesvalley"));
    }
}
