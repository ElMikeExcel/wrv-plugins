// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.core.screens;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The /servercommands chat panel, ported from ExcelBedrockMenu
 * 1.5.28's commandsPanelLines / catalogGroupIndexFor — same design
 * language (gold headers, aqua markers, yellow commands, gray
 * descriptions), but the rows now come from the commands_menu
 * registry (the Bukkit screen resolves them per viewer) instead of
 * the retired hand-kept catalogue. Group order and names are the
 * layout's; the registry has nine groups where the old catalogue
 * had eight (the conversion's Fixes & Help split).
 */
public final class CommandsPanel {

    /** One panel row: the command as displayed, and its description. */
    public record Row(String display, String description) {}

    /**
     * Resolves a /servercommands argument to a group index: a
     * 1-based group number, or a case-insensitive prefix of the
     * group name ("claims" for Claims & Land). -1 when nothing
     * matches.
     */
    public static int groupIndexFor(String arg, List<String> groupNames) {
        if (arg == null) return -1;
        String wanted = arg.trim().toLowerCase(Locale.ROOT);
        if (wanted.isEmpty()) return -1;
        try {
            int n = Integer.parseInt(wanted);
            return (n >= 1 && n <= groupNames.size()) ? n - 1 : -1;
        } catch (NumberFormatException ignored) {
            // not a number; fall through to name matching
        }
        for (int g = 0; g < groupNames.size(); g++) {
            if (groupNames.get(g).toLowerCase(Locale.ROOT).startsWith(wanted)) {
                return g;
            }
        }
        return -1;
    }

    /**
     * The panel lines. group -1 prints every group; otherwise just
     * the one group, with the pointer back to the whole list.
     */
    public static List<String> panelLines(int group, List<String> groupNames,
                                          List<List<Row>> rowsByGroup) {
        List<String> lines = new ArrayList<>();
        if (group >= 0) {
            lines.add("§6— Server Commands: " + groupNames.get(group) + " —");
            addGroup(lines, groupNames.get(group), rowsByGroup.get(group));
            lines.add("§7/servercommands shows every group.");
            return lines;
        }
        lines.add("§6— Server Commands —");
        for (int g = 0; g < groupNames.size(); g++) {
            addGroup(lines, groupNames.get(g), rowsByGroup.get(g));
        }
        lines.add("§7That is the whole list. In the menu, tap Server Commands "
                + "for tap-to-run buttons.");
        return lines;
    }

    private static void addGroup(List<String> lines, String name, List<Row> rows) {
        lines.add("§6" + name);
        for (Row r : rows) {
            lines.add("§b» §e" + r.display() + " §7- " + r.description());
        }
    }

    private CommandsPanel() {}
}
