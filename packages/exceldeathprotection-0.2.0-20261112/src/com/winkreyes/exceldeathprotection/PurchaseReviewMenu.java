package com.winkreyes.exceldeathprotection;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.LongSupplier;

import static com.winkreyes.exceldeathprotection.PurchaseCoordinator.*;

/** Fixed-quantity native inventory review; all money and grants remain in PurchaseCoordinator. */
final class PurchaseReviewMenu implements Listener {
    static final int PRODUCT = 4, PRICE = 10, BALANCE = 12, DIFFERENCE = 14, BACK = 18, CONFIRM = 22;
    static final long REVIEW_MS = 20_000L;
    interface Gateway {
        Result quote(Player player, Product product);
        Result preview(Player player, Product product);
        Result confirm(Player player, Product product);
        void cancel(UUID playerId);
        void report(Player player, Result result);
    }
    interface Host {
        Inventory inventory(InventoryHolder holder, String title);
        ItemStack item(Material material, String name, List<String> lore);
        void later(Runnable action);
        void back(Player player);
    }
    static final class Review implements InventoryHolder {
        final UUID owner;
        final Player player;
        final Product product;
        Inventory inventory;
        Result result;
        double quotedPrice;
        long openedAt;
        boolean pending;
        boolean armedQuote;
        Object action;
        Review(Player player, Product product, Result result, long openedAt) {
            this.player = player; this.owner = player.getUniqueId(); this.product = product; this.result = result;
            this.quotedPrice = result.price(); this.openedAt = openedAt;
            this.armedQuote = result.status() == Status.ARMED;
        }
        @Override public Inventory getInventory() { return inventory; }
    }
    private record Opening(Object request, Inventory source) {}
    private final Gateway gateway;
    private final Host host;
    private final LongSupplier clock;
    private final Map<UUID, Review> reviews = new HashMap<>();
    private final Map<UUID, Opening> openings = new HashMap<>();
    private boolean stopped;

