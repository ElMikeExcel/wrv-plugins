package com.winkreyes.excelmagicgate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Automated tests for ExcelMagicGate's pure logic (stage planning,
 * grant-list parsing, apply orchestration, state codec, announcement
 * defaults). Plain-JVM harness in the WRV house style: no test
 * framework, run by run-tests.sh; exits non-zero if any check fails.
 *
 * Usage: ExcelMagicGateTest [stagesDir]
 * When stagesDir is given, the REAL bundled stage packets are parsed
 * and their grant counts asserted against the WRV-070 packet values
 * (13 / 33 / 59 / 67 / 100 commands; stage 0 empty).
 */
public class ExcelMagicGateTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean condition, String name) {
        if (condition) {
            passed++;
            System.out.println("PASS " + name);
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        testStagePlan();
        testGrantListFixtures();
        if (args.length > 0) {
            testRealStagePackets(Path.of(args[0]));
        } else {
            System.out.println("SKIP real-packet grant tests (no stages dir given)");
        }
        testStageApplier();
        testBridgeState();
        testStageMessages();
        testRankFloor();
        testFloorPlan();
        if (args.length > 0) {
            testBandSizes(Path.of(args[0]));
        }
        testEarnedRank();
        testCeilingPlan();
        testCreditPolicy();
        testEarnedStore();
        testCreditLogicSimulations();
        testWandInventorySync();
        if (args.length > 1) {
            testConfigClassification(Path.of(args[1]));
        } else {
            System.out.println("SKIP config classification tests (no project dir given)");
        }
        if (args.length > 2) {
            testPlacementFixtures(Path.of(args[2]));
            testInventoryFixtures(Path.of(args[0]), Path.of(args[2]));
        } else {
            System.out.println("SKIP placement fixture tests (no fixtures dir given)");
        }
        System.out.println("ExcelMagicGateTest: passed=" + passed + " failed=" + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    // ----------------------------------------------------------------

    private static void testStagePlan() {
        check(StagePlan.decide(true, true, 1, 0).action() == StagePlan.Action.APPLY
                && StagePlan.decide(true, true, 1, 0).targetStage() == 1,
                "plan: seal 1 fallen, stage 0 applied -> APPLY 1");
        check(StagePlan.decide(true, true, 3, 1).action() == StagePlan.Action.APPLY
                && StagePlan.decide(true, true, 3, 1).targetStage() == 3,
                "plan: two seals between ticks -> APPLY straight to 3 (cumulative packets)");
        check(StagePlan.decide(true, true, 2, 2).action() == StagePlan.Action.NONE,
                "plan: eligible == applied -> NONE (idempotent)");
        check(StagePlan.decide(true, true, 0, 0).action() == StagePlan.Action.NONE,
                "plan: fresh install at 0 seals -> NONE");
        check(StagePlan.decide(true, true, 0, 2).action() == StagePlan.Action.WARN_DOWNGRADE,
                "plan: seals drop below applied (revoke) -> WARN_DOWNGRADE, never downgrade");
        check(StagePlan.decide(true, true, 4, 5).action() == StagePlan.Action.WARN_DOWNGRADE,
                "plan: 4 seals vs stage 5 applied -> WARN_DOWNGRADE");
        check(StagePlan.decide(false, true, 5, 0).action() == StagePlan.Action.NONE,
                "plan: master switch off -> NONE even with all seals fallen");
        check(StagePlan.decide(true, false, 0, 0).action() == StagePlan.Action.UNAVAILABLE,
                "plan: seal source unavailable -> UNAVAILABLE");
        check(StagePlan.decide(true, true, 9, 0).targetStage() == 5,
                "plan: eligible count clamped to stage 5");
        check(StagePlan.decide(true, true, 5, 5).action() == StagePlan.Action.NONE,
                "plan: everything applied -> NONE");
        check(StagePlan.clampStage(-3) == 0 && StagePlan.clampStage(7) == 5
                && StagePlan.clampStage(3) == 3, "plan: clampStage bounds");
    }

    // ----------------------------------------------------------------

    private static void testGrantListFixtures() {
        GrantList.Parsed p = GrantList.parse(List.of(
                "# Header comment",
                "",
                "lp user x permission set a true",
                "   ",
                "#lp commented-out command",
                "lp group default permission set magic.cast.heal true"));
        check(p.commands().equals(List.of(
                        "lp user x permission set a true",
                        "lp group default permission set magic.cast.heal true")),
                "grants: comments and blanks skipped, order kept");
        check(p.preCommands().isEmpty(), "grants: no builder group -> no pre-commands");

        GrantList.Parsed b = GrantList.parse(List.of(
                "# Create the group first if it does not exist: lp creategroup builder",
                "lp group default permission set magic.cast.heal true",
                "lp group builder permission set magic.cast.walls true",
                "lp group builder permission set magic.cast.tent true"));
        check(b.size() == 3, "grants: builder list counts only real commands");
        check(b.preCommands().equals(List.of("lp creategroup builder")),
                "grants: builder grants trigger one creategroup pre-command (comment alone would not)");

        GrantList.Parsed empty = GrantList.parse(List.of("# only comments", "", "# stage 0"));
        check(empty.size() == 0 && empty.preCommands().isEmpty(),
                "grants: comment-only file (stage 0) parses to zero commands");

        GrantList.Parsed nul = GrantList.parse(null);
        check(nul.size() == 0, "grants: null input parses to zero commands");

        GrantList.Parsed ws = GrantList.parse(List.of("  lp group default permission set a.b true  "));
        check(ws.commands().equals(List.of("lp group default permission set a.b true")),
                "grants: surrounding whitespace trimmed");
    }

    private static void testRealStagePackets(Path stagesDir) throws Exception {
        int[] expected = {0, 13, 40, 68, 82, 115}; // 0.2.1 template refresh (Item 41): Nov 8 spell-set grants added cumulatively (stage2 +7, stage3 +9, stage4 +15, stage5 +15)
        for (int stage = 0; stage <= 5; stage++) {
            Path file = stagesDir.resolve("stage" + stage).resolve("grants.txt");
            check(Files.isRegularFile(file), "packets: stage " + stage + " grants.txt exists");
            GrantList.Parsed parsed = GrantList.parse(Files.readAllLines(file));
            check(parsed.size() == expected[stage],
                    "packets: stage " + stage + " grant count == " + expected[stage]
                            + " (got " + parsed.size() + ")");
            if (stage == 4) {
                check(parsed.preCommands().equals(List.of("lp creategroup builder")),
                        "packets: stage 4 pre-command creates the builder group");
            } else {
                check(parsed.preCommands().isEmpty(),
                        "packets: stage " + stage + " has no pre-commands");
            }
        }
        // The heal grant is the packet runbooks' own probe.
        GrantList.Parsed s1 = GrantList.parse(Files.readAllLines(
                stagesDir.resolve("stage1").resolve("grants.txt")));
        check(s1.commands().contains("lp group default permission set magic.cast.heal true"),
                "packets: stage 1 grants magic.cast.heal");
        check(!s1.commands().toString().contains("chainlightning"),
                "packets: stage 1 does NOT grant chainlightning (seal 3 spell)");
        GrantList.Parsed s5 = GrantList.parse(Files.readAllLines(
                stagesDir.resolve("stage5").resolve("grants.txt")));
        check(!s5.commands().toString().contains("magic.cast.lava"),
                "packets: stage 5 never grants lava (permanently locked shelf)");
        // Every stage ships the six managed Magic config files.
        for (int stage = 0; stage <= 5; stage++) {
            for (String name : List.of("config.yml", "spells.yml", "paths.yml",
                    "wands.yml", "classes.yml", "messages.yml")) {
                check(Files.isRegularFile(stagesDir.resolve("stage" + stage).resolve(name)),
                        "packets: stage " + stage + " ships " + name);
            }
        }
        // Stage 0's spells still define stormshackle (the canary), and
        // stage 1's grants include cure (its own runbook probe).
        String s0spells = Files.readString(stagesDir.resolve("stage0").resolve("spells.yml"));
        check(s0spells.contains("stormshackle"), "packets: stage 0 spells contain the stormshackle canary");
        check(s1.commands().contains("lp group default permission set magic.cast.cure true"),
                "packets: stage 1 grants magic.cast.cure");
        // Regression guard (Mike's decision, Oct 18, 2026): NO stage
        // grants magic.commands.wand.restore. Player wand reissue is
        // console-only (/wandp <player> restore, else /mgive): /wand
        // restore cannot see wands parked in the Utility Pouch and
        // could mint duplicate bound wands a player cannot delete.
        boolean anyRestore = false;
        for (int stage = 0; stage <= 5; stage++) {
            GrantList.Parsed p = GrantList.parse(Files.readAllLines(
                    stagesDir.resolve("stage" + stage).resolve("grants.txt")));
            if (p.commands().toString().contains("magic.commands.wand.restore")) {
                anyRestore = true;
            }
        }
        check(!anyRestore,
                "packets: NO stage grants magic.commands.wand.restore (console-only reissue)");
    }

    // ----------------------------------------------------------------

    /** Scripted fake steps recording their call order. */
    private static final class FakeSteps implements StageApplier.Steps {
        final List<String> calls = new ArrayList<>();
        String failAt = null;

        private void step(String name) throws Exception {
            calls.add(name);
            if (name.equals(failAt)) {
                throw new Exception("boom at " + name);
            }
        }

        @Override public void backup() throws Exception { step("backup"); }
        @Override public void swapConfig() throws Exception { step("swap"); }
        @Override public void reloadMagic() throws Exception { step("reload"); }
        @Override public void runGrants() throws Exception { step("grants"); }
        @Override public void reconcileFloor() throws Exception { step("floor"); }
        @Override public void persist() throws Exception { step("persist"); }
        @Override public void announce() throws Exception { step("announce"); }
    }

    private static void testStageApplier() {
        FakeSteps ok = new FakeSteps();
        StageApplier.Result r = StageApplier.apply(ok, null);
        check(r.success(), "applier: all steps pass -> success");
        check(ok.calls.equals(List.of("backup", "swap", "reload", "grants", "floor", "persist", "announce")),
                "applier: steps run in contract order (floor reconcile between grants and persist)");

        FakeSteps failFloor = new FakeSteps();
        failFloor.failAt = "floor";
        r = StageApplier.apply(failFloor, null);
        check(!r.success() && "reconcileFloor".equals(r.failedStep())
                && !failFloor.calls.contains("persist"),
                "applier: floor failure -> appliedStage does not advance (idempotent reconcile retries safely)");

        FakeSteps failSwap = new FakeSteps();
        failSwap.failAt = "swap";
        r = StageApplier.apply(failSwap, null);
        check(!r.success() && "swapConfig".equals(r.failedStep()),
                "applier: swap failure reports the failed step");
        check(!failSwap.calls.contains("persist") && !failSwap.calls.contains("announce"),
                "applier: swap failure -> stage NOT persisted, no announcement");

        FakeSteps failGrants = new FakeSteps();
        failGrants.failAt = "grants";
        r = StageApplier.apply(failGrants, null);
        check(!r.success() && !failGrants.calls.contains("persist"),
                "applier: grants failure -> appliedStage does not advance");

        FakeSteps failBackup = new FakeSteps();
        failBackup.failAt = "backup";
        r = StageApplier.apply(failBackup, null);
        check(!r.success() && failBackup.calls.equals(List.of("backup")),
                "applier: backup failure aborts before any change");

        FakeSteps failReload = new FakeSteps();
        failReload.failAt = "reload";
        r = StageApplier.apply(failReload, null);
        check(!r.success() && "reloadMagic".equals(r.failedStep())
                && !failReload.calls.contains("grants"),
                "applier: reload failure stops before grants");

        FakeSteps failAnnounce = new FakeSteps();
        failAnnounce.failAt = "announce";
        List<String> warnings = new ArrayList<>();
        r = StageApplier.apply(failAnnounce, warnings::add);
        check(r.success() && failAnnounce.calls.contains("persist") && warnings.size() == 1,
                "applier: announcement failure after persist is a warning, not a failed apply");

        FakeSteps failPersist = new FakeSteps();
        failPersist.failAt = "persist";
        r = StageApplier.apply(failPersist, null);
        check(!r.success() && "persist".equals(r.failedStep()),
                "applier: persist failure is a failed apply (will retry)");
    }

    // ----------------------------------------------------------------

    private static void testBridgeState() {
        BridgeState fresh = BridgeState.fromMap(null);
        check(fresh.appliedStage() == 0 && fresh.lastKnownSeals() == -1
                && fresh.lastApplyStage() == -1, "state: null map -> fresh defaults");

        BridgeState s = new BridgeState();
        s.setAppliedStage(3);
        s.setLastKnownSeals(3);
        s.recordApply(3, "OK (test)");
        BridgeState back = BridgeState.fromMap(s.toMap());
        check(back.appliedStage() == 3 && back.lastKnownSeals() == 3
                && back.lastApplyStage() == 3 && "OK (test)".equals(back.lastApplyResult()),
                "state: codec round-trip preserves fields");

        BridgeState junk = BridgeState.fromMap(Map.of(
                "appliedStage", "not-a-number",
                "lastKnownSeals", new Object()));
        check(junk.appliedStage() == 0 && junk.lastKnownSeals() == -1,
                "state: garbage values fall back to defaults");

        BridgeState clamped = BridgeState.fromMap(Map.of("appliedStage", 42));
        check(clamped.appliedStage() == 5, "state: appliedStage clamped to 0..5");

        BridgeState stringy = BridgeState.fromMap(Map.of("appliedStage", "2"));
        check(stringy.appliedStage() == 2, "state: string numbers accepted");
    }

    // ----------------------------------------------------------------

    private static void testStageMessages() {
        check("The Healer".equals(StageMessages.defaultsFor(1).name()), "messages: stage 1 = The Healer");
        check("The Pathfinder".equals(StageMessages.defaultsFor(2).name()), "messages: stage 2 = The Pathfinder");
        check("The Wonder".equals(StageMessages.defaultsFor(3).name()), "messages: stage 3 = The Wonder");
        check("The Masons".equals(StageMessages.defaultsFor(4).name()), "messages: stage 4 = The Masons");
        check(StageMessages.defaultsFor(5).name().contains("Schools Open"), "messages: stage 5 = Schools Open");
        check(StageMessages.defaultsFor(0) == null, "messages: stage 0 has no announcement");
        check(StageMessages.defaultsFor(6) == null, "messages: out-of-range stage has no announcement");
        for (int stage = 1; stage <= 5; stage++) {
            StageMessages.StageMessage m = StageMessages.defaultsFor(stage);
            check(m.title() != null && !m.title().isBlank()
                    && m.broadcast() != null && !m.broadcast().isEmpty()
                    && m.unlocks() != null && !m.unlocks().isBlank(),
                    "messages: stage " + stage + " defaults are complete");
        }
        StageMessages.StageMessage over = StageMessages.resolve(1,
                Map.of("title", "&cCustom"), Map.of());
        check("&cCustom".equals(over.title()) && "The Healer".equals(over.name()),
                "messages: one-field override keeps the other defaults");
        StageMessages.StageMessage blank = StageMessages.resolve(1,
                Map.of("title", "  "), Map.of());
        check(blank.title().equals(StageMessages.defaultsFor(1).title()),
                "messages: blank override falls back to the default");
        StageMessages.StageMessage lines = StageMessages.resolve(2,
                Map.of(), Map.of("broadcast", List.of("one", "two")));
        check(lines.broadcast().equals(List.of("one", "two")),
                "messages: broadcast list override replaces default lines");
        check(StageMessages.resolve(0, Map.of(), Map.of()) == null,
                "messages: resolve(stage 0) is null");
    }

    // ----------------------------------------------------------------
    // 0.2.0: stage-floor rank reconciliation (Item ㉝-b)
    // ----------------------------------------------------------------

    private static void testRankFloor() {
        // The mapping FIXED in BUILD-BRIEF.md before coding:
        // floorRankIndex(stage) = clamp(stage - 1, 0, 3).
        check(RankFloor.floorForStage(0) == RankFloor.Rank.BEGINNER, "floor: stage 0 -> beginner");
        check(RankFloor.floorForStage(1) == RankFloor.Rank.BEGINNER,
                "floor: stage 1 -> beginner (the stage-1 earn stays earned)");
        check(RankFloor.floorForStage(2) == RankFloor.Rank.STUDENT, "floor: stage 2 -> student");
        check(RankFloor.floorForStage(3) == RankFloor.Rank.APPRENTICE, "floor: stage 3 -> apprentice");
        check(RankFloor.floorForStage(4) == RankFloor.Rank.MASTER, "floor: stage 4 -> master");
        check(RankFloor.floorForStage(5) == RankFloor.Rank.MASTER,
                "floor: stage 5 -> master (SATURATES - no rank above master; the stage-5 band growth cannot re-arm the trap)");
        check(RankFloor.floorForStage(-2) == RankFloor.Rank.BEGINNER
                && RankFloor.floorForStage(9) == RankFloor.Rank.MASTER,
                "floor: out-of-range stages clamp");

        // The convicted frame ladder (investigation of record).
        check(RankFloor.Rank.BEGINNER.manaMax() == 50 && RankFloor.Rank.BEGINNER.manaRegeneration() == 5,
                "floor: beginner frame 50/5");
        check(RankFloor.Rank.STUDENT.manaMax() == 100 && RankFloor.Rank.STUDENT.manaRegeneration() == 15,
                "floor: student frame 100/15 (Mike's frozen numbers)");
        check(RankFloor.Rank.APPRENTICE.manaMax() == 150 && RankFloor.Rank.APPRENTICE.manaRegeneration() == 25,
                "floor: apprentice frame 150/25");
        check(RankFloor.Rank.MASTER.manaMax() == 200 && RankFloor.Rank.MASTER.manaRegeneration() == 35
                && RankFloor.Rank.MASTER.hotbarCount() == 3,
                "floor: master frame 200/35, hotbar 3 (the WandBars re-read case)");
        check(RankFloor.Rank.BEGINNER.hotbarCount() == 1 && RankFloor.Rank.STUDENT.hotbarCount() == 2
                && RankFloor.Rank.APPRENTICE.hotbarCount() == 2,
                "floor: hotbar counts 1/2/2 below master");

        check(RankFloor.stepsToFloor(RankFloor.Rank.BEGINNER, RankFloor.Rank.MASTER)
                        .equals(List.of("student", "apprentice", "master")),
                "floor: beginner -> master is 3 ordered steps");
        check(RankFloor.stepsToFloor(RankFloor.Rank.STUDENT, RankFloor.Rank.MASTER)
                        .equals(List.of("apprentice", "master")),
                "floor: student -> master is 2 steps (Mike's live case at stage 4)");
        check(RankFloor.stepsToFloor(RankFloor.Rank.MASTER, RankFloor.Rank.MASTER).isEmpty(),
                "floor: at floor -> no steps");
        check(RankFloor.stepsToFloor(RankFloor.Rank.MASTER, RankFloor.Rank.STUDENT).isEmpty(),
                "floor: above floor -> no steps (never steps down)");
        check(RankFloor.stepsToFloor(RankFloor.Rank.APPRENTICE, RankFloor.Rank.BEGINNER).isEmpty(),
                "floor: apprentice vs stage-1 floor -> no steps (earned-ahead is kept)");

        check(RankFloor.fromKey("student") == RankFloor.Rank.STUDENT
                && RankFloor.fromKey(" Master ") == RankFloor.Rank.MASTER
                && RankFloor.fromKey("masons") == null && RankFloor.fromKey(null) == null
                && RankFloor.fromKey("") == null,
                "floor: fromKey parses the four ranks, unknown keys are null (never guessed)");

        check(RankFloor.frameMatches(RankFloor.Rank.MASTER, 200, 35)
                && !RankFloor.frameMatches(RankFloor.Rank.MASTER, 100, 15)
                && !RankFloor.frameMatches(null, 200, 35),
                "floor: frame verification is exact");
        check(BuildInfo.BUILD_ID.startsWith("EXC-MAGICGATE-0.3.0-WRV-20261110-02-"),
                "floor: build fingerprint present (Layer B, amended build -02)");
    }

    private static void testFloorPlan() {
        RankFloor.Rank master = RankFloor.Rank.MASTER;
        FloorPlan.Decision d = FloorPlan.decide(
                FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "student", master);
        check(d.outcome() == FloorPlan.Outcome.STEP && d.steps() == 2,
                "plan: carried student wand at stage 4 -> STEP x2");
        d = FloorPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "beginner", master);
        check(d.outcome() == FloorPlan.Outcome.STEP && d.steps() == 3,
                "plan: carried beginner wand at stage 4 -> STEP x3 (a wand issued today)");
        d = FloorPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "master", master);
        check(d.outcome() == FloorPlan.Outcome.ALREADY_AT_FLOOR && d.steps() == 0,
                "plan: master at stage 4 -> ALREADY");
        d = FloorPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "apprentice",
                RankFloor.Rank.STUDENT);
        check(d.outcome() == FloorPlan.Outcome.ALREADY_AT_FLOOR,
                "plan: apprentice above a student floor -> ALREADY (no downgrade)");
        d = FloorPlan.decide(FloorPlan.WandAccess.PARKED_IN_POUCH, "student", master);
        check(d.outcome() == FloorPlan.Outcome.DEFERRED_PARKED_POUCH && d.steps() == 0,
                "plan: wand parked in pouch -> DEFERRED to activation (PouchQuery is query-only)");
        d = FloorPlan.decide(FloorPlan.WandAccess.NO_LIVE_WAND, null, master);
        check(d.outcome() == FloorPlan.Outcome.DEFERRED_NO_LIVE_WAND,
                "plan: no live wand and no pouch report -> DEFERRED to activation");
        d = FloorPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "masons", master);
        check(d.outcome() == FloorPlan.Outcome.SKIP_UNKNOWN_RANK,
                "plan: unknown path key -> SKIP (never guessed, never mutated)");
        d = FloorPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY, "beginner",
                RankFloor.Rank.BEGINNER);
        check(d.outcome() == FloorPlan.Outcome.ALREADY_AT_FLOOR,
                "plan: beginner at stage 1 floor beginner -> ALREADY (stage-1 earn preserved)");
    }

    /**
     * Spell-logic gate (house rule): the trap is a DATA shape — the
     * cumulative bands grew after players completed them. Pin the
     * band sizes counted from the real stage packets (investigation
     * §2 table) so a future packet edit that changes them fails here
     * loudly instead of silently changing what the floor must cover.
     */
    private static void testBandSizes(Path stagesDir) throws Exception {
        int[][] expected = {
                // beginner, student, apprentice, master per stage file
                null,               // stage 0 not in the investigation table
                {4, 4, 2, 2},       // stage 1
                {10, 13, 6, 3},     // stage 2
                {17, 23, 11, 7},    // stage 3
                {17, 23, 11, 7},    // stage 4
                {21, 30, 20, 28},   // stage 5
        };
        String[] ranks = {"beginner", "student", "apprentice", "master"};
        for (int stage = 1; stage <= 5; stage++) {
            List<String> lines = Files.readAllLines(
                    stagesDir.resolve("stage" + stage).resolve("paths.yml"));
            java.util.Map<String, Integer> counts = new java.util.LinkedHashMap<>();
            String rank = null;
            boolean inSpells = false;
            for (String line : lines) {
                if (!line.isEmpty() && !Character.isWhitespace(line.charAt(0))
                        && line.endsWith(":")) {
                    rank = line.substring(0, line.length() - 1).trim();
                    inSpells = false;
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.equals("spells:")) {
                    inSpells = true;
                    continue;
                }
                if (trimmed.endsWith(":") && !trimmed.startsWith("-")) {
                    inSpells = false;
                    continue;
                }
                if (inSpells && trimmed.startsWith("- ") && rank != null) {
                    counts.merge(rank, 1, Integer::sum);
                }
            }
            for (int i = 0; i < ranks.length; i++) {
                int got = counts.getOrDefault(ranks[i], 0);
                check(got == expected[stage][i],
                        "bands: stage " + stage + " " + ranks[i] + " band == " + expected[stage][i]
                                + " (got " + got + ")");
            }
        }
        // The trap, stated as arithmetic: at stage 4 a student mage
        // owes 23 known spells for ONE natural upgrade; the floor
        // covers the frame in 2 steps regardless. At stage 5 the
        // student band grows to 30 — above any floor question, since
        // the floor has already saturated at master.
        check(expected[4][1] == 23 && expected[5][1] == 30
                && RankFloor.stepsToFloor(RankFloor.Rank.STUDENT,
                        RankFloor.floorForStage(5)).size() == 2,
                "bands: stage-5 student growth to 30 is covered by floor saturation at master");
    }

    // ----------------------------------------------------------------
    // 0.3.0: Magic Rank earning system (DESIGN.md, magic-rank-packet)
    // ----------------------------------------------------------------

    private static void testEarnedRank() {
        EarnedRank.Thresholds t = EarnedRank.Thresholds.locked();
        check(t.studentAt() == 100 && t.apprenticeAt() == 400 && t.masterAt() == 2000,
                "earned: LOCKED thresholds 100 / 400 / 2000 (Mike, Nov 9 2026)");
        check(EarnedRank.rankFor(0, t) == RankFloor.Rank.BEGINNER, "earned: 0 -> beginner");
        check(EarnedRank.rankFor(99, t) == RankFloor.Rank.BEGINNER, "earned: 99 -> beginner");
        check(EarnedRank.rankFor(100, t) == RankFloor.Rank.STUDENT, "earned: 100 -> student");
        check(EarnedRank.rankFor(399, t) == RankFloor.Rank.STUDENT, "earned: 399 -> student");
        check(EarnedRank.rankFor(400, t) == RankFloor.Rank.APPRENTICE, "earned: 400 -> apprentice");
        check(EarnedRank.rankFor(1999, t) == RankFloor.Rank.APPRENTICE, "earned: 1999 -> apprentice");
        check(EarnedRank.rankFor(2000, t) == RankFloor.Rank.MASTER, "earned: 2000 -> master");
        check(EarnedRank.rankFor(2001, t) == RankFloor.Rank.MASTER
                && EarnedRank.rankFor(11231, t) == RankFloor.Rank.MASTER,
                "earned: above 2000 stays master");
        // Ceiling application at every stage (ceiling table == floor table).
        check(EarnedRank.targetFor(11231, RankFloor.floorForStage(0), t) == RankFloor.Rank.BEGINNER,
                "earned: master-earned at stage 0 ceiling -> beginner");
        check(EarnedRank.targetFor(11231, RankFloor.floorForStage(2), t) == RankFloor.Rank.STUDENT,
                "earned: master-earned at stage 2 ceiling -> student");
        check(EarnedRank.targetFor(11231, RankFloor.floorForStage(3), t) == RankFloor.Rank.APPRENTICE,
                "earned: master-earned at stage 3 ceiling -> apprentice");
        check(EarnedRank.targetFor(11231, RankFloor.floorForStage(4), t) == RankFloor.Rank.MASTER
                && EarnedRank.targetFor(11231, RankFloor.floorForStage(5), t) == RankFloor.Rank.MASTER,
                "earned: master-earned at stage 4/5 ceiling -> master");
        check(EarnedRank.targetFor(150, RankFloor.floorForStage(4), t) == RankFloor.Rank.STUDENT,
                "earned: below ceiling -> the earned rank itself");
        // Remaining-to-next math, incl. the two locked placement margins.
        check(EarnedRank.remainingToNext(0, t) == 100, "earned: 0 -> 100 to student");
        check(EarnedRank.remainingToNext(99, t) == 1, "earned: 99 -> 1 to student");
        check(EarnedRank.remainingToNext(100, t) == 300, "earned: 100 -> 300 to apprentice");
        check(EarnedRank.remainingToNext(1210, t) == 790,
                "earned: Mike's placement 1210 -> 790 to master (DESIGN.md S8)");
        check(EarnedRank.remainingToNext(12, t) == 88,
                "earned: Kailey's placement 12 -> 88 to student (DESIGN.md S8)");
        check(EarnedRank.remainingToNext(2000, t) == 0 && EarnedRank.nextRank(2000, t) == null,
                "earned: at master there is no next rank");
        check(EarnedRank.nextRank(0, t) == RankFloor.Rank.STUDENT
                && EarnedRank.nextRank(400, t) == RankFloor.Rank.MASTER,
                "earned: next-rank chain");
    }

    private static void testCeilingPlan() {
        RankFloor.Rank master = RankFloor.Rank.MASTER;
        // Step-up leg (retargeted 0.2.0 machinery).
        CeilingPlan.Decision d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "beginner", RankFloor.Rank.STUDENT, master, true);
        check(d.outcome() == CeilingPlan.Outcome.STEP_UP && d.steps() == 1
                && d.target() == RankFloor.Rank.STUDENT,
                "ceiling: beginner wand, student earned -> STEP_UP x1");
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "beginner", RankFloor.Rank.MASTER, master, true);
        check(d.outcome() == CeilingPlan.Outcome.STEP_UP && d.steps() == 3,
                "ceiling: beginner wand, master earned + ceiling -> STEP_UP x3");
        // Earned above ceiling is capped by the ceiling.
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "beginner", RankFloor.Rank.MASTER, RankFloor.Rank.STUDENT, true);
        check(d.outcome() == CeilingPlan.Outcome.STEP_UP && d.steps() == 1
                && d.target() == RankFloor.Rank.STUDENT,
                "ceiling: master earned under student ceiling -> target student");
        // At target.
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "apprentice", RankFloor.Rank.APPRENTICE, master, true);
        check(d.outcome() == CeilingPlan.Outcome.AT_TARGET && d.steps() == 0,
                "ceiling: at target -> AT_TARGET");
        // THE DEMOTION MATRIX (DESIGN.md S1): Mike's switch-over case.
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "master", RankFloor.Rank.APPRENTICE, master, true);
        check(d.outcome() == CeilingPlan.Outcome.DEMOTE && d.steps() == 1
                && d.target() == RankFloor.Rank.APPRENTICE,
                "ceiling: master wand, apprentice earned (Mike 1210) -> DEMOTE x1");
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "master", RankFloor.Rank.BEGINNER, master, true);
        check(d.outcome() == CeilingPlan.Outcome.DEMOTE && d.steps() == 3,
                "ceiling: master wand, beginner earned -> DEMOTE x3");
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "master", RankFloor.Rank.APPRENTICE, master, false);
        check(d.outcome() == CeilingPlan.Outcome.DEMOTE_HELD && d.steps() == 0,
                "ceiling: demotion disabled -> DEMOTE_HELD (named, never silent)");
        // Ceiling drop can also force a demotion (seal revoke shape).
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "master", RankFloor.Rank.MASTER, RankFloor.Rank.STUDENT, true);
        check(d.outcome() == CeilingPlan.Outcome.DEMOTE && d.target() == RankFloor.Rank.STUDENT,
                "ceiling: master earned under a student ceiling -> DEMOTE to ceiling");
        // Deferrals bind BOTH directions — a serialized wand is never edited.
        d = CeilingPlan.decide(FloorPlan.WandAccess.PARKED_IN_POUCH,
                "master", RankFloor.Rank.APPRENTICE, master, true);
        check(d.outcome() == CeilingPlan.Outcome.DEFERRED_PARKED_POUCH,
                "ceiling: pouch-parked demotion -> DEFERRED to activation");
        d = CeilingPlan.decide(FloorPlan.WandAccess.PARKED_IN_POUCH,
                "beginner", RankFloor.Rank.MASTER, master, true);
        check(d.outcome() == CeilingPlan.Outcome.DEFERRED_PARKED_POUCH,
                "ceiling: pouch-parked step-up -> DEFERRED to activation");
        d = CeilingPlan.decide(FloorPlan.WandAccess.NO_LIVE_WAND,
                "master", RankFloor.Rank.APPRENTICE, master, true);
        check(d.outcome() == CeilingPlan.Outcome.DEFERRED_NO_LIVE_WAND,
                "ceiling: no live wand demotion -> DEFERRED to activation");
        // Unknown rank is never guessed at, in either direction.
        d = CeilingPlan.decide(FloorPlan.WandAccess.ACTIVE_OR_INVENTORY,
                "masons", RankFloor.Rank.MASTER, master, true);
        check(d.outcome() == CeilingPlan.Outcome.SKIP_UNKNOWN_RANK,
                "ceiling: unknown path key -> SKIP (never guessed, never mutated)");
        check(CeilingPlan.oneDown(RankFloor.Rank.MASTER) == RankFloor.Rank.APPRENTICE
                && CeilingPlan.oneDown(RankFloor.Rank.STUDENT) == RankFloor.Rank.BEGINNER
                && CeilingPlan.oneDown(RankFloor.Rank.BEGINNER) == null,
                "ceiling: oneDown chain ends at beginner");
        check(RankFloor.stepsTo(RankFloor.Rank.BEGINNER, RankFloor.Rank.APPRENTICE)
                        .equals(List.of("student", "apprentice"))
                && RankFloor.stepsTo(RankFloor.Rank.MASTER, RankFloor.Rank.MASTER).isEmpty(),
                "ceiling: stepsTo arbitrary target");
    }

    private static CreditPolicy.Observation obs(String key, boolean success, boolean base,
            boolean heal, boolean targeted, boolean correlated, boolean playerTarget,
            int daily, int pair) {
        return new CreditPolicy.Observation(key, success, base, heal, targeted,
                correlated, playerTarget, daily, pair);
    }

    private static void testCreditPolicy() {
        CreditPolicy p = CreditPolicy.defaults();
        // Family normalization mirrors SpellKey.getBaseKey().
        check(CreditPolicy.familyOf("mend|4").equals("mend")
                && CreditPolicy.familyOf("stormshackle").equals("stormshackle")
                && CreditPolicy.familyOf(" Heal|2 ").equals("heal")
                && CreditPolicy.familyOf(null).isEmpty(),
                "credit: tier key -> base family normalization");
        // Every DESIGN.md S3 no-credit family classifies NO_CREDIT.
        List<String> noCredit = new ArrayList<>();
        noCredit.addAll(CreditPolicy.DEFAULT_NO_CREDIT_MOVEMENT);
        noCredit.addAll(CreditPolicy.DEFAULT_NO_CREDIT_PASSIVE);
        noCredit.addAll(CreditPolicy.DEFAULT_NO_CREDIT_UTILITY);
        boolean allExcluded = true;
        for (String f : noCredit) {
            if (p.modeFor(f) != CreditPolicy.GateMode.NO_CREDIT) {
                allExcluded = false;
                System.out.println("  not excluded: " + f);
            }
        }
        check(allExcluded, "credit: all DESIGN S3 no-credit families excluded ("
                + noCredit.size() + " listed)");
        check(p.modeFor("fling") == CreditPolicy.GateMode.NO_CREDIT,
                "credit: fling excluded at any tier (the type specimen)");
        // Heal-gate set, feed included (the placement swing, DESIGN S3).
        boolean healOk = true;
        for (String f : CreditPolicy.DEFAULT_HEAL_GATE) {
            if (p.modeFor(f) != CreditPolicy.GateMode.HEAL_GATE) {
                healOk = false;
            }
        }
        check(healOk && p.modeFor("feed") == CreditPolicy.GateMode.HEAL_GATE,
                "credit: heal-gate set incl. feed CREDITED (Marvin placement swing)");
        check(p.modeFor("stormshackle") == CreditPolicy.GateMode.DAMAGE_STRICT,
                "credit: stormshackle damage-gated (actions, not its utility category)");
        check(p.modeFor("arrow") == CreditPolicy.GateMode.DAMAGE_CORRELATED
                && p.modeFor("missile") == CreditPolicy.GateMode.DAMAGE_CORRELATED,
                "credit: projectile families correlated-gated");
        check(p.modeFor("push") == CreditPolicy.GateMode.TARGET_GATE
                && p.modeFor("dash") == CreditPolicy.GateMode.DAMAGE_STRICT,
                "credit: uncertain-table defaults (push target-gate, dash damage-gate)");
        check(p.modeFor("wolf") == CreditPolicy.GateMode.SUCCESS_ONLY
                && p.modeFor("boon") == CreditPolicy.GateMode.SUCCESS_ONLY
                && p.modeFor("ward") == CreditPolicy.GateMode.SUCCESS_ONLY,
                "credit: summons/buffs degrade to success-only + daily cap by design");
        // The decision chain.
        check(p.decide(obs("fling|4", true, true, false, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_EXCLUDED_FAMILY,
                "credit: fling|4 successful cast -> DENIED (excluded family)");
        check(p.decide(obs("mend|3", false, true, true, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_NOT_SUCCESS,
                "credit: fizzled heal -> DENIED (not success)");
        check(p.decide(obs("mend|3", true, false, true, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_SUBCONTEXT,
                "credit: Repeat sub-context echo -> DENIED (count once per real cast)");
        check(p.decide(obs("mend|3", true, true, false, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_HEAL_NOT_OBSERVED,
                "credit: heal on a full-health target -> DENIED (no regain observed)");
        check(p.decide(obs("mend|3", true, true, true, false, false, false, 0, 0))
                == CreditPolicy.Decision.CREDIT,
                "credit: heal that landed -> CREDIT");
        check(p.decide(obs("stormshackle", true, true, false, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_NO_TARGET,
                "credit: damage into a wall -> DENIED (no targeted entity)");
        check(p.decide(obs("stormshackle", true, true, false, true, false, false, 0, 0))
                == CreditPolicy.Decision.CREDIT,
                "credit: damage on a mob -> CREDIT");
        check(p.decide(obs("arrow|6", true, true, false, false, true, false, 0, 0))
                == CreditPolicy.Decision.CREDIT,
                "credit: projectile with correlated hit -> CREDIT");
        check(p.decide(obs("arrow|6", true, true, false, false, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_NO_TARGET,
                "credit: projectile with no hit evidence -> DENIED");
        // Daily cap boundary: 49 credits, 50th credits, 51st denied.
        check(p.decide(obs("boon|2", true, true, false, false, false, false, 49, 0))
                == CreditPolicy.Decision.CREDIT,
                "credit: daily count 49 -> the 50th cast CREDITs");
        check(p.decide(obs("boon|2", true, true, false, false, false, false, 50, 0))
                == CreditPolicy.Decision.DENIED_DAILY_CAP,
                "credit: daily count 50 -> DENIED (cap reached)");
        check(p.decide(obs("boon|2", true, true, false, false, false, false, 51, 0))
                == CreditPolicy.Decision.DENIED_DAILY_CAP,
                "credit: daily count 51 -> DENIED");
        // Pair window: player targets bounded, mob targets never.
        check(p.decide(obs("frost|2", true, true, false, true, false, true, 0, 4))
                == CreditPolicy.Decision.CREDIT,
                "credit: 5th pair credit inside window -> CREDIT");
        check(p.decide(obs("frost|2", true, true, false, true, false, true, 0, 5))
                == CreditPolicy.Decision.DENIED_PAIR_WINDOW,
                "credit: 6th pair credit inside window -> DENIED (hit-trade bound)");
        check(p.decide(obs("frost|2", true, true, false, true, false, false, 0, 99))
                == CreditPolicy.Decision.CREDIT,
                "credit: mob targets are never pair-windowed (farming mobs is playing)");
    }

    private static void testEarnedStore() {
        java.time.LocalDate today = java.time.LocalDate.of(2026, 11, 10);
        EarnedStore s = new EarnedStore();
        check(!s.placementDone() && s.playersTracked() == 0 && s.creditedTotal("x") == 0,
                "store: fresh store is empty, placement not done");
        check(s.seedPlacement("u1", 1210) && !s.seedPlacement("u1", 5)
                && s.creditedTotal("u1") == 1210,
                "store: placement seeds once and never overwrites");
        EarnedStore.Row row = s.rowForCredit("u1");
        row.addCredit("mend", today);
        row.addCredit("mend", today);
        row.addCredit("mend|ignored", today); // family keys arrive normalized; bucket is by family string as given
        check(s.creditedTotal("u1") == 1213 && row.dailyCount("mend", today) == 2,
                "store: credits accrue to total + today's family bucket");
        check(row.dailyCount("mend", today.plusDays(1)) == 0,
                "store: a stale bucket reads as 0 (Chicago day roll)");
        check(row.rollDay(today.plusDays(1)) && row.dailyCount("mend", today.plusDays(1)) == 0
                && s.creditedTotal("u1") == 1213,
                "store: day roll clears the bucket, never the lifetime total");
        s.markPlacementDone("2026-11-10", "mage-pull-fixture");
        EarnedStore back = EarnedStore.fromMap(s.toMap());
        check(back.placementDone() && "mage-pull-fixture".equals(back.placementSource())
                && back.creditedTotal("u1") == 1213
                && back.row("u1").placedTotal() == 1210,
                "store: codec round-trip preserves placement + totals + placed component");
        EarnedStore junk = EarnedStore.fromMap(Map.of(
                "players", Map.of("u9", Map.of("credited_total", "lots", "daily", "junk")),
                "placement", "not-a-map"));
        check(junk.creditedTotal("u9") == 0 && !junk.placementDone(),
                "store: garbage rows fall back to defaults (tolerant load)");
        check(s.seedFallback("u2", 42) && !s.seedFallback("u2", 1)
                && s.row("u2").placedTotal() == 0 && s.creditedTotal("u2") == 42,
                "store: fallback seed is marked as not-a-placement (placed_total 0)");
    }

    /**
     * Spell-logic gate for the CREDIT system (house rule — "what
     * happens if a player holds the button down forever?"), run as
     * deterministic simulations over CreditPolicy + EarnedStore. No
     * spells.yml is touched by 0.3.0, so the spell-audit harness is
     * not re-run; these simulations are the logic tests for the new
     * surface the packet creates (DESIGN.md S9).
     */
    private static void testCreditLogicSimulations() {
        CreditPolicy p = CreditPolicy.defaults();
        java.time.LocalDate today = java.time.LocalDate.of(2026, 11, 10);

        // Fling held forever -> 0 credit, at every tier.
        int credits = 0;
        for (int i = 0; i < 10000; i++) {
            if (p.decide(obs("fling|4", true, true, false, false, false, false, 0, 0))
                    == CreditPolicy.Decision.CREDIT) {
                credits++;
            }
        }
        check(credits == 0, "logic: fling held forever -> 0 credit (excluded family)");

        // Mend held forever on a FULL-health target -> 0 (heal gate).
        credits = 0;
        for (int i = 0; i < 1000; i++) {
            if (p.decide(obs("mend|4", true, true, false, false, false, false, 0, 0))
                    == CreditPolicy.Decision.CREDIT) {
                credits++;
            }
        }
        check(credits == 0, "logic: mend on full-health target forever -> 0 credit (heal gate)");

        // Mend on a hurt target -> credits until the family cap, then 0.
        EarnedStore store = new EarnedStore();
        EarnedStore.Row r = store.rowForCredit("sim");
        credits = 0;
        for (int i = 0; i < 200; i++) {
            CreditPolicy.Decision d = p.decide(obs("mend|4", true, true, true,
                    false, false, false, r.dailyCount("mend", today), 0));
            if (d == CreditPolicy.Decision.CREDIT) {
                credits++;
                r.addCredit("mend", today);
            }
        }
        check(credits == 50 && store.creditedTotal("sim") == 50,
                "logic: mend spam on hurt target -> exactly 50 credits/day (family cap)");

        // Damage held forever into a wall/sky -> 0; at a penned mob -> cap.
        credits = 0;
        for (int i = 0; i < 1000; i++) {
            if (p.decide(obs("missile|3", true, true, false, false, false, false, 0, 0))
                    == CreditPolicy.Decision.CREDIT) {
                credits++;
            }
        }
        check(credits == 0, "logic: damage into a wall forever -> 0 credit (no target)");
        EarnedStore.Row r2 = store.rowForCredit("sim2");
        credits = 0;
        for (int i = 0; i < 200; i++) {
            CreditPolicy.Decision d = p.decide(obs("missile|3", true, true, false,
                    true, false, false, r2.dailyCount("missile", today), 0));
            if (d == CreditPolicy.Decision.CREDIT) {
                credits++;
                r2.addCredit("missile", today);
            }
        }
        check(credits == 50, "logic: damage at a penned mob -> exactly 50 credits/day (family cap)");

        // Two-account hit-trade: pair window bounds the loop yield.
        int pairCredits = 0;
        credits = 0;
        for (int i = 0; i < 200; i++) {
            CreditPolicy.Decision d = p.decide(obs("frost|2", true, true, false,
                    true, false, true, 0, pairCredits));
            if (d == CreditPolicy.Decision.CREDIT) {
                credits++;
                pairCredits++;
            }
        }
        check(credits == CreditPolicy.DEFAULT_PAIR_WINDOW_MAX,
                "logic: hit-trade loop -> pair window stops yield at K="
                        + CreditPolicy.DEFAULT_PAIR_WINDOW_MAX + " per window");

        // Buff spam (boon) -> success-only, bounded by the family cap.
        EarnedStore.Row r3 = store.rowForCredit("sim3");
        credits = 0;
        for (int i = 0; i < 200; i++) {
            CreditPolicy.Decision d = p.decide(obs("boon|3", true, true, false,
                    false, false, false, r3.dailyCount("boon", today), 0));
            if (d == CreditPolicy.Decision.CREDIT) {
                credits++;
                r3.addCredit("boon", today);
            }
        }
        check(credits == 50, "logic: boon spam -> exactly 50 credits/day (cap is the bound)");

        // Passive procs are not casts: critical on every hit -> 0.
        check(p.decide(obs("critical", true, true, false, true, false, false, 0, 0))
                == CreditPolicy.Decision.DENIED_EXCLUDED_FAMILY,
                "logic: passive proc (critical) -> 0 credit by family");
    }

    /** The shipped config.yml IS the classification: build a policy
     *  from it and assert it agrees with the packet defaults on every
     *  classified family (DESIGN.md S3: classification ships as config). */
    private static void testConfigClassification(Path projectDir) throws Exception {
        Path configFile = projectDir.resolve("config.yml");
        check(Files.isRegularFile(configFile), "config: shipped config.yml exists");
        Map<String, Object> root;
        try (var in = Files.newInputStream(configFile)) {
            root = new org.yaml.snakeyaml.Yaml().load(in);
        }
        check(root != null && root.get("earning") instanceof Map, "config: earning section present");
        @SuppressWarnings("unchecked")
        Map<String, Object> earning = (Map<String, Object>) root.get("earning");
        check(Boolean.TRUE.equals(earning.get("enabled")), "config: earning enabled by default");
        check(Boolean.TRUE.equals(earning.get("demotion-enabled")),
                "config: demotion-enabled true (ships only with the rig demotion proof)");
        @SuppressWarnings("unchecked")
        Map<String, Object> th = (Map<String, Object>) earning.get("thresholds");
        check(((Number) th.get("student")).longValue() == 100
                && ((Number) th.get("apprentice")).longValue() == 400
                && ((Number) th.get("master")).longValue() == 2000,
                "config: locked thresholds 100/400/2000 in shipped config");
        check(((Number) earning.get("daily-cap-per-family")).intValue() == 50,
                "config: daily cap 50 per family in shipped config");
        CreditPolicy fromConfig = policyFromConfigMap(earning);
        CreditPolicy defaults = CreditPolicy.defaults();
        List<String> probes = new ArrayList<>();
        probes.addAll(CreditPolicy.DEFAULT_NO_CREDIT_MOVEMENT);
        probes.addAll(CreditPolicy.DEFAULT_NO_CREDIT_PASSIVE);
        probes.addAll(CreditPolicy.DEFAULT_NO_CREDIT_UTILITY);
        probes.addAll(CreditPolicy.DEFAULT_HEAL_GATE);
        probes.addAll(CreditPolicy.DEFAULT_DAMAGE_STRICT);
        probes.addAll(CreditPolicy.DEFAULT_DAMAGE_CORRELATED);
        probes.addAll(CreditPolicy.DEFAULT_TARGET_GATE);
        probes.addAll(List.of("wolf", "boon", "ward", "cure", "resurrection",
                "extinguish", "bubble", "invincible", "shell", "shrink",
                "tendril", "counterspell", "tame", "gills", "piggyback"));
        boolean agree = true;
        for (String f : probes) {
            if (fromConfig.modeFor(f) != defaults.modeFor(f)) {
                agree = false;
                System.out.println("  config/default mode mismatch: " + f
                        + " config=" + fromConfig.modeFor(f) + " default=" + defaults.modeFor(f));
            }
        }
        check(agree, "config: shipped classification agrees with packet defaults on "
                + probes.size() + " probe families");
        // Amendment: the shipped supplemental-grants map must equal
        // the code fallback (WandInventorySync.DEFAULT_SUPPLEMENTAL).
        Object invRaw = earning.get("inventory-reconcile");
        check(invRaw instanceof Map, "config: inventory-reconcile section present (amendment)");
        if (invRaw instanceof Map<?, ?> invSection) {
            check(Boolean.TRUE.equals(invSection.get("enabled")),
                    "config: inventory-reconcile enabled by default (amendment)");
            Object suppRaw = invSection.get("supplemental-grants");
            boolean suppAgree = suppRaw instanceof Map<?, ?>;
            if (suppRaw instanceof Map<?, ?> supp) {
                for (Map.Entry<String, List<String>> e
                        : WandInventorySync.DEFAULT_SUPPLEMENTAL.entrySet()) {
                    Object v = supp.get(e.getKey());
                    java.util.Set<String> suppFromConfig = new java.util.HashSet<>();
                    if (v instanceof List<?> list) {
                        for (Object o : list) {
                            suppFromConfig.add(String.valueOf(o).trim().toLowerCase(java.util.Locale.ROOT));
                        }
                    }
                    if (!suppFromConfig.equals(new java.util.HashSet<>(e.getValue()))) {
                        suppAgree = false;
                        System.out.println("  supplemental mismatch at " + e.getKey()
                                + ": config=" + suppFromConfig + " default=" + e.getValue());
                    }
                }
            }
            check(suppAgree, "config: supplemental grants agree with the code defaults "
                    + "(summoning tree: archer/bruiser/guardian/revenant/ghast/mount)");
        }
    }

    /** Mirrors ExcelMagicGate.policyFromConfig's list reading, on the
     *  raw SnakeYAML map (kept in the test so the shipped config is
     *  verified without a server). */
    private static CreditPolicy policyFromConfigMap(Map<String, Object> earning) {
        java.util.function.Function<String, java.util.Set<String>> set = key -> {
            Object v = earning.get(key);
            java.util.Set<String> out = new java.util.LinkedHashSet<>();
            if (v instanceof List<?> list) {
                for (Object o : list) {
                    out.add(String.valueOf(o).trim().toLowerCase(java.util.Locale.ROOT));
                }
            }
            return out;
        };
        int cap = earning.get("daily-cap-per-family") instanceof Number n ? n.intValue() : 50;
        int pair = earning.get("pair-window-max") instanceof Number n ? n.intValue() : 5;
        return new CreditPolicy(set.apply("no-credit-families"), set.apply("heal-gate-families"),
                set.apply("damage-strict-families"), set.apply("damage-correlated-families"),
                set.apply("target-gate-families"), cap, pair);
    }

    /**
     * The placement regression net (DESIGN.md S8/S9): the 11 Nov 9
     * mage .dat fixtures must reproduce the locked placements under
     * the shipped credited definition — Jester 11,231 Master, Marvin
     * 2,178 Master (the feed swing), Mike 1,210 Apprentice, Kailey 12,
     * Rylie 0, Racimbio 11, everyone else 0.
     */
    private static void testPlacementFixtures(Path fixturesDir) throws Exception {
        check(Files.isDirectory(fixturesDir), "placement: fixtures dir exists");
        Map<String, long[]> expected = Map.of(
                "e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e", new long[]{17821, 11231},
                "8399c623-786f-448c-8b11-f54504721819", new long[]{3734, 2178},
                "00000000-0000-0000-0009-01feb74ea7be", new long[]{1232, 1210},
                "00000000-0000-0000-0009-01f40ccd59de", new long[]{12, 12},
                "00000000-0000-0000-0009-01f05d0d739d", new long[]{0, 0},
                "aad1b617-f5db-4357-acff-b5a5204a46d2", new long[]{11, 11});
        CreditPolicy policy = CreditPolicy.defaults();
        EarnedRank.Thresholds t = EarnedRank.Thresholds.locked();
        try (var stream = Files.list(fixturesDir)) {
            List<Path> files = stream.filter(f -> f.getFileName().toString().endsWith(".dat"))
                    .sorted().toList();
            check(files.size() == 11, "placement: 11 fixture files (got " + files.size() + ")");
            for (Path f : files) {
                String uuid = f.getFileName().toString().replace(".dat", "");
                Map<String, Object> dat;
                try (var in = Files.newInputStream(f)) {
                    dat = new org.yaml.snakeyaml.Yaml().load(in);
                }
                @SuppressWarnings("unchecked")
                Map<String, Object> spells = dat == null ? Map.of()
                        : (Map<String, Object>) dat.getOrDefault("spells", Map.of());
                long raw = PlacementCalculator.rawTotal(spells);
                long credited = PlacementCalculator.creditedTotal(spells, policy);
                long[] exp = expected.get(uuid);
                if (exp != null) {
                    check(raw == exp[0] && credited == exp[1],
                            "placement: " + uuid + " raw=" + raw + " credited=" + credited
                                    + " (expected " + exp[0] + "/" + exp[1] + ") -> "
                                    + EarnedRank.rankFor(credited, t).key());
                } else {
                    check(raw == 0 && credited == 0,
                            "placement: " + uuid + " is a zero-cast record");
                }
            }
        }
        // The locked placements, stated as ranks.
        check(EarnedRank.rankFor(11231, t) == RankFloor.Rank.MASTER
                && EarnedRank.rankFor(2178, t) == RankFloor.Rank.MASTER
                && EarnedRank.rankFor(1210, t) == RankFloor.Rank.APPRENTICE
                && EarnedRank.rankFor(12, t) == RankFloor.Rank.BEGINNER,
                "placement: locked placements hold (Jester+Marvin Master, Mike Apprentice, Kailey below Student)");
    }

    // ----------------------------------------------------------------
    // 0.3.0 amendment: wand spell-inventory reconcile (D2)
    // ----------------------------------------------------------------

    private static void testWandInventorySync() {
        Map<String, List<String>> supp = WandInventorySync.DEFAULT_SUPPLEMENTAL;
        List<String> pathBeginner = List.of("heal", "boon");
        List<String> extraBeginner = List.of("aqualung");

        // Key normalization mirrors SpellKey base-key semantics.
        check(WandInventorySync.baseKey("Mend|4").equals("mend")
                && WandInventorySync.baseKey(" tame ").equals("tame")
                && WandInventorySync.baseKey(null).isEmpty()
                && WandInventorySync.baseKey("archer").equals("archer"),
                "invsync: tier key -> base key normalization");

        // The expected set accumulates path + extra + supplemental
        // upward, and NEVER above the target rank.
        java.util.Set<String> expB = WandInventorySync.expectedFor(
                RankFloor.Rank.BEGINNER, pathBeginner, extraBeginner, supp);
        check(expB.contains("heal") && expB.contains("aqualung") && !expB.contains("archer"),
                "invsync: beginner target = beginner path + extras, no supplemental yet");
        java.util.Set<String> expS = WandInventorySync.expectedFor(
                RankFloor.Rank.STUDENT, pathBeginner, extraBeginner, supp);
        check(expS.contains("archer") && !expS.contains("bruiser")
                && !expS.contains("guardian"),
                "invsync: student target gains archer only (tree root, DESIGN S2.2)");
        java.util.Set<String> expA = WandInventorySync.expectedFor(
                RankFloor.Rank.APPRENTICE, pathBeginner, extraBeginner, supp);
        check(expA.contains("archer") && expA.contains("bruiser")
                && !expA.contains("guardian") && !expA.contains("ghast"),
                "invsync: apprentice target gains archer + bruiser, no master tier");
        java.util.Set<String> expM = WandInventorySync.expectedFor(
                RankFloor.Rank.MASTER, pathBeginner, extraBeginner, supp);
        check(expM.containsAll(List.of("archer", "bruiser", "guardian",
                        "revenant", "ghast", "mount")),
                "invsync: master target gains the full summoning tree (six spells)");

        // (a) A wand missing a current-rank path spell gets it granted.
        List<String> missing = WandInventorySync.missingFor(expM,
                List.of("heal", "boon", "aqualung"));
        check(missing.equals(List.of("archer", "bruiser", "ghast",
                        "guardian", "mount", "revenant")),
                "invsync: wand missing path spells -> exactly the missing set, sorted");
        // A held TIER of a family counts as holding the spell.
        check(WandInventorySync.missingFor(expM,
                        List.of("heal", "boon", "aqualung", "archer|2", "bruiser",
                                "guardian", "revenant", "ghast", "mount")).isEmpty(),
                "invsync: held tier key (archer|2) counts as holding the base spell");
        // (b) A spell above the player's rank is NOT granted: it is
        // not in the target's expected set, so it cannot be missing.
        List<String> missingAppr = WandInventorySync.missingFor(expA,
                List.of("heal", "boon", "aqualung"));
        check(missingAppr.equals(List.of("archer", "bruiser"))
                && !missingAppr.contains("guardian") && !missingAppr.contains("tame"),
                "invsync: above-rank spells are never in the grant set");
        // (c) Idempotence: after granting the missing set, a second
        // sweep grants nothing.
        java.util.Set<String> after = new java.util.HashSet<>(
                List.of("heal", "boon", "aqualung"));
        after.addAll(missing);
        check(WandInventorySync.missingFor(expM, after).isEmpty(),
                "invsync: second sweep changes nothing (idempotent)");

        // Slots: grants take max+1 upward in sorted order (the
        // tame-fix packet's placement rule); empty wand starts at 0;
        // existing slots are never renumbered or reused.
        Map<String, Integer> slots = WandInventorySync.slotsFor(
                List.of("archer", "tame"), Map.of("heal", 1, "greaterheal", 79));
        check(slots.get("archer") == 80 && slots.get("tame") == 81,
                "invsync: grant slots continue after the current max (79 -> 80, 81)");
        check(WandInventorySync.slotsFor(List.of("heal"), Map.of()).get("heal") == 0,
                "invsync: empty wand grants start at slot 0");
    }

    /** Cumulative path spell sets for one stage packet, read from
     *  the REAL paths.yml (spells + extra_spells, inherit chain
     *  beginner -> student -> apprentice -> master, exactly the
     *  accumulation Magic's WandUpgradePath.getSpells performs). */
    @SuppressWarnings("unchecked")
    private static Map<String, java.util.Set<String>> stagePathSets(Path stagesDir, int stage)
            throws Exception {
        Map<String, Object> root;
        try (var in = Files.newInputStream(
                stagesDir.resolve("stage" + stage).resolve("paths.yml"))) {
            root = new org.yaml.snakeyaml.Yaml().load(in);
        }
        Map<String, java.util.Set<String>> out = new java.util.LinkedHashMap<>();
        java.util.Set<String> acc = new java.util.LinkedHashSet<>();
        for (String rank : List.of("beginner", "student", "apprentice", "master")) {
            Map<String, Object> block = (Map<String, Object>) root.get(rank);
            if (block != null) {
                for (String key : List.of("spells", "extra_spells")) {
                    Object v = block.get(key);
                    if (v instanceof List<?> list) {
                        for (Object o : list) {
                            acc.add(WandInventorySync.baseKey(String.valueOf(o)));
                        }
                    }
                }
            }
            out.put(rank, new java.util.LinkedHashSet<>(acc));
        }
        return out;
    }

    /** One fixture wand's parsed spell_inventory (base keys), read
     *  from the mage .dat YAML (wands -> <template> -> wand ->
     *  spell_inventory) — the parsed representation of the two the
     *  tame-fix packet names. */
    @SuppressWarnings("unchecked")
    private static Map<String, Integer> fixtureWandInventory(Path file, String template)
            throws Exception {
        Map<String, Object> dat;
        try (var in = Files.newInputStream(file)) {
            dat = new org.yaml.snakeyaml.Yaml().load(in);
        }
        Map<String, Object> wands = (Map<String, Object>) dat.get("wands");
        Map<String, Object> entry = (Map<String, Object>) wands.get(template);
        Map<String, Object> wand = (Map<String, Object>) entry.get("wand");
        Map<String, Object> inv = (Map<String, Object>) wand.get("spell_inventory");
        Map<String, Integer> out = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, Object> e : inv.entrySet()) {
            out.put(e.getKey(), ((Number) e.getValue()).intValue());
        }
        return out;
    }

    /**
     * The placement/switch-over grant pass over the REAL Nov 9
     * fixtures (DESIGN: the first reconcile after install fills each
     * placed player's wand to their PLACED rank). Expected sets use
     * the live stage's (stage 4) path lists + the supplemental
     * summoning tree; expectations below were computed from these
     * fixtures, not assumed.
     */
    private static void testInventoryFixtures(Path stagesDir, Path fixturesDir) throws Exception {
        Map<String, java.util.Set<String>> stage4 = stagePathSets(stagesDir, 4);
        java.util.function.BiFunction<String, java.util.Set<String>, java.util.Set<String>> expected =
                (rankKey, pathSet) -> WandInventorySync.expectedFor(
                        RankFloor.fromKey(rankKey), pathSet, List.of(),
                        WandInventorySync.DEFAULT_SUPPLEMENTAL);
        // NOTE: stagePathSets already folds extra_spells into the
        // cumulative set, so extras are passed empty here.

        Path jesterFile = fixturesDir.resolve("e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e.dat");
        Path marvinFile = fixturesDir.resolve("8399c623-786f-448c-8b11-f54504721819.dat");
        Path mikeFile = fixturesDir.resolve("00000000-0000-0000-0009-01feb74ea7be.dat");

        // Jester, placed Master, master wand: the six summoning-tree
        // spells + disposal (a path spell his wand somehow lacks)
        // are missing; tame is already his and must NOT re-grant.
        Map<String, Integer> jesterInv = fixtureWandInventory(jesterFile, "master");
        List<String> jesterMissing = WandInventorySync.missingFor(
                expected.apply("master", stage4.get("master")), jesterInv.keySet());
        check(jesterMissing.equals(List.of("archer", "bruiser", "disposal",
                        "ghast", "guardian", "mount", "revenant")),
                "invsync-fixture: Jester (Master) master wand gains the six tree spells "
                        + "+ disposal (got " + jesterMissing + ")");
        check(!jesterMissing.contains("tame"),
                "invsync-fixture: Jester already holds tame -> not re-granted");

        // Marvin, placed Master (credited 2,178) on his APPRENTICE
        // wand: the placement pass fills him to Master — tame and
        // the full tree land, at slots after his current max (79).
        Map<String, Integer> marvinInv = fixtureWandInventory(marvinFile, "apprentice");
        List<String> marvinMissing = WandInventorySync.missingFor(
                expected.apply("master", stage4.get("master")), marvinInv.keySet());
        check(marvinMissing.equals(List.of("archer", "blessing", "blink", "bruiser",
                        "chainlightning", "disposal", "ghast", "guardian", "lightning",
                        "mount", "pollinate", "recall_death", "revenant", "tame")),
                "invsync-fixture: Marvin (placed Master) apprentice wand fills to Master "
                        + "incl. tame + the six (got " + marvinMissing + ")");
        Map<String, Integer> marvinSlots = WandInventorySync.slotsFor(marvinMissing, marvinInv);
        check(marvinSlots.get("archer") == 80 && marvinSlots.get("tame") == 93
                && marvinSlots.size() == 14,
                "invsync-fixture: Marvin's grants take slots 80..93 after his max 79 "
                        + "(tame at 93 in sorted order)");
        // The same wand at an APPRENTICE target (the negative the
        // task names): master-only tame and the master tree tier
        // are NOT granted beyond the placed rank's path lists.
        List<String> marvinApprMissing = WandInventorySync.missingFor(
                expected.apply("apprentice", stage4.get("apprentice")), marvinInv.keySet());
        check(marvinApprMissing.equals(List.of("archer", "bruiser", "disposal"))
                && !marvinApprMissing.contains("tame")
                && !marvinApprMissing.contains("guardian")
                && !marvinApprMissing.contains("ghast"),
                "invsync-fixture: Marvin's wand at APPRENTICE target gains archer/bruiser/"
                        + "disposal only — no tame, no master tree tier");

        // Mike, placed Apprentice (credited 1,210), student wand:
        // fills to Apprentice (archer + bruiser land); tame and the
        // master tier stay out.
        Map<String, Integer> mikeInv = fixtureWandInventory(mikeFile, "student");
        List<String> mikeMissing = WandInventorySync.missingFor(
                expected.apply("apprentice", stage4.get("apprentice")), mikeInv.keySet());
        check(mikeMissing.equals(List.of("archer", "bruiser", "disposal", "gather",
                        "glide", "growth", "hat", "lift", "recall", "smite", "stash",
                        "superheal", "wave")),
                "invsync-fixture: Mike (placed Apprentice) student wand fills to Apprentice "
                        + "(got " + mikeMissing + ")");
        check(!mikeMissing.contains("tame") && !mikeMissing.contains("guardian"),
                "invsync-fixture: Mike's Apprentice fill grants no master spells");

        // Idempotence over every fixture case: granting the missing
        // set leaves a second sweep with nothing to do.
        boolean idem = true;
        for (var kase : List.of(
                Map.entry(jesterInv, jesterMissing), Map.entry(marvinInv, marvinMissing),
                Map.entry(mikeInv, mikeMissing))) {
            java.util.Set<String> held = new java.util.HashSet<>(kase.getKey().keySet());
            held.addAll(kase.getValue());
            // Recompute against each case's own target by trying all
            // three targets: none may report further missing beyond
            // what its own grant covered — checked per-case below.
            if (held.size() != kase.getKey().size() + kase.getValue().size()) {
                idem = false;
            }
        }
        check(idem, "invsync-fixture: grant sets are disjoint from held sets (no re-grants)");
        java.util.Set<String> marvinAfter = new java.util.HashSet<>(marvinInv.keySet());
        marvinAfter.addAll(marvinMissing);
        check(WandInventorySync.missingFor(
                        expected.apply("master", stage4.get("master")), marvinAfter).isEmpty(),
                "invsync-fixture: Marvin after the placement pass -> second sweep grants nothing");
    }
}
