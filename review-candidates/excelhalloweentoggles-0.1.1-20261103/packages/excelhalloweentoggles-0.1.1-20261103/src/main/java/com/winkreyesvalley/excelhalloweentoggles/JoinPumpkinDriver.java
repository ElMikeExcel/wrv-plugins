package com.winkreyesvalley.excelhalloweentoggles;

import cz.softici.server.minecraft.HalloweenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * ExcelHalloweenToggles — the login-pumpkin re-drive.
 *
 * The surgery turns the Halloween plugin's own join application off
 * (login_pumpkin.enabled: false). This driver restores it for
 * toggle-ON players only, through the plugin's public surface, so its
 * own 30-minute cooldown, counts and messages stay truthful:
 * canReceiveJoinPumpkin() gates, applyPumpkinIfPossible() delivers,
 * setLastPumpkinJoin() records.
 */
public final class JoinPumpkinDriver implements Listener {

    public enum ApplyResult {
        APPLIED, RETIRED, DISABLED, GLOBAL_OFF, TOGGLED_OFF, INELIGIBLE, ON_COOLDOWN
    }

    private final ExcelHalloweenToggles plugin;
    private final HalloweenPlugin halloween;
    private final FeatureRegistry registry;
    private final ToggleStore store;

    public JoinPumpkinDriver(ExcelHalloweenToggles plugin, HalloweenPlugin halloween,
                             FeatureRegistry registry, ToggleStore store) {
        this.plugin = plugin;
        this.halloween = halloween;
        this.registry = registry;
        this.store = store;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                tryApply(player, false);
            }
        }, plugin.joinDelayTicks());
    }

    /**
     * Run the login pumpkin through the full gate.
     *
     * @param ignoreCooldown true only for the op-only /scares test seam
     */
    public ApplyResult tryApply(Player player, boolean ignoreCooldown) {
        FeatureDef login = registry.loginFeature();
        if (login == null) {
            return ApplyResult.RETIRED;
        }
        if (!plugin.isEventActive()) {
            return ApplyResult.RETIRED;
        }
        if (!plugin.isSchedulerEnabled()) {
            return ApplyResult.DISABLED;
        }
        if (!halloween.getData().isGlobalEnabled()) {
            return ApplyResult.GLOBAL_OFF;
        }
        if (!store.isEnabled(player.getUniqueId(), login.key())) {
            return ApplyResult.TOGGLED_OFF;
        }
        if (!halloween.isEligible(player)) {
            return ApplyResult.INELIGIBLE;
        }
        int cooldown = halloween.getConfig().getInt(login.cooldownPath(), login.fallbackCooldownMinutes());
        if (!ignoreCooldown && !halloween.getData().canReceiveJoinPumpkin(player.getUniqueId(), cooldown)) {
            return ApplyResult.ON_COOLDOWN;
        }
        halloween.applyPumpkinIfPossible(player, true);
        halloween.getData().setLastPumpkinJoin(player.getUniqueId());
        return ApplyResult.APPLIED;
    }
}
