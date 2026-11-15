package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.core.Ledger;
import com.winkreyes.excelbraingames.core.Money;
import com.winkreyes.excelbraingames.core.Payouts;

/**
 * Economy math: the $1,500/day cap (incl. partial payment at the boundary),
 * streak bonus steps, kid-tier half payout for adults (Mike's Q6 amendment,
 * Nov 4, 2026 — round DOWN to the cent), speed/par bonus, pays-once shape.
 * All amounts in cents: cap $1,500 = 150000, bonus $10/day up to $50.
 */
public final class LedgerTest {

    static final long CAP = 150_000, PER_DAY = 1_000, MAX_BONUS = 5_000;

    public static void main(String[] args) {
        Check ck = new Check("LedgerTest");
        capBoundary(ck);
        streakSteps(ck);
        streakResetAndSameDay(ck);
        kidHalf(ck);
        speedAndParBonus(ck);
        zeroAndExhausted(ck);
        resetDayAndPrune(ck);
        moneyFormat(ck);
        ck.finish();
    }

    static void capBoundary(Check ck) {
        Ledger l = new Ledger();
        // Day 1: first completion $1,490 base + $10 streak bonus = exactly at cap.
        Ledger.Settlement s1 = l.settle("2026-11-04", 149_000, CAP, PER_DAY, MAX_BONUS);
        ck.eq(149_000, s1.basePaid(), "cap: base paid in full");
        ck.eq(1_000, s1.bonusPaid(), "cap: streak bonus paid");
        ck.eq(150_000, s1.totalPaid(), "cap: total lands exactly on cap");
        ck.that(!s1.capped(), "cap: landing exactly on the cap is not 'capped'");
        // Second completion the same day pays nothing — cap is exhausted.
        Ledger.Settlement s2 = l.settle("2026-11-04", 9_000, CAP, PER_DAY, MAX_BONUS);
        ck.eq(0, s2.totalPaid(), "cap: nothing more pays today");
        ck.eq(150_000, l.earnedOn("2026-11-04"), "cap: daily total stays at cap");

        // Partial payment at the boundary: $1,495 already earned, round pays $90 + no bonus (2nd today)
        Ledger l2 = new Ledger();
        l2.settle("2026-11-04", 148_500, CAP, PER_DAY, MAX_BONUS); // 148500 + 1000 bonus = 149500
        ck.eq(149_500, l2.earnedOn("2026-11-04"), "cap: setup total");
        Ledger.Settlement s3 = l2.settle("2026-11-04", 9_000, CAP, PER_DAY, MAX_BONUS);
        ck.eq(500, s3.totalPaid(), "cap: partial pays only the remaining $5.00");
        ck.that(s3.capped(), "cap: partial payment is flagged capped");
        ck.eq(150_000, l2.earnedOn("2026-11-04"), "cap: total never exceeds cap");
    }

    static void streakSteps(Check ck) {
        Ledger l = new Ledger();
        String[] days = {"2026-11-01", "2026-11-02", "2026-11-03", "2026-11-04", "2026-11-05", "2026-11-06"};
        long[] wantBonus = {1_000, 2_000, 3_000, 4_000, 5_000, 5_000}; // $10..$50, then clamped
        for (int i = 0; i < days.length; i++) {
            Ledger.Settlement s = l.settle(days[i], 10_000, CAP, PER_DAY, MAX_BONUS);
            ck.eq(wantBonus[i], s.bonusPaid(), "streak: day " + (i + 1) + " bonus");
            ck.eq(i + 1, s.streakAfter(), "streak: day " + (i + 1) + " streak count");
            ck.that(s.firstPaidOfDay(), "streak: first paid completion flagged");
        }
    }

