package com.winkreyes.excelbosstrial.core;

/**
 * Which denial broadcast a refused kill gets. Mike's SEAL
 * FEEDBACK DOCTRINE (Oct 19, 2026): silent during the fight - no
 * per-hit warnings, no telegraphing - but EVERY failed seal
 * clearly explains afterward WHY it was not broken, in player
 * words, naming the specific reason. "Not giving them feedback
 * after a fight is just brutal."
 *
 * Every denial already names its reason (share names the counted
 * percent against the need, presence names the empty ground,
 * cooldown names the time left) EXCEPT the banded ones: a warden
 * kill denied on share after height-band exclusions reads, under
 * the generic share text, like the game miscounted - the tower
 * sniper KNOWS his arrows landed. So a warden DENY_SHARE whose
 * encounter recorded height-excluded hits gets the height
 * variant, which names the level rule itself.
 *
 * 0.2.1 adds two more named reasons, on the same principle: the
 * warden RANGE variant (the horizontal shooting gallery - same
 * miscount complaint, rotated 90 degrees) and the wither CAGE
 * variant (a pocket-melee cage kill can pass the raw share
 * percent, so the generic text would be a lie twice over: the
 * percent was fine and the seal still refused). Pure selection
 * logic, Bukkit-free; the adapter only maps slots to text.
 */
public final class DenialMessage {

    /** The denial broadcast slots. */
    public enum Slot {
        /** Not a denial (a credit, or no verdict): no denial text. */
        NONE,
        /** Generic share denial: names the counted % vs the need. */
        SHARE,
        /** Warden height denial: names the level rule specifically. */
        SHARE_HEIGHT,
        /** Warden range denial (0.2.1): names the reach rule. */
        SHARE_RANGE,
        /** Wither cage denial (0.2.1): it never got to fight. */
        SHARE_CAGE,
        /** Presence denial: names the empty ground at the fall. */
        PRESENCE,
        /** Cooldown denial: names the time left. */
        COOLDOWN
    }

    private DenialMessage() {
    }

    /**
     * The slot for one death's facts (pre-0.2.1 shape: height
     * exclusions only). Delegates with no range exclusions and
     * no cage fact.
     */
    public static Slot select(BossKind kind, CreditJudge.Verdict verdict,
                              int heightExcludedHits) {
        return select(kind, verdict, heightExcludedHits, 0, false);
    }

    /**
     * The slot for one death's facts. The band variants require
     * the WARDEN (the bands are warden-only) and a share denial;
     * when both bands failed the height text wins (it was
     * settled first, and either text names a true reason). The
     * cage variant requires the WITHER, a share denial, and the
     * fought-free facts. Any other boss - or a share denial with
     * no exclusions and no cage - keeps the generic share text.
     */
    public static Slot select(BossKind kind, CreditJudge.Verdict verdict,
                              int heightExcludedHits, int rangeExcludedHits,
                              boolean cagedWither) {
        if (verdict == null) {
            return Slot.NONE;
        }
        return switch (verdict) {
            case CREDIT -> Slot.NONE;
            case DENY_SHARE -> shareSlot(kind, heightExcludedHits,
                    rangeExcludedHits, cagedWither);
            case DENY_PRESENCE -> Slot.PRESENCE;
            case DENY_COOLDOWN -> Slot.COOLDOWN;
        };
    }

    private static Slot shareSlot(BossKind kind, int heightExcludedHits,
                                  int rangeExcludedHits, boolean cagedWither) {
        if (kind == BossKind.WITHER && cagedWither) {
            return Slot.SHARE_CAGE;
        }
        if (kind == BossKind.WARDEN && heightExcludedHits > 0) {
            return Slot.SHARE_HEIGHT;
        }
        if (kind == BossKind.WARDEN && rangeExcludedHits > 0) {
            return Slot.SHARE_RANGE;
        }
        return Slot.SHARE;
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.

