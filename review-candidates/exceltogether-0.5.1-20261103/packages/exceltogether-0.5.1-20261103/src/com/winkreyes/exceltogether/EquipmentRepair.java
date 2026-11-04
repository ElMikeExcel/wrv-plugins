package com.winkreyes.exceltogether;

import org.bukkit.Material;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * ExcelTogether 0.5.1 - the write-back equipment repair behind the Magic
 * party-repair actions (ExcelPartyRepairAction / ExcelRepairItemAction).
 *
 * Why this exists: Magic's stock RepairItemAction cannot repair WORN gear
 * on Paper 26.3. EntityEquipment getters hand out copies
 * (CraftEntityEquipment returns asBukkitCopy), so the stock action zeroes
 * the damage on a throwaway and reports CAST while nothing in the world
 * changes (proven in ~/workspace/spell-audit/REPAIREQUIP-REDESIGN.md §4).
 * The fix is one discipline: get -> zero the damage -> SET BACK through
 * the equipment setter, which writes through (asNMSCopy -> setItemSlot).
 *
 * Slot scope mirrors the stock action exactly: with armor, helmet /
 * chestplate / leggings / boots; with held_item, the MAIN hand only.
 * The offhand is never read and never written. Wands are excluded via
 * the caller's predicate (MagicAPI.isWand in production).
 *
 * The damage reads/writes go through a DamageModel seam so the decision
 * logic and the write-back ordering are unit-testable headless; the
 * production model uses the Damageable item meta. Excel - Wink Reyes Valley.
 */
public final class EquipmentRepair {
    private EquipmentRepair() {}

    /** Reads and zeroes an item's damage. META is the in-game model. */
    public interface DamageModel {
        /** True for an empty (air) stack, which is never repairable. */
        boolean isAir(ItemStack stack);
        /** Max durability of the item; 0 (or less) means not damageable. */
        int maxDamage(ItemStack stack);
        /** Current damage; 0 when the item carries none or is not damageable. */
        int damage(ItemStack stack);
        /** Set the item's damage to 0, on the given stack instance. */
        void zeroDamage(ItemStack stack);
    }

    /** Production model: the Damageable item meta, the modern Bukkit path. */
    public static final DamageModel META = new DamageModel() {
        @Override public boolean isAir(ItemStack stack) {
            return stack.getType() == Material.AIR;
        }
        @Override public int maxDamage(ItemStack stack) {
            return stack.getType().getMaxDurability();
        }
        @Override public int damage(ItemStack stack) {
            ItemMeta meta = stack.getItemMeta();
            return meta instanceof Damageable d ? d.getDamage() : 0;
        }
        @Override public void zeroDamage(ItemStack stack) {
            ItemMeta meta = stack.getItemMeta();
            if (meta instanceof Damageable d) {
                d.setDamage(0);
                stack.setItemMeta(meta);
            }
        }
    };

    /**
     * The slots a repair covers, in stock order. The offhand is absent by
     * design: stock RepairItemAction never touches it, and 0.5.1 mirrors
     * stock semantics exactly.
     */
    public static List<EquipmentSlot> slots(boolean armor, boolean heldItem) {
        List<EquipmentSlot> out = new ArrayList<>(5);
        if (armor) {
            out.add(EquipmentSlot.HEAD);
            out.add(EquipmentSlot.CHEST);
            out.add(EquipmentSlot.LEGS);
            out.add(EquipmentSlot.FEET);
        }
        if (heldItem) out.add(EquipmentSlot.HAND);
        return out;
    }

    /** True when the stack is a real, damaged, repairable non-wand item. */
    public static boolean repairable(ItemStack stack, Predicate<ItemStack> isWand, DamageModel model) {
        return stack != null
                && !model.isAir(stack)
                && !isWand.test(stack)
                && model.maxDamage(stack) > 0
                && model.damage(stack) > 0;
    }

    /**
     * Repair every covered slot of the given equipment. Each repaired item
     * is read, zeroed, and SET BACK through the equipment setter - the
     * write-back Paper 26.3 requires. Returns the number of items repaired.
     */
    public static int repair(EntityEquipment equipment, boolean armor, boolean heldItem,
                             Predicate<ItemStack> isWand, DamageModel model) {
        if (equipment == null) return 0;
        int repaired = 0;
        for (EquipmentSlot slot : slots(armor, heldItem)) {
            ItemStack stack = equipment.getItem(slot);
            if (!repairable(stack, isWand, model)) continue;
            model.zeroDamage(stack);
            equipment.setItem(slot, stack);
            repaired++;
        }
        return repaired;
    }
}
