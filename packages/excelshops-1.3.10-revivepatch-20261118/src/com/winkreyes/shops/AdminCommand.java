// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * The console-admin half of ExcelShops 1.3.8 (first built as the
 * 1.3.5 console-admin build on the 1.3.4 tree, then re-based
 * unchanged onto the reconstructed 1.3.7 tree — see the 1.3.8
 * build record; Mike's Nov 3, 2026 console-admin rule; the
 * console-admin coverage audit of Nov 9, 2026 found ExcelShops
 * a FAIL because /excelshop replies "Use this in game..." to
 * non-players and there was no admin node):
 *
 *   excelshopadmin status <player>
 *   excelshopadmin reload
 *
 * on a SEPARATE command, gated by the excelshops.admin permission
 * (default: op). The server console is an admin by construction
 * (it satisfies op-default permissions in Bukkit/Paper, and the
 * glue also lets a console sender through explicitly, the
 * Landmarks/Cosmetics shape), so both forms run from the
 * Shockbyte console. /excelshop itself is NOT loosened: its
 * player-only gate is byte-for-byte the 1.3.4 one.
 *
 * Deliberately READ + RELOAD ONLY. ExcelShops is money-adjacent
 * (player chest shops + a journalled checkout), so v1 adds no
 * money-moving, grant, revoke, delete, or edit operation. Status
 * only reads the plugin's real state: the ShopRegistry (shops
 * owned, with each shop's offers and prices exactly as
 * registered), the target's Vault balance (read through the same
 * economy provider every display uses), and the CheckoutService
 * pending state (recovery flag + live quote count, via the
 * read-only counters added with this command). Reload swaps config +
 * spawn catalog + mobile catalog only, all-or-nothing; it never
 * touches the registry, the checkout journal, or any shop.
 *
 * No Bukkit dependencies: the gate arithmetic, the argument
 * routing, and the exact reply texts live here and are
 * unit-tested on a plain JVM; the glue (ShopAdminCommand) only
 * resolves names, reads the live state, and delivers messages.
 */
public final class AdminCommand {

    /** The admin permission node (plugin.yml, default: op). */
    public static final String PERMISSION = "excelshops.admin";

    /** Sent to a non-console sender who lacks the admin node. */
    public static final String DENIED =
            "You do not have permission to administer ExcelShops (" + PERMISSION + ").";

    /** Usage for a bare or unknown /excelshopadmin invocation. */
    public static final String USAGE =
            "Usage: /excelshopadmin status <player> | /excelshopadmin reload";

    /** Usage for a malformed admin status invocation. */
    public static final String USAGE_STATUS = "Usage: /excelshopadmin status <player>";

    private AdminCommand() {}

    /** What an /excelshopadmin invocation asks for. */
    public enum Action { STATUS, RELOAD, USAGE, USAGE_STATUS }

    /** A routed invocation: the action plus the status target text (null unless STATUS). */
    public record Route(Action action, String target) {}

