package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ExcelProtect 0.1.5 "Clarity" — the item chain-of-custody query
 * (SPEC §3): `/excelprotect item <player> <item> [hours]`. Pure
 * assembly over scanned lines from the economy, items, containers and
 * deaths streams — or from the player's timeline file, whose lines
 * carry the same shapes plus role/stream fields the assembly ignores.
 * Oldest first, one plain-language line per event, delivery outcomes
 * folded into their trade line via the chain id. This is the query
 * the Nov 5 mending-book investigation needed and did not have: the
 * records were complete, but no single answer assembled them.
 * Excel — Wink Reyes Valley
 */
public final class ItemChainReport {
    private ItemChainReport() {}

    public record Report(int total, List<String> lines, int chains) {}

    private record Ev(long tsMs, int rank, String line) {}

    /**
     * Assemble the chain for one player + item over [fromMs, toMs].
     * Item match: the query is a case-insensitive substring of the
     * item id or of its fingerprint label (so "mending", "enchanted_book"
     * and "diamond_sword" all work), as the other queries match.
     */
    public static Report build(List<String> scanned, String who, String itemQuery, long fromMs, long toMs) {
        String q = itemQuery.toLowerCase(java.util.Locale.ROOT);
        // delivery outcomes by chain id (folded into their trade line)
        Map<String, String> deliveredByChain = new HashMap<>();
        for (String line : scanned)
            if ("item_delivery".equals(Json.strField(line, "type"))) {
                String chain = Json.strField(line, "chain");
                String d = Json.strField(line, "delivered");
                if (chain != null && d != null) deliveredByChain.put(chain, d);
            }
        // chains whose trade renders in this window — their delivery
        // records fold into the trade line instead of standing alone
        java.util.Set<String> tradeChains = new java.util.HashSet<>();
        for (String line : scanned) {
            String type = Json.strField(line, "type");
            if (!"villager_trade".equals(type) && !"shop_purchase".equals(type)) continue;
            long ts = (long) Json.numField(line, "ts_ms", -1);
            if (ts < fromMs || ts > toMs) continue;
            if (!matches(Json.rawField(line, "item"), q)) continue;
            String chain = Json.strField(line, "chain");
            if (chain != null) tradeChains.add(chain);
        }
        List<Ev> evs = new ArrayList<>();
        java.util.Set<String> chainsSeen = new java.util.HashSet<>();
        for (String line : scanned) {
            long ts = (long) Json.numField(line, "ts_ms", -1);
            if (ts < fromMs || ts > toMs) continue;
            String type = Json.strField(line, "type");
            if (type == null) continue;
            String rendered = render(line, type, who, q, deliveredByChain, tradeChains);
            if (rendered == null) continue;
            String chain = Json.strField(line, "chain");
            if (chain != null) chainsSeen.add(chain);
            evs.add(new Ev(ts, rank(type), rendered));
        }
        evs.sort(Comparator.comparingLong(Ev::tsMs).thenComparingInt(Ev::rank));
        List<String> out = new ArrayList<>();
        for (Ev e : evs) out.add(e.line());
        return new Report(out.size(), out, chainsSeen.size());
    }

    /** Causal order within one millisecond: acquisition, delivery, ground, storage, recovery of it, loss. */
    private static int rank(String type) {
        return switch (type) {
            case "villager_trade", "shop_purchase", "item_craft" -> 0;
            case "item_delivery" -> 1;
            case "item_drop" -> 2;
            case "container_transaction" -> 3;
            case "item_pickup" -> 4;
            case "item_despawn", "item_destroy", "item_destroyed" -> 5;
            case "lostfound_recovery" -> 6;
            default -> 7;
        };
    }

