package com.winkreyes.exceltrainingwheels.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Seal compensation for the damage shield (0.1.1 — Mike's ABSOLUTE
 * LINE, Oct 19, 2026): a protected player must take HALF OF A
 * MOB'S NORMAL (UNSCALED) DAMAGE from a hostile mob at every seal
 * state, in every world. "Normal" is the mob's damage BEFORE the
 * natural-elite ladder's damage-dealt multiplier (EliteMobs
 * MobCombatSettings scaledDamageToPlayerMultiplier) is applied.
 *
 * EliteMobs multiplies an elite's outgoing damage by the current
 * ladder step, so a flat base factor would let a protected player
 * take base x dealt from an elite (0.5 x 1.50 = 0.75 of normal
 * today, 0.5 x 2.25 = 1.125 at Seal 5 — MORE than a normal hit).
 * The shield therefore divides the player's base factor by the
 * CURRENT dealt multiplier for ELITE damagers only:
 *
 *     eliteFactor(seal) = baseFactor / dealtMultiplier(seal)
 *
 * Non-elite hostile mobs are never ladder-scaled, so their factor
 * stays the flat base factor at every seal. Pure logic; the Bukkit
 * glue owns elite detection (EliteMobs gateway seam) and the seal
 * count (reflective GuildGate read, cached), and passes both in.
 *
 * Scope bound: EliteMobs GUILD-WORLD content (post-Seal-5) is not
 * ladder-scaled, so no compensation applies to it by this table —
 * the flat base factor governs there until guild prep verifies its
 * own numbers (see BUILD.md).
 */
public final class CompensationTable {

    public record ParseResult(CompensationTable table, List<String> errors) {}

    /**
     * The shipped ladder (0.1.6: the Elite Seal Ladder Mike approved
     * Oct 25, 2026): EliteMobs scaledDamageToPlayerMultiplier by
     * seals broken. Index = seals broken. Seal 4 is the strength
     * peak; Seal 5 is a spawn-only step, so its damage repeats 2.20.
     * (0.1.1 to 0.1.5 shipped the v4 ladder 1.50 .. 2.25.)
     *
     * Since 0.1.6 this table is the FALLBACK: when the EliteMobs
     * seam is bound the glue divides by the LIVE multiplier
     * EliteMobs is using (see core/CombatDivisor) and warns when
     * this table disagrees with it.
     */
    public static final double[] DEFAULT_DEALT = {1.60, 1.75, 1.90, 2.05, 2.20, 2.20};

    /** A dealt multiplier outside [1.0, 5.0] is a config typo, not a ladder. */
    public static final double MIN_MULTIPLIER = 1.0;
    public static final double MAX_MULTIPLIER = 5.0;

    private final double[] dealt;

    private CompensationTable(double[] dealt) {
        this.dealt = dealt;
    }

    public static boolean validMultiplier(double m) {
        return !Double.isNaN(m) && !Double.isInfinite(m) && m >= MIN_MULTIPLIER && m <= MAX_MULTIPLIER;
    }

    /** Build directly from values (each validated); size must be >= 1. */
    public static CompensationTable of(double... values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("compensation table needs at least one row");
        }
        for (double v : values) {
            if (!validMultiplier(v)) {
                throw new IllegalArgumentException("dealt multiplier out of [" + MIN_MULTIPLIER
                        + "," + MAX_MULTIPLIER + "]: " + v);
            }
        }
        return new CompensationTable(values.clone());
    }

    /**
     * Build from the raw config representation: seal-index-string
     * -> multiplier-string, laid over {@code defaults}. A missing,
     * unparsable, or out-of-range row falls back to the default for
     * that seal and is reported in errors (the glue logs them); one
     * bad row never sinks the table, and a bad row can never produce
     * a WEAKER shield than the shipped ladder gives. Keys beyond
     * the table size are reported and ignored.
     */
    public static ParseResult fromRaw(Map<String, String> raw, double[] defaults) {
        if (defaults == null || defaults.length == 0) {
            throw new IllegalArgumentException("defaults must provide at least one row");
        }
        double[] values = defaults.clone();
        List<String> errors = new ArrayList<>();
        if (raw != null) {
            List<String> keys = new ArrayList<>(raw.keySet());
            keys.sort(null);
            for (String key : keys) {
                int index;
                try {
                    index = Integer.parseInt(key.trim());
                } catch (Exception e) {
                    errors.add("compensation table: key '" + key + "' is not a seal count - ignored");
                    continue;
                }
                if (index < 0 || index >= values.length) {
                    errors.add("compensation table: seal " + index + " is outside the table (0.."
                            + (values.length - 1) + ") - ignored");
                    continue;
                }
                String text = raw.get(key);
                double parsed;
                try {
                    parsed = Double.parseDouble(text == null ? "" : text.trim());
                } catch (Exception e) {
                    errors.add("compensation table: dealt multiplier for seal " + index
                            + " is not a number ('" + text + "') - using default " + values[index]);
                    continue;
                }
                if (!validMultiplier(parsed)) {
                    errors.add("compensation table: dealt multiplier for seal " + index
                            + " is out of [" + MIN_MULTIPLIER + "," + MAX_MULTIPLIER + "] (" + text
                            + ") - using default " + values[index]);
                    continue;
                }
                values[index] = parsed;
            }
        }
        return new ParseResult(new CompensationTable(values), errors);
    }

    public int size() {
        return dealt.length;
    }

    /** The ladder's damage-dealt multiplier at a seal count (clamped to the table). */
    public double dealtMultiplier(int sealsBroken) {
        int i = Math.max(0, Math.min(sealsBroken, dealt.length - 1));
        return dealt[i];
    }

    /**
     * The shield factor for an ELITE damager: the player's base
     * factor divided by the current ladder multiplier, so the
     * damage that lands is base x the mob's UNSCALED damage.
     */
    public double eliteFactor(double baseFactor, int sealsBroken) {
        if (!ProtectedRegistry.validFactor(baseFactor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + baseFactor);
        }
        double eff = baseFactor / dealtMultiplier(sealsBroken);
        return Math.max(0.0, Math.min(1.0, eff));
    }

    /** The factor to apply: compensated for elites, the flat base factor otherwise. */
    public double effectiveFactor(double baseFactor, boolean elite, int sealsBroken) {
        if (!ProtectedRegistry.validFactor(baseFactor)) {
            throw new IllegalArgumentException("factor out of [0,1]: " + baseFactor);
        }
        return elite ? eliteFactor(baseFactor, sealsBroken) : baseFactor;
    }
}

// ExcelTrainingWheels — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
