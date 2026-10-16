package com.winkreyes.rewardguard.integration;

import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.events.experience.McMMOPlayerPreXpGainEvent;
import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import com.winkreyes.rewardguard.ExcelRewardGuard;
import com.winkreyes.rewardguard.RewardScale;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * mcMMO enforcement. mcMMO's XP gain events carry the player and the amount
 * but not the killed entity, so the reduction works off the suppression
 * window the main class plants when a DENY-classified mob dies (see
 * XpSuppression for the exact matching rules). Only kill-based reasons are
 * eligible: PVE (the kill itself) and SHARED_PVE (party share).
 *
 * The scaling happens exactly once, on McMMOPlayerXpGainEvent at LOWEST
 * priority, via setRawXpGained(raw * percent / 100). That placement matters
 * twice over, both verified in the installed mcMMO build's own source:
 * McMMOPlayer.applyXpGain feeds the pre-gain amount into
 * EventUtils.handleXpGainEvent, which applies the XpGain event's raw value
 * after every listener has run — so scaling the pre-gain event as well
 * would scale twice, and scaling at LOWEST means weekend and party
 * multipliers (which listen later) multiply the already-reduced amount:
 * the farm check still runs before the multipliers, as in 1.0.
 *
 * At farm-reward-percent 0 the handlers reproduce 1.0 exactly: the pre-gain
 * event is cancelled and the gain event is zeroed and cancelled. At 100
 * both handlers are a no-op pass-through.
 *
 * This class is only loaded when the mcMMO plugin is present (the main class
 * checks before registering it), so the mcMMO classes are safe to reference.
 */
public final class McMmoVeto implements Listener {
    private final ExcelRewardGuard plugin;

    public McMmoVeto(ExcelRewardGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPreXpGain(McMMOPlayerPreXpGainEvent event) {
        // Only the percent-0 veto acts here (1.0 behavior). Above 0 the
        // pre-gain amount must pass through untouched: it becomes the raw
        // value the XpGain handler below scales, exactly once.
        if (plugin.farmRewardPercent() > 0) return;
        if (matchesDeniedKill(event.getPlayer(), event.getXpGainReason())) {
            event.setCancelled(true);
            plugin.countMcMmoIntervention();
            logIntervention(event.getPlayer(), event.getSkill().name(), event.getXpGainReason(), "blocked");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onXpGain(McMMOPlayerXpGainEvent event) {
        int percent = plugin.farmRewardPercent();
        if (percent >= 100) return;
        if (!matchesDeniedKill(event.getPlayer(), event.getXpGainReason())) return;
        float raw = event.getRawXpGained();
        if (percent <= 0) {
            event.setRawXpGained(0.0f);
            event.setCancelled(true);
            plugin.countMcMmoIntervention();
            logIntervention(event.getPlayer(), event.getSkill().name(), event.getXpGainReason(),
                "blocked (raw XP " + raw + " -> 0)");
            return;
        }
        event.setRawXpGained(RewardScale.scaleFloat(raw, percent));
        plugin.countMcMmoIntervention();
        logIntervention(event.getPlayer(), event.getSkill().name(), event.getXpGainReason(),
            "scaled to " + percent + "% (raw XP " + raw + " -> " + event.getRawXpGained() + ")");
    }

    private boolean matchesDeniedKill(Player player, XPGainReason reason) {
        if (reason != XPGainReason.PVE && reason != XPGainReason.SHARED_PVE) return false;
        Location location = player.getLocation();
        if (location.getWorld() == null) return false;
        return plugin.suppression().shouldSuppress(player.getUniqueId(), location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(), plugin.currentTick(), true);
    }

    private void logIntervention(Player player, String skill, XPGainReason reason, String action) {
        if (plugin.debugLogging()) {
            plugin.getLogger().info("mcMMO XP " + action + " for " + player.getName()
                + " (" + skill + ", " + reason + ") inside a denied-kill window.");
        }
    }
}
