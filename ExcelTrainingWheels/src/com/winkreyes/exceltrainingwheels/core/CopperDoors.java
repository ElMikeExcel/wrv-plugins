package com.winkreyes.exceltrainingwheels.core;

import java.util.Set;

/**
 * The copper door family for the door-tier ladder (Mike, Oct 19,
 * 2026): every oxidation level, waxed or not — eight materials,
 * one tier. Matched by exact Material name so the family is
 * unit-testable on a plain JVM (the glue passes
 * {@code block.getType().name()}); Bukkit Material names are
 * uppercase, and the match is deliberately case-sensitive so a
 * lookalike name can never slip a non-copper door into the tier.
 */
public final class CopperDoors {

    private static final Set<String> NAMES = Set.of(
            "COPPER_DOOR", "EXPOSED_COPPER_DOOR",
            "WEATHERED_COPPER_DOOR", "OXIDIZED_COPPER_DOOR",
            "WAXED_COPPER_DOOR", "WAXED_EXPOSED_COPPER_DOOR",
            "WAXED_WEATHERED_COPPER_DOOR", "WAXED_OXIDIZED_COPPER_DOOR");

    private CopperDoors() {}

    public static boolean isCopperDoor(String materialName) {
        return materialName != null && NAMES.contains(materialName);
    }
}
