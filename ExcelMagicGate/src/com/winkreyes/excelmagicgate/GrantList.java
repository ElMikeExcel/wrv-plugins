package com.winkreyes.excelmagicgate;

import java.util.ArrayList;
import java.util.List;

/**
 * Parser for a stage's LuckPerms grant list (pure, no Bukkit deps).
 *
 * The stage directories ship the packets' own
 * luckperms-default-group-grants.txt files verbatim as grants.txt: the
 * exact, human-audited command lists from the WRV-070 packets, comments
 * and all. Applying a stage runs those commands verbatim as console
 * commands - the bridge never invents a grant of its own.
 *
 * Parsing rules:
 * - Blank lines and lines starting with '#' are skipped.
 * - Every other line is one console command, kept in file order.
 * - If any command targets the "builder" LuckPerms group (the Seal 4
 *   Masons grants), a single "lp creategroup builder" pre-command is
 *   added ahead of the list: the packet file only mentions group
 *   creation in a comment, and the builder grants fail without the group.
 */
public final class GrantList {

    public record Parsed(List<String> preCommands, List<String> commands) {
        public int size() {
            return commands.size();
        }
    }

    private GrantList() {}

    public static Parsed parse(List<String> lines) {
        List<String> commands = new ArrayList<>();
        boolean needsBuilderGroup = false;
        if (lines != null) {
            for (String raw : lines) {
                if (raw == null) {
                    continue;
                }
                String line = raw.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                commands.add(line);
                if (line.startsWith("lp group builder ")) {
                    needsBuilderGroup = true;
                }
            }
        }
        List<String> pre = needsBuilderGroup
                ? List.of("lp creategroup builder")
                : List.of();
        return new Parsed(pre, List.copyOf(commands));
    }
}
