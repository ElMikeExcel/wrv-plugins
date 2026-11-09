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
        check("0.5.2".equals(plugin.getString("version")),
                "plugin.yml version is 0.5.2");
        check(plugin.getStringList("softdepend").contains("ExcelTrainingWheels"),
                "plugin.yml softdepend carries ExcelTrainingWheels");
        String description = plugin.getString("description", "");
        check(description.contains("0.4.0 is the Wither-night fix train"),
                "plugin.yml description carries the 0.4.0 sentence (whole scalar parsed)");
        check(description.contains("0.4.1 adds the post-fight stats surface"),
                "plugin.yml description carries the 0.4.1 sentence");
        check(description.contains("0.5.2 is the combined Item 35 build"),
                "plugin.yml description carries the 0.5.2 sentence");
        check(description.contains("gives every boss its voice"),
                "plugin.yml description names BOTH feature sets (Packet B + voice)");
        check(!description.contains("carries no voice features"),
                "plugin.yml description retires the 0.5.1 no-voice sentence");

        File configYml = new File("config.yml");
        check(configYml.isFile(), "config.yml present at tree root");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configYml);
        for (String chapter : new String[] {"wither", "elder-guardian", "warden", "dragon"}) {
            check(config.getBoolean(chapter + ".pressure.pulse-respect-training-wheels", false),
                    "config.yml parses with the 0.3.7 key true: " + chapter);
        }
        check(config.getBoolean("fight-tracking.stats-popup", false),
                "config.yml parses with the 0.4.1 key true: fight-tracking.stats-popup");
        check(config.getBoolean("fight-tracking.stats-command", false),
                "config.yml parses with the 0.4.1 key true: fight-tracking.stats-command");
        // Packet B (0.5.1) config pins.
        check(config.getDouble("feedback.display-scale", 0) == 1000.0,
                "config.yml parses with the Packet B key: feedback.display-scale 1000");
        check(config.getBoolean("dragon.sprinkle.enabled", false),
                "config.yml parses with the Packet B key true: dragon.sprinkle.enabled (D-4 armed)");
        check(config.getInt("dragon.sprinkle.cadence-seconds", 0) == 180,
                "config.yml parses with the Packet B key: dragon.sprinkle.cadence-seconds 180");
        check(config.getInt("dragon.sprinkle.max-alive", 0) == 10,
                "config.yml parses with the Packet B key: dragon.sprinkle.max-alive 10");
        check(config.getInt("dragon.sprinkle.max-per-fight", 0) == 40,
                "config.yml parses with the Packet B key: dragon.sprinkle.max-per-fight 40");
        check(!config.getBoolean("dragon.adds.enabled", true),
                "config.yml parses with the Packet B key false: dragon.adds.enabled (retired)");
        check(config.getInt("dragon.flip-crystals.count", 0) == 4,
                "config.yml parses with the Packet B key: dragon.flip-crystals.count 4");
        check(config.getInt("dragon.flip-crystals.max-live", 0) == 6,
                "config.yml parses with the Packet B key: dragon.flip-crystals.max-live 6");
        check(config.getBoolean("elder-guardian.relay.enabled", false),
                "config.yml parses with the Packet B key true: elder-guardian.relay.enabled");

        // Voice (0.5.2) config pins: the taunt-sounds alignment
        // and every chapter's voice slugs, as bundled.
        check(config.getStringList("wither.taunts").size() == 10,
                "config.yml: wither.taunts carries the 10 shipped lines");
        check(config.getStringList("wither.taunt-sounds").size() == 10,
                "config.yml: wither.taunt-sounds is index-aligned (10 entries)");
        check("eat_01".equals(config.getStringList("wither.taunt-sounds").get(0)),
                "config.yml: taunt-sounds[0] is eat_01 (MORE.)");
        check("eat_05".equals(config.getStringList("wither.taunt-sounds").get(4)),
                "config.yml: taunt-sounds[4] is eat_05 (Feed me, champions.)");
        check("eat_10".equals(config.getStringList("wither.taunt-sounds").get(9)),
                "config.yml: taunt-sounds[9] is eat_10");
        check("flip".equals(config.getString("dragon.voice.flip")),
                "config.yml parses with the voice key: dragon.voice.flip");
        check("beat50p2".equals(config.getString("dragon.voice.beat-50-phase2")),
                "config.yml parses with the voice key: dragon.voice.beat-50-phase2");
        check("addwave".equals(config.getString("dragon.voice.addwave")),
                "config.yml parses with the voice key: dragon.voice.addwave");
        check("relay".equals(config.getString("elder-guardian.voice.relay")),
                "config.yml parses with the voice key: elder-guardian.voice.relay");
        check("lastdoor".equals(config.getString("elder-guardian.voice.lastdoor")),
                "config.yml parses with the voice key: elder-guardian.voice.lastdoor");
        check("boom".equals(config.getString("warden.voice.boom")),
                "config.yml parses with the voice key: warden.voice.boom");
        check("boom_alt".equals(config.getString("warden.voice.boom-alt")),
                "config.yml parses with the voice key: warden.voice.boom-alt");

        System.out.println("PluginYmlParseTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
