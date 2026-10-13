// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.geysermc.fixtures.FormFixtures;
import org.geysermc.fixtures.GeyserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** Deterministic API fixtures; real Geyser and phone rendering are separate acceptance checks. */
class LegacyShopUiTest {
    private WRVShops plugin;
    private LegacyTrading trading;
    private LegacyShopUi ui;
    private Player player;
    private PluginManager plugins;
    private YamlConfiguration config;
    private LegacyTrading.Offer offer;
    private final UUID playerId = UUID.fromString("00000000-0000-0000-0000-000000000321");
    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private final AtomicReference<Inventory> opened = new AtomicReference<>();
    private final Map<Inventory, String> titles = new HashMap<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<ItemStack> items;

    @BeforeEach void setup() throws Exception {
        FormFixtures.reset(); GeyserFixture.reset();
        plugin = mock(WRVShops.class); trading = mock(LegacyTrading.class); player = mock(Player.class);
        Server server = mock(Server.class); plugins = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        config = new YamlConfiguration(); config.set("bedrock-forms", false);
        when(plugin.getConfig()).thenReturn(config); when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server); when(server.getPluginManager()).thenReturn(plugins);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegacyShopUiTest"));
        when(player.getUniqueId()).thenReturn(playerId); when(player.isOnline()).thenReturn(true);
        when(trading.available()).thenReturn(true);
        when(trading.confirm(eq(player), any())).thenReturn(new LegacyTrading.Outcome(true, false, "Authoritative trade success."));
        when(trading.quote(eq(player), any(), any(), anyInt())).thenAnswer(call -> quote(call.getArgument(2), call.getArgument(3), call.getArgument(3)));
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> { tasks.add(call.getArgument(1)); return mock(BukkitTask.class); });
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(call -> {
                    Inventory inventory = inventory(call.getArgument(0), call.getArgument(1));
                    titles.put(inventory, call.getArgument(2));
                    return inventory;
                });
        bukkit.when(() -> Bukkit.getPlayer(playerId)).thenReturn(player);
        items = mockConstruction(ItemStack.class, (stack, context) -> {
            Material type = context.arguments().isEmpty() ? Material.STONE : (Material) context.arguments().getFirst();
            configureItem(stack, type, 1, meta(null, List.of()));
        });
        opened.set(inventory(null, 9));
        when(player.getOpenInventory()).thenAnswer(call -> view(opened.get()));
        doAnswer(call -> {
            Inventory old = opened.getAndSet(call.getArgument(0));
            closeEvent(old);
            return view(opened.get());
        }).when(player).openInventory(any(Inventory.class));
        doAnswer(call -> {
            Inventory old = opened.getAndSet(inventory(null, 9));
            closeEvent(old);
            return null;
        }).when(player).closeInventory();
        ui = new LegacyShopUi(plugin, trading);
        offer = offer(8, true, true);
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
    }
    private LegacyTrading.Offer offer(int bundle, boolean buy, boolean sell) {
        return new LegacyTrading.Offer(UUID.randomUUID(), "Owner", item(Material.DIAMOND, 8, meta("Custom treasure", List.of("Original lore"))),
                bundle, "13.37 credits", "6.25 credits", buy, sell);
    }
    private LegacyTrading.Quote quote(LegacyTrading.Direction direction, int requested, int actual) {
        return new LegacyTrading.Quote(UUID.randomUUID(), direction, requested, actual, 12345,
                offer.item(), "SERVER QUOTE\nTotal: 123.45 tokens", Instant.now().plusSeconds(60));
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
    private void tap(int slot) { click(opened.get(), slot, ClickType.LEFT); flush(); }
    private void flush() { while (!tasks.isEmpty()) tasks.removeFirst().run(); }
    private void choose(LegacyTrading.Direction direction) { ui.showOffer(player, offer); tap(direction == LegacyTrading.Direction.BUY ? 11 : 15); }
    private void review(LegacyTrading.Direction direction) { choose(direction); tap(31); }
    private void confirmation(LegacyTrading.Direction direction) { review(direction); tap(11); }
    private void nativeForms() {
        config.set("bedrock-forms", true); Plugin geyser = mock(Plugin.class);
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(geyser); when(geyser.isEnabled()).thenReturn(true);
    }
    private String lore(int slot) { return String.join("\n", opened.get().getItem(slot).getItemMeta().getLore()); }

    private Economy worldBalance(double initialBalance) {
        Economy economy = mock(Economy.class);
        ServicesManager services = mock(ServicesManager.class);
        @SuppressWarnings("unchecked") RegisteredServiceProvider<Economy> registration = mock(RegisteredServiceProvider.class);
        World world = mock(World.class);
        when(player.getWorld()).thenReturn(world); when(world.getName()).thenReturn("shop_world");
        when(plugin.getServer().getServicesManager()).thenReturn(services);
        when(services.getRegistration(Economy.class)).thenReturn(registration);
        when(registration.getProvider()).thenReturn(economy); when(economy.isEnabled()).thenReturn(true);
        when(economy.hasAccount(player, "shop_world")).thenReturn(true);
        when(economy.getBalance(player, "shop_world")).thenReturn(initialBalance);
        when(plugin.getBalanceDisplay()).thenReturn(new BalanceDisplay(plugin, economy));
        return economy;
    }

    @ParameterizedTest @EnumSource(LegacyTrading.Direction.class)
    void everyInventoryScreenShowsWorldBalanceAndQuotedPreviewRefreshesAtConfirmation(LegacyTrading.Direction direction) {
        Economy economy = worldBalance(500);
        ui.showOffer(player, offer);
        assertTrue(titles.get(opened.get()).startsWith("Balance $500.00 | "));
        assertTrue(lore(4).contains("Current balance: $500.00"));
        tap(direction == LegacyTrading.Direction.BUY ? 11 : 15);
        assertTrue(titles.get(opened.get()).startsWith("Balance $500.00 | "));
        assertTrue(lore(13).contains("Current balance: $500.00"));
        assertTrue(lore(13).contains("Total: calculated at review."));
        assertFalse(lore(13).contains("Balance after"));
        tap(31);
        assertTrue(titles.get(opened.get()).startsWith("Balance $500.00 | "));
        assertTrue(lore(4).contains(direction == LegacyTrading.Direction.BUY
                ? "Purchase cost: -$123.45" : "Sale proceeds: +$123.45"));
        assertTrue(lore(4).contains(direction == LegacyTrading.Direction.BUY
                ? "Balance after purchase: $376.55" : "Balance after sale: $623.45"));
        when(economy.getBalance(player, "shop_world")).thenReturn(20.0);
        tap(11);
        assertTrue(titles.get(opened.get()).startsWith("Balance $20.00 | "));
        assertTrue(lore(4).contains("Current balance: $20.00"));
        assertTrue(lore(11).contains(direction == LegacyTrading.Direction.BUY
                ? "Balance after purchase: -$103.45" : "Balance after sale: $143.45"));
        assertEquals(direction == LegacyTrading.Direction.BUY, lore(11).contains("Insufficient funds."));
        verify(economy, never()).getBalance(player);
        verify(trading, never()).confirm(any(), any());
    }

    @ParameterizedTest @EnumSource(LegacyTrading.Direction.class)
    void everyNativeFormShowsCurrentBalanceAndFreshExactQuotePreview(LegacyTrading.Direction direction) {
        Economy economy = worldBalance(500);
        nativeForms(); choose(direction);
        assertTrue(FormFixtures.last().text.startsWith("Current balance: $500.00\n"));
        assertTrue(FormFixtures.last().text.contains("Total: calculated at review."));
        assertFalse(FormFixtures.last().text.contains("Balance after"));
        FormFixtures.last().number("8"); flush();
        assertTrue(FormFixtures.last().text.startsWith("Current balance: $500.00\n"));
        assertTrue(FormFixtures.last().text.contains(direction == LegacyTrading.Direction.BUY
                ? "Purchase cost: -$123.45" : "Sale proceeds: +$123.45"));
        assertTrue(FormFixtures.last().text.contains(direction == LegacyTrading.Direction.BUY
                ? "Balance after purchase: $376.55" : "Balance after sale: $623.45"));
        when(economy.getBalance(player, "shop_world")).thenReturn(200.0);
        FormFixtures.last().choice(0); flush();
        assertTrue(FormFixtures.last().text.startsWith("Current balance: $200.00\n"));
        assertTrue(FormFixtures.last().text.contains(direction == LegacyTrading.Direction.BUY
                ? "Balance after purchase: $76.55" : "Balance after sale: $323.45"));
        verify(economy, never()).getBalance(player);
        verify(trading, never()).confirm(any(), any());
    }

    @Test void missingBalanceDisplayKeepsUnknownBalanceHonestAndExactQuoteVisible() {
        review(LegacyTrading.Direction.BUY);
        assertTrue(titles.get(opened.get()).startsWith("Balance unavailable | "));
        assertTrue(lore(4).contains("Current balance: unavailable"));
        assertTrue(lore(4).contains("Purchase cost: -$123.45"));
        assertTrue(lore(4).contains("Balance after purchase: unavailable"));
    }

    @Test void offerKeepsOriginalItemMetadataBundleAndTrustedPriceLabelsWithoutPreparingOrCancelling() throws Exception {
        ui.showOffer(player, offer);
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        ItemStack icon = opened.get().getItem(4);
        assertEquals(Material.DIAMOND, icon.getType());
        assertEquals("Custom treasure", icon.getItemMeta().getDisplayName());
        assertTrue(lore(4).contains("Original lore")); assertTrue(lore(4).contains("Sign bundle: 8 items"));
        assertTrue(lore(4).contains("13.37 credits")); assertTrue(lore(4).contains("6.25 credits"));
        assertEquals(List.of("Original lore"), offer.item().getItemMeta().getLore());
        verify(trading, never()).quote(any(), any(), any(), anyInt()); verify(trading, never()).cancel(any());
    }

    @ParameterizedTest @EnumSource(value = ClickType.class, names = {"LEFT", "RIGHT"})
    void eitherOrdinaryTapChoosesSameDirectionWithoutTrade(ClickType type) throws Exception {
        ui.showOffer(player, offer); click(opened.get(), 15, type); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); assertTrue(lore(13).contains("Sell quantity: 8 items"));
        verify(trading, never()).quote(any(), any(), any(), anyInt()); verify(trading, never()).confirm(any(), any());
    }

    @ParameterizedTest @EnumSource(value = ClickType.class, names = {"LEFT", "RIGHT"}, mode = EnumSource.Mode.EXCLUDE)
    void nonTapControlsCannotChooseOrConfirm(ClickType type) {
        ui.showOffer(player, offer); InventoryClickEvent event = click(opened.get(), 11, type); flush();
        verify(event).setCancelled(true); assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        verify(trading, never()).confirm(any(), any());
    }

    @ParameterizedTest @ValueSource(ints = {-999, -1, 27, 28, 60})
    void outsideAndBottomInventorySlotsAreCancelled(int slot) {
        ui.showOffer(player, offer); InventoryClickEvent event = click(opened.get(), slot, ClickType.LEFT); flush();
        verify(event).setCancelled(true); assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
    }

    @Test void dragIsCancelledAndDoesNotAct() {
        ui.showOffer(player, offer); InventoryDragEvent event = mock(InventoryDragEvent.class);
        when(event.getView()).thenAnswer(call -> view(opened.get())); ui.drag(event);
        verify(event).setCancelled(true); assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
    }

    @Test void anotherPlayerCannotUseThisPagesActions() {
        ui.showOffer(player, offer); Player other = mock(Player.class); when(other.getUniqueId()).thenReturn(UUID.randomUUID());
        InventoryClickEvent event = mock(InventoryClickEvent.class); when(event.getView()).thenAnswer(call -> view(opened.get()));
        when(event.getWhoClicked()).thenReturn(other); when(event.getRawSlot()).thenReturn(11); when(event.getClick()).thenReturn(ClickType.LEFT);
        ui.click(event); flush(); verify(event).setCancelled(true); assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
    }

    @Test void unavailableDirectionHasNoAction() throws Exception {
        ui.showOffer(player, offer(8, false, true)); tap(11);
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        assertTrue(lore(11).contains("Unavailable")); verify(trading, never()).quote(any(), any(), any(), anyInt());
    }

    @ParameterizedTest @ValueSource(ints = {-9, 0, 1, 8, 2304, 9000})
    void initialNumberIsClampedButOriginalBundleLabelIsRetained(int bundle) {
        offer = offer(bundle, true, true); ui.showOffer(player, offer); assertTrue(lore(4).contains("Sign bundle: " + bundle)); tap(11);
        assertTrue(lore(13).contains("quantity: " + Math.max(1, Math.min(2304, bundle)) + " items"));
        assertTrue(lore(13).contains("/excelshop amount <wholequantity>"));
    }

    @ParameterizedTest @ValueSource(strings = {"", "0", "-1", "+1", "1.5", "1e2", "2305", "2147483648", "1 2", "two"})
    void invalidAmountDoesNotQuoteOrCommit(String value) throws Exception {
        choose(LegacyTrading.Direction.BUY); assertTrue(ui.enterNumber(player, value)); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        verify(trading, never()).quote(any(), any(), any(), anyInt()); verify(trading, never()).confirm(any(), any());
    }

    @Test void nullAmountDoesNotQuoteOrCommit() throws Exception {
        choose(LegacyTrading.Direction.BUY); assertTrue(ui.enterNumber(player, null)); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); verify(trading, never()).quote(any(), any(), any(), anyInt());
    }

    @ParameterizedTest @ValueSource(ints = {1, 2304})
    void validExactAmountWaitsForServerThreadAndOnlyCreatesReview(int quantity) throws Exception {
        choose(LegacyTrading.Direction.SELL); assertTrue(ui.enterNumber(player, " " + quantity + " "));
        verify(trading, never()).quote(any(), any(), any(), anyInt()); flush();
        verify(trading).quote(player, offer.id(), LegacyTrading.Direction.SELL, quantity);
        assertEquals(LegacyShopUi.ViewKind.SELL_REVIEW, ui.currentKind(playerId)); verify(trading, never()).confirm(any(), any());
    }

    @ParameterizedTest @EnumSource(LegacyTrading.Direction.class)
    void reviewThenSeparateFinalConfirmationUsesSameQuoteAndTruthfulResult(LegacyTrading.Direction direction) throws Exception {
        LegacyTrading.Quote quote = quote(direction, 8, 5);
        when(trading.quote(player, offer.id(), direction, 8)).thenReturn(quote);
        review(direction); assertTrue(lore(4).contains("Total: 123.45 tokens")); assertTrue(lore(4).contains("Requested: 8 items"));
        verify(trading, never()).confirm(any(), any()); tap(11);
        assertEquals(direction == LegacyTrading.Direction.BUY ? LegacyShopUi.ViewKind.BUY_CONFIRM : LegacyShopUi.ViewKind.SELL_CONFIRM, ui.currentKind(playerId));
        assertTrue(opened.get().getItem(11).getItemMeta().getDisplayName().contains("5 items"));
        assertTrue(lore(11).contains("Total: 123.45 tokens")); verify(trading, never()).confirm(any(), any());
        tap(11); verify(trading, times(1)).quote(player, offer.id(), direction, 8); verify(trading).confirm(player, quote.id());
        assertNull(ui.currentKind(playerId)); verify(player).sendMessage(org.bukkit.ChatColor.GREEN + "Authoritative trade success.");
    }

    @Test void queuedRepeatedFinalClicksCommitOnlyOnceIncludingReentrantClick() {
        confirmation(LegacyTrading.Direction.BUY); Inventory old = opened.get();
        when(trading.confirm(eq(player), any())).thenAnswer(call -> {
            click(old, 11, ClickType.LEFT); flush();
            return new LegacyTrading.Outcome(true, false, "One completed trade.");
        });
        click(old, 11, ClickType.LEFT); click(old, 11, ClickType.RIGHT); flush();
        verify(trading, times(1)).confirm(eq(player), any()); assertNull(ui.currentKind(playerId));
    }

    @Test void queuedDuplicateQuantityCommandCreatesOnlyOneQuote() throws Exception {
        choose(LegacyTrading.Direction.BUY); ui.enterNumber(player, "9"); ui.enterNumber(player, "10"); flush();
        verify(trading, times(1)).quote(player, offer.id(), LegacyTrading.Direction.BUY, 9);
        verify(trading, never()).quote(player, offer.id(), LegacyTrading.Direction.BUY, 10);
    }

    @Test void pageReplacementCloseCannotCancelNewPage() {
        ui.showOffer(player, offer); Inventory old = opened.get(); tap(11); closeEvent(old);
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); verify(trading, never()).cancel(any());
    }

    @Test void closingCurrentInventoryInvalidatesQueuedConfirmation() {
        confirmation(LegacyTrading.Direction.BUY); Inventory old = opened.get(); click(old, 11, ClickType.LEFT); player.closeInventory(); flush();
        assertNull(ui.currentKind(playerId)); verify(trading).cancel(playerId); verify(trading, never()).confirm(any(), any());
    }

    @Test void backRejectsOldConfirmWithoutRevokingOfferAndNewQuantityCreatesFreshQuote() throws Exception {
        confirmation(LegacyTrading.Direction.BUY); Inventory old = opened.get(); tap(13); click(old, 11, ClickType.LEFT); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); verify(trading, never()).cancel(any());
        verify(trading, never()).confirm(any(), any()); ui.enterNumber(player, "12"); flush();
        verify(trading).quote(player, offer.id(), LegacyTrading.Direction.BUY, 12);
    }

    @Test void cancelAndDisconnectRevokeQueuedWork() {
        confirmation(LegacyTrading.Direction.BUY); click(opened.get(), 11, ClickType.LEFT); ui.cancel(player); flush();
        verify(trading, never()).confirm(any(), any()); assertNull(ui.currentKind(playerId));
        choose(LegacyTrading.Direction.SELL); ui.enterNumber(player, "9"); ui.disconnected(playerId); flush();
        assertNull(ui.currentKind(playerId)); verify(trading, times(2)).cancel(playerId);
    }

    @Test void disableOrOfflinePlayerRejectsQueuedConfirmation() {
        confirmation(LegacyTrading.Direction.BUY); click(opened.get(), 11, ClickType.LEFT); when(plugin.isEnabled()).thenReturn(false); flush();
        verify(trading, never()).confirm(any(), any()); when(plugin.isEnabled()).thenReturn(true);
        click(opened.get(), 11, ClickType.LEFT); when(player.isOnline()).thenReturn(false); flush(); verify(trading, never()).confirm(any(), any());
        ui.closeAll(); assertNull(ui.currentKind(playerId)); verify(trading).cancel(playerId);
    }

    @Test void unavailableTradingRejectsQueuedConfirmation() {
        confirmation(LegacyTrading.Direction.SELL); click(opened.get(), 11, ClickType.LEFT); when(trading.available()).thenReturn(false); flush();
        verify(trading, never()).confirm(any(), any()); assertNull(ui.currentKind(playerId)); verify(trading).cancel(playerId);
    }

    @Test void failureOutcomeUsesBridgeMessageWithoutInventingSuccess() {
        confirmation(LegacyTrading.Direction.BUY); when(trading.confirm(eq(player), any())).thenReturn(new LegacyTrading.Outcome(false, true, "Administrator recovery required."));
        tap(11); verify(player).sendMessage(org.bukkit.ChatColor.YELLOW + "Administrator recovery required.");
        verify(player, never()).sendMessage(org.bukkit.ChatColor.GREEN + "Authoritative trade success.");
    }

    @Test void thrownConfirmationDoesNotClaimNoMoneyMoved() {
        confirmation(LegacyTrading.Direction.BUY); when(trading.confirm(eq(player), any())).thenThrow(new IllegalStateException("Unknown result")); tap(11);
        verify(player).sendMessage(org.bukkit.ChatColor.RED + "The trade result could not be verified. Ask an administrator before trying again.");
        assertNull(ui.currentKind(playerId));
    }

    @Test void quoteExceptionCancelsWithoutOpeningFinalConfirmation() throws Exception {
        choose(LegacyTrading.Direction.BUY); when(trading.quote(eq(player), any(), any(), anyInt())).thenThrow(new IllegalStateException("Price changed; reopen the sign.")); tap(31);
        assertNull(ui.currentKind(playerId)); verify(trading).cancel(playerId); verify(trading, never()).confirm(any(), any());
        verify(player).sendMessage(org.bukkit.ChatColor.RED + "Price changed; reopen the sign.");
    }


    @Test void reentrantNewOfferDuringQuotePreparationWinsOverOlderReview() throws Exception {
        choose(LegacyTrading.Direction.BUY);
        LegacyTrading.Offer newer = offer(64, true, true);
        when(trading.quote(eq(player), any(), any(), anyInt())).thenAnswer(call -> {
            ui.showOffer(player, newer);
            return quote(LegacyTrading.Direction.BUY, 8, 8);
        });
        tap(31);
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        assertTrue(lore(4).contains("Sign bundle: 64 items"));
        verify(trading, never()).cancel(any()); verify(trading, never()).confirm(any(), any());
    }

    @Test void reentrantNewOfferSurvivesOlderQuoteFailure() throws Exception {
        choose(LegacyTrading.Direction.BUY);
        LegacyTrading.Offer newer = offer(64, true, true);
        when(trading.quote(eq(player), any(), any(), anyInt())).thenAnswer(call -> {
            ui.showOffer(player, newer);
            throw new IllegalStateException("Old quote failed");
        });
        tap(31);
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        verify(trading, never()).cancel(any()); verify(trading, never()).confirm(any(), any());
    }

    @Test void reentrantCancellationDuringQuotePreparationCannotOpenReview() throws Exception {
        choose(LegacyTrading.Direction.BUY);
        when(trading.quote(eq(player), any(), any(), anyInt())).thenAnswer(call -> {
            ui.cancel(player);
            return quote(LegacyTrading.Direction.BUY, 8, 8);
        });
        tap(31);
        assertNull(ui.currentKind(playerId)); verify(trading, never()).confirm(any(), any());
    }

    @Test void numberCommandOnlyWorksOnQuantityPage() {
        assertFalse(ui.enterNumber(player, "8")); ui.showOffer(player, offer); assertFalse(ui.enterNumber(player, "8"));
        review(LegacyTrading.Direction.BUY); assertFalse(ui.enterNumber(player, "8")); tap(11); assertFalse(ui.enterNumber(player, "8"));
    }

    @Test void nativeNumberReviewAndFinalCallbacksAreSeparateAndMainThreadOnly() throws Exception {
        nativeForms(); choose(LegacyTrading.Direction.SELL);
        FormFixtures.State number = FormFixtures.last(); assertEquals("8", number.initial); assertTrue(number.text.contains("ITEMS, not bundles"));
        number.number("17"); verify(trading, never()).quote(any(), any(), any(), anyInt()); flush();
        assertEquals(LegacyShopUi.ViewKind.SELL_REVIEW, ui.currentKind(playerId)); FormFixtures.State review = FormFixtures.last();
        assertTrue(review.text.contains("Total: 123.45 tokens")); review.choice(0); verify(trading, never()).confirm(any(), any()); flush();
        assertEquals(LegacyShopUi.ViewKind.SELL_CONFIRM, ui.currentKind(playerId)); FormFixtures.State confirm = FormFixtures.last();
        assertTrue(confirm.buttons.getFirst().contains("Sell")); assertTrue(confirm.buttons.getFirst().contains("17 items"));
        confirm.choice(0); confirm.choice(0); confirm.close(); verify(trading, never()).confirm(any(), any()); flush();
        verify(trading, times(1)).confirm(eq(player), any()); verify(trading, times(1)).quote(player, offer.id(), LegacyTrading.Direction.SELL, 17);
    }

    @Test void lateNativeCloseOrResultCannotCancelOrTradeThroughNewOffer() {
        nativeForms(); choose(LegacyTrading.Direction.BUY); FormFixtures.State old = FormFixtures.last();
        ui.showOffer(player, offer(64, true, true)); old.close(); old.number("8"); flush();
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId)); verify(trading, never()).cancel(any()); verify(trading, never()).confirm(any(), any());
    }

    @Test void nativeFinalCloseInvalidatesQuoteAndBlocksLateConfirmation() {
        nativeForms(); choose(LegacyTrading.Direction.BUY); FormFixtures.last().number("8"); flush(); FormFixtures.last().choice(0); flush();
        FormFixtures.State confirm = FormFixtures.last(); confirm.close(); confirm.choice(0); flush();
        assertNull(ui.currentKind(playerId)); verify(trading).cancel(playerId); verify(trading, never()).confirm(any(), any());
    }

    @Test void rejectedNativeSendFallsBackAndLateNativeCallbacksAreInert() {
        nativeForms(); GeyserFixture.sendAccepted = false; choose(LegacyTrading.Direction.BUY); FormFixtures.State rejected = FormFixtures.last();
        assertTrue(opened.get().getHolder() instanceof LegacyShopUi.Page); rejected.number("9"); rejected.close(); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); verify(trading, never()).cancel(any());
    }
}