    /**
     * Route raw command arguments. Exact shapes only: "status"
     * with exactly one target, or "reload" alone; a malformed
     * status answers with the status usage line and every other
     * shape with the combined usage line. Case-insensitive,
     * matching every other Excel command.
     */
    public static Route route(String[] args) {
        if (args == null) return new Route(Action.USAGE, null);
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            return new Route(Action.RELOAD, null);
        }
        if (args.length >= 1 && args[0].equalsIgnoreCase("status")) {
            if (args.length == 2) return new Route(Action.STATUS, args[1]);
            return new Route(Action.USAGE_STATUS, null);
        }
        return new Route(Action.USAGE, null);
    }

    /**
     * The admin gate: the console always administers (it is the
     * operator channel this surface exists for); any other sender
     * needs the permission node (op by default).
     */
    public static boolean isAdmin(boolean senderIsConsole, boolean hasPermission) {
        return senderIsConsole || hasPermission;
    }

    /**
     * The rejection for a status target that resolves no way at
     * all (not an online player, not a shop owner on record, not
     * a UUID). Nothing was read to produce it beyond the
     * resolution lookups themselves.
     */
    public static String rejection(String target) {
        return "No online player named '" + target + "', no shop owner by that name, "
                + "and it is not a UUID. Nothing was looked up.";
    }

    /** One registered offer, flattened for display (item type name, unit price, stock cap). */
    public record OfferInfo(String itemType, long unitPriceCents, int stockLimit) {}

    /**
     * One owned shop, flattened for display. chest/sign are
     * preformatted position strings supplied by the glue (world
     * + coordinates); offers are in registration order.
     */
    public record ShopInfo(UUID shopId, String chest, String sign, long revision, List<OfferInfo> offers) {
        public ShopInfo { offers = List.copyOf(offers); }
    }

    /**
     * Everything a status reply is built from — a snapshot of the
     * plugin's real state taken by the glue. balanceText is a
     * preformatted amount (e.g. "$1,234.56") or a word like
     * "unavailable"; checkoutReason is only read when
     * checkoutRecovery is true.
     */
    public record StatusData(String displayName, UUID playerId, boolean online, String balanceText,
                             boolean registryHealthy, int totalShops, List<ShopInfo> shops,
                             boolean checkoutRecovery, String checkoutReason, int pendingQuotes) {
        public StatusData { shops = List.copyOf(shops); }
    }

    /**
     * The status reply lines for one player. READ-ONLY by
     * construction: this only formats the snapshot it is given.
     * The shops section names each owned shop (short id,
     * revision, chest/sign positions) and every registered offer
     * with its unit price and stock cap, so an operator can
     * answer "what is this player selling, and at what price"
     * without joining the game. The pending section reports the
     * checkout state: a paused (recovery-required) checkout is
     * the one pending condition that needs an operator, and live
     * quotes are counted, never listed with amounts or cancelled.
     */
    public static List<String> statusLines(StatusData data) {
        String who = (data.displayName() == null || data.displayName().isBlank())
                ? "that player" : data.displayName();
        String id = data.playerId() == null ? "unknown uuid" : data.playerId().toString();
        java.util.ArrayList<String> lines = new java.util.ArrayList<>();
        lines.add("ExcelShops status for " + who + " (" + id + "):");
        lines.add("Online: " + (data.online() ? "yes" : "no"));
        lines.add("Balance: " + (data.balanceText() == null ? "unavailable" : data.balanceText()));
        if (!data.registryHealthy()) {
            lines.add("Shops owned: unavailable (the shop registry is not healthy).");
        } else if (data.shops().isEmpty()) {
            lines.add("Shops owned: 0 (of " + data.totalShops() + " registered).");
            lines.add("No shops owned.");
        } else {
            lines.add("Shops owned: " + data.shops().size()
                    + " (of " + data.totalShops() + " registered).");
            for (ShopInfo shop : data.shops()) {
                String shopId = shop.shopId() == null ? "unknown" : shop.shopId().toString().substring(0, 8);
                lines.add("Shop " + shopId + " rev " + shop.revision()
                        + " at chest " + shop.chest() + " (sign " + shop.sign() + "): "
                        + shop.offers().size() + " offer(s)");
                for (OfferInfo offer : shop.offers()) {
                    lines.add("  - " + offer.itemType() + " @ " + money(offer.unitPriceCents())
                            + " each, stock cap " + offer.stockLimit());
                }
            }
        }
        if (data.checkoutRecovery()) {
            String reason = (data.checkoutReason() == null || data.checkoutReason().isBlank())
                    ? "no reason recorded" : data.checkoutReason();
            lines.add("Pending: CHECKOUT PAUSED for reconciliation: " + reason);
        } else if (data.pendingQuotes() > 0) {
            lines.add("Pending: " + data.pendingQuotes()
                    + " active quote(s) for this player; checkout is running.");
        } else {
            lines.add("Pending: none (no active quotes; checkout is running).");
        }
        return List.copyOf(lines);
    }

    /** The one-line reload success reply, with the reloaded catalog sizes. */
    public static String reloadSuccess(int spawnOffers, int mobileProducts) {
        return "ExcelShops reloaded: config, spawn catalog (" + spawnOffers
                + " offers) and mobile catalog (" + mobileProducts + " products).";
    }

    /**
     * The one-line reload failure reply. A failed reload changes
     * nothing: the glue builds the new catalogs first and swaps
     * only on success, so the previous config and catalogs are
     * still the live ones when this is sent.
     */
    public static String reloadFailure(String detail) {
        String why = (detail == null || detail.isBlank()) ? "unknown error" : detail;
        String separator = why.endsWith(".") ? " " : ". ";
        return "ExcelShops reload failed: " + why + separator
                + "Previous config and catalogs are unchanged.";
    }

    /** Dollar formatting identical to BalanceDisplay.money (US locale, half-up). */
    public static String money(long cents) {
        BigDecimal amount = BigDecimal.valueOf(cents, 2);
        DecimalFormat format = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.US));
        format.setRoundingMode(RoundingMode.HALF_UP);
        return (amount.signum() < 0 ? "-$" : "$") + format.format(amount.abs());
    }
}
