package com.winkreyes.exceldowned;

import com.elmakers.mine.bukkit.action.ActionFactory;
import com.elmakers.mine.bukkit.action.BaseSpellAction;
import com.elmakers.mine.bukkit.api.action.CastContext;
import com.elmakers.mine.bukkit.api.event.CastEvent;
import com.elmakers.mine.bukkit.api.event.PreCastEvent;
import com.elmakers.mine.bukkit.api.spell.SpellResult;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * Magic 11.2.5 integration (DESIGN §6), verdicts verified against the
 * shipped jar with javap:
 *
 *  - PreCastEvent is Cancellable and carries Mage + Spell — the pre-cast
 *    gate. Cancelling means the spell never performs: no mana, no
 *    cooldown. Used for (a) downed mages cast nothing, (b) Resurrection
 *    may not start unless a downed player is within gate range.
 *  - CastEvent backstop: if a Resurrection cast completes without a
 *    revive landing, the spell's own clearCooldown() (present on the API
 *    Spell interface) refunds the 10-minute cooldown.
 *  - A custom "Resurrect" action is registered with ActionFactory
 *    (registerActionClass exists in the jar) and performs the revive
 *    from the cast context. Registration happens at this plugin's
 *    enable — after Magic loaded its templates — so the action path is
 *    belt-and-braces: the CastEvent listener performs the same revive
 *    through ExcelDowned.revive, which is idempotent via the state
 *    machine. Whichever path fires first consumes the session.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class MagicBridge implements Listener {

    static final String SPELL_KEY = "resurrection";

    private final ExcelDowned plugin;

    public MagicBridge(ExcelDowned plugin) { this.plugin = plugin; }

    void tryRegisterAction() { registerAction(plugin.getLogger()); }

    /**
     * 0.1.3: static so ExcelDowned.onLoad can register before Magic's
     * onEnable loads its spell templates (Defect 2 fix shape 2). Safe to
     * call twice (load + enable): a repeat failure is caught and logged,
     * and the CastEvent listener route carries the spell regardless.
     */
    static void registerAction(java.util.logging.Logger logger) {
        try {
            ActionFactory.registerActionClass("Resurrect", ResurrectAction.class);
            logger.info("ExcelDowned: custom Magic action 'Resurrect' registered.");
        } catch (Throwable t) {
            logger.info("ExcelDowned: custom action registration unavailable ("
                    + t.getMessage() + "); CastEvent listener route carries Resurrection.");
        }
    }

    // ----- pre-cast gates: nothing is spent when these cancel -----

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onPreCast(PreCastEvent e) {
        Player caster = e.getMage() == null ? null : e.getMage().getPlayer();
        if (caster == null) return;
        if (plugin.isDowned(caster.getUniqueId())) {
            e.setCancelled(true); // downed mages cast nothing (DESIGN §2)
            return;
        }
        String key = e.getSpell() == null || e.getSpell().getKey() == null
                ? "" : e.getSpell().getKey();
        if (!SPELL_KEY.equals(key)) return;
        if (findDowned(caster, plugin.gateRange).isEmpty()) {
            e.setCancelled(true);
            caster.sendMessage(ChatColor.RED
                    + "✝ Resurrection: no downed player nearby — nothing was spent.");
        }
    }

    // ----- cast completion: perform the revive, or refund the cooldown -----

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCast(CastEvent e) {
        if (e.getSpell() == null || e.getSpell().getKey() == null) return;
        if (!SPELL_KEY.equals(e.getSpell().getKey())) return;
        Player caster = e.getMage() == null ? null : e.getMage().getPlayer();
        if (caster == null) return;
        if (plugin.wasRevivedByRecently(caster.getUniqueId(), 3000)) {
            plugin.consumeReviveMark(caster.getUniqueId());
            return; // the custom action (or an earlier listener pass) did it
        }
        Player target = pickAimedTarget(caster, e);
        if (target != null && plugin.revive(caster, target)) return;
        // no revive landed: refund the 10-minute cooldown (DESIGN §6)
        try { e.getSpell().clearCooldown(); } catch (Throwable ignored) {}
        caster.sendMessage(ChatColor.RED
                + "✝ Resurrection found no downed player in your aim — cooldown refunded.");
    }

    /** Downed players within range of the caster (party-blind, D140). */
    List<Player> findDowned(Player caster, double range) {
        List<Player> out = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.equals(caster) || !plugin.isDowned(p.getUniqueId())) continue;
            if (!p.getWorld().equals(caster.getWorld())) continue;
            if (p.getLocation().distanceSquared(caster.getLocation()) > range * range) continue;
            out.add(p);
        }
        return out;
    }

    /**
     * The cast's own target entity wins; otherwise the downed player
     * closest to the caster's aim inside the revive range.
     */
    Player pickAimedTarget(Player caster, CastEvent e) {
        List<Player> candidates = findDowned(caster, plugin.reviveRange);
        if (candidates.isEmpty()) return null;
        try {
            Entity t = e.getSpell().getTargetEntity();
            if (t instanceof Player && candidates.contains(t)) return (Player) t;
        } catch (Throwable ignored) {}
        Vector aim = caster.getEyeLocation().getDirection();
        Player best = null;
        double bestDot = 0.5; // ~60-degree cone
        for (Player c : candidates) {
            Vector to = c.getLocation().add(0, 1, 0).toVector()
                    .subtract(caster.getEyeLocation().toVector());
            if (to.lengthSquared() < 0.01) return c;
            double dot = to.normalize().dot(aim);
            if (dot > bestDot) { bestDot = dot; best = c; }
        }
        return best != null ? best : (candidates.size() == 1 ? candidates.get(0) : null);
    }

    /** Custom Magic action: performs the revive from the cast context. */
    public static class ResurrectAction extends BaseSpellAction {
        @Override
        public SpellResult perform(CastContext context) {
            try {
                Player caster = context.getMage() == null ? null : context.getMage().getPlayer();
                if (caster == null) return SpellResult.FAIL;
                ExcelDowned downed = (ExcelDowned) Bukkit.getPluginManager().getPlugin("ExcelDowned");
                if (downed == null) return SpellResult.FAIL;
                Entity t = context.getTargetEntity();
                if (t instanceof Player && downed.isDowned(t.getUniqueId())
                        && FixPolicy.withinReviveRange(
                                ((Player) t).getWorld().equals(caster.getWorld()),
                                ((Player) t).getLocation().distanceSquared(caster.getLocation()),
                                downed.reviveRange)
                        && downed.revive(caster, (Player) t)) {
                    return SpellResult.CAST;
                }
                // aimed target wasn't it: fall back to nearest downed player
                // 0.1.3 (Defect 2 fix shape 5): the fallback now applies the
                // SAME same-world + revive-range checks as findDowned —
                // the 0.1.2 fallback checked neither and could revive across
                // the map / across worlds.
                Player best = null;
                double bestD = Double.MAX_VALUE;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.equals(caster) || !downed.isDowned(p.getUniqueId())) continue;
                    double d = p.getLocation().distanceSquared(caster.getLocation());
                    if (!FixPolicy.withinReviveRange(p.getWorld().equals(caster.getWorld()),
                            d, downed.reviveRange)) continue;
                    if (d < bestD) { bestD = d; best = p; }
                }
                if (best != null && downed.revive(caster, best)) return SpellResult.CAST;
                return SpellResult.FAIL;
            } catch (Throwable t) {
                return SpellResult.FAIL;
            }
        }

        @Override
        public void initialize(com.elmakers.mine.bukkit.api.spell.Spell spell,
                               ConfigurationSection parameters) {
            // no parameters of our own; the spell config carries range/mana
        }
    }
}
