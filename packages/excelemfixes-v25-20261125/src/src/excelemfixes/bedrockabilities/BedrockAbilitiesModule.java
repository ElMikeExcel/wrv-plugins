package excelemfixes.bedrockabilities;

import excelemfixes.ExcelEMFixes;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * The bedrock-abilities module (module id bedrock-abilities):
 * lets Bedrock players fire EliteMobs class abilities - the fold
 * of the standalone ExcelEMAbilityBedrockBridge 0.2.0 (live name
 * ExcelEliteMobsAbilityBedrockBridge) into this pack
 * (consolidation programme Wave 4, ruling D245, Nov 25, 2026).
 * Behavior is 0.2.0's, ported class for class; the /abilities
 * command name, its /classabilities alias, its status shape and
 * its exceleitemobsabilitybedrockbridge.admin permission node
 * are exactly the standalone's (frozen per programme gate 8).
 *
 * WHY THIS MODULE EXISTS (the REASON): an EliteMobs platform
 * gap - EM's only ACS ability input route is the Java F-key
 * gesture layer and fLayerSupported hard-returns false for
 * Bedrock players, so Bedrock players could not fire class
 * abilities at all. For a Bedrock player, /abilities opens a
 * Cumulus form whose buttons call the same
 * AdvancedCombatModule.useAbility entry point EM's own F-key
 * router ends at. EliteMobs still enforces class, controls,
 * cost, targeting and threat; this module adds no costs,
 * cooldowns or targeting of its own. Java players are
 * untouched: /abilities prints one line pointing at the F key
 * and EM's native flow is never intercepted. Built Oct 19,
 * 2026 as the standalone bridge.
 *
 * THE EM COMPILE-PIN DECISION (programme Wave 4, recorded
 * here as the build record requires): the standalone REFUSED
 * to build against an EliteMobs jar whose MD5 was not pinned
 * (0.2.0 pinned EM 10.9.14). The module REPLACES that
 * compile-time pin with this pack's house posture - guarded
 * seams plus a boot-time ACS probe - because a compile pin
 * inside a multi-module pack would block every other module's
 * build on any EliteMobs bump. Concretely: the module compiles
 * against the pack's existing compile-time-only EliteMobs jar
 * (10.9.10; the ACS surface it uses is identical in 10.9.8,
 * 10.9.10 and 10.9.14 - the standalone's own surface diff and
 * cross-version rig fires proved 10.9.8<->10.9.10, and 0.2.0
 * was built and rig-proven against 10.9.14); at runtime every
 * EliteMobs type lives behind the AcsGateway seam, only
 * EmAcsGateway and the isolated AcsProbe name EliteMobs types,
 * every gateway call is wrapped in try/catch(Throwable), the
 * failure-reason mapping is BY NAME (an EliteMobs that adds a
 * reason degrades it to a plain UNKNOWN line, never a broken
 * switch), and the boot-time probe below disables this module
 * leg cleanly - one console line, the rest of the pack
 * untouched - if EM's surface has moved. Live runs EM 10.9.16
 * at fold time; the module's first live boot under 10.9.16 is
 * itself part of the install window's verification, and the
 * post-install live Bedrock cast (G1 harness) is the proof
 * that closes the wave.
 *
 * FAIL-SOFT, three shapes (the standalone's, re-homed): EM
 * initializes ACS in its POST-initialization phase, after
 * every plugin's onEnable, so "not initialized yet" is the
 * NORMAL state at enable time on a healthy server.
 *  - EM absent: one console line; the module leg stays inert
 *    (no command), the pack boots clean.
 *  - EM present, ACS classes unreachable (an EM update moved
 *    them): the boot probe reports UNAVAILABLE; one console
 *    line; the module leg stays inert, the pack boots clean.
 *  - EM present, ACS still starting: gateway + command enable
 *    in standby; a watcher polls every 5 s and logs the
 *    bridge coming online once ACS is up; a tap during
 *    standby gets a plain "unavailable right now" line.
 * Unlike the standalone (which disabled its whole plugin in
 * shapes 1-2), the module never disables the pack - only its
 * own leg stands down, which is exactly the programme's
 * boot-probe leg: module disables cleanly, pack unaffected.
 *
 * OWNER-USABILITY (D246): toggleable
 * (modules.bedrock-abilities.enabled) and config-driven
 * (debug-log-fires). No server-specific names, paths or
 * content IDs appear in code or shipped defaults; the shipped
 * default for debug-log-fires is false, which is also WRV's
 * live standalone value.
 *
 * DEPENDENCY (programme gate 7 - self-contained pack): no new
 * hard depend. EliteMobs is reached through the pack's
 * existing softdepend. The Bedrock leg adds NO hard depend on
 * Geyser or Floodgate: plugin.yml gains floodgate as a
 * softdepend only (Geyser-Spigot was already there), and all
 * Cumulus/Floodgate/Geyser access is reflection through the
 * plugins' own classloaders (BridgeForms), presence-guarded -
 * if Geyser/Floodgate is absent, the form leg stays inert
 * (the /abilities chat fallback still lists the player's
 * abilities) and the pack boots and runs clean. EliteMobs
 * remains the pack's only required companion plugin.
 *
 * STATE: none. The module writes no files and holds no
 * per-player state; EliteMobs keeps all class/progression
 * data. With the module off, /abilities does not exist.
 */
public final class BedrockAbilitiesModule implements FixModule {

    private ModuleContext ctx;
    private AcsGateway gateway;
    private boolean debugLogFires;
    private BukkitTask watcher;

    @Override
    public String id() {
        return "bedrock-abilities";
    }

    @Override
    public String displayName() {
        return "Bedrock Abilities";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        ConfigurationSection sec = ctx.moduleConfig();
        debugLogFires = sec != null && sec.getBoolean("debug-log-fires", false);

        Plugin eliteMobs = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (eliteMobs == null || !eliteMobs.isEnabled()) {
            ctx.logger().info("EliteMobs not found or not enabled - "
                    + "Bedrock ability bridge standing down (bedrock-abilities inert).");
            return;
        }

        AcsProbe.Result probe = AcsProbe.probe();
        if (probe == AcsProbe.Result.UNAVAILABLE) {
            ctx.logger().info("EliteMobs class system (ACS) could not be reached "
                    + "(an EliteMobs update may have moved it) - "
                    + "Bedrock ability bridge standing down (bedrock-abilities inert).");
            return;
        }

        gateway = new EmAcsGateway(ctx.logger());
        ctx.registerCommand("abilities", List.of("classabilities"),
                "/abilities or /abilities status", new AbilitiesCommand(this));
        if (probe == AcsProbe.Result.READY) {
            ctx.logger().info("Bedrock ability bridge ready: /abilities "
                    + "(EliteMobs class abilities for Bedrock players).");
        } else {
            // Normal on a healthy server: EliteMobs initializes
            // ACS in its post-initialization phase, after all
            // plugins enable. Stand by; watch for the transition.
            ctx.logger().info("EliteMobs class system (ACS) is still starting - "
                    + "Bedrock ability bridge standing by; it comes online "
                    + "automatically when ACS is up.");
            watchForAcs();
        }
    }

    @Override
    public void onDisable() {
        if (watcher != null) {
            watcher.cancel();
            watcher = null;
        }
        gateway = null;
    }

    @Override
    public void reload() {
        ConfigurationSection sec = ctx.moduleConfig();
        debugLogFires = sec != null && sec.getBoolean("debug-log-fires", false);
    }

    /** Polls until ACS initializes, logs the transition once, then stops. */
    private void watchForAcs() {
        watcher = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), () -> {
            if (AcsProbe.probe() == AcsProbe.Result.READY) {
                ctx.logger().info("EliteMobs class system (ACS) is up - "
                        + "Bedrock ability bridge online: /abilities");
                if (watcher != null) {
                    watcher.cancel();
                    watcher = null;
                }
            }
        }, 100L, 100L);
    }

    AcsGateway gateway() {
        return gateway;
    }

    /** The debug-log-fires config value, for the status surface. */
    boolean debugLogFires() {
        return debugLogFires;
    }

    java.util.logging.Logger logger() {
        return ctx.logger();
    }

    org.bukkit.plugin.Plugin plugin() {
        return ctx.plugin();
    }

    /** The pack version without its leading v, for the status line's "(v..)" shape. */
    String version() {
        String v = ExcelEMFixes.VERSION;
        return v.startsWith("v") ? v.substring(1) : v;
    }

    /**
     * The fire path the form's tap handler ends at - public so
     * the rig driver can exercise this exact code path
     * headlessly (the standalone's rig hook, kept). Always runs
     * the EliteMobs call on the main thread. On a refusal, the
     * player gets the bridge's plain-words line; EliteMobs sends
     * its own specific feedback as well.
     */
    public void fireAbility(Player player, BridgeSlot slot) {
        if (slot == null || player == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(ctx.plugin(), () -> fireAbility(player, slot));
            return;
        }
        AcsGateway gw = gateway;
        if (gw == null || !gw.moduleReady()) {
            player.sendMessage("Class abilities are unavailable right now.");
            return;
        }
        FireOutcome outcome = gw.fire(player, slot);
        if (debugLogFires) {
            ctx.logger().info("Bridge fire " + player.getName() + " " + slot
                    + " -> " + (outcome.successful() ? "SUCCESS" : "FAIL " + outcome.failure())
                    + (outcome.abilityId() != null ? " (" + outcome.abilityId() + ")" : ""));
        }
        if (!outcome.successful()) {
            player.sendMessage(FailureMessages.line(outcome.failure()));
        }
    }
}
