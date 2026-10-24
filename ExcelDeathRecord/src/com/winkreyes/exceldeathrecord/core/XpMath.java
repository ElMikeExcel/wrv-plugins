package com.winkreyes.exceldeathrecord.core;

/** Vanilla XP math. A player's totalExperience only counts XP picked
 *  up since their last death and ignores /xp levels, so the level is
 *  the reliable record of what they had. */
public final class XpMath {
    private XpMath() {}

    /** XP points needed to go from level 0 to the start of {@code level}. */
    public static int pointsForLevel(int level) {
        if (level <= 0) return 0;
        long l = level;
        if (level <= 16) return (int) (l * l + 6 * l);
        if (level <= 31) return (int) Math.round(2.5 * l * l - 40.5 * l + 360);
        return (int) Math.min(Integer.MAX_VALUE, Math.round(4.5 * l * l - 162.5 * l + 2220));
    }

    /** Points to give back for a death: at least the recorded level's worth. */
    public static int restorePoints(int xpLevel, int totalExperience) {
        return Math.max(Math.max(0, totalExperience), pointsForLevel(xpLevel));
    }
}
