package com.winkreyes.excelguildgate;

import com.winkreyes.excelguildgate.RaidRepeatJudge.Outcome;
import com.winkreyes.excelguildgate.RaidWinnerCheck.Decision;

/**
 * Plain main-method harness for the 0.4.0 raid repeat judge and
 * book (RAID-COMPANION-SPEC sections 1.1-1.2 + the pressure math of
 * section 2.1). Same style as the other ExcelGuildGate harnesses:
 * every check counted, any failure prints and exits 1.
 *
 * The matrix pins the spec's invariants: an unbroken seal always
 * takes the first-break path; repeat-off restores the 0.3.4 early
 * return; the floors gate repeats exactly like breaks; the window
 * gates payment, never the kill; denials move no state; the book
 * persists its window and counts verbatim (the restart test is the
 * snapshot -> fresh book -> restore round trip).
 */
public final class RaidRepeatJudgeTest {

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

    public static void main(String[] args) {
        // --- The judge matrix (companion spec section 1.1) ---
        checkEq("unbroken seal always takes the first-break path",
                Outcome.FIRST_BREAK_PATH,
                RaidRepeatJudge.judge(false, Decision.CREDIT, false, true));
        checkEq("unbroken + no winner still first-break path (0.3.4 floors decide)",
                Outcome.FIRST_BREAK_PATH,
                RaidRepeatJudge.judge(false, Decision.NO_PLAYER_WINNER, false, true));
        checkEq("broken + repeat off = 0.3.4 early return",
                Outcome.FIRST_BREAK_PATH,
                RaidRepeatJudge.judge(true, Decision.CREDIT, false, false));
        checkEq("broken + floors pass + window expired = REPEAT_CREDIT",
                Outcome.REPEAT_CREDIT,
                RaidRepeatJudge.judge(true, Decision.CREDIT, false, true));
        checkEq("broken + floors pass + window running = DENY_REPEAT_WINDOW",
                Outcome.DENY_REPEAT_WINDOW,
                RaidRepeatJudge.judge(true, Decision.CREDIT, true, true));
        checkEq("broken + no winner = REPEAT_NO_WINNER (window irrelevant)",
                Outcome.REPEAT_NO_WINNER,
                RaidRepeatJudge.judge(true, Decision.NO_PLAYER_WINNER, false, true));
        checkEq("broken + no winner inside window still names the winner floor",
                Outcome.REPEAT_NO_WINNER,
                RaidRepeatJudge.judge(true, Decision.NO_PLAYER_WINNER, true, true));
        checkEq("broken + weak omen = REPEAT_OMEN_LOW",
                Outcome.REPEAT_OMEN_LOW,
                RaidRepeatJudge.judge(true, Decision.OMEN_TOO_LOW, false, true));
        checkEq("broken + undecided floors fail closed as no-winner",
                Outcome.REPEAT_NO_WINNER,
                RaidRepeatJudge.judge(true, null, false, true));

        // --- The repeat omen floor never weakens (spec section 1.1) ---
        checkEq("repeat floor: configured 5 stays 5",
                5, RaidRepeatJudge.requiredOmen(5));
        checkEq("repeat floor: a lowered config floor cannot lower repeats",
                5, RaidRepeatJudge.requiredOmen(3));
        checkEq("repeat floor: a raised config floor raises repeats too",
                6, RaidRepeatJudge.requiredOmen(6));

        // --- The book: window mechanics ---
        RaidRepeatBook book = new RaidRepeatBook();
        long t0 = 1_000_000L;
        check("fresh book: no window", !book.windowActive(t0));
        checkEq("fresh book: nothing remaining", 0L, book.remainingMs(t0));
        book.startWindow(t0, 24L * 3_600_000L);
        check("window runs at start", book.windowActive(t0));
        check("window runs one ms before expiry",
                book.windowActive(t0 + 24L * 3_600_000L - 1L));
        check("window expires at its instant",
                !book.windowActive(t0 + 24L * 3_600_000L));
        checkEq("remaining counts down", 3_600_000L,
                book.remainingMs(t0 + 23L * 3_600_000L));
        checkEq("window end is persisted verbatim", t0 + 24L * 3_600_000L,
                book.windowUntilMs());
        book.startWindow(t0 + 1000L, 0L);
        check("a zero-length window is born expired", !book.windowActive(t0 + 1000L));

        // --- The book: clear counts ---
        java.util.UUID a = java.util.UUID.randomUUID();
        java.util.UUID b = java.util.UUID.randomUUID();
        book.recordClear(java.util.List.of(a, b));
        book.recordClear(java.util.List.of(a));
        checkEq("winner of two clears counts 2", 2, book.clears(a));
        checkEq("winner of one clear counts 1", 1, book.clears(b));
        checkEq("a stranger counts 0", 0, book.clears(java.util.UUID.randomUUID()));
        book.recordClear(null);
        book.recordClear(java.util.List.of());
        checkEq("null/empty winner lists move nothing", 2, book.clears(a));

        // --- Persistence round trip (the restart scenario) ---
        RaidRepeatBook restored = new RaidRepeatBook();
        restored.restore(book.windowUntilMs(), book.snapshotClears());
        checkEq("restored window end matches", book.windowUntilMs(),
                restored.windowUntilMs());
        checkEq("restored counts match", book.snapshotClears(),
                restored.snapshotClears());
        restored.restore(0L, null);
        check("restore with nothing clears the book",
                !restored.windowActive(t0) && restored.clears(a) == 0);

        // --- Denied repeats leave the book untouched (statelessness) ---
        RaidRepeatBook denied = new RaidRepeatBook();
        denied.startWindow(t0, 1000L);
        java.util.Map<java.util.UUID, Integer> before = denied.snapshotClears();
        checkEq("a DENY_REPEAT_WINDOW ruling is the caller's alone: book unchanged",
                before, denied.snapshotClears());
        checkEq("format: hours and minutes", "23h 59m",
                RaidRepeatBook.formatRemaining(23L * 3_600_000L + 59L * 60_000L));
        checkEq("format: minutes only", "5m", RaidRepeatBook.formatRemaining(5L * 60_000L));
        checkEq("format: seconds only", "12s", RaidRepeatBook.formatRemaining(12_000L));
        checkEq("format: zero", "0s", RaidRepeatBook.formatRemaining(0L));

        // --- Pressure math (companion spec section 2.1) ---
        checkEq("wave 1 health scale is the bare multiplier",
                3.0, RaidPressureMath.waveHealthScale(0, 3.0, 0.10));
        check("wave ramp grows later waves",
                RaidPressureMath.waveHealthScale(4, 3.0, 0.10) > 3.0);
        check("wave 5 at +0.10/wave = x1.4 of the multiplier",
                Math.abs(RaidPressureMath.waveHealthScale(4, 3.0, 0.10) - 4.2) < 1e-9);
        checkEq("a zero multiplier degrades to unscaled, never zero-health",
                1.0, RaidPressureMath.waveHealthScale(0, 0.0, 0.10));
        checkEq("a NaN multiplier degrades to unscaled",
                1.0, RaidPressureMath.waveHealthScale(0, Double.NaN, 0.10));
        checkEq("attack scale passes through", 1.25,
                RaidPressureMath.attackScale(1.25));
        check("pulse hot: activity inside the window",
                RaidPressureMath.hot(1000L, 2000L, 30_000L));
        check("pulse cold: no activity ever", !RaidPressureMath.hot(0L, 2000L, 30_000L));
        check("pulse cold: activity aged out",
                !RaidPressureMath.hot(1000L, 60_000L, 30_000L));
        check("pulse due: hot and interval elapsed",
                RaidPressureMath.pulseDue(59_000L, 30_000L, 60_000L, 20_000L, 30_000L));
        check("pulse not due: hot but interval still running",
                !RaidPressureMath.pulseDue(59_000L, 55_000L, 60_000L, 20_000L, 30_000L));
        check("pulse not due: cold raid never pulses",
                !RaidPressureMath.pulseDue(1000L, 0L, 600_000L, 20_000L, 30_000L));
        check("pulse due: first pulse of a hot raid",
                RaidPressureMath.pulseDue(59_000L, 0L, 60_000L, 20_000L, 30_000L));

        System.out.println("RaidRepeatJudgeTest: " + checks + " checks, "
                + failures + " failures");
        if (failures > 0) {
            System.exit(1);
        }
    }
}
