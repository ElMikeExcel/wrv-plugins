/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The PER-PLAYER FIRST-BREAK book (ExcelBossTrial 0.5.6; Mike,
 * Nov 11, 2026, put-aside item 2): first-break EXTRAS — signature
 * loot (the base, non-reforged signature claims), the per-champion
 * trophy, and the Warden EliteMobs starter kit — pay the first
 * time THAT PLAYER personally breaks a seal: once ever per player
 * per seal, in any cycle. Veterans never re-earn them.
 *
 * Deliberately NOT in this book:
 * - party/server one-time elements (the guild-hall plaque record
 *   and the "Sealbreakers" party title) — those stay single
 *   historical events, guarded by the host's guild-once flag;
 * - the Raid Village Standard — its exception is untouched: it
 *   pays on the raid break event AND every repeat, exactly as
 *   before (the host may NOTE a raid personal first break here
 *   for the record, but nothing gates on it);
 * - cash — the cash taper (CashTaper) prices from lifetime clear
 *   counts, a separate ladder that also never resets.
 *
 * Pure logic; the adapter persists the snapshot in state.yml and
 * flushes BEFORE any extra moves (the house pay-order discipline:
 * a crash after the flush can underpay, never double-pay).
 * A seal cycle boundary NEVER clears this book.
 */
public final class FirstBreakBook {

    private final Set<String> paid = new LinkedHashSet<>();

    private static String key(String sealId, UUID player) {
        return sealId + "|" + player;
    }

    /** Has this player already earned this seal's extras? */
    public synchronized boolean isPaid(String sealId, UUID player) {
        return sealId != null && player != null && paid.contains(key(sealId, player));
    }

    /**
     * Mark this player's extras for this seal as paid.
     *
     * @return true when this call is the player's first mark
     *         (the extras are owed on this break); false when
     *         they were already a veteran of this seal.
     */
    public synchronized boolean markPaid(String sealId, UUID player) {
        if (sealId == null || player == null) {
            return false;
        }
        return paid.add(key(sealId, player));
    }

    /** The players who have earned one seal's extras. */
    public synchronized List<UUID> playersFor(String sealId) {
        List<UUID> out = new ArrayList<>();
        String prefix = sealId + "|";
        for (String k : paid) {
            if (k.startsWith(prefix)) {
                try {
                    out.add(UUID.fromString(k.substring(prefix.length())));
                } catch (IllegalArgumentException ignored) {
                    // a malformed key degrades to skipped
                }
            }
        }
        return out;
    }

    public synchronized int size() {
        return paid.size();
    }

    /** Snapshot: the flat key list ("sealId|uuid"), sorted for
     *  deterministic state files. */
    public synchronized List<String> snapshot() {
        List<String> out = new ArrayList<>(paid);
        java.util.Collections.sort(out);
        return out;
    }

    /** Restore from a snapshot list; malformed rows are skipped. */
    public synchronized void restore(List<String> snapshot) {
        paid.clear();
        if (snapshot == null) {
            return;
        }
        for (String k : snapshot) {
            if (k == null) {
                continue;
            }
            int bar = k.indexOf('|');
            if (bar <= 0 || bar == k.length() - 1) {
                continue;
            }
            try {
                UUID.fromString(k.substring(bar + 1));
                paid.add(k);
            } catch (IllegalArgumentException ignored) {
                // a malformed row degrades to skipped
            }
        }
    }
}
