package com.winkreyes.excelbraingames;

import com.winkreyes.excelbraingames.games.memory.MemoryRound;
import com.winkreyes.excelbraingames.games.sudoku.SudokuRound;
import com.winkreyes.excelbraingames.games.text.QuickMathRound;
import com.winkreyes.excelbraingames.games.text.ScrambleRound;
import com.winkreyes.excelbraingames.games.wordsearch.WordSearchRound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Per-player YAML under plugins/ExcelBrainGames/data/<uuid>.yml (SPEC §9.5):
 * human-readable, small, fleet-consistent. Daily totals older than 35 days
 * are pruned on load. A name index (names.yml) lets console admin commands
 * resolve players who are offline.
 */
public final class DataStore {

    private final File dir;
    private final Logger log;
    private final Map<UUID, PlayerData> cache = new HashMap<>();
    private final Map<String, UUID> nameIndex = new HashMap<>(); // lowercase name -> uuid

    public DataStore(File dataRoot, Logger log) {
        this.dir = new File(dataRoot, "data");
        this.log = log;
        //noinspection ResultOfMethodCallIgnored
        dir.mkdirs();
        loadNameIndex();
    }

    private File fileFor(UUID uuid) { return new File(dir, uuid + ".yml"); }
    private File indexFile() { return new File(dir, "names.yml"); }

    private void loadNameIndex() {
        File f = indexFile();
        if (!f.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("names");
        if (sec == null) return;
        for (String name : sec.getKeys(false)) {
            try { nameIndex.put(name.toLowerCase(), UUID.fromString(sec.getString(name, ""))); }
            catch (IllegalArgumentException ignored) { /* skip malformed */ }
        }
    }

    private void saveNameIndex() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<String, UUID> e : nameIndex.entrySet()) {
            y.set("names." + e.getKey(), e.getValue().toString());
        }
        try { y.save(indexFile()); }
        catch (IOException e) { log.warning("ExcelBrainGames: could not save names.yml: " + e.getMessage()); }
    }

    public void noteName(UUID uuid, String name) {
        if (name == null || name.isBlank()) return;
        UUID old = nameIndex.put(name.toLowerCase(), uuid);
        PlayerData pd = cache.get(uuid);
        if (pd != null) pd.name = name;
        if (!uuid.equals(old)) saveNameIndex();
    }

    public UUID uuidForName(String name) {
        return nameIndex.get(name.toLowerCase());
    }

    public PlayerData get(UUID uuid, String nameIfNew, ZoneId zone) {
        PlayerData pd = cache.get(uuid);
        if (pd != null) {
            if (nameIfNew != null && !nameIfNew.isBlank()) pd.name = nameIfNew;
            return pd;
        }
        pd = new PlayerData(uuid, nameIfNew);
        File f = fileFor(uuid);
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            pd.name = y.getString("name", pd.name);
            Map<String, Long> daily = new HashMap<>();
            ConfigurationSection dsec = y.getConfigurationSection("daily");
            if (dsec != null) {
                for (String day : dsec.getKeys(false)) daily.put(day, y.getLong("daily." + day));
            }
            pd.ledger.restore(daily, y.getInt("streak", 0), y.getString("last-paid-date"));
            String cutoff = LocalDate.now(zone).minusDays(35).toString();
            pd.ledger.pruneOlderThan(cutoff);
            ConfigurationSection ws = y.getConfigurationSection("wordsearch");
            if (ws != null) {
                try { pd.wordSearch = WordSearchRound.fromMap(sectionToMap(ws)); }
                catch (RuntimeException e) { log.warning("ExcelBrainGames: dropped corrupt saved Word Search round for " + uuid); }
            }
            ConfigurationSection mm = y.getConfigurationSection("memory");
            if (mm != null) {
                try { pd.memory = MemoryRound.fromMap(sectionToMap(mm)); }
                catch (RuntimeException e) { log.warning("ExcelBrainGames: dropped corrupt saved Memory Match round for " + uuid); }
            }
            ConfigurationSection sd = y.getConfigurationSection("sudoku");
            if (sd != null) {
                try { pd.sudoku = SudokuRound.fromMap(sectionToMap(sd)); }
                catch (RuntimeException e) { log.warning("ExcelBrainGames: dropped corrupt saved Sudoku round for " + uuid); }
            }
            ConfigurationSection qm = y.getConfigurationSection("quickmath");
            if (qm != null) {
                try { pd.quickMath = QuickMathRound.fromMap(sectionToMap(qm)); }
                catch (RuntimeException e) { log.warning("ExcelBrainGames: dropped corrupt saved Quick Math round for " + uuid); }
            }
            ConfigurationSection sc = y.getConfigurationSection("scramble");
            if (sc != null) {
                try { pd.scramble = ScrambleRound.fromMap(sectionToMap(sc)); }
                catch (RuntimeException e) { log.warning("ExcelBrainGames: dropped corrupt saved Word Scramble round for " + uuid); }
            }
            pd.riddleDate = y.getString("riddle.date", "");
            pd.riddleAttempts = y.getInt("riddle.attempts", 0);
            pd.riddleHints = y.getInt("riddle.hints", 0);
            pd.riddleSolved = y.getBoolean("riddle.solved", false);
            pd.riddlePaid = y.getBoolean("riddle.paid", false);
        }
        cache.put(uuid, pd);
        return pd;
    }

    /** Read-only peek without creating state for unknown players (admin use). */
    public PlayerData peek(UUID uuid, ZoneId zone) {
        if (!fileFor(uuid).exists() && !cache.containsKey(uuid)) return null;
        return get(uuid, null, zone);
    }

    public void save(PlayerData pd) {
        YamlConfiguration y = new YamlConfiguration();
        y.set("name", pd.name);
        Map<String, Object> daily = new LinkedHashMap<>();
        pd.ledger.dailyCents().entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(e -> daily.put(e.getKey(), e.getValue()));
        y.set("daily", daily);
        y.set("streak", pd.ledger.streak());
        y.set("last-paid-date", pd.ledger.lastPaidDate());
        y.set("wordsearch", pd.wordSearch == null ? null : pd.wordSearch.toMap());
        y.set("memory", pd.memory == null ? null : pd.memory.toMap());
        y.set("sudoku", pd.sudoku == null ? null : pd.sudoku.toMap());
        y.set("quickmath", pd.quickMath == null ? null : pd.quickMath.toMap());
        y.set("scramble", pd.scramble == null ? null : pd.scramble.toMap());
        y.set("riddle.date", pd.riddleDate);
        y.set("riddle.attempts", pd.riddleAttempts);
        y.set("riddle.hints", pd.riddleHints);
        y.set("riddle.solved", pd.riddleSolved);
        y.set("riddle.paid", pd.riddlePaid);
        try { y.save(fileFor(pd.uuid)); }
        catch (IOException e) { log.warning("ExcelBrainGames: could not save data for " + pd.uuid + ": " + e.getMessage()); }
        noteName(pd.uuid, pd.name);
    }

    public void saveAll() {
        for (PlayerData pd : cache.values()) save(pd);
    }

    private static Map<String, Object> sectionToMap(ConfigurationSection sec) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (String key : sec.getKeys(false)) out.put(key, sec.get(key));
        return out;
    }
}