    PurchaseReviewMenu(Gateway gateway, Host host, LongSupplier clock) {
        this.gateway = gateway; this.host = host; this.clock = clock;
    }
    /** Always a new non-charging quote, including repeated commands/taps. */
    void open(Player player, Product product) {
        if (stopped) return;
        UUID id = player.getUniqueId();
        cancel(id);
        Opening opening = new Opening(new Object(), player.getOpenInventory().getTopInventory());
        openings.put(id, opening);
        host.later(() -> {
            if (stopped || openings.get(id) != opening) return;
            openings.remove(id);
            if (!player.isOnline() || player.isDead()
                    || player.getOpenInventory().getTopInventory() != opening.source()) return;
            Result result = gateway.quote(player, product);
            Review review = new Review(player, product, result, clock.getAsLong());
            review.inventory = host.inventory(review, product == Product.BASIC ? "Review Death Protection" : "Review +Death Protection");
            render(review);
            player.openInventory(review.inventory);
            if (player.getOpenInventory().getTopInventory() == review.inventory) reviews.put(id, review);
            else gateway.cancel(id); // Another plugin rejected/replaced the inventory open.
        });
    }
    void refresh(Player player) {
        Review review = reviews.get(player.getUniqueId());
        if (review == null || stopped) return;
        if (!current(player, review)) { cancel(player.getUniqueId()); return; }
        if (review.pending) return;
        review.result = gateway.preview(player, review.product);
        render(review); // Preview must never arm or extend the confirmation lifetime.
    }
    void shutdown() {
        stopped = true;
        for (Review review : new ArrayList<>(reviews.values())) {
            cancel(review.owner);
            if (review.player.isOnline() && review.player.getOpenInventory().getTopInventory() == review.inventory)
                review.player.closeInventory();
        }
        openings.keySet().forEach(gateway::cancel);
        openings.clear();
    }
    private boolean current(Player player, Review review) {
        return player.isOnline() && !player.isDead() && reviews.get(player.getUniqueId()) == review
                && review.owner.equals(player.getUniqueId())
                && player.getOpenInventory().getTopInventory() == review.inventory;
    }
    private boolean expired(Review review) {
        long now = clock.getAsLong();
        return now < review.openedAt || now - review.openedAt >= REVIEW_MS;
    }
    private boolean canConfirm(Review review) {
        return review.armedQuote && review.result.status() == Status.ARMED && !expired(review)
                && Double.compare(review.quotedPrice, review.result.price()) == 0;
    }
    private void cancel(UUID id) {
        reviews.remove(id); openings.remove(id); gateway.cancel(id);
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Review review)) return;
        event.setCancelled(true); // Also block bottom-inventory shift, hotbar, double-click, drop and outside clicks.
        if (!(event.getWhoClicked() instanceof Player player) || !current(player, review)) return;
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        int slot = event.getRawSlot();
        if (slot != BACK && slot != CONFIRM) return;
        if (review.pending && slot != BACK) return;
        Object action = new Object();
        review.action = action; // Back may supersede an unexecuted Confirm; duplicate Confirm cannot.
        boolean chargeAllowed = slot == CONFIRM && canConfirm(review);
        review.pending = true; // Consume this UI action before a second packet can schedule another one.
        host.later(() -> {
            if (stopped || review.action != action) return;
            if (!current(player, review)) {
                // An old callback must not cancel a newer review, but must retire its own stale token.
                if (reviews.get(review.owner) == review) cancel(review.owner);
                return;
            }
            if (slot == BACK) {
                cancel(review.owner);
                player.closeInventory();
                host.back(player);
                return;
            }
            // An expired/disabled button only refreshes its review, even if money arrived meanwhile.
            Result result = chargeAllowed && !expired(review)
                    ? gateway.confirm(player, review.product) : gateway.quote(player, review.product);
            review.result = result;
            review.armedQuote = result.status() == Status.ARMED;
            review.pending = false;
            if (result.status() == Status.PURCHASED || result.status() == Status.DECLINED
                    || result.status() == Status.REFUNDED || result.status() == Status.LOCKED) {
                cancel(review.owner);
                player.closeInventory();
                gateway.report(player, result);
                return;
            }
            if (result.status() == Status.ARMED || !chargeAllowed) {
                review.quotedPrice = result.price(); review.openedAt = clock.getAsLong();
            }
            render(review);
        });
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Review) event.setCancelled(true);
    }
    @EventHandler public void close(InventoryCloseEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        Review current = reviews.get(id);
        if (current != null && event.getInventory() == current.inventory) cancel(id);
        Opening opening = openings.get(id);
        if (opening != null && event.getInventory() == opening.source()) { openings.remove(id); gateway.cancel(id); }
    }
    @EventHandler public void quit(PlayerQuitEvent event) { cancel(event.getPlayer().getUniqueId()); }

    static String money(double value) { return String.format(Locale.US, "%,.2f", value); }
    static String shortfall(double price, double balance) {
        // A positive deficit must never be displayed as $0.00 when a provider supports sub-cent balances.
        BigDecimal missing = BigDecimal.valueOf(price).subtract(BigDecimal.valueOf(balance)).max(BigDecimal.ZERO);
        return String.format(Locale.US, "%,.2f", missing.setScale(2, RoundingMode.CEILING));
    }
    private void put(Review review, int slot, Material material, String name, String... lore) {
        review.inventory.setItem(slot, host.item(material, name, List.of(lore)));
    }
    private void render(Review review) {
        Result result = review.result;
        String product = review.product == Product.BASIC ? "Death Protection" : "+Death Protection Potion";
        if (review.product == Product.BASIC) {
            put(review, PRODUCT, Material.CHEST, "§f" + product, "§7Quantity: §f1",
                    "§7Activates immediately after purchase.", "§7Keeps items for one death. XP not saved.",
                    "§7No extra abilities. One active protection.");
        } else {
            put(review, PRODUCT, Material.POTION, "§f" + product, "§7Quantity: §f1",
                    "§7Drink the potion to activate later.", "§7Keeps items for one death. XP not saved.",
                    "§bConduit: underwater breathing and vision.", "§bFaster mining, including on land.",
                    "§7One active protection at a time.");
        }
        boolean priceValid = Double.isFinite(result.price()) && result.price() > 0;
        boolean balanceValid = Double.isFinite(result.balance()) && result.balance() >= 0;
        put(review, PRICE, Material.GOLD_INGOT, priceValid ? "§ePrice: $" + money(result.price()) : "§cPrice unavailable", "§7Fixed quantity: 1");
        put(review, BALANCE, Material.GOLD_NUGGET, balanceValid ? "§eBalance: $" + money(result.balance()) : "§cBalance unavailable", "§7Refreshes while this review is open.");
        if (priceValid && balanceValid) {
            boolean enough = result.balance() >= result.price();
            put(review, DIFFERENCE, enough ? Material.EMERALD : Material.REDSTONE,
                    enough ? "§aAfter purchase: $" + money(result.balance() - result.price()) : "§cNeed $" + shortfall(result.price(), result.balance()) + " more",
                    enough ? "§7Projected remaining balance." : "§7You cannot afford this purchase yet.", "§7No money charged by this review.");
        } else put(review, DIFFERENCE, Material.BARRIER, "§cPurchase unavailable", "§7No money charged by this review.");
        put(review, BACK, Material.ARROW, "§eBack to Spawn Shop", "§7Cancel this review. No charge.");
        if (canConfirm(review)) {
            long seconds = Math.max(1L, (REVIEW_MS - (clock.getAsLong() - review.openedAt) + 999L) / 1000L);
            put(review, CONFIRM, Material.LIME_WOOL, "§aConfirm: $" + money(result.price()), "§7Buy exactly 1. Charges only when confirmed.",
                    "§7Review expires in " + seconds + "s.", "§7Balance and eligibility checked again.");
        } else {
            String reason = reason(review);
            put(review, CONFIRM, Material.RED_WOOL, "§c" + reason, "§7Purchase disabled. No charge.", "§7Tap to refresh this review.");
        }
    }
    private String reason(Review review) {
        if (review.result.status() == Status.INSUFFICIENT_FUNDS) return "Not enough money";
        if (review.result.status() == Status.INELIGIBLE) return switch (review.result.detail()) {
            case "ALREADY_ACTIVE" -> "Protection already active";
            case "NO_SPACE" -> "Make an empty inventory slot";
            default -> "Purchase unavailable";
        };
        if (review.result.status() == Status.LOCKED) return "Admin review required";
        if (review.result.status() != Status.ARMED) return "Purchase unavailable";
        if (Double.compare(review.quotedPrice, review.result.price()) != 0) return "Price changed: review again";
        if (expired(review)) return "Review expired: refresh";
        if (!review.armedQuote) return "Refresh review to confirm";
        return "Refresh review";
    }
}
