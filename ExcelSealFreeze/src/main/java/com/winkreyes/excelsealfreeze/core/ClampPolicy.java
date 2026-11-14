package com.winkreyes.excelsealfreeze.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure clamp policy: a progression field is clamped only when its current
 * value EXCEEDS the baseline value, and it is clamped TO the baseline -
 * never below it, and a value under baseline (e.g. a Score gone negative
 * from deaths) is left exactly as it is. The enforcer can only ever remove
 * gains; it can never grant, reduce-below-baseline, or touch any column
 * outside Progression.COLUMNS.
 */
public final class ClampPolicy {

    /** One column assignment to apply: SET column = value for one player row. */
    public static final class Clamp {
        public final String column;
        /** Long value for integral columns; CurrencyV2 uses doubleValue. */
        public final long longValue;
        public final double doubleValue;
        public final boolean isDouble;

        private Clamp(String column, long value) {
            this.column = column;
            this.longValue = value;
            this.doubleValue = value;
            this.isDouble = false;
        }

        private Clamp(String column, double value) {
            this.column = column;
            this.longValue = Math.round(value);
            this.doubleValue = value;
            this.isDouble = true;
        }
    }

    private ClampPolicy() {
    }

    /**
     * Diff current against baseline. A null baseline means the player has no
     * snapshot entry (first joined during the freeze): the post-wipe era
     * baseline is all zeros.
     */
    public static List<Clamp> diff(Progression.ProgRow baseline, Progression.ProgRow current) {
        Progression.ProgRow base = baseline != null ? baseline : Progression.ProgRow.zeros(current.uuid);
        List<Clamp> out = new ArrayList<>();
        if (current.highestLevelKilled > base.highestLevelKilled) {
            out.add(new Clamp("HighestLevelKilled", base.highestLevelKilled));
        }
        if (current.score > base.score) {
            out.add(new Clamp("Score", base.score));
        }
        if (current.kills > base.kills) {
            out.add(new Clamp("Kills", base.kills));
        }
        if (current.currencyV2 > base.currencyV2) {
            out.add(new Clamp("CurrencyV2", base.currencyV2));
        }
        if (current.currencyCents > base.currencyCents) {
            out.add(new Clamp("CurrencyCents", base.currencyCents));
        }
        for (int i = 0; i < Progression.SKILL_COLUMNS.length; i++) {
            if (current.skillXp[i] > base.skillXp[i]) {
                out.add(new Clamp(Progression.SKILL_COLUMNS[i], base.skillXp[i]));
            }
        }
        return out;
    }

    /** Builds the parameterized UPDATE for one row's clamps; null when there is nothing to do. */
    public static String updateSql(List<Clamp> clamps) {
        if (clamps == null || clamps.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("UPDATE PlayerData SET ");
        for (int i = 0; i < clamps.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append('"').append(clamps.get(i).column).append("\" = ?");
        }
        sb.append(" WHERE PlayerUUID = ?");
        return sb.toString();
    }
}
