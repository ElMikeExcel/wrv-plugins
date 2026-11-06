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

/**
 * ExcelProtect — the DeathRecord live-records IMPORTER (audit §1.1: a HARD retirement
 * gate — open Lost & Found claims run to ~Nov 13, 2026). Reads DeathRecord's
 * deaths/<uuid>/*.yml and burned/<owner|unknown>/*.yml, preserving byte payloads
 * verbatim (re-encoded into Protect's gzip+Base64 payload format), and emits them
 * into the deaths stream with imported=true + orig_ts_ms so the Lost & Found window
 * is judged on the ORIGINAL time. Idempotent via a source-marks file.
 * Excel — Wink Reyes Valley
 */
public final class Importer {
    private final Ledger ledger;
    private final File marksFile;
    private final Set<String> done = new HashSet<>();

    public Importer(Ledger ledger, File dataDir) {
        this.ledger = ledger;
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

    public record Result(int deaths, int burns, int skipped, int errors) { }

    public Result importDeathRecord(File root) {
        int deaths = 0, burns = 0, skipped = 0, errors = 0;
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
                if (isDeath) { emitDeath(y, f); deaths++; }
                else { emitBurn(y, f); burns++; }
                done.add(key);
            } catch (Exception e) { errors++; }
        }
        saveMarks();
        ledger.emit("system", null, new Json.Obj().put("type", "import")
                .put("source", "deathrecord").put("deaths", deaths).put("burns", burns)
                .put("skipped", skipped).put("errors", errors));
        return new Result(deaths, burns, skipped, errors);
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

    private Json.Arr itemsFrom(YamlConfiguration y, String... keys) {
        Json.Arr a = new Json.Arr();
        for (String key : keys) {
            List<?> list = y.getList(key);
            if (list == null) continue;
            for (Object o : list) if (o instanceof Map<?, ?> m) {
                ItemFact f = factFrom(m);
                if (f != null) a.add(f.toJson());
            }
        }
        return a;
    }

    private void emitDeath(YamlConfiguration y, File f) {
        Json.Obj o = new Json.Obj().put("type", "death_snapshot")
                .put("imported", true)
                .put("orig_ts_ms", y.getLong("epoch-millis", 0))
                .put("death_id", "imported:" + f.getName())
                .put("player", y.getString("player"))
                .put("player_uuid", y.getString("uuid"))
                .put("death_message", y.getString("death-message"))
                .put("xp_level", y.getInt("xp-level", 0))
                .put("total_experience", y.getInt("total-experience", 0))
                .put("dropped_exp", y.getInt("dropped-exp", 0))
                .put("gamemode", y.getString("gamemode"));
        Json.Arr items = itemsFrom(y, "items", "drops", "inventory");
        o.put("inventory", items);
        org.bukkit.Location loc = locOf(y);
        ledger.emit("deaths", loc, o);
    }

    private void emitBurn(YamlConfiguration y, File f) {
        ItemFact fact = null;
        ConfigurationSection itemSec = y.getConfigurationSection("item");
        if (itemSec != null) fact = factFrom(itemSec.getValues(false));
        if (fact == null) {
            // item body may be top-level keys in the same file
            fact = factFrom(y.getValues(false));
        }
        Json.Obj o = new Json.Obj().put("type", "item_destroyed")
                .put("imported", true)
                .put("orig_ts_ms", y.getLong("epoch-millis", 0))
                .put("cause", y.getString("cause"))
                .put("owner", y.getString("owner"))
                .put("owner_uuid", y.getString("owner-uuid"));
        if (fact != null) o.put("item", fact.toJson());
        ledger.emit("deaths", locOf(y), o);
    }

    private org.bukkit.Location locOf(YamlConfiguration y) {
        String world = y.getString("world");
        if (world == null) return null;
        org.bukkit.World w = org.bukkit.Bukkit.getWorld(world);
        if (w == null) return null;
        return new org.bukkit.Location(w, y.getDouble("x"), y.getDouble("y"), y.getDouble("z"));
    }
}
