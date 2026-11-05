package com.winkreyes.excellandmarks.core;

import java.util.UUID;

/**
 * One registered spot (SPEC §1). Mutable record owned by the Registry;
 * the Bukkit layer persists it to spots.yml on every change.
 */
public final class Spot {

    public enum Type { LANDMARK, PLOT }
    public enum Tier { SPOT, LANDMARK, HEART }
    public enum Mode { NONE, RENTED, OWNED }

    public final String id;
    public final String world;
    public final int x, y, z;
    public String name;
    public final Type type;
    public final Tier tier;
    public long rentCents;
    public long buyCents;
    /** PLOT only: half-extent of the square region centred on the sign. */
    public final int size;
    public final long createdMs;

    public UUID ownerId;          // null = available
    public String ownerName;      // last-known name, for signs/menus while offline
    public Mode mode = Mode.NONE;
    public long paidThroughMs;    // RENTED only
    public Long claimId;          // GP subdivision id, PLOT only, nullable

    public Spot(String id, String world, int x, int y, int z, String name, Type type, Tier tier,
                long rentCents, long buyCents, int size, long createdMs) {
        this.id = id;
        this.world = world;
        this.x = x; this.y = y; this.z = z;
        this.name = name;
        this.type = type;
        this.tier = tier;
        this.rentCents = rentCents;
        this.buyCents = buyCents;
        this.size = size;
        this.createdMs = createdMs;
    }

    public boolean isAvailable() { return ownerId == null; }
    public boolean isPlot() { return type == Type.PLOT; }

    public String signKey() { return world + "@" + x + "," + y + "," + z; }

    /** Region membership (SPEC §5): square on X/Z, config Y band around the sign. */
    public boolean inRegion(String w, int bx, int by, int bz, int yBelow, int yAbove) {
        if (!world.equals(w)) return false;
        if (Math.abs(bx - x) > size || Math.abs(bz - z) > size) return false;
        return by >= y - yBelow && by <= y + yAbove;
    }

    public void clearOwnership() {
        ownerId = null;
        ownerName = null;
        mode = Mode.NONE;
        paidThroughMs = 0L;
        claimId = null;
    }
}
