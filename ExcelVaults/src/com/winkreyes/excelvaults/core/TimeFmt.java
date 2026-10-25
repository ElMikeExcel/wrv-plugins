package com.winkreyes.excelvaults.core;

/** Time left as "Xh Ym" (minutes rounded up so a player never sees "0m" while it is still locked). */
public final class TimeFmt {
    private TimeFmt() {}
    public static String hm(long ms) {
        if (ms <= 0) return "0m";
        long min = (ms + 59_999) / 60_000;
        long h = min / 60, m = min % 60;
        return h == 0 ? m + "m" : h + "h " + m + "m";
    }
}
