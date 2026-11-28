package excelemfixes.leash;

import com.magmaguy.elitemobs.api.EliteMobHealEvent;
import com.magmaguy.elitemobs.api.EliteMobSpawnEvent;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfig;
import com.magmaguy.elitemobs.config.custombosses.CustomBossesConfigFields;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.mobconstructor.custombosses.RegionalBossEntity;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.projectiles.ProjectileSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * All EliteMobs-touching code for LeashModule. This class is only
 * loaded when EliteMobs is present (the main class checks first), so
 * its EM imports never resolve against a server without EliteMobs.
 *
 * Leg 1 — native-leash suppression (carried from 0.2.0): every
 * loaded boss config's in-memory leashRadius is set to 0 (EM's
 * disabled sentinel) via CustomBossesConfigFields.setLeashRadius —
 * a bare field write, bytecode-verified on the pinned 10.9.14 jar;
 * boss files are never written. With EM's native leash off, none of
 * its three spec-breaking behaviors can engage underneath ours: the
 * soft return at half radius, the damage-invulnerable return, and
 * the heal-on-return-end with the target never cleared.
 *
 * Leg 2 — the D183 return engine. EM's leash only ever governs
 * regional bosses (they alone have a placed spawn point), so the
 * engine tracks RegionalBossEntity instances from
 * EntityTracker.getEliteMobEntities():
 *  - home = EliteEntity.getSpawnLocation(), recorded on
 *    EliteMobSpawnEvent and lazily on first poll sight;
 *  - a combat elite (EM inCombat, or damagers present, or a Bukkit
 *    target set) found strictly beyond the configured range from
 *    home is deaggroed — Mob.setTarget(null) +
 *    EliteEntity.clearDamagers(), the same two calls 0.1.0 used —
 *    and marked RETURNING;
 *  - while RETURNING the poll keeps it targetless, re-issues the
 *    pathfinder walk home at EM's hard-return speed (2.0), clamps
 *    its health to the value the return started with, and — after
 *    5 s without progress or 60 s total — teleports it home (EM's
 *    teleport-on-fail precedent) and runs the arrival sequence;
 *  - arrival (within ReturnPolicy.ARRIVE_RADIUS of home): the
 *    RETURNING mark is removed FIRST, then EliteEntity.fullHeal()
 *    runs — the stock full heal, whose EliteMobHealEvent now fires
 *    uncancelled so heal-keyed pack behavior (a sleep power's sleep
 *    at home) proceeds exactly as stock — UNLESS the D251 tuning
 *    suppresses it: an elite marked combat-touched (landed player
 *    damage this episode, tracked from the damage listener and
 *    seeded from EM's damager records at return start) arrives
 *    with NO heal call at all and keeps its current HP;
 *  - player damage that actually lands on a RETURNING elite
 *    (MONITOR, ignoreCancelled; damager resolved directly or via a
 *    projectile's shooter) breaks the return: mark removed,
 *    Mob.setTarget(damager) — instant re-aggro, and the arrival
 *    heal simply never runs;
 *  - EliteMobHealEvent is cancelled for RETURNING elites:
 *    fullHeal() fires that event first and honors cancellation
 *    (bytecode-verified), so EM's combat-end full heal — or any
 *    other heal — cannot land mid-return and defeat the
 *    no-heal-if-damaged rule. The health clamp above is the
 *    backstop for any heal path that bypasses the event.
 *  - The same event is the veto point for EM's combat-end
 *    reset heal (v30): EM's per-elite combat watchdog fires
 *    EliteMobExitCombatEvent when no player is within the
 *    boss's follow range, and that event's constructor calls
 *    fullHeal() (MobCombatSettings'
 *    regenerateCustomBossHealthOnCombatEnd, on by default) —
 *    an instant heal to full wherever the boss stands, leash
 *    or no leash. The constructor clears EM's combat flag
 *    BEFORE healing, so the reset heal is the only full heal
 *    that presents with the elite out of combat; for a
 *    combat-touched elite (the D251 mark) it is cancelled.
 *    Untouched elites keep the stock combat-end heal, and
 *    in-combat full heals (phase resets, powers) are never
 *    touched. See ReturnPolicy.shouldVetoCombatEndHeal.
 */
public final class EmLeashBridge implements Listener {

    /** Walk-home speed — EM's own hard-return speed. */
    private static final double RETURN_SPEED = 2.0;

    /** One elite's in-progress return. */
    private static final class ReturnState {
        final Location home;
        final long startedAtMillis;
        final double healthCap;
        double lastDistance;
        long lastProgressAtMillis;

        ReturnState(Location home, long nowMillis, double distance, double healthCap) {
            this.home = home;
            this.startedAtMillis = nowMillis;
            this.lastProgressAtMillis = nowMillis;
            this.lastDistance = distance;
            this.healthCap = healthCap;
        }
    }

    private final LeashModule plugin;
    private final LeashModule.Settings settings;
    private boolean appliedOnce = false;

    /** Spawn points by entity UUID — the "home" of the spec. */
    private final Map<UUID, Location> homes = new HashMap<>();
    /** Elites currently walking home, by entity UUID. */
    private final Map<UUID, ReturnState> returning = new HashMap<>();
    /**
     * D251: elites that have taken player damage since spawn or
     * since their last completed return ("combat-touched"), by
     * tracker UUID. A touched elite's return completes WITHOUT
     * the arrival heal; the mark clears when that return
     * completes, so the next episode starts clean.
     */
    private final Set<UUID> combatTouched = new HashSet<>();

    public EmLeashBridge(LeashModule plugin, LeashModule.Settings settings) {
        this.plugin = plugin;
        this.settings = settings;
    }

    // ---- Leg 1: native-leash suppression ----

    /**
     * Suppresses EM's native leash on every boss in EM's loaded
     * registry: any leashRadius above 0 is set to 0. Writes only
     * where the current value is above 0, so repeat passes are
     * silent no-ops unless EM reloaded its configs (or a pack
     * update landed) underneath us and restored a native leash.
     */
    public synchronized void applyLeashRemoval() {
        Map<String, ? extends CustomBossesConfigFields> bosses = CustomBossesConfig.getCustomBosses();
        if (bosses == null || bosses.isEmpty()) {
            if (!appliedOnce) {
                plugin.logger().warning("LEASH suppression: EliteMobs boss registry is empty — nothing applied yet.");
            }
            appliedOnce = true;
            return;
        }
        int seen = 0, leashed = 0, suppressed = 0;
        for (Map.Entry<String, ? extends CustomBossesConfigFields> entry : bosses.entrySet()) {
            CustomBossesConfigFields fields = entry.getValue();
            if (fields == null) continue;
            seen++;
            String filename = fields.getFilename() != null ? fields.getFilename() : entry.getKey();
            double current = fields.getLeashRadius();
            if (current > 0) leashed++;
            if (LeashRemovalPolicy.classify(current) == LeashRemovalPolicy.ChangeKind.UNLEASH) {
                fields.setLeashRadius(LeashRemovalPolicy.effectiveTarget(current));
                suppressed++;
                plugin.debug("LEASH " + filename + ": native leashRadius " + LeashModule.trim(current)
                        + " -> 0 (suppressed; D183 return is plugin-run)");
            }
        }
        if (!appliedOnce || suppressed > 0) {
            plugin.logger().info("LEASH suppression applied: bosses seen " + seen + ", with native leash "
                    + leashed + ", suppressed " + suppressed + " — EM native leash off for all bosses");
        }
        appliedOnce = true;
    }

    // ---- Leg 2: the return engine ----

    /**
     * The state maps are keyed by the EntityTracker map key (see
     * pollLeashes) — EM's own elite UUID, which for regional bosses
     * is a persistent identity that survives respawns and is NOT
     * the living entity's Bukkit UUID. Event handlers receive the
     * Bukkit entity, so they must resolve the tracker key first;
     * keying by the raw Bukkit UUID here silently missed every
     * return state (rig-found: mid-return damage never broke the
     * return, heals were never event-cancelled).
     */
    private UUID trackerKey(Entity entity) {
        Map<UUID, EliteEntity> tracked = EntityTracker.getEliteMobEntities();
        if (tracked == null) return entity.getUniqueId();
        if (tracked.containsKey(entity.getUniqueId())) return entity.getUniqueId();
        for (Map.Entry<UUID, EliteEntity> entry : tracked.entrySet()) {
            LivingEntity living = entry.getValue().getLivingEntity();
            if (living != null && living.getUniqueId().equals(entity.getUniqueId())) return entry.getKey();
        }
        return entity.getUniqueId();
    }

    /** Tracker key for an elite the event itself hands us. */
    private UUID trackerKey(EliteEntity elite) {
        Map<UUID, EliteEntity> tracked = EntityTracker.getEliteMobEntities();
        if (tracked == null) return null;
        for (Map.Entry<UUID, EliteEntity> entry : tracked.entrySet()) {
            if (entry.getValue() == elite) return entry.getKey();
        }
        LivingEntity living = elite.getLivingEntity();
        return living != null ? trackerKey(living) : null;
    }

    /** Records an elite's home the moment EM spawns it. */
    @EventHandler
    public void onEliteSpawn(EliteMobSpawnEvent event) {
        if (!(event.getEliteMobEntity() instanceof RegionalBossEntity)) return;
        Location spawn = event.getEliteMobEntity().getSpawnLocation();
        if (spawn != null) homes.put(trackerKey(event.getEntity()), spawn.clone());
    }

    /** Death ends everything: no home, no return state, no touch. */
    @EventHandler
    public void onDeath(EntityDeathEvent event) {
        UUID id = trackerKey(event.getEntity());
        homes.remove(id);
        returning.remove(id);
        combatTouched.remove(id);
    }

    /**
     * The free-fight fix: player damage that lands on a RETURNING
     * elite re-aggroes it on the damager immediately, and the
     * arrival heal never runs. Runs at MONITOR with ignoreCancelled
     * so only damage that actually landed counts — cancelled damage
     * (protection plugins, spawn protection) is not a re-pull.
     * The same landed damage also sets the D251 combat-touched
     * mark (see combatTouched), whether or not a return is in
     * progress.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Player damager = resolvePlayer(event.getDamager());
        if (damager == null) return;
        UUID id = trackerKey(event.getEntity());
        // D251: landed player damage marks the elite
        // combat-touched for this episode — if it later leash-
        // returns, the arrival heal is suppressed (it keeps its
        // current HP; recovery is the standing regen only).
        combatTouched.add(id);
        if (ReturnPolicy.onPlayerDamage(returning.containsKey(id)) != ReturnPolicy.DamageOutcome.REAGGRO) return;
        returning.remove(id);
        if (event.getEntity() instanceof Mob) {
            Mob mob = (Mob) event.getEntity();
            mob.getPathfinder().stopPathfinding();
            mob.setTarget(damager);
        }
        plugin.debug("LEASH return broken: " + event.getEntity().getType() + " damaged by "
                + damager.getName() + " mid-return — re-aggroed, no heal");
    }

    /**
     * Heal guard, two branches:
     *  - While an elite is RETURNING, its heals are cancelled.
     *    fullHeal() fires EliteMobHealEvent first and honors
     *    cancellation (bytecode-verified), so EM's combat-end
     *    full heal cannot land at return start and defeat the
     *    no-heal-if-damaged rule.
     *  - v30: EM's combat-end reset heal is cancelled for a
     *    combat-touched elite that is not returning. The reset
     *    heal is a fullHeal() called from the
     *    EliteMobExitCombatEvent constructor, which has already
     *    cleared the elite's combat flag — so it is the only
     *    full heal presenting out of combat. The veto consumes
     *    nothing: the combat-touched mark stays (it ends on
     *    return completion, death, or despawn, as before), and
     *    cancelling fullHeal also skips its damager clear, so
     *    the fight's participation record survives the veto.
     * Heals for untouched elites — including the arrival heal,
     * which under D251 only ever runs for them — and every
     * in-combat heal are never touched.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onHeal(EliteMobHealEvent event) {
        UUID id = trackerKey(event.getEliteEntity());
        if (id == null) return;
        if (returning.containsKey(id)) {
            event.setCancelled(true);
            plugin.debug("LEASH heal held: heal cancelled for a returning elite (heals only on arrival)");
            return;
        }
        EliteEntity elite = event.getEliteEntity();
        boolean veto = ReturnPolicy.shouldVetoCombatEndHeal(
                settings.noCombatEndHealAfterPlayerDamage,
                event.isFullHeal(),
                combatTouched.contains(id),
                elite.isInCombat(),
                ReturnPolicy.scopeApplies(settings.noCombatEndHealBossFiles, bossFilename(elite)));
        if (veto) {
            event.setCancelled(true);
            plugin.debug("LEASH combat-end heal held: " + bossName(elite)
                    + " took player damage this episode — EliteMobs combat-end reset heal "
                    + "cancelled; stays at " + LeashModule.trim(elite.getHealth()) + " HP");
        }
    }

    /** The 10-tick engine poll. Runs on the main thread. */
    public void pollLeashes() {
        Map<UUID, EliteEntity> tracked = EntityTracker.getEliteMobEntities();
        if (tracked == null) return;
        long now = System.currentTimeMillis();
        // Sweep state for entities EM no longer tracks (despawn,
        // chunk unload, removal): their episode is over either way.
        homes.keySet().removeIf(id -> !tracked.containsKey(id));
        returning.keySet().removeIf(id -> !tracked.containsKey(id));
        combatTouched.removeIf(id -> !tracked.containsKey(id));
        for (Map.Entry<UUID, EliteEntity> entry : tracked.entrySet()) {
            EliteEntity elite = entry.getValue();
            if (!(elite instanceof RegionalBossEntity)) continue;
            LivingEntity living = elite.getLivingEntity();
            if (living == null) continue;
            UUID id = entry.getKey();
            if (!living.isValid() || living.isDead()) {
                homes.remove(id);
                returning.remove(id);
                combatTouched.remove(id);
                continue;
            }
            Location home = homes.get(id);
            if (home == null) {
                Location spawn = elite.getSpawnLocation();
                if (spawn == null || spawn.getWorld() == null) continue;
                home = spawn.clone();
                homes.put(id, home);
            }
            if (living.getWorld() != home.getWorld()) {
                // Dimension change ends the episode (spec): drop any
                // return in progress; distance across worlds is
                // meaningless and vanilla/EM drop the aggro itself.
                returning.remove(id);
                combatTouched.remove(id);
                continue;
            }
            double distance = living.getLocation().distance(home);
            ReturnState state = returning.get(id);
            if (state == null) {
                boolean inCombat = elite.isInCombat() || elite.hasDamagers()
                        || (living instanceof Mob && ((Mob) living).getTarget() != null);
                if (ReturnPolicy.shouldTriggerReturn(false, inCombat, distance, settings.leashRange)) {
                    startReturn(id, elite, living, home, distance, now);
                }
            } else {
                stepReturn(id, elite, living, state, distance, now);
            }
        }
    }

    private void startReturn(UUID id, EliteEntity elite, LivingEntity living,
                             Location home, double distance, long now) {
        // D251 seeding: EM's own damager records name the players
        // who damaged this elite in the current combat — if any
        // exist, the episode is combat-touched even if our damage
        // listener missed a hit (e.g. damage that landed before
        // this module enabled). Read BEFORE clearDamagers wipes
        // the record below.
        if (elite.hasDamagers()) combatTouched.add(id);
        // The deaggro: exactly the two calls 0.1.0 used — the Bukkit
        // target plus EM's own combat-reset bundle (damagers, aggro/
        // threat, skill contributions, contribution sessions, and
        // the forced-target lease).
        if (living instanceof Mob) ((Mob) living).setTarget(null);
        elite.clearDamagers();
        returning.put(id, new ReturnState(home.clone(), now, distance, elite.getHealth()));
        if (living instanceof Mob) ((Mob) living).getPathfinder().moveTo(home, RETURN_SPEED);
        plugin.debug("LEASH return started: " + bossName(elite) + " pulled "
                + LeashModule.trim(distance) + " > " + LeashModule.trim(settings.leashRange)
                + " blocks from home — deaggroed, heading home");
    }

    private void stepReturn(UUID id, EliteEntity elite, LivingEntity living,
                            ReturnState state, double distance, long now) {
        // Backstop clamp: no heal path may raise it mid-return.
        if (elite.getHealth() > state.healthCap) elite.setHealth(state.healthCap);
        if (ReturnPolicy.hasArrived(distance)) {
            arriveHome(id, elite, living);
            return;
        }
        if (living instanceof Mob) {
            Mob mob = (Mob) living;
            // Nothing may re-aggro it on the way home except player
            // damage (handled in onDamage, which removes the mark
            // first) — clear any stray target and keep it walking.
            if (mob.getTarget() != null) mob.setTarget(null);
            mob.getPathfinder().moveTo(state.home, RETURN_SPEED);
        }
        if (distance < state.lastDistance - 0.5) {
            state.lastDistance = distance;
            state.lastProgressAtMillis = now;
        }
        if (ReturnPolicy.shouldTeleportFallback(now - state.startedAtMillis, now - state.lastProgressAtMillis)) {
            // EM's own teleport-on-fail precedent: the walk is not
            // getting it home, so it goes home directly.
            living.teleport(state.home);
            arriveHome(id, elite, living);
        }
    }

    private void arriveHome(UUID id, EliteEntity elite, LivingEntity living) {
        // Unmark FIRST: the arrival heal must fire its stock
        // EliteMobHealEvent uncancelled (see onHeal), so heal-keyed
        // pack behavior at home (primis_sleep) proceeds as stock.
        returning.remove(id);
        if (living instanceof Mob) ((Mob) living).getPathfinder().stopPathfinding();
        // D251: the episode ends here either way, so the touch
        // mark is consumed now — the next episode starts clean.
        boolean touched = combatTouched.remove(id);
        boolean inScope = ReturnPolicy.scopeApplies(settings.noReturnHealBossFiles, bossFilename(elite));
        if (ReturnPolicy.arrivalHeal(settings.noReturnHealAfterPlayerDamage, touched, inScope)
                == ReturnPolicy.ArrivalHeal.FULL) {
            elite.fullHeal();
            plugin.debug("LEASH return complete: " + bossName(elite) + " home — healed to full, stays home");
        } else {
            // No heal call at all: the elite keeps exactly the HP
            // it arrived with (the mid-return clamp already held
            // it at or below its return-start health). Recovery
            // from here is the standing out-of-combat regen only.
            plugin.debug("LEASH return complete: " + bossName(elite) + " home — NO heal (D251: "
                    + "it took player damage this episode; stays at "
                    + LeashModule.trim(elite.getHealth()) + " HP)");
        }
    }

    /** The boss's EliteMobs filename, or null when it cannot be read. */
    private String bossFilename(EliteEntity elite) {
        try {
            return ((RegionalBossEntity) elite).getCustomBossesConfigFields().getFilename();
        } catch (Throwable t) {
            return null;
        }
    }

    private String bossName(EliteEntity elite) {
        String filename = bossFilename(elite);
        return filename != null ? filename : elite.getLivingEntity().getType().name();
    }

    private static Player resolvePlayer(Entity damager) {
        if (damager instanceof Player) return (Player) damager;
        if (damager instanceof Projectile) {
            ProjectileSource shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }
}
