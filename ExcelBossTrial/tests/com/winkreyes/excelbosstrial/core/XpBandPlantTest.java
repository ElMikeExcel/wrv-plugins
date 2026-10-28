package com.winkreyes.excelbosstrial.core;

import com.winkreyes.excelbosstrial.core.XpBandPlant.Damager;

/**
 * The plant mirror (0.2.4): exactly the hit shapes the installed
 * mcMMO fork pays combat XP for plant a verdict - no more (a
 * phantom token would shift the FIFO), no fewer (a missing token
 * would leave a cheesed hit at full pay). Source-pinned against
 * CombatUtils.processCombatAttack / resolveMeleeSkill and
 * ItemUtils (fork build 2.3.003-WRV-XP-2, Oct 22, 2026).
 */
public final class XpBandPlantTest {

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

    private static void pays(Damager d, String hand, String label) {
        check(XpBandPlant.paysCombatXp(d, hand), label + " should plant");
    }

    private static void skips(Damager d, String hand, String label) {
        check(!XpBandPlant.paysCombatXp(d, hand), label + " should NOT plant");
    }

    public static void main(String[] args) {
        // Non-melee damagers: the hand is irrelevant.
        pays(Damager.WOLF, "DIAMOND_SWORD", "wolf bite (owner gains Taming)");
        pays(Damager.WOLF, null, "wolf bite, hand unknown");
        pays(Damager.TRIDENT, "TRIDENT", "thrown trident");
        pays(Damager.TRIDENT, "BOW", "thrown trident, hand already switched");
        pays(Damager.ARROW, "BOW", "bow arrow");
        pays(Damager.ARROW, "CROSSBOW", "crossbow bolt");
        pays(Damager.ARROW, null, "arrow, hand unknown");

        // Shapes mcMMO never pays: no plant, whatever the hand.
        skips(Damager.OTHER, "DIAMOND_SWORD", "crystal/anchor/TNT/cloud shape");
        skips(Damager.OTHER, null, "other shape, hand unknown");
        skips(null, "DIAMOND_SWORD", "null damager");

        // Melee with a claimed skill: plants.
        pays(Damager.MELEE, "DIAMOND_SWORD", "sword swing");
        pays(Damager.MELEE, "WOODEN_SWORD", "wooden sword swing");
        pays(Damager.MELEE, "NETHERITE_AXE", "axe swing");
        pays(Damager.MELEE, "TRIDENT", "trident held and swung (Tridents melee)");
        pays(Damager.MELEE, "MACE", "mace swing");
        pays(Damager.MELEE, "IRON_SPEAR", "spear swing (Spears)");
        pays(Damager.MELEE, "AIR", "bare hand (Unarmed)");
        pays(Damager.MELEE, null, "hand unknown reads as paying (documented asymmetry)");

        // Melee with NO claimed skill under the stock config
        // (Skills.Unarmed.Items_As_Unarmed defaults false): no gain
        // event fires in mcMMO, so no plant.
        skips(Damager.MELEE, "BOW", "bow swung in melee pays nothing");
        skips(Damager.MELEE, "CROSSBOW", "crossbow swung in melee pays nothing");
        skips(Damager.MELEE, "TORCH", "torch in hand pays nothing (default config)");
        skips(Damager.MELEE, "IRON_PICKAXE", "pickaxe is a tool, not an axe");
        skips(Damager.MELEE, "DIAMOND_SHOVEL", "shovel swing pays nothing");
        skips(Damager.MELEE, "FISHING_ROD", "fishing rod swing pays nothing");
        skips(Damager.MELEE, "SHIELD", "shield in main hand pays nothing");

        System.out.println("XpBandPlantTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}

