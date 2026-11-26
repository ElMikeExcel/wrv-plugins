package excelemfixes.questdest;

import org.yaml.snakeyaml.Yaml;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The quest -> destination table behind the quest-destinations
 * fix, loaded from quest-destinations.yml (bundled copy, or the
 * data-folder override). Pure data + parsing: no Bukkit, no
 * EliteMobs types, so the plain-JVM suites exercise it directly.
 *
 * WHY THIS TABLE EXISTS: EliteMobs' quest tracker builds a
 * destination from the target boss's spawn locations. Bosses that
 * live in instanced dungeons (Counselor's Quarters, Trial of Ice,
 * Ice Elemental Lair, ...) spawn only inside per-player instance
 * worlds (em_id_*), so the tracker has no presentable destination
 * and shows nothing (upstream EliteMobs issue #241). The player
 * still needs to know where to go: the entry wormhole in the
 * parent world. That is what each row records.
 *
 * ADDING A ROW: any quest whose kill targets spawn only inside an
 * instanced (em_id_*) world belongs here. quest-file is the
 * customquests filename; x/y/z are the entry wormhole's location1
 * in the parent world (from its wormholes/ file). No code changes.
 */
public final class QuestDestinations {

    /** One row of the table. */
    public record Destination(String questFile, String questName,
                              String target, String place,
                              String world, String worldLabel,
                              double x, double y, double z,
                              String entry, String icon,
                              List<String> directions,
                              String glowMode, String glowEntityName) {
        public Destination {
            directions = directions == null ? List.of()
                    : List.copyOf(directions);
            icon = icon == null || icon.isBlank() ? "COMPASS" : icon;
            worldLabel = worldLabel == null || worldLabel.isBlank()
                    ? world : worldLabel;
            glowMode = glowMode == null || glowMode.isBlank()
                    ? "marker" : glowMode.strip().toLowerCase();
            if (!glowMode.equals("marker") && !glowMode.equals("entity")) {
                throw new IllegalArgumentException(
                        "quest-destinations.yml: glow-mode '" + glowMode
                                + "' for " + questFile
                                + " must be 'marker' or 'entity'");
            }
            if (glowMode.equals("entity")
                    && (glowEntityName == null || glowEntityName.isBlank())) {
                throw new IllegalArgumentException(
                        "quest-destinations.yml: glow-mode 'entity' for "
                                + questFile + " needs glow-entity-name");
            }
        }

        /** True when guidance should light a named entity itself. */
        public boolean glowIsEntity() {
            return glowMode.equals("entity");
        }

        /** Player-facing coordinates, e.g. "512.5, 42, 724.5". */
        public String coords() {
            return formatCoord(x) + ", " + formatCoord(y) + ", "
                    + formatCoord(z);
        }
    }

    /** A player's active quest, as read from EliteMobs data. */
    public record ActiveQuest(String file, String name) {}

    private final List<Destination> all;
    private final Map<String, Destination> byFile;
    private final java.util.Set<String> ignoredFiles;

    private QuestDestinations(List<Destination> all,
                              java.util.Set<String> ignoredFiles) {
        this.all = List.copyOf(all);
        this.byFile = new LinkedHashMap<>();
        for (Destination d : all) {
            byFile.put(d.questFile(), d);
        }
        this.ignoredFiles = java.util.Set.copyOf(ignoredFiles);
    }

    public List<Destination> all() {
        return all;
    }

    /**
     * Files the coverage scan should stay quiet about (the data
     * file's "ignored" list — e.g. EliteMobs' own test quest).
     */
    public java.util.Set<String> ignoredFiles() {
        return ignoredFiles;
    }

    public Destination byFile(String questFile) {
        return byFile.get(questFile);
    }

    public boolean covers(String questFile) {
        return byFile.containsKey(questFile);
    }

    /**
     * The destinations for the given active quest files, in table
     * order (not player-quest order) so menus are stable.
     */
    public List<Destination> forActive(List<String> activeFiles) {
        List<Destination> out = new ArrayList<>();
        for (Destination d : all) {
            if (activeFiles.contains(d.questFile())) out.add(d);
        }
        return out;
    }

    /** 512.0 -> "512", 512.5 -> "512.5", 975.41 -> "975.4". */
    public static String formatCoord(double v) {
        if (v == Math.rint(v)) return Long.toString(Math.round(v));
        return Double.toString(Math.round(v * 10.0) / 10.0);
    }

    // ------------------------------------------------------------ loading

    public static QuestDestinations load(Reader reader) {
        Object root = new Yaml().load(reader);
        if (!(root instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(
                    "quest-destinations.yml: root is not a map");
        }
        Object rawList = map.get("destinations");
        if (!(rawList instanceof List<?> list)) {
            throw new IllegalArgumentException(
                    "quest-destinations.yml: 'destinations' list missing");
        }
        List<Destination> out = new ArrayList<>();
        for (Object raw : list) {
            if (!(raw instanceof Map<?, ?> row)) {
                throw new IllegalArgumentException(
                        "quest-destinations.yml: row is not a map");
            }
            out.add(parseRow(row));
        }
        // An empty list is valid: the shipped template is blank,
        // and the coverage scan tells the owner which of their
        // quests need rows.
        java.util.Set<String> ignored = new java.util.LinkedHashSet<>();
        Object rawIgnored = map.get("ignored");
        if (rawIgnored instanceof List<?> igList) {
            for (Object v : igList) {
                if (v != null && !v.toString().isBlank()) {
                    ignored.add(v.toString().strip());
                }
            }
        }
        return new QuestDestinations(out, ignored);
    }

    private static Destination parseRow(Map<?, ?> row) {
        String questFile = reqString(row, "quest-file");
        return new Destination(
                questFile,
                reqString(row, "quest-name"),
                reqString(row, "target"),
                reqString(row, "place"),
                reqString(row, "world"),
                optString(row, "world-label"),
                reqDouble(row, "x", questFile),
                reqDouble(row, "y", questFile),
                reqDouble(row, "z", questFile),
                reqString(row, "entry"),
                optString(row, "icon"),
                stringList(row.get("directions"), questFile),
                optString(row, "glow-mode"),
                optString(row, "glow-entity-name"));
    }

    private static String reqString(Map<?, ?> row, String key) {
        Object v = row.get(key);
        if (v == null || v.toString().isBlank()) {
            throw new IllegalArgumentException(
                    "quest-destinations.yml: missing '" + key + "'");
        }
        return v.toString().strip();
    }

    private static String optString(Map<?, ?> row, String key) {
        Object v = row.get(key);
        return v == null ? null : v.toString().strip();
    }

    private static double reqDouble(Map<?, ?> row, String key,
                                    String questFile) {
        Object v = row.get(key);
        if (v instanceof Number n) return n.doubleValue();
        throw new IllegalArgumentException(
                "quest-destinations.yml: '" + key + "' for "
                        + questFile + " is not a number");
    }

    private static List<String> stringList(Object raw, String questFile) {
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new IllegalArgumentException(
                    "quest-destinations.yml: 'directions' for "
                            + questFile + " missing or empty");
        }
        List<String> out = new ArrayList<>();
        for (Object o : list) {
            if (o != null && !o.toString().isBlank()) {
                out.add(o.toString().strip());
            }
        }
        if (out.isEmpty()) {
            throw new IllegalArgumentException(
                    "quest-destinations.yml: 'directions' for "
                            + questFile + " has no text");
        }
        return out;
    }
}
