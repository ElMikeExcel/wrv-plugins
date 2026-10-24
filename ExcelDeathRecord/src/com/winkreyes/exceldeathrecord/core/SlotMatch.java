package com.winkreyes.exceldeathrecord.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Where each dropped item sat at death. The drop list ("drop-0",
 * ...) carries no slot, but the inventory snapshot does, and the
 * bytes are identical for the same stack. Each drop is matched to
 * the first unused inventory entry with the same bytes; "" when no
 * match (e.g. a plugin added the drop). Equipment slots are what
 * a same-player restore puts back on.
 */
public final class SlotMatch {
    public static final List<String> EQUIP = List.of("helmet", "chestplate", "leggings", "boots", "offhand");

    private SlotMatch() {}

    public static List<String> originalSlots(List<DeathRecord.ItemEntry> drops, List<DeathRecord.ItemEntry> inventory) {
        boolean[] used = new boolean[inventory.size()];
        List<String> out = new ArrayList<>();
        for (DeathRecord.ItemEntry d : drops) {
            String slot = "";
            for (int i = 0; i < inventory.size(); i++) {
                DeathRecord.ItemEntry inv = inventory.get(i);
                if (!used[i] && !d.bytesB64.isEmpty() && d.bytesB64.equals(inv.bytesB64)) {
                    used[i] = true; slot = inv.slot; break;
                }
            }
            out.add(slot);
        }
        return out;
    }

    public static boolean isEquip(String slot) { return EQUIP.contains(slot); }
}
