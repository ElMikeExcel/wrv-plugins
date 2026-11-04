package com.winkreyes.excelguildgate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Plain main-method test harness for RaidWinnerCheck (TestPlayerA's raid
 * floors: Oct 19, 2026, a raid credits only when at least one
 * player stands among its winners; Oct 20, 2026, the victory must
 * also come under the strongest omen, Bad Omen V. Same style as
 * the other ExcelGuildGate harnesses: no test framework, every
 * check counted, any failure prints and exits the JVM with
 * status 1.)
 *
 * The check is type-agnostic by design - the adapter passes the
 * RaidFinishEvent's List&lt;Player&gt;, the harness passes stand-in
 * objects, and the semantics under test (present / absent / null)
 * are identical either way.
 *
 * Run via run-tests.sh.
 */
public final class RaidWinnerCheckTest {

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
        if (!java.util.Objects.equals(expected, actual)) {
            failures++;
            System.out.println("FAIL: " + name + " expected=" + expected + " actual=" + actual);
        }
    }

    /** A stand-in for a winner entry (the real list holds Players). */
    private static Object winner() {
        return new Object();
    }

    public static void main(String[] args) {
        creditCases();
        denialCases();
        nullSafety();
        deniedThenHonest();
        omenMatrix();
        deniedThenHonestOmen();
        System.out.println("RaidWinnerCheckTest: " + (checks - failures) + "/" + checks
                + " checks passed" + (failures == 0 ? " - ALL PASS" : " - " + failures + " FAILED"));
        if (failures > 0) {
            System.exit(1);
        }
    }

    private static void creditCases() {
        check("one player among the winners counts",
                RaidWinnerCheck.anyPlayerWinner(List.of(winner())));
        checkEq("one winner: decision is CREDIT",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner())));
        check("a full winners list counts",
                RaidWinnerCheck.anyPlayerWinner(List.of(winner(), winner(), winner())));
        checkEq("several winners: decision is CREDIT",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner(), winner())));
        check("the decision is stable across repeat reads of the same list",
                RaidWinnerCheck.decide(List.of(winner()))
                        == RaidWinnerCheck.decide(List.of(winner())));
    }

    private static void denialCases() {
        check("an empty winners list does not count (golem / trap / AFK clear)",
                !RaidWinnerCheck.anyPlayerWinner(List.of()));
        checkEq("empty winners: decision is NO_PLAYER_WINNER",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(List.of()));
        check("an ArrayList emptied after the fact does not count", !RaidWinnerCheck
                .anyPlayerWinner(new ArrayList<>()));
    }

    private static void nullSafety() {
        check("a null winners list does not count and does not throw",
                !RaidWinnerCheck.anyPlayerWinner(null));
        checkEq("null winners: decision is NO_PLAYER_WINNER",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(null));
        check("a list of only nulls does not count",
                !RaidWinnerCheck.anyPlayerWinner(Arrays.asList(null, null)));
        check("a null entry does not hide a real winner behind it",
                RaidWinnerCheck.anyPlayerWinner(Arrays.asList(null, winner())));
        checkEq("mixed null + winner: decision is CREDIT",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(Arrays.asList(null, winner())));
    }

    private static void deniedThenHonest() {
        // The denial is stateless - it records nothing, starts no
        // cooldown, and leaves the next raid to be judged on its
        // own winners. A denied farm clear followed by an honest
        // win must credit on the second raid.
        checkEq("first raid (no winners) is denied",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(List.of()));
        checkEq("second raid (a winner) still credits after the denial",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner())));
        checkEq("and a third no-winner raid is denied again - no state leaks either way",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(List.of()));
    }

    private static void omenMatrix() {
        // TestPlayerA's omen floor (Oct 20, 2026): the seal answers only
        // to the strongest omen. Winner floor judged first, so the
        // cross-cases must land on the TRUE reason.
        int req = RaidWinnerCheck.REQUIRED_OMEN_LEVEL;
        checkEq("the shipped requirement is the vanilla cap, Bad Omen V",
                5, RaidWinnerCheck.REQUIRED_OMEN_LEVEL);
        for (int omen = 1; omen < req; omen++) {
            checkEq("omen " + omen + " with a player winner: OMEN_TOO_LOW",
                    RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                    RaidWinnerCheck.decide(List.of(winner()), omen, req));
        }
        checkEq("omen V with a player winner: CREDIT",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner()), req, req));
        checkEq("omen V with NO winner: still NO_PLAYER_WINNER (the winner floor governs)",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(List.of(), req, req));
        checkEq("omen V with a null winners list: NO_PLAYER_WINNER, no throw",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(null, req, req));
        checkEq("omen I with NO winner: NO_PLAYER_WINNER, not OMEN_TOO_LOW (winner floor first)",
                RaidWinnerCheck.Decision.NO_PLAYER_WINNER,
                RaidWinnerCheck.decide(List.of(), 1, req));
        checkEq("a read above the requirement credits (the core never denies a stronger omen)",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner()), req + 1, req));
        checkEq("omen 0 with a winner: OMEN_TOO_LOW (never credit on a thin read)",
                RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                RaidWinnerCheck.decide(List.of(winner()), 0, req));
        checkEq("a negative omen read with a winner: OMEN_TOO_LOW",
                RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                RaidWinnerCheck.decide(List.of(winner()), -1, req));
        checkEq("config override: required 3, omen III with a winner credits",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner()), 3, 3));
        checkEq("config override: required 3, omen II with a winner is OMEN_TOO_LOW",
                RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                RaidWinnerCheck.decide(List.of(winner()), 2, 3));
    }

    private static void deniedThenHonestOmen() {
        // The omen denial is as stateless as the winner denial:
        // a low-omen win spends nothing, and the Bad Omen V raid
        // that follows is judged on its own omen alone.
        int req = RaidWinnerCheck.REQUIRED_OMEN_LEVEL;
        checkEq("first raid (winner, omen I) is denied on the omen",
                RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                RaidWinnerCheck.decide(List.of(winner()), 1, req));
        checkEq("second raid (winner, omen V) still credits after the denial",
                RaidWinnerCheck.Decision.CREDIT,
                RaidWinnerCheck.decide(List.of(winner()), req, req));
        checkEq("and a later omen II raid is denied again - no state leaks either way",
                RaidWinnerCheck.Decision.OMEN_TOO_LOW,
                RaidWinnerCheck.decide(List.of(winner()), 2, req));
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.
