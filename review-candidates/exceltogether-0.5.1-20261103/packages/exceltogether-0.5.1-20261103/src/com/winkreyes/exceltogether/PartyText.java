package com.winkreyes.exceltogether;

/** Central wording for the party bonus note (0.4.x), so every surface says it the same way. */
public final class PartyText {
    public static final String NOT_IN_PARTY = "You are not in a party. Party up to earn more!";

    private PartyText() { }

    public static String bonusNote(int pct, int radius, int sharePct, boolean mcmmoXp) {
        String s = "+" + pct + "% Jobs money and XP when a party member is within " + radius + " blocks.";
        if (sharePct > 0) {
            s += " Nearby party members also share " + sharePct + "% of each other's Jobs gains" + (mcmmoXp ? " and mcMMO skill XP." : ".");
        }
        return s;
    }
}
