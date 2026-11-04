/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.dragon;

/**
 * 0.2.11 (B-4): the wipe-on-update decision table. Mike's rule
 * (Oct 30, 2026): a saved fight is wiped when the build changes —
 * closed out and archived, never restored — while same-build state
 * restores exactly as before, and a state with no open battle is
 * never the wipe's business (DONE is a record, not a trial).
 */
public final class StaleTrialPolicyTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // --- isOpen: a live phase with a dragon on record.
        check(StaleTrialPolicy.isOpen(BattlePhase.PHASE1, "u"), "PHASE1 with a dragon is open");
        check(StaleTrialPolicy.isOpen(BattlePhase.PHASE2, "u"), "PHASE2 with a dragon is open");
        check(StaleTrialPolicy.isOpen(BattlePhase.TRANSITION, "u"), "TRANSITION with a dragon is open");
        check(!StaleTrialPolicy.isOpen(BattlePhase.NONE, "u"), "NONE is not an open trial");
        check(!StaleTrialPolicy.isOpen(BattlePhase.DONE, "u"), "DONE is a record, not an open trial");
        check(!StaleTrialPolicy.isOpen(BattlePhase.PHASE1, null), "a phase with no dragon is not open");
        check(!StaleTrialPolicy.isOpen(null, "u"), "no phase is not open");

        // --- onLoad: the version gate.
        check(StaleTrialPolicy.onLoad("0.2.10-marker-fix", "0.2.11", true)
                        == StaleTrialPolicy.Action.CLOSE_STALE,
                "open battle + version change closes stale");
        check(StaleTrialPolicy.onLoad(null, "0.2.11", true)
                        == StaleTrialPolicy.Action.CLOSE_STALE,
                "open battle + ABSENT stamp closes stale (the first stamped boot wipes)");
        check(StaleTrialPolicy.onLoad("0.2.11", "0.2.11", true)
                        == StaleTrialPolicy.Action.RESTORE,
                "open battle + same version restores as today");
        check(StaleTrialPolicy.onLoad("0.2.10-marker-fix", "0.2.11", false)
                        == StaleTrialPolicy.Action.RESTORE,
                "no open battle restores even across versions (DONE/counters untouched)");
        check(StaleTrialPolicy.onLoad(null, "0.2.11", false)
                        == StaleTrialPolicy.Action.RESTORE,
                "no open battle + absent stamp restores");
        check(StaleTrialPolicy.onLoad("0.2.11", "0.2.11", false)
                        == StaleTrialPolicy.Action.RESTORE,
                "same version, no open battle restores");

        // --- Archive naming: beside the state file, never a deletion.
        check(StaleTrialPolicy.archiveName("0.2.10-marker-fix")
                        .equals("state.yml.archived-0.2.10-marker-fix"),
                "archive name carries the wiped build");
        check(StaleTrialPolicy.archiveName(null).equals("state.yml.archived-unstamped"),
                "unstamped state archives as unstamped");
        check(StaleTrialPolicy.archiveName("").equals("state.yml.archived-unstamped"),
                "blank stamp archives as unstamped");
        check(!StaleTrialPolicy.archiveName("a/b c").contains("/")
                        && !StaleTrialPolicy.archiveName("a/b c").contains(" "),
                "archive name is filesystem-safe");

        System.out.println("StaleTrialPolicyTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
