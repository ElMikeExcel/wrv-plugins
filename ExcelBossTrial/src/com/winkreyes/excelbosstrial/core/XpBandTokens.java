package com.winkreyes.excelbosstrial.core;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The XP band governor's verdict tokens (0.2.4, Mike's pick
 * Oct 22, 2026 - "Option B": hits BossTrial classifies as out-of-band
 * pay 25% mcMMO XP in real time; in-band hits pay full; no clawback).
 *
 * Mechanics: for every governed hit whose shape mcMMO pays combat XP
 * for (see XpBandPlant), the BossTrial damage listener plants one
 * token here, in hit order, stamped with the server tick of the hit
 * and the hit's band verdict (counted / not counted). The XP listener
 * (ExcelRewardGuard) polls once per McMMOPlayerXpGainEvent with
 * reason PVE for the SAME player; a poll consumes the oldest fresh
 * token and answers its verdict. mcMMO schedules the matching gain
 * about a tick after the hit (AwardCombatXpTask via FoliaLib
 * runAtEntity), and gains for one player fire in hit order, so FIFO
 * per player pairs every gain with its own hit's verdict.
 *
 * Fail-safe by construction: no token (feed off, listener late, TTL
 * lapsed) polls as null and the gain is paid in full - the pre-0.2.4
 * behavior. A token can never scale an unrelated later gain: it is
 * consumed on use and expires TTL_TICKS after its hit.
 *
 * Pure state (no Bukkit), so the pairing rules are unit-tested
 * directly. All access is synchronized: plants happen on the damage
 * event's region thread and polls on the gaining player's region
 * thread, which need not be the same thread on Folia-style
 * scheduling. Queues are tiny (a handful of in-flight hits), so the
 * lock is never contended in practice.
 */
public final class XpBandTokens {

    /**
     * A token planted at tick T may be consumed by a poll at any tick
     * up to and including T + TTL_TICKS. mcMMO's combat XP task runs
     * ~1 tick after the hit; 5 ticks (0.25 s) covers scheduling slack
     * while keeping any theoretical cross-contamination window far
     * below a human's fastest re-attack (a full-charge swing is
     * ~12 ticks), so a stale token can never outlive into a later,
     * unrelated hit of the same player.
     */
    public static final long TTL_TICKS = 5;

    /**
     * Hard cap on queued verdicts per player. Real in-flight depth is
     * 1-3; the cap exists so a pathological burst (or a listener that
     * stops polling mid-fight) can never grow memory unboundedly. The
     * OLDEST tokens drop first: they are the ones whose gains have
     * most likely already fired (or never will), and dropping them
     * only ever degrades toward full pay.
     */
    public static final int MAX_PENDING_PER_PLAYER = 64;

    private record Token(boolean counted, long tick) {
    }

    private final Map<UUID, Deque<Token>> queues = new HashMap<>();

    /** Plants one hit's verdict. Called once per governed, XP-paying hit, in hit order. */
    public synchronized void plant(UUID player, boolean counted, long tick) {
        Deque<Token> q = queues.computeIfAbsent(player, k -> new ArrayDeque<>());
        purgeExpired(q, tick);
        while (q.size() >= MAX_PENDING_PER_PLAYER) {
            q.pollFirst();
        }
        q.addLast(new Token(counted, tick));
    }

    /**
     * Consumes the oldest fresh verdict for the player, or null when
     * none applies (no token, or every queued token has expired).
     * Null = "no opinion" = pay in full.
     */
    public synchronized Boolean poll(UUID player, long tick) {
        Deque<Token> q = queues.get(player);
        if (q == null) {
            return null;
        }
        purgeExpired(q, tick);
        Token t = q.pollFirst();
        if (q.isEmpty()) {
            queues.remove(player);
        }
        return t == null ? null : t.counted();
    }

    /** Fresh (unexpired, unconsumed) verdicts currently queued for the player. Diagnostics. */
    public synchronized int pending(UUID player, long tick) {
        Deque<Token> q = queues.get(player);
        if (q == null) {
            return 0;
        }
        purgeExpired(q, tick);
        if (q.isEmpty()) {
            queues.remove(player);
            return 0;
        }
        return q.size();
    }

    /** Fresh verdicts queued across all players. Diagnostics (/bosstrial status). */
    public synchronized int totalPending(long tick) {
        int total = 0;
        var it = queues.entrySet().iterator();
        while (it.hasNext()) {
            Deque<Token> q = it.next().getValue();
            purgeExpired(q, tick);
            if (q.isEmpty()) {
                it.remove();
            } else {
                total += q.size();
            }
        }
        return total;
    }

    /** Drops one player's queue (logout / reset hygiene). */
    public synchronized void clear(UUID player) {
        queues.remove(player);
    }

    /** Drops every queue (plugin disable). */
    public synchronized void clearAll() {
        queues.clear();
    }

    private static void purgeExpired(Deque<Token> q, long now) {
        Token head;
        while ((head = q.peekFirst()) != null && now - head.tick() > TTL_TICKS) {
            q.pollFirst();
        }
    }
}

