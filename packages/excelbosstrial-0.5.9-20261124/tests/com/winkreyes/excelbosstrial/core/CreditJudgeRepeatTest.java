/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Map;

/**
 * The repeat layer of the credit judge (0.3.0, SPEC 2.2): judged
 * AFTER every 0.2.11 gate, in the settled order - denial cooldown,
 * share/presence denials, CREDIT on an unbroken seal, then the
 * repeat split. The base judge's answers are pinned unchanged.
 */
public final class CreditJudgeRepeatTest {

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

    private static void is(CreditJudge.Verdict actual, CreditJudge.Verdict expected, String label) {
        check(actual == expected, label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        // The base judge is untouched.
        is(CreditJudge.judge(true, true, false), CreditJudge.Verdict.CREDIT, "base: clean credit");
        is(CreditJudge.judge(false, true, false), CreditJudge.Verdict.DENY_SHARE, "base: share denial");
        is(CreditJudge.judge(true, false, false), CreditJudge.Verdict.DENY_PRESENCE, "base: presence denial");
        is(CreditJudge.judge(true, true, true), CreditJudge.Verdict.DENY_COOLDOWN, "base: cooldown denial");

        // Repeat layer: only a base CREDIT on a broken seal splits.
        is(CreditJudge.judge(true, true, false, false, false, true),
                CreditJudge.Verdict.CREDIT, "unbroken seal: first-break CREDIT");
        is(CreditJudge.judge(true, true, false, true, false, true),
                CreditJudge.Verdict.REPEAT_CREDIT, "broken + window expired: REPEAT_CREDIT");
        is(CreditJudge.judge(true, true, false, true, true, true),
                CreditJudge.Verdict.DENY_REPEAT_WINDOW, "broken + window running: DENY_REPEAT_WINDOW");
        is(CreditJudge.judge(true, true, false, true, false, false),
                CreditJudge.Verdict.CREDIT, "repeat layer off: broken seal still plain CREDIT (0.2.11)");

        // Denials pass through the repeat layer unchanged, in order.
        is(CreditJudge.judge(false, true, false, true, false, true),
                CreditJudge.Verdict.DENY_SHARE, "share denial beats the repeat split");
        is(CreditJudge.judge(true, false, false, true, true, true),
                CreditJudge.Verdict.DENY_PRESENCE, "presence denial beats the repeat split");
        is(CreditJudge.judge(true, true, true, true, false, true),
                CreditJudge.Verdict.DENY_COOLDOWN, "denial cooldown beats the repeat split");
        is(CreditJudge.judge(false, false, true, true, true, true),
                CreditJudge.Verdict.DENY_SHARE,
                "base order preserved: a share-failed kill during cooldown is DENY_SHARE (it refreshes)");
        is(CreditJudge.judge(true, true, true, true, true, true),
                CreditJudge.Verdict.DENY_COOLDOWN,
                "base order preserved: a fought kill during cooldown is DENY_COOLDOWN, repeat layer never reached");

        // Cooldown consequences: repeat verdicts never start one.
        check(!CreditJudge.startsCooldown(CreditJudge.Verdict.REPEAT_CREDIT),
                "REPEAT_CREDIT starts no denial cooldown");
        check(!CreditJudge.startsCooldown(CreditJudge.Verdict.DENY_REPEAT_WINDOW),
                "DENY_REPEAT_WINDOW starts no denial cooldown");
        check(CreditJudge.startsCooldown(CreditJudge.Verdict.DENY_SHARE),
                "DENY_SHARE still starts its cooldown");
        check(!CreditJudge.startsCooldown(CreditJudge.Verdict.CREDIT),
                "CREDIT starts no cooldown");

        // The repeat window book: explicit durations, persistence.
        CooldownBook book = new CooldownBook();
        long now = 1_000_000L;
        long day = 24L * 3600 * 1000;
        book.start("wither", now, day);
        check(book.isActive("wither", now + 1), "window active just after the clear");
        check(book.isActive("wither", now + day - 1), "window active at 23:59:59.999");
        check(!book.isActive("wither", now + day), "window expired at exactly 24h");
        check(book.remainingMs("wither", now) == day, "remaining is the full window at start");
        check(!book.isActive("warden", now), "an untouched seal has no window");
        Map<String, Object> snap = book.snapshot();
        CooldownBook restored = new CooldownBook();
        restored.restore(snap);
        check(restored.isActive("wither", now + 3600_000L),
                "window survives a snapshot/restore round trip (restart persistence)");
        check(restored.remainingMs("wither", now) == day, "restored remaining matches");

        System.out.println("CreditJudgeRepeatTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
