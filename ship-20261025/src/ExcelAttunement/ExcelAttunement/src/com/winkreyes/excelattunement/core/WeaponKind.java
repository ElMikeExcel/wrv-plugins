package com.winkreyes.excelattunement.core;

/** How a landed (or in-flight) hit was delivered. Only martial kinds attune. */
public enum WeaponKind {
    NONE(false),
    /** Sword / axe / mace in hand (melee). */
    MELEE(true),
    BOW(true),
    CROSSBOW(true),
    /** Trident, melee or thrown — same kind either way. */
    TRIDENT(true);

    private final boolean martial;

    WeaponKind(boolean martial) {
        this.martial = martial;
    }

    public boolean isMartial() {
        return martial;
    }
}
