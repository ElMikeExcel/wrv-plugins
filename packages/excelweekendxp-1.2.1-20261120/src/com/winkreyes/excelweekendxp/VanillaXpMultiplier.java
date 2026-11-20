package com.winkreyes.excelweekendxp;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

/**
 * Owns the vanilla (green-bar) XP multiplier inside WeekendXP (1.2.1, audit finding F-1).
 *
 * <p>1.2.0 probed the patched mcMMO for a vanilla-XP bridge API
 * ({@code isVanillaXpBonusSupported} / {@code setVanillaXpEventMultiplier} /
 * {@code getVanillaXpEventMultiplier}) that WRV-XP-2 never exposed, so the vanilla leg
 * could never engage. This class replaces that probe entirely: WeekendXP keeps the
 * multiplier as its own state and scales vanilla XP gains in a
 * {@link PlayerExpChangeEvent} listener. No mcMMO API is involved.
 *
 * <p>No double-count with the {@code mcmmo:xprate} leg: mcMMO skill XP flows through
 * {@code McMMOPlayer.addXp} (internal profile math) and the patched mcMMO source
 * contains zero references to {@code PlayerExpChangeEvent}, so a skill-XP gain never
 * passes through this listener. Only Bukkit vanilla XP gains are scaled here.
 *
 * <p>Rounding: XP amounts are ints, so a scaled amount is rounded to the nearest int
 * and never reduced below the original amount (the standing "vanilla XP never nerfed"
 * rule). On weekdays the multiplier is exactly 1.0 and the listener returns without
 * touching the event at all, so weekday amounts are bit-for-bit unchanged.
 */
final class VanillaXpMultiplier implements Listener {
    private volatile double multiplier = 1.0;

    double getMultiplier() {
        return multiplier;
    }

    /**
     * Sets the current multiplier. Returns true on success. Only ever boosts, never
     * reduces: values below 1.0 (and non-finite values) are rejected.
     */
    boolean applyMultiplier(double value) {
        if (!Double.isFinite(value) || value < 1.0) {
            throw new IllegalArgumentException("Vanilla XP multiplier must be finite and at least 1.0");
        }
        multiplier = value;
        return true;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        double current = multiplier;
        if (current <= 1.0) {
            return;
        }
        int amount = event.getAmount();
        if (amount <= 0) {
            return;
        }
        int scaled = (int) Math.round(amount * current);
        if (scaled > amount) {
            event.setAmount(scaled);
        }
    }
}
