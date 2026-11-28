package excelemfixes.timedevents;

/**
 * The immutable settings snapshot for the timed-event-guide
 * module: read from config at enable and at /eventguide reload,
 * then swapped atomically. A typo in the file degrades to the
 * nearest sane value (the house clamping style of
 * PartyDropsSettings / SkullSettings).
 *
 * Shipped defaults are generic balance choices (D246 — no
 * server-specific names, paths or content IDs): relocate a
 * timed event's boss when EliteMobs' own scatter left it more
 * than 48 blocks from the nearest player, land it 20-48 blocks
 * out, keep the vanilla far-away despawn from eating it, tell
 * the world it happened, and stand a pillar of light on it.
 */
public record TimedEventSettings(boolean enabled,
                                 boolean relocate,
                                 double relocationMinDistance,
                                 double relocationMaxDistance,
                                 int safeSpotAttempts,
                                 boolean preventFarDespawn,
                                 boolean broadcast,
                                 boolean broadcastServerWide,
                                 boolean beacon,
                                 double beaconViewDistance,
                                 int columnHeight,
                                 int columnStep,
                                 int refreshTicks,
                                 int pendingTimeoutSeconds,
                                 int maxTrackMinutes,
                                 boolean debug) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final boolean DEFAULT_RELOCATE = true;
    public static final double DEFAULT_RELOCATION_MIN_DISTANCE = 20.0;
    public static final double DEFAULT_RELOCATION_MAX_DISTANCE = 48.0;
    public static final int DEFAULT_SAFE_SPOT_ATTEMPTS = 24;
    public static final boolean DEFAULT_PREVENT_FAR_DESPAWN = true;
    public static final boolean DEFAULT_BROADCAST = true;
    public static final boolean DEFAULT_BROADCAST_SERVER_WIDE = false;
    public static final boolean DEFAULT_BEACON = true;
    public static final double DEFAULT_BEACON_VIEW_DISTANCE = 256.0;
    public static final int DEFAULT_COLUMN_HEIGHT = 40;
    public static final int DEFAULT_COLUMN_STEP = 2;
    public static final int DEFAULT_REFRESH_TICKS = 10;
    public static final int DEFAULT_PENDING_TIMEOUT_SECONDS = 300;
    public static final int DEFAULT_MAX_TRACK_MINUTES = 40;
    public static final boolean DEFAULT_DEBUG = false;

    public static TimedEventSettings defaults() {
        return new TimedEventSettings(DEFAULT_ENABLED, DEFAULT_RELOCATE,
                DEFAULT_RELOCATION_MIN_DISTANCE, DEFAULT_RELOCATION_MAX_DISTANCE,
                DEFAULT_SAFE_SPOT_ATTEMPTS, DEFAULT_PREVENT_FAR_DESPAWN,
                DEFAULT_BROADCAST, DEFAULT_BROADCAST_SERVER_WIDE,
                DEFAULT_BEACON, DEFAULT_BEACON_VIEW_DISTANCE,
                DEFAULT_COLUMN_HEIGHT, DEFAULT_COLUMN_STEP,
                DEFAULT_REFRESH_TICKS, DEFAULT_PENDING_TIMEOUT_SECONDS,
                DEFAULT_MAX_TRACK_MINUTES, DEFAULT_DEBUG);
    }

    /**
     * Build a snapshot from raw config values (nulls = keys
     * absent), clamped: relocation min in [0, 128], max in
     * [min, 256] (a max below the min is raised to the min),
     * safe-spot attempts in [1, 40], beacon view distance in
     * [16, 1024], column height in [4, 120], column step in
     * [1, 10], refresh ticks in [2, 200], pending timeout in
     * [30, 3600] seconds, max track in [1, 240] minutes.
     * Non-finite numbers fall back to the defaults above.
     */
    public static TimedEventSettings of(Boolean enabled, Boolean relocate,
                                        Double relocationMinDistance,
                                        Double relocationMaxDistance,
                                        Integer safeSpotAttempts,
                                        Boolean preventFarDespawn,
                                        Boolean broadcast,
                                        Boolean broadcastServerWide,
                                        Boolean beacon,
                                        Double beaconViewDistance,
                                        Integer columnHeight,
                                        Integer columnStep,
                                        Integer refreshTicks,
                                        Integer pendingTimeoutSeconds,
                                        Integer maxTrackMinutes,
                                        Boolean debug) {
        boolean e = enabled != null ? enabled : DEFAULT_ENABLED;
        boolean r = relocate != null ? relocate : DEFAULT_RELOCATE;
        double min = DEFAULT_RELOCATION_MIN_DISTANCE;
        if (relocationMinDistance != null && Double.isFinite(relocationMinDistance)
                && relocationMinDistance >= 0.0 && relocationMinDistance <= 128.0) {
            min = relocationMinDistance;
        }
        double max = DEFAULT_RELOCATION_MAX_DISTANCE;
        if (relocationMaxDistance != null && Double.isFinite(relocationMaxDistance)
                && relocationMaxDistance >= min && relocationMaxDistance <= 256.0) {
            max = relocationMaxDistance;
        }
        if (max < min) max = min;
        int attempts = DEFAULT_SAFE_SPOT_ATTEMPTS;
        if (safeSpotAttempts != null && safeSpotAttempts >= 1 && safeSpotAttempts <= 40) {
            attempts = safeSpotAttempts;
        }
        boolean pfd = preventFarDespawn != null ? preventFarDespawn : DEFAULT_PREVENT_FAR_DESPAWN;
        boolean bc = broadcast != null ? broadcast : DEFAULT_BROADCAST;
        boolean bcw = broadcastServerWide != null ? broadcastServerWide : DEFAULT_BROADCAST_SERVER_WIDE;
        boolean bn = beacon != null ? beacon : DEFAULT_BEACON;
        double view = DEFAULT_BEACON_VIEW_DISTANCE;
        if (beaconViewDistance != null && Double.isFinite(beaconViewDistance)
                && beaconViewDistance >= 16.0 && beaconViewDistance <= 1024.0) {
            view = beaconViewDistance;
        }
        int height = DEFAULT_COLUMN_HEIGHT;
        if (columnHeight != null && columnHeight >= 4 && columnHeight <= 120) {
            height = columnHeight;
        }
        int step = DEFAULT_COLUMN_STEP;
        if (columnStep != null && columnStep >= 1 && columnStep <= 10) {
            step = columnStep;
        }
        int refresh = DEFAULT_REFRESH_TICKS;
        if (refreshTicks != null && refreshTicks >= 2 && refreshTicks <= 200) {
            refresh = refreshTicks;
        }
        int pending = DEFAULT_PENDING_TIMEOUT_SECONDS;
        if (pendingTimeoutSeconds != null && pendingTimeoutSeconds >= 30
                && pendingTimeoutSeconds <= 3600) {
            pending = pendingTimeoutSeconds;
        }
        int track = DEFAULT_MAX_TRACK_MINUTES;
        if (maxTrackMinutes != null && maxTrackMinutes >= 1 && maxTrackMinutes <= 240) {
            track = maxTrackMinutes;
        }
        boolean d = debug != null ? debug : DEFAULT_DEBUG;
        return new TimedEventSettings(e, r, min, max, attempts, pfd, bc, bcw, bn,
                view, height, step, refresh, pending, track, d);
    }
}
