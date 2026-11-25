/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;

/**
 * 0.5.8: the emloot form's pure contract - the mode word, the
 * loot-file and count validation, and the stack sizing the
 * adapter feeds the claims store. Garbage is refused at this
 * layer with NO state change; whether a well-formed file name
 * names a real EliteMobs item is EM-registry-bound and stays
 * adapter-side (EmLootGateway), pinned by the rig proof, the
 * same split the material form keeps with Material.isItem().
 */
public final class GrantClaimEmLootPlanTest {

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
        // --- The mode word.
        check(GrantClaimPlan.isEmLootForm("emloot"), "the mode word matches");
        check(GrantClaimPlan.isEmLootForm(" EmLoot "), "the mode word trims + folds case");
        check(!GrantClaimPlan.isEmLootForm("signature"), "signature is not the emloot form");
        check(!GrantClaimPlan.isEmLootForm("NETHERITE_SWORD"), "a material is not the emloot form");
        check(!GrantClaimPlan.isEmLootForm(null), "a null mode word is not the emloot form");
        check(!GrantClaimPlan.isSignatureForm("emloot"), "emloot is not the signature form");

        // --- Happy paths: the count word is optional.
        var bare = GrantClaimPlan.parseEmLoot("primis_ice_village_alanus_dagger.yml", null);
        check(bare.ok() && bare.plan().count() == 1
                        && "primis_ice_village_alanus_dagger.yml".equals(bare.plan().lootFile()),
                "no count word defaults to 1");
        var counted = GrantClaimPlan.parseEmLoot("some_loot.yml", "3");
        check(counted.ok() && counted.plan().count() == 3, "an explicit count parses");
        check(GrantClaimPlan.parseEmLoot("some_loot.yml",
                        String.valueOf(GrantClaimPlan.MAX_COUNT)).ok(),
                "the 64x64 bound itself is allowed");
        check(GrantClaimPlan.parseEmLoot("  padded.yml  ", " 2 ").ok(),
                "both words trim");

        // --- Loot files refused: blank, null, paths.
        check(GrantClaimPlan.parseEmLoot("", "1").error() == GrantClaimPlan.EmLootError.LOOT_FILE,
                "empty loot file refused");
        check(GrantClaimPlan.parseEmLoot("   ", "1").error() == GrantClaimPlan.EmLootError.LOOT_FILE,
                "blank loot file refused");
        check(GrantClaimPlan.parseEmLoot(null, "1").error() == GrantClaimPlan.EmLootError.LOOT_FILE,
                "null loot file refused");
        check(GrantClaimPlan.parseEmLoot("primis/dagger.yml", "1").error()
                        == GrantClaimPlan.EmLootError.LOOT_FILE,
                "a path is not a bare file name");
        check(GrantClaimPlan.parseEmLoot("..\\dagger.yml", "1").error()
                        == GrantClaimPlan.EmLootError.LOOT_FILE,
                "a windows path is not a bare file name");

        // --- Counts refused: zero, negative, over the bound, junk.
        check(GrantClaimPlan.parseEmLoot("a.yml", "0").error() == GrantClaimPlan.EmLootError.COUNT,
                "zero refused");
        check(GrantClaimPlan.parseEmLoot("a.yml", "-2").error() == GrantClaimPlan.EmLootError.COUNT,
                "negative refused");
        check(GrantClaimPlan.parseEmLoot("a.yml", String.valueOf(GrantClaimPlan.MAX_COUNT + 1))
                        .error() == GrantClaimPlan.EmLootError.COUNT,
                "one over the bound refused");
        check(GrantClaimPlan.parseEmLoot("a.yml", "abc").error() == GrantClaimPlan.EmLootError.COUNT,
                "non-numeric refused");
        check(GrantClaimPlan.parseEmLoot("a.yml", "1.5").error() == GrantClaimPlan.EmLootError.COUNT,
                "fractional refused");
        check(GrantClaimPlan.parseEmLoot("a.yml", "").error() == GrantClaimPlan.EmLootError.COUNT,
                "an empty count word is junk, not the default");
        check(GrantClaimPlan.parseEmLoot("a.yml", "99999999999999999999").error()
                        == GrantClaimPlan.EmLootError.COUNT,
                "overflowing count refused, not wrapped");

        // --- Stack sizing (the adapter supplies the built
        // stack's own max stack size): a unique EM weapon stacks
        // to 1, so a count of 3 is three separately built stacks.
        check(GrantClaimPlan.split(3, 1).equals(List.of(1, 1, 1)),
                "3 at max 1 builds three single stacks");
        check(GrantClaimPlan.split(130, 64).equals(List.of(64, 64, 2)),
                "a stackable EM consumable splits like a material");

        System.out.println("GrantClaimEmLootPlanTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
