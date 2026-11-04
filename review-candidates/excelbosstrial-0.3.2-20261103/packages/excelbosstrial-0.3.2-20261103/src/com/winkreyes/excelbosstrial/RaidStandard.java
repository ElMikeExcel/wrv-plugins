/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

/**
 * The Village Standard (0.3.0, RAID-COMPANION-SPEC): the minted
 * raid signature - a shield-base standard carried in the offhand.
 * Best-in-slot by construction: durability far beyond a vanilla
 * shield (config max-damage), Unbreaking + Mending, a speed and
 * attack-speed (Haste) aura for the holder and allies within the
 * configured radius, and a damage reduction that by default
 * protects the HOLDER only - the config scope switch
 * (raid-standard.damage-reduction-scope: holder|aura) can widen
 * it later by edit alone, per Mike's settlement.
 *
 * Everything balance-shaped is a config key under raid-standard.*
 * with the settled values as defaults. The class is loaded lazily
 * through {@link #load} so a config with the standard disabled
 * costs nothing.
 */
final class RaidStandard implements Listener {

    private final JavaPlugin plugin;
    private final String displayName;
    private final int unbreakingLevel;
    private final boolean mending;
    private final int maxDamage;
    private final double auraRadius;
    private final int speedAmplifier;
    private final int hasteAmplifier;
    private final int auraDurationTicks;
    private final double damageReduction;
    private final boolean reductionAuraScope;
    private final NamespacedKey markerKey;

    private RaidStandard(JavaPlugin plugin) {
        this.plugin = plugin;
        var cfg = plugin.getConfig();
        displayName = cfg.getString("raid-standard.name", "The Village Standard");
        unbreakingLevel = Math.max(0, cfg.getInt("raid-standard.unbreaking-level", 3));
        mending = cfg.getBoolean("raid-standard.mending", true);
        maxDamage = Math.max(1, cfg.getInt("raid-standard.max-damage", 2048));
        auraRadius = Math.max(0, cfg.getDouble("raid-standard.aura-radius-blocks", 50));
        speedAmplifier = Math.max(0, cfg.getInt("raid-standard.aura-speed-amplifier", 0));
        hasteAmplifier = Math.max(0, cfg.getInt("raid-standard.aura-haste-amplifier", 0));
        auraDurationTicks = Math.max(20, cfg.getInt("raid-standard.aura-duration-ticks", 40));
        damageReduction = Math.min(0.9, Math.max(0,
                cfg.getDouble("raid-standard.damage-reduction", 0.15)));
        reductionAuraScope = "aura".equalsIgnoreCase(
                cfg.getString("raid-standard.damage-reduction-scope", "holder"));
        markerKey = new NamespacedKey(plugin, "raid_standard");
    }

    /** Load + register when enabled. Returns the instance (also
     *  used purely as the item factory when events are off). */
    static RaidStandard load(ExcelBossTrial plugin) {
        if (!plugin.getConfig().getBoolean("raid-standard.enabled", true)) {
            return null;
        }
        RaidStandard standard = new RaidStandard(plugin);
        org.bukkit.Bukkit.getPluginManager().registerEvents(standard, plugin);
        org.bukkit.Bukkit.getScheduler().runTaskTimer(plugin, standard::auraTick, 20L, 20L);
        return standard;
    }

    /** Mint one standard. */
    ItemStack buildStandard() {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§f" + displayName);
            meta.setLore(List.of(
                    "§7The standard of the village that stood.",
                    "§7Carry it in your off hand: you and your",
                    "§7allies nearby move and strike faster,",
                    "§7and its bearer shrugs off harm."));
            if (unbreakingLevel > 0) {
                meta.addEnchant(Enchantment.UNBREAKING, unbreakingLevel, true);
            }
            if (mending) {
                meta.addEnchant(Enchantment.MENDING, 1, true);
            }
            if (meta instanceof Damageable damageable) {
                damageable.setMaxDamage(maxDamage);
            }
            meta.getPersistentDataContainer().set(markerKey, PersistentDataType.BYTE, (byte) 1);
            shield.setItemMeta(meta);
        }
        return shield;
    }

    private boolean holdsStandard(Player player) {
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (offhand == null || offhand.getType() != Material.SHIELD || !offhand.hasItemMeta()) {
            return false;
        }
        return offhand.getItemMeta().getPersistentDataContainer()
                .has(markerKey, PersistentDataType.BYTE);
    }

    /** The aura: holder + allies within radius, refreshed on the
     *  1 s cadence with a duration that bridges the gap. */
    private void auraTick() {
        for (Player holder : plugin.getServer().getOnlinePlayers()) {
            if (!holdsStandard(holder)) {
                continue;
            }
            for (Player ally : holder.getWorld().getPlayers()) {
                if (ally.getLocation().distanceSquared(holder.getLocation())
                        > auraRadius * auraRadius) {
                    continue;
                }
                ally.addPotionEffect(new PotionEffect(
                        PotionEffectType.SPEED, auraDurationTicks, speedAmplifier,
                        false, false, true));
                ally.addPotionEffect(new PotionEffect(
                        PotionEffectType.HASTE, auraDurationTicks, hasteAmplifier,
                        false, false, true));
            }
        }
    }

    /** The reduction: holder-only by default; the aura scope
     *  widens it to anyone standing in a holder's aura. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHolderDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim) || damageReduction <= 0) {
            return;
        }
        boolean covered = holdsStandard(victim);
        if (!covered && reductionAuraScope) {
            for (Player holder : victim.getWorld().getPlayers()) {
                if (holdsStandard(holder)
                        && holder.getLocation().distanceSquared(victim.getLocation())
                                <= auraRadius * auraRadius) {
                    covered = true;
                    break;
                }
            }
        }
        if (covered) {
            event.setDamage(event.getDamage() * (1.0 - damageReduction));
        }
    }
}

// ExcelBossTrial - a Wink Reyes Valley Excel plugin.
