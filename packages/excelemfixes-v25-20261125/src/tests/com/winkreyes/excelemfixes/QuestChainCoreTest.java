import excelemfixes.questchain.QuestChainCore;
import excelemfixes.questchain.QuestChainCore.NextStep;
import excelemfixes.questchain.QuestChainCore.QuestInfo;
import excelemfixes.shared.TextUtil;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Quest-chain clarity: the walk names the deepest unfinished
 * link of a locked chain (the specimen is Jester's lighthouse
 * chain, Nov 24, 2026, rebuilt from the live quest files),
 * stops at the first quest the player can actually accept,
 * stays silent when nothing is chain-locked, dedupes chains
 * that share a next step, caps its lines, and survives
 * cycles and permissions no loaded quest grants.
 */
public final class QuestChainCoreTest {

    private static final String SICK_MAN =
            "primis_pontis_groves_lighthouse_sick_man.yml";
    private static final String NEMIAN =
            "primis_groves_nemus_leader.yml";
    private static final String BROMIA =
            "primis_pontis_mille_general_bromia.yml";

    private static final String P_MAIN11 =
            "elitequest.primis_main_quest_11.yml";
    private static final String P_GROVES =
            "elitequest.primis_pontis_mille_the_groves_call.yml";
    private static final String P_POP =
            "elitequest.primis_pontis_groves_population_control.yml";
    private static final String P_LIGHT1 =
            "elitequest.primis_pontis_groves_lighthouse_1.yml";

    private static QuestInfo quest(String file, String name,
                                   List<String> accept,
                                   String lockout,
                                   String turnIn,
                                   List<String> objectiveNpcs,
                                   String objectiveNpcName,
                                   String objectiveLocation) {
        return new QuestInfo(file, name, accept, lockout, turnIn,
                objectiveNpcs, objectiveNpcName, objectiveLocation);
    }

    /** The lighthouse chain exactly as the live files define it. */
    private static QuestChainCore specimen() {
        List<QuestInfo> quests = List.of(
                quest("primis_pontis_groves_lighthouse_2.yml",
                        "Do not ask Jeeves", List.of(P_LIGHT1),
                        "elitequest.primis_pontis_groves_lighthouse_2.yml",
                        SICK_MAN,
                        List.of("primis_caeruleaum_jeeves.yml"),
                        "Jeeves", "at Caeruleaum Village"),
                quest("primis_pontis_groves_lighthouse_1.yml",
                        "Feeding the sick", List.of(P_POP),
                        P_LIGHT1, SICK_MAN,
                        List.of(SICK_MAN),
                        "Alanus Secundas", "at the Lighthouse"),
                quest("primis_pontis_groves_population_control.yml",
                        "Population control!", List.of(P_GROVES),
                        P_POP, NEMIAN,
                        List.of("primis_roofed_forest_spider.yml",
                                "primis_pontis_redwoods_spider_legs.yml"),
                        null, null),
                quest("primis_pontis_mille_the_groves_call.yml",
                        "The groves call!", List.of(P_MAIN11),
                        P_GROVES, NEMIAN,
                        List.of(NEMIAN),
                        "King Nemian", "in The Groves"),
                quest("primis_main_quest_11.yml",
                        "The main quest, part 11", List.of(),
                        P_MAIN11, null,
                        List.of(), null, null));
        Map<String, List<String>> givers = Map.of(
                SICK_MAN, List.of(
                        "primis_pontis_groves_lighthouse_2.yml"),
                NEMIAN, List.of(
                        "primis_pontis_groves_population_control.yml",
                        "primis_pontis_groves_lighthouse_1.yml"),
                BROMIA, List.of(
                        "primis_pontis_mille_the_groves_call.yml"));
        Map<String, String> names = Map.of(
                SICK_MAN, "Alanus Secundas",
                NEMIAN, "King Nemian",
                BROMIA, "Bromia Curio");
        return new QuestChainCore(quests, givers, names);
    }

