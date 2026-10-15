package com.winkreyes.leaderboard.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The Build Mode adjustment ledger state machine.
 *
 * The paper layer watches ExcelBuildMode's active-index directory: while a
 * player's session file exists, a session is active. When a session starts,
 * the player's current mined total is remembered; when it ends, the mined
 * delta is folded into the player's permanent adjustment, which the Miner
 * board subtracts. Persistence format is one "uuid=long" per line so the
 * whole machine is testable off-server. */
public final class AdjustmentLedger {

    private final Map<UUID, Long> adjustments = new HashMap<>();
    private final Map<UUID, Long> sessionStarts = new HashMap<>();

    public synchronized void noteSessionStart(UUID player, long minedNow) {
        sessionStarts.putIfAbsent(player, minedNow);
    }

    public synchronized void noteSessionEnd(UUID player, long minedNow) {
        Long start = sessionStarts.remove(player);
        if (start == null) {
            return; // End without a start (e.g. leaderboard loaded mid-session): no adjustment.
        }
        long delta = BoardMath.sessionDelta(start, minedNow);
        if (delta > 0) {
            adjustments.merge(player, delta, Long::sum);
        }
    }

    public synchronized boolean inSession(UUID player) {
        return sessionStarts.containsKey(player);
    }

    public synchronized long adjustmentFor(UUID player) {
        return adjustments.getOrDefault(player, 0L);
    }

    public synchronized Map<UUID, Long> adjustmentsSnapshot() {
        return Map.copyOf(adjustments);
    }

    public synchronized String serialize() {
        StringBuilder out = new StringBuilder();
        for (Map.Entry<UUID, Long> e : adjustments.entrySet()) {
            out.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        return out.toString();
    }

    public static AdjustmentLedger deserialize(String text) {
        AdjustmentLedger ledger = new AdjustmentLedger();
        if (text == null) {
            return ledger;
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int eq = trimmed.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            try {
                UUID id = UUID.fromString(trimmed.substring(0, eq).trim());
                long value = Long.parseLong(trimmed.substring(eq + 1).trim());
                if (value > 0) {
                    ledger.adjustments.put(id, value);
                }
            } catch (IllegalArgumentException ignored) {
                // A corrupt line never blocks the board; it is skipped.
            }
        }
        return ledger;
    }
}
