package excelemfixes.dropguide;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Runtime resolver from a Drop Guide mob to the place a player
 * physically goes to fight it — derived entirely from
 * EliteMobs' own data files at scan time, so any content pack
 * (present or future) is covered with no code changes:
 *
 * <ol>
 *   <li>a boss that spawns in a normal world: its spawn point;
 *   <li>a boss that only spawns in an instanced world
 *       ({@code em_id_*}): the wormhole whose {@code location2}
 *       names the content package that owns that world — the
 *       entry end ({@code location1}) is the destination;
 *   <li>when no wormhole leads to the package: the teleporter
 *       NPC whose command is {@code em dungeontp <package>} —
 *       the NPC itself is the destination (entity glow).
 * </ol>
 *
 * Keys are Drop Guide mob ids (the slug of the boss filename
 * stem, exactly as the index generator computes them).
 */
public final class EntryPoints {

    /** A resolved destination. entityName null = marker glow. */
    public record Entry(String world, double x, double y, double z,
                        String entityName, String label) {
        public boolean entityGlow() {
            return entityName != null && !entityName.isBlank();
        }

        public String coords() {
            return trim(x) + ", " + trim(y) + ", " + trim(z);
        }

        private static String trim(double v) {
            return v == Math.rint(v) ? Long.toString(Math.round(v)) : Double.toString(v);
        }
    }

    private final Map<String, Entry> byMobId;

    private EntryPoints(Map<String, Entry> byMobId) {
        this.byMobId = byMobId;
    }

    public static EntryPoints empty() {
        return new EntryPoints(Map.of());
    }

    public Entry entryForMob(String mobId) {
        return mobId == null ? null : byMobId.get(mobId);
    }

    public boolean hasEntry(String mobId) {
        return entryForMob(mobId) != null;
    }

    public int size() {
        return byMobId.size();
    }

    /** Drop Guide mob id for a boss filename (generator parity). */
    public static String mobIdForFile(String filename) {
        String stem = filename;
        if (stem.toLowerCase().endsWith(".yml")) stem = stem.substring(0, stem.length() - 4);
        return slug(stem);
    }

    /** The index generator's slug: lowercase, non [a-z0-9] runs to "_". */
    public static String slug(String s) {
        String out = s.toLowerCase().replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return out.isEmpty() ? "x" : out;
    }

    /** Strip EM/MiniMessage markup from a display name. */
    public static String cleanName(String raw) {
        if (raw == null) return "";
        String s = raw.replaceAll("<[^>]*>", "")
                .replaceAll("\\$[A-Za-z]+", "")
                .replaceAll("&[0-9a-fk-orx]", "")
                .replaceAll("§.", "")
                .replaceAll("\\s+", " ")
                .trim();
        return s;
    }

    // ------------------------------------------------------------ scan

