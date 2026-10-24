package com.winkreyes.exceldeathrecord.core;

/**
 * The Burn Ledger's on-disk format: the same hand-rolled YAML
 * subset discipline as RecordFormat (one record per file,
 * double-quoted scalars, identical escaping), so both ledgers
 * read the same way in an editor and the round-trip is pinned
 * by the same style of unit test.
 *
 *   # ExcelDeathRecord burn-ledger v1
 *   owner: ".BedrockAlex"
 *   owner-uuid: "00000000-0000-0000-0009-000000000001"
 *   epoch-millis: 1792724700000
 *   time: "2026-10-22 21:25:00"
 *   cause: "LAVA"
 *   world: "minecraft:the_nether"
 *   x: 100.5
 *   y: 32.0
 *   z: -40.5
 *   age-seconds: 42
 *   item:
 *     slot: "destroyed"
 *     id: "minecraft:diamond_sword"
 *     count: 1
 *     damage: 394
 *     enchants: "minecraft:sharpness=4"
 *     bytes: "<base64>"
 *
 * owner / owner-uuid are written as "" when the owner is
 * unknown — the record never guesses. age-seconds is omitted
 * when unknown (-1).
 */
public final class BurnFormat {

    private BurnFormat() {}

    public static String write(DestructionRecord r) {
        StringBuilder sb = new StringBuilder(2048);
        sb.append("# ExcelDeathRecord burn-ledger v1\n");
        kv(sb, "owner", RecordFormat.q(r.ownerName));
        kv(sb, "owner-uuid", RecordFormat.q(r.ownerUuid));
        kv(sb, "epoch-millis", Long.toString(r.epochMillis));
        kv(sb, "time", RecordFormat.q(r.timeText));
        kv(sb, "cause", RecordFormat.q(r.cause));
        kv(sb, "world", RecordFormat.q(r.world));
        kv(sb, "x", Double.toString(r.x));
        kv(sb, "y", Double.toString(r.y));
        kv(sb, "z", Double.toString(r.z));
        if (r.ageSeconds >= 0) kv(sb, "age-seconds", Long.toString(r.ageSeconds));
        sb.append("item:\n");
        DeathRecord.ItemEntry e = r.item;
        sb.append("  slot: ").append(RecordFormat.q(e.slot)).append('\n');
        sb.append("  id: ").append(RecordFormat.q(e.id)).append('\n');
        sb.append("  count: ").append(e.count).append('\n');
        if (e.damage >= 0) sb.append("  damage: ").append(e.damage).append('\n');
        if (!e.enchants.isEmpty()) sb.append("  enchants: ").append(RecordFormat.q(RecordFormat.packEnchants(e.enchants))).append('\n');
        if (!e.customName.isEmpty()) sb.append("  name: ").append(RecordFormat.q(e.customName)).append('\n');
        sb.append("  bytes: ").append(RecordFormat.q(e.bytesB64)).append('\n');
        RecordFormat.writeRestoreMark(sb, "  ", e);
        return sb.toString();
    }

    public static DestructionRecord read(String text) {
        DestructionRecord r = new DestructionRecord();
        boolean inItem = false;
        for (String line : text.split("\n", -1)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("  ") && inItem) {
                applyItem(r.item, line.substring(2));
                continue;
            }
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim();
            String val = colon + 1 < line.length() ? line.substring(colon + 1).trim() : "";
            switch (key) {
                case "item" -> inItem = true;
                case "owner" -> r.ownerName = RecordFormat.unq(val);
                case "owner-uuid" -> r.ownerUuid = RecordFormat.unq(val);
                case "epoch-millis" -> r.epochMillis = parseLong(val);
                case "time" -> r.timeText = RecordFormat.unq(val);
                case "cause" -> r.cause = RecordFormat.unq(val);
                case "world" -> r.world = RecordFormat.unq(val);
                case "x" -> r.x = parseDouble(val);
                case "y" -> r.y = parseDouble(val);
                case "z" -> r.z = parseDouble(val);
                case "age-seconds" -> r.ageSeconds = parseLong(val);
                default -> { /* forward-compatible ignore */ }
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
            case "slot" -> item.slot = RecordFormat.unq(val);
            case "id" -> item.id = RecordFormat.unq(val);
            case "count" -> item.count = (int) parseLong(val);
            case "damage" -> item.damage = (int) parseLong(val);
            case "enchants" -> item.enchants = RecordFormat.unpackEnchants(RecordFormat.unq(val));
            case "name" -> item.customName = RecordFormat.unq(val);
            case "bytes" -> item.bytesB64 = RecordFormat.unq(val);
            default -> RecordFormat.applyRestoreKey(item, key, val);
        }
    }

    private static void kv(StringBuilder sb, String key, String rawValue) {
        sb.append(key).append(": ").append(rawValue).append('\n');
    }

    private static long parseLong(String v) {
        try { return Long.parseLong(v.trim()); } catch (RuntimeException e) { return 0; }
    }

    private static double parseDouble(String v) {
        try { return Double.parseDouble(v.trim()); } catch (RuntimeException e) { return 0; }
    }
}
