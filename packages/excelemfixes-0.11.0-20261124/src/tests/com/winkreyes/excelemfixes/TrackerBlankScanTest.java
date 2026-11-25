import excelemfixes.questdest.TrackerBlankScan;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The expansion-generic defect-class rule: a quest is
 * tracker-blank when it has KILL_CUSTOM objectives and every
 * target boss spawns only in instanced (em_id_*) worlds or has
 * no natural spawns. Fixtures use invented packs — the rule must
 * not know Primis from anything else.
 */
public class TrackerBlankScanTest {

    public static void run() {
        System.out.println("== TrackerBlankScanTest");
        Map<String, Set<String>> quests = Map.of(
                "packa_quest_1.yml", Set.of("packa_field_boss.yml"),
                "packa_quest_2.yml", Set.of("packa_cave_boss.yml"),
                "packa_quest_3.yml", Set.of("packa_cave_boss.yml",
                        "packa_summoned_add.yml"),
                "packa_quest_4.yml", Set.of("packa_field_boss.yml",
                        "packa_cave_boss.yml"),
                "packa_quest_5.yml", Set.of(),
                "packb_quest_1.yml", Set.of("packb_deep_boss.yml"));
        Map<String, List<String>> bosses = Map.of(
                "packa_field_boss.yml", List.of("world", "em_packa_town"),
                "packa_cave_boss.yml", List.of("em_id_packa_cave"),
                "packa_summoned_add.yml", List.of(),
                "packb_deep_boss.yml", List.of("em_id_packb_deep",
                        "em_id_packb_deep2"));

        Set<String> blank = TrackerBlankScan.trackerBlank(quests, bosses);
        Check.ok(!blank.contains("packa_quest_1.yml"),
                "overworld-spawn boss quest is not blank");
        Check.ok(blank.contains("packa_quest_2.yml"),
                "instanced-only boss quest is blank");
        Check.ok(blank.contains("packa_quest_3.yml"),
                "instanced boss + spawn-less summon is blank");
        Check.ok(!blank.contains("packa_quest_4.yml"),
                "one overworld boss is enough to not be blank");
        Check.ok(!blank.contains("packa_quest_5.yml"),
                "no kill objectives is not this defect class");
        Check.ok(blank.contains("packb_quest_1.yml"),
                "a future pack's instanced quest is blank too");

        Check.eq("em_id_primis_counselor",
                TrackerBlankScan.spawnWorld(
                        "em_id_primis_counselor,149.5,95.5,58.5,0.0,0.0:1676132680896"),
                "spawn world parses from EM's location string");
        Check.ok(TrackerBlankScan.isInstancedWorld("em_id_anything"),
                "em_id_ prefix marks an instance world");
        Check.ok(!TrackerBlankScan.isInstancedWorld("em_primis"),
                "the pack's main world is not an instance");
        System.out.println("TrackerBlankScanTest done");
    }
}
