package com.winkreyes.excelbosstrial.core;

/**
 * The four seal bosses ExcelBossTrial governs. The raid seal is
 * deliberately absent: Mike excluded it from the seal-integrity design
 * (Oct 18, 2026), and nothing in this plugin touches it.
 */
public enum BossKind {
    WITHER("wither", "The Wither"),
    ELDER_GUARDIAN("elder_guardian", "An Elder Guardian"),
    WARDEN("warden", "The Warden"),
    ENDER_DRAGON("ender_dragon", "The Ender Dragon");

    private final String sealId;
    private final String displayName;

    BossKind(String sealId, String displayName) {
        this.sealId = sealId;
        this.displayName = displayName;
    }

    /** The ExcelGuildGate seal id this boss feeds. */
    public String sealId() {
        return sealId;
    }

    public String displayName() {
        return displayName;
    }

    public static BossKind fromSealId(String sealId) {
        for (BossKind k : values()) {
            if (k.sealId.equals(sealId)) {
                return k;
            }
        }
        return null;
    }
}
