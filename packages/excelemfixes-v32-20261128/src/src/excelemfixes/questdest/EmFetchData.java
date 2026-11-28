package excelemfixes.questdest;

import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.config.customtreasurechests.CustomTreasureChestConfigFields;
import com.magmaguy.elitemobs.config.customtreasurechests.CustomTreasureChestsConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfig;
import com.magmaguy.elitemobs.config.npcs.NPCsConfigFields;
import com.magmaguy.elitemobs.config.wormholes.WormholeConfig;
import com.magmaguy.elitemobs.config.wormholes.WormholeConfigFields;
import com.magmaguy.elitemobs.items.customitems.CustomItem;
import com.magmaguy.elitemobs.items.customloottable.EliteCustomLootEntry;
import com.magmaguy.elitemobs.playerdata.database.PlayerData;
import com.magmaguy.elitemobs.quests.CustomQuest;
import com.magmaguy.elitemobs.quests.Quest;
import com.magmaguy.elitemobs.quests.objectives.ClassUnlockObjective;
import com.magmaguy.elitemobs.quests.objectives.CustomFetchObjective;
import com.magmaguy.elitemobs.quests.objectives.DialogObjective;
import com.magmaguy.elitemobs.quests.objectives.Objective;
import excelemfixes.questglow.QuestGlowCore;
import excelemfixes.shared.TextUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * The EliteMobs bridge for fetch-destination derivation (v26)
 * and CLASS_UNLOCK parity (FIX-LIST B6): the only derivation
 * class besides EmDialogData that touches EliteMobs types. It
 * answers the questions the pure FetchDerivation policy needs:
 *
 * 1. What is THIS player's copy of a quest asking for RIGHT
 *    NOW? The first incomplete objective, in objective order,
 *    classified by type — DIALOG and CLASS_UNLOCK carry their
 *    target NPC's config filename (DialogObjective /
 *    ClassUnlockObjective.getNpcFilename); FETCH_ITEM carries
 *    the item's customitems filename (CustomFetchObjective
 *    .getKey — EliteMobs' own parser stores the quest file's
 *    filename: field there). Keying on the CURRENT objective
 *    is what keeps a derivation from ever shadowing another
 *    phase's channel.
 * 2. Who drops that item? EliteMobs maintains the answer
 *    itself: CustomBossesConfig.bossesDropping(itemFile) is
 *    built from every boss's loaded loot table. Each dropper's
 *    spawns come from its own config fields.
 * 3. Which chests hold it? Every enabled treasure chest's
 *    loot list, scanned for the item filename (the same entry
 *    shapes the guidance census parses offline: bare
 *    "file.yml:chance" strings, filename= strings, maps with a
 *    filename key, and parsed loot-entry objects).
 * 4. How does a player ENTER an instanced world? A wormhole
 *    whose other endpoint sits in that world (its parent-world
 *    endpoint is the entry), or — when no wormhole names it —
 *    a teleporter NPC whose command runs EliteMobs' dungeon
 *    teleport into a dungeon whose world is that instance
 *    (dungeon -> world read from EliteMobs' own
 *    content_packages files in its data folder, the
 *    TrackerBlankScan data-folder pattern).
 *
 * Every read is guarded exactly like EmDialogData's: any
 * EliteMobs hiccup degrades to "no derivation" instead of an
 * error in a player's face. Nothing here parses or writes an
 * EliteMobs quest/boss file for content; everything comes
 * from EliteMobs' loaded registries and per-player quest
 * state (the content_packages scan only maps a dungeon file
 * to the world name EliteMobs itself declares for it).
 */
public final class EmFetchData {

    private EmFetchData() {}

    /** The kind of a quest's current incomplete objective. */
    public enum CurrentKind { DIALOG, FETCH_ITEM, CLASS_UNLOCK, OTHER, NONE }

    /**
     * The current incomplete objective of a carried quest:
     * its kind, plus the NPC filename (DIALOG / CLASS_UNLOCK)
     * or the item filename + display name (FETCH_ITEM).
     */
    public record CurrentObjective(CurrentKind kind, String npcFile,
                                   String itemFile, String itemName) {}

