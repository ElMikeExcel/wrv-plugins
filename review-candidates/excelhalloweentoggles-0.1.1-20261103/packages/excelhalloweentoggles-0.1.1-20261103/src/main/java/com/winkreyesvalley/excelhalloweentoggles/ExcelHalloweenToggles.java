package com.winkreyesvalley.excelhalloweentoggles;

import cz.softici.server.minecraft.HalloweenPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * ExcelHalloweenToggles — per-player Halloween scare toggles.
 *
 * Companion to Halloween_Softici 3.4.0 (Softici). Mike's call, Oct 31,
 * 2026: "Each player picks their own scares." The Halloween plugin's
 * only per-player switch is all-or-nothing (/halloween disable); this
 * companion adds the seven honest per-feature gates the design proved
 * out (login pumpkin + the six ambient scares) via a scheduler
 * takeover: the plugin's own chains are config-dead after the install
 * packet's 7-flag surgery, and ScareScheduler re-drives the same
 * scares through the plugin's public triggerEvent / eventDelayTicks /
 * isEligible surface, gated on each player's own toggles.
 *
 * Nothing outside the seven can be gated per player without faking it,
 * so nothing else is offered (design §4). Defaults are all ON.
 *
 * Retirement: the companion stops scheduling on its own after the
 * configured end date (2026-11-05, America/Chicago) and goes inert; if
 * Halloween_Softici is ever absent it disables itself with one console
 * line, so a forgotten jar after the event is harmless.
 */
public final class ExcelHalloweenToggles extends JavaPlugin {

    private HalloweenPlugin halloween;
    private FeatureRegistry registry;
    private ToggleStore store;
    private ScareScheduler scheduler;
    private JoinPumpkinDriver loginDriver;
    private ScaresPlaceholders placeholders;
    private BukkitTask sweepTask;

