package com.winkreyes.excelemleash;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelEMLeash — the MMO-style leash (D183).
 *
 * Mike's ruling (Nov 21, 2026, range picked at 96 blocks): an elite
 * has a home — its spawn point — and fights normally anywhere
 * within the leash range of it. Pulled beyond the range it deaggros,
 * heads home, and heals to full when it arrives; player damage
 * mid-return re-aggroes it instantly and the heal never happens.
 * Aggro still ends on death or on leaving the world/dimension.
 *
 * Two legs, both in EmLeashBridge (all EliteMobs imports live
 * there; this class is loaded only with EliteMobs present):
 *  - Native-leash suppression (carried from 0.2.0 / D182): every
 *    loaded boss's in-memory leashRadius is set to 0, so EM's own
 *    leash can never engage underneath ours — its soft return would
 *    start at HALF the radius (48 at range 96, mid-camp-fight), its
 *    returning bosses are damage-invulnerable (so the spec's
 *    mid-return re-aggro could never happen), and it never clears
 *    the target (the original Jester loop).
 *  - The return engine (new in 0.3.0): a 10-tick poll over EM's
 *    tracked regional bosses runs the D183 state machine — trigger
 *    past the range, deaggro via EM's own two calls, pathfinder
 *    walk home with teleport fallback, heal-event guard so no heal
 *    can land mid-return, heal on arrival via EM's fullHeal().
 *
 * D183 supersedes D182 (reset removed entirely, 0.2.0) and D181;
 * Jester's original short-leash design stays retired — this leash
 * exists only at MMO distance.
 */
public final class ExcelEMLeash extends JavaPlugin {

    /** Immutable snapshot of this plugin's config, taken at enable. */
    public static final class Settings {
        public final boolean leashEnabled;
        public final double leashRange;
        public final boolean debug;

        Settings(boolean leashEnabled, double leashRange, boolean debug) {
            this.leashEnabled = leashEnabled;
            this.leashRange = leashRange;
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
            getLogger().warning("ExcelEMLeash: EliteMobs not found — plugin inert (the leash needs EliteMobs).");
            return;
        }
        try {
            bridge = new EmLeashBridge(this, settings);
        } catch (Throwable t) {
            getLogger().severe("ExcelEMLeash: failed to link EliteMobs API — plugin inert. " + t);
            return;
        }
        if (settings.leashEnabled) {
            getServer().getPluginManager().registerEvents(bridge, this);
            // Suppression: first apply 20 ticks after enable
            // (belt-and-braces: EM's registry is populated in its own
            // onEnable, which softdepend ordering already guarantees
            // runs before ours), then a 5-minute self-heal re-apply.
            // EM 10.9.14 exposes no load/reload event, so the
            // repeating pass is what makes the suppression survive
            // /em reloads and pack updates: it only writes when a
            // boss still carries a native leash radius.
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    bridge.applyLeashRemoval();
                } catch (Throwable t) {
                    getLogger().warning("ExcelEMLeash: native-leash suppression apply failed: " + t);
                }
            }, 20L, 6000L);
            // Return engine: 10-tick poll over EM's tracked elites.
            getServer().getScheduler().runTaskTimer(this, () -> {
                try {
                    bridge.pollLeashes();
                } catch (Throwable t) {
                    getLogger().warning("ExcelEMLeash: leash poll failed: " + t);
                }
            }, 20L, 10L);
        }
        getLogger().info("ExcelEMLeash 0.3.0 ready — MMO leash "
                + (settings.leashEnabled
                        ? "on: deaggro + return + heal past " + trim(settings.leashRange) + " blocks from spawn"
                        : "off")
                + " (D183).");
    }

    private Settings loadSettings() {
        // The leash defaults ON when its key is absent: a jar swap
        // onto a live 0.2.0 config (which has no leash section) must
        // still run the D183 leash. The 0.2.0 master switch is still
        // honored when present and false, so an operator who turned
        // the plugin off under 0.2.0 stays off.
        boolean enabled = getConfig().getBoolean("leash.enabled", true);
        if (!getConfig().getBoolean("leash-removal.enabled", true)) enabled = false;
        double range = getConfig().getDouble("leash.range", 96.0);
        if (!(range > 0)) {
            getLogger().warning("ExcelEMLeash: leash.range " + range + " is not positive — using 96.");
            range = 96.0;
        }
        boolean debug = getConfig().getBoolean("debug", true);
        return new Settings(enabled, range, debug);
    }

    static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    public void debug(String message) {
        if (settings != null && settings.debug) getLogger().info("[debug] " + message);
    }
}
