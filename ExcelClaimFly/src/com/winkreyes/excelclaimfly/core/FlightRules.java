package com.winkreyes.excelclaimfly.core;

/**
 * Pure decision logic for Build Flight (no Bukkit types, unit tested).
 * Given what the server knows about a player right now, say whether
 * flight should be allowed, and if not, why.
 */
public final class FlightRules {
    private FlightRules() {}

    /** Why flight is off. NONE = flight allowed. */
    public enum Block { NONE, NO_PASS, OTHER_MODE, BLOCKED_WORLD, OUTSIDE_CLAIM, COMBAT }

    /** Snapshot of the facts for one player at one moment. */
    public record Facts(boolean hasPass, boolean survivalOrAdventure, boolean worldBlocked,
                        boolean inOwnClaim, boolean combatTagged, boolean combatRuleOn) {}

    public static Block decide(Facts f) {
        if (!f.hasPass()) return Block.NO_PASS;
        if (!f.survivalOrAdventure()) return Block.OTHER_MODE;
        if (f.worldBlocked()) return Block.BLOCKED_WORLD;
        if (!f.inOwnClaim()) return Block.OUTSIDE_CLAIM;
        if (f.combatRuleOn() && f.combatTagged()) return Block.COMBAT;
        return Block.NONE;
    }

    /** "23:41" style countdown, or "1:02:05" past an hour. Never negative. */
    public static String clock(long millis) {
        long s = Math.max(0L, (millis + 999L) / 1000L);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, sec) : String.format("%d:%02d", m, sec);
    }

    /** Plain words, rounded UP to whole minutes so a warning at 4:59 says "5 min": "23 min", "1 min", "1 hr 5 min". */
    public static String words(long millis) {
        long totalMin = (Math.max(0L, millis) + 59_999L) / 60_000L;
        long h = totalMin / 60, m = totalMin % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " hr" : h + " hr " + m + " min";
    }

    /**
     * Which warning (if any) is due. Warnings are given once per pass end time.
     * Returns the threshold in minutes (5 or 1) or 0 for none.
     * lastWarned = the smallest threshold already warned for this end time (or Integer.MAX_VALUE).
     */
    public static int warningDue(long remainingMs, int lastWarned, int[] thresholdsMin) {
        int due = 0;
        for (int t : thresholdsMin) {
            if (remainingMs <= t * 60_000L && remainingMs > 0 && t < lastWarned) {
                if (due == 0 || t < due) due = t;
            }
        }
        return due;
    }

    /** Extend-or-refuse rule for a re-buy: new total must stay within the cap. */
    public static boolean canExtend(long remainingMs, long addMs, long capMs) {
        return Math.max(0L, remainingMs) + addMs <= capMs;
    }
}
