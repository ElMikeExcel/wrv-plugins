package com.winkreyes.excelemcompanion.tracker;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.winkreyes.excelemcompanion.shared.TextUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPInputStream;

/**
 * Per-boss kill counts from ExcelProtect's entity records — the
 * server's per-entity source of truth (Mike's data-first ruling).
 * EliteMobs' own counters are global totals only, so lifetime
 * per-boss and per-dungeon numbers come from here:
 *
 *   an entity_death record in entities-*.jsonl[.gz] whose killer
 *   is the player (killer.kind = "player", killer.uuid = player)
 *   is one killing blow by that player on the recorded nameplate.
 *
 * Records are matched to catalog bosses by world + cleaned
 * nameplate (see countFor); where a boss cannot be matched
 * reliably the tracker shows downed/not-downed from EliteMobs
 * lockouts instead of a number — counts are never fabricated.
 *
 * ExcelProtect records begin 2026-11-02 on this server, before
 * EliteMobs content opened (guild, Nov 14), so coverage of elite
 * bosses is effectively complete; the menus label counts as
 * "server records" to stay honest about the source.
 */
public final class ProtectKills {

    private ProtectKills() {}

    public static String key(String cleanedName, String baseWorld) {
        return cleanedName + "@" + baseWorld;
    }

    /** Instance worlds are "<base>_<n>": strip the trailing number. */
    public static String baseWorld(String world) {
        if (world == null) return "";
        return world.replaceAll("_\\d+$", "");
    }

    /**
     * The verified count for one catalog boss, or 0 when no reliable
     * count exists (the caller then falls back to downed display).
     */
    public static int countFor(Map<String, Integer> kills,
                               TrackerCatalog catalog,
                               TrackerCatalog.PlaceInfo place,
                               TrackerCatalog.BossInfo boss) {
        if (kills == null) return 0;
        String cleaned = boss.cleanedName();
        if (!place.world().isBlank()) {
            return kills.getOrDefault(
                    key(cleaned, baseWorld(place.world())), 0);
        }
        if (!catalog.bossNameUnique(cleaned)) return 0;
        int total = 0;
        for (Map.Entry<String, Integer> e : kills.entrySet()) {
            String k = e.getKey();
            int cut = k.lastIndexOf('@');
            if (cut > 0 && k.substring(0, cut).equals(cleaned)) {
                total += e.getValue();
            }
        }
        return total;
    }

    /** Clears of one dungeon = the player's killing blows on its
     *  objective (final) bosses in the dungeon's world. */
    public static int dungeonClears(Map<String, Integer> kills,
                                    TrackerCatalog.DungeonInfo dungeon) {
        if (kills == null) return 0;
        int total = 0;
        String world = baseWorld(dungeon.world());
        for (String objective : dungeon.objectives()) {
            total += kills.getOrDefault(
                    key(TextUtil.cleanNameplate(objective), world), 0);
        }
        return total;
    }

    /**
     * Scans every entities stream in the ExcelProtect folder and
     * counts the player's killing blows per (boss name, world).
     * Blocking — call off the main thread.
     */
    public static Map<String, Integer> aggregate(File protectDir, UUID uuid) {
        Map<String, Integer> counts = new HashMap<>();
        if (protectDir == null || !protectDir.isDirectory()) return counts;
        File[] files = protectDir.listFiles((dir, name) ->
                name.startsWith("entities-")
                        && (name.endsWith(".jsonl")
                        || name.endsWith(".jsonl.gz")));
        if (files == null) return counts;
        String uuidText = uuid.toString();
        for (File file : files) {
            try (BufferedReader reader = open(file)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.contains("\"entity_death\"")
                            || !line.contains(uuidText)) {
                        continue;
                    }
                    countLine(line, uuidText, counts);
                }
            } catch (Exception e) {
                // A torn or unreadable day file must not sink the
                // whole aggregation; its counts are simply absent.
            }
        }
        return counts;
    }

    private static void countLine(String line, String uuidText,
                                  Map<String, Integer> counts) {
        try {
            JsonObject e = JsonParser.parseString(line).getAsJsonObject();
            if (!"entity_death".equals(str(e, "type"))) return;
            if (!e.has("killer") || !e.get("killer").isJsonObject()) return;
            JsonObject killer = e.getAsJsonObject("killer");
            if (!"player".equals(str(killer, "kind"))) return;
            if (!uuidText.equals(str(killer, "uuid"))) return;
            String name = TextUtil.cleanNameplate(str(e, "name"));
            if (name.isBlank()) return;
            counts.merge(key(name, baseWorld(str(e, "world"))), 1,
                    Integer::sum);
        } catch (Exception ignored) {
            // Malformed line: skip, never guess.
        }
    }

    private static BufferedReader open(File file) throws Exception {
        if (file.getName().endsWith(".gz")) {
            return new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(new FileInputStream(file)),
                    StandardCharsets.UTF_8));
        }
        return new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8));
    }

    private static String str(JsonObject obj, String field) {
        return obj.has(field) && obj.get(field).isJsonPrimitive()
                ? obj.get(field).getAsString() : "";
    }
}
