package excelemfixes.questchain;

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
 * The EliteMobs bridge for quest-chain-clarity: the only
 * class in the module that touches EliteMobs types (the
 * EmQuestData / EmQuestGlowData pattern). Everything is read
 * from EliteMobs' own loaded registries — the live, parsed
 * form of its customquests / npcs files, with EliteMobs' own
 * defaults already applied — and nothing is ever written.
 *
 * A player's completed quests are read the way EliteMobs
 * itself reads them in CustomQuest.hasPermissionForQuest:
 * as Bukkit metadata markers named by each quest's lockout
 * permission, which EliteMobs' PlayerQuestCooldowns sets
 * when a quest completes and re-applies at login. No
 * EliteMobs file is parsed here and no permission plugin is
 * consulted — the marker IS EliteMobs' completion record.
 *
 * Every read is guarded: an EliteMobs hiccup degrades to
 * empty data (no explanation that click) instead of an error
 * in a player's face — a missed explanation is always
 * preferable to a wrong one.
 */
public final class EmQuestChainData {

    private final Logger log;
    private boolean harvestWarned;

    public EmQuestChainData(Logger log) {
        this.log = log;
    }

    /** A fresh chain index from EliteMobs' loaded data. */
    public QuestChainCore snapshot() {
        try {
            Map<String, CustomQuestsConfigFields> quests =
                    CustomQuestsConfig.getCustomQuests();
            Map<String, NPCsConfigFields> npcs =
                    NPCsConfig.getNpcEntities();
            if (quests == null || npcs == null) {
                // EliteMobs populates its registries a few
                // seconds after boot (observed on the mirror:
                // null at enable, live ~10 s later). An empty
                // snapshot is retried on the next refresh.
                return new QuestChainCore(List.of(), Map.of(),
                        Map.of());
            }
            List<QuestChainCore.QuestInfo> infos = new ArrayList<>();
            for (CustomQuestsConfigFields fields : quests.values()) {
                if (fields == null || !fields.isEnabled()) continue;
                infos.add(toInfo(fields));
            }
            Map<String, List<String>> npcQuestFiles = new HashMap<>();
            Map<String, String> npcNames = new HashMap<>();
            for (Map.Entry<String, NPCsConfigFields> e
                    : npcs.entrySet()) {
                NPCsConfigFields fields = e.getValue();
                if (fields == null || !fields.isEnabled()) continue;
                List<String> given = fields.getQuestFilenames();
                npcQuestFiles.put(e.getKey(),
                        given == null ? List.of() : List.copyOf(given));
                npcNames.put(e.getKey(), TextUtil.cleanNameplate(
                        fields.getName() == null ? "" : fields.getName()));
            }
            return new QuestChainCore(infos, npcQuestFiles, npcNames);
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-chain-clarity: EM "
                    + "registry read failed: " + t);
            return new QuestChainCore(List.of(), Map.of(), Map.of());
        }
    }

    private static QuestChainCore.QuestInfo toInfo(
            CustomQuestsConfigFields fields) {
        List<String> acceptPerms = new ArrayList<>();
        String singular = fields.getQuestAcceptPermission();
        if (singular != null && !singular.isBlank()) {
            acceptPerms.add(singular.trim());
        }
        List<String> listed = fields.getQuestAcceptPermissions();
        if (listed != null) {
            for (String perm : listed) {
                if (perm != null && !perm.isBlank()
                        && !acceptPerms.contains(perm.trim())) {
                    acceptPerms.add(perm.trim());
                }
            }
        }
        List<String> objectiveNpcFiles = new ArrayList<>();
        String objectiveNpcName = null;
        String objectiveLocation = null;
        Map<String, Map<String, Object>> objectives =
                fields.getCustomObjectives();
        if (objectives != null) {
            for (Map<String, Object> objective : objectives.values()) {
                if (objective == null) continue;
                Object filename = objective.get("filename");
                if (filename instanceof String s && !s.isBlank()
                        && !objectiveNpcFiles.contains(s.trim())) {
                    objectiveNpcFiles.add(s.trim());
                }
                if (objectiveNpcName == null) {
                    Object npcName = objective.get("npcName");
                    if (npcName instanceof String s && !s.isBlank()) {
                        objectiveNpcName = TextUtil.plain(s);
                    }
                }
                if (objectiveLocation == null) {
                    Object location = objective.get("location");
                    if (location instanceof String s && !s.isBlank()) {
                        objectiveLocation = TextUtil.plain(s);
                    }
                }
            }
        }
        String turnIn = fields.getTurnInNPC();
        return new QuestChainCore.QuestInfo(
                fields.getFilename(),
                TextUtil.plain(fields.getQuestName()),
                acceptPerms,
                fields.getQuestLockoutPermission(),
                turnIn == null || turnIn.isBlank() ? null : turnIn.trim(),
                objectiveNpcFiles,
                objectiveNpcName,
                objectiveLocation);
    }

    /**
     * The quest permissions this player holds, of the ones the
     * index names: EliteMobs' completion markers, read exactly
     * as EliteMobs' own accept check reads them. Null when the
     * read fails — the caller stays silent rather than guess
     * at a player's progress.
     */
    public Set<String> heldPerms(Player player, Set<String> perms) {
        Set<String> out = new HashSet<>();
        try {
            for (String perm : perms) {
                if (player.hasMetadata(perm)) out.add(perm);
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-chain-clarity: EM "
                    + "completion-marker read failed for "
                    + player.getName() + ": " + t);
            return null;
        }
        return out;
    }

    /**
     * The config filename of a quest object from one of
     * EliteMobs' quest events (0.18.0 signposting reads the
     * completed quest this way). Null for anything that is
     * not a custom quest — those completions are not chain
     * links this module can name.
     */
    public String questFile(Quest quest) {
        try {
            if (quest instanceof CustomQuest custom) {
                String file = custom.getConfigurationFilename();
                if (file != null && !file.isBlank()) return file;
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-chain-clarity: EM "
                    + "completed-quest read failed: " + t);
        }
        return null;
    }

    /** Whether EliteMobs has this player's data loaded — its
     *  own interaction handler refuses to answer before that,
     *  and markers read before it would be wrong. */
    public boolean playerDataReady(Player player) {
        try {
            return PlayerData.isInMemory(player);
        } catch (Throwable t) {
            return false;
        }
    }

    /** The player's carried custom quests (filenames). */
    public Set<String> activeQuestFiles(Player player) {
        Set<String> out = new HashSet<>();
        try {
            List<Quest> quests = PlayerData.getQuests(
                    player.getUniqueId());
            if (quests == null) return out;
            for (Quest q : quests) {
                if (q instanceof CustomQuest custom) {
                    String file = custom.getConfigurationFilename();
                    if (file != null && !file.isBlank()) out.add(file);
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-chain-clarity: EM "
                    + "active quest read failed for "
                    + player.getName() + ": " + t);
            return Set.of();
        }
        return out;
    }

    /**
     * The config filename a spawned NPC belongs to, resolved
     * from its config-fields object: the registry's own entry
     * (identity first — EliteMobs spawns NPCs from the
     * registry's fields objects — then a value match on name
     * + quest list, then a unique name match). Same resolution
     * as quest-giver-glow's bridge.
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
            log.warning("ExcelEMFixes quest-chain-clarity: EM "
                    + "NPC filename resolve failed: " + t);
        }
        return null;
    }

    /**
     * Every NPC EliteMobs currently has spawned: entity UUID
     * -> NPC config filename, harvested from EliteMobs'
     * persistent-object registry (for the NPCs already
     * standing when the module enabled — EliteMobs loads its
     * hub world at boot, so its NPCs spawn before any listener
     * can be registered). Same reflective, fully-guarded read
     * as quest-giver-glow's bridge: any shape change degrades
     * to an empty harvest and spawn events still identify the
     * rest.
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
                log.warning("ExcelEMFixes quest-chain-clarity: "
                        + "EM live NPC harvest unavailable (" + t
                        + ") — falling back to spawn events.");
            }
            return Map.of();
        }
        return out;
    }
}
