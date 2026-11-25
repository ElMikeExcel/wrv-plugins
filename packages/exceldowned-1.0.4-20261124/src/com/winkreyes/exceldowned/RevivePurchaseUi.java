package com.winkreyes.exceldowned;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The Revive Potion purchase review UI (ExcelDowned 1.0.0 charge
 * leg, W6 sections 3.3 + 4) — the GiveUpUi pattern carried to the
 * shop side: one state (a ticket bound to the buyer), two doors —
 * a Java inventory menu and a Bedrock ModalForm via BedrockForms.
 *
 * Discipline (W6 section 4): opening the review runs the
 * coordinator's quote — it ARMS the purchase token and charges
 * NOTHING. Only the Confirm button calls coordinator.confirm, and
 * exactly once per ticket; Back / ESC / dismiss clears the token
 * and charges nothing. The menu shows the product picture (a cloned
 * minted display item, slot 4), the price from the armed quote, the
 * buyer's current balance, and the balance after purchase (in the
 * Confirm item's lore and in the Bedrock form body). Opening a new
 * review replaces any prior one; quit / becoming downed / feature
 * disable invalidates through invalidate().
 *
 * This is a DIFFERENT UI from RevivePotionUi (the revive confirm);
 * the two are never merged (W6 section 3.3).
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public final class RevivePurchaseUi implements Listener {

    static final int SLOT_CONFIRM = 2;
    static final int SLOT_PICTURE = 4;
    static final int SLOT_BACK = 6;

    static final String ACTION_CONFIRM = "confirm";
    static final String ACTION_BACK = "back";

    /** Marks the review inventory; the click listener trusts this
     *  map, never the ItemStack (the RevivePotionHolder principle). */
    static final class PurchaseHolder implements InventoryHolder {
        private final Map<Integer, String> slotActions;
        private Inventory inventory;
        PurchaseHolder(Map<Integer, String> slotActions) { this.slotActions = slotActions; }
        String actionAt(int slot) { return slotActions.get(slot); }
        void bind(Inventory inventory) { this.inventory = inventory; }
        @Override public Inventory getInventory() { return inventory; }
    }

    /** One presented review: the buyer and the quote it stands on. */
    private static final class Ticket {
        final UUID buyerId;
        RevivePurchaseCoordinator.Result quote;
        Ticket(UUID buyerId, RevivePurchaseCoordinator.Result quote) {
            this.buyerId = buyerId;
            this.quote = quote;
        }
    }

    private final ExcelDowned plugin;
    private final Map<UUID, Ticket> tickets = new ConcurrentHashMap<>();
    private final Set<UUID> javaOpen = ConcurrentHashMap.newKeySet();

    public RevivePurchaseUi(ExcelDowned plugin) { this.plugin = plugin; }

    // ---------- presentation ----------

    /**
     * Open the purchase review for a buyer (the console shop
     * dispatch lands here). Runs the coordinator quote first: a
     * non-ARMED quote is reported to the buyer and no UI opens.
     * Returns the quote result (null only when the caller already
     * refused upstream); ARMED means a review is standing.
     */
    public RevivePurchaseCoordinator.Result openReview(Player buyer) {
        RevivePurchaseCoordinator.Result quote = plugin.purchaseQuote(buyer);
        if (quote.status() != RevivePurchaseCoordinator.Status.ARMED) {
            plugin.reportPurchase(buyer, quote);
            return quote;
        }
        Ticket ticket = new Ticket(buyer.getUniqueId(), quote);
        tickets.put(buyer.getUniqueId(), ticket); // opening replaces any prior review
        present(buyer, ticket);
        return quote;
    }

    /** Present (or re-present after a re-arm) the ticket's door. */
    private void present(Player buyer, Ticket ticket) {
        boolean bedrock = BedrockForms.isBedrockPlayer(buyer, plugin.getLogger());
        if (GiveUpRoute.route(bedrock, BedrockForms.isFormApiAvailable())
                == GiveUpRoute.Route.BEDROCK_FORM) {
            String body = "Revive Potion — quantity 1\n\n"
                    + "Price: $" + money(ticket.quote.price()) + "\n"
                    + "Your balance: $" + money(ticket.quote.balance()) + "\n"
                    + "Balance after: $" + money(ticket.quote.balance() - ticket.quote.price())
                    + "\n\nConfirm charges $" + money(ticket.quote.price())
                    + " and delivers exactly one Revive Potion.\n"
                    + "Back charges nothing.";
            boolean sent = BedrockForms.sendModalForm(buyer,
                    "✝ Buy a Revive Potion?", body,
                    "✔ Confirm — $" + money(ticket.quote.price()), "Back",
                    first -> Bukkit.getScheduler().runTask(plugin,
                            () -> onFormAnswer(buyer, ticket, first)),
                    () -> Bukkit.getScheduler().runTask(plugin,
                            () -> onBack(buyer, ticket)),
                    plugin.getLogger());
            if (sent) return;
            tickets.remove(buyer.getUniqueId(), ticket);
            // form could not be sent: fall through to the Java menu door
            tickets.put(buyer.getUniqueId(), ticket);
        }
        Inventory inv = buildMenu(buyer, ticket.quote);
        javaOpen.add(buyer.getUniqueId());
        buyer.openInventory(inv);
    }

    private Inventory buildMenu(Player buyer, RevivePurchaseCoordinator.Result quote) {
        Map<Integer, String> actions = new LinkedHashMap<>();
        actions.put(SLOT_CONFIRM, ACTION_CONFIRM);
        actions.put(SLOT_BACK, ACTION_BACK);
        PurchaseHolder holder = new PurchaseHolder(actions);
        Inventory inv = Bukkit.createInventory(holder, 9,
                ChatColor.DARK_GREEN + "✝ Buy a Revive Potion?");
        holder.bind(inv);
        ItemStack filler = named(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);
        inv.setItem(SLOT_CONFIRM, named(Material.GREEN_CONCRETE,
                ChatColor.GREEN + "" + ChatColor.BOLD + "✔ CONFIRM — $" + money(quote.price()),
                ChatColor.GRAY + "Buy exactly 1 Revive Potion.",
                ChatColor.GRAY + "Price: $" + money(quote.price()),
                ChatColor.GRAY + "Your balance: $" + money(quote.balance()),
                ChatColor.GRAY + "Balance after: $" + money(quote.balance() - quote.price()),
                ChatColor.GRAY + "Charges only when you confirm."));
        inv.setItem(SLOT_PICTURE, RevivePotionItem.create(plugin).clone());
        inv.setItem(SLOT_BACK, named(Material.ARROW,
                ChatColor.RED + "" + ChatColor.BOLD + "Back",
                ChatColor.GRAY + "Close — no charge."));
        return inv;
    }

    private static ItemStack named(Material material, String name, String... lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) meta.setLore(Arrays.asList(lore));
            stack.setItemMeta(meta);
        }
        return stack;
    }

    static String money(double value) {
        return String.format("%,.2f", value);
    }

    // ---------- answers ----------

    private void onFormAnswer(Player buyer, Ticket ticket, boolean confirm) {
        if (tickets.get(buyer.getUniqueId()) != ticket) return; // stale form
        if (!buyer.isOnline()) { tickets.remove(buyer.getUniqueId(), ticket); return; }
        if (confirm) onConfirm(buyer, ticket);
        else onBack(buyer, ticket);
    }

    /**
     * Confirm: the coordinator runs the whole section-4 transaction.
     * Called exactly once per ticket — the ticket is consumed before
     * the result is handled, so a duplicate answer can never debit
     * twice. An ARMED result means the coordinator re-armed at a
     * fresh quote (price changed / token lapsed): the review is
     * re-presented on the new quote, still uncharged.
     */
    private void onConfirm(Player buyer, Ticket ticket) {
        if (tickets.get(buyer.getUniqueId()) != ticket) return;
        RevivePurchaseCoordinator.Result result = plugin.purchaseConfirm(buyer);
        if (result.status() == RevivePurchaseCoordinator.Status.ARMED) {
            ticket.quote = result;
            present(buyer, ticket);
            return;
        }
        tickets.remove(buyer.getUniqueId(), ticket);
        closeJavaMenu(buyer);
        plugin.reportPurchase(buyer, result);
    }

    /** Back / ESC / dismiss: token cleared, no charge. */
    private void onBack(Player buyer, Ticket ticket) {
        if (tickets.get(buyer.getUniqueId()) != ticket) return;
        tickets.remove(buyer.getUniqueId(), ticket);
        plugin.purchaseCancel(buyer);
        closeJavaMenu(buyer);
    }

    // ---------- Java menu events ----------

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PurchaseHolder)) return;
        event.setCancelled(true); // a menu is not a container
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player buyer = (Player) event.getWhoClicked();
        if (event.getClickedInventory() != event.getInventory()) return;
        PurchaseHolder holder = (PurchaseHolder) event.getInventory().getHolder();
        String action = holder.actionAt(event.getSlot());
        if (action == null) return;
        Ticket ticket = tickets.get(buyer.getUniqueId());
        if (ticket == null) return;
        if (ACTION_CONFIRM.equals(action)) onConfirm(buyer, ticket);
        else if (ACTION_BACK.equals(action)) onBack(buyer, ticket);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PurchaseHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof PurchaseHolder)) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player buyer = (Player) event.getPlayer();
        javaOpen.remove(buyer.getUniqueId());
        // Closing without confirming (ESC) is the Back path: the
        // ticket dies and the token is cleared. After a Confirm or
        // Back the ticket is already gone, so this is a no-op there.
        Ticket ticket = tickets.remove(buyer.getUniqueId());
        if (ticket != null) plugin.purchaseCancel(buyer);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        invalidate(event.getPlayer().getUniqueId());
    }

    // ---------- refresh + lifecycle ----------

    /**
     * Per-second hook (state task): re-render an open Java review
     * from a coordinator PREVIEW — preview never arms, charges, or
     * extends the token, so the display can track the live balance
     * without ever moving money (the DeathProtection refresh shape).
     */
    public void tickRefresh() {
        if (javaOpen.isEmpty()) return;
        for (UUID id : Set.copyOf(javaOpen)) {
            Ticket ticket = tickets.get(id);
            Player buyer = Bukkit.getPlayer(id);
            if (ticket == null || buyer == null) { javaOpen.remove(id); continue; }
            if (buyer.getOpenInventory() == null
                    || !(buyer.getOpenInventory().getTopInventory().getHolder()
                            instanceof PurchaseHolder)) {
                continue;
            }
            RevivePurchaseCoordinator.Result preview = plugin.purchasePreview(buyer);
            if (preview.status() == RevivePurchaseCoordinator.Status.ARMED) {
                ticket.quote = preview;
                Inventory inv = buildMenu(buyer, preview);
                // re-render in place: swap the three meaningful slots
                Inventory open = buyer.getOpenInventory().getTopInventory();
                for (int slot : new int[]{SLOT_CONFIRM, SLOT_PICTURE, SLOT_BACK}) {
                    open.setItem(slot, inv.getItem(slot));
                }
            }
        }
    }

    /** Drop a buyer's review (quit, buyer downed, feature disable). */
    public void invalidate(UUID buyerId) {
        tickets.remove(buyerId);
        javaOpen.remove(buyerId);
        plugin.purchaseCancelId(buyerId);
        Player p = Bukkit.getPlayer(buyerId);
        if (p != null) closeJavaMenu(p);
    }

    public void invalidateAll() {
        for (UUID id : Set.copyOf(tickets.keySet())) invalidate(id);
        tickets.clear();
        javaOpen.clear();
    }

    public void shutdown() {
        for (UUID id : Set.copyOf(javaOpen)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) closeJavaMenu(p);
        }
        javaOpen.clear();
        tickets.clear();
    }

    private void closeJavaMenu(Player player) {
        if (player.getOpenInventory() != null
                && player.getOpenInventory().getTopInventory().getHolder()
                        instanceof PurchaseHolder) {
            player.closeInventory();
        }
        javaOpen.remove(player.getUniqueId());
    }
}
