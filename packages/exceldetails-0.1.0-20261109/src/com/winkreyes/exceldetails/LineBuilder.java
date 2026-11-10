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
}
