package excelemfixes.enderman;

import com.magmaguy.elitemobs.advancedcombat.abilities.ClassAbilityProjectileCarrier;
import com.magmaguy.elitemobs.dungeons.EliteMobsWorld;
import com.magmaguy.elitemobs.entitytracker.EntityTracker;
import com.magmaguy.elitemobs.magmacore.projectiles.MagicProjectileMarker;
import com.magmaguy.elitemobs.mobconstructor.EliteEntity;
import com.magmaguy.elitemobs.skills.SkillType;
import com.magmaguy.elitemobs.skills.WeaponIdentityResolver;
import excelemfixes.FixModule;
import excelemfixes.ModuleContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Fix module: enderman magic resistance (design ruling D195,
 * Nov 22, 2026 — total projectile immunity for endermen is
 * rejected as a design outcome; endermen get resistance instead).
 *
 * THE DEFECT (a vanilla rule EliteMobs inherits, not an EliteMobs
 * code bug): an enderman can NEVER be damaged by a projectile.
 * Paper's EnderMan.hurtServer refuses every projectile-tagged
 * damage source outright — it does not fire a damage event, it
 * only teleports away. Every EliteMobs magic attack is
 * projectile-delivered at the point of contact, so magic and
 * ranged builds whiff 100% of their hits on enderman content
 * (the Primis regional boss Manifested Nightmare, the Enderlad
 * event bosses, Living Shadow, and natural elite endermen): a
 * player who invested in a wand cannot fight — or gain weapon /
 * class XP — on any of it. Melee is unaffected, which made the
 * wall invisible to melee players.
 *
 * What actually happens per attack channel, proven on the exact
 * live stack (Paper 26.2 + EliteMobs 10.9.10, rig probe
 * 2026-11-22; see the build record):
 *  - Bows / crossbows / tridents: the arrow geometrically hits,
 *    ProjectileHitEvent fires, no damage event ever fires, zero
 *    damage. Total whiff.
 *  - Wand / staff bolts (FreeMinecraftModels magic weapons,
 *    resolved by EliteMobs): the bolt homes on its target; if
 *    the enderman does not blink during the flight, EliteMobs
 *    applies the damage as a player-sourced hit and it lands in
 *    full. If the enderman blinks mid-flight the flight resolves
 *    against empty space (or another mob) and the cast is lost —
 *    against a pack of teleporting endermen most casts whiff.
 *  - Class abilities (Arcane Bolt and siblings): the carrier
 *    projectile hits, but EliteMobs applies ability damage with
 *    the projectile as the damage source — refused like any
 *    other projectile. Total whiff.
 *
 * THIS MODULE — observe + reconcile, per targeted enderman:
 *  1. It watches projectile hits on in-scope endermen and magic
 *     casts aimed at them, snapshotting effective HP per tick.
 *  2. A hit that LANDED (measured HP drop) is reconciled to the
 *     configured landed fraction by restoring the resisted share
 *     of the drop directly (no events fired, so no formula
 *     re-entry; a killing blow is never undone — the mob is
 *     already dead and is skipped).
 *  3. A hit that was REFUSED (no HP drop) is re-applied once as
 *     a player-sourced damage call — the one shape vanilla
 *     endermen accept — so EliteMobs' own damage pipeline
 *     computes the damage from the shooter's gear and credits
 *     the shooter in its damager ledger (weapon/class XP and
 *     loot shares flow exactly as for a real hit; rig-proven:
 *     the re-applied hit is credited under the held weapon's
 *     skill). The re-applied damage is then reconciled to the
 *     landed fraction the same way.
 * Melee is never observed and never adjusted: resistance applies
 * to projectile/magic only, and endermen keep every other
 * behaviour — they still teleport freely, still hit as hard,
 * still leash and heal by EliteMobs' own rules.
 *
 * Scope: elite endermen anywhere (EliteMobs content by
 * definition), plus natural endermen in EliteMobs-scope worlds
 * (EliteMobs' world registry, the config world list, or the
 * config world-name prefix — shipped default "em_").
 */
public final class EndermanResistanceModule implements FixModule, Listener {

