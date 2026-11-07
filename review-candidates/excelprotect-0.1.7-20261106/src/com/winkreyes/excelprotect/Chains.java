package com.winkreyes.excelprotect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ExcelProtect 0.1.5 "Clarity" — item chain tracker (SPEC §1/§2). An
 * acquisition (villager trade, shop purchase) mints a chain id; later
 * records for the same player + item signature (drop, pickup, despawn,
 * destroy, container deposit/withdrawal) are stamped with that id and a
 * link confidence. In-memory only: chain state is a matching index over
 * records, never a record itself — the streams stay the truth.
 *
 * Signatures are the ItemFact fingerprint minus count: id + enchants +
 * custom name. Vanilla items are not unique instances, so matching is
 * STATE-FILTERED and conservative: candidates must be in a state the
 * event can resolve from (a pickup can only resolve a GROUND chain, a
 * drop only an OPEN/HELD one). One candidate → exact; several → the
 * oldest, marked probable; none → no stamp. A wrong exact is worse
 * than a probable, and a probable is worse than silence only when it
 * hides ambiguity — the item query prints the link kind verbatim.
 * Excel — Wink Reyes Valley
 */
public final class Chains {

    /** Item signature: id + enchants + custom name (count deliberately excluded). */
    public record Sig(String id, Map<String, Integer> enchants, String name) {
        public Sig {
            enchants = enchants == null ? Map.of() : Map.copyOf(new TreeMap<>(enchants));
        }
        public String key() {
            StringBuilder b = new StringBuilder(id).append('|').append(name == null ? "" : name).append('|');
            enchants.forEach((k, v) -> b.append(k).append('=').append(v).append(','));
            return b.toString();
        }
        /** Display label in the ItemFact fingerprint style: id + name + enchant keys. */
        public String label() {
            return id + (name != null ? " \"" + name + "\"" : "")
                    + (enchants.isEmpty() ? "" : " " + enchants.keySet());
        }
    }

    public static Sig of(ItemFact f) {
        return f == null ? null : new Sig(f.id(), f.enchants(), f.name());
    }

    /** Signature from a rendered item-fact JSON object (the streams' shape), or null. */
    public static Sig fromItemJson(String raw) {
        if (raw == null) return null;
        String id = Json.strField(raw, "id");
        if (id == null) return null;
        Map<String, Integer> ench = new TreeMap<>();
        String eraw = Json.rawField(raw, "enchants");
        if (eraw != null) {
            // {"minecraft:mending":1,"minecraft:unbreaking":3} — key:number pairs
            int i = 0;
            while (true) {
                int q1 = eraw.indexOf('"', i);
                if (q1 < 0) break;
                int q2 = eraw.indexOf('"', q1 + 1);
                if (q2 < 0) break;
                String k = eraw.substring(q1 + 1, q2);
                int colon = eraw.indexOf(':', q2);
                if (colon < 0) break;
                int p = colon + 1, q = p;
                while (q < eraw.length() && "-0123456789".indexOf(eraw.charAt(q)) >= 0) q++;
                try { ench.put(k, Integer.parseInt(eraw.substring(p, q))); } catch (Exception ignored) { }
                i = q;
            }
        }
        return new Sig(id, ench, Json.strField(raw, "name"));
    }

    public enum Delivered { INVENTORY, GROUND, UNKNOWN }

    /**
     * Delivery classification (SPEC §1): inventory wins (the delivery
     * provably arrived); ground requires a FRESH matching item entity
     * near the player (the caller's freshness bound is what separates
     * this delivery from an identical older item lying nearby);
     * otherwise unknown — a stated answer, never a guess.
     */
    public static Delivered classify(boolean inInventory, boolean groundFreshNear) {
        if (inInventory) return Delivered.INVENTORY;
        if (groundFreshNear) return Delivered.GROUND;
        return Delivered.UNKNOWN;
    }

    enum State { OPEN, HELD, GROUND, STORED, CLOSED }

