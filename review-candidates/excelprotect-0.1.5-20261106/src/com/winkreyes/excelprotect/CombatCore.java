package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * ExcelProtect — COMBAT ANALYTICS family (SPEC §3.10): the shared fight-window damage
 * ledger (ExcelTelemetry's model, proven 2026-11-01), per-event combat damage/healing
 * records for player-involved combat, fight aggregation with a config gap window, and
 * Magic CastEvent heal-spell claims via a fail-soft reflective adapter (the same feed
 * BossTrial 0.3.1 uses). Boss/seal fight tables arrive via api.FightTableEvent.
 * Excel — Wink Reyes Valley
 */
public final class CombatCore implements Listener {
    private final Ledger ledger;
    private final Settings cfg;
    private final Plugin plugin;
    final DamageLedger kills = new DamageLedger(5000);
    private final Map<UUID, Claim> claims = new HashMap<>();
    private final Map<String, Fight> fights = new HashMap<>();
    private long fightSeq = 0;

    private record Claim(String spell, long ms) { }

    private static final class Fight {
        final String id; long lastMs; double total;
        final Map<String, double[]> players = new TreeMap<>(); // name -> [dealt, taken, healedDone, healedReceived]
        Fight(String id, long now) { this.id = id; this.lastMs = now; }
        double[] row(String name) { return players.computeIfAbsent(name, k -> new double[4]); }
    }

    public CombatCore(Plugin plugin, Ledger ledger, Settings cfg) {
        this.plugin = plugin; this.ledger = ledger; this.cfg = cfg;
    }

    /** Fail-soft Magic adapter: register for CastEvent by name; absence is silent. */
    public void hookMagic() {
        Plugin magic = Bukkit.getPluginManager().getPlugin("Magic");
        if (magic == null || !magic.isEnabled()) { plugin.getLogger().info("[ExcelProtect] combat: Magic not present — spell attribution off (fail-soft)"); return; }
        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> cls = (Class<? extends Event>)
                    Class.forName("com.elmakers.mine.bukkit.api.event.CastEvent");
            Bukkit.getPluginManager().registerEvent(cls, this, EventPriority.MONITOR,
                    (EventExecutor) (l, ev) -> onCast(ev), plugin, true);
            plugin.getLogger().info("[ExcelProtect] combat: Magic CastEvent feed hooked (spell attribution on)");
        } catch (Throwable t) {
            plugin.getLogger().info("[ExcelProtect] combat: Magic CastEvent unavailable — spell attribution off (fail-soft)");
        }
    }

    private void onCast(Event ev) {
        try {
            Object mage = ev.getClass().getMethod("getMage").invoke(ev);
            Object ctrl = mage.getClass().getMethod("getController").invoke(mage);
            Object p = ctrl.getClass().getMethod("getPlayer").invoke(ctrl);
            Object spell = ev.getClass().getMethod("getSpell").invoke(ev);
            String key = (String) spell.getClass().getMethod("getKey").invoke(spell);
            if (p instanceof Player pl && key != null)
                claims.put(pl.getUniqueId(), new Claim(key, System.currentTimeMillis()));
        } catch (Throwable ignored) { }
    }

    public String consumeClaim(UUID caster) {
        Claim c = claims.get(caster);
        if (c == null) return null;
        if (System.currentTimeMillis() - c.ms() > 1000) { claims.remove(caster); return null; }
        claims.remove(caster);
        return c.spell();
    }

    /**
     * 0.1.3: read a fresh claim WITHOUT consuming it. DealtWatch/HealingWatch
     * peek so their incident attribution sees the same cast this class's own
     * per-event listeners are about to consume — they are registered BEFORE
     * this listener for exactly that reason (ExcelProtect.onEnable). A peek
     * never starves the consume: both layers attribute the same event.
     */
    public String peekClaim(UUID caster) {
        Claim c = claims.get(caster);
        if (c == null) return null;
        if (System.currentTimeMillis() - c.ms() > 1000) return null;
        return c.spell();
    }

    static String attackerKey(Entity damager, EntityDamageEvent.DamageCause cause) {
        if (damager == null) return "env:" + cause.name().toLowerCase();
        if (damager instanceof Player p) return "player:" + p.getUniqueId();
        return "entity:" + damager.getType().getKey() + ":" + damager.getUniqueId();
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof LivingEntity victim)) return;
        Entity damager = null;
        if (e instanceof EntityDamageByEntityEvent by) {
            damager = by.getDamager();
            if (damager instanceof Projectile proj && proj.getShooter() instanceof Entity shooter) damager = shooter;
        }
        double dmg = e.getFinalDamage();
        if (dmg <= 0) return;
        String kind = damager instanceof Player ? "player" : damager != null ? "entity" : "environment";
        String label = damager instanceof Player p ? p.getName()
                : damager != null ? damager.getType().getKey().toString() : null;
        kills.record(victim.getUniqueId(), System.currentTimeMillis(), attackerKey(damager, e.getCause()),
                kind, damager == null ? null : damager.getUniqueId().toString(), label,
                e.getCause().name(), dmg);
        // combat family: only player-involved events (SPEC §3.10)
        boolean playerInvolved = victim instanceof Player || damager instanceof Player;
        if (!playerInvolved) return;
        if (cfg.combatDamage()) {
            Json.Obj o = new Json.Obj().put("type", "combat_damage")
                    .put("victim", victim.getType().getKey().toString())
                    .put("victim_uuid", victim.getUniqueId().toString())
                    .put("amount", round2(dmg)).put("cause", e.getCause().name());
            if (victim.getCustomName() != null) o.put("victim_name", victim.getCustomName());
            if (damager instanceof Player dp) {
                o.put("attacker", dp.getName()).put("attacker_uuid", dp.getUniqueId().toString());
                ItemFact w = ItemFact.of(dp.getInventory().getItemInMainHand(), false);
                if (w != null) o.put("weapon", w.toJson());
                String spell = consumeClaim(dp.getUniqueId());
                if (spell != null) o.put("spell", spell);
            } else if (damager != null) {
                o.put("attacker_entity", damager.getType().getKey().toString())
                 .put("attacker_uuid", damager.getUniqueId().toString());
            }
            ledger.emit("combat", victim.getLocation(), o);
        }
        if (cfg.combatFights()) {
            Fight f = currentFight(victim, damager);
            if (f != null) {
                f.lastMs = System.currentTimeMillis(); f.total += dmg;
                if (damager instanceof Player dp) f.row(dp.getName())[0] += dmg;
                if (victim instanceof Player vp) f.row(vp.getName())[1] += dmg;
            }
        }
    }

    @org.bukkit.event.EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (!cfg.combatHealing() || !(e.getEntity() instanceof Player target)) return;
        double amt = e.getAmount();
        if (amt <= 0) return;
        // Magic heal pairing (BossTrial 0.3.1's model): a fresh cast claim by a nearby
        // caster explains a CUSTOM regain — credit the spell + healer.
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.CUSTOM) {
            for (Player cand : Bukkit.getOnlinePlayers()) {
                if (cand.equals(target) || !cand.getWorld().equals(target.getWorld())) continue;
                if (cand.getLocation().distanceSquared(target.getLocation()) > 2500) continue;
                Claim c = claims.get(cand.getUniqueId());
                if (c != null && System.currentTimeMillis() - c.ms() <= 1000) {
                    claims.remove(cand.getUniqueId());
                    creditHeal(cand, target, amt, c.spell());
                    return;
                }
            }
        }
        Json.Obj o = new Json.Obj().put("type", "combat_heal")
                .put("target", target.getName()).put("target_uuid", target.getUniqueId().toString())
                .put("amount", round2(amt)).put("source", e.getRegainReason().name());
        ledger.emit("combat", target.getLocation(), o);
        if (cfg.combatFights()) {
            Fight f = currentFight(target, null);
            if (f != null) f.row(target.getName())[3] += amt;
        }
    }

    /** Credit a heal to a caster (Magic heal claims resolved by BossTrial-style pairing: healer casts, target regains). */
    public void creditHeal(Player healer, Player target, double amount, String spell) {
        if (!cfg.combatHealing()) return;
        Json.Obj o = new Json.Obj().put("type", "combat_heal")
                .put("target", target.getName()).put("target_uuid", target.getUniqueId().toString())
                .put("amount", round2(amount)).put("source", "SPELL");
        if (spell != null) o.put("spell", spell);
        if (healer != null) o.put("healer", healer.getName()).put("healer_uuid", healer.getUniqueId().toString());
        ledger.emit("combat", target.getLocation(), o);
        if (cfg.combatFights()) {
            Fight f = currentFight(target, healer);
            if (f != null) {
                f.row(target.getName())[3] += amount;
                if (healer != null) f.row(healer.getName())[2] += amount;
            }
        }
    }

    private Fight currentFight(Entity a, Entity b) {
        long now = System.currentTimeMillis();
        String key = fightKey(a, b);
        Fight f = fights.get(key);
        if (f == null || now - f.lastMs > cfg.fightGapSeconds() * 1000L) {
            if (f != null) closeFight(f);
            f = new Fight("fight-" + (++fightSeq) + "-" + (now / 1000), now);
            fights.put(key, f);
        }
        return f;
    }

    private String fightKey(Entity a, Entity b) {
        // general fights group by the non-player party when there is one (a boss/mob), else by world area cell
        Entity anchor = !(a instanceof Player) ? a : (b != null && !(b instanceof Player)) ? b : a;
        if (anchor != null && !(anchor instanceof Player))
            return "ent:" + anchor.getUniqueId();
        org.bukkit.Location l = a.getLocation();
        return "area:" + l.getWorld().getName() + ":" + (l.getBlockX() >> 5) + ":" + (l.getBlockZ() >> 5);
    }

    private void closeFight(Fight f) {
        if (f.total < cfg.fightMinDamage()) return;
        Json.Arr rows = new Json.Arr();
        f.players.forEach((name, v) -> rows.add(new Json.Obj().put("player", name)
                .put("damage_dealt", round2(v[0])).put("damage_taken", round2(v[1]))
                .put("healing_done", round2(v[2])).put("healing_received", round2(v[3]))));
        ledger.emit("combat", null, new Json.Obj().put("type", "fight_summary")
                .put("encounter", f.id).put("source", "general")
                .put("total_damage", round2(f.total)).put("players", rows));
    }

    /** Called by the flush timer: close fights whose gap window expired. */
    public void tick() {
        if (!cfg.combatFights()) return;
        long now = System.currentTimeMillis();
        fights.values().removeIf(f -> {
            if (now - f.lastMs > cfg.fightGapSeconds() * 1000L) { closeFight(f); return true; }
            return false;
        });
        kills.prune(now - 10 * 60_000L);
    }

    static double round2(double v) { return Math.round(v * 100.0) / 100.0; }
}
