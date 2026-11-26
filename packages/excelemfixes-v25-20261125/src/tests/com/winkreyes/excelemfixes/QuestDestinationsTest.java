import excelemfixes.questdest.QuestDestinations;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * The bundled quest-destinations.yml: shape, the three verified
 * rows (coordinates read from the live EliteMobs wormhole files on
 * Nov 22, 2026), and the matcher that decides which of a player's
 * active quests get a destination from us.
 */
public class QuestDestinationsTest {

    static QuestDestinations load() throws Exception {
        // WRV's deployed table (the jar ships a blank template).
        try (FileReader r = new FileReader("deploy/quest-destinations.yml",
                StandardCharsets.UTF_8)) {
            return QuestDestinations.load(r);
        }
    }

    static QuestDestinations loadBundled() throws Exception {
        try (FileReader r = new FileReader("src/quest-destinations.yml",
                StandardCharsets.UTF_8)) {
            return QuestDestinations.load(r);
        }
    }

    public static void run() {
        System.out.println("== QuestDestinationsTest");
        QuestDestinations table;
        try {
            table = load();
        } catch (Exception e) {
            Check.ok(false, "bundled table loads: " + e);
            return;
        }
        Check.eq(11, table.all().size(), "eleven destinations in WRV's table");
        try {
            QuestDestinations bundled = loadBundled();
            Check.eq(0, bundled.all().size(),
                    "the shipped template is blank (no server's content)");
        } catch (Exception e) {
            Check.ok(false, "bundled template loads: " + e);
        }
        Check.ok(table.ignoredFiles().contains("test_quest.yml"),
                "EM's test quest is on the ignore list");
        // The coverage-scan additions (Nov 22): every tracker-blank
        // quest in the live tree, entries verified from the live
        // wormhole files / teleporter NPC spawns.
        QuestDestinations.Destination q9 =
                table.byFile("primis_main_quest_9.yml");
        Check.ok(q9 != null && q9.coords().equals("942.5, 45, 619.5"),
                "q9 Gladius Statue wormhole coords");
        QuestDestinations.Destination q28 =
                table.byFile("primis_main_quest_28.yml");
        Check.ok(q28 != null && q28.coords().equals("1193.5, 151, 1001.5"),
                "q28 teleporter NPC coords");
        QuestDestinations.Destination sewage = table.byFile(
                "primis_pontis_mille_serious_sewage.yml");
        Check.ok(sewage != null
                        && sewage.coords().equals("583.4, 39, 763.5"),
                "sewers wormhole coords");

        QuestDestinations.Destination q15 =
                table.byFile("primis_main_quest_15.yml");
        Check.ok(q15 != null, "quest 15 covered");
        Check.eq("Severance payback!", q15.questName(), "q15 name");
        Check.eq("Counselor Machiavelli", q15.target(), "q15 target");
        Check.eq("Pontis Mille", q15.place(), "q15 place");
        Check.eq("em_primis", q15.world(), "q15 world");
        Check.eq("512.5, 42, 724.5", q15.coords(),
                "q15 coords = Advisor wormhole location1 (live-verified)");
        Check.ok(q15.entry().contains("Advisor wormhole"), "q15 entry named");
        Check.ok(q15.directions().size() >= 3, "q15 has directions");

        QuestDestinations.Destination q18 =
                table.byFile("primis_main_quest_18.yml");
        Check.ok(q18 != null, "quest 18 covered");
        Check.eq("The trial of ice", q18.questName(), "q18 name");
        Check.eq("1209.5, 212, 1399.5", q18.coords(),
                "q18 coords = Trial of Ice wormhole location1 (live-verified)");
        Check.ok(q18.directions().stream()
                        .anyMatch(s -> s.contains("Amethyst")),
                "q18 directions carry the amethyst-block path");

        QuestDestinations.Destination q19 =
                table.byFile("primis_main_quest_19.yml");
        Check.ok(q19 != null, "quest 19 covered");
        Check.eq("Chill out", q19.questName(), "q19 name");
        Check.eq("975.4, 42, 1655.5", q19.coords(),
                "q19 coords = arena wormhole location1 (live-verified)");

        for (QuestDestinations.Destination d : table.all()) {
            Check.ok(!d.target().isBlank() && !d.place().isBlank()
                            && !d.entry().isBlank()
                            && !d.directions().isEmpty(),
                    d.questFile() + " fully populated");
            Check.ok(d.questFile().endsWith(".yml"),
                    d.questFile() + " keyed by quest filename");
        }

        // Matcher: only covered quests, in table order.
        List<QuestDestinations.Destination> mixed = table.forActive(List.of(
                "primis_main_quest_19.yml", "primis_main_quest_3.yml",
                "primis_main_quest_15.yml"));
        Check.eq(2, mixed.size(), "matcher keeps only covered quests");
        Check.eq("primis_main_quest_15.yml", mixed.get(0).questFile(),
                "matcher returns table order, not player order");
        Check.ok(table.forActive(List.of("primis_main_quest_3.yml")).isEmpty(),
                "uncovered quest yields nothing");
        Check.ok(table.forActive(List.of()).isEmpty(),
                "no active quests yields nothing");
        Check.ok(!table.covers("primis_main_quest_14.yml"),
                "quest 14 (regional temple) not covered");

        // Glow modes: wormhole entries are places (marker); the
        // two teleporter-NPC entries are entities (the NPC glows).
        for (QuestDestinations.Destination d : table.all()) {
            boolean npcEntry = d.questFile().equals("primis_main_quest_28.yml")
                    || d.questFile().equals(
                            "primis_pontis_mille_the_battle_for_gladius.yml");
            Check.eq(npcEntry, d.glowIsEntity(),
                    d.questFile() + " glow mode matches its entry kind");
            if (npcEntry) {
                Check.ok(d.glowEntityName() != null
                                && !d.glowEntityName().isBlank(),
                        d.questFile() + " names its teleporter NPC");
            }
        }

        // Coordinate formatting.
        Check.eq("512", QuestDestinations.formatCoord(512.0), "whole coord");
        Check.eq("512.5", QuestDestinations.formatCoord(512.5), "half coord");
        Check.eq("975.4", QuestDestinations.formatCoord(975.41),
                "hundredths round to tenths");
        Check.eq("-113.5", QuestDestinations.formatCoord(-113.5),
                "negative coord");
    }
}
