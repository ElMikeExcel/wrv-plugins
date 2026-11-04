package com.winkreyesvalley.excelmagiclevel.core;

import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * ExcelMagicLevel — the generator is the ONLY source of per-level template
 * data. These tests pin the shipped 1000-level table (SPEC §10 as amended
 * Oct 31, 2026) and prove a rescale regenerates instead of recompiling.
 */
public class ModifierTemplatesTest {
    private final Curve standard = new Curve(1000, 11.0, 0.30, 1, Map.of());
    private final RewardTable rewards = new RewardTable(
            new Boost(0.00023773773773773774, 0.00023773773773774, 0.0000950950950950951),
            Set.of(25, 50, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000));

    @Test
    public void keyPaddingFollowsLevelCount() {
        assertEquals("wrv_magiclevel_02", ModifierTemplates.keyFor("wrv_magiclevel_", 2, 20));
        assertEquals("wrv_magiclevel_20", ModifierTemplates.keyFor("wrv_magiclevel_", 20, 20));
        assertEquals("wrv_magiclevel_007", ModifierTemplates.keyFor("wrv_magiclevel_", 7, 100));
        assertEquals("wrv_magiclevel_100", ModifierTemplates.keyFor("wrv_magiclevel_", 100, 100));
        assertEquals("wrv_magiclevel_0007", ModifierTemplates.keyFor("wrv_magiclevel_", 7, 1000));
        assertEquals("wrv_magiclevel_1000", ModifierTemplates.keyFor("wrv_magiclevel_", 1000, 1000));
    }

    @Test
    public void fragmentCarriesTheThousandLevelTable() {
        String fragment = ModifierTemplates.generateFragment("wrv_magiclevel_", standard, rewards);
        // 999 templates: levels 2..1000, 4-digit keys.
        assertEquals(999, fragment.lines().filter(l -> l.startsWith("wrv_magiclevel_")).count());
        assertTrue(fragment.contains("wrv_magiclevel_0002:\n  name: Magic Level 2\n"));
        // Spot rows carry the exact even-spread values, 6-decimal rendered
        // (the render is what Magic reads; drift tolerance is 1e-6).
        assertTrue(fragment.contains("  mana_max_boost: 0.000238\n  mana_regeneration_boost: 0.000238\n  cost_reduction: 0.000095\n"));
        assertTrue(fragment.contains("wrv_magiclevel_0100:"));
        assertTrue(fragment.contains("  mana_max_boost: 0.023536\n  mana_regeneration_boost: 0.023536\n  cost_reduction: 0.009414\n"));
        assertTrue(fragment.contains("wrv_magiclevel_0500:"));
        assertTrue(fragment.contains("  mana_max_boost: 0.118631\n  mana_regeneration_boost: 0.118631\n  cost_reduction: 0.047452\n"));
        // Cap row: +23.75 / +23.75 / -9.5, no float dust.
        assertTrue(fragment.contains("wrv_magiclevel_1000:"));
        assertTrue(fragment.contains("  mana_max_boost: 0.2375\n  mana_regeneration_boost: 0.2375\n  cost_reduction: 0.095\n"));
        assertFalse("no float dust anywhere", fragment.contains("0.2375000000") || fragment.contains("0000000000"));
    }

    @Test
    public void generationIsDeterministic() {
        String a = ModifierTemplates.generateFragment("wrv_magiclevel_", standard, rewards);
        String b = ModifierTemplates.generateFragment("wrv_magiclevel_", standard, rewards);
        assertEquals(a, b);
    }

    @Test
    public void rescaleRegenerates() {
        Curve hundred = new Curve(100, 100.0, 1.5, 10, Map.of());
        RewardTable legacy = new RewardTable(new Boost(0.0125, 0.0125, 0.005), Set.of(5, 10, 15, 20));
        String fragment = ModifierTemplates.generateFragment("wrv_magiclevel_", hundred, legacy);
        assertEquals(99, fragment.lines().filter(l -> l.startsWith("wrv_magiclevel_")).count());
        assertTrue(fragment.contains("wrv_magiclevel_100:"));
        // level 100 on the legacy per-level line: 99 steps of +1.25%
        assertTrue(fragment.contains("  mana_max_boost: 1.2375\n"));
    }

    @Test
    public void ownershipPredicate() {
        assertTrue(ModifierTemplates.isOurs("wrv_magiclevel_", "wrv_magiclevel_07"));
        assertTrue(ModifierTemplates.isOurs("wrv_magiclevel_", "wrv_magiclevel_0500"));
        assertTrue(!ModifierTemplates.isOurs("wrv_magiclevel_", "wrv_boon_reserve"));
        assertTrue(!ModifierTemplates.isOurs("wrv_magiclevel_", null));
    }
}
