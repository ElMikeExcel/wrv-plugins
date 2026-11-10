/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Damage-feedback settings (ExcelBossTrial 0.3.6, the feel patch):
 * read adapter-side from the top-level {@code feedback:} config
 * block plus per-chapter {@code <chapter>.feedback} overrides (the
 * fight-tracking / milestone / sprinkle precedent - no core verdict
 * rule consumes them, so BossTrialConfig's canonical record stays
 * untouched).
 *
 * The feedback path is DISPLAY ONLY (FEEL-AUDIT-20261102 section
 * 6e): the boss bar title carries the true pool numbers and each
 * champion's action bar carries their own damage total, so gear
 * progression is visible on every hit. No value here can change a
 * fight, a verdict, a pool or a reward; turning everything off
 * leaves BossTrial behaving exactly as 0.3.5.
 *
 * Settled defaults (Mike, Nov 2, 2026, approving the audit's
 * option (e)): pool numbers ON, per-player totals on the action
 * bar, viewer range 64, action-bar throttle 500 ms, sweep title
 * cadence 1000 ms.
 */
public record FeedbackSettings(boolean poolNumbers, String damageTotalMode,
                               String titleFormat, String damageTotalFormat,
                               int viewerRange, long damageTotalMinIntervalMs,
                               long titleTickMs, String exhaustedFormat,
                               double displayScale) {

    /** damage-total mode: no per-player totals anywhere. */
    public static final String MODE_OFF = "off";
    /** damage-total mode: per-player totals on the action bar. */
    public static final String MODE_ACTIONBAR = "actionbar";

    public static final String DEFAULT_TITLE_FORMAT = "{name} — {remaining} / {total}";
    /** 0.4.0 (PM-4): the action bar's numbers are POOL-CREDIT
     *  space (the fight's scoring space), and the default string
     *  now NAMES that space - no surface presents a pool-space
     *  number beside an entity-space one without naming both. */
    public static final String DEFAULT_DAMAGE_TOTAL_FORMAT = "{last} hit · {total} pool damage total";
    /** 0.4.0 (PM-5): the bar's named state for the beat between
     *  the pool-exhaustion verdict firing and the death event
     *  landing - a bare "0 / {total}" on a living boss is the
     *  Nov 7 lie shape and never renders. {name} only. */
    public static final String DEFAULT_EXHAUSTED_FORMAT = "{name} — FALLING…";
    public static final int DEFAULT_VIEWER_RANGE = 64;
    public static final long DEFAULT_MIN_INTERVAL_MS = 500L;
    public static final long DEFAULT_TITLE_TICK_MS = 1000L;
    /** Viewer-range ceiling: a bar audience never needs a bigger
     *  scan than this; the cap keeps a junk config value from
     *  turning every sweep into a world scan. */
    public static final int MAX_VIEWER_RANGE = 512;

    public static FeedbackSettings defaults() {
        return new FeedbackSettings(true, MODE_ACTIONBAR,
                DEFAULT_TITLE_FORMAT, DEFAULT_DAMAGE_TOTAL_FORMAT,
                DEFAULT_VIEWER_RANGE, DEFAULT_MIN_INTERVAL_MS,
                DEFAULT_TITLE_TICK_MS, DEFAULT_EXHAUSTED_FORMAT,
                DisplayScale.DEFAULT_SCALE);
    }

    /**
     * Sanitized construction: an unknown damage-total mode reads
     * as the settled default (actionbar) - a typo can silence the
     * totals only by spelling "off" correctly; a non-positive
     * viewer range reads as the default; a negative interval reads
     * as the default (0 is a real choice: no throttle); blank
     * formats read as the settled defaults; NaN-shaped numbers
     * never reach this record (the adapter's typed reads degrade
     * to the defaults first).
     */
    public static FeedbackSettings sanitized(boolean poolNumbers, String damageTotalMode,
                                             String titleFormat, String damageTotalFormat,
                                             int viewerRange, long damageTotalMinIntervalMs,
                                             long titleTickMs) {
        return sanitized(poolNumbers, damageTotalMode, titleFormat, damageTotalFormat,
                viewerRange, damageTotalMinIntervalMs, titleTickMs, null);
    }

    /** The 0.4.0 form, with the verdict-pending (exhausted) title.
     *  Display scale reads as the settled default (1000). */
    public static FeedbackSettings sanitized(boolean poolNumbers, String damageTotalMode,
                                             String titleFormat, String damageTotalFormat,
                                             int viewerRange, long damageTotalMinIntervalMs,
                                             long titleTickMs, String exhaustedFormat) {
        return sanitized(poolNumbers, damageTotalMode, titleFormat, damageTotalFormat,
                viewerRange, damageTotalMinIntervalMs, titleTickMs, exhaustedFormat,
                DisplayScale.DEFAULT_SCALE);
    }

    /** The Packet B form (Item 35): with the display scale -
     *  feedback.display-scale, the uniform multiplier every
     *  player-facing pool-space number renders at (DisplayScale;
     *  settled default 1000, sanitized there - junk reads as the
     *  default, never as a flattened ladder). */
    public static FeedbackSettings sanitized(boolean poolNumbers, String damageTotalMode,
                                             String titleFormat, String damageTotalFormat,
                                             int viewerRange, long damageTotalMinIntervalMs,
                                             long titleTickMs, String exhaustedFormat,
                                             double displayScale) {
        String mode = damageTotalMode == null
                ? MODE_ACTIONBAR : damageTotalMode.strip().toLowerCase(java.util.Locale.ROOT);
        if (!mode.equals(MODE_OFF) && !mode.equals(MODE_ACTIONBAR)) {
            mode = MODE_ACTIONBAR;
        }
        int range = viewerRange <= 0 ? DEFAULT_VIEWER_RANGE
                : Math.min(viewerRange, MAX_VIEWER_RANGE);
        long interval = damageTotalMinIntervalMs < 0
                ? DEFAULT_MIN_INTERVAL_MS : damageTotalMinIntervalMs;
        long tick = titleTickMs <= 0 ? DEFAULT_TITLE_TICK_MS : titleTickMs;
        return new FeedbackSettings(poolNumbers, mode,
                blankTo(titleFormat, DEFAULT_TITLE_FORMAT),
                blankTo(damageTotalFormat, DEFAULT_DAMAGE_TOTAL_FORMAT),
                range, interval, tick,
                blankTo(exhaustedFormat, DEFAULT_EXHAUSTED_FORMAT),
                DisplayScale.sanitized(displayScale));
    }

    private static String blankTo(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    /**
     * The pool-numbers switch from a RAW config value (the
     * adapter reads it untyped so junk is testable here): a
     * Boolean reads as itself, the strings "true"/"false" read
     * as themselves, and anything else - a missing key included -
     * reads as the settled default ON (a broken value never
     * silently retires the numbers).
     */
    /**
     * The damage-total mode from a raw config value, read
     * UNTYPED for the same reason as poolNumbersValue - and one
     * sharper: in YAML an unquoted {@code off} (the documented
     * way to write this very setting) parses as the boolean
     * false, not a string, and a typed getString would hand
     * back null and silently resurrect the action bar on a
     * config that says off (rig finding F4). A boolean false
     * reads as "off", a boolean true as the settled action bar;
     * a string passes through for sanitize() to judge; anything
     * else (a number, a list) reads as ABSENT (null) so a junk
     * chapter override can never override at all.
     */
    public static String damageTotalModeValue(Object raw) {
        if (raw instanceof Boolean b) {
            return b ? MODE_ACTIONBAR : MODE_OFF;
        }
        if (raw instanceof String s) {
            return s;
        }
        return null;
    }

    public static boolean poolNumbersValue(Object raw) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw instanceof String s) {
            if (s.strip().equalsIgnoreCase("true")) {
                return true;
            }
            if (s.strip().equalsIgnoreCase("false")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Apply a chapter's overrides over these (global) settings:
     * each override is independent - a null override inherits,
     * a present one replaces (sanitized on the way in, so a
     * chapter's junk mode reads as the settled default, never
     * as a silent off).
     */
    public FeedbackSettings withOverrides(Boolean poolNumbersOverride,
                                          String damageTotalModeOverride) {
        boolean pool = poolNumbersOverride != null ? poolNumbersOverride : poolNumbers;
        String mode = damageTotalModeOverride == null ? damageTotalMode
                : sanitized(true, damageTotalModeOverride, "x", "x", 1, 0, 1).damageTotalMode();
        return new FeedbackSettings(pool, mode, titleFormat, damageTotalFormat,
                viewerRange, damageTotalMinIntervalMs, titleTickMs, exhaustedFormat,
                displayScale);
    }

    /** True when this chapter shows the numbered boss bar. */
    public boolean poolNumbersOn() {
        return poolNumbers;
    }

    /** True when per-player totals ride the action bar. */
    public boolean actionBarOn() {
        return MODE_ACTIONBAR.equals(damageTotalMode);
    }

    /** True when the display has nothing to do for a chapter. */
    public boolean dark() {
        return !poolNumbers && !actionBarOn();
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