    /** Ticks between a physical projectile hit and its settle. */
    private static final long HIT_SETTLE_TICKS = 2L;
    /** Ticks a magic cast is given to land before it is settled. */
    private static final long CAST_SETTLE_TICKS = 30L;
    /** How far from the caster a cast's target is inferred. */
    private static final double TARGET_RANGE = 40.0;

    private ModuleContext ctx;
    private BukkitTask tickTask;
    private boolean active;
    private long tick;

    private double damageFraction = 0.5;
    private Set<String> configWorlds = Set.of();
    private String worldPrefix = "em_";
    private double rerouteSeedElite = 6.0;
    private double rerouteSeedNatural = 2.0;
    private double maxRerouteDistance = 48.0;
    private boolean debug;

    private boolean eliteMobsPresent;
    private boolean registryLinkFailed;

    private record Snapshot(double absorption, double health) {
        double effective() { return absorption + health; }
    }

    private enum Kind { PROJECTILE_HIT, MAGIC_HIT, ABILITY_HIT, MAGIC_CAST }

    private static final class Pending {
        final UUID shooterId;
        final UUID targetId;
        final UUID projectileId;
        Kind kind;
        long settleTick;
        Snapshot before;
        String weaponCategory;
        double seed;
        /** Shooter-to-target distance when the hit/cast was observed. */
        double observedDistance;

        Pending(UUID shooterId, UUID targetId, UUID projectileId, Kind kind,
                long settleTick, Snapshot before, String weaponCategory, double seed,
                double observedDistance) {
            this.shooterId = shooterId;
            this.targetId = targetId;
            this.projectileId = projectileId;
            this.kind = kind;
            this.settleTick = settleTick;
            this.before = before;
            this.weaponCategory = weaponCategory;
            this.seed = seed;
            this.observedDistance = observedDistance;
        }
    }

    /** Effective-HP snapshots of watched endermen, refreshed per tick. */
    private final Map<UUID, Snapshot> snapshots = new HashMap<>();
    /** Observed hits/casts awaiting their settle tick. */
    private final List<Pending> pending = new ArrayList<>();
    /** Cast records keyed by their bolt's projectile UUID. */
    private final Map<UUID, Pending> castByProjectile = new HashMap<>();
    /** Projectiles that already produced a hit record. */
    private final Set<UUID> hitProjectiles = new HashSet<>();
    /** Learned wand/staff application sizes for re-route seeds. */
    private final LearnedAmounts learned = new LearnedAmounts();

    @Override
    public String id() {
        return "enderman-resistance";
    }

    @Override
    public String displayName() {
        return "Enderman Magic Resistance";
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        this.ctx = ctx;
        loadSettings();
        eliteMobsPresent = Bukkit.getPluginManager().getPlugin("EliteMobs") != null;
        active = true;
        Bukkit.getPluginManager().registerEvents(this, ctx.plugin());
        tickTask = Bukkit.getScheduler().runTaskTimer(ctx.plugin(), this::tick, 1L, 1L);
        ctx.logger().info("Enderman Magic Resistance ready - projectile/magic damage "
                + "lands at " + Math.round(damageFraction * 100.0) + "% on endermen "
                + "in EliteMobs scope (elite endermen anywhere; natural endermen in "
                + "registry/list/\"" + worldPrefix + "\"-prefix worlds).");
        if (!eliteMobsPresent) {
            ctx.logger().warning("EliteMobs not detected: elite identity and the "
                    + "world registry are unavailable; natural-enderman scope runs "
                    + "on the config list/prefix only and re-applied hits land as "
                    + "plain vanilla damage.");
        }
    }

    @Override
    public void onDisable() {
        active = false;
        HandlerList.unregisterAll(this);
        if (tickTask != null) {
            tickTask.cancel();
            tickTask = null;
        }
        snapshots.clear();
        pending.clear();
        castByProjectile.clear();
        hitProjectiles.clear();
        learned.clear();
    }

    @Override
    public void reload() {
        loadSettings();
    }

