package com.winkreyes.exceltrainingwheels.core;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * All zombies break doors (0.1.3, Mike, Oct 23, 2026, 11:14 PM CDT):
 * vanilla rolls door-breaking at spawn for at most 10% of zombies
 * (scaled by regional difficulty). With this rule ON, every zombie
 * of a listed type is granted it. EliteMobs elites are never
 * touched (EliteMobs owns their behavior). Drowned have no door
 * goal in vanilla, so they are never in the default list. Pure
 * decision logic, no Bukkit.
 */
public final class ZombieDoorRule {

    /** Default types: the zombie family that runs the vanilla break-door goal. */
    public static final List<String> DEFAULT_TYPES = List.of("ZOMBIE", "HUSK", "ZOMBIE_VILLAGER");

    /** Types that can never use the goal (granting is a no-op, so they are refused). */
    public static final Set<String> NEVER = Set.of("DROWNED");

    private ZombieDoorRule() {}

    /** Normalize a configured type list; null/empty -> defaults; unknown-to-goal types dropped. */
    public static Set<String> parseTypes(List<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            out.addAll(DEFAULT_TYPES);
            return out;
        }
        for (String s : raw) {
            if (s == null) continue;
            String t = s.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty() || NEVER.contains(t)) continue;
            out.add(t);
        }
        return out;
    }

    /**
     * Grant door-breaking to this entity?
     * @param featureOn   doors.all-zombies-break-doors (and the master switch)
     * @param typeName    Bukkit EntityType name
     * @param isElite     EliteMobs tags it as an elite
     * @param alreadyCan  it already can break doors (nothing to do)
     */
    public static boolean shouldGrant(boolean featureOn, Set<String> types, String typeName,
                                      boolean isElite, boolean alreadyCan) {
        if (!featureOn || typeName == null || types == null) return false;
        if (isElite || alreadyCan) return false;
        return types.contains(typeName);
    }
}
