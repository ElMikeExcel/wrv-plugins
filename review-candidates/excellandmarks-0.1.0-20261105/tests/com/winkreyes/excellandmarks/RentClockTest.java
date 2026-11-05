package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.RentClock;
import com.winkreyes.excellandmarks.core.Spot;

import java.util.Random;
import java.util.UUID;

/**
 * Rent clock + grace math (SPEC §5): exact boundaries at paid-through
 * and grace end, renewal stacking (early renew loses nothing; grace
 * renew starts from now), and state classification across a randomized
 * sweep.
 */
public final class RentClockTest {

    static final long T0 = 1_000_000_000_000L; // arbitrary fixed epoch
    static final int PERIOD = 30, GRACE = 7;

    public static void main(String[] args) {
        Check ck = new Check("RentClockTest");
        boundaries(ck);
        renewalStacking(ck);
        classification(ck);
        propertySweep(ck);
        ck.finish();
    }

    static Spot rented(long paidThrough) {
        Spot s = new Spot("lm-1", "world", 0, 64, 0, "T", Spot.Type.PLOT, Spot.Tier.SPOT,
                50_000, 1_000_000, 3, T0);
        s.ownerId = UUID.randomUUID();
        s.ownerName = "R";
        s.mode = Spot.Mode.RENTED;
        s.paidThroughMs = paidThrough;
        return s;
    }

    static void boundaries(Check ck) {
        long pt = T0 + RentClock.DAY_MS * PERIOD;
        ck.eq(pt, RentClock.rentStart(T0, PERIOD), "rentStart = now + one period");
        Spot s = rented(pt);
        ck.that(RentClock.stateAt(s, pt - 1, GRACE) == RentClock.State.ACTIVE, "1ms before paid-through: ACTIVE");
        ck.that(RentClock.stateAt(s, pt, GRACE) == RentClock.State.ACTIVE, "exactly at paid-through: still ACTIVE");
        ck.that(RentClock.stateAt(s, pt + 1, GRACE) == RentClock.State.GRACE, "1ms after paid-through: GRACE");
        long ge = RentClock.graceEnd(pt, GRACE);
        ck.eq(pt + RentClock.DAY_MS * GRACE, ge, "grace end = paid-through + 7d");
        ck.that(RentClock.stateAt(s, ge, GRACE) == RentClock.State.GRACE, "exactly at grace end: still GRACE");
        ck.that(RentClock.stateAt(s, ge + 1, GRACE) == RentClock.State.LAPSED, "1ms after grace end: LAPSED");
        ck.that(!RentClock.dueForFinalize(s, ge, GRACE), "not due at grace end exactly");
        ck.that(RentClock.dueForFinalize(s, ge + 1, GRACE), "due 1ms after grace end");

        Spot free = new Spot("lm-2", "world", 0, 64, 0, "F", Spot.Type.LANDMARK, Spot.Tier.SPOT,
                50_000, 1_000_000, 0, T0);
        ck.that(RentClock.stateAt(free, ge + 999_999, GRACE) == RentClock.State.AVAILABLE, "unowned spot: AVAILABLE at any time");
        Spot owned = rented(0);
        owned.mode = Spot.Mode.OWNED;
        ck.that(RentClock.stateAt(owned, ge + 999_999, GRACE) == RentClock.State.ACTIVE, "owned spot never lapses");
        ck.that(!RentClock.dueForFinalize(owned, ge + 999_999, GRACE), "owned spot never due");
    }

    static void renewalStacking(Check ck) {
        long pt = T0 + RentClock.DAY_MS * PERIOD;
        // Early renewal: stacks onto paid-through, nothing lost.
        long early = RentClock.renew(T0 + RentClock.DAY_MS, pt, PERIOD);
        ck.eq(pt + RentClock.DAY_MS * PERIOD, early, "early renew stacks from paid-through");
        // Renewal exactly at paid-through: same base (max of equals).
        ck.eq(pt + RentClock.DAY_MS * PERIOD, RentClock.renew(pt, pt, PERIOD), "renew at paid-through stacks from paid-through");
        // Grace renewal: starts from now, not from the stale paid-through.
        long graceNow = pt + RentClock.DAY_MS * 3;
        ck.eq(graceNow + RentClock.DAY_MS * PERIOD, RentClock.renew(graceNow, pt, PERIOD), "grace renew stacks from now");
    }

    static void classification(Check ck) {
        long pt = T0 + RentClock.DAY_MS * PERIOD;
        Spot s = rented(pt);
        // Full month of ACTIVE then a full week of GRACE then LAPSED — sweep by hours.
        for (long h = 0; h <= (PERIOD + GRACE) * 24L; h++) {
            long now = T0 + h * 3_600_000L;
            RentClock.State want = now <= pt ? RentClock.State.ACTIVE
                    : now <= pt + GRACE * RentClock.DAY_MS ? RentClock.State.GRACE : RentClock.State.LAPSED;
            ck.that(RentClock.stateAt(s, now, GRACE) == want, "state at hour " + h);
        }
    }

    static void propertySweep(Check ck) {
        Random rng = new Random(20261105L);
        for (int i = 0; i < 20_000; i++) {
            long now = T0 + (long) (rng.nextDouble() * 200 * RentClock.DAY_MS);
            long pt = T0 + (long) (rng.nextDouble() * 100 * RentClock.DAY_MS);
            Spot s = rented(pt);
            RentClock.State st = RentClock.stateAt(s, now, GRACE);
            boolean wantDue = now > pt + GRACE * RentClock.DAY_MS;
            ck.that(RentClock.dueForFinalize(s, now, GRACE) == wantDue, "due == past grace end (i=" + i + ")");
            ck.that((st == RentClock.State.LAPSED) == wantDue, "LAPSED iff due (i=" + i + ")");
            ck.that(st != RentClock.State.AVAILABLE, "rented spot never AVAILABLE (i=" + i + ")");
            // Renewal never moves paid-through backwards.
            long renewed = RentClock.renew(now, pt, PERIOD);
            ck.that(renewed >= pt + PERIOD * RentClock.DAY_MS - PERIOD * RentClock.DAY_MS || renewed > pt,
                    "renew extends (i=" + i + ")");
            ck.that(renewed == Math.max(now, pt) + PERIOD * RentClock.DAY_MS, "renew formula (i=" + i + ")");
        }
    }
}
