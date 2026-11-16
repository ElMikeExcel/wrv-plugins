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
    private final org.bukkit.plugin.Plugin plugin;
    private final Ledger ledger;
    private final Settings cfg;
    private final Chains chains;

    public Adapters(Plugin plugin, Ledger ledger, Settings cfg, Chains chains) {
        this.plugin = plugin; this.ledger = ledger; this.cfg = cfg; this.chains = chains;
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
        // 0.1.5 Clarity: mint the chain on the trade record itself, then
        // resolve where the item landed on the next tick (SPEC §1/§2).
        Chains.Sig sig = Chains.of(f);
        String chain = sig == null ? null
                : chains.mint(e.getPlayer().getUniqueId(), sig, System.currentTimeMillis());
        if (chain != null) {
            o.put("chain", chain);
            scheduleDelivery(e.getPlayer(), chain, sig, f, f.count(), "villager_trade");
        }
        ledger.emit("economy", e.getPlayer().getLocation(), o);
    }

    // ---------- 0.1.5 delivery resolution (next tick, main thread) ----------
    private void scheduleDelivery(org.bukkit.entity.Player p, String chain, Chains.Sig sig,
                                  ItemFact fact, int count, String source) {
        try {
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin,
                    () -> { try { resolveDelivery(p, chain, sig, fact, count, source); }
                            catch (Throwable ignored) { } }, 1L);
        } catch (Throwable ignored) { }
    }

    /**
     * Where did the acquired item go? Inventory scan by signature
     * (total count >= acquired count), else a FRESH matching item
     * entity near the player (ticksLived <= 100 — an older identical
     * item lying nearby is not this delivery), else unknown. Emits the
     * linked `item_delivery` record; the ledger is append-only, so the
     * outcome is a follow-up, never an edit (SPEC §1).
     */
    private void resolveDelivery(org.bukkit.entity.Player p, String chain, Chains.Sig sig,
                                 ItemFact fact, int count, String source) {
        Chains.Delivered outcome;
        if (!p.isOnline()) {
            outcome = Chains.Delivered.UNKNOWN;
        } else {
            int found = 0;
            for (org.bukkit.inventory.ItemStack s : p.getInventory().getContents()) {
                ItemFact f2 = ItemFact.of(s, false);
                if (f2 != null && sig.equals(Chains.of(f2))) found += f2.count();
            }
            boolean inInv = found >= Math.max(1, count);
            boolean ground = false;
            if (!inInv) {
                int r = cfg.clarityGroundRadius();
                for (org.bukkit.entity.Entity en : p.getNearbyEntities(r, r, r)) {
                    if (en instanceof org.bukkit.entity.Item it && it.getTicksLived() <= 100) {
                        ItemFact f2 = ItemFact.of(it.getItemStack(), false);
                        if (f2 != null && sig.equals(Chains.of(f2))) { ground = true; break; }
                    }
                }
            }
            outcome = Chains.classify(inInv, ground);
        }
        chains.noteDelivery(chain, outcome, System.currentTimeMillis());
        Json.Obj o = new Json.Obj().put("type", "item_delivery")
                .put("player", p.getName()).put("player_uuid", p.getUniqueId().toString())
                .put("chain", chain).put("source", source)
                .put("delivered", outcome.name().toLowerCase(java.util.Locale.ROOT));
        if (fact != null) o.put("item", fact.toJson());
        ledger.emit("economy", p.isOnline() ? p.getLocation() : null, o);
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
        // 0.1.5 Clarity: shop purchases chain + resolve delivery like
        // villager trades, when the emitter's fields carry an extractable
        // player + item and the player is online (SPEC §1). Anything
        // unextractable passes through exactly as before — no chain, no
        // delivery record, no guessed fields.
        if ("shop_purchase".equals(e.kind())) {
            try {
                Object playerV = e.fields().get("player");
                Chains.Sig sig = null;
                int count = 1;
                Object itemV = e.fields().get("item");
                if (itemV instanceof java.util.Map<?, ?> m && m.get("id") != null) {
                    java.util.Map<String, Integer> ench = new java.util.TreeMap<>();
                    if (m.get("enchants") instanceof java.util.Map<?, ?> em)
                        em.forEach((k, v) -> { if (v instanceof Number n) ench.put(String.valueOf(k), n.intValue()); });
                    sig = new Chains.Sig(String.valueOf(m.get("id")), ench,
                            m.get("name") == null ? null : String.valueOf(m.get("name")));
                    if (m.get("count") instanceof Number n) count = n.intValue();
                } else if (e.fields().get("item_id") != null) {
                    sig = new Chains.Sig(String.valueOf(e.fields().get("item_id")), java.util.Map.of(), null);
                }
                org.bukkit.entity.Player p = playerV == null ? null
                        : org.bukkit.Bukkit.getPlayerExact(String.valueOf(playerV));
                if (p != null && sig != null) {
                    String chain = chains.mint(p.getUniqueId(), sig, System.currentTimeMillis());
                    if (chain != null) {
                        o.put("chain", chain);
                        ItemFact fact = new ItemFact(sig.id(), count, 0, sig.name(),
                                sig.enchants().isEmpty() ? null : sig.enchants(), null, null, null, null);
                        scheduleDelivery(p, chain, sig, fact, count, "shop_purchase");
                    }
                }
            } catch (Throwable ignored) { }
        }
        ledger.emit("economy", null, o);
    }
}
