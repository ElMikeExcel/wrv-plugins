/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The auto-present classifier (0.3.0, SPEC 5.1): every skip path
 * of the old silent attemptPresent now has a name, a priority,
 * and a transient/terminal class that drives the retry queue.
 * The Elder-night defect was a skip with none of the three.
 */
public final class PresentOutcomeTest {

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

    private static PresentOutcome of(boolean online, boolean mode, boolean foreign,
                                     boolean magic, boolean unconsumed, boolean presented) {
        return PresentOutcome.classify(new PresentOutcome.Facts(
                online, mode, foreign, magic, unconsumed, presented));
    }

    public static void main(String[] args) {
        check(of(true, true, false, false, true, false) == PresentOutcome.PRESENTED,
                "clean facts: PRESENTED");
        check(of(false, true, false, false, true, false) == PresentOutcome.PLAYER_GONE,
                "offline: PLAYER_GONE");
        check(of(false, false, true, true, false, true) == PresentOutcome.PLAYER_GONE,
                "gone beats every other fact (priority 1)");
        check(of(true, false, true, true, true, false) == PresentOutcome.GAMEMODE,
                "wrong gamemode beats container and magic (priority 2)");
        check(of(true, true, true, true, true, false) == PresentOutcome.FOREIGN_INVENTORY,
                "a foreign container beats the magic state (priority 3)");
        check(of(true, true, false, true, true, false) == PresentOutcome.MAGIC_INVENTORY_STATE,
                "magic inventory state named (priority 4)");
        check(of(true, true, false, false, false, true) == PresentOutcome.ALREADY_PRESENTED,
                "pending but all presented: ALREADY_PRESENTED");
        check(of(true, true, false, false, false, false) == PresentOutcome.NOTHING_TO_PRESENT,
                "nothing pending: NOTHING_TO_PRESENT");
        check(PresentOutcome.classify(null) == PresentOutcome.PLAYER_GONE,
                "null facts read as gone, never crash");

        // Retry classes.
        check(PresentOutcome.PLAYER_GONE.transientFailure(), "gone is transient (they may return)");
        check(PresentOutcome.GAMEMODE.transientFailure(), "gamemode is transient");
        check(PresentOutcome.FOREIGN_INVENTORY.transientFailure(), "foreign container is transient");
        check(PresentOutcome.MAGIC_INVENTORY_STATE.transientFailure(), "magic state is transient");
        check(!PresentOutcome.PRESENTED.transientFailure(), "presented is not a failure");
        check(!PresentOutcome.NOTHING_TO_PRESENT.transientFailure(), "nothing-to-present is terminal");
        check(!PresentOutcome.ALREADY_PRESENTED.transientFailure(), "already-presented is terminal");

        System.out.println("PresentOutcomeTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
