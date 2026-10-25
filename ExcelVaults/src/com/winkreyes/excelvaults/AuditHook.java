package com.winkreyes.excelvaults;

import java.lang.reflect.Method;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Writes a line into ExcelAuditLog's daily CSV (event "vault") when that plugin is running.
 * ExcelAuditLog 0.2.0 has no public API, so this calls its log(Player, String, String) by reflection.
 * Missing plugin or a changed method = silently skipped (ExcelVaults keeps its own purchases.csv either way).
 */
final class AuditHook {
    private Method log;
    private Plugin target;
    private boolean warned;

    boolean log(Player p, String details, java.util.logging.Logger logger) {
        try {
            Plugin al = Bukkit.getPluginManager().getPlugin("ExcelAuditLog");
            if (al == null || !al.isEnabled()) return false;
            if (log == null || target != al) {
                Method m = al.getClass().getDeclaredMethod("log", Player.class, String.class, String.class);
                m.setAccessible(true);
                log = m; target = al;
            }
            log.invoke(al, p, "vault", details);
            return true;
        } catch (Throwable t) {
            log = null;
            if (!warned) { warned = true; logger.warning("ExcelAuditLog hook failed (purchases still go to purchases.csv): " + t); }
            return false;
        }
    }

    boolean available() {
        Plugin al = Bukkit.getPluginManager().getPlugin("ExcelAuditLog");
        return al != null && al.isEnabled();
    }
}
