package com.winkreyes.exceltips.core;

import java.util.Locale;
import java.util.Set;

/** Small pure helpers: command labels and "ok" chat replies. */
public final class Words {
    private Words() {}

    /** "/Essentials:TPA bob" -> "tpa". Empty for blank input. */
    public static String label(String commandLine) {
        if (commandLine == null) return "";
        String s = commandLine.trim();
        while (s.startsWith("/")) s = s.substring(1);
        int sp = s.indexOf(' ');
        if (sp >= 0) s = s.substring(0, sp);
        int c = s.indexOf(':');
        if (c >= 0) s = s.substring(c + 1);
        return s.toLowerCase(Locale.ROOT);
    }

    private static final Set<String> OK = Set.of("ok", "okay", "k", "got it", "ok got it", "okay got it", "kk", "oki");

    /** Is this chat message a plain "ok" style reply? */
    public static boolean isOk(String msg) {
        if (msg == null) return false;
        String s = msg.trim().toLowerCase(Locale.ROOT).replaceAll("[!.,]+", " ").replaceAll("\\s+", " ").trim();
        return OK.contains(s);
    }

    /** "ExcelTrash>=0.2.0" -> {"ExcelTrash", "0.2.0"}; "AutoSort" -> {"AutoSort", ""}. */
    public static String[] pluginSpec(String spec) {
        if (spec == null) return new String[]{"", ""};
        String s = spec.trim();
        int i = s.indexOf(">=");
        return i < 0 ? new String[]{s, ""} : new String[]{s.substring(0, i).trim(), s.substring(i + 2).trim()};
    }

    /** "0.2.1-SNAPSHOT" >= "0.2.0"? Compares the leading numbers; blank want = any version. */
    public static boolean versionAtLeast(String have, String want) {
        if (want == null || want.isBlank()) return true;
        if (have == null) return false;
        int[] a = nums(have), b = nums(want);
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int x = i < a.length ? a[i] : 0, y = i < b.length ? b[i] : 0;
            if (x != y) return x > y;
        }
        return true;
    }

    /** Leading dotted numbers: "2.12.0-SNAPSHOT" -> [2, 12, 0], "1.6.0.1" -> [1, 6, 0, 1]. */
    private static int[] nums(String v) {
        java.util.List<Integer> out = new java.util.ArrayList<>();
        for (String q : v.trim().split("\\.")) {
            String d = q.replaceAll("^([0-9]*).*$", "$1");
            if (d.isEmpty()) break;
            out.add(Integer.parseInt(d.length() > 9 ? d.substring(0, 9) : d));
            if (!q.matches("[0-9]+")) break;
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    /** No em dashes or en dashes in player text (house rule). */
    public static boolean hasDash(String s) { return s != null && (s.indexOf('\u2014') >= 0 || s.indexOf('\u2013') >= 0); }
}
