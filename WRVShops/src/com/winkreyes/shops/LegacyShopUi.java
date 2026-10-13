// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.plugin.IllegalPluginAccessException;

/** Tap-friendly, server-owned confirmation pages for existing ChestShop signs. */
public final class LegacyShopUi implements Listener {
    public enum ViewKind { OFFER, QUANTITY, BUY_REVIEW, BUY_CONFIRM, SELL_REVIEW, SELL_CONFIRM }
    public static final class Page implements InventoryHolder {
        final UUID player;
        final ViewKind kind;
        final Map<Integer, Runnable> actions = new HashMap<>();
        Inventory inventory;
        Consumer<String> numberInput;
        boolean consumed;
        Page(UUID player, ViewKind kind) { this.player = player; this.kind = kind; }
        @Override public Inventory getInventory() { return inventory; }
        public ViewKind kind() { return kind; }
    }

    private static final int MAX_QUANTITY = 2304;
    private final WRVShops plugin;
    private final LegacyTrading trading;
    private final BedrockForms forms;
    private final Map<UUID, Page> pages = new HashMap<>();

    public LegacyShopUi(WRVShops plugin, LegacyTrading trading) {
        this.plugin = Objects.requireNonNull(plugin);
        this.trading = Objects.requireNonNull(trading);
        forms = new BedrockForms(plugin);
    }

    public ViewKind currentKind(UUID id) {
        Page page = pages.get(id);
        return page == null ? null : page.kind;
    }

    private Page page(Player player, ViewKind kind, String title, int size) {
        Page page = new Page(player.getUniqueId(), kind);
        page.inventory = Bukkit.createInventory(page, size, balanceTitle(player, title));
        // Replace before closing the old inventory: its close event must not revoke this page.
        pages.put(player.getUniqueId(), page);
        return page;
    }

    private String balanceTitle(Player player, String title) {
        BalanceDisplay display = plugin.getBalanceDisplay();
        return display == null ? "Balance unavailable | " + title : display.title(player, title, true);
    }

    private String currentBalance(Player player) {
        BalanceDisplay display = plugin.getBalanceDisplay();
        return display == null ? "Current balance: unavailable" : display.current(player, true);
    }

    private String quoteBalance(Player player, LegacyTrading.Quote quote) {
        BalanceDisplay display = plugin.getBalanceDisplay();
        boolean selling = quote.direction() == LegacyTrading.Direction.SELL;
        return display == null ? "Current balance: unavailable\n"
                + (selling ? "Sale proceeds: +" : "Purchase cost: -") + BalanceDisplay.money(quote.totalCents())
                + "\nBalance after " + (selling ? "sale" : "purchase") + ": unavailable"
                : display.preview(player, quote.totalCents(), selling, true);
    }

