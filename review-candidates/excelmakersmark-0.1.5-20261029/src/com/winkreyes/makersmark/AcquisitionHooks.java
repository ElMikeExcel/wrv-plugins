/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.Gear;
import com.winkreyes.makersmark.core.TransferEvidence;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Container;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/** Observes actual transfers, then stamps ONLY a confirmed new destination next tick.
 * Never modifies a chest preview, source stack, ground entity or arbitrary inventory contents.
 * Multiple interactions in one tick and any ambiguous identity fail closed. */
final class AcquisitionHooks implements Listener {
    private final ExcelMakersMark plugin;
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, Integer> lastInteractionTick = new HashMap<>();

    private static final class Pending {
        final Player player;
        final ItemStack wanted;
        final List<ItemStack> before;
        final IntSupplier sourceAfter;
        final BooleanSupplier cancelled, validSource;
        final int sourceBefore;
        Pending(Player p, ItemStack wanted, List<ItemStack> before, int sourceBefore,
                IntSupplier sourceAfter, BooleanSupplier cancelled, BooleanSupplier validSource) {
            this.player=p; this.wanted=wanted; this.before=before; this.sourceBefore=sourceBefore;
            this.sourceAfter=sourceAfter; this.cancelled=cancelled; this.validSource=validSource;
        }
    }

    AcquisitionHooks(ExcelMakersMark plugin) { this.plugin=plugin; }

    private static boolean survivalLike(Player p) {
        return p.getGameMode()==GameMode.SURVIVAL || p.getGameMode()==GameMode.ADVENTURE;
    }

    private boolean claimable(ItemStack item) {
        return item!=null && item.getAmount()==1 && plugin.eligible(item)
                && Gear.weapon(item.getType().name()) && !plugin.marks().read(item).hasProvenance();
    }

    /** Also invalidates a pending claim when the next interaction itself is not eligible. */
    private boolean begin(Player p) {
        UUID id=p.getUniqueId();
        pending.remove(id);
        int tick=Bukkit.getCurrentTick();
        Integer previous=lastInteractionTick.put(id,tick);
        return previous==null || previous!=tick;
    }

    private static ItemStack copy(ItemStack item) {
        return item==null || item.getType().isAir() ? null : item.clone();
    }

    private static List<ItemStack> inventorySnapshot(Inventory inventory) {
        List<ItemStack> result=new ArrayList<>();
        for(int i=0;i<inventory.getSize();i++) result.add(copy(inventory.getItem(i)));
        return result;
    }

    /** Last slot is the cursor. All slots included so stationary duplicates cannot be mistaken for the new transfer. */
    private static List<ItemStack> playerSnapshot(Player p) {
        List<ItemStack> result=inventorySnapshot(p.getInventory());
        result.add(copy(p.getItemOnCursor()));
        return result;
    }

    private static boolean storageType(Inventory inventory) {
        return switch(inventory.getType()) {
            case CHEST, BARREL, SHULKER_BOX, HOPPER, DISPENSER, DROPPER -> true;
            default -> false;
        };
    }

