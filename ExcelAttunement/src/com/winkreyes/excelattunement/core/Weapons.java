package com.winkreyes.excelattunement.core;

/**
 * Which held-item names count as martial weapons (design: sword, axe,
 * mace, trident, bow, crossbow — shields and tools NEVER trigger).
 *
 * Pure name rules over Bukkit Material names, so the whole matrix is
 * unit-testable without a server:
 *   *_SWORD, *_AXE              -> MELEE
 *   MACE                        -> MELEE
 *   TRIDENT                     -> TRIDENT (melee hit or thrown)
 *   BOW / CROSSBOW              -> ranged kinds (for completeness)
 * Everything else is NONE — including *_PICKAXE, *_SHOVEL, *_HOE
 * (tools), SHIELD, sticks/wands, and bare hands (null/AIR).
 */
public final class Weapons {
    private Weapons() {}

    public static WeaponKind kindOfItemName(String materialName) {
        if (materialName == null) return WeaponKind.NONE;
        String n = materialName.trim().toUpperCase();
        if (n.equals("TRIDENT")) return WeaponKind.TRIDENT;
        if (n.equals("BOW")) return WeaponKind.BOW;
        if (n.equals("CROSSBOW")) return WeaponKind.CROSSBOW;
        if (n.equals("MACE")) return WeaponKind.MELEE;
        if (n.endsWith("_SWORD")) return WeaponKind.MELEE;
        if (n.endsWith("_AXE")) return WeaponKind.MELEE;
        return WeaponKind.NONE;
    }

    public static boolean isMartialItemName(String materialName) {
        return kindOfItemName(materialName).isMartial();
    }

    /**
     * Kind for a projectile launched by a player, from the projectile's
     * entity-type name and the item in the shooter's hand at launch.
     * Only arrows/bolts (AbstractArrow family) and thrown tridents are
     * martial projectiles; snowballs, eggs, potions and ender pearls
     * are not, whoever threw them.
     */
    public static WeaponKind kindOfProjectile(String entityTypeName, String handItemName) {
        if (entityTypeName == null) return WeaponKind.NONE;
        String t = entityTypeName.trim().toUpperCase();
        if (t.equals("TRIDENT")) return WeaponKind.TRIDENT;
        if (t.equals("ARROW") || t.equals("SPECTRAL_ARROW")) {
            WeaponKind hand = kindOfItemName(handItemName);
            if (hand == WeaponKind.CROSSBOW) return WeaponKind.CROSSBOW;
            // A player-launched arrow comes from a bow in any real play
            // path; if the hand somehow reads otherwise, the arrow is
            // still the player's ranged weapon hit.
            return WeaponKind.BOW;
        }
        return WeaponKind.NONE;
    }
}
