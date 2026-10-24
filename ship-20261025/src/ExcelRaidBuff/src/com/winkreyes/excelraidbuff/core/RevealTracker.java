package com.winkreyes.excelraidbuff.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The stuck-wave reveal's per-raid stall clocks (0.1.1, Mike's
 * pick Oct 23, 2026) — Bukkit-free so the state machine is
 * unit-testable. Time is injected by the adapter (milliseconds).
 *
 * A raid makes PROGRESS when any of these happen: a raid member
 * dies, a raid member takes damage, a raid member deals damage,
 * or a new wave begins. The adapter routes every one of those
 * signals to {@link #onProgress}. When a raid with living
 * members has made no progress for the configured quiet span,
 * {@link #evaluate} flips it to REVEALED; the adapter glows the
 * members and owns the effect bookkeeping (this class tracks
 * no entities). Any progress signal, or the raid ending,
 * un-flips it.
 *
 * The one announce line per raid is tracked here too: owed
 * from the raid's first sighting until the adapter marks it
 * sent on the raid's first activation.
 */
public final class RevealTracker {

    /** What one sweep evaluation decided for a raid. */
    public enum Activation { NONE, ACTIVATED, STILL_REVEALED }

    private static final class RaidState {
        long lastProgressMs;
        boolean revealed;
        boolean announced;

        RaidState(long nowMs) {
            this.lastProgressMs = nowMs;
        }
    }

    private final Map<Integer, RaidState> raids = new HashMap<>();

    private RaidState state(int raidKey, long nowMs) {
        return raids.computeIfAbsent(raidKey, k -> new RaidState(nowMs));
    }

    /** A raid was sighted (wave spawn, sweep). Starts its clock if new. */
    public void onRaidSeen(int raidKey, long nowMs) {
        state(raidKey, nowMs);
    }

    /**
     * A progress signal for this raid: its stall clock resets,
     * and a live reveal ends.
     *
     * @return true when the raid WAS revealed and the signal
     *         cleared it — the adapter clears the glow it applied
     */
    public boolean onProgress(int raidKey, long nowMs) {
        RaidState s = state(raidKey, nowMs);
        s.lastProgressMs = nowMs;
        boolean was = s.revealed;
        s.revealed = false;
        return was;
    }

    /**
     * One sweep's verdict for a raid. A raid with no living
     * members never reveals (nothing to hunt). The stall span
     * is measured from the last progress signal — or from the
     * raid's first sighting when it has never made progress.
     */
    public Activation evaluate(int raidKey, long nowMs, long quietMs, boolean hasLivingMembers) {
        RaidState s = state(raidKey, nowMs);
        if (!hasLivingMembers) return Activation.NONE;
        if (nowMs - s.lastProgressMs < quietMs) return Activation.NONE;
        if (s.revealed) return Activation.STILL_REVEALED;
        s.revealed = true;
        return Activation.ACTIVATED;
    }

    public boolean isRevealed(int raidKey) {
        RaidState s = raids.get(raidKey);
        return s != null && s.revealed;
    }

    /** The one announce line is still owed for this raid. */
    public boolean announceOwed(int raidKey) {
        RaidState s = raids.get(raidKey);
        return s != null && !s.announced;
    }

    /** The adapter sent the line (or the raid's first activation passed with the line off). */
    public void markAnnounced(int raidKey) {
        RaidState s = raids.get(raidKey);
        if (s != null) s.announced = true;
    }

    /**
     * The raid ended (finish / stop / no longer active): drop
     * its state.
     *
     * @return true when it was revealed — the adapter clears glow
     */
    public boolean onRaidEnd(int raidKey) {
        RaidState s = raids.remove(raidKey);
        return s != null && s.revealed;
    }

    /**
     * Drop every raid not in the active set (raids that ended
     * without reaching the finish/stop handlers).
     *
     * @return the dropped keys that were revealed
     */
    public Set<Integer> retainOnly(Set<Integer> activeKeys) {
        Set<Integer> cleared = new HashSet<>();
        raids.keySet().removeIf(key -> {
            if (activeKeys.contains(key)) return false;
            if (raids.get(key).revealed) cleared.add(key);
            return true;
        });
        return cleared;
    }

    /**
     * Full reset (plugin disable, or the feature toggled off in
     * a reload): drop all state.
     *
     * @return the keys that were revealed
     */
    public Set<Integer> reset() {
        Set<Integer> cleared = new HashSet<>();
        for (Map.Entry<Integer, RaidState> e : raids.entrySet()) {
            if (e.getValue().revealed) cleared.add(e.getKey());
        }
        raids.clear();
        return cleared;
    }

    /** Raids currently tracked (diagnostics / tests). */
    public int trackedRaids() {
        return raids.size();
    }
}