    private static final CurrentObjective NONE =
            new CurrentObjective(CurrentKind.NONE, null, null, null);

    /**
     * The first incomplete objective of the player's carried
     * quest with the given file, classified. NONE when the
     * player is not carrying the quest; OTHER when the current
     * objective is a type no derivation covers.
     */
    public static CurrentObjective currentObjective(UUID uuid,
                                                    String questFile,
                                                    Logger log) {
        try {
            List<Quest> quests = PlayerData.getQuests(uuid);
            if (quests == null) return NONE;
            for (Quest q : quests) {
                if (!(q instanceof CustomQuest custom)) continue;
                String file = custom.getConfigurationFilename();
                if (file == null || !file.equals(questFile)) continue;
                try {
                    if (custom.getQuestObjectives() == null
                            || custom.getQuestObjectives()
                                    .getObjectives() == null) {
                        return NONE;
                    }
                    for (Objective objective : custom
                            .getQuestObjectives().getObjectives()) {
                        if (objective.isObjectiveCompleted()) continue;
                        if (objective instanceof DialogObjective dialog) {
                            String npc = dialog.getNpcFilename();
                            return npc == null || npc.isBlank()
                                    ? NONE
                                    : new CurrentObjective(
                                            CurrentKind.DIALOG,
                                            npc.trim(), null, null);
                        }
                        if (objective instanceof ClassUnlockObjective unlock) {
                            String npc = unlock.getNpcFilename();
                            return npc == null || npc.isBlank()
                                    ? NONE
                                    : new CurrentObjective(
                                            CurrentKind.CLASS_UNLOCK,
                                            npc.trim(), null, null);
                        }
                        if (objective instanceof CustomFetchObjective fetch) {
                            String item = fetch.getKey();
                            if (item == null || item.isBlank()) return NONE;
                            item = item.trim();
                            return new CurrentObjective(
                                    CurrentKind.FETCH_ITEM, null,
                                    item, itemName(item));
                        }
                        return new CurrentObjective(
                                CurrentKind.OTHER, null, null, null);
                    }
                    return NONE;
                } catch (Throwable ignored) {
                    // An unreadable objective set contributes no
                    // derivation — never a guessed target.
                    return NONE;
                }
            }
            return NONE;
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM current "
                    + "objective read failed for " + uuid + ": " + t);
            return NONE;
        }
    }

    /** The item's display name from EliteMobs' item registry. */
    private static String itemName(String itemFile) {
        try {
            CustomItem item = CustomItem.getCustomItem(itemFile);
            if (item == null || item.getCustomItemsConfigFields() == null) {
                return null;
            }
            String name = TextUtil.cleanNameplate(
                    item.getCustomItemsConfigFields().getName() == null
                            ? "" : item.getCustomItemsConfigFields().getName());
            return name == null || name.isBlank() ? null : name;
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Every enabled boss whose loaded loot table contains the
     * item (EliteMobs' own bossesDropping index), with its
     * spawn points parsed by the glow module's anchor parser.
     */
    public static List<FetchDerivation.BossSource> droppersFor(
            String itemFile, Logger log) {
        List<FetchDerivation.BossSource> out = new ArrayList<>();
        try {
            Set<String> files = CustomBossesConfig.bossesDropping(itemFile);
            if (files == null) return out;
            for (String bossFile : files) {
                try {
                    CustomBossesConfigFields fields =
                            CustomBossesConfig.getCustomBoss(bossFile);
                    if (fields == null || !fields.isEnabled()) continue;
                    List<FetchDerivation.Point> spawns = new ArrayList<>();
                    if (fields.getSpawnLocations() != null) {
                        for (String raw : fields.getSpawnLocations()) {
                            QuestGlowCore.Anchor a =
                                    QuestGlowCore.parseAnchor(raw);
                            if (a != null) {
                                spawns.add(new FetchDerivation.Point(
                                        a.world(), a.x(), a.y(), a.z()));
                            }
                        }
                    }
                    String name = TextUtil.cleanNameplate(
                            fields.getName() == null ? "" : fields.getName());
                    out.add(new FetchDerivation.BossSource(
                            bossFile,
                            name == null || name.isBlank() ? bossFile : name,
                            spawns));
                } catch (Throwable ignored) {
                    // One unreadable boss never sinks the rest.
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM dropper "
                    + "read failed for " + itemFile + ": " + t);
        }
        return out;
    }

    /**
     * Every enabled treasure chest whose loot list contains
     * the item, with its first location.
     */
    public static List<FetchDerivation.ChestSource> chestsFor(
            String itemFile, Logger log) {
        List<FetchDerivation.ChestSource> out = new ArrayList<>();
        try {
            Map<String, CustomTreasureChestConfigFields> registry =
                    CustomTreasureChestsConfig
                            .getCustomTreasureChestConfigFields();
            if (registry == null) return out;
            for (Map.Entry<String, CustomTreasureChestConfigFields> e
                    : registry.entrySet()) {
                try {
                    CustomTreasureChestConfigFields fields = e.getValue();
                    if (fields == null || !fields.isEnabled()) continue;
                    if (!lootContains(fields.getLootList(), itemFile)) {
                        continue;
                    }
                    FetchDerivation.Point at = null;
                    if (fields.getLocationsString() != null) {
                        for (String raw : fields.getLocationsString()) {
                            QuestGlowCore.Anchor a =
                                    QuestGlowCore.parseAnchor(raw);
                            if (a != null) {
                                at = new FetchDerivation.Point(
                                        a.world(), a.x(), a.y(), a.z());
                                break;
                            }
                        }
                    }
                    if (at == null && fields.getLocation() != null
                            && fields.getLocation().getWorld() != null) {
                        at = new FetchDerivation.Point(
                                fields.getLocation().getWorld().getName(),
                                fields.getLocation().getX(),
                                fields.getLocation().getY(),
                                fields.getLocation().getZ());
                    }
                    if (at != null) {
                        out.add(new FetchDerivation.ChestSource(
                                e.getKey(), at));
                    }
                } catch (Throwable ignored) {
                    // One unreadable chest never sinks the rest.
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM chest "
                    + "read failed for " + itemFile + ": " + t);
        }
        return out;
    }

    /** Does a raw EliteMobs loot list name the item file? */
    static boolean lootContains(List<Object> loot, String itemFile) {
        if (loot == null) return false;
        for (Object entry : loot) {
            if (entry == null) continue;
            if (entry instanceof EliteCustomLootEntry elite) {
                if (itemFile.equals(elite.getFilename())) return true;
            } else if (entry instanceof Map<?, ?> map) {
                Object fn = map.get("filename");
                if (fn != null && itemFile.equals(fn.toString().trim())) {
                    return true;
                }
            } else {
                String s = entry.toString();
                if (s.startsWith(itemFile + ":")
                        || s.startsWith(itemFile + "=")
                        || s.contains("filename=" + itemFile)
                        || s.contains("filename: " + itemFile)
                        || s.equals(itemFile)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * The entry into an instanced world: a wormhole endpoint in
     * a non-instanced world whose other endpoint is in (or
     * names, by blueprint file) that world; else a teleporter
     * NPC whose dungeon-teleport command targets a dungeon in
     * that world. Null when neither exists — never guessed.
     */
    public static FetchDerivation.EntrySource entryFor(
            String instanceWorld, File emDataFolder, Logger log) {
        try {
            Map<String, WormholeConfigFields> wormholes =
                    WormholeConfig.getWormholes();
            if (wormholes != null) {
                List<String> files = new ArrayList<>(wormholes.keySet());
                files.sort(String::compareTo);
                for (String file : files) {
                    WormholeConfigFields fields = wormholes.get(file);
                    if (fields == null || !fields.isEnabled()) continue;
                    QuestGlowCore.Anchor a1 =
                            QuestGlowCore.parseAnchor(fields.getLocation1());
                    QuestGlowCore.Anchor a2 =
                            QuestGlowCore.parseAnchor(fields.getLocation2());
                    FetchDerivation.Point entry = entrySide(
                            a1, a2, instanceWorld,
                            fields.getLocation1(), fields.getLocation2());
                    if (entry == null) {
                        entry = entrySide(
                                a2, a1, instanceWorld,
                                fields.getLocation2(), fields.getLocation1());
                    }
                    if (entry != null) {
                        return new FetchDerivation.EntrySource(
                                "Instance wormhole", entry, false);
                    }
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM wormhole "
                    + "read failed: " + t);
        }
        // Teleporter NPC fallback: an NPC whose command runs
        // EliteMobs' dungeon teleport into a dungeon declared
        // for this world.
        try {
            Map<String, String> dungeonWorlds =
                    dungeonWorlds(emDataFolder);
            if (dungeonWorlds.isEmpty()) return null;
            Map<String, NPCsConfigFields> npcs = NPCsConfig.getNpcEntities();
            if (npcs == null) return null;
            List<String> files = new ArrayList<>(npcs.keySet());
            files.sort(String::compareTo);
            for (String file : files) {
                NPCsConfigFields fields = npcs.get(file);
                if (fields == null || !fields.isEnabled()) continue;
                String command = fields.getCommand();
                if (command == null) continue;
                for (Map.Entry<String, String> dw : dungeonWorlds.entrySet()) {
                    if (!dw.getValue().equals(instanceWorld)) continue;
                    if (!command.contains("dungeontp")
                            || !command.contains(dw.getKey())) continue;
                    DialogDerivation.NpcSpawn spawn =
                            EmDialogData.spawnFor(file, log);
                    if (spawn != null) {
                        String name = spawn.npcName() == null
                                || spawn.npcName().isBlank()
                                ? file : spawn.npcName();
                        return new FetchDerivation.EntrySource(
                                name,
                                new FetchDerivation.Point(
                                        spawn.world(), spawn.x(),
                                        spawn.y(), spawn.z()),
                                true);
                    }
                }
            }
        } catch (Throwable t) {
            log.warning("ExcelEMFixes quest-destinations: EM teleporter "
                    + "read failed: " + t);
        }
        return null;
    }

    /**
     * One wormhole side as the entry, when the OTHER side is in
     * (or blueprint-names) the instance world and THIS side is a
     * parseable point in a non-instanced world.
     */
    private static FetchDerivation.Point entrySide(
            QuestGlowCore.Anchor side, QuestGlowCore.Anchor other,
            String instanceWorld, String sideRaw, String otherRaw) {
        if (side == null || FetchDerivation.isInstanceWorld(side.world())) {
            return null;
        }
        boolean otherIsInstance = other != null
                && other.world().equals(instanceWorld);
        boolean otherNamesInstance = other == null && otherRaw != null
                && otherRaw.trim().endsWith(".yml")
                && ("em_id_" + otherRaw.trim()
                        .substring(0, otherRaw.trim().length() - 4))
                        .equals(instanceWorld);
        if (!otherIsInstance && !otherNamesInstance) return null;
        return new FetchDerivation.Point(
                side.world(), side.x(), side.y(), side.z());
    }

    /**
     * Dungeon config filename -> declared world name, read from
     * EliteMobs' own content_packages files (filename +
     * worldName keys only). Cached per data folder.
     */
    private static volatile File cachedFolder;
    private static volatile Map<String, String> cachedDungeons = Map.of();

    static synchronized Map<String, String> dungeonWorlds(File folder) {
        if (folder == null) return Map.of();
        if (folder.equals(cachedFolder)) return cachedDungeons;
        Map<String, String> out = new java.util.LinkedHashMap<>();
        File dir = new File(folder, "content_packages");
        File[] files = dir.isDirectory() ? dir.listFiles() : null;
        if (files != null) {
            for (File f : files) {
                if (!f.getName().endsWith(".yml")) continue;
                try (InputStream in = new FileInputStream(f)) {
                    Object root = new Yaml().load(new InputStreamReader(
                            in, StandardCharsets.UTF_8));
                    if (root instanceof Map<?, ?> map
                            && map.get("worldName") != null) {
                        out.put(f.getName(),
                                map.get("worldName").toString().trim());
                    }
                } catch (Throwable ignored) {
                    // An unreadable package file names no dungeon.
                }
            }
        }
        cachedFolder = folder;
        cachedDungeons = Map.copyOf(out);
        return cachedDungeons;
    }
}
