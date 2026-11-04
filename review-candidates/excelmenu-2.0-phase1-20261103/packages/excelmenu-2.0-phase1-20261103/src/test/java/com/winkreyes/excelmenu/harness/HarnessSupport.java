package com.winkreyes.excelmenu.harness;

import java.util.List;
import java.util.Map;

/**
 * Shared harness vocabulary: one visible row (id, label, dispatch key)
 * and the normalisations that let three different action dialects be
 * compared on INTENT:
 *
 *   Java definition lines  "[openguimenu] teleport_confirm_spawn" and
 *   Bedrock's modal confirm + "spawn" are the same door: teleport:spawn.
 *   "[player] excelsilkwatch:silkwarnings off" (Java trio) and
 *   "silkwarnings toggle" (Bedrock branch) both flip the CURRENT
 *   state: toggle:silkwarnings. (The Java form writes the absolute
 *   opposite of the rendered state — a pre-existing asymmetry the
 *   harness notes but does not hide; see PHASE-0-REPORT.)
 */
final class HarnessSupport {

    record Row(String id, String label, String key) {}

    /** One viewer state in the golden matrix. */
    record ViewerState(boolean guildLocked, String silk, String aim,
                       String autojoin, boolean inWindow) {
        String describe() {
            return "guild=" + (guildLocked ? "locked" : "open")
                    + " silk=" + silk + " aim=" + aim + " autojoin=" + autojoin
                    + " window=" + (inWindow ? "in" : "out");
        }
    }

    /** Bedrock command string -> registry entry id (the unification map). */
    static final Map<String, String> COMMAND_TO_ID = Map.ofEntries(
            Map.entry("spawn", "spawn"),
            Map.entry("tpmenu", "tpmenu"),
            Map.entry("excelparty", "party"),
            Map.entry("pouch", "utility_pouch"),
            Map.entry("spellinfo", "spellbook"),
            Map.entry("shop", "shop"),
            Map.entry("cosmetics", "cosmetics"),
            Map.entry("ag", "guild"),
            Map.entry("jobs browse", "jobs"),
            Map.entry("skillsperks", "skills"),
            Map.entry("leaderboard", "leaderboard"),
            Map.entry("lostfound", "lostfound"),
            Map.entry("trash", "trash"),
            Map.entry("bossrewards", "boss_rewards"),
            Map.entry("motd", "info"),
            Map.entry("servercommands", "commands"),
            Map.entry("serverwebsite", "website"),
            Map.entry("whatsnew", "whatsnew"),
            Map.entry("ask", "ask"),
            Map.entry("wandstyle", "wandstyle"),
            Map.entry("autosort", "autosort"),
            Map.entry("mark", "makersmark_settings"),
            Map.entry("menujoin", "autojoin"),
            Map.entry("silkwarnings", "silkwarnings"),
            Map.entry("aimassist", "aimassist"));

    /** Java item id in server_menu.yml -> registry entry id. */
    static String javaItemToEntry(String itemId) {
        return switch (itemId) {
            case "guild_locked", "guild_open" -> "guild";
            case "autojoin_on", "autojoin_off" -> "autojoin";
            case "silkwarnings_on", "silkwarnings_off", "silkwarnings_unavailable" ->
                    "silkwarnings";
            case "aimassist_on", "aimassist_off", "aimassist_unavailable" -> "aimassist";
            default -> itemId;
        };
    }

    /** Normalise a Java/new action line list to one dispatch key. */
    static String dispatchKey(List<String> lines) {
        if (lines == null || lines.isEmpty()) return "none";
        for (String l : lines) if (l.startsWith("[json]")) return "website";
        for (String l : lines) {
            if (l.startsWith("[openguimenu] teleport_confirm_spawn")) return "teleport:spawn";
            if (l.startsWith("[openguimenu] teleport_confirm_guild")) return "teleport:guild";
        }
        for (String l : lines) {
            if (l.startsWith("[openguimenu] ")) {
                return "menu:" + l.substring("[openguimenu] ".length()).trim();
            }
        }
        for (String l : lines) {
            if (l.startsWith("[player] ")) {
                String cmd = l.substring("[player] ".length()).trim();
                if (cmd.equals("tpmenu")) return "screen:tpmenu";
                if (cmd.startsWith("excelsilkwatch:silkwarnings")) return "toggle:silkwarnings";
                if (cmd.startsWith("excelaimassist:aimassist")) return "toggle:aimassist";
                return "cmd:" + cmd;
            }
        }
        for (String l : lines) if (l.startsWith("[message]")) return "message";
        return "none";
    }

    /** Bedrock group colour table, transcribed from 1.5.27 groupColor(). */
    static String bedrockGroupColor(String command) {
        return switch (command) {
            case "spawn", "tpmenu", "excelparty", "ag" -> "§1";
            case "pouch", "spellinfo", "jobs browse", "skillsperks", "leaderboard",
                    "lostfound", "trash", "bossrewards" -> "§5";
            case "shop", "cosmetics" -> "§2";
            case "motd", "servercommands", "serverwebsite", "whatsnew", "ask" -> "§n";
            case "wandstyle", "autosort", "mark", "menujoin", "silkwarnings",
                    "aimassist" -> "§8";
            default -> "";
        };
    }

    /** Bedrock dispatch key for one COMMANDS entry in one state. */
    static String bedrockKey(String command, ViewerState s) {
        return switch (command) {
            case "spawn" -> "teleport:spawn";
            case "ag" -> s.guildLocked() ? "cmd:seals" : "teleport:guild";
            case "skillsperks" -> "screen:skillsperks";
            case "tpmenu" -> "screen:tpmenu";
            case "ask" -> "screen:ask";
            case "serverwebsite" -> "website";
            case "silkwarnings" -> isOnOff(s.silk()) ? "toggle:silkwarnings" : "message";
            case "aimassist" -> isOnOff(s.aim()) ? "toggle:aimassist" : "message";
            default -> "cmd:" + command;
        };
    }

    static boolean isOnOff(String v) {
        return "ON".equals(v) || "OFF".equals(v);
    }

    /** The label 1.5.27 computes for its state-bearing buttons. */
    static String bedrockStateLabel(String base, String command, ViewerState s) {
        if (command.equals("silkwarnings")) {
            return "Silk Touch Warnings: " + (isOnOff(s.silk()) ? s.silk() : "unavailable");
        }
        if (command.equals("aimassist")) {
            return "Aim Assist: " + (isOnOff(s.aim()) ? s.aim() : "unavailable");
        }
        return base; // guild lockedLabel() returns the base label unchanged
    }

    private HarnessSupport() {}
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
