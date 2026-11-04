package com.winkreyesvalley.excelmagiclevel.store;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelMagicLevel — players.yml persistence (SPEC §7.1).
 *
 * <ul>
 *   <li>Writes are debounced (the plugin calls {@link #saveIfDue} on a
 *       timer), immediate on level-up ({@link #saveNow}), and in full on
 *       disable.</li>
 *   <li>Every successful save first rotates the previous file to
 *       {@code players.yml.bak} — one backup copy, always the last good
 *       state before this write.</li>
 *   <li>A corrupt file at load is NEVER silently zeroed: it is preserved
 *       under a dated {@code .corrupt-} name, a loud warning goes out, and
 *       the store starts empty. A missing file with a surviving .bak is
 *       recovered from the .bak, also loudly. Individually malformed
 *       entries are skipped with a warning each — one bad row never takes
 *       the whole family's progress down with it.</li>
 * </ul>
 */
public final class PlayerStore {
    /** Receives the loud warnings this store is required to make. */
    public interface WarnSink {
        void warn(String message);
    }

    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final File file;
    private final WarnSink warn;
    private final Map<UUID, PlayerRecord> records = new LinkedHashMap<>();
    private boolean dirty;
    private long lastChangeMillis;

    public PlayerStore(File file, WarnSink warn) {
        this.file = file;
        this.warn = warn;
    }

    public File file() {
        return file;
    }

    public File backupFile() {
        return new File(file.getParentFile(), file.getName() + ".bak");
    }

    public synchronized void load() {
        records.clear();
        dirty = false;
        File source = file;
        if (!file.exists()) {
            File bak = backupFile();
            if (bak.exists()) {
                warn.warn("ExcelMagicLevel: players.yml is MISSING but players.yml.bak exists — "
                        + "recovering from the backup. Investigate before trusting this state.");
                source = bak;
            } else {
                return; // first run ever: empty is correct, nothing to warn about
            }
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.load(source);
        } catch (Exception ex) {
            preserveCorrupt(source);
            warn.warn("ExcelMagicLevel: players.yml is CORRUPT (" + ex.getMessage() + "). "
                    + "It has been preserved under a dated .corrupt- name and the store starts EMPTY. "
                    + "Nobody's progress was silently zeroed — restore from the preserved file or the .bak.");
            return;
        }
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) return;
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException ex) {
                warn.warn("ExcelMagicLevel: skipping players.yml entry with non-UUID key '" + key + "'.");
                continue;
            }
            ConfigurationSection entry = players.getConfigurationSection(key);
            if (entry == null) {
                warn.warn("ExcelMagicLevel: skipping players.yml entry '" + key + "' (not a section).");
                continue;
            }
            long xp = entry.getLong("total-xp", 0);
            long casts = entry.getLong("total-casts", 0);
            boolean quiet = entry.getBoolean("quiet", false);
            if (xp < 0 || casts < 0) {
                warn.warn("ExcelMagicLevel: skipping players.yml entry '" + key + "' (negative xp/casts).");
                continue;
            }
            records.put(uuid, new PlayerRecord(xp, casts, quiet));
        }
    }

    private void preserveCorrupt(File source) {
        try {
            File dest = new File(source.getParentFile(),
                    source.getName() + ".corrupt-" + LocalDateTime.now().format(STAMP));
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            warn.warn("ExcelMagicLevel: could not preserve the corrupt players file: " + ioe.getMessage());
        }
    }

    /** Read-only lookup; never creates a record. */
    public synchronized PlayerRecord find(UUID player) {
        return records.get(player);
    }

    /** Lookup that creates (and dirties) — the award path only. */
    public synchronized PlayerRecord record(UUID player) {
        PlayerRecord existing = records.get(player);
        if (existing != null) return existing;
        PlayerRecord created = new PlayerRecord();
        records.put(player, created);
        markDirty();
        return created;
    }

    public synchronized Map<UUID, PlayerRecord> snapshot() {
        return new LinkedHashMap<>(records);
    }

    public synchronized int size() {
        return records.size();
    }

    public synchronized void markDirty() {
        dirty = true;
        lastChangeMillis = System.currentTimeMillis();
    }

    public synchronized boolean isDirty() {
        return dirty;
    }

    /** Debounced save: writes only if dirty and the quiet period has elapsed. */
    public synchronized boolean saveIfDue(long nowMillis, long debounceMillis) {
        if (!dirty || (nowMillis - lastChangeMillis) < debounceMillis) return false;
        try {
            saveNow();
            return true;
        } catch (IOException ex) {
            warn.warn("ExcelMagicLevel: debounced save of players.yml failed: " + ex.getMessage());
            return false;
        }
    }

    public synchronized void saveNow() throws IOException {
        File parent = file.getParentFile();
        if (parent != null) Files.createDirectories(parent.toPath());
        YamlConfiguration yaml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerRecord> e : records.entrySet()) {
            String path = "players." + e.getKey();
            yaml.set(path + ".total-xp", e.getValue().totalXp());
            yaml.set(path + ".total-casts", e.getValue().totalCasts());
            yaml.set(path + ".quiet", e.getValue().quiet());
        }
        File tmp = new File(parent, file.getName() + ".tmp");
        yaml.save(tmp);
        if (file.exists()) {
            Files.copy(file.toPath(), backupFile().toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.move(tmp.toPath(), file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException atomicFailed) {
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        dirty = false;
    }
}
