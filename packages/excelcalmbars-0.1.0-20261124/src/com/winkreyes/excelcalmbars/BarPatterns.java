package com.winkreyes.excelcalmbars;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Positive identification of XP-gain boss bars by their plain-text
 * title. A bar is held ONLY when its title matches one of the
 * configured XP patterns AND does not match the never-hold list.
 * Anything else — real boss bars, quest bars, the ExcelDeathProtection
 * status bar, any title we cannot positively identify — always passes
 * straight through. Matching is deliberately anchored and strict:
 * a missed XP bar merely shows (the pre-fix behaviour), while a
 * wrongly held boss bar would be a real defect, so every pattern
 * errs on the side of passing.
 *
 * The default patterns are built from the live title templates:
 * - Jobs (locale bossBarOutput): "Lvl 37 Explorer: 633.25/5846 xp (+2.35)"
 * - mcMMO (locale XPBar.*): "Swords Lv.268", plus the early-game
 *   boost title "Learning a new skill..."
 * - EliteMobs (skills.yml skillBarTitleFormat): "Swords Lv.13 (+48 XP)"
 *   and skillLevelUpTitleFormat: "✦ Swords LEVEL UP! → Lv.14 ✦"
 */
public final class BarPatterns {

    private final List<Pattern> xpPatterns = new ArrayList<>();
    private final List<String> neverHoldSubstrings = new ArrayList<>();

    public BarPatterns(FileConfiguration config) {
        for (String section : new String[]{"jobs", "mcmmo", "elitemobs"}) {
            for (String regex : config.getStringList("patterns." + section)) {
                xpPatterns.add(Pattern.compile(regex));
            }
        }
        for (String s : config.getStringList("never-hold-substrings")) {
            neverHoldSubstrings.add(s.toLowerCase());
        }
    }

    /** True only for a positively identified XP-gain bar title. */
    public boolean isXpBar(String plainTitle) {
        if (plainTitle == null || plainTitle.isEmpty()) return false;
        if (isNeverHold(plainTitle)) return false;
        for (Pattern p : xpPatterns) {
            if (p.matcher(plainTitle).matches()) return true;
        }
        return false;
    }

    /** The standing-status / protected titles that must never be held. */
    public boolean isNeverHold(String plainTitle) {
        if (plainTitle == null) return false;
        String lower = plainTitle.toLowerCase();
        for (String s : neverHoldSubstrings) {
            if (!s.isEmpty() && lower.contains(s)) return true;
        }
        return false;
    }
}
