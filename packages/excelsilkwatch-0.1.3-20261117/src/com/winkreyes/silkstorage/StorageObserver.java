// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.silkstorage;

import java.util.*;
import java.util.function.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;

/** Reads only. The original event is checked again after vanilla has processed it. */
public final class StorageObserver implements Listener {
    private record Pending(Player player, Inventory storage, ItemStack incoming,
                           int before, Cancellable event) {}
    private final Map<UUID, Pending> pending = new HashMap<>();
    private final Map<UUID, Long> lastWarning = new HashMap<>();
    private final Consumer<Runnable> nextTick;
    private final Consumer<Player> warning;
    private final LongSupplier clock;
    private final long cooldownMillis;
    private final Predicate<UUID> enabled;

    public StorageObserver(Consumer<Runnable> nextTick, Consumer<Player> warning,
                           LongSupplier clock, long cooldownMillis) {
        this(nextTick, warning, clock, cooldownMillis, id -> true);
    }

    public StorageObserver(Consumer<Runnable> nextTick, Consumer<Player> warning,
                           LongSupplier clock, long cooldownMillis, Predicate<UUID> enabled) {
        this.enabled = enabled;
        this.nextTick = nextTick; this.warning = warning; this.clock = clock;
        this.cooldownMillis = Math.max(0, cooldownMillis);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        flush(player.getUniqueId()); // Distinguish deposit then withdrawal within one tick.
        if (event.isCancelled() || event instanceof InventoryCreativeEvent) return;
        Inventory top = event.getView().getTopInventory();
        if (!nativeEnderChest(player, top)) return;
        boolean inTop = event.getRawSlot() >= 0 && event.getRawSlot() < top.getSize();
        ItemStack incoming = null;
        switch (event.getAction()) {
            case PLACE_ALL, PLACE_ONE, PLACE_SOME, SWAP_WITH_CURSOR -> {
                if (inTop) incoming = event.getCursor();
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                if (event.getClickedInventory() == event.getView().getBottomInventory())
                    incoming = event.getCurrentItem();
            }
            case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD -> {
                if (inTop) {
                    if (event.getClick() == ClickType.SWAP_OFFHAND)
                        incoming = player.getInventory().getItemInOffHand();
                    else if (event.getHotbarButton() >= 0 && event.getHotbarButton() < 9)
                        incoming = player.getInventory().getItem(event.getHotbarButton());
                }
            }
            default -> { }
        }
        observe(player, top, incoming, event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void drag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        flush(player.getUniqueId());
        Inventory top = event.getView().getTopInventory();
        if (event.isCancelled() || !nativeEnderChest(player, top)) return;
        if (event.getRawSlots().stream().noneMatch(slot -> slot >= 0 && slot < top.getSize())) return;
        observe(player, top, event.getOldCursor(), event);
    }

    // A close is dispatched after the preceding transaction: inspect before another plugin
    // can clear a virtual inventory. Never open or close a UI ourselves.
    @EventHandler(priority = EventPriority.LOWEST)
    public void close(InventoryCloseEvent event) { flush(event.getPlayer().getUniqueId()); }

    @EventHandler
    public void quit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
        lastWarning.remove(event.getPlayer().getUniqueId());
    }

    private void observe(Player player, Inventory top, ItemStack incoming, Cancellable event) {
        if (!enabled.test(player.getUniqueId())) return;
        if (incoming == null || incoming.getAmount() <= 0 ||
                !SilkContents.containsSilkTouch(incoming)) return;
        ItemStack key = incoming.clone(); key.setAmount(1); // Only our private clone changes.
        Pending p = new Pending(player, top, key, count(top, key), event);
        pending.put(player.getUniqueId(), p);
        nextTick.accept(() -> {
            if (pending.get(player.getUniqueId()) == p) flush(player.getUniqueId());
        });
    }

    private static boolean nativeEnderChest(Player player, Inventory top) {
        return top.getType() == InventoryType.ENDER_CHEST && top.equals(player.getEnderChest());
    }

    private static int count(Inventory inventory, ItemStack key) {
        int count = 0;
        for (ItemStack item : inventory.getContents())
            if (item != null && item.isSimilar(key)) count += item.getAmount();
        return count;
    }

    private void flush(UUID id) {
        Pending p = pending.remove(id);
        if (p == null || !enabled.test(id) || p.event.isCancelled() || !p.player.isOnline()) return;
        if (count(p.storage, p.incoming) <= p.before) return;
        long now = clock.getAsLong();
        Long previous = lastWarning.get(id);
        if (previous != null && now - previous < cooldownMillis) return;
        lastWarning.put(id, now);
        warning.accept(p.player);
    }

    public void clear() { pending.clear(); lastWarning.clear(); }
}
