package com.winkreyes.exceltrainingwheels.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Night Hearing (0.1.7, Mike Oct 25, 2026, 2:34 PM CDT).
 *
 * From Seal 2 on, at night in the overworld, zombies HEAR loud
 * player actions (doors, trapdoors, gates, mining, placing,
 * hitting, shooting, containers, note blocks, jukeboxes) and come
 * for the player who made the noise. The existing door rules do
 * the rest: every zombie breaks wooden doors, copper falls to the
 * 45s siege, iron to the 60s siege, and any door holds for a
 * protected player. Hearing never breaks blocks and never spawns
 * mobs; it only gives nearby, idle zombies a target.
 *
 * Pure decision logic plus two small books (per-player cooldown,
 * pulled-zombie clocks) on a caller-supplied millisecond clock.
 * No Bukkit; the glue owns events, entities and targeting.
 */
public final class NightHearing {

    public static final int DEFAULT_MIN_SEALS = 2;
    public static final long NIGHT_START = 13000L;
    public static final long NIGHT_END = 23000L;
    public static final double DEFAULT_RANGE = 16.0;
    public static final int DEFAULT_MAX_PER_NOISE = 3;
    public static final int DEFAULT_MAX_PER_PLAYER = 6;
    public static final long DEFAULT_COOLDOWN_SECONDS = 20L;
    public static final long DEFAULT_GIVE_UP_SECONDS = 60L;

    /** Vanilla game-event keys that count as a LOUD player action. */
    public static final List<String> LOUD_EVENTS = List.of(
            "block_open", "block_close",          // doors, trapdoors, fence gates
            "block_place", "block_destroy",       // building and mining
            "entity_damage", "entity_damaged",    // hitting (key name differs by version)
            "projectile_shoot",                   // bows, crossbows, tridents, snowballs
            "container_open", "container_close",  // chests, barrels, shulkers
            "shulker_open", "shulker_close",
            "note_block_play", "jukebox_play");

    /** Footsteps: only with night-hearing.hear-footsteps (off by default). */
    public static final String STEP = "step";

    /** Zombie types that can hear (the door-breaking family). */
    public static final List<String> DEFAULT_TYPES = List.of("ZOMBIE", "HUSK", "ZOMBIE_VILLAGER");

    private NightHearing() {}

    // ---------- when is it on ----------

    /** World time (0..23999, wraps) is night: 13000 to 23000 inclusive. */
    public static boolean isNight(long worldTime) {
        long t = Math.floorMod(worldTime, 24000L);
        return t >= NIGHT_START && t <= NIGHT_END;
    }

    /**
     * Is hearing live in this world right now? Fails CLOSED: an
     * unknown seal count (GuildGate unreachable) means OFF, never a
     * guess that turns it on.
     */
    public static boolean active(boolean featureOn, boolean masterOn, boolean sealKnown, int seals,
                                 int minSeals, boolean overworld, boolean excludedWorld, long worldTime) {
        if (!featureOn || !masterOn || !sealKnown) return false;
        if (seals < minSeals) return false;
        if (!overworld || excludedWorld) return false;
        return isNight(worldTime);
    }

    /** Excluded: listed by exact name, or the name starts with a listed prefix (EliteMobs "em_"). */
    public static boolean excludedWorld(String worldName, Set<String> names, List<String> prefixes) {
        if (worldName == null) return true;
        if (names != null && names.contains(worldName)) return true;
        if (prefixes != null) {
            for (String p : prefixes) {
                if (p != null && !p.isEmpty() && worldName.toLowerCase(Locale.ROOT).startsWith(p.toLowerCase(Locale.ROOT))) return true;
            }
        }
        return false;
    }

    // ---------- what is a noise, how far does it carry ----------

    /** Game-event key (with or without "minecraft:") counts as a noise? */
    public static boolean isNoise(String key, boolean hearFootsteps) {
        if (key == null) return false;
        String k = key.toLowerCase(Locale.ROOT);
        int colon = k.indexOf(':');
        if (colon >= 0) k = k.substring(colon + 1);
        if (STEP.equals(k)) return hearFootsteps;
        return LOUD_EVENTS.contains(k);
    }