    /** Real storage only: excludes virtual/plugin menus, player inventories and processing results. */
    private static BooleanSupplier realStorageGuard(Inventory top, Player player) {
        if(top.getType()==InventoryType.ENDER_CHEST && top.equals(player.getEnderChest()))
            return ()->top.equals(player.getEnderChest());
        if(!storageType(top)) return null;
        InventoryHolder holder=top.getHolder();
        if(holder instanceof StorageMinecart cart) {
            if(!cart.isValid() || !cart.getInventory().equals(top)) return null;
            return ()->cart.isValid() && cart.getInventory().equals(top);
        }
        if(!(holder instanceof Container) && !(holder instanceof DoubleChest)) return null;
        if(!holder.getInventory().equals(top)) return null;
        Location at=top.getLocation();
        if(at==null || at.getWorld()==null) return null;
        if(!at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)) return null;
        org.bukkit.Material type=at.getBlock().getType();
        return ()->at.getWorld().isChunkLoaded(at.getBlockX()>>4,at.getBlockZ()>>4)
                && at.getBlock().getType()==type && holder.getInventory().equals(top);
    }

    private static boolean supported(InventoryAction action) {
        return switch(action) {
            case PICKUP_ALL, PICKUP_HALF, PICKUP_ONE, PICKUP_SOME,
                    SWAP_WITH_CURSOR, MOVE_TO_OTHER_INVENTORY, HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> true;
            default -> false;
        };
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onClick(InventoryClickEvent e) {
        if(!(e.getWhoClicked() instanceof Player p)) return;
        boolean first=begin(p);
        if(!first || e.isCancelled() || e instanceof InventoryCreativeEvent || !survivalLike(p)
                || !supported(e.getAction())) return;
        Inventory top=e.getView().getTopInventory();
        if(!top.equals(e.getClickedInventory()) || e.getRawSlot()<0 || e.getRawSlot()>=top.getSize()) return;
        BooleanSupplier real=realStorageGuard(top,p);
        if(real==null) return;
        ItemStack item=e.getCurrentItem();
        if(!claimable(item)) return;
        ItemStack wanted=item.clone();
        List<ItemStack> before=playerSnapshot(p);
        int sourceSlot=e.getSlot();
        queue(new Pending(p,wanted,before,1,
                ()->wanted.equals(top.getItem(sourceSlot)) ? 1 : 0,e::isCancelled,real));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPickup(EntityPickupItemEvent e) {
        if(!(e.getEntity() instanceof Player p)) return;
        boolean first=begin(p);
        if(!first || e.isCancelled() || !survivalLike(p) || e.getRemaining()!=0) return;
        Item source=e.getItem();
        ItemStack item=source.getItemStack();
        if(!claimable(item)) return;
        ItemStack wanted=item.clone();
        List<ItemStack> before=playerSnapshot(p);
        queue(new Pending(p,wanted,before,1,
                ()->source.isValid() && !source.isDead() ? 1 : 0,
                e::isCancelled,()->!source.isValid() || source.isDead()));
    }

    private void queue(Pending candidate) {
        pending.put(candidate.player.getUniqueId(),candidate);
        Bukkit.getScheduler().runTask(plugin,()->finish(candidate));
    }

    private void finish(Pending candidate) {
        Player p=candidate.player;
        if(!pending.remove(p.getUniqueId(),candidate) || !p.isOnline() || !survivalLike(p)) return;
        if(candidate.cancelled.getAsBoolean() || !candidate.validSource.getAsBoolean()) return;
        List<ItemStack> after=playerSnapshot(p);
        OptionalInt index=TransferEvidence.uniqueNewSlot(candidate.before,after,candidate.wanted,
                candidate.sourceBefore,candidate.sourceAfter.getAsInt(),false,true);
        if(index.isEmpty()) return;
        int slot=index.getAsInt();
        boolean cursor=slot==p.getInventory().getSize();
        ItemStack actual=cursor ? p.getItemOnCursor() : p.getInventory().getItem(slot);
        if(!candidate.wanted.equals(actual) || !claimable(actual)) return;
        ItemStack changed=actual.clone();
        if(!plugin.claimFound(p,changed)) return;
        if(cursor) p.setItemOnCursor(changed); else p.getInventory().setItem(slot,changed);
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onDrag(InventoryDragEvent e) { if(e.getWhoClicked() instanceof Player p) begin(p); }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onDrop(PlayerDropItemEvent e) { begin(e.getPlayer()); }
    @EventHandler(priority=EventPriority.MONITOR)
    public void onSwap(PlayerSwapHandItemsEvent e) { begin(e.getPlayer()); }
    @EventHandler public void onQuit(PlayerQuitEvent e) {
        UUID id=e.getPlayer().getUniqueId();pending.remove(id);lastInteractionTick.remove(id);
    }
}
