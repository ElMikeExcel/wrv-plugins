package com.winkreyes.excelbraingames;

/** Minimal assertion counter in the fleet test style (no JUnit dependency). */
public final class Check {
    private int passed, failed;
    private final String suite;

    public Check(String suite) { this.suite = suite; }

    public void that(boolean cond, String what) {
        if (cond) passed++;
        else { failed++; System.out.println("  FAIL: " + what); }
    }

    public void eq(long expected, long actual, String what) {
        that(expected == actual, what + " (expected " + expected + ", got " + actual + ")");
    }

    public void finish() {
        System.out.println(suite + ": " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
