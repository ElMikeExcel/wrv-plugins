package com.winkreyes.exceldetails;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One player's accumulators for one segment (or the overall totals).
 * Pure data — no Bukkit. Amounts are final-damage HP.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class CombatStats {
    private double damageDone;
    private double damageTaken;
    private double healingDone;
    private long hits;
    private final Map<String, Double> doneByTarget = new LinkedHashMap<>();
    private final Map<String, Double> takenBySource = new LinkedHashMap<>();

    public void addDamageDone(double amount, String target) {
        if (amount <= 0) return;
        damageDone += amount;
        hits++;
        if (target != null) doneByTarget.merge(target, amount, Double::sum);
    }

    public void addDamageTaken(double amount, String source) {
        if (amount <= 0) return;
        damageTaken += amount;
        if (source != null) takenBySource.merge(source, amount, Double::sum);
    }

    public void addHealing(double amount) {
        if (amount <= 0) return;
        healingDone += amount;
    }

    public double damageDone() { return damageDone; }
    public double damageTaken() { return damageTaken; }
    public double healingDone() { return healingDone; }
    public long hits() { return hits; }
    public Map<String, Double> doneByTarget() { return doneByTarget; }
    public Map<String, Double> takenBySource() { return takenBySource; }

    public double forWindow(Window w) {
        return switch (w) {
            case DAMAGE_DONE -> damageDone;
            case DAMAGE_TAKEN -> damageTaken;
            case HEALING_DONE -> healingDone;
        };
    }

    /**
     * Rebuild a totals-only block (0.2.0 persistence restore). The
     * by-target / by-source breakdown maps are session-scoped and
     * start empty; only the totals + hit count ride data.yml.
     */
    public static CombatStats fromTotals(double done, double taken, double healing, long hits) {
        CombatStats c = new CombatStats();
        c.damageDone = Math.max(0, done);
        c.damageTaken = Math.max(0, taken);
        c.healingDone = Math.max(0, healing);
        c.hits = Math.max(0, hits);
        return c;
    }

    /** Fold another stats block into this one (segment -> overall). */
    public void absorb(CombatStats other) {
        damageDone += other.damageDone;
        damageTaken += other.damageTaken;
        healingDone += other.healingDone;
        hits += other.hits;
        other.doneByTarget.forEach((k, v) -> doneByTarget.merge(k, v, Double::sum));
        other.takenBySource.forEach((k, v) -> takenBySource.merge(k, v, Double::sum));
    }

    public CombatStats copy() {
        CombatStats c = new CombatStats();
        c.absorb(this);
        return c;
    }

    public enum Window {
        DAMAGE_DONE("Damage Done"),
        DAMAGE_TAKEN("Damage Taken"),
        HEALING_DONE("Healing Done");

        private final String title;
        Window(String title) { this.title = title; }
        public String title() { return title; }

        public Window next() {
            Window[] all = values();
            return all[(ordinal() + 1) % all.length];
        }

        public static Window parse(String s) {
            if (s == null) return null;
            return switch (s.toLowerCase()) {
                case "damage", "done", "dps", "damage_done" -> DAMAGE_DONE;
                case "taken", "damage_taken", "dtps" -> DAMAGE_TAKEN;
                case "healing", "heal", "healing_done", "hps" -> HEALING_DONE;
                default -> null;
            };
        }
    }
}
