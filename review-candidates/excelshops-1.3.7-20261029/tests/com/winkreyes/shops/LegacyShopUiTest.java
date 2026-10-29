// Excel signature - WRV maintained source; original authorship and license notices preserved.
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
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
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
import org.geysermc.fixtures.FloodgateFixture;
import org.geysermc.fixtures.GeyserFixture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

/** 1.3.4 Back-flow regressions for the legacy sign-shop UI: every form's Back
 *  control and X-close step back exactly one screen, and no close path can trade. */
class LegacyShopUiTest {
    private WRVShops plugin;
    private LegacyTrading trading;
    private LegacyShopUi ui;
    private Player player;
    private PluginManager plugins;
    private YamlConfiguration config;
    private Economy vault;
    private ItemStack offerItem;
    private LegacyTrading.Offer offer;
    private Material stockMaterial;
    private final UUID playerId = new UUID(0L, 0x000000000777L);
    private final Deque<Runnable> tasks = new ArrayDeque<>();
    private final AtomicReference<Inventory> opened = new AtomicReference<>();
    private final List<String> inventoryTitles = new ArrayList<>();
    private MockedStatic<Bukkit> bukkit;
    private MockedConstruction<ItemStack> items;

    @BeforeEach void setup() throws Exception {
        FormFixtures.reset(); GeyserFixture.reset(); FloodgateFixture.reset();
        plugin = mock(WRVShops.class); trading = mock(LegacyTrading.class); player = mock(Player.class);
        Server server = mock(Server.class); plugins = mock(PluginManager.class);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        config = new YamlConfiguration(); config.set("bedrock-forms", true);
        when(plugin.getConfig()).thenReturn(config); when(plugin.isEnabled()).thenReturn(true);
        when(plugin.getServer()).thenReturn(server); when(server.getPluginManager()).thenReturn(plugins);
        when(server.getScheduler()).thenReturn(scheduler);
        when(plugin.getLogger()).thenReturn(Logger.getLogger("LegacyShopUiTest"));
        Plugin geyser = mock(Plugin.class);
        when(plugins.getPlugin("Geyser-Spigot")).thenReturn(geyser); when(geyser.isEnabled()).thenReturn(true);
        World world = mock(World.class); when(world.getName()).thenReturn("world");
        when(player.getUniqueId()).thenReturn(playerId); when(player.isOnline()).thenReturn(true);
        when(player.getName()).thenReturn("Buyer"); when(player.getWorld()).thenReturn(world);
        vault = mock(Economy.class);
        ServicesManager services = mock(ServicesManager.class);
        @SuppressWarnings("unchecked") RegisteredServiceProvider<Economy> provider = mock(RegisteredServiceProvider.class);
        when(server.getServicesManager()).thenReturn(services);
        when(services.getRegistration(Economy.class)).thenReturn(provider);
        when(provider.getProvider()).thenReturn(vault); when(vault.isEnabled()).thenReturn(true);
        when(vault.hasAccount(player, "world")).thenReturn(true); when(vault.getBalance(player, "world")).thenReturn(1500.0);
        when(plugin.getBalanceDisplay()).thenReturn(new BalanceDisplay(plugin, vault));
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            tasks.add(call.getArgument(1)); return mock(BukkitTask.class);
        });
        when(trading.available()).thenReturn(true);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(InventoryHolder.class), anyInt(), anyString()))
                .thenAnswer(call -> { inventoryTitles.add(call.getArgument(2)); return inventory(call.getArgument(0), call.getArgument(1)); });
        items = mockConstruction(ItemStack.class, (stack, context) -> {
            Material type = context.arguments().isEmpty() ? Material.STONE : (Material) context.arguments().getFirst();
            configureItem(stack, type, 1, meta(null, List.of()));
        });
        stockMaterial = Material.DIAMOND; // Real enum: itemDetails() calls getType().name().
        offerItem = item(stockMaterial, 1, meta("Custom treasure", List.of("Original lore")));
        offer = new LegacyTrading.Offer(UUID.randomUUID(), "Seller", offerItem, 10, "$5.00", "$4.00", true, true);
        when(trading.quote(any(), any(), any(), anyInt())).thenAnswer(call -> {
            LegacyTrading.Direction direction = call.getArgument(2); int requested = call.getArgument(3);
            return new LegacyTrading.Quote(UUID.randomUUID(), direction, requested, requested,
                    requested * 50L, offerItem, "Fixture quote description", Instant.now().plusSeconds(60));
        });
        when(trading.confirm(any(), any())).thenReturn(new LegacyTrading.Outcome(true, false, "Trade complete."));
        opened.set(inventory(null, 9));
        when(player.getOpenInventory()).thenAnswer(call -> view(opened.get()));
        doAnswer(call -> { Inventory old = opened.getAndSet(call.getArgument(0)); closeEvent(old); return view(opened.get()); })
                .when(player).openInventory(any(Inventory.class));
        doAnswer(call -> { Inventory old = opened.getAndSet(inventory(null, 9)); closeEvent(old); return null; })
                .when(player).closeInventory();
        ui = new LegacyShopUi(plugin, trading);
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
    private void click(int slot) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenAnswer(call -> view(opened.get())); when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot); when(event.getClick()).thenReturn(ClickType.LEFT);
        ui.click(event); flush();
    }
    private void flush() { while (!tasks.isEmpty()) tasks.removeFirst().run(); }
    private String title(int slot) { return ChatColor.stripColor(opened.get().getItem(slot).getItemMeta().getDisplayName()); }
    private void openOffer() { ui.showOffer(player, offer); assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId)); }
    private void openBuyQuantity() { openOffer(); click(11); assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId)); }
    private void neverTraded() throws Exception {
        verify(trading, never()).confirm(any(), any());
    }

    @Test void quantityFormCarriesBackDropdownDefaultingToUseAmount() throws Exception {
        openBuyQuantity();
        FormFixtures.State form = FormFixtures.last();
        assertEquals("10", form.initial);
        assertEquals("When you tap Submit:", form.dropdownLabel);
        assertEquals(List.of("Use this amount", "Back to Buy / Sell"), form.dropdownOptions);
        assertEquals(0, form.dropdownDefault);
        neverTraded();
    }

    @Test void quantityDropdownBackReturnsToOfferAndIgnoresTypedAmount() throws Exception {
        openBuyQuantity();
        FormFixtures.last().number("25", 1); flush();
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        verify(trading, never()).quote(any(), any(), any(), anyInt());
        neverTraded();
    }

    @Test void quantityCloseStepsBackToOffer() throws Exception {
        openBuyQuantity();
        FormFixtures.State form = FormFixtures.last(); form.close(); flush();
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        form.number("25", 0); flush(); // The closed form is inert.
        assertEquals(LegacyShopUi.ViewKind.OFFER, ui.currentKind(playerId));
        verify(trading, never()).quote(any(), any(), any(), anyInt());
        neverTraded();
    }

    @Test void reviewCloseStepsBackToQuantityAndNeverTrades() throws Exception {
        openBuyQuantity();
        FormFixtures.last().number("25", 0); flush();
        assertEquals(LegacyShopUi.ViewKind.BUY_REVIEW, ui.currentKind(playerId));
        assertEquals(List.of("Continue to final Buy confirmation", "Back", "Cancel"), FormFixtures.last().buttons);
        FormFixtures.State review = FormFixtures.last(); review.close(); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        assertEquals("25", FormFixtures.last().initial);
        review.choice(0); flush(); // A late tap on the closed review cannot advance.
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        neverTraded();
    }

    @Test void confirmCloseStepsBackToQuantityAndNeverTrades() throws Exception {
        openBuyQuantity();
        FormFixtures.last().number("25", 0); flush();
        FormFixtures.last().choice(0); flush();
        assertEquals(LegacyShopUi.ViewKind.BUY_CONFIRM, ui.currentKind(playerId));
        FormFixtures.State confirm = FormFixtures.last(); confirm.close(); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        confirm.choice(0); flush(); // A late tap on the closed confirm can never trade.
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        neverTraded();
    }

    @Test void sellReviewCloseStepsBackToQuantityAndNeverTrades() throws Exception {
        openOffer(); click(15);
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        FormFixtures.last().number("30", 0); flush();
        assertEquals(LegacyShopUi.ViewKind.SELL_REVIEW, ui.currentKind(playerId));
        assertEquals(List.of("Continue to final Sell confirmation", "Back", "Cancel"), FormFixtures.last().buttons);
        FormFixtures.last().close(); flush();
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        neverTraded();
    }

    @Test void explicitConfirmStillTradesExactlyOnce() throws Exception {
        openBuyQuantity();
        FormFixtures.last().number("25", 0); flush();
        FormFixtures.last().choice(0); flush();
        FormFixtures.State confirm = FormFixtures.last(); confirm.choice(0); confirm.choice(0); flush();
        verify(trading, times(1)).confirm(eq(player), any());
        assertNull(ui.currentKind(playerId));
    }

    @Test void guiReviewReturnButtonIsLabeledBack() throws Exception {
        config.set("bedrock-forms", false); // Java GUI path: no forms are sent.
        openOffer(); click(11);
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        click(31); // Review Buy · 10 items
        assertEquals(LegacyShopUi.ViewKind.BUY_REVIEW, ui.currentKind(playerId));
        assertEquals("Back", title(13));
        click(13); // Back returns to the quantity GUI, whose own Back names its target.
        assertEquals(LegacyShopUi.ViewKind.QUANTITY, ui.currentKind(playerId));
        assertEquals("Back to Buy / Sell", title(36));
        neverTraded();
    }
}
