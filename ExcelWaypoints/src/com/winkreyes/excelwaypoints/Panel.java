package com.winkreyes.excelwaypoints;

import java.util.ArrayList;
import java.util.List;

/** Chat panel model: lines of text segments, some clickable. Turned into chat components by the plugin layer. */
public final class Panel {
    public static final class Seg {
        public final String text; public final String run; public final String suggest; public final String hover;
        Seg(String t, String run, String suggest, String hover) { text = t; this.run = run; this.suggest = suggest; this.hover = hover; }
        public boolean clickable() { return run != null || suggest != null; }
    }
    public static final class Line {
        public final List<Seg> segs = new ArrayList<>();
        public Line text(String t) { segs.add(new Seg(t, null, null, null)); return this; }
        public Line button(String label, String runCommand) { segs.add(new Seg("[" + label + "]", runCommand, null, label)); return this; }
        public Line fill(String label, String suggestCommand) { segs.add(new Seg("[" + label + "]", null, suggestCommand, label)); return this; }
        public Line gap() { segs.add(new Seg(" ", null, null, null)); return this; }
        public String plain() { StringBuilder b = new StringBuilder(); for (Seg s : segs) b.append(s.text); return b.toString(); }
    }
    public final List<Line> lines = new ArrayList<>();
    public Line line() { Line l = new Line(); lines.add(l); return l; }
    public Panel text(String t) { line().text(t); return this; }
    public String plain() { StringBuilder b = new StringBuilder(); for (Line l : lines) b.append(l.plain()).append('\n'); return b.toString(); }
    public int longestButton() { int m = 0; for (Line l : lines) for (Seg s : l.segs) if (s.clickable()) m = Math.max(m, s.text.length()); return m; }
}
