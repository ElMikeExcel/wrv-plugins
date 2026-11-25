package excelemfixes.leash;

import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.HandlerList;
import org.bukkit.scheduler.BukkitTask;

/**
 * Leash module — the MMO-style leash (design D183), folded in from
 * the standalone ExcelEMLeash 0.3.0 (same code, module glue).
 *
 * The rule (Mike's pick, Nov 21, 2026, range 96 blocks): an elite
 * has a home — its spawn point — and fights normally anywhere
 * within the leash range of it. Pulled beyond the range it
 * deaggros, heads home, and heals to full when it arrives; player
 * damage mid-return re-aggroes it instantly and the heal never
 * happens. Aggro still ends on death or on leaving the
 * world/dimension.
 *
 * Two legs, both in EmLeashBridge (all EliteMobs imports live
 * there; this class is loaded only with EliteMobs present):
 *  - Native-leash suppression: every loaded boss's in-memory
 *    leashRadius is set to 0, so EM's own leash can never engage
 *    underneath ours — its soft return would start at HALF the
 *    radius (mid-camp-fight), its returning bosses are
 *    damage-invulnerable (so the mid-return re-aggro could never
 *    happen), and it never clears the target.
 *  - The return engine: a 10-tick poll over EM's tracked regional
 *    bosses runs the state machine — trigger past the range,
 *    deaggro via EM's own clear calls, pathfinder walk home with
 *    teleport fallback, heal-event guard so no heal can land
 *    mid-return, heal on arrival via EM's fullHeal().
 */
public final class LeashModule implements FixModule {

    /** Immutable snapshot of this module's config, taken at enable. */
    public static final class Settings {
        public final double leashRange;
        public final boolean debug;

        Settings(double leashRange, boolean debug) {
            this.leashRange = leashRange;
            this.debug = debug;
        }
    }

    private ModuleContext ctx;
    private Settings settings;
    private EmLeashBridge bridge;
    private BukkitTask suppressionTask;
    private BukkitTask pollTask;

    @Override
    public String id() {
        return "leash";
    }

    @Override
    public String displayName() {
        return "MMO Leash";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        settings = loadSettings();
        if (Bukkit.getPluginManager().getPlugin("EliteMobs") == null) {
            ctx.logger().warning("Leash: EliteMobs not found — module inert (the leash needs EliteMobs).");
            return;
        }
        try {
            bridge = new EmLeashBridge(this, settings);
        } catch (Throwable t) {
            ctx.logger().severe("Leash: failed to link EliteMobs API — module inert. " + t);
            return;
        }
        Bukkit.getPluginManager().registerEvents(bridge, ctx.plugin());
        // Suppression: first apply 20 ticks after enable
        // (belt-and-braces: EM's registry is populated in its own
        // onEnable, which softdepend ordering already guarantees
        // runs before ours), then a 5-minute self-heal re-apply.
        // EM 10.9.14 exposes no load/reload event, so the repeating
        // pass is what makes the suppression survive /em reloads and
        // pack updates: it only writes when a boss still carries a
        // native leash radius.
        suppressionTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), () -> {
            try {
                bridge.applyLeashRemoval();
            } catch (Throwable t) {
                ctx.logger().warning("Leash: native-leash suppression apply failed: " + t);
            }
        }, 20L, 6000L);
        // Return engine: 10-tick poll over EM's tracked elites.
        pollTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), () -> {
            try {
                bridge.pollLeashes();
            } catch (Throwable t) {
                ctx.logger().warning("Leash: leash poll failed: " + t);
            }
        }, 20L, 10L);
        ctx.logger().info("Leash ready — MMO leash on: deaggro + return + heal past "
                + trim(settings.leashRange) + " blocks from spawn (D183).");
    }

    @Override
    public void onDisable() {
        if (suppressionTask != null) {
            suppressionTask.cancel();
            suppressionTask = null;
        }
        if (pollTask != null) {
            pollTask.cancel();
            pollTask = null;
        }
        if (bridge != null) {
            HandlerList.unregisterAll(bridge);
            bridge = null;
        }
    }

    private Settings loadSettings() {
        ConfigurationSection cfg = ctx.moduleConfig();
        double range = cfg == null ? 96.0 : cfg.getDouble("range", 96.0);
        if (!(range > 0)) {
            ctx.logger().warning("Leash: range " + range + " is not positive — using 96.");
            range = 96.0;
        }
        boolean debug = cfg != null && cfg.getBoolean("debug", false);
        return new Settings(range, debug);
    }

    static String trim(double value) {
        return value == Math.rint(value) ? Long.toString(Math.round(value)) : Double.toString(value);
    }

    /** Bridge logging access (the bridge predates the module glue). */
    public Logger logger() {
        return ctx.logger();
    }

    public void debug(String message) {
        if (settings != null && settings.debug) ctx.logger().info("[debug] " + message);
    }
}
