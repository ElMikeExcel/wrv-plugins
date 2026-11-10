/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * One chapter's elite-sprinkle settings (ExcelBossTrial 0.3.3,
 * ELITE-SPRINKLE-STUDY-20261101 sections 3 and 7; reworked
 * 0.4.0, PM-6 - the Wither-night sprinkle ruling): the
 * per-chapter switches the adjustability law requires, read
 * adapter-side from the chapter's {@code sprinkle:} config
 * block (the milestone-settings precedent - no core verdict
 * rule consumes them, so BossTrialConfig's canonical record
 * stays untouched).
 *
 * 0.4.0 shape: the sprinkle is CADENCE-based, never rolled.
 * Mike's ruling after the Nov 7 Wither fight (three 35% rolls,
 * three misses, zero adds in the fight that needed them):
 * every milestone stamp (75/50/25%) owes a wave, and a timed
 * wave falls every {@code cadenceSeconds} (default 150) on top
 * of that, cycling the chapter's {@code waves} rotation. A wave
 * that cannot spawn (the alive cap is full) is DEFERRED, never
 * spent - it fires the moment room opens. The per-fight cap is
 * the only retirement. The 0.3.3 {@code chance} and
 * {@code count} fields survive as parsed-but-inert legacy: old
 * configs keep loading, and nothing reads them anymore.
 *
 * A wave is a mob pool drawn in order and cycled (the types
 * idiom), a count, and a placement: {@code ring} (the spawn
 * ring around the anchor) or {@code aerial} (above the arena -
 * the ghast's entry). Plain mob types only - no disguise key
 * exists anywhere in this design (the stage-2 standing rule:
 * LibsDisguises bosses are invisible to Bedrock players).
 */
public record SprinkleSettings(boolean enabled, double chance, int count,
                               List<Integer> levels, int maxAlive,
                               int maxPerFight, double leashRadius,
                               List<String> types,
                               int cadenceSeconds, List<Wave> waves) {

    /** One wave of the rotation: a cycled type pool, a count,
     *  and a placement ("ring" or "aerial"). */
    public record Wave(List<String> types, int count, String placement) {

        public boolean aerial() {
            return "aerial".equals(placement);
        }

        /** The mob type drawn for the i-th elite of this wave (cycled). */
        public String typeFor(int waveIndex) {
            return types.get(Math.floorMod(waveIndex, types.size()));
        }
    }

    /** The settled stamp multipliers (75% / 50% / 25% remaining).
     *  Legacy since 0.4.0: kept so the master's sprinkle block
     *  parses exactly as before; the cadence judge never reads
     *  them. */
    public static final List<Double> DEFAULT_STAMP_MULTIPLIERS =
            List.of(0.75, 1.0, 1.25);

    /** Legacy since 0.4.0 (the chance ceiling of the retired
     *  roller); parsed for config compatibility only. */
    public static final double DEFAULT_CHANCE_CEILING = 0.90;

    /** The cadence default (seconds between timed waves). */
    public static final int DEFAULT_CADENCE_SECONDS = 150;

    /** The settled announcement (Mike's pick, Nov 1, 2026, 4:05 PM
     *  CST: a wave is ANNOUNCED, never silent). {chapter} is the
     *  boss's display name, {count} the elites spawned. */
    public static final String DEFAULT_ANNOUNCE_TEXT =
            "&4The seal fights back&7 - {count} elite reinforcements join "
                    + "the battle against {chapter}!";

    /**
     * Sanitized construction. Counts and caps clamp to sane
     * ranges; the levels list always ends up with exactly three
     * positive entries (a short list repeats its last entry, an
     * empty one falls back to the chapter default); an empty
     * types or waves list falls back to the chapter default;
     * the cadence clamps to [15, 3600] seconds.
     */
    public static SprinkleSettings sanitized(boolean enabled, double chance, int count,
                                             List<Integer> levels, int maxAlive,
                                             int maxPerFight, double leashRadius,
                                             List<String> types, int cadenceSeconds,
                                             List<Wave> waves, SprinkleSettings fallback) {
        return sanitized(enabled, chance, count, levels, maxAlive, maxPerFight,
                leashRadius, types, cadenceSeconds, waves, fallback, null);
    }

    /**
     * Sanitized construction, loud (0.4.0 rebuild, Mike's
     * ruling Nov 8, 2026: the defect was never the ceiling -
     * it was the SILENCE). Identical outputs to the quiet
     * overload; whenever clamping or a fail-dark replacement
     * actually CHANGES a configured value, one line per change
     * is appended to {@code adjustments} (key, configured
     * value, applied value) for the adapter to log at boot.
     * Safety ceilings sit far above design values, and a
     * clamp that fires is always loud: silence means the
     * config loaded untouched. A null list keeps it quiet.
     */
    public static SprinkleSettings sanitized(boolean enabled, double chance, int count,
                                             List<Integer> levels, int maxAlive,
                                             int maxPerFight, double leashRadius,
                                             List<String> types, int cadenceSeconds,
                                             List<Wave> waves, SprinkleSettings fallback,
                                             List<String> adjustments) {
        double c;
        if (Double.isNaN(chance) || Double.isInfinite(chance)) {
            c = 0.0;
            if (adjustments != null) {
                adjustments.add("chance is not a number — set to 0.0 (fail-dark)");
            }
        } else {
            c = clamped("chance", chance, 0.0, 1.0, adjustments);
        }
        int n = clamped("count", count, 0, 8, adjustments);
        int alive = clamped("max-alive", maxAlive, 0, 16, adjustments);
        int perFight = clamped("max-per-fight", maxPerFight, 0, 140, adjustments);
        double leash;
        if (Double.isNaN(leashRadius) || Double.isInfinite(leashRadius)) {
            leash = fallback.leashRadius();
            if (adjustments != null) {
                adjustments.add("leash-radius is not a number — set to "
                        + fallback.leashRadius() + " (the chapter default)");
            }
        } else {
            leash = clamped("leash-radius", leashRadius, 4.0, 128.0, adjustments);
        }
        List<Integer> lv = new java.util.ArrayList<>();
        if (levels != null) {
            for (Integer v : levels) {
                if (v != null && v > 0) {
                    lv.add(Math.min(200, v));
                }
            }
        }
        if (lv.isEmpty()) {
            lv.addAll(fallback.levels());
        }
        while (lv.size() < 3) {
            lv.add(lv.get(lv.size() - 1));
        }
        if (lv.size() > 3) {
            lv = new java.util.ArrayList<>(lv.subList(0, 3));
        }
        List<String> tp = sanitizedTypes(types);
        if (tp.isEmpty()) {
            tp.addAll(fallback.types());
        }
        int cadence = clamped("cadence-seconds", cadenceSeconds, 15, 3600, adjustments);
        List<Wave> wv = new java.util.ArrayList<>();
        if (waves != null) {
            for (Wave w : waves) {
                if (w != null) {
                    wv.add(sanitizedWave(w, fallback.waves().get(0),
                            adjustments, wv.size()));
                }
            }
        }
        if (wv.isEmpty()) {
            wv.addAll(fallback.waves());
        }
        return new SprinkleSettings(enabled, c, n, List.copyOf(lv), alive,
                perFight, leash, List.copyOf(tp), cadence, List.copyOf(wv));
    }

    /** One wave, sanitized against a fallback wave's pool. */
    public static Wave sanitizedWave(Wave wave, Wave fallback) {
        return sanitizedWave(wave, fallback, null, 0);
    }

    /** One wave, sanitized loud: a clamped wave count records
     *  its adjustment like any chapter scalar. */
    private static Wave sanitizedWave(Wave wave, Wave fallback,
                                      List<String> adjustments, int waveIndex) {
        List<String> tp = sanitizedTypes(wave.types());
        if (tp.isEmpty()) {
            tp.addAll(fallback.types());
        }
        int n = clamped("waves[" + waveIndex + "].count", wave.count(), 1, 8,
                adjustments);
        String placement = "aerial".equals(wave.placement()) ? "aerial" : "ring";
        return new Wave(List.copyOf(tp), n, placement);
    }

    /** Clamp an int, recording the change when it fires. */
    private static int clamped(String key, int value, int floor, int ceiling,
                               List<String> adjustments) {
        int out = Math.max(floor, Math.min(ceiling, value));
        if (adjustments != null && out != value) {
            adjustments.add(key + " " + value
                    + (value > ceiling ? " above ceiling" : " below floor")
                    + " — clamped to " + out);
        }
        return out;
    }

    /** Clamp a double, recording the change when it fires. */
    private static double clamped(String key, double value, double floor,
                                  double ceiling, List<String> adjustments) {
        double out = Math.max(floor, Math.min(ceiling, value));
        if (adjustments != null && out != value) {
            adjustments.add(key + " " + value
                    + (value > ceiling ? " above ceiling" : " below floor")
                    + " — clamped to " + out);
        }
        return out;
    }

    private static List<String> sanitizedTypes(List<String> types) {
        List<String> tp = new java.util.ArrayList<>();
        if (types != null) {
            for (String t : types) {
                if (t != null && !t.isBlank()) {
                    tp.add(t.trim().toUpperCase(java.util.Locale.ROOT));
                }
            }
        }
        return tp;
    }

    /** The fixed elite level for a stamp/rotation index (cycles
     *  the three settled levels). */
    public int levelFor(int stampIndex) {
        return levels.get(Math.floorMod(stampIndex, levels.size()));
    }

    /** The mob type drawn for the i-th elite of a wave (cycled).
     *  Legacy pool accessor - waves carry their own pools now. */
    public String typeFor(int waveIndex) {
        return types.get(Math.floorMod(waveIndex, types.size()));
    }

    /** The wave a rotation index owes (cycled). */
    public Wave waveFor(int rotationIndex) {
        return waves.get(Math.floorMod(rotationIndex, waves.size()));
    }

    /**
     * The settled defaults per chapter (study section 7, caps
     * and waves per the 0.4.0 ruling). Every chapter ships
     * DISABLED; the numbers below are the design Mike settled,
     * present as config defaults so arming a chapter is a
     * one-key edit. Caps rose with the cadence (6 -> 24 per
     * fight, 3 -> 5 alive): a guaranteed wave every 150 s plus
     * three milestone waves must have room to matter.
     */
    public static SprinkleSettings defaults(BossKind kind) {
        return switch (kind) {
            case WITHER -> new SprinkleSettings(false, 0.35, 2,
                    List.of(20, 22, 24), 5, 24, 40.0,
                    List.of("WITHER_SKELETON", "WITHER_SKELETON"),
                    DEFAULT_CADENCE_SECONDS,
                    List.of(new Wave(List.of("GHAST"), 1, "aerial"),
                            new Wave(List.of("ZOMBIFIED_PIGLIN"), 3, "ring"),
                            new Wave(List.of("WITHER_SKELETON"), 2, "ring")));
            case WARDEN -> new SprinkleSettings(false, 0.40, 2,
                    List.of(22, 24, 26), 5, 24, 32.0,
                    List.of("SKELETON", "ZOMBIE"),
                    DEFAULT_CADENCE_SECONDS,
                    List.of(new Wave(List.of("SKELETON", "ZOMBIE"), 2, "ring")));
            case ENDER_DRAGON -> new SprinkleSettings(false, 0.30, 2,
                    List.of(18, 20, 22), 5, 24, 48.0,
                    List.of("ENDERMAN", "ENDERMAN"),
                    DEFAULT_CADENCE_SECONDS,
                    List.of(new Wave(List.of("ENDERMAN", "ENDERMAN"), 2, "ring")));
            case ELDER_GUARDIAN -> new SprinkleSettings(false, 0.30, 2,
                    List.of(18, 20, 22), 5, 24, 24.0,
                    List.of("GUARDIAN", "DROWNED"),
                    DEFAULT_CADENCE_SECONDS,
                    List.of(new Wave(List.of("GUARDIAN", "DROWNED"), 2, "ring")));
        };
    }
}
