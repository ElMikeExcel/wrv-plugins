/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/**
 * 0.2.11: the pure half of /bosstrial grantclaim — validate the
 * console input (material name + count) and the stack-splitting
 * arithmetic, so garbage is rejected with NO state change before
 * anything reaches the claims store. The adapter
 * (ExcelBossTrial.handleGrantClaim) resolves the player, checks
 * Material.isItem() (registry-bound in Paper 26, so it cannot live
 * in this Bukkit-free layer), splits with split(), and writes the
 * claim through TeamClaims.
 *
 * The count bound is a sanity bound, not a balance knob: a grant
 * is a remediation for an owed set, and 64 stacks of 64 is beyond
 * any plausible one. The command's on/off toggle is the config
 * key (commands.grantclaim-enabled).
 */
public final class GrantClaimPlan {

    /** The largest grant: 64 stacks x 64 items. */
    public static final int MAX_COUNT = 64 * 64;

    /** Why a parse failed (the adapter words the console error). */
    public enum Error { MATERIAL, COUNT }

    /** A validated grant: the material and the total count. */
    public record Plan(Material material, int count) {
    }

    /** Parse outcome: exactly one of plan / error is non-null. */
    public record Result(Plan plan, Error error) {
        public boolean ok() {
            return plan != null;
        }
    }

    private GrantClaimPlan() {
    }

    /**
     * Validate a material name and a count. The name must resolve
     * (Material.matchMaterial) and must not be air. The count must
     * be a plain integer in 1..MAX_COUNT.
     */
    public static Result parse(String materialName, String countText) {
        Material material = materialName == null
                ? null : Material.matchMaterial(materialName.strip());
        if (material == null || material == Material.AIR
                || material == Material.CAVE_AIR || material == Material.VOID_AIR) {
            return new Result(null, Error.MATERIAL);
        }
        int count;
        try {
            count = Integer.parseInt(countText == null ? "" : countText.strip());
        } catch (NumberFormatException notANumber) {
            return new Result(null, Error.COUNT);
        }
        if (count < 1 || count > MAX_COUNT) {
            return new Result(null, Error.COUNT);
        }
        return new Result(new Plan(material, count), null);
    }

    /** 0.4.0 (PM-10): the signature form's mode word -
     *  {@code grantclaim <player> signature <chapter>} grants the
     *  chapter's signature set instead of a raw material (the
     *  Nov 7 gap: an owed signature could not be remediated at
     *  all - grantclaim only spoke materials). */
    public static boolean isSignatureForm(String modeWord) {
        return modeWord != null && modeWord.strip().equalsIgnoreCase("signature");
    }

    /** 0.5.8: the emloot form's mode word -
     *  {@code grantclaim <player> emloot <loot-file> [count]}
     *  credits a genuine EliteMobs custom item (Mike, Nov 24,
     *  2026: item returns ride the bossrewards chest, and an
     *  owed EM item could not be credited at all - grantclaim
     *  spoke only vanilla materials and BossTrial's own seal
     *  sets, and EM's own loot give needs the recipient online
     *  standing in front of it). */
    public static boolean isEmLootForm(String modeWord) {
        return modeWord != null && modeWord.strip().equalsIgnoreCase("emloot");
    }

    /** Why an emloot parse failed (the adapter words the error). */
    public enum EmLootError { LOOT_FILE, COUNT }

    /** A validated emloot grant: the loot file and total count. */
    public record EmLootPlan(String lootFile, int count) {
    }

    /** EmLoot parse outcome: exactly one of plan / error non-null. */
    public record EmLootResult(EmLootPlan plan, EmLootError error) {
        public boolean ok() {
            return plan != null;
        }
    }

    /**
     * Validate an emloot grant: a bare EliteMobs custom-item
     * file name (no path separators - the adapter resolves it
     * against EM's own registry, which is the authority on
     * whether the file exists, exactly as Material.isItem()
     * stays adapter-side for the material form) and a count in
     * 1..MAX_COUNT. The count word may be null (the form's
     * [count] is optional): null means 1.
     */
    public static EmLootResult parseEmLoot(String lootFile, String countText) {
        String file = lootFile == null ? "" : lootFile.strip();
        if (file.isEmpty() || file.indexOf('/') >= 0 || file.indexOf('\\') >= 0) {
            return new EmLootResult(null, EmLootError.LOOT_FILE);
        }
        int count = 1;
        if (countText != null) {
            try {
                count = Integer.parseInt(countText.strip());
            } catch (NumberFormatException notANumber) {
                return new EmLootResult(null, EmLootError.COUNT);
            }
            if (count < 1 || count > MAX_COUNT) {
                return new EmLootResult(null, EmLootError.COUNT);
            }
        }
        return new EmLootResult(new EmLootPlan(file, count), null);
    }

    /** 0.4.0 (PM-10): whether the chapter word names the raid
     *  signature - the RaidStandard shield, which is not a
     *  SealLoot table entry and takes its own adapter path. */
    public static boolean isRaidSignature(String chapterWord) {
        return chapterWord != null && chapterWord.strip().equalsIgnoreCase("raid");
    }

    /** 0.4.0 (PM-10): resolve a signature chapter word to its
     *  chapter (the friendly aliases dragon/elder included), or
     *  null when the word names no loot chapter. */
    public static BossKind signatureChapter(String chapterWord) {
        if (chapterWord == null) {
            return null;
        }
        String word = chapterWord.strip().toLowerCase(java.util.Locale.ROOT);
        return switch (word) {
            case "dragon" -> BossKind.ENDER_DRAGON;
            case "elder" -> BossKind.ELDER_GUARDIAN;
            default -> BossKind.fromSealId(word);
        };
    }

    /**
     * Split a validated count into stack sizes of at most maxStack
     * (the material's own maximum, supplied by the adapter): 130 at
     * 64 -> [64, 64, 2]; 3 at 1 -> [1, 1, 1].
     */
    public static List<Integer> split(int count, int maxStack) {
        int step = Math.max(1, maxStack);
        List<Integer> sizes = new ArrayList<>();
        for (int left = count; left > 0; left -= step) {
            sizes.add(Math.min(step, left));
        }
        return List.copyOf(sizes);
    }
}
