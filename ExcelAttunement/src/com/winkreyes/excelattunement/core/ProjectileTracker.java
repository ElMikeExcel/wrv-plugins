package com.winkreyes.excelattunement.core;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-flight martial projectiles: projectile id -> (shooter, kind).
 *
 * A bow/crossbow/trident hit lands as an EntityDamageByEntityEvent
 * whose damager is the projectile, not the player, and the event does
 * not carry the weapon. The glue records every player launch of an
 * arrow-family or trident projectile here; the damage layer resolves
 * the shooter + kind from this tracker. Damage is judged AT LANDING:
 * an arrow loosed before the shooter entered caster stance is still
 * zeroed if it lands while caster is active, and a missed shot simply
 * never produces a damage event, so a miss can never set a stance.
 * Impact only MARKS a record spent: Paper fires ProjectileHitEvent
 * before the damage event of the same impact, so records stay
 * resolvable for a short grace window after impact and are swept
 * afterwards (or by age).
 */
public final class ProjectileTracker {
    public record Shot(UUID shooter, WeaponKind kind, long launchedAt, long hitAt) {
        boolean inFlight() {
            return hitAt < 0L;
        }
    }

    /** Records older than this are swept (an arrow never flies this long). */
    public static final long MAX_AGE_MS = 60_000L;
    /**
     * A hit record stays resolvable this long after impact. On Paper,
     * ProjectileHitEvent fires BEFORE the EntityDamageByEntityEvent of
     * the same impact (proven on the scratch rig, Oct 19, 2026), so
     * forgetting at hit time would blind the damage layer entirely.
     */
    public static final long HIT_GRACE_MS = 2_000L;
    /** Defensive cap so a pathological launch storm cannot grow the map. */
    public static final int MAX_ENTRIES = 4096;

    private final Map<UUID, Shot> shots = new HashMap<>();

    public void record(UUID projectileId, UUID shooter, WeaponKind kind, long now) {
        if (projectileId == null || shooter == null || kind == null || !kind.isMartial()) return;
        if (shots.size() >= MAX_ENTRIES) sweep(now);
        if (shots.size() >= MAX_ENTRIES) return;
        shots.put(projectileId, new Shot(shooter, kind, now, -1L));
    }

    public Optional<Shot> resolve(UUID projectileId, long now) {
        if (projectileId == null) return Optional.empty();
        Shot shot = shots.get(projectileId);
        if (shot == null) return Optional.empty();
        if (!shot.inFlight() && now - shot.hitAt() > HIT_GRACE_MS) return Optional.empty();
        return Optional.of(shot);
    }

    /** The projectile hit something: mark it spent (see HIT_GRACE_MS). */
    public void markHit(UUID projectileId, long now) {
        if (projectileId == null) return;
        Shot shot = shots.get(projectileId);
        if (shot != null && shot.inFlight()) {
            shots.put(projectileId, new Shot(shot.shooter(), shot.kind(), shot.launchedAt(), now));
        }
    }

    /** Drop a record outright. */
    public void forget(UUID projectileId) {
        if (projectileId != null) shots.remove(projectileId);
    }

    /** Drop everything one shooter has in flight (quit/death cleanup). */
    public void forgetShooter(UUID shooter) {
        shots.values().removeIf(s -> s.shooter().equals(shooter));
    }

    /** Drop spent records past their grace and stale in-flight records. */
    public int sweep(long now) {
        int dropped = 0;
        Iterator<Map.Entry<UUID, Shot>> it = shots.entrySet().iterator();
        while (it.hasNext()) {
            Shot shot = it.next().getValue();
            boolean spent = !shot.inFlight() && now - shot.hitAt() > HIT_GRACE_MS;
            boolean stale = now - shot.launchedAt() > MAX_AGE_MS;
            if (spent || stale) {
                it.remove();
                dropped++;
            }
        }
        return dropped;
    }

    public int size() {
        return shots.size();
    }

    public void clear() {
        shots.clear();
    }
}
