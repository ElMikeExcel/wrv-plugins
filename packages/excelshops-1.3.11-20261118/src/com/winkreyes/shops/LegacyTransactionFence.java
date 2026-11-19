// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import java.util.function.Supplier;
import org.bukkit.event.*;

/** Defense in depth for an accidentally unpaired installation; protection stays loaded. */
final class LegacyTransactionFence implements Listener {
    private final Supplier<LegacyShopBridge> bridge;
    LegacyTransactionFence(Supplier<LegacyShopBridge> bridge){this.bridge=bridge;}
    private void pre(PreTransactionEvent event){LegacyShopBridge b=bridge.get();if(b==null||!b.allowsPreparation(event))event.setCancelled(true);}
    private void transaction(TransactionEvent event){LegacyShopBridge b=bridge.get();if(b==null||!b.allowsCommit(event))event.setCancelled(true);}
    @EventHandler(priority=EventPriority.LOWEST) public void preFirst(PreTransactionEvent e){pre(e);}
    @EventHandler(priority=EventPriority.HIGHEST) public void preLast(PreTransactionEvent e){pre(e);}
    @EventHandler(priority=EventPriority.LOWEST) public void tradeFirst(TransactionEvent e){transaction(e);}
    @EventHandler(priority=EventPriority.HIGHEST) public void tradeLast(TransactionEvent e){transaction(e);}
    @EventHandler(priority=EventPriority.LOWEST) public void transfer(CurrencyTransferEvent e){
        LegacyShopBridge b=bridge.get();if(b==null||!b.allowsCommit(e.getTransactionEvent()))e.setHandled(true);
    }
}
