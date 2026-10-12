// Confirmation guard modification, 2026-10-12. See NOTICE and COPYING.LESSER.
package com.Acrobot.ChestShop.Listeners.PostTransaction;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Confirmation.ConfirmationGate;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import static com.Acrobot.ChestShop.Events.TransactionEvent.TransactionType.BUY;

/**
 * @author Acrobot
 */
public class EconomicModule implements Listener {

    @EventHandler(ignoreCancelled = true)
    public static void onBuyTransaction(TransactionEvent event) {
        if (event.isCancelled() || !ConfirmationGate.beginMoney(event)) return;
        CurrencyTransferEvent currencyTransferEvent = new CurrencyTransferEvent(
                event.getExactPrice(),
                event.getClient(),
                event.getOwnerAccount().getUuid(),
                event.getTransactionType() == BUY ? CurrencyTransferEvent.Direction.PARTNER : CurrencyTransferEvent.Direction.INITIATOR,
                event
        );
        ConfirmationGate.bindTransfer(event, currencyTransferEvent);
        try {
            ChestShop.callEvent(currencyTransferEvent);
        } catch (RuntimeException | Error failure) {
            ConfirmationGate.fail(event, failure);
            throw failure;
        } finally {
            ConfirmationGate.finishMoney(event, currencyTransferEvent.wasHandled());
        }
        if (!currencyTransferEvent.wasHandled()) event.setCancelled(true);
    }
}
