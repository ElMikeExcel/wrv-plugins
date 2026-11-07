package com.winkreyes.excelautosort;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every player-facing string comes from config.yml's {@code messages:}
 * section (SPEC §9, config-first). This class only fetches, substitutes
 * {placeholders} and renders '&' color codes; wording lives in config.
 */
public final class Messages {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private FileConfiguration cfg;

    public Messages(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public void reload(FileConfiguration cfg) {
        this.cfg = cfg;
    }

    public String raw(String path, String fallback) {
        return cfg.getString("messages." + path, fallback);
    }

    public String raw(String path, String fallback, Map<String, String> vars) {
        String s = raw(path, fallback);
        if (vars != null) {
            for (Map.Entry<String, String> e : vars.entrySet()) {
                s = s.replace("{" + e.getKey() + "}", e.getValue());
            }
        }
        return s;
    }

    public List<String> rawList(String path, List<String> fallback, Map<String, String> vars) {
        List<String> out = new ArrayList<>();
        for (String s : cfg.getStringList("messages." + path)) {
            if (vars != null) {
                for (Map.Entry<String, String> e : vars.entrySet()) {
                    s = s.replace("{" + e.getKey() + "}", e.getValue());
                }
            }
            out.add(s);
        }
        return out.isEmpty() ? new ArrayList<>(fallback) : out;
    }

    public Component comp(String path, String fallback) {
        return LEGACY.deserialize(raw(path, fallback));
    }

    public Component comp(String path, String fallback, Map<String, String> vars) {
        return LEGACY.deserialize(raw(path, fallback, vars));
    }

    public Component of(String legacy) {
        return LEGACY.deserialize(legacy == null ? "" : legacy);
    }

    public List<Component> comps(List<String> legacyLines) {
        List<Component> out = new ArrayList<>();
        for (String s : legacyLines) out.add(of(s));
        return out;
    }

    public void send(Player p, String path, String fallback) {
        p.sendMessage(comp(path, fallback));
    }

    public void send(Player p, String path, String fallback, Map<String, String> vars) {
        p.sendMessage(comp(path, fallback, vars));
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
