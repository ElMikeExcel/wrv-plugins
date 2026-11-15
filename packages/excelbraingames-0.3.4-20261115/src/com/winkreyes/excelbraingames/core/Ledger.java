package com.winkreyes.excelbraingames.core;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The shared Brain Games earnings ledger for one player: per-day totals
 * (ISO date -> cents), the consecutive-day streak, and the settlement math
 * for the $1,500/day cap (SPEC §9.2). Pure logic — no Bukkit, no Vault.
 *
 * Rules:
 *  - A settlement pays min(base + streakBonus, capRoom); a partial payment at
 *    the cap boundary pays only up to the cap.
 *  - The streak bonus lands on the FIRST paid completion of a day:
 *    $10 x streak length, max $50 (both config-driven, passed in). The bonus
 *    counts toward the cap.
 *  - Streak increments when the previous paid day was yesterday, resets to 1
 *    after a gap, and does not change on later completions the same day.
 *  - A settlement of 0 base (or one fully eaten by an exhausted cap) changes
 *    nothing — pays-once is enforced by the round's paid flag, not here.
 */
public final class Ledger {

    /** What a single completion actually paid. */
    public record Settlement(long basePaid, long bonusPaid, long totalPaid,
                             boolean capped, int streakAfter, boolean firstPaidOfDay) {
        public static final Settlement NONE = new Settlement(0, 0, 0, false, 0, false);
    }

    private final Map<String, Long> dailyCents = new HashMap<>();
    private int streak;
    private String lastPaidDate; // ISO yyyy-MM-dd or null

    public long earnedOn(String isoDate) {
        return dailyCents.getOrDefault(isoDate, 0L);
    }

    public int streak() { return streak; }
    public String lastPaidDate() { return lastPaidDate; }
    public Map<String, Long> dailyCents() { return dailyCents; }

    public void restore(Map<String, Long> daily, int streak, String lastPaidDate) {
        dailyCents.clear();
        if (daily != null) dailyCents.putAll(daily);
        this.streak = streak;
        this.lastPaidDate = lastPaidDate;
    }

    /** Zero one day's total (admin resetday). Streak state is untouched. */
    public void resetDay(String isoDate) {
        dailyCents.remove(isoDate);
    }

    /** Drop daily totals older than the retention window (35 days, SPEC §9.5). */
    public void pruneOlderThan(String isoCutoffExclusive) {
        Iterator<String> it = dailyCents.keySet().iterator();
        while (it.hasNext()) {
            if (it.next().compareTo(isoCutoffExclusive) < 0) it.remove();
        }
    }

    /**
     * Settle one completed round.
     *
     * @param today             ISO date of the completion day (America/Chicago)
     * @param baseCents         the round's payout after tier/kid/speed adjustment
     * @param capCents          daily cap
     * @param bonusPerDayCents  streak bonus per streak day
     * @param bonusMaxCents     streak bonus ceiling
     */
    public Settlement settle(String today, long baseCents, long capCents,
                             long bonusPerDayCents, long bonusMaxCents) {
        if (baseCents <= 0) return Settlement.NONE;
        long earned = earnedOn(today);
        long room = Math.max(0, capCents - earned);
        boolean firstToday = !today.equals(lastPaidDate);
        int newStreak = streak;
        long bonus = 0;
        if (firstToday) {
            String yesterday = LocalDate.parse(today).minusDays(1).toString();
            newStreak = yesterday.equals(lastPaidDate) ? streak + 1 : 1;
            bonus = Math.min(bonusMaxCents, bonusPerDayCents * newStreak);
        }
        long want = baseCents + bonus;
        long paid = Math.min(want, room);
        long basePaid = Math.min(baseCents, paid);
        long bonusPaid = paid - basePaid;
        if (paid > 0) {
            dailyCents.put(today, earned + paid);
            streak = newStreak;
            lastPaidDate = today;
        }
        return new Settlement(basePaid, bonusPaid, paid, paid < want, newStreak, firstToday);
    }
}
