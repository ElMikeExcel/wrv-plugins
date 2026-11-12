package com.winkreyes.excelsummons;

import java.util.Map;

/**
 * The rank-scaled summon cap (0.1.4 amendment, Mike, Nov 10, 2026,
 * 10:35 AM ruling, ladder LOCKED 10:37 AM): how many counted
 * summons a player may field depends on their EARNED magic rank —
 *
 *   Beginner 1 · Student 2 · Apprentice 3 · Master 5
 *
 * The cap VALUES live in the bridge config (summon-cap-by-rank) —
 * changing a rung is a config edit, never a rebuild. This class
 * owns only the rank derivation and the fail-safe cap lookup.
 *
 * Rank source: MagicGate's earned rank, read from the same record
 * the wand reconcile reads — plugins/ExcelMagicGate/earned.yml,
 * whose per-player row carries a credited cast total (the file
 * holds no rank string). The rank is derived from that total with
 * ExcelMagicGate 0.3.0 EarnedRank's LOCKED thresholds, mirrored
 * here verbatim so the two plugins can never disagree about what
 * a total means:
 *
 *   Student at 100 credited casts · Apprentice at 400 · Master at 2,000
 *
 * Fail-safe (supervisor direction): when a player's rank cannot
 * be read — no earned.yml row, file missing, unreadable total —
 * the cap fails to the LOWEST configured rung, never to the
 * highest and never to unlimited.
 *
 * Pure and unit-tested; the file read lives in ExcelSummons.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RankCap {

    private RankCap() {}

    /** Locked earned-rank thresholds (credited casts), mirrored
     *  from ExcelMagicGate's EarnedRank (Mike, Nov 9, 2026). */
    public static final long STUDENT_AT = 100;
    public static final long APPRENTICE_AT = 400;
    public static final long MASTER_AT = 2000;

    public static final String BEGINNER = "beginner";
    public static final String STUDENT = "student";
    public static final String APPRENTICE = "apprentice";
    public static final String MASTER = "master";

    /** The earned rank key for a credited cast total. */
    public static String rankForCredited(long credited) {
        if (credited >= MASTER_AT) return MASTER;
        if (credited >= APPRENTICE_AT) return APPRENTICE;
        if (credited >= STUDENT_AT) return STUDENT;
        return BEGINNER;
    }

    /** Normalize a rank string to a config key, or null when it
     *  names no rung of the ladder. */
    public static String normalize(String rank) {
        if (rank == null) return null;
        String r = rank.trim().toLowerCase();
        switch (r) {
            case BEGINNER:
            case STUDENT:
            case APPRENTICE:
            case MASTER:
                return r;
            default:
                return null;
        }
    }

    /** Display form for messages ("apprentice" -> "Apprentice";
     *  an unreadable rank reads "Unranked", honestly). */
    public static String display(String rank) {
        String r = normalize(rank);
        if (r == null) return "Unranked";
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    /**
     * The counted-summon cap for a rank, from the config map.
     * An unknown/unreadable rank — or a rung missing from the
     * map — fails to the LOWEST configured value. An empty map
     * fails to 1.
     */
    public static int capForRank(Map<String, Integer> capByRank, String rank) {
        if (capByRank == null || capByRank.isEmpty()) return 1;
        int lowest = Integer.MAX_VALUE;
        for (Integer v : capByRank.values()) {
            if (v != null && v > 0) lowest = Math.min(lowest, v);
        }
        if (lowest == Integer.MAX_VALUE) return 1;
        String r = normalize(rank);
        if (r == null) return lowest;
        Integer cap = null;
        for (Map.Entry<String, Integer> e : capByRank.entrySet()) {
            if (e.getKey() != null && e.getKey().trim().equalsIgnoreCase(r)) {
                cap = e.getValue();
                break;
            }
        }
        return (cap == null || cap <= 0) ? lowest : cap;
    }

    /** The locked ladder, used when the config section is absent:
     *  Beginner 1 · Student 2 · Apprentice 3 · Master 5. */
    public static Map<String, Integer> lockedLadder() {
        Map<String, Integer> m = new java.util.LinkedHashMap<>();
        m.put(BEGINNER, 1);
        m.put(STUDENT, 2);
        m.put(APPRENTICE, 3);
        m.put(MASTER, 5);
        return m;
    }
}
