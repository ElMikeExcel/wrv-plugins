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
 *
 * D251 tuning (v29, Nov 26, 2026): the arrival heal is now
 * conditional. An elite that took player damage during the
 * episode does NOT heal on arrival — it comes home at its
 * current HP and recovers only via the standing out-of-combat
 * regen (D184); the full heal survives only for genuinely
 * untouched returns. This kills the teleport-heal cycle Jester
 * reported (spell damage teleports an Enderman-type boss past
 * its leash, and the return used to reset the fight for free).
 * Config: no-return-heal-after-player-damage (default true),
 * optionally scoped by no-return-heal-boss-files.
 *
 * Combat-end tuning (v30, Nov 27, 2026): the v29 live proof
 * found the second heal path in the same report — EliteMobs
 * heals a custom boss to full the instant combat ends (its
 * per-elite watchdog fires EliteMobExitCombatEvent when no
 * player is within the boss's follow range, and the event's
 * constructor full-heals under MobCombatSettings'
 * regenerateCustomBossHealthOnCombatEnd), wherever the boss
 * stands, leash or no leash. The bridge's heal guard now also
 * vetoes that reset heal for combat-touched elites (see
 * ReturnPolicy.shouldVetoCombatEndHeal for the exact match).
 * Config: no-combat-end-heal-after-player-damage (default
 * true), optionally scoped by no-combat-end-heal-boss-files.
 */
public final class LeashModule implements FixModule {

    /** Immutable snapshot of this module's config, taken at enable. */
    public static final class Settings {
        public final double leashRange;
        public final boolean debug;
        /** D251: suppress the arrival heal for combat-touched returns. */
        public final boolean noReturnHealAfterPlayerDamage;
        /** D251 scope: empty = every boss; else exact boss filenames. */
        public final java.util.List<String> noReturnHealBossFiles;
        /** v30: veto EM's combat-end reset heal for combat-touched elites. */
        public final boolean noCombatEndHealAfterPlayerDamage;
        /** v30 scope: empty = every boss; else exact boss filenames. */
        public final java.util.List<String> noCombatEndHealBossFiles;

        Settings(double leashRange, boolean debug, boolean noReturnHealAfterPlayerDamage,
                 java.util.List<String> noReturnHealBossFiles,
                 boolean noCombatEndHealAfterPlayerDamage,
                 java.util.List<String> noCombatEndHealBossFiles) {
            this.leashRange = leashRange;
            this.debug = debug;
            this.noReturnHealAfterPlayerDamage = noReturnHealAfterPlayerDamage;
            this.noReturnHealBossFiles = noReturnHealBossFiles;
            this.noCombatEndHealAfterPlayerDamage = noCombatEndHealAfterPlayerDamage;
            this.noCombatEndHealBossFiles = noCombatEndHealBossFiles;
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
        if (settings.noReturnHealAfterPlayerDamage) {
            ctx.logger().info("Leash return-heal tuning on (D251): an elite that took player "
                    + "damage keeps its current HP on return — no heal; scope: "
                    + (settings.noReturnHealBossFiles.isEmpty()
                        ? "all bosses." : settings.noReturnHealBossFiles.size() + " boss file(s)."));
        }
        if (settings.noCombatEndHealAfterPlayerDamage) {
            ctx.logger().info("Leash combat-end heal tuning on (v30): an elite that took player "
                    + "damage does not heal when EliteMobs ends its combat — no reset; scope: "
                    + (settings.noCombatEndHealBossFiles.isEmpty()
                        ? "all bosses." : settings.noCombatEndHealBossFiles.size() + " boss file(s)."));
        }
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
        // D251 tuning: shipped ON (it is the fix); an owner who
        // wants the classic D183 heal-every-return sets it false.
        boolean noHeal = cfg == null
                || cfg.getBoolean("no-return-heal-after-player-damage", true);
        java.util.List<String> scope = new java.util.ArrayList<>();
        if (cfg != null) {
            for (String name : cfg.getStringList("no-return-heal-boss-files")) {
                if (name != null && !name.trim().isEmpty()) scope.add(name.trim());
            }
        }
        // v30 combat-end tuning: shipped ON (it is the fix); an
        // owner who wants EM's stock combat-end reset heal sets
        // it false.
        boolean noCombatEndHeal = cfg == null
                || cfg.getBoolean("no-combat-end-heal-after-player-damage", true);
        java.util.List<String> combatEndScope = new java.util.ArrayList<>();
        if (cfg != null) {
            for (String name : cfg.getStringList("no-combat-end-heal-boss-files")) {
                if (name != null && !name.trim().isEmpty()) combatEndScope.add(name.trim());
            }
        }
        return new Settings(range, debug, noHeal, java.util.List.copyOf(scope),
                noCombatEndHeal, java.util.List.copyOf(combatEndScope));
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
