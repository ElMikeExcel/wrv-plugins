/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * Elite sprinkle settings (0.3.3, study section 7): the settled
 * per-chapter defaults (every chapter dark), the stamp level
 * ladder, the cycled type pool, and sanitization - fail-dark on
 * the chance, clamps on every number, fallbacks for short or
 * empty lists.
 */
public final class SprinkleSettingsTest {

    private static int checks;
    private static int failures;

    private static void check(boolean ok, String label) {
        checks++;
        if (!ok) {
            failures++;
            System.out.println("FAIL: " + label);
        }
    }

    private static void eqD(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // --- the settled defaults, chapter by chapter (study section 7)
        SprinkleSettings wither = SprinkleSettings.defaults(BossKind.WITHER);
        check(!wither.enabled() && wither.chance() == 0.35 && wither.count() == 2
                        && wither.levels().equals(List.of(20, 22, 24))
                        && wither.maxAlive() == 5 && wither.maxPerFight() == 24
                        && wither.cadenceSeconds() == 150
                        && wither.waves().size() == 3
                        && wither.waves().get(0).types().equals(List.of("GHAST"))
                        && wither.waves().get(0).aerial()
                        && wither.waves().get(1).types().equals(List.of("ZOMBIFIED_PIGLIN"))
                        && wither.waves().get(1).count() == 3
                        && wither.waves().get(2).types().equals(List.of("WITHER_SKELETON"))
                        && wither.waves().get(2).count() == 2
                        && wither.leashRadius() == 40.0
                        && wither.types().equals(List.of("WITHER_SKELETON", "WITHER_SKELETON")),
                "wither defaults: dark, 0.35, 2x wither skeletons 20/22/24, leash 40");
        SprinkleSettings warden = SprinkleSettings.defaults(BossKind.WARDEN);
        check(!warden.enabled() && warden.chance() == 0.40
                        && warden.levels().equals(List.of(22, 24, 26))
                        && warden.leashRadius() == 32.0
                        && warden.types().equals(List.of("SKELETON", "ZOMBIE")),
                "warden defaults: dark, 0.40, skeleton+zombie 22/24/26, leash 32");
        SprinkleSettings dragon = SprinkleSettings.defaults(BossKind.ENDER_DRAGON);
        check(!dragon.enabled() && dragon.chance() == 0.30
                        && dragon.levels().equals(List.of(18, 20, 22))
                        && dragon.leashRadius() == 48.0
                        && dragon.types().equals(List.of("ENDERMAN", "ENDERMAN")),
                "dragon defaults: dark, 0.30, 2x endermen 18/20/22, leash 48");
        SprinkleSettings elder = SprinkleSettings.defaults(BossKind.ELDER_GUARDIAN);
        check(!elder.enabled() && elder.chance() == 0.30
                        && elder.levels().equals(List.of(18, 20, 22))
                        && elder.leashRadius() == 24.0
                        && elder.types().equals(List.of("GUARDIAN", "DROWNED")),
                "elder defaults: dark, 0.30, guardian+drowned 18/20/22, leash 24");
        for (BossKind kind : BossKind.values()) {
            check(!SprinkleSettings.defaults(kind).enabled(),
                    kind + " ships dark (enabled: false)");
        }

        // --- stamp levels and the cycled type pool
        check(wither.levelFor(0) == 20 && wither.levelFor(1) == 22
                && wither.levelFor(2) == 24, "levels climb by stamp");
        check(warden.typeFor(0).equals("SKELETON") && warden.typeFor(1).equals("ZOMBIE")
                && warden.typeFor(2).equals("SKELETON"), "type pool cycles in order");
        check(dragon.typeFor(0).equals("ENDERMAN") && dragon.typeFor(1).equals("ENDERMAN"),
                "a doubled pool entry spawns two of the same type");

        // --- sanitization: the chance fails DARK, numbers clamp
        SprinkleSettings nan = SprinkleSettings.sanitized(true, Double.NaN, 2,
                List.of(20, 22, 24), 3, 6, 40.0, List.of("ZOMBIE"), 150, null, wither);
        eqD(nan.chance(), 0.0, "NaN chance sanitizes to 0 (fail-dark)");
        SprinkleSettings over = SprinkleSettings.sanitized(true, 1.7, 99,
                List.of(20, 22, 24), 99, 999, 9999.0, List.of("ZOMBIE"), 150, null, wither);
        check(over.chance() == 1.0 && over.count() == 8 && over.maxAlive() == 16
                && over.maxPerFight() == 140 && over.leashRadius() == 128.0,
                "over-range values clamp to their ceilings");
        SprinkleSettings atCap = SprinkleSettings.sanitized(true, 0.35, 2,
                List.of(20, 22, 24), 5, 140, 40.0, List.of("ZOMBIE"), 150, null, wither);
        check(atCap.maxPerFight() == 140,
                "a max-per-fight of exactly 140 loads unsanitized-down (the ruled ceiling)");
        SprinkleSettings under = SprinkleSettings.sanitized(true, -0.5, -3,
                List.of(20, 22, 24), -1, -1, 0.5, List.of("ZOMBIE"), 150, null, wither);
        check(under.chance() == 0.0 && under.count() == 0 && under.maxAlive() == 0
                && under.maxPerFight() == 0 && under.leashRadius() == 4.0,
                "under-range values clamp to their floors");

        // --- list fallbacks
        SprinkleSettings shortLevels = SprinkleSettings.sanitized(false, 0.3, 2,
                List.of(18), 3, 6, 24.0, List.of("DROWNED"), 150, null, elder);
        check(shortLevels.levels().equals(List.of(18, 18, 18)),
                "a one-entry levels list repeats its last entry to three");
        SprinkleSettings noLevels = SprinkleSettings.sanitized(false, 0.3, 2,
                List.of(), 3, 6, 24.0, List.of("DROWNED"), 150, null, elder);
        check(noLevels.levels().equals(List.of(18, 20, 22)),
                "an empty levels list falls back to the chapter default");
        SprinkleSettings longLevels = SprinkleSettings.sanitized(false, 0.3, 2,
                List.of(1, 2, 3, 4, 5), 3, 6, 24.0, List.of("DROWNED"), 150, null, elder);
        check(longLevels.levels().equals(List.of(1, 2, 3)),
                "a long levels list trims to the three stamps");
        SprinkleSettings noTypes = SprinkleSettings.sanitized(false, 0.3, 2,
                List.of(18, 20, 22), 3, 6, 24.0, List.of(), 150, null, elder);
        check(noTypes.types().equals(List.of("GUARDIAN", "DROWNED")),
                "an empty types list falls back to the chapter pool");
        SprinkleSettings messyTypes = SprinkleSettings.sanitized(false, 0.3, 2,
                List.of(18, 20, 22), 3, 6, 24.0, List.of(" zombie ", "", "Skeleton"), 150, null, elder);
        check(messyTypes.types().equals(List.of("ZOMBIE", "SKELETON")),
                "types trim, uppercase, and skip blanks");

        // --- loud surfacing (0.4.0 rebuild): a clamp that fires
        // is RECORDED, never silent; the outputs do not change.
        java.util.List<String> adj = new java.util.ArrayList<>();
        SprinkleSettings loud = SprinkleSettings.sanitized(true, 1.7, 99,
                List.of(20, 22, 24), 99, 999, 9999.0, List.of("ZOMBIE"), 5,
                null, wither, adj);
        check(loud.chance() == 1.0 && loud.count() == 8 && loud.maxAlive() == 16
                        && loud.maxPerFight() == 140 && loud.leashRadius() == 128.0
                        && loud.cadenceSeconds() == 15,
                "the loud overload returns the same sanitized values as the quiet one");
        check(adj.stream().anyMatch(a -> a.startsWith("max-per-fight 999")
                        && a.contains("140")),
                "an over-range max-per-fight records its adjustment: " + adj);
        check(adj.stream().anyMatch(a -> a.startsWith("max-alive 99")),
                "an over-range max-alive records its adjustment");
        check(adj.stream().anyMatch(a -> a.startsWith("cadence-seconds 5")),
                "an under-range cadence records its adjustment");
        java.util.List<String> nanAdj = new java.util.ArrayList<>();
        SprinkleSettings.sanitized(true, Double.NaN, 2,
                List.of(20, 22, 24), 3, 6, 40.0, List.of("ZOMBIE"), 150,
                null, wither, nanAdj);
        check(nanAdj.size() == 1 && nanAdj.get(0).contains("chance"),
                "a NaN chance records exactly its one adjustment: " + nanAdj);
        java.util.List<String> quietAdj = new java.util.ArrayList<>();
        SprinkleSettings.sanitized(true, 0.35, 2,
                List.of(20, 22, 24), 5, 24, 40.0, List.of("ZOMBIE"), 150,
                null, wither, quietAdj);
        check(quietAdj.isEmpty(),
                "in-range values record no adjustments (silence = untouched)");

        // --- the global defaults are the settled ones
        check(SprinkleSettings.DEFAULT_STAMP_MULTIPLIERS.equals(List.of(0.75, 1.0, 1.25)),
                "stamp multipliers default to [0.75, 1.0, 1.25]");
        eqD(SprinkleSettings.DEFAULT_CHANCE_CEILING, 0.90, "chance ceiling defaults to 0.90");
        check(SprinkleSettings.DEFAULT_ANNOUNCE_TEXT.contains("{chapter}")
                        && SprinkleSettings.DEFAULT_ANNOUNCE_TEXT.contains("{count}"),
                "the announce text carries both placeholders");

        System.out.println("SprinkleSettingsTest: " + checks + " checks, " + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
