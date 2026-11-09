package com.winkreyes.excelprotect;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ExcelProtect 0.1.3 — the Bukkit glue for HEALING incidents (SPEC §3): a
 * MONITOR listener over EntityRegainHealthEvent for PLAYER targets, feeding
 * the pure HealingIncidents coalescer; closed incidents go to the Ledger's
 * async writer on the `healing` stream — the event path does no IO.
 *
 * Attribution law (honest, no guessing):
 *   - CUSTOM regains: a fresh Magic cast claim by a nearby player (the
 *     CombatCore pairing model, resolved by PEEK — this listener is
 *     registered BEFORE CombatCore so the peek runs before CombatCore's
 *     consume; the target's own claim means a self-cast heal). One cast can
 *     heal over several events: after the claim is consumed, later events
 *     of the same burst reuse the attribution cached here for the target
 *     (inside the coalesce gap), so a heal-over-time stays one incident.
 *   - Every other RegainReason (REGEN, SATIATED, EATING, POTION, MAGIC,
 *     MAGIC_REGEN, …) is the target's own recovery systems — potions drunk,
 *     food, regeneration — so healer = target (self-healing). Vanilla gives
 *     EntityRegainHealthEvent no thrower, so a splash potion thrown by
 *     another player is indistinguishable from drinking one; it records as
 *     self, and SPEC says so plainly.
 *   - A CUSTOM regain with no claim and no fresh cache: healer = null.
 * Excel — Wink Reyes Valley
 */
public final class HealingWatch implements Listener {
    private final Ledger ledger;
    private final HealingIncidents incidents;
    private final CombatCore combat;
    private final long gapMs;
    /** target -> last spell attribution seen for them (healer, spell, ms). */
    private final Map<UUID, Object[]> attrCache = new HashMap<>();

    public HealingWatch(Plugin plugin, Ledger ledger, Settings cfg, CombatCore combat) {
        this.ledger = ledger;
        this.combat = combat;
        this.gapMs = cfg.damageIncidentGapMs();
        this.incidents = new HealingIncidents(gapMs);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeal(EntityRegainHealthEvent e) {
        if (!(e.getEntity() instanceof Player target)) return;
        double amt = e.getAmount();
        if (amt <= 0) return;
        long now = System.currentTimeMillis();
        EntityRegainHealthEvent.RegainReason reason = e.getRegainReason();

        UUID healerUuid = null;
        String healer = null, spell = null, source;
        if (reason == EntityRegainHealthEvent.RegainReason.CUSTOM) {
            // Claim pairing by peek: another nearby caster first (CombatCore's
            // model), then the target's own claim (a self-cast heal).
            Player caster = null;
            String key = null;
            if (combat != null) {
                for (Player cand : Bukkit.getOnlinePlayers()) {
                    if (cand.equals(target) || !cand.getWorld().equals(target.getWorld())) continue;
                    if (cand.getLocation().distanceSquared(target.getLocation()) > 2500) continue;
                    key = combat.peekClaim(cand.getUniqueId());
                    if (key != null) { caster = cand; break; }
                }
                if (caster == null) {
                    key = combat.peekClaim(target.getUniqueId());
                    if (key != null) caster = target;
                }
            }
            if (caster != null) {
                healerUuid = caster.getUniqueId();
                healer = caster.getName();
                spell = key;
                source = "SPELL";
                attrCache.put(target.getUniqueId(), new Object[]{healerUuid, healer, spell, now});
            } else {
                Object[] cached = attrCache.get(target.getUniqueId());
                if (cached != null && now - (Long) cached[3] <= gapMs) {
                    healerUuid = (UUID) cached[0];
                    healer = (String) cached[1];
                    spell = (String) cached[2];
                    source = "SPELL";
                } else {
                    source = "CUSTOM";
                }
            }
        } else {
            healerUuid = target.getUniqueId();
            healer = target.getName();
            source = reason.name();
        }
        boolean self = healerUuid != null && healerUuid.equals(target.getUniqueId());
        String sourceKey = (healerUuid == null ? "unknown" : healerUuid.toString())
                + "|" + (spell != null ? "SPELL:" + spell : reason.name());
        Location loc = target.getLocation();
        AttributeInstance maxHp = target.getAttribute(Attribute.MAX_HEALTH);
        HealingIncidents.Tick tick = new HealingIncidents.Tick(
                target.getUniqueId(), target.getName(),
                loc.getWorld() == null ? "" : loc.getWorld().getName(),
                loc.getX(), loc.getY(), loc.getZ(),
                sourceKey, healerUuid, healer, self,
                source, reason.name(), spell,
                amt, target.getHealth(), maxHp == null ? 20.0 : maxHp.getValue(), now);
        emit(incidents.record(tick));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        emit(incidents.closeTarget(e.getPlayer().getUniqueId(), "quit"));
        attrCache.remove(e.getPlayer().getUniqueId());
    }

    /** Sweep tick (1 s, main thread, map work only): close gap-expired incidents. */
    public void tick() {
        emit(incidents.sweep(System.currentTimeMillis()));
    }

    /** Plugin shutdown: close everything so no heal tail is lost. */
    public void closeAll(String reason) {
        emit(incidents.closeAll(reason));
    }

    public int openCount() { return incidents.openCount(); }

    private void emit(List<HealingIncidents.Incident> closed) {
        for (HealingIncidents.Incident inc : closed) {
            World w = inc.world == null || inc.world.isEmpty() ? null : Bukkit.getWorld(inc.world);
            Location loc = w == null ? null : new Location(w, inc.x, inc.y, inc.z);
            ledger.emit("healing", loc, inc.render());
        }
    }
}
