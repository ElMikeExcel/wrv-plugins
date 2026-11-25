package excelemfixes.enderman;

import java.util.List;
import java.util.UUID;

/**
 * Pure cone selection for inferring a magic cast's intended
 * target: given a direction (a projectile's initial velocity or
 * the caster's look vector) and the candidate endermen as
 * offsets from the cast origin, pick the candidate inside the
 * cone that the direction points at most directly (smallest
 * angle; ties break to the nearest). No Bukkit types.
 */
public final class CastTargeting {

    private CastTargeting() {}

    /** One candidate target as an offset from the cast origin. */
    public record Candidate(UUID id, double dx, double dy, double dz) {}

    /**
     * @param dirX/dirY/dirZ the cast direction (need not be normalized)
     * @param maxDistance    candidates farther than this are ignored
     * @param minDot         minimum cosine of the angle between the
     *                       direction and the candidate offset
     * @return the chosen candidate's id, or null when none qualifies
     */
    public static UUID select(double dirX, double dirY, double dirZ,
                              List<Candidate> candidates,
                              double maxDistance, double minDot) {
        double dirLen = Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (!(dirLen > 0.0) || candidates == null) return null;
        UUID best = null;
        double bestDot = minDot;
        double bestDist = Double.MAX_VALUE;
        for (Candidate c : candidates) {
            if (c == null || c.id() == null) continue;
            double dist = Math.sqrt(c.dx() * c.dx() + c.dy() * c.dy() + c.dz() * c.dz());
            if (dist < 0.5 || dist > maxDistance) continue;
            double dot = (dirX * c.dx() + dirY * c.dy() + dirZ * c.dz()) / (dirLen * dist);
            if (dot < minDot) continue;
            if (best == null || dot > bestDot + 1.0e-9
                    || (Math.abs(dot - bestDot) <= 1.0e-9 && dist < bestDist)) {
                best = c.id();
                bestDot = dot;
                bestDist = dist;
            }
        }
        return best;
    }
}
