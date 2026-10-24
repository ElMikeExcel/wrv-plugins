package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Launch-time position snapshots for player-fired projectiles
 * (0.2.1, triple-check FLAG 4). The warden's share bands judge
 * the RESPONSIBLE PLAYER's position - but judging it when the
 * arrow LANDS invites the pearl trick: fire from outside the
 * band, relocate inside it before impact, and the out-of-band
 * shot counts. The adapter records the shooter's position at
 * ProjectileLaunchEvent, keyed by the projectile's UUID; the
 * band judgment then reads the LAUNCH position - where the
 * shot was actually taken from.
 *
 * Pure storage, Bukkit-free: the adapter supplies positions and
 * timestamps. Entries are pruned by age from the maintenance
 * pass (a projectile's damage lands within seconds of launch;
 * 30 s is generous) - a hit is deliberately NOT forgotten on
 * impact, because the hit event precedes the damage event it
 * causes in the same tick.
 */
public final class LaunchSnapshotLog {

    private record Snap(double x, double y, double z, long nowMs) {
    }

    private final Map<UUID, Snap> snaps = new HashMap<>();

    /** Record (or overwrite) the launch position of a projectile. */
    public synchronized void record(UUID projectile, double x, double y, double z,
                                    long nowMs) {
        if (projectile == null) {
            return;
        }
        snaps.put(projectile, new Snap(x, y, z, nowMs));
    }

    /** The launch position {x, y, z} of a projectile, or null when none was recorded. */
    public synchronized double[] lookup(UUID projectile) {
        Snap s = projectile == null ? null : snaps.get(projectile);
        return s == null ? null : new double[] {s.x(), s.y(), s.z()};
    }

    /** Drop every snapshot older than {@code maxAgeMs} (maintenance hygiene). */
    public synchronized void prune(long nowMs, long maxAgeMs) {
        snaps.entrySet().removeIf(e -> nowMs - e.getValue().nowMs() > maxAgeMs);
    }

    /** Snapshots currently held (status/tests). */
    public synchronized int size() {
        return snaps.size();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
