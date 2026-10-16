package com.winkreyes.rewardguard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Short-lived suppression windows planted when a DENY-classified mob dies.
 *
 * Why this exists: mcMMO's XP gain events carry the player, the skill and the
 * amount, but NOT the killed entity, so a veto listener cannot ask "which mob
 * was this XP for?". The kill itself is observable though: mcMMO grants combat
 * XP (and party share) synchronously while it processes the death, so a window
 * planted at the start of death processing (EntityDeathEvent, LOWEST priority,
 * before mcMMO's own NORMAL-priority death handler) covers exactly the gains
 * that death produces. A gain is suppressed only when ALL of these hold:
 *   - it is a kill-based gain (the caller passes killReason for PVE/SHARED_PVE),
 *   - it lands within windowTicks ticks of the death,
 *   - it is in the same world as the death,
 *   - the gaining player is the killer, or stands within radiusBlocks of the
 *     death spot (party share range; a ranged killer is covered by the
 *     killer match, not the radius).
 * Pure logic on purpose: no Bukkit types, so the policy is unit-testable
 * without a server.
 */
public final class XpSuppression {
    public record Marker(UUID killer, String world, double x, double y, double z, long deathTick) { }

    private final List<Marker> markers = new ArrayList<>();
    private final double radiusSquared;
    private final long windowTicks;

    public XpSuppression(double radiusBlocks, long windowTicks) {
        this.radiusSquared = radiusBlocks * radiusBlocks;
        this.windowTicks = windowTicks;
    }

    public synchronized void plant(UUID killer, String world, double x, double y, double z, long tick) {
        purge(tick);
        markers.add(new Marker(killer, world, x, y, z, tick));
    }

    public synchronized boolean shouldSuppress(UUID player, String world, double x, double y, double z,
                                               long tick, boolean killReason) {
        if (!killReason || player == null || world == null) return false;
        purge(tick);
        for (Marker marker : markers) {
            if (tick < marker.deathTick()) continue;
            if (!marker.world().equals(world)) continue;
            if (player.equals(marker.killer())) return true;
            double dx = x - marker.x();
            double dy = y - marker.y();
            double dz = z - marker.z();
            if (dx * dx + dy * dy + dz * dz <= radiusSquared) return true;
        }
        return false;
    }

    public synchronized int activeMarkers(long tick) {
        purge(tick);
        return markers.size();
    }

    private void purge(long tick) {
        Iterator<Marker> it = markers.iterator();
        while (it.hasNext()) {
            if (tick - it.next().deathTick() > windowTicks) it.remove();
        }
    }
}
