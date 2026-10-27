package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Display-only latest-message arbitration. Never changes SP or stance state. */
public final class EarnedBarGate {
    public enum Result { STALE, EXPIRED, WAIT_STANCE, WAIT_CLEAR, WAIT_PARTY, PARTY_SKIP, SEND }
    private static final long TTL_MS = 60_000;
    private long sequence;
    private static final class Pending {
        long token, expires;
        int clearPolls;
        int partyWaits;
        Pending(long t, long now) { token = t; expires = now + TTL_MS; }
    }
    private final Map<UUID, Pending> pending = new HashMap<>();
    public long offer(UUID id, long now) {
        Pending next = new Pending(++sequence, now);
        pending.put(id, next);
        return next.token;
    }
    public Result decide(UUID id, long token, long now, boolean stanceBusyOrUnknown, boolean partyBusy) {
        Pending p = pending.get(id);
        if (p == null || p.token != token) return Result.STALE;
        if (now >= p.expires) { pending.remove(id); return Result.EXPIRED; }
        if (stanceBusyOrUnknown) { p.clearPolls = 0; return Result.WAIT_STANCE; }
        // Polls are spaced ten server ticks apart by the adapter. Five waits ensure
        // the installed Attunement's 20-tick expiry/clear writer has had its turn,
        // plus 30 further ticks of clear-message visibility, including under TPS lag.
        if (p.clearPolls++ < 5) return Result.WAIT_CLEAR;
        if (partyBusy) {
            if (p.partyWaits++ >= 3) { pending.remove(id); return Result.PARTY_SKIP; }
            return Result.WAIT_PARTY;
        }
        pending.remove(id);
        return Result.SEND;
    }
    public boolean isCurrent(UUID id, long token) { Pending p = pending.get(id); return p != null && p.token == token; }
    public void forget(UUID id) { pending.remove(id); }
    public void clear() { pending.clear(); }
}
