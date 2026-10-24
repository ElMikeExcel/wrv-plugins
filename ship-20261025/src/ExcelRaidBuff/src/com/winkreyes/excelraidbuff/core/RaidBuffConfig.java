package com.winkreyes.excelraidbuff.core;

import java.util.Map;

/**
 * Every knob of ExcelRaidBuff, with the settled defaults (Mike,
 * Oct 20, 2026 late — his raid-buff decision and his elites-in-
 * raids addition). Pure data: the Bukkit adapter fills it from
 * config.yml via {@link #fromMap}; tests fill it directly.
 * Validation clamps instead of throwing — a typo'd config
 * degrades toward vanilla, never crashes the plugin, and can
 * never WEAKEN a raider below vanilla (multipliers clamp at 1.0
 * from below).
 */
public final class RaidBuffConfig {

    public final boolean enabled;

    // The one player-facing line.
    public final boolean announceEnabled;
    public final int announceMinOmen;
    public final String announceLine;

    // Attribute scaling, indexed by Bad Omen level 1..5 (slot 0 unused).
    private final double[] healthByOmen;
    private final double[] damageByOmen;
    public final double lateWaveHealthRamp;
    public final int lateWaveCount;

    // EliteMobs elites among the raiders (Mike's addition).
    public final boolean elitesEnabled;
    private final double[] eliteShareByOmen;
    public final double eliteWaveRampFrom;
    public final double eliteWaveRampTo;
    private final int[] eliteLevelBonusByOmen;
    /** Used only if EliteMobs' own cap read fails; the live cap wins when readable. */
    public final int eliteLevelCapFallback;

    public final int sweepPeriodTicks;

    // Stuck-wave reveal (0.1.1, Mike's pick Oct 23, 2026): a raid
    // that makes no progress for quietSeconds glows its living
    // members until the fight resumes. Cosmetic only.
    public final boolean revealEnabled;
    public final int revealQuietSeconds;
    public final boolean revealAnnounce;
    public final String revealLine;

    // Raid expiry (0.1.2, Mike's order Oct 23, 2026): a raid's
    // lifetime cap. A hard wall-clock limit — fighting does NOT
    // reset it (that is the reveal's quiet clock, a different
    // thing) — on a persisted clock, so restarts don't reset it
    // either. At the cap the raid dies down: members leave with
    // no loot, and the raid is neither won nor lost.
    public final boolean expiryEnabled;
    public final double expiryHours;
    public final String expiryLine;

    public static final double[] DEFAULT_HEALTH = {1.0, 1.00, 1.25, 1.50, 1.80, 2.20};
    public static final double[] DEFAULT_DAMAGE = {1.0, 1.00, 1.08, 1.15, 1.22, 1.30};
    public static final double[] DEFAULT_ELITE_SHARE = {0.0, 0.00, 0.05, 0.10, 0.15, 0.18};
    public static final int[] DEFAULT_ELITE_LEVEL_BONUS = {0, 0, 0, 5, 10, 15};
    public static final String DEFAULT_ANNOUNCE_LINE =
            "&cThese raiders are hardened by the dark omen. Stand together.";
    public static final String DEFAULT_REVEAL_LINE =
            "&eThe remaining raiders are revealed — hunt them down.";
    /** The reveal quiet span can never be shorter than this, nor longer. */
    public static final int REVEAL_QUIET_MIN = 30;
    public static final int REVEAL_QUIET_MAX = 900;
    public static final String DEFAULT_EXPIRY_LINE =
            "&eThe raid has died down — the raiders have given up and gone.";
    public static final double DEFAULT_EXPIRY_HOURS = 6.0;
    /** The lifetime cap's floor (36 s — rig-sized values stay legal) and ceiling (a week). */
    public static final double EXPIRY_HOURS_MIN = 0.01;
    public static final double EXPIRY_HOURS_MAX = 168.0;

