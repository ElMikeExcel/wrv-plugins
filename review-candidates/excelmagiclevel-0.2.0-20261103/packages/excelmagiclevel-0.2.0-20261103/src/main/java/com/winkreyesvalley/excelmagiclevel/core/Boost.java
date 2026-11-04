package com.winkreyesvalley.excelmagiclevel.core;

/**
 * ExcelMagicLevel — one level's cumulative boost totals, as fractions
 * (0.0125 = +1.25%). Only properties Magic's modifier palette actually
 * has (SPEC §0.2): mana pool, mana regen, cast cost. There is no damage
 * property in Magic, by design — Magic Level is sustain, never burst.
 */
public record Boost(double manaMaxBoost, double manaRegenerationBoost, double costReduction) {
    public static final Boost NONE = new Boost(0.0, 0.0, 0.0);

    public Boost scale(double factor) {
        return new Boost(manaMaxBoost * factor, manaRegenerationBoost * factor, costReduction * factor);
    }
}