    private void loadSettings() {
        org.bukkit.configuration.ConfigurationSection cfg = ctx.moduleConfig();
        damageFraction = ResistanceMath.sanitizeFraction(
                cfg == null ? 0.5 : cfg.getDouble("damage-fraction", 0.5));
        configWorlds = cfg == null ? Set.of() : new HashSet<>(cfg.getStringList("worlds"));
        worldPrefix = cfg == null ? "em_" : cfg.getString("world-prefix", "em_");
        rerouteSeedElite = ResistanceMath.sanitizeSeed(
                cfg == null ? 6.0 : cfg.getDouble("reroute-seed-elite", 6.0), 6.0);
        rerouteSeedNatural = ResistanceMath.sanitizeSeed(
                cfg == null ? 2.0 : cfg.getDouble("reroute-seed-natural", 2.0), 2.0);
        maxRerouteDistance = cfg == null ? 48.0 : cfg.getDouble("max-reroute-distance", 48.0);
        if (!(maxRerouteDistance > 0.0) || maxRerouteDistance > 512.0) maxRerouteDistance = 48.0;
        debug = cfg != null && cfg.getBoolean("debug", false);
    }

    // ------------------------------------------------------------------
    // Scope
    // ------------------------------------------------------------------

    private boolean isElite(Enderman enderman) {
        if (!eliteMobsPresent) return false;
        try {
            return EntityTracker.getEliteMobEntity(enderman) != null;
        } catch (Throwable t) {
            return false;
        }
    }

    private boolean naturalWorldInScope(World world) {
        boolean registrySays = false;
        if (eliteMobsPresent && !registryLinkFailed) {
            try {
                registrySays = EliteMobsWorld.isEliteMobsWorld(world.getUID());
            } catch (Throwable t) {
                registryLinkFailed = true;
                ctx.logger().warning("Enderman Magic Resistance: EliteMobs world "
                        + "registry call failed (" + t + "); natural-enderman scope "
                        + "falls back to the config list/prefix.");
            }
        }
        return EndermanScope.isInScope(false, registrySays,
                configWorlds.contains(world.getName()), world.getName(), worldPrefix);
    }

    private boolean inScope(Enderman enderman) {
        return isElite(enderman) || naturalWorldInScope(enderman.getWorld());
    }

    // ------------------------------------------------------------------
    // Observation: magic casts (marked bolt launches)
    // ------------------------------------------------------------------

