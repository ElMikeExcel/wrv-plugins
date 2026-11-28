import excelemfixes.partydrops.MultiplierMath;

import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/** Multiplier table + fractional rounding (spec §Shape, §4, §9). */
public class MultiplierMathTest {

    /** A RandomSource that counts its draws and delegates to a seeded Random. */
    static final class CountingRandom implements MultiplierMath.RandomSource {
        final Random delegate;
        final AtomicInteger draws = new AtomicInteger();
        CountingRandom(long seed) { this.delegate = new Random(seed); }
        @Override public double nextDouble() { draws.incrementAndGet(); return delegate.nextDouble(); }
    }

    public static void run() {
        System.out.println("MultiplierMathTest");

        // --- multiplier table: participants 0,1,2,3,4,5,6,12 -> 1,1,1.5,2,2.5,3,3,3
        int[] counts = {0, 1, 2, 3, 4, 5, 6, 12};
        double[] want = {1, 1, 1.5, 2, 2.5, 3, 3, 3};
        for (int i = 0; i < counts.length; i++) {
            Check.close(want[i], MultiplierMath.multiplier(counts[i], 0.5, 3.0),
                    "multiplier(" + counts[i] + ") = " + want[i]);
        }
        // negative count is a caller bug; still exactly 1x, never a discount
        Check.close(1.0, MultiplierMath.multiplier(-3, 0.5, 3.0), "multiplier(-3) = 1");
        // config plumbing: other step / cap values flow through
        Check.close(2.0, MultiplierMath.multiplier(3, 0.5, 2.0), "cap 2.0 clamps trio to 2.0");
        Check.close(1.25, MultiplierMath.multiplier(2, 0.25, 3.0), "step 0.25 duo = 1.25");
        Check.close(1.0, MultiplierMath.multiplier(5, 0.0, 3.0), "step 0 = always 1x");
        // invalid config values fall back to the spec defaults
        Check.close(1.5, MultiplierMath.multiplier(2, Double.NaN, 3.0), "NaN step falls back to 0.5");
        Check.close(3.0, MultiplierMath.multiplier(9, 0.5, Double.NaN), "NaN cap falls back to 3.0");
        Check.close(1.5, MultiplierMath.multiplier(2, -1.0, 3.0), "negative step falls back to 0.5");
        Check.close(1.5, MultiplierMath.multiplier(2, 0.5, 0.5), "cap below 1 falls back to 3.0 path (duo 1.5)");

        // --- exact integer cases consume ZERO random draws
        CountingRandom rng = new CountingRandom(42L);
        Check.eq(5, MultiplierMath.scaledAmount(5, 1.0, rng), "amount 5 at 1.0x = 5");
        Check.eq(0, rng.draws.get(), "1.0x consumed no draws");
        Check.eq(6, MultiplierMath.scaledAmount(2, 3.0, rng), "amount 2 at 3.0x = 6");
        Check.eq(4, MultiplierMath.scaledAmount(2, 2.0, rng), "amount 2 at 2.0x = 4");
        Check.eq(9, MultiplierMath.scaledAmount(3, 3.0, rng), "amount 3 at 3.0x = 9");
        Check.eq(0, rng.draws.get(), "integer totals consumed no draws");
        Check.eq(0, MultiplierMath.scaledAmount(0, 2.5, rng), "amount 0 stays 0");
        Check.eq(-2, MultiplierMath.scaledAmount(-2, 2.5, rng), "negative amount passes through");
        Check.eq(0, rng.draws.get(), "degenerate amounts consumed no draws");

        // --- fractional rounding: fixed-seed distributions, exact in expectation
        CountingRandom r1 = new CountingRandom(20261020L);
        int trials = 20_000;
        long sum = 0;
        for (int i = 0; i < trials; i++) sum += MultiplierMath.scaledAmount(1, 1.5, r1);
        Check.near(1.5, (double) sum / trials, 0.02, "amount 1 at 1.5x: mean ~1.5 over 20k");
        Check.eq(trials, r1.draws.get(), "fractional case draws once per stack");

        CountingRandom r2 = new CountingRandom(777L);
        sum = 0;
        for (int i = 0; i < trials; i++) sum += MultiplierMath.scaledAmount(3, 2.5, r2);
        Check.near(7.5, (double) sum / trials, 0.03, "amount 3 at 2.5x: mean ~7.5 over 20k");

        CountingRandom r3 = new CountingRandom(99L);
        sum = 0;
        for (int i = 0; i < trials; i++) sum += MultiplierMath.scaledAmount(2, 1.5, r3);
        Check.eq(3L * trials, sum, "amount 2 at 1.5x = exactly 3 every time (7.5-style frac only)");
        Check.eq(0, r3.draws.get(), "amount 2 at 1.5x has zero fraction: no draws");

        // bounds: a scaled stack is always floor or floor+1, never outside
        CountingRandom r4 = new CountingRandom(1234L);
        boolean inBounds = true;
        for (int i = 0; i < 5_000; i++) {
            int v = MultiplierMath.scaledAmount(1, 1.5, r4);
            if (v != 1 && v != 2) inBounds = false;
            int w = MultiplierMath.scaledAmount(3, 2.5, r4);
            if (w != 7 && w != 8) inBounds = false;
        }
        Check.ok(inBounds, "scaled amounts always floor or floor+1");

        // a rigged source pins the Bernoulli edge behaviour exactly
        MultiplierMath.RandomSource alwaysLow = () -> 0.0;
        MultiplierMath.RandomSource alwaysHigh = () -> 0.999999;
        Check.eq(2, MultiplierMath.scaledAmount(1, 1.5, alwaysLow), "draw 0.0 < 0.5 -> extra item");
        Check.eq(1, MultiplierMath.scaledAmount(1, 1.5, alwaysHigh), "draw ~1.0 >= 0.5 -> no extra");
        MultiplierMath.RandomSource atBoundary = () -> 0.5;
        Check.eq(1, MultiplierMath.scaledAmount(1, 1.5, atBoundary), "draw exactly == frac -> no extra (strict <)");
    }
}
