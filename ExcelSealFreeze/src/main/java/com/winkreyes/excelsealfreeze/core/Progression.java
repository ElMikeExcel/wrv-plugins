package com.winkreyes.excelsealfreeze.core;

/**
 * The EliteMobs progression column set clamped by ExcelSealFreeze.
 *
 * Source of truth: plugins/EliteMobs/data/player_data.db, table PlayerData
 * (verified against the G4 live pull, sha256 b9d251a0..., 31 columns, 11 rows).
 *
 * IN SCOPE (Mike's wipe/freeze set, Nov 14 2026): HighestLevelKilled, Score,
 * Kills, CurrencyV2, CurrencyCents and every SkillXP_* column.
 * NEVER TOUCHED: Deaths, GamblingDebt, GamblingDebtCents, all BLOB columns
 * (QuestStatus, lockouts, cooldowns), BackTeleportLocation and the player
 * preference flags - history and identity, not progression.
 */
public final class Progression {

    /** SkillXP columns in PlayerData schema order. */
    public static final String[] SKILL_COLUMNS = {
            "SkillXP_ARMOR", "SkillXP_SWORDS", "SkillXP_AXES", "SkillXP_BOWS",
            "SkillXP_CROSSBOWS", "SkillXP_TRIDENTS", "SkillXP_HOES", "SkillXP_MACES",
            "SkillXP_SPEARS", "SkillXP_STAVES", "SkillXP_WANDS"
    };

    /** Every clamped column, in the order used for SELECT / UPDATE building. */
    public static final String[] COLUMNS;

    static {
        COLUMNS = new String[5 + SKILL_COLUMNS.length];
        COLUMNS[0] = "HighestLevelKilled";
        COLUMNS[1] = "Score";
        COLUMNS[2] = "Kills";
        COLUMNS[3] = "CurrencyV2";
        COLUMNS[4] = "CurrencyCents";
        System.arraycopy(SKILL_COLUMNS, 0, COLUMNS, 5, SKILL_COLUMNS.length);
    }

    private Progression() {
    }

    /** One player's progression values (a PlayerData row, progression columns only). */
    public static final class ProgRow {
        public final String uuid;
        public final long highestLevelKilled;
        public final long score;
        public final long kills;
        public final double currencyV2;
        public final long currencyCents;
        /** Skill XP per SKILL_COLUMNS order. */
        public final long[] skillXp;

        public ProgRow(String uuid, long highestLevelKilled, long score, long kills,
                       double currencyV2, long currencyCents, long[] skillXp) {
            if (skillXp.length != SKILL_COLUMNS.length) {
                throw new IllegalArgumentException("skillXp must have " + SKILL_COLUMNS.length + " entries");
            }
            this.uuid = uuid;
            this.highestLevelKilled = highestLevelKilled;
            this.score = score;
            this.kills = kills;
            this.currencyV2 = currencyV2;
            this.currencyCents = currencyCents;
            this.skillXp = skillXp.clone();
        }

        /** The post-wipe era baseline for a player with no snapshot entry: all zeros. */
        public static ProgRow zeros(String uuid) {
            return new ProgRow(uuid, 0L, 0L, 0L, 0.0d, 0L, new long[SKILL_COLUMNS.length]);
        }

        /** Numeric value of a clamped column by name (CurrencyV2 returned as double). */
        public double valueOf(String column) {
            switch (column) {
                case "HighestLevelKilled": return highestLevelKilled;
                case "Score": return score;
                case "Kills": return kills;
                case "CurrencyV2": return currencyV2;
                case "CurrencyCents": return currencyCents;
                default:
                    for (int i = 0; i < SKILL_COLUMNS.length; i++) {
                        if (SKILL_COLUMNS[i].equals(column)) return skillXp[i];
                    }
                    throw new IllegalArgumentException("not a progression column: " + column);
            }
        }
    }
}
