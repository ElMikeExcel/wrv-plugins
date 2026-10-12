// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.util.*;
import org.bukkit.inventory.ItemStack;

/** Exact metadata, storage slots only, and no overflow-to-ground behavior. */
public final class InventoryPlan {
    private InventoryPlan() {}
    public static ItemStack[] copy(ItemStack[] items) {
        ItemStack[] result = items.clone();
        for (int i=0;i<result.length;i++) if (result[i] != null) result[i] = result[i].clone();
        return result;
    }
    public static int count(ItemStack[] items, ItemStack sample) {
        int result=0;
        for (ItemStack i:items) if (same(i,sample)) result=Math.addExact(result,i.getAmount());
        return result;
    }
    public static boolean same(ItemStack a, ItemStack b) {
        return a != null && b != null && !a.getType().isAir() && a.isSimilar(b);
    }
    public static ItemStack[] remove(ItemStack[] before, ItemStack sample, int amount) {
        if (amount<1 || count(before,sample)<amount) throw new IllegalArgumentException("Stock changed; please choose again.");
        ItemStack[] after=copy(before); int left=amount;
        for (int i=0;i<after.length&&left>0;i++) if (same(after[i],sample)) {
            int n=Math.min(left,after[i].getAmount()); left-=n;
            if (n==after[i].getAmount()) after[i]=null; else after[i].setAmount(after[i].getAmount()-n);
        }
        return after;
    }
    public static ItemStack[] add(ItemStack[] before, ItemStack sample, int amount, int inventoryMax) {
        if (amount<1) throw new IllegalArgumentException("Invalid amount");
        ItemStack[] after=copy(before); int left=amount;
        int max=Math.min(sample.getMaxStackSize(),inventoryMax);
        if (max<1) throw new IllegalArgumentException("Invalid stack size");
        for (int i=0;i<after.length&&left>0;i++) if (same(after[i],sample)) {
            int n=Math.min(left,Math.max(0,max-after[i].getAmount()));left-=n;after[i].setAmount(after[i].getAmount()+n);
        }
        for (int i=0;i<after.length&&left>0;i++) if (after[i]==null || after[i].getType().isAir()) {
            int n=Math.min(left,max);left-=n;after[i]=sample.clone();after[i].setAmount(n);
        }
        if (left!=0) throw new IllegalArgumentException("Your inventory needs more room. Nothing was charged.");
        return after;
    }
    public static String fingerprint(ItemStack item) {
        ItemStack normalized=item.clone(); normalized.setAmount(1);
        return Base64.getEncoder().encodeToString(normalized.serializeAsBytes());
    }
    public static String snapshot(ItemStack[] items) {
        StringBuilder b=new StringBuilder();
        for (ItemStack i:items) b.append(i==null?"-":Base64.getEncoder().encodeToString(i.serializeAsBytes())).append(';');
        return b.toString();
    }
}
