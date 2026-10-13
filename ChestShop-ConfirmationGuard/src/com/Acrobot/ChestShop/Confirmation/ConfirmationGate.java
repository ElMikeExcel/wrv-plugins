/*
 * Confirmation guard addition, 2026-10-12.
 * Licensed under GNU LGPL 2.1; see COPYING.LESSER and NOTICE.
 */
package com.Acrobot.ChestShop.Confirmation;

import com.Acrobot.ChestShop.ChestShop;
import com.Acrobot.Breeze.Utils.InventoryUtil;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Containers.AdminInventory;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Events.TransactionEvent;
import com.Acrobot.ChestShop.Listeners.Economy.Plugins.VaultListener;
import com.Acrobot.ChestShop.Listeners.Player.PlayerInteract;
import com.Acrobot.ChestShop.Listeners.PreTransaction.PermissionChecker;
import com.Acrobot.ChestShop.Listeners.PostTransaction.TransactionMessageSender;
import com.Acrobot.ChestShop.Database.Account;
import com.Acrobot.ChestShop.UUIDs.NameManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.GameMode;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Permanent default-deny boundary inside ChestShop's own classloader. An enabled
 * matching adapter owns an unforgeable controller and a single exact-event permit.
 * This is a player-safety boundary, not a sandbox for hostile server plugins.
 */
public final class ConfirmationGate {
    public static final int API_VERSION = 1;
    public static final String BUILD_ID = "chestshop474-confirmation.1";
    public static final int MAX_REQUESTED_ITEMS = 2304;
    private static Controller controller;
    private static Permit active;
    private static boolean preparing;
    private static PreTransactionEvent preparingEvent;
    private static CurrencyTransferEvent acknowledging;
    private ConfirmationGate() {}

    public static int apiVersion() { return API_VERSION; }
    public static String buildId() { return BUILD_ID; }

    @FunctionalInterface
    public interface OfferHandler { void offer(Player player, Sign sign); }

    /** Register the actual enabled ExcelShops plugin object, never a player token. */
    public static Controller register(Plugin owner, OfferHandler handler) {
        requireMainThread();
        Objects.requireNonNull(handler, "handler");
        if (owner == null || !"ExcelShops".equals(owner.getName()) || !owner.isEnabled()
                || Bukkit.getPluginManager().getPlugin("ExcelShops") != owner)
            throw new IllegalArgumentException("Only the active ExcelShops adapter may register");
        if (active != null || preparing) throw new IllegalStateException("A trade is in progress");
        if (controller != null && controller.valid()) throw new IllegalStateException("Adapter already registered");
        controller = new Controller(owner, handler);
        return controller;
    }

    /** Called by the preserved upstream interaction/protection path. Never trades. */
    public static void offer(Player player, Sign sign) {
        if (!Bukkit.isPrimaryThread()) return;
        Controller current = controller;
        if (current == null || !current.valid()) {
            player.sendMessage(ChatColor.RED + "Shop confirmation is unavailable. No trade was made.");
            return;
        }
        try { current.handler.offer(player, sign); }
        catch (RuntimeException failure) {
            player.sendMessage(ChatColor.RED + "Shop confirmation failed. No trade was made.");
            current.owner.getLogger().log(Level.SEVERE, "ChestShop confirmation offer failed", failure);
        }
    }

    /** Unsupported providers/configurations are explicitly closed before validation. */
    public static String unavailableReason() {
        if (!Bukkit.isPrimaryThread()) return "Confirmation requires the main server thread";
        if (Properties.PRICE_PRECISION != 2) return "Confirmation guard 1 requires PRICE_PRECISION=2";
        if (Properties.TAX_AMOUNT != 0.0 || Properties.SERVER_TAX_AMOUNT != 0.0)
            return "Confirmation guard 1 requires both ChestShop tax rates to be zero";
        if (!Properties.SERVER_ECONOMY_ACCOUNT.isEmpty()
                || !new UUID(0, 0).equals(Properties.SERVER_ECONOMY_ACCOUNT_UUID)
                || NameManager.getServerEconomyAccount() != null)
            return "Confirmation guard 1 does not support a server economy account";
        Plugin essentials = Bukkit.getPluginManager().getPlugin("Essentials");
        if (essentials == null || !essentials.isEnabled()
                || !"2.22.0".equals(essentials.getDescription().getVersion()))
            return "Confirmation guard 1 requires verified EssentialsX 2.22.0";
        RegisteredServiceProvider<Economy> registration = Bukkit.getServicesManager().getRegistration(Economy.class);
        Economy provider = registration == null ? null : registration.getProvider();
        if (provider == null || provider != VaultListener.getProvider())
            return "ChestShop and Vault must use the same verified Essentials economy provider";
        String providerClass = provider.getClass().getName();
        if (!providerClass.equals("com.earth2me.essentials.economy.vault.VaultEconomyProvider")
                && !providerClass.equals("net.milkbowl.vault.economy.plugins.Economy_Essentials"))
            return "This economy provider has not been verified for read-only previews";
        return null;
    }

