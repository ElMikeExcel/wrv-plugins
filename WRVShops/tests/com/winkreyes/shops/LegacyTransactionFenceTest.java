// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LegacyTransactionFenceTest {
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void missingBridgeCancelsPreparationAtBothBoundaries(boolean first) {
        LegacyTransactionFence fence = new LegacyTransactionFence(() -> null);
        PreTransactionEvent event = mock(PreTransactionEvent.class);
        if (first) fence.preFirst(event); else fence.preLast(event);
        verify(event).setCancelled(true);
    }
    @ParameterizedTest @ValueSource(booleans = {true, false})
    void missingBridgeCancelsCommitAtBothBoundaries(boolean first) {
        LegacyTransactionFence fence = new LegacyTransactionFence(() -> null);
        TransactionEvent event = mock(TransactionEvent.class);
        if (first) fence.tradeFirst(event); else fence.tradeLast(event);
        verify(event).setCancelled(true);
    }
    @Test void missingBridgeSuppressesCurrencyTransfer() {
        CurrencyTransferEvent event = mock(CurrencyTransferEvent.class);
        new LegacyTransactionFence(() -> null).transfer(event);
        verify(event).setHandled(true);
    }
    @Test void unapprovedPreparationCommitAndMoneyAreRejected() {
        LegacyShopBridge bridge = mock(LegacyShopBridge.class); LegacyTransactionFence fence = new LegacyTransactionFence(() -> bridge);
        PreTransactionEvent pre = mock(PreTransactionEvent.class); TransactionEvent trade = mock(TransactionEvent.class);
        CurrencyTransferEvent money = mock(CurrencyTransferEvent.class); when(money.getTransactionEvent()).thenReturn(trade);
        fence.preFirst(pre); fence.preLast(pre); fence.tradeFirst(trade); fence.tradeLast(trade); fence.transfer(money);
        verify(pre, times(2)).setCancelled(true); verify(trade, times(2)).setCancelled(true); verify(money).setHandled(true);
    }
    @Test void admittedEventsArePreservedWithoutUndoingOtherPluginsCancellation() {
        LegacyShopBridge bridge = mock(LegacyShopBridge.class); LegacyTransactionFence fence = new LegacyTransactionFence(() -> bridge);
        PreTransactionEvent pre = mock(PreTransactionEvent.class); TransactionEvent trade = mock(TransactionEvent.class);
        CurrencyTransferEvent money = mock(CurrencyTransferEvent.class); when(money.getTransactionEvent()).thenReturn(trade);
        when(bridge.allowsPreparation(pre)).thenReturn(true); when(bridge.allowsCommit(trade)).thenReturn(true);
        fence.preFirst(pre); fence.preLast(pre); fence.tradeFirst(trade); fence.tradeLast(trade); fence.transfer(money);
        verify(pre, never()).setCancelled(anyBoolean()); verify(trade, never()).setCancelled(anyBoolean()); verify(money, never()).setHandled(anyBoolean());
    }
    @Test void fenceUsesFailClosedEventPrioritiesAndStillSeesCancelledEvents() throws Exception {
        for (String name : new String[]{"preFirst", "preLast", "tradeFirst", "tradeLast", "transfer"}) {
            Class<?> event = name.startsWith("pre") ? PreTransactionEvent.class : name.startsWith("trade") ? TransactionEvent.class : CurrencyTransferEvent.class;
            EventHandler handler = LegacyTransactionFence.class.getMethod(name, event).getAnnotation(EventHandler.class);
            assertNotNull(handler); assertFalse(handler.ignoreCancelled());
            assertEquals(name.endsWith("Last") ? EventPriority.HIGHEST : EventPriority.LOWEST, handler.priority());
        }
    }
}
