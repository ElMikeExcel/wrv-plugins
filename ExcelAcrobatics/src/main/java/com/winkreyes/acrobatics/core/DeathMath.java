package com.winkreyes.acrobatics.core;

/** Death-fall math: the vanilla XP a death actually costs, and the
 * Acrobatics award for a fatal fall.
 *
 * Mike's gate (Oct 14, 2026): a fatal fall pays Acrobatics XP only when
 * the death costs the player a decent amount of normal Minecraft XP, so
 * suicide-farming with empty XP bars pays nothing and every paid death
 * costs more real XP-earning time than the Acrobatics XP is worth. */
public final class DeathMath {

    private DeathMath() {}

    /** Total experience points represented by a whole level (vanilla curve). */
    public static int totalXpForLevel(int level) {
        if (level <= 0) {
            return 0;
        }
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    /** Points needed to go from this level to the next (the XP bar size). */
    public static int xpBarCapacity(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    /** The player's total XP points right now (levels plus bar progress). */
    public static int totalXp(int level, float progress) {
        return totalXpForLevel(level) + Math.round(xpBarCapacity(level) * Math.max(0, Math.min(1, progress)));
    }

    /** What a death costs: everything the player had, minus the orbs that
     * drop and can be recovered by running back. */
    public static int xpLostOnDeath(int level, float progress, int droppedExp) {
        return Math.max(0, totalXp(level, progress) - Math.max(0, droppedExp));
    }

    /** The death gate (Mike's rule, clarified Oct 14, 2026): the ONLY
     * condition is that the death actually cost a decent amount of
     * vanilla XP. Level on its own is not gated; a near-empty bar loses
     * almost nothing past the recoverable orbs and cannot pass, and a
     * keepInventory world where nothing is lost cannot pass either. */
    public static boolean gatePasses(int xpLost, int minXpLost) {
        return xpLost >= minXpLost;
    }

    /** Acrobatics award for a fatal fall. The basis is the fall's FINAL
     * damage (already reduced by armor and Feather Falling in the damage
     * event, so no second reduction is applied here); the Feather Falling
     * XP compensation mirrors mcMMO's, as in the water path. No style
     * bonus: style pays for sticking the landing. */
    public static int deathAwardXp(double finalDamage, int featherFallLevel,
                                   double xpPerDamagePoint, double featherFallXpMultiplier,
                                   int maxXpPerLanding) {
        if (finalDamage <= 0) {
            return 0;
        }
        double xp = finalDamage * xpPerDamagePoint;
        if (featherFallLevel > 0) {
            xp *= featherFallXpMultiplier;
        }
        long rounded = Math.round(xp);
        return (int) Math.max(0, Math.min(maxXpPerLanding, rounded));
    }
}
