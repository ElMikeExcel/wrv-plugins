package com.winkreyes.excelattunement.core;

/** The two attunement stances (Combat Attunement design, Oct 19, 2026). */
public enum Stance {
    NONE,
    /** Set by landing a hit with a martial weapon. Blocks wand casting. */
    MARTIAL,
    /** Set by a combat cast, or a support cast while in combat. Weapon damage is zeroed. */
    CASTER
}
