package com.winkreyes.excelprotect;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect — economy family, money deltas (AuditLog parity): Vault balances are
 * polled by the flush timer; a change emits a `money` event tagged with the player's
 * most recent command inside the tagging window. Fail-soft: no Vault economy = off.
 * Also the charge path Lost & Found uses (service_charge events with balance_before).
 * Excel — Wink Reyes Valley
 */
public final class EconomyWatch implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private Economy eco;
    private final Map<UUID, Double> lastBalance = new HashMap<>();
    private final Map<UUID, Cmd> lastCmd = new HashMap<>();

    private record Cmd(String text, long ms) { }

    public EconomyWatch(Ledger ledger, Settings cfg) { this.ledger = ledger; this.cfg = cfg; }

    public boolean hook() {
        try {
            RegisteredServiceProvider<Economy> rsp =
                    Bukkit.getServicesManager().getRegistration(Economy.class);
            if (rsp == null || rsp.getProvider() == null) return false;
            eco = rsp.getProvider();
            return true;
        } catch (Throwable t) { return false; }
    }

    public boolean on() { return eco != null; }
    public double balance(Player p) { return eco == null ? 0 : eco.getBalance(p); }

    /** Charge via Vault (fail-soft: returns false when no economy or insufficient). */
    public boolean charge(Player p, double amount, String what) {
        if (eco == null) return false;
        double before = eco.getBalance(p);
        if (before < amount) return false;
        eco.withdrawPlayer(p, amount);
        if (cfg.ecoCharges())
            ledger.emit("economy", p.getLocation(), new Json.Obj().put("type", "service_charge")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                    .put("service", what).put("amount", amount).put("balance_before", before));
        lastBalance.put(p.getUniqueId(), eco.getBalance(p));
        return true;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCmd(PlayerCommandPreprocessEvent e) {
        lastCmd.put(e.getPlayer().getUniqueId(), new Cmd(e.getMessage(), System.currentTimeMillis()));
    }

    /** Called by the flush timer (async-safe reads; emits are queued). */
    public void poll() {
        if (eco == null || !cfg.ecoMoney()) return;
        for (Player p : Bukkit.getOnlinePlayers()) {
            double bal = eco.getBalance(p);
            Double prev = lastBalance.put(p.getUniqueId(), bal);
            if (prev == null || Math.abs(bal - prev) < 0.005) continue;
            Json.Obj o = new Json.Obj().put("type", "money")
                    .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                    .put("delta", CombatCore.round2(bal - prev))
                    .put("balance", CombatCore.round2(bal));
            Cmd c = lastCmd.get(p.getUniqueId());
            if (c != null && System.currentTimeMillis() - c.ms() <= 15_000) o.put("recent", c.text());
            ledger.emit("economy", p.getLocation(), o);
        }
    }
}
