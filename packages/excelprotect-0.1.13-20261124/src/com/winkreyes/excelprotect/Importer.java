package com.winkreyes.excelprotect;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * ExcelProtect — the DeathRecord live-records IMPORTER (audit §1.1: a HARD retirement
 * gate — open Lost & Found claims run to ~Nov 13, 2026). Reads DeathRecord's
 * deaths/<uuid>/*.yml and burned/<owner|unknown>/*.yml, preserving byte payloads
 * verbatim (re-encoded into Protect's gzip+Base64 payload format), and emits them
 * into the deaths stream with imported=true + orig_ts_ms so the Lost & Found window
 * is judged on the ORIGINAL time. Idempotent via a source-marks file.
 *
 * 0.1.7 (Mike's decision Nov 6, 2026 — "build the importer fix first, then import
 * clean"; census ~/workspace/protect-rollout-item13a/CENSUS-20261106.md found
 * R-burns=4 already-restored items that the 0.1.0 importer would have re-offered):
 *  (a) DeathRecord's restored marks are HONORED. A burn whose item carries
 *      restored-times > 0 imports stamped source_restored AND pre-marked in
 *      Recovery's restore marks under the exact id recoverable() computes, so
 *      /lostfound never offers it and it can never be restored twice. Death
 *      items already restored in DeathRecord are EXCLUDED from the imported
 *      snapshot (a staff restore hands over inventory+drops wholesale, so
 *      inclusion would double-give); a death whose every item was restored is
 *      marked restored outright ("death:<death_id>", the Rollback mark), and a
 *      death with xp-restored-times > 0 imports WITHOUT xp_level, so its XP
 *      cannot be granted a second time either.
 *  (b) DeathRecord's per-player Lost & Found state (lostfound/players/*.yml)
 *      MIGRATES into Protect's per-player state — free-used flag, paid recovery
 *      count, last-recovery time — via ImportedLfState deltas: never a reset,
 *      never a double-count, even across repeated imports of a still-growing
 *      DeathRecord state file.
 *  (c) Idempotency unchanged for record files (path marks) and delta-exact for
 *      state files: importing the same folder twice changes nothing.
 * Excel — Wink Reyes Valley
 */
public final class Importer {
    private final Ledger ledger;
    private final Recovery recovery;
    private final ImportedLfState lfState;
    private final File marksFile;
    private final Set<String> done = new HashSet<>();

    public Importer(Ledger ledger, Recovery recovery, File dataDir) {
        this.ledger = ledger;
        this.recovery = recovery;
        this.lfState = new ImportedLfState(dataDir);
        marksFile = new File(dataDir, "imported-sources.json");
        try {
            if (marksFile.exists())
                for (String s : Files.readString(marksFile.toPath(), StandardCharsets.UTF_8).split("[,\\[\\]\"]+"))
                    if (s.length() > 4) done.add(s.trim());
        } catch (Exception ignored) { }
    }

    private synchronized void saveMarks() {
        try {
            StringBuilder m = new StringBuilder("[");
            boolean first = true;
            for (String s : done) { if (!first) m.append(','); m.append('"').append(s.replace("\\", "/")).append('"'); first = false; }
            Files.writeString(marksFile.toPath(), m.append(']').toString(), StandardCharsets.UTF_8);
        } catch (Exception ignored) { }
    }

    public record Result(int deaths, int burns, int skipped, int errors,
                         int restoredBurns, int restoredItems, int lfState) { }

