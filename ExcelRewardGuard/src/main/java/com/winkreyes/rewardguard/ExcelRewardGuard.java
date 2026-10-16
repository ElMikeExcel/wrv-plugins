package com.winkreyes.rewardguard;

import com.winkreyes.rewardguard.integration.EliteVerdictLog;
import com.winkreyes.rewardguard.integration.JobsVeto;
import com.winkreyes.rewardguard.integration.McMmoVeto;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
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
 *
 * Fall-softening half (1.2, Mike's decision, Oct 16): sky farms defeat the
 * birth marks — their mobs spawn naturally in the dark at height, fall, and
 * are finished with one hit. A FallDamageTracker therefore accumulates each
 * mob's FALL damage, and a kill whose target took at least
 * farm-fall-damage-percent (default 50) of its max health as fall damage
 * counts as farmed too, earning the same reduced share through the same two
 * enforcement paths (see FallDamageTracker for the exact rules, including
 * the cumulative-after-healing rule and the accepted ravine false
 * positive). Origin verdicts keep priority for the log label, and a kill is
 * never scaled twice. Everything 1.1 did for non-fall kills is unchanged.
 */
public final class ExcelRewardGuard extends JavaPlugin implements Listener {
    private ProvenanceStore provenance;
    private XpSuppression suppression;
    private FallDamageTracker fallTracker;
    private final AtomicBoolean reportedFailure = new AtomicBoolean();
    private final AtomicLong tickCounter = new AtomicLong();
    private final AtomicLong jobsInterventions = new AtomicLong();
    private final AtomicLong mcMmoInterventions = new AtomicLong();
    private final AtomicLong eliteVerdicts = new AtomicLong();
    private int farmRewardPercent = RewardScale.DEFAULT_PERCENT;
    private int farmFallDamagePercent = FallDamageTracker.DEFAULT_THRESHOLD_PERCENT;
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
        Object rawFallThreshold = getConfig().get("farm-fall-damage-percent");
        RewardScale.ParsedPercent parsedFall = RewardScale.parsePercent(rawFallThreshold, FallDamageTracker.DEFAULT_THRESHOLD_PERCENT);
        farmFallDamagePercent = parsedFall.percent();
        if (parsedFall.adjusted()) {
            getLogger().warning("farm-fall-damage-percent value '" + rawFallThreshold + "' is not a whole number in 0-100; using "
                + farmFallDamagePercent + "%. Fix config.yml to silence this warning.");
        }
        provenance = new ProvenanceStore();
        fallTracker = new FallDamageTracker();
        suppression = new XpSuppression(
            getConfig().getDouble("party-share-radius-blocks", 40.0),
            getConfig().getLong("suppression-window-ticks", 2L));
        getServer().getPluginManager().registerEvents(this, this);
        getServer().getScheduler().runTaskTimer(this, tickCounter::incrementAndGet, 1L, 1L);

        getLogger().info("Provenance marking ON: spawner, spawn-egg and Nether-portal mobs are marked at birth and stay marked.");
        getLogger().info("Farm reward percent: " + farmRewardPercent + "% (config farm-reward-percent; 0 = block all farm rewards like 1.0, 100 = no reduction).");
        getLogger().info("Fall-softened kill detection ON: a mob whose cumulative fall damage reaches " + farmFallDamagePercent
            + "% of its max health counts as farmed and earns the same " + farmRewardPercent
            + "% (config farm-fall-damage-percent: " + farmFallDamagePercent + ").");
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

    @Override public void onDisable() {
        // The fall tracker is in-memory only; clearing it here means a
        // reload never inherits stale fall history (and frees the map).
        if (fallTracker != null) fallTracker.clear();
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

    /** The configured fall-softening threshold (0-100): cumulative fall damage, as a percent of max health, at which a kill counts as farmed. */
    public int farmFallDamagePercent() { return farmFallDamagePercent; }

    public FallDamageTracker fallTracker() { return fallTracker; }

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

    /** True when the entity's cumulative fall damage reaches the configured threshold (see FallDamageTracker). */
    public boolean isFallSoftened(LivingEntity entity) {
        if (fallTracker == null || entity == null) return false;
        return fallTracker.isSoftened(entity.getUniqueId(), farmFallDamagePercent);
    }

    /** The entity's cumulative fall damage as a percent of its most recently observed max health; 0 when untracked. */
    public double fallDamagePercentOf(LivingEntity entity) {
        if (fallTracker == null || entity == null) return 0.0;
        return fallTracker.fallDamagePercent(entity.getUniqueId());
    }

    /**
     * The combined enforcement classification: the reason this kill counts
     * as farmed, or null when it does not. The origin verdict wins the label
     * when both signals fire, so a kill is classified (and scaled) exactly
     * once. Reasons: a provenance reason (e.g. spawn_origin_spawner) or
     * FallDamageTracker.FALL_SOFTENED_REASON.
     */
    public String farmedReason(LivingEntity entity) {
        if (entity == null) return null;
        String[] decision = farmDecision(entity);
        return FallDamageTracker.farmedReason(
            "DENY".equals(decision[0]), decision[1], isFallSoftened(entity));
    }

    /** True when the kill counts as farmed by either signal: origin DENY or fall-softened. */
    public boolean isFarmed(LivingEntity entity) {
        return farmedReason(entity) != null;
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
     * Accumulates fall damage for the softened-kill signature. MONITOR
     * priority with ignoreCancelled, so only falls that actually happened
     * count, and the amount is the FINAL damage (after armor/enchantment
     * math — for fall damage that is what the health bar actually lost).
     * Players are not tracked: a player's death pays no kill rewards, and
     * excluding them keeps the map proportional to mobs that fell.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(event.getEntity() instanceof LivingEntity entity)) return;
        if (entity instanceof Player) return;
        if (fallTracker == null) return;
        AttributeInstance maxHealth = entity.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) return;
        fallTracker.recordFall(entity.getUniqueId(), event.getFinalDamage(), maxHealth.getValue());
    }

    /**
     * Plants the mcMMO suppression window for a farmed death (origin-denied
     * or fall-softened). LOWEST priority so the window exists before
     * mcMMO's own NORMAL-priority death handler grants the kill XP and
     * party share synchronously inside this same dispatch. Vanilla XP drops
     * are deliberately not touched here or anywhere else.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onDeathPlantWindow(EntityDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim instanceof Player) return;
        String reason = farmedReason(victim);
        if (reason == null) return;
        Player killer = victim.getKiller();
        var location = victim.getLocation();
        suppression.plant(killer == null ? null : killer.getUniqueId(),
            location.getWorld() == null ? "" : location.getWorld().getName(),
            location.getX(), location.getY(), location.getZ(), currentTick());
        if (debugLogging) {
            if (FallDamageTracker.FALL_SOFTENED_REASON.equals(reason)) {
                getLogger().info("Suppression window planted for fall-softened death of " + victim.getType()
                    + " (fall damage " + oneDecimal(fallDamagePercentOf(victim)) + "% of max health, threshold "
                    + farmFallDamagePercent + "%).");
            } else {
                getLogger().info("Suppression window planted for denied death of " + victim.getType()
                    + " (reason " + reason + ").");
            }
        }
    }

    /**
     * Drops the victim's fall record once death processing is done.
     * MONITOR priority: every enforcement reader (the window plant above at
     * LOWEST, Jobs' own death handlers, the mcMMO window match) runs inside
     * this same dispatch before MONITOR, so the record is guaranteed to be
     * present for the whole kill and gone afterwards — for player kills and
     * non-player deaths alike.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeathCleanup(EntityDeathEvent event) {
        if (fallTracker != null) fallTracker.remove(event.getEntity().getUniqueId());
    }

    /**
     * Drops fall records for entities leaving the world without dying
     * (despawn, discard, unload, ...). DEATH is skipped on purpose: the
     * death cleanup above owns that path, with ordering guarantees this
     * event does not give.
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityRemove(EntityRemoveEvent event) {
        if (fallTracker == null) return;
        if (event.getCause() == EntityRemoveEvent.Cause.DEATH) return;
        fallTracker.remove(event.getEntity().getUniqueId());
    }

    /** Drops fall records for every entity in an unloading chunk. */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitiesUnload(EntitiesUnloadEvent event) {
        if (fallTracker == null) return;
        List<UUID> ids = new ArrayList<>();
        for (Entity entity : event.getEntities()) ids.add(entity.getUniqueId());
        fallTracker.removeAll(ids);
    }

    private static String oneDecimal(double value) {
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    private void reportFailure(RuntimeException failure) {
        if (reportedFailure.compareAndSet(false, true)) {
            getLogger().severe("Provenance classification failed; the API is now unavailable for this lifetime. " + failure.getClass().getSimpleName());
        }
    }
}
