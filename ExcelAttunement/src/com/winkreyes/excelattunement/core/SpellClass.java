package com.winkreyes.excelattunement.core;

/**
 * How a Magic spell counts for attunement (from the spell's own
 * definition in the MagicGate stage configs — see spell-classes.txt
 * and tools/gen_spell_table.py).
 */
public enum SpellClass {
    /** Damage / debuff / fight-control spells. A cast always sets caster stance. */
    COMBAT,
    /** Heal / buff spells. A cast sets caster stance only while the caster is in combat. */
    SUPPORT,
    /** Everything else. A cast never sets any stance. */
    UTILITY
}
