/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The SEAL-CYCLE book (ExcelBossTrial 0.5.6; seal reset law,
 * Mike, Nov 10, 2026 + decision-log D7; design of record
 * ~/workspace/sealcycle-20261110/DESIGN-AND-BUILDSCOPE.md
 * section 3.2, rules C-1..C-3 as they apply to BossTrial's own
 * books):
 *
 * - a BROKEN seal stays broken: the lifetime broken set is
 *   history and is never cleared by a cycle boundary;
 * - the guild CYCLE count resets ONLY at 5/5: when a credit
 *   completes the required count, the completed cycle is
 *   archived, the cycle number increments, and the new cycle
 *   starts at 0 of 5 — every seal, including the completing
 *   one, starts unbroken FOR CYCLE PURPOSES;
 * - stage 5 / the Elite door, once unlocked at the first 5/5,
 *   STAYS OPEN permanently: {@code guildOpened} is a latch —
 *   set at the first completion, never cleared, and
 *   {@link #eliteDoorOpen()} reads the latch, never the
 *   current cycle count alone.
 *
 * This is BossTrial's mirror of the cycle state: GuildGate owns
 * the guild seal record itself, and BossTrial re-keys its own
 * books (credited-seal fallback + repeat windows) off the
 * boundary this book detects. Lifetime clear counts (the cash
 * taper ladder) and the per-player first-break book are NOT
 * cycle state and are never touched here.
 *
 * Pure logic; the adapter persists the snapshot in state.yml.
 */
public final class SealCycle {

    public enum CreditResult {
        /** First credit of this seal in the current cycle; the
         *  cycle is not complete. */
        CREDITED,
        /** The seal already stood credited in this cycle. */
        ALREADY_IN_CYCLE,
        /** This credit completed the cycle: the latch is set,
         *  the cycle is archived, and a new cycle has begun. */
        CYCLE_COMPLETED
    }

    private final int requiredCount;
    private int cycleNumber = 1;
    private boolean guildOpened;
    private long openedTimestampMs;
    private final Set<String> lifetimeBroken = new LinkedHashSet<>();
    private final Set<String> cycleBroken = new LinkedHashSet<>();
    private final List<String> history = new ArrayList<>();

    public SealCycle(int requiredCount) {
        this.requiredCount = Math.max(1, requiredCount);
    }

    public synchronized int requiredCount() {
        return requiredCount;
    }

    public synchronized int cycleNumber() {
        return cycleNumber;
    }

    /** The P-2 latch: true from the first 5/5 onward, forever. */
    public synchronized boolean guildOpened() {
        return guildOpened;
    }

    public synchronized long openedTimestampMs() {
        return openedTimestampMs;
    }

    /** Stage 5 / the Elite door: open once the latch is set, and
     *  open while the current cycle stands complete — which, by
     *  construction, is only the instant before the boundary
     *  rolls the cycle, so in practice this IS the latch. */
    public synchronized boolean eliteDoorOpen() {
        return guildOpened || cycleBroken.size() >= requiredCount;
    }

    /** Lifetime reading: has this seal EVER been broken? */
    public synchronized boolean isBrokenLifetime(String sealId) {
        return lifetimeBroken.contains(sealId);
    }

    /** Cycle reading: does this seal stand broken in the
     *  CURRENT cycle? */
    public synchronized boolean isBrokenInCycle(String sealId) {
        return cycleBroken.contains(sealId);
    }

    /** The current cycle's progress, 0..requiredCount. */
    public synchronized int cycleCount() {
        return cycleBroken.size();
    }

    public synchronized int lifetimeCount() {
        return lifetimeBroken.size();
    }

    public synchronized List<String> history() {
        return List.copyOf(history);
    }

    /**
     * Credit one seal's break at {@code nowMs}. A break of a
     * seal already credited in this cycle changes nothing. The
     * completing credit archives the cycle and opens the next
     * one in the same call (the completing seal belongs to the
     * COMPLETED cycle's archive, per design rule C-3(v)).
     */
    public synchronized CreditResult credit(String sealId, long nowMs) {
        if (sealId == null || sealId.isBlank()) {
            return CreditResult.ALREADY_IN_CYCLE;
        }
        lifetimeBroken.add(sealId);
        if (!cycleBroken.add(sealId)) {
            return CreditResult.ALREADY_IN_CYCLE;
        }
        if (cycleBroken.size() < requiredCount) {
            return CreditResult.CREDITED;
        }
        // The boundary (C-3): latch, archive, increment.
        if (!guildOpened) {
            guildOpened = true;
            openedTimestampMs = nowMs;
        }
        history.add("cycle " + cycleNumber + " completed at " + nowMs
                + " by " + sealId + " (seals: " + String.join(",", cycleBroken) + ")");
        cycleNumber++;
        cycleBroken.clear();
        return CreditResult.CYCLE_COMPLETED;
    }

    /** Snapshot for state.yml (plain maps/lists only). */
    public synchronized Map<String, Object> snapshot() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cycleNumber", cycleNumber);
        m.put("guildOpened", guildOpened);
        m.put("openedTimestampMs", openedTimestampMs);
        m.put("lifetimeBroken", new ArrayList<>(lifetimeBroken));
        m.put("cycleBroken", new ArrayList<>(cycleBroken));
        m.put("history", new ArrayList<>(history));
        return m;
    }

    /** Restore; absent keys migrate to cycle 1, latch unset —
     *  the pre-cycle state of record (design section 3.5). */
    @SuppressWarnings("unchecked")
    public synchronized void restore(Map<String, Object> snapshot) {
        cycleNumber = 1;
        guildOpened = false;
        openedTimestampMs = 0L;
        lifetimeBroken.clear();
        cycleBroken.clear();
        history.clear();
        if (snapshot == null) {
            return;
        }
        if (snapshot.get("cycleNumber") instanceof Number n) {
            cycleNumber = Math.max(1, n.intValue());
        }
        if (snapshot.get("guildOpened") instanceof Boolean b) {
            guildOpened = b;
        }
        if (snapshot.get("openedTimestampMs") instanceof Number n) {
            openedTimestampMs = n.longValue();
        }
        restoreSet(lifetimeBroken, snapshot.get("lifetimeBroken"));
        restoreSet(cycleBroken, snapshot.get("cycleBroken"));
        if (snapshot.get("history") instanceof List<?> list) {
            for (Object o : list) {
                if (o != null) {
                    history.add(String.valueOf(o));
                }
            }
        }
    }

    private static void restoreSet(Set<String> target, Object raw) {
        if (raw instanceof List<?> list) {
            for (Object o : list) {
                if (o != null && !String.valueOf(o).isBlank()) {
                    target.add(String.valueOf(o));
                }
            }
        }
    }
}
