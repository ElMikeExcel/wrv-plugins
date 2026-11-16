package com.winkreyes.exceldowned;

/**
 * Pure policy for the 0.1.3 fix chain (P-DOWNED-REVIVE-1, board run 364).
 * No Bukkit: every rule the listeners apply is decided here so the
 * regression tests exercise the production logic, not a copy of it.
 *
 *  - Defect 3 (totem first): a lethal hit on a standing player who holds
 *    a totem of undying in the main OR off hand does NOT enter DOWNED —
 *    the event is left untouched so vanilla consumes the totem. A totem
 *    in the inventory (not in a hand) does not count (vanilla parity).
 *    Void lethal hits never enter DOWNED and never consult the totem.
 *  - Defect 1 (window rule, evidence verdict run 363): damage taken
 *    while downed eats ZERO bleed-out window by default. The Nov 14
 *    incident session closed at 246.84 s elapsed + 54.00 s eaten =
 *    300.84 s, the largest consumer being friendly/rescue-side damage
 *    (Jester_Turtle, 41 rows / 34.53 s). The bleed-out is a pure
 *    wall-clock window unless an operator explicitly re-enables eating
 *    in config (damage-eats-window: true) — the old scaled mechanism
 *    (DownedSession.eatDamage) is retained for that opt-in only.
 *  - Defect 2 (Resurrection): the revive fallback resolves only inside
 *    the one aligned revive range and only in the caster's world, and a
 *    spell grant is only "verified" when the spell template actually
 *    resolves — wand-data presence alone is not verification (the
 *    Nov 14 grouprenewal/resurrection NPE was reported "verified"
 *    while Magic could not construct the spell).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class FixPolicy {

    private FixPolicy() {}

    /** True when a totem of undying is held in either hand. */
    public static boolean holdsTotem(boolean mainHandTotem, boolean offHandTotem) {
        return mainHandTotem || offHandTotem;
    }

    /**
     * True when a standing player's hit enters DOWNED: the hit must be
     * lethal, non-void, not already cancelled, and the player must hold
     * no totem in either hand (totem first — Defect 3 ruling).
     */
    public static boolean entersDowned(boolean lethal, boolean voidCause,
                                       boolean alreadyCancelled, boolean hasTotem) {
        return lethal && !voidCause && !alreadyCancelled && !hasTotem;
    }

    /**
     * Bleed-out millis eaten by a damage-while-downed event. Zero unless
     * the operator opt-in (damage-eats-window) is on; then the legacy
     * scaled mechanism applies.
     */
    public static long windowEatenMillis(boolean damageEatsWindow, double scaledDamage,
                                          double secondsPerPoint) {
        if (!damageEatsWindow) return 0;
        return DownedMath.windowMillisEaten(scaledDamage, secondsPerPoint);
    }

    /** True when a revive target is in the caster's world and in range. */
    public static boolean withinReviveRange(boolean sameWorld, double distanceSquared,
                                            double range) {
        return sameWorld && distanceSquared <= range * range;
    }

    /**
     * A grant is verified only when BOTH the wand data carries the key
     * AND Magic's template for the key resolves. Presence alone (the
     * pre-fix fallback's test) is not verification.
     */
    public static boolean grantVerified(boolean wandDataPresent, boolean templateResolves) {
        return wandDataPresent && templateResolves;
    }
}
