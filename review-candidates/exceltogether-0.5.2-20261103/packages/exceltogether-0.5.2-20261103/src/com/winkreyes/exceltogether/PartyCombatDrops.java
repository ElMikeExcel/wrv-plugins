package com.winkreyes.exceltogether;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
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
 * The 0.4.0 party combat extras, one listener: friendly-fire blocking between party members
 * (melee, projectiles, TNT, pets, harmful potions and lingering clouds), bonus drops for plain
 * mob loot when enough of the party is together, and the HP scoreboard refresh after a join.
 * All rules live in PartyRules; this class only gathers the Bukkit facts.
 */
public final class PartyCombatDrops implements Listener {
    private final Plugin plugin;
    private final PartyManager mgr;
    private final Logger log;
    private final boolean ffOn;
    private final boolean ffPets;
    private final boolean ffPotions;
    private final boolean dropsOn;
    private final boolean debug;
    private final double mult;
    private final double radius;
    private final int minTogether;
    private final Set<String> reasons;
    private final Set<String> excluded;
    private final PartyRules.KillCap cap;
    private final long noticeMs;
    private final Map<UUID, Long> lastNotice = new HashMap<>();
    private final String hpObjective;

    public PartyCombatDrops(Plugin plugin, PartyManager mgr, double bonusRadius) {
        this.plugin = plugin;
        this.mgr = mgr;
        this.log = plugin.getLogger();
        FileConfiguration cfg = plugin.getConfig();
        this.ffOn = cfg.getBoolean("friendly-fire.block-party-damage", true);
        this.ffPets = cfg.getBoolean("friendly-fire.protect-party-pets", true);
        this.ffPotions = cfg.getBoolean("friendly-fire.block-harmful-potions", true);
        this.dropsOn = cfg.getBoolean("party-drops.enabled", true);
        this.mult = Math.max(1.0, Math.min(10.0, cfg.getDouble("party-drops.multiplier", 2.0)));
        this.minTogether = Math.max(2, cfg.getInt("party-drops.min-together", 3));
        double r = cfg.getDouble("party-drops.radius", -1.0);
        this.radius = r > 0.0 ? r : bonusRadius;
        this.reasons = PartyRules.upperSet(cfg.getStringList("party-drops.allowed-spawn-reasons"), PartyRules.DEFAULT_ALLOWED_REASONS);
        this.excluded = PartyRules.upperSet(cfg.getStringList("party-drops.excluded-mobs"), PartyRules.DEFAULT_EXCLUDED_TYPES);
        this.cap = new PartyRules.KillCap(cfg.getInt("party-drops.max-boosted-kills-per-hour", 150));
        this.noticeMs = Math.max(0L, cfg.getLong("party-drops.notice-cooldown-seconds", 600L)) * 1000L;
        this.debug = cfg.getBoolean("party-drops.debug", false);
        this.hpObjective = cfg.getString("hp-refresh-objective", "hp");
    }

    public String summary() {
        return "Party friendly fire: " + (ffOn ? "BLOCKED (pets " + (ffPets ? "on" : "off") + ", potions " + (ffPotions ? "on" : "off") + ")" : "allowed")
                + ". Party drops: " + (dropsOn ? "x" + RangeIndicator.multText(mult) + " when " + minTogether + "+ party members are within " + (int) radius
                + " blocks, cap " + plugin.getConfig().getInt("party-drops.max-boosted-kills-per-hour", 150) + " boosted kills/hour" : "OFF")
                + ". hp refresh: " + (hpObjective == null || hpObjective.isBlank() ? "OFF" : "objective '" + hpObjective + "'");
    }

    /** The player responsible for an entity's harm: the player themselves, a projectile's shooter, a pet's owner, a cloud's or TNT's source. */
    static Player responsiblePlayer(Entity e) {
        if (e == null) return null;
        if (e instanceof Player p) return p;
        if (e instanceof Projectile proj && proj.getShooter() instanceof Player p) return p;
        if (e instanceof Tameable t && t.isTamed() && t.getOwner() instanceof Player p) return p;
        if (e instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Player p) return p;
        if (e instanceof TNTPrimed tnt && tnt.getSource() instanceof Player p) return p;
        return null;
    }

    private boolean same(Player attacker, Player victim) {
        return attacker != null && victim != null
                && PartyRules.blockFriendlyFire(ffOn, attacker.getUniqueId(), victim.getUniqueId(), mgr::sameParty);
    }

