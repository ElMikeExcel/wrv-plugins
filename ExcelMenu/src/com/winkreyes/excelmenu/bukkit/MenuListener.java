package com.winkreyes.excelmenu.bukkit;

import com.winkreyes.excelmenu.core.JavaConfirm;
import com.winkreyes.excelmenu.core.MenuDef;
import com.winkreyes.excelmenu.core.Selector;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;

/**
 * Chest-GUI clicks. A menu is not a container: every click in a
 * view whose holder is ours is cancelled, and the action that runs
 * is the one the holder's slot map names — never anything derived
 * from the clicked ItemStack.
 */
final class MenuListener implements Listener {

    private final MenuService service;

    MenuListener(MenuService service) {
        this.service = service;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().getHolder() instanceof ConfirmHolder confirm) {
            onConfirmClick(event, confirm);
            return;
        }
        if (!(event.getInventory().getHolder() instanceof MenuHolder holder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getInventory()) return; // bottom-inventory click
        MenuDef def = service.menu(holder.menuId());
        if (def == null) return;
        String itemId = holder.itemAt(event.getSlot());
        if (itemId == null) return;
        MenuDef.ItemDef item = def.item(itemId);
        if (item == null) return;
        List<String> lines = Selector.clickLines(item, kindOf(event.getClick()));
        String kind = kindOf(event.getClick());
        if (lines == null) { lines = Selector.clickLines(item, "any"); kind = "any"; }
        // 0.1.4: a spending click opens the Java Confirm/Cancel screen
        // instead of charging on the same click (Bedrock already gets
        // its item-detail form first).
        if (JavaConfirm.required(item, lines)) {
            service.openConfirm(player, def, item, kind, lines);
            return;
        }
        service.dispatcher().runAll(player, def.id + ":" + item.id(), lines,
                service.session(player, def));
    }

    private void onConfirmClick(InventoryClickEvent event, ConfirmHolder confirm) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() != event.getInventory()) return;
        if (!player.getUniqueId().equals(confirm.player())) return;
        ClickType click = event.getClick();
        if (click != ClickType.LEFT && click != ClickType.RIGHT) return; // no shift/number-key/double-click confirms
        int slot = event.getSlot();
        if (slot == ConfirmHolder.SLOT_CONFIRM) {
            if (!confirm.consume()) return;
            MenuDef def = service.menu(confirm.menuId());
            if (def == null || def.item(confirm.itemId()) == null) {
                player.closeInventory();
                player.sendMessage("\u00a7cThat item is no longer available. Nothing was charged.");
                return;
            }
            if (confirm.expired(System.currentTimeMillis())) {
                service.openMenu(player, confirm.menuId());
                player.sendMessage("\u00a7eThat confirm screen expired. Nothing was charged. Pick the item again.");
                return;
            }
            service.confirmed(player, def, confirm.itemId(), confirm.lines());
        } else if (slot == ConfirmHolder.SLOT_CANCEL) {
            if (!confirm.consume()) return;
            service.openMenu(player, confirm.menuId());
            player.sendMessage("\u00a77Cancelled. Nothing was charged.");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ConfirmHolder) {
            event.setCancelled(true);
            return;
        }
        if (event.getInventory().getHolder() instanceof MenuHolder) {
            event.setCancelled(true);
        }
    }

    private static String kindOf(ClickType click) {
        return switch (click) {
            case LEFT -> "left";
            case RIGHT -> "right";
            case SHIFT_LEFT -> "shift-left";
            case SHIFT_RIGHT -> "shift-right";
            case MIDDLE -> "middle";
            default -> "any";
        };
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
