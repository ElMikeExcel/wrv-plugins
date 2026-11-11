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
        check("0.5.5".equals(plugin.getString("version")),
                "plugin.yml version is 0.5.5");
        check(plugin.getStringList("softdepend").contains("ExcelTrainingWheels"),
                "plugin.yml softdepend carries ExcelTrainingWheels");
        String description = plugin.getString("description", "");
        check(description.contains("0.4.0 is the Wither-night fix train"),
                "plugin.yml description carries the 0.4.0 sentence (whole scalar parsed)");
        check(description.contains("0.4.1 adds the post-fight stats surface"),
                "plugin.yml description carries the 0.4.1 sentence");
        check(description.contains("0.5.2 is the combined Item 35 build"),
                "plugin.yml description carries the 0.5.2 sentence");
        check(description.contains("0.5.3 files the Item 39 Warden retune"),
                "plugin.yml description carries the 0.5.3 sentence");
        check(description.contains("0.5.4 is the elder sprinkle melee fix"),
                "plugin.yml description carries the 0.5.4 sentence");
        check(description.contains("0.5.5 adds the Warden first-break layer"),
                "plugin.yml description carries the 0.5.5 sentence");
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
            check(!config.getBoolean(chapter + ".pressure.apply-to-sonic-boom", true),
                    "config.yml ships the 0.5.3 key false: " + chapter);
        }
        check(config.getDouble("warden.pressure.outgoing-multiplier", 0) == 1.0,
                "config.yml ships the 0.5.3 R2 default: warden.pressure.outgoing-multiplier 1.0");
        check(config.getDouble("warden.pressure.ramp-max", 0) == 1.5,
                "config.yml ships the 0.5.3 R2 default: warden.pressure.ramp-max 1.5");
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
        // Melee fix (0.5.4) config pins: the elder ships both
        // keys; no other chapter ships either key.
        check(config.getDouble("elder-guardian.sprinkle.knockback-resistance", 0) == -1.0,
                "config.yml ships the 0.5.4 key: elder-guardian.sprinkle.knockback-resistance -1.0");
        check(config.getInt("elder-guardian.sprinkle.stagger-ticks", 0) == 40,
                "config.yml ships the 0.5.4 key: elder-guardian.sprinkle.stagger-ticks 40");
        check(!config.isSet("wither.sprinkle.knockback-resistance")
                        && !config.isSet("warden.sprinkle.knockback-resistance")
                        && !config.isSet("dragon.sprinkle.knockback-resistance")
                        && !config.isSet("wither.sprinkle.stagger-ticks")
                        && !config.isSet("warden.sprinkle.stagger-ticks")
                        && !config.isSet("dragon.sprinkle.stagger-ticks"),
                "config.yml ships the 0.5.4 keys for the elder ONLY (other chapters opt in later)");

        // First-break (0.5.5) config pins: the warden block
        // ships the settled slate and the full EM kit.
        check(config.getBoolean("first-break.warden.enabled", false),
                "config.yml ships the 0.5.5 key: first-break.warden.enabled true");
        check(config.getLong("first-break.warden.cash-per-champion", 0) == 10000L,
                "config.yml ships the 0.5.5 key: first-break.warden.cash-per-champion 10000 (D6 total)");
        check(config.getInt("first-break.warden.jackpot-extra-rolls", 0) == 2,
                "config.yml ships the 0.5.5 key: first-break.warden.jackpot-extra-rolls 2");
        check(config.getLong("first-break.warden.accelerants.mcmmo-xp", 0) == 5000L,
                "config.yml ships the 0.5.5 key: first-break.warden.accelerants.mcmmo-xp 5000");
        check(config.getLong("first-break.warden.accelerants.magiclevel-xp", 0) == 250L,
                "config.yml ships the 0.5.5 key: first-break.warden.accelerants.magiclevel-xp 250");
        check("Sealbreakers".equals(config.getString("first-break.warden.title")),
                "config.yml ships the 0.5.5 key: first-break.warden.title Sealbreakers");
        check(config.getMapList("first-break.warden.em-kit").size() == 9,
                "config.yml ships the 0.5.5 EM kit: 9 rows");
        check(config.getString("messages.first-break-headline", "").contains("first break"),
                "config.yml ships the 0.5.5 key: messages.first-break-headline");

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
