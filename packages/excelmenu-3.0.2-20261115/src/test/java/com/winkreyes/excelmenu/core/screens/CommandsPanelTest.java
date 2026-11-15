// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The /servercommands panel logic, pinned to the 1.5.28 port's
 * shapes over registry-shaped data: group resolution (number,
 * prefix, no match) and the panel lines for one group and for all.
 */
public class CommandsPanelTest {

    private static final List<String> GROUPS = List.of(
            "Getting Around", "Money & Jobs", "Skills & Levels", "Shops & Trading",
            "Claims & Land", "Guild & Quests", "Server Info", "Fixes & Help",
            "Your Items & Help");

    private static final List<List<CommandsPanel.Row>> ROWS = List.of(
            List.of(new CommandsPanel.Row("/spawn", "Teleport back to the village spawn")),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of());

    @Test
    public void groupResolution() {
        assertEquals(0, CommandsPanel.groupIndexFor("1", GROUPS));
        assertEquals(8, CommandsPanel.groupIndexFor("9", GROUPS));
        assertEquals(-1, CommandsPanel.groupIndexFor("10", GROUPS));
        assertEquals(-1, CommandsPanel.groupIndexFor("0", GROUPS));
        assertEquals(4, CommandsPanel.groupIndexFor("claims", GROUPS));
        assertEquals(4, CommandsPanel.groupIndexFor("CLAIMS", GROUPS));
        assertEquals(6, CommandsPanel.groupIndexFor("server", GROUPS));
        assertEquals(-1, CommandsPanel.groupIndexFor("nope", GROUPS));
        assertEquals(-1, CommandsPanel.groupIndexFor("", GROUPS));
        assertEquals(-1, CommandsPanel.groupIndexFor(null, GROUPS));
    }

    @Test
    public void singleGroupPanel() {
        List<String> lines = CommandsPanel.panelLines(0, GROUPS, ROWS);
        assertEquals("§6— Server Commands: Getting Around —", lines.get(0));
        assertEquals("§6Getting Around", lines.get(1));
        assertEquals("§b» §e/spawn §7- Teleport back to the village spawn",
                lines.get(2));
        assertEquals("§7/servercommands shows every group.",
                lines.get(lines.size() - 1));
    }

    @Test
    public void allGroupsPanel() {
        List<String> lines = CommandsPanel.panelLines(-1, GROUPS, ROWS);
        assertEquals("§6— Server Commands —", lines.get(0));
        assertTrue(lines.contains("§6Your Items & Help"));
        assertEquals("§7That is the whole list. In the menu, tap Server Commands "
                + "for tap-to-run buttons.", lines.get(lines.size() - 1));
    }
}