    private LocalDate endDate;
    private ZoneId zone;
    private boolean schedulerEnabled;
    private boolean debug;
    private long retryTicks;
    private long joinDelayTicks;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        Plugin hw = Bukkit.getPluginManager().getPlugin("Halloween_Softici");
        if (hw == null || !hw.isEnabled() || !(hw instanceof HalloweenPlugin)) {
            getLogger().info("[ExcelHalloweenToggles] Halloween_Softici is not present — "
                    + "ExcelHalloweenToggles is inert without the Halloween event and disables itself.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        halloween = (HalloweenPlugin) hw;

        try {
            endDate = LocalDate.parse(getConfig().getString("event.end-date", "2026-11-05").trim());
            zone = ZoneId.of(getConfig().getString("event.timezone", "America/Chicago").trim());
        } catch (RuntimeException e) {
            getLogger().severe("[ExcelHalloweenToggles] Bad event window config (" + e.getMessage()
                    + ") — disabling rather than guessing the retirement date.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        schedulerEnabled = getConfig().getBoolean("scheduler.enabled", true);
        debug = getConfig().getBoolean("debug", false);
        retryTicks = Math.max(20L, getConfig().getLong("scheduler.retry-seconds", 60L) * 20L);
        joinDelayTicks = Math.max(0L, getConfig().getLong("scheduler.join-delay-ticks", 40L));

        try {
            registry = FeatureRegistry.fromConfig(getConfig());
        } catch (IllegalArgumentException e) {
            getLogger().severe("[ExcelHalloweenToggles] Feature registry invalid: " + e.getMessage()
                    + " — disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        store = new ToggleStore(new File(getDataFolder(), "players.yml"), registry, getLogger());
        scheduler = new ScareScheduler(this, halloween, registry, store);
        loginDriver = new JoinPumpkinDriver(this, halloween, registry, store);
        Bukkit.getPluginManager().registerEvents(loginDriver, this);
        Bukkit.getPluginManager().registerEvents(scheduler, this);

        PluginCommand cmd = getCommand("scares");
        if (cmd == null) {
            getLogger().severe("[ExcelHalloweenToggles] /scares command missing from plugin.yml — disabling.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        ScaresCommand executor = new ScaresCommand(this, halloween, registry, store, scheduler, loginDriver);
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
        // 0.1.1: the tappable surfaces (Bedrock form + Java chest GUI).
        // The GUI flips through the command's own flip path; the form
        // is driven from the command via the mirrored GeyserForms helper.
        ScaresGui scaresGui = new ScaresGui(this, registry, store, executor::flipFeature);
        executor.setGui(scaresGui);
        Bukkit.getPluginManager().registerEvents(scaresGui, this);

        checkTriggerableSurface();
        checkSurgeryState();

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            try {
                placeholders = new ScaresPlaceholders(getDescription().getVersion(), registry, store);
                placeholders.register();
            } catch (Throwable t) {
                getLogger().warning("[ExcelHalloweenToggles] PlaceholderAPI bridge failed ("
                        + t.getMessage() + ") — menu state labels will show unavailable; /scares unaffected.");
                placeholders = null;
            }
        }

        if (schedulerEnabled) {
            long sweepTicks = Math.max(100L, getConfig().getLong("scheduler.sweep-seconds", 60L) * 20L);
            sweepTask = Bukkit.getScheduler().runTaskTimer(this, () -> {
                if (!isEventActive()) {
                    scheduler.stopAll();
                    return;
                }
                for (Player p : Bukkit.getOnlinePlayers()) {
                    scheduler.ensurePlayer(p);
                }
            }, sweepTicks, sweepTicks);
        }

        getLogger().info("[ExcelHalloweenToggles] ExcelHalloweenToggles " + getDescription().getVersion()
                + " awake: " + registry.all().size() + " per-player toggles ("
                + registry.ambient().size() + " ambient scares under scheduler takeover"
                + (registry.loginFeature() != null ? " + login pumpkin" : "")
                + "); event window through " + endDate + " (" + zone + "), then auto-retired.");
    }

    @Override
    public void onDisable() {
        if (sweepTask != null) {
            sweepTask.cancel();
            sweepTask = null;
        }
        if (scheduler != null) {
            scheduler.stopAll();
        }
        if (store != null) {
            store.save();
        }
        if (placeholders != null) {
            try {
                placeholders.unregister();
            } catch (Throwable ignored) {
                // PlaceholderAPI already gone — nothing to unregister from.
            }
            placeholders = null;
        }
    }

    /** Cross-check the registry against the live jar's own trigger list. */
    private void checkTriggerableSurface() {
        try {
            List<String> triggerable = HalloweenPlugin.TRIGGERABLE;
            for (FeatureDef f : registry.ambient()) {
                if (triggerable == null || !triggerable.contains(f.halloweenEvent())) {
                    getLogger().warning("[ExcelHalloweenToggles] Halloween's TRIGGERABLE list does not contain '"
                            + f.halloweenEvent() + "' — the " + f.key() + " toggle can gate scheduling "
                            + "but its fires may be declined by the plugin. Check the Halloween version.");
                }
            }
        } catch (Throwable t) {
            getLogger().warning("[ExcelHalloweenToggles] Could not read Halloween TRIGGERABLE ("
                    + t.getMessage() + ") — skipping the surface cross-check.");
        }
    }

    /**
     * The takeover only works once the install packet's surgery has set
     * the seven Halloween flags false. If any is still true, the
     * plugin's own chain for that scare is still live and no toggle can
     * gate it — say so in the log, by name, at every boot.
     */
    private void checkSurgeryState() {
        List<String> stillOn = new ArrayList<>();
        for (FeatureDef f : registry.all()) {
            if (halloween.getConfig().getBoolean(f.enabledPath(), false)) {
                stillOn.add(f.enabledPath());
            }
        }
        if (!stillOn.isEmpty()) {
            getLogger().warning("[ExcelHalloweenToggles] SURGERY NOT APPLIED: these Halloween config flags "
                    + "are still true — " + String.join(", ", stillOn)
                    + ". The Halloween plugin's own chains for those scares are still live and per-player "
                    + "toggles cannot gate them until the install packet's config surgery sets them false.");
        }
    }

    // ---- shared state for the scheduler / driver / command ----

    public boolean isEventActive() {
        return endDate != null && EventWindow.isActiveAt(Instant.now(), zone, endDate);
    }

    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }

    public boolean isDebug() {
        return debug;
    }

    public long retryTicks() {
        return retryTicks;
    }

    public long joinDelayTicks() {
        return joinDelayTicks;
    }

    /** Config-driven, color-translated message text. */
    public String text(String path, String fallback) {
        String raw = getConfig().getString(path, fallback);
        return ChatColor.translateAlternateColorCodes('&', raw == null ? fallback : raw);
    }
}
