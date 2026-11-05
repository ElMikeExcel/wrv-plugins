package com.winkreyes.excellandmarks.core;

/**
 * Rent clock math (SPEC §5), pure and deterministic: all times are epoch
 * millis passed in, never read from the wall clock, so tests pin every
 * boundary exactly.
 */
public final class RentClock {

    public static final long DAY_MS = 86_400_000L;

    public enum State { AVAILABLE, ACTIVE, GRACE, LAPSED }

    private RentClock() { }

    /** Paid-through for a fresh rental starting at {@code now}. */
    public static long rentStart(long now, int periodDays) {
        return now + periodDays * DAY_MS;
    }

    /**
     * Renewal stacks from the later of now / current paid-through
     * (ExcelCosmetics precedent: rentals keep their time — renewing early
     * never shortens a rental, renewing in grace keeps the original base).
     */
    public static long renew(long now, long paidThrough, int periodDays) {
        return Math.max(now, paidThrough) + periodDays * DAY_MS;
    }

    public static long graceEnd(long paidThrough, int graceDays) {
        return paidThrough + graceDays * DAY_MS;
    }

    public static State stateAt(Spot spot, long now, int graceDays) {
        if (spot.isAvailable()) return State.AVAILABLE;
        if (spot.mode == Spot.Mode.OWNED) return State.ACTIVE;
        if (now <= spot.paidThroughMs) return State.ACTIVE;
        if (now <= graceEnd(spot.paidThroughMs, graceDays)) return State.GRACE;
        return State.LAPSED;
    }

    /** True when the clock should finalize this spot at {@code now}. */
    public static boolean dueForFinalize(Spot spot, long now, int graceDays) {
        return stateAt(spot, now, graceDays) == State.LAPSED;
    }
}
