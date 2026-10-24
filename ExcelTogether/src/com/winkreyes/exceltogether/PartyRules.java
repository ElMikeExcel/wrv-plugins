package com.winkreyes.exceltogether;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;

/**
 * ExcelTogether 0.4.0 (WRV party drops + party friendly fire): the pure decision rules, no Bukkit
 * calls, so they are unit-tested on a plain JVM. The Bukkit listener (PartyCombatDrops) only
 * gathers facts and asks these methods.
 */
public final class PartyRules {
    private PartyRules() { }

    // ---------------------------------------------------------------- friendly fire

    /**
     * True when the hit must be cancelled: friendly fire is on in config, attacker and victim
     * are two different players, and both are in the same ExcelTogether party. Unknown attacker
     * (null) is never blocked, so mobs, lava and fall damage are untouched.
     */
    public static boolean blockFriendlyFire(boolean enabled, UUID attacker, UUID victim, BiPredicate<UUID, UUID> sameParty) {
        if (!enabled || attacker == null || victim == null || attacker.equals(victim)) return false;
        return sameParty.test(attacker, victim);
    }

    // ---------------------------------------------------------------- party drops

    /** Spawn reasons that count as a normal, naturally found mob (allow list, everything else pays 1x). */
    public static final Set<String> DEFAULT_ALLOWED_REASONS = Set.of(
            "NATURAL", "CHUNK_GEN", "DEFAULT", "JOCKEY", "MOUNT", "PATROL", "REINFORCEMENTS",
            "VILLAGE_INVASION", "SLIME_SPLIT", "LIGHTNING", "DROWNED", "INFECTION", "TRAP",
            "SILVERFISH_BLOCK", "ENDER_PEARL", "PIGLIN_ZOMBIFIED", "FROZEN", "METAMORPHOSIS");

    /** Mob types that never get party drops (bosses have ExcelBossTrial spoils; players have Death Record). */
    public static final Set<String> DEFAULT_EXCLUDED_TYPES = Set.of(
            "PLAYER", "ARMOR_STAND", "WITHER", "ENDER_DRAGON", "WARDEN", "ELDER_GUARDIAN",
            "VILLAGER", "WANDERING_TRADER", "IRON_GOLEM", "SNOW_GOLEM", "COPPER_GOLEM");

    /** Why a kill did or did not get the party drop multiplier (also used for the debug console line). */
    public enum Verdict {
        BOOST, DISABLED, NO_KILLER, EXCLUDED_TYPE, ELITEMOBS, SPAWNER, SPAWN_REASON, TAMED, NOT_IN_PARTY, TOO_FEW_TOGETHER, KILL_CAP;
        public boolean boost() { return this == BOOST; }
    }

    /** Facts about one kill, gathered by the listener. */
    public static final class Kill {
        public boolean killerIsPlayer;      // entity.getKiller() != null (vanilla: a player hit it in the last ~5 seconds)
        public String type;                 // EntityType name
        public boolean eliteMobs;           // any elitemobs:* key on the mob (EliteMobs elite, boss or NPC)
        public boolean fromSpawner;         // Paper fromMobSpawner()
        public String spawnReason;          // Paper getEntitySpawnReason() name
        public boolean tamed;               // a tamed pet (never boosted)
        public boolean killerInParty;
        public int togetherCount;           // killer + party members in range (alive, same world, not spectator)
        public boolean underKillCap = true; // killer still under max-boosted-kills-per-hour
    }

    public static Verdict judge(boolean enabled, Kill k, int minTogether, Set<String> allowedReasons, Set<String> excludedTypes) {
        if (!enabled) return Verdict.DISABLED;
        if (!k.killerIsPlayer) return Verdict.NO_KILLER;
        if (k.type == null || excludedTypes.contains(k.type.toUpperCase(Locale.ROOT))) return Verdict.EXCLUDED_TYPE;
        if (k.eliteMobs) return Verdict.ELITEMOBS;
        if (k.fromSpawner) return Verdict.SPAWNER;
        if (k.spawnReason == null || !allowedReasons.contains(k.spawnReason.toUpperCase(Locale.ROOT))) return Verdict.SPAWN_REASON;
        if (k.tamed) return Verdict.TAMED;
        if (!k.killerInParty) return Verdict.NOT_IN_PARTY;
        if (k.togetherCount < minTogether) return Verdict.TOO_FEW_TOGETHER;
        if (!k.underKillCap) return Verdict.KILL_CAP;
        return Verdict.BOOST;
    }

    /**
     * How many EXTRA items to add for a stack of {@code amount} at multiplier m (m >= 1).
     * Whole part exact; fractional part rolled once per stack (exact in expectation).
     * Integer multipliers (the 2.0 default) never touch the random source.
     */
    public static int extraItems(int amount, double m, DoubleSupplier rng) {
        if (amount <= 0 || !(m > 1.0) || !Double.isFinite(m)) return 0;
        double exact = amount * (m - 1.0);
        int whole = (int) Math.floor(exact + 1e-9);
        double frac = exact - whole;
        if (frac > 1e-9 && rng.getAsDouble() < frac) whole++;
        return whole;
    }

    /** Parse a config list into an upper-case set; empty or missing falls back to the default. */
    public static Set<String> upperSet(Collection<String> in, Set<String> fallback) {
        if (in == null || in.isEmpty()) return fallback;
        java.util.HashSet<String> s = new java.util.HashSet<>();
        for (String x : in) if (x != null && !x.isBlank()) s.add(x.trim().toUpperCase(Locale.ROOT));
        return s.isEmpty() ? fallback : Set.copyOf(s);
    }

    /** Player-facing line (plain ASCII, short, reads fine on Bedrock and Java). */
    public static String dropsOnLine(int together, double m) {
        return "[Party] " + together + " of you are together: mob drops x" + RangeIndicator.multText(m) + "!";
    }

    /** Sliding one-hour kill counter per player (anti mob-farm). */
    public static final class KillCap {
        private final int perHour; private final java.util.Map<UUID, java.util.ArrayDeque<Long>> log = new java.util.HashMap<>();
        public KillCap(int perHour) { this.perHour = perHour; }
        /** Records a boosted kill if allowed; false = over the cap (0 or less = no cap). */
        public boolean tryUse(UUID id, long now) {
            if (perHour <= 0) return true;
            java.util.ArrayDeque<Long> q = log.computeIfAbsent(id, k -> new java.util.ArrayDeque<>());
            while (!q.isEmpty() && now - q.peekFirst() >= 3_600_000L) q.pollFirst();
            if (q.size() >= perHour) return false;
            q.addLast(now); return true;
        }
        public boolean wouldAllow(UUID id, long now) {
            if (perHour <= 0) return true;
            java.util.ArrayDeque<Long> q = log.get(id);
            if (q == null) return true;
            long n = q.stream().filter(t -> now - t < 3_600_000L).count();
            return n < perHour;
        }
        public void forget(UUID id) { log.remove(id); }
    }

    static Set<String> enumNames(Class<? extends Enum<?>> c) {
        java.util.HashSet<String> s = new java.util.HashSet<>();
        for (Enum<?> e : c.getEnumConstants()) s.add(e.name());
        return s;
    }
}
