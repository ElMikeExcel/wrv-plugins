package com.winkreyes.excelprotect;

import com.winkreyes.excelprotect.api.EconomyEvent;
import com.winkreyes.excelprotect.api.FightTableEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.Map;

/**
 * ExcelProtect — emitter adapters (SPEC §4): the BossTrial fight-table publish
 * (combat + seal verdict) and the economy emitter contract. mcMMO lives in
 * McmmoAdapter so its classes never load when mcMMO is absent.
 * Excel — Wink Reyes Valley
 */
public final class Adapters implements Listener {
    private final Ledger ledger;
    private final Settings cfg;

    public Adapters(Plugin plugin, Ledger ledger, Settings cfg) {
        this.ledger = ledger; this.cfg = cfg;
    }

    // ---------- BossTrial fight table ----------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onFightTable(FightTableEvent e) {
        Json.Arr rows = new Json.Arr();
        double dealt = 0;
        for (Map<String, Object> r : e.rows()) {
            Json.Obj o = new Json.Obj();
            r.forEach((k, v) -> { if (v != null) o.put(k, v); });
            rows.add(o);
            if (r.get("damage_dealt") instanceof Number n) dealt += n.doubleValue();
        }
        if (cfg.combatFights())
            ledger.emit("combat", null, new Json.Obj().put("type", "fight_summary")
                    .put("encounter", e.encounter()).put("source", "bosstrial")
                    .put("verdict", e.verdict()).put("credit", e.credit())
                    .put("pool", e.pool()).put("duration_s", e.durationSeconds())
                    .put("total_damage", CombatCore.round2(dealt)).put("players", rows));
        ledger.emit("audit", null, new Json.Obj().put("type", "seal_verdict")
                .put("seal", e.encounter()).put("verdict", e.verdict()).put("credit", e.credit())
                .put("pool", e.pool()).put("duration_s", e.durationSeconds()).put("players", rows));
    }

    // ---------- publish API door ----------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onRecord(com.winkreyes.excelprotect.api.RecordEvent e) {
        if (!cfg.publish()) return;
        String stream = e.stream();
        if (stream == null || !Exporter.STREAMS.contains(stream)) return;
        Json.Obj o = new Json.Obj();
        e.fields().forEach((k, v) -> { if (v != null) o.put(k, v); });
        if (Json.strField(o.render(), "type") == null) o.put("type", "published");
        ledger.emit(stream, null, o);
    }

    // ---------- villager trades (economy completion) ----------
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPurchase(io.papermc.paper.event.player.PlayerPurchaseEvent e) {
        if (!cfg.ecoShop()) return;
        org.bukkit.inventory.MerchantRecipe trade = e.getTrade();
        ItemFact f = ItemFact.of(trade.getResult(), false);
        Json.Obj o = new Json.Obj().put("type", "villager_trade")
                .put("player", e.getPlayer().getName())
                .put("player_uuid", e.getPlayer().getUniqueId().toString());
        if (f != null) o.put("item", f.toJson());
        if (!trade.getIngredients().isEmpty()) {
            ItemFact price = ItemFact.of(trade.getIngredients().get(0), false);
            if (price != null) o.put("price", price.toJson());
        }
        ledger.emit("economy", e.getPlayer().getLocation(), o);
    }

    /** Fail-soft Jobs adapter: JobsPaymentEvent by name (SPEC A3.8). */
    public void hookJobs(org.bukkit.plugin.Plugin plugin) {
        org.bukkit.plugin.Plugin jobs = org.bukkit.Bukkit.getPluginManager().getPlugin("Jobs");
        if (jobs == null || !jobs.isEnabled()) return;
        try {
            @SuppressWarnings("unchecked")
            Class<? extends org.bukkit.event.Event> cls = (Class<? extends org.bukkit.event.Event>)
                    Class.forName("com.gamingmesh.jobs.api.JobsPaymentEvent");
            org.bukkit.Bukkit.getPluginManager().registerEvent(cls, this, EventPriority.MONITOR,
                    (org.bukkit.plugin.EventExecutor) (l, ev) -> onJobsPayment(ev), plugin, true);
            plugin.getLogger().info("[ExcelProtect] economy: Jobs payment feed hooked");
        } catch (Throwable t) {
            plugin.getLogger().info("[ExcelProtect] economy: Jobs payment feed unavailable (fail-soft)");
        }
    }

    private void onJobsPayment(org.bukkit.event.Event ev) {
        try {
            Object player = ev.getClass().getMethod("getPlayer").invoke(ev);
            Object amount = ev.getClass().getMethod("getAmount").invoke(ev);
            Json.Obj o = new Json.Obj().put("type", "jobs_payout");
            if (player instanceof org.bukkit.OfflinePlayer op) {
                o.put("player", op.getName());
                if (op.getUniqueId() != null) o.put("player_uuid", op.getUniqueId().toString());
            }
            if (amount instanceof Number n) o.put("amount", CombatCore.round2(n.doubleValue()));
            ledger.emit("economy", null, o);
        } catch (Throwable ignored) { }
    }

    // ---------- economy emitters ----------
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEconomy(EconomyEvent e) {
        boolean enabled = switch (e.kind()) {
            case "shop_purchase" -> cfg.ecoShop();
            case "reward_adjustment" -> cfg.ecoRewards();
            case "service_charge" -> cfg.ecoCharges();
            case "service_cast" -> cfg.ecoCasts();
            default -> true;
        };
        if (!enabled) return;
        Json.Obj o = new Json.Obj().put("type", e.kind());
        e.fields().forEach((k, v) -> { if (v != null) o.put(k, v); });
        ledger.emit("economy", null, o);
    }
}
