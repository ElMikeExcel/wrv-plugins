import excelemfixes.tracker.ProtectKills;
import excelemfixes.tracker.TrackerCatalog;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import java.io.OutputStreamWriter;

/**
 * ExcelProtect aggregation: only the player's own killing blows on
 * elite nameplates count; other players, mob killers, environment
 * deaths and non-death lines never do. The aggregated map must feed
 * the exact Sewers numbers the independent count produced.
 */
public final class ProtectKillsTest {

    private static final UUID JESTER =
            UUID.fromString("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e");
    private static final UUID OTHER =
            UUID.fromString("8399c623-786f-448c-8b11-f54504721819");

    private static String death(String world, String name, String kind,
                                String uuid) {
        return "{\"type\":\"entity_death\",\"world\":\"" + world
                + "\",\"name\":\"" + name + "\",\"elite\":true,"
                + "\"killer\":{\"kind\":\"" + kind + "\",\"uuid\":\""
                + uuid + "\",\"name\":\"k\"}}";
    }

    public static void run() {
        System.out.println("ProtectKillsTest");
        Check.eq("em_id_primis_sewers",
                ProtectKills.baseWorld("em_id_primis_sewers_12"),
                "instance world strips its number");
        Check.eq("em_primis", ProtectKills.baseWorld("em_primis"),
                "plain world is unchanged");

        File dir;
        try {
            dir = Files.createTempDirectory("protect-test").toFile();
            String shredder = "§4『§c11§4』§f §5The Shredder";
            StringBuilder day1 = new StringBuilder();
            // Jester kills the Shredder twice (two instances).
            day1.append(death("em_id_primis_sewers_1", shredder,
                    "player", JESTER.toString())).append('\n');
            day1.append(death("em_id_primis_sewers_2", shredder,
                    "player", JESTER.toString())).append('\n');
            // Another player's Shredder kill: not Jester's.
            day1.append(death("em_id_primis_sewers_1", shredder,
                    "player", OTHER.toString())).append('\n');
            // A mob got the killing blow: not a player kill.
            day1.append(death("em_id_primis_sewers_1", shredder,
                    "entity", "00000000-0000-0000-0000-000000000000"))
                    .append('\n');
            // A non-death line mentioning Jester: ignored.
            day1.append("{\"type\":\"combat_damage\",\"attacker_uuid\":\"")
                    .append(JESTER).append("\"}\n");
            // A malformed line: skipped, never fatal.
            day1.append("{not json\n");
            try (FileWriter w = new FileWriter(
                    new File(dir, "entities-2026-11-20.jsonl"),
                    StandardCharsets.UTF_8)) {
                w.write(day1.toString());
            }
            // A gzipped day file counts too (Noxious Ninja ×3).
            String ninja = "§2[§a10§2]§f §2Noxious Ninja";
            try (OutputStreamWriter w = new OutputStreamWriter(
                    new GZIPOutputStream(new java.io.FileOutputStream(
                            new File(dir, "entities-2026-11-19.jsonl.gz"))),
                    StandardCharsets.UTF_8)) {
                for (int i = 0; i < 3; i++) {
                    w.write(death("em_id_primis_sewers_4", ninja,
                            "player", JESTER.toString()));
                    w.write('\n');
                }
            }
        } catch (Exception e) {
            Check.ok(false, "fixture written (" + e + ")");
            return;
        }

        Map<String, Integer> kills = ProtectKills.aggregate(dir, JESTER);
        Check.eq(2, kills.getOrDefault(
                        ProtectKills.key("The Shredder", "em_id_primis_sewers"),
                        0),
                "only Jester's two Shredder killing blows count");
        Check.eq(3, kills.getOrDefault(
                        ProtectKills.key("Noxious Ninja", "em_id_primis_sewers"),
                        0),
                "gzipped day files aggregate too");

        Map<String, Integer> otherKills = ProtectKills.aggregate(dir, OTHER);
        Check.eq(1, otherKills.getOrDefault(
                        ProtectKills.key("The Shredder", "em_id_primis_sewers"),
                        0),
                "the other player sees only his own kill");

        // The aggregated map feeds the catalog math exactly.
        TrackerCatalog catalog;
        try (FileReader r = new FileReader("deploy/tracker-catalog.yml",
                StandardCharsets.UTF_8)) {
            catalog = TrackerCatalog.load(r);
        } catch (Exception e) {
            Check.ok(false, "catalog loads (" + e + ")");
            return;
        }
        TrackerCatalog.DungeonInfo sewers = catalog.dungeons().stream()
                .filter(d -> d.id().equals("primis_sewers"))
                .findFirst().orElseThrow();
        Check.eq(2, ProtectKills.dungeonClears(kills, sewers),
                "Sewers clears = Shredder killing blows (2)");
        TrackerCatalog.PlaceInfo sewersPlace =
                catalog.placeById("primis_sewers");
        TrackerCatalog.BossInfo shredderBoss = sewersPlace.bosses().stream()
                .filter(b -> b.cleanedName().equals("The Shredder"))
                .findFirst().orElseThrow();
        Check.eq(2, ProtectKills.countFor(kills, catalog, sewersPlace,
                        shredderBoss),
                "countFor resolves the Shredder by world + nameplate");

        // A missing ExcelProtect folder degrades to an empty map.
        Check.ok(ProtectKills.aggregate(new File(dir, "nope"), JESTER)
                        .isEmpty(),
                "missing records folder yields no counts, not an error");
    }
}
