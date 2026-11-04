// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * Live GuildGate reads for the ported screens, ported from
 * ExcelBedrockMenu 1.5.28: reflection against GuildGateAPI in the
 * GuildGate plugin's own classloader (no hard dependency). Every
 * failure reads fail-soft, exactly as the original: an unreadable
 * guild is "not locked", and an unreadable seal count falls back to
 * the lock state (5 once the guild stands open, 0 while sealed or
 * unknown), so a screen never claims seals that have not fallen.
 */
final class GuildGateReads {

    private GuildGateReads() {}

    /** Live count of broken guild seals, for the Skills & Perks
     *  Magic line ("N of 5 today"). */
    static int sealsBrokenCount() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) {
                return 0;
            }
            ClassLoader loader = guildGate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, loader);
            Object count = apiClass.getMethod("sealsBrokenCount").invoke(null);
            if (count instanceof Number) {
                return ((Number) count).intValue();
            }
            return 0;
        } catch (Exception e) {
            return isGuildLocked() ? 0 : 5;
        }
    }

    /** True while ExcelGuildGate is installed and reports the guild locked. */
    static boolean isGuildLocked() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) {
                return false;
            }
            ClassLoader loader = guildGate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, loader);
            Object unlocked = apiClass.getMethod("isUnlocked").invoke(null);
            return !Boolean.TRUE.equals(unlocked);
        } catch (Exception e) {
            return false;
        }
    }
}
