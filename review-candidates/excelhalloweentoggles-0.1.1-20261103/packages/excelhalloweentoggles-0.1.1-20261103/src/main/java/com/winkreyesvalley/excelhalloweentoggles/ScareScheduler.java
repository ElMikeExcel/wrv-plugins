package com.winkreyesvalley.excelhalloweentoggles;

import cz.softici.server.minecraft.HalloweenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelHalloweenToggles — the scheduler takeover.
 *
 * The design's one clean seam: the Halloween plugin's own per-player
 * task chains are config-dead (the install packet's surgery sets the
 * seven events.*.enabled / login_pumpkin.enabled flags false, and a
 * disabled task's scheduleNext() returns without scheduling). This
 * class runs the replacement chains — same shape as the plugin's own
 * tasks, one one-shot per player per scare:
 *
 *   delay  = halloween.eventDelayTicks(player, min, max)   (the plugin's
 *            own math, so the Witching Hour interval multiplier is
 *            inherited exactly; min/max read live from the Halloween
 *            config through the registry's paths)
 *   fire   = only when ScareGate says FIRE, re-checked at fire time
 *            against the plugin's live global state and isEligible()
 *   effect = halloween.triggerEvent(event, player, console) — the admin
 *            command's own path; stats and protections intact
 *
 * A toggled-off feature is never scheduled; toggling off cancels a
 * pending chain. Toggling on starts one. A periodic sweep (wired in the
 * main class) re-ensures chains for online players, which is how a
 * global /halloween on resumes scares for already-online players.
 */
public final class ScareScheduler implements Listener {

    private final ExcelHalloweenToggles plugin;
    private final HalloweenPlugin halloween;
    private final FeatureRegistry registry;
    private final ToggleStore store;
    private final Map<UUID, Map<String, BukkitTask>> chains = new HashMap<>();

    public ScareScheduler(ExcelHalloweenToggles plugin, HalloweenPlugin halloween,
                          FeatureRegistry registry, ToggleStore store) {
        this.plugin = plugin;
        this.halloween = halloween;
        this.registry = registry;
        this.store = store;
    }

    /** Join starts the player's chains at once (the sweep is only a
     *  safety net); quit tears them down. */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        ensurePlayer(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        stopPlayer(event.getPlayer().getUniqueId());
    }

    /** Start chains for every ambient feature this player has ON. */
    public synchronized void ensurePlayer(Player player) {
        if (player == null) {
            return;
        }
        for (FeatureDef f : registry.ambient()) {
            ensureFeature(player, f);
        }
    }

    /** Reconcile one feature's chain with the player's toggle. */
    public synchronized void ensureFeature(Player player, FeatureDef feature) {
        if (player == null || feature == null || feature.isLogin()) {
            return;
        }
        if (!plugin.isSchedulerEnabled() || !plugin.isEventActive()
                || !store.isEnabled(player.getUniqueId(), feature.key())) {
            cancel(player.getUniqueId(), feature.key());
            return;
        }
        Map<String, BukkitTask> mine = chains.computeIfAbsent(player.getUniqueId(), u -> new HashMap<>());
        if (!mine.containsKey(feature.key())) {
            schedule(player, feature, freshDelay(player, feature));
        }
    }

    public synchronized void stopPlayer(UUID uuid) {
        Map<String, BukkitTask> mine = chains.remove(uuid);
        if (mine != null) {
            mine.values().forEach(BukkitTask::cancel);
        }
    }

    public synchronized void stopAll() {
        chains.values().forEach(m -> m.values().forEach(BukkitTask::cancel));
        chains.clear();
    }

    private void cancel(UUID uuid, String featureKey) {
        Map<String, BukkitTask> mine = chains.get(uuid);
        if (mine != null) {
            BukkitTask t = mine.remove(featureKey);
            if (t != null) {
                t.cancel();
            }
            if (mine.isEmpty()) {
                chains.remove(uuid);
            }
        }
    }

    private long freshDelay(Player player, FeatureDef feature) {
        int min = halloween.getConfig().getInt(feature.minPath(), feature.fallbackMin());
        int max = halloween.getConfig().getInt(feature.maxPath(), feature.fallbackMax());
        long delay;
        try {
            delay = halloween.eventDelayTicks(player, min, max);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[ExcelHalloweenToggles] eventDelayTicks failed for "
                    + feature.key() + " (" + e.getMessage() + ") — using retry cadence.");
            return plugin.retryTicks();
        }
        if (delay <= 0L) {
            delay = plugin.retryTicks();
        }
        if (plugin.isDebug()) {
            plugin.getLogger().info("[ExcelHalloweenToggles] debug: " + feature.key() + " for "
                    + player.getName() + " in " + delay + " ticks (min " + min + ", max " + max + ")");
        }
        return delay;
    }

    private synchronized void schedule(Player player, FeatureDef feature, long delayTicks) {
        cancel(player.getUniqueId(), feature.key());
        UUID uuid = player.getUniqueId();
        String key = feature.key();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> fire(uuid, key), delayTicks);
        chains.computeIfAbsent(uuid, u -> new HashMap<>()).put(key, task);
    }

    private void fire(UUID uuid, String featureKey) {
        FeatureDef feature = registry.byKey(featureKey);
        Player player = Bukkit.getPlayer(uuid);
        synchronized (this) {
            Map<String, BukkitTask> mine = chains.get(uuid);
            if (mine != null) {
                mine.remove(featureKey);
            }
        }
        if (feature == null || player == null || !player.isOnline()) {
            stopPlayer(uuid);
            return;
        }
        boolean globalOn;
        boolean eligible;
        try {
            globalOn = halloween.getData().isGlobalEnabled();
            eligible = halloween.isEligible(player);
        } catch (RuntimeException e) {
            plugin.getLogger().warning("[ExcelHalloweenToggles] Halloween state check failed for "
                    + featureKey + " (" + e.getMessage() + ") — chain stopped.");
            return;
        }
        ScareGate.Decision decision = ScareGate.decide(
                plugin.isEventActive(),
                plugin.isSchedulerEnabled(),
                globalOn,
                store.isEnabled(uuid, featureKey),
                eligible);
        switch (decision) {
            case FIRE -> {
                boolean fired;
                try {
                    fired = halloween.triggerEvent(feature.halloweenEvent(), player, Bukkit.getConsoleSender());
                } catch (RuntimeException e) {
                    plugin.getLogger().warning("[ExcelHalloweenToggles] triggerEvent("
                            + feature.halloweenEvent() + ") threw (" + e.getMessage() + ") — chain stopped.");
                    return;
                }
                if (plugin.isDebug()) {
                    plugin.getLogger().info("[ExcelHalloweenToggles] debug: fired " + featureKey
                            + " at " + player.getName() + " (plugin accepted: " + fired + ")");
                }
                synchronized (this) {
                    schedule(player, feature, freshDelay(player, feature));
                }
            }
            case RETRY -> {
                synchronized (this) {
                    schedule(player, feature, plugin.retryTicks());
                }
            }
            case STOP -> {
                // Chain ends: toggle off / retired / scheduler disabled.
            }
        }
    }
}
