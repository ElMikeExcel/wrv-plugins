/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.List;
import org.bukkit.Material;

/**
 * 0.2.11: the grantclaim input contract — garbage is refused with
 * NO state change (the adapter never reaches the store on a failed
 * parse), and a valid grant splits into legal stack sizes. The
 * store round-trip itself is pinned by GrantClaimStoreTest; the
 * end-to-end command is rig-proven.
 */
public final class GrantClaimPlanTest {

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
        // --- Happy paths.
        var breath = GrantClaimPlan.parse("DRAGON_BREATH", "4");
        check(breath.ok() && breath.plan().material() == Material.DRAGON_BREATH
                        && breath.plan().count() == 4,
                "breath x4 parses");
        check(GrantClaimPlan.parse("dragon_breath", "130").ok(),
                "names are case-insensitive");
        check(GrantClaimPlan.parse("END_CRYSTAL", String.valueOf(GrantClaimPlan.MAX_COUNT)).ok(),
                "the 64x64 bound itself is allowed");

        // --- Splitting (the adapter supplies the material's own
        // max stack size; isItem/maxStack are registry-bound in
        // Paper 26 and stay adapter-side).
        check(GrantClaimPlan.split(130, 64).equals(List.of(64, 64, 2)), "130 at 64 splits 64+64+2");
        check(GrantClaimPlan.split(3, 1).equals(List.of(1, 1, 1)), "3 at 1 splits one per stack");
        check(GrantClaimPlan.split(64, 64).equals(List.of(64)), "64 at 64 is one stack");
        check(GrantClaimPlan.split(4096, 64).size() == 64, "the bound splits into 64 stacks");
        check(GrantClaimPlan.split(5, 0).equals(List.of(1, 1, 1, 1, 1)),
                "a degenerate max stack is floored to 1");

        // --- Materials refused: garbage, air, null. (Non-item
        // materials like WATER resolve by name and are refused
        // adapter-side via Material.isItem(); the rig pins that.)
        check(GrantClaimPlan.parse("NOT_A_MATERIAL", "4").error() == GrantClaimPlan.Error.MATERIAL,
                "garbage material refused");
        check(GrantClaimPlan.parse("AIR", "4").error() == GrantClaimPlan.Error.MATERIAL,
                "air refused");
        check(GrantClaimPlan.parse(null, "4").error() == GrantClaimPlan.Error.MATERIAL,
                "null material refused");
        check(GrantClaimPlan.parse("", "4").error() == GrantClaimPlan.Error.MATERIAL,
                "empty material refused");

        // --- Counts refused: zero, negative, over the bound, junk.
        check(GrantClaimPlan.parse("DRAGON_BREATH", "0").error() == GrantClaimPlan.Error.COUNT,
                "zero refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", "-5").error() == GrantClaimPlan.Error.COUNT,
                "negative refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", String.valueOf(GrantClaimPlan.MAX_COUNT + 1))
                        .error() == GrantClaimPlan.Error.COUNT,
                "one over the bound refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", "abc").error() == GrantClaimPlan.Error.COUNT,
                "non-numeric refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", "4.5").error() == GrantClaimPlan.Error.COUNT,
                "fractional refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", null).error() == GrantClaimPlan.Error.COUNT,
                "null count refused");
        check(GrantClaimPlan.parse("DRAGON_BREATH", "99999999999999999999")
                        .error() == GrantClaimPlan.Error.COUNT,
                "overflowing count refused, not wrapped");

        // 0.4.0 (PM-10): the signature form's pure decisions.
        check(GrantClaimPlan.isSignatureForm("signature"), "the mode word matches");
        check(GrantClaimPlan.isSignatureForm(" Signature "), "the mode word trims + folds case");
        check(!GrantClaimPlan.isSignatureForm("NETHERITE_SWORD"), "a material is not the signature form");
        check(!GrantClaimPlan.isSignatureForm(null), "a null mode word is not the signature form");
        check(GrantClaimPlan.isRaidSignature("raid"), "raid names the raid signature");
        check(GrantClaimPlan.isRaidSignature("RAID"), "raid folds case");
        check(!GrantClaimPlan.isRaidSignature("wither"), "a chapter is not the raid");
        check(GrantClaimPlan.signatureChapter("wither") == BossKind.WITHER, "wither resolves");
        check(GrantClaimPlan.signatureChapter("warden") == BossKind.WARDEN, "warden resolves");
        check(GrantClaimPlan.signatureChapter("dragon") == BossKind.ENDER_DRAGON, "the dragon alias resolves");
        check(GrantClaimPlan.signatureChapter("ender_dragon") == BossKind.ENDER_DRAGON, "the dragon seal id resolves");
        check(GrantClaimPlan.signatureChapter("elder") == BossKind.ELDER_GUARDIAN, "the elder alias resolves");
        check(GrantClaimPlan.signatureChapter("elder_guardian") == BossKind.ELDER_GUARDIAN, "the elder seal id resolves");
        check(GrantClaimPlan.signatureChapter("raid") == null, "raid is not a loot chapter (adapter special case)");
        check(GrantClaimPlan.signatureChapter("creeper") == null, "an unknown word resolves to nothing");
        check(GrantClaimPlan.signatureChapter(null) == null, "a null word resolves to nothing");

        System.out.println("GrantClaimPlanTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) {
            System.exit(1);
        }
    }
}
