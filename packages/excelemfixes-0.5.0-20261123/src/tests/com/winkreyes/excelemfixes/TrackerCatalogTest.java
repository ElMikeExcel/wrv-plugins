import excelemfixes.shared.TextUtil;
import excelemfixes.tracker.TrackerCatalog;

import java.io.FileReader;
import java.nio.charset.StandardCharsets;

/**
 * The generated tracker catalog (from the live EliteMobs configs)
 * must reproduce the verified denominators — 111 player quests,
 * a 31-file main chain, 14 enabled instanced dungeons — from data,
 * never from hardcoded numbers in the plugin.
 */
public final class TrackerCatalogTest {

    public static void run() {
        System.out.println("TrackerCatalogTest");
        TrackerCatalog catalog;
        try (FileReader r = new FileReader("deploy/tracker-catalog.yml",
                StandardCharsets.UTF_8)) {
            catalog = TrackerCatalog.load(r);
        } catch (Exception e) {
            Check.ok(false, "catalog loads (" + e + ")");
            return;
        }
        Check.eq(111, catalog.questTotal(),
                "111 player quests derived from configs");
        Check.eq(31, catalog.mainChain().size(),
                "main chain is 31 files (prologue + 30)");
        Check.eq("primis_main_quest_.yml", catalog.mainChain().get(0),
                "main chain starts with the prologue");
        Check.eq("primis_main_quest_27.yml", catalog.mainChain().get(27),
                "chain position 27 is main quest 27");
        Check.eq(14, catalog.dungeons().size(),
                "14 enabled instanced dungeons derived from configs");
        Check.eq(38, catalog.places().size(), "38 places");

        TrackerCatalog.DungeonInfo sewers = catalog.dungeons().stream()
                .filter(d -> d.id().equals("primis_sewers"))
                .findFirst().orElse(null);
        Check.ok(sewers != null, "Sewers dungeon exists");
        if (sewers != null) {
            Check.eq("em_id_primis_sewers", sewers.world(),
                    "Sewers world is the instanced dungeon world");
            Check.ok(sewers.objectives().stream().anyMatch(o ->
                            TextUtil.cleanNameplate(o).equals("The Shredder")),
                    "Sewers objective (final) boss is The Shredder");
            Check.ok(sewers.bossFiles()
                            .contains("primis_sewer_the_shredder_p1.yml"),
                    "Sewers boss files include the Shredder (lockout map)");
        }
        TrackerCatalog.QuestInfo q27 = catalog.quest("primis_main_quest_27.yml");
        Check.ok(q27 != null
                        && TextUtil.plain(q27.name()).equals("The Four Guardians"),
                "main quest 27 is The Four Guardians");
        Check.ok(catalog.quest("test_quest.yml") == null,
                "test quest is excluded from the catalog");
    }
}
