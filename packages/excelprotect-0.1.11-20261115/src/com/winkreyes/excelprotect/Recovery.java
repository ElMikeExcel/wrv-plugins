package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelProtect — the RECOVERY domain (SPEC §3.6/§3.7): Lost & Found over the burn
 * ledger + staff death-snapshot restores, with DeathRecord's semantics — once-only
 * restore marks, the free-first rule chain (once ever, only if no earlier recovery),
 * per-player state, tell-owner notices, economy-audited charges. Marks and player
 * state are small JSON files in the data folder (restore-grade state, exempt from
 * retention). Excel — Wink Reyes Valley
 */
public final class Recovery {
    private final Ledger ledger;
    private final Settings cfg;
    private volatile Watch watch;
    private final File marksFile, playersFile;

    public void setWatch(Watch w) { this.watch = w; }
    private final Set<String> restored = new HashSet<>();
    private final Map<String, PlayerState> players = new HashMap<>();

    static final class PlayerState {
        boolean freeUsed; long freeUsedEpoch; int recoveries; long lastRecoveryEpoch;
    }

    public record LedgerEntry(String id, long tsMs, String ownerUuid, String ownerName,
                              String cause, ItemFact item, String line) { }

    public Recovery(Ledger ledger, Settings cfg, File dataDir) {
        this.ledger = ledger; this.cfg = cfg;
        marksFile = new File(dataDir, "restore-marks.json");
        playersFile = new File(dataDir, "lostfound-players.json");
        load();
    }

    // ---------- persistence (small files; written on change) ----------
    private synchronized void load() {
        try {
            if (marksFile.exists())
                for (String s : Files.readString(marksFile.toPath(), StandardCharsets.UTF_8).split("[,\\[\\]\"]+"))
                    if (s.length() > 8) restored.add(s.trim());
        } catch (Exception ignored) { }
        try {
            if (playersFile.exists()) {
                String all = Files.readString(playersFile.toPath(), StandardCharsets.UTF_8);
                for (String obj : splitObjects(all)) {
                    String uuid = Json.strField(obj, "uuid");
                    if (uuid == null) continue;
                    PlayerState st = new PlayerState();
                    st.freeUsed = obj.contains("\"free_used\":true");
                    st.freeUsedEpoch = (long) Json.numField(obj, "free_used_epoch", 0);
                    st.recoveries = (int) Json.numField(obj, "recoveries", 0);
                    st.lastRecoveryEpoch = (long) Json.numField(obj, "last_recovery_epoch", 0);
                    players.put(uuid, st);
                }
            }
        } catch (Exception ignored) { }
    }

