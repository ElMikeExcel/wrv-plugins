package com.winkreyes.excelwitherskulls;

import com.winkreyes.excelwitherskulls.core.SkullMath;
import com.winkreyes.excelwitherskulls.core.SkullSettings;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelWitherSkulls — Wink Reyes Valley (Excel signature).
 *
 * <p>The trimmed rewards package's skull piece (Mike's approval,
 * Oct 21, 2026; Jester's feedback was the trigger): an ELITE
 * wither skeleton that dies to a player kill gets its skull
 * chance topped up toward a configured target (default 25%).
 * EliteMobs never touches a natural elite's vanilla drops on this
 * server — no suppression, but no bonus either — so an elite
 * wither skeleton pays exactly a regular one's 2.5% skull chance
 * (audit: ~/workspace/elitemobs-feedback-2026-10-21). This plugin
 * is the bonus, and nothing else: it reads EliteMobs' own tagger
 * to know an elite, reads the final vanilla drops list, and when
 * vanilla's roll left no skull it rolls the difference and adds
 * exactly one. Regular wither skeletons, non-player kills and
 * every other drop are never touched, EliteMobs' item system is
 * never touched (its drops stay OFF until Seal 5, per Mike), and
 * the plugin only ever ADDS — a target below vanilla's effective
 * chance for a kill makes it do nothing.
 */
public final class ExcelWitherSkulls extends JavaPlugin {

    public static final String PERM_ADMIN = "excelwitherskulls.admin";

    private volatile SkullSettings settings = SkullSettings.defaults();
    private EmGateway gateway;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = loadSettings();
        gateway = new EmGateway(getLogger());
        getServer().getPluginManager().registerEvents(new SkullListener(this, gateway), this);
        WitherSkullsCommand cmd = new WitherSkullsCommand(this);
        if (getCommand("witherskulls") != null) {
            getCommand("witherskulls").setExecutor(cmd);
            getCommand("witherskulls").setTabCompleter(cmd);
        }
        // Probe the seam once so a missing EliteMobs is loud at
        // boot, not discovered at the first fortress run.
        boolean seam = gateway.available();
        getLogger().info("ExcelWitherSkulls enabled: "
                + (settings.enabled() ? "top-up ON" : "top-up DISABLED by config")
                + ", target skull chance " + settings.targetChancePercent() + "%"
                + " (realized " + String.format("%.2f",
                        SkullMath.realizedTotalPercent(settings.targetChancePercent(), 0))
                + "% with no Looting)"
                + ", EliteMobs seam " + (seam ? "BOUND" : "UNAVAILABLE (top-up inert)"));
    }

    /** The live settings snapshot (swapped whole on reload). */
    public SkullSettings settings() {
        return settings;
    }

    /**
     * Re-read config.yml and swap the snapshot. A broken file
     * keeps the previous snapshot — a reload can never wedge the
     * plugin or throw into the command.
     */
    public synchronized boolean reloadAll() {
        try {
            reloadConfig();
            settings = loadSettings();
            return true;
        } catch (Throwable t) {
            getLogger().warning("ExcelWitherSkulls: config reload failed (" + t
                    + ") - keeping the previous settings");
            return false;
        }
    }

    boolean seamAvailable() {
        return gateway != null && gateway.available();
    }

    private SkullSettings loadSettings() {
        Boolean enabled = getConfig().contains("enabled")
                ? getConfig().getBoolean("enabled") : null;
        Double target = getConfig().contains("target-chance-percent")
                ? getConfig().getDouble("target-chance-percent") : null;
        Boolean debug = getConfig().contains("debug-log")
                ? getConfig().getBoolean("debug-log") : null;
        return SkullSettings.of(enabled, target, debug);
    }
}
