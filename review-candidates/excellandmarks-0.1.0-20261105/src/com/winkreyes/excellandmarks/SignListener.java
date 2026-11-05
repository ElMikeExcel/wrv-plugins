package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.core.TapRouter;
import com.winkreyes.excellandmarks.store.Store;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Sign registration + tap routing + protection (SPEC §2, §3, §6).
 * Interact runs at HIGH with the pure TapRouter deciding; anything that
 * is not a main-hand right-click on a registered sign is left entirely
 * alone (Oct 15 lesson: exactly one menu, zero collateral cancels).
 */
public final class SignListener implements Listener {

    private final ExcelLandmarks plugin;
    private final Map<UUID, Long> lastOpen = new HashMap<>();

    public SignListener(ExcelLandmarks plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------ registration

    @EventHandler(priority = EventPriority.HIGH)
    public void onSignChange(SignChangeEvent e) {
        String line0 = ChatColor.stripColor(e.getLine(0) == null ? "" : e.getLine(0)).trim();
        if (!line0.equalsIgnoreCase("[Landmark]")) return;
        Player player = e.getPlayer();
        Block b = e.getBlock();

        if (plugin.registry().bySign(b.getWorld().getName(), b.getX(), b.getY(), b.getZ()) != null) {
            e.setCancelled(true);
            player.sendMessage("§cThat sign is already a registered landmark spot — manage it with /landmarks admin.");
            return;
        }
        if (!player.hasPermission("landmarks.admin")) {
            e.setLine(0, "Landmark");
            player.sendMessage("§cOnly an admin can register a landmark spot.");
            return;
        }
        // Parse TYPE:TIER (line 3) and size (line 4)
        Spot.Type type = plugin.settings().defaultType;
        Spot.Tier tier = plugin.settings().defaultTier;
        String tt = (e.getLine(2) == null ? "" : e.getLine(2)).trim().toUpperCase();
        if (!tt.isEmpty()) {
            String[] parts = tt.split("[:\\s]+");
            try {
                if (parts.length >= 1 && !parts[0].isEmpty()) type = Spot.Type.valueOf(parts[0]);
                if (parts.length >= 2 && !parts[1].isEmpty()) tier = Spot.Tier.valueOf(parts[1]);
            } catch (IllegalArgumentException ex) {
                e.setCancelled(true);
                player.sendMessage("§cLandmark sign line 3 must look like PLOT:HEART or LANDMARK:SPOT.");
                return;
            }
        }
        Integer size = null;
        String sz = (e.getLine(3) == null ? "" : e.getLine(3)).trim();
        if (!sz.isEmpty()) {
            try {
                size = Integer.parseInt(sz);
                if (size < 1 || size > 25) throw new NumberFormatException("range");
            } catch (NumberFormatException ex) {
                e.setCancelled(true);
                player.sendMessage("§cLandmark sign line 4 (plot size) must be a number 1–25.");
                return;
            }
        }
        String name = e.getLine(1) == null ? "" : e.getLine(1).trim();
        Spot spot = plugin.service().register(b.getWorld(), b.getX(), b.getY(), b.getZ(), name, type, tier, size);
        e.setCancelled(true);
        // The block's sign state settles after this event; rewrite next tick.
        plugin.getServer().getScheduler().runTask(plugin, () -> plugin.service().refreshSign(spot));
        player.sendMessage("§aRegistered landmark spot §f" + spot.id + " §a(" + spot.name + ", "
                + spot.type + ":" + spot.tier + ").");
    }

    // ------------------------------------------------------------- taps

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        Block b = e.getClickedBlock();
        if (b == null) return;
        Spot spot = plugin.registry().bySign(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        boolean registeredSign = spot != null && b.getState() instanceof Sign;
        TapRouter.Action action = switch (e.getAction()) {
            case RIGHT_CLICK_BLOCK -> TapRouter.Action.RIGHT_CLICK_BLOCK;
            case LEFT_CLICK_BLOCK -> TapRouter.Action.LEFT_CLICK_BLOCK;
            case RIGHT_CLICK_AIR -> TapRouter.Action.RIGHT_CLICK_AIR;
            case LEFT_CLICK_AIR -> TapRouter.Action.LEFT_CLICK_AIR;
            case PHYSICAL -> TapRouter.Action.PHYSICAL;
        };
        TapRouter.Hand hand = e.getHand() == EquipmentSlot.OFF_HAND ? TapRouter.Hand.OFF_HAND : TapRouter.Hand.HAND;
        long last = lastOpen.getOrDefault(e.getPlayer().getUniqueId(), 0L);
        TapRouter.Route route = TapRouter.route(action, hand, registeredSign,
                System.currentTimeMillis() - last);
        if (route != TapRouter.Route.OPEN_MENU) return;
        e.setCancelled(true);
        e.setUseInteractedBlock(Event.Result.DENY);
        lastOpen.put(e.getPlayer().getUniqueId(), System.currentTimeMillis());
        plugin.menus().openSpot(e.getPlayer(), spot);
    }

    // -------------------------------------------------------- protection

    @EventHandler(priority = EventPriority.HIGH)
    public void onBreak(BlockBreakEvent e) {
        Block b = e.getBlock();
        Spot spot = plugin.registry().bySign(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        if (spot != null && b.getState() instanceof Sign) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThat sign belongs to landmark spot " + spot.id
                    + " — an admin can remove it with /landmarks admin remove " + spot.id + ".");
            return;
        }
        Store.Box box = plugin.store().boxAt(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        if (box != null && !e.getPlayer().getUniqueId().equals(box.owner)
                && !e.getPlayer().hasPermission("landmarks.admin")) {
            e.setCancelled(true);
            e.getPlayer().sendMessage("§cThat redemption box belongs to " + box.ownerName + ".");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onOpen(InventoryOpenEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (e.getInventory().getLocation() == null) return;
        Block b = e.getInventory().getLocation().getBlock();
        Store.Box box = plugin.store().boxAt(b.getWorld().getName(), b.getX(), b.getY(), b.getZ());
        if (box != null && !player.getUniqueId().equals(box.owner)
                && !player.hasPermission("landmarks.admin")) {
            e.setCancelled(true);
            player.sendMessage("§cThat redemption box belongs to " + box.ownerName + ".");
        }
    }
}
