/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * The damage-intake factor (0.3.0, SPEC 0.3): the entity bar
 * never exceeds the platform cap (rig-measured 1,024 on Paper
 * 26.3-235) while the ledger judges the full configured pool.
 * The invariants pinned here are the ones the rig scenarios
 * re-prove live: identity under the cap, exact fraction mirror,
 * and death timing that lands on the configured pool exactly.
 */
public final class IntakeFactorTest {

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

    private static void close(double actual, double expected, String label) {
        check(Math.abs(actual - expected) < 1e-9,
                label + " (expected " + expected + ", got " + actual + ")");
    }

    public static void main(String[] args) {
        double cap = 1024;

        // Identity at and under the cap: 0.2.11 behavior, bit for bit.
        check(!IntakeFactor.active(cap, 300), "vanilla wither pool: intake inactive");
        check(!IntakeFactor.active(cap, 1024), "pool exactly at cap: inactive");
        close(IntakeFactor.factor(cap, 1000), 1.0, "factor 1 under cap");
        close(IntakeFactor.entityMax(cap, 1000), 1000, "entity max is the pool under cap");
        close(IntakeFactor.toLedger(45, 1.0), 45, "ledger conversion identity at f=1");
        close(IntakeFactor.toEntity(45, 1.0), 45, "entity conversion identity at f=1");

        // The rig's failing pools, now governed.
        check(IntakeFactor.active(cap, 2400), "pool 2,400: intake active");
        close(IntakeFactor.factor(cap, 2400), 1024.0 / 2400.0, "factor for pool 2,400");
        close(IntakeFactor.entityMax(cap, 2400), 1024, "entity max clamps to the cap");
        close(IntakeFactor.factor(cap, 16000), 1024.0 / 16000.0, "factor for pool 16,000");
        close(IntakeFactor.entityMax(cap, 16000), 1024, "entity max for pool 16,000 still the cap");

        // Round trip: a governed hit of 45 ledger damage lands as
        // 45*f entity damage and records back as exactly 45.
        double f = IntakeFactor.factor(cap, 21900);
        close(IntakeFactor.toLedger(IntakeFactor.toEntity(45, f), f), 45,
                "governed 45-damage hit round-trips exactly");
        close(IntakeFactor.toEntity(45, f), 45 * 1024.0 / 21900.0,
                "entity damage is the ledger damage times f");

        // The bar mirrors the ledger's remaining fraction.
        close(IntakeFactor.entityHealthFor(cap, 21900, 0, 0), 1024,
                "untouched pool: full entity bar");
        close(IntakeFactor.entityHealthFor(cap, 21900, 10950, 0), 512,
                "half the pool dealt: half the bar");
        close(IntakeFactor.entityHealthFor(cap, 21900, 10950, 10950), 0,
                "dealt + unseen = pool: bar empty");
        close(IntakeFactor.entityHealthFor(cap, 21900, 21900, 0), 0,
                "pool fully dealt: entity dies on the exact blow");
        close(IntakeFactor.entityHealthFor(cap, 21900, 99999, 0), 0,
                "overkill clamps at zero, never negative");
        close(IntakeFactor.entityHealthFor(cap, 1000, 250, 0), 750,
                "sub-cap pools mirror without the factor");

        // Defensive inputs.
        close(IntakeFactor.factor(cap, 0), 1.0, "zero pool: factor 1");
        close(IntakeFactor.factor(0, 5000), 1.0, "zero cap: factor 1 (intake off)");
        close(IntakeFactor.toLedger(10, 0), 10, "zero factor reads as identity, never divides");

        System.out.println("IntakeFactorTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
