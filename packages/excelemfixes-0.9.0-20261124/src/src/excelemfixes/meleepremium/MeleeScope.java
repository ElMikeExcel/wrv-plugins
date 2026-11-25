package excelemfixes.meleepremium;

import java.util.Locale;

/**
 * The module's world-scope decision as a pure function (unit-
 * testable): a world is in scope when EliteMobs' own registry
 * claims it OR the config world-name list names it OR its name
 * starts with the config world-name prefix (shipped default
 * "em_", the pack's world-naming family).
 *
 * This is the pack's standard EM-scope rule (the food-heal
 * module's registry-or-list gate, plus the enderman module's
 * prefix signal) WITHOUT the enderman module's elite-anywhere
 * exception: an elite that wanders into the overworld (a timed
 * event boss, say) is fought under overworld rules — the
 * premium is priced for EliteMobs worlds, and the overworld is
 * never touched.
 */
public final class MeleeScope {

    private MeleeScope() {}

    public static boolean isWorldInScope(boolean registrySays,
                                         boolean listSays,
                                         String worldName,
                                         String worldPrefix) {
        if (registrySays || listSays) return true;
        if (worldName == null || worldPrefix == null
                || worldPrefix.isEmpty()) return false;
        return worldName.toLowerCase(Locale.ROOT)
                .startsWith(worldPrefix.toLowerCase(Locale.ROOT));
    }
}
