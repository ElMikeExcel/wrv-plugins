package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 0.3.5: Arrange mode's intent interpreter (design 2.3). Pure logic, no Bukkit.
 *
 * One interpreter, two input dialects: Java's real drag events and Bedrock's
 * tap-tap (pickup commit, then place commit) are both translated by the hook
 * into the same primitive inputs here, so the two platforms can never grow
 * different arrange rules:
 *
 *   pickup(spell, fromLoadoutSlot)  the player lifted a bar icon
 *   place(target)                   the player put it down: a bar slot, outside
 *                                   the bar row, or a protected slot
 *   drop()                          the player dropped it (Q) with it lifted
 *   drag(spell, from, targets)      one whole drag gesture in a single call
 *
 * Rules:
 *  - Not armed: every input is NONE (the mode is off for this player).
 *  - Place with nothing lifted, and places onto a protected slot (the wand
 *    slot, the exit column), are inert: NONE / RETURN, the lift forgotten.
 *  - Place outside the bar row is REMOVE; onto another bar slot is MOVE;
 *    back onto its own slot is RETURN.
 *  - Drop is REMOVE, lifted or (dropSlot) straight off the icon's slot.
 *  - A drag whose target set reaches outside the bar row is REMOVE; otherwise
 *    MOVE to the farthest bar target in the set.
 *  - Duplicates: a real client plus its protocol translation can commit the
 *    same gesture twice; a second terminal commit for the same spell within
 *    {@link #DEDUPE_MS} of a REMOVE / MOVE is swallowed as NONE.
 */
public final class ArrangePlan {
    public static final long DEDUPE_MS = 500;

    public enum TargetKind { BAR, OUTSIDE, PROTECTED }

    /** One place target: a loadout slot (BAR), anywhere outside the bar row, or a protected slot. */
    public static final class Target {
        public final TargetKind kind;
        public final int loadoutSlot;
        private Target(TargetKind k, int s) { kind = k; loadoutSlot = s; }
        public static Target bar(int loadoutSlot) { return new Target(TargetKind.BAR, loadoutSlot); }
        public static Target outside() { return new Target(TargetKind.OUTSIDE, -1); }
        public static Target protectedSlot() { return new Target(TargetKind.PROTECTED, -1); }
    }

    public enum Intent { NONE, REMOVE, MOVE, RETURN }

    public static final class Result {
        public final Intent intent;
        public final String spell;
        /** REMOVE: the slot it leaves. MOVE: the slot it goes to. */
        public final int slot;
        Result(Intent i, String s, int slot) { intent = i; spell = s; this.slot = slot; }
        public boolean terminal() { return intent == Intent.REMOVE || intent == Intent.MOVE; }
    }

    private static final Result NONE = new Result(Intent.NONE, null, -1);

    private static final class Lift { String spell; int from; }
    private static final class Done { String spell; long at; }

    private final Map<UUID, Boolean> armed = new HashMap<>();
    private final Map<UUID, Lift> lifts = new HashMap<>();
    private final Map<UUID, Done> lastDone = new HashMap<>();

    public synchronized void setArmed(UUID id, boolean on) {
        if (id == null) return;
        if (on) armed.put(id, Boolean.TRUE);
        else { armed.remove(id); lifts.remove(id); }
    }

    public synchronized boolean isArmed(UUID id) { return id != null && armed.containsKey(id); }
    public synchronized boolean hasLift(UUID id) { return id != null && lifts.containsKey(id); }

    /** The lifted spell, or null (0.3.9: the hook's re-lift guard reads it, mirroring DragWritePlan). */
    public synchronized String liftedSpell(UUID id) {
        Lift l = id == null ? null : lifts.get(id);
        return l == null ? null : l.spell;
    }

    /** The loadout slot the lift started from (0.3.9: the re-lift guard's origin check). */
    public synchronized int liftedFrom(UUID id) {
        Lift l = id == null ? null : lifts.get(id);
        return l == null ? Integer.MIN_VALUE : l.from;
    }

    /** The player lifted a bar icon (a second lift replaces the first). */
    public synchronized void pickup(UUID id, String spell, int fromLoadoutSlot, long now) {
        if (!isArmed(id) || spell == null) return;
        Lift l = new Lift();
        l.spell = spell; l.from = fromLoadoutSlot;
        lifts.put(id, l);
    }

    /** The player put the lifted icon down. */
    public synchronized Result place(UUID id, Target target, long now) {
        Lift l = id == null ? null : lifts.remove(id);
        if (!isArmed(id) || l == null || target == null) return NONE;
        switch (target.kind) {
            case PROTECTED: return new Result(Intent.RETURN, l.spell, l.from);
            case OUTSIDE: return commit(id, new Result(Intent.REMOVE, l.spell, l.from), now);
            case BAR:
                if (target.loadoutSlot == l.from) return new Result(Intent.RETURN, l.spell, l.from);
                return commit(id, new Result(Intent.MOVE, l.spell, target.loadoutSlot), now);
            default: return NONE;
        }
    }

    /** Dropped (Q) with the icon lifted. */
    public synchronized Result drop(UUID id, long now) {
        Lift l = id == null ? null : lifts.remove(id);
        if (!isArmed(id) || l == null) return NONE;
        return commit(id, new Result(Intent.REMOVE, l.spell, l.from), now);
    }

    /** Dropped (Q) while hovering the icon in its own slot (nothing lifted). */
    public synchronized Result dropSlot(UUID id, String spell, int fromLoadoutSlot, long now) {
        if (!isArmed(id) || spell == null) return NONE;
        lifts.remove(id);
        return commit(id, new Result(Intent.REMOVE, spell, fromLoadoutSlot), now);
    }

    /** One whole drag gesture: the icon, where it started, every slot the drag touched. */
    public synchronized Result drag(UUID id, String spell, int fromLoadoutSlot, List<Target> targets, long now) {
        if (!isArmed(id) || spell == null || targets == null || targets.isEmpty()) return NONE;
        lifts.remove(id);
        Target farthest = null;
        for (Target t : targets) {
            if (t == null) continue;
            if (t.kind == TargetKind.OUTSIDE) return commit(id, new Result(Intent.REMOVE, spell, fromLoadoutSlot), now);
            if (t.kind == TargetKind.BAR && (farthest == null
                    || Math.abs(t.loadoutSlot - fromLoadoutSlot) > Math.abs(farthest.loadoutSlot - fromLoadoutSlot))) farthest = t;
        }
        if (farthest == null) return new Result(Intent.RETURN, spell, fromLoadoutSlot); // only protected slots touched
        if (farthest.loadoutSlot == fromLoadoutSlot) return new Result(Intent.RETURN, spell, fromLoadoutSlot);
        return commit(id, new Result(Intent.MOVE, spell, farthest.loadoutSlot), now);
    }

    /** Forget any pending lift (the read form opened, the screen closed). */
    public synchronized void cancelLift(UUID id) { if (id != null) lifts.remove(id); }

    public synchronized void forget(UUID id) {
        if (id == null) return;
        armed.remove(id); lifts.remove(id); lastDone.remove(id);
    }

    public synchronized void clear() { armed.clear(); lifts.clear(); lastDone.clear(); }

    /** Dedupe: the same spell's terminal commit twice inside the window counts once. */
    private Result commit(UUID id, Result r, long now) {
        Done d = lastDone.get(id);
        if (d != null && d.spell != null && d.spell.equals(r.spell) && now - d.at < DEDUPE_MS && now >= d.at) return NONE;
        Done n = new Done();
        n.spell = r.spell; n.at = now;
        lastDone.put(id, n);
        return r;
    }
}
