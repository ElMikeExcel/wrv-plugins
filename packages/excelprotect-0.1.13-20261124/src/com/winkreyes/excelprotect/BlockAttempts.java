package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect 0.1.12 (P-PROTECT-EM-1 fix; 0.1.11 was Option A) — BLOCK ATTEMPTS.
 * The pure coalescer: consecutive cancelled block break/place attempts by
 * the same player at the same position, with the same action and block
 * type, within the gap window merge into ONE block_attempt record on the
 * separate `attempts` stream. No Bukkit types — the Bukkit glue
 * (BlockAttemptWatch) resolves player/block/position and feeds attempts
 * in; closed attempts render through Json/Ledger exactly like every other
 * stream. An attempt changed no block: the record deliberately carries
 * NO before/after/sign fields, so no rollback path can ever select it.
 *
 * 0.1.12 adds the additive `via` field (DIAGNOSIS §5.1.4, decision D95):
 * the observation point that saw the attempt — "damage" (cancelled
 * BlockDamageEvent), "can_build" (denied BlockCanBuildEvent), "break" /
 * "place" (the terminal-event backstops). `via` is diagnostic ONLY: it
 * is deliberately NOT part of the coalescer key, so an early-stage feed
 * and a terminal backstop feed for the same physical attempt still merge
 * into ONE record (the no-double-count invariant, DIAGNOSIS §5.1.2);
 * the record keeps the FIRST feed's via — the earliest observation point.
 * Excel — Wink Reyes Valley
 */
public final class BlockAttempts {

    /** One resolved attempt, fed by the glue. x/y/z are block coordinates. */
    public static final class Attempt {
        public final UUID playerUuid;
        public final String player, world, action, block, via;
        public final int x, y, z;
        public final long ms;

        public Attempt(UUID playerUuid, String player, String world, int x, int y, int z,
                       String action, String block, long ms) {
            this(playerUuid, player, world, x, y, z, action, block, defaultVia(action), ms);
        }

        public Attempt(UUID playerUuid, String player, String world, int x, int y, int z,
                       String action, String block, String via, long ms) {
            this.playerUuid = playerUuid; this.player = player; this.world = world;
            this.x = x; this.y = y; this.z = z;
            this.action = action; this.block = block;
            this.via = (via == null || via.isEmpty()) ? defaultVia(action) : via;
            this.ms = ms;
        }

        /** The terminal-event backstop via for an action (legacy feeds). */
        static String defaultVia(String action) {
            return "place".equals(action) ? "place" : "break";
        }
    }

    /** One open (still merging) or closed attempt record. */
    public static final class Record {
        public final UUID playerUuid;
        public final String player, world, action, block, via;
        public final int x, y, z;
        public final long firstMs;
        public long lastMs;
        public int count;
        public String closed; // window | quit | shutdown

        Record(Attempt a) {
            this.playerUuid = a.playerUuid; this.player = a.player; this.world = a.world;
            this.x = a.x; this.y = a.y; this.z = a.z;
            this.action = a.action; this.block = a.block; this.via = a.via;
            this.firstMs = a.ms; this.lastMs = a.ms;
            merge(a);
        }

        void merge(Attempt a) {
            count++;
            lastMs = a.ms;
        }

        // NOTE (0.1.12): `via` is intentionally NOT part of the key — see
        // the class javadoc. One physical attempt = at most one record,
        // whichever observation point(s) fed it.
        static String keyOf(String world, int x, int y, int z, String action, String block) {
            return world + "|" + x + "," + y + "," + z + "|" + action + "|" + block;
        }

        String key() { return keyOf(world, x, y, z, action, block); }

        /** The record body (the Ledger envelope adds ts/ts_ms + world/x/y/z). */
        public Json.Obj render() {
            return new Json.Obj().put("type", "block_attempt")
                    .put("player", player).put("player_uuid", playerUuid.toString())
                    .put("action", action).put("block", block)
                    .put("via", via)
                    .put("cancelled", true)
                    .put("count", count)
                    .put("first_ts_ms", firstMs)
                    .put("last_ts_ms", lastMs)
                    .put("closed", closed);
        }
    }

    private final long gapMs;
    private final Map<UUID, Map<String, Record>> open = new HashMap<>();

    public BlockAttempts(long gapMs) {
        this.gapMs = Math.max(0, gapMs);
    }

    public long gapMs() { return gapMs; }

    /**
     * Feed one attempt. An open record for the same key whose gap expired
     * closes first (reason "window") and a fresh record starts; returns the
     * records this attempt closed (0 or 1).
     */
    public List<Record> feed(Attempt a) {
        Map<String, Record> byKey = open.computeIfAbsent(a.playerUuid, k -> new HashMap<>());
        String key = Record.keyOf(a.world, a.x, a.y, a.z, a.action, a.block);
        Record cur = byKey.get(key);
        List<Record> closedOut = new ArrayList<>();
        if (cur != null && a.ms - cur.lastMs > gapMs) {
            cur.closed = "window";
            byKey.remove(key);
            closedOut.add(cur);
            cur = null;
        }
        if (cur == null) byKey.put(key, new Record(a));
        else cur.merge(a);
        return closedOut;
    }

    /** Alias in the DamageIncidents vocabulary. */
    public List<Record> record(Attempt a) { return feed(a); }

    /** Close every record whose gap has expired by nowMs (the sweep tick). */
    public List<Record> tick(long nowMs) {
        List<Record> out = new ArrayList<>();
        open.values().forEach(byKey -> byKey.values().removeIf(rec -> {
            if (nowMs - rec.lastMs > gapMs) { rec.closed = "window"; out.add(rec); return true; }
            return false;
        }));
        open.values().removeIf(Map::isEmpty);
        return out;
    }

    /** Alias in the DamageIncidents vocabulary. */
    public List<Record> sweep(long nowMs) { return tick(nowMs); }

    /** Close all of one player's open records (quit). */
    public List<Record> closePlayer(UUID uuid, String reason) {
        Map<String, Record> byKey = open.remove(uuid);
        if (byKey == null || byKey.isEmpty()) return List.of();
        List<Record> out = new ArrayList<>(byKey.values());
        out.sort((a, b) -> Long.compare(a.lastMs, b.lastMs));
        for (Record rec : out) rec.closed = reason;
        return out;
    }

    /** Close everything (plugin shutdown, so no attempt tail is lost). */
    public List<Record> closeAll(String reason) {
        List<Record> out = new ArrayList<>();
        for (UUID u : new ArrayList<>(open.keySet())) out.addAll(closePlayer(u, reason));
        return out;
    }

    public int openCount() {
        int n = 0;
        for (Map<String, Record> m : open.values()) n += m.size();
        return n;
    }
}
