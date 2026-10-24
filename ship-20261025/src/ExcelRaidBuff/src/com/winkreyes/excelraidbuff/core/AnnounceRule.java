package com.winkreyes.excelraidbuff.core;

/**
 * The one player-facing line of this plugin (Mike's shape, Oct 20,
 * 2026 late): when a MEANINGFULLY buffed raid begins, the village
 * hears once, in player words and with no numbers, that these
 * raiders are hardened by the omen. Everything else about the
 * buff is silent — no nametags, no per-wave spam.
 */
public final class AnnounceRule {

    private AnnounceRule() {}

    public static boolean shouldAnnounce(boolean pluginEnabled, boolean announceEnabled,
                                         int omenLevel, int minOmenLevel, boolean alreadyAnnounced) {
        if (!pluginEnabled || !announceEnabled) return false;
        if (alreadyAnnounced) return false;
        return omenLevel >= Math.max(1, minOmenLevel) && omenLevel <= 5;
    }
}
