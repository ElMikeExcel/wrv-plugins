package com.winkreyes.exceltogether;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.AreaEffectCloudApplyEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionEffectTypeCategory;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * ExcelTogether 0.4.0 Bukkit glue for three small features:
 *  1. Party friendly fire: members of the same ExcelTogether party cannot hurt each other
 *     (melee, arrows, tridents, fireworks, TNT they lit, Magic spell damage that names the caster,
 *     harmful splash/lingering potions, fire from flame arrows, and their tamed pets).
 *     PvP against everyone else is unchanged.
 *  2. Party drops: a normal mob killed by a player whose party has min-together members close by
 *     drops x multiplier of its plain loot. EliteMobs mobs are skipped entirely (ExcelPartyDrops owns
 *     those), as are spawner mobs, bred/egg/command/plugin-spawned mobs, bosses and players.
 *  3. hp scoreboard refresh: the vanilla health objective "hp" shows 0 for a player until their
 *     health changes; one refresh shortly after join fixes the display. No-op if the objective is missing.
 */
public final class PartyCombatDrops implements Listener {
    private final org.bukkit.plugin.Plugin plugin; private final PartyManager mgr; private final Logger log;
    private final boolean ffOn, ffPets, ffPotions;
    private final boolean dropsOn, debug; private final double mult, radius; private final int minTogether;
    private final Set<String> reasons, excluded; private final PartyRules.KillCap cap;
    private final long noticeMs; private final Map<UUID, Long> lastNotice = new HashMap<>();
    private final String hpObjective;

    public PartyCombatDrops(org.bukkit.plugin.Plugin plugin, PartyManager mgr, double defaultRadius) {
        this.plugin = plugin; this.mgr = mgr; this.log = plugin.getLogger();
        var c = plugin.getConfig();
        ffOn = c.getBoolean("friendly-fire.block-party-damage", true);
        ffPets = c.getBoolean("friendly-fire.protect-party-pets", true);
        ffPotions = c.getBoolean("friendly-fire.block-harmful-potions", true);
        dropsOn = c.getBoolean("party-drops.enabled", true);
        mult = Math.max(1.0, Math.min(10.0, c.getDouble("party-drops.multiplier", 2.0)));
        minTogether = Math.max(2, c.getInt("party-drops.min-together", 3));
        double r = c.getDouble("party-drops.radius", -1);
        radius = r > 0 ? r : defaultRadius;
        reasons = PartyRules.upperSet(c.getStringList("party-drops.allowed-spawn-reasons"), PartyRules.DEFAULT_ALLOWED_REASONS);
        excluded = PartyRules.upperSet(c.getStringList("party-drops.excluded-mobs"), PartyRules.DEFAULT_EXCLUDED_TYPES);
        cap = new PartyRules.KillCap(c.getInt("party-drops.max-boosted-kills-per-hour", 150));
        noticeMs = Math.max(0, c.getLong("party-drops.notice-cooldown-seconds", 600)) * 1000L;
        debug = c.getBoolean("party-drops.debug", false);
        hpObjective = c.getString("hp-refresh-objective", "hp");
    }

    public String summary() {
        return "Party friendly fire: " + (ffOn ? "BLOCKED (pets " + (ffPets ? "on" : "off") + ", potions " + (ffPotions ? "on" : "off") + ")" : "allowed")
                + ". Party drops: " + (dropsOn ? "x" + RangeIndicator.multText(mult) + " when " + minTogether + "+ party members are within " + (int) radius
                + " blocks, cap " + plugin.getConfig().getInt("party-drops.max-boosted-kills-per-hour", 150) + " boosted kills/hour" : "OFF")
                + ". hp refresh: " + (hpObjective == null || hpObjective.isBlank() ? "OFF" : "objective '" + hpObjective + "'");
    }

    // ================================================================ friendly fire