    private static String plain(NextStep step) {
        return TextUtil.plain(QuestChainCore.formatLine(step));
    }

    public static void run() {
        System.out.println("QuestChainCoreTest:");
        QuestChainCore core = specimen();

        // Nothing held: the walk grounds out at the first
        // quest of the chain the player can accept — the main
        // quest, whose own accept list is empty.
        List<NextStep> steps = core.nextSteps(
                SICK_MAN, Set.of(), Set.of(), 3);
        Check.eq(1, steps.size(), "no-perms: one deduped step");
        Check.eq("primis_main_quest_11.yml", steps.get(0).questFile(),
                "no-perms: walk reaches the chain's ground");

        // The specimen: main quest done, nothing else — the
        // next step is The Groves Call, given by Bromia, and
        // the place is the one its objective names.
        steps = core.nextSteps(SICK_MAN, Set.of(P_MAIN11), Set.of(), 3);
        Check.eq(1, steps.size(), "specimen: one step");
        Check.eq("The groves call!", steps.get(0).questName(),
                "specimen: names The groves call!");
        Check.eq("Bromia Curio", steps.get(0).giverName(),
                "specimen: giver is Bromia Curio");
        Check.eq("in The Groves", steps.get(0).location(),
                "specimen: location from the objective");
        Check.eq("Locked: finish 'The groves call!' first — "
                        + "Bromia Curio in The Groves gives it.",
                plain(steps.get(0)), "specimen: the line itself");

        // Groves Call done too: Population Control is next,
        // King Nemian gives it, and its objectives name no
        // place, so the line carries none.
        steps = core.nextSteps(SICK_MAN,
                Set.of(P_MAIN11, P_GROVES), Set.of(), 3);
        Check.eq(1, steps.size(), "groves done: one step");
        Check.eq("Locked: finish 'Population control!' first — "
                        + "King Nemian gives it.",
                plain(steps.get(0)), "groves done: the line itself");

        // Population Control done: Feeding the Sick itself is
        // now acceptable, so the only locked chain left is
        // Do not ask Jeeves — and its next step IS Feeding
        // the Sick (given by King Nemian, at the Lighthouse).
        steps = core.nextSteps(SICK_MAN,
                Set.of(P_MAIN11, P_GROVES, P_POP), Set.of(), 3);
        Check.eq(1, steps.size(), "pop done: one step");
        Check.eq("Locked: finish 'Feeding the sick' first — "
                        + "King Nemian at the Lighthouse gives it.",
                plain(steps.get(0)), "pop done: the line itself");

        // Everything done: silence.
        steps = core.nextSteps(SICK_MAN,
                Set.of(P_MAIN11, P_GROVES, P_POP, P_LIGHT1),
                Set.of(), 3);
        Check.eq(0, steps.size(), "all done: no steps");

        // A carried quest is never reported: carrying
        // lighthouse_2 with only the main quest done still
        // surfaces the chain through lighthouse_1 — once.
        steps = core.nextSteps(SICK_MAN, Set.of(P_MAIN11),
                Set.of("primis_pontis_groves_lighthouse_2.yml"), 3);
        Check.eq(1, steps.size(), "carried quest skipped, chain kept");
        Check.eq("The groves call!", steps.get(0).questName(),
                "carried quest skipped: same next step");

        // Clicking the giver mid-chain works the same: at
        // King Nemian with the main quest done, both of his
        // locked quests walk to The Groves Call — one line.
        steps = core.nextSteps(NEMIAN, Set.of(P_MAIN11), Set.of(), 3);
        Check.eq(1, steps.size(), "nemian click: deduped to one line");
        Check.eq("The groves call!", steps.get(0).questName(),
                "nemian click: names The groves call!");

        // A permission no loaded quest grants: the chain
        // cannot be named, so nothing is said.
        QuestChainCore orphan = new QuestChainCore(
                List.of(quest("q_orphan.yml", "Orphan quest",
                        List.of("elitequest.nothere.yml"),
                        "elitequest.q_orphan.yml", "npc_a.yml",
                        List.of(), null, null)),
                Map.of("npc_a.yml", List.of("q_orphan.yml")),
                Map.of("npc_a.yml", "Npc A"));
        Check.eq(0, orphan.nextSteps("npc_a.yml",
                Set.of(), Set.of(), 3).size(),
                "unnameable prerequisite: silent");

        // A cycle (two quests gating each other) terminates
        // silently instead of looping.
        QuestChainCore cyclic = new QuestChainCore(
                List.of(
                        quest("q_a.yml", "Quest A",
                                List.of("elitequest.q_b.yml"),
                                "elitequest.q_a.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_b.yml", "Quest B",
                                List.of("elitequest.q_a.yml"),
                                "elitequest.q_b.yml", "npc_a.yml",
                                List.of(), null, null)),
                Map.of("npc_a.yml", List.of("q_a.yml", "q_b.yml")),
                Map.of("npc_a.yml", "Npc A"));
        Check.eq(0, cyclic.nextSteps("npc_a.yml",
                Set.of(), Set.of(), 3).size(),
                "cycle: silent, no hang");

        // The line cap holds across several distinct chains.
        QuestChainCore many = new QuestChainCore(
                List.of(
                        quest("q_1.yml", "One", List.of("elitequest.q_0.yml"),
                                "elitequest.q_1.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_2.yml", "Two", List.of("elitequest.q_0.yml"),
                                "elitequest.q_2.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_0.yml", "Zero", List.of(),
                                "elitequest.q_0.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_3.yml", "Three", List.of("elitequest.q_4.yml"),
                                "elitequest.q_3.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_4.yml", "Four", List.of(),
                                "elitequest.q_4.yml", "npc_a.yml",
                                List.of(), null, null)),
                Map.of("npc_a.yml", List.of(
                        "q_1.yml", "q_2.yml", "q_3.yml")),
                Map.of("npc_a.yml", "Npc A"));
        List<NextStep> capped = many.nextSteps(
                "npc_a.yml", Set.of(), Set.of(), 1);
        Check.eq(1, capped.size(), "cap: maxLines respected");
        List<NextStep> two = many.nextSteps(
                "npc_a.yml", Set.of(), Set.of(), 3);
        Check.eq(2, two.size(),
                "cap: distinct next steps, deduped");

        // A quest with several unmet accept permissions walks
        // its first one.
        QuestChainCore multi = new QuestChainCore(
                List.of(
                        quest("q_gate.yml", "Gated",
                                List.of("elitequest.q_a.yml",
                                        "elitequest.q_b.yml"),
                                "elitequest.q_gate.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_a.yml", "Quest A", List.of(),
                                "elitequest.q_a.yml", "npc_a.yml",
                                List.of(), null, null),
                        quest("q_b.yml", "Quest B", List.of(),
                                "elitequest.q_b.yml", "npc_a.yml",
                                List.of(), null, null)),
                Map.of("npc_a.yml", List.of("q_gate.yml")),
                Map.of("npc_a.yml", "Npc A"));
        List<NextStep> m = multi.nextSteps(
                "npc_a.yml", Set.of(), Set.of(), 3);
        Check.eq(1, m.size(), "multi-perm: one step");
        Check.eq("Quest A", m.get(0).questName(),
                "multi-perm: walks the first unmet permission");

        // An NPC with no quests at all: silence.
        Check.eq(0, core.nextSteps("primis_nobody.yml",
                Set.of(), Set.of(), 3).size(),
                "unknown NPC: no steps");

        // ---- signposting (0.18.0): a completion names the
        // quests it just unlocked, and only those.
        // Completing the main quest unlocks The Groves Call
        // — and NOT Feeding the Sick, which still waits on
        // Population Control two links further down.
        List<NextStep> unlocked = core.unlockedBy(
                "primis_main_quest_11.yml",
                Set.of(P_MAIN11), Set.of(), 3);
        Check.eq(1, unlocked.size(), "unlock: one quest named");
        Check.eq("New quest unlocked: 'The groves call!' — "
                        + "Bromia Curio in The Groves gives it.",
                TextUtil.plain(QuestChainCore.formatUnlockLine(
                        unlocked.get(0))),
                "unlock: the line itself");

        // Completing The Groves Call unlocks Population
        // Control, given by King Nemian, no place named.
        unlocked = core.unlockedBy(
                "primis_pontis_mille_the_groves_call.yml",
                Set.of(P_MAIN11, P_GROVES), Set.of(), 3);
        Check.eq(1, unlocked.size(), "unlock 2: one quest named");
        Check.eq("New quest unlocked: 'Population control!' — "
                        + "King Nemian gives it.",
                TextUtil.plain(QuestChainCore.formatUnlockLine(
                        unlocked.get(0))),
                "unlock 2: the line itself");

        // Completing Population Control unlocks Feeding the
        // Sick (King Nemian hands it out, at the Lighthouse).
        unlocked = core.unlockedBy(
                "primis_pontis_groves_population_control.yml",
                Set.of(P_MAIN11, P_GROVES, P_POP), Set.of(), 3);
        Check.eq(1, unlocked.size(), "unlock 3: one quest named");
        Check.eq("primis_pontis_groves_lighthouse_1.yml",
                unlocked.get(0).questFile(),
                "unlock 3: Feeding the sick named");

        // Completing Feeding the Sick unlocks Do not ask
        // Jeeves — Alanus hands it out; its own objective
        // names the place.
        unlocked = core.unlockedBy(
                "primis_pontis_groves_lighthouse_1.yml",
                Set.of(P_MAIN11, P_GROVES, P_POP, P_LIGHT1),
                Set.of(), 3);
        Check.eq(1, unlocked.size(), "unlock 4: one quest named");
        Check.eq("New quest unlocked: 'Do not ask Jeeves' — "
                        + "Alanus Secundas at Caeruleaum Village "
                        + "gives it.",
                TextUtil.plain(QuestChainCore.formatUnlockLine(
                        unlocked.get(0))),
                "unlock 4: the line itself");

        // A quest the player already finished once is not
        // news: Population Control completed earlier, then
        // The Groves Call completes (again, on a repeat) —
        // nothing is newly unlocked.
        Check.eq(0, core.unlockedBy(
                        "primis_pontis_mille_the_groves_call.yml",
                        Set.of(P_MAIN11, P_GROVES, P_POP),
                        Set.of(), 3).size(),
                "unlock: already-completed stays silent");

        // A quest already being carried is not news either.
        Check.eq(0, core.unlockedBy(
                        "primis_main_quest_11.yml",
                        Set.of(P_MAIN11),
                        Set.of("primis_pontis_mille_the_groves_call.yml"),
                        3).size(),
                "unlock: carried quest stays silent");

        // Unknown completion / unreadable progress: silence.
        Check.eq(0, core.unlockedBy("primis_nothere.yml",
                Set.of(P_MAIN11), Set.of(), 3).size(),
                "unlock: unknown quest stays silent");
        Check.eq(0, core.unlockedBy("primis_main_quest_11.yml",
                null, Set.of(), 3).size(),
                "unlock: null progress stays silent");

        // The cap holds for signposting too.
        Check.eq(1, many.unlockedBy("q_0.yml",
                        Set.of("elitequest.q_0.yml"), Set.of(), 1)
                        .size(),
                "unlock: cap respected");
        Check.eq(2, many.unlockedBy("q_0.yml",
                        Set.of("elitequest.q_0.yml"), Set.of(), 3)
                        .size(),
                "unlock: both direct dependents named");
    }
}
