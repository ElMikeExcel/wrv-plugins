package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelProtect 0.1.4 — the WHERE report: one player's location timeline
 * rendered as a readable route (start → anchors → latest) for ANY sender,
 * the DamageReport pattern — pure assembly over scanned `locations`
 * stream lines, so the whole form is unit-tested without a server.
 *
 * Rendering rules (kept deliberately legible in a console):
 *  - records sort by ts_ms; at equal timestamps anchors precede samples
 *    (the anchor EXPLAINS the position the sample then confirms).
 *  - the first record is the start step; the last record is always the
 *    latest step (marked "latest" when it was not otherwise shown).
 *  - anchors are always steps — they are the story (join, quit,
 *    teleport + cause, world change, respawn, gamemode).
 *  - samples become steps only when they show real travel: a world
 *    change, or ≥ MOVE_STEP_BLOCKS from the last SHOWN point. A player
 *    standing around generates samples but no route noise.
 *  - the rendered steps cap at the caller's limit; omitted middle steps
 *    are counted, never silently dropped (Report.hiddenSteps).
 * Excel — Wink Reyes Valley
 */
public final class WhereReport {
    private WhereReport() {}

    /** A sample counts as travel when it is at least this far (blocks, 3D) from the last shown point. */
    public static final double MOVE_STEP_BLOCKS = 32.0;

    public record Report(int records, int anchors, int samples, int steps,
                         int hiddenSteps, List<String> lines) {}

    private record Pt(long ts, boolean anchor, String world, double x, double y, double z, String line) {}

    /** Build the route over scanned locations lines for one player. */
    public static Report build(List<String> scannedLines, int limit) {
        List<Pt> pts = new ArrayList<>();
        int anchors = 0, samples = 0;
        for (String line : scannedLines) {
            String type = Json.strField(line, "type");
            boolean isAnchor = "location_anchor".equals(type);
            if (!isAnchor && !"location_sample".equals(type)) continue;
            String world = Json.strField(line, "world");
            if (world == null) continue;
            long ts = (long) Json.numField(line, "ts_ms", -1);
            if (ts < 0) continue;
            if (isAnchor) anchors++; else samples++;
            pts.add(new Pt(ts, isAnchor, world,
                    Json.numField(line, "x", 0), Json.numField(line, "y", 0), Json.numField(line, "z", 0), line));
        }
        pts.sort((a, b) -> {
            int c = Long.compare(a.ts(), b.ts());
            if (c != 0) return c;
            return Boolean.compare(b.anchor(), a.anchor()); // anchors first at equal ts
        });

        List<String> steps = new ArrayList<>();
        Pt lastShown = null;
        for (int i = 0; i < pts.size(); i++) {
            Pt p = pts.get(i);
            boolean first = i == 0, last = i == pts.size() - 1;
            boolean show;
            if (first || p.anchor()) show = true;
            else if (lastShown == null) show = true;
            else show = !p.world().equals(lastShown.world())
                    || dist(p, lastShown) >= MOVE_STEP_BLOCKS;
            if (show) {
                steps.add(step(p, first ? "start" : null));
                lastShown = p;
            } else if (last) {
                steps.add(step(p, "latest"));
                lastShown = p;
            }
        }
        int hidden = 0;
        List<String> out = steps;
        if (steps.size() > limit && limit >= 2) {
            out = new ArrayList<>(steps.subList(0, limit - 1));
            out.add(steps.get(steps.size() - 1));
            hidden = steps.size() - limit;
        }
        return new Report(pts.size(), anchors, samples, steps.size(), hidden, out);
    }

    private static double dist(Pt a, Pt b) {
        double dx = a.x() - b.x(), dy = a.y() - b.y(), dz = a.z() - b.z();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /** One route step: "21:14:05 start @ world 100,64,-200" / "21:15:02 teleport (plugin) → world_nether 12,70,40". */
    private static String step(Pt p, String tag) {
        String clock = clock(p.ts());
        if (!p.anchor()) {
            return clock + " " + (tag == null ? "moved" : tag) + " @ " + at(p);
        }
        String kind = Json.strField(p.line(), "anchor");
        StringBuilder sb = new StringBuilder(clock).append(' ');
        if (tag != null) sb.append(tag).append(' ');
        switch (kind == null ? "" : kind) {
            case "teleport" -> {
                String cause = Json.strField(p.line(), "cause");
                sb.append("teleport").append(cause == null ? "" : " (" + cause + ")").append(" → ")
                  .append(Json.strField(p.line(), "to_world")).append(' ')
                  .append((long) Json.numField(p.line(), "to_x", 0)).append(',')
                  .append((long) Json.numField(p.line(), "to_y", 0)).append(',')
                  .append((long) Json.numField(p.line(), "to_z", 0));
            }
            case "world_change" -> sb.append("world change ")
                    .append(Json.strField(p.line(), "from_world")).append(" → ").append(p.world())
                    .append(" @ ").append(coords(p));
            case "respawn" -> {
                sb.append("respawn @ ").append(at(p));
                if (p.line().contains("\"is_bed_spawn\":true")) sb.append(" (bed)");
                else if (p.line().contains("\"is_anchor_spawn\":true")) sb.append(" (anchor)");
            }
            case "gamemode" -> sb.append("gamemode ")
                    .append(Json.strField(p.line(), "gamemode_from")).append("→")
                    .append(Json.strField(p.line(), "gamemode_to")).append(" @ ").append(at(p));
            default -> sb.append(kind == null ? "anchor" : kind).append(" @ ").append(at(p));
        }
        return sb.toString();
    }

    /** One-line anchor summary for InspectReport: "21:14:05 anchor teleport (plugin) @ world 100,64,-200". */
    public static String summarizeAnchor(String line) {
        if (!"location_anchor".equals(Json.strField(line, "type"))) return null;
        long ts = (long) Json.numField(line, "ts_ms", -1);
        String world = Json.strField(line, "world");
        if (ts < 0 || world == null) return null;
        Pt p = new Pt(ts, true, world, Json.numField(line, "x", 0),
                Json.numField(line, "y", 0), Json.numField(line, "z", 0), line);
        String s = step(p, null);
        // step() already leads with the clock; re-shape "HH:MM:SS teleport …" → "HH:MM:SS anchor teleport …"
        int sp = s.indexOf(' ');
        return sp < 0 ? s : s.substring(0, sp) + " anchor" + s.substring(sp);
    }

    private static String at(Pt p) { return p.world() + " " + coords(p); }
    private static String coords(Pt p) {
        return (long) p.x() + "," + (long) p.y() + "," + (long) p.z();
    }
    private static String clock(long ts) {
        String iso = Ledger.isoOf(ts);
        return iso.length() >= 19 ? iso.substring(11, 19) : iso;
    }
}
