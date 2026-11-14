package com.winkreyes.excelprotect;

import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Excel — per-victim damage accounting between first hit and death (0.3.0), mirroring the
 * ExcelTelemetry kill schema that solved the Oct 31 armor-stand case: by_attacker keys are
 * "player:&lt;uuid&gt;", "entity:&lt;type&gt;:&lt;uuid&gt;" or "env:&lt;cause&gt;"; by_cause keys are
 * Bukkit DamageCause names. Pure logic on plain values; unit-tested.
 */
public final class DamageLedger {
    public static final class Book {
        public final Map<String, Double> byAttacker = new TreeMap<>();
        public final Map<String, Double> byCause = new TreeMap<>();
        public long firstMs, lastMs;
        public double total;
        public String lastAttackerKey, lastKind, lastUuid, lastLabel, lastCause;
        /** Victim elite level probed at DAMAGE time (0.1.7): >= 0 elite level,
         *  EliteProbe.NOT_ELITE probed-not-elite, EliteProbe.UNKNOWN never
         *  answered. Cached because EliteMobs unregisters a mob from its
         *  EntityTracker during its own death handling — an at-death probe
         *  can read NOT_ELITE for a mob that was an elite while it was being
         *  hit (the exact reason Telemetry caches elite status from damage
         *  events instead of probing at death). */
        public int eliteLevel = EliteProbe.UNKNOWN;

        /** Merge one damage-time probe: an elite answer (>= 0) always lands
         *  and sticks; NOT_ELITE lands only while nothing is known (a later
         *  conversion can still upgrade it); UNKNOWN never overwrites. */
        public void noteElite(int lvl) {
            if (lvl >= 0) eliteLevel = lvl;
            else if (lvl == EliteProbe.NOT_ELITE && eliteLevel == EliteProbe.UNKNOWN) eliteLevel = lvl;
        }

        void record(long ms, String attackerKey, String kind, String uuid, String label, String cause, double dmg) {
            if (firstMs == 0) firstMs = ms;
            lastMs = ms;
            total += dmg;
            byAttacker.merge(attackerKey, dmg, Double::sum);
            byCause.merge(cause, dmg, Double::sum);
            lastAttackerKey = attackerKey; lastKind = kind; lastUuid = uuid; lastLabel = label; lastCause = cause;
        }

        public double fightSeconds() {
            return firstMs == 0 ? 0 : (lastMs - firstMs) / 1000.0;
        }

        public Json.Obj damageJson() {
            Json.Obj byA = new Json.Obj(); byAttacker.forEach((k, v) -> byA.put(k, round(v)));
            Json.Obj byC = new Json.Obj(); byCause.forEach((k, v) -> byC.put(k, round(v)));
            return new Json.Obj().put("total", round(total)).put("by_attacker", byA).put("by_cause", byC);
        }

        /** Share of total damage from falling — Telemetry parity (0.1.7): same
         *  cause key (FALL) and same definition as Telemetry's DamageLedger. */
        public double fallShare() {
            if (total <= 0) return 0;
            return byCause.getOrDefault("FALL", 0.0) / total;
        }

        /** Share of total damage dealt by explosions (block + entity) — Telemetry parity (0.1.7). */
        public double explosionShare() {
            if (total <= 0) return 0;
            double ex = byCause.getOrDefault("BLOCK_EXPLOSION", 0.0)
                    + byCause.getOrDefault("ENTITY_EXPLOSION", 0.0);
            return ex / total;
        }

        static double round(double d) { return Math.round(d * 10.0) / 10.0; }
    }

    private final ConcurrentHashMap<UUID, Book> books = new ConcurrentHashMap<>();
    private final int cap;

    public DamageLedger(int cap) { this.cap = Math.max(100, cap); }

    public void record(UUID victim, long ms, String attackerKey, String kind, String uuid, String label, String cause, double dmg) {
        if (books.size() > cap) prune(ms - 60_000L);
        books.computeIfAbsent(victim, k -> new Book()).record(ms, attackerKey, kind, uuid, label, cause, dmg);
    }

    /** Remove and return the victim's book at death (null when the death had no recorded damage). */
    public Book take(UUID victim) { return books.remove(victim); }

    /** The victim's live book without removing it (null when none). */
    public Book book(UUID victim) { return books.get(victim); }

    public void forget(UUID victim) { books.remove(victim); }

    /** Drop books whose last damage is older than cutoffMs (despawned entities never die on record). */
    public void prune(long cutoffMs) {
        books.entrySet().removeIf(e -> e.getValue().lastMs < cutoffMs);
    }

    public int size() { return books.size(); }
}
