package com.Acrobot.ChestShop.Confirmation;

import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.Economy.*;
import com.Acrobot.ChestShop.Listeners.Economy.EconomyAdapter;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Listeners.Economy.TaxModule;
import com.Acrobot.ChestShop.Listeners.Economy.Plugins.VaultListener;
import com.Acrobot.ChestShop.Listeners.Player.PlayerInteract;
import com.Acrobot.ChestShop.Listeners.PreTransaction.PermissionChecker;
import net.milkbowl.vault.economy.Economy;
import com.Acrobot.ChestShop.Listeners.Modules.StockCounterModule;
import com.Acrobot.ChestShop.Listeners.PostTransaction.EconomicModule;
import com.Acrobot.ChestShop.Listeners.PostTransaction.ItemManager;
import com.Acrobot.ChestShop.Listeners.PostTransaction.TransactionMessageSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ConfirmationGateTest {
    MockedStatic<Bukkit> bukkit;
    MockedStatic<ChestShop> chestShop;
    MockedStatic<ConfirmationGate> gate;
    MockedStatic<InventoryUtil> inventoryUtil;
    MockedStatic<VaultListener> vault;
    MockedStatic<PlayerInteract> playerInteract;
    MockedStatic<PermissionChecker> permissionChecker;
    Economy provider;
    RegisteredServiceProvider<Economy> providerRegistration;
    World world;
    Block signBlock;
    Sign liveSign;
    Plugin owner;
    ConfirmationGate.Controller controller;
    Player player;
    Inventory shop, customer;
    TestItem shopItem, clientItem;
    Sign sign;
    Consumer<TransactionEvent> dispatch = ignored -> {};

    @BeforeEach void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        bukkit.when(Bukkit::getCurrentTick).thenReturn(100);
        PluginManager manager = mock(PluginManager.class);
        bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
        owner = mock(Plugin.class);
        when(owner.getName()).thenReturn("ExcelShops");
        when(owner.getLogger()).thenReturn(java.util.logging.Logger.getLogger("ConfirmationGateTest"));
        when(owner.isEnabled()).thenReturn(true);
        when(manager.getPlugin("ExcelShops")).thenReturn(owner);
        chestShop = mockStatic(ChestShop.class);
        ChestShop plugin = mock(ChestShop.class);
        when(plugin.isEnabled()).thenReturn(true);
        chestShop.when(ChestShop::getPlugin).thenReturn(plugin);
        chestShop.when(() -> ChestShop.callEvent(any())).thenAnswer(invocation -> {
            if (invocation.getArgument(0) instanceof TransactionEvent event) dispatch.accept(event);
            return null;
        });
        gate = mockStatic(ConfirmationGate.class, CALLS_REAL_METHODS);
        gate.when(ConfirmationGate::unavailableReason).thenReturn(null);
        inventoryUtil = mockStatic(InventoryUtil.class);
        inventoryUtil.when(() -> InventoryUtil.hasItems(any(), any())).thenReturn(true);
        inventoryUtil.when(() -> InventoryUtil.fits(any(ItemStack[].class), any(Inventory.class))).thenReturn(true);
        provider = mock(Economy.class); when(provider.isEnabled()).thenReturn(true);
        ServicesManager services = mock(ServicesManager.class);
        providerRegistration = mock(RegisteredServiceProvider.class);
        when(providerRegistration.getProvider()).thenReturn(provider);
        when(services.getRegistration(Economy.class)).thenReturn(providerRegistration);
        bukkit.when(Bukkit::getServicesManager).thenReturn(services);
        vault = mockStatic(VaultListener.class);
        vault.when(VaultListener::getProvider).thenReturn(provider);
        playerInteract = mockStatic(PlayerInteract.class);
        playerInteract.when(() -> PlayerInteract.canPrepareTrade(any(), any())).thenReturn(true);
        permissionChecker = mockStatic(PermissionChecker.class);
        permissionChecker.when(() -> PermissionChecker.hasTradePermission(any(), any(), any(), any())).thenReturn(true);
        controller = ConfirmationGate.register(owner, (p, s) -> {});
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        world = mock(World.class); when(world.getUID()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true); when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 1, 64, 1));
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        shop = mock(Inventory.class); customer = mock(Inventory.class); sign = mock(Sign.class);
        shopItem = new TestItem(10); clientItem = new TestItem(0);
        when(shop.getContents()).thenAnswer(i -> new ItemStack[]{shopItem});
        when(customer.getContents()).thenAnswer(i -> new ItemStack[]{clientItem});
        when(shop.getStorageContents()).thenAnswer(i -> new ItemStack[]{shopItem});
        when(customer.getStorageContents()).thenAnswer(i -> new ItemStack[]{clientItem});
        signBlock = mock(Block.class); liveSign = mock(Sign.class);
        when(signBlock.getState()).thenReturn(liveSign);
        for (Sign candidate : new Sign[]{sign, liveSign}) {
            when(candidate.getBlock()).thenReturn(signBlock);
            when(candidate.getLocation()).thenReturn(new Location(world, 1, 64, 1));
            when(candidate.getWorld()).thenReturn(world);
            BlockData data = mock(BlockData.class); when(data.getAsString()).thenReturn("minecraft:oak_wall_sign[facing=north]");
            when(candidate.getBlockData()).thenReturn(data);
            when(candidate.getLines()).thenReturn(new String[]{"Owner", "1", "B 5 : 2 S", "Stone"});
        }
        Properties.TAX_AMOUNT = 0; Properties.SERVER_TAX_AMOUNT = 0;
    }
    @AfterEach void cleanUp() {
        if (controller != null) controller.close();
        if (permissionChecker != null) permissionChecker.close();
        if (playerInteract != null) playerInteract.close();
        if (vault != null) vault.close();
        if (inventoryUtil != null) inventoryUtil.close();
        if (gate != null) gate.close();
        if (chestShop != null) chestShop.close();
        if (bukkit != null) bukkit.close();
    }
    TransactionEvent event() {
        return new TransactionEvent(TransactionEvent.TransactionType.BUY, shop, customer, player,
                new Account("Owner", UUID.randomUUID()), new ItemStack[]{new TestItem(1)}, new BigDecimal("5"), sign);
    }
    // Test-only reflective access exercises the capability state machine independently
    // of Bukkit sign/economy preview integration, which is covered by the Paper harness.
    ConfirmationGate.PreparedTrade prepare(TransactionEvent event) throws Exception {
        Constructor<ConfirmationGate.PreparedTrade> c = ConfirmationGate.PreparedTrade.class
                .getDeclaredConstructor(ConfirmationGate.Controller.class, TransactionEvent.class);
        c.setAccessible(true); return c.newInstance(controller, event);
    }
    CurrencyTransferEvent money(TransactionEvent event) {
        assertTrue(ConfirmationGate.beginMoney(event));
        CurrencyTransferEvent transfer = new CurrencyTransferEvent(event.getExactPrice(), player,
                event.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, event);
        ConfirmationGate.bindTransfer(event, transfer);
        assertTrue(ConfirmationGate.beginTax(transfer));
        assertTrue(ConfirmationGate.beginTransfer(transfer));
        ConfirmationGate.finishMoney(event, true);
        return transfer;
    }
    void items(TransactionEvent event) {
        assertTrue(ConfirmationGate.beginItems(event));
        shopItem.setAmount(9); clientItem.setAmount(1);
        ConfirmationGate.finishItems(event, 0);
    }
    @Test void rawPreTransactionCannotRunValidationOrBeUncancelled() {
        PreTransactionEvent event = new PreTransactionEvent(shop, customer, new ItemStack[]{new TestItem(1)},
                BigDecimal.ONE, player, new Account("Owner", UUID.randomUUID()), sign, TransactionEvent.TransactionType.BUY);
        assertTrue(event.isCancelled()); event.setCancelled(false); assertTrue(event.isCancelled());
        assertFalse(ConfirmationGate.isPrepared(event));
    }
    @Test void runtimeHandshakeMethodsIdentifyInstalledGuard() {
        assertEquals(1, ConfirmationGate.apiVersion());
        assertEquals("chestshop474-confirmation.1", ConfirmationGate.buildId());
    }
    @Test void rawTransactionCannotBeUncancelled() {
        TransactionEvent event = event();
        assertTrue(event.isCancelled()); event.setCancelled(false); assertTrue(event.isCancelled());
        assertFalse(ConfirmationGate.beginMoney(event)); assertFalse(ConfirmationGate.beginItems(event));
    }
    @Test void directPublicMutationHandlersDenyWithoutPermit() {
        TransactionEvent event = event();
        EconomicModule.onBuyTransaction(event);
        ItemManager.shopItemRemover(event); ItemManager.inventoryItemRemover(event);
        StockCounterModule.onTransaction(event);
        CurrencyTransferEvent transfer = new CurrencyTransferEvent(BigDecimal.TEN, player,
                event.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, event);
        TaxModule.onCurrencyTransfer(transfer);
        assertFalse(transfer.wasHandled()); assertFalse(ConfirmationGate.beginTransfer(transfer));
        assertEquals(10, shopItem.getAmount()); assertEquals(0, clientItem.getAmount());
        chestShop.verify(() -> ChestShop.callEvent(any()), never());
        verify(sign, never()).update(anyBoolean());
    }
    @Test void noParentCurrencyTransferIsDenied() {
        CurrencyTransferEvent transfer = new CurrencyTransferEvent(BigDecimal.TEN, player, UUID.randomUUID(), CurrencyTransferEvent.Direction.PARTNER);
        assertFalse(ConfirmationGate.beginTax(transfer)); assertFalse(ConfirmationGate.beginTransfer(transfer));
    }
    @Test void successfulReceiptSurvivesPermitRetirementAndCannotReplay() throws Exception {
        TransactionEvent event = event(); var prepared = prepare(event);
        dispatch = e -> { assertFalse(e.isCancelled()); money(e); items(e); };
        var result = controller.execute(prepared);
        assertTrue(result.succeeded(), result.toString()); assertTrue(event.isCancelled());
        assertFalse(controller.execute(prepared).admitted());
        assertEquals(9, shopItem.getAmount()); assertEquals(1, clientItem.getAmount());
    }
    @Test void closeDuringMoneyFinishesAdmittedCommitButDeniesNewAdmissions() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); controller.close(); assertFalse(controller.isActive()); items(e); };
        assertTrue(controller.execute(prepared).succeeded());
        assertFalse(controller.execute(prepare(event())).admitted());
    }
    @Test void disableDuringCommitDoesNotStrandAdmittedItems() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); when(owner.isEnabled()).thenReturn(false); items(e); };
        assertTrue(controller.execute(prepared).succeeded());
        assertFalse(controller.execute(prepare(event())).admitted());
    }
    @Test void disabledAdapterCannotAdmitNewTrade() throws Exception {
        var prepared = prepare(event()); when(owner.isEnabled()).thenReturn(false);
        assertFalse(controller.execute(prepared).admitted());
    }
    @Test void retiredControllerCannotAuthorizeNewControllerHandle() throws Exception {
        var prepared = prepare(event()); controller.close();
        controller = ConfirmationGate.register(owner, (p,s) -> {});
        assertFalse(controller.execute(prepared).admitted());
    }
    @Test void reentrantAndCopiedEventsAreDenied() throws Exception {
        var prepared = prepare(event()); var nested = prepare(event());
        dispatch = e -> {
            assertFalse(controller.execute(nested).admitted());
            TransactionEvent raw = event(); raw.setCancelled(false); assertTrue(raw.isCancelled());
            money(e); items(e);
        };
        assertTrue(controller.execute(prepared).succeeded());
    }
    @Test void moneyItemsAndTransferPhasesAreOneUse() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> {
            CurrencyTransferEvent transfer = money(e);
            assertFalse(ConfirmationGate.beginMoney(e));
            assertFalse(ConfirmationGate.beginTax(transfer));
            assertFalse(ConfirmationGate.beginTransfer(transfer));
            items(e); assertFalse(ConfirmationGate.beginItems(e));
        };
        assertTrue(controller.execute(prepared).succeeded());
    }
    @Test void copiedCurrencyEventDoesNotBorrowActivePermit() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> {
            money(e);
            CurrencyTransferEvent copied = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            assertFalse(ConfirmationGate.beginTax(copied)); assertFalse(ConfirmationGate.beginTransfer(copied));
            items(e);
        };
        assertTrue(controller.execute(prepared).succeeded());
    }
    @Test void stalePreparedHandleAndChangedItemsCannotBeAdmitted() throws Exception {
        var prepared = prepare(event()); bukkit.when(Bukkit::getCurrentTick).thenReturn(101);
        assertFalse(controller.execute(prepared).admitted());
        bukkit.when(Bukkit::getCurrentTick).thenReturn(100);
        var changed = prepare(event()); changed.event().getStock()[0].setAmount(2);
        assertFalse(controller.execute(changed).admitted());
    }
    @Test void failedMoneyNeverAllowsItems() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { assertTrue(ConfirmationGate.beginMoney(e)); ConfirmationGate.finishMoney(e, false);
            e.setCancelled(false); assertFalse(ConfirmationGate.beginItems(e)); };
        var result = controller.execute(prepared);
        assertFalse(result.succeeded()); assertFalse(result.itemsAttempted()); assertNotNull(result.error());
        assertEquals(10, shopItem.getAmount());
    }
    @Test void inventoryChangedDuringMoneyIsAmbiguousAndNeverMovesItems() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); shopItem.setAmount(8); assertFalse(ConfirmationGate.beginItems(e));
            e.setCancelled(false); assertTrue(e.isCancelled()); };
        var result = controller.execute(prepared);
        assertTrue(result.moneyHandled()); assertFalse(result.itemsAttempted()); assertFalse(result.succeeded());
        assertNotNull(result.error()); assertEquals(0, clientItem.getAmount());
    }
    @Test void leftoversAndWrongFinalDeltaCannotReportSuccess() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); assertTrue(ConfirmationGate.beginItems(e)); ConfirmationGate.finishItems(e, 1); };
        assertFalse(controller.execute(prepared).succeeded());
        var wrong = prepare(event());
        dispatch = e -> { money(e); assertTrue(ConfirmationGate.beginItems(e)); ConfirmationGate.finishItems(e, 0); };
        assertFalse(controller.execute(wrong).succeeded());
    }
    @Test void callbackChangesAfterItemPhaseAreDetected() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); items(e); clientItem.setAmount(2); };
        var result = controller.execute(prepared);
        assertFalse(result.succeeded()); assertNotNull(result.error());
    }
    @Test void callbackErrorProducesImmutablePartialReceiptAndRetiresPermit() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); throw new IllegalStateException("injected failure"); };
        var result = controller.execute(prepared);
        assertTrue(result.moneyHandled()); assertFalse(result.succeeded()); assertNotNull(result.error());
        assertTrue(prepared.event().isCancelled());
    }
    @Test void noStorageFitBeforeMoneyPreventsPayment() throws Exception {
        var prepared = prepare(event());
        inventoryUtil.when(() -> InventoryUtil.fits(any(ItemStack[].class), any(Inventory.class))).thenReturn(false);
        dispatch = e -> assertFalse(ConfirmationGate.beginMoney(e));
        var result = controller.execute(prepared);
        assertFalse(result.moneyAttempted()); assertFalse(result.succeeded());
    }
    @Test void changingTransferPriceOrPartnerCannotPay() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> {
            assertTrue(ConfirmationGate.beginMoney(e));
            CurrencyTransferEvent transfer = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            ConfirmationGate.bindTransfer(e, transfer);
            transfer.setAmountSent(BigDecimal.ONE);
            assertFalse(ConfirmationGate.beginTax(transfer)); assertFalse(ConfirmationGate.beginTransfer(transfer));
            ConfirmationGate.finishMoney(e, false);
        };
        assertFalse(controller.execute(prepared).succeeded());
    }
    @Test void wrongTypeItemHandlerDoesNotConsumeItemPermit() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { money(e); ItemManager.inventoryItemRemover(e); items(e); };
        assertTrue(controller.execute(prepared).succeeded());
    }
    @Test void freshSignEditInsideTransactionEventStopsBeforePayment() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { when(liveSign.getLines()).thenReturn(new String[]{"Owner", "1", "B 99", "Stone"});
            assertFalse(ConfirmationGate.beginMoney(e)); };
        var result = controller.execute(prepared);
        assertFalse(result.moneyAttempted()); assertFalse(result.succeeded());
    }
    @Test void signReplacementOrBlockDataChangeStopsBeforePayment() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { when(signBlock.getState()).thenReturn(mock(BlockState.class)); assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(prepared).moneyAttempted());
        when(signBlock.getState()).thenReturn(liveSign);
        var second = prepare(event());
        dispatch = e -> { when(liveSign.getBlockData().getAsString()).thenReturn("minecraft:oak_wall_sign[facing=south]");
            assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(second).moneyAttempted());
    }
    @Test void disconnectTeleportAndWorldChangeStopBeforePayment() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { when(player.isOnline()).thenReturn(false); assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(prepared).moneyAttempted()); when(player.isOnline()).thenReturn(true);
        var teleport = prepare(event());
        dispatch = e -> { when(player.getLocation()).thenReturn(new Location(world, 100, 64, 1)); assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(teleport).moneyAttempted()); when(player.getLocation()).thenReturn(new Location(world, 1, 64, 1));
        var changedWorld = prepare(event());
        dispatch = e -> { when(player.getWorld()).thenReturn(mock(World.class)); assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(changedWorld).moneyAttempted());
    }
    @Test void revokedAccessOrTradePermissionStopsBeforePayment() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> { playerInteract.when(() -> PlayerInteract.canPrepareTrade(any(), any())).thenReturn(false);
            assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(prepared).moneyAttempted());
        playerInteract.when(() -> PlayerInteract.canPrepareTrade(any(), any())).thenReturn(true);
        var deniedPermission = prepare(event());
        dispatch = e -> { permissionChecker.when(() -> PermissionChecker.hasTradePermission(any(), any(), any(), any())).thenReturn(false);
            assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(deniedPermission).moneyAttempted());
    }
    @Test void changedProviderBeforeMoneyOrDebitIsRefused() throws Exception {
        var prepared = prepare(event()); Economy replacement = mock(Economy.class);
        dispatch = e -> { vault.when(VaultListener::getProvider).thenReturn(replacement);
            when(providerRegistration.getProvider()).thenReturn(replacement); assertFalse(ConfirmationGate.beginMoney(e)); };
        assertFalse(controller.execute(prepared).moneyAttempted());
        vault.when(VaultListener::getProvider).thenReturn(provider); when(providerRegistration.getProvider()).thenReturn(provider);
        var beforeDebit = prepare(event());
        dispatch = e -> {
            assertTrue(ConfirmationGate.beginMoney(e));
            CurrencyTransferEvent transfer = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            ConfirmationGate.bindTransfer(e, transfer); assertTrue(ConfirmationGate.beginTax(transfer));
            vault.when(VaultListener::getProvider).thenReturn(replacement); when(providerRegistration.getProvider()).thenReturn(replacement);
            assertFalse(ConfirmationGate.beginTransfer(transfer));
        };
        assertFalse(controller.execute(beforeDebit).moneyHandled());
    }
    @Test void admittedProviderRemainsPinnedAfterDebitStarts() throws Exception {
        var prepared = prepare(event()); Economy replacement = mock(Economy.class);
        dispatch = e -> {
            assertTrue(ConfirmationGate.beginMoney(e));
            CurrencyTransferEvent transfer = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            ConfirmationGate.bindTransfer(e, transfer); assertTrue(ConfirmationGate.beginTax(transfer));
            assertTrue(ConfirmationGate.beginTransfer(transfer));
            vault.when(VaultListener::getProvider).thenReturn(replacement); when(providerRegistration.getProvider()).thenReturn(replacement);
            assertSame(provider, ConfirmationGate.economyForMutation(replacement));
            ConfirmationGate.finishMoney(e, true); items(e);
        };
        assertTrue(controller.execute(prepared).succeeded());
        assertSame(replacement, ConfirmationGate.economyForMutation(replacement));
    }
    @Test void receiptsAreDeferredUntilExplicitSuccessfulAcknowledgementAndOnlySentOnce() throws Exception {
        var prepared = prepare(event()); AtomicInteger receipts = new AtomicInteger();
        try (MockedStatic<TransactionMessageSender> messages = mockStatic(TransactionMessageSender.class, CALLS_REAL_METHODS)) {
            messages.when(() -> TransactionMessageSender.sendAcknowledged(any())).thenAnswer(i -> {
                assertTrue(ConfirmationGate.isAcknowledging(i.getArgument(0))); receipts.incrementAndGet(); return null;
            });
            dispatch = e -> { CurrencyTransferEvent transfer = money(e); TransactionMessageSender.onCurrencyTransfer(transfer);
                assertEquals(0, receipts.get()); assertFalse(controller.acknowledgeSuccess(prepared)); items(e); };
            assertTrue(controller.execute(prepared).succeeded()); assertEquals(0, receipts.get());
            assertTrue(controller.acknowledgeSuccess(prepared)); assertEquals(1, receipts.get());
            assertFalse(controller.acknowledgeSuccess(prepared)); assertEquals(1, receipts.get());
        }
    }
    @Test void failedTradeAndUnsolicitedReceiptCannotAcknowledge() throws Exception {
        var prepared = prepare(event()); dispatch = e -> { assertTrue(ConfirmationGate.beginMoney(e)); ConfirmationGate.finishMoney(e, false); };
        assertFalse(controller.execute(prepared).succeeded()); assertFalse(controller.acknowledgeSuccess(prepared));
        CurrencyTransferEvent raw = new CurrencyTransferEvent(BigDecimal.ONE, player, UUID.randomUUID(), CurrencyTransferEvent.Direction.PARTNER, event());
        raw.setHandled(true); TransactionMessageSender.onCurrencyTransfer(raw); TransactionMessageSender.sendAcknowledged(raw);
        assertFalse(ConfirmationGate.isAcknowledging(raw));
    }
    @Test void notificationFailureIsConsumedAndDoesNotChangeCompletedTrade() throws Exception {
        var prepared = prepare(event()); dispatch = e -> { money(e); items(e); };
        var completed = controller.execute(prepared); assertTrue(completed.succeeded());
        try (MockedStatic<TransactionMessageSender> messages = mockStatic(TransactionMessageSender.class)) {
            messages.when(() -> TransactionMessageSender.sendAcknowledged(any())).thenThrow(new IllegalStateException("injected receipt failure"));
            assertFalse(controller.acknowledgeSuccess(prepared)); assertFalse(controller.acknowledgeSuccess(prepared));
            messages.verify(() -> TransactionMessageSender.sendAcknowledged(any()), times(1));
        }
        assertTrue(completed.succeeded()); assertEquals(9, shopItem.getAmount()); assertEquals(1, clientItem.getAmount());
    }
    @Test void currencyCallbackTeleportIsRefusedBeforeActualDebit() throws Exception {
        var prepared = prepare(event());
        dispatch = e -> {
            assertTrue(ConfirmationGate.beginMoney(e));
            CurrencyTransferEvent transfer = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            ConfirmationGate.bindTransfer(e, transfer); assertTrue(ConfirmationGate.beginTax(transfer));
            when(player.getWorld()).thenReturn(mock(World.class)); assertFalse(ConfirmationGate.beginTransfer(transfer));
        };
        assertFalse(controller.execute(prepared).moneyHandled());
    }
    @Test void debitCallbackTeleportKeepsPayoutInOriginalWorld() throws Exception {
        assertTransferWorldPinned(false);
    }
    @Test void debitCallbackTeleportKeepsFailedPayoutRefundInOriginalWorld() throws Exception {
        assertTransferWorldPinned(true);
    }
    private void assertTransferWorldPinned(boolean failPayout) throws Exception {
        var prepared = prepare(event()); AtomicInteger adds = new AtomicInteger();
        chestShop.when(() -> ChestShop.callEvent(any())).thenAnswer(i -> {
            Object event = i.getArgument(0);
            if (event instanceof TransactionEvent transaction) dispatch.accept(transaction);
            else if (event instanceof CurrencySubtractEvent subtract) {
                assertSame(world, subtract.getWorld()); subtract.setHandled(true);
                when(player.getWorld()).thenReturn(mock(World.class));
            } else if (event instanceof CurrencyAddEvent add) {
                assertSame(world, add.getWorld());
                add.setHandled(!failPayout || adds.incrementAndGet() > 1);
            }
            return null;
        });
        dispatch = e -> {
            assertTrue(ConfirmationGate.beginMoney(e));
            CurrencyTransferEvent transfer = new CurrencyTransferEvent(e.getExactPrice(), player,
                    e.getOwnerAccount().getUuid(), CurrencyTransferEvent.Direction.PARTNER, e);
            ConfirmationGate.bindTransfer(e, transfer); assertTrue(ConfirmationGate.beginTax(transfer));
            new TestEconomyAdapter().transfer(transfer);
            ConfirmationGate.finishMoney(e, transfer.wasHandled());
            if (!failPayout) items(e);
        };
        var result = controller.execute(prepared);
        assertEquals(!failPayout, result.succeeded(), result.toString());
        if (failPayout) assertEquals(2, adds.get());
    }
    static final class TestEconomyAdapter extends EconomyAdapter {
        void transfer(CurrencyTransferEvent event) { processTransfer(event); }
        @Override public ProviderInfo getProviderInfo() { return null; }
        @Override public void onAmountCheck(CurrencyAmountEvent event) { }
        @Override public void onCurrencyCheck(CurrencyCheckEvent event) { }
        @Override public void onAccountCheck(AccountCheckEvent event) { }
        @Override public void onCurrencyFormat(CurrencyFormatEvent event) { }
        @Override public void onCurrencyAdd(CurrencyAddEvent event) { }
        @Override public void onCurrencySubtraction(CurrencySubtractEvent event) { }
        @Override public void onCurrencyTransfer(CurrencyTransferEvent event) { }
        @Override public void onCurrencyHoldCheck(CurrencyHoldEvent event) { }
    }
    static final class TestItem extends ItemStack {
        int amount;
        TestItem(int amount) { super(); this.amount = amount; }
        @Override public Material getType() { return Material.STONE; }
        @Override public int getAmount() { return amount; }
        @Override public void setAmount(int value) { amount = value; }
        @Override public TestItem clone() { return new TestItem(amount); }
        @Override public boolean isSimilar(ItemStack item) { return item instanceof TestItem; }
        @Override public boolean equals(Object item) { return item instanceof TestItem other && amount == other.amount; }
        @Override public int hashCode() { return Objects.hash(Material.STONE, amount); }
    }
}
