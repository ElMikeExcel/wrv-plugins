package com.winkreyes.exceldetails;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pure sidebar line assembly (unit-tested, no Bukkit). Given a line
 * budget it sheds in the house order: footer first, then the lowest
 * ranked rows; the header and the viewer's own row are shed last.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class LineBuilder {

    private LineBuilder() {}

    public static List<String> build(CombatStats.Window window,
                                     List<SegmentTracker.Row> rows,
                                     UUID viewer, double total,
                                     long elapsedMs, int maxRows, int budget) {
        List<String> out = new ArrayList<>();
        if (budget <= 0) return out;

        String header = "§6§lDetails! §7" + window.title();
        out.add(header);
        if (budget == 1) return out;

        double ownAmount = 0;
        int ownRank = -1;
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).id().equals(viewer)) { ownAmount = rows.get(i).amount(); ownRank = i; }
        }

        boolean withFooter = budget >= 3;
        int rowSlots = Math.min(maxRows, budget - 1 - (withFooter ? 1 : 0));
        if (rowSlots < 0) rowSlots = 0;

        List<SegmentTracker.Row> shown = new ArrayList<>();
        for (int i = 0; i < Math.min(rowSlots, rows.size()); i++) shown.add(rows.get(i));
        // The viewer's own row is shed last: if it fell below the cut
        // and there is a slot to give, it takes the lowest shown slot.
        if (ownRank >= rowSlots && rowSlots > 0) {
            shown.set(rowSlots - 1, rows.get(ownRank));
        }

        for (SegmentTracker.Row r : shown) {
            int rank = rows.indexOf(r) + 1;
            boolean own = r.id().equals(viewer);
            out.add((own ? "§a" : "§f") + rank + ". " + r.name()
                    + " §e" + MeterMath.compact(r.amount())
                    + " §7(" + MeterMath.percent(r.amount(), total) + "%)");
        }

        if (withFooter) {
            out.add("§7" + MeterMath.clock(elapsedMs) + " • you "
                    + MeterMath.compact(MeterMath.perSecond(ownAmount, elapsedMs)) + "/s");
        }
        return out;
    }

    // ----- 0.2.0: the checkbox mix (stacked sections, one box) -----

    /** One metric's slice of the mix: its window, ranked rows, and row total. */
    public record Section(CombatStats.Window window, List<SegmentTracker.Row> rows, double total) {}

    /**
     * Compose every enabled section into ONE line list under a hard
     * budget (the caller passes 15 standalone, 15 − EM lines when
     * piggybacking — the combined box never exceeds the sidebar
     * ceiling). maxRowsPerSection caps each section's rows BEFORE the
     * budget squeeze (config max-rows is per-section in 0.2.0).
     *
     * Rules (DESIGN build-0.2.0 §2): a section with no rows renders
     * nothing; each live section gets its own 0.1.0-format header;
     * under pressure the shared footer sheds first, then rows one at
     * a time from the fullest section (ties: the LAST such section in
     * canonical order), never below 1 row while any section shows 2+;
     * then whole sections drop from the end of canonical order. The
     * viewer's own row is retained per section, as in 0.1.0. The
     * footer carries the clock + the viewer's per-second in the FIRST
     * rendered section. An empty result means NO box at all.
     */
    public static List<String> buildSections(List<Section> sections, UUID viewer,
                                             long elapsedMs, int maxRowsPerSection, int budget) {
        List<String> out = new ArrayList<>();
        if (budget <= 0 || sections == null) return out;

        List<Section> secs = new ArrayList<>();
        for (Section s : sections) {
            if (s != null && s.rows() != null && !s.rows().isEmpty()) secs.add(s);
        }
        if (secs.isEmpty()) return out;

        List<Integer> counts = new ArrayList<>();
        for (Section s : secs) {
            counts.add(Math.min(Math.max(0, maxRowsPerSection), s.rows().size()));
        }
        boolean footer = true;
        while (totalLines(secs, counts, footer) > budget) {
            if (footer) { footer = false; continue; }
            int idx = -1, best = 1;
            for (int i = 0; i < secs.size(); i++) {
                int c = counts.get(i);
                if (c > 1 && c >= best) { idx = i; best = c; }
            }
            if (idx >= 0) { counts.set(idx, counts.get(idx) - 1); continue; }
            if (secs.size() > 1) {
                secs.remove(secs.size() - 1);
                counts.remove(counts.size() - 1);
                continue;
            }
            counts.set(0, 0); // lone section, budget 1: header alone survives
            break;
        }

        for (int i = 0; i < secs.size(); i++) {
            Section s = secs.get(i);
            out.add("§6§lDetails! §7" + s.window().title());
            int rowSlots = counts.get(i);
            if (rowSlots <= 0) continue;
            List<SegmentTracker.Row> rows = s.rows();
            List<SegmentTracker.Row> shown = new ArrayList<>();
            for (int j = 0; j < Math.min(rowSlots, rows.size()); j++) shown.add(rows.get(j));
            int ownRank = -1;
            for (int j = 0; j < rows.size(); j++) {
                if (rows.get(j).id().equals(viewer)) { ownRank = j; break; }
            }
            if (ownRank >= rowSlots) shown.set(rowSlots - 1, rows.get(ownRank));
            for (SegmentTracker.Row r : shown) {
                int rank = rows.indexOf(r) + 1;
                boolean own = r.id().equals(viewer);
                out.add((own ? "§a" : "§f") + rank + ". " + r.name()
                        + " §e" + MeterMath.compact(r.amount())
                        + " §7(" + MeterMath.percent(r.amount(), s.total()) + "%)");
            }
        }

        if (footer) {
            double ownFirst = 0;
            for (SegmentTracker.Row r : secs.get(0).rows()) {
                if (r.id().equals(viewer)) { ownFirst = r.amount(); break; }
            }
            out.add("§7" + MeterMath.clock(elapsedMs) + " • you "
                    + MeterMath.compact(MeterMath.perSecond(ownFirst, elapsedMs)) + "/s");
        }
        return out;
    }

    private static int totalLines(List<Section> secs, List<Integer> counts, boolean footer) {
        int t = secs.size() + (footer ? 1 : 0);
        for (int c : counts) t += c;
        return t;
    }
}
