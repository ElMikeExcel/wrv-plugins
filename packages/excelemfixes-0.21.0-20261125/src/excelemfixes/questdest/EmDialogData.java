package excelemfixes.questdest;

import com.magmaguy.elitemobs.config.npcs.NPCsConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfigFields;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import com.magmaguy.elitemobs.quests.objectives.DialogObjective;
import com.magmaguy.elitemobs.quests.objectives.Objective;
import excelemfixes.questglow.QuestGlowCore;
import excelemfixes.shared.TextUtil;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The EliteMobs bridge for dialog-destination derivation
 * (0.19.0): the only derivation class that touches EliteMobs
 * types (the EmQuestData / EmQuestGlowData pattern). It answers
 * the two questions the pure DialogDerivation policy needs:
 *
 * 1. Which NPC does THIS player's copy of a quest send them to
 *    talk to right now? Read from the carried quest's own live
 *    objective objects: the first incomplete DialogObjective,
 *    in objective order, carries its NPC's config filename
 *    (EliteMobs' DialogObjective.getNpcFilename). A completed
 *    dialog drops out the moment it is done, which is exactly
 *    when the beam must move on to the next objective.
 * 2. Where does that NPC live? Its spawn location from
 *    EliteMobs' NPC registry (NPCsConfig — the same source the
 *    quest-giver-glow harvest reads), parsed with the glow
 *    module's own anchor parser so both modules agree on what
 *    a spawn location is.
 *
 * Every read is guarded: an EliteMobs hiccup, an unreadable
 * quest, an unknown/disabled NPC or an unparseable spawn
 * location all degrade to "no derivation" (the table row, or
 * no destination, stands) instead of an error in a player's
 * face — for guidance a missed destination is always
 * preferable to a wrong one. Nothing here parses or writes an
 * EliteMobs file; everything comes from EliteMobs' loaded
 * registries and per-player quest state.
 */
public final class EmDialogData {

    private EmDialogData() {}

    /**
     * The NPC config filename of the first incomplete DIALOG
     * objective of the player's carried quest with the given
     * file, or null when the player is not carrying it or it
     * has no dialog objective outstanding.
     */
    public static String dialogNpcFor(UUID uuid, String questFile,
                                      Logger log) {
        try {
            List<Quest> quests = PlayerData.getQuests(uuid);
            if (quests == null) return null;
            for (Quest q : quests) {
                if (!(q instanceof CustomQuest custom)) continue;
                String file = custom.getConfigurationFilename();
                if (file == null || !file.equals(questFile)) continue;
                try {
                    if (custom.getQuestObjectives() == null
                            || custom.getQuestObjectives()
                                    .getObjectives() == null) {
                        return null;
                    }
                    for (Objective objective : custom
                            .getQuestObjectives().getObjectives()) {
                        if (objective instanceof DialogObjective dialog
                                && !dialog.isObjectiveCompleted()) {
                            String npc = dialog.getNpcFilename();
                            if (npc != null && !npc.isBlank()) {
                                return npc.trim();
                            }
                        }
                    }
                } catch (Throwable ignored) {
                    // An unreadable objective set contributes no
                    // dialog — never a guessed target.
                    return null;
                }
                return null;
            }
            return null;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM dialog "
                    + "objective read failed for " + uuid + ": " + t);
            return null;
        }
    }

    /**
     * The spawn of an NPC from EliteMobs' NPC registry, or null
     * when the NPC is unknown, disabled, or has no parseable
     * spawn location (EliteMobs' "no fixed home" shapes are
     * never guessed at — the same truthful boundary the glow
     * module documents for anchorless NPCs).
     */
    public static DialogDerivation.NpcSpawn spawnFor(String npcFile,
                                                     Logger log) {
        try {
            Map<String, NPCsConfigFields> registry =
                    NPCsConfig.getNpcEntities();
            if (registry == null) return null;
            NPCsConfigFields fields = registry.get(npcFile);
            if (fields == null || !fields.isEnabled()) return null;
            QuestGlowCore.Anchor anchor =
                    QuestGlowCore.parseAnchor(fields.getSpawnLocation());
            if (anchor == null) return null;
            String name = TextUtil.cleanNameplate(
                    fields.getName() == null ? "" : fields.getName());
            return new DialogDerivation.NpcSpawn(
                    npcFile,
                    name == null || name.isBlank() ? npcFile : name,
                    anchor.world(), anchor.x(), anchor.y(), anchor.z());
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM NPC "
                    + "spawn read failed for " + npcFile + ": " + t);
            return null;
        }
    }
}
