package com.winkreyes.excelclaims;

/**
 * ExcelClaims — claim-block accrual math, kept pure so the unit tests
 * can pin it exactly. Model: the carry counts sixtieths of a block.
 * Each minute of play adds (rate per hour) sixtieths; every 60
 * sixtieths pay out one block, capped at the player's accrued limit.
 * At the live rate of 100/hour this pays 1 block most minutes and 2
 * every third minute — 100 per hour exactly, no drift.
 * Excel — Iris / Mike.
 */
public final class Accrual {

    private Accrual() { }

    /** Advance one minute. Mutates the record's carry and accrued. */
    public static void accrueMinute(PlayerRecord pr, int perHour, int cap) {
        if (perHour <= 0 || pr.accrued >= cap) return;
        pr.playMinutesCarry += perHour;
        int whole = pr.playMinutesCarry / 60;
        if (whole > 0) {
            pr.playMinutesCarry -= whole * 60;
            pr.accrued = Math.min(cap, pr.accrued + whole);
        }
    }

    /** Remaining blocks = accrued + bonus − used (top-level area). */
    public static int remaining(int accrued, int bonus, int usedArea) {
        return accrued + bonus - usedArea;
    }
}

// ExcelClaims — Iris / Mike, for Wink Reyes Valley
