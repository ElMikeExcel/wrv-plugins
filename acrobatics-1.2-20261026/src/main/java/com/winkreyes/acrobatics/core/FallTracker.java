package com.winkreyes.acrobatics.core;

/** Per-player fall tracking for water landings. Pure, no Bukkit types.
 *
 * 1.0 bug class this fixes: the game zeroes its fall counter the moment a
 * player touches water, and a landing packet can carry onGround=true in
 * the same move that enters the water (shallow puddles), which wiped the
 * remembered peak before the water check ran. Here the water check on the
 * DESTINATION position runs first, and the peak is backed up by the
 * highest airborne Y so a puddle landing still measures the real drop. */
public final class FallTracker {

    private float peak;
    private float lastGameFall;
    private double airTopY;
    private boolean airborne;
    private boolean latched;

    /** One move step.
     * @param inWaterAtDestination water at the position being moved to
     * @param onGround client says on ground
     * @param resetsFall climbing, cobweb, slime etc.: this move cancels the fall
     * @param gameFall the game's own fall counter right now
     * @param toY destination Y
     * @return fall distance in blocks if this move ends a qualifying
     *         water landing, else -1 */
    public float step(boolean inWaterAtDestination, boolean onGround, boolean resetsFall,
                      float gameFall, double toY, double minFallBlocks) {
        if (inWaterAtDestination) {
            float best = peak;
            if (gameFall > best) {
                best = gameFall;
            }
            if (airborne) {
                float drop = (float) (airTopY - toY);
                if (drop > best) {
                    best = drop;
                }
            }
            float result = -1;
            if (!latched && best >= minFallBlocks) {
                latched = true;
                result = best;
            }
            clearAir();
            return result;
        }
        if (onGround) {
            clearAir();
            latched = false;
            return -1;
        }
        if (resetsFall || (lastGameFall > 0 && gameFall == 0)) {
            peak = 0;
            airTopY = toY;
            airborne = true;
            lastGameFall = gameFall;
            return -1;
        }
        if (!airborne) {
            airborne = true;
            airTopY = toY;
        } else if (toY > airTopY) {
            airTopY = toY;
        }
        if (gameFall > peak) {
            peak = gameFall;
        }
        lastGameFall = gameFall;
        return -1;
    }

    /** Teleport, respawn, world change: forget the fall. */
    public void reset() {
        clearAir();
        latched = false;
    }

    private void clearAir() {
        peak = 0;
        lastGameFall = 0;
        airTopY = 0;
        airborne = false;
    }
}

