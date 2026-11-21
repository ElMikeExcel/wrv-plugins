package com.winkreyes.excelweekendxp;

import com.magmaguy.elitemobs.api.EliteSkillXpGainEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Owns the EliteMobs skill-XP weekend multiplier inside WeekendXP (1.3.0,
 * decision D155: Mike's Nov 20, 2026 direct — the Fri-Sun 1.2x must also cover
 * EliteMobs skill XP).
 *
 * <p>This class mirrors {@link VanillaXpMultiplier} exactly: WeekendXP keeps
 * the multiplier as its own volatile state and scales gains in an event
 * listener. The event is EliteMobs' own API event,
 * {@link EliteSkillXpGainEvent}, which EliteMobs fires for combat skill-XP
 * gains (its {@code Reason} enum has exactly one value, COMBAT). The handler
 * runs at {@code HIGHEST} with {@code ignoreCancelled = true}, so a gain
 * another plugin cancelled is never resurrected or scaled here.
 *
 * <p>Exact payouts stay exact by construction: programmatic grants through
 * {@code PlayerData.addSkillXP} (ExcelQuestXP quest payouts, quest backpay)
 * do NOT fire this event, so they never pass through this listener and are
 * never scaled. Only event-fired combat gains are scaled — there is no
 * grant-path code in this plugin.
 *
 * <p>No world filter: the event only fires on elite kills, which is what
 * "EliteMobs XP" is; overworld timed-event elites count too (D121 keeps those
 * events as a feature).
 *
 * <p>Rounding: XP amounts are longs, so a scaled amount is rounded to the
 * nearest long and never reduced below the original amount (the standing
 * "XP never nerfed" rule). On weekdays the multiplier is exactly 1.0 and the
 * listener returns without touching the event at all, so weekday amounts are
 * bit-for-bit unchanged.
 */
final class EmSkillXpMultiplier implements Listener {
    private volatile double multiplier = 1.0;

    double getMultiplier() {
        return multiplier;
    }

    /**
     * Sets the current multiplier. Returns true on success. Only ever boosts,
     * never reduces: values below 1.0 (and non-finite values) are rejected.
     */
    boolean applyMultiplier(double value) {
        if (!Double.isFinite(value) || value < 1.0) {
            throw new IllegalArgumentException(
                    "EliteMobs skill XP multiplier must be finite and at least 1.0");
        }
        multiplier = value;
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEliteSkillXpGain(EliteSkillXpGainEvent event) {
        double current = multiplier;
        if (current <= 1.0) {
            return;
        }
        long xp = event.getXp();
        if (xp <= 0) {
            return;
        }
        long scaled = Math.round(xp * current);
        if (scaled > xp) {
            event.setXp(scaled);
        }
    }
}
