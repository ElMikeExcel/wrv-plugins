package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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

    // ------------------------------------------------- 0.1.4: all-stream sections
    /**
     * One stream's slice of an inspect-player answer (0.1.4): the latest
     * {@code shown} one-liners, how many reportable events the stream holds
     * in total for this player, and how many were cut (the "…and N more"
     * tail the command layer prints). {@code samples} is the locations
     * stream's raw sample count — samples are counted, never itemized
     * (the route is /excelprotect where's job).
     */
    public record Section(String stream, List<String> lines, int total, int shown, int samples) {
        public int more() { return total - shown; }
    }

    /** Section display order: presence first, then combat, then world/economy, then meta. */
    public static final List<String> SECTION_ORDER = List.of(
            "players", "locations", "combat", "damage", "dealt", "healing",
            "economy", "containers", "items", "blocks", "entities", "deaths",
            "snapshots", "audit", "admin", "system");

    /**
     * Build per-stream sections from per-stream scanned lines for one
     * player. Streams with no reportable events are omitted entirely.
     * {@code perCap} caps EACH section's lines (the [n] argument).
     */
    public static List<Section> buildSections(Map<String, List<String>> byStream, String who, int perCap) {
        return buildSections(byStream, who, perCap, System.currentTimeMillis());
    }

    /**
     * As {@link #buildSections(Map, String, int)} with the query's
     * "now" — 0.1.5 uses it for the items section's drop pairing:
     * each drop is annotated with its resolution (picked up / despawned
     * / destroyed) or flagged UNRECOVERED with its age (SPEC §4).
     */
    public static List<Section> buildSections(Map<String, List<String>> byStream, String who, int perCap, long nowMs) {
        List<Section> out = new ArrayList<>();
        for (String stream : SECTION_ORDER) {
            List<String> scanned = byStream.get(stream);
            if (scanned == null || scanned.isEmpty()) continue;
            Map<String, String> pairing = "items".equals(stream) ? pairDrops(scanned, nowMs) : Map.of();
            List<String> sorted = new ArrayList<>(scanned);
            sorted.sort(Comparator.comparingLong((String l) -> (long) Json.numField(l, "ts_ms", 0)).reversed());
            List<String> lines = new ArrayList<>();
            int total = 0, samples = 0;
            for (String line : sorted) {
                if ("location_sample".equals(Json.strField(line, "type"))) { samples++; continue; }
                String s = summarize(line, who);
                if (s == null) continue;
                String suffix = pairing.get(line);
                if (suffix != null) s += suffix;
                total++;
                if (lines.size() < perCap) lines.add(s);
            }
            if (total == 0 && samples == 0) continue;
            if (samples > 0)
                lines.add("· " + samples + " location sample(s) — full route: /excelprotect where " + who);
            out.add(new Section(stream, lines, total, Math.min(total, perCap), samples));
        }
        return out;
    }

    // ------------------------------------------------- 0.1.5: drop pairing
    private record DropEv(String line, long ts, Chains.Sig sig, String playerUuid, String chain) {}
    private record ResEv(long ts, String type, Chains.Sig sig, String player, String playerUuid,
                         String ownerUuid, String chain, String reason) {}

    /**
     * Pair each item_drop with the earliest later resolution of the
     * same item: a pickup (by the dropper, or of the dropper's item),
     * a despawn, or a destroy. Chain ids, when both sides carry them,
     * decide the match; otherwise the item signature + ownership does.
     * Returns drop-line → annotation suffix; unpaired drops are
     * UNRECOVERED with their age at {@code nowMs}.
     */
    static Map<String, String> pairDrops(List<String> itemsLines, long nowMs) {
        List<DropEv> drops = new ArrayList<>();
        List<ResEv> resolutions = new ArrayList<>();
        for (String line : itemsLines) {
            String type = Json.strField(line, "type");
            if (type == null) continue;
            long ts = (long) Json.numField(line, "ts_ms", 0);
            Chains.Sig sig = Chains.fromItemJson(Json.rawField(line, "item"));
            switch (type) {
                case "item_drop" -> drops.add(new DropEv(line, ts, sig,
                        Json.strField(line, "player_uuid"), Json.strField(line, "chain")));
                case "item_pickup", "item_despawn", "item_destroy" -> resolutions.add(new ResEv(ts, type, sig,
                        Json.strField(line, "player"), Json.strField(line, "player_uuid"),
                        Json.strField(line, "owner_uuid"), Json.strField(line, "chain"),
                        Json.strField(line, "reason")));
                default -> { }
            }
        }
        drops.sort(Comparator.comparingLong(DropEv::ts));
        resolutions.sort(Comparator.comparingLong(ResEv::ts));
        boolean[] used = new boolean[resolutions.size()];
        Map<String, String> out = new java.util.IdentityHashMap<>();
        for (DropEv d : drops) {
            int pick = -1;
            for (int i = 0; i < resolutions.size(); i++) {
                if (used[i]) continue;
                ResEv r = resolutions.get(i);
                if (r.ts() < d.ts()) continue;
                if (!resolves(d, r)) continue;
                pick = i;
                break;
            }
            if (pick >= 0) {
                used[pick] = true;
                ResEv r = resolutions.get(pick);
                out.put(d.line(), switch (r.type()) {
                    case "item_pickup" -> " -> picked up " + clockOf(r.ts())
                            + (r.player() == null ? "" : " by " + r.player());
                    case "item_despawn" -> " -> despawned " + clockOf(r.ts());
                    default -> " -> destroyed (" + (r.reason() == null ? "?" : r.reason()) + ") " + clockOf(r.ts());
                });
            } else {
                out.put(d.line(), " -> UNRECOVERED (" + fmtAge(nowMs - d.ts()) + " ago)");
            }
        }
        return out;
    }

    private static boolean resolves(DropEv d, ResEv r) {
        // chain ids on both sides decide outright
        if (d.chain() != null && r.chain() != null) return d.chain().equals(r.chain());
        if (d.sig() == null || r.sig() == null || !d.sig().key().equals(r.sig().key())) return false;
        return switch (r.type()) {
            case "item_pickup" -> (d.playerUuid() != null && d.playerUuid().equals(r.playerUuid()))
                    || (d.playerUuid() != null && d.playerUuid().equals(r.ownerUuid()));
            case "item_despawn", "item_destroy" -> r.ownerUuid() == null
                    || (d.playerUuid() != null && d.playerUuid().equals(r.ownerUuid()));
            default -> false;
        };
    }

    private static String clockOf(long ms) {
        if (ms <= 0) return "??:??:??";
        String iso = Ledger.isoOf(ms);
        return iso.length() >= 19 ? iso.substring(11, 19) : iso;
    }

    /** Compact age: 45m / 2h14m / 3d. */
    static String fmtAge(long ms) {
        long m = Math.max(0, ms / 60_000);
        if (m < 60) return m + "m";
        long h = m / 60;
        if (h < 48) return h + "h" + (m % 60 == 0 ? "" : (m % 60) + "m");
        return (h / 24) + "d";
    }

    /** One compact line for a ledger event, or null when the type is not inspect-reportable. */
    public static String summarize(String line) { return summarize(line, null); }

    /**
     * As {@link #summarize(String)} with the inspected player's name for the
     * direction-sensitive summaries (healing). {@code who} may be null (the
     * legacy merged-scan path), in which case healing renders from the
     * target's perspective.
     */
    public static String summarize(String line, String who) {
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
            // ---- 0.1.4: every remaining stream becomes inspect-reportable ----
            case "player_join" -> time + " join" + locSuffix(line);
            case "player_leave" -> time + " leave" + locSuffix(line);
            case "player_teleport" -> {
                String cause = Json.strField(line, "cause");
                yield time + " teleport" + (cause == null ? "" : " (" + cause + ")") + " → "
                        + Json.strField(line, "to_world") + " "
                        + (long) Json.numField(line, "to_x", 0) + ","
                        + (long) Json.numField(line, "to_y", 0) + ","
                        + (long) Json.numField(line, "to_z", 0);
            }
            case "player_command" -> {
                String cmd = Json.strField(line, "command");
                yield cmd == null ? null : time + " command " + trunc(cmd, 60);
            }
            case "player_chat" -> {
                String msg = Json.strField(line, "message");
                yield msg == null ? null : time + " chat: " + trunc(msg, 60);
            }
            case "player_death" -> {
                String msg = Json.strField(line, "death_message");
                yield time + " death" + (msg == null ? "" : " — " + msg) + locSuffix(line);
            }
            case "location_anchor" -> WhereReport.summarizeAnchor(line);
            case "damage_incident" -> DamageReport.summarize(line);
            case "damage_dealt_incident" -> DealtReport.summarize(line);
            case "healing_incident" -> HealingReport.summarize(line,
                    who != null ? who : Json.strField(line, "target"));
            case "combat_damage" -> {
                String attacker = Json.strField(line, "attacker");
                String src = attacker != null ? attacker : shortId(Json.strField(line, "attacker_entity"));
                String victim = shortId(Json.strField(line, "victim"));
                String cause = Json.strField(line, "cause");
                yield time + " combat: " + src + " hit " + victim + " for "
                        + Json.num(Json.numField(line, "amount", 0))
                        + (cause == null ? "" : " (" + cause.toLowerCase().replace('_', ' ') + ")");
            }
            case "combat_heal" -> {
                String target = Json.strField(line, "target");
                String source = Json.strField(line, "source");
                String spell = Json.strField(line, "spell");
                String healer = Json.strField(line, "healer");
                yield time + " combat heal: " + (target == null ? "?" : target) + " +"
                        + Json.num(Json.numField(line, "amount", 0))
                        + (source == null ? "" : " (" + source + ")")
                        + (spell == null ? "" : " [" + spell + "]")
                        + (healer == null ? "" : " by " + healer);
            }
            case "fight_summary" -> {
                String raw = Json.rawField(line, "players");
                int n = raw == null ? 0 : raw.split("\"damage_dealt\"", -1).length - 1;
                String enc = Json.strField(line, "encounter");
                yield time + " fight" + (enc == null ? "" : " " + enc) + " — "
                        + Json.num(Json.numField(line, "total_damage", 0)) + " total dmg, "
                        + n + " combatant(s)";
            }
            case "money" -> {
                double delta = Json.numField(line, "delta", 0);
                yield time + " money " + (delta >= 0 ? "+" : "") + Json.num(delta)
                        + " (balance " + Json.num(Json.numField(line, "balance", 0)) + ")";
            }
            case "service_charge" -> {
                String svc = Json.strField(line, "service");
                yield time + " service charge" + (svc == null ? "" : " (" + svc + ")") + " "
                        + Json.num(Json.numField(line, "amount", 0));
            }
            // ---- 0.1.5: trades + deliveries become inspect-reportable
            // (0.1.4's economy section knew only money + service charges,
            // so a trade was invisible to inspect even though the stream
            // held it — one of the Nov 5 investigation's clarity gaps).
            case "villager_trade" -> {
                String fp = itemFingerprint(line);
                String priceRaw = Json.rawField(line, "price");
                String price = priceRaw == null ? null : fingerprintOfRaw(priceRaw);
                yield fp == null ? null : time + " villager trade " + fp
                        + (price == null ? "" : " for " + price) + actorSuffix(line) + locSuffix(line);
            }
            case "shop_purchase" -> {
                String fp = itemFingerprint(line);
                yield time + " shop purchase" + (fp == null ? "" : " " + fp) + actorSuffix(line) + locSuffix(line);
            }
            case "item_delivery" -> {
                String fp = itemFingerprint(line);
                String d = Json.strField(line, "delivered");
                yield time + " delivery " + (d == null ? "unknown" : d)
                        + (fp == null ? "" : " " + fp) + actorSuffix(line) + locSuffix(line);
            }
            case "jobs_payout" -> time + " jobs payout "
                    + Json.num(Json.numField(line, "amount", 0)) + actorSuffix(line);
            case "entity_death" -> {
                String ent = shortId(Json.strField(line, "entity"));
                String killerRaw = Json.rawField(line, "killer");
                String killer = killerRaw == null ? null : Json.strField(killerRaw, "name");
                yield time + " entity death " + ent + (killer == null ? "" : " by " + killer) + locSuffix(line);
            }
            case "entity_spawn" -> time + " entity spawn " + shortId(Json.strField(line, "entity")) + locSuffix(line);
            case "entity_change" -> time + " entity change " + shortId(Json.strField(line, "entity")) + locSuffix(line);
            case "entity_tame" -> {
                String owner = Json.strField(line, "owner");
                yield time + " entity tamed " + shortId(Json.strField(line, "entity"))
                        + (owner == null ? "" : " by " + owner) + locSuffix(line);
            }
            case "xp_level" -> time + " xp level " + (long) Json.numField(line, "old_level", 0)
                    + "→" + (long) Json.numField(line, "new_level", 0);
            case "enchant" -> time + " enchant (cost " + (long) Json.numField(line, "cost", 0) + ")" + locSuffix(line);
            case "enderchest_open" -> time + " ender chest opened" + locSuffix(line);
            case "enderchest_close" -> time + " ender chest closed" + locSuffix(line);
            case "inventory_snapshot" -> {
                // compact by design: reason + item COUNT only — the fingerprint
                // list itself is exactly the bulk the 0.1.1 scope rule kept out
                // of chat, and it stays out; the stream is askable, not dumped.
                String raw = Json.rawField(line, "items");
                int n = raw == null ? 0 : ItemFact.listFromJson(raw).size();
                String reason = Json.strField(line, "reason");
                yield time + " inventory snapshot" + (reason == null ? "" : " (" + reason + ")")
                        + " — " + n + " item(s)";
            }
            case "mcmmo_snapshot" -> {
                String reason = Json.strField(line, "reason");
                yield time + " mcMMO snapshot" + (reason == null ? "" : " (" + reason + ")")
                        + " — power level " + (long) Json.numField(line, "power_level", 0);
            }
            case "mcmmo_level_up" -> {
                String skill = Json.strField(line, "skill");
                yield time + " mcMMO level up" + (skill == null ? "" : " " + skill)
                        + " → " + (long) Json.numField(line, "level", 0);
            }
            case "lostfound_recovery" -> {
                String fp = itemFingerprint(line);
                yield fp == null ? null : time + " recovered " + fp
                        + (line.contains("\"free\":true") ? " (free)" : " ($" + (long) Json.numField(line, "price", 0) + ")");
            }
            case "admin" -> {
                String action = Json.strField(line, "action");
                String detail = Json.strField(line, "detail");
                yield time + " admin " + (action == null ? "?" : action)
                        + (detail == null ? "" : ": " + trunc(detail, 50));
            }
            default -> null;
        };
    }

    private static String trunc(String s, int max) {
        return s.length() > max ? s.substring(0, max) + "…" : s;
    }

    private static String shortId(String key) {
        if (key == null) return "?";
        return key.startsWith("minecraft:") ? key.substring("minecraft:".length()) : key;
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
        return fingerprintOfRaw(raw);
    }

    private static String fingerprintOfRaw(String rawFact) {
        List<ItemFact> facts = ItemFact.listFromJson("[" + rawFact + "]");
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