    public Result importDeathRecord(File root) {
        int deaths = 0, burns = 0, skipped = 0, errors = 0;
        int restoredBurns = 0, restoredItems = 0, lfFiles = 0;
        File deathsDir = new File(root, "deaths");
        File burnedDir = new File(root, "burned");
        List<File> files = new ArrayList<>();
        collect(deathsDir, files); collect(burnedDir, files);
        for (File f : files) {
            String key = f.getAbsolutePath();
            if (done.contains(key)) { skipped++; continue; }
            try {
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                boolean isDeath = f.getAbsolutePath().contains(File.separator + "deaths" + File.separator);
                if (isDeath) { restoredItems += emitDeath(y, f); deaths++; }
                else { if (emitBurn(y, f)) restoredBurns++; burns++; }
                done.add(key);
            } catch (Exception e) { errors++; }
        }
        // (b) Lost & Found player state — mutable files, merged by delta (never path-marked).
        File lfDir = new File(new File(root, "lostfound"), "players");
        List<File> stateFiles = new ArrayList<>();
        collect(lfDir, stateFiles);
        stateFiles.sort((a, b) -> a.getName().compareTo(b.getName()));
        for (File f : stateFiles) {
            try {
                String base = f.getName();
                UUID uuid = UUID.fromString(base.substring(0, base.length() - ".yml".length()));
                YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
                boolean drFree = y.getBoolean("free-used", false);
                long drFreeEpoch = y.getLong("free-used-epoch", 0);
                int drRecoveries = y.getInt("recoveries", 0);
                long drLastEpoch = y.getLong("last-recovery-epoch", 0);
                String key = f.getAbsolutePath();
                ImportedLfState.Delta d = lfState.deltaFor(key, drRecoveries, drFree);
                recovery.applyImportedLfState(uuid, d.applyFreeUsed() ? drFree : false,
                        drFreeEpoch, d.recoveriesDelta(), drLastEpoch);
                lfState.commit(key, drRecoveries, drFree);
                lfFiles++;
            } catch (Exception e) { errors++; }
        }
        saveMarks();
        ledger.emit("system", null, new Json.Obj().put("type", "import")
                .put("source", "deathrecord").put("deaths", deaths).put("burns", burns)
                .put("skipped", skipped).put("errors", errors)
                .put("restored_burns", restoredBurns).put("restored_items", restoredItems)
                .put("lf_state", lfFiles));
        return new Result(deaths, burns, skipped, errors, restoredBurns, restoredItems, lfFiles);
    }

    private void collect(File dir, List<File> out) {
        if (dir == null || !dir.isDirectory()) return;
        File[] kids = dir.listFiles();
        if (kids == null) return;
        for (File k : kids) {
            if (k.isDirectory()) collect(k, out);
            else if (k.getName().endsWith(".yml")) out.add(k);
        }
    }

    /** DeathRecord's per-item restored count (RecordFormat/BurnFormat restored-times), 0 when absent. */
    static int restoredTimes(Map<?, ?> m) {
        Object v = m.get("restored-times");
        return v instanceof Number n ? n.intValue() : 0;
    }

    private ItemFact factFrom(Map<?, ?> m) {
        Object idObj = m.get("id");
        String id = idObj == null ? "" : String.valueOf(idObj);
        if (id.isEmpty() || id.equals("null")) return null;
        int count = m.get("count") instanceof Number n ? n.intValue() : 1;
        int damage = m.get("damage") instanceof Number n ? n.intValue() : 0;
        String name = m.get("name") == null ? null : String.valueOf(m.get("name"));
        String bytes = m.get("bytes") == null ? null : String.valueOf(m.get("bytes"));
        String payload = null;
        if (bytes != null && !bytes.isEmpty()) {
            try { payload = ItemFact.encode(Base64.getDecoder().decode(bytes)); } catch (Exception ignored) { }
        }
        Map<String, Integer> ench = null;
        if (m.get("enchants") instanceof Map<?, ?> em && !em.isEmpty()) {
            ench = new java.util.TreeMap<>();
            for (Map.Entry<?, ?> e : em.entrySet())
                if (e.getValue() instanceof Number n) ench.put(String.valueOf(e.getKey()), n.intValue());
        }
        return new ItemFact(id, count, damage, name, ench, null, null, payload, null);
    }

    private record DeathItems(Json.Arr items, int restored, int total) { }

