package excelemfixes.questdest;

import java.util.List;

/**
 * The turn-in-phase destination switch (v27, quest-guidance
 * re-audit FIX-LIST B2): when a tracked/carried quest's
 * objectives are ALL complete, the quest's resolved
 * destination switches to the turn-in NPC's spawn. Pure data
 * + decision: no Bukkit, no EliteMobs types, so the plain-JVM
 * suites exercise every branch directly. The EliteMobs reads
 * that feed it live in EmTurnInData; the spawn resolution
 * reuses EmDialogData (the 0.19.0 NPC-registry read), and the
 * consumers (the quest beacon, /questwhere, the glow guide)
 * all read the result through the one shared resolution point
 * in QuestDestinationModule, so this single switch moves all
 * three surfaces together.
 *
 * WHY THIS EXISTS (quest-guidance re-audit, Nov 26, 2026):
 * guidance followed a quest's objectives but never its final
 * step. When the last objective completed, the destination
 * stayed wherever the objective phase had pointed — for
 * "Storm's end" (primis_main_quest_14) the Storm Temple
 * instance entry, 414 blocks from Shogun Navita in Pontis
 * Mille, the NPC the finished quest turns in to. The re-audit
 * counted this as the turn-in-far class: a turn-in NPC who is
 * a different person, never visited during the quest, far
 * from the nearest objective. The only stock signal was
 * EliteMobs' green glow once the player was already nearby —
 * which is to say, once they no longer needed directions.
 *
 * THE RULES, in order:
 * 1. ALL COMPLETE, OR NOTHING. The switch fires only when
 *    the player's own copy of the quest has at least one
 *    objective and every objective is complete. Any objective
 *    outstanding — or an unreadable objective set — and this
 *    policy stays silent: the objective-phase channels (table
 *    row, dialog/fetch derivation) own the destination,
 *    exactly as before.
 * 2. THE SWITCH OUTRANKS THE TABLE. Unlike the 0.19.0 dialog
 *    derivation, a hand-written table row does NOT keep
 *    precedence here: a row encodes where the quest's
 *    OBJECTIVES happen (an instance entry, a kill ground),
 *    and once no objective remains that place is spent — the
 *    one-row-per-quest table cannot express the phase change,
 *    which is the whole defect. The module therefore consults
 *    this policy BEFORE the table. (The row still stands for
 *    every earlier phase, untouched.)
 * 3. TURN-IN NPC, ELSE THE GIVER. The destination is the
 *    quest file's turnInNPC spawn; when the quest names no
 *    turn-in NPC, the quest giver's spawn (the NPC whose
 *    quest list names this quest — EliteMobs' own record of
 *    who hands it out) stands in, since turning in to the
 *    person who gave the quest is EliteMobs' default shape.
 * 4. NEVER GUESSED. An unknown/disabled NPC or an
 *    unparseable spawn means no switch: the pre-v27
 *    resolution (table row, or nothing) stands. A missed
 *    destination is always preferable to a wrong one.
 *
 * Everything is derived from EliteMobs' own quest/NPC data
 * at runtime: no quest, NPC or place is named in code, so
 * any server whose quests name turn-in NPCs gets the same
 * coverage, and future content is covered the day it lands.
 */
public final class TurnInDerivation {

    private TurnInDerivation() {}

    /** Where a resolved destination came from. */
    public enum Source { TURN_IN, NONE }

    /** The resolution outcome: the winning source +
     *  destination (destination is null exactly when source
     *  is NONE). */
    public record Resolution(Source source,
                             QuestDestinations.Destination destination) {}

    /**
     * Choose a quest's turn-in-phase destination.
     *
     * @param enabled     the switch toggle
     *                    (quest-destinations.derive-turn-in-destinations)
     * @param allComplete true when the player's copy of the
     *                    quest has at least one objective and
     *                    every objective is complete
     * @param questFile   the quest's configuration filename
     * @param questName   the quest's display name (for the
     *                    derived row; may be null — the file
     *                    stands in)
     * @param spawn       the turn-in NPC's spawn (the giver's
     *                    when the quest names no turn-in NPC),
     *                    or null when it cannot be resolved
     */
    public static Resolution resolve(boolean enabled,
                                     boolean allComplete,
                                     String questFile, String questName,
                                     DialogDerivation.NpcSpawn spawn) {
        if (enabled && allComplete && spawn != null) {
            return new Resolution(Source.TURN_IN,
                    derivedDestination(questFile, questName, spawn));
        }
        return new Resolution(Source.NONE, null);
    }

    /**
     * The turn-in destination as a table-shaped row, so every
     * existing consumer (beam, /questwhere screens, the glow
     * guide) treats it exactly like a hand-written row. The
     * glow mode is "entity": the destination IS a person, so
     * guidance lights the NPC itself rather than a marker —
     * the same shape the dialog derivation uses.
     */
    public static QuestDestinations.Destination derivedDestination(
            String questFile, String questName,
            DialogDerivation.NpcSpawn spawn) {
        String npc = spawn.npcName() == null || spawn.npcName().isBlank()
                ? spawn.npcFile() : spawn.npcName();
        String quest = questName == null || questName.isBlank()
                ? questFile : questName;
        return new QuestDestinations.Destination(
                questFile,
                quest,
                "Turn in to " + npc,
                npc,
                spawn.world(),
                spawn.world(),
                spawn.x(), spawn.y(), spawn.z(),
                "Turn in to " + npc,
                "COMPASS",
                List.of("You have finished everything this quest "
                        + "asks — head back to " + npc + " to "
                        + "turn it in and collect your reward."),
                "entity",
                npc);
    }
}