    public static final class Controller implements AutoCloseable {
        private final Plugin owner;
        private final OfferHandler handler;
        private boolean closed;
        private Controller(Plugin owner, OfferHandler handler) { this.owner = owner; this.handler = handler; }
        private boolean valid() {
            return !closed && controller == this && owner.isEnabled()
                    && ChestShop.getPlugin() != null && ChestShop.getPlugin().isEnabled()
                    && Bukkit.getPluginManager().getPlugin("ExcelShops") == owner;
        }
        public boolean isActive() { return Bukkit.isPrimaryThread() && valid(); }

        /**
         * Refreshes the block state, rechecks eligibility, builds explicit quantity,
         * and dispatches PreTransactionEvent once. No currency/item/sign mutation.
         * Null means upstream rejected the trade; unsupported setup throws with reason.
         */
        public PreparedTrade prepare(Sign offeredSign, Player player,
                TransactionEvent.TransactionType type, int requestedItems) {
            requireMainThread();
            if (!valid()) throw new IllegalStateException("Confirmation adapter unavailable");
            if (active != null || preparing) throw new IllegalStateException("Reentrant preparation refused");
            String reason = unavailableReason();
            if (reason != null) throw new IllegalStateException(reason);
            if (offeredSign == null || player == null || !player.isOnline()
                    || !(offeredSign.getBlock().getState() instanceof Sign sign)) return null;
            if (!player.getWorld().equals(sign.getWorld())
                    || player.getLocation().distanceSquared(sign.getLocation().add(.5, .5, .5)) > 36) return null;
            preparing = true;
            try {
                PreTransactionEvent event = PlayerInteract.prepareTrade(sign, player, type, requestedItems);
                if (event == null) return null;
                preparingEvent = event;
                ChestShop.callEvent(event);
                if (event.isCancelled() || event.getExactPrice() == null || event.getExactPrice().signum() < 0
                        || event.getOwnerInventory() == null || event.getClientInventory() == null
                        || event.getStock() == null || event.getStock().length == 0) return null;
                long count = 0;
                for (ItemStack stack : event.getStock()) {
                    if (stack == null || stack.getAmount() <= 0) return null;
                    count += stack.getAmount();
                }
                if (count > MAX_REQUESTED_ITEMS || count > requestedItems) return null;
                return new PreparedTrade(this, new TransactionEvent(event, sign));
            } finally { preparingEvent = null; preparing = false; }
        }

        /** Consumes a newly prepared exact handle before dispatch. Never retries. */
        public CommitResult execute(PreparedTrade trade) {
            requireMainThread();
            if (!valid() || trade == null || trade.controller != this || trade.used)
                return CommitResult.denied("Invalid, replayed, or reentrant confirmation capability");
            trade.used = true;
            if (active != null || preparing) return CommitResult.denied("Reentrant confirmation capability refused");
            String reason = unavailableReason();
            if (reason != null) return CommitResult.denied(reason);
            if (trade.tick != Bukkit.getCurrentTick() || !trade.snapshot.matches(trade.event))
                return CommitResult.denied("Prepared trade is stale or was changed");
            Permit permit = new Permit(trade);
            active = permit;
            try {
                ChestShop.callEvent(trade.event);
                if (permit.itemsCompleted && !permit.inventoryDeltaMatches())
                    fail(trade.event, new IllegalStateException("Inventories changed after the item phase"));
                CommitResult result = new CommitResult(true, permit.moneyAttempted, permit.moneyHandled,
                        permit.itemsAttempted, permit.itemsCompleted, trade.event.isCancelled(), permit.error);
                if (result.succeeded()) trade.receipt = freezeReceipt(permit.transfer);
                return result;
            } catch (RuntimeException | Error failure) {
                fail(trade.event, failure);
                return new CommitResult(true, permit.moneyAttempted, permit.moneyHandled,
                        permit.itemsAttempted, permit.itemsCompleted, true, permit.error);
            } finally { active = null; }
        }
        /**
         * Emit preserved upstream receipts after the adapter has durably committed
         * and verified balances/items. Consumes once before sending, with no trade
         * redispatch. Notification failure does not reverse completed economics.
         */
        public boolean acknowledgeSuccess(PreparedTrade trade) {
            requireMainThread();
            if (trade == null || trade.controller != this || trade.receipt == null || trade.acknowledged
                    || active != null || preparing || acknowledging != null) return false;
            trade.acknowledged = true;
            acknowledging = trade.receipt;
            try {
                TransactionMessageSender.sendAcknowledged(trade.receipt);
                return true;
            } catch (RuntimeException | Error failure) {
                owner.getLogger().log(Level.WARNING, "A committed ChestShop trade receipt could not be delivered", failure);
                return false;
            } finally { acknowledging = null; }
        }
        @Override public void close() {
            requireMainThread();
            closed = true;
            if (controller == this) controller = null;
            // Do not revoke the already-admitted synchronous money/items permit.
        }
    }

