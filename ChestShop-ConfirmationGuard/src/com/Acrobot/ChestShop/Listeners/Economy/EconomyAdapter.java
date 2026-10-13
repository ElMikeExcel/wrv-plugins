// Confirmation guard modification, 2026-10-12. See NOTICE and COPYING.LESSER.
package com.Acrobot.ChestShop.Listeners.Economy;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Events.Economy.AccountCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyAddEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyAmountEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyCheckEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyFormatEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyHoldEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencySubtractEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import org.bukkit.event.Listener;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public abstract class EconomyAdapter implements Listener {

    @Nullable
    public abstract ProviderInfo getProviderInfo();

    public abstract void onAmountCheck(CurrencyAmountEvent event);

    public abstract void onCurrencyCheck(CurrencyCheckEvent event);

    public abstract void onAccountCheck(AccountCheckEvent event);

    public abstract void onCurrencyFormat(CurrencyFormatEvent event);

    public abstract void onCurrencyAdd(CurrencyAddEvent event);

    public abstract void onCurrencySubtraction(CurrencySubtractEvent event);

    public abstract void onCurrencyTransfer(CurrencyTransferEvent event);

    public abstract void onCurrencyHoldCheck(CurrencyHoldEvent event);

    /**
     * Convenience method to process transfers by first subtracting and then adding
     *
     * @param event The CurrencyTransferEvent to process
     */
    protected void processTransfer(CurrencyTransferEvent event) {
        if (event.wasHandled() || !com.Acrobot.ChestShop.Confirmation.ConfirmationGate.beginTransfer(event)) {
            return;
        }

        // A callback may teleport the initiator; all legs belong to one world.
        World world = event.getWorld();
        BigDecimal amountSent = event.getAmountSent();
        CurrencySubtractEvent currencySubtractEvent = new CurrencySubtractEvent(amountSent, event.getSender(), world);
        if (!NameManager.isAdminShop(event.getSender())) {
            ChestShop.callEvent(currencySubtractEvent);
        } else {
            currencySubtractEvent.setHandled(true);
        }

        if (!currencySubtractEvent.wasHandled()) {
            return;
        }

        BigDecimal amountReceived = event.getAmountReceived().subtract(amountSent.subtract(currencySubtractEvent.getAmount()));
        CurrencyAddEvent currencyAddEvent = new CurrencyAddEvent(amountReceived, event.getReceiver(), world);
        if (!NameManager.isAdminShop(event.getReceiver())) {
            ChestShop.callEvent(currencyAddEvent);
        } else {
            currencyAddEvent.setHandled(true);
        }

        if (currencyAddEvent.wasHandled()) {
            event.setHandled(true);
        } else {
            CurrencyAddEvent currencyResetEvent = new CurrencyAddEvent(
                    currencySubtractEvent.getAmount(),
                    event.getSender(),
                    world
            );
            ChestShop.callEvent(currencyResetEvent);
        }
    }

    public static class ProviderInfo {
        private final String name;
        private final String version;

        public ProviderInfo(String name, String version) {
            this.name = name;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getVersion() {
            return version;
        }
    }
}