    /**
     * A wand/staff cast is observed one tick after launch (the
     * bolt's marker is set by then) and its intended target is
     * inferred from the bolt's line of flight. The record settles
     * after the flight window: a landed cast is reconciled, a
     * whiffed cast (its enderman blinked away) is re-applied.
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onLaunch(ProjectileLaunchEvent event) {
        if (!active) return;
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof AbstractArrow)) return;
        if (!(projectile.getShooter() instanceof Player caster)) return;
        UUID projectileId = projectile.getUniqueId();
        Bukkit.getScheduler().runTaskLater(ctx.plugin(), () -> {
            try {
                if (!active || !projectile.isValid()) return;
                if (castByProjectile.containsKey(projectileId)) return;
                boolean marked;
                try {
                    marked = MagicProjectileMarker.isMarked(projectile);
                } catch (Throwable t) {
                    return; // EliteMobs magic layer unavailable: nothing to observe
                }
                if (!marked) return;
                Enderman target = inferTarget(caster, projectile);
                if (target == null) return;
                Snapshot before = snapshots.get(target.getUniqueId());
                if (before == null) before = snapshotOf(target);
                String category = weaponCategory(caster);
                double seed = learned.get(caster.getUniqueId(), category,
                        isElite(target) ? rerouteSeedElite : rerouteSeedNatural);
                Pending record = new Pending(caster.getUniqueId(), target.getUniqueId(),
                        projectileId, Kind.MAGIC_CAST, tick + CAST_SETTLE_TICKS,
                        before, category, seed,
                        caster.getLocation().distance(target.getLocation()));
                pending.add(record);
                castByProjectile.put(projectileId, record);
                debug("cast observed caster=" + caster.getName() + " target=" + target.getUniqueId()
                        + " seed=" + fmt(seed) + " category=" + category);
            } catch (Throwable failure) {
                logThrottled("cast observation", failure);
            }
        }, 1L);
    }

    private Enderman inferTarget(Player caster, Projectile projectile) {
        try {
            Location origin = projectile.getLocation();
            Vector direction = projectile.getVelocity();
            if (direction.lengthSquared() < 1.0e-6) direction = caster.getEyeLocation().getDirection();
            World world = origin.getWorld();
            if (world == null) return null;
            RayTraceResult ray = world.rayTraceEntities(origin, direction, TARGET_RANGE, 0.75,
                    e -> e instanceof Enderman ender && inScope(ender));
            if (ray != null && ray.getHitEntity() instanceof Enderman hit) return hit;
            List<CastTargeting.Candidate> candidates = new ArrayList<>();
            for (Entity nearby : caster.getNearbyEntities(TARGET_RANGE, 24.0, TARGET_RANGE)) {
                if (nearby instanceof Enderman ender && inScope(ender)) {
                    Location loc = ender.getLocation().add(0, 1.5, 0);
                    candidates.add(new CastTargeting.Candidate(ender.getUniqueId(),
                            loc.getX() - origin.getX(), loc.getY() - origin.getY(),
                            loc.getZ() - origin.getZ()));
                }
            }
            UUID chosen = CastTargeting.select(direction.getX(), direction.getY(), direction.getZ(),
                    candidates, TARGET_RANGE, 0.90);
            if (chosen == null) return null;
            Entity entity = Bukkit.getEntity(chosen);
            return entity instanceof Enderman ender ? ender : null;
        } catch (Throwable t) {
            return null;
        }
    }

    // ------------------------------------------------------------------
    // Observation: projectile hits on endermen
    // ------------------------------------------------------------------

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!active) return;
        try {
            if (!(event.getHitEntity() instanceof Enderman target)) return;
            Projectile projectile = event.getEntity();
            if (!(projectile instanceof AbstractArrow arrow)) return;
            if (!(projectile.getShooter() instanceof Player shooter)) return;
            if (!inScope(target)) return;
            if (!hitProjectiles.add(projectile.getUniqueId())) return; // one record per projectile

            Pending cast = castByProjectile.remove(projectile.getUniqueId());
            if (cast != null && cast.targetId.equals(target.getUniqueId())) {
                // The cast's own bolt arrived: settle it on the hit
                // schedule, keeping the launch-time HP baseline.
                cast.kind = Kind.MAGIC_HIT;
                cast.settleTick = tick + HIT_SETTLE_TICKS;
                debug("cast bolt arrived caster=" + shooter.getName() + " target=" + target.getUniqueId());
                return;
            }

            Snapshot before = snapshots.get(target.getUniqueId());
            if (before == null) before = snapshotOf(target);
            boolean carrier = false;
            boolean marked = false;
            try { carrier = ClassAbilityProjectileCarrier.isCarrier(projectile); } catch (Throwable ignored) {}
            try { marked = MagicProjectileMarker.isMarked(projectile); } catch (Throwable ignored) {}
            Kind kind = carrier ? Kind.ABILITY_HIT : marked ? Kind.MAGIC_HIT : Kind.PROJECTILE_HIT;
            String category = weaponCategory(shooter);
            double seed;
            if (kind == Kind.PROJECTILE_HIT) {
                double estimated = ResistanceMath.arrowSeed(arrow.getDamage(), arrow.getVelocity().length());
                seed = estimated > 0.0 ? estimated
                        : (isElite(target) ? rerouteSeedElite : rerouteSeedNatural);
            } else {
                seed = learned.get(shooter.getUniqueId(), category,
                        isElite(target) ? rerouteSeedElite : rerouteSeedNatural);
            }
            pending.add(new Pending(shooter.getUniqueId(), target.getUniqueId(),
                    projectile.getUniqueId(), kind, tick + HIT_SETTLE_TICKS,
                    before, category, seed,
                    shooter.getLocation().distance(target.getLocation())));
            debug("hit observed kind=" + kind + " shooter=" + shooter.getName()
                    + " target=" + target.getUniqueId() + " seed=" + fmt(seed));
        } catch (Throwable failure) {
            logThrottled("hit observation", failure);
        }
    }

    /**
     * Learns the size of real wand/staff damage applications: at
     * LOWEST priority the event still carries the application's
     * own amount, before EliteMobs' formula rewrites it. Only
     * applications that belong to an observed cast/hit record are
     * learned from, so a wand used as a melee stick never pollutes
     * the seed.
     */
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onDamageLowest(EntityDamageByEntityEvent event) {
        if (!active) return;
        try {
            if (!(event.getEntity() instanceof Enderman target)) return;
            if (!(event.getDamager() instanceof Player damager)) return;
            if (event.getCause() != org.bukkit.event.entity.EntityDamageEvent.DamageCause.ENTITY_ATTACK) return;
            String category = weaponCategory(damager);
            if (!"WANDS".equals(category) && !"STAVES".equals(category)) return;
            for (Pending record : pending) {
                if (record.shooterId.equals(damager.getUniqueId())
                        && record.targetId.equals(target.getUniqueId())) {
                    learned.remember(damager.getUniqueId(), category, event.getDamage());
                    return;
                }
            }
        } catch (Throwable failure) {
            logThrottled("application learning", failure);
        }
    }

