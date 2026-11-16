package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Public facade for BossTrial / Protect consumption (DESIGN §5, §7):
 * isDowned polling for the party-wipe rule, direct revive, session read.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class ExcelDownedAPI {

    private ExcelDownedAPI() {}

    private static ExcelDowned plugin() {
        return (ExcelDowned) Bukkit.getPluginManager().getPlugin("ExcelDowned");
    }

    public static boolean isDowned(UUID playerId) {
        ExcelDowned p = plugin();
        return p != null && p.isDowned(playerId);
    }

    public static boolean isDowned(Player player) {
        return player != null && isDowned(player.getUniqueId());
    }

    /** Remaining bleed-out millis, or -1 when the player is not downed. */
    public static long bleedOutRemainingMs(UUID playerId) {
        ExcelDowned p = plugin();
        if (p == null) return -1;
        DownedSession s = p.sessionOf(playerId);
        return s == null ? -1 : s.remainingMs(System.currentTimeMillis());
    }

    /** Revive via the same exactly-once path the spell uses. */
    public static boolean revive(Player healer, Player target) {
        ExcelDowned p = plugin();
        return p != null && p.revive(healer, target);
    }
}
