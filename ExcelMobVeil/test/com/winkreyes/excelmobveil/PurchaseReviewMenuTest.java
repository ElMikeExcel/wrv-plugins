package com.winkreyes.excelmobveil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.*;

import static com.winkreyes.excelmobveil.PurchaseCoordinator.*;
import static com.winkreyes.excelmobveil.PurchaseReviewMenu.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PurchaseReviewMenuTest {
    @TempDir Path dir;
    final AtomicLong now = new AtomicLong(1_000L);
    final UUID id = UUID.randomUUID();
    Player player;
    InventoryView view;
    Inventory source;
    final AtomicReference<Inventory> top = new AtomicReference<>();
    final Deque<Runnable> tasks = new ArrayDeque<>();
    final Map<Inventory, Map<Integer, ItemStack>> slots = new IdentityHashMap<>();
    final Map<ItemStack, String> texts = new IdentityHashMap<>();
    final Map<Product, Double> prices = new EnumMap<>(Product.class);
    final List<Result> reports = new ArrayList<>();
    double balance = 100_000;
    Eligibility eligibility = Eligibility.ELIGIBLE;
    int debits, deliveries, credits, backCalls, quoteCalls, previews;
    PurchaseCoordinator coordinator;
    PurchaseJournal journal;
    PurchaseReviewMenu menu;

    @BeforeEach void setup() throws Exception {
        player = mock(Player.class); source = mock(Inventory.class); view = mock(InventoryView.class);
        top.set(source);
        when(player.getUniqueId()).thenReturn(id);
        when(player.isOnline()).thenReturn(true);
        when(player.getOpenInventory()).thenReturn(view);
        when(view.getTopInventory()).thenAnswer(i -> top.get());
        doAnswer(i -> { top.set(i.getArgument(0)); return view; }).when(player).openInventory(any(Inventory.class));
        doAnswer(i -> { top.set(source); return null; }).when(player).closeInventory();
        prices.put(Product.VEIL, 2_000.0);
        journal = new PurchaseJournal(dir.resolve("purchase.journal"));
        coordinator = new PurchaseCoordinator(new Ports() {
            public Eligibility check(UUID playerId, Product product) { return eligibility; }
            public double balance(UUID playerId) { return balance; }
            public MoneyResult withdraw(UUID playerId, double amount) { debits++; balance -= amount; return new MoneyResult(true, amount, balance); }
            public MoneyResult deposit(UUID playerId, double amount) { credits++; balance += amount; return new MoneyResult(true, amount, balance); }
            public DeliveryOutcome deliver(UUID playerId, Product product) { deliveries++; return DeliveryOutcome.SUCCESS; }
        }, journal, now::get, REVIEW_MS, 1_000);
        menu = new PurchaseReviewMenu(new Gateway() {
            public Result quote(Player p, Product product) { quoteCalls++; return coordinator.quote(p.getUniqueId(), product, prices.get(product)); }
            public Result preview(Player p, Product product) { previews++; return coordinator.preview(p.getUniqueId(), product, prices.get(product)); }
            public Result confirm(Player p, Product product) { return coordinator.confirm(p.getUniqueId(), product, prices.get(product)); }
            public void cancel(UUID playerId) { coordinator.cancel(playerId); }
            public void report(Player p, Result result) { reports.add(result); }
        }, new Host() {
            public Inventory inventory(InventoryHolder holder, String title) {
                Inventory inventory = mock(Inventory.class);
                when(inventory.getHolder()).thenReturn(holder);
                when(inventory.getSize()).thenReturn(27);
                Map<Integer, ItemStack> content = new HashMap<>(); slots.put(inventory, content);
                doAnswer(i -> { content.put(i.getArgument(0), i.getArgument(1)); return null; }).when(inventory).setItem(anyInt(), any(ItemStack.class));
                return inventory;
            }
            public ItemStack item(Material material, String name, List<String> lore) {
                ItemStack item = mock(ItemStack.class); when(item.getType()).thenReturn(material);
                texts.put(item, name + "\n" + String.join("\n", lore)); return item;
            }
            public void later(Runnable action) { tasks.add(action); }
            public void back(Player p) { backCalls++; }
        }, now::get);
    }
    void drain() { while (!tasks.isEmpty()) tasks.remove().run(); }
    void open(Product product) { menu.open(player, product); drain(); }
    String text(int slot) { return texts.get(slots.get(top.get()).get(slot)); }
    InventoryClickEvent click(int slot, ClickType type) {
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getView()).thenReturn(view); when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(slot); when(event.getClick()).thenReturn(type);
        when(event.isLeftClick()).thenReturn(type.isLeftClick()); when(event.isRightClick()).thenReturn(type.isRightClick());
        when(event.isShiftClick()).thenReturn(type.isShiftClick());
        menu.click(event); return event;
    }
    void confirm() { click(CONFIRM, ClickType.LEFT); drain(); }
    void close() {
        Inventory old = top.get();
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getPlayer()).thenReturn(player); when(event.getInventory()).thenReturn(old);
        top.set(source); menu.close(event);
    }
    void noCharge() { assertEquals(0, debits); assertEquals(0, deliveries); assertEquals(0, credits); assertTrue(journal.entries().isEmpty()); }

    @Test void openingVeilShowsOnePotionPriceAndStoredActivationWithoutPayment() {
        open(Product.VEIL);
        assertTrue(text(PRODUCT).contains("Quantity: §f1"));
        assertTrue(text(PRODUCT).contains("Veil Potion"));
        assertTrue(text(PRODUCT).contains("drink to activate later"));
        assertTrue(text(PRODUCT).contains("Each drink adds 8 real-time minutes"));
        assertTrue(text(PRODUCT).contains("Attacking clears all remaining veil time"));
        assertTrue(text(PRODUCT).contains("Farming keeps the veil active"));
        assertTrue(text(PRODUCT).contains("body is invisible"));
        assertTrue(text(PRODUCT).contains("Armor and held items remain visible"));
        assertTrue(text(PRICE).contains("$2,000.00")); assertTrue(text(BALANCE).contains("$100,000.00"));
        assertTrue(text(DIFFERENCE).contains("$98,000.00")); assertTrue(text(CONFIRM).contains("Confirm: $2,000.00")); noCharge();
    }
    @Test void insufficientFundsShowExactShortfallAndDisabledConfirm() {
        balance = 1_234.56;
        open(Product.VEIL); assertTrue(text(DIFFERENCE).contains("$765.44 more"));
        assertTrue(text(BALANCE).contains("$1,234.56"));
        assertTrue(text(CONFIRM).contains("Not enough money")); confirm(); noCharge();
    }

    @Test void zeroBalanceAndOneCentShortAreNotRoundedAway() {
        balance=0; open(Product.VEIL); assertTrue(text(DIFFERENCE).contains("$2,000.00 more"));
        balance=1_999.99; menu.refresh(player); assertTrue(text(DIFFERENCE).contains("$0.01 more")); noCharge();
        assertEquals("0.01", shortfall(2_000, 1_999.999));
    }
    @Test void exactBalanceMayConfirmOnceAndLeavesZero() {
        balance=2_000; open(Product.VEIL); assertTrue(text(DIFFERENCE).contains("$0.00")); confirm();
        assertEquals(1,debits); assertEquals(1,deliveries); assertEquals(0,balance); assertEquals(Status.PURCHASED,reports.getFirst().status());
    }
    @Test void repeatedBuyCommandsAlwaysQuoteNeverPay() {
        open(Product.VEIL); open(Product.VEIL); open(Product.VEIL); noCharge();
        confirm(); assertEquals(1,debits);
    }
    @Test void latestQueuedRequestWinsWithoutOpeningStaleReview() {
        menu.open(player,Product.VEIL); menu.open(player,Product.VEIL); drain();
        assertEquals(1,quoteCalls); assertTrue(text(PRODUCT).contains("Veil Potion")); noCharge();
    }
    @Test void queuedOpenDoesNotOverrideNewerNavigation() {
        menu.open(player,Product.VEIL); Inventory newer=mock(Inventory.class); top.set(newer); drain();
        assertSame(newer,top.get()); assertEquals(0,quoteCalls); noCharge();
    }
    @Test void closeBeforeScheduledOpenCancelsIt() {
        menu.open(player,Product.VEIL); close(); drain(); assertEquals(0,quoteCalls); noCharge();
    }
    @Test void confirmChargesOnceDespiteRepeatedClickPackets() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); click(CONFIRM,ClickType.RIGHT); click(CONFIRM,ClickType.LEFT);
        assertEquals(1,tasks.size()); drain(); assertEquals(1,debits); assertEquals(1,deliveries);
    }
    @Test void forbiddenClickTypesAndBottomClicksCannotBuyOrMoveItems() {
        open(Product.VEIL);
        for(ClickType type:List.of(ClickType.SHIFT_LEFT,ClickType.SHIFT_RIGHT,ClickType.NUMBER_KEY,ClickType.SWAP_OFFHAND,
                ClickType.DOUBLE_CLICK,ClickType.DROP,ClickType.CONTROL_DROP,ClickType.MIDDLE,ClickType.CREATIVE)) {
            verify(click(CONFIRM,type)).setCancelled(true);
        }
        verify(click(27,ClickType.LEFT)).setCancelled(true); verify(click(-999,ClickType.RIGHT)).setCancelled(true);
        assertTrue(tasks.isEmpty()); noCharge();
    }
    @Test void draggingAcrossOrWithinReviewIsCancelled() {
        open(Product.VEIL); InventoryDragEvent event=mock(InventoryDragEvent.class); when(event.getView()).thenReturn(view);
        menu.drag(event); verify(event).setCancelled(true); noCharge();
    }
    @Test void closeCancelsQueuedConfirmAndItsToken() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); close(); drain(); noCharge();
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void backReturnsToShopAndCancelsWithoutCharge() {
        open(Product.VEIL); click(BACK,ClickType.LEFT); drain(); assertEquals(1,backCalls); assertSame(source,top.get()); noCharge();
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void timeoutRefreshRequiresNewDistinctConfirm() {
        open(Product.VEIL); now.addAndGet(REVIEW_MS); menu.refresh(player);
        assertTrue(text(CONFIRM).contains("expired")); confirm(); noCharge(); assertTrue(text(CONFIRM).contains("Confirm:"));
        confirm(); assertEquals(1,debits);
    }
    @Test void expiryBetweenClickAndExecutionCannotCharge() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); now.addAndGet(REVIEW_MS); drain(); noCharge();
        assertTrue(text(CONFIRM).contains("Confirm:"));
    }
    @Test void previewNeverExtendsExpiry() {
        open(Product.VEIL); now.addAndGet(19_999); menu.refresh(player); now.incrementAndGet(); menu.refresh(player);
        assertTrue(text(CONFIRM).contains("expired")); confirm(); noCharge();
    }
    @Test void balanceDroppingAtConfirmDisablesAndShowsFreshShortfall() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); balance=1_999.99; drain();
        assertTrue(text(DIFFERENCE).contains("$0.01 more")); assertTrue(text(CONFIRM).contains("Not enough money")); noCharge();
    }
    @Test void toppingUpAfterFailureRequiresRequoteThenConfirm() {
        open(Product.VEIL); balance=1; confirm(); noCharge(); balance=50_000; menu.refresh(player);
        assertTrue(text(BALANCE).contains("$50,000.00")); assertTrue(text(CONFIRM).contains("Refresh review to confirm")); confirm(); noCharge(); confirm(); assertEquals(1,debits);
    }
    @Test void initiallyPoorPlayerCanRefreshThenBuyAfterDeposit() {
        balance=1; open(Product.VEIL); balance=20_000; confirm(); noCharge();
        assertTrue(text(CONFIRM).contains("Confirm:")); confirm(); assertEquals(1,debits); assertEquals(18_000,balance);
    }
    @Test void changedPriceRequiresFreshReviewEvenBeforeRefreshTick() {
        open(Product.VEIL); prices.put(Product.VEIL,2_500.0); confirm(); noCharge();
        assertTrue(text(PRICE).contains("$2,500.00")); confirm(); assertEquals(1,debits); assertEquals(97_500,balance);
    }
    @Test void previewedPriceChangeDisablesOldConfirm() {
        open(Product.VEIL); prices.put(Product.VEIL,2_500.0); menu.refresh(player);
        assertTrue(text(CONFIRM).contains("Price changed")); confirm(); noCharge(); confirm(); assertEquals(1,debits);
    }
    @Test void eligibilityChangeBeforeConfirmNeverCharges() {
        open(Product.VEIL); eligibility=Eligibility.OFFLINE; confirm(); noCharge();
        assertTrue(text(CONFIRM).contains("unavailable"));
    }
    @Test void fullInventoryBlocksReviewPurchase() {
        eligibility=Eligibility.NO_SPACE; open(Product.VEIL); assertTrue(text(CONFIRM).contains("empty inventory slot")); confirm(); noCharge();
    }
    @Test void unknownBalancesAreNotPresentedAsZeroOrNaN() {
        balance=Double.NaN; open(Product.VEIL);
        assertTrue(text(BALANCE).contains("unavailable")); assertFalse(text(BALANCE).contains("NaN"));
        assertTrue(text(CONFIRM).contains("unavailable")); confirm(); noCharge();
    }
    @Test void wrongPlayerCannotActOnOwnersReview() {
        open(Product.VEIL); Player attacker=mock(Player.class); when(attacker.getUniqueId()).thenReturn(UUID.randomUUID());
        InventoryClickEvent event=mock(InventoryClickEvent.class); when(event.getView()).thenReturn(view); when(event.getWhoClicked()).thenReturn(attacker);
        when(event.getRawSlot()).thenReturn(CONFIRM); when(event.isLeftClick()).thenReturn(true); when(event.getClick()).thenReturn(ClickType.LEFT);
        menu.click(event); verify(event).setCancelled(true); assertTrue(tasks.isEmpty()); noCharge();
    }
    @Test void newerMenuMakesOldQueuedConfirmInert() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); menu.open(player,Product.VEIL); drain(); noCharge();
        assertTrue(text(PRODUCT).contains("Veil Potion"));
    }
    @Test void quitCancelsQueuedAction() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); PlayerQuitEvent event=mock(PlayerQuitEvent.class);
        when(event.getPlayer()).thenReturn(player); menu.quit(event); drain(); noCharge();
    }
    @Test void shutdownClosesOwnedReviewAndPreventsQueuedActions() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); menu.shutdown(); drain();
        assertSame(source,top.get()); noCharge(); menu.open(player,Product.VEIL); drain(); noCharge();
    }
    @Test void deathBeforeQueuedConfirmRetiresItsToken() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); when(player.isDead()).thenReturn(true); drain(); noCharge();
        when(player.isDead()).thenReturn(false);
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void newerViewWithoutCloseEventRetiresQueuedReviewToken() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); Inventory newer=mock(Inventory.class); top.set(newer); drain();
        assertSame(newer,top.get()); noCharge();
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void refreshRetiresInvalidPendingReviewBeforeCallback() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); top.set(source); menu.refresh(player); drain();
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void backSupersedesQueuedButUnexecutedConfirm() {
        open(Product.VEIL); click(CONFIRM,ClickType.LEFT); click(BACK,ClickType.LEFT); drain();
        assertEquals(1,backCalls); assertSame(source,top.get()); noCharge();
        assertEquals(Status.ARMED,coordinator.confirm(id,Product.VEIL,2_000).status()); noCharge();
    }
    @Test void unrelatedInventoryIsIgnored() {
        InventoryClickEvent event=mock(InventoryClickEvent.class); when(event.getView()).thenReturn(view);
        menu.click(event); verify(event,never()).setCancelled(anyBoolean()); noCharge();
    }
}
