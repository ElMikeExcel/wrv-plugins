package excelemfixes.enderman;

import java.util.Locale;

/**
 * The module's scope decision as a pure function: an enderman is
 * in scope when it is an EliteMobs elite (anywhere — elites are
 * EliteMobs content by definition, including natural elites in
 * the End) OR when it is a natural enderman in an EliteMobs-scope
 * world, where world scope is EliteMobs' own registry OR the
 * config world-name list OR the config world-name prefix
 * (shipped default "em_", the pack's world-naming family).
 */
public final class EndermanScope {

    private EndermanScope() {}

    public static boolean isInScope(boolean elite, boolean registrySays,
                                    boolean listSays, String worldName,
                                    String worldPrefix) {
        if (elite) return true;
        if (registrySays || listSays) return true;
        if (worldName == null || worldPrefix == null || worldPrefix.isEmpty()) return false;
        return worldName.toLowerCase(Locale.ROOT)
                .startsWith(worldPrefix.toLowerCase(Locale.ROOT));
    }
}
