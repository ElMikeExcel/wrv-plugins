// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.Acrobot.Breeze.Utils.PriceUtil;
import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.ChestShop.Confirmation.ConfirmationGate;
import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.Events.ItemParseEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.FileJournal;
import java.io.File;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Tests bridge orchestration with a mocked upstream guard/economy, not an installed plugin stack. */
class LegacyShopBridgeTest {
    private MockedStatic<Bukkit> bukkit;
    private MockedStatic<ConfirmationGate> gate;
    private MockedStatic<ChestShop> chestShop;
    private MockedStatic<ChestShopSign> signs;
    private MockedStatic<PriceUtil> prices;
    private MockedStatic<NameManager> names;
    private MockedConstruction<FileJournal> journals;
    private LegacyShopBridge bridge;
    private LegacyShopUi ui;
    private WRVShops plugin;
    private CheckoutService checkout;
    private Economy economy;
    private FileJournal journal;
    private ConfirmationGate.Controller controller;
    private ConfirmationGate.PreparedTrade prepared;
    private TransactionEvent event;
    private Player player;
    private Sign sign;
    private ItemStack stock;
    private Inventory ownerInventory, clientInventory;
    private OfflinePlayer clientAccount, ownerAccount;
    private final UUID clientId = UUID.fromString("00000000-0000-0000-0000-000000000123");
    private final UUID ownerId = UUID.fromString("00000000-0000-0000-0000-000000000456");
    private final AtomicReference<ConfirmationGate.OfferHandler> handler = new AtomicReference<>();
    private final AtomicReference<BigDecimal> price = new AtomicReference<>(new BigDecimal("10.00"));
    private final AtomicReference<Double> clientBalance = new AtomicReference<>(100.0), ownerBalance = new AtomicReference<>(50.0);
    private final AtomicReference<LegacyTrading.Direction> direction = new AtomicReference<>(LegacyTrading.Direction.BUY);

