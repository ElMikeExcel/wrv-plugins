package com.winkreyes.excelcosmetics.core;

/** Humanized remaining time for the picker and status output.
 *  This is the ONLY place remaining time is ever shown — expiry
 *  itself is silent; nothing pushes a message at the player. */
public final class TimeText {

    private TimeText() {}

    public static String remaining(Grant grant, long now) {
        if (grant.isPermanent()) return "Owned forever";
        long left = grant.remainingMillis(now);
        if (left <= 0) return "Expired";
        long minutes = left / 60000L;
        long days = minutes / (24 * 60);
        long hours = (minutes % (24 * 60)) / 60;
        long mins = minutes % 60;
        if (days > 0) return days + "d " + hours + "h left";
        if (hours > 0) return hours + "h " + mins + "m left";
        if (mins > 0) return mins + "m left";
        return "under a minute left";
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
