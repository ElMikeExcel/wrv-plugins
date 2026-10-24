package com.winkreyes.excelguide.core;

import java.util.UUID;

/**
 * Read-only personal-data source for the Guide (SPEC 5.1). The Bukkit
 * glue implements this against the live plugins (mcMMO, Jobs, Vault,
 * Bukkit statistics, GuildGateAPI, Magic); the unit tests implement it
 * with a fake. Every summary is one chat line with no persona prefix
 * (the engine adds routing, the glue adds the prefix). A null return
 * means "unavailable right now" - the engine answers "I can't see that
 * right now" rather than guessing (SPEC 5.1).
 */
public interface StatsView {

    /** e.g. "mcMMO: Mining 142, Acrobatics 88 - overall 861". */
    String mcMMOSummary(UUID player);

    /** One named skill, e.g. "Your Mining level is 142 (mcMMO)." Null if the skill is unknown. */
    String mcMMOSkill(UUID player, String skillToken);

    /** e.g. "Jobs: Miner level 12, Farmer level 5". */
    String jobsSummary(UUID player);

    /** e.g. "You've played 23 h 40 m on this server." */
    String playtimeSummary(UUID player);

    /** e.g. "Your balance is $1,234.56." */
    String moneySummary(UUID player);

    /**
     * The player's GriefPrevention claim-block numbers (0.2.2):
     * {accrued, bonus, used, remaining}, read live from GP. Null
     * means GP data is unavailable - the engine then serves the
     * generic claim-amounts answer rather than guessing a number.
     * Default null so sources without GP simply don't offer it.
     */
    default long[] claimBlocks(UUID player) {
        return null;
    }

    /**
     * The player's Magic standing. Only called when sealsBroken() is
     * at least 1 (before that the engine serves the lore line). Null if
     * Magic data is unavailable.
     */
    String magicStanding(UUID player);

    /** Guild campaign state (GuildGateAPI on live; faked in tests). */
    int sealsBroken();

    int sealsTotal();
}
