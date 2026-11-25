package com.winkreyes.excelcalmbars;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

/**
 * Pattern identification check against the shipped config.yml:
 * the real titles from Marvin's screenshot and the live templates
 * must be identified as XP bars; boss bars, the ExcelDeathProtection
 * status bar, quest text and near-miss shapes must NOT be.
 * Plain main-method assertions (no test framework needed).
 */
public final class BarPatternsTest {

    private static int failures = 0;

    private static void expect(BarPatterns patterns, String title,
                               boolean xp) {
        boolean actual = patterns.isXpBar(title);
        if (actual != xp) {
            failures++;
            System.out.println("FAIL: '" + title + "' expected xp="
                    + xp + " got " + actual);
        } else {
            System.out.println("ok:   '" + title + "' xp=" + actual);
        }
    }

    public static void main(String[] args) throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.load(new File(args[0]));
        BarPatterns patterns = new BarPatterns(config);

        // --- must be identified as XP bars ---
        // Jobs (live locale template output)
        expect(patterns, "Lvl 37 Explorer: 633.25/5846 xp (+2.35)", true);
        expect(patterns, "Lvl 33 Hunter: 2529.65/4686 xp (+4.69)", true);
        expect(patterns, "Lvl 1 Miner: 0/100 xp", true);
        // mcMMO (live locale XPBar.*)
        expect(patterns, "Swords Lv.268", true);
        expect(patterns, "Mining Lv.3", true);
        expect(patterns, "Woodcutting Lv.100", true);
        expect(patterns, "Learning a new skill...", true);
        // EliteMobs (live skills.yml formats)
        expect(patterns, "Swords Lv.13 (+48 XP)", true);
        expect(patterns, "Armor Lv.10 (+16 XP)", true);
        expect(patterns, "✦ Swords LEVEL UP! → Lv.14 ✦", true);

        // --- must NEVER be held ---
        // ExcelDeathProtection standing status bar
        expect(patterns, "Inventory protected: 1 death", false);
        expect(patterns, "Inventory protected: 2 deaths", false);
        // Real boss bars
        expect(patterns, "Wither", false);
        expect(patterns, "Ender Dragon", false);
        expect(patterns, "Redwood Rockhound", false);
        expect(patterns, "✦ The Warden ✦", false);
        // Quest / dialogue style titles
        expect(patterns, "Talk to King Nemian", false);
        // Near-miss shapes
        expect(patterns, "Lvl 5 Explorer", false);
        expect(patterns, "Swords Lv.", false);
        expect(patterns, "Combat Lv. 12", false);
        expect(patterns, "", false);
        expect(patterns, null, false);

        if (failures > 0) {
            System.out.println(failures + " FAILURES");
            System.exit(1);
        }
        System.out.println("ALL PATTERN CHECKS PASSED");
    }
}
