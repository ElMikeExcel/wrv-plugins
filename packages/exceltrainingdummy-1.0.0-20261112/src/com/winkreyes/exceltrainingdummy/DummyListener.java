package com.winkreyes.exceltrainingdummy;

import com.winkreyes.exceltrainingdummy.core.DamageLedger;
import com.winkreyes.exceltrainingdummy.core.ReportText;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.UUID;

/**
 * Combat intake + the tap-the-dummy door.
 *
 * Intake discipline: the final damage is READ from the event, the
 * event's damage is then set to 0 — the dummy's virtual pool takes
 * the hit, the entity never does, so no death/XP/loot/economy path
 * can fire (DESIGN §7). The event is NOT cancelled, so the rest of
 * the house stack (Protect / Details listeners) still sees the hit
 * exactly as it would on a real mob.
 */
public final class DummyListener implements Listener {
    private final ExcelTrainingDummy plugin;

    public DummyListener(ExcelTrainingDummy plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Dummy d = plugin.dummyOf(event.getEntity());
        if (d == null) return;
        double dealt = event.getFinalDamage();
        event.setDamage(0);
        if (dealt <= 0 || d.session.resetting() || d.session.finished()) return;

        UUID attackerId = null;
        DamageLedger.Category category = DamageLedger.Category.OTHER;
        Entity damager = event.getDamager();
        if (damager instanceof Player) {
            attackerId = damager.getUniqueId();
            switch (event.getCause()) {
                case ENTITY_ATTACK, ENTITY_SWEEP_ATTACK -> category = DamageLedger.Category.MELEE;
                case MAGIC, CUSTOM, DRAGON_BREATH, WITHER -> category = DamageLedger.Category.SPELLS;
                default -> category = DamageLedger.Category.OTHER;
            }
        } else if (damager instanceof Projectile proj) {
            Object shooter = proj.getShooter();
            if (shooter instanceof Player) {
                attackerId = ((Player) shooter).getUniqueId();
                category = DamageLedger.Category.PROJECTILES;
            } else if (shooter instanceof Tameable tame && tame.getOwnerUniqueId() != null) {
                attackerId = tame.getOwnerUniqueId();
                category = DamageLedger.Category.SUMMONS;
            }
        } else if (damager instanceof Tameable tame && tame.getOwnerUniqueId() != null) {
            // A summon hitting the dummy: its damage attributes to the owner (DESIGN §7).
            attackerId = tame.getOwnerUniqueId();
            category = DamageLedger.Category.SUMMONS;
        } else if (damager instanceof LivingEntity) {
            attackerId = damager.getUniqueId();
            category = DamageLedger.Category.OTHER;
        }
        if (attackerId == null) return;

        long now = System.currentTimeMillis();
        d.lastActivityMillis = now;
        double crossed = d.session.recordDamage(attackerId, category, dealt, now);
        plugin.refreshNameplate(d);
        if (crossed == -1) {
            plugin.onTestFinished(d);
        } else if (crossed > 0) {
            String line = ReportText.checkpointLine(d.session, attackerId, now, crossed,
                    d.bossArmor, plugin.scaleFactor());
            Player attacker = plugin.getServer().getPlayer(attackerId);
            if (attacker != null) attacker.sendMessage(Component.text(line, NamedTextColor.AQUA));
            if (!attackerId.equals(d.ownerId)) {
                Player owner = plugin.getServer().getPlayer(d.ownerId);
                if (owner != null) owner.sendMessage(Component.text(line, NamedTextColor.AQUA));
            }
        }
    }

    /** Environmental damage (fire, falls, void…) never touches a dummy either. */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent) return; // handled above
        if (plugin.dummyOf(event.getEntity()) != null) event.setDamage(0);
    }

    /** Belt and braces: if a dummy somehow dies, it drops nothing and pays no XP. */
    @EventHandler(priority = EventPriority.HIGH)
    public void onDeath(EntityDeathEvent event) {
        if (!plugin.isDummy(event.getEntity())) return;
        event.getDrops().clear();
        event.setDroppedExp(0);
        Dummy d = plugin.dummyOf(event.getEntity());
        if (d != null) plugin.removeDummy(d, false);
    }

    /** Healer practice: the tester's own healing during a hazardous test feeds its HPS. */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRegain(EntityRegainHealthEvent event) {
        if (event.getEntity() instanceof Player p) plugin.recordHealing(p, event.getAmount());
    }

    /**
     * Tap-the-dummy (DESIGN, two doors §2): with a compass in hand,
     * tapping your own dummy opens ITS settings; anyone else's tap
     * prints the dummy's live status + current test readout. Without
     * a compass this is an ordinary interaction and nothing happens.
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Dummy d = plugin.dummyOf(event.getRightClicked());
        if (d == null) return;
        Player player = event.getPlayer();
        if (player.getInventory().getItemInMainHand().getType() != Material.COMPASS) return;
        event.setCancelled(true);
        long now = System.currentTimeMillis();
        if (d.ownerId.equals(player.getUniqueId())) {
            DummyMenus.openDummySettings(plugin, player, d);
        } else {
            for (String line : ReportText.statusReadout(d.session, d.ownerName, now,
                    d.hazard, d.bossArmor, plugin.scaleFactor())) {
                player.sendMessage(Component.text(line, NamedTextColor.AQUA));
            }
        }
    }

    /** Menu clicks: the menus are control panels, never item sources. */
    @EventHandler
    public void onMenuClick(org.bukkit.event.inventory.InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getInventory().getHolder() instanceof DummyMenus.SpawnMenu
                || event.getInventory().getHolder() instanceof DummyMenus.DummySettingsMenu) {
            event.setCancelled(true);
            if (event.getClickedInventory() == event.getInventory()) {
                DummyMenus.handleClick(plugin, p, event.getInventory().getHolder(), event.getSlot());
            }
        }
    }

    @EventHandler
    public void onMenuDrag(org.bukkit.event.inventory.InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof DummyMenus.SpawnMenu
                || event.getInventory().getHolder() instanceof DummyMenus.DummySettingsMenu) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        plugin.onOwnerQuit(event.getPlayer().getUniqueId());
    }
}
