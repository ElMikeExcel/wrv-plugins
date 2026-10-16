package com.winkreyes.rewardguard;

import com.winkreyes.rewardguard.integration.EliteVerdictLog;
import com.winkreyes.rewardguard.integration.JobsVeto;
import com.winkreyes.rewardguard.integration.McMmoVeto;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Farm-reward guard for Wink Reyes Valley.
 *
 * Provenance half (original Moon candidate, Oct 14): mobs born from spawners,
 * spawn eggs or Nether portals are marked in their own persistent data at
 * birth, the mark is sticky across transformations, and farmDecision() answers
 * DENY / UNKNOWN for any consumer.
 *
 * Enforcement half (completion, Oct 15): the guard reduces the rewards itself
 * through the reward plugins' own events. Since 1.1 (Mike's decision, Oct 15)
 * a marked mob earns a REDUCED share of its rewards — farm-reward-percent,
 * default 65 — instead of nothing:
 *   - Jobs: JobsPrePaymentEvent and JobsExpGainEvent carry the killed entity,
 *     so the reduction is exact per kill: every component (money, points, XP)
 *     has a setter on the installed Jobs 5.2.6.3 events (verified by javap)
 *     and is scaled, never cancelled, at any percent above 0.
 *   - mcMMO: its XP gain events carry no entity, so a denied death plants a
 *     short suppression window (see XpSuppression) and PVE / SHARED_PVE gains
 *     inside it are scaled at LOWEST priority, before any multiplier plugin
 *     can grow them; a reduced amount times a multiplier stays reduced.
 *   - EliteMobs: 10.9.8 exposes no cancellable reward event (EliteMobDeathEvent
 *     is informational; verified by javap), so the guard logs denied elite
 *     deaths for observability and claims no veto there.
 * farm-reward-percent 0 reproduces the 1.0 zero behavior exactly, and 100
 * turns the reduction off (full rewards). Vanilla XP drops are never touched
 * at any setting.
 */
public final class ExcelRewardGuard extends JavaPlugin implements Listener {
    private ProvenanceStore provenance;
    private XpSuppression suppression;
    private final AtomicBoolean reportedFailure = new AtomicBoolean();
    private final AtomicLong tickCounter = new AtomicLong();
    private final AtomicLong jobsInterventions = new AtomicLong();
    private final AtomicLong mcMmoInterventions = new AtomicLong();
    private final AtomicLong eliteVerdicts = new AtomicLong();
    private int farmRewardPercent = RewardScale.DEFAULT_PERCENT;
    private boolean debugLogging;

    @Override public void onEnable() {
        saveDefaultConfig();
        debugLogging = getConfig().getBoolean("debug-logging", false);
        Object rawPercent = getConfig().get("farm-reward-percent");
        RewardScale.ParsedPercent parsed = RewardScale.parsePercent(rawPercent);
        farmRewardPercent = parsed.percent();
        if (parsed.adjusted()) {
            getLogger().warning("farm-reward-percent value '" + rawPercent + "' is not a whole number in 0-100; using "
                + farmRewardPercent + "%. Fix config.yml to silence this warning.");
        }
        provenance = new ProvenanceStore();
        suppression = new XpSuppression(
            getConfig().getDouble("party-share-radius-blocks", 40.0),
            getConfig().getLong("suppression-window-ticks", 2L));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, tickCounter::incrementAndGet, 1L, 1L);

