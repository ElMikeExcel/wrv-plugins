package excelemfixes.api;

import excelemfixes.questdest.GlowGuide;
import org.bukkit.entity.Player;

/**
 * The glow-guidance surface for other plugins — designed so a
 * companion-style menu plugin can glow quest
 * targets (usually via /questwhere guide, which also prints the
 * directions), and its drop guide glows a boss/NPC or a place
 * through this facade. Expansion-agnostic: callers pass world,
 * coordinates and (for entities) the display name — no pack,
 * quest or place is known to this API.
 *
 * Everything no-ops cleanly (Result.UNAVAILABLE) when
 * ExcelEMFixes is absent, its quest-destinations module is off,
 * or glow guidance is disabled in its config — callers never
 * need to check for the plugin first.
 */
public final class GlowApi {

    public enum Result { ENTITY, MARKER, UNAVAILABLE }

    private static volatile GlowGuide delegate;

    private GlowApi() {}

    /** Called by the quest-destinations module as it enables. */
    public static void register(GlowGuide guide) {
        delegate = guide;
    }

    /** Called by the quest-destinations module as it disables. */
    public static void unregister(GlowGuide guide) {
        if (delegate == guide) delegate = null;
    }

    public static boolean isAvailable() {
        GlowGuide g = delegate;
        return g != null && g.enabled();
    }

    /** Lights a temporary marker at a place for the player. */
    public static Result glowMarker(Player player, String world,
                                    double x, double y, double z,
                                    String label) {
        GlowGuide g = delegate;
        if (g == null || !g.enabled()) return Result.UNAVAILABLE;
        return g.glowMarkerAt(player, world, x, y, z, label)
                ? Result.MARKER : Result.UNAVAILABLE;
    }

    /**
     * Lights the named entity near a point for the player; when
     * the entity is not there, lights a marker at the point
     * instead (Result.MARKER says which happened).
     */
    public static Result glowEntity(Player player, String world,
                                    double x, double y, double z,
                                    String entityName) {
        GlowGuide g = delegate;
        if (g == null || !g.enabled()) return Result.UNAVAILABLE;
        GlowGuide.Outcome o = g.glowEntityAt(player, world, x, y, z,
                entityName);
        return switch (o.kind()) {
            case ENTITY -> Result.ENTITY;
            case MARKER -> Result.MARKER;
            default -> Result.UNAVAILABLE;
        };
    }
}
