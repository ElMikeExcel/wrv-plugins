/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * The shipped YAML must parse the way Paper parses it
 * (0.3.7): plugin.yml's description is one long PLAIN scalar,
 * so a colon-space anywhere in it ("shape: with") makes the
 * whole plugin unloadable - InvalidDescriptionException at
 * bootstrap, before any code runs. The 0.3.7 first candidate
 * shipped exactly that defect and the unit suite (which never
 * parsed the file) stayed green; the rig caught it. This
 * guard parses plugin.yml and config.yml from the tree root
 * (build-test.sh runs from there) with Bukkit's own YAML
 * loader and pins the identity fields, so the defect class
 * cannot ride a green suite again.
 */
public final class PluginYmlParseTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        File pluginYml = new File("plugin.yml");
        check(pluginYml.isFile(), "plugin.yml present at tree root");
        YamlConfiguration plugin = YamlConfiguration.loadConfiguration(pluginYml);
        check("ExcelBossTrial".equals(plugin.getString("name")),
                "plugin.yml name is ExcelBossTrial (parse reached the mapping)");
        check("0.3.7".equals(plugin.getString("version")),
                "plugin.yml version is 0.3.7");
        check(plugin.getStringList("softdepend").contains("ExcelTrainingWheels"),
                "plugin.yml softdepend carries ExcelTrainingWheels");
        String description = plugin.getString("description", "");
        check(description.contains("0.3.7 closes the TrainingWheels pulse gap"),
                "plugin.yml description carries the 0.3.7 sentence (whole scalar parsed)");

        File configYml = new File("config.yml");
        check(configYml.isFile(), "config.yml present at tree root");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configYml);
        for (String chapter : new String[] {"wither", "elder-guardian", "warden", "dragon"}) {
            check(config.getBoolean(chapter + ".pressure.pulse-respect-training-wheels", false),
                    "config.yml parses with the 0.3.7 key true: " + chapter);
        }

        System.out.println("PluginYmlParseTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
