package com.winkreyes.excelguildgate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Plain main-method test harness for NarrationGuide (same style as
 * CatchUpTrackerTest: no test framework, every check counted, any failure
 * prints and exits the JVM with status 1).
 *
 * Run via run-tests.sh, which compiles src + tests and runs this class.
 */
public final class NarrationGuideTest {

    private static int checks = 0;
    private static int failures = 0;

    private static void check(String name, boolean condition) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("FAIL: " + name);
        }
    }

    private static void checkEq(String name, Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    public static void main(String[] args) {
        defaultSoundPerStage();
        defaultGuidePerStage();
        outOfRangeStagesAreSilent();
        masterToggleSilencesEverything();
        replayPicksHighestMissedBeat();
        replayToggleOffSilencesOnlyReplay();
        replayEdgeCases();
        soundOverrideReplacesAndBlankSilences();
        guideOverrideWinsWholesale();
        partialOverridesFallBackPerStage();
        garbageEntriesAreTolerated();
        delayIsClampedAndKept();
        System.out.println("NarrationGuideTest: " + (checks - failures) + "/" + checks + " checks passed"
                + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void defaultSoundPerStage() {
        NarrationGuide g = NarrationGuide.defaults(true, true, 2L);
        for (int stage = 1; stage <= 5; stage++) {
            checkEq("default sound stage " + stage,
                    Optional.of("wrv:guildgate.seal" + stage), g.momentSound(stage));
        }
    }

    private static void defaultGuidePerStage() {
        NarrationGuide g = NarrationGuide.defaults(true, true, 2L);
        for (int stage = 1; stage <= 5; stage++) {
            List<String> lines = g.guideLines(stage);
            check("default guide stage " + stage + ": non-empty", !lines.isEmpty());
            check("default guide stage " + stage + ": at most 5 lines", lines.size() <= 5);
        }
        // The stage guides must not be one shared blob: each names its beat.
        check("guide 1 names Healer", g.guideLines(1).get(0).contains("Healer"));
        check("guide 2 names Pathfinder", g.guideLines(2).get(0).contains("Pathfinder"));
        check("guide 3 names Wonder", g.guideLines(3).get(0).contains("Wonder"));
        check("guide 4 names Masons", g.guideLines(4).get(0).contains("Masons"));
        check("guide 5 names Guild Opens", g.guideLines(5).get(0).contains("Guild Opens"));
        // Stage 1 carries the practical wand line (TestPlayerA's example), now
        // finalized village-first (Oct 17, 2026): the old quartermaster
        // in the village issues a bound apprentice wand at Seal 1.
        check("guide 1: wand line present",
                g.guideLines(1).stream().anyMatch(l -> l.contains("need a wand")));
        check("guide 1: quartermaster in the village",
                g.guideLines(1).stream().anyMatch(l -> l.contains("quartermaster in the village")));
        check("guide 1: bound apprentice wand",
                g.guideLines(1).stream().anyMatch(l -> l.contains("bound apprentice wand")));
        check("guide 1: old generic wand line gone",
                g.guideLines(1).stream().noneMatch(l -> l.contains("every mage's road")));
        // Every guide ends with the Skill Trees pointer.
        for (int stage = 1; stage <= 5; stage++) {
            List<String> lines = g.guideLines(stage);
            check("guide " + stage + ": Skill Trees pointer",
                    lines.get(lines.size() - 1).contains("Skill Trees"));
        }
    }

    private static void outOfRangeStagesAreSilent() {
        NarrationGuide g = NarrationGuide.defaults(true, true, 2L);
        for (int bad : new int[]{-1, 0, 6, 99}) {
            check("stage " + bad + ": no sound", g.momentSound(bad).isEmpty());
            check("stage " + bad + ": no guide", g.guideLines(bad).isEmpty());
        }
    }

    private static void masterToggleSilencesEverything() {
        NarrationGuide off = NarrationGuide.defaults(false, true, 2L);
        check("off: enabled() false", !off.enabled());
        for (int stage = 1; stage <= 5; stage++) {
            check("off: stage " + stage + " no sound", off.momentSound(stage).isEmpty());
            check("off: stage " + stage + " no guide", off.guideLines(stage).isEmpty());
        }
        check("off: no replay", off.replaySound(List.of(1, 2)).isEmpty());
    }

    private static void replayPicksHighestMissedBeat() {
        NarrationGuide g = NarrationGuide.defaults(true, true, 2L);
        checkEq("replay [1,2,3] -> seal3",
                Optional.of("wrv:guildgate.seal3"), g.replaySound(List.of(1, 2, 3)));
        checkEq("replay [5] -> seal5",
                Optional.of("wrv:guildgate.seal5"), g.replaySound(List.of(5)));
        checkEq("replay [2,4] -> seal4 (max, not last)",
                Optional.of("wrv:guildgate.seal4"), g.replaySound(List.of(2, 4)));
        checkEq("replay [4,2] -> seal4 (order-independent)",
                Optional.of("wrv:guildgate.seal4"), g.replaySound(List.of(4, 2)));
    }

    private static void replayToggleOffSilencesOnlyReplay() {
        NarrationGuide g = NarrationGuide.defaults(true, false, 2L);
        check("replay off: catchupReplay() false", !g.catchupReplay());
        check("replay off: no replay sound", g.replaySound(List.of(3)).isEmpty());
        checkEq("replay off: moment sound still plays",
                Optional.of("wrv:guildgate.seal3"), g.momentSound(3));
        check("replay off: guide still runs", !g.guideLines(3).isEmpty());
    }

    private static void replayEdgeCases() {
        NarrationGuide g = NarrationGuide.defaults(true, true, 2L);
        check("replay: null list", g.replaySound(null).isEmpty());
        check("replay: empty list", g.replaySound(List.of()).isEmpty());
        check("replay: out-of-range beats", g.replaySound(List.of(9)).isEmpty());
        check("replay: zero beat", g.replaySound(List.of(0)).isEmpty());
        checkEq("replay: nulls among beats ignored",
                Optional.of("wrv:guildgate.seal2"),
                g.replaySound(Arrays.asList(null, 2, null)));
        // Stateless/pure: consulting the plan twice gives the same answer
        // (once-only delivery is CatchUpTracker's job, via seen-stage).
        checkEq("replay: pure across calls",
                g.replaySound(List.of(1, 3)), g.replaySound(List.of(1, 3)));
    }

    private static void soundOverrideReplacesAndBlankSilences() {
        List<String> sounds = new ArrayList<>(Arrays.asList(
                "custom:one", "", null, "  custom:four  ", null));
        NarrationGuide g = NarrationGuide.build(true, true, 2L, sounds, null);
        checkEq("sound override stage 1", Optional.of("custom:one"), g.momentSound(1));
        check("sound override stage 2: blank silences voice",
                g.momentSound(2).isEmpty());
        check("sound override stage 2: guide still runs",
                !g.guideLines(2).isEmpty());
        checkEq("sound override stage 3: null entry falls back",
                Optional.of("wrv:guildgate.seal3"), g.momentSound(3));
        checkEq("sound override stage 4: trimmed",
                Optional.of("custom:four"), g.momentSound(4));
        checkEq("sound override stage 5: null entry falls back",
                Optional.of("wrv:guildgate.seal5"), g.momentSound(5));
        // The replay respects a silenced stage too.
        check("sound override: replay of silenced stage 2 is silent",
                g.replaySound(List.of(2)).isEmpty());
    }

    private static void guideOverrideWinsWholesale() {
        List<List<String>> guides = new ArrayList<>();
        guides.add(null);
        guides.add(List.of("&aCustom line one.", "&7Custom line two."));
        List<String> emptyGuide = List.of();
        guides.add(emptyGuide);
        NarrationGuide g = NarrationGuide.build(true, true, 2L, null, guides);
        check("guide override stage 1: null falls back", !g.guideLines(1).isEmpty());
        checkEq("guide override stage 2: exact custom lines",
                List.of("&aCustom line one.", "&7Custom line two."), g.guideLines(2));
        check("guide override stage 3: empty list means no guide",
                g.guideLines(3).isEmpty());
        checkEq("guide override stage 3: sound unaffected",
                Optional.of("wrv:guildgate.seal3"), g.momentSound(3));
        check("guide override stage 4: absent falls back", !g.guideLines(4).isEmpty());
        check("guide override stage 5: absent falls back", !g.guideLines(5).isEmpty());
    }

    private static void partialOverridesFallBackPerStage() {
        // Short tables (e.g. a config that only defines stage 1) leave the
        // remaining stages on their defaults.
        NarrationGuide g = NarrationGuide.build(true, true, 2L,
                List.of("custom:only"),
                List.of(List.of("&7Only line.")));
        checkEq("partial: stage 1 sound", Optional.of("custom:only"), g.momentSound(1));
        checkEq("partial: stage 1 guide", List.of("&7Only line."), g.guideLines(1));
        checkEq("partial: stage 5 sound default",
                Optional.of("wrv:guildgate.seal5"), g.momentSound(5));
        check("partial: stage 5 guide default", !g.guideLines(5).isEmpty());
    }

    private static void garbageEntriesAreTolerated() {
        List<List<String>> guides = new ArrayList<>();
        guides.add(Arrays.asList(null, "  ", "&7Real line."));
        NarrationGuide g = NarrationGuide.build(true, true, 2L, null, guides);
        checkEq("garbage: blank/null guide lines dropped",
                List.of("&7Real line."), g.guideLines(1));
    }

    private static void delayIsClampedAndKept() {
        checkEq("delay kept", 3L,
                NarrationGuide.defaults(true, true, 3L).guideLineDelaySeconds());
        checkEq("negative delay clamps to 0", 0L,
                NarrationGuide.defaults(true, true, -5L).guideLineDelaySeconds());
    }
}
