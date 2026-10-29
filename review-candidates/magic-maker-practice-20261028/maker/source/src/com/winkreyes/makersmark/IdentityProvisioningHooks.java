package com.winkreyes.makersmark;
import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.*;
/** Offline candidate listener: provision authoritative idle owned inventory, never a deferred shot source. */
final class IdentityProvisioningHooks implements Listener {
    private final ExcelMakersMark plugin;
    private final Set<UUID> queued=new HashSet<>();
    IdentityProvisioningHooks(ExcelMakersMark plugin){this.plugin=plugin;}
    private void later(Player player){
        if(!queued.add(player.getUniqueId()))return;
        Bukkit.getScheduler().runTask(plugin,()->{
            queued.remove(player.getUniqueId());
            if(plugin.isEnabled()&&player.isOnline()&&plugin.counts(player))OwnedSlotIdentity.provision(player,plugin);
        });
    }
    @EventHandler(priority=EventPriority.MONITOR)public void join(PlayerJoinEvent e){later(e.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR)public void close(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)later(p);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void held(PlayerItemHeldEvent e){later(e.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void swap(PlayerSwapHandItemsEvent e){later(e.getPlayer());}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void craft(CraftItemEvent e){if(e.getWhoClicked() instanceof Player p)later(p);}
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)public void pickup(EntityPickupItemEvent e){if(e.getEntity() instanceof Player p)later(p);}
    @EventHandler public void quit(PlayerQuitEvent e){queued.remove(e.getPlayer().getUniqueId());}
}