    private static final class Chain {
        final String id; final UUID player; final Sig sig;
        State state = State.OPEN;
        long lastMs;
        Chain(String id, UUID player, Sig sig, long nowMs) {
            this.id = id; this.player = player; this.sig = sig; this.lastMs = nowMs;
        }
    }

    private final Settings cfg;
    private final Map<String, Chain> byId = new HashMap<>();
    private final AtomicInteger seq = new AtomicInteger();
    private volatile String seqDay = "";

    public Chains(Settings cfg) { this.cfg = cfg; }

    /** Mint a chain for an acquisition; returns the chain id stamped on the record. */
    public synchronized String mint(UUID player, Sig sig, long nowMs) {
        if (player == null || sig == null) return null;
        prune(nowMs);
        String day = Ledger.dayOf(nowMs);
        if (!day.equals(seqDay)) { seqDay = day; seq.set(0); }
        String id = "ch-" + day.replace("-", "") + "-" + String.format("%04d", seq.incrementAndGet());
        byId.put(id, new Chain(id, player, sig, nowMs));
        return id;
    }

    /** Record the delivery outcome for a minted chain (SPEC §1). */
    public synchronized void noteDelivery(String chainId, Delivered d, long nowMs) {
        Chain c = byId.get(chainId);
        if (c == null) return;
        c.lastMs = nowMs;
        if (c.state == State.OPEN)
            c.state = d == Delivered.GROUND ? State.GROUND : State.HELD;
        // UNKNOWN leaves the state HELD: unproven either way; later
        // drops/pickups still resolve against it, marked as they land.
    }

    /**
     * Match an items/containers event to an open chain and advance its
     * state. kind: drop | pickup | despawn | destroy | deposit |
     * withdraw. Returns {chainId, "exact"|"probable"} or null when no
     * chain can claim the event.
     */
    public synchronized String[] match(UUID player, Sig sig, String kind, long nowMs) {
        if (player == null || sig == null) return null;
        prune(nowMs);
        List<State> from = switch (kind) {
            case "drop" -> List.of(State.OPEN, State.HELD);
            case "pickup" -> List.of(State.GROUND, State.OPEN);
            case "despawn", "destroy" -> List.of(State.GROUND, State.OPEN);
            case "deposit" -> List.of(State.OPEN, State.HELD);
            case "withdraw" -> List.of(State.STORED);
            default -> List.of();
        };
        State to = switch (kind) {
            case "drop", "despawn", "destroy" -> kind.equals("drop") ? State.GROUND : State.CLOSED;
            case "pickup", "withdraw" -> State.HELD;
            case "deposit" -> State.STORED;
            default -> null;
        };
        if (to == null) return null;
        List<Chain> candidates = new ArrayList<>();
        for (Chain c : byId.values())
            if (c.player.equals(player) && c.sig.key().equals(sig.key()) && from.contains(c.state))
                candidates.add(c);
        if (candidates.isEmpty()) return null;
        candidates.sort((a, b) -> Long.compare(a.lastMs, b.lastMs));
        Chain chosen = candidates.get(0);
        chosen.state = to;
        chosen.lastMs = nowMs;
        return new String[]{chosen.id, candidates.size() == 1 ? "exact" : "probable"};
    }

    /** Pickup matching across players: the picker's chains first, then the item owner's. */
    public synchronized String[] matchPickup(UUID picker, UUID owner, Sig sig, long nowMs) {
        String[] m = match(picker, sig, "pickup", nowMs);
        if (m == null && owner != null && !owner.equals(picker)) m = match(owner, sig, "pickup", nowMs);
        return m;
    }

    /** Open chain count (status/tests). */
    public synchronized int openCount() {
        int n = 0;
        for (Chain c : byId.values()) if (c.state != State.CLOSED) n++;
        return n;
    }

    private void prune(long nowMs) {
        long expiry = cfg.chainExpirySeconds() * 1000L;
        Iterator<Chain> it = byId.values().iterator();
        while (it.hasNext()) if (nowMs - it.next().lastMs > expiry) it.remove();
    }
}
