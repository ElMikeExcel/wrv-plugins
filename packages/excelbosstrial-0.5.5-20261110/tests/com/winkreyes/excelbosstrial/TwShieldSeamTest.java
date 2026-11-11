/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import java.util.OptionalDouble;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * The TrainingWheels shield seam (0.3.7, TW-PRESSURE-CHECK fix
 * shape 1): a protected player's factor reaches the pulse, an
 * unprotected player reads as no shield, and the seam fails soft
 * to exactly 1.0 - no exception ever escapes, and a dead seam is
 * not consulted again. The reflective surface itself is proven
 * on the rig against the real TrainingWheels jar; here the
 * fail-soft contract is pinned with stub factor sources.
 */
public final class TwShieldSeamTest {

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

    private static final Logger LOG = Logger.getLogger("TwShieldSeamTest");
    private static final UUID PROTECTED = UUID.fromString("00000000-0000-0000-0009-000000000001");
    private static final UUID STRANGER = UUID.fromString("00000000-0000-0000-0009-000000000002");

    public static void main(String[] args) {
        // Protected: the factor reaches the caller, repeatably.
        TwShieldSeam shielded = new TwShieldSeam(LOG,
                id -> id.equals(PROTECTED) ? OptionalDouble.of(0.25) : OptionalDouble.empty());
        close(shielded.factorFor(PROTECTED), 0.25, "protected player: factor 0.25");
        close(shielded.factorFor(PROTECTED), 0.25, "protected player: factor 0.25 again");

        // Unprotected (empty): no shield, exactly 1.0.
        close(shielded.factorFor(STRANGER), 1.0, "unprotected player: 1.0");

        // Absent partner: a lookup that always reads empty is the
        // standalone shape - full pulse, no error.
        TwShieldSeam absent = new TwShieldSeam(LOG, id -> OptionalDouble.empty());
        close(absent.factorFor(PROTECTED), 1.0, "partner absent: 1.0");

        // Garbage factors are sanitized at the seam: never an
        // amplification, never an accidental zero.
        TwShieldSeam nan = new TwShieldSeam(LOG, id -> OptionalDouble.of(Double.NaN));
        close(nan.factorFor(PROTECTED), 1.0, "NaN factor: 1.0");
        TwShieldSeam over = new TwShieldSeam(LOG, id -> OptionalDouble.of(2.5));
        close(over.factorFor(PROTECTED), 1.0, "factor 2.5: 1.0");
        TwShieldSeam zero = new TwShieldSeam(LOG, id -> OptionalDouble.of(0.0));
        close(zero.factorFor(PROTECTED), 0.0, "genuine factor 0.0: honored");

        // A throwing lookup: 1.0, no exception escapes, and the
        // dead seam is never consulted again.
        AtomicInteger calls = new AtomicInteger();
        TwShieldSeam broken = new TwShieldSeam(LOG, id -> {
            calls.incrementAndGet();
            throw new IllegalStateException("partner blew up");
        });
        close(broken.factorFor(PROTECTED), 1.0, "throwing lookup: 1.0 (no exception escapes)");
        close(broken.factorFor(PROTECTED), 1.0, "dead seam: still 1.0");
        check(calls.get() == 1, "dead seam not consulted again (calls=" + calls.get() + ")");

        // Null player: 1.0, and the lookup is not consulted at all.
        AtomicInteger nullCalls = new AtomicInteger();
        TwShieldSeam any = new TwShieldSeam(LOG, id -> {
            nullCalls.incrementAndGet();
            return OptionalDouble.of(0.25);
        });
        close(any.factorFor(null), 1.0, "null player: 1.0");
        check(nullCalls.get() == 0, "null player: lookup not consulted");

        System.out.println("TwShieldSeamTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
