/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.IntSupplier;

/**
 * Per-encounter taunt state (ExcelBossTrial 0.4.0 rebuild;
 * Mike's request, Nov 8, 2026), the SprinkleBook shape: one
 * small state record per open boss, pruned with the
 * encounter map. The book is the rate limiter and the line
 * picker; the ADAPTER owns the truth rule (it calls
 * {@link #onEat} only when an eat actually happened - the
 * boss's WITHER-reason kill-heal), so nothing in here can
 * make the boss speak decoratively.
 *
 * The speaking rule: the FIRST eat of a fight always
 * speaks. After that, an eat speaks only when at least the
 * cooldown has passed since the last spoken line; eats in
 * between are counted and swallowed. The line is drawn
 * from the chapter's list through the caller's picker and
 * never repeats the immediately previous line when the
 * list has more than one entry (a colliding draw advances
 * by one) - the fight hears variety, not a loop.
 */
public final class TauntBook {

    /** One boss's taunt state for one encounter. */
    private static final class State {
        /** Whether any line has been spoken this fight. */
        boolean spoken;
        /** When the last line was spoken (the cooldown clock). */
        long lastSpokeMs;
        /** The last line's index (-1 = none yet). */
        int lastLineIndex = -1;
        /** Every eat observed, spoken or swallowed. */
        int eats;
    }

    private final Map<UUID, State> states = new HashMap<>();

    /**
     * Record one eat for a boss and ask whether it speaks.
     * Returns the line to say, or empty when the cooldown
     * swallows this eat (or the chapter has no lines).
     * {@code picker} draws the candidate line index; it is
     * only consulted when a line will actually be spoken.
     */
    public synchronized Optional<String> onEat(UUID bossUuid, long nowMs,
                                               List<String> lines,
                                               long cooldownMs,
                                               IntSupplier picker) {
        State st = states.computeIfAbsent(bossUuid, k -> new State());
        st.eats++;
        if (lines == null || lines.isEmpty()) {
            return Optional.empty();
        }
        if (st.spoken && nowMs - st.lastSpokeMs < Math.max(0L, cooldownMs)) {
            return Optional.empty();
        }
        int idx = Math.floorMod(picker.getAsInt(), lines.size());
        if (lines.size() > 1 && idx == st.lastLineIndex) {
            idx = (idx + 1) % lines.size();
        }
        st.spoken = true;
        st.lastSpokeMs = nowMs;
        st.lastLineIndex = idx;
        return Optional.of(lines.get(idx));
    }

    /** Every eat observed for a boss, spoken or swallowed. */
    public synchronized int eats(UUID bossUuid) {
        State st = states.get(bossUuid);
        return st == null ? 0 : st.eats;
    }

    /** Forget one boss (its encounter closed). */
    public synchronized void close(UUID bossUuid) {
        states.remove(bossUuid);
    }

    /** Forget every boss not in the live set (the encounter
     *  map's prune sweep - state never outlives its fight). */
    public synchronized void retainOnly(Set<UUID> liveBosses) {
        states.keySet().retainAll(liveBosses);
    }

    public synchronized boolean isEmpty() {
        return states.isEmpty();
    }
}
