package excelemfixes.questdest;

import com.magmaguy.elitemobs.config.npcs.NPCsConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfigFields;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import com.magmaguy.elitemobs.quests.objectives.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The EliteMobs bridge for the turn-in-phase destination
 * switch (v27, FIX-LIST B2): the only turn-in class that
 * touches EliteMobs types (the EmDialogData / EmFetchData
 * pattern). It answers the two questions the pure
 * TurnInDerivation policy needs:
 *
 * 1. Is THIS player's copy of a quest finished? Read from
 *    the carried quest's own live objective objects: the
 *    quest counts as finished only when it has at least one
 *    objective and every objective reports complete. (A
 *    quest with no objectives at all is never called
 *    finished — there is nothing to have completed, and
 *    EliteMobs' own lifecycle owns that shape.)
 * 2. Who does the finished quest turn in to? The quest
 *    file's turnInNPC (CustomQuestsConfigFields
 *    .getTurnInNPC on the carried quest's own config
 *    fields); when the quest names none, the quest giver —
 *    the enabled NPC whose quest list (NPCsConfigFields
 *    .getQuestFilenames) names this quest, chosen by sorted
 *    filename so every player resolves the same giver when
 *    several NPCs hand the quest out.
 *
 * The NPC's spawn itself is NOT read here: EmDialogData
 * .spawnFor resolves it from the same NPC registry read the
 * dialog derivation uses, so both derivations agree on what
 * an NPC spawn is.
 *
 * Every read is guarded exactly like the sibling bridges:
 * an EliteMobs hiccup, an unreadable quest or an unknown
 * NPC all degrade to "no switch" (the pre-v27 resolution
 * stands) instead of an error in a player's face — for
 * guidance a missed destination is always preferable to a
 * wrong one. Nothing here parses or writes an EliteMobs
 * file; everything comes from EliteMobs' loaded registries
 * and per-player quest state.
 */
public final class EmTurnInData {

    private EmTurnInData() {}

    /**
     * A carried quest's turn-in state: whether the player
     * carries it at all, whether every objective is complete,
     * and the turn-in NPC's config filename (the giver's when
     * the quest names no turn-in NPC; null when neither can
     * be named — the spawn may still fail to resolve later,
     * which the policy treats as "no switch").
     */
    public record TurnInState(boolean carried, boolean allComplete,
                              String npcFile) {}

    private static final TurnInState NOT_CARRIED =
            new TurnInState(false, false, null);

    /**
     * The turn-in state of the player's carried quest with
     * the given file. NOT_CARRIED when the player is not
     * carrying it (or EliteMobs cannot say).
     */
    public static TurnInState turnInState(UUID uuid, String questFile,
                                          Logger log) {
        try {
            List<Quest> quests = PlayerData.getQuests(uuid);
            if (quests == null) return NOT_CARRIED;
            for (Quest q : quests) {
                if (!(q instanceof CustomQuest custom)) continue;
                String file = custom.getConfigurationFilename();
                if (file == null || !file.equals(questFile)) continue;
                boolean allComplete = allObjectivesComplete(custom);
                String npc = turnInNpcFile(custom, questFile, log);
                return new TurnInState(true, allComplete, npc);
            }
            return NOT_CARRIED;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM turn-in "
                    + "state read failed for " + uuid + ": " + t);
            return NOT_CARRIED;
        }
    }

    /**
     * True only when the quest has at least one objective and
     * every one of them is complete. An unreadable objective
     * set is NOT complete — never a guessed finish.
     */
    private static boolean allObjectivesComplete(CustomQuest custom) {
        try {
            if (custom.getQuestObjectives() == null
                    || custom.getQuestObjectives().getObjectives() == null) {
                return false;
            }
            List<Objective> objectives =
                    custom.getQuestObjectives().getObjectives();
            if (objectives.isEmpty()) return false;
            for (Objective objective : objectives) {
                if (!objective.isObjectiveCompleted()) return false;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * The quest's turn-in NPC filename: its own turnInNPC
     * field first; the quest giver (an enabled NPC whose
     * quest list names this quest) when the field is blank.
     * Null when neither names anyone.
     */
    private static String turnInNpcFile(CustomQuest custom,
                                        String questFile, Logger log) {
        try {
            if (custom.getCustomQuestsConfigFields() != null) {
                String turnIn = custom.getCustomQuestsConfigFields()
                        .getTurnInNPC();
                if (turnIn != null && !turnIn.isBlank()) {
                    return turnIn.trim();
                }
            }
        } catch (Throwable ignored) {
            // Fall through to the giver scan.
        }
        try {
            Map<String, NPCsConfigFields> registry =
                    NPCsConfig.getNpcEntities();
            if (registry == null) return null;
            List<String> files = new ArrayList<>(registry.keySet());
            files.sort(String::compareTo);
            for (String npcFile : files) {
                NPCsConfigFields fields = registry.get(npcFile);
                if (fields == null || !fields.isEnabled()) continue;
                List<String> given = fields.getQuestFilenames();
                if (given != null && given.contains(questFile)) {
                    return npcFile;
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM quest "
                    + "giver scan failed for " + questFile + ": " + t);
        }
        return null;
    }
}