    private synchronized void save() {
        try {
            StringBuilder m = new StringBuilder("[");
            boolean first = true;
            for (String s : restored) { if (!first) m.append(','); m.append('"').append(s).append('"'); first = false; }
            Files.writeString(marksFile.toPath(), m.append(']').toString(), StandardCharsets.UTF_8);
            Json.Arr arr = new Json.Arr();
            players.forEach((uuid, st) -> arr.add(new Json.Obj().put("uuid", uuid)
                    .put("free_used", st.freeUsed).put("free_used_epoch", st.freeUsedEpoch)
                    .put("recoveries", st.recoveries).put("last_recovery_epoch", st.lastRecoveryEpoch)));
            Files.writeString(playersFile.toPath(), arr.render(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    private static List<String> splitObjects(String arr) {
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

    // ---------- burn ledger ----------
    /** Record a destroyed item for an OFFLINE owner (uuid + name only). */
    public void recordDestroyedOffline(java.util.UUID ownerUuid, String ownerName, String cause,
                                       Location loc, ItemStack stack, long ageTicks) {
        if (!cfg.burnLedger() || stack == null) return;
        if (ageTicks >= 0 && ageTicks < cfg.ghostMinAgeTicks()) return;
        ItemFact f = ItemFact.of(stack, cfg.payloadAll()
                || cfg.payloadFor(stack.getType().getKey().toString()));
        if (f == null) return;
        Json.Obj o = new Json.Obj().put("type", "item_destroyed").put("cause", cause).put("item", f.toJson());
        if (ownerUuid != null) o.put("owner_uuid", ownerUuid.toString());
        if (ownerName != null) o.put("owner", ownerName);
        ledger.emit("deaths", loc, o);
    }

    /** Record a destroyed item in the deaths stream and (config) tell the owner. */
    public void recordDestroyed(Player owner, String cause, Location loc, ItemStack stack, long ageTicks) {
        if (!cfg.burnLedger() || stack == null) return;
        if (ageTicks >= 0 && ageTicks < cfg.ghostMinAgeTicks()) return; // ghost exclusion
        boolean valuable = stack.getItemMeta() != null && (stack.getItemMeta().hasDisplayName()
                || stack.getItemMeta().hasEnchants())
                || cfg.payloadFor(stack.getType().getKey().toString());
        ItemFact f = ItemFact.of(stack, cfg.payloadAll() || valuable);
        if (f == null) return;
        Json.Obj o = new Json.Obj().put("type", "item_destroyed")
                .put("cause", cause)
                .put("item", f.toJson());
        if (owner != null) o.put("owner", owner.getName()).put("owner_uuid", owner.getUniqueId().toString());
        ledger.emit("deaths", loc, o);
        Watch w = watch;
        if (w != null && f.payload() != null) w.valuableDestroyed(owner, cause, f, loc);
        if (owner != null && cfg.lfTellOwner() && f.payload() != null) {
            owner.sendMessage("§e[Lost & Found] §fYour " + stack.getType().name().toLowerCase()
                    + " was destroyed (" + cause.toLowerCase() + "). Type /lostfound within "
                    + cfg.lfWindowDays() + " days to recover it.");
        }
    }

    /** A player's recoverable ledger entries: owned, payload-carrying, in window, unrestored. */
    public List<LedgerEntry> recoverable(UUID player) {
        List<LedgerEntry> out = new ArrayList<>();
        long cutoff = System.currentTimeMillis() - cfg.lfWindowDays() * 86_400_000L;
        for (File file : ledger.streamFiles("deaths")) {
            try (var r = Ledger.open(file)) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (!line.contains("\"item_destroyed\"")) continue;
                    long ts = (long) Json.numField(line, "ts_ms", 0);
                    long orig = (long) Json.numField(line, "orig_ts_ms", 0);
                    if ((orig > 0 ? orig : ts) < cutoff) continue;
                    if (!player.toString().equals(Json.strField(line, "owner_uuid"))) continue;
                    String itemRaw = Json.rawField(line, "item");
                    if (itemRaw == null) continue;
                    List<ItemFact> facts = ItemFact.listFromJson("[" + itemRaw + "]");
                    if (facts.isEmpty() || facts.get(0).payload() == null) continue;
                    String id = ts + ":" + facts.get(0).fingerprint();
                    if (restored.contains(id)) continue;
                    out.add(new LedgerEntry(id, ts, player.toString(), Json.strField(line, "owner"),
                            Json.strField(line, "cause"), facts.get(0), line));
                }
            } catch (Exception ignored) { }
        }
        out.sort((a, b) -> Long.compare(b.tsMs(), a.tsMs()));
        return out;
    }

    public boolean isRestored(String id) { return restored.contains(id); }
    public synchronized void markRestored(String id) { restored.add(id); save(); }

    /**
     * 0.1.7 importer: mark the item carried by an emitted deaths-stream line as
     * already restored, computing the entry id EXACTLY as recoverable() does
     * (ts_ms + ":" + first item's fingerprint, parsed from the line itself), so
     * an imported record that DeathRecord already restored can never be offered
     * by /lostfound or restored a second time. No-op on a line without a
     * payload-carrying item.
     */
    public void markLineRestored(String line) {
        if (line == null) return;
        String itemRaw = Json.rawField(line, "item");
        if (itemRaw == null) return;
        List<ItemFact> facts = ItemFact.listFromJson("[" + itemRaw + "]");
        if (facts.isEmpty() || facts.get(0).payload() == null) return;
        long ts = (long) Json.numField(line, "ts_ms", 0);
        markRestored(ts + ":" + facts.get(0).fingerprint());
    }

    /**
     * 0.1.7 importer: merge DeathRecord's per-player Lost & Found state into
     * Protect's. Monotonic by construction — a merge can never un-use a free
     * recovery, never lower the recovery count, and never move the last-recovery
     * time backwards. recoveriesDelta is the caller-computed not-yet-applied
     * remainder (ImportedLfState), so re-importing the same state is a no-op.
     * freeUsedEpoch is applied only when the merge newly sets the free flag and
     * Protect has no epoch of its own for it.
     */
    public synchronized void applyImportedLfState(UUID player, boolean freeUsed, long freeUsedEpoch,
                                                  int recoveriesDelta, long lastRecoveryEpoch) {
        PlayerState st = state(player);
        if (freeUsed && !st.freeUsed) {
            st.freeUsed = true;
            if (st.freeUsedEpoch <= 0) st.freeUsedEpoch = freeUsedEpoch;
        }
        if (recoveriesDelta > 0) st.recoveries += recoveriesDelta;
        if (lastRecoveryEpoch > st.lastRecoveryEpoch) st.lastRecoveryEpoch = lastRecoveryEpoch;
        save();
    }

    public PlayerState state(UUID player) {
        return players.computeIfAbsent(player.toString(), k -> new PlayerState());
    }

    /** The free-first rule chain (DeathRecord parity): free-first on, never used, no earlier recovery (if configured), balance below threshold. */
    public boolean freeApplies(UUID player, double balance) {
        if (!cfg.lfFreeFirst()) return false;
        PlayerState st = state(player);
        if (st.freeUsed) return false;
        if (cfg.lfFreeOnlyIfNoEarlier() && st.recoveries > 0) return false;
        return balance < cfg.lfFreeThreshold();
    }

    public synchronized void noteRecovery(UUID player, boolean wasFree) {
        PlayerState st = state(player);
        st.recoveries++;
        st.lastRecoveryEpoch = System.currentTimeMillis();
        if (wasFree) { st.freeUsed = true; st.freeUsedEpoch = st.lastRecoveryEpoch; }
        save();
    }

    public boolean cooldownActive(UUID player) {
        if (cfg.lfCooldownHours() <= 0) return false;
        PlayerState st = state(player);
        return st.lastRecoveryEpoch > 0
                && System.currentTimeMillis() - st.lastRecoveryEpoch < cfg.lfCooldownHours() * 3_600_000L;
    }
}
