package com.winkreyes.excelattunement.core;

/**
 * 0.1.4 player-facing stance text. Kept short so the action bar fits a
 * phone screen on Bedrock (longest line 33 characters).
 */
public final class StanceText {
    private StanceText() {}

    public static final String CLEAR = "Stance clear";

    /** "Caster stance: weapons locked 24s" / "Fighter stance: magic locked 18s". */
    public static String actionBar(Stance stance, long seconds) {
        if (stance == Stance.CASTER) return "Caster stance: weapons locked " + seconds + "s";
        if (stance == Stance.MARTIAL) return "Fighter stance: magic locked " + seconds + "s";
        return CLEAR;
    }

    public static String title(LockRule.Block block) {
        return block == LockRule.Block.WAND ? "Magic locked" : "Weapons locked";
    }

    public static String subtitle(Stance stance, long seconds) {
        if (stance == Stance.CASTER) return "Caster stance " + seconds + "s";
        if (stance == Stance.MARTIAL) return "Fighter stance " + seconds + "s";
        return "";
    }
}