    private static String render(String line, String type, String who, String q,
                                 Map<String, String> deliveredByChain, java.util.Set<String> tradeChains) {
        String time = timeOf(line);
        return switch (type) {
            case "villager_trade", "shop_purchase" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                StringBuilder b = new StringBuilder(time).append(" TRADE ")
                        .append("villager_trade".equals(type) ? "villager" : "shop")
                        .append(' ').append(fp(raw)).append(locSuffix(line));
                String price = Json.rawField(line, "price");
                if (price != null) b.append(" paid ").append(fp(price));
                String chain = Json.strField(line, "chain");
                String d = chain == null ? null : deliveredByChain.get(chain);
                b.append(" -> delivered ").append(d == null ? "UNRECORDED" : d.toUpperCase(java.util.Locale.ROOT));
                if (chain != null) b.append(" [").append(chain).append(']');
                yield b.toString();
            }
            case "item_delivery" -> {
                // folded into the trade line when the trade renders in
                // this window; standalone only when it does not
                String chain = Json.strField(line, "chain");
                if (chain != null && tradeChains.contains(chain)) yield null;
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                String d = Json.strField(line, "delivered");
                yield time + " DELIVERY " + (d == null ? "UNKNOWN" : d.toUpperCase(java.util.Locale.ROOT))
                        + " " + fp(raw) + locSuffix(line)
                        + (chain == null ? "" : " [" + chain + "]");
            }
            case "item_drop" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                yield time + " DROP " + fp(raw) + locSuffix(line) + chainSuffix(line);
            }
            case "item_pickup" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                String picker = Json.strField(line, "player");
                StringBuilder b = new StringBuilder(time).append(" PICKUP ").append(fp(raw));
                if (picker != null) b.append(" by ").append(picker);
                b.append(locSuffix(line));
                if (Json.strField(line, "chain") != null) b.append(" -> chain in inventory");
                yield b.toString();
            }
            case "item_despawn", "item_destroy" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                String reason = Json.strField(line, "reason");
                yield time + " " + ("item_despawn".equals(type) ? "DESPAWN " : "DESTROYED ")
                        + fp(raw) + locSuffix(line)
                        + " -> chain closed: lost (" + (reason == null ? type.substring(5) : reason) + ")";
            }
            case "item_craft" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                yield time + " CRAFT " + fp(raw) + locSuffix(line) + chainSuffix(line);
            }
            case "container_transaction" -> {
                String dep = matchingFacts(Json.rawField(line, "deposits"), q);
                String wd = matchingFacts(Json.rawField(line, "withdrawals"), q);
                if (dep == null && wd == null) yield null;
                String kind = Json.strField(line, "container");
                StringBuilder b = new StringBuilder(time);
                if (dep != null) b.append(" DEPOSIT ").append(dep).append(" -> ").append(kind == null ? "container" : kind);
                if (wd != null) {
                    if (dep != null) b.append(";");
                    b.append(" WITHDRAW ").append(wd).append(" <- ").append(kind == null ? "container" : kind);
                }
                b.append(locSuffix(line));
                yield b.toString();
            }
            case "item_destroyed" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                String cause = Json.strField(line, "cause");
                yield time + " DESTROYED " + fp(raw) + " (" + (cause == null ? "?" : cause) + ")" + locSuffix(line);
            }
            case "lostfound_recovery" -> {
                String raw = Json.rawField(line, "item");
                if (!matches(raw, q)) yield null;
                yield time + " RECOVERED via Lost & Found " + fp(raw) + locSuffix(line);
            }
            default -> null;
        };
    }

    private static String chainSuffix(String line) {
        String chain = Json.strField(line, "chain");
        if (chain == null) return "";
        String link = Json.strField(line, "link");
        return " [" + chain + (link == null ? "" : " " + link) + "]";
    }

    /** Does this item-fact JSON match the query (id substring or label substring)? */
    static boolean matches(String rawItem, String q) {
        if (rawItem == null) return false;
        String id = Json.strField(rawItem, "id");
        if (id != null && id.toLowerCase(java.util.Locale.ROOT).contains(q)) return true;
        return fp(rawItem).toLowerCase(java.util.Locale.ROOT).contains(q);
    }

    /** Fingerprint label: id#count + custom name + [enchant, keys]. */
    static String fp(String rawItem) {
        if (rawItem == null) return "?";
        String id = Json.strField(rawItem, "id");
        long count = (long) Json.numField(rawItem, "count", 1);
        StringBuilder b = new StringBuilder(id == null ? "?" : id).append('#').append(count);
        String name = Json.strField(rawItem, "name");
        if (name != null) b.append(" \"").append(name).append('"');
        String eraw = Json.rawField(rawItem, "enchants");
        if (eraw != null) {
            List<String> keys = Json.strings(eraw);
            if (!keys.isEmpty()) b.append(" [").append(String.join(", ", keys)).append(']');
        }
        return b.toString();
    }

    /** "id#count, id#count" for the facts in a deposits/withdrawals array that match, or null. */
    private static String matchingFacts(String rawArray, String q) {
        if (rawArray == null) return null;
        List<String> out = new ArrayList<>();
        for (String obj : splitFacts(rawArray)) if (matches(obj, q)) out.add(fp(obj));
        return out.isEmpty() ? null : String.join(", ", out);
    }

    private static List<String> splitFacts(String arr) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = -1; boolean str = false, esc = false;
        for (int i = 0; i < arr.length(); i++) {
            char c = arr.charAt(i);
            if (str) { if (esc) esc = false; else if (c == '\\') esc = true; else if (c == '"') str = false; continue; }
            if (c == '"') str = true;
            else if (c == '{') { if (depth++ == 0) start = i; }
            else if (c == '}') { if (--depth == 0 && start >= 0) { out.add(arr.substring(start, i + 1)); start = -1; } }
        }
        return out;
    }

    private static String timeOf(String line) {
        long ms = (long) Json.numField(line, "ts_ms", 0);
        if (ms <= 0) return "??:??:??";
        String iso = Ledger.isoOf(ms);
        return iso.length() >= 19 ? iso.substring(11, 19) : iso;
    }

    private static String locSuffix(String line) {
        String w = Json.strField(line, "world");
        if (w == null || !line.contains("\"x\":")) return "";
        return " @ " + w + " " + (long) Json.numField(line, "x", 0) + ","
                + (long) Json.numField(line, "y", 0) + "," + (long) Json.numField(line, "z", 0);
    }
}
