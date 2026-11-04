package com.winkreyes.guildgate.api;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * ExcelGuildGate 0.4.0 (RAID-COMPANION-SPEC section 1.3): fired
 * AFTER a repeat raid clear has been judged and its bookkeeping
 * PERSISTED - the repeat window has already restarted and the
 * winners' clear counts have already risen in state.yml when this
 * event reaches a listener. GuildGate never pays rewards; a
 * listener (ExcelBossTrial's raid reward adapter) hears this and
 * pays the repeat slate through its own claims path.
 *
 * The pay-order rule is the point of the ordering: a crash between
 * the ruling and the rewards can lose a payout, never double one,
 * because any replay finds the window already running and judges
 * DENY_REPEAT_WINDOW instead.
 *
 * Not cancellable by design: the ruling is final when this fires.
 * A listener that cannot pay (its own dependency absent, a full
 * claims ledger) logs and skips its own rows; the credit stands.
 */
public final class RaidRepeatEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();

    private final String sealId;
    private final List<UUID> winners;
    private final Map<UUID, String> winnerNames;
    private final int omenLevel;
    private final Map<UUID, Integer> clearCounts;
    private final long windowResetMs;
    private final long timestampMs;

    public RaidRepeatEvent(String sealId, List<UUID> winners,
                           Map<UUID, String> winnerNames, int omenLevel,
                           Map<UUID, Integer> clearCounts,
                           long windowResetMs, long timestampMs) {
        this.sealId = sealId;
        this.winners = winners == null ? List.of() : List.copyOf(winners);
        this.winnerNames = winnerNames == null ? Map.of() : Map.copyOf(winnerNames);
        this.omenLevel = omenLevel;
        this.clearCounts = clearCounts == null ? Map.of() : Map.copyOf(clearCounts);
        this.windowResetMs = windowResetMs;
        this.timestampMs = timestampMs;
    }

    /** The seal the repeat belongs to (always "raid" today). */
    public String sealId() {
        return sealId;
    }

    /** The raid's winners - vanilla's crowned heroes, the repeat's champions. */
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

    /**
     * Every winner's clear count AFTER this clear was recorded
     * (a winner absent from the map has no recorded clear).
     * LinkedHashMap copy: iteration order is the winners' order.
     */
    public Map<UUID, Integer> clearCounts() {
        return new LinkedHashMap<>(clearCounts);
    }

    /** The instant the restarted repeat window ends (epoch ms). */
    public long windowResetMs() {
        return windowResetMs;
    }

    /** When the repeat was judged (epoch ms). */
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
