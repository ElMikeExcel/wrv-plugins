package com.winkreyes.makersmark.core;

/** Which non-stackable gear gets a mark or counters, decided by material name. */
public final class Gear {
    private Gear() {}

    public enum Kind { NONE, TOOL, MELEE, RANGED, ARMOR, SHIELD, ELYTRA }

    public static Kind kind(String m) {
        if (m == null || m.startsWith("LEGACY_")) return Kind.NONE;
        if (m.endsWith("_SWORD") || m.endsWith("_SPEAR") || m.equals("MACE") || m.equals("TRIDENT")) return Kind.MELEE;
        if (m.endsWith("_AXE")) return Kind.MELEE; // axes are weapons and tools; they count kills
        if (m.equals("BOW") || m.equals("CROSSBOW")) return Kind.RANGED;
        if (m.endsWith("_HELMET") || m.endsWith("_CHESTPLATE") || m.endsWith("_LEGGINGS") || m.endsWith("_BOOTS")) return Kind.ARMOR;
        if (m.equals("SHIELD")) return Kind.SHIELD;
        if (m.equals("ELYTRA")) return Kind.ELYTRA;
        if (m.endsWith("_PICKAXE") || m.endsWith("_SHOVEL") || m.endsWith("_HOE") || m.equals("SHEARS")
                || m.equals("FLINT_AND_STEEL") || m.equals("FISHING_ROD") || m.equals("BRUSH")
                || m.equals("CARROT_ON_A_STICK") || m.equals("WARPED_FUNGUS_ON_A_STICK")) return Kind.TOOL;
        return Kind.NONE;
    }

    /** Signed gear: allowlisted kind AND a max stack size of 1. */
    public static boolean markable(String m, int maxStack) { return maxStack == 1 && kind(m) != Kind.NONE; }

    /** Counts kills (melee or ranged; tridents count both ways). */
    public static boolean weapon(String m) { Kind k = kind(m); return k == Kind.MELEE || k == Kind.RANGED; }

    /** Counts hits blocked while worn. Elytra is worn in the chest slot but is not armor. */
    public static boolean armor(String m) { return kind(m) == Kind.ARMOR; }
}
