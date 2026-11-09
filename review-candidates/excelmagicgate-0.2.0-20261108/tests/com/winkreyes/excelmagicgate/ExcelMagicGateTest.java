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
        int[] expected = {0, 13, 33, 59, 67, 100};
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
        check(BuildInfo.BUILD_ID.startsWith("EXC-MAGICGATE-0.2.0-WRV-20261108-01-"),
                "floor: build fingerprint present (Layer B)");
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
}
