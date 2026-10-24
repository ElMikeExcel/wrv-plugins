package com.winkreyes.exceldeathrecord.core;

/**
 * 0.3.0: lostfound.csv (plugin folder), one row per Lost &amp; Found
 * attempt that reached the money step. restores.csv keeps its 0.2.0
 * header (staff = SELF for these rows); the money lives here:
 *
 *   time_utc,player,item,burn_file,price,charged,balance_before,result
 *
 * result: ok | refunded-mark-failed | refunded-give-failed. Same
 * quoting and formula guard as restores.csv.
 */
public final class LostFoundCsv {
    public static final String HEADER = "time_utc,player,item,burn_file,price,charged,balance_before,result";

    private LostFoundCsv() {}

    public static String row(long epochMillis, String player, String item, String file, String priceText,
                             double charged, double balanceBefore, String result) {
        return String.join(",", RestoreCsv.timeUtc(epochMillis), RestoreCsv.f(player), RestoreCsv.f(item),
                RestoreCsv.f(file), RestoreCsv.f(priceText), String.format(java.util.Locale.ROOT, "%.2f", charged),
                String.format(java.util.Locale.ROOT, "%.2f", balanceBefore), RestoreCsv.f(result));
    }
}
