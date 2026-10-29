/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player choices, saved in plugins/ExcelMakersMark/players.yml. Defaults: signing on, stats on. */
final class Prefs {
    static final class P { boolean mark = true, stats = true, tipOk = false; int tipShows = 0; }

    private final ExcelMakersMark plugin;
    private final File file;
    private final Map<UUID, P> map = new HashMap<>();
    private boolean dirty, scheduled;

    Prefs(ExcelMakersMark plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "players.yml");
        load();
    }

    void load() {
        map.clear();
        if (!file.exists()) return;
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection s = y.getConfigurationSection("players");
        if (s == null) return;
        for (String k : s.getKeys(false)) {
            try {
                UUID u = UUID.fromString(k);
                P p = new P();
                p.mark = s.getBoolean(k + ".mark", true);
                p.stats = s.getBoolean(k + ".stats", true);
                p.tipOk = s.getBoolean(k + ".tip-ok", false);
                p.tipShows = s.getInt(k + ".tip-shows", 0);
                map.put(u, p);
            } catch (IllegalArgumentException ignored) { }
        }
    }

    P get(UUID u) { return map.computeIfAbsent(u, x -> new P()); }
    boolean has(UUID u) { return map.containsKey(u); }

    void changed() {
        dirty = true;
        if (scheduled || !plugin.isEnabled()) return;
        scheduled = true;
        Bukkit.getScheduler().runTaskLater(plugin, () -> { scheduled = false; save(); }, 40L);
    }

    void save() {
        if (!dirty) return;
        YamlConfiguration y = new YamlConfiguration();
        y.options().setHeader(java.util.List.of("ExcelMakersMark player choices. mark = sign my crafts, stats = count kills, hits and bosses."));
        for (Map.Entry<UUID, P> e : map.entrySet()) {
            String k = "players." + e.getKey();
            y.set(k + ".mark", e.getValue().mark);
            y.set(k + ".stats", e.getValue().stats);
            y.set(k + ".tip-ok", e.getValue().tipOk);
            y.set(k + ".tip-shows", e.getValue().tipShows);
        }
        try {
            plugin.getDataFolder().mkdirs();
            File tmp = new File(file.getPath() + ".tmp");
            y.save(tmp);
            // Excel: keep the authoritative file and dirty state if replacement fails.
            if (!tmp.renameTo(file)) throw new IOException("Temporary preference file could not replace players.yml; original retained");
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().warning("Could not save players.yml: " + ex.getMessage());
        }
    }
}
