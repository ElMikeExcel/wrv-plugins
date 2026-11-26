package excelemfixes.witherskulls;

/**
 * The immutable settings snapshot: read from config at enable and
 * at /witherskulls reload, then swapped atomically (a reload can
 * never leave a half-read config live). Every value is clamped,
 * never thrown — a typo in the file degrades to the nearest sane
 * value, with the raw problems reported by the loader, in the
 * RaidBuffConfig house style.
 */
public record SkullSettings(boolean enabled, double targetChancePercent, boolean debugLog) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final double DEFAULT_TARGET_PERCENT = 25.0;
    public static final boolean DEFAULT_DEBUG_LOG = false;

    public static SkullSettings defaults() {
        return new SkullSettings(DEFAULT_ENABLED, DEFAULT_TARGET_PERCENT, DEFAULT_DEBUG_LOG);
    }

    /**
     * Build a snapshot from raw config values (nulls = keys absent).
     * The target is clamped into [0, 100]; a non-finite target is
     * treated as absent and falls back to the default.
     */
    public static SkullSettings of(Boolean enabled, Double targetChancePercent, Boolean debugLog) {
        boolean e = enabled != null ? enabled : DEFAULT_ENABLED;
        double t = DEFAULT_TARGET_PERCENT;
        if (targetChancePercent != null && Double.isFinite(targetChancePercent)) {
            t = SkullMath.clampPercent(targetChancePercent);
        }
        boolean d = debugLog != null ? debugLog : DEFAULT_DEBUG_LOG;
        return new SkullSettings(e, t, d);
    }
}
