/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The per-player damage-feedback opt-outs (ExcelBossTrial 0.3.6):
 * who has run {@code /bosstrial feedback off}. An opted-out
 * player gets no action-bar damage totals and is removed from
 * every BossTrial-owned boss bar audience. Everyone else -
 * the default - sees the feedback.
 *
 * Pure set + persistence shape: the adapter persists
 * {@link #snapshot()} as a string list in state.yml (beside the
 * cooldowns, the AtomicStateFile pattern) and restores it at
 * load. Malformed rows degrade to skipped, never a failed load.
 */
public final class FeedbackOptOuts {

    private final Set<UUID> optedOut = new LinkedHashSet<>();

    /** True when this player has opted out of damage feedback. */
    public synchronized boolean isOptedOut(UUID player) {
        return player != null && optedOut.contains(player);
    }

    /**
     * Set a player's opt-out state.
     *
     * @return true when the state changed (the adapter saves
     *         state only on a change)
     */
    public synchronized boolean setOptedOut(UUID player, boolean optOut) {
        if (player == null) {
            return false;
        }
        return optOut ? optedOut.add(player) : optedOut.remove(player);
    }

    /** How many players are currently opted out. */
    public synchronized int size() {
        return optedOut.size();
    }

    /** The persisted shape: UUID strings, insertion order. */
    public synchronized List<String> snapshot() {
        List<String> out = new ArrayList<>();
        for (UUID id : optedOut) {
            out.add(id.toString());
        }
        return out;
    }

    /** Restore from the persisted shape; junk rows are skipped. */
    public synchronized void restore(List<String> rows) {
        optedOut.clear();
        if (rows == null) {
            return;
        }
        for (String row : rows) {
            if (row == null) {
                continue;
            }
            try {
                optedOut.add(UUID.fromString(row.strip()));
            } catch (IllegalArgumentException ignored) {
                // a malformed uuid row degrades to skipped
            }
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
