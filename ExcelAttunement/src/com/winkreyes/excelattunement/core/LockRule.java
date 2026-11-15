package com.winkreyes.excelattunement.core;

/**
 * 0.1.4 stance LOCK (Mike, Oct 24, 2026: "Is there no way to just lock
 * the weapon away?"). Pure decision, unit-tested; the Bukkit glue only
 * describes the item that is about to land in the main hand.
 *
 *  - CASTER stance + weapon-lock: a martial weapon (sword, axe, mace,
 *    trident, bow, crossbow) may not be moved INTO the main hand.
 *  - MARTIAL stance + wand-lock: a Magic wand may not be moved into
 *    the main hand.
 *  - Magic's own items (wands, spell icons of the action-bar casting
 *    mode) are never treated as martial weapons, whatever their
 *    material, so the Magic bar keeps working in caster stance.
 *  - The effect locks stay as the backstop: a weapon already in hand
 *    still deals 0 in caster stance, a wand already in hand is still
 *    blocked at PreCastEvent in martial stance.
 */
public final class LockRule {
    private LockRule() {}

    public enum Block { NONE, WEAPON, WAND }

    public static Block check(Stance stance, String materialName, boolean magicItem, boolean wand,
                              boolean weaponLock, boolean wandLock) {
        if (stance == Stance.CASTER && weaponLock && !magicItem
                && Weapons.isMartialItemName(materialName)) {
            return Block.WEAPON;
        }
        if (stance == Stance.MARTIAL && wandLock && wand) {
            return Block.WAND;
        }
        return Block.NONE;
    }
}