    /** Death snapshot items, EXCLUDING any DeathRecord already restored (0.1.7a). */
    private DeathItems deathItems(YamlConfiguration y, String... keys) {
        Json.Arr a = new Json.Arr();
        int restored = 0, total = 0;
        for (String key : keys) {
            List<?> list = y.getList(key);
            if (list == null) continue;
            for (Object o : list) if (o instanceof Map<?, ?> m) {
                ItemFact f = factFrom(m);
                if (f == null) continue;
                total++;
                if (restoredTimes(m) > 0) { restored++; continue; }
                a.add(f.toJson());
            }
        }
        return new DeathItems(a, restored, total);
    }

    /** Emits one death snapshot; returns how many items were excluded as already-restored. */
    private int emitDeath(YamlConfiguration y, File f) {
        String deathId = "imported:" + f.getName();
        boolean xpRestored = y.getInt("xp-restored-times", 0) > 0;
        Json.Obj o = new Json.Obj().put("type", "death_snapshot")
                .put("imported", true)
                .put("orig_ts_ms", y.getLong("epoch-millis", 0))
                .put("death_id", deathId)
                .put("player", y.getString("player"))
                .put("player_uuid", y.getString("uuid"))
                .put("death_message", y.getString("death-message"))
                .put("total_experience", y.getInt("total-experience", 0))
                .put("dropped_exp", y.getInt("dropped-exp", 0))
                .put("gamemode", y.getString("gamemode"));
        // XP already handed back by DeathRecord must not ride the snapshot:
        // Rollback.restoreDeath grants from xp_level, so an xp-restored death
        // imports with xp_level omitted (its reader treats absent as skip).
        if (xpRestored) o.put("xp_restored_in_source", true);
        else o.put("xp_level", y.getInt("xp-level", 0));
        DeathItems di = deathItems(y, "items", "drops", "inventory");
        if (di.restored() > 0) o.put("source_restored_items", di.restored());
        o.put("inventory", di.items());
        ledger.emit("deaths", locOf(y), o);
        // A death whose EVERY recorded item was already restored has nothing
        // left to give: mark it restored under the same once-only mark the
        // staff restore path checks, so it can never be committed later.
        if (di.total() > 0 && di.restored() == di.total())
            recovery.markRestored("death:" + deathId);
        return di.restored();
    }

    /** Emits one burn record; returns true when DeathRecord had already restored it. */
    private boolean emitBurn(YamlConfiguration y, File f) {
        ItemFact fact = null;
        Map<?, ?> itemMap = null;
        ConfigurationSection itemSec = y.getConfigurationSection("item");
        if (itemSec != null) { itemMap = itemSec.getValues(false); fact = factFrom(itemMap); }
        if (fact == null) {
            // item body may be top-level keys in the same file
            itemMap = y.getValues(false);
            fact = factFrom(itemMap);
        }
        int restored = itemMap == null ? 0 : restoredTimes(itemMap);
        Json.Obj o = new Json.Obj().put("type", "item_destroyed")
                .put("imported", true)
                .put("orig_ts_ms", y.getLong("epoch-millis", 0))
                .put("cause", y.getString("cause"))
                .put("owner", y.getString("owner"))
                .put("owner_uuid", y.getString("owner-uuid"));
        if (restored > 0) o.put("source_restored", true).put("source_restored_times", restored);
        if (fact != null) o.put("item", fact.toJson());
        String line = ledger.emit("deaths", locOf(y), o);
        // The re-offer hazard the census named: pre-mark the emitted entry
        // restored under recoverable()'s own id, so Lost & Found skips it.
        if (restored > 0) recovery.markLineRestored(line);
        return restored > 0;
    }

    private org.bukkit.Location locOf(YamlConfiguration y) {
        String world = y.getString("world");
        if (world == null) return null;
        org.bukkit.World w;
        try { w = org.bukkit.Bukkit.getWorld(world); }
        catch (Throwable t) { return null; } // an unresolvable world costs the pin, never the record
        if (w == null) return null;
        return new org.bukkit.Location(w, y.getDouble("x"), y.getDouble("y"), y.getDouble("z"));
    }
}
