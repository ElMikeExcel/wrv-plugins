package com.winkreyes.exceleliteladder.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** One row of the Elite Seal Ladder: the six MobCombatSettings.yml values for a step. */
public record LadderStep(int step, double spawn, int cap, double normElite, double normPlayer,
                         double scaledElite, double scaledPlayer) {

    /** Key -> value, in file order (lines 6, 9, 91, 94, 109, 112). */
    public Map<String, Double> values() {
        Map<String, Double> m = new LinkedHashMap<>();
        m.put(MobCombatFile.SPAWN, spawn);
        m.put(MobCombatFile.CAP, (double) cap);
        m.put(MobCombatFile.NORM_ELITE, normElite);
        m.put(MobCombatFile.NORM_PLAYER, normPlayer);
        m.put(MobCombatFile.SCALED_ELITE, scaledElite);
        m.put(MobCombatFile.SCALED_PLAYER, scaledPlayer);
        return m;
    }

    public String label() {
        return "S" + step;
    }

    public String summary() {
        return "spawn " + MobCombatFile.format(MobCombatFile.SPAWN, spawn)
                + ", cap " + cap
                + ", toElite " + MobCombatFile.format(MobCombatFile.SCALED_ELITE, scaledElite)
                + ", toPlayer " + MobCombatFile.format(MobCombatFile.SCALED_PLAYER, scaledPlayer)
                + ", dungeon toElite/toPlayer " + MobCombatFile.format(MobCombatFile.NORM_ELITE, normElite)
                + "/" + MobCombatFile.format(MobCombatFile.NORM_PLAYER, normPlayer);
    }
}
