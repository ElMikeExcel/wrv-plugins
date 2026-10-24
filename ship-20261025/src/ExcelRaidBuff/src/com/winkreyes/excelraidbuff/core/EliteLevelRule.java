package com.winkreyes.excelraidbuff.core;

/**
 * The level a converted raid elite gets. The anchor is EliteMobs'
 * own ruler: the level EM would roll for a natural elite spawning
 * at that spot in front of those players (its public
 * getNaturalMobLevel, which prices the nearby players' gear and
 * skill). The raid context then adds the omen's flat bonus from
 * config — at Bad Omen V the same gear faces a somewhat higher
 * class of elite — and the result is hard-clamped to the natural
 * elite level cap EM itself is configured with, read live from
 * EM's config API (the config fallback only covers that read
 * failing). A converted raider is therefore never a class of
 * elite the server's own natural spawns couldn't produce.
 */
public final class EliteLevelRule {

    private EliteLevelRule() {}

    /**
     * @param naturalLevel EM's natural-spawn level for this spot
     *                     and these players (values below 1 degrade
     *                     to 1 before the bonus)
     * @param omenBonus    the config bonus for the raid's omen level
     * @param cap          the natural elite level cap (EM's, live-read)
     */
    public static int levelFor(int naturalLevel, int omenBonus, int cap) {
        int safeCap = Math.max(1, cap);
        int base = Math.max(1, naturalLevel);
        long lvl = (long) base + Math.max(0, omenBonus);
        return (int) Math.min(lvl, safeCap);
    }
}