    private void button(Page page, int slot, Material material, String title, Runnable action, String... lore) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.RESET + title);
        icon.setItemMeta(meta);
        item(page, slot, icon, lore);
        if (action != null) page.actions.put(slot, action);
    }

    /** Clone the genuine item and append UI details; retain name, lore, enchants and other metadata. */
    private void item(Page page, int slot, ItemStack source, String... lore) {
        ItemStack icon = source.clone();
        icon.setAmount(Math.max(1, Math.min(icon.getAmount(), icon.getMaxStackSize())));
        ItemMeta meta = icon.getItemMeta();
        if (meta != null) {
            List<String> lines = new ArrayList<>();
            if (meta.hasLore()) lines.addAll(meta.getLore());
            if (!lines.isEmpty()) lines.add("");
            for (String line : lore) lines.add(ChatColor.GRAY + line);
            meta.setLore(lines);
            icon.setItemMeta(meta);
        }
        page.inventory.setItem(slot, icon);
    }

    /** Form threads and inventory callbacks can only enqueue one action for the current page. */
    private void later(Player player, Page expected, Runnable action) {
        if (!plugin.isEnabled()) return;
        try {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (!plugin.isEnabled() || !player.isOnline()
                        || pages.get(player.getUniqueId()) != expected || expected.consumed) return;
                expected.consumed = true;
                try {
                    boolean available = trading.available();
                    if (pages.get(player.getUniqueId()) != expected) return;
                    if (!available) {
                        fail(player, "This shop is unavailable. Please ask an administrator before trying again.");
                        return;
                    }
                    action.run();
                } catch (Exception ex) {
                    if (pages.get(player.getUniqueId()) == expected) fail(player, ex.getMessage());
                }
            });
        } catch (IllegalPluginAccessException disabled) {
            // Disable invalidates the pages; never run a callback off the server thread.
        }
    }

    private void fail(Player player, String message) {
        cancel(player);
        player.sendMessage(ChatColor.RED + (message == null || message.isBlank()
                ? "That shop is unavailable. Please reopen the sign." : message));
    }

    public void cancel(Player player) {
        pages.remove(player.getUniqueId());
        trading.cancel(player.getUniqueId());
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof Page) player.closeInventory();
    }

    public void disconnected(UUID playerId) {
        pages.remove(playerId);
        trading.cancel(playerId);
    }

    public void closeAll() {
        for (UUID id : List.copyOf(pages.keySet())) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) disconnected(id); else cancel(player);
        }
        pages.clear();
    }

    public boolean enterNumber(Player player, String value) {
        Page page = pages.get(player.getUniqueId());
        if (page == null || page.numberInput == null || page.consumed) return false;
        later(player, page, () -> page.numberInput.accept(value));
        return true;
    }

    @EventHandler public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Page page)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !page.player.equals(player.getUniqueId())
                || pages.get(player.getUniqueId()) != page) return;
        // Plain left and right taps are equivalent. Never move items with shift, hotbar, drop, etc.
        if (event.getClick() != ClickType.LEFT && event.getClick() != ClickType.RIGHT) return;
        if (event.getRawSlot() < 0 || event.getRawSlot() >= page.inventory.getSize()) return;
        Runnable action = page.actions.get(event.getRawSlot());
        if (action != null) later(player, page, action);
    }

    @EventHandler public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof Page) event.setCancelled(true);
    }

    @EventHandler public void close(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof Page page
                && pages.get(event.getPlayer().getUniqueId()) == page) {
            disconnected(event.getPlayer().getUniqueId());
        }
    }

    public void showOffer(Player player, LegacyTrading.Offer offer) {
        if (!plugin.isEnabled() || !player.isOnline() || !trading.available()) {
            fail(player, "This shop is unavailable. Please reopen the sign later.");
            return;
        }
        Objects.requireNonNull(offer);
        // The bridge has just issued this offer. Calling trading.cancel here would revoke it.
        Page page = page(player, ViewKind.OFFER, "Chest shop · Buy or Sell", 27);
        String name = ShopAdapter.name(offer.item());
        item(page, 4, offer.item(), currentBalance(player), "Shop owner: " + offer.owner(), "Item: " + name,
                "Sign bundle: " + offer.bundleQuantity() + " items",
                "Buy from shop: " + price(offer, LegacyTrading.Direction.BUY),
                "Sell to shop: " + price(offer, LegacyTrading.Direction.SELL),
                "Choose Buy or Sell, then quantity.", "Review and confirm before any trade.");
        button(page, 11, offer.canBuy() ? Material.LIME_CONCRETE : Material.GRAY_CONCRETE,
                "Buy from shop", offer.canBuy() ? () -> quantity(player, offer, LegacyTrading.Direction.BUY, initial(offer)) : null,
                price(offer, LegacyTrading.Direction.BUY), "You receive items and pay the shop.");
        button(page, 15, offer.canSell() ? Material.GOLD_BLOCK : Material.GRAY_CONCRETE,
                "Sell to shop", offer.canSell() ? () -> quantity(player, offer, LegacyTrading.Direction.SELL, initial(offer)) : null,
                price(offer, LegacyTrading.Direction.SELL), "You give items to the shop and receive money.");
        button(page, 22, Material.BARRIER, "Cancel", () -> cancel(player));
        // Inventory chooser deliberately exposes the real item tooltip on Java and Bedrock.
        player.openInventory(page.inventory);
    }

    private static int initial(LegacyTrading.Offer offer) { return Math.max(1, Math.min(MAX_QUANTITY, offer.bundleQuantity())); }
    private static String verb(LegacyTrading.Direction direction) { return direction == LegacyTrading.Direction.BUY ? "Buy" : "Sell"; }
    private static String price(LegacyTrading.Offer offer, LegacyTrading.Direction direction) {
        boolean allowed = direction == LegacyTrading.Direction.BUY ? offer.canBuy() : offer.canSell();
        if (!allowed) return "Unavailable";
        return (direction == LegacyTrading.Direction.BUY ? offer.buyPrice() : offer.sellPrice())
                + " per sign bundle of " + offer.bundleQuantity();
    }

    private void quantity(Player player, LegacyTrading.Offer offer, LegacyTrading.Direction direction, int requested) {
        int count = Math.max(1, Math.min(MAX_QUANTITY, requested));
        Page page = page(player, ViewKind.QUANTITY, verb(direction) + " · item quantity", 45);
        Consumer<String> input = value -> {
            final int number;
            try {
                if (value == null || !value.trim().matches("[0-9]+")) throw new IllegalArgumentException();
                number = Integer.parseInt(value.trim());
                if (number < 1 || number > MAX_QUANTITY) throw new IllegalArgumentException();
            } catch (IllegalArgumentException invalid) {
                player.sendMessage(ChatColor.RED + "Enter a whole item quantity from 1 to " + MAX_QUANTITY + ".");
                quantity(player, offer, direction, count);
                return;
            }
            review(player, offer, direction, number);
        };
        page.numberInput = input;
        String details = currentBalance(player) + "\nTotal: calculated at review.\n"
                + verb(direction) + " " + ShopAdapter.name(offer.item()) + "\n"
                + price(offer, direction) + "\nEnter the number of ITEMS, not bundles: 1–" + MAX_QUANTITY
                + ".\nThe server calculates the available quantity and total for review."
                + "\nThen a separate final confirmation."
                + "\nOr use /excelshop amount <wholequantity>" + itemDetails(offer.item());
        player.closeInventory();
        if (forms.number(player, verb(direction) + " · choose item quantity", details, String.valueOf(count),
                value -> later(player, page, () -> input.accept(value)),
                () -> later(player, page, () -> cancel(player)))) return;
        item(page, 13, offer.item(), currentBalance(player), "Total: calculated at review.",
                verb(direction) + " quantity: " + count + " items", price(offer, direction),
                "Quantity is items, not bundles.", "Use buttons, or /excelshop amount <wholequantity>",
                "The server calculates your total at review.");
        int[] changes = {-64, -8, -1, 1, 8, 64};
        int[] slots = {18, 19, 20, 24, 25, 26};
        for (int i = 0; i < changes.length; i++) {
            int change = changes[i];
            button(page, slots[i], change < 0 ? Material.RED_DYE : Material.LIME_DYE,
                    (change > 0 ? "+" : "") + change + " items",
                    () -> quantity(player, offer, direction, count + change));
        }
        button(page, 31, Material.LIME_CONCRETE, "Review " + verb(direction) + " · " + count + " items",
                () -> review(player, offer, direction, count), "No trade until final confirmation.");
        button(page, 36, Material.ARROW, "Back to Buy / Sell", () -> showOffer(player, offer));
        button(page, 44, Material.BARRIER, "Cancel", () -> cancel(player));
        player.openInventory(page.inventory);
    }

    private void review(Player player, LegacyTrading.Offer offer, LegacyTrading.Direction direction, int count) {
        Page origin = pages.get(player.getUniqueId());
        final LegacyTrading.Quote quote;
        try {
            // One chosen direction, only after quantity selection. A new quote supersedes any previous one.
            quote = trading.quote(player, offer.id(), direction, count);
            // Quote preparation fires upstream events. A reentrant close or newer offer wins.
            if (pages.get(player.getUniqueId()) != origin || !plugin.isEnabled() || !player.isOnline()) return;
            if (quote == null || quote.direction() != direction || quote.requestedQuantity() != count
                    || quote.quantity() < 1 || quote.quantity() > MAX_QUANTITY || quote.totalCents() < 0
                    || quote.description() == null || quote.description().isBlank()) {
                throw new IllegalStateException("The shop could not prepare a valid quote. Please reopen the sign.");
            }
        } catch (Exception failure) {
            if (pages.get(player.getUniqueId()) == origin) fail(player, failure.getMessage());
            return;
        }
        Page page = page(player, direction == LegacyTrading.Direction.BUY ? ViewKind.BUY_REVIEW : ViewKind.SELL_REVIEW,
                "Review " + verb(direction), 27);
        String summary = quoteBalance(player, quote) + "\n" + quoteText(quote);
        String text = summary + "\nNo trade yet. Continue to a separate final confirmation.";
        Runnable next = () -> confirm(player, offer, quote);
        Runnable change = () -> quantity(player, offer, direction, quote.requestedQuantity());
        player.closeInventory();
        if (forms.choices(player, "Review " + verb(direction), text + itemDetails(quote.item()),
                new String[]{"Continue to final " + verb(direction) + " confirmation", "Change quantity", "Cancel"},
                id -> later(player, page, () -> { if (id == 0) next.run(); else if (id == 1) change.run(); else cancel(player); }),
                () -> later(player, page, () -> cancel(player)))) return;
        item(page, 4, quote.item(), summary.split("\\R"));
        button(page, 11, Material.LIME_CONCRETE, "Continue to " + verb(direction) + " confirmation", next,
                "This is review only. No trade yet.");
        button(page, 13, Material.ARROW, "Change quantity", change);
        button(page, 15, Material.BARRIER, "Cancel", () -> cancel(player));
        player.openInventory(page.inventory);
    }

    private void confirm(Player player, LegacyTrading.Offer offer, LegacyTrading.Quote quote) {
        LegacyTrading.Direction direction = quote.direction();
        Page page = page(player, direction == LegacyTrading.Direction.BUY ? ViewKind.BUY_CONFIRM : ViewKind.SELL_CONFIRM,
                "Confirm " + verb(direction) + "?", 27);
        String summary = quoteBalance(player, quote) + "\n" + quoteText(quote);
        String text = summary + "\n"
                + (direction == LegacyTrading.Direction.BUY
                ? "Confirm Buy pays the shop and gives you these items."
                : "Confirm Sell takes these items from you and pays you.")
                + "\nThe server checks the shop again before trading.";
        Runnable commit = () -> {
            // Consume before any inventory close, economy event or plugin callback can reenter.
            if (!pages.remove(player.getUniqueId(), page)) return;
            player.closeInventory();
            try {
                LegacyTrading.Outcome result = trading.confirm(player, quote.id());
                if (result == null || result.message() == null || result.message().isBlank()) {
                    throw new IllegalStateException("Missing transaction result");
                }
                player.sendMessage((result.success() && !result.recoveryRequired() ? ChatColor.GREEN : ChatColor.YELLOW) + result.message());
            } catch (RuntimeException failure) {
                trading.cancel(player.getUniqueId());
                plugin.getLogger().severe("Legacy shop confirmation result could not be verified: " + failure.getClass().getSimpleName());
                player.sendMessage(ChatColor.RED + "The trade result could not be verified. Ask an administrator before trying again.");
            }
        };
        // Page replacement makes the old quote unreachable; a subsequent review supersedes it in the bridge.
        Runnable back = () -> quantity(player, offer, direction, quote.requestedQuantity());
        String label = "Confirm " + verb(direction) + " · " + quote.quantity() + " items";
        player.closeInventory();
        if (forms.choices(player, "Confirm " + verb(direction) + "?", text + itemDetails(quote.item()),
                new String[]{label, "Back / change quantity", "Cancel"},
                id -> later(player, page, () -> { if (id == 0) commit.run(); else if (id == 1) back.run(); else cancel(player); }),
                () -> later(player, page, () -> cancel(player)))) return;
        item(page, 4, quote.item(), text.split("\\R"));
        button(page, 11, Material.LIME_CONCRETE, label, commit, summary.split("\\R"));
        button(page, 13, Material.ARROW, "Back / change quantity", back);
        button(page, 15, Material.BARRIER, "Cancel", () -> cancel(player));
        player.openInventory(page.inventory);
    }

    private static String quoteText(LegacyTrading.Quote quote) {
        return verb(quote.direction()) + " " + quote.quantity() + " items: " + ShopAdapter.name(quote.item())
                + (quote.quantity() != quote.requestedQuantity() ? "\nRequested: " + quote.requestedQuantity() + " items" : "")
                + "\n" + quote.description();
    }

    private static String itemDetails(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return "";
        StringBuilder text = new StringBuilder("\nItem type: ").append(item.getType().name().toLowerCase(java.util.Locale.ROOT).replace('_', ' '));
        Map<Enchantment, Integer> enchantments = new LinkedHashMap<>(meta.getEnchants());
        if (meta instanceof EnchantmentStorageMeta book) enchantments.putAll(book.getStoredEnchants());
        enchantments.forEach((enchantment, level) -> text.append("\n").append(enchantment.getKey()).append(" ").append(level));
        if (meta instanceof Damageable damage && damage.hasDamage()) text.append("\nWear: ").append(damage.getDamage());
        if (meta instanceof PotionMeta potion) {
            if (potion.getBasePotionType() != null) text.append("\nPotion: ").append(potion.getBasePotionType().name());
            potion.getCustomEffects().forEach(effect -> text.append("\n").append(effect.getType().getKey())
                    .append(" ").append(effect.getAmplifier() + 1).append(" · ").append(effect.getDuration()).append(" ticks"));
        }
        if (meta.hasLore()) for (String line : meta.getLore()) text.append("\n").append(ChatColor.stripColor(line));
        return text.toString();
    }
}
