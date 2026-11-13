package com.winkreyes.excelskillspanel.provider.wrv;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

/**
 * The guild-seal count for the Magic header's "Guild seals: N of
 * 5" line — the proven GuildGate reflective-read pattern
 * (ExcelMenu's GuildGateReads, live since the Skills &amp; Perks
 * screen): reflection against GuildGateAPI in the GuildGate
 * plugin's own classloader, no hard dependency.
 *
 * Panel adaptation: where the menu screen fails soft to a
 * lock-state-derived number, the panel must never print a seal
 * count it did not actually read — so ANY failure here returns
 * null and the header omits the line entirely (DESIGN §7.3: no
 * faked header fields).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class GuildSealsReads {
    private GuildSealsReads() {}

    /** Broken-seal count as actually read from GuildGateAPI, or
     *  null when GuildGate is absent or the read fails. */
    public static Integer sealsBrokenCount() {
        try {
            Plugin guildGate = Bukkit.getPluginManager().getPlugin("ExcelGuildGate");
            if (guildGate == null || !guildGate.isEnabled()) return null;
            ClassLoader loader = guildGate.getClass().getClassLoader();
            Class<?> apiClass = Class.forName("com.winkreyes.guildgate.api.GuildGateAPI",
                    true, loader);
            Object count = apiClass.getMethod("sealsBrokenCount").invoke(null);
            return count instanceof Number ? ((Number) count).intValue() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
