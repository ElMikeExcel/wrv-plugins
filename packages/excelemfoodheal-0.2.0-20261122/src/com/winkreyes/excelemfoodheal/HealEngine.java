package com.winkreyes.excelemfoodheal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Pure D162 heal-budget state machine (no Bukkit): one active meal
 * budget per player, combat marks, pause/resume/expiry decisions.
 * Time is injected (millis) so unit tests drive it deterministically.
 *
 * <p>Invariant: one meal heals at most its total % of max health.
 * The engine tracks the budget in % units; the Bukkit layer converts
 * to HP against the player's CURRENT max health and reports the %
 * actually delivered, which is the only thing that drains the budget.
 * Reporting 0 delivered (player at full health) leaves the budget
 * untouched — it waits, exactly like the combat pause, until the
 * 90 s expiry.
 */
public final class HealEngine {

    /** Tunables (config-driven in the plugin; defaults are the D162 constants). */
    public record Settings(double instantPercent,
                           double ratePercentPerSecond,
                           long combatWindowMillis,
                           long budgetExpiryMillis) {}

    public enum TickKind {
        /** Deliver up to percentToDeliver this tick (resumed=true when a pause just ended). */
        DELIVER,
        /** First tick observed in combat: the budget is now waiting. */
        PAUSED,
        /** Not deliverable this tick and no transition to report. */
        WAIT,
        /** Budget died of old age; remainingPercent is what went undelivered. */
        EXPIRED
    }

    public record TickDecision(TickKind kind, double percentToDeliver, double remainingPercent, boolean resumed) {}

    private static final class Meal {
        final String foodName;
        final double totalPercent;
        final long mealAtMillis;
        double remainingPercent;
        boolean paused;

        Meal(String foodName, double totalPercent, long mealAtMillis, double remainingPercent) {
            this.foodName = foodName;
            this.totalPercent = totalPercent;
            this.mealAtMillis = mealAtMillis;
            this.remainingPercent = remainingPercent;
        }
    }

    private final Settings settings;
    private final Map<UUID, Meal> meals = new HashMap<>();
    private final Map<UUID, Long> lastCombatMillis = new HashMap<>();

    public HealEngine(Settings settings) {
        this.settings = settings;
    }

    public Settings settings() {
        return settings;
    }

    /**
     * Start (or replace) a meal budget. Returns the instant % the
     * caller should try to deliver immediately; the caller reports
     * what it actually delivered via {@link #reportDelivered}, and
     * the budget keeps the rest (total - delivered). Re-eating
     * replaces the active budget outright — budgets never stack.
     */
    public double startMeal(UUID player, String foodName, double totalPercent, long nowMillis) {
        meals.put(player, new Meal(foodName, totalPercent, nowMillis, totalPercent));
        return Math.min(settings.instantPercent(), totalPercent);
    }

    /**
     * Record HP actually delivered, as % of the player's current max
     * health. Returns true when the budget is finished (or absent).
     */
    public boolean reportDelivered(UUID player, double deliveredPercent) {
        Meal meal = meals.get(player);
        if (meal == null) return true;
        if (deliveredPercent > 0) {
            meal.remainingPercent -= deliveredPercent;
        }
        if (meal.remainingPercent <= 1e-9) {
            meals.remove(player);
            return true;
        }
        return false;
    }

    public void markCombat(UUID player, long nowMillis) {
        lastCombatMillis.put(player, nowMillis);
    }

    public boolean inCombat(UUID player, long nowMillis) {
        Long last = lastCombatMillis.get(player);
        return last != null && nowMillis - last < settings.combatWindowMillis();
    }

    public boolean hasMeal(UUID player) {
        return meals.containsKey(player);
    }

    public double remainingPercent(UUID player) {
        Meal meal = meals.get(player);
        return meal == null ? 0.0 : meal.remainingPercent;
    }

    public String mealFood(UUID player) {
        Meal meal = meals.get(player);
        return meal == null ? null : meal.foodName;
    }

    public Set<UUID> activePlayers() {
        return Set.copyOf(meals.keySet());
    }

    /** Drop a player's budget and combat mark (quit / death). */
    public void forget(UUID player) {
        meals.remove(player);
        lastCombatMillis.remove(player);
    }

    /**
     * One per-second decision for a player. Expiry is checked first —
     * a budget dies at its deadline even mid-pause. While in combat
     * the budget waits: the first in-combat tick reports PAUSED,
     * later ones WAIT, and the first deliverable tick after combat
     * ends reports DELIVER with resumed=true.
     */
    public TickDecision tick(UUID player, long nowMillis) {
        Meal meal = meals.get(player);
        if (meal == null) return new TickDecision(TickKind.WAIT, 0.0, 0.0, false);
        if (nowMillis - meal.mealAtMillis >= settings.budgetExpiryMillis()) {
            double remaining = meal.remainingPercent;
            meals.remove(player);
            return new TickDecision(TickKind.EXPIRED, 0.0, remaining, false);
        }
        if (inCombat(player, nowMillis)) {
            if (!meal.paused) {
                meal.paused = true;
                return new TickDecision(TickKind.PAUSED, 0.0, meal.remainingPercent, false);
            }
            return new TickDecision(TickKind.WAIT, 0.0, meal.remainingPercent, false);
        }
        boolean resumed = meal.paused;
        meal.paused = false;
        double amount = Math.min(settings.ratePercentPerSecond(), meal.remainingPercent);
        return new TickDecision(TickKind.DELIVER, amount, meal.remainingPercent, resumed);
    }
}