    public static final class PreparedTrade {
        private final Controller controller;
        private final TransactionEvent event;
        private final Snapshot snapshot;
        private final Economy provider;
        private final int tick;
        private boolean used;
        private CurrencyTransferEvent receipt;
        private boolean acknowledged;
        private PreparedTrade(Controller controller, TransactionEvent event) {
            this.controller = controller;
            this.event = event;
            this.snapshot = new Snapshot(event);
            this.provider = VaultListener.getProvider();
            this.tick = Bukkit.getCurrentTick();
        }
        /** Read-only inspection. Mutating this event invalidates admission. */
        public TransactionEvent event() { return event; }
    }

    public record CommitResult(boolean admitted, boolean moneyAttempted, boolean moneyHandled,
            boolean itemsAttempted, boolean itemsCompleted, boolean cancelled, String error) {
        public boolean succeeded() { return admitted && moneyHandled && itemsCompleted && !cancelled && error == null; }
        private static CommitResult denied(String error) { return new CommitResult(false, false, false, false, false, true, error); }
    }

    /** Pure exact-identity admission check, including after close/disable mid-commit. */
    public static boolean isAdmitted(TransactionEvent event) {
        return active != null && active.event == event && active.thread == Thread.currentThread() && active.error == null;
    }
    public static boolean isAcknowledging(CurrencyTransferEvent event) {
        return event != null && acknowledging == event && Bukkit.isPrimaryThread();
    }
    public static boolean isPreparing() { return preparing; }
    public static boolean isPrepared(PreTransactionEvent event) {
        return preparing && preparingEvent == event && Bukkit.isPrimaryThread()
                && controller != null && controller.valid();
    }
    public static boolean beginMoney(TransactionEvent event) {
        if (!isAdmitted(event) || event.isCancelled() || active.moneyAttempted) return false;
        if (unavailableReason() != null || !active.providerMatches() || !active.snapshot.matches(event)
                || !active.inventoriesUnchanged() || !active.clientEligible()) {
            fail(event, new IllegalStateException("Trade, provider, access, or player changed before the money phase"));
            return false;
        }
        Inventory source = event.getTransactionType() == TransactionEvent.TransactionType.BUY
                ? event.getOwnerInventory() : event.getClientInventory();
        Inventory destination = event.getTransactionType() == TransactionEvent.TransactionType.BUY
                ? event.getClientInventory() : event.getOwnerInventory();
        if (!containsStorageStock(source, event.getStock()) || !InventoryUtil.hasItems(event.getStock(), source)
                || !InventoryUtil.fits(event.getStock(), destination)) {
            fail(event, new IllegalStateException("Stock or storage capacity changed before the money phase"));
            return false;
        }
        active.moneyAttempted = true;
        return true;
    }
    public static void bindTransfer(TransactionEvent event, CurrencyTransferEvent transfer) {
        if (!isAdmitted(event) || !active.moneyAttempted || active.transfer != null
                || transfer.getTransactionEvent() != event)
            throw new IllegalStateException("Invalid transfer binding");
        active.transfer = transfer;
    }
    private static boolean validTransfer(CurrencyTransferEvent transfer) {
        TransactionEvent event = transfer.getTransactionEvent();
        if (!isAdmitted(event) || active.transfer != transfer || !active.moneyAttempted
                || active.moneyFinished || event.isCancelled()) return false;
        return transfer.getInitiator() == event.getClient()
                && Objects.equals(transfer.getPartner(), active.snapshot.owner)
                && transfer.getDirection() == (event.getTransactionType() == TransactionEvent.TransactionType.BUY
                    ? CurrencyTransferEvent.Direction.PARTNER : CurrencyTransferEvent.Direction.INITIATOR)
                && Objects.equals(transfer.getAmountSent(), active.snapshot.price)
                && Objects.equals(transfer.getAmountReceived(), active.snapshot.price);
    }
    public static boolean beginTax(CurrencyTransferEvent transfer) {
        if (!validTransfer(transfer) || active.taxAttempted) return false;
        active.taxAttempted = true;
        // Guard 1 supports zero taxes only, including changes during a callback.
        if (Properties.TAX_AMOUNT != 0.0 || Properties.SERVER_TAX_AMOUNT != 0.0) {
            fail(active.event, new IllegalStateException("Tax configuration changed during confirmation"));
            return false;
        }
        return true;
    }
    public static boolean beginTransfer(CurrencyTransferEvent transfer) {
        if (!validTransfer(transfer) || active.transferAttempted) return false;
        if (!active.providerMatches() || !active.snapshot.matches(active.event)
                || !active.inventoriesUnchanged() || !active.clientEligible()) {
            fail(active.event, new IllegalStateException("Trade, provider, access, inventory or player changed before debit"));
            return false;
        }
        active.transferAttempted = true;
        return true;
    }
    /** Pin actual debit/deposit/refund to the admitted provider once transfer begins. */
    public static Economy economyForMutation(Economy fallback) {
        return active != null && active.thread == Thread.currentThread() && active.transferAttempted
                && !active.moneyFinished ? active.provider : fallback;
    }
    public static void finishMoney(TransactionEvent event, boolean handled) {
        if (!isAdmitted(event) || !active.moneyAttempted || active.moneyFinished) return;
        active.moneyFinished = true;
        active.moneyHandled = handled && active.transferAttempted;
        if (!active.moneyHandled) {
            event.setCancelled(true);
            if (active.error == null) active.error = "Currency transfer did not complete; check balances before any retry";
        }
    }
    public static boolean beginItems(TransactionEvent event) {
        if (!isAdmitted(event) || event.isCancelled() || !active.moneyFinished
                || !active.moneyHandled || active.itemsAttempted) return false;
        if (!active.snapshot.matches(event) || !active.inventoriesUnchanged()) {
            fail(event, new IllegalStateException("Inventory or trade changed during currency transfer; reconciliation required"));
            return false;
        }
        active.itemsAttempted = true;
        return true;
    }
    public static boolean stackTo64(TransactionEvent event) { return isAdmitted(event) && active.stackTo64; }
    public static void finishItems(TransactionEvent event, int leftovers) {
        if (!isAdmitted(event) || !active.itemsAttempted || active.itemsCompleted) return;
        if (leftovers != 0 || !active.inventoryDeltaMatches()) {
            fail(event, new IllegalStateException("Incomplete item transfer (leftovers=" + leftovers + "); reconciliation required"));
            return;
        }
        active.itemsCompleted = true;
    }
    public static boolean itemsCompleted(TransactionEvent event) { return isAdmitted(event) && active.itemsCompleted; }
    public static void fail(TransactionEvent event, Throwable failure) {
        if (!isAdmitted(event)) return;
        event.setCancelled(true);
        if (active.error == null) active.error = failure.getClass().getSimpleName() + ": " + failure.getMessage();
    }
    private static void requireMainThread() {
        if (!Bukkit.isPrimaryThread()) throw new IllegalStateException("ChestShop confirmation is main-thread only");
    }
    private static CurrencyTransferEvent freezeReceipt(CurrencyTransferEvent transfer) {
        TransactionEvent event = transfer.getTransactionEvent();
        Account account = new Account(event.getOwnerAccount().getName(), event.getOwnerAccount().getShortName(),
                event.getOwnerAccount().getUuid());
        TransactionEvent receipt = new TransactionEvent(event.getTransactionType(), event.getOwnerInventory(),
                event.getClientInventory(), event.getClient(), account, cloneStacks(event.getStock()),
                event.getExactPrice(), event.getSign());
        return new CurrencyTransferEvent(transfer.getAmountSent(), transfer.getAmountReceived(),
                transfer.getInitiator(), transfer.getPartner(), transfer.getDirection(), receipt);
    }
    private static ItemStack[] cloneStacks(ItemStack[] stacks) {
        return Arrays.stream(stacks).map(s -> s == null ? null : s.clone()).toArray(ItemStack[]::new);
    }
    private static Map<ItemStack, Integer> counts(ItemStack[] stacks) {
        Map<ItemStack, Integer> counts = new HashMap<>();
        for (ItemStack item : stacks) {
            if (item == null || (item.getType() == org.bukkit.Material.AIR || item.getType() == org.bukkit.Material.CAVE_AIR || item.getType() == org.bukkit.Material.VOID_AIR) || item.getAmount() <= 0) continue;
            ItemStack key = item.clone(); key.setAmount(1);
            counts.merge(key, item.getAmount(), Math::addExact);
        }
        return counts;
    }
    private static boolean containsStorageStock(Inventory source, ItemStack[] stock) {
        Map<ItemStack, Integer> available = counts(source.getStorageContents());
        for (Map.Entry<ItemStack, Integer> requested : counts(stock).entrySet())
            if (available.getOrDefault(requested.getKey(), 0) < requested.getValue()) return false;
        return true;
    }
    private static boolean deltaMatches(ItemStack[] before, ItemStack[] after, ItemStack[] stock, int direction) {
        Map<ItemStack, Integer> expected = counts(before);
        for (Map.Entry<ItemStack, Integer> item : counts(stock).entrySet())
            expected.merge(item.getKey(), direction * item.getValue(), Math::addExact);
        expected.values().removeIf(value -> value == 0);
        return expected.equals(counts(after));
    }
    private static final class Snapshot {
        final UUID owner;
        final String ownerName;
        final ItemStack[] stock;
        final BigDecimal price;
        final String[] signLines;
        final Location signLocation;
        final String signBlockData;
        Snapshot(TransactionEvent event) {
            owner = event.getOwnerAccount().getUuid(); ownerName = event.getOwnerAccount().getName();
            stock = cloneStacks(event.getStock()); price = event.getExactPrice();
            signLines = event.getSign().getLines().clone();
            signLocation = event.getSign().getLocation().clone();
            signBlockData = event.getSign().getBlockData().getAsString();
        }
        boolean matches(TransactionEvent event) {
            if (!(event.getSign().getBlock().getState() instanceof Sign current)) return false;
            return signLocation.equals(current.getLocation())
                    && signBlockData.equals(current.getBlockData().getAsString())
                    && Arrays.equals(signLines, current.getLines()) && Objects.equals(owner, event.getOwnerAccount().getUuid())
                    && Objects.equals(ownerName, event.getOwnerAccount().getName())
                    && Arrays.equals(stock, event.getStock()) && Objects.equals(price, event.getExactPrice())
                    && Arrays.equals(signLines, event.getSign().getLines());
        }
    }
    private static final class Permit {
        final TransactionEvent event;
        final Snapshot snapshot;
        final Economy provider;
        final Thread thread = Thread.currentThread();
        final boolean stackTo64 = Properties.STACK_TO_64;
        final ItemStack[] ownerBefore;
        final ItemStack[] clientBefore;
        CurrencyTransferEvent transfer;
        boolean moneyAttempted, moneyFinished, moneyHandled, taxAttempted, transferAttempted;
        boolean itemsAttempted, itemsCompleted;
        String error;
        Permit(PreparedTrade trade) {
            event = trade.event; snapshot = trade.snapshot; provider = trade.provider;
            ownerBefore = cloneStacks(event.getOwnerInventory().getContents());
            clientBefore = cloneStacks(event.getClientInventory().getContents());
        }
        boolean providerMatches() {
            RegisteredServiceProvider<Economy> current = Bukkit.getServicesManager().getRegistration(Economy.class);
            return provider != null && VaultListener.getProvider() == provider
                    && current != null && current.getProvider() == provider && provider.isEnabled();
        }
        boolean clientEligible() {
            Player client = event.getClient();
            if (!client.isOnline() || !client.getWorld().equals(snapshot.signLocation.getWorld())
                    || client.getLocation().distanceSquared(snapshot.signLocation.clone().add(.5,.5,.5)) > 36
                    || (Properties.IGNORE_CREATIVE_MODE && client.getGameMode() == GameMode.CREATIVE)) return false;
            if (!(event.getSign().getBlock().getState() instanceof Sign current)) return false;
            return PlayerInteract.canPrepareTrade(current, client)
                    && PermissionChecker.hasTradePermission(client, event.getTransactionType(), current, event.getStock());
        }
        boolean inventoriesUnchanged() {
            return Arrays.equals(ownerBefore, event.getOwnerInventory().getContents())
                    && Arrays.equals(clientBefore, event.getClientInventory().getContents());
        }
        boolean inventoryDeltaMatches() {
            int clientDirection = event.getTransactionType() == TransactionEvent.TransactionType.BUY ? 1 : -1;
            return deltaMatches(clientBefore, event.getClientInventory().getContents(), snapshot.stock, clientDirection)
                    && (event.getOwnerInventory() instanceof AdminInventory
                    || deltaMatches(ownerBefore, event.getOwnerInventory().getContents(), snapshot.stock, -clientDirection));
        }
    }
}
