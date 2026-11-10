package com.winkreyes.exceldetails;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * data.yml — ALL of the meter's persistence in one pure class so the
 * full round-trip is unit-testable without a running server:
 *
 *   exceldetails-build: <build id>
 *   players.<uuid>.enabled / .window            (0.1.0, kept)
 *   players.<uuid>.show-damage/-taken/-healing  (0.2.0 mix)
 *   overall.<uuid>.{name,damage-done,damage-taken,healing,hits}
 *                                               (0.2.0 totals)
 *
 * Migration: a player with NO show-* keys but a legacy window key
 * loads with shown = {that window} — a 0.1.0 player who had switched
 * to Damage Taken keeps exactly that view under the mix model.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class DataFile {

    private DataFile() {}

    public static void save(File file, PlayerPrefs prefs, SegmentTracker tracker) throws IOException {
        YamlConfiguration yml = new YamlConfiguration();
        yml.set("exceldetails-build", BuildInfo.BUILD_ID);
        prefs.enabledSnapshot().forEach((id, on) -> yml.set("players." + id + ".enabled", on));
        prefs.windowSnapshot().forEach((id, w) -> yml.set("players." + id + ".window", w.name()));
        prefs.shownSnapshot().forEach((id, set) -> {
            yml.set("players." + id + ".show-damage", set.contains(CombatStats.Window.DAMAGE_DONE));
            yml.set("players." + id + ".show-taken", set.contains(CombatStats.Window.DAMAGE_TAKEN));
            yml.set("players." + id + ".show-healing", set.contains(CombatStats.Window.HEALING_DONE));
        });
        Map<UUID, String> names = tracker.overallNames();
        tracker.overallSnapshot().forEach((id, s) -> {
            String base = "overall." + id + ".";
            yml.set(base + "name", names.get(id));
            yml.set(base + "damage-done", s.damageDone());
            yml.set(base + "damage-taken", s.damageTaken());
            yml.set(base + "healing", s.healingDone());
            yml.set(base + "hits", s.hits());
        });
        yml.save(file);
    }

    public static void load(File file, PlayerPrefs prefs, SegmentTracker tracker) {
        if (file == null || !file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);

        if (yml.isConfigurationSection("players")) {
            for (String key : yml.getConfigurationSection("players").getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    continue; // a hand-edited bad UUID line is skipped, never fatal
                }
                String base = "players." + key + ".";
                prefs.setEnabled(id, yml.getBoolean(base + "enabled", true));
                CombatStats.Window w = CombatStats.Window.parse(yml.getString(base + "window"));
                if (w != null) prefs.setWindow(id, w);
                boolean hasMix = yml.contains(base + "show-damage")
                        || yml.contains(base + "show-taken")
                        || yml.contains(base + "show-healing");
                if (hasMix) {
                    prefs.setShown(id, CombatStats.Window.DAMAGE_DONE, yml.getBoolean(base + "show-damage", true));
                    prefs.setShown(id, CombatStats.Window.DAMAGE_TAKEN, yml.getBoolean(base + "show-taken", false));
                    prefs.setShown(id, CombatStats.Window.HEALING_DONE, yml.getBoolean(base + "show-healing", false));
                } else if (w != null) {
                    prefs.showOnly(id, w); // 0.1.0 migration (see class doc)
                }
            }
        }

        if (yml.isConfigurationSection("overall")) {
            Map<UUID, CombatStats> totals = new LinkedHashMap<>();
            Map<UUID, String> names = new LinkedHashMap<>();
            for (String key : yml.getConfigurationSection("overall").getKeys(false)) {
                UUID id;
                try {
                    id = UUID.fromString(key);
                } catch (IllegalArgumentException ignored) {
                    continue;
                }
                String base = "overall." + key + ".";
                totals.put(id, CombatStats.fromTotals(
                        yml.getDouble(base + "damage-done"),
                        yml.getDouble(base + "damage-taken"),
                        yml.getDouble(base + "healing"),
                        yml.getLong(base + "hits")));
                String name = yml.getString(base + "name");
                if (name != null) names.put(id, name);
            }
            tracker.restoreOverall(totals, names);
        }
    }
}
