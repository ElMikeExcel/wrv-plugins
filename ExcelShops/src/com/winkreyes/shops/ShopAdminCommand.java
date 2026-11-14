// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import com.winkreyes.shops.store.ShopRecord;
import com.winkreyes.shops.store.ShopRegistry;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Bukkit glue for /excelshopadmin (1.3.8; first built as 1.3.5
 * on the 1.3.4 tree, re-based unchanged onto 1.3.7) — the SEPARATE
 * console-safe admin command the Nov 9, 2026 console-admin audit
 * prescribed for ExcelShops. All decisions and reply texts live
 * in AdminCommand (Bukkit-free, unit-tested); this class only
 * checks the sender, resolves the target, snapshots the live
 * state, and delivers messages.
 *
 * Target resolution is the house shape plus one step this
 * plugin's own state makes honest: an ONLINE player by exact
 * name first, then a raw UUID, then the ShopRegistry's own
 * owner records (a shop stores its owner's name, so an offline
 * shop owner can be looked up by the name on their shops — no
 * name-to-UUID record is invented; a player who owns nothing and
 * is offline is still addressable by UUID only).
 *
 * Status is read-only end to end: it reads the registry, the
 * Vault balance, and the checkout's pending counters, and it
 * creates, moves, and writes nothing.
 */
public final class ShopAdminCommand implements CommandExecutor {
    private final WRVShops plugin;

    public ShopAdminCommand(WRVShops plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!AdminCommand.isAdmin(sender instanceof ConsoleCommandSender,
                sender.hasPermission(AdminCommand.PERMISSION))) {
            sender.sendMessage(AdminCommand.DENIED);
            return true;
        }
        AdminCommand.Route route = AdminCommand.route(args);
        switch (route.action()) {
            case RELOAD -> {
                sender.sendMessage(plugin.reloadAdminData());
                return true;
            }
            case STATUS -> {
                handleStatus(sender, route.target());
                return true;
            }
            case USAGE_STATUS -> {
                sender.sendMessage(AdminCommand.USAGE_STATUS);
                return true;
            }
            default -> {
                sender.sendMessage(AdminCommand.USAGE);
                return true;
            }
        }
    }

    private void handleStatus(CommandSender sender, String targetText) {
        ResolvedTarget target = resolve(sender, targetText);
        if (target == null) return;
        ShopRegistry registry = plugin.getRegistry();
        boolean healthy = registry != null && registry.healthy();
        List<AdminCommand.ShopInfo> shops = new ArrayList<>();
        int total = 0;
        if (healthy) {
            total = registry.all().size();
            for (ShopRecord shop : registry.all()) {
                if (!shop.ownerId().equals(target.uuid())) continue;
                List<AdminCommand.OfferInfo> offers = new ArrayList<>();
                for (var offer : shop.offers()) {
                    offers.add(new AdminCommand.OfferInfo(
                            offer.item().getType().name(), offer.unitPriceCents(), offer.stockLimit()));
                }
                shops.add(new AdminCommand.ShopInfo(shop.id(),
                        describe(shop.chest()), describe(shop.sign()), shop.revision(), offers));
            }
        }
        boolean recovery = plugin.getCheckout() != null && plugin.getCheckout().recoveryRequired();
        String reason = recovery ? plugin.getCheckout().recoveryReason() : "";
        int pending = plugin.getCheckout() == null ? 0 : plugin.getCheckout().activeQuotesFor(target.uuid());
        AdminCommand.StatusData data = new AdminCommand.StatusData(
                target.display(), target.uuid(), target.online(), balanceText(target.uuid()),
                healthy, total, shops, recovery, reason, pending);
        for (String line : AdminCommand.statusLines(data)) sender.sendMessage(line);
    }

    /** A resolved admin target: UUID + the name to show for it + whether they are online. */
    private record ResolvedTarget(UUID uuid, String display, boolean online) {}

    private ResolvedTarget resolve(CommandSender sender, String targetText) {
        Player online = plugin.getServer().getPlayerExact(targetText);
        if (online != null) return new ResolvedTarget(online.getUniqueId(), online.getName(), true);
        try {
            UUID id = UUID.fromString(targetText);
            OfflinePlayer known = plugin.getServer().getOfflinePlayer(id);
            String name = known.getName();
            return new ResolvedTarget(id, (name == null || name.isBlank()) ? targetText : name,
                    known.isOnline());
        } catch (IllegalArgumentException notUuid) {
            // Not a UUID: fall through to the registry's owner records.
        }
        ShopRegistry registry = plugin.getRegistry();
        if (registry != null && registry.healthy()) {
            for (ShopRecord shop : registry.all()) {
                if (shop.ownerName().equalsIgnoreCase(targetText)) {
                    return new ResolvedTarget(shop.ownerId(), shop.ownerName(), false);
                }
            }
        }
        sender.sendMessage(AdminCommand.rejection(targetText));
        return null;
    }

    /** "world <short uuid> @ x,y,z" — enough to walk to the shop; the full ids stay in the header/shop lines. */
    private static String describe(com.winkreyes.shops.store.BlockPos pos) {
        return "world " + pos.worldId().toString().substring(0, 8)
                + " @ " + pos.x() + "," + pos.y() + "," + pos.z();
    }

    /**
     * The target's Vault balance as display text, read from the
     * server's current economy provider exactly as BalanceDisplay
     * reads it for its displays. Display only — never an
     * authorization; "unavailable" whenever the provider, the
     * account, or the value cannot be read.
     */
    private String balanceText(UUID playerId) {
        try {
            RegisteredServiceProvider<Economy> current =
                    plugin.getServer().getServicesManager().getRegistration(Economy.class);
            if (current == null || current.getProvider() == null || !current.getProvider().isEnabled()) {
                return "unavailable";
            }
            Economy economy = current.getProvider();
            OfflinePlayer account = plugin.getServer().getOfflinePlayer(playerId);
            if (!economy.hasAccount(account)) return "unavailable";
            double value = economy.getBalance(account);
            if (!Double.isFinite(value)) return "unavailable";
            long cents = BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP)
                    .movePointRight(2).longValueExact();
            return AdminCommand.money(cents);
        } catch (RuntimeException | LinkageError unavailable) {
            return "unavailable";
        }
    }
}
