package com.winkreyes.exceltrainingwheels.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The iron-door siege state machine, pure (the glue feeds it one
 * observation per cadence per (door, zombie) pair and renders the
 * effects: cracks, sounds, the break itself).
 *
 * Rules (Mike's design, Oct 19, 2026):
 *  - A siege exists per DOOR, credited to ONE zombie ("fastest
 *    single zombie counts") — a second zombie at the same door is
 *    IGNORED while the claim stands; progress never stacks.
 *  - Progress accrues one step per qualifying observation and
 *    completes at thresholdSteps.
 *  - Progress RESETS fully when: the credited zombie's target
 *    changes, the target becomes protected, or observations stop
 *    for longer than the grace window (target died / logged off /
 *    zombie died / zombie lost the door) — the glue's sweep()
 *    performs those resets.
 *  - A protected target never starts a siege and stops one cold.
 *  - Concurrent sieges are hard-capped (maxSieges).
 */
public final class SiegeBook {

    public enum Event { STARTED, PROGRESS, COMPLETED, RESET, IGNORED }

    private static final class Siege {
        final UUID besieger;
        final UUID target;
        long steps;
        long lastSeenTick;

        Siege(UUID besieger, UUID target, long nowTick) {
            this.besieger = besieger;
            this.target = target;
            this.steps = 0;
            this.lastSeenTick = nowTick;
        }
    }

    private final int maxSieges;
    private final long thresholdSteps;
    private final long graceTicks;
    private final Map<String, Siege> sieges = new LinkedHashMap<>();

    public SiegeBook(int maxSieges, long thresholdSteps, long graceTicks) {
        if (maxSieges < 1) throw new IllegalArgumentException("maxSieges < 1");
        if (thresholdSteps < 1) throw new IllegalArgumentException("thresholdSteps < 1");
        if (graceTicks < 0) throw new IllegalArgumentException("graceTicks < 0");
        this.maxSieges = maxSieges;
        this.thresholdSteps = thresholdSteps;
        this.graceTicks = graceTicks;
    }

    /**
     * One cadence observation: "this zombie is at this door, path-
     * blocked, hunting this (un)protected player right now."
     */
    public Event observe(String doorKey, UUID besieger, UUID target,
                         boolean targetProtected, long nowTick) {
        if (doorKey == null || besieger == null || target == null) return Event.IGNORED;
        Siege s = sieges.get(doorKey);
        if (targetProtected) {
            // Never starts; if this very target was under siege, stops cold.
            if (s != null && s.target.equals(target)) {
                sieges.remove(doorKey);
                return Event.RESET;
            }
            return Event.IGNORED;
        }
        if (s != null) {
            if (!s.besieger.equals(besieger)) return Event.IGNORED; // no stacking
            if (!s.target.equals(target)) {
                sieges.remove(doorKey);
                return Event.RESET; // hunt changed mid-siege: full reset
            }
            s.steps++;
            s.lastSeenTick = nowTick;
            if (s.steps >= thresholdSteps) {
                sieges.remove(doorKey);
                return Event.COMPLETED;
            }
            return Event.PROGRESS;
        }
        if (sieges.size() >= maxSieges) return Event.IGNORED; // hard cap
        Siege fresh = new Siege(besieger, target, nowTick);
        fresh.steps = 1;
        if (fresh.steps >= thresholdSteps) return Event.COMPLETED;
        sieges.put(doorKey, fresh);
        return Event.STARTED;
    }

    /**
     * Reset every siege whose credited zombie stopped observing
     * (died, lost the door, target gone). Returns the door keys
     * that were reset so the glue can clear their crack overlays.
     */
    public List<String> sweep(long nowTick) {
        List<String> reset = new ArrayList<>();
        Iterator<Map.Entry<String, Siege>> it = sieges.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Siege> e = it.next();
            if (nowTick - e.getValue().lastSeenTick > graceTicks) {
                it.remove();
                reset.add(e.getKey());
            }
        }
        return reset;
    }

    public int size() {
        return sieges.size();
    }

    public boolean active(String doorKey) {
        return sieges.containsKey(doorKey);
    }

    public long steps(String doorKey) {
        Siege s = sieges.get(doorKey);
        return s == null ? 0 : s.steps;
    }

    /** 0..1 progress for crack rendering; 0 when no siege stands. */
    public double progress(String doorKey) {
        Siege s = sieges.get(doorKey);
        if (s == null) return 0.0;
        return Math.min(1.0, (double) s.steps / (double) thresholdSteps);
    }
}
