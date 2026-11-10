package com.winkreyes.exceldetails;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

/**
 * ExcelDetails — the Details-for-Minecraft damage meter. Live sidebar
 * sections (damage done / damage taken / healing done, any mix the
 * player ticks) over a shared encounter segment, piggybacking
 * EliteMobs' party sidebar at negative scores when EM manages the
 * player's board. 0.2.1: the shared segment is DISPLAY-scoped per
 * viewer — you see your own rows plus your party's (ExcelTogether
 * and EliteMobs parties both count); see PartyScope / PartyLookup.
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

        // 0.2.1: party scoping seams (soft, reflection, fail-soft) —
        // the sidebar shows a viewer only their own + party rows.
        renderer = new SidebarRenderer(tracker, prefs, new PartyLookup(getLogger()), maxRows);
        loadPrefs();

        DetailsCommands commands = new DetailsCommands(this);
        getCommand("details").setExecutor(commands);
        getCommand("details").setTabCompleter(commands);
        getServer().getPluginManager().registerEvents(new DetailsListener(this), this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DetailsPlaceholders(prefs).register();
            getLogger().info("ExcelDetails: PlaceholderAPI expansion registered (%details_show_damage%, %details_show_taken%, %details_show_healing%).");
        }

        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            boolean ended = tracker.tick(now, timeoutMs);
            if (ended) {
                // 0.2.0: merged overall totals persist at every close,
                // and only a real fight announces (churn fix — micro
                // segments and post-clear empty closes stay silent).
                if (tracker.lastCloseAnnounceable() && getConfig().getBoolean("announce-segments", true)) {
                    getLogger().info("ExcelDetails: segment closed ("
                            + MeterMath.clock(tracker.elapsedMs(now)) + "), totals merged into overall.");
                }
                savePrefs();
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

    // ----- persistence (data.yml via DataFile: prefs + mix + overall totals) -----

    private void loadPrefs() {
        prefsFile = new File(getDataFolder(), "data.yml");
        DataFile.load(prefsFile, prefs, tracker);
    }

    public void savePrefs() {
        if (prefsFile == null) return;
        try {
            DataFile.save(prefsFile, prefs, tracker);
        } catch (IOException e) {
            getLogger().warning("ExcelDetails: could not save data.yml — " + e.getMessage());
        }
    }
}
