/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * One chapter's elite-sprinkle settings (ExcelBossTrial 0.3.3,
 * ELITE-SPRINKLE-STUDY-20261101 sections 3 and 7): the
 * per-chapter switches the adjustability law requires, read
 * adapter-side from the chapter's {@code sprinkle:} config block
 * (the milestone-settings precedent - no core verdict rule
 * consumes them, so BossTrialConfig's canonical record stays
 * untouched).
 *
 * The sprinkle is CHANCE-based per milestone stamp, never
 * guaranteed (Mike, Nov 1, 2026, 3:54 PM CST): at each 75/50/25%
 * stamp the chapter rolls once against {@code chance} times the
 * stamp multiplier; a passed roll spawns {@code count} elites at
 * the stamp's fixed level; a failed roll is silent in game. Every
 * chapter ships with {@code enabled: false} - Mike arms chapters
 * individually.
 *
 * The spawn pool is {@code types}, drawn in order and cycled:
 * with count 2, [SKELETON, ZOMBIE] spawns one of each and
 * [ENDERMAN, ENDERMAN] spawns two endermen. Plain mob types
 * only - no disguise key exists anywhere in this design (the
 * stage-2 standing rule: LibsDisguises bosses are invisible to
 * Bedrock players).
 */
public record SprinkleSettings(boolean enabled, double chance, int count,
                               List<Integer> levels, int maxAlive,
                               int maxPerFight, double leashRadius,
                               List<String> types) {

    /** The settled stamp multipliers (75% / 50% / 25% remaining):
     *  later stamps roll better odds (study section 3). */
    public static final List<Double> DEFAULT_STAMP_MULTIPLIERS =
            List.of(0.75, 1.0, 1.25);

    /** Effective chance never exceeds this, however the keys stack. */
    public static final double DEFAULT_CHANCE_CEILING = 0.90;

    /** The settled announcement (Mike's pick, Nov 1, 2026, 4:05 PM
     *  CST: a passed roll is ANNOUNCED, never silent). {chapter}
     *  is the boss's display name, {count} the elites spawned. */
    public static final String DEFAULT_ANNOUNCE_TEXT =
            "&4The seal fights back&7 - {count} elite reinforcements join "
                    + "the battle against {chapter}!";

    /**
     * Sanitized construction. Fail-dark on the chance (a NaN
     * chance reads as 0: a malformed key must never make elites
     * MORE likely); counts and caps clamp to sane ranges; the
     * levels list always ends up with exactly three positive
     * entries (a short list repeats its last entry, an empty one
     * falls back to the chapter default); an empty types list
     * falls back to the chapter default pool.
     */
    public static SprinkleSettings sanitized(boolean enabled, double chance, int count,
                                             List<Integer> levels, int maxAlive,
                                             int maxPerFight, double leashRadius,
                                             List<String> types, SprinkleSettings fallback) {
        double c = (Double.isNaN(chance) || Double.isInfinite(chance))
                ? 0.0 : Math.max(0.0, Math.min(1.0, chance));
        int n = Math.max(0, Math.min(8, count));
        int alive = Math.max(0, Math.min(16, maxAlive));
        int perFight = Math.max(0, Math.min(48, maxPerFight));
        double leash = (Double.isNaN(leashRadius) || Double.isInfinite(leashRadius))
                ? fallback.leashRadius()
                : Math.max(4.0, Math.min(128.0, leashRadius));
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
        List<String> tp = new java.util.ArrayList<>();
        if (types != null) {
            for (String t : types) {
                if (t != null && !t.isBlank()) {
                    tp.add(t.trim().toUpperCase(java.util.Locale.ROOT));
                }
            }
        }
        if (tp.isEmpty()) {
            tp.addAll(fallback.types());
        }
        return new SprinkleSettings(enabled, c, n, List.copyOf(lv), alive,
                perFight, leash, List.copyOf(tp));
    }

    /** The fixed elite level for a stamp bit (0 = 75%, 1 = 50%, 2 = 25%). */
    public int levelFor(int stampIndex) {
        return levels.get(Math.max(0, Math.min(2, stampIndex)));
    }

    /** The mob type drawn for the i-th elite of a wave (cycled). */
    public String typeFor(int waveIndex) {
        return types.get(Math.floorMod(waveIndex, types.size()));
    }

    /**
     * The settled defaults per chapter (study section 7). Every
     * chapter ships DISABLED; the numbers below are the design
     * Mike settled, present as config defaults so arming a
     * chapter is a one-key edit.
     */
    public static SprinkleSettings defaults(BossKind kind) {
        return switch (kind) {
            case WITHER -> new SprinkleSettings(false, 0.35, 2,
                    List.of(20, 22, 24), 3, 6, 40.0,
                    List.of("WITHER_SKELETON", "WITHER_SKELETON"));
            case WARDEN -> new SprinkleSettings(false, 0.40, 2,
                    List.of(22, 24, 26), 3, 6, 32.0,
                    List.of("SKELETON", "ZOMBIE"));
            case ENDER_DRAGON -> new SprinkleSettings(false, 0.30, 2,
                    List.of(18, 20, 22), 3, 6, 48.0,
                    List.of("ENDERMAN", "ENDERMAN"));
            case ELDER_GUARDIAN -> new SprinkleSettings(false, 0.30, 2,
                    List.of(18, 20, 22), 3, 6, 24.0,
                    List.of("GUARDIAN", "DROWNED"));
        };
    }
}
