// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.winkreyes.shops.checkout.CheckoutService;
import com.winkreyes.shops.checkout.Money;
import com.winkreyes.shops.store.SetupProposal;
import com.winkreyes.shops.store.ShopOffer;
import com.winkreyes.shops.store.ShopRecord;
import com.winkreyes.shops.store.ShopRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.fixtures.FormFixtures;
import org.geysermc.fixtures.GeyserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** API-level regressions: inventory identity and queued actions, not actual phone cursor rendering. */
class ShopUiTest {
    private WRVShops plugin;
    private ShopRegistry registry;
    private ShopUi ui;
    private Player player;
    private PluginManager plugins;
    private YamlConfiguration config;
    private CheckoutService checkout;
    private CheckoutService.Adapter economy;
    private CheckoutService.Transfer transfer;
    private ShopAdapter.Selection selection;
    private SetupProposal proposal;
    private Material stockMaterial;
    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000321");
    private final UUID sellerId = UUID.fromString("00000000-0000-0000-0000-000000000654");
    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private final AtomicReference<Inventory> opened = new AtomicReference<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<ItemStack> items;

    @BeforeEach void setup() throws Exception {
        FormFixtures.reset(); GeyserFixture.reset();
        plugin = mock(WRVShops.class); registry = mock(ShopRegistry.class); player = mock(Player.class);
        Server server = mock(Server.class); plugins = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        config = new YamlConfiguration(); config.set("bedrock-forms", false);
        when(plugin.getConfig()).thenReturn(config); when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server); when(server.getPluginManager()).thenReturn(plugins);
        when(server.getScheduler()).thenReturn(scheduler); when(plugin.getRegistry()).thenReturn(registry);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("ShopUiTest"));
        when(player.getUniqueId()).thenReturn(playerId); when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Buyer");
        economy = mock(CheckoutService.Adapter.class); transfer = mock(CheckoutService.Transfer.class);
        when(transfer.auditEvidence()).thenReturn("Fixture buyer and chest before/after snapshots");
        when(transfer.apply()).thenReturn(CheckoutService.MutationResult.APPLIED);
        when(economy.prepare(any())).thenReturn(CheckoutService.Preparation.ready(transfer));
        when(economy.withdraw(any(), anyLong())).thenReturn(CheckoutService.MoneyResult.SUCCESS);
        when(economy.deposit(any(), anyLong())).thenReturn(CheckoutService.MoneyResult.SUCCESS);
        checkout = spy(new CheckoutService(economy, mock(CheckoutService.Journal.class),
                Clock.fixed(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC), Duration.ofSeconds(60)));
        when(plugin.getCheckout()).thenReturn(checkout);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); return mock(BukkitTask.class);
        });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(call -> inventory(call.getArgument(0), call.getArgument(1)));
        bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
        items = mockConstruction(ItemStack.class, (stack, context) -> {
            Material type = context.arguments().isEmpty() ? Material.STONE : (Material) context.arguments().getFirst();
            configureItem(stack, type, 1, meta(null, List.of()));
        });
        opened.set(inventory(null, 9));
        when(player.getOpenInventory()).thenAnswer(call -> view(opened.get()));
        doAnswer(call -> {
            Inventory old = opened.getAndSet(call.getArgument(0)); closeEvent(old); return view(opened.get());
        }).when(player).openInventory(any(Inventory.class));
        doAnswer(call -> {
            Inventory old = opened.getAndSet(inventory(null, 9)); closeEvent(old); return null;
        }).when(player).closeInventory();
        // Paper Material.isAir() consults server registries, unavailable in an API-only JVM.
        // A non-air material fixture keeps this test about UI state rather than server bootstrap.
        stockMaterial = mock(Material.class); when(stockMaterial.isAir()).thenReturn(false);
        ui = new ShopUi(plugin); selection = selection(2304, 1, 125);
        proposal = mock(SetupProposal.class);
        when(proposal.shopId()).thenReturn(UUID.randomUUID());
        when(proposal.contents()).thenAnswer(call -> new ItemStack[]{item(stockMaterial, 8, meta("Custom treasure", List.of("Original lore")))});
        when(registry.findById(proposal.shopId())).thenReturn(Optional.empty());
        when(registry.commit(eq(player), eq(proposal), anyList())).thenReturn(mock(ShopRecord.class));
        SpawnCatalog catalog = mock(SpawnCatalog.class);
        when(plugin.getCatalog()).thenReturn(catalog); when(catalog.all()).thenReturn(List.of());
    }

    @AfterEach void cleanup() { if (items != null) items.close(); if (bukkit != null) bukkit.close(); }

    private static ItemMeta meta(String display, List<String> lore) {
        ItemMeta meta = mock(ItemMeta.class);
        AtomicReference<String> name = new AtomicReference<>(display);
        AtomicReference<List<String>> lines = new AtomicReference<>(List.copyOf(lore));
        when(meta.hasDisplayName()).thenAnswer(call -> name.get() != null);
        when(meta.getDisplayName()).thenAnswer(call -> name.get());
        doAnswer(call -> { name.set(call.getArgument(0)); return null; }).when(meta).setDisplayName(anyString());
        when(meta.hasLore()).thenAnswer(call -> !lines.get().isEmpty());
        when(meta.getLore()).thenAnswer(call -> new ArrayList<>(lines.get()));
        doAnswer(call -> { lines.set(List.copyOf(call.getArgument(0))); return null; }).when(meta).setLore(anyList());
        when(meta.getEnchants()).thenReturn(Map.of());
        when(meta.clone()).thenAnswer(call -> meta(name.get(), lines.get()));
        return meta;
    }

    private static ItemStack item(Material type, int amount, ItemMeta meta) {
        ItemStack item = mock(ItemStack.class); configureItem(item, type, amount, meta); return item;
    }
    private static void configureItem(ItemStack item, Material type, int initial, ItemMeta initialMeta) {
        AtomicReference<ItemMeta> meta = new AtomicReference<>(initialMeta);
        AtomicReference<Integer> amount = new AtomicReference<>(initial);
        when(item.getType()).thenReturn(type); when(item.getMaxStackSize()).thenReturn(64);
        when(item.getAmount()).thenAnswer(call -> amount.get());
        doAnswer(call -> { amount.set(call.getArgument(0)); return null; }).when(item).setAmount(anyInt());
        when(item.hasItemMeta()).thenReturn(true); when(item.getItemMeta()).thenAnswer(call -> meta.get().clone());
        when(item.setItemMeta(any())).thenAnswer(call -> { meta.set(call.getArgument(0)); return true; });
        when(item.clone()).thenAnswer(call -> item(type, amount.get(), meta.get().clone()));
        when(item.isSimilar(any())).thenAnswer(call -> {
            ItemStack other = call.getArgument(0);
            return other != null && other.getType() == type
                    && Objects.equals(meta.get().getDisplayName(), other.getItemMeta().getDisplayName())
                    && Objects.equals(meta.get().getLore(), other.getItemMeta().getLore());
        });
    }
    private ShopAdapter.Selection selection(int available, int bundle, long price) {
        return new ShopAdapter.Selection("spawn:treasure", 1, sellerId, "Custom treasure",
                item(stockMaterial, 1, meta("Custom treasure", List.of("Original lore"))),
                bundle, price, available, "trusted-stock-fingerprint", null);
    }
    private static Inventory inventory(InventoryHolder holder, int size) {
        Inventory inventory = mock(Inventory.class); Map<Integer, ItemStack> slots = new HashMap<>();
        when(inventory.getHolder()).thenReturn(holder); when(inventory.getSize()).thenReturn(size);
        doAnswer(call -> { slots.put(call.getArgument(0), call.getArgument(1)); return null; }).when(inventory).setItem(anyInt(), any());
        when(inventory.getItem(anyInt())).thenAnswer(call -> slots.get(call.getArgument(0)));
        return inventory;
    }
    private InventoryView view(Inventory inventory) {
        InventoryView view = mock(InventoryView.class); when(view.getTopInventory()).thenReturn(inventory); return view;
    }
    private void closeEvent(Inventory inventory) {
        if (ui == null) return;
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getInventory()).thenReturn(inventory); when(event.getPlayer()).thenReturn(player); ui.close(event);
    }
    private InventoryClickEvent click(Inventory inventory, int slot, ClickType type) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenAnswer(call -> view(inventory)); when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot); when(event.getClick()).thenReturn(type); ui.click(event); return event;
    }
    private void queue(int... slots) { for (int slot : slots) click(opened.get(), slot, ClickType.LEFT); }
    private void tap(int slot) { queue(slot); flush(); }
    private void flush() { while (!tasks.isEmpty()) tasks.removeFirst().run(); }
    private String lore(int slot) { return ChatColor.stripColor(String.join("\n", opened.get().getItem(slot).getItemMeta().getLore())); }
    private String title(int slot) { return ChatColor.stripColor(opened.get().getItem(slot).getItemMeta().getDisplayName()); }
    private void assertQuantity(int count) {
        String expected = "Quantity: " + count + (selection.bundleSize() > 1
                ? " bundles (" + count * selection.bundleSize() + " items)" : "");
        assertTrue(lore(13).lines().anyMatch(expected::equals), () -> "Expected " + expected + " in " + lore(13));
    }
    private void quantity(int count) { ui.quantity(player, selection, count); }
    private void confirmation(int count) { quantity(count); tap(31); tap(11); }
    private void setupPrice() { ui.offerSetup(player, proposal); tap(11); tap(0); }
    private void nativeForms() {
        config.set("bedrock-forms", true); Plugin geyser = mock(Plugin.class);
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(geyser); when(geyser.isEnabled()).thenReturn(true);
    }
    private void noMoneyMoved() throws Exception {
        verify(economy, never()).withdraw(any(), anyLong()); verify(economy, never()).deposit(any(), anyLong());
        verify(transfer, never()).apply();
    }
    private void sameOpenPage(Inventory inventory, InventoryHolder page) {
        assertSame(inventory, opened.get(), "Adjustments must retain the client inventory handle");
        assertSame(page, opened.get().getHolder(), "Adjustments must retain the session token");
        verify(player, never()).openInventory(any(Inventory.class)); verify(player, never()).closeInventory();
    }

    @ParameterizedTest @EnumSource(value = ClickType.class, names = {"LEFT", "RIGHT"})
    void queuedQuantityAdjustmentsKeepInventoryAndPageAndAccumulateEveryTap(ClickType type) throws Exception {
        quantity(10); Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder();
        clearInvocations(player);
        for (int slot : new int[]{24, 24, 25, 20, 26, 19}) click(inventory, slot, type);
        assertQuantity(10); // Event callbacks must wait for the scheduler.
        flush(); sameOpenPage(inventory, page);
        assertQuantity(75); assertTrue(lore(13).contains("$1.25 × 75 = $93.75"));
        assertEquals(1, lore(13).split("Original lore", -1).length - 1, "Renders must not append duplicate metadata");
        assertEquals(List.of("Original lore"), selection.item().getItemMeta().getLore()); noMoneyMoved();
    }

    @ParameterizedTest @CsvSource({"18, -64", "19, -8", "20, -1", "24, 1", "25, 8", "26, 64"})
    void everyQuantityButtonUsesCurrentValueAndClampsInPlace(int slot, int delta) throws Exception {
        quantity(100); Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(slot, slot); flush();
        int expected = Math.max(1, Math.min(2304, Math.max(1, 100 + delta) + delta));
        assertQuantity(expected); sameOpenPage(inventory, page); noMoneyMoved();
    }

    @ParameterizedTest @CsvSource({"7, 1, 125, 7", "2304, 64, 125, 36", "2304, 1, 3000000000, 3"})
    void quantityUpperBoundUsesStockBundleCapacityAndTotalLimit(int stock, int bundle, long price, int expected) throws Exception {
        selection = selection(stock, bundle, price); quantity(1);
        Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(26, 26, 26); flush();
        assertQuantity(expected); sameOpenPage(inventory, page); noMoneyMoved();
        queue(18, 18, 18); flush(); assertQuantity(1); sameOpenPage(inventory, page);
    }

    @Test void queuedReviewUsesLatestQuantityAndIgnoresOldPageWorkAfterTransition() throws Exception {
        quantity(1); Inventory old = opened.get();
        queue(24, 25, 31, 26, 31); flush();
        assertEquals(ShopUi.ViewKind.BUY_REVIEW, ui.currentKind(playerId));
        assertNotSame(old, opened.get()); assertTrue(lore(4).contains("10 × Custom treasure"));
        assertTrue(lore(4).contains("Total: $12.50"));
        verify(checkout, never()).quote(any(), any(), anyString(), anyLong(), anyInt(), anyLong(), anyString()); noMoneyMoved();
        tap(11);
        verify(checkout).quote(playerId, sellerId, selection.offerId(), 1, 10, 1250, selection.fingerprint());
        assertEquals(ShopUi.ViewKind.BUY_CONFIRM, ui.currentKind(playerId)); noMoneyMoved();
    }

    @Test void confirmationBackKeepsReviewedQuantityAndOldQueuedConfirmCannotCharge() throws Exception {
        confirmation(10); Inventory old = opened.get(); queue(13, 11); flush();
        assertEquals(ShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); assertQuantity(10);
        click(old, 11, ClickType.LEFT); flush(); noMoneyMoved();
        queue(24, 31); flush(); tap(11); tap(11);
        verify(economy).withdraw(playerId, 1375); verify(economy).deposit(sellerId, 1375); verify(transfer).apply();
    }

    @Test void duplicateAndReentrantFinalClicksChargeAndDeliverExactlyOnce() throws Exception {
        confirmation(10); Inventory old = opened.get(); noMoneyMoved();
        when(economy.withdraw(playerId, 1250)).thenAnswer(call -> {
            click(old, 11, ClickType.LEFT); flush(); return CheckoutService.MoneyResult.SUCCESS;
        });
        click(old, 11, ClickType.LEFT); click(old, 11, ClickType.RIGHT); flush();
        verify(checkout, times(1)).confirm(any(), eq(playerId));
        verify(economy, times(1)).withdraw(playerId, 1250); verify(economy, times(1)).deposit(sellerId, 1250);
        verify(transfer, times(1)).apply(); assertNull(ui.currentKind(playerId));
        verify(player).sendMessage(ChatColor.GREEN + "Bought 10 Custom treasure for $12.50.");
    }

    @Test void closingCurrentPageDiscardsQueuedQuantityClicks() throws Exception {
        quantity(10); queue(24, 31); player.closeInventory(); flush();
        assertNull(ui.currentKind(playerId)); verify(checkout).invalidateBuyer(playerId); noMoneyMoved();
    }

    @Test void cancellingCurrentPageDiscardsQueuedConfirmation() throws Exception {
        confirmation(10); queue(11); ui.cancel(player); flush();
        assertNull(ui.currentKind(playerId)); verify(checkout).invalidateBuyer(playerId); noMoneyMoved();
    }

    @Test void closingConfirmationRevokesQuoteAndDiscardsQueuedFinalAction() throws Exception {
        confirmation(10); queue(11); player.closeInventory(); flush();
        assertNull(ui.currentKind(playerId)); verify(checkout).invalidateBuyer(playerId); noMoneyMoved();
    }

    @Test void replacingSessionDiscardsOldQueuedButtonsAndLateClose() throws Exception {
        quantity(10); Inventory old = opened.get(); queue(24, 31); quantity(40);
        Inventory current = opened.get(); closeEvent(old); flush();
        assertSame(current, opened.get()); assertEquals(ShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        assertQuantity(40); noMoneyMoved();
    }

    @Test void disconnectDiscardsQueuedConfirmation() throws Exception {
        confirmation(10); queue(11); ui.disconnected(playerId); flush();
        assertNull(ui.currentKind(playerId)); verify(checkout).invalidateBuyer(playerId); noMoneyMoved();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void disableOrOfflinePlayerCannotExecuteQueuedConfirmation(boolean disable) throws Exception {
        confirmation(10); queue(11);
        if (disable) when(plugin.isEnabled()).thenReturn(false); else when(player.isOnline()).thenReturn(false);
        flush(); noMoneyMoved(); verify(checkout, never()).confirm(any(), any());
    }

    @Test void quantityBackAndCancelStayNonTransactional() throws Exception {
        quantity(10); queue(36, 31); flush(); assertEquals(ShopUi.ViewKind.CATALOG, ui.currentKind(playerId));
        quantity(10); queue(44, 31); flush(); assertNull(ui.currentKind(playerId)); noMoneyMoved();
    }

    @ParameterizedTest @EnumSource(value = ClickType.class, names = {"LEFT", "RIGHT"}, mode = EnumSource.Mode.EXCLUDE)
    void nonTapClicksCannotAdjustOrConfirm(ClickType type) throws Exception {
        quantity(10); InventoryClickEvent event = click(opened.get(), 24, type); flush();
        verify(event).setCancelled(true); assertQuantity(10);
        confirmation(10); click(opened.get(), 11, type); flush(); noMoneyMoved();
    }

    @ParameterizedTest @ValueSource(ints = {-999, -1, 45, 46, 80})
    void outsideAndBottomInventorySlotsAreCancelledWithoutActions(int slot) throws Exception {
        quantity(10); InventoryClickEvent event = click(opened.get(), slot, ClickType.LEFT); flush();
        verify(event).setCancelled(true); assertQuantity(10); noMoneyMoved();
    }

    @Test void dragIsCancelledWithoutChangingQuantity() throws Exception {
        quantity(10); InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenAnswer(call -> view(opened.get())); ui.drag(event);
        verify(event).setCancelled(true); assertQuantity(10); noMoneyMoved();
    }

    @Test void anotherPlayerCannotUseOwnersPage() throws Exception {
        quantity(10); Player other = mock(Player.class); when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenAnswer(call -> view(opened.get())); when(event.getWhoClicked()).thenReturn(other);
        when(event.getRawSlot()).thenReturn(24); when(event.getClick()).thenReturn(ClickType.LEFT);
        ui.click(event); flush(); verify(event).setCancelled(true); assertQuantity(10); noMoneyMoved();
    }

    @Test void queuedPriceAdjustmentsKeepInventoryAndPageAndAccumulateEveryTap() throws Exception {
        setupPrice(); Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(27, 27, 28, 29, 30, 22); assertTrue(lore(13).contains("Unit price: $1.00")); flush();
        sameOpenPage(inventory, page); assertTrue(lore(13).contains("Unit price: $12.11"));
        assertEquals("Use $12.11 per item", title(40));
        assertEquals(1, lore(13).split("Original lore", -1).length - 1);
        verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @ParameterizedTest @CsvSource({"18, -10000", "19, -1000", "20, -100", "21, -10", "22, -1", "27, 1", "28, 10", "29, 100", "30, 1000", "31, 10000"})
    void everyPriceButtonUsesCurrentValueAndClampsInPlace(int slot, long delta) throws Exception {
        setupPrice(); Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(slot, slot); flush(); long expected = Math.max(1, Math.min(10000000000L, Math.max(1, 100 + delta) + delta));
        assertTrue(lore(13).contains("Unit price: $" + Money.format(expected)));
        assertEquals("Use $" + Money.format(expected) + " per item", title(40)); sameOpenPage(inventory, page); noMoneyMoved();
    }

    @Test void maximumPriceRemainsClampedWithoutReopening() throws Exception {
        setupPrice(); assertTrue(ui.enterNumber(player, "100000000.00")); flush(); tap(0);
        Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(27, 28, 29, 30, 31); flush(); sameOpenPage(inventory, page);
        assertTrue(lore(13).contains("Unit price: $100000000.00"));
        queue(22, 22); flush(); assertTrue(lore(13).contains("Unit price: $99999999.98")); sameOpenPage(inventory, page);
        verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @Test void queuedPriceSaveUsesLatestValueThenReviewAndSeparateConfirmationPublishOnce() throws Exception {
        setupPrice(); Inventory oldPrice = opened.get(); queue(27, 28, 29, 40, 31, 40); flush();
        assertEquals(ShopUi.ViewKind.SETUP_ITEMS, ui.currentKind(playerId)); assertTrue(lore(0).contains("$2.11 each"));
        assertNotSame(oldPrice, opened.get()); verify(registry, never()).commit(any(), any(), anyList());
        tap(31); assertEquals(ShopUi.ViewKind.SETUP_REVIEW, ui.currentKind(playerId));
        assertTrue(lore(0).contains("$2.11 each")); assertTrue(lore(0).contains("Stock value: $16.88"));
        verify(registry, never()).commit(any(), any(), anyList());
        tap(49); assertEquals(ShopUi.ViewKind.SETUP_CONFIRM, ui.currentKind(playerId));
        verify(registry, never()).commit(any(), any(), anyList()); queue(49, 49); flush();
        @SuppressWarnings("unchecked") ArgumentCaptor<List<ShopOffer>> offers = ArgumentCaptor.forClass(List.class);
        verify(registry, times(1)).commit(eq(player), eq(proposal), offers.capture());
        assertEquals(1, offers.getValue().size()); assertEquals(211, offers.getValue().getFirst().unitPriceCents());
        assertNull(ui.currentKind(playerId)); noMoneyMoved();
    }

    @Test void priceBackDiscardsUnsavedAdjustmentAndReviewBackRetainsSavedPrice() throws Exception {
        setupPrice(); queue(29, 45, 40); flush();
        assertEquals(ShopUi.ViewKind.SETUP_ITEMS, ui.currentKind(playerId)); assertTrue(lore(0).contains("Price needed"));
        tap(31); assertEquals(ShopUi.ViewKind.SETUP_ITEMS, ui.currentKind(playerId));
        tap(0); assertTrue(lore(13).contains("Unit price: $1.00")); queue(29, 40); flush(); tap(31);
        assertEquals(ShopUi.ViewKind.SETUP_REVIEW, ui.currentKind(playerId)); tap(45); tap(0);
        assertTrue(lore(13).contains("Unit price: $2.00")); queue(30, 45); flush(); tap(0);
        assertTrue(lore(13).contains("Unit price: $2.00")); verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @Test void priceCancelDiscardsQueuedSaveAndOldPageCannotModifyNewDraft() throws Exception {
        setupPrice(); Inventory old = opened.get(); queue(29, 53, 40); flush(); assertNull(ui.currentKind(playerId));
        setupPrice(); click(old, 40, ClickType.LEFT); flush();
        assertEquals(ShopUi.ViewKind.SETUP_PRICE, ui.currentKind(playerId)); assertTrue(lore(13).contains("Unit price: $1.00"));
        verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @Test void closingPriceDiscardsQueuedSaveWithoutPublishing() throws Exception {
        setupPrice(); queue(29, 40); player.closeInventory(); flush();
        assertNull(ui.currentKind(playerId)); verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @ParameterizedTest @ValueSource(booleans = {true, false})
    void setupReviewOrFinalCancelCannotPublishQueuedContinuation(boolean finalPage) throws Exception {
        setupPrice(); tap(40); tap(31); if (finalPage) tap(49);
        queue(53, 49); flush(); assertNull(ui.currentKind(playerId));
        verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }


    @ParameterizedTest @ValueSource(strings = {"", "0", "-1", "1.5", "2305", "2147483648", "two"})
    void invalidQuantityCommandRetainsLatestAdjustedValue(String input) throws Exception {
        quantity(10); queue(24, 25); flush(); assertQuantity(19);
        assertTrue(ui.enterNumber(player, input)); flush();
        assertEquals(ShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); assertQuantity(19);
        tap(31); assertTrue(lore(4).contains("19 × Custom treasure")); noMoneyMoved();
    }

    @ParameterizedTest @ValueSource(strings = {"", "0", "-1", "1.001", "100000000.01", "two"})
    void invalidPriceCommandRetainsLatestAdjustedValue(String input) throws Exception {
        setupPrice(); queue(29, 28, 27); flush(); assertTrue(lore(13).contains("Unit price: $2.11"));
        assertTrue(ui.enterNumber(player, input)); flush();
        assertEquals(ShopUi.ViewKind.SETUP_PRICE, ui.currentKind(playerId)); assertTrue(lore(13).contains("Unit price: $2.11"));
        assertEquals("Use $2.11 per item", title(40)); tap(40); assertTrue(lore(0).contains("$2.11 each"));
        verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }

    @Test void nativeQuantityStillUsesSeparateReviewAndFinalConfirmation() throws Exception {
        nativeForms(); quantity(10); FormFixtures.State number = FormFixtures.last(); assertEquals("10", number.initial);
        number.number("17"); assertEquals(ShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); flush();
        assertEquals(ShopUi.ViewKind.BUY_REVIEW, ui.currentKind(playerId)); assertTrue(FormFixtures.last().text.contains("Total: $21.25")); noMoneyMoved();
        FormFixtures.last().choice(0); flush(); assertEquals(ShopUi.ViewKind.BUY_CONFIRM, ui.currentKind(playerId)); noMoneyMoved();
        FormFixtures.State confirm = FormFixtures.last(); confirm.choice(0); confirm.choice(0); confirm.close(); flush();
        verify(economy, times(1)).withdraw(playerId, 2125); verify(transfer, times(1)).apply();
        assertNull(ui.currentKind(playerId));
    }

    @Test void staleNativeNumberAndCloseCallbacksCannotReplaceNewSession() throws Exception {
        nativeForms(); quantity(10); FormFixtures.State old = FormFixtures.last(); old.number("17");
        quantity(40); FormFixtures.State current = FormFixtures.last(); flush(); old.close(); flush();
        assertSame(current, FormFixtures.last()); assertEquals("40", current.initial);
        assertEquals(ShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); noMoneyMoved();
    }

    @Test void nativeFinalCloseBlocksLateConfirmation() throws Exception {
        nativeForms(); quantity(10); FormFixtures.last().number("17"); flush(); FormFixtures.last().choice(0); flush();
        FormFixtures.State confirm = FormFixtures.last(); confirm.close(); confirm.choice(0); flush();
        assertNull(ui.currentKind(playerId)); noMoneyMoved(); verify(checkout).invalidateBuyer(playerId);
    }

    @Test void rejectedNativeSendFallbackKeepsInventoryStableAndCallbacksInert() throws Exception {
        nativeForms(); GeyserFixture.sendAccepted = false; quantity(10); FormFixtures.State rejected = FormFixtures.last();
        Inventory inventory = opened.get(); InventoryHolder page = inventory.getHolder(); clearInvocations(player);
        queue(24, 24); rejected.number("100"); rejected.close(); flush();
        sameOpenPage(inventory, page); assertQuantity(12); noMoneyMoved();
    }

    @Test void nativePriceSaveThenReviewStillRequiresExplicitPublish() throws Exception {
        nativeForms(); ui.offerSetup(player, proposal); FormFixtures.last().choice(0); flush(); tap(0);
        assertEquals(ShopUi.ViewKind.SETUP_PRICE, ui.currentKind(playerId)); assertEquals("1.00", FormFixtures.last().initial);
        FormFixtures.last().number("2.11"); flush(); assertEquals(ShopUi.ViewKind.SETUP_ITEMS, ui.currentKind(playerId));
        tap(31); assertEquals(ShopUi.ViewKind.SETUP_REVIEW, ui.currentKind(playerId));
        assertTrue(FormFixtures.last().text.contains("$2.11 each")); verify(registry, never()).commit(any(), any(), anyList());
        FormFixtures.last().choice(0); flush(); assertEquals(ShopUi.ViewKind.SETUP_CONFIRM, ui.currentKind(playerId));
        verify(registry, never()).commit(any(), any(), anyList()); FormFixtures.last().choice(2); flush();
        assertNull(ui.currentKind(playerId)); verify(registry, never()).commit(any(), any(), anyList()); noMoneyMoved();
    }
}
