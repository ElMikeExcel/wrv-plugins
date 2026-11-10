package com.winkreyes.exceldetails;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 0.2.1 party scoping — the pure half (unit-tested, no Bukkit).
 *
 * Mike's ruling (Nov 9, 2026, after his live report that the meter
 * showed players outside his party): the sidebar shows ONLY the
 * viewer's own rows plus their party members' rows. "Party" is the
 * union of the viewer's ExcelTogether party and their EliteMobs
 * party (either system counts). Solo, the scope is the viewer alone.
 *
 * This is DISPLAY scoping only: the encounter segment stays one
 * shared, server-wide segment (same clock, same tracking, same
 * overall totals) — only the rows a given viewer is shown change.
 * The seam half (asking Together / EliteMobs who is in the viewer's
 * party) lives in PartyLookup and fails soft to an empty set, so a
 * missing seam degrades the view to own rows, never to everyone.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PartyScope {

    private PartyScope() {}

    /**
     * The viewer's scope set: always the viewer, plus every member
     * either party system reports (each party's member list already
     * includes the viewer; null sets are ignored).
     */
    public static Set<UUID> scopeOf(UUID viewer, Set<UUID> togetherMembers, Set<UUID> eliteMobsMembers) {
        Set<UUID> out = new LinkedHashSet<>();
        out.add(viewer);
        if (togetherMembers != null) out.addAll(togetherMembers);
        if (eliteMobsMembers != null) out.addAll(eliteMobsMembers);
        return out;
    }

    /**
     * Ranked segment rows reduced to the scope, ranked order
     * preserved. A null scope means self-only (the fail-soft floor).
     * The viewer's own row is always visible, scope or not.
     */
    public static List<SegmentTracker.Row> visibleRows(List<SegmentTracker.Row> ranked, UUID viewer, Set<UUID> scope) {
        List<SegmentTracker.Row> out = new ArrayList<>();
        if (ranked == null) return out;
        for (SegmentTracker.Row r : ranked) {
            if (r.id().equals(viewer) || (scope != null && scope.contains(r.id()))) out.add(r);
        }
        return out;
    }

    /** Section total over the FILTERED rows — shares are within-group. */
    public static double totalOf(List<SegmentTracker.Row> rows) {
        double t = 0;
        if (rows != null) for (SegmentTracker.Row r : rows) t += r.amount();
        return t;
    }
}
