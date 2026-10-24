package com.winkreyes.excelattunement.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 0.1.6 arena locks (Mike, Oct 24, 2026), pure logic, no Bukkit.
 *
 * Some Magic (elmakers) movement spells are sealed near the Ender Dragon
 * while she lives: inside the arena zone (a vertical cylinder around the
 * End main island, all heights) of a world whose dragon is alive, a
 * locked spell cannot be cast. Outside the zone, or with no live dragon
 * in that world, every spell works. The lock follows the dragon, not
 * the seal: the first fight and every re-fight / re-summon are covered.
 *
 * Shipped rules: glide, fling (every level, so the leap alias, which
 * casts as fling|4, is covered), hulkleap, lift and blink. Lift is also
 * blocked when its target is a player standing inside an active zone,
 * wherever the caster stands. backflip, recall, gather, dash and evade
 * stay open; each is a one-line config add.
 *
 * Spell keys match on the base key: "glide", "Glide" and "glide|2" all
 * match a rule naming "glide". A world is in the zone's scope by exact
 * name (case-insensitive) or by its environment name (THE_END ...).
 */
public final class ArenaLock {
    private ArenaLock() {}

    /** The vanilla main island reaches about 100 blocks from 0,0; see config.yml for the reasoning. */
    public static final double DEFAULT_RADIUS = 200.0;
    public static final String MESSAGE_TEMPLATE = "%s is sealed near the dragon while she lives";

    /** Spells left open by default; each is one config line away from being locked. */
    public static final List<String> OPEN_BY_DEFAULT = List.of("backflip", "recall", "gather", "dash", "evade");

    /** What the world's dragon fight says right now. */
    public enum DragonState { ALIVE, NONE, UNKNOWN }

    /** The arena: worlds in scope (names / environments) and a vertical cylinder in each. */
    public record Zone(Set<String> worlds, Set<String> environments, double centerX, double centerZ, double radius) {
        public Zone {
            worlds = normLower(worlds);
            environments = normEnv(environments);
            if (!(radius >= 0) || Double.isInfinite(radius)) radius = DEFAULT_RADIUS;
        }

        public static Zone defaults() {
            return new Zone(Set.of(), Set.of("THE_END"), 0.0, 0.0, DEFAULT_RADIUS);
        }

        public boolean worldInScope(String worldName, String environment) {
            if (worldName != null && worlds.contains(worldName.trim().toLowerCase(Locale.ROOT))) return true;
            return environment != null && environments.contains(environment.trim().toUpperCase(Locale.ROOT));
        }

        /** Horizontal distance only: the zone covers every height (void below, sky above). */
        public boolean contains(String worldName, String environment, double x, double z) {
            if (!worldInScope(worldName, environment)) return false;
            double dx = x - centerX, dz = z - centerZ;
            return dx * dx + dz * dz <= radius * radius;
        }
    }

    /**
     * One locked spell (or alias set). stopRunning: entering an active
     * zone also stops a running cast of it (Glide). protectTargets: the
     * cast is also blocked when its target is a player inside an active
     * zone, wherever the caster is (Lift).
     */
    public record Rule(String id, Set<String> spells, String message, boolean stopRunning, boolean protectTargets) {
        public Rule {
            id = id == null ? "" : id.trim().toLowerCase(Locale.ROOT);
            spells = normSpells(spells);
            message = message == null ? "" : message;
        }

        /** A rule with no spell can never block. */
        public boolean usable() {
            return !spells.isEmpty();
        }
    }

    /** "glide" -> "Glide is sealed near the dragon while she lives". */
    public static String defaultMessage(String id) {
        String b = baseKey(id);
        if (b.isEmpty()) return "";
        String name = Character.toUpperCase(b.charAt(0)) + b.substring(1);
        return String.format(Locale.ROOT, MESSAGE_TEMPLATE, name);
    }

    /** A rule built from a one-line config add: spells = [id], default message, no extras. */
    public static Rule simple(String id) {
        return new Rule(id, Set.of(baseKey(id)), defaultMessage(id), false, false);
    }

    /**
     * The shipped rules, also used when the live config has no arena-locks
     * section (or no rules key). Only Glide is stopped on entry: it is the
     * only one of these that keeps running (Fling's fall protection and
     * Lift's levitation on someone else are left alone).
     */
    public static List<Rule> defaults() {
        return List.of(
                new Rule("glide", Set.of("glide"), defaultMessage("glide"), true, false),
                simple("fling"),
                simple("hulkleap"),
                new Rule("lift", Set.of("lift"), defaultMessage("lift"), false, true),
                simple("blink"));
    }

