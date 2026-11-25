package excelemfixes.questglow;

import com.magmaguy.elitemobs.config.customquests.CustomQuestsConfig;
import com.magmaguy.elitemobs.config.customquests.CustomQuestsConfigFields;
import com.magmaguy.elitemobs.config.npcs.NPCsConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfigFields;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import excelemfixes.shared.TextUtil;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The EliteMobs bridge for quest-giver-glow: the only class in
 * the module that touches EliteMobs types (the EmQuestData /
 * EmQuestGate pattern). Everything is read from EliteMobs' own
 * loaded registries and per-player state — no EliteMobs file is
 * parsed here and none is ever written.
 *
 * Availability is EliteMobs' own verdict, not a re-derivation:
 * a quest counts as available for a player exactly when
 * {@code CustomQuest.hasPermissionForQuest} — the same check
 * EliteMobs runs before offering a quest — passes, and the
 * player is not already carrying the quest. That one call
 * carries EliteMobs' whole accept state: the quest being
 * enabled, its accept permissions (the chain gates quest packs
 * hang on previous completions), and its lockout state (a
 * finished one-time quest stays locked out; a repeatable quest
 * is locked out only inside its lockout minutes).
 *
 * Every read is guarded: an EliteMobs hiccup degrades to empty
 * data (no glow that cycle) instead of an error in a player's
 * face — for this module a missed glow is always preferable to
 * a wrong one.
 */
public final class EmQuestGlowData {

    private final Logger log;
    private boolean harvestWarned;

    public EmQuestGlowData(Logger log) {
        this.log = log;
    }

    /** Quest file -> the NPC file its turnInNPC names (enabled
     *  quests with a turn-in NPC only). */
    public Map<String, String> turnInByQuestFile() {
        Map<String, String> out = new HashMap<>();
        try {
            Map<String, CustomQuestsConfigFields> quests =
                    CustomQuestsConfig.getCustomQuests();
            if (quests == null) {
                // EliteMobs populates its registries a few
                // seconds after boot (observed on the mirror:
                // null at enable, live ~10 s later). An empty
                // read is retried on the next refresh, silently.
                return out;
            }
            for (CustomQuestsConfigFields fields : quests.values()) {
                if (fields == null || !fields.isEnabled()) continue;
                String turnIn = fields.getTurnInNPC();
                if (turnIn != null && !turnIn.isBlank()) {
                    out.put(fields.getFilename(), turnIn.trim());
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-giver-glow: EM quest "
                    + "registry read failed: " + t);
            return Map.of();
        }
        return out;
    }

    /**
     * Every NPC that can glow: NPCs that hand out at least one
     * quest, plus NPCs named as some quest's turn-in (a pure
     * turn-in NPC may hand out nothing itself). Disabled NPCs
     * never appear. An NPC whose data names no home spot (the
     * content-package-spawned shape — the Adventurers' Guild
     * greeter Dux is one) is kept with an empty anchor list and
     * matched by name near the viewer instead; it is never
     * dropped, because dropping it would silently un-light
     * exactly the greeters players meet first.
     */
    public List<QuestGlowCore.NpcInfo> npcs(Set<String> turnInTargets) {
        List<QuestGlowCore.NpcInfo> out = new ArrayList<>();
        try {
            Map<String, NPCsConfigFields> registry =
                    NPCsConfig.getNpcEntities();
            if (registry == null) {
                // See turnInByQuestFile: late-populating
                // registry; retry next refresh, silently.
                return out;
            }
            for (Map.Entry<String, NPCsConfigFields> e
                    : registry.entrySet()) {
                NPCsConfigFields fields = e.getValue();
                if (fields == null || !fields.isEnabled()) continue;
                List<String> quests = fields.getQuestFilenames();
                boolean gives = quests != null && !quests.isEmpty();
                boolean takes = turnInTargets != null
                        && turnInTargets.contains(e.getKey());
                if (!gives && !takes) continue;
                List<QuestGlowCore.Anchor> anchors = new ArrayList<>();
                QuestGlowCore.Anchor home =
                        QuestGlowCore.parseAnchor(fields.getSpawnLocation());
                if (home != null) anchors.add(home);
                if (fields.getLocations() != null) {
                    for (String loc : fields.getLocations()) {
                        QuestGlowCore.Anchor a =
                                QuestGlowCore.parseAnchor(loc);
                        if (a != null) anchors.add(a);
                    }
                }
                out.add(new QuestGlowCore.NpcInfo(
                        e.getKey(),
                        TextUtil.cleanNameplate(
                                fields.getName() == null
                                        ? "" : fields.getName()),
                        gives ? List.copyOf(quests) : List.of(),
                        List.copyOf(anchors)));
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-giver-glow: EM NPC "
                    + "registry read failed: " + t);
            return List.of();
        }
        return out;
    }

    /**
     * The config filename a spawned NPC belongs to, resolved
     * from its config-fields object: the registry's own entry
     * (identity first — EliteMobs spawns NPCs from the
     * registry's fields objects — then a value match on name
     * + quest list). Null when the NPC is not in the loaded
     * registry (an instanced or script-spawned NPC the glow
     * has no business lighting).
     */
    public String filenameFor(NPCsConfigFields fields) {
        if (fields == null) return null;
        try {
            Map<String, NPCsConfigFields> registry =
                    NPCsConfig.getNpcEntities();
            if (registry == null) return null;
            for (Map.Entry<String, NPCsConfigFields> e
                    : registry.entrySet()) {
                if (e.getValue() == fields) return e.getKey();
            }
            for (Map.Entry<String, NPCsConfigFields> e
                    : registry.entrySet()) {
                NPCsConfigFields candidate = e.getValue();
                if (candidate == null) continue;
                boolean sameName = java.util.Objects.equals(
                        candidate.getName(), fields.getName());
                boolean sameQuests = java.util.Objects.equals(
                        candidate.getQuestFilenames(),
                        fields.getQuestFilenames());
                if (sameName && sameQuests) return e.getKey();
            }
            // Last resort: the NPC's name alone, when exactly
            // one registry entry carries it (a spawned fields
            // copy whose quest list EliteMobs adjusted at
            // runtime must still resolve to its config).
            String byName = null;
            for (Map.Entry<String, NPCsConfigFields> e
                    : registry.entrySet()) {
                NPCsConfigFields candidate = e.getValue();
                if (candidate != null
                        && java.util.Objects.equals(
                                candidate.getName(),
                                fields.getName())) {
                    if (byName != null) {
                        byName = null;
                        break;
                    }
                    byName = e.getKey();
                }
            }
            return byName;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-giver-glow: EM NPC "
                    + "filename resolve failed: " + t);
        }
        return null;
    }

