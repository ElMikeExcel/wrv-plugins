// Confirmation guard modification, 2026-10-12. See NOTICE and COPYING.LESSER.
package com.Acrobot.ChestShop.Listeners.PostTransaction;

import com.Acrobot.Breeze.Utils.ImplementationAdapter;
import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Confirmation.ConfirmationGate;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.block.BlockState;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.BUY;
import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.SELL;

/**
 * @author Acrobot
 */
public class ItemManager implements Listener {
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public static void shopItemRemover(TransactionEvent event) {
        if (event.getTransactionType() != BUY) {
            return;
        }

        guardedTransfer(event, event.getOwnerInventory(), event.getClientInventory());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public static void inventoryItemRemover(TransactionEvent event) {
        if (event.getTransactionType() != SELL) {
            return;
        }

        guardedTransfer(event, event.getClientInventory(), event.getOwnerInventory());
    }

    private static void guardedTransfer(TransactionEvent event, Inventory sourceInventory, Inventory targetInventory) {
        if (event.isCancelled() || !ConfirmationGate.beginItems(event)) return;
        try {
            int leftovers = 0;
            for (ItemStack item : event.getStock()) {
                leftovers += ConfirmationGate.stackTo64(event)
                        ? InventoryUtil.transfer(item, sourceInventory, targetInventory, 64)
                        : InventoryUtil.transfer(item, sourceInventory, targetInventory);
            }
            update(sourceInventory.getHolder());
            update(targetInventory.getHolder());
            ConfirmationGate.finishItems(event, leftovers);
        } catch (RuntimeException | Error failure) {
            ConfirmationGate.fail(event, failure);
            throw failure;
        }
    }

    private static void update(InventoryHolder holder) {
        if (holder instanceof Player) {
            ((Player) holder).updateInventory();
        } else if (holder instanceof BlockState) {
            ((BlockState) holder).update();
        } else if (holder instanceof DoubleChest) {
            update(ImplementationAdapter.getLeftSide((DoubleChest) holder, false));
            update(ImplementationAdapter.getRightSide((DoubleChest) holder, false));
        }
    }
}
