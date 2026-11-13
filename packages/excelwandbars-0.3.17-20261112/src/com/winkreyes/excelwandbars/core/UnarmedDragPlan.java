package com.winkreyes.excelwandbars.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.3.6: the honest-drag gate (Nov 1, 2026 investigation, Option B). Outside
 * Arrange mode a drag or click-commit on a spell icon in the inventory screen
 * was never a persistence write path: the icons physically moved, nothing
 * wrote BarLoadout, and the next wand draw re-rendered the saved order over
 * the drag. The gesture now cancels instead of faking success, and the
 * player is pointed at the path that does persist (/spellbar, then Arrange
 * by dragging) — at most once per inventory-screen session, and never more
 * than once per {@link #MIN_SIGNPOST_GAP_MS} however the screens flicker
 * (a loadout repaint closes and reopens the view, which also ends a
 * "session" here; the time floor is what caps the rate).
 *
 * Pure: the hook reduces each Bukkit event to (armed, iconTouched) and does
 * the cancelling and messaging itself.
 */
public final class UnarmedDragPlan {
    public enum Verdict { PASS, CANCEL, CANCEL_AND_SIGNPOST }

    /** Hard floor between two signposts for one player, across sessions. */
    public static final long MIN_SIGNPOST_GAP_MS = 10_000;

    private final Map<UUID, Long> lastSignpost = new HashMap<>();
    private final Set<UUID> shownThisSession = new HashSet<>();

    /**
     * Judge one gesture. Armed gestures belong to ArrangePlan and always
     * pass; gestures that touch no spell icon pass; an unarmed gesture on an
     * icon cancels, with the signpost when the throttle allows it.
     */
    public Verdict judge(UUID id, boolean armed, boolean iconTouched, long now) {
        if (armed || !iconTouched) return Verdict.PASS;
        if (!shownThisSession.contains(id)
                && now - lastSignpost.getOrDefault(id, 0L) >= MIN_SIGNPOST_GAP_MS) {
            shownThisSession.add(id);
            lastSignpost.put(id, now);
            return Verdict.CANCEL_AND_SIGNPOST;
        }
        return Verdict.CANCEL;
    }

    /** The player's own inventory screen closed: the next session may signpost again. */
    public void screenClosed(UUID id) { shownThisSession.remove(id); }

    public void forget(UUID id) { shownThisSession.remove(id); lastSignpost.remove(id); }

    public void clear() { shownThisSession.clear(); lastSignpost.clear(); }
}
