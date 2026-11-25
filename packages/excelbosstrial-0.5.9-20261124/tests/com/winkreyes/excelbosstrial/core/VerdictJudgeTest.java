/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The pool-exhaustion verdict (0.4.0, PM-2): judged on NET
 * damage against the pool, at an epsilon that forgives float
 * dust but never a real hit's worth of health. A degenerate
 * pool never exhausts.
 */
public final class VerdictJudgeTest {

    private static int passed;
    private static int failed;

    private static void check(boolean ok, String label) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        check(!VerdictJudge.exhausted(21900, 0), "a fresh pool is not exhausted");
        check(!VerdictJudge.exhausted(21900, 16430), "the Nov 7 gross (75%) does not exhaust");
        check(VerdictJudge.exhausted(21900, 21900), "net == pool exhausts");
        check(VerdictJudge.exhausted(21900, 23000), "net past the pool exhausts");
        check(VerdictJudge.exhausted(21900, 21900 - 5e-10), "float dust under the pool still exhausts");
        check(!VerdictJudge.exhausted(21900, 21900 - 0.5), "half a pool HP short does not exhaust");
        check(!VerdictJudge.exhausted(0, 0), "a zero pool never exhausts");
        check(!VerdictJudge.exhausted(-100, 500), "a negative pool never exhausts");
        check(!VerdictJudge.exhausted(Double.NaN, 500), "a NaN pool never exhausts");
        check(!VerdictJudge.exhausted(21900, Double.NaN), "NaN damage never exhausts");
        check(!VerdictJudge.exhausted(21900, -50), "negative net never exhausts");

        System.out.println("VerdictJudgeTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
