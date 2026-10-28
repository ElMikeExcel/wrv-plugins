package com.winkreyes.excelbosstrial.core;

/**
 * The warden give-up rule (0.2.2, seal-hardening option 1B -
 * owner, Oct 22, 2026, from the analysis of PlayerB's
 * Oct 22 perch farm: 4 warden kills in 20 minutes, 261 hits,
 * EVERY one band-excluded, 0.0% counted, 0 damage taken).
 *
 * The credit rules already make a perch kill worthless - it
 * earns no seal. What they never policed is the LOOP: a
 * 1300-HP warden killed at zero risk every ~2 minutes, paying
 * vanilla loot and XP forever, a stable end-state that never
 * has to become an honest attempt.
 *
 * So: if a warden's ledger records ONLY excluded (band) or
 * unattributed damage for a sustained window, the fight never
 * came to the warden, and it burrows away - the encounter ends
 * with no death, no drops, no verdict, and no cooldown. The
 * clock runs from the LATEST fight evidence, where evidence is
 * either a dealt blow that COUNTED toward the share or a blow
 * the warden itself landed on a player: both prove a real
 * fight is happening at the warden's own level. Excluded and
 * unattributed damage (out-of-band arrows, crystals answering
 * to nobody, lava, suffocation) never restarts the clock -
 * they are exactly the non-fight this rule judges.
 *
 * Honest play is structurally untouched: a melee fighter lands
 * counted blows constantly; a kiter inside the bands counts;
 * a player being boomed back is in a fight the warden is
 * winning, not one it abandons. A window of 0 disables the
 * rule. WARDEN ONLY - the wither flies to its attackers and
 * the elders live behind their own geometry; neither carries
 * bands, so neither carries this rule.
 *
 * Pure predicate, Bukkit-free: the adapter supplies the
 * ledger's stamps.
 */
public final class WardenGiveUp {

    /** The settled window: 45 seconds of no honest fight. */
    public static final long DEFAULT_WINDOW_MS = 45_000L;

    private WardenGiveUp() {
    }

    /**
     * True when the warden gives up: real damage was dealt
     * (an encounter with zero dealt damage never triggers -
     * there is no non-fight to end), and no fight evidence has
     * landed for the full window.
     *
     * @param totalDamage  the ledger's total dealt damage
     * @param firstDealtMs when the first dealt damage landed (0 = none)
     * @param lastCountedMs when the last share-counted blow landed (0 = none)
     * @param lastTakenMs  when the boss last landed a blow on a player (0 = none)
     * @param nowMs        the current time
     * @param windowMs     the give-up window (0 or negative disables)
     */
    public static boolean due(double totalDamage, long firstDealtMs,
                              long lastCountedMs, long lastTakenMs,
                              long nowMs, long windowMs) {
        if (windowMs <= 0 || totalDamage <= 0 || firstDealtMs <= 0) {
            return false;
        }
        long clockStart = Math.max(firstDealtMs,
                Math.max(lastCountedMs, lastTakenMs));
        return nowMs - clockStart >= windowMs;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

