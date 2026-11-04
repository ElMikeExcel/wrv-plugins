package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelHalloweenToggles — per-player toggle store.
 *
 * Backed by the plugin's own players.yml:
 * <pre>
 * players:
 *   &lt;uuid&gt;:
 *     pumpkin_curse: false
 * </pre>
 * Defaults are ALL ON: a player with no row, or a feature with no entry,
 * gets the full event exactly as designed — toggles only ever subtract,
 * and only when the player themselves flips one. State survives relogs
 * and restarts because it lives on disk, saved on every change and again
 * on disable.
 *
 * Feature keys are validated against the registry: an unknown key is a
 * programming/config error and is rejected loudly, never stored.
 */
public final class ToggleStore {

    private final File file;
    private final FeatureRegistry registry;
    private final Logger logger;
    private YamlConfiguration yaml;

    public ToggleStore(File file, FeatureRegistry registry, Logger logger) {
        this.file = file;
        this.registry = registry;
        this.logger = logger;
        load();
    }

    public synchronized void load() {
        yaml = new YamlConfiguration();
        if (file.exists()) {
            try {
                yaml.load(file);
            } catch (Exception e) {
                logger.warning("[ExcelHalloweenToggles] players.yml unreadable (" + e.getMessage()
                        + ") — starting from defaults (all toggles ON).");
                yaml = new YamlConfiguration();
            }
        }
    }

    public synchronized void save() {
        try {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }
            yaml.save(file);
        } catch (IOException e) {
            logger.warning("[ExcelHalloweenToggles] could not save players.yml: " + e.getMessage());
        }
    }

    /** The player's state for a feature; absent means ON (the default). */
    public synchronized boolean isEnabled(UUID player, String featureKey) {
        requireKnown(featureKey);
        return yaml.getBoolean(path(player, featureKey), true);
    }

    public synchronized void setEnabled(UUID player, String featureKey, boolean enabled) {
        requireKnown(featureKey);
        yaml.set(path(player, featureKey), enabled);
        save();
    }

    /** Flip a feature and return its NEW state. */
    public synchronized boolean toggle(UUID player, String featureKey) {
        boolean next = !isEnabled(player, featureKey);
        setEnabled(player, featureKey, next);
        return next;
    }

    private String path(UUID player, String featureKey) {
        return "players." + player + "." + featureKey;
    }

    private void requireKnown(String featureKey) {
        if (!registry.knows(featureKey)) {
            throw new IllegalArgumentException("unknown feature key '" + featureKey + "'");
        }
    }
}
