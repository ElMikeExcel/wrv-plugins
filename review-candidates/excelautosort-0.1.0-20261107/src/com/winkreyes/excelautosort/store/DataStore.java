package com.winkreyes.excelautosort.store;

import com.winkreyes.excelautosort.BuildInfo;
import com.winkreyes.excelautosort.core.ChestKeys;
import com.winkreyes.excelautosort.core.Mode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelAutoSort's own state (SPEC §9): players.yml (the ON set),
 * chests.yml (filter records), buffers.yml (recovery entries) — all
 * under {@code data/} in the plugin folder. Saves are atomic (temp +
 * rename) on every mutation; loads are fail-soft per record — a corrupt
 * record is skipped and logged, never fatal (SPEC §11 T10).
 *
 * Chunk unload/load touches nothing: records are pure data, resolved
 * against the world only when an event names their key.
 */
public final class DataStore {

    private final File dir;
    private final Logger log;

    public final Set<UUID> sortOn = new HashSet<>();
    public final Map<UUID, ChestRecord> chests = new LinkedHashMap<>();
    /** block key → chest id, for O(1) event lookup. */
    public final Map<String, UUID> byKey = new HashMap<>();
    public final Map<UUID, List<BufferEntry>> buffers = new LinkedHashMap<>();

    public DataStore(File pluginDir, Logger log) {
        this.dir = new File(pluginDir, "data");
        this.log = log;
    }

    // ------------------------------------------------------------ load

    public void loadAll() {
        sortOn.clear();
        chests.clear();
        byKey.clear();
        buffers.clear();
        loadPlayers();
        loadChests();
        loadBuffers();
    }

    private void loadPlayers() {
        File f = new File(dir, "players.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        for (String s : y.getStringList("on")) {
            try {
                sortOn.add(UUID.fromString(s.trim()));
            } catch (IllegalArgumentException skipped) {
                log.warning("ExcelAutoSort: skipped a malformed UUID in players.yml");
            }
        }
    }

