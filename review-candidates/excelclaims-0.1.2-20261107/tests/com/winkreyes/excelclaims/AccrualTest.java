package com.winkreyes.excelclaims;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Accrual math against the live config values (100/hour, cap 80,000). */
public class AccrualTest {

    private PlayerRecord record(int accrued) {
        return new PlayerRecord(UUID.randomUUID(), accrued, 0);
    }

    private void playMinutes(PlayerRecord pr, int minutes, int perHour, int cap) {
        for (int i = 0; i < minutes; i++) Accrual.accrueMinute(pr, perHour, cap);
    }

    @Test
    void liveRatePaysExactly100PerHour() {
        PlayerRecord pr = record(0);
        playMinutes(pr, 60, 100, 80000);
        assertEquals(100, pr.accrued);
    }

    @Test
    void liveRateHalfHour() {
        PlayerRecord pr = record(0);
        playMinutes(pr, 30, 100, 80000);
        assertEquals(50, pr.accrued);
    }

    @Test
    void capStopsAccrual() {
        PlayerRecord pr = record(79990);
        playMinutes(pr, 60, 100, 80000);
        assertEquals(80000, pr.accrued);
        playMinutes(pr, 600, 100, 80000);
        assertEquals(80000, pr.accrued);
    }

    @Test
    void dividingRate() {
        PlayerRecord pr = record(0);
        playMinutes(pr, 60, 60, 80000);
        assertEquals(60, pr.accrued);
    }

    @Test
    void nonDividingRateHasNoDrift() {
        PlayerRecord pr = record(0);
        playMinutes(pr, 120, 250, 80000);
        assertEquals(500, pr.accrued);
        PlayerRecord pr2 = record(0);
        playMinutes(pr2, 7, 250, 80000);
        assertEquals(29, pr2.accrued, "7 min at 250/hr = 29.16 -> 29 whole blocks, fraction carried");
        playMinutes(pr2, 53, 250, 80000);
        assertEquals(250, pr2.accrued, "the carried fraction pays out by the hour mark");
    }

    @Test
    void remainingMath() {
        assertEquals(19, Accrual.remaining(100, 0, 81));
        assertEquals(1500, Accrual.remaining(1000, 500, 0));
        assertEquals(-10, Accrual.remaining(100, 0, 110));
    }

    @Test
    void perPlayerLimitOverride() {
        PlayerRecord pr = record(0);
        pr.accruedLimitOverride = 250;
        playMinutes(pr, 600, 100, pr.accruedLimitOverride);
        assertEquals(250, pr.accrued);
    }
}

// ExcelClaims tests — Iris / Mike
