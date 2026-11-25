package excelemfixes.questbeacon;

/**
 * Where the guide beam stands, in pure numbers (no Bukkit), so
 * the plain-JVM suites exercise it directly.
 *
 * THE SHAPE (Mike, Nov 22, 2026, 11:44 PM CT): a beam planted AT
 * a far destination cannot be seen from far away — a beacon beam
 * (and any pillar of light) only renders inside the client's
 * loaded-chunk / view-distance range, and no plugin can lift that
 * client-side limit. So the single guide beam RIDES AT THE EDGE
 * of the player's view in the destination's direction: a horizon
 * pillar that leads the player in from any distance, repositioned
 * as they move. Once the destination itself is inside that range,
 * the beam SETTLES onto the real destination. One beam, direction
 * without a prescribed path — the breadcrumb chain of beacons
 * Mike considered and dropped the same night is deliberately NOT
 * implemented here.
 */
public final class BeamGeometry {

    private BeamGeometry() {}

    /**
     * The beam's standing spot for one refresh.
     *
     * @param x        beam column X
     * @param z        beam column Z
     * @param settled  true when the beam stands on the destination
     *                 itself; false when it rides the view edge
     * @param distance horizontal distance player -> destination
     */
    public record Anchor(double x, double z, boolean settled,
                         double distance) {}

    /** Horizontal distance between two points. */
    public static double horizontalDistance(double ax, double az,
                                            double bx, double bz) {
        return Math.hypot(bx - ax, bz - az);
    }

    /**
     * The anchor for a player at (px, pz) guided to (dx, dz):
     * settled on the destination when it is within edgeDistance,
     * otherwise exactly edgeDistance from the player along the
     * straight line to the destination.
     */
    public static Anchor anchor(double px, double pz,
                                double dx, double dz,
                                double edgeDistance) {
        double vx = dx - px;
        double vz = dz - pz;
        double dist = Math.hypot(vx, vz);
        if (dist <= edgeDistance) {
            return new Anchor(dx, dz, true, dist);
        }
        double scale = edgeDistance / dist;
        return new Anchor(px + vx * scale, pz + vz * scale, false, dist);
    }
}
