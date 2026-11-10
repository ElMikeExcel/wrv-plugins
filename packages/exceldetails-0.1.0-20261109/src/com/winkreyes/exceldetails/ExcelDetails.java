package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * ExcelDetails — the Details-for-Minecraft damage meter. Live sidebar
 * windows (damage done / damage taken / healing done) over a shared
 * encounter segment, piggybacking EliteMobs' party sidebar at
 * negative scores when EM manages the player's board.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelDetails extends JavaPlugin {

    private final SegmentTracker tracker = new SegmentTracker();
    private final PlayerPrefs prefs = new PlayerPrefs();
    private SidebarRenderer renderer;
    private File prefsFile;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        int maxRows = Math.max(1, Math.min(12, getConfig().getInt("max-rows", 6)));
        long timeoutMs = Math.max(1, getConfig().getInt("combat-timeout-seconds", 8)) * 1000L;
        int refreshTicks = Math.max(2, getConfig().getInt("refresh-ticks", 10));

        renderer = new SidebarRenderer(tracker, prefs, maxRows);
        loadPrefs();

        DetailsCommands commands = new DetailsCommands(this);
        getCommand("details").setExecutor(commands);
        getCommand("details").setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new DetailsListener(this), this);

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            boolean ended = tracker.tick(now, timeoutMs);
            if (ended && getConfig().getBoolean("announce-segments", true)) {
                getLogger().info("ExcelDetails: segment closed ("
                        + MeterMath.clock(tracker.elapsedMs(now)) + "), totals merged into overall.");
            }
            for (var player : Bukkit.getOnlinePlayers()) {
                renderer.render(player, now);
            }
        }, refreshTicks, refreshTicks);

        getLogger().info("ExcelDetails READY version=" + getDescription().getVersion()
                + " build=" + BuildInfo.BUILD_ID
                + " refresh=" + refreshTicks + "t timeout=" + (timeoutMs / 1000) + "s rows=" + maxRows);
    }

    @Override
    public void onDisable() {
        for (var player : Bukkit.getOnlinePlayers()) {
            renderer.forget(player);
        }
        savePrefs();
    }

    public SegmentTracker tracker() { return tracker; }
    public PlayerPrefs prefs() { return prefs; }
    public SidebarRenderer renderer() { return renderer; }

    // ----- prefs persistence (data.yml: toggles + windows only; segments are live instruments) -----

    private void loadPrefs() {
        prefsFile = new File(getDataFolder(), "data.yml");
        if (!prefsFile.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(prefsFile);
        if (yml.isConfigurationSection("players")) {
            for (String key : yml.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID id = UUID.fromString(key);
                    prefs.setEnabled(id, yml.getBoolean("players." + key + ".enabled", true));
                    CombatStats.Window w = CombatStats.Window.parse(yml.getString("players." + key + ".window"));
                    if (w != null) prefs.setWindow(id, w);
                } catch (IllegalArgumentException ignored) {
                    // a hand-edited bad UUID line is skipped, never fatal
                }
            }
        }
    }

    public void savePrefs() {
        if (prefsFile == null) return;
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("exceldetails-build", BuildInfo.BUILD_ID);
        prefs.enabledSnapshot().forEach((id, on) -> yml.set("players." + id + ".enabled", on));
        prefs.windowSnapshot().forEach((id, w) -> yml.set("players." + id + ".window", w.name()));
        try {
            yml.save(prefsFile);
        } catch (IOException e) {
            getLogger().warning("ExcelDetails: could not save data.yml — " + e.getMessage());
        }
    }
}
