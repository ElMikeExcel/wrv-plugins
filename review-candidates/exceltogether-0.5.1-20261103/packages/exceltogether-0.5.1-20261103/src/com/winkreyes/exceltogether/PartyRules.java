package com.winkreyes.exceltogether;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.function.DoubleSupplier;

/**
 * Pure rules for the 0.4.0 party combat extras: friendly-fire blocking and bonus mob drops.
 * No Bukkit types; the listener (PartyCombatDrops) fills in the facts and acts on the verdict.
 */
public final class PartyRules {
    public static final Set<String> DEFAULT_ALLOWED_REASONS = Set.of(
            "NATURAL", "CHUNK_GEN", "DEFAULT", "JOCKEY", "MOUNT", "PATROL", "REINFORCEMENTS",
            "VILLAGE_INVASION", "SLIME_SPLIT", "LIGHTNING", "DROWNED", "INFECTION", "TRAP",
            "SILVERFISH_BLOCK", "ENDER_PEARL", "PIGLIN_ZOMBIFIED", "FROZEN", "METAMORPHOSIS");
    public static final Set<String> DEFAULT_EXCLUDED_TYPES = Set.of(
            "PLAYER", "ARMOR_STAND", "WITHER", "ENDER_DRAGON", "WARDEN", "ELDER_GUARDIAN",
            "VILLAGER", "WANDERING_TRADER", "IRON_GOLEM", "SNOW_GOLEM", "COPPER_GOLEM");

    public enum Verdict {
        BOOST, DISABLED, NO_KILLER, EXCLUDED_TYPE, ELITEMOBS, SPAWNER, SPAWN_REASON, TAMED,
        NOT_IN_PARTY, TOO_FEW_TOGETHER, KILL_CAP;

        public boolean boost() { return this == BOOST; }
    }

    /** Facts about one kill, filled in by the listener. */
    public static final class Kill {
        public boolean killerIsPlayer;
        public String type;
        public boolean eliteMobs;
        public boolean fromSpawner;
        public String spawnReason;
        public boolean tamed;
        public boolean killerInParty;
        public int togetherCount;
        public boolean underKillCap = true;
    }

    /** Rolling-hour cap on boosted kills per player. */
    public static final class KillCap {
        private final int perHour;
        private final Map<UUID, ArrayDeque<Long>> log = new HashMap<>();

        public KillCap(int perHour) { this.perHour = perHour; }

        public boolean tryUse(UUID id, long now) {
            if (perHour <= 0) return true;
            ArrayDeque<Long> q = log.computeIfAbsent(id, k -> new ArrayDeque<>());
            while (!q.isEmpty() && now - q.peekFirst() >= 3_600_000L) q.pollFirst();
            if (q.size() >= perHour) return false;
            q.addLast(now);
            return true;
        }

        public boolean wouldAllow(UUID id, long now) {
            if (perHour <= 0) return true;
            ArrayDeque<Long> q = log.get(id);
            if (q == null) return true;
            long n = q.stream().filter(t -> now - t < 3_600_000L).count();
            return n < perHour;
        }

        public void forget(UUID id) { log.remove(id); }
    }

    private PartyRules() { }

    /** Party damage is blocked only when the rule is on, attacker and victim differ, and they share a party. */
    public static boolean blockFriendlyFire(boolean on, UUID attacker, UUID victim, BiPredicate<UUID, UUID> sameParty) {
        if (!on || attacker == null || victim == null || attacker.equals(victim)) return false;
        return sameParty.test(attacker, victim);
    }

    /** Should this kill's plain drops be multiplied? Checks run cheapest-refusal first; the order is the contract. */
    public static Verdict judge(boolean on, Kill kill, int minTogether, Set<String> allowedReasons, Set<String> excludedTypes) {
        if (!on) return Verdict.DISABLED;
        if (!kill.killerIsPlayer) return Verdict.NO_KILLER;
        if (kill.type == null || excludedTypes.contains(kill.type.toUpperCase(Locale.ROOT))) return Verdict.EXCLUDED_TYPE;
        if (kill.eliteMobs) return Verdict.ELITEMOBS;
        if (kill.fromSpawner) return Verdict.SPAWNER;
        if (kill.spawnReason == null || !allowedReasons.contains(kill.spawnReason.toUpperCase(Locale.ROOT))) return Verdict.SPAWN_REASON;
        if (kill.tamed) return Verdict.TAMED;
        if (!kill.killerInParty) return Verdict.NOT_IN_PARTY;
        if (kill.togetherCount < minTogether) return Verdict.TOO_FEW_TOGETHER;
        if (!kill.underKillCap) return Verdict.KILL_CAP;
        return Verdict.BOOST;
    }

    /** Extra items for a stack of n at multiplier mult: the whole part, plus the fraction as a chance of one more. */
    public static int extraItems(int n, double mult, DoubleSupplier rng) {
        if (n <= 0 || !(mult > 1.0) || !Double.isFinite(mult)) return 0;
        double total = n * (mult - 1.0);
        int whole = (int) Math.floor(total + 1.0E-9);
        double frac = total - whole;
        if (frac > 1.0E-9 && rng.getAsDouble() < frac) whole++;
        return whole;
    }

    /** Config list to an upper-case set; blank or empty config keeps the defaults. */
    public static Set<String> upperSet(Collection<String> values, Set<String> defaults) {
        if (values == null || values.isEmpty()) return defaults;
        HashSet<String> out = new HashSet<>();
        for (String v : values) {
            if (v == null || v.isBlank()) continue;
            out.add(v.trim().toUpperCase(Locale.ROOT));
        }
        return out.isEmpty() ? defaults : Set.copyOf(out);
    }

    public static String dropsOnLine(int together, double mult) {
        return "[Party] " + together + " of you are together: mob drops x" + RangeIndicator.multText(mult) + "!";
    }

    static Set<String> enumNames(Class<? extends Enum<?>> type) {
        HashSet<String> out = new HashSet<>();
        for (Enum<?> e : type.getEnumConstants()) out.add(e.name());
        return out;
    }
}
