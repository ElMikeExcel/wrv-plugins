package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.Registry;
import com.winkreyes.excellandmarks.core.Settings;
import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.hook.GpAdapter;
import com.winkreyes.excellandmarks.hook.ProtectAttribution;
import com.winkreyes.excellandmarks.menu.FormPort;
import com.winkreyes.excellandmarks.menu.Menus;
import com.winkreyes.excellandmarks.service.LandmarkService;
import com.winkreyes.excellandmarks.store.Store;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

/**
 * ExcelLandmarks 0.1.0 — spawn-build naming / rent / buy (SPEC.md).
 * Sign-registered spots, rent-or-buy through the menu, GP subdivisions
 * for plots, and the Protect-attribution lapse finalize with a
 * never-expiring redemption box.
 */
public final class ExcelLandmarks extends JavaPlugin {

    private Settings settings;
    private final Registry registry = new Registry();
    private Store store;
    private LandmarkService service;
    private Menus menus;
    private Economy economy;
    private GpAdapter gp = GpAdapter.NONE;
    private ProtectAttribution protect;
    private FormPort forms = FormPort.NONE;

    public Settings settings() { return settings; }
    public Registry registry() { return registry; }
    public Store store() { return store; }
    public LandmarkService service() { return service; }
    public Menus menus() { return menus; }
    public Economy economy() { return economy; }
    public GpAdapter gp() { return gp; }
    public ProtectAttribution protect() { return protect; }
    public FormPort forms() { return forms; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = loadSettings(getConfig());
        store = new Store(getDataFolder(), getLogger());
        store.loadSpots(registry);
        store.loadRedemptions();

        // Economy (Vault is a hard depend; tolerate absence gracefully)
        try {
            RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        } catch (Throwable t) {
            getLogger().warning("ExcelLandmarks: Vault economy lookup failed: " + t);
        }
        if (economy == null) getLogger().warning("ExcelLandmarks: no Vault economy provider — purchases will refuse safely.");

        // GriefPrevention adapter — fail-soft by construction
        gp = initGp();
        // ExcelProtect attribution source — read-only, fail-soft
        // logs-dir is relative to THIS plugin's data folder ("../ExcelProtect"
        // = the sibling Protect folder inside plugins/) — rig-caught Nov 5, 2026:
        // passing the plugins folder itself resolved one level too high.
        protect = new ProtectAttribution(getDataFolder(), settings.protectLogsDir, getLogger());
        // Floodgate forms — guarded so a missing plugin is a no-op (Java inventories still work)
        forms = initForms();

        service = new LandmarkService(this);
        menus = new Menus(this);
        getServer().getPluginManager().registerEvents(new SignListener(this), this);
        getServer().getPluginManager().registerEvents(menus, this);
        LandmarksCommand cmd = new LandmarksCommand(this);
        if (getCommand("landmarks") != null) getCommand("landmarks").setExecutor(cmd);

        // Rent clock: periodic sweep + an immediate catch-up pass
        long tickMs = Math.max(1, settings.clockTickMinutes) * 60L * 20L;
        getServer().getScheduler().runTaskTimer(this, () -> service.sweepClock(), tickMs, tickMs);
        service.sweepClock();
        for (Spot s : registry.all()) service.refreshSign(s);

        getLogger().info("ExcelLandmarks " + getDescription().getVersion() + " enabled: spots=" + registry.size()
                + ", gp=" + (gp.available() ? "ON" : "OFF") + " (subdivisions)"
                + ", protect-attribution=" + (protect.available() ? "ON" : "OFF")
                + ", floodgate-forms=" + (forms.available() ? "ON" : "OFF")
                + ", rent-period=" + settings.rentPeriodDays + "d, grace=" + settings.graceDays + "d");
    }

    @Override
    public void onDisable() {
        if (store != null) {
            store.saveSpots(registry);
            store.saveRedemptions();
        }
    }

    public void reloadAll() {
        reloadConfig();
        settings = loadSettings(getConfig());
        // logs-dir is relative to THIS plugin's data folder ("../ExcelProtect"
        // = the sibling Protect folder inside plugins/) — rig-caught Nov 5, 2026:
        // passing the plugins folder itself resolved one level too high.
        protect = new ProtectAttribution(getDataFolder(), settings.protectLogsDir, getLogger());
        for (Spot s : registry.all()) service.refreshSign(s);
    }

    private Settings loadSettings(FileConfiguration c) {
        Settings s = new Settings();
        for (Spot.Tier t : Spot.Tier.values()) {
            String base = "tiers." + t.name().toLowerCase();
            s.rentCents.put(t, Math.round(c.getDouble(base + ".rent", s.rentCents.get(t) / 100.0) * 100.0));
            s.buyCents.put(t, Math.round(c.getDouble(base + ".buy", s.buyCents.get(t) / 100.0) * 100.0));
            s.plotSizes.put(t, c.getInt(base + ".plot-size", s.plotSizes.get(t)));
        }
        try {
            s.defaultType = Spot.Type.valueOf(c.getString("defaults.type", "LANDMARK").toUpperCase());
        } catch (IllegalArgumentException ignored) { }
        try {
            s.defaultTier = Spot.Tier.valueOf(c.getString("defaults.tier", "SPOT").toUpperCase());
        } catch (IllegalArgumentException ignored) { }
        s.rentPeriodDays = c.getInt("rent-period-days", 30);
        s.graceDays = c.getInt("grace-days", 7);
        s.clockTickMinutes = c.getInt("clock-tick-minutes", 15);
        s.plotYBelow = c.getInt("plot.y-below", 3);
        s.plotYAbove = c.getInt("plot.y-above", 12);
        s.redemptionSearchRadius = c.getInt("redemption.search-radius", 3);
        s.protectLogsDir = c.getString("protect.logs-dir", "../ExcelProtect");
        return s;
    }

    private GpAdapter initGp() {
        try {
            if (getServer().getPluginManager().getPlugin("GriefPrevention") == null) return GpAdapter.NONE;
            Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object hook = Class.forName("com.winkreyes.excellandmarks.hook.GpHook")
                    .getConstructor(java.util.logging.Logger.class).newInstance(getLogger());
            return (GpAdapter) hook;
        } catch (Throwable t) {
            getLogger().warning("ExcelLandmarks: GriefPrevention present but its API could not be linked (" + t
                    + ") — naming works, plot build rights do not grant.");
            return GpAdapter.NONE;
        }
    }

    private FormPort initForms() {
        try {
            if (getServer().getPluginManager().getPlugin("floodgate") == null) return FormPort.NONE;
            Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object port = Class.forName("com.winkreyes.excellandmarks.menu.FloodgateForms")
                    .getConstructor(java.util.logging.Logger.class).newInstance(getLogger());
            return (FormPort) port;
        } catch (Throwable t) {
            getLogger().warning("ExcelLandmarks: Floodgate present but the forms API could not be linked (" + t
                    + ") — Bedrock players get the inventory menu instead.");
            return FormPort.NONE;
        }
    }
}