    /** The player behind a hit, or null. Projectiles, TNT and Magic damage resolve through Paper's DamageSource. */
    static Player responsiblePlayer(Entity e) {
        if (e == null) return null;
        if (e instanceof Player p) return p;
        if (e instanceof Projectile pr && pr.getShooter() instanceof Player p) return p;
        if (e instanceof Tameable t && t.isTamed() && t.getOwner() instanceof Player p) return p;
        if (e instanceof AreaEffectCloud cl && cl.getSource() instanceof Player p) return p;
        if (e instanceof org.bukkit.entity.TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    private boolean same(Player a, Player b) {
        return a != null && b != null && PartyRules.blockFriendlyFire(ffOn, a.getUniqueId(), b.getUniqueId(), mgr::sameParty);
    }

    /** Owner of a tamed pet if that owner is a player, else null. */
    private static Player petOwner(Entity e) {
        return e instanceof Tameable t && t.isTamed() && t.getOwner() instanceof Player p ? p : null;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!ffOn) return;
        Entity victim = e.getEntity();
        Player victimPlayer = victim instanceof Player p ? p : (ffPets ? petOwner(victim) : null);
        if (victimPlayer == null) return;
        Player attacker = null;
        if (e instanceof EntityDamageByEntityEvent be) attacker = responsiblePlayer(be.getDamager());
        if (attacker == null) {
            try {
                var src = e.getDamageSource();
                attacker = responsiblePlayer(src.getCausingEntity());
                if (attacker == null) attacker = responsiblePlayer(src.getDirectEntity());
            } catch (Throwable ignored) { }
        }
        if (same(attacker, victimPlayer)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent e) {
        if (!ffOn) return;
        Player victim = e.getEntity() instanceof Player p ? p : (ffPets ? petOwner(e.getEntity()) : null);
        if (same(responsiblePlayer(e.getCombuster()), victim)) e.setCancelled(true);
    }

    private static boolean harmful(Collection<PotionEffect> effects) {
        for (PotionEffect pe : effects) {
            try { if (pe.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) return true; }
            catch (Throwable t) { return true; }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        if (!ffOn || !ffPotions) return;
        ThrownPotion pot = e.getPotion();
        ProjectileSource src = pot.getShooter();
        if (!(src instanceof Player thrower) || !harmful(pot.getEffects())) return;
        for (LivingEntity le : new ArrayList<>(e.getAffectedEntities())) {
            Player owner = le instanceof Player p ? p : (ffPets ? petOwner(le) : null);
            if (same(thrower, owner)) e.setIntensity(le, 0.0);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent e) {
        if (!ffOn || !ffPotions) return;
        AreaEffectCloud cl = e.getEntity();
        if (!(cl.getSource() instanceof Player thrower)) return;
        List<PotionEffect> fx = new ArrayList<>(cl.getCustomEffects());
        try { if (cl.getBasePotionType() != null) fx.addAll(cl.getBasePotionType().getPotionEffects()); } catch (Throwable ignored) { }
        if (!harmful(fx)) return;
        e.getAffectedEntities().removeIf(le -> same(thrower, le instanceof Player p ? p : (ffPets ? petOwner(le) : null)));
    }

    // ================================================================ party drops

    /** Killer plus party members alive, online, not spectating, same world, within radius of the killer. */
    int togetherCount(Player killer) {
        Location a = killer.getLocation();
        if (a.getWorld() == null) return 1;
        int n = 1; double r2 = radius * radius;
        for (Player o : a.getWorld().getPlayers()) {
            if (o.getUniqueId().equals(killer.getUniqueId()) || o.isDead() || o.getGameMode() == GameMode.SPECTATOR) continue;
            if (!mgr.sameParty(killer.getUniqueId(), o.getUniqueId())) continue;
            if (o.getLocation().distanceSquared(a) <= r2) n++;
        }
        return n;
    }

    private static boolean hasEliteMobsTag(Entity e) {
        try {
            for (NamespacedKey k : e.getPersistentDataContainer().getKeys())
                if ("elitemobs".equalsIgnoreCase(k.getNamespace())) return true;
        } catch (Throwable t) { return true; }   // cannot tell: treat as EliteMobs and leave it alone
        return false;
    }

    /** Plain loot only: stackable, no name, lore, enchantments or plugin data. */
    private static boolean plain(ItemStack it) {
        if (it == null || it.getType().isAir() || it.getAmount() <= 0 || it.getMaxStackSize() <= 1) return false;
        if (!it.hasItemMeta()) return true;
        ItemMeta m = it.getItemMeta();
        if (m == null) return true;
        if (m.hasDisplayName() || m.hasLore() || m.hasEnchants() || m.hasCustomModelData()) return false;
        try { if (!m.getPersistentDataContainer().isEmpty()) return false; } catch (Throwable ignored) { }
        return true;
    }

    /** Items the mob was carrying (picked up, chest, saddle, armor): never multiplied, so nothing can be duped. */
    private static List<ItemStack> carried(LivingEntity le) {
        List<ItemStack> out = new ArrayList<>();
        EntityEquipment eq = le.getEquipment();
        if (eq != null) for (EquipmentSlot s : EquipmentSlot.values()) {
            try { ItemStack it = eq.getItem(s); if (it != null && !it.getType().isAir()) out.add(it); } catch (Throwable ignored) { }
        }
        if (le instanceof InventoryHolder h) {
            try { for (ItemStack it : h.getInventory().getContents()) if (it != null && !it.getType().isAir()) out.add(it); } catch (Throwable ignored) { }
        }
        return out;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!dropsOn) return;
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;                    // player deaths: never (Death Record owns those)
        Player killer = le.getKiller();
        PartyRules.Kill k = new PartyRules.Kill();
        k.killerIsPlayer = killer != null;
        k.type = le.getType().name();
        long now = System.currentTimeMillis();
        PartyRules.Verdict v;
        if (killer == null) v = PartyRules.judge(true, k, minTogether, reasons, excluded);
        else {
            k.eliteMobs = hasEliteMobsTag(le);
            try { k.fromSpawner = le.fromMobSpawner(); } catch (Throwable t) { k.fromSpawner = true; }
            try { var sr = le.getEntitySpawnReason(); k.spawnReason = sr == null ? null : sr.name(); } catch (Throwable t) { k.spawnReason = null; }
            k.tamed = le instanceof Tameable t && t.isTamed();
            k.killerInParty = mgr.inParty(killer.getUniqueId());
            k.togetherCount = k.killerInParty ? togetherCount(killer) : 1;
            k.underKillCap = cap.wouldAllow(killer.getUniqueId(), now);
            v = PartyRules.judge(true, k, minTogether, reasons, excluded);
        }
        if (!v.boost()) {
            if (debug && killer != null) log.info("[party-drops] 1x " + k.type + " by " + killer.getName() + ": " + v + " (reason " + k.spawnReason + ", together " + k.togetherCount + ")");
            return;
        }
        List<ItemStack> carried = carried(le);
        List<ItemStack> add = new ArrayList<>();
        int extraTotal = 0;
        for (ItemStack d : e.getDrops()) {
            if (!plain(d)) continue;
            boolean isCarried = false;
            for (ItemStack c : carried) if (c.isSimilar(d)) { isCarried = true; break; }
            if (isCarried) continue;
            int extra = PartyRules.extraItems(d.getAmount(), mult, () -> ThreadLocalRandom.current().nextDouble());
            extraTotal += extra;
            while (extra > 0) {
                int n = Math.min(extra, d.getMaxStackSize());
                ItemStack c = d.clone(); c.setAmount(n); add.add(c); extra -= n;
            }
        }
        if (extraTotal <= 0) return;
        cap.tryUse(killer.getUniqueId(), now);
        e.getDrops().addAll(add);
        log.info("[party-drops] x" + RangeIndicator.multText(mult) + " " + k.type + " by " + killer.getName() + " (" + k.togetherCount + " together): +" + extraTotal + " items");
        notifyParty(killer, k.togetherCount, now);
    }

    /** One short chat line to the party members in range, at most once per notice-cooldown per player. */
    private void notifyParty(Player killer, int together, long now) {
        if (noticeMs <= 0) return;
        List<Player> who = new ArrayList<>(); who.add(killer);
        double r2 = radius * radius;
        for (Player o : killer.getWorld().getPlayers())
            if (!o.equals(killer) && mgr.sameParty(killer.getUniqueId(), o.getUniqueId()) && o.getLocation().distanceSquared(killer.getLocation()) <= r2) who.add(o);
        String line = PartyRules.dropsOnLine(together, mult);
        for (Player p : who) {
            Long last = lastNotice.get(p.getUniqueId());
            if (last != null && now - last < noticeMs) continue;
            lastNotice.put(p.getUniqueId(), now);
            p.sendMessage(net.kyori.adventure.text.Component.text(line, net.kyori.adventure.text.format.NamedTextColor.GOLD));
        }
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { lastNotice.remove(e.getPlayer().getUniqueId()); }

    // ================================================================ hp scoreboard refresh

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (hpObjective == null || hpObjective.isBlank()) return;
        UUID id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshHp(id), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshHp(id), 40L);
    }

    /** Sets the player's score on the health objective to ceil(health + absorption), the same number vanilla shows. */
    public void refreshHp(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null || !p.isOnline()) return;
        try {
            Objective o = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(hpObjective);
            if (o == null) return;
            o.getScore(p.getName()).setScore((int) Math.ceil(p.getHealth() + p.getAbsorptionAmount()));
        } catch (Throwable t) {
            if (debug) log.info("[hp-refresh] skipped: " + t);
        }
    }
}
