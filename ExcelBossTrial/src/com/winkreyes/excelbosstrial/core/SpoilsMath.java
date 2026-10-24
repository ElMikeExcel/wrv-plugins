package com.winkreyes.excelbosstrial.core;

/**
 * The SPOILS RULE math (Mike's idea, decided Oct 18, 2026): seal-boss
 * drops multiply by the number of participants in range at the death
 * (2 fighters = 2x, 3 = 3x, 4 = 4x), capped at 5x. Solo pays exactly
 * vanilla (1x). The dragon drops no items, so its XP scales instead
 * (12,000 first kill / 500 repeat). EliteMobs loot and raid drops are
 * excluded - they never pass through here.
 *
 * SPOILS FOLLOW THE VERDICT (Mike, Oct 19, 2026, from the seal
 * review): the multiplier applies ONLY on a kill that CREDITS. A
 * denied kill (share, presence, or cooldown) drops plain vanilla
 * loot / unscaled XP however many champions stood by - under the
 * original rule a refused wither kill with five champions standing
 * in range could still drop five nether stars. That gate is
 * {@link #multiplierFor}; the bare {@link #multiplier} champion
 * count math is unchanged.
 */
public final class SpoilsMath {

    public static final int DEFAULT_CAP = 5;
    public static final int DRAGON_XP_FIRST = 12000;
    public static final int DRAGON_XP_REPEAT = 500;

    private SpoilsMath() {
    }

    /** The drop multiplier for a present-participant count. */
    public static int multiplier(int presentParticipants, int cap) {
        int c = Math.max(1, cap);
        if (presentParticipants <= 1) {
            return 1;
        }
        return Math.min(presentParticipants, c);
    }

    /**
     * The multiplier a death actually pays, after the verdict gate:
     * spoils enabled AND verdict CREDIT pays the champion-count
     * multiplier; spoils off, or any denial (DENY_SHARE,
     * DENY_PRESENCE, DENY_COOLDOWN), pays exactly 1x. Elder kills
     * arrive here with their own verdict (a trio-progress kill
     * carries CREDIT), so each elder's drops follow its own fight.
     */
    public static int multiplierFor(boolean spoilsEnabled, CreditJudge.Verdict verdict,
                                    int presentParticipants, int cap) {
        if (!spoilsEnabled || verdict != CreditJudge.Verdict.CREDIT) {
            return 1;
        }
        return multiplier(presentParticipants, cap);
    }

    /** Extra copies of each drop to ADD for a multiplier (mult - 1 each). */
    public static int extraCopies(int multiplier) {
        return Math.max(0, multiplier - 1);
    }

    /** The dragon's total XP for a kill: base (first/repeat) x multiplier. */
    public static int dragonXp(boolean firstKill, int multiplier) {
        int base = firstKill ? DRAGON_XP_FIRST : DRAGON_XP_REPEAT;
        return base * Math.max(1, multiplier);
    }

    /** Extra XP to top up beyond what vanilla will drop on its own. */
    public static int dragonXpTopUp(boolean firstKill, int multiplier, int vanillaBase) {
        return Math.max(0, dragonXp(firstKill, multiplier) - Math.max(0, vanillaBase));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
