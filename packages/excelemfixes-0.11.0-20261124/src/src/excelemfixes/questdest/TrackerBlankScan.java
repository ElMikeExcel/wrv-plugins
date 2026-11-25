package excelemfixes.questdest;

import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Expansion-generic coverage scan. Reads EliteMobs' OWN content
 * data — every quest file under customquests/ and every boss file
 * under custombosses/, in every content package, present and
 * future — and works out which quests belong to the tracker-blank
 * defect class: the quest has KILL_CUSTOM objectives, and every
 * target boss spawns only inside an instanced world (EM instance
 * worlds are named em_id_*) or has no natural spawn locations at
 * all (summons), so EliteMobs' tracker can never show a
 * destination for it (upstream issue #241).
 *
 * Nothing here knows any pack's name, any quest number, or any
 * place: a future ExcelExpansions pack is scanned the day it
 * drops in, and any tracker-blank quest the destination table
 * does not cover yet is reported by filename so a data row can
 * be added — no code change, ever.
 */
public final class TrackerBlankScan {

    private TrackerBlankScan() {}

    /** EM's instanced-dungeon world naming convention. */
    public static boolean isInstancedWorld(String worldName) {
        return worldName != null && worldName.startsWith("em_id_");
    }

    // ------------------------------------------------------------ pure rule

    /**
     * @param questKillBosses quest file -> boss files named by its
     *                        KILL_CUSTOM objectives
     * @param bossSpawnWorlds boss file -> world names parsed from
     *                        its spawnLocations (empty = none)
     * @return the quest files in the tracker-blank defect class
     */
    public static Set<String> trackerBlank(
            Map<String, Set<String>> questKillBosses,
            Map<String, List<String>> bossSpawnWorlds) {
        Set<String> blank = new TreeSet<>();
        for (Map.Entry<String, Set<String>> e : questKillBosses.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            boolean allBlank = true;
            for (String boss : e.getValue()) {
                List<String> worlds = bossSpawnWorlds.get(boss);
                if (worlds == null) worlds = List.of();
                boolean bossBlank = worlds.stream()
                        .allMatch(TrackerBlankScan::isInstancedWorld);
                // Note: an empty spawn list also counts as blank —
                // the tracker has nothing to show for it either.
                if (!bossBlank) {
                    allBlank = false;
                    break;
                }
            }
            if (allBlank) blank.add(e.getKey());
        }
        return blank;
    }

    /** World name out of an EM spawn-location string ("w,x,y,z,..."). */
    public static String spawnWorld(String spawnLocation) {
        if (spawnLocation == null) return "";
        int cut = spawnLocation.length();
        for (char sep : new char[]{',', ';', ':'}) {
            int i = spawnLocation.indexOf(sep);
            if (i >= 0 && i < cut) cut = i;
        }
        return spawnLocation.substring(0, cut).strip();
    }

    // ------------------------------------------------------------ data walk

    /** Result of scanning an EliteMobs data folder. */
    public record Scan(Set<String> trackerBlankQuests,
                       Set<String> uncoveredQuests,
                       int questsScanned, int bossesScanned) {}

    /**
     * Walks an EliteMobs plugin folder (customquests/ +
     * custombosses/, recursively through every pack subfolder)
     * and diffs the defect class against the covered table.
     * ignoredFiles (the data file's "ignored" list — files that
     * are not real player content, like EM's own test quest) are
     * excluded from the uncovered report.
     */
    public static Scan scan(File eliteMobsFolder, Set<String> coveredFiles,
                            Set<String> ignoredFiles) {
        Map<String, Set<String>> questKillBosses = new HashMap<>();
        Map<String, List<String>> bossSpawnWorlds = new HashMap<>();
        Yaml yaml = new Yaml();

        for (File questFile : ymlFiles(new File(eliteMobsFolder, "customquests"))) {
            Map<String, Object> root = load(yaml, questFile);
            if (root == null) continue;
            Set<String> bosses = new LinkedHashSet<>();
            Object objectives = root.get("customObjectives");
            if (objectives instanceof Map<?, ?> objMap) {
                for (Object value : objMap.values()) {
                    if (!(value instanceof Map<?, ?> obj)) continue;
                    Object type = obj.get("objectiveType");
                    Object filename = obj.get("filename");
                    if (type != null && filename != null
                            && type.toString().equalsIgnoreCase("KILL_CUSTOM")) {
                        bosses.add(filename.toString());
                    }
                }
            }
            questKillBosses.put(questFile.getName(), bosses);
        }

        for (File bossFile : ymlFiles(new File(eliteMobsFolder, "custombosses"))) {
            Map<String, Object> root = load(yaml, bossFile);
            if (root == null) continue;
            List<String> worlds = new ArrayList<>();
            Object spawns = root.get("spawnLocations");
            if (spawns instanceof List<?> list) {
                for (Object s : list) {
                    if (s != null) worlds.add(spawnWorld(s.toString()));
                }
            }
            bossSpawnWorlds.put(bossFile.getName(), worlds);
        }

        Set<String> blank = trackerBlank(questKillBosses, bossSpawnWorlds);
        Set<String> uncovered = new TreeSet<>(blank);
        uncovered.removeAll(coveredFiles);
        uncovered.removeAll(ignoredFiles);
        return new Scan(blank, uncovered,
                questKillBosses.size(), bossSpawnWorlds.size());
    }

    private static List<File> ymlFiles(File dir) {
        List<File> out = new ArrayList<>();
        collect(dir, out);
        return out;
    }

    private static void collect(File dir, List<File> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                collect(f, out);
            } else if (f.getName().endsWith(".yml")) {
                out.add(f);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> load(Yaml yaml, File file) {
        try (InputStream in = new FileInputStream(file)) {
            Object parsed = yaml.load(in);
            return parsed instanceof Map ? (Map<String, Object>) parsed : null;
        } catch (Exception e) {
            return null;
        }
    }
}
