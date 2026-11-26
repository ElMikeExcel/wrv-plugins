package excelemfixes.questdest;

import java.util.List;

/**
 * The dialog-destination derivation policy (0.19.0): how a
 * quest's destination is chosen when EliteMobs' own data can
 * say where the player should go next. Pure data + decision:
 * no Bukkit, no EliteMobs types, so the plain-JVM suites
 * exercise every branch directly. The EliteMobs reads that
 * feed it live in EmDialogData; the consumers (the quest
 * beacon, /questwhere) read the result through the
 * quest-destination API.
 *
 * WHY THIS EXISTS (Jester's report, Nov 25, 2026): he was
 * carrying "The groves call!", whose only objective is talking
 * to King Nemian — and got no beam and no /questwhere answer,
 * because the destinations table only ever covered the
 * tracker-blank kill quests (11 hand-written rows). A
 * glow/beacon audit the same night counted 48 of 105 Primis
 * quests carrying DIALOG objectives, 46 of them with no table
 * row at all: a tracked dialog quest could never produce a
 * beam, and the two dialog quests that did have rows pointed
 * at their kill destinations, never at the person to talk to.
 *
 * THE RULES, in order:
 * 1. TABLE ROWS KEEP PRECEDENCE. A hand-written destinations
 *    table row stands whenever one exists — rows encode
 *    instance entry points EliteMobs' data cannot express,
 *    which is what the table exists for.
 * 2. DERIVATION FILLS THE GAP. Only when a tracked/carried
 *    quest has NO table row is its destination derived from
 *    its current incomplete DIALOG objective: that objective's
 *    target NPC, at its spawn location from EliteMobs' NPC
 *    registry. Because the derivation always reads the
 *    CURRENT incomplete dialog objective, the beam follows
 *    the current objective — when a dialog completes, the
 *    next incomplete dialog objective (if any) takes over,
 *    and when none remains there is no derived destination.
 * 3. NOTHING OTHERWISE. No row, no dialog: no destination —
 *    exactly the pre-0.19.0 behavior.
 *
 * Everything is derived from EliteMobs' own quest/NPC data at
 * runtime: no quest, NPC or place is named in code, so any
 * server whose content uses dialog objectives gets the same
 * coverage, and future content is covered the day it lands —
 * no hand-maintained rows duplicating data EliteMobs holds.
 */
public final class DialogDerivation {

    private DialogDerivation() {}

    /**
     * A dialog objective's target NPC, resolved from EliteMobs'
     * NPC registry: its config filename, display name and spawn
     * location. A null spawn (the NPC is not in the registry, is
     * disabled, or has no parseable spawn location) means "no
     * derivation" — the destination is never guessed at.
     */
    public record NpcSpawn(String npcFile, String npcName,
                           String world, double x, double y, double z) {}

    /** Where a resolved destination came from. */
    public enum Source { TABLE, DIALOG, NONE }

    /** The resolution outcome: the winning source + destination
     *  (destination is null exactly when source is NONE). */
    public record Resolution(Source source,
                             QuestDestinations.Destination destination) {}

    /**
     * Choose a quest's destination.
     *
     * @param enabled   the derivation toggle
     *                  (quest-destinations.derive-dialog-destinations)
     * @param tableRow  the destinations-table row for the quest,
     *                  or null when the table does not cover it;
     *                  a non-null row always wins (rule 1)
     * @param questFile the quest's configuration filename
     * @param questName the quest's display name (for the derived
     *                  row; may be null — the file stands in)
     * @param spawn     the current incomplete dialog objective's
     *                  target NPC spawn, or null when the quest
     *                  has no dialog objective outstanding (or
     *                  its target cannot be resolved)
     */
    public static Resolution resolve(boolean enabled,
                                     QuestDestinations.Destination tableRow,
                                     String questFile, String questName,
                                     NpcSpawn spawn) {
        if (tableRow != null) {
            return new Resolution(Source.TABLE, tableRow);
        }
        if (enabled && spawn != null) {
            return new Resolution(Source.DIALOG,
                    derivedDestination(questFile, questName, spawn));
        }
        return new Resolution(Source.NONE, null);
    }

    /**
     * The derived destination as a table-shaped row, so every
     * existing consumer (beam, /questwhere screens, the glow
     * guide) treats it exactly like a hand-written row. The
     * glow mode is "entity": the destination IS a person, so
     * guidance lights the NPC itself rather than a marker.
     */
    public static QuestDestinations.Destination derivedDestination(
            String questFile, String questName, NpcSpawn spawn) {
        String npc = spawn.npcName() == null || spawn.npcName().isBlank()
                ? spawn.npcFile() : spawn.npcName();
        String quest = questName == null || questName.isBlank()
                ? questFile : questName;
        return new QuestDestinations.Destination(
                questFile,
                quest,
                "Talk to " + npc,
                npc,
                spawn.world(),
                spawn.world(),
                spawn.x(), spawn.y(), spawn.z(),
                "Talk to " + npc,
                "COMPASS",
                List.of("Your quest sends you to talk to " + npc
                        + " — head to the marked spot and speak "
                        + "with them."),
                "entity",
                npc);
    }
}
