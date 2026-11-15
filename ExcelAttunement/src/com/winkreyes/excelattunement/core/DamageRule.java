package com.winkreyes.excelattunement.core;

/**
 * The two damage decisions, kept pure so unit tests pin them and the
 * Bukkit layer is a thin adapter:
 *
 *  ZEROING — while the attacker's CASTER stance is active, damage
 *  from their martial weapons (melee OR projectile, judged at landing)
 *  is set to 0. The event is NOT cancelled: the hit still registers
 *  (knockback lands, and downstream ledgers — ExcelBossTrial caps,
 *  ExcelTelemetry — see a real hit that dealt 0).
 *
 *  TRIGGER — a martial hit sets/refreshes martial stance only when it
 *  actually landed: event not cancelled and final damage > 0. This is
 *  the no-flip rule: a caster's weapon hits are zeroed first, so their
 *  final damage is 0 and they can never flip caster -> martial by
 *  swinging. AttunementState.onMartialHit additionally refuses while
 *  caster is active (defense in depth).
 */
public final class DamageRule {
    private DamageRule() {}

    public static boolean shouldZero(Stance attackerStance, WeaponKind kind) {
        return attackerStance == Stance.CASTER && kind != null && kind.isMartial();
    }

    public static boolean isMartialTrigger(WeaponKind kind, boolean cancelled, double finalDamage) {
        return kind != null && kind.isMartial() && !cancelled && finalDamage > 0.0;
    }
}
