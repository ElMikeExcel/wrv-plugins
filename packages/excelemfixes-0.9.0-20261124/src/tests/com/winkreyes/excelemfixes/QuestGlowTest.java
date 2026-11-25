import excelemfixes.questglow.GlowState;
import excelemfixes.questglow.QuestGlowCore;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quest-giver-glow core: the availability definition (who
 * glows, for whom, and — the expensive direction to get wrong
 * — who must NOT), anchor parsing, and the radius check. The
 * fixtures mirror the real welcome chain's shape: guide_0
 * hands out ag_welcome_quest_0, whose turn-in NPC is guide_1.
 */
public final class QuestGlowTest {

    private static QuestGlowCore.NpcInfo npc(String file,
                                             List<String> quests) {
        return new QuestGlowCore.NpcInfo(file, file, quests, List.of());
    }

    public static void run() {
        System.out.println("== QuestGlowTest ==");

        // ---- anchor parsing
        QuestGlowCore.Anchor a = QuestGlowCore.parseAnchor(
                "em_primis,100,64,-200,0,0");
        Check.ok(a != null && a.world().equals("em_primis")
                        && a.x() == 100 && a.y() == 64 && a.z() == -200,
                "anchor: full spawn string parses");
        QuestGlowCore.Anchor b = QuestGlowCore.parseAnchor(
                "world,1.5,2.5,3.5");
        Check.ok(b != null && b.x() == 1.5 && b.z() == 3.5,
                "anchor: bare world,x,y,z parses");
        Check.eq(null, QuestGlowCore.parseAnchor("null"),
                "anchor: literal null is no home");
        Check.eq(null, QuestGlowCore.parseAnchor(""),
                "anchor: blank is no home");
        Check.eq(null, QuestGlowCore.parseAnchor(null),
                "anchor: null is no home");
        Check.eq(null, QuestGlowCore.parseAnchor("world,1,2"),
                "anchor: too few fields is no home");
        Check.eq(null, QuestGlowCore.parseAnchor("world,a,b,c"),
                "anchor: non-numeric is no home");

        // ---- radius
        Check.ok(QuestGlowCore.within(3, 4, 0, 5),
                "within: 3-4-5 lands exactly on the boundary");
        Check.ok(!QuestGlowCore.within(3, 4, 0.1, 5),
                "within: just past the boundary is out");
        Check.ok(QuestGlowCore.within(0, 0, 0, 64),
                "within: standing on the spot is in");

        // ---- the welcome-chain scenario
        QuestGlowCore.NpcInfo guide0 = npc("guide_0.yml",
                List.of("ag_welcome_quest_0.yml"));
        QuestGlowCore.NpcInfo guide1 = npc("guide_1.yml",
                List.of("ag_welcome_quest_1.yml",
                        "ag_adventurer_training.yml"));

        // Fresh player: welcome quest 0 is available at guide_0.
        Check.eq(GlowState.ACCEPT, QuestGlowCore.stateFor(
                        guide0, Set.of("ag_welcome_quest_0.yml"),
                        Map.of()),
                "fresh player: the giver glows ACCEPT");
        // A player EliteMobs offers nothing (completed / locked
        // out / chain-gated — all read as 'not available' here).
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        guide0, Set.of(), Map.of()),
                "nothing available: the giver stays dark");
        // The quest is carried now (the bridge subtracts carried
        // quests from the available set): no accept glow.
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        guide0, Set.of(), Map.of()),
                "carried quest: no ACCEPT at the giver");
        // Carried and finished, turned in at guide_1: guide_1
        // glows TURNIN even though the quest is not in its list.
        Check.eq(GlowState.TURNIN, QuestGlowCore.stateFor(
                        guide1, Set.of(),
                        Map.of("ag_welcome_quest_0.yml", "guide_1.yml")),
                "finished quest: the turn-in NPC glows TURNIN");
        // Carried but NOT finished: no turn-in glow anywhere.
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        guide1, Set.of(), Map.of()),
                "unfinished quest: no TURNIN glow");
        // A turn-in owed to a DIFFERENT NPC lights nothing here.
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        guide1, Set.of(),
                        Map.of("ag_welcome_quest_0.yml", "guide_0.yml")),
                "turn-in owed elsewhere: this NPC stays dark");

        // ---- precedence and multi-quest NPCs
        Check.eq(GlowState.TURNIN, QuestGlowCore.stateFor(
                        guide1, Set.of("ag_welcome_quest_1.yml"),
                        Map.of("ag_welcome_quest_0.yml", "guide_1.yml")),
                "both states at one NPC: TURNIN wins over ACCEPT");
        Check.eq(GlowState.ACCEPT, QuestGlowCore.stateFor(
                        guide1,
                        Set.of("ag_adventurer_training.yml"),
                        Map.of()),
                "one available quest of several: ACCEPT");
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        npc("santa.yml", List.of("xmas_quest_0.yml")),
                        Set.of("ag_welcome_quest_0.yml"), Map.of()),
                "someone else's available quest lights nothing here");

        // ---- defensive shapes
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        null, Set.of("ag_welcome_quest_0.yml"),
                        Map.of()),
                "null NPC: NONE");
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        guide0, null, null),
                "null player data: NONE");
        Check.eq(GlowState.NONE, QuestGlowCore.stateFor(
                        npc("quest_giver.yml", List.of()), Set.of(),
                        Map.of()),
                "an NPC with no quest list never glows ACCEPT");

        // ---- anchorless NPCs (the package-spawned shape: the
        // guild greeter Dux has spawnLocation 'null' and no
        // locations on the live server). Anchors locate an NPC;
        // they never gate its state — the module matches these
        // by name near the viewer.
        QuestGlowCore.NpcInfo dux = new QuestGlowCore.NpcInfo(
                "guide_0.yml", "Dux",
                List.of("ag_welcome_quest_0.yml"), List.of());
        Check.eq(GlowState.ACCEPT, QuestGlowCore.stateFor(
                        dux, Set.of("ag_welcome_quest_0.yml"),
                        Map.of()),
                "anchorless giver: ACCEPT still decided");
        Check.eq(GlowState.TURNIN, QuestGlowCore.stateFor(
                        dux, Set.of(),
                        Map.of("ag_welcome_quest_0.yml",
                                "guide_0.yml")),
                "anchorless giver: TURNIN still decided");
    }
}
