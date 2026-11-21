package com.winkreyes.excelemleash;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * ExcelEMLeash — the EliteMobs leash fix (D167).
 *
 * Leg A (aggro-clear): when a regional boss's leash return completes,
 * clear its target + EM combat memory so it stays home (healed, per
 * EM's design) instead of charging straight back out. EM exposes no
 * config for this; the handler lives in EmLeashBridge.
 *
 * Leg B (radii management): a floor + per-boss overrides from THIS
 * plugin's config, applied to EliteMobs' loaded boss configuration
 * objects at runtime (no boss files are edited by this plugin), so a
 * content-pack update cannot silently undo the floor.
 *
 * All EliteMobs imports are confined to EmLeashBridge, which is only
 * loaded when EliteMobs is present (softdepend).
 */
public final class ExcelEMLeash extends JavaPlugin {

    /** Immutable snapshot of this plugin's config, taken at enable. */
    public static final class Settings {
        public final boolean aggroClearEnabled;
        public final boolean radiiEnabled;
        public final double minimumRadius;
        public final Map<String, Double> overrides;
        public final boolean debug;

        Settings(boolean aggroClearEnabled, boolean radiiEnabled, double minimumRadius,
                 Map<String, Double> overrides, boolean debug) {
            this.aggroClearEnabled = aggroClearEnabled;
            this.radiiEnabled = radiiEnabled;
            this.minimumRadius = minimumRadius;
            this.overrides = overrides;
            this.debug = debug;
        }
    }

    private Settings settings;
    private EmLeashBridge bridge;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        settings = loadSettings();

        if (getServer().getPluginManager().getPlugin("EliteMobs") == null) {
            getLogger().warning("ExcelEMLeash: EliteMobs not found — plugin inert (both legs need EliteMobs).");
            return;
        }
        try {
            bridge = new EmLeashBridge(this, settings);
        } catch (Throwable t) {
            getLogger().severe("ExcelEMLeash: failed to link EliteMobs API — plugin inert. " + t);
            return;
        }
        if (settings.aggroClearEnabled) {
            getServer().getPluginManager().registerEvents(bridge, this);
        }
        if (settings.radiiEnabled) {
            // First apply 20 ticks after enable (belt-and-braces: EM's
            // registry is populated in its own onEnable, which
            // softdepend ordering already guarantees runs before ours),
            // then a 5-minute self-heal re-apply. EM 10.9.14 exposes no
            // load/reload event, so the repeating pass is what makes
            // the floor survive /em reloads and pack updates: it only
            // writes when a value differs from its computed target.
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    bridge.applyRadii();
                } catch (Throwable t) {
                    getLogger().warning("ExcelEMLeash: radii apply failed: " + t);
                }
            }, 20L, 6000L);
        }
        getLogger().info("ExcelEMLeash 0.1.0 ready — aggro-clear "
                + (settings.aggroClearEnabled ? "on" : "off") + ", radii floor "
                + (settings.radiiEnabled ? String.valueOf(settings.minimumRadius) : "off") + ".");
    }

    private Settings loadSettings() {
        boolean aggro = getConfig().getBoolean("aggro-clear.enabled", true);
        boolean radii = getConfig().getBoolean("radii.enabled", true);
        double floor = getConfig().getDouble("radii.minimum-radius", 35.0);
        boolean debug = getConfig().getBoolean("debug", true);
        Map<String, Double> overrides = new HashMap<>();
        ConfigurationSection section = getConfig().getConfigurationSection("radii.overrides");
        if (section != null) {
            // Deep keys, not shallow ones: Bukkit splits a key on '.',
            // so a quoted filename key like "feral_wolf.yml" arrives as
            // a "feral_wolf" section holding a "yml" leaf. getKeys(true)
            // rejoins the path segments with '.', which reconstructs
            // the original filename exactly.
            for (String key : section.getKeys(true)) {
                if (section.isDouble(key) || section.isInt(key)) {
                    overrides.put(key, section.getDouble(key));
                } else if (!section.isConfigurationSection(key)) {
                    getLogger().warning("ExcelEMLeash: ignoring non-numeric radii override for '" + key + "'.");
                }
            }
        }
        return new Settings(aggro, radii, floor, Collections.unmodifiableMap(overrides), debug);
    }

    public void debug(String message) {
        if (settings != null && settings.debug) getLogger().info("[debug] " + message);
    }
}
