package excelemfixes.meleepremium;

/**
 * The melee-premium hit classifier as a pure decision (unit-
 * testable): a hit earns the premium only when it is a DIRECT,
 * physical, hand-delivered attack by a player — the one delivery
 * that pays the melee-range risk the premium compensates.
 *
 * Excluded, deliberately:
 *  - PROJECTILE cause / projectile damager: bows, crossbows,
 *    trident throws, wand/staff bolts — ranged and magic
 *    deliveries kite for free and get no premium.
 *  - ENTITY_SWEEP_ATTACK: the sweep is a secondary area echo of
 *    a sword swing, not the single-target hit the ruling prices;
 *    the main target's ENTITY_ATTACK carries the premium.
 *  - Magic weapons (wand/staff in hand) and EM magic hits: a
 *    wand swung as a stick is still EliteMobs magic delivery,
 *    classified as magic by EliteMobs' own weapon identity —
 *    the premium is for melee weapons, not for magic taking a
 *    step closer.
 *  - Custom damage: synthetic damage calls (other plugins'
 *    reroutes, ability echoes) are not a player swinging a
 *    weapon; premium-ing them would compound other systems'
 *    numbers.
 *  - Ranged attacks flagged by EliteMobs itself.
 */
public final class MeleeClassifier {

    private MeleeClassifier() {}

    /** How the damage reached the target. */
    public enum DamagerKind { DIRECT_PLAYER, PROJECTILE, OTHER }

    public static boolean isPremiumHit(String causeName,
                                       DamagerKind damagerKind,
                                       boolean rangedAttack,
                                       boolean magicWeapon,
                                       boolean magicHit,
                                       boolean customDamage) {
        if (!"ENTITY_ATTACK".equals(causeName)) return false;
        if (damagerKind != DamagerKind.DIRECT_PLAYER) return false;
        if (rangedAttack) return false;
        if (magicWeapon) return false;
        if (magicHit) return false;
        if (customDamage) return false;
        return true;
    }
}
