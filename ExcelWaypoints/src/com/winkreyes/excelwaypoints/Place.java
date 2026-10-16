package com.winkreyes.excelwaypoints;

import java.util.UUID;

/** One saved place. Plain data, no Bukkit types, so it is easy to test. */
public final class Place {
    public enum Type {
        HOUSE("House"), FARM("Farm"), MINE("Mine"), OTHER("Other");
        public final String label;
        Type(String l) { label = l; }
        public static Type parse(String s) {
            if (s != null) for (Type t : values()) if (t.name().equalsIgnoreCase(s.trim()) || t.label.equalsIgnoreCase(s.trim())) return t;
            return OTHER;
        }
    }

    public final String id;
    public final UUID owner;
    public String ownerName;
    public String name;
    public Type type;
    public final String world;
    public final double x, y, z;
    public boolean shareable;
    public boolean allowVisit;
    public final long created;

    public Place(String id, UUID owner, String ownerName, String name, Type type, String world,
                 double x, double y, double z, boolean shareable, boolean allowVisit, long created) {
        this.id = id; this.owner = owner; this.ownerName = ownerName; this.name = name; this.type = type;
        this.world = world; this.x = x; this.y = y; this.z = z;
        this.shareable = shareable; this.allowVisit = allowVisit; this.created = created;
    }
}
