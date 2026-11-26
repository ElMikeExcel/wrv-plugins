package excelemfixes.partydrops;

/**
 * The immutable settings snapshot for the party-drops module:
 * read from config at enable and at /partydrops reload, then
 * swapped atomically. Clamping replicates the standalone
 * ExcelPartyDrops 0.1.0 loadSettings() ranges exactly (a typo in
 * the file degrades to the nearest sane value; the loader logs
 * the raw problems, in the house style of SkullSettings).
 *
 * Shipped defaults ARE the spec values (PARTY-DROPS-SPEC-2026-10-20
 * section 8): radius 50.0, step 0.5, multiplier cap 3.0,
 * participant cap 5, hint cooldown 600 s, debug off,
 * respect-farming-cap off (a reserved no-op key, as in 0.1.0).
 */
public record PartyDropsSettings(boolean enabled, double partyRadius,
                                 double stepPerExtra, double multiplierCap,
                                 int participantCap, boolean respectFarmingCap,
                                 long hintCooldownSeconds, boolean debug) {

    public static final boolean DEFAULT_ENABLED = true;
    public static final double DEFAULT_RADIUS = 50.0;
    public static final double DEFAULT_STEP = 0.5;
    public static final double DEFAULT_MULTIPLIER_CAP = 3.0;
    public static final int DEFAULT_PARTICIPANT_CAP = 5;
    public static final boolean DEFAULT_RESPECT_FARMING_CAP = false;
    public static final long DEFAULT_HINT_COOLDOWN_SECONDS = 600L;
    public static final boolean DEFAULT_DEBUG = false;

    public static PartyDropsSettings defaults() {
        return new PartyDropsSettings(DEFAULT_ENABLED, DEFAULT_RADIUS,
                DEFAULT_STEP, DEFAULT_MULTIPLIER_CAP, DEFAULT_PARTICIPANT_CAP,
                DEFAULT_RESPECT_FARMING_CAP, DEFAULT_HINT_COOLDOWN_SECONDS,
                DEFAULT_DEBUG);
    }

    /**
     * Build a snapshot from raw config values (nulls = keys absent),
     * with the standalone 0.1.0 clamps: radius in (0, 200], step in
     * [0, 2], multiplier cap in [1, 10], participant cap in [1, 20],
     * hint cooldown in [0, 86400] seconds; out-of-range or
     * non-finite values fall back to the defaults above.
     */
    public static PartyDropsSettings of(Boolean enabled, Double partyRadius,
                                        Double stepPerExtra, Double multiplierCap,
                                        Integer participantCap, Boolean respectFarmingCap,
                                        Long hintCooldownSeconds, Boolean debug) {
        boolean e = enabled != null ? enabled : DEFAULT_ENABLED;
        double r = DEFAULT_RADIUS;
        if (partyRadius != null && Double.isFinite(partyRadius)
                && partyRadius > 0.0 && partyRadius <= 200.0) {
            r = partyRadius;
        }
        double s = DEFAULT_STEP;
        if (stepPerExtra != null && Double.isFinite(stepPerExtra)
                && stepPerExtra >= 0.0 && stepPerExtra <= 2.0) {
            s = stepPerExtra;
        }
        double c = DEFAULT_MULTIPLIER_CAP;
        if (multiplierCap != null && Double.isFinite(multiplierCap)
                && multiplierCap >= 1.0 && multiplierCap <= 10.0) {
            c = multiplierCap;
        }
        int p = DEFAULT_PARTICIPANT_CAP;
        if (participantCap != null && participantCap >= 1 && participantCap <= 20) {
            p = participantCap;
        }
        boolean f = respectFarmingCap != null ? respectFarmingCap : DEFAULT_RESPECT_FARMING_CAP;
        long h = DEFAULT_HINT_COOLDOWN_SECONDS;
        if (hintCooldownSeconds != null && hintCooldownSeconds >= 0L
                && hintCooldownSeconds <= 86_400L) {
            h = hintCooldownSeconds;
        }
        boolean d = debug != null ? debug : DEFAULT_DEBUG;
        return new PartyDropsSettings(e, r, s, c, p, f, h, d);
    }
}
