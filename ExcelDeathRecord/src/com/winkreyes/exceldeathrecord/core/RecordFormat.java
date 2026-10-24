package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The on-disk record format: a small, documented YAML subset,
 * written and read by hand so the format is pinned by unit tests
 * and a record file stays readable in any text editor.
 *
 * Layout (all string scalars double-quoted, escapes \\ \" \n \r \t):
 *
 *   # ExcelDeathRecord snapshot v1
 *   player: ".BedrockAlex"
 *   uuid: "00000000-0000-0000-0009-000000000001"
 *   epoch-millis: 1792724644000
 *   time: "2026-10-22 21:24:04"
 *   world: "minecraft:overworld"
 *   x: -1280.5
 *   y: 71.0
 *   z: -300.2
 *   gamemode: "SURVIVAL"
 *   death-message: "..."
 *   damage-cause: "ENTITY_ATTACK"
 *   killer: "Zombie (minecraft:zombie)"
 *   xp-level: 20
 *   total-experience: 1395
 *   dropped-exp: 87
 *   keep-inventory: false
 *   drops:
 *     - slot: "drop-0"
 *       id: "minecraft:diamond_sword"
 *       count: 1
 *       damage: 394
 *       enchants: "minecraft:sharpness=4, minecraft:fire_aspect=2"
 *       name: "..."
 *       bytes: "<base64>"
 *   inventory:
 *     - slot: "storage-0"
 *       ...
 *
 * Optional item lines (damage / enchants / name) are written only
 * when they carry a value; an empty section is written as
 * "drops: []". Enchants pack as "id=level" pairs joined by ", "
 * (an enchantment id never contains '=' or ',', so the packing
 * is unambiguous). read() is the exact inverse of write() —
 * round-trip equality is unit-tested, including hostile strings
 * (quotes, backslashes, colons, unicode, a leading-dot Bedrock
 * name with spaces).
 */
public final class RecordFormat {

    private RecordFormat() {}

    public static String write(DeathRecord r) {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("# ExcelDeathRecord snapshot v1\n");
        kv(sb, "player", q(r.playerName));
        kv(sb, "uuid", q(r.playerUuid));
        kv(sb, "epoch-millis", Long.toString(r.epochMillis));
        kv(sb, "time", q(r.timeText));
        kv(sb, "world", q(r.world));
        kv(sb, "x", Double.toString(r.x));
        kv(sb, "y", Double.toString(r.y));
        kv(sb, "z", Double.toString(r.z));
        kv(sb, "gamemode", q(r.gamemode));
        kv(sb, "death-message", q(r.deathMessage));
        kv(sb, "damage-cause", q(r.damageCause));
        kv(sb, "killer", q(r.killer));
        kv(sb, "xp-level", Integer.toString(r.xpLevel));
        kv(sb, "total-experience", Integer.toString(r.totalExperience));
        kv(sb, "dropped-exp", Integer.toString(r.droppedExp));
        kv(sb, "keep-inventory", Boolean.toString(r.keepInventory));
        if (r.xpRestoredTimes > 0) {
            kv(sb, "xp-restored-times", Integer.toString(r.xpRestoredTimes));
            kv(sb, "xp-restored-to", q(r.xpRestoredTo));
            kv(sb, "xp-restored-by", q(r.xpRestoredBy));
            kv(sb, "xp-restored-at", q(r.xpRestoredAt));
        }
        section(sb, "drops", r.drops);
        section(sb, "inventory", r.inventory);
        return sb.toString();
    }

    private static void kv(StringBuilder sb, String key, String rawValue) {
        sb.append(key).append(": ").append(rawValue).append('\n');
    }

    private static void section(StringBuilder sb, String key, List<DeathRecord.ItemEntry> items) {
        if (items.isEmpty()) {
            sb.append(key).append(": []\n");
            return;
        }
        sb.append(key).append(":\n");
        for (DeathRecord.ItemEntry e : items) {
            sb.append("  - slot: ").append(q(e.slot)).append('\n');
            sb.append("    id: ").append(q(e.id)).append('\n');
            sb.append("    count: ").append(e.count).append('\n');
            if (e.damage >= 0) sb.append("    damage: ").append(e.damage).append('\n');
            if (!e.enchants.isEmpty()) sb.append("    enchants: ").append(q(packEnchants(e.enchants))).append('\n');
            if (!e.customName.isEmpty()) sb.append("    name: ").append(q(e.customName)).append('\n');
            sb.append("    bytes: ").append(q(e.bytesB64)).append('\n');
            writeRestoreMark(sb, "    ", e);
        }
    }

    /** The 0.2.0 restore mark, written only once an item has been
     *  restored. Item-level keys: a 0.1.0 reader (and ExcelAuditLog
     *  0.2.0) ignores them, so a rollback never misreads a file.
     *  No key starts with "id" or "count" (AuditLog scans those). */
    static void writeRestoreMark(StringBuilder sb, String indent, DeathRecord.ItemEntry e) {
        if (e.restoredTimes <= 0) return;
        sb.append(indent).append("restored-times: ").append(e.restoredTimes).append('\n');
        sb.append(indent).append("restored-to: ").append(q(e.restoredTo)).append('\n');
        sb.append(indent).append("restored-to-uuid: ").append(q(e.restoredToUuid)).append('\n');
        sb.append(indent).append("restored-by: ").append(q(e.restoredBy)).append('\n');
        sb.append(indent).append("restored-epoch: ").append(e.restoredEpoch).append('\n');
        sb.append(indent).append("restored-at: ").append(q(e.restoredAt)).append('\n');
    }

    /** Shared item-key reader for both ledgers; false = not a restore key. */
    static boolean applyRestoreKey(DeathRecord.ItemEntry item, String key, String val) {
        switch (key) {
            case "restored-times" -> item.restoredTimes = (int) parseLong(val);
            case "restored-to" -> item.restoredTo = unq(val);
            case "restored-to-uuid" -> item.restoredToUuid = unq(val);
            case "restored-by" -> item.restoredBy = unq(val);
            case "restored-epoch" -> item.restoredEpoch = parseLong(val);
            case "restored-at" -> item.restoredAt = unq(val);
            default -> { return false; }
        }
        return true;
    }

    public static String packEnchantsPublic(Map<String, Integer> enchants) { return packEnchants(enchants); }

    static String packEnchants(Map<String, Integer> enchants) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> en : enchants.entrySet()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(en.getKey()).append('=').append(en.getValue());
        }
        return sb.toString();
    }

    static Map<String, Integer> unpackEnchants(String packed) {
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        if (packed == null || packed.isEmpty()) return out;
        for (String pair : packed.split(", ")) {
            int eq = pair.indexOf('=');
            if (eq <= 0) continue;
            try {
                out.put(pair.substring(0, eq), Integer.parseInt(pair.substring(eq + 1)));
            } catch (NumberFormatException ignored) {
                // A corrupt pair never sinks the record.
            }
        }
        return out;
    }

    /** Quote a string scalar: wraps in double quotes, escaping
     *  backslash, quote, and control characters. */
    static String q(String s) {
        if (s == null) s = "";
        StringBuilder sb = new StringBuilder(s.length() + 2);
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\' -> sb.append("\\\\");
                case '"' -> sb.append("\\\"");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /** Inverse of q(): expects the leading quote; tolerant of an
     *  unquoted scalar (returns it trimmed) so a hand-edited file
     *  still parses. */
    static String unq(String raw) {
        if (raw == null) return "";
        String s = raw.trim();
        if (s.length() < 2 || s.charAt(0) != '"') return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"') break;
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 'r' -> sb.append('\r');
                    case 't' -> sb.append('\t');
                    case '\\' -> sb.append('\\');
                    case '"' -> sb.append('"');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static DeathRecord read(String text) {
        DeathRecord r = new DeathRecord();
        List<DeathRecord.ItemEntry> current = null;
        DeathRecord.ItemEntry item = null;
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("  - ")) {
                if (current != null) {
                    item = new DeathRecord.ItemEntry();
                    current.add(item);
                    applyItem(item, line.substring(4));
                }
                continue;
            }
            if (line.startsWith("    ") && item != null) {
                applyItem(item, line.substring(4));
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim();
            String val = colon + 1 < line.length() ? line.substring(colon + 1).trim() : "";
            switch (key) {
                case "drops" -> { current = r.drops; item = null; }
                case "inventory" -> { current = r.inventory; item = null; }
                case "player" -> r.playerName = unq(val);
                case "uuid" -> r.playerUuid = unq(val);
                case "epoch-millis" -> r.epochMillis = parseLong(val);
                case "time" -> r.timeText = unq(val);
                case "world" -> r.world = unq(val);
                case "x" -> r.x = parseDouble(val);
                case "y" -> r.y = parseDouble(val);
                case "z" -> r.z = parseDouble(val);
                case "gamemode" -> r.gamemode = unq(val);
                case "death-message" -> r.deathMessage = unq(val);
                case "damage-cause" -> r.damageCause = unq(val);
                case "killer" -> r.killer = unq(val);
                case "xp-level" -> r.xpLevel = (int) parseLong(val);
                case "total-experience" -> r.totalExperience = (int) parseLong(val);
                case "dropped-exp" -> r.droppedExp = (int) parseLong(val);
                case "keep-inventory" -> r.keepInventory = Boolean.parseBoolean(val);
                case "xp-restored-times" -> r.xpRestoredTimes = (int) parseLong(val);
                case "xp-restored-to" -> r.xpRestoredTo = unq(val);
                case "xp-restored-by" -> r.xpRestoredBy = unq(val);
                case "xp-restored-at" -> r.xpRestoredAt = unq(val);
                default -> { /* unknown key: forward-compatible ignore */ }
            }
        }
        return r;
    }

    private static void applyItem(DeathRecord.ItemEntry item, String kvLine) {
        int colon = kvLine.indexOf(':');
        if (colon < 0) return;
        String key = kvLine.substring(0, colon).trim();
        String val = colon + 1 < kvLine.length() ? kvLine.substring(colon + 1).trim() : "";
        switch (key) {
            case "slot" -> item.slot = unq(val);
            case "id" -> item.id = unq(val);
            case "count" -> item.count = (int) parseLong(val);
            case "damage" -> item.damage = (int) parseLong(val);
            case "enchants" -> item.enchants = unpackEnchants(unq(val));
            case "name" -> item.customName = unq(val);
            case "bytes" -> item.bytesB64 = unq(val);
            default -> applyRestoreKey(item, key, val);
        }
    }

    static long parseLong(String v) {
        try { return Long.parseLong(v.trim()); } catch (RuntimeException e) { return 0; }
    }

    private static double parseDouble(String v) {
        try { return Double.parseDouble(v.trim()); } catch (RuntimeException e) { return 0; }
    }

    /** ItemEntry list helper for tests and callers that build
     *  records programmatically. */
    public static List<DeathRecord.ItemEntry> items(DeathRecord.ItemEntry... entries) {
        return new ArrayList<>(List.of(entries));
    }
}
