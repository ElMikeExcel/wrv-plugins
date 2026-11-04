package com.winkreyesvalley.excelmagiclevel.adapter.magic;

import com.elmakers.mine.bukkit.api.magic.MageController;
import com.elmakers.mine.bukkit.api.magic.ModifierTemplate;
import com.winkreyesvalley.excelmagiclevel.core.Boost;
import com.winkreyesvalley.excelmagiclevel.core.Curve;
import com.winkreyesvalley.excelmagiclevel.core.ModifierTemplates;
import com.winkreyesvalley.excelmagiclevel.core.RewardTable;

import java.util.ArrayList;
import java.util.List;

/**
 * ExcelMagicLevel — the enable-time consistency check (SPEC §7.2). The
 * reward table lives in two mirrored places by design (our config.yml +
 * Magic's modifiers file); this check is the contract that the two copies
 * can never drift SILENTLY. It warns per template and never blocks
 * enabling — boosts are soft power, a stale template is a warning, not
 * an outage.
 */
public final class TemplateDriftCheck {
    private static final double TOLERANCE = 1e-6;

    private TemplateDriftCheck() {
    }

    /** @return one warning string per problem found; empty = fully in sync. */
    public static List<String> check(MageController controller, String prefix, Curve curve, RewardTable rewards) {
        List<String> warnings = new ArrayList<>();
        for (int level = 2; level <= curve.maxLevel(); level++) {
            String key = ModifierTemplates.keyFor(prefix, level, curve.maxLevel());
            ModifierTemplate template = controller.getModifierTemplate(key);
            if (template == null) {
                warnings.add("modifier template " + key + " is MISSING from Magic's modifiers — "
                        + "Magic Level " + level + " will grant no boost until the generated fragment is installed");
                continue;
            }
            Boost expected = rewards.boostAt(level);
            compare(warnings, key, "mana_max_boost", template.getDouble("mana_max_boost", Double.NaN), expected.manaMaxBoost());
            compare(warnings, key, "mana_regeneration_boost", template.getDouble("mana_regeneration_boost", Double.NaN), expected.manaRegenerationBoost());
            compare(warnings, key, "cost_reduction", template.getDouble("cost_reduction", Double.NaN), expected.costReduction());
        }
        return warnings;
    }

    private static void compare(List<String> warnings, String key, String property, double actual, double expected) {
        if (Double.isNaN(actual) || Math.abs(actual - expected) > TOLERANCE) {
            warnings.add("modifier template " + key + " has " + property + "=" + actual
                    + " but ExcelMagicLevel's config reward table says " + expected
                    + " — regenerate the modifiers fragment from config");
        }
    }
}
