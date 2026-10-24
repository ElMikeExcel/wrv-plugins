package com.winkreyes.exceldeathrecord;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

/**
 * 0.3.0: a server upgrading from 0.2.0 already has a config.yml with
 * only display-zone, and saveDefaultConfig never touches an existing
 * file. So the first 0.3.0 start appends the Lost &amp; Found block
 * (from the bundled config.yml, the "# ---- 0.3.0" marker to the end)
 * when the file has no lost-and-found section yet, after copying the
 * old file to config.yml.bak-pre-0.3.0 (an existing backup is never
 * overwritten). Comments and the owner's other keys are kept as-is.
 * If anything fails the bundled defaults still apply in memory.
 */
final class ConfigUpgrade {
    static final String MARKER = "# ---- 0.3.0 Lost & Found";

    private ConfigUpgrade() {}

    static void addMissingLostFoundBlock(JavaPlugin plugin) {
        try {
            File f = new File(plugin.getDataFolder(), "config.yml");
            if (!f.exists()) return;
            String text = Files.readString(f.toPath(), StandardCharsets.UTF_8);
            if (text.contains("\nlost-and-found:") || text.startsWith("lost-and-found:")) return;
            String bundled;
            try (InputStream in = plugin.getResource("config.yml")) {
                if (in == null) return;
                bundled = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            int at = bundled.indexOf(MARKER);
            if (at < 0) return;
            File bak = new File(plugin.getDataFolder(), "config.yml.bak-pre-0.3.0");
            if (!bak.exists()) Files.copy(f.toPath(), bak.toPath());
            String block = (text.endsWith("\n") ? "\n" : "\n\n") + bundled.substring(at);
            Files.writeString(f.toPath(), block, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            plugin.reloadConfig();
            plugin.getLogger().info("ExcelDeathRecord: added the 0.3.0 Lost & Found settings to config.yml (old file kept as "
                    + bak.getName() + ").");
        } catch (Exception e) {
            plugin.getLogger().warning("ExcelDeathRecord: could not add the Lost & Found settings to config.yml ("
                    + e.getMessage() + "); the built-in defaults are used.");
        }
    }
}
