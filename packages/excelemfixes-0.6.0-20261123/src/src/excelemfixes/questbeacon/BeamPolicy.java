package excelemfixes.questbeacon;

/**
 * When the guide beam shows, and in which shape — the settled
 * design's end conditions as one pure decision (no Bukkit), so
 * the plain-JVM suites exercise every branch directly.
 *
 * The beam lives only while a quest is TRACKED (selected in
 * EliteMobs' tracker). It ends on:
 * - untrack (no tracked quest, or the player turned their own
 *   beam off),
 * - completion (a completed quest leaves the tracker, so it
 *   reads as no-track / a different tracked quest),
 * - arrival (the player is within the arrival radius of the
 *   destination — the beam has done its job; /questwhere and the
 *   glow still work from there),
 * and it never shows for a quest the destination table does not
 * cover, or while the player is in a different world than the
 * destination (a direction across worlds means nothing).
 */
public final class BeamPolicy {

    private BeamPolicy() {}

    public enum Mode {
        /** Beam rides the edge of the player's view. */
        SHOW_EDGE,
        /** Beam stands on the destination itself. */
        SHOW_SETTLED,
        /** The player turned their own beam off. */
        HIDE_PLAYER_OFF,
        /** No quest is tracked right now. */
        HIDE_NO_TRACK,
        /** The tracked quest has no destination-table row. */
        HIDE_UNCOVERED,
        /** Player and destination are in different worlds. */
        HIDE_WRONG_WORLD,
        /** The player has arrived (within the arrival radius). */
        HIDE_ARRIVED
    }

    /**
     * @param playerOff    the player's own /questbeacon off switch
     * @param trackedFile  the tracked quest's file, or null
     * @param covered      the destination table has a row for it
     * @param sameWorld    player world == destination world
     * @param horizDist    horizontal distance to the destination
     * @param arrivalRadius arrival distance (beam ends at/below)
     * @param edgeDistance  view-edge distance (settle at/below)
     */
    public static Mode decide(boolean playerOff, String trackedFile,
                              boolean covered, boolean sameWorld,
                              double horizDist, double arrivalRadius,
                              double edgeDistance) {
        if (playerOff) return Mode.HIDE_PLAYER_OFF;
        if (trackedFile == null || trackedFile.isBlank()) {
            return Mode.HIDE_NO_TRACK;
        }
        if (!covered) return Mode.HIDE_UNCOVERED;
        if (!sameWorld) return Mode.HIDE_WRONG_WORLD;
        if (horizDist <= arrivalRadius) return Mode.HIDE_ARRIVED;
        if (horizDist <= edgeDistance) return Mode.SHOW_SETTLED;
        return Mode.SHOW_EDGE;
    }

    /** True for the two modes that draw the beam. */
    public static boolean shows(Mode mode) {
        return mode == Mode.SHOW_EDGE || mode == Mode.SHOW_SETTLED;
    }
}
