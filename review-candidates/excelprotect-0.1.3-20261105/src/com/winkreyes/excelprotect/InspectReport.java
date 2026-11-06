package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ExcelProtect 0.1.1 — text inspect reports for ANY sender (console included).
 * The click-to-inspect mode answers "what happened at this block" for a player
 * standing in the world; these reports answer the same recorded data as text:
 * a player's recent recorded activity, and the recorded history at one position.
 * Pure logic over ledger lines — the command layer does the scanning (Exporter)
 * and this class does the sorting + one-line summaries, so the whole path is
 * unit-testable with no server. Summaries NEVER include item payloads (the
 * "bytes" field) — fingerprints only, like the export listings.
 * Excel — Wink Reyes Valley
 */
public final class InspectReport {
    private InspectReport() {}

    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 25;

    /** A finished report: the summary lines to print (latest first) + how many reportable events matched in total. */
    public record Report(List<String> lines, int total) {}

    /** Limit argument: default 10, clamped to 1..25, non-numeric → default. */
    public static int parseLimit(String s) {
        if (s == null || s.isBlank()) return DEFAULT_LIMIT;
        try {
            int n = Integer.parseInt(s.trim());
            return Math.max(1, Math.min(MAX_LIMIT, n));
        } catch (NumberFormatException e) { return DEFAULT_LIMIT; }
    }

    /** Build a latest-first report from scanned ledger lines (any mix of streams). */
    public static Report build(List<String> scanned, int limit) {
        List<String> sorted = new ArrayList<>(scanned);
        sorted.sort(Comparator.comparingLong((String l) -> (long) Json.numField(l, "ts_ms", 0)).reversed());
        List<String> out = new ArrayList<>();
        int total = 0;
        for (String line : sorted) {
            String s = summarize(line);
            if (s == null) continue;
            total++;
            if (out.size() < limit) out.add(s);
        }
        return new Report(out, total);
    }

    /** One compact line for a ledger event, or null when the type is not inspect-reportable. */
    public static String summarize(String line) {
        String type = Json.strField(line, "type");
        if (type == null) return null;
        String time = timeOf(line);
        return switch (type) {
            case "block_change" -> {
                String action = Json.strField(line, "action");
                String block = Json.strField(line, "block");
                StringBuilder b = new StringBuilder(time).append(" block ").append(action)
                        .append(' ').append(block).append(actorSuffix(line)).append(locSuffix(line));
                if ("sign_edit".equals(action)) {
                    String raw = Json.rawField(line, "sign_after");
                    if (raw != null) {
                        List<String> lines = Json.strings(raw);
                        String joined = String.join(" / ", lines).trim();
                        if (!joined.isEmpty())
                            b.append(" sign: \"").append(joined.length() > 40 ? joined.substring(0, 40) + "…" : joined).append('"');
                    }
                }
                yield b.toString();
            }
            case "container_transaction" -> {
                String kind = Json.strField(line, "container");
                StringBuilder b = new StringBuilder(time).append(" container ").append(kind == null ? "?" : kind)
                        .append(actorSuffix(line)).append(locSuffix(line)).append(':');
                String dep = itemsSuffix(Json.rawField(line, "deposits"));
                String wd = itemsSuffix(Json.rawField(line, "withdrawals"));
                if (dep == null && wd == null) b.append(" no item movement");
                else {
                    if (dep != null) b.append(" deposited ").append(dep);
                    if (wd != null) b.append(" withdrew ").append(wd);
                }
                yield b.toString();
            }
            case "item_drop", "item_pickup", "item_craft" -> {
                String verb = switch (type) { case "item_drop" -> "drop"; case "item_pickup" -> "pickup"; default -> "craft"; };
                String fp = itemFingerprint(line);
                yield fp == null ? null : time + " " + verb + " " + fp + actorSuffix(line) + locSuffix(line);
            }
            case "item_despawn", "item_destroy" -> {
                String fp = itemFingerprint(line);
                String reason = Json.strField(line, "reason");
                yield fp == null ? null : time + " item lost (" + (reason == null ? type.substring(5) : reason) + ") "
                        + fp + locSuffix(line);
            }
            case "death_snapshot" -> {
                String msg = Json.strField(line, "death_message");
                yield time + " death" + (msg == null ? "" : " — " + msg) + locSuffix(line);
            }
            case "item_destroyed" -> {
                String fp = itemFingerprint(line);
                String cause = Json.strField(line, "cause");
                yield fp == null ? null : time + " destroyed (" + (cause == null ? "?" : cause) + ") " + fp
                        + actorSuffix(line) + locSuffix(line);
            }
            default -> null;
        };
    }

    private static String timeOf(String line) {
        long ms = (long) Json.numField(line, "ts_ms", 0);
        if (ms <= 0) return "??:??:??";
        String iso = Ledger.isoOf(ms);
        return iso.length() >= 19 ? iso.substring(11, 19) : iso;
    }

    private static String actorSuffix(String line) {
        String p = Json.strField(line, "player");
        if (p == null) p = Json.strField(line, "owner");
        if (p != null) return " by " + p;
        String cause = Json.strField(line, "cause");
        return cause != null ? " (" + cause + ")" : "";
    }

    private static String locSuffix(String line) {
        String w = Json.strField(line, "world");
        if (w == null || !line.contains("\"x\":")) return "";
        long x = (long) Json.numField(line, "x", 0), y = (long) Json.numField(line, "y", 0),
             z = (long) Json.numField(line, "z", 0);
        return " @ " + w + " " + x + "," + y + "," + z;
    }

    /** Fingerprint (id#count + custom name) of the line's "item" fact — never the payload. */
    private static String itemFingerprint(String line) {
        String raw = Json.rawField(line, "item");
        if (raw == null) return null;
        List<ItemFact> facts = ItemFact.listFromJson("[" + raw + "]");
        return facts.isEmpty() ? null : facts.get(0).fingerprint();
    }

    /** "id#count, id#count" for a deposits/withdrawals array (max 4 shown), or null when absent/empty. */
    private static String itemsSuffix(String rawArray) {
        if (rawArray == null) return null;
        List<ItemFact> facts = ItemFact.listFromJson(rawArray);
        if (facts.isEmpty()) return null;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(4, facts.size()); i++) {
            if (i > 0) b.append(", ");
            b.append(facts.get(i).fingerprint());
        }
        if (facts.size() > 4) b.append(", +").append(facts.size() - 4).append(" more");
        return b.toString();
    }
}
