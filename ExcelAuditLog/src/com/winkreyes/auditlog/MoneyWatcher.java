package com.winkreyes.auditlog;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/** Polls Vault balances of online players and logs deltas with the player's recent command as context. */
final class MoneyWatcher {
    private final Economy eco;
    private final Map<UUID, Double> last = new ConcurrentHashMap<>();

    private MoneyWatcher(Economy eco) { this.eco = eco; }

    static MoneyWatcher create() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        return rsp == null ? null : new MoneyWatcher(rsp.getProvider());
    }

    String name() { return eco.getName(); }

    /** Main thread: read balances. Returns nothing; logs via plugin. */
    void poll(ExcelAuditLog p) {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            double now;
            try { now = eco.getBalance(pl); } catch (Throwable t) { continue; }
            Double before = last.put(pl.getUniqueId(), now);
            if (before == null) continue;
            double d = now - before;
            if (Math.abs(d) < 0.005) continue;
            p.log(pl, "money", String.format(java.util.Locale.US, "%s%.2f balance=%.2f recent=%s",
                    d > 0 ? "+" : "", d, now, p.recentContext(pl.getUniqueId())));
        }
    }

    void seed(Player pl) { try { last.put(pl.getUniqueId(), eco.getBalance(pl)); } catch (Throwable ignored) {} }

    void forget(Player pl, ExcelAuditLog p) { poll1(pl, p); last.remove(pl.getUniqueId()); }

    private void poll1(Player pl, ExcelAuditLog p) {
        Double before = last.get(pl.getUniqueId());
        if (before == null) return;
        double now; try { now = eco.getBalance(pl); } catch (Throwable t) { return; }
        double d = now - before;
        if (Math.abs(d) >= 0.005) p.log(pl, "money", String.format(java.util.Locale.US, "%s%.2f balance=%.2f recent=%s",
                d > 0 ? "+" : "", d, now, p.recentContext(pl.getUniqueId())));
    }
}
