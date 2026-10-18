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
        @Override public void persist() throws Exception { step("persist"); }
        @Override public void announce() throws Exception { step("announce"); }
    }

    private static void testStageApplier() {
        FakeSteps ok = new FakeSteps();
        StageApplier.Result r = StageApplier.apply(ok, null);
        check(r.success(), "applier: all steps pass -> success");
        check(ok.calls.equals(List.of("backup", "swap", "reload", "grants", "persist", "announce")),
                "applier: steps run in contract order");

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
}
