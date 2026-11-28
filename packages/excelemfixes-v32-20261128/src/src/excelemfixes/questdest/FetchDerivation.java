package excelemfixes.questdest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The fetch-destination derivation policy (v26): how a quest's
 * destination is chosen when its CURRENT incomplete objective
 * is FETCH_ITEM and EliteMobs' own data can say where the item
 * comes from. Pure data + decision: no Bukkit, no EliteMobs
 * types, so the plain-JVM suites exercise every branch
 * directly. The EliteMobs reads that feed it live in
 * EmFetchData; the consumers (the quest beacon, /questwhere)
 * read the result through the quest-destination API.
 *
 * WHY THIS EXISTS (quest-guidance re-audit, Nov 26, 2026):
 * the Nov 26 re-audit counted 36 FETCH_ITEM steps across the
 * live content: 14 were GUIDED by nothing at all — EliteMobs'
 * tracker derives only from kill targets, so a fetch step is
 * silent, and the destinations table holds one hand-written
 * row per quest, which cannot follow a quest across phases.
 * The Nov 26 FIX-LIST's B1 is the durable fix: derive the
 * fetch destination from the objective item's source, exactly
 * as the audit's census resolves it offline. The hand-written
 * fetch rows filed alongside it are the stopgap; this
 * derivation covers future content the day it lands.
 *
 * THE RULES, in order:
 * 1. TABLE ROWS KEEP PRECEDENCE. A hand-written destinations
 *    table row stands whenever one exists (the module checks
 *    the table before ever consulting this policy).
 * 2. CURRENT OBJECTIVE ONLY. The module feeds this policy
 *    only when the quest's current incomplete objective is
 *    FETCH_ITEM, so a table row and a derivation can never
 *    shadow each other across phases: when the fetch step
 *    completes, the next objective's own channel takes over.
 * 3. SOURCE PRECEDENCE, mirroring the census: (i) a boss
 *    whose loot contains the item and which spawns in a
 *    non-instanced world -> the centroid of that boss's
 *    spawns in that world; (ii) droppers that spawn ONLY
 *    inside instanced (em_id_*) worlds -> the instance entry
 *    (a wormhole endpoint in the parent world, or the
 *    teleporter NPC's spawn); (iii) a treasure chest whose
 *    loot list contains the item -> the chest location.
 * 4. DETERMINISM. When several sources qualify, the winner
 *    is chosen by sorted filename, never by registry order,
 *    so every player sees the same destination.
 * 5. NOTHING OTHERWISE. No source in EliteMobs' data: no
 *    destination — a missed destination is always preferable
 *    to a guessed one.
 *
 * Everything is derived from EliteMobs' own boss/chest/
 * wormhole/NPC data at runtime: no quest, item, boss or
 * place is named in code, so any server whose content uses
 * fetch objectives gets the same coverage.
 */
public final class FetchDerivation {

    private FetchDerivation() {}

    /** A world + coordinates point (a spawn, a chest, an entry). */
    public record Point(String world, double x, double y, double z) {}

    /** One boss that drops the item, with its spawn points. */
    public record BossSource(String bossFile, String bossName,
                             List<Point> spawns) {
        public BossSource {
            spawns = spawns == null ? List.of() : List.copyOf(spawns);
        }
    }

    /** One treasure chest whose loot list contains the item. */
    public record ChestSource(String chestFile, Point location) {}

    /**
     * The entry into an instanced world: a wormhole endpoint in
     * the parent world, or the teleporter NPC that sends
     * players there (its spawn, in the parent world).
     */
    public record EntrySource(String label, Point location,
                              boolean npcEntity) {}

    /** Where a resolved fetch destination came from. */
    public enum Kind { BOSS, INSTANCE, CHEST, NONE }

    /** The resolution outcome: kind + destination (null iff NONE). */
    public record Resolution(Kind kind,
                             QuestDestinations.Destination destination) {}

    /** True for EliteMobs' per-player instance worlds. */
    public static boolean isInstanceWorld(String world) {
        return world != null && world.startsWith("em_id_");
    }

    /**
     * Choose a fetch destination.
     *
     * @param questFile  the quest's configuration filename
     * @param questName  the quest's display name (may be null)
     * @param itemFile   the current fetch objective's item
     *                   filename (EliteMobs customitems file)
     * @param itemName   the item's display name (may be null —
     *                   the file stands in)
     * @param droppers   every enabled boss whose loot contains
     *                   the item, with its spawn points
     * @param chests     every chest whose loot contains the item
     * @param entryFor   resolver from an instanced world name to
     *                   its entry point, or null when unknown
     */
    public static Resolution resolve(
            String questFile, String questName,
            String itemFile, String itemName,
            List<BossSource> droppers, List<ChestSource> chests,
            java.util.function.Function<String, EntrySource> entryFor) {
        String item = itemName == null || itemName.isBlank()
                ? itemFile : itemName;
        String quest = questName == null || questName.isBlank()
                ? questFile : questName;

        List<BossSource> bosses = new ArrayList<>(
                droppers == null ? List.of() : droppers);
        bosses.sort(Comparator.comparing(BossSource::bossFile));

        // (i) An open-world dropper: centroid of its spawns in
        // the first (sorted) non-instanced world it spawns in.
        for (BossSource boss : bosses) {
            List<String> worlds = new ArrayList<>();
            for (Point p : boss.spawns()) {
                if (!isInstanceWorld(p.world())
                        && !worlds.contains(p.world())) {
                    worlds.add(p.world());
                }
            }
            worlds.sort(String::compareTo);
            for (String world : worlds) {
                Point c = centroid(boss.spawns(), world);
                if (c != null) {
                    String mob = boss.bossName() == null
                            || boss.bossName().isBlank()
                            ? boss.bossFile() : boss.bossName();
                    return new Resolution(Kind.BOSS, new QuestDestinations
                            .Destination(
                            questFile, quest, item, mob,
                            world, world, c.x(), c.y(), c.z(),
                            item, "CHEST",
                            List.of("This quest needs " + item
                                    + ", which drops from " + mob
                                    + ".",
                                    "Head to the marked spot — that is"
                                    + " where " + mob + " appears."),
                            "marker", null));
                }
            }
        }

        // (ii) Instance-only droppers: the instance entry.
        if (entryFor != null) {
            Set<String> instanceWorlds = new LinkedHashSet<>();
            for (BossSource boss : bosses) {
                for (Point p : boss.spawns()) {
                    if (isInstanceWorld(p.world())) {
                        instanceWorlds.add(p.world());
                    }
                }
            }
            List<String> worlds = new ArrayList<>(instanceWorlds);
            worlds.sort(String::compareTo);
            for (String world : worlds) {
                EntrySource entry = entryFor.apply(world);
                if (entry != null && entry.location() != null) {
                    Point at = entry.location();
                    return new Resolution(Kind.INSTANCE,
                            new QuestDestinations.Destination(
                            questFile, quest, item, entry.label(),
                            at.world(), at.world(),
                            at.x(), at.y(), at.z(),
                            entry.label(), "ENDER_PEARL",
                            List.of("This quest needs " + item
                                    + ", which is found inside an"
                                    + " instance — that is why the"
                                    + " tracker shows nothing.",
                                    "Enter at the marked spot and"
                                    + " look for " + item + " inside."),
                            entry.npcEntity() ? "entity" : "marker",
                            entry.npcEntity() ? entry.label() : null));
                }
            }
        }

        // (iii) A treasure chest whose loot contains the item.
        List<ChestSource> chestList = new ArrayList<>(
                chests == null ? List.of() : chests);
        chestList.sort(Comparator.comparing(ChestSource::chestFile));
        for (ChestSource chest : chestList) {
            if (chest.location() == null) continue;
            Point at = chest.location();
            return new Resolution(Kind.CHEST, new QuestDestinations
                    .Destination(
                    questFile, quest, item, "Treasure chest",
                    at.world(), at.world(), at.x(), at.y(), at.z(),
                    item, "CHEST",
                    List.of("This quest needs " + item + ", which is"
                            + " in the treasure chest at the marked"
                            + " spot.",
                            "Head to the coordinates and open the"
                            + " chest."),
                    "marker", null));
        }

        return new Resolution(Kind.NONE, null);
    }

    /** Mean of the points in one world, or null when none. */
    public static Point centroid(List<Point> points, String world) {
        double sx = 0, sy = 0, sz = 0;
        int n = 0;
        for (Point p : points) {
            if (!p.world().equals(world)) continue;
            sx += p.x(); sy += p.y(); sz += p.z(); n++;
        }
        if (n == 0) return null;
        return new Point(world, sx / n, sy / n, sz / n);
    }
}
