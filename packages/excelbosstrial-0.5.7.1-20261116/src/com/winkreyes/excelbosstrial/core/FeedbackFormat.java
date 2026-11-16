/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial.core;

import java.util.Locale;

/**
 * The feel patch's text rules (ExcelBossTrial 0.3.6): how ledger
 * numbers become the strings a player reads - the boss bar title's
 * true pool numbers and the action bar's per-player damage line.
 *
 * Pure and pinned: grouping is US-locale BY CONSTANT (never the
 * server locale - the numbers must read the same on every host),
 * and every rounding happens HERE, at display time only, always
 * from the true ledger value. The display never accumulates its
 * own rounded state: two 16.5 crits move the title by exactly 33
 * because the title always renders (pool - true dealt), never
 * (last title - rounded hit).
 */
public final class FeedbackFormat {

    private FeedbackFormat() {
    }

    /**
     * The short chapter name a title carries. BossKind.displayName
     * carries articles ("An Elder Guardian") that read wrong
     * mid-title; these are the settled short forms.
     */
    public static String shortName(BossKind kind) {
        if (kind == null) {
            return "Boss";
        }
        return switch (kind) {
            case WITHER -> "Wither";
            case ELDER_GUARDIAN -> "Elder Guardian";
            case WARDEN -> "Warden";
            case ENDER_DRAGON -> "Ender Dragon";
        };
    }

    /**
     * An integer pool number with US thousands separators
     * ("8,431"). Rounds the true value half-up at display; NaN
     * and infinities degrade to 0 - a title never prints "NaN".
     */
    public static String grouped(double value) {
        return String.format(Locale.US, "%,d", Math.round(clean(value)));
    }

    /**
     * A one-decimal damage number ("4.0", "16.5", "23.8") - the
     * action bar's precision, chosen so fractional spell hits
     * visibly accumulate in the running total. NaN degrades to 0.
     */
    public static String oneDecimal(double value) {
        return String.format(Locale.US, "%.1f", clean(value));
    }

    private static double clean(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0 : value;
    }

    /**
     * Render a boss bar title from a format: {name} is the
     * chapter's short name, {remaining} and {total} the grouped
     * pool numbers. A null/blank format reads as the settled
     * default; unknown placeholders pass through literally (an
     * owner's typo stays visible, never an exception).
     */
    public static String title(String format, BossKind kind,
                               double remaining, double total) {
        String f = format == null || format.isBlank()
                ? FeedbackSettings.DEFAULT_TITLE_FORMAT : format;
        return f.replace("{name}", shortName(kind))
                .replace("{remaining}", grouped(remaining))
                .replace("{total}", grouped(total));
    }

    /**
     * Render an action-bar damage line from a format: {last} is
     * this hit's ledger damage, {total} the player's running
     * ledger total - both one-decimal. A null/blank format reads
     * as the settled default; unknown placeholders pass through.
     */
    public static String actionBar(String format, double lastHit, double playerTotal) {
        String f = format == null || format.isBlank()
                ? FeedbackSettings.DEFAULT_DAMAGE_TOTAL_FORMAT : format;
        return f.replace("{last}", oneDecimal(lastHit))
                .replace("{total}", oneDecimal(playerTotal));
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
