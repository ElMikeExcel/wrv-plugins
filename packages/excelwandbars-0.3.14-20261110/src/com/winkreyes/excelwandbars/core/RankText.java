package com.winkreyes.excelwandbars.core;

import java.util.ArrayList;
import java.util.List;

/** Rank-up by casting (2026-10-26): every player-facing line. Short (phones), no em dashes, never mentions seals. */
public final class RankText {
    private RankText() {}

    public static String num(long n) { return String.format(java.util.Locale.ROOT, "%,d", n); }

    /** "Student: 340/1,600 SP" or "Master: top rank". */
    public static String progress(String rankName, long sp, long need, boolean top) {
        if (top) return rankName + ": top rank";
        return rankName + ": " + num(Math.min(sp, need)) + "/" + num(need) + " SP";
    }

    /** Spell Book header lines (both editions). */
    public static List<String> header(String rankName, String nextName, long sp, long need, RankLadder.Step step, String bonusTag, String howText) {
        List<String> out = new ArrayList<>();
        boolean top = step == RankLadder.Step.TOP;
        out.add(progress(rankName, sp, need, top) + (top || nextName == null ? "" : " to " + nextName));
        if (step == RankLadder.Step.NOT_OPEN && nextName != null) out.add(nextName + " rank is not open yet.");
        else if (!top && howText != null && !howText.isBlank()) out.add(howText.trim());
        if (!top && bonusTag != null && !bonusTag.isBlank()) out.add("Bonus now: " + bonusTag);
        return out;
    }

    /** Action bar after a paid cast: "+2 SP (x1.2 weekend) | Student 340/1,600". */
    public static String earned(int gained, String bonusLabel, double mult, String rankName, long sp, long need, boolean top) {
        StringBuilder sb = new StringBuilder("+").append(gained).append(" SP");
        if (mult > 1.0001 && bonusLabel != null && !bonusLabel.isBlank()) sb.append(" (x").append(EarnMath.fmt(mult)).append(' ').append(bonusLabel).append(')');
        sb.append(" | ").append(top ? rankName + " (top rank)" : rankName + " " + num(Math.min(sp, need)) + "/" + num(need));
        return sb.toString();
    }

    public static String fill(String template, String rank, int spells) {
        if (template == null) return "";
        return template.replace("{rank}", rank == null ? "" : rank).replace("{spells}", String.valueOf(spells)).replace("\u2014", ",").trim();
    }
}

