// Confirmation guard modification, 2026-10-12. See NOTICE and COPYING.LESSER.
package com.Acrobot.ChestShop.Listeners.PreTransaction;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Permission;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import org.bukkit.entity.Player;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.Locale;

import static com.Acrobot.ChestShop.Events.PreTransactionEvent.TransactionOutcome.CLIENT_DOES_NOT_HAVE_PERMISSION;
import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.BUY;

/**
 * @author Acrobot
 */
public class PermissionChecker implements Listener {
    @EventHandler
    public static void onPermissionCheck(PreTransactionEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!hasTradePermission(event.getClient(), event.getTransactionType(), event.getSign(), event.getStock()))
            event.setCancelled(CLIENT_DOES_NOT_HAVE_PERMISSION);
    }

    /** Shared pure predicate used again immediately before confirmed payment. */
    public static boolean hasTradePermission(Player client, TransactionEvent.TransactionType transactionType,
                                            Sign sign, ItemStack[] items) {
        String itemLine = ChestShopSign.getItem(sign);
        if (itemLine.contains("#") && Permission.hasPermissionSetFalse(client, (transactionType == BUY ? Permission.BUY_ID : Permission.SELL_ID) + itemLine)) {
            return false;
        }

        for (ItemStack stock : items) {
            String matID = stock.getType().toString().toLowerCase(Locale.ROOT);

            boolean hasPerm;

            if (transactionType == BUY) {
                hasPerm = Permission.has(client, Permission.BUY) && !Permission.hasPermissionSetFalse(client, Permission.BUY_ID + matID)
                        || Permission.has(client, Permission.BUY_ID + matID);
            } else {
                hasPerm = Permission.has(client, Permission.SELL) && !Permission.hasPermissionSetFalse(client, Permission.SELL_ID + matID)
                        || Permission.has(client, Permission.SELL_ID + matID);
            }

            if (!hasPerm) {
                return false;
            }
        }
        return true;
    }
}