    /** Hearing range: sneaking halves it. Never negative. */
    public static double range(double base, boolean sneaking) {
        double r = Math.max(0.0, base);
        return sneaking ? r / 2.0 : r;
    }

    /**
     * Can this player be heard at all? Protected players are NEVER
     * heard (silently; no message anywhere). Nor creative/spectator,
     * the Bedrock loading grace, a MobVeil veil, sleeping, or dead.
     */
    public static boolean playerHearable(String gameMode, boolean isProtected, boolean inGrace,
                                         boolean veiled, boolean sleeping, boolean dead) {
        if (isProtected || inGrace || veiled || sleeping || dead) return false;
        if (gameMode == null) return false;
        String g = gameMode.toUpperCase(Locale.ROOT);
        return g.equals("SURVIVAL") || g.equals("ADVENTURE");
    }

    /** Can this zombie be pulled? Listed type, idle (no target), not an EliteMobs elite. */
    public static boolean zombieEligible(Set<String> types, String typeName, boolean hasTarget, boolean elite) {
        if (types == null || typeName == null) return false;
        if (hasTarget || elite) return false;
        return types.contains(typeName);
    }

    /** How many to pull for this noise: per-noise cap, minus room left under the per-player cap. */
    public static int pullCount(int candidates, int currentHunters, int maxPerNoise, int maxPerPlayer) {
        int room = Math.max(0, maxPerPlayer - Math.max(0, currentHunters));
        return Math.max(0, Math.min(Math.min(candidates, Math.max(0, maxPerNoise)), room));
    }

    /** Normalize a type list; null/empty -> defaults; DROWNED never (no door goal). */
    public static Set<String> parseTypes(List<String> raw) {
        Set<String> out = new LinkedHashSet<>();
        if (raw == null || raw.isEmpty()) {
            out.addAll(DEFAULT_TYPES);
            return out;
        }
        for (String s : raw) {
            if (s == null) continue;
            String t = s.trim().toUpperCase(Locale.ROOT);
            if (t.isEmpty() || t.equals("DROWNED")) continue;
            out.add(t);
        }
        if (out.isEmpty()) out.addAll(DEFAULT_TYPES);
        return out;
    }

    public static double sanitizeRange(double raw) {
        if (Double.isNaN(raw) || raw < 1.0) return 1.0;
        return Math.min(raw, 48.0);
    }

    public static int sanitizeCount(int raw, int max) {
        return Math.max(0, Math.min(raw, max));
    }

    public static long sanitizeSeconds(long raw, long max) {
        return Math.max(0L, Math.min(raw, max));
    }

    // ---------- per-player cooldown ----------

    /** A noise that pulled at least one zombie starts the player's cooldown. */
    public static final class Cooldown {
        private final long millis;
        private final Map<UUID, Long> last = new HashMap<>();

        public Cooldown(long millis) { this.millis = Math.max(0L, millis); }

        public boolean ready(UUID player, long now) {
            Long t = last.get(player);
            return t == null || now - t >= millis;
        }

        public void start(UUID player, long now) { last.put(player, now); }

        public void forget(UUID player) { last.remove(player); }

        public int size() { return last.size(); }
    }

    // ---------- pulled zombies ----------

    /**
     * Each pulled zombie's clock. It gives up when it has gone
     * give-up millis without reaching the player AND without
     * working on a door (a zombie at a door is making progress, so
     * the 45s copper and 60s iron sieges are never cut short).
     */
    public static final class Hunters {
        public record Entry(UUID player, long pulledAt, long lastProgress) {}

        public enum Verdict { KEEP, REACHED, GIVE_UP, LOST }

        private final long giveUpMillis;
        private final Map<UUID, Entry> byZombie = new HashMap<>();

        public Hunters(long giveUpMillis) { this.giveUpMillis = Math.max(0L, giveUpMillis); }

        public void pull(UUID zombie, UUID player, long now) {
            byZombie.put(zombie, new Entry(player, now, now));
        }

        public Entry get(UUID zombie) { return byZombie.get(zombie); }

        public boolean contains(UUID zombie) { return byZombie.containsKey(zombie); }

        public int size() { return byZombie.size(); }

        public java.util.Set<UUID> zombies() { return new java.util.HashSet<>(byZombie.keySet()); }

