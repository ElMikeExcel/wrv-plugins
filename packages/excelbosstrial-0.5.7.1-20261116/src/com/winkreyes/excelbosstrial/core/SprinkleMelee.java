/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The sprinkle melee-feel rules (ExcelBossTrial 0.5.4, the
 * Elder sprinkle melee fix, Mike Nov 9, 2026): two per-chapter
 * knobs on the sprinkle settings, judged here as pure logic -
 *
 *   - KNOCKBACK RESISTANCE: a sprinkle add spawns through the
 *     EliteMobs conversion, which stamps its own knockback
 *     resistance on the mob; in the monument the adds read as
 *     rooted - a melee hit barely moves them, so the melee
 *     players who are the elder fight's point cannot shove
 *     the wave around. A chapter that sets this key has the
 *     value forced onto every sprinkle spawn AFTER conversion
 *     (the conversion's own stamp never survives it). The
 *     configured range runs negative on purpose: a negative
 *     setting is the chapter author's way of saying "as
 *     knockable as the game allows" - the APPLIED value
 *     floors at 0.0, the vanilla attribute minimum, which is
 *     full vanilla knockback (zero resistance). An UNSET key
 *     (NaN) leaves the converted mob's attribute exactly as
 *     the conversion left it: chapters that never opt in
 *     fight exactly as they did in 0.5.3.
 *
 *   - STAGGER: a wave used to land all at once - the whole
 *     ring in one tick, an instant wall. A chapter with
 *     stagger-ticks &gt; 0 spreads its ring spawns evenly
 *     across that many ticks instead: the i-th add of a
 *     count-add wave lands at round(stagger * i / (count-1)),
 *     so the first add is immediate and the last lands
 *     exactly at the window's end. 0 (or a one-add wave) is
 *     the 0.5.3 instant ring, unchanged.
 *
 * Neither knob touches a damage number anywhere: the first
 * moves a spawn attribute, the second moves spawn TIMES.
 */
public final class SprinkleMelee {

    private SprinkleMelee() {
    }

    /** The unset sentinel for the knockback key: NaN in the
     *  settings record means "this chapter never opted in -
     *  leave the converted mob's attribute untouched". */
    public static final double KNOCKBACK_UNSET = Double.NaN;

    /** The configured knockback range (sanitize clamp): a
     *  chapter may ask for up to full immunity (1.0) or for
     *  the negative floor (-1.0, "as knockable as possible"). */
    public static final double KNOCKBACK_MIN = -1.0;
    public static final double KNOCKBACK_MAX = 1.0;

    /** The stagger ceiling (sanitize clamp), in ticks: ten
     *  seconds is far past any readable ring entrance; a
     *  larger ask is a config error, clamped loud like every
     *  other sprinkle number. */
    public static final int MAX_STAGGER_TICKS = 200;

    /** True when a chapter has opted into the knockback fix. */
    public static boolean knockbackSet(double configured) {
        return !Double.isNaN(configured);
    }

    /** Sanitize the configured value: NaN stays NaN (unset is
     *  a state, not an error); an infinite value fails to
     *  UNSET (the chapter simply has no fix - the pre-0.5.4
     *  behavior, never a surprise number); a finite value
     *  clamps into [KNOCKBACK_MIN, KNOCKBACK_MAX]. */
    public static double sanitizedKnockback(double configured) {
        if (Double.isNaN(configured)) {
            return KNOCKBACK_UNSET;
        }
        if (Double.isInfinite(configured)) {
            return KNOCKBACK_UNSET;
        }
        return Math.max(KNOCKBACK_MIN, Math.min(KNOCKBACK_MAX, configured));
    }

    /** The value actually written to the spawn's attribute:
     *  the configured value floored at the vanilla attribute
     *  minimum 0.0 (negative asks land exactly there - full
     *  vanilla knockback) and capped at 1.0. Only call when
     *  {@link #knockbackSet} is true. */
    public static double appliedKnockback(double configured) {
        return Math.max(0.0, Math.min(1.0, configured));
    }

    /**
     * The spawn delay, in ticks, of the index-th add of a
     * staggered wave: the adds spread evenly across the
     * stagger window, first add immediate, last add exactly
     * at the window's end. A zero/negative window, a one-add
     * wave, or a bad index all read 0 (the instant ring).
     */
    public static int delayTicks(int index, int count, int staggerTicks) {
        if (staggerTicks <= 0 || count <= 1 || index <= 0) {
            return 0;
        }
        if (index >= count - 1) {
            return staggerTicks;
        }
        return (int) Math.round((double) staggerTicks * index / (double) (count - 1));
    }
}