    private RaidBuffConfig(boolean enabled, boolean announceEnabled, int announceMinOmen,
                           String announceLine, double[] healthByOmen, double[] damageByOmen,
                           double lateWaveHealthRamp, int lateWaveCount,
                           boolean elitesEnabled, double[] eliteShareByOmen,
                           double eliteWaveRampFrom, double eliteWaveRampTo,
                           int[] eliteLevelBonusByOmen, int eliteLevelCapFallback,
                           int sweepPeriodTicks,
                           boolean revealEnabled, int revealQuietSeconds,
                           boolean revealAnnounce, String revealLine,
                           boolean expiryEnabled, double expiryHours,
                           String expiryLine) {
        this.enabled = enabled;
        this.announceEnabled = announceEnabled;
        this.announceMinOmen = announceMinOmen;
        this.announceLine = announceLine;
        this.healthByOmen = healthByOmen;
        this.damageByOmen = damageByOmen;
        this.lateWaveHealthRamp = lateWaveHealthRamp;
        this.lateWaveCount = lateWaveCount;
        this.elitesEnabled = elitesEnabled;
        this.eliteShareByOmen = eliteShareByOmen;
        this.eliteWaveRampFrom = eliteWaveRampFrom;
        this.eliteWaveRampTo = eliteWaveRampTo;
        this.eliteLevelBonusByOmen = eliteLevelBonusByOmen;
        this.eliteLevelCapFallback = eliteLevelCapFallback;
        this.sweepPeriodTicks = sweepPeriodTicks;
        this.revealEnabled = revealEnabled;
        this.revealQuietSeconds = revealQuietSeconds;
        this.revealAnnounce = revealAnnounce;
        this.revealLine = revealLine;
        this.expiryEnabled = expiryEnabled;
        this.expiryHours = expiryHours;
        this.expiryLine = expiryLine;
    }

    /** The lifetime cap in milliseconds (the tracker's unit). */
    public long expiryCapMs() {
        return Math.round(expiryHours * 3_600_000.0);
    }

    public double healthMultiplier(int omenLevel) {
        if (omenLevel < 1 || omenLevel > 5) return 1.0;
        return healthByOmen[omenLevel];
    }

    public double damageMultiplier(int omenLevel) {
        if (omenLevel < 1 || omenLevel > 5) return 1.0;
        return damageByOmen[omenLevel];
    }

    public double eliteShare(int omenLevel) {
        if (omenLevel < 1 || omenLevel > 5) return 0.0;
        return eliteShareByOmen[omenLevel];
    }

    public int eliteLevelBonus(int omenLevel) {
        if (omenLevel < 1 || omenLevel > 5) return 0;
        return eliteLevelBonusByOmen[omenLevel];
    }

    /** The settled defaults, no config file involved. */
    public static RaidBuffConfig defaults() {
        return new RaidBuffConfig(true, true, 3, DEFAULT_ANNOUNCE_LINE,
                DEFAULT_HEALTH.clone(), DEFAULT_DAMAGE.clone(), 1.15, 2,
                true, DEFAULT_ELITE_SHARE.clone(), 0.6, 1.4,
                DEFAULT_ELITE_LEVEL_BONUS.clone(), 150, 40,
                true, 180, true, DEFAULT_REVEAL_LINE,
                true, DEFAULT_EXPIRY_HOURS, DEFAULT_EXPIRY_LINE);
    }