    /**
     * Every NPC EliteMobs currently has spawned: entity UUID
     * -> NPC config filename, harvested from EliteMobs'
     * persistent-object registry. This exists for the NPCs
     * that were already standing when the module enabled —
     * EliteMobs loads its hub world at boot, so its greeters
     * spawn before any listener can be registered (mirror-
     * proven, Nov 24, 2026: the anchored greeter could be
     * adopted from its post, the anchorless one could not be
     * found at all). The registry is EliteMobs-internal, so
     * the read is reflective and fully guarded: any shape
     * change degrades to an empty harvest and the module
     * falls back to spawn events + post adoption, never to
     * an error.
     */
    public Map<UUID, String> harvestLiveNpcs() {
        Map<UUID, String> out = new HashMap<>();
        try {
            Class<?> handlerClass = Class.forName(
                    "com.magmaguy.elitemobs.mobconstructor."
                            + "PersistentObjectHandler");
            java.lang.reflect.Field registryField =
                    handlerClass.getDeclaredField("persistentObjects");
            registryField.setAccessible(true);
            Object registry = registryField.get(null);
            if (registry == null) return out;
            java.lang.reflect.Method valuesMethod =
                    registry.getClass().getMethod("values");
            // The registry is a Guava synchronized multimap:
            // the method is public but its class is
            // package-private, so access must be opened
            // explicitly (mirror-proven, Nov 24, 2026).
            valuesMethod.setAccessible(true);
            Object values = valuesMethod.invoke(registry);
            if (!(values instanceof Iterable<?> handlers)) {
                return out;
            }
            java.lang.reflect.Field objectField =
                    handlerClass.getDeclaredField("persistentObject");
            objectField.setAccessible(true);
            Class<?> npcClass = Class.forName(
                    "com.magmaguy.elitemobs.npcs.NPCEntity");
            java.lang.reflect.Method getVillager =
                    npcClass.getMethod("getVillager");
            java.lang.reflect.Method getFields =
                    npcClass.getMethod("getNPCsConfigFields");
            for (Object handler : handlers) {
                try {
                    Object persistent = objectField.get(handler);
                    if (persistent == null
                            || !npcClass.isInstance(persistent)) {
                        continue;
                    }
                    Object villager = getVillager.invoke(persistent);
                    Object fields = getFields.invoke(persistent);
                    if (villager instanceof org.bukkit.entity.Villager v
                            && fields instanceof NPCsConfigFields f) {
                        String file = filenameFor(f);
                        if (file != null) {
                            out.put(v.getUniqueId(), file);
                        }
                    }
                } catch (Throwable ignored) {
                    // One unreadable entry never blocks the rest.
                }
            }
        } catch (Throwable t) {
            if (!harvestWarned) {
                harvestWarned = true;
                log.warning("ExcelEMFixes quest-giver-glow: EM "
                        + "live NPC harvest unavailable (" + t
                        + ") — falling back to spawn events and "
                        + "post adoption.");
            }
            return Map.of();
        }
        return out;
    }

    /**
     * The quests this player could walk up and accept right
     * now: EliteMobs' own accept check passes and the quest is
     * not already carried.
     */
    public Set<String> availableQuests(Player player,
                                       Set<String> activeFiles) {
        Set<String> out = new HashSet<>();
        try {
            Map<String, CustomQuestsConfigFields> quests =
                    CustomQuestsConfig.getCustomQuests();
            if (quests == null) {
                return out;
            }
            for (CustomQuestsConfigFields fields : quests.values()) {
                if (fields == null || !fields.isEnabled()) continue;
                String file = fields.getFilename();
                if (activeFiles.contains(file)) continue;
                if (CustomQuest.hasPermissionForQuest(player, fields)) {
                    out.add(file);
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-giver-glow: EM quest "
                    + "availability read failed for "
                    + player.getName() + ": " + t);
            return Set.of();
        }
        return out;
    }

    /** The player's carried custom quests: file -> objectives
     *  finished (ready to turn in). */
    public Map<String, Boolean> activeQuests(Player player) {
        Map<String, Boolean> out = new HashMap<>();
        try {
            List<Quest> quests = PlayerData.getQuests(
                    player.getUniqueId());
            if (quests == null) return out;
            for (Quest q : quests) {
                if (q instanceof CustomQuest custom) {
                    String file = custom.getConfigurationFilename();
                    if (file == null || file.isBlank()) continue;
                    boolean over = false;
                    try {
                        over = custom.getQuestObjectives() != null
                                && custom.getQuestObjectives().isOver();
                    } catch (Throwable ignored) {
                        // An unreadable objective set reads as
                        // "not finished" — never a false turn-in.
                    }
                    out.put(file, over);
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-giver-glow: EM active "
                    + "quest read failed for " + player.getName()
                    + ": " + t);
            return Map.of();
        }
        return out;
    }
}