    private static Player petOwner(Entity e) {
        return e instanceof Tameable t && t.isTamed() && t.getOwner() instanceof Player p ? p : null;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        if (!ffOn) return;
        Entity entity = e.getEntity();
        Player victim = entity instanceof Player p ? p : (ffPets ? petOwner(entity) : null);
        if (victim == null) return;
        Player attacker = null;
        if (e instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent by = (EntityDamageByEntityEvent) e;
            attacker = responsiblePlayer(by.getDamager());
        }
        if (attacker == null) {
            try {
                org.bukkit.damage.DamageSource ds = e.getDamageSource();
                attacker = responsiblePlayer(ds.getCausingEntity());
                if (attacker == null) attacker = responsiblePlayer(ds.getDirectEntity());
            } catch (Throwable ignored) { }
        }
        if (same(attacker, victim)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCombust(EntityCombustByEntityEvent e) {
        if (!ffOn) return;
        Player victim = e.getEntity() instanceof Player p ? p : (ffPets ? petOwner(e.getEntity()) : null);
        if (same(responsiblePlayer(e.getCombuster()), victim)) e.setCancelled(true);
    }

    private static boolean harmful(Collection<PotionEffect> effects) {
        for (PotionEffect fx : effects) {
            try {
                if (fx.getType().getCategory() == PotionEffectTypeCategory.HARMFUL) return true;
            } catch (Throwable t) { return true; }
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSplash(PotionSplashEvent e) {
        if (!ffOn || !ffPotions) return;
        ThrownPotion potion = e.getPotion();
        ProjectileSource shooter = potion.getShooter();
        Player thrower;
        if (shooter instanceof Player) {
            thrower = (Player) shooter;
            if (!harmful(potion.getEffects())) return;
        } else return;
        for (LivingEntity le : new ArrayList<>(e.getAffectedEntities())) {
            Player victim = le instanceof Player p ? p : (ffPets ? petOwner(le) : null);
            if (same(thrower, victim)) e.setIntensity(le, 0.0);
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCloud(AreaEffectCloudApplyEvent e) {
        if (!ffOn || !ffPotions) return;
        AreaEffectCloud cloud = e.getEntity();
        if (!(cloud.getSource() instanceof Player source)) return;
        List<PotionEffect> effects = new ArrayList<>(cloud.getCustomEffects());
        try {
            if (cloud.getBasePotionType() != null) effects.addAll(cloud.getBasePotionType().getPotionEffects());
        } catch (Throwable ignored) { }
        if (!harmful(effects)) return;
        e.getAffectedEntities().removeIf(le -> same(source, le instanceof Player p ? p : (ffPets ? petOwner(le) : null)));
    }

    /** Killer plus same-party members in radius in the same world (the killer counts as one). */
    int togetherCount(Player player) {
        Location loc = player.getLocation();
        if (loc.getWorld() == null) return 1;
        int n = 1;
        double r2 = radius * radius;
        for (Player o : loc.getWorld().getPlayers()) {
            if (o.getUniqueId().equals(player.getUniqueId()) || o.isDead() || o.getGameMode() == GameMode.SPECTATOR) continue;
            if (!mgr.sameParty(player.getUniqueId(), o.getUniqueId())) continue;
            if (o.getLocation().distanceSquared(loc) <= r2) n++;
        }
        return n;
    }

    private static boolean hasEliteMobsTag(Entity e) {
        try {
            for (NamespacedKey k : e.getPersistentDataContainer().getKeys()) {
                if ("elitemobs".equalsIgnoreCase(k.getNamespace())) return true;
            }
        } catch (Throwable t) { return true; }
        return false;
    }

    /** Plain stackable loot only: no named/enchanted/custom items, nothing unstackable. */
    private static boolean plain(ItemStack s) {
        if (s == null || s.getType().isAir() || s.getAmount() <= 0 || s.getMaxStackSize() <= 1) return false;
        if (!s.hasItemMeta()) return true;
        ItemMeta meta = s.getItemMeta();
        if (meta == null) return true;
        if (meta.hasDisplayName() || meta.hasLore() || meta.hasEnchants() || meta.hasCustomModelData()) return false;
        try {
            if (!meta.getPersistentDataContainer().isEmpty()) return false;
        } catch (Throwable ignored) { }
        return true;
    }

    /** Everything the entity was carrying or wearing: drops matching any of it are gear, not loot. */
    private static List<ItemStack> carried(LivingEntity le) {
        List<ItemStack> out = new ArrayList<>();
        EntityEquipment eq = le.getEquipment();
        if (eq != null) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                try {
                    ItemStack s = eq.getItem(slot);
                    if (s != null && !s.getType().isAir()) out.add(s);
                } catch (Throwable ignored) { }
            }
        }
        if (le instanceof InventoryHolder holder) {
            try {
                for (ItemStack s : holder.getInventory().getContents()) {
                    if (s != null && !s.getType().isAir()) out.add(s);
                }
            } catch (Throwable ignored) { }
        }
        return out;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!dropsOn) return;
        LivingEntity le = e.getEntity();
        if (le instanceof Player) return;
        Player killer = le.getKiller();
        PartyRules.Kill kill = new PartyRules.Kill();
        kill.killerIsPlayer = killer != null;
        kill.type = le.getType().name();
        long now = System.currentTimeMillis();
        PartyRules.Verdict verdict;
        if (killer == null) {
            verdict = PartyRules.judge(true, kill, minTogether, reasons, excluded);
        } else {
            kill.eliteMobs = hasEliteMobsTag(le);
            try { kill.fromSpawner = le.fromMobSpawner(); } catch (Throwable t) { kill.fromSpawner = true; }
            try {
                org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason sr = le.getEntitySpawnReason();
                kill.spawnReason = sr == null ? null : sr.name();
            } catch (Throwable t) { kill.spawnReason = null; }
            kill.tamed = le instanceof Tameable t && t.isTamed();
            kill.killerInParty = mgr.inParty(killer.getUniqueId());
            kill.togetherCount = kill.killerInParty ? togetherCount(killer) : 1;
            kill.underKillCap = cap.wouldAllow(killer.getUniqueId(), now);
            verdict = PartyRules.judge(true, kill, minTogether, reasons, excluded);
        }
        if (!verdict.boost()) {
            if (debug && killer != null) {
                log.info("[party-drops] 1x " + kill.type + " by " + killer.getName() + ": " + verdict
                        + " (reason " + kill.spawnReason + ", together " + kill.togetherCount + ")");
            }
            return;
        }
        List<ItemStack> carriedItems = carried(le);
        List<ItemStack> extras = new ArrayList<>();
        int total = 0;
        for (ItemStack drop : e.getDrops()) {
            if (!plain(drop)) continue;
            boolean gear = false;
            for (ItemStack c : carriedItems) {
                if (c.isSimilar(drop)) { gear = true; break; }
            }
            if (gear) continue;
            int extra = PartyRules.extraItems(drop.getAmount(), mult, () -> ThreadLocalRandom.current().nextDouble());
            total += extra;
            while (extra > 0) {
                int chunk = Math.min(extra, drop.getMaxStackSize());
                ItemStack add = drop.clone();
                add.setAmount(chunk);
                extras.add(add);
                extra -= chunk;
            }
        }
        if (total <= 0) return;
        cap.tryUse(killer.getUniqueId(), now);
        e.getDrops().addAll(extras);
        log.info("[party-drops] x" + RangeIndicator.multText(mult) + " " + kill.type + " by " + killer.getName()
                + " (" + kill.togetherCount + " together): +" + total + " items");
        notifyParty(killer, kill.togetherCount, now);
    }

    private void notifyParty(Player killer, int together, long now) {
        if (noticeMs <= 0) return;
        List<Player> recipients = new ArrayList<>();
        recipients.add(killer);
        double r2 = radius * radius;
        for (Player o : killer.getWorld().getPlayers()) {
            if (o.equals(killer) || !mgr.sameParty(killer.getUniqueId(), o.getUniqueId())
                    || o.getLocation().distanceSquared(killer.getLocation()) > r2) continue;
            recipients.add(o);
        }
        String line = PartyRules.dropsOnLine(together, mult);
        for (Player p : recipients) {
            Long last = lastNotice.get(p.getUniqueId());
            if (last != null && now - last < noticeMs) continue;
            lastNotice.put(p.getUniqueId(), now);
            p.sendMessage(Component.text(line, NamedTextColor.GOLD));
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) { lastNotice.remove(e.getPlayer().getUniqueId()); }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent e) {
        if (hpObjective == null || hpObjective.isBlank()) return;
        UUID id = e.getPlayer().getUniqueId();
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshHp(id), 2L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> refreshHp(id), 40L);
    }

    /** The vanilla health objective only updates on damage; write the real score so nameplates stay honest. */
    public void refreshHp(UUID id) {
        Player p = Bukkit.getPlayer(id);
        if (p == null || !p.isOnline()) return;
        try {
            Objective obj = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(hpObjective);
            if (obj == null) return;
            obj.getScore(p.getName()).setScore((int) Math.ceil(p.getHealth() + p.getAbsorptionAmount()));
        } catch (Throwable t) {
            if (debug) log.info("[hp-refresh] skipped: " + t);
        }
    }
}
