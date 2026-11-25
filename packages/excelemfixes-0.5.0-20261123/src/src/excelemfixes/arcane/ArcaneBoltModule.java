package excelemfixes.arcane;

import com.magmaguy.elitemobs.advancedcombat.abilities.ClassAbilityProjectileCarrier;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fix module: class-ability damage (upstream EliteMobs ACS defect;
 * player-visible symptom: Arcane Bolt feels weak and Spell Echo
 * does nothing for abilities).
 *
 * The defect, proven against EliteMobs 10.9.10: class-ability
 * damage is dealt by a direct native damage call
 * (ClassAbilityDamage -> NMS damageWithoutCooldown) that bypasses
 * every damage event. Two consequences:
 *
 * 1. Timed outgoing-damage buffs (Spell Echo and every other
 *    STRENGTH / SPELL_STRENGTH effect) are applied by
 *    TimedCombatModifiers only inside EliteMobDamagedByPlayerEvent
 *    - an event ability damage never fires - so buffs structurally
 *    cannot apply to abilities.
 * 2. Ability damage is computed with the weapon adjustment
 *    hardcoded to 1.0 (AdvancedDamageScaling.classAbility), so a
 *    signature ability falls behind basic attacks as gear
 *    progresses. EliteMobs exposes no balance config for any of
 *    this ("no balance settings are exposed yet" -
 *    AdvancedCombatSystem.yml).
 *
 * The workaround observes the one public trace an ability hit
 * leaves - its carrier projectile (PDC marker
 * elitemobs:class_ability_projectile) striking an elite - measures
 * the damage EliteMobs actually dealt (effective-HP drop, the same
 * before/after measurement ClassAbilityDamage itself uses), and
 * tops the hit up:
 *
 *     extra = dealt x (ability-damage-scale x buffFactor - 1)
 *
 * The top-up is a direct health/absorption adjustment: no damage
 * events are fired, so there is no formula re-entry and no
 * double-dip of the buff through EliteMobs' event listener, no XP
 * of any kind is paid by this module, and kill credit stays with
 * the ability's own damage source. buffFactor is the caster's
 * strongest unexpired ANY/SPELL outgoing modifier, read from
 * EliteMobs' live modifier state (read-only reflection, fails
 * safe to 1.0) - EliteMobs' own numbers, applied where its event
 * pipeline forgot them.
 *
 * Scope: projectile-delivered ability hits on elites. Instant,
 * zone and summon signatures do not ride the carrier projectile
 * and are not covered. Non-elite targets are untouched.
 */
public final class ArcaneBoltModule implements FixModule, Listener {

    private ModuleContext ctx;
    private Plugin eliteMobsPlugin;
    private EmInternals internals;
    private BukkitTask tickTask;
    private boolean active;

    private double abilityDamageScale = 1.0;
    private boolean buffApplicationEnabled = true;
    private double maxExtraFractionOfMaxHealth = 1.0;
    private boolean debugLog;

    /** Last per-tick effective-HP snapshot per elite entity UUID. */
    private final Map<UUID, Double> lastHpByElite = new HashMap<>();
    /** Carrier hits captured this tick, settled on the next tick. */
    private final List<PendingHit> pendingHits = new ArrayList<>();

    private record PendingHit(UUID casterId, UUID eliteId, double hpBefore) {
    }

    @Override
    public String id() {
        return "arcane-bolt";
    }