        getLogger().info("Provenance marking ON: spawner, spawn-egg and Nether-portal mobs are marked at birth and stay marked.");
        getLogger().info("Farm reward percent: " + farmRewardPercent + "% (config farm-reward-percent; 0 = block all farm rewards like 1.0, 100 = no reduction).");
        registerIntegration("Jobs", getConfig().getBoolean("jobs-veto", true),
            () -> getServer().getPluginManager().registerEvents(new JobsVeto(this), this),
            jobsActiveLine(),
            "Jobs enforcement OFF by config; marked kills can still earn full Jobs rewards.");
        registerIntegration("mcMMO", getConfig().getBoolean("mcmmo-veto", true),
            () -> getServer().getPluginManager().registerEvents(new McMmoVeto(this), this),
            mcMmoActiveLine(),
            "mcMMO enforcement OFF by config; marked kills can still earn full mcMMO XP.");
        Plugin eliteMobs = getServer().getPluginManager().getPlugin("EliteMobs");
        if (eliteMobs != null && eliteMobs.isEnabled() && getConfig().getBoolean("elitemobs-verdict-logging", true)) {
            getServer().getPluginManager().registerEvents(new EliteVerdictLog(this), this);
            getLogger().info("EliteMobs found: verdict logging only. EliteMobs 10.9.8 has no cancellable reward event, so the guard cannot reduce its coin showers; its own anti-exploit caps still apply.");
        } else if (eliteMobs == null) {
            getLogger().info("EliteMobs not found: elite verdict logging inactive.");
        }
        getLogger().info("Vanilla XP drops are never modified by this plugin.");
    }

    private String jobsActiveLine() {
        if (farmRewardPercent <= 0) {
            return "Jobs vetoes ACTIVE: marked kills earn no Jobs money and no Jobs XP (payment events cancelled; farm-reward-percent is 0).";
        }
        if (farmRewardPercent >= 100) {
            return "Jobs found: farm-reward-percent is 100, so marked kills earn full Jobs money and Jobs XP (no reduction applied).";
        }
        return "Jobs reward scaling ACTIVE: marked kills earn " + farmRewardPercent
            + "% of their Jobs money, points and XP (payment event amounts scaled; farm-reward-percent: " + farmRewardPercent + ").";
    }

    private String mcMmoActiveLine() {
        if (farmRewardPercent <= 0) {
            return "mcMMO veto ACTIVE: marked kills earn no mcMMO XP (kill XP zeroed before multipliers and party share; farm-reward-percent is 0).";
        }
        if (farmRewardPercent >= 100) {
            return "mcMMO found: farm-reward-percent is 100, so marked kills earn full mcMMO XP (no reduction applied).";
        }
        return "mcMMO reward scaling ACTIVE: marked kills earn " + farmRewardPercent
            + "% of their mcMMO XP (kill XP scaled before multipliers and party share; farm-reward-percent: " + farmRewardPercent + ").";
    }

    private void registerIntegration(String name, boolean wanted, Runnable register, String activeLine, String offLine) {
        Plugin plugin = getServer().getPluginManager().getPlugin(name);
        if (plugin == null || !plugin.isEnabled()) {
            getLogger().info(name + " not found: its reward enforcement is inactive; provenance marking and the farmDecision API still work.");
            return;
        }
        if (!wanted) {
            getLogger().info(offLine);
            return;
        }
        try {
            register.run();
            getLogger().info(activeLine);
        } catch (NoClassDefFoundError | RuntimeException failure) {
            getLogger().severe(name + " integration failed to register (" + failure.getClass().getSimpleName()
                + "); its reward enforcement is inactive. Provenance marking still works.");
        }
    }

    public int farmApiVersion() { return 1; }

    /** The configured farm-reward percent (0-100): the share of Jobs/mcMMO rewards a marked kill still earns. */
    public int farmRewardPercent() { return farmRewardPercent; }

    /** Snapshot while the entity object still exposes PDC, including during death callbacks. */
    public String[] farmDecision(LivingEntity entity) {
        if (!isEnabled() || provenance == null || entity == null || reportedFailure.get()) return FarmVerdict.unavailable().apiValue();
        try {
            return provenance.inspect(entity.getPersistentDataContainer()).apiValue();
        } catch (RuntimeException failure) {
            reportFailure(failure);
            return FarmVerdict.unavailable().apiValue();
        }
    }

    /** True when the entity's provenance verdict is DENY (including the fail-closed unavailable state). */
    public boolean isDenied(LivingEntity entity) {
        return "DENY".equals(farmDecision(entity)[0]);
    }

    public long currentTick() { return tickCounter.get(); }

    public XpSuppression suppression() { return suppression; }

    public boolean debugLogging() { return debugLogging; }

    public void countJobsIntervention() { jobsInterventions.incrementAndGet(); }
    public void countMcMmoIntervention() { mcMmoInterventions.incrementAndGet(); }
    public void countEliteVerdict() { eliteVerdicts.incrementAndGet(); }
    public long jobsInterventions() { return jobsInterventions.get(); }
    public long mcMmoInterventions() { return mcMmoInterventions.get(); }
    public long eliteVerdicts() { return eliteVerdicts.get(); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent event) {
        try {
            provenance.recordSpawn(event.getEntity().getPersistentDataContainer(), event.getSpawnReason().name());
        } catch (RuntimeException failure) {
            reportFailure(failure);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (!(event.getEntity() instanceof LivingEntity source)) return;
        try {
            for (var entity : event.getTransformedEntities()) {
                if (entity instanceof LivingEntity target) {
                    provenance.copy(source.getPersistentDataContainer(), target.getPersistentDataContainer());
                }
            }
        } catch (RuntimeException failure) {
            reportFailure(failure);
        }
    }

    /**
     * Plants the mcMMO suppression window for a denied death. LOWEST priority so
     * the window exists before mcMMO's own NORMAL-priority death handler grants
     * the kill XP and party share synchronously inside this same dispatch.
     * Vanilla XP drops are deliberately not touched here or anywhere else.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathPlantWindow(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;
        if (!isDenied(victim)) return;
        Player killer = victim.getKiller();
        var location = victim.getLocation();
        suppression.plant(killer == null ? null : killer.getUniqueId(),
            location.getWorld() == null ? "" : location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(), currentTick());
        if (debugLogging) {
            getLogger().info("Suppression window planted for denied death of " + victim.getType()
                + " (reason " + farmDecision(victim)[1] + ").");
        }
    }

    private void reportFailure(RuntimeException failure) {
        if (reportedFailure.compareAndSet(false, true)) {
            getLogger().severe("Provenance classification failed; the API is now unavailable for this lifetime. " + failure.getClass().getSimpleName());
        }
    }
}