    public static EntryPoints scan(File emDataFolder, Logger logger) {
        Map<String, Entry> out = new HashMap<>();
        if (emDataFolder == null || !emDataFolder.isDirectory()) return new EntryPoints(out);

        // Content packages: worldName <-> package filename.
        Map<String, String> packageByWorld = new HashMap<>();
        for (File f : ymlFiles(new File(emDataFolder, "content_packages"), false)) {
            YamlConfiguration y = load(f);
            if (y == null) continue;
            String world = y.getString("worldName");
            if (world != null && !world.isBlank()) packageByWorld.put(world, f.getName());
        }

        // Wormholes: package filename -> entry point (location1).
        Map<String, Entry> wormholeByPackage = new HashMap<>();
        for (File f : ymlFiles(new File(emDataFolder, "wormholes"), true)) {
            YamlConfiguration y = load(f);
            if (y == null) continue;
            String pkg = y.getString("location2");
            double[] loc = parseLoc(y.getString("location1"));
            String world = worldOf(y.getString("location1"));
            if (pkg == null || loc == null || world == null) continue;
            wormholeByPackage.putIfAbsent(pkg, new Entry(world, loc[0], loc[1], loc[2],
                    null, "wormhole"));
        }

        // Teleporter NPCs: package filename -> the NPC itself.
        Map<String, Entry> npcByPackage = new HashMap<>();
        for (File f : ymlFiles(new File(emDataFolder, "npcs"), true)) {
            YamlConfiguration y = load(f);
            if (y == null) continue;
            String command = y.getString("command");
            if (command == null || !command.strip().startsWith("em dungeontp")) continue;
            String[] parts = command.strip().split("\\s+");
            if (parts.length < 3) continue;
            String pkg = parts[2];
            double[] loc = parseLoc(y.getString("spawnLocation"));
            String world = worldOf(y.getString("spawnLocation"));
            if (loc == null || world == null) continue;
            String name = cleanName(y.getString("name"));
            npcByPackage.putIfAbsent(pkg, new Entry(world, loc[0], loc[1], loc[2],
                    name.isBlank() ? null : name, name.isBlank() ? "teleporter" : name));
        }

        // Bosses: resolve each file's entry.
        for (File f : ymlFiles(new File(emDataFolder, "custombosses"), true)) {
            YamlConfiguration y = load(f);
            if (y == null) continue;
            List<String> spawns = y.getStringList("spawnLocations");
            if (spawns.isEmpty() && y.getString("spawnLocation") != null) {
                spawns = List.of(y.getString("spawnLocation"));
            }
            Entry entry = null;
            String bossName = cleanName(y.getString("name"));
            for (String spawn : spawns) {
                String world = worldOf(spawn);
                double[] loc = parseLoc(spawn);
                if (world == null || loc == null) continue;
                if (!world.startsWith("em_id_")) {
                    entry = new Entry(world, loc[0], loc[1], loc[2], null,
                            bossName.isBlank() ? "boss spawn" : bossName);
                    break;
                }
            }
            if (entry == null) {
                for (String spawn : spawns) {
                    String world = worldOf(spawn);
                    if (world == null) continue;
                    String pkg = packageByWorld.get(world);
                    if (pkg == null) continue;
                    entry = wormholeByPackage.get(pkg);
                    if (entry == null) entry = npcByPackage.get(pkg);
                    if (entry != null) break;
                }
            }
            if (entry != null) out.putIfAbsent(mobIdForFile(f.getName()), entry);
        }
        if (logger != null) {
            logger.info("Drop Guide glow: resolved entry points for " + out.size() + " mob(s).");
        }
        return new EntryPoints(out);
    }

    private static YamlConfiguration load(File f) {
        try {
            return YamlConfiguration.loadConfiguration(f);
        } catch (Throwable t) {
            return null;
        }
    }

    private static List<File> ymlFiles(File dir, boolean recursive) {
        List<File> out = new ArrayList<>();
        collect(dir, recursive, out);
        out.sort(Comparator.comparing(f -> f.getPath().toLowerCase()));
        return out;
    }

    private static void collect(File dir, boolean recursive, List<File> out) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                if (recursive) collect(f, true, out);
            } else if (f.getName().toLowerCase().endsWith(".yml")
                    && !f.getName().contains(".bak")) {
                out.add(f);
            }
        }
    }

    /** "world,x,y,z,..." -> world part, or null. */
    private static String worldOf(String loc) {
        if (loc == null) return null;
        int comma = loc.indexOf(',');
        return comma < 0 ? null : loc.substring(0, comma).trim();
    }

    /** "world,x,y,z,..." -> {x,y,z}, or null. */
    private static double[] parseLoc(String loc) {
        if (loc == null) return null;
        String[] parts = loc.split(",");
        if (parts.length < 4) return null;
        try {
            return new double[]{Double.parseDouble(parts[1].trim()),
                    Double.parseDouble(parts[2].trim()),
                    Double.parseDouble(parts[3].trim())};
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
