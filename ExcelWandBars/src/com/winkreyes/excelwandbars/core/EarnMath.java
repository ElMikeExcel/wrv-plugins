package com.winkreyes.excelwandbars.core;

/**
 * Rank-up by casting (2026-10-26): how many SP one successful cast earns.
 * Same throttle as Magic's own path earning (BaseSpell: earns scaled by time since the
 * last paid cast of that spell over earns_cooldown), then the server's XP bonuses
 * (capped), then a daily soft cap so long grinding sessions slow down.
 */
public final class EarnMath {
    private EarnMath() {}

    /** Magic's formula: floor(earns * min(1, elapsed / cooldown)). cooldown 0 = always full. */
    public static int throttled(int earns, long elapsedMs, long cooldownMs) {
        if (earns <= 0) return 0;
        if (cooldownMs <= 0 || elapsedMs >= cooldownMs) return earns;
        if (elapsedMs <= 0) return 0;
        return (int) Math.floor(earns * (double) elapsedMs / (double) cooldownMs);
    }

    /** The bonus that applies right now. */
    public static final class Bonus {
        public final double multiplier;
        public final String label; // "" when no bonus, else e.g. "weekend + group"
        Bonus(double m, String l) { multiplier = m; label = l; }
        public boolean active() { return multiplier > 1.0001; }
        /** "SP x1.25 (group)" or "" */
        public String tag() { return active() ? "SP x" + fmt(multiplier) + " (" + label + ")" : ""; }
    }

    /** Weekend and group bonuses multiply, then the total is capped (cap <= 1 = no bonus at all). */
    public static Bonus bonus(double weekend, double group, double cap) {
        double w = weekend > 1.0001 ? weekend : 1.0;
        double g = group > 1.0001 ? group : 1.0;
        double m = w * g;
        if (cap > 0 && m > cap) m = Math.max(1.0, cap);
        String l = "";
        if (w > 1.0001 && g > 1.0001) l = "weekend + group";
        else if (w > 1.0001) l = "weekend";
        else if (g > 1.0001) l = "group";
        if (m <= 1.0001) { m = 1.0; l = ""; }
        return new Bonus(Math.round(m * 1000) / 1000.0, l);
    }

    /**
     * Daily soft cap. Up to fullPerDay SP a day earns at full rate, then at slowRate
     * until maxPerDay, then nothing. 0 = that limit is off. Returns the SP (fractional)
     * this award may add given what was already earned today.
     */
    public static double daily(double award, double earnedToday, double fullPerDay, double maxPerDay, double slowRate) {
        if (award <= 0) return 0;
        double out = 0, left = award, at = earnedToday;
        if (fullPerDay > 0 && at < fullPerDay) {
            double take = Math.min(left, fullPerDay - at);
            out += take; left -= take; at += take;
        } else if (fullPerDay <= 0) {
            out = award; left = 0; at += award;
        }
        if (left > 0) {
            double slowAward = left * Math.max(0, slowRate);
            if (maxPerDay > 0) slowAward = Math.min(slowAward, Math.max(0, maxPerDay - at));
            out += slowAward;
        }
        if (maxPerDay > 0 && earnedToday + out > maxPerDay) out = Math.max(0, maxPerDay - earnedToday);
        return out;
    }

    public static String fmt(double m) {
        String s = String.format(java.util.Locale.ROOT, "%.2f", m);
        while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return s;
    }
}

