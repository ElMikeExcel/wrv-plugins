package com.winkreyes.excelwaypoints;

/** Distance and direction words. Minecraft: +x is east, +z is south. */
public final class Compass {
    private Compass() {}
    private static final String[] WORDS = {"north", "north-east", "east", "south-east", "south", "south-west", "west", "north-west"};
    private static final String[] SHORT = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final String[] ARROW = {"\u2191", "\u2197", "\u2192", "\u2198", "\u2193", "\u2199", "\u2190", "\u2196"};

    static int octant(double dx, double dz) {
        double deg = Math.toDegrees(Math.atan2(dx, -dz));       // 0 = north, 90 = east
        if (deg < 0) deg += 360;
        return (int) Math.floor((deg + 22.5) / 45.0) % 8;
    }
    public static int distance(double dx, double dz) { return (int) Math.round(Math.sqrt(dx * dx + dz * dz)); }
    public static String word(double dx, double dz) { return WORDS[octant(dx, dz)]; }
    public static String shortWord(double dx, double dz) { return SHORT[octant(dx, dz)]; }
    public static String arrow(double dx, double dz) { return ARROW[octant(dx, dz)]; }

    /** "120 blocks north-east", "1 block west", or "right here". Flat distance (x and z). */
    public static String describe(double dx, double dz) {
        int d = distance(dx, dz);
        if (d < 3) return "right here";
        return d + (d == 1 ? " block " : " blocks ") + word(dx, dz);
    }
    /** Short form for phone buttons: "120 NE". */
    public static String brief(double dx, double dz) {
        int d = distance(dx, dz);
        if (d < 3) return "here";
        return d + " " + shortWord(dx, dz);
    }
}
