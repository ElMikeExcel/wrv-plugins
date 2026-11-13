package com.winkreyes.excelskillspanel.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The unified Skills panel model (menu programme item 2, DESIGN
 * §7.3): one pure model computed from provider reads, rendered by
 * a Java chest presentation and a Bedrock Cumulus form
 * presentation — the ExcelSummons pattern. No Bukkit types live
 * here, so the harness proves the panel's content directly.
 *
 * A provider supplies header blocks (labelled lines + optional
 * bar) and skill rows (id, display name, icon key, level, XP into
 * the level, the level's XP span, optional rank label, detail
 * lines). The model assembles them in provider order and owns
 * every player-visible string the two presentations share: the
 * block-bar rendering, the compact per-skill line, the Bedrock
 * body, and the chat fallback lines.
 *
 * Bars: a row's span is the level's TOTAL XP span (mcMMO's
 * getXPToNextLevel semantics; for Magic, xp-into + xp-to-next).
 * A span of 0 (or less) means the level is capped — the bar
 * renders full and the numbers line says MAX instead of
 * inventing a fraction.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class PanelModel {
    private PanelModel() {}

    /** Segments in every rendered progress bar. */
    public static final int BAR_SEGMENTS = 10;
    public static final String BAR_FILLED = "\u25B0";
    public static final String BAR_EMPTY = "\u25B1";

    /** Thousands-separated number formatting, one implementation
     *  for both presentations. */
    public static String fmt(long n) {
        return String.format("%,d", n);
    }

    /** The percent sign as rendered INTO BEDROCK FORM TEXT:
     *  U+FF05 FULLWIDTH PERCENT SIGN.
     *
     *  0.1.1 (Mike, Nov 13, 2026): the model's strings carry the
     *  ASCII "%" (U+0025) and the Cumulus form built from them
     *  retains it exactly — both harness-proven — yet on Mike's
     *  live Bedrock panel EVERY "%" in the form body vanished
     *  ("34 \u2014 10 / 29 XP" for "34% \u2014 ...", boosts
     *  "+0.5943," for "+0.5943%,"): the glyph alone is dropped
     *  downstream of Cumulus, in the Bedrock client's form-text
     *  rendering, where U+0025 introduces format/translation
     *  substitutions. The Java chest (Adventure components) and
     *  the chat fallback never pass through that renderer and
     *  keep the ASCII sign. Form-bound body text therefore swaps
     *  U+0025 for U+FF05 — a distinct codepoint the Bedrock
     *  renderer passes through, drawn as a percent sign. Applied
     *  ONLY at the Bedrock send boundary (PanelMenus), never in
     *  the model: the bar + percent both stay, on both platforms. */
    public static final String BEDROCK_PERCENT = "\uFF05";

    /** Render a model string safe for a Bedrock form body:
     *  every ASCII "%" becomes {@link #BEDROCK_PERCENT}. Pure
     *  and idempotent (U+FF05 contains no U+0025). */
    public static String bedrockSafe(String s) {
        return s == null ? null : s.replace("%", BEDROCK_PERCENT);
    }

    /** Colour the bar run inside an arbitrary model line for a
     *  STRING surface (0.2.1, Item 9b): the first maximal run of
     *  bar glyphs (\u25b0/\u25b1) gets its filled (\u25b0)
     *  prefix wrapped in legacy gold ({@code \u00a76}) and the
     *  remainder in gray ({@code \u00a77}), exactly
     *  {@link BarData#coloredBar()}'s split. Lines with no bar
     *  run return unchanged. Used for the Bedrock DETAIL forms,
     *  whose lines are the shared detailLines (the Java chest
     *  colours the same lines as components in PanelMenus). */
    public static String colorizeBar(String line) {
        if (line == null) return null;
        int start = -1;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\u25b0' || c == '\u25b1') { start = i; break; }
        }
        if (start < 0) return line;
        int end = start;
        while (end < line.length()
                && (line.charAt(end) == '\u25b0' || line.charAt(end) == '\u25b1')) end++;
        int split = start;
        while (split < end && line.charAt(split) == '\u25b0') split++;
        return line.substring(0, start) + "\u00a76" + line.substring(start, split)
                + "\u00a77" + line.substring(split, end) + line.substring(end);
    }

    /** A progress bar over (into, span). Capped (span &lt;= 0)
     *  renders full. Fractions clamp to [0, 1]. */
    public record BarData(long into, long span) {
        public boolean capped() { return span <= 0; }

        public double fraction() {
            if (capped()) return 1.0;
            double f = (double) into / (double) span;
            if (f < 0) return 0;
            return Math.min(f, 1.0);
        }

        public int percent() { return (int) Math.round(fraction() * 100.0); }

        /** Filled segment count of this bar (0..BAR_SEGMENTS). */
        public int filledCount() {
            return (int) Math.round(fraction() * BAR_SEGMENTS);
        }

        /** The block bar alone, e.g. {@code ▰▰▰▰▰▰▱▱▱▱}. */
        public String bar() {
            int filled = filledCount();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < BAR_SEGMENTS; i++) {
                sb.append(i < filled ? BAR_FILLED : BAR_EMPTY);
            }
            return sb.toString();
        }

        /** The bar with the FILLED portion in GOLD (0.2.1, menu
         *  programme Item 9b \u2014 Mike, Nov 13, 2026: "Make the
         *  colored in parts gold so it shows more easily at a
         *  glance"): legacy {@code \u00a76} (gold) over the filled
         *  segments, {@code \u00a77} (gray) over the empty ones.
         *  This is the STRING form used by the Bedrock form body
         *  (Geyser renders legacy codes in form text); the Java
         *  chest colours the same split with NamedTextColor.GOLD
         *  components in PanelMenus, from the same filledCount. */
        public String coloredBar() {
            int filled = filledCount();
            StringBuilder sb = new StringBuilder("\u00a76");
            for (int i = 0; i < BAR_SEGMENTS; i++) {
                if (i == filled) sb.append("\u00a77");
                sb.append(i < filled ? BAR_FILLED : BAR_EMPTY);
            }
            if (filled == BAR_SEGMENTS) sb.append("\u00a77");
            return sb.toString();
        }

        /** {@link #line()} with the gold-filled bar (Bedrock). */
        public String lineColored() {
            if (capped()) return coloredBar() + " MAX";
            return coloredBar() + " " + percent() + "% \u2014 " + fmt(Math.max(0, into))
                    + " / " + fmt(span) + " XP";
        }

        /** {@link #compact()} with the gold-filled bar (Bedrock). */
        public String compactColored() {
            if (capped()) return coloredBar() + " MAX";
            return coloredBar() + " " + percent() + "%";
        }

        /** The full numbers line: bar, percent, and
         *  "into / span XP" — or MAX at the cap. */
        public String line() {
            if (capped()) return bar() + " MAX";
            return bar() + " " + percent() + "% \u2014 " + fmt(Math.max(0, into))
                    + " / " + fmt(span) + " XP";
        }

        /** The compact one-line form the Bedrock panel body uses:
         *  bar + percent, no counts. */
        public String compact() {
            if (capped()) return bar() + " MAX";
            return bar() + " " + percent() + "%";
        }
    }

    /** One character-header block: a title, its labelled lines,
     *  and an optional progress bar. {@code hasDetail} marks the
     *  block whose tap opens a detail view (the Magic block). */
    public record HeaderBlock(String id, String title, String iconKey,
                              List<String> lines, BarData bar, boolean hasDetail) {
        public HeaderBlock {
            lines = lines == null ? List.of() : List.copyOf(lines);
        }

        /** Lore/body lines: the labelled lines, then the bar line
         *  when the block carries a bar. */
        public List<String> allLines() {
            List<String> out = new ArrayList<>(lines);
            if (bar != null) out.add(bar.line());
            return List.copyOf(out);
        }

        /** {@link #allLines()} with the gold-filled bar (0.2.1)
         *  for string surfaces (the Bedrock panel body). */
        public List<String> allLinesColored() {
            List<String> out = new ArrayList<>(lines);
            if (bar != null) out.add(bar.lineColored());
            return List.copyOf(out);
        }
    }

    /** One per-skill row. {@code rankLabel} is nullable (shown only
     *  when a verified surface supplied one). {@code bar} is
     *  nullable: mcMMO's child skills (Salvage, Smelting) have a
     *  level derived from their parent skills but no XP of their
     *  own — ExperienceAPI's XP reads throw for them — so their
     *  rows honestly carry no bar instead of a fake one. */
    public record SkillRow(String id, String name, String iconKey, int level,
                           BarData bar, String rankLabel, List<String> detailLines) {
        public SkillRow {
            detailLines = detailLines == null ? List.of() : List.copyOf(detailLines);
        }

        /** Bedrock panel-body compact line: "Mining 214 ▰▰▰▰▱▱ 62%",
         *  or "Salvage 12 (child skill)" for a bar-less child row. */
        public String compactLine() {
            if (bar == null) return name + " " + level + " (child skill)";
            return name + " " + level + " " + bar.compact();
        }

        /** {@link #compactLine()} with the gold-filled bar
         *  (0.2.1) for the Bedrock panel body. */
        public String compactLineColored() {
            if (bar == null) return name + " " + level + " (child skill)";
            return name + " " + level + " " + bar.compactColored();
        }

        /** Bedrock panel button label: "Mining — Lv 214". */
        public String buttonLabel() {
            return name + " \u2014 Lv " + level;
        }
    }

    /** The assembled panel: header blocks and skill rows in
     *  provider order, plus the Magic detail view's lines (empty
     *  when no provider supplied a detail view). */
    public record Panel(List<HeaderBlock> headers, List<SkillRow> rows,
                        List<String> magicDetailLines) {
        public Panel {
            headers = headers == null ? List.of() : List.copyOf(headers);
            rows = rows == null ? List.of() : List.copyOf(rows);
            magicDetailLines = magicDetailLines == null ? List.of() : List.copyOf(magicDetailLines);
        }

        public HeaderBlock header(String id) {
            for (HeaderBlock h : headers) if (h.id().equals(id)) return h;
            return null;
        }

        public boolean hasMagicDetail() { return !magicDetailLines.isEmpty(); }

        /** The Bedrock step-1 body: every header block (title,
         *  lines, bar), then one compact line per skill. Bars
         *  render gold-filled (0.2.1, Item 9b) via the colored
         *  variants; the chat fallback keeps the plain bars. */
        public String bedrockBody() {
            StringBuilder sb = new StringBuilder();
            for (HeaderBlock h : headers) {
                sb.append(h.title()).append("\n");
                for (String line : h.allLinesColored()) sb.append(line).append("\n");
                sb.append("\n");
            }
            for (SkillRow r : rows) sb.append(r.compactLineColored()).append("\n");
            return sb.toString().strip();
        }

        /** The chat fallback: the same content as plain chat lines
         *  (a form failure never leaves the player with nothing). */
        public List<String> chatLines() {
            List<String> out = new ArrayList<>();
            out.add("Skills");
            for (HeaderBlock h : headers) {
                out.add(h.title());
                out.addAll(h.allLines());
            }
            for (SkillRow r : rows) out.add(r.compactLine());
            return List.copyOf(out);
        }
    }
}
