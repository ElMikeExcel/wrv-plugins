package excelemfixes.timedevents;

/**
 * Per-player "where is it from me" text for the broadcast:
 * an 8-way compass direction plus a rounded block distance.
 * Pure math — no Bukkit. Minecraft axes: -Z is north, +Z is
 * south, +X is east, -X is west.
 */
public final class CompassText {

    private static final String[] SECTORS = {
            "north", "north-east", "east", "south-east",
            "south", "south-west", "west", "north-west"
    };

    private CompassText() {}

    /**
     * The 8-way compass direction of the offset (dx, dz) —
     * target minus viewer. A zero offset reads "north" (the
     * caller only asks when the target is measurably away).
     */
    public static String direction8(double dx, double dz) {
        double bearing = Math.toDegrees(Math.atan2(dx, -dz));
        if (bearing < 0) bearing += 360.0;
        int sector = (int) Math.round(bearing / 45.0) % 8;
        return SECTORS[sector];
    }

    /** Horizontal distance in whole blocks, rounded. */
    public static long blocks(double dx, double dz) {
        return Math.round(Math.hypot(dx, dz));
    }

    /**
     * The guidance clause for a viewer in the event's world,
     * e.g. "87 blocks north-east of you". dx/dz are
     * target minus viewer.
     */
    public static String guidance(double dx, double dz) {
        return blocks(dx, dz) + " blocks " + direction8(dx, dz) + " of you";
    }
}
