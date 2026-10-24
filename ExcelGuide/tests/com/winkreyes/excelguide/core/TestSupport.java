package com.winkreyes.excelguide.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * Shared fixtures for the ExcelGuide harnesses: a check counter in the
 * family style, the real project files (intents.yml, config.yml,
 * knowledge.yml via -Dexcelguide.proj), a fake clock, and a fake
 * StatsView whose seal state tests can flip.
 */
final class TestSupport {

    private TestSupport() {}

    /** Counted checks; finish() prints the tally and exits nonzero on failure. */
    static final class T {
        int checks;
        int failures;

        void check(String name, boolean condition) {
            checks++;
            if (!condition) {
                failures++;
                System.out.println("FAIL: " + name);
            }
        }

        void eq(String name, Object expected, Object actual) {
            checks++;
            if (!expected.equals(actual)) {
                failures++;
                System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
            }
        }

        void finish(String harness) {
            System.out.println(harness + ": " + (checks - failures) + "/" + checks
                    + " checks passed" + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
            if (failures > 0) {
                System.exit(1);
            }
        }
    }

    /** Mutable clock for cooldown/clarify tests. */
    static final class FakeClock implements LongSupplier {
        long now = 1_700_000_000_000L;

        @Override
        public long getAsLong() {
            return now;
        }

        void advanceSeconds(long s) {
            now += s * 1000L;
        }
    }

    /** Fake live data. Seal count is mutable so both Magic branches run. */
    static final class FakeStats implements StatsView {
        int seals = 0;
        final int total = 5;
        boolean nulls = false;

        @Override public String mcMMOSummary(UUID p) { return nulls ? null : "mcMMO overall 861 (Mining 142, Acrobatics 88)"; }
        @Override public String mcMMOSkill(UUID p, String skill) {
            if (nulls) {
                return null;
            }
            return switch (skill) {
                case "mining" -> "Mining (mcMMO) level 142";
                case "fishing" -> "Fishing (mcMMO) level 37";
                default -> null;
            };
        }
        @Override public String jobsSummary(UUID p) { return nulls ? null : "Jobs: Miner 12, Farmer 5"; }
        @Override public String playtimeSummary(UUID p) { return nulls ? null : "Played 23 h 40 m"; }
        @Override public String moneySummary(UUID p) { return nulls ? null : "Balance: $1,234.56"; }
        /**
         * GP claim blocks {accrued, bonus, used, remaining} (0.2.2).
         * Mike and Avery carry the REAL numbers read off the live
         * server Oct 18, 2026 (~/workspace/claim-blocks/), so the
         * combined-answer tests assert the exact strings a player
         * sees. nulls -> null: the no-GP fallback case.
         */
        @Override public long[] claimBlocks(UUID p) {
            if (nulls) {
                return null;
            }
            if (p.equals(MIKE.id())) {
                return new long[]{484, 0, 0, 484};
            }
            if (p.equals(AVERY.id())) {
                return new long[]{3492, 0, 1099, 2393};
            }
            return new long[]{100, 0, 0, 100};
        }
        @Override public String magicStanding(UUID p) { return nulls ? null : "Magic: Student path - level 1, 2 spells known"; }
        @Override public int sealsBroken() { return seals; }
        @Override public int sealsTotal() { return total; }
    }

    static final GuideEngine.PlayerRef MIKE =
            new GuideEngine.PlayerRef(UUID.fromString("11111111-1111-1111-1111-111111111111"), ".OwnerTest");
    static final GuideEngine.PlayerRef AVERY =
            new GuideEngine.PlayerRef(UUID.fromString("22222222-2222-2222-2222-222222222222"), ".testkite");
    static final GuideEngine.PlayerRef ROWAN =
            new GuideEngine.PlayerRef(UUID.fromString("33333333-3333-3333-3333-333333333333"), ".TestRunner");
    static final java.util.List<String> ONLINE =
            java.util.List.of(".OwnerTest", ".testkite", ".TestRunner", "ExamplePlayer");

    static Path projDir() {
        String p = System.getProperty("excelguide.proj");
        if (p == null) {
            throw new IllegalStateException("run via run-tests.sh (needs -Dexcelguide.proj)");
        }
        return Path.of(p);
    }

    static String readProj(String name) {
        try {
            return Files.readString(projDir().resolve(name), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("cannot read project file " + name, e);
        }
    }

    static GuideConfig realConfig() {
        return GuideConfig.fromYaml(readProj("config.yml"));
    }

    static IntentSet realIntents() {
        return IntentSet.fromYaml(readProj("intents.yml"));
    }

    static KnowledgeBase realKnowledge() {
        return KnowledgeBase.fromYaml(readProj("knowledge.yml"));
    }

    static GuideEngine engine(FakeStats stats, FakeClock clock) {
        return new GuideEngine(realConfig(), realIntents(), realKnowledge(), stats, clock, null);
    }

    static GuideEngine engine(FakeStats stats, FakeClock clock, UnansweredLog log) {
        return new GuideEngine(realConfig(), realIntents(), realKnowledge(), stats, clock, log);
    }
}