    /** "Glide|2 " -> "glide". Null/blank -> "". */
    public static String baseKey(String key) {
        if (key == null) return "";
        String k = key.trim().toLowerCase(Locale.ROOT);
        int bar = k.indexOf('|');
        if (bar >= 0) k = k.substring(0, bar).trim();
        return k;
    }

    public static boolean spellMatches(Rule r, String spellKey) {
        String b = baseKey(spellKey);
        return r != null && !b.isEmpty() && r.spells().contains(b);
    }

    /**
     * Fold the fight's signals into one state. Each signal is TRUE (the
     * dragon is alive / the fight is on), FALSE, or null (could not be
     * read). Any TRUE wins (ALIVE); otherwise any unreadable signal makes
     * the state UNKNOWN; only all-FALSE is NONE.
     */
    public static DragonState decide(Boolean... signals) {
        boolean unknown = signals == null;
        if (signals != null) {
            for (Boolean s : signals) {
                if (s == null) unknown = true;
                else if (s) return DragonState.ALIVE;
            }
        }
        return unknown ? DragonState.UNKNOWN : DragonState.NONE;
    }

    /** Does this dragon state hold the lock? Unknown counts as alive unless configured otherwise. */
    public static boolean dragonHolds(DragonState s, boolean blockIfUnknown) {
        if (s == null) return blockIfUnknown;
        return switch (s) {
            case ALIVE -> true;
            case NONE -> false;
            case UNKNOWN -> blockIfUnknown;
        };
    }

    /** Is this spot inside an active zone right now? */
    public static boolean activeAt(boolean enabled, Zone z, String worldName, String environment,
                                   double x, double zc, DragonState s, boolean blockIfUnknown) {
        if (!enabled || z == null) return false;
        return z.contains(worldName, environment, x, zc) && dragonHolds(s, blockIfUnknown);
    }

    /** First rule that blocks this cast by a caster standing in (or out of) an active zone, or null. */
    public static Rule firstBlocking(boolean enabled, List<Rule> rules, String spellKey,
                                     boolean casterInActiveZone, boolean bypass) {
        if (!enabled || bypass || !casterInActiveZone || rules == null) return null;
        for (Rule r : rules) if (r.usable() && spellMatches(r, spellKey)) return r;
        return null;
    }

    /** First rule for this spell that protects players in an active zone from being targeted, or null. */
    public static Rule targetRule(boolean enabled, List<Rule> rules, String spellKey, boolean bypass) {
        if (!enabled || bypass || rules == null) return null;
        for (Rule r : rules) if (r.usable() && r.protectTargets() && spellMatches(r, spellKey)) return r;
        return null;
    }

    /** Block this targeted cast? Only a player target standing in an active zone is protected. */
    public static boolean blocksTarget(Rule r, boolean targetIsPlayer, boolean targetIsCaster,
                                       boolean targetInActiveZone) {
        return r != null && r.protectTargets() && targetIsPlayer && !targetIsCaster && targetInActiveZone;
    }

    /** Base keys stopped on entering an active zone (rules with stop-running). */
    public static Set<String> stopOnEntry(boolean enabled, List<Rule> rules, boolean bypass) {
        Set<String> out = new LinkedHashSet<>();
        if (!enabled || bypass || rules == null) return out;
        for (Rule r : rules) if (r.usable() && r.stopRunning()) out.addAll(r.spells());
        return out;
    }

    private static Set<String> normSpells(Collection<String> in) {
        Set<String> out = new LinkedHashSet<>();
        if (in != null) for (String s : in) { String b = baseKey(s); if (!b.isEmpty()) out.add(b); }
        return Set.copyOf(out);
    }

    private static Set<String> normLower(Collection<String> in) {
        Set<String> out = new LinkedHashSet<>();
        if (in != null) for (String s : in) if (s != null && !s.isBlank()) out.add(s.trim().toLowerCase(Locale.ROOT));
        return Set.copyOf(out);
    }

    private static Set<String> normEnv(Collection<String> in) {
        Set<String> out = new LinkedHashSet<>();
        if (in != null) for (String s : in) if (s != null && !s.isBlank()) out.add(s.trim().toUpperCase(Locale.ROOT));
        return Set.copyOf(out);
    }
}
