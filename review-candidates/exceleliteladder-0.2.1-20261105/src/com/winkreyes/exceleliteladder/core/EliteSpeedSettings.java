package com.winkreyes.exceleliteladder.core;

import java.util.List;

/**
 * ExcelEliteLadder 0.2.1 — the elite movement-speed buff settings (config.yml
 * section {@code elite-speed:}), parsed defensively from raw config values.
 *
 * The buff: every NATURAL elite (never a custom boss) gets a Speed potion
 * effect, applied by the Bukkit glue from these settings. The mechanism is
 * EliteMobs' own {@code movement_speed} power channel (a potion EFFECT, not
 * an attribute modifier) and our Halloween fork's spawn-buff pattern
 * (permanent, hidden). Design record: SPEED-NOTE.md in the retune packet.
 *
 * Pure Java (raw Objects in, settings out) so the plain-JVM suite can drive
 * the parse/clamp rules with exactly the shapes Bukkit's config hands over.
 * Unlike ladder.yml — operator-edited under strict refusal — config.yml is
 * the plugin's own house file, so the posture here is the house one for
 * config: a bad value falls back to the default AND is named in a warning,
 * never silently coerced.
 */
public final class EliteSpeedSettings {

    /** The effect channel parameters the glue must use verbatim. */
    public static final boolean EFFECT_AMBIENT = false;
    public static final boolean EFFECT_PARTICLES = false;
    public static final boolean EFFECT_ICON = false;

    /** Potion amplifier ceiling: vanilla stores it in a byte on the wire; past 127 it wraps. */
    public static final int AMPLIFIER_MAX = 127;

    public static final boolean DEFAULT_ENABLED = true;
    /** 0 = Speed I (+20%) — Mike's "tiny". EM's own boss power uses 1 (Speed II). */
    public static final int DEFAULT_AMPLIFIER = 0;
    /** -1 in config = permanent for the mob's life (Integer.MAX_VALUE ticks). */
    public static final int PERMANENT = Integer.MAX_VALUE;

    private final boolean enabled;
    private final int amplifier;
    private final int durationTicks;
    private final boolean permanent;

    private EliteSpeedSettings(boolean enabled, int amplifier, int durationTicks, boolean permanent) {
        this.enabled = enabled;
        this.amplifier = amplifier;
        this.durationTicks = durationTicks;
        this.permanent = permanent;
    }

    public static EliteSpeedSettings defaults() {
        return new EliteSpeedSettings(DEFAULT_ENABLED, DEFAULT_AMPLIFIER, PERMANENT, true);
    }

    /**
     * Parse the three raw config values. Any of them may be null (key absent —
     * the bundled defaults in the jar's config.yml normally supply them, but a
     * hand-trimmed live config must still parse). Every fallback or clamp is
     * recorded in {@code warnings} for the caller to log.
     */
    public static EliteSpeedSettings parse(Object enabledRaw, Object amplifierRaw, Object durationRaw,
                                           List<String> warnings) {
        boolean enabled = DEFAULT_ENABLED;
        if (enabledRaw instanceof Boolean b) {
            enabled = b;
        } else if (enabledRaw != null) {
            warnings.add("elite-speed.enabled must be true or false, found " + describe(enabledRaw)
                    + " — using the default (" + DEFAULT_ENABLED + ")");
        }

        int amplifier = DEFAULT_AMPLIFIER;
        if (amplifierRaw != null) {
            Integer a = wholeNumber(amplifierRaw);
            if (a == null) {
                warnings.add("elite-speed.amplifier must be a whole number, found " + describe(amplifierRaw)
                        + " — using the default (" + DEFAULT_AMPLIFIER + ")");
            } else if (a < 0 || a > AMPLIFIER_MAX) {
                amplifier = Math.max(0, Math.min(AMPLIFIER_MAX, a));
                warnings.add("elite-speed.amplifier " + a + " is outside 0.." + AMPLIFIER_MAX
                        + " — clamped to " + amplifier);
            } else {
                amplifier = a;
            }
        }

        int duration = PERMANENT;
        boolean permanent = true;
        if (durationRaw != null) {
            Long d = wholeLong(durationRaw);
            if (d == null) {
                warnings.add("elite-speed.duration-ticks must be a whole number, found " + describe(durationRaw)
                        + " — using the default (-1, permanent)");
            } else if (d == -1L || d >= PERMANENT) {
                duration = PERMANENT;
                permanent = true;
            } else if (d >= 1L) {
                duration = d.intValue();
                permanent = false;
            } else {
                warnings.add("elite-speed.duration-ticks must be -1 (permanent) or a positive tick count, found "
                        + d + " — using the default (-1, permanent)");
            }
        }
        return new EliteSpeedSettings(enabled, amplifier, duration, permanent);
    }

    private static Integer wholeNumber(Object v) {
        Long d = wholeLong(v);
        if (d == null || d < Integer.MIN_VALUE || d > Integer.MAX_VALUE) {
            return null;
        }
        return d.intValue();
    }

    private static Long wholeLong(Object v) {
        if (!(v instanceof Number n)) {
            return null;
        }
        double d = n.doubleValue();
        if (Double.isNaN(d) || Double.isInfinite(d) || d != Math.rint(d)) {
            return null;
        }
        return (long) d;
    }

    private static String describe(Object v) {
        if (v instanceof String s) return "'" + s + "' (text)";
        if (v instanceof Boolean b) return b + " (true/false)";
        return v + " (" + v.getClass().getSimpleName() + ")";
    }

    public boolean enabled() {
        return enabled;
    }

    public int amplifier() {
        return amplifier;
    }

    /** Effective duration in ticks; Integer.MAX_VALUE when permanent. */
    public int durationTicks() {
        return durationTicks;
    }

    public boolean permanent() {
        return permanent;
    }

    /**
     * The one gate every application path runs: the feature must be on,
     * EliteMobs must be present, and the entity must be a NATURAL elite
     * (custom bosses are excluded — they can roll EM's own movement_speed
     * power themselves).
     */
    public boolean shouldBuff(boolean eliteMobsPresent, boolean naturalEntity) {
        return enabled && eliteMobsPresent && naturalEntity;
    }

    /** "Speed I" for amplifier 0, "Speed II" for 1, ... Roman through X, then plain numbers. */
    public String amplifierLabel() {
        int level = amplifier + 1;
        String roman = switch (level) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> Integer.toString(level);
        };
        return "Speed " + roman;
    }

    /** Status-command form: "on (Speed I, permanent)" / "on (Speed II, 100000 ticks)" / "off". */
    public String describe() {
        if (!enabled) {
            return "off";
        }
        return "on (" + amplifierLabel() + ", " + (permanent ? "permanent" : durationTicks + " ticks") + ")";
    }

    /** Enable-line form: "ON (amplifier 0, duration permanent)" / "OFF". */
    public String bootText() {
        if (!enabled) {
            return "OFF";
        }
        return "ON (amplifier " + amplifier + ", duration " + (permanent ? "permanent" : durationTicks + " ticks") + ")";
    }
}
