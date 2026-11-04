package com.winkreyes.excelguildgate;

import java.util.Collection;

/**
 * Mike's raid floor (Oct 19, 2026, adopted from the seal review):
 * a raid credits its seal ONLY when at least one player stands
 * among the raid's winners at victory. Pure and Bukkit-free, in
 * the house pattern of CatchUpTracker / NarratorPackPrompt; the
 * Bukkit adapter passes the RaidFinishEvent's winners list in
 * as an opaque collection.
 *
 * What "winners" means, verified against the Paper 26.2-123
 * server jar (net.minecraft.world.entity.raid.Raid, victory
 * path): when a raid is won, the game walks its
 * heroesOfTheVillage set and the winners list handed to
 * RaidFinishEvent is exactly the Bukkit players for the heroes
 * who are live, non-spectator players in the world at that
 * moment - the same people vanilla crowns with Hero of the
 * Village. An entity only enters heroesOfTheVillage when a
 * PLAYER kills a raider (the registration call site checks the
 * killer's type is PLAYER), so a raid cleared by iron golems,
 * traps, or an AFK kill chamber crowns nobody and its winners
 * list is empty.
 *
 * The floor is deliberately nothing stricter than Mike's words:
 * one player among the winners. There is no damage-share test
 * here - Mike excluded the raid from ExcelBossTrial's share
 * system on his word, and this check lives inside GuildGate's
 * existing victory rule.
 *
 * Mike's second raid floor (Oct 20, 2026, after the first seal
 * fell to an easiest-tier raid): the victory must come under
 * the strongest omen - Bad Omen level {@link #REQUIRED_OMEN_LEVEL},
 * the vanilla cap (Raid.getMaxRaidOmenLevel() is the constant 5
 * in the 26.2 jar; the level is set when the raid absorbs the
 * triggering player's Raid Omen effect and is read back off the
 * same Raid object at finish via Raid#getBadOmenLevel()). The
 * omen floor is judged AFTER the winner floor, so a raid with a
 * real winner and a weak omen hears the omen reason - the true
 * one - and a raid with no winner still hears the winner reason,
 * exactly as before. The requirement arrives as an input (the
 * Bukkit adapter reads it from config with this constant as the
 * code fallback), so the whole matrix stays unit-testable here.
 *
 * Stateless by design: a denied raid changes nothing (no credit,
 * no cooldown anywhere), so the next honestly won raid decides
 * on its own winners alone.
 */
public final class RaidWinnerCheck {

    /**
     * The Bad Omen level the raid seal requires: the strongest
     * omen vanilla has. In the 26.2 jar getMaxRaidOmenLevel() is
     * the constant 5, so no raid can ever carry more; a config
     * override above this simply makes the seal unearnable until
     * the number is sane again (the matrix tests pin that shape).
     */
    public static final int REQUIRED_OMEN_LEVEL = 5;

    /** The credit decision for a finished raid. */
    public enum Decision {
        /** A player among the winners, under a strong-enough omen: the seal credits. */
        CREDIT,
        /** Nobody crowned: the seal does not break. */
        NO_PLAYER_WINNER,
        /** A real winner, but the omen behind the raid was too weak: the seal does not break. */
        OMEN_TOO_LOW
    }

    private RaidWinnerCheck() {
    }

    /**
     * True when the winners collection names at least one winner.
     * A null collection, an empty one, or one holding only nulls
     * all read as "no player among the winners": the check must
     * never throw on a thin event and never credit on a guess.
     */
    public static boolean anyPlayerWinner(Collection<?> winners) {
        if (winners == null) {
            return false;
        }
        for (Object winner : winners) {
            if (winner != null) {
                return true;
            }
        }
        return false;
    }

    /** The full decision for one raid's winners list. */
    public static Decision decide(Collection<?> winners) {
        return anyPlayerWinner(winners) ? Decision.CREDIT : Decision.NO_PLAYER_WINNER;
    }

    /**
     * The full decision for one finished raid: the winner floor
     * first (an uncrowned raid is NO_PLAYER_WINNER whatever its
     * omen was), then the omen floor. A badOmenLevel below
     * requiredOmenLevel - including a nonsense negative read -
     * is OMEN_TOO_LOW: the check must never credit on a guess.
     */
    public static Decision decide(Collection<?> winners, int badOmenLevel, int requiredOmenLevel) {
        if (!anyPlayerWinner(winners)) {
            return Decision.NO_PLAYER_WINNER;
        }
        if (badOmenLevel < requiredOmenLevel) {
            return Decision.OMEN_TOO_LOW;
        }
        return Decision.CREDIT;
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.
