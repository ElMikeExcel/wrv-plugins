package com.winkreyes.guildgate.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * ExcelGuildGate 0.4.0 (RAID-COMPANION-SPEC sections 1.3 + 3.3):
 * fired when the raid seal BREAKS - the first qualifying victory -
 * after the credit and the repeat bookkeeping (window started,
 * winners seeded at one clear) are already persisted. It exists
 * because Mike settled the raid signature as a MINTED item paid by
 * the reward layer on the first break as well as on repeats
 * (spec section 3.3, option B): GuildGate credits seals and pays
 * nothing, so the first break announces itself here, in parallel
 * with {@link RaidRepeatEvent}'s announcement of a repeat.
 *
 * Not cancellable: the seal has fallen when this fires.
 */
public final class RaidBreakEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String sealId;
    private final List<UUID> winners;
    private final Map<UUID, String> winnerNames;
    private final int omenLevel;
    private final long timestampMs;

    public RaidBreakEvent(String sealId, List<UUID> winners,
                          Map<UUID, String> winnerNames, int omenLevel,
                          long timestampMs) {
        this.sealId = sealId;
        this.winners = winners == null ? List.of() : List.copyOf(winners);
        this.winnerNames = winnerNames == null ? Map.of() : Map.copyOf(winnerNames);
        this.omenLevel = omenLevel;
        this.timestampMs = timestampMs;
    }

    /** The seal that broke (always "raid" today). */
    public String sealId() {
        return sealId;
    }

    /** The raid's winners - vanilla's crowned heroes, the break's champions. */
    public List<UUID> winners() {
        return winners;
    }

    /** Winner display names by UUID, for reward provenance stamps. */
    public Map<UUID, String> winnerNames() {
        return winnerNames;
    }

    /** The Bad Omen level the winning raid carried. */
    public int omenLevel() {
        return omenLevel;
    }

    /** When the seal broke (epoch ms). */
    public long timestampMs() {
        return timestampMs;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

// ExcelGuildGate - a Wink Reyes Valley Excel plugin.