    private void loadChests() {
        File f = new File(dir, "chests.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        if (!y.isConfigurationSection("chests")) return;
        for (String idStr : y.getConfigurationSection("chests").getKeys(false)) {
            try {
                String base = "chests." + idStr + ".";
                ChestRecord r = new ChestRecord(UUID.fromString(idStr));
                r.keys = ChestKeys.canonical(y.getStringList(base + "keys"));
                if (r.keys.isEmpty()) throw new IllegalArgumentException("no keys");
                String owner = y.getString(base + "owner", "");
                r.owner = UUID.fromString(owner);
                r.ownerName = y.getString(base + "owner-name", "");
                r.mode = Mode.fromId(y.getString(base + "mode", "off"));
                r.items.addAll(y.getStringList(base + "items"));
                r.createdMs = y.getLong(base + "created", 0);
                r.updatedMs = y.getLong(base + "updated", 0);
                r.suspended = y.getBoolean(base + "suspended", false);
                r.suspendReason = y.getString(base + "suspend-reason", "");
                chests.put(r.id, r);
                for (String k : r.keys) byKey.put(k, r.id);
            } catch (RuntimeException e) {
                log.warning("ExcelAutoSort: skipped a corrupt chest record (" + idStr + "): " + e.getMessage());
            }
        }
    }

    private void loadBuffers() {
        File f = new File(dir, "buffers.yml");
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        if (!y.isConfigurationSection("buffers")) return;
        for (String ownerStr : y.getConfigurationSection("buffers").getKeys(false)) {
            UUID owner;
            try {
                owner = UUID.fromString(ownerStr);
            } catch (IllegalArgumentException e) {
                log.warning("ExcelAutoSort: skipped a buffer with a malformed owner (" + ownerStr + ")");
                continue;
            }
            List<BufferEntry> list = new ArrayList<>();
            for (Map<?, ?> raw : y.getMapList("buffers." + ownerStr)) {
                try {
                    BufferEntry e = new BufferEntry(
                            String.valueOf(raw.get("id")),
                            owner,
                            String.valueOf(raw.get("item")),
                            String.valueOf(raw.get("material")),
                            ((Number) raw.get("count")).intValue(),
                            ((Number) raw.get("removed-at")).longValue(),
                            ((Number) raw.get("expires-at")).longValue(),
                            String.valueOf(raw.get("chest-key")),
                            String.valueOf(raw.get("cause")));
                    if (e.itemB64.isEmpty() || e.id.isEmpty() || "null".equals(e.id)) {
                        throw new IllegalArgumentException("empty entry");
                    }
                    list.add(e);
                } catch (RuntimeException ex) {
                    log.warning("ExcelAutoSort: skipped a corrupt buffer entry for " + ownerStr);
                }
            }
            if (!list.isEmpty()) buffers.put(owner, list);
        }
    }

    // ------------------------------------------------------------ save

    public void savePlayers() {
        YamlConfiguration y = new YamlConfiguration();
        List<String> on = new ArrayList<>();
        for (UUID u : sortOn) on.add(u.toString());
        on.sort(String::compareTo);
        y.set("on", on);
        writeAtomic(new File(dir, "players.yml"), y);
    }

    public void saveChests() {
        YamlConfiguration y = new YamlConfiguration();
        for (ChestRecord r : chests.values()) {
            String base = "chests." + r.id + ".";
            y.set(base + "keys", ChestKeys.canonical(r.keys));
            y.set(base + "owner", r.owner == null ? "" : r.owner.toString());
            y.set(base + "owner-name", r.ownerName);
            y.set(base + "mode", r.mode.id());
            y.set(base + "items", new ArrayList<>(r.items));
            y.set(base + "created", r.createdMs);
            y.set(base + "updated", r.updatedMs);
            y.set(base + "suspended", r.suspended);
            y.set(base + "suspend-reason", r.suspendReason);
        }
        writeAtomic(new File(dir, "chests.yml"), y);
    }

    public void saveBuffers() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, List<BufferEntry>> e : buffers.entrySet()) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (BufferEntry b : e.getValue()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", b.id);
                m.put("item", b.itemB64);
                m.put("material", b.materialKey);
                m.put("count", b.count);
                m.put("removed-at", b.removedAtMs);
                m.put("expires-at", b.expiresAtMs);
                m.put("chest-key", b.chestKey);
                m.put("cause", b.cause);
                list.add(m);
            }
            y.set("buffers." + e.getKey(), list);
        }
        writeAtomic(new File(dir, "buffers.yml"), y);
    }

    public void saveAll() {
        savePlayers();
        saveChests();
        saveBuffers();
    }

    /** Temp file + rename, with the build header stamped on the file (Layer B/C). */
    private void writeAtomic(File target, YamlConfiguration y) {
        try {
            dir.mkdirs();
            File tmp = new File(dir, target.getName() + ".tmp");
            y.save(tmp);
            String body = Files.readString(tmp.toPath(), StandardCharsets.UTF_8);
            String header = "# ExcelAutoSort data file — build " + BuildInfo.BUILD_ID
                    + " — written by the plugin; edit only while the server is stopped.\n";
            Files.writeString(tmp.toPath(), header + body, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.warning("ExcelAutoSort: could not save " + target.getName() + " (" + e.getMessage() + ")");
        }
    }

    // ------------------------------------------------------------ index upkeep

    public void indexRecord(ChestRecord r) {
        chests.put(r.id, r);
        for (String k : r.keys) byKey.put(k, r.id);
    }

    public void reindexRecord(ChestRecord r) {
        byKey.values().removeIf(id -> id.equals(r.id));
        for (String k : r.keys) byKey.put(k, r.id);
    }

    public void removeRecord(ChestRecord r) {
        chests.remove(r.id);
        for (String k : r.keys) byKey.remove(k, r.id);
    }

    public ChestRecord byKey(String blockKey) {
        UUID id = byKey.get(blockKey);
        return id == null ? null : chests.get(id);
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