    static void streakResetAndSameDay(Check ck) {
        Ledger l = new Ledger();
        l.settle("2026-11-01", 10_000, CAP, PER_DAY, MAX_BONUS);
        l.settle("2026-11-02", 10_000, CAP, PER_DAY, MAX_BONUS);
        // Skip 11-03: streak resets to 1 on 11-04.
        Ledger.Settlement s = l.settle("2026-11-04", 10_000, CAP, PER_DAY, MAX_BONUS);
        ck.eq(1, s.streakAfter(), "streak: gap resets to 1");
        ck.eq(1_000, s.bonusPaid(), "streak: reset bonus is $10");
        // Same day second completion: no bonus, streak unchanged.
        Ledger.Settlement s2 = l.settle("2026-11-04", 10_000, CAP, PER_DAY, MAX_BONUS);
        ck.eq(0, s2.bonusPaid(), "streak: no bonus on second completion of the day");
        ck.eq(1, s2.streakAfter(), "streak: unchanged within the day");
        ck.that(!s2.firstPaidOfDay(), "streak: second completion not flagged first");
    }

    static void kidHalf(Check ck) {
        // Mike's Q6 amendment: adults on kid tiers earn HALF, rounded DOWN to the cent.
        ck.eq(2_000, Payouts.adjust(4_000, 0, true, false), "kid tier: adult earns half of $40 = $20");
        ck.eq(4_000, Payouts.adjust(4_000, 0, true, true), "kid tier: kid-flagged earns full $40");
        ck.eq(1_250, Payouts.adjust(2_501, 0, true, false), "kid tier: odd cents round DOWN (2501 -> 1250)");
        ck.eq(9_000, Payouts.adjust(9_000, 0, false, false), "adult tier: no kid adjustment");
        ck.eq(1_500, Payouts.adjust(3_000, 0, true, false), "kid tier: adult earns half of $30 = $15");
        // ...and the halved amount still flows through the cap like any payout.
        Ledger l = new Ledger();
        Ledger.Settlement s = l.settle("2026-11-04", Payouts.adjust(4_000, 0, true, false), CAP, PER_DAY, MAX_BONUS);
        ck.eq(2_000, s.basePaid(), "kid tier: halved base settles in full under cap");
        ck.eq(1_000, s.bonusPaid(), "kid tier: streak bonus is NOT halved (it is not a tier payout)");
    }

    static void speedAndParBonus(Check ck) {
        ck.eq(10_800, Payouts.adjust(9_000, 20, false, false), "speed bonus: +20% on $90 = $108");
        ck.eq(18_000, Payouts.adjust(15_000, 20, false, false), "speed bonus: +20% on $150 = $180");
        ck.eq(9_000, Payouts.adjust(9_000, 0, false, false), "no bonus percent: unchanged");
        ck.eq(12_000, Payouts.adjust(10_000, 20, false, false), "par bonus: +20% on $100 = $120");
    }

    static void zeroAndExhausted(Check ck) {
        Ledger l = new Ledger();
        Ledger.Settlement s = l.settle("2026-11-04", 0, CAP, PER_DAY, MAX_BONUS);
        ck.eq(0, s.totalPaid(), "zero base settles zero");
        ck.eq(0, l.earnedOn("2026-11-04"), "zero base records nothing");
        ck.that(l.lastPaidDate() == null, "zero base does not start a streak");
    }

    static void resetDayAndPrune(Check ck) {
        Ledger l = new Ledger();
        l.settle("2026-10-01", 10_000, CAP, PER_DAY, MAX_BONUS);
        l.settle("2026-11-04", 10_000, CAP, PER_DAY, MAX_BONUS);
        l.resetDay("2026-11-04");
        ck.eq(0, l.earnedOn("2026-11-04"), "resetday zeroes today");
        ck.eq(11_000, l.earnedOn("2026-10-01"), "resetday leaves other days alone");
        l.pruneOlderThan("2026-10-02");
        ck.eq(0, l.earnedOn("2026-10-01"), "prune drops old day totals");
    }

    static void moneyFormat(Check ck) {
        ck.that(Money.fmt(150_000).equals("$1,500.00"), "fmt cap");
        ck.that(Money.fmt(2_000).equals("$20.00"), "fmt twenty");
        ck.that(Money.fmt(5).equals("$0.05"), "fmt five cents");
        ck.eq(4_000, Money.dollarsToCents(40.00), "dollars to cents");
        ck.eq(1_250, Money.halfRoundedDown(2_501), "half rounds down to the cent");
    }
}
