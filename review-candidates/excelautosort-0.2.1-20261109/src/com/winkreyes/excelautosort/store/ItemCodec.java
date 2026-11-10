package com.winkreyes.excelautosort.store;

import org.bukkit.inventory.ItemStack;

import java.util.Base64;

/**
 * ItemStack ↔ Base64 via Bukkit's own byte serialization: a recovered
 * stack is the same stack — enchants, name, damage and all (§6.2).
 */
public final class ItemCodec {

    private ItemCodec() {}

    public static String toB64(ItemStack stack) {
        if (stack == null) return "";
        return Base64.getEncoder().encodeToString(stack.serializeAsBytes());
    }

    public static ItemStack fromB64(String b64) {
        if (b64 == null || b64.isEmpty()) return null;
        try {
            return ItemStack.deserializeBytes(Base64.getDecoder().decode(b64));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** "minecraft:netherite_sword" form, for Protect records and display. */
    public static String materialKeyOf(ItemStack stack) {
        return stack == null ? "" : stack.getType().getKey().toString();
    }
}

// ExcelAutoSort — Iris / Mike, for Wink Reyes Valley
