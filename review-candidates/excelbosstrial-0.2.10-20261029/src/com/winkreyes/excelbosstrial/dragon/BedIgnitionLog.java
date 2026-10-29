/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bed-ignition attribution for the dragon share gate (0.2.1,
 * triple-check FLAG 3). A bed lit in the End explodes as a
 * BLOCK_EXPLOSION with no damager entity, so Bukkit attributes
 * its damage to nobody - and under the share gate every bed hit
 * would count AGAINST the family, denying exactly the bed-led
 * fight Mike sanctioned ("cap, don't ban": the caps already
 * bound beds to 60/event, 75/tick).
 *
 * So the chapter remembers who lit what, where, and when: a
 * PlayerInteractEvent on a bed block in the End records an
 * ignition, and a BLOCK_EXPLOSION damage event near that block
 * within a few ticks is attributed to the lighter. The radius
 * is measured from the explosion's victim (the dragon, whose
 * location sits inside a very large hitbox) to the bed block:
 * 10 blocks covers a perch bed going off at her head from her
 * location point at her feet, while the 5-tick window keeps
 * two players' sequential beds attributed to their own lighters
 * (the most recent matching ignition wins).
 *
 * Pure storage, Bukkit-free, in the dragon package's style:
 * the chapter supplies positions and ticks.
 */
public final class BedIgnitionLog {

    /** How near (3D blocks) a bed ignition explains an explosion. */
    public static final double RADIUS_BLOCKS = 10.0;
    /** How many ticks after ignition an explosion still names its lighter. */
    public static final int WINDOW_TICKS = 5;
    /** Entries are pruned once they are far outside the window. */
    public static final int PRUNE_TICKS = 40;

    private record Ignition(UUID player, double x, double y, double z, long tick) {
    }

    private final List<Ignition> ignitions = new ArrayList<>();

    /** Record one bed ignition (the adapter passes the block's center). */
    public synchronized void record(UUID player, double x, double y, double z, long tick) {
        if (player == null) {
            return;
        }
        prune(tick);
        ignitions.add(new Ignition(player, x, y, z, tick));
    }

    /**
     * The lighter of the most recent ignition within
     * {@link #RADIUS_BLOCKS} of the point and {@link #WINDOW_TICKS}
     * of the tick, or null when no ignition explains the blast.
     */
    public synchronized UUID lighterNear(double x, double y, double z, long tick) {
        UUID best = null;
        long bestTick = Long.MIN_VALUE;
        for (Ignition i : ignitions) {
            long dt = tick - i.tick();
            if (dt < 0 || dt > WINDOW_TICKS) {
                continue;
            }
            double dx = i.x() - x;
            double dy = i.y() - y;
            double dz = i.z() - z;
            double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (d <= RADIUS_BLOCKS && i.tick() >= bestTick) {
                best = i.player();
                bestTick = i.tick();
            }
        }
        return best;
    }

    /** Drop ignitions far outside the attribution window. */
    public synchronized void prune(long nowTick) {
        ignitions.removeIf(i -> nowTick - i.tick() > PRUNE_TICKS);
    }

    /** Ignitions currently held (status/tests). */
    public synchronized int size() {
        return ignitions.size();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

