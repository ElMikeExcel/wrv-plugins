package com.winkreyes.rewardguard.integration;

import com.magmaguy.elitemobs.api.EliteMobDeathEvent;
import com.winkreyes.rewardguard.ExcelRewardGuard;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * EliteMobs observability only. EliteMobDeathEvent in EliteMobs 10.9.8 is an
 * informational event: it does not implement Cancellable (verified by javap
 * against the installed jar), and EliteMobs pays its coin showers through its
 * own internal handling, so there is no public API point where this guard can
 * veto an elite reward. What the guard can do is count and log the deaths of
 * DENY-classified elites so the gap is visible in the server log instead of
 * silent. EliteMobs' own anti-exploit caps remain its farm control.
 *
 * This class is only loaded when the EliteMobs plugin is present (the main
 * class checks before registering it), so the EliteMobs classes are safe to
 * reference.
 */
public final class EliteVerdictLog implements Listener {
    private final ExcelRewardGuard plugin;

    public EliteVerdictLog(ExcelRewardGuard plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEliteDeath(EliteMobDeathEvent event) {
        if (event.getEntity() instanceof LivingEntity entity && plugin.isDenied(entity)) {
            plugin.countEliteVerdict();
            plugin.getLogger().info("Denied-provenance elite died (" + entity.getType()
                + "). ExcelRewardGuard cannot veto EliteMobs rewards (no cancellable API event in 10.9.8); verdict logged for the record.");
        }
    }
}
