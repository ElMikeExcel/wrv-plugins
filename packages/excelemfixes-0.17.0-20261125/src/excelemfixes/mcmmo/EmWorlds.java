package excelemfixes.mcmmo;

import java.util.List;
import java.util.Locale;

/**
 * Recognises EliteMobs worlds by name, for the EM-world gain
 * ceiling. The rules are the retired mcMMO fork census (the
 * Nov 15, 2026 world-gate recognition, verified then against
 * every live content package's worldName, the boot log's
 * world-load lines, and ExcelProtect's player streams),
 * carried into the module unchanged in behaviour:
 *
 * <ul>
 *   <li>Every EliteMobs world name starts with {@code em_} —
 *       the guild hub ({@code em_adventurers_guild}), Primis
 *       ({@code em_primis}), the regional worlds, and the
 *       instanced-dungeon bases ({@code em_id_*}). An instanced
 *       copy appends {@code _<instance number>} to the base
 *       name, so the prefix rule covers instances with no
 *       extra work.</li>
 *   <li>The one exception is the Goblin Kingdom world,
 *       {@code GoblinKingdom_vf}, matched by exact name plus
 *       its own would-be instance copies.</li>
 *   <li>Anything beyond those built-in rules is config, never
 *       code: the module config's extra names / prefixes, so a
 *       future pack with an off-pattern world name is a config
 *       edit, not a rebuild.</li>
 * </ul>
 *
 * <p>Fail posture: a name that is NOT positively matched is
 * not an EliteMobs world, and gains there are never touched by
 * the EM-world ceiling. The overworld can therefore never be
 * caught by accident — an unrecognised name simply leaves
 * gains alone.
 *
 * <p>Pure logic (no Bukkit): names in, verdict out.
 */
public final class EmWorlds {

    /** Every EliteMobs base world and instanced copy carries this prefix. */
    private static final String ELITE_MOBS_WORLD_PREFIX = "em_";

    /** The one live EliteMobs world with no {@code em_} prefix (lowercased). */
    private static final String GOBLIN_KINGDOM_WORLD_NAME = "goblinkingdom_vf";

    private EmWorlds() {
    }

    /**
     * Whether a world name belongs to an EliteMobs world under
     * the built-in rules alone.
     *
     * @param worldName the Bukkit world name, may be null
     * @return true only on a positive match
     */
    public static boolean isEmWorld(String worldName) {
        return isEmWorld(worldName, List.of(), List.of());
    }

    /**
     * Whether a world name belongs to an EliteMobs world: the
     * built-in rules, or the configured extra names/prefixes.
     *
     * @param worldName the Bukkit world name, may be null
     * @param extraNames exact world names from config (any case)
     * @param extraPrefixes world-name prefixes from config (any case)
     * @return true only on a positive match; null/empty never matches
     */
    public static boolean isEmWorld(String worldName,
                                    List<String> extraNames,
                                    List<String> extraPrefixes) {
        if (worldName == null || worldName.isEmpty()) return false;
        String lower = worldName.toLowerCase(Locale.ROOT);

        if (lower.startsWith(ELITE_MOBS_WORLD_PREFIX)) return true;

        // The Goblin Kingdom exception, plus its instanced
        // copies, which append "_<instance number>" to the
        // base name exactly as the em_ instances do.
        if (lower.equals(GOBLIN_KINGDOM_WORLD_NAME)
                || lower.startsWith(GOBLIN_KINGDOM_WORLD_NAME + "_")) {
            return true;
        }

        if (extraNames != null) {
            for (String name : extraNames) {
                if (name != null && lower.equals(
                        name.toLowerCase(Locale.ROOT))) return true;
            }
        }
        if (extraPrefixes != null) {
            for (String prefix : extraPrefixes) {
                if (prefix != null && !prefix.isEmpty() && lower.startsWith(
                        prefix.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }
}