    @BeforeEach void setup() throws Exception {
        plugin = mock(WRVShops.class); economy = mock(Economy.class); checkout = mock(CheckoutService.class); ui = mock(LegacyShopUi.class);
        Server server = mock(Server.class); ServicesManager services = mock(ServicesManager.class);
        @SuppressWarnings("unchecked") RegisteredServiceProvider<Economy> registration = mock(RegisteredServiceProvider.class);
        when(server.getServicesManager()).thenReturn(services); when(services.getRegistration(Economy.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(economy); when(economy.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server); when(plugin.isEnabled()).thenReturn(true); when(plugin.getCheckout()).thenReturn(checkout);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegacyShopBridgeTest")); when(plugin.getDataFolder()).thenReturn(new File("unused-bridge-fixture"));
        when(checkout.tryExternalLease()).thenAnswer(call -> mock(CheckoutService.ExternalLease.class));
        BukkitScheduler scheduler = mock(BukkitScheduler.class); when(server.getScheduler()).thenReturn(scheduler);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> { ((Runnable) call.getArgument(1)).run(); return mock(BukkitTask.class); });
        bukkit = mockStatic(Bukkit.class); bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
        controller = mock(ConfirmationGate.Controller.class); when(controller.isActive()).thenReturn(true);
        gate = mockStatic(ConfirmationGate.class); gate.when(ConfirmationGate::apiVersion).thenReturn(1);
        gate.when(ConfirmationGate::buildId).thenReturn("chestshop474-confirmation.1");
        gate.when(() -> ConfirmationGate.register(eq(plugin), any())).thenAnswer(call -> { handler.set(call.getArgument(1)); return controller; });
        journals = mockConstruction(FileJournal.class);
        signs = mockStatic(ChestShopSign.class); prices = mockStatic(PriceUtil.class); names = mockStatic(NameManager.class);
        World world = mock(World.class); when(world.getUID()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000789")); when(world.getName()).thenReturn("world");
        sign = mock(Sign.class); Block block = mock(Block.class); BlockData data = mock(BlockData.class);
        when(sign.getWorld()).thenReturn(world); when(sign.getLocation()).thenAnswer(call -> new Location(world, 0, 64, 0));
        when(sign.getY()).thenReturn(64); when(sign.getBlockData()).thenReturn(data); when(data.getAsString()).thenReturn("minecraft:oak_wall_sign[facing=north]");
        when(sign.getLines()).thenReturn(new String[]{"Owner", "8", "B 10:S 5", "STONE"});
        when(world.getBlockAt(0, 64, 0)).thenReturn(block); when(world.getBlockAt(any(Location.class))).thenReturn(block); when(block.getState()).thenReturn(sign); when(sign.getBlock()).thenReturn(block);
        signs.when(() -> ChestShopSign.isValid(sign)).thenReturn(true); signs.when(() -> ChestShopSign.getItem(sign)).thenReturn("STONE");
        signs.when(() -> ChestShopSign.getPrice(sign)).thenReturn("B 10:S 5"); signs.when(() -> ChestShopSign.getOwner(sign)).thenReturn("Owner"); signs.when(() -> ChestShopSign.getQuantity(sign)).thenReturn(8);
        prices.when(() -> PriceUtil.getExactBuyPrice("B 10:S 5")).thenReturn(new BigDecimal("10")); prices.when(() -> PriceUtil.getExactSellPrice("B 10:S 5")).thenReturn(new BigDecimal("5"));
        player = mock(Player.class); when(player.getUniqueId()).thenReturn(clientId); when(player.isOnline()).thenReturn(true);
        when(player.hasPermission("wrvshops.use")).thenReturn(true); when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenAnswer(call -> new Location(world, .5, 64, 1));
        clientAccount = mock(OfflinePlayer.class); ownerAccount = mock(OfflinePlayer.class);
        when(server.getOfflinePlayer(clientId)).thenReturn(clientAccount); when(server.getOfflinePlayer(ownerId)).thenReturn(ownerAccount);
        when(economy.getBalance(clientAccount, "world")).thenAnswer(call -> clientBalance.get());
        when(economy.getBalance(ownerAccount, "world")).thenAnswer(call -> ownerBalance.get());
        stock = mock(ItemStack.class); when(stock.clone()).thenReturn(stock); when(stock.getType()).thenReturn(Material.STONE); when(stock.getAmount()).thenReturn(8); when(stock.isSimilar(stock)).thenReturn(true);
        when(stock.serialize()).thenReturn(Map.of("type", "STONE", "amount", 8));
        chestShop = mockStatic(ChestShop.class); chestShop.when(() -> ChestShop.callEvent(any())).thenAnswer(call -> {
            if (call.getArgument(0) instanceof ItemParseEvent parse) parse.setItem(stock);
            return null;
        });
        ownerInventory = mock(Inventory.class); clientInventory = mock(Inventory.class);
        when(ownerInventory.getStorageContents()).thenReturn(new ItemStack[]{stock}); when(clientInventory.getStorageContents()).thenReturn(new ItemStack[36]);
        event = mock(TransactionEvent.class); Account owner = mock(Account.class); when(owner.getUuid()).thenReturn(ownerId);
        when(event.getOwnerAccount()).thenReturn(owner); when(event.getClient()).thenReturn(player); when(event.getSign()).thenReturn(sign);
        when(event.getStock()).thenReturn(new ItemStack[]{stock}); when(event.getExactPrice()).thenAnswer(call -> price.get());
        when(event.getOwnerInventory()).thenReturn(ownerInventory); when(event.getClientInventory()).thenReturn(clientInventory);
        when(event.getTransactionType()).thenAnswer(call -> direction.get() == LegacyTrading.Direction.BUY ? TransactionEvent.TransactionType.BUY : TransactionEvent.TransactionType.SELL);
        prepared = mock(ConfirmationGate.PreparedTrade.class); when(prepared.event()).thenReturn(event);
        when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> {
            direction.set(call.getArgument(2) == TransactionEvent.TransactionType.BUY ? LegacyTrading.Direction.BUY : LegacyTrading.Direction.SELL);
            return prepared;
        });
        when(controller.execute(prepared)).thenAnswer(call -> {
            double amount = price.get().doubleValue(), delta = direction.get() == LegacyTrading.Direction.BUY ? -amount : amount;
            clientBalance.set(clientBalance.get() + delta); ownerBalance.set(ownerBalance.get() - delta);
            return new ConfirmationGate.CommitResult(true, true, true, true, true, false, null);
        });
        bridge = new LegacyShopBridge(plugin, economy, 60); bridge.setUi(ui); journal = journals.constructed().getFirst();
    }
    @AfterEach void cleanup() throws Exception {
        if (bridge != null) bridge.close(false);
        if (names != null) names.close(); if (prices != null) prices.close(); if (signs != null) signs.close();
        if (chestShop != null) chestShop.close(); if (journals != null) journals.close(); if (gate != null) gate.close(); if (bukkit != null) bukkit.close();
    }
    private LegacyTrading.Offer offer() {
        handler.get().offer(player, sign); ArgumentCaptor<LegacyTrading.Offer> capture = ArgumentCaptor.forClass(LegacyTrading.Offer.class);
        verify(ui, atLeastOnce()).showOffer(eq(player), capture.capture()); return capture.getValue();
    }
    private LegacyTrading.Quote quote() throws Exception { return bridge.quote(player, offer().id(), LegacyTrading.Direction.BUY, 8); }

    @Test void openingSignDoesNotPrepareTradeOrJournalAnIntent() throws Exception {
        LegacyTrading.Offer offer = offer(); assertTrue(offer.canBuy()); assertTrue(offer.canSell()); assertEquals(8, offer.bundleQuantity());
        verify(controller, never()).prepare(any(), any(), any(), anyInt()); verify(controller, never()).execute(any()); verify(journal, never()).record(any(), any(), anyString());
    }
    @ParameterizedTest @EnumSource(LegacyTrading.Direction.class)
    void chosenDirectionQuotesThenCommitsOnceWithDurableIntentBeforeExecution(LegacyTrading.Direction direction) throws Exception {
        LegacyTrading.Quote quote = bridge.quote(player, offer().id(), direction, 8);
        verify(controller, never()).execute(any()); verify(journal, never()).record(any(), any(), anyString());
        LegacyTrading.Outcome outcome = bridge.confirm(player, quote.id()); assertTrue(outcome.success()); assertFalse(outcome.recoveryRequired());
        var order = inOrder(journal, controller); order.verify(journal).record(any(), eq(CheckoutService.Stage.LEGACY_INTENT), anyString());
        order.verify(controller).execute(prepared); order.verify(journal).record(any(), eq(CheckoutService.Stage.LEGACY_COMMITTED), anyString());
        order.verify(controller).acknowledgeSuccess(prepared);
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, times(1)).execute(any());
    }
    @Test void failedReceiptDoesNotUndoOrReplayCompletedTrade() throws Exception {
        LegacyTrading.Quote quote=quote();doThrow(new IllegalStateException("Receipt unavailable")).when(controller).acknowledgeSuccess(prepared);
        assertTrue(bridge.confirm(player,quote.id()).success());verify(checkout,never()).pauseExternal(anyString());
        assertFalse(bridge.confirm(player,quote.id()).success());verify(controller,times(1)).execute(prepared);
    }
    @Test void failedDurableCompletionNeverAcknowledgesSuccess() throws Exception {
        LegacyTrading.Quote quote=quote();doThrow(new java.io.IOException("Disk error")).when(journal).record(any(),eq(CheckoutService.Stage.LEGACY_COMMITTED),anyString());
        assertTrue(bridge.confirm(player,quote.id()).recoveryRequired());verify(controller,never()).acknowledgeSuccess(any());
    }
    @Test void freeTradeKeepsZeroServerTotalAndStillRequiresFinalConfirmation() throws Exception {
        price.set(BigDecimal.ZERO); LegacyTrading.Quote quote = quote(); assertEquals(0, quote.totalCents()); assertTrue(quote.description().contains("$0.00"));
        verify(controller, never()).execute(any()); assertTrue(bridge.confirm(player, quote.id()).success());
        ArgumentCaptor<CheckoutService.Quote> audit = ArgumentCaptor.forClass(CheckoutService.Quote.class);
        verify(journal).record(audit.capture(), eq(CheckoutService.Stage.LEGACY_INTENT), anyString()); assertEquals(0, audit.getValue().amountMinorUnits());
    }
    @Test void cancelledQuoteCannotCommit() throws Exception {
        LegacyTrading.Quote quote = quote(); bridge.cancel(clientId); assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, never()).execute(any());
    }
    @Test void newOfferInvalidatesOldQuote() throws Exception {
        LegacyTrading.Quote quote = quote(); offer(); assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, never()).execute(any());
    }
    @Test void changedPriceRejectsBeforeIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); price.set(new BigDecimal("11.00")); assertFalse(bridge.confirm(player, quote.id()).success());
        verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @Test void changedStockRejectsBeforeIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); when(ownerInventory.getStorageContents()).thenReturn(new ItemStack[27]);
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @Test void changedBalanceRejectsBeforeIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); clientBalance.set(110.0); assertFalse(bridge.confirm(player, quote.id()).success());
        verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @ParameterizedTest @ValueSource(strings = {"-1", "0.001", "100000000.01"})
    void invalidPriceIsRejectedDuringQuote(String value) {
        LegacyTrading.Offer offer = offer(); price.set(new BigDecimal(value));
        assertThrows(Exception.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8)); verify(controller, never()).execute(any());
    }
    @Test void cancellationDuringQuotePreparationPreventsIssuingAuthority() {
        LegacyTrading.Offer offer = offer(); when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> { bridge.cancel(clientId); return prepared; });
        assertThrows(IllegalArgumentException.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8)); verify(controller, never()).execute(any());
    }
    @Test void cancellationDuringFinalPreparationPreventsIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> { bridge.cancel(clientId); return prepared; });
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @Test void newerOfferDuringFinalPreparationPreventsOldIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> { offer(); return prepared; });
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @Test void disconnectDuringFinalPreparationPreventsIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote(); when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> { when(player.isOnline()).thenReturn(false); return prepared; });
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }
    @Test void ambiguousExecutionRequiresRecoveryAndCannotReplay() throws Exception {
        LegacyTrading.Quote quote = quote(); doReturn(new ConfirmationGate.CommitResult(true, true, false, false, false, true, "Unknown debit outcome")).when(controller).execute(prepared);
        LegacyTrading.Outcome outcome = bridge.confirm(player, quote.id()); assertFalse(outcome.success()); assertTrue(outcome.recoveryRequired());
        verify(checkout).pauseExternal(anyString()); verify(journal).record(any(), eq(CheckoutService.Stage.RECOVERY_REQUIRED), anyString()); verify(controller,never()).acknowledgeSuccess(any());
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, times(1)).execute(any());
    }
    @Test void oversizedFailureEvidenceStillLatchesGlobalRecovery() throws Exception {
        LegacyTrading.Quote quote=quote();doReturn(new ConfirmationGate.CommitResult(true,true,false,false,false,true,"x".repeat(CheckoutService.MAX_EVIDENCE_BYTES+1))).when(controller).execute(prepared);
        assertTrue(bridge.confirm(player,quote.id()).recoveryRequired());
        ArgumentCaptor<String> reason=ArgumentCaptor.forClass(String.class);verify(checkout).pauseExternal(reason.capture());assertTrue(reason.getValue().length()<=2048);
        ArgumentCaptor<String> detail=ArgumentCaptor.forClass(String.class);verify(journal).record(any(),eq(CheckoutService.Stage.RECOVERY_REQUIRED),detail.capture());assertTrue(detail.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=CheckoutService.MAX_EVIDENCE_BYTES);
    }
    @Test void definiteUnchangedDenialRecordsNoChange() throws Exception {
        LegacyTrading.Quote quote = quote(); doReturn(new ConfirmationGate.CommitResult(false, false, false, false, false, true, "Unavailable")).when(controller).execute(prepared);
        LegacyTrading.Outcome outcome = bridge.confirm(player, quote.id()); assertFalse(outcome.success()); assertFalse(outcome.recoveryRequired());
        verify(journal).record(any(), eq(CheckoutService.Stage.LEGACY_NO_CHANGE), anyString()); verify(checkout, never()).pauseExternal(anyString());
    }
    @Test void pausedNativeCheckoutAlsoStopsLegacyQuotesAndConfirmations() throws Exception {
        LegacyTrading.Quote quote = quote(); when(checkout.recoveryRequired()).thenReturn(true); assertFalse(bridge.available());
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, never()).execute(any());
    }


    @Test void refusedNestedQuoteInvalidatesOlderInFlightQuoteAttempt() {
        LegacyTrading.Offer offer = offer();
        CheckoutService.ExternalLease lease = mock(CheckoutService.ExternalLease.class);
        when(checkout.tryExternalLease()).thenReturn(lease, null);
        when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> {
            assertThrows(IllegalStateException.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.SELL, 8));
            return prepared;
        });
        assertThrows(IllegalArgumentException.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8));
        verify(controller, never()).execute(any());
    }
    @Test void refusedNestedQuoteDuringFinalPreparationInvalidatesOlderConfirmation() throws Exception {
        LegacyTrading.Offer offer = offer(); LegacyTrading.Quote quote = bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8);
        CheckoutService.ExternalLease lease = mock(CheckoutService.ExternalLease.class);
        when(checkout.tryExternalLease()).thenReturn(lease, null);
        when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> {
            assertThrows(IllegalStateException.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.SELL, 8));
            return prepared;
        });
        assertFalse(bridge.confirm(player, quote.id()).success());
        verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }

    @Test void disabledCapturedEconomyPausesBeforeQuotePreparation() {
        LegacyTrading.Offer offer = offer(); when(economy.isEnabled()).thenReturn(false);
        assertFalse(bridge.available());
        assertThrows(IllegalStateException.class, () -> bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8));
        verify(controller, never()).prepare(any(), any(), any(), anyInt());
    }
    @Test void replacedVaultProviderPausesBeforeConfirmation() throws Exception {
        LegacyTrading.Quote quote = quote();
        @SuppressWarnings("unchecked") RegisteredServiceProvider<Economy> replacement = mock(RegisteredServiceProvider.class);
        Economy other = mock(Economy.class); when(other.isEnabled()).thenReturn(true); when(replacement.getProvider()).thenReturn(other);
        when(plugin.getServer().getServicesManager().getRegistration(Economy.class)).thenReturn(replacement);
        assertFalse(bridge.available()); assertFalse(bridge.confirm(player, quote.id()).success()); verify(controller, never()).execute(any());
    }
    @Test void anotherPlayerCannotUseQuoteAuthority() throws Exception {
        LegacyTrading.Quote quote = quote(); Player other = mock(Player.class); when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse(bridge.confirm(other, quote.id()).success()); verify(controller, never()).execute(any());
        assertTrue(bridge.confirm(player, quote.id()).success());
    }
    @Test void lostLocationDuringFinalPreparationPreventsIntentOrExecution() throws Exception {
        LegacyTrading.Quote quote = quote();
        when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> {
            when(player.getLocation()).thenAnswer(ignored -> new Location(player.getWorld(), 100, 64, 100));
            return prepared;
        });
        assertFalse(bridge.confirm(player, quote.id()).success()); verify(journal, never()).record(any(), any(), anyString()); verify(controller, never()).execute(any());
    }

    @Test void closeRevokesOffersQuotesAndController() throws Exception {
        LegacyTrading.Quote quote = quote(); bridge.close(true); assertFalse(bridge.available()); assertFalse(bridge.confirm(player, quote.id()).success());
        verify(controller).close(); verify(journal).close(); verify(controller, never()).execute(any());
    }
    @Test void preparationAllowanceIsScopedToExactPlayerSignAndMainThread() throws Exception {
        LegacyTrading.Offer offer = offer(); PreTransactionEvent pre = mock(PreTransactionEvent.class);
        when(pre.getClient()).thenReturn(player); when(pre.getSign()).thenReturn(sign); assertFalse(bridge.allowsPreparation(pre));
        when(controller.prepare(eq(sign), eq(player), any(), anyInt())).thenAnswer(call -> {
            assertTrue(bridge.allowsPreparation(pre)); bukkit.when(Bukkit::isPrimaryThread).thenReturn(false); assertFalse(bridge.allowsPreparation(pre));
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(true); return prepared;
        });
        bridge.quote(player, offer.id(), LegacyTrading.Direction.BUY, 8); assertFalse(bridge.allowsPreparation(pre));
    }
    @Test void commitAllowanceIsExactEventAndOnlyDuringExecute() throws Exception {
        LegacyTrading.Quote quote = quote(); assertFalse(bridge.allowsCommit(event));
        TransactionEvent other = mock(TransactionEvent.class);
        doAnswer(call -> {
            assertTrue(bridge.allowsCommit(event)); assertFalse(bridge.allowsCommit(other));
            bukkit.when(Bukkit::isPrimaryThread).thenReturn(false); assertFalse(bridge.allowsCommit(event)); bukkit.when(Bukkit::isPrimaryThread).thenReturn(true);
            return new ConfirmationGate.CommitResult(false, false, false, false, false, true, "Fixture denied");
        }).when(controller).execute(prepared);
        bridge.confirm(player, quote.id()); verify(controller).execute(prepared); assertFalse(bridge.allowsCommit(event));
    }
}