    @SuppressWarnings("unchecked")
    public static RaidBuffConfig fromMap(Map<String, Object> root) {
        if (root == null) return defaults();
        Map<String, Object> announce = child(root, "announce");
        Map<String, Object> scaling = child(root, "scaling");
        Map<String, Object> elites = child(root, "elites");
        Map<String, Object> reveal = child(root, "reveal");
        Map<String, Object> expiry = child(root, "expiry");

        double[] health = DEFAULT_HEALTH.clone();
        double[] damage = DEFAULT_DAMAGE.clone();
        fillMultipliers(health, child(scaling, "health-multiplier"), 1.0, 5.0);
        fillMultipliers(damage, child(scaling, "damage-multiplier"), 1.0, 5.0);

        double[] share = DEFAULT_ELITE_SHARE.clone();
        fillShares(share, child(elites, "share"));
        int[] bonus = DEFAULT_ELITE_LEVEL_BONUS.clone();
        fillBonuses(bonus, child(elites, "level-bonus"));

        return new RaidBuffConfig(
                bool(root.get("enabled"), true),
                bool(announce.get("enabled"), true),
                clampInt(num(announce.get("min-omen-level"), 3), 1, 5),
                str(announce.get("line"), DEFAULT_ANNOUNCE_LINE),
                health, damage,
                clamp(num(scaling.get("late-wave-health-ramp"), 1.15), 1.0, 2.0),
                clampInt(num(scaling.get("late-wave-count"), 2), 0, 8),
                bool(elites.get("enabled"), true),
                share,
                clamp(num(elites.get("wave-ramp-from"), 0.6), 0.0, 3.0),
                clamp(num(elites.get("wave-ramp-to"), 1.4), 0.0, 3.0),
                bonus,
                clampInt(num(elites.get("level-cap-fallback"), 150), 1, 1000),
                clampInt(num(root.get("sweep-period-ticks"), 40), 10, 200),
                bool(reveal.get("enabled"), true),
                clampInt(num(reveal.get("quiet-seconds"), 180), REVEAL_QUIET_MIN, REVEAL_QUIET_MAX),
                bool(reveal.get("announce"), true),
                str(reveal.get("line"), DEFAULT_REVEAL_LINE),
                bool(expiry.get("enabled"), true),
                clamp(num(expiry.get("hours"), DEFAULT_EXPIRY_HOURS), EXPIRY_HOURS_MIN, EXPIRY_HOURS_MAX),
                str(expiry.get("line"), DEFAULT_EXPIRY_LINE));
    }

    // ---- small parse helpers (clamp, never throw) ----

    private static Map<String, Object> child(Map<String, Object> parent, String key) {
        if (parent == null) return Map.of();
        Object v = parent.get(key);
        if (v instanceof Map<?, ?> m) {
            @SuppressWarnings("unchecked") Map<String, Object> cast = (Map<String, Object>) m;
            return cast;
        }
        return Map.of();
    }

    private static boolean bool(Object v, boolean fallback) {
        return v instanceof Boolean b ? b : fallback;
    }

    private static String str(Object v, String fallback) {
        return v instanceof String s && !s.isBlank() ? s : fallback;
    }

    private static double num(Object v, double fallback) {
        return v instanceof Number n ? n.doubleValue() : fallback;
    }

    private static double clamp(double v, double lo, double hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return Math.max(lo, Math.min(hi, v));
    }

    private static int clampInt(double v, int lo, int hi) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return lo;
        return Math.max(lo, Math.min(hi, (int) Math.round(v)));
    }

    private static void fillMultipliers(double[] target, Map<String, Object> byOmen, double lo, double hi) {
        for (int omen = 1; omen <= 5; omen++) {
            Object v = byOmen.get(String.valueOf(omen));
            if (v instanceof Number n) target[omen] = clamp(n.doubleValue(), lo, hi);
        }
    }

    private static void fillShares(double[] target, Map<String, Object> byOmen) {
        for (int omen = 1; omen <= 5; omen++) {
            Object v = byOmen.get(String.valueOf(omen));
            if (v instanceof Number n) target[omen] = clamp(n.doubleValue(), 0.0, 1.0);
        }
    }

    private static void fillBonuses(int[] target, Map<String, Object> byOmen) {
        for (int omen = 1; omen <= 5; omen++) {
            Object v = byOmen.get(String.valueOf(omen));
            if (v instanceof Number n) target[omen] = clampInt(n.doubleValue(), 0, 200);
        }
    }
}