    private String weaponCategory(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (hand == null || hand.getType().isAir()) return "UNKNOWN";
        if (eliteMobsPresent) {
            try {
                SkillType skill = WeaponIdentityResolver.progressionSkill(hand);
                if (skill != null) return skill.name();
            } catch (Throwable ignored) {}
        }
        return hand.getType().name();
    }

    // ------------------------------------------------------------------
    // Per-tick settle + snapshot
    // ------------------------------------------------------------------

    private void tick() {
        tick++;
        try {
            settleDue();
        } catch (Throwable failure) {
            logThrottled("pending settle", failure);
        }
        try {
            snapshotWatched();
        } catch (Throwable failure) {
            logThrottled("HP snapshot", failure);
        }
    }

    private void settleDue() {
        if (pending.isEmpty()) return;
        List<Pending> due = new ArrayList<>();
        pending.removeIf(record -> {
            if (record.settleTick <= tick) {
                due.add(record);
                return true;
            }
            return false;
        });
        if (due.isEmpty()) return;
        for (Pending record : due) {
            if (record.projectileId != null) castByProjectile.remove(record.projectileId);
        }
        // Group by target, preserving observation order; records on
        // one target settle as a chain measured against the running
        // HP, so a same-window volley is reconciled exactly in
        // aggregate.
        Map<UUID, List<Pending>> byTarget = new LinkedHashMap<>();
        for (Pending record : due) {
            byTarget.computeIfAbsent(record.targetId, ignored -> new ArrayList<>()).add(record);
        }
        for (Map.Entry<UUID, List<Pending>> entry : byTarget.entrySet()) {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Enderman target) || target.isDead() || !target.isValid()) continue;
            Snapshot cursor = entry.getValue().get(0).before;
            for (Pending record : entry.getValue()) {
                if (target.isDead()) break;
                Snapshot now = snapshotOf(target);
                double delta = cursor.effective() - now.effective();
                ResistanceMath.Decision decision = ResistanceMath.decision(delta, true);
                if (decision == ResistanceMath.Decision.LANDED) {
                    double restore = ResistanceMath.restoreAmount(damageFraction, delta);
                    if (restore > ResistanceMath.EPSILON) {
                        applyRestore(target, cursor, now, restore);
                    }
                    debug("landed kind=" + record.kind + " dealt=" + fmt(delta)
                            + " restored=" + fmt(restore) + " target=" + target.getUniqueId());
                } else {
                    reroute(record, target);
                }
                cursor = snapshotOf(target);
            }
            snapshots.put(target.getUniqueId(), cursor);
        }
    }

    /**
     * Re-applies one refused hit as a player-sourced damage call —
     * the shape vanilla endermen accept — so EliteMobs' own
     * pipeline values it from the shooter's gear and credits the
     * shooter. The damage the call actually dealt is measured and
     * reconciled to the landed fraction immediately (the call is
     * synchronous, so the measurement is exact).
     */
    private void reroute(Pending record, Enderman target) {
        Player shooter = Bukkit.getPlayer(record.shooterId);
        if (shooter == null || !shooter.isOnline()) {
            debug("reroute skipped (shooter offline) target=" + target.getUniqueId());
            return;
        }
        // The distance that matters is the one at observation
        // time: the hit genuinely connected (or the cast was
        // genuinely aimed) at that range. The enderman's own
        // teleport afterwards — the exact whiff this module
        // exists to repair — must not void it, so the settle-time
        // check is only a same-area sanity cap.
        if (!shooter.getWorld().equals(target.getWorld())
                || record.observedDistance > maxRerouteDistance
                || shooter.getLocation().distanceSquared(target.getLocation()) > 160.0 * 160.0) {
            debug("reroute skipped (out of range) observed=" + fmt(record.observedDistance)
                    + " target=" + target.getUniqueId());
            return;
        }
        Snapshot pre = snapshotOf(target);
        try {
            target.damage(Math.max(0.01, record.seed), shooter);
        } catch (Throwable failure) {
            logThrottled("re-applied hit", failure);
            return;
        }
        if (target.isDead()) {
            debug("rerouted kind=" + record.kind + " killed target=" + target.getUniqueId());
            return; // a killing blow stands in full, as with landed hits
        }
        Snapshot post = snapshotOf(target);
        double dealt = pre.effective() - post.effective();
        if (dealt > ResistanceMath.EPSILON) {
            double restore = ResistanceMath.restoreAmount(damageFraction, dealt);
            if (restore > ResistanceMath.EPSILON) {
                applyRestore(target, pre, post, restore);
            }
            debug("rerouted kind=" + record.kind + " seed=" + fmt(record.seed)
                    + " dealt=" + fmt(dealt) + " restored=" + fmt(restore)
                    + " target=" + target.getUniqueId());
        } else {
            debug("rerouted kind=" + record.kind + " dealt=0 target=" + target.getUniqueId());
        }
    }

    /** Restores effective HP directly — no damage/heal events are fired. */
    private void applyRestore(Enderman target, Snapshot before, Snapshot now, double amount) {
        double[] restored = ResistanceMath.restoreSplit(
                before.absorption(), before.health(), now.absorption(), now.health(), amount);
        try {
            target.setAbsorptionAmount(restored[0]);
            if (restored[1] > 0.0) target.setHealth(restored[1]);
        } catch (Throwable failure) {
            logThrottled("restore application", failure);
        }
    }

    private void snapshotWatched() {
        Set<UUID> seen = new HashSet<>();
        // Elite endermen anywhere.
        if (eliteMobsPresent) {
            try {
                for (EliteEntity elite : new ArrayList<>(EntityTracker.getEliteMobEntities().values())) {
                    LivingEntity living = elite.getLivingEntity();
                    if (living instanceof Enderman ender && !ender.isDead()) {
                        snapshots.put(ender.getUniqueId(), snapshotOf(ender));
                        seen.add(ender.getUniqueId());
                    }
                }
            } catch (Throwable ignored) {}
        }
        // Natural endermen in scope worlds.
        for (World world : Bukkit.getWorlds()) {
            if (!naturalWorldInScope(world)) continue;
            for (Enderman ender : world.getEntitiesByClass(Enderman.class)) {
                if (ender.isDead()) continue;
                snapshots.put(ender.getUniqueId(), snapshotOf(ender));
                seen.add(ender.getUniqueId());
            }
        }
        // Pending targets keep their baseline even if a watch pass
        // missed them (chunk edge, tracker lag).
        for (Pending record : pending) seen.add(record.targetId);
        snapshots.keySet().retainAll(seen);
        // Hit-projectile dedup only needs to live as long as records do.
        if (pending.isEmpty()) hitProjectiles.clear();
    }

    private static Snapshot snapshotOf(LivingEntity entity) {
        return new Snapshot(Math.max(0.0, entity.getAbsorptionAmount()),
                Math.max(0.0, entity.getHealth()));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private void debug(String message) {
        if (debug) ctx.logger().info("ENDERMAN-RESISTANCE " + message);
    }

    private static String fmt(double value) {
        return String.format("%.3f", value);
    }

    private long lastFailureLogNanos;

    private void logThrottled(String where, Throwable failure) {
        long now = System.nanoTime();
        if (now - lastFailureLogNanos < 60_000_000_000L) return;
        lastFailureLogNanos = now;
        ctx.logger().warning("Enderman Magic Resistance: failure during " + where + " ("
                + failure.getClass().getSimpleName() + ": " + failure.getMessage()
                + "); module continues.");
    }
}
