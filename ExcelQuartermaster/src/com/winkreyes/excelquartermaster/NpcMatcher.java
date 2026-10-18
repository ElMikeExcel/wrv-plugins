package com.winkreyes.excelquartermaster;

/**
 * Pure matching rules for "is this entity the Old Quartermaster?".
 *
 * No Bukkit dependencies: the listener hands in plain values (the entity's
 * custom name, its world name and coordinates), so the rules are
 * unit-testable on a plain JVM.
 *
 * An entity matches when BOTH hold:
 * - Name: the entity's custom name, with any legacy section-sign color
 *   codes stripped and surrounding whitespace trimmed, equals the
 *   configured name, ignoring case. A null or blank name never matches.
 * - Anchor (only when enabled): the entity is in the configured world and
 *   within the configured radius (3D distance) of the anchor point. The
 *   boundary counts as inside. A null world on either side never matches
 *   while the anchor is on. A negative radius clamps to 0 (same block
 *   only). With the anchor disabled, location is ignored entirely.
 */
public final class NpcMatcher {

    private NpcMatcher() {}

    /** Strip legacy § color/format codes and trim. Null reads as "". */
    public static String normalizeName(String raw) {
        if (raw == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c == '§') {
                if (i + 1 < raw.length()) {
                    i++; // swallow the code character too
                }
                continue; // a trailing lone § is dropped as well
            }
            out.append(c);
        }
        return out.toString().trim();
    }

    /** True when the entity's custom name is the configured NPC name. */
    public static boolean nameMatches(String entityCustomName, String configuredName) {
        String entity = normalizeName(entityCustomName);
        String configured = normalizeName(configuredName);
        return !entity.isEmpty() && !configured.isEmpty() && entity.equalsIgnoreCase(configured);
    }

    /** True when the location passes the anchor check (see class docs). */
    public static boolean anchorMatches(boolean enabled,
                                          String anchorWorld, double ax, double ay, double az,
                                          double radius,
                                          String entityWorld, double ex, double ey, double ez) {
        if (!enabled) {
            return true;
        }
        if (anchorWorld == null || entityWorld == null || !anchorWorld.equals(entityWorld)) {
            return false;
        }
        double r = Math.max(0.0, radius);
        double dx = ex - ax;
        double dy = ey - ay;
        double dz = ez - az;
        return dx * dx + dy * dy + dz * dz <= r * r;
    }

    /** The full match: name AND anchor. */
    public static boolean matches(String entityCustomName, String configuredName,
                                    boolean anchorEnabled,
                                    String anchorWorld, double ax, double ay, double az,
                                    double radius,
                                    String entityWorld, double ex, double ey, double ez) {
        return nameMatches(entityCustomName, configuredName)
                && anchorMatches(anchorEnabled, anchorWorld, ax, ay, az, radius,
                                 entityWorld, ex, ey, ez);
    }
}
