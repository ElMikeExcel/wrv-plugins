package com.winkreyes.exceleitemobsabilitybedrockbridge;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ExcelEliteMobsAbilityBedrockBridge - the Bedrock doorbell for EliteMobs class
 * abilities.
 *
 * EliteMobs fires class abilities only from its Java F-key gesture
 * layer, which it hard-gates off for Bedrock players; the ability
 * engine underneath has no platform check (verified against the
 * pinned EliteMobs 10.9.14 jar - see BUILD.md). For a Bedrock player,
 * /abilities opens a Cumulus form whose buttons call the same
 * AdvancedCombatModule.useAbility entry point the gesture router
 * ends at. EliteMobs still enforces class, controls, cost, targeting
 * and threat; this plugin adds no costs, cooldowns or targeting.
 *
 * Fail-soft by design, in three distinct shapes (all proven on the
 * scratch rig - see BUILD.md):
 *  - EliteMobs absent: one console line, plugin disables itself.
 *  - EliteMobs present but the class module cannot be REACHED (an
 *    EliteMobs update moved its unpublished internals): one console
 *    line, plugin disables itself. A moved internal is permanent for
 *    the boot; standing down is the correct response.
 *  - EliteMobs present, module reachable, but ACS has not INITIALIZED
 *    yet: EliteMobs initializes ACS in its post-initialization phase,
 *    which runs after every plugin's onEnable, so "not initialized" is
 *    the normal state at our enable time even on a healthy server.
 *    The bridge therefore stands BY instead of standing down: the
 *    command stays registered, every call re-probes the module, and
 *    the bridge comes online by itself the moment ACS is up.
 * Never an enable error in any shape.
 */
public final class ExcelEliteMobsAbilityBedrockBridge extends JavaPlugin {

    private AcsGateway gateway;
    private boolean debugLogFires;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debugLogFires = getConfig().getBoolean("debug-log-fires", false);

        Plugin eliteMobs = getServer().getPluginManager().getPlugin("EliteMobs");
        if (eliteMobs == null || !eliteMobs.isEnabled()) {
            getLogger().info("EliteMobs not found or not enabled - "
                    + "Bedrock ability bridge standing down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        AcsProbe.Result probe = AcsProbe.probe();
        if (probe == AcsProbe.Result.UNAVAILABLE) {
            getLogger().info("EliteMobs class system (ACS) could not be reached "
                    + "(an EliteMobs update may have moved it) - "
                    + "Bedrock ability bridge standing down.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        gateway = new EmAcsGateway(getLogger());
        AbilitiesCommand command = new AbilitiesCommand(this);
        if (getCommand("abilities") != null) {
            getCommand("abilities").setExecutor(command);
        }
        if (probe == AcsProbe.Result.READY) {
            getLogger().info("Bedrock ability bridge ready: /abilities "
                    + "(EliteMobs class abilities for Bedrock players).");
        } else {
            // Normal on a healthy server: EliteMobs initializes ACS in
            // its post-initialization phase, after all plugins enable.
            // Stand by; watch for the transition so the console record
            // shows the bridge coming online on its own.
            getLogger().info("EliteMobs class system (ACS) is still starting - "
                    + "Bedrock ability bridge standing by; it comes online "
                    + "automatically when ACS is up.");
            watchForAcs();
        }
    }

    /** Polls the module until ACS initializes, logs the transition once, then stops. */
    private void watchForAcs() {
        final org.bukkit.scheduler.BukkitTask[] task = new org.bukkit.scheduler.BukkitTask[1];
        task[0] = getServer().getScheduler().runTaskTimer(this, () -> {
            if (AcsProbe.probe() == AcsProbe.Result.READY) {
                getLogger().info("EliteMobs class system (ACS) is up - "
                        + "Bedrock ability bridge online: /abilities");
                if (task[0] != null) {
                    task[0].cancel();
                }
            }
        }, 100L, 100L);
    }

    AcsGateway gateway() {
        return gateway;
    }

    /** The debug-log-fires config value, for the 0.1.1 status surface. */
    boolean debugLogFires() {
        return debugLogFires;
    }

    /**
     * The fire path the form's tap handler ends at - public so the
     * scratch-rig driver can exercise this exact code path headlessly.
     * Always runs the EliteMobs call on the main thread (Cumulus
     * callbacks already arrive there; the guard covers every caller).
     * On a refusal, the player gets the bridge's plain-words line;
     * EliteMobs sends its own specific feedback as well.
     */
    public void fireAbility(Player player, BridgeSlot slot) {
        if (slot == null || player == null) {
            return;
        }
        if (!Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTask(this, () -> fireAbility(player, slot));
            return;
        }
        AcsGateway gw = gateway;
        if (gw == null || !gw.moduleReady()) {
            player.sendMessage("Class abilities are unavailable right now.");
            return;
        }
        FireOutcome outcome = gw.fire(player, slot);
        if (debugLogFires) {
            getLogger().info("Bridge fire " + player.getName() + " " + slot
                    + " -> " + (outcome.successful() ? "SUCCESS" : "FAIL " + outcome.failure())
                    + (outcome.abilityId() != null ? " (" + outcome.abilityId() + ")" : ""));
        }
        if (!outcome.successful()) {
            player.sendMessage(FailureMessages.line(outcome.failure()));
        }
    }
}
