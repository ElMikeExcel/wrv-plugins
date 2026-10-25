package com.winkreyes.excelvaults.core;

import java.util.*;

/**
 * In-memory cooldown records: vault -> (player -> openedAt ms). openedAt 0 = reopen pending.
 * Also per-player paid-reopen counts per day (for the optional daily limit).
 * Plain maps so it can be unit tested; the plugin persists it with YAML.
 */
public final class Store {
    public static final class Rec {
        public boolean ominous;
        public final Map<UUID, Long> opened = new LinkedHashMap<>();
    }
    private final Map<String, Rec> vaults = new TreeMap<>();
    /** uuid -> "yyyy-mm-dd|normal" / "yyyy-mm-dd|ominous" -> count */
    private final Map<UUID, Map<String, Integer>> paid = new HashMap<>();
    private boolean dirty;

    public Long get(VaultKey k, UUID p) { Rec r = vaults.get(k.toString()); return r == null ? null : r.opened.get(p); }
    public boolean isOminous(VaultKey k) { Rec r = vaults.get(k.toString()); return r != null && r.ominous; }

    public void put(VaultKey k, UUID p, long openedAt, boolean ominous) {
        Rec r = vaults.computeIfAbsent(k.toString(), x -> new Rec());
        r.ominous = ominous;
        r.opened.put(p, openedAt);
        dirty = true;
    }
    public boolean remove(VaultKey k, UUID p) {
        Rec r = vaults.get(k.toString());
        if (r == null || r.opened.remove(p) == null) return false;
        if (r.opened.isEmpty()) vaults.remove(k.toString());
        dirty = true;
        return true;
    }
    public void removeVault(VaultKey k) { if (vaults.remove(k.toString()) != null) dirty = true; }

    public record Entry(VaultKey key, UUID player, long openedAt, boolean ominous) {}

    public List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (Map.Entry<String, Rec> e : vaults.entrySet()) {
            VaultKey k = VaultKey.parse(e.getKey());
            if (k == null) continue;
            for (Map.Entry<UUID, Long> o : e.getValue().opened.entrySet()) out.add(new Entry(k, o.getKey(), o.getValue(), e.getValue().ominous));
        }
        return out;
    }
    public List<Entry> forPlayer(UUID p) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : all()) if (e.player().equals(p)) out.add(e);
        return out;
    }
    /** Admin reset: every recorded vault of this player becomes reopen-pending. Returns how many. */
    public int resetPlayer(UUID p) {
        int n = 0;
        for (Rec r : vaults.values()) if (r.opened.containsKey(p)) { r.opened.put(p, 0L); n++; }
        if (n > 0) dirty = true;
        return n;
    }
    /** Clears this player's paid-reopen counts (all days). Returns how many entries were removed. */
    public int clearPaid(UUID p) {
        Map<String, Integer> m = paid.remove(p);
        if (m == null || m.isEmpty()) return 0;
        dirty = true;
        return m.size();
    }
    /** Test/support hook: move this player's open times back (cooldown passes sooner). */
    public int age(UUID p, long ms) {
        int n = 0;
        for (Rec r : vaults.values()) {
            Long t = r.opened.get(p);
            if (t != null && t > 0) { r.opened.put(p, Math.max(1, t - ms)); n++; }
        }
        if (n > 0) dirty = true;
        return n;
    }
    public int vaultCount() { return vaults.size(); }
    public int recordCount() { int n = 0; for (Rec r : vaults.values()) n += r.opened.size(); return n; }

    public int paidToday(UUID p, String day, boolean ominous) {
        Map<String, Integer> m = paid.get(p);
        return m == null ? 0 : m.getOrDefault(day + "|" + (ominous ? "ominous" : "normal"), 0);
    }
    public void addPaid(UUID p, String day, boolean ominous) {
        Map<String, Integer> m = paid.computeIfAbsent(p, x -> new TreeMap<>());
        m.merge(day + "|" + (ominous ? "ominous" : "normal"), 1, Integer::sum);
        m.keySet().removeIf(key -> !key.startsWith(day + "|")); // keep only today
        dirty = true;
    }

    public boolean dirty() { return dirty; }
    public void clean() { dirty = false; }

    // ---- plain-map form for YAML (keeps this class free of Bukkit) ----
    public Map<String, Object> toMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("format", 1);
        Map<String, Object> vs = new LinkedHashMap<>();
        for (Map.Entry<String, Rec> e : vaults.entrySet()) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("ominous", e.getValue().ominous);
            Map<String, Object> ps = new LinkedHashMap<>();
            for (Map.Entry<UUID, Long> o : e.getValue().opened.entrySet()) ps.put(o.getKey().toString(), o.getValue());
            v.put("players", ps);
            vs.put(e.getKey(), v);
        }
        root.put("vaults", vs);
        Map<String, Object> pd = new LinkedHashMap<>();
        for (Map.Entry<UUID, Map<String, Integer>> e : paid.entrySet()) if (!e.getValue().isEmpty()) pd.put(e.getKey().toString(), new LinkedHashMap<>(e.getValue()));
        root.put("paid", pd);
        return root;
    }

    /** Lenient: bad keys or values are skipped and counted. */
    @SuppressWarnings("unchecked")
    public int fromMap(Map<String, Object> root) {
        vaults.clear(); paid.clear();
        int skipped = 0;
        if (root == null) return 0;
        Object vs = root.get("vaults");
        if (vs instanceof Map<?, ?> vm) for (Map.Entry<?, ?> e : vm.entrySet()) {
            VaultKey k = VaultKey.parse(String.valueOf(e.getKey()));
            if (k == null || !(e.getValue() instanceof Map<?, ?> v)) { skipped++; continue; }
            Rec r = new Rec();
            r.ominous = Boolean.TRUE.equals(v.get("ominous"));
            if (v.get("players") instanceof Map<?, ?> ps) for (Map.Entry<?, ?> o : ps.entrySet()) {
                try { r.opened.put(UUID.fromString(String.valueOf(o.getKey())), ((Number) o.getValue()).longValue()); }
                catch (RuntimeException bad) { skipped++; }
            }
            if (!r.opened.isEmpty()) vaults.put(k.toString(), r);
        }
        Object pd = root.get("paid");
        if (pd instanceof Map<?, ?> pm) for (Map.Entry<?, ?> e : pm.entrySet()) {
            try {
                UUID u = UUID.fromString(String.valueOf(e.getKey()));
                Map<String, Integer> m = new TreeMap<>();
                if (e.getValue() instanceof Map<?, ?> c) for (Map.Entry<?, ?> x : c.entrySet()) m.put(String.valueOf(x.getKey()), ((Number) x.getValue()).intValue());
                paid.put(u, m);
            } catch (RuntimeException bad) { skipped++; }
        }
        dirty = false;
        return skipped;
    }
}
