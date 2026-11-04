package com.winkreyesvalley.excelmagiclevel.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * ExcelMagicLevel — GENERATES Magic's per-level modifier templates from the
 * config curve + reward table (SPEC §0.2 condition 1, §5.2). Nothing in this
 * plugin hand-writes a per-level table: the templates are a pure function of
 * config data, so a level-count rescale or a reward retune regenerates them
 * instead of needing a rebuild.
 *
 * <p>The generated fragment is appended to Magic's {@code modifiers.yml} by
 * the install packet (Phase 3). At runtime the plugin never writes Magic's
 * files — it only reads the live templates back for the drift check.</p>
 */
public final class ModifierTemplates {
    private ModifierTemplates() {
    }

    /** Zero-pad width for level numbers in keys: 20 → 2 ("02".."20"), 1000 → 4. */
    public static int keyWidth(int maxLevel) {
        return Math.max(2, String.valueOf(maxLevel).length());
    }

    /** e.g. keyFor("wrv_magiclevel_", 2, 20) → "wrv_magiclevel_02". */
    public static String keyFor(String prefix, int level, int maxLevel) {
        String num = String.valueOf(level);
        int width = keyWidth(maxLevel);
        if (num.length() < width) {
            num = "0".repeat(width - num.length()) + num;
        }
        return prefix + num;
    }

    /** True for any key this plugin owns (used by reconcile/strip). */
    public static boolean isOurs(String prefix, String key) {
        return key != null && key.startsWith(prefix);
    }

    /** A YAML number with no float dust: 0.0125 × 19 renders "0.2375", never 0.23750000000000002. */
    static String num(double perLevelIncrement, int steps) {
        return BigDecimal.valueOf(perLevelIncrement)
                .multiply(BigDecimal.valueOf(steps))
                .setScale(6, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }

    /**
     * The full modifiers.yml fragment for levels 2..maxLevel, generated from
     * the config tables. Deterministic: same config in, same bytes out.
     */
    public static String generateFragment(String prefix, Curve curve, RewardTable rewards) {
        StringBuilder out = new StringBuilder();
        out.append("# ExcelMagicLevel modifier templates — GENERATED from ExcelMagicLevel's\n");
        out.append("# config.yml (curve + rewards tables) by ModifierTemplates. Do not hand-edit:\n");
        out.append("# change the config and regenerate. One template per level >= 2; values are\n");
        out.append("# the cumulative totals AT that level. Excel — Wink Reyes Valley.\n");
        Boost per = rewards.perLevel();
        for (int level = 2; level <= curve.maxLevel(); level++) {
            int steps = level - 1;
            out.append(keyFor(prefix, level, curve.maxLevel())).append(":\n");
            out.append("  name: Magic Level ").append(level).append('\n');
            out.append("  description: ExcelMagicLevel reward at Magic Level ").append(level)
                    .append(" — mana pool +").append(Percents.format(per.manaMaxBoost() * steps))
                    .append("%, mana regen +").append(Percents.format(per.manaRegenerationBoost() * steps))
                    .append("%, cast cost -").append(Percents.format(per.costReduction() * steps))
                    .append("%.\n");
            out.append("  mana_max_boost: ").append(num(per.manaMaxBoost(), steps)).append('\n');
            out.append("  mana_regeneration_boost: ").append(num(per.manaRegenerationBoost(), steps)).append('\n');
            out.append("  cost_reduction: ").append(num(per.costReduction(), steps)).append('\n');
        }
        return out.toString();
    }
}