        /**
         * One check of a pulled zombie.
         * @param stillHunting its target is still the player it heard
         * @param reached      within reach of that player (now a normal fight)
         * @param atDoor       working on a door toward that player
         */
        public Verdict check(UUID zombie, boolean stillHunting, boolean reached, boolean atDoor, long now) {
            Entry e = byZombie.get(zombie);
            if (e == null) return Verdict.LOST;
            if (!stillHunting) { byZombie.remove(zombie); return Verdict.LOST; }
            if (reached) { byZombie.remove(zombie); return Verdict.REACHED; }
            long progress = atDoor ? now : e.lastProgress();
            if (atDoor) byZombie.put(zombie, new Entry(e.player(), e.pulledAt(), now));
            if (now - progress >= giveUpMillis) { byZombie.remove(zombie); return Verdict.GIVE_UP; }
            return Verdict.KEEP;
        }

        public void remove(UUID zombie) { byZombie.remove(zombie); }

        /** Drop every zombie pulled toward this player; returns their ids (the glue clears targets). */
        public java.util.List<UUID> releasePlayer(UUID player) {
            java.util.List<UUID> out = new java.util.ArrayList<>();
            Iterator<Map.Entry<UUID, Entry>> it = byZombie.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<UUID, Entry> me = it.next();
                if (me.getValue().player().equals(player)) { out.add(me.getKey()); it.remove(); }
            }
            return out;
        }

        public java.util.List<UUID> releaseAll() {
            java.util.List<UUID> out = new java.util.ArrayList<>(byZombie.keySet());
            byZombie.clear();
            return out;
        }
    }

    // ---------- bed ----------

    /**
     * Force the bed open? Only for the "monsters nearby" refusal
     * (NOT_SAFE), only while hearing is live there. Never overrides
     * "not night", "too far", "obstructed" or a wrong dimension.
     */
    public static boolean forceBed(boolean bedOptionOn, boolean hearingActiveHere, String bedResult) {
        return bedOptionOn && hearingActiveHere && "NOT_SAFE".equals(bedResult);
    }

    /** Normal-difficulty wood fallback runs only when the world is NOT on Hard (Hard = vanilla does it). */
    public static boolean woodFallbackRuns(boolean optionOn, String difficulty) {
        return optionOn && difficulty != null && !"HARD".equalsIgnoreCase(difficulty) && !"PEACEFUL".equalsIgnoreCase(difficulty);
    }

    // ---------- door steering ----------
    //
    // A heard zombie with no walkable way to the player goes, in vanilla,
    // to the closest spot outside, usually a bare wall, and stands there.
    // Steering sends it to the outside face of a door near the player
    // instead, where the door's own siege takes over (wood, copper, iron).

    public static final double DEFAULT_STEER_RADIUS = 12.0;

    public enum DoorKind { WOOD, COPPER, IRON }

    /** Which siege a steered zombie works on this door; null = not a door we steer to. */
    public static DoorKind steerKind(String materialName, boolean copperTierOn) {
        if (materialName == null) return null;
        String m = materialName.toUpperCase(java.util.Locale.ROOT);
        if (!m.endsWith("_DOOR") || m.endsWith("TRAPDOOR")) return null;
        if (m.equals("IRON_DOOR")) return DoorKind.IRON;
        if (CopperDoors.isCopperDoor(m)) return copperTierOn ? DoorKind.COPPER : null;
        return DoorKind.WOOD;
    }

    /** Lower is better: a short walk for the zombie, then a door close to the player. */
    public static double steerScore(int pathNodes, double insideToPlayer) {
        return Math.max(0, pathNodes) + 2.0 * Math.max(0.0, insideToPlayer);
    }

    /** The outside face is the one farther from the player than the inside face. */
    public static boolean outsideFace(double faceToPlayerSq, double otherFaceToPlayerSq) {
        return otherFaceToPlayerSq < faceToPlayerSq;
    }

    /** Is a zombie standing on the spot in front of the door? */
    public static boolean onSpot(double horizontalDistSq, double dy, double radius) {
        return horizontalDistSq <= radius * radius && Math.abs(dy) < 1.5;
    }

    public static double sanitizeSteerRadius(double raw) {
        if (Double.isNaN(raw)) return DEFAULT_STEER_RADIUS;
        return Math.max(4.0, Math.min(24.0, raw));
    }
}
