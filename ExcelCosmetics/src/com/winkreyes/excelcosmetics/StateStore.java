package com.winkreyes.excelcosmetics;

import com.winkreyes.excelcosmetics.core.Grant;
import com.winkreyes.excelcosmetics.core.GrantStore;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * players.yml persistence for the GrantStore. The file is the
 * merged truth: grants are stored already-stacked, so loading
 * never re-applies rental rules. A failed save returns false —
 * the buy path treats that as a failed grant and refunds.
 */
final class StateStore {

    private final GrantStore store = new GrantStore();
    private final File file;
    private final Logger log;

    StateStore(File file, Logger log) {
        this.file = file;
        this.log = log;
    }

    GrantStore store() { return store; }

    void load() {
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = yml.getConfigurationSection("players");
        if (players == null) return;
        int loaded = 0;
        for (String key : players.getKeys(false)) {
            UUID uuid;
            try {
                uuid = UUID.fromString(key);
            } catch (IllegalArgumentException e) {
                log.warning("ExcelCosmetics: skipping malformed player key '" + key + "' in players.yml");
                continue;
            }
            ConfigurationSection sec = players.getConfigurationSection(key);
            if (sec == null) continue;
            PlayerState state = store.state(uuid);
            List<Map<?, ?>> grantList = sec.getMapList("grants");
            for (Map<?, ?> m : grantList) {
                try {
                    Kind kind = Kind.valueOf(String.valueOf(m.get("kind")));
                    String id = String.valueOf(m.get("id"));
                    long granted = ((Number) m.get("granted")).longValue();
                    long expires = ((Number) m.get("expires")).longValue();
                    state.loadGrant(new Grant(kind, id, granted, expires));
                } catch (Exception e) {
                    log.warning("ExcelCosmetics: skipping malformed grant for " + key + ": " + e.getMessage());
                }
            }
            ConfigurationSection eq = sec.getConfigurationSection("equipped");
            if (eq != null) {
                state.loadEquipped(eq.getString("color"), eq.getString("effect"), eq.getString("set"));
            }
            loaded++;
        }
        log.info("ExcelCosmetics: loaded cosmetic state for " + loaded + " player(s)");
    }

    /** Persist everything. Returns false when the write fails —
     *  callers that charged money must treat that as no-grant. */
    boolean save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Map.Entry<UUID, PlayerState> e : store.snapshot().entrySet()) {
            PlayerState s = e.getValue();
            String base = "players." + e.getKey();
            List<Map<String, Object>> grants = new java.util.ArrayList<>();
            for (Grant g : s.grants()) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("kind", g.kind().name());
                m.put("id", g.id());
                m.put("granted", g.grantedAtMillis());
                m.put("expires", g.expiresAtMillis());
                grants.add(m);
            }
            yml.set(base + ".grants", grants);
            if (s.equippedColorId() != null) yml.set(base + ".equipped.color", s.equippedColorId());
            if (s.equippedEffectId() != null) yml.set(base + ".equipped.effect", s.equippedEffectId());
            if (s.equippedSetId() != null) yml.set(base + ".equipped.set", s.equippedSetId());
        }
        try {
            file.getParentFile().mkdirs();
            yml.save(file);
            return true;
        } catch (IOException e) {
            log.severe("ExcelCosmetics: FAILED to save players.yml: " + e.getMessage());
            return false;
        }
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
