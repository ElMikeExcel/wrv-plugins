package com.winkreyes.excelbosstrial.dragon;

/**
 * How a damage event's damager relates to the battle dragon, resolved
 * by the Bukkit adapter so the core never touches Bukkit types:
 * CONTACT  - the dragon itself (melee / body / wing), including its
 *            ComplexEntityPart children;
 * FIREBALL - a DragonFireball whose shooter is the dragon;
 * BREATH   - an AreaEffectCloud whose source is the dragon;
 * OTHER    - anything else (never buffed).
 */
public enum DamagerKind {
    CONTACT,
    FIREBALL,
    BREATH,
    OTHER
}
