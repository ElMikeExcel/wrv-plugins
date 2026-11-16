/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

/**
 * Sonic boom pressure exemption (Item 39 R1, ported from the
 * retune test build onto the 0.5.2 line for 0.5.3, Nov 9,
 * 2026): the pure identification predicate for the Warden's
 * sonic boom on the boss-to-player pressure path.
 *
 * Why this rule exists: the pressure handler scales EVERY
 * boss-to-player hit by the chapter's outgoing scale (chapter
 * multiplier under the attrition ramp) with no cause filter, so
 * the boom - vanilla 15 HP, armor-ignoring - rode the ramp like
 * a melee hit. Under the Item 39 R2 candidate (multiplier 1.0,
 * ramp-max 1.5) the past-cap boom read 22.5 HP in the rig: a
 * standing one-shot through full health from minute ~8.3 onward
 * (TEST-ROUNDS.md round 1, step 3 FAIL on the boom axis). Mike's
 * baseline rule is no standing one-shot, beacon-free, so the
 * Warden chapter opts out of pressure for the boom alone via
 * {@code warden.pressure.apply-to-sonic-boom: false} and the boom
 * stays at its exact vanilla damage at every fight point, while
 * melee keeps scaling (past-cap melee base 67.5 under R2).
 * Round 2 of the retune rounds proved the pair on the test
 * build: boom exactly 15.0000 at minute 0, mid-ramp and past
 * cap; past-cap melee still exactly 67.5.
 *
 * Identification: SONIC_BOOM is Warden-only in vanilla, and the
 * handler only runs for a damager that resolved to a boss kind
 * with an open encounter ledger, so the pair (cause ==
 * SONIC_BOOM, kind == WARDEN) can only be the trial Warden's own
 * boom. The kind test is kept anyway (belt-and-braces, and it
 * keeps the exemption chapter-local like every other rule in the
 * handler): a future boss dealing a boom-shaped hit is not
 * silently exempted by a key set on the Warden chapter.
 *
 * Shipped default (0.5.3, Mike's approved filing of the retune):
 * the key defaults to FALSE - in the bundled config and in the
 * code fallback alike - so the retune is the behavior a config
 * that predates the key gets. (The R1 test build carried a
 * conservative default TRUE so its rig configs changed nothing
 * by accident; the shipped filing deliberately reverses that,
 * because 0.5.3 IS the retune.) Setting the key TRUE restores
 * the pre-0.5.3 pressure-scaled boom. For every chapter but the
 * Warden the key is inert: no other boss deals a sonic boom.
 *
 * The Bukkit adapter owns the handler; the decision lives here,
 * in the PressureMath / ReflectRule style.
 */
public final class SonicBoomRule {

    private SonicBoomRule() {
    }

    /**
     * True when a boss-to-player damage event is exempt from the
     * pressure scale under R1: the SONIC_BOOM cause, a Warden
     * damager, and the chapter's apply-to-sonic-boom key OFF.
     * Every other combination - any other cause, any other boss
     * kind, or the key on - is not exempt and scales exactly as
     * before.
     */
    public static boolean isExempt(boolean sonicBoomCause, BossKind damagerKind,
                                   boolean applyToSonicBoom) {
        return sonicBoomCause && damagerKind == BossKind.WARDEN && !applyToSonicBoom;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
