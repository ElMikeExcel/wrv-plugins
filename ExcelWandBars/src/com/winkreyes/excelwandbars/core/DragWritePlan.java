package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 0.3.7: plain drags are a real loadout write path (Mike, Nov 1, 2026: the
 * Arrange button works, but dragging a spell onto the bar with the wand in
 * hand must just work — that was the original Oct 31 expectation, and 0.3.6's
 * cancel-with-signpost only made the old non-write honest). This is
 * ArrangePlan's sibling for the UNARMED screen: the same primitive inputs
 * (pickup / place / drop / drag) and the same targets, with two differences:
 *
 *  - The lifted spell may come from anywhere in the player's inventory — the
 *    bar row, the main grid, the all-spells presentation — so a lift records
 *    the spell's loadout slot when it is on the bar, or -1 ("off-bar": the
 *    Spellbook / another page) when it is not. Identity comes from the icon
 *    itself; the model slot is looked up, never derived from the grid slot
 *    the finger happened to touch.
 *  - Placing onto a bar slot is a PUT (BarLoadout.put semantics: an off-bar
 *    spell takes the slot and the replaced spell returns to the Spellbook;
 *    an on-bar spell swaps or moves). Placing outside the bar row is a
 *    REMOVE when the spell came from the bar (drag-off-to-remove, layout
 *    only — the spell stays learned) and an inert RETURN when it did not.
 *
 * The hook cancels every interpreted event (the icons are virtual, exactly
 * as in Arrange) and executes the intent through LoadoutHook's drag methods,
 * so the model change, the save and the repaint are the /spellbar machinery.
 * Duplicates are swallowed like Arrange's: a real client plus its protocol
 * translation can commit the same gesture twice, so a second terminal commit
 * for the same spell inside {@link #DEDUPE_MS} counts once.
 *
 * The plan also owns the confirmation throttle: a successful write earns one
 * short action-bar line, at most one per gap, however fast the drags land.
 */
public final class DragWritePlan {
    public static final long DEDUPE_MS = 500;

    // Target kinds reuse ArrangePlan's vocabulary by value: BAR (a loadout
    // slot), OUTSIDE (anywhere outside the bar row), PROTECTED (the wand
    // slot, the exit column).
    public enum Intent { NONE, PUT, REMOVE, RETURN }

    public static final class Result {
        public final Intent intent;
        public final String spell;
        /** PUT: the slot it goes to. REMOVE: the slot it leaves. */
        public final int slot;
        Result(Intent i, String s, int slot) { intent = i; spell = s; this.slot = slot; }
        public boolean terminal() { return intent == Intent.PUT || intent == Intent.REMOVE; }
    }

    private static final Result NONE = new Result(Intent.NONE, null, -1);

    private static final class Lift { String spell; int from; }
    private static final class Done { String spell; long at; }

    private final Map<UUID, Lift> lifts = new HashMap<>();
    private final Map<UUID, Done> lastDone = new HashMap<>();
    private final Map<UUID, Long> lastConfirm = new HashMap<>();
    private final Map<UUID, Long> lastWrite = new HashMap<>();

    public synchronized boolean hasLift(UUID id) { return id != null && lifts.containsKey(id); }

    /** The lifted spell, or null (the hook re-derives intents from the plan, this is for tests). */
    public synchronized String liftedSpell(UUID id) {
        Lift l = id == null ? null : lifts.get(id);
        return l == null ? null : l.spell;
    }

    /** The loadout slot the lift started from, or -1 when it came from off the bar (0.3.9). */
    public synchronized int liftedFrom(UUID id) {
        Lift l = id == null ? null : lifts.get(id);
        return l == null ? Integer.MIN_VALUE : l.from;
    }

    /**
     * 0.3.9 (defect #4): is this click the SAME pickup gesture delivered
     * twice? A Bedrock tap can arrive as more than one click event; the
     * duplicate lands on the lift's ORIGIN — the same bar slot for an on-bar
     * lift, a grid slot for an off-bar (list) lift — showing the same spell.
     * Spell identity alone is NOT the signature: after a repaint flicker the
     * restored inventory can legitimately display the lifted spell's icon on
     * the place target, and eating that click as a "duplicate" drops the
     * commit (the first 0.3.9 rig pass proved exactly that trade).
     *
     * @param liftFrom          the lift's origin loadout slot, -1 = off-bar
     * @param clickedBarRow     the click landed in the hotbar row
     * @param clickedLoadoutSlot the click's loadout slot on the lift's page (-1 = not a spell column)
     */
    public static boolean isDuplicatePickup(int liftFrom, boolean clickedBarRow,
            int clickedLoadoutSlot, String clickedSpell, String liftedSpell) {
        if (clickedSpell == null || liftedSpell == null || !clickedSpell.equals(liftedSpell)) return false;
        if (liftFrom >= 0) return clickedBarRow && clickedLoadoutSlot == liftFrom;
        return !clickedBarRow; // an off-bar lift's duplicate re-fires on the grid, never on the bar row
    }

    /** The player lifted a spell icon. fromLoadoutSlot is its bar slot, or -1 when it is not on the bar. */
    public synchronized void pickup(UUID id, String spell, int fromLoadoutSlot, long now) {
        if (id == null || spell == null) return;
        Lift l = new Lift();
        l.spell = spell; l.from = fromLoadoutSlot;
        lifts.put(id, l);
    }

    /** The player put the lifted icon down. */
    public synchronized Result place(UUID id, ArrangePlan.Target target, long now) {
        Lift l = id == null ? null : lifts.remove(id);
        if (l == null || target == null) return NONE;
        return resolve(id, l.spell, l.from, target, now);
    }

    /**
     * A place commit whose pickup was never recorded (the cursor already held
     * the icon when the screen stream started, or a dialect skipped the
     * pickup): judge it as a lift of that spell from its looked-up slot.
     */
    public synchronized Result placeWith(UUID id, String spell, int fromLoadoutSlot, ArrangePlan.Target target, long now) {
        if (id == null || spell == null || target == null) return NONE;
        lifts.remove(id);
        return resolve(id, spell, fromLoadoutSlot, target, now);
    }

    private Result resolve(UUID id, String spell, int from, ArrangePlan.Target target, long now) {
        switch (target.kind) {
            case PROTECTED: return new Result(Intent.RETURN, spell, from);
            case OUTSIDE:
                return from >= 0 ? commit(id, new Result(Intent.REMOVE, spell, from), now)
                        : new Result(Intent.RETURN, spell, from);
            case BAR:
                if (target.loadoutSlot == from) return new Result(Intent.RETURN, spell, from);
                return commit(id, new Result(Intent.PUT, spell, target.loadoutSlot), now);
            default: return NONE;
        }
    }

    /** Dropped (Q) with the icon lifted. An off-bar spell dropped is inert (it never left the book). */
    public synchronized Result drop(UUID id, long now) {
        Lift l = id == null ? null : lifts.remove(id);
        if (l == null) return NONE;
        return l.from >= 0 ? commit(id, new Result(Intent.REMOVE, l.spell, l.from), now)
                : new Result(Intent.RETURN, l.spell, l.from);
    }

    /** Dropped (Q) while hovering the icon (nothing lifted). */
    public synchronized Result dropSlot(UUID id, String spell, int fromLoadoutSlot, long now) {
        if (id == null || spell == null) return NONE;
        lifts.remove(id);
        return fromLoadoutSlot >= 0 ? commit(id, new Result(Intent.REMOVE, spell, fromLoadoutSlot), now)
                : NONE;
    }

    /**
     * One whole drag gesture: the icon, where it started (-1 = off-bar), every
     * slot the drag touched. Mirrors Arrange's drag rule: any outside target
     * means REMOVE (from the bar) / RETURN (from off-bar); otherwise the bar
     * target wins — the farthest from the origin for an on-bar drag, the first
     * bar target touched for an off-bar one (there is no origin to measure
     * from, and a painted single-icon drag lands on one slot in practice).
     */
    public synchronized Result drag(UUID id, String spell, int fromLoadoutSlot, List<ArrangePlan.Target> targets, long now) {
        if (id == null || spell == null || targets == null || targets.isEmpty()) return NONE;
        lifts.remove(id);
        ArrangePlan.Target firstBar = null, farthest = null;
        for (ArrangePlan.Target t : targets) {
            if (t == null) continue;
            if (t.kind == ArrangePlan.TargetKind.OUTSIDE) {
                return fromLoadoutSlot >= 0
                        ? commit(id, new Result(Intent.REMOVE, spell, fromLoadoutSlot), now)
                        : new Result(Intent.RETURN, spell, fromLoadoutSlot);
            }
            if (t.kind == ArrangePlan.TargetKind.BAR) {
                if (firstBar == null) firstBar = t;
                if (farthest == null
                        || Math.abs(t.loadoutSlot - Math.max(0, fromLoadoutSlot)) > Math.abs(farthest.loadoutSlot - Math.max(0, fromLoadoutSlot))) farthest = t;
            }
        }
        ArrangePlan.Target pick = fromLoadoutSlot >= 0 ? farthest : firstBar;
        if (pick == null) return new Result(Intent.RETURN, spell, fromLoadoutSlot); // only protected slots touched
        if (pick.loadoutSlot == fromLoadoutSlot) return new Result(Intent.RETURN, spell, fromLoadoutSlot);
        return commit(id, new Result(Intent.PUT, spell, pick.loadoutSlot), now);
    }

    /** Forget any pending lift (the read form opened, the screen really closed). */
    public synchronized void cancelLift(UUID id) { if (id != null) lifts.remove(id); }

    /** A write just landed: the screen session is live (the hook may restore the bar after the repaint). */
    public synchronized void touch(UUID id, long now) { if (id != null) lastWrite.put(id, now); }

    /** True when a write landed within the window (a repaint may have closed the bar mid-session). */
    public synchronized boolean recentWrite(UUID id, long now, long windowMs) {
        Long at = id == null ? null : lastWrite.get(id);
        return at != null && now - at >= 0 && now - at <= windowMs;
    }

    /** Confirmation throttle: true (and records) when a confirmation line may be shown now. */
    public synchronized boolean confirmDue(UUID id, long now, long gapMs) {
        if (id == null) return false;
        Long at = lastConfirm.get(id);
        if (at != null && now - at < Math.max(0, gapMs) && now >= at) return false;
        lastConfirm.put(id, now);
        return true;
    }

    public synchronized void forget(UUID id) {
        if (id == null) return;
        lifts.remove(id); lastDone.remove(id); lastConfirm.remove(id); lastWrite.remove(id);
    }

    public synchronized void clear() { lifts.clear(); lastDone.clear(); lastConfirm.clear(); lastWrite.clear(); }

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
