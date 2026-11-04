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