    @Override
    public String displayName() {
        return "Arcane Bolt Balance";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        eliteMobsPlugin = Bukkit.getPluginManager().getPlugin("EliteMobs");
        if (eliteMobsPlugin == null || !eliteMobsPlugin.isEnabled()) {
            ctx.logger().warning("Arcane Bolt Balance: EliteMobs not "
                    + "found or not enabled; module is inert.");
            return;
        }
        internals = new EmInternals(ctx.logger());
        active = true;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        // Per-tick: settle last tick's carrier hits, then snapshot HP.
        tickTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), this::tick, 1L, 1L);
        ctx.logger().info("Arcane Bolt Balance ready - ability damage scale "
                + abilityDamageScale + ", buff application "
                + (buffApplicationEnabled ? "on" : "off") + ".");
    }

    @Override
    public void onDisable() {
        active = false;
        HandlerList.unregisterAll(this);
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        pendingHits.clear();
        lastHpByElite.clear();
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        ConfigurationSection section = ctx.moduleConfig();
        if (section == null) return;
        abilityDamageScale = section.getDouble("ability-damage-scale", abilityDamageScale);
        buffApplicationEnabled = section.getBoolean("buff-application-enabled", buffApplicationEnabled);
        maxExtraFractionOfMaxHealth = section.getDouble(
                "max-extra-fraction-of-max-health", maxExtraFractionOfMaxHealth);
        debugLog = section.getBoolean("debug-log", debugLog);
    }

    // ------------------------------------------------------------------
    // Hit observation
    // ------------------------------------------------------------------

    /**
     * MONITOR: EliteMobs has fully applied the ability damage by now
     * on either of its two impact paths. We only record the hit here;
     * the damage is measured on the next tick against the pre-hit
     * snapshot, which also makes the measurement immune to event
     * ordering.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onCarrierHit(ProjectileHitEvent event) {
        if (!active) return;
        try {
            Projectile projectile = event.getEntity();
            if (!ClassAbilityProjectileCarrier.isCarrier(projectile)) return;
            if (!(event.getHitEntity() instanceof LivingEntity target)) return;
            if (!(projectile.getShooter() instanceof Player caster)) return;
            if (EntityTracker.getEliteMobEntity(target) == null) return;
            Double hpBefore = lastHpByElite.get(target.getUniqueId());
            if (hpBefore == null) return; // spawned this tick; no baseline yet
            pendingHits.add(new PendingHit(caster.getUniqueId(), target.getUniqueId(), hpBefore));
        } catch (Throwable failure) {
            logThrottled("hit observation", failure);
        }
    }

    // ------------------------------------------------------------------
    // Per-tick settle + snapshot
    // ------------------------------------------------------------------

    private void tick() {
        try {
            settlePendingHits();
        } catch (Throwable failure) {
            logThrottled("pending settle", failure);
        }
        try {
            snapshotElites();
        } catch (Throwable failure) {
            logThrottled("HP snapshot", failure);
        }
    }

    private void settlePendingHits() {
        if (pendingHits.isEmpty()) return;
        List<PendingHit> batch = new ArrayList<>(pendingHits);
        pendingHits.clear();
        // Group by elite, preserving capture order. Compensation is
        // linear in dealt damage, so settling a same-tick volley as
        // one chain (each step measured against the running HP) is
        // exact in aggregate.
        Map<UUID, List<PendingHit>> byElite = new HashMap<>();
        for (PendingHit hit : batch) {
            byElite.computeIfAbsent(hit.eliteId(), ignored -> new ArrayList<>()).add(hit);
        }
        for (Map.Entry<UUID, List<PendingHit>> entry : byElite.entrySet()) {
            EliteEntity elite = EntityTracker.getEliteMobEntities().get(entry.getKey());
            if (elite == null) continue;
            LivingEntity target = elite.getLivingEntity();
            if (target == null || target.isDead() || !target.isValid()) continue;
            double cursor = entry.getValue().get(0).hpBefore();
            for (PendingHit hit : entry.getValue()) {
                double hpNow = effectiveHp(target);
                double dealt = cursor - hpNow;
                if (dealt > 0.0) {
                    applyCompensation(hit.casterId(), target, dealt);
                    hpNow = effectiveHp(target);
                    lastHpByElite.put(target.getUniqueId(), hpNow);
                }
                cursor = hpNow;
                if (target.isDead()) break;
            }
        }
    }

    private void snapshotElites() {
        Map<UUID, EliteEntity> elites = EntityTracker.getEliteMobEntities();
        List<UUID> ids = new ArrayList<>(elites.keySet());
        lastHpByElite.keySet().retainAll(ids);
        for (UUID id : ids) {
            EliteEntity elite = elites.get(id);
            if (elite == null) continue;
            LivingEntity living = elite.getLivingEntity();
            if (living == null || living.isDead()) {
                lastHpByElite.remove(id);
                continue;
            }
            lastHpByElite.put(id, effectiveHp(living));
        }
    }

    // ------------------------------------------------------------------
    // Compensation
    // ------------------------------------------------------------------

    /**
     * Applies the top-up for one measured ability hit and returns the
     * extra damage dealt. Public so test drivers can exercise the
     * exact production path.
     */
    public double applyCompensation(UUID casterId, LivingEntity target, double dealt) {
        if (!active || target == null || target.isDead()) return 0.0;
        double buffFactor = 1.0;
        if (buffApplicationEnabled && internals != null) {
            buffFactor = internals.strongestOutgoingMultiplier(eliteMobsPlugin, casterId);
        }
        double extra = CompensationMath.extraDamage(dealt, abilityDamageScale, buffFactor);
        if (extra <= 0.0) return 0.0;
        double maxHealth = maxHealth(target);
        if (maxHealth > 0.0) {
            extra = Math.min(extra, maxHealth * Math.max(0.0, maxExtraFractionOfMaxHealth));
        }
        extra = Math.min(extra, effectiveHp(target));
        if (extra <= 0.0) return 0.0;
        double[] split = CompensationMath.applySplit(
                target.getAbsorptionAmount(), target.getHealth(), extra);
        target.setAbsorptionAmount(split[0]);
        target.setHealth(split[1]);
        if (debugLog) {
            ctx.logger().info(String.format(
                    "ARCANE-BOLT dealt=%.2f scale=%.2f buff=%.3f extra=%.2f elite=%s caster=%s",
                    dealt, abilityDamageScale, buffFactor, extra,
                    target.getUniqueId(), casterId));
        }
        return extra;
    }

    // ------------------------------------------------------------------
    // Helpers / test-facing state
    // ------------------------------------------------------------------

    static double effectiveHp(LivingEntity entity) {
        return Math.max(0.0, entity.getHealth()) + Math.max(0.0, entity.getAbsorptionAmount());
    }

    private static double maxHealth(LivingEntity entity) {
        var attribute = entity.getAttribute(Attribute.MAX_HEALTH);
        return attribute == null ? 0.0 : attribute.getValue();
    }

    /** Whether the EliteMobs buff bridge has located the modifier state. */
    public boolean isBuffBridgeReady() {
        return internals != null && internals.isReady();
    }

    /** Last snapshotted effective HP for an elite, or null if untracked. */
    public Double trackedHp(UUID eliteEntityId) {
        return lastHpByElite.get(eliteEntityId);
    }

    private long lastFailureLogNanos;

    private void logThrottled(String where, Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        ctx.logger().warning("Arcane Bolt Balance: failure during " + where + " ("
                + failure.getClass().getSimpleName() + ": " + failure.getMessage()
                + "); module continues.");
    }
}
