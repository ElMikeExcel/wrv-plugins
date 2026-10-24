package com.winkreyes.exceldeathrecord.core;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * restores.csv (plugin folder), one row per restored item (and one
 * "xp" row when XP is given back):
 *
 *   time_utc,staff,source_player,death_file,item,target,item_no,count,source,forced
 *
 * time_utc is ISO-8601 with Z. death_file is relative to the
 * plugin folder (deaths/<uuid>/<id>.yml or burned/<owner>/<id>.yml).
 * Fields are RFC-4180 quoted when needed, and a leading = + - @
 * is prefixed with ' so a spreadsheet never runs it as a formula.
 */
public final class RestoreCsv {
    public static final String HEADER = "time_utc,staff,source_player,death_file,item,target,item_no,count,source,forced";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).withZone(ZoneOffset.UTC);
    private static final DateTimeFormatter UTC_TEXT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.ROOT).withZone(ZoneOffset.UTC);

    private RestoreCsv() {}

    public static String timeUtc(long epochMillis) { return TS.format(Instant.ofEpochMilli(epochMillis)); }
    public static String markTime(long epochMillis) { return UTC_TEXT.format(Instant.ofEpochMilli(epochMillis)); }

    public static String row(long epochMillis, String staff, String sourcePlayer, String file, String item,
                             String target, String itemNo, int count, String source, boolean forced) {
        return String.join(",", timeUtc(epochMillis), f(staff), f(sourcePlayer), f(file), f(item), f(target),
                f(itemNo), Integer.toString(count), f(source), Boolean.toString(forced));
    }

    static String f(String s) {
        if (s == null) s = "";
        if (!s.isEmpty() && "=+-@".indexOf(s.charAt(0)) >= 0) s = "'" + s;
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
