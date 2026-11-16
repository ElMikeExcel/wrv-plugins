package com.winkreyes.exceltogether;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * ExcelTogether 0.5.2 - the on-disk half of RepairGate: the confirmed-cast
 * timestamps, one per caster, in `party-repair-cooldowns.yml` inside the
 * plugin's data folder. Written the moment a cast confirms (and again at
 * disable), read once at enable - that is what makes Repair Equipment's
 * 4-hour lockout survive both restarts and relogs. Deleting the file
 * clears every lockout; nothing else reads or writes it.
 *
 * Fail-soft by construction: a missing or unreadable file is a clean
 * slate (one warning, never an exception), and a failed save only logs -
 * the in-memory gate keeps enforcing until the process ends.
 * Excel - Wink Reyes Valley.
 */
public final class RepairCooldownStore {

    /** Load the confirmed-cast map; empty when the file is absent or unreadable. */
    public Map<UUID, Long> load(File file, Logger log) {
        Map<UUID, Long> out = new HashMap<>();
        if (file == null || !file.isFile()) return out;
        try {
            YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
            ConfigurationSection sec = yml.getConfigurationSection("cooldowns");
            if (sec == null) return out;
            for (String key : sec.getKeys(false)) {
                try {
                    out.put(UUID.fromString(key), sec.getLong(key));
                } catch (IllegalArgumentException bad) {
                    if (log != null) log.warning("ExcelTogether: skipping a malformed party-repair cooldown entry ('" + key + "').");
                }
            }
        } catch (Throwable t) {
            if (log != null) log.warning("ExcelTogether: could not read " + file.getName()
                    + " (" + t.getClass().getSimpleName() + ") - party-repair lockouts start clean.");
            return new HashMap<>();
        }
        return out;
    }

    /** Persist the confirmed-cast map (already pruned by the gate). */
    public void save(File file, Map<UUID, Long> confirmed, Logger log) {
        if (file == null) return;
        try {
            YamlConfiguration yml = new YamlConfiguration();
            ConfigurationSection sec = yml.createSection("cooldowns");
            for (Map.Entry<UUID, Long> e : confirmed.entrySet()) {
                sec.set(e.getKey().toString(), e.getValue());
            }
            File parent = file.getParentFile();
            if (parent != null) parent.mkdirs();
            yml.save(file);
        } catch (Throwable t) {
            if (log != null) log.warning("ExcelTogether: could not write " + file.getName()
                    + " (" + t.getClass().getSimpleName() + ") - party-repair lockouts hold until restart only.");
        }
    }
}
