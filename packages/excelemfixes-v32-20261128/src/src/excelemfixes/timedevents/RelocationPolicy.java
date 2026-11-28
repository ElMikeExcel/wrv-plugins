package excelemfixes.timedevents;

/**
 * The spawn leg's decisions, pure: whether a freshly spawned
 * timed-event boss needs moving at all, and how far out a
 * relocated boss should land. The block-level safety checks
 * live in the EliteMobs bridge (they need a live world); the
 * policy here only ever answers distance questions.
 *
 * EliteMobs' own timed-event spawner already anchors on a
 * random eligible online player, but it scatters the boss
 * 24-128 blocks per axis away (up to ~180 blocks diagonally,
 * in an unknown direction, through whatever terrain lies
 * between). This module's band is deliberately tighter: a
 * boss further than relocation-max-distance from the NEAREST
 * player is re-placed inside [min, max] of that player.
 */
public final class RelocationPolicy {

    public enum Decision { STAY, MOVE }

    private RelocationPolicy() {}

    /**
     * MOVE when the boss's horizontal distance to the nearest
     * player in its world exceeds the configured maximum.
     * A negative distance means "no player in the world" —
     * the event is left exactly as EliteMobs placed it.
     */
    public static Decision decide(double nearestPlayerDistance, double maxDistance) {
        if (nearestPlayerDistance < 0) return Decision.STAY;
        return nearestPlayerDistance > maxDistance ? Decision.MOVE : Decision.STAY;
    }

    /**
     * The distance a relocated boss lands from its anchor
     * player: min + roll x (max - min), with the roll clamped
     * to [0, 1]. The caller supplies the roll so tests are
     * deterministic; the bridge passes a random one.
     */
    public static double targetDistance(double roll01, double min, double max) {
        double roll = Math.max(0.0, Math.min(1.0, roll01));
        if (max < min) max = min;
        return min + roll * (max - min);
    }
}
