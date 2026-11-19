// SPDX-License-Identifier: MIT
package com.winkreyes.shops;

import static org.junit.jupiter.api.Assertions.*;

import com.winkreyes.shops.checkout.CheckoutService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Console-admin surface (1.3.8; first built as 1.3.5 on the
 * 1.3.4 tree, re-based onto the reconstructed 1.3.7 tree): the SEPARATE /excelshopadmin
 * command — gate arithmetic, argument routing, the exact status
 * texts against fabricated snapshots of the plugin's real state
 * shapes (shops owned / no shops / offline owner / unhealthy
 * registry / paused checkout / live quotes), the read-only
 * guarantee, the CheckoutService pending counters the status
 * reads, the reload reply texts, and the plugin.yml wiring
 * (version, the admin command + node, no loosening of
 * /excelshop). The Bukkit glue (ShopAdminCommand) is exercised
 * on the Paper rig; everything it delegates to is pinned here.
 */
class AdminCommandTest {

    private static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID OTHER = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID SHOP_ONE = UUID.fromString("11111111-2222-3333-4444-555555555555");
    private static final UUID SHOP_TWO = UUID.fromString("99999999-8888-7777-6666-555555555555");

    private static AdminCommand.ShopInfo shopOne() {
        return new AdminCommand.ShopInfo(SHOP_ONE,
                "world 12345678 @ 100,64,200", "world 12345678 @ 101,64,200", 3,
                List.of(new AdminCommand.OfferInfo("TORCH", 600_000, 2304),
                        new AdminCommand.OfferInfo("OAK_LOG", 150, 64)));
    }

    private static AdminCommand.ShopInfo shopTwo() {
        return new AdminCommand.ShopInfo(SHOP_TWO,
                "world 12345678 @ -40,70,90", "world 12345678 @ -40,70,91", 1,
                List.of(new AdminCommand.OfferInfo("DIAMOND", 250_050, 16)));
    }

    private static AdminCommand.StatusData data(List<AdminCommand.ShopInfo> shops) {
        return new AdminCommand.StatusData("Marvin", OWNER, true, "$1,234.56",
                true, 5, shops, false, "", 0);
    }

    // ---- the gate: console allowed, permission holders allowed, others denied

    @Test void gateMatrix() {
        assertTrue(AdminCommand.isAdmin(true, false), "console allowed (even without the node)");
        assertTrue(AdminCommand.isAdmin(true, true), "console with the node allowed");
        assertTrue(AdminCommand.isAdmin(false, true), "in-game admin (node held) allowed");
        assertFalse(AdminCommand.isAdmin(false, false), "non-admin player denied");
    }

    @Test void permissionNodeAndDenialText() {
        assertEquals("excelshops.admin", AdminCommand.PERMISSION);
        assertTrue(AdminCommand.DENIED.contains("excelshops.admin"),
                "denial line names the node it needs");
    }

    // ---- argument routing (what the glue dispatches on)

    @Test void routeStatusAndReload() {
        AdminCommand.Route status = AdminCommand.route(new String[] {"status", "Marvin"});
        assertEquals(AdminCommand.Action.STATUS, status.action());
        assertEquals("Marvin", status.target());
        assertEquals(AdminCommand.Action.STATUS,
                AdminCommand.route(new String[] {"STATUS", "Marvin"}).action(),
                "subcommand matching is case-insensitive");
        assertEquals(AdminCommand.Action.RELOAD,
                AdminCommand.route(new String[] {"reload"}).action());
        assertEquals(AdminCommand.Action.RELOAD,
                AdminCommand.route(new String[] {"RELOAD"}).action());
    }

    @Test void routeMalformedForms() {
        assertEquals(AdminCommand.Action.USAGE, AdminCommand.route(new String[] {}).action());
        assertEquals(AdminCommand.Action.USAGE, AdminCommand.route(null).action());
        assertEquals(AdminCommand.Action.USAGE_STATUS,
                AdminCommand.route(new String[] {"status"}).action(), "status without a target");
        assertEquals(AdminCommand.Action.USAGE_STATUS,
                AdminCommand.route(new String[] {"status", "A", "B"}).action(), "status with extra args");
        assertEquals(AdminCommand.Action.USAGE,
                AdminCommand.route(new String[] {"reload", "extra"}).action(), "reload takes no args");
        assertEquals(AdminCommand.Action.USAGE,
                AdminCommand.route(new String[] {"bogus"}).action(), "unknown subcommand");
        assertEquals(AdminCommand.Action.USAGE,
                AdminCommand.route(new String[] {"grant", "Marvin"}).action(),
                "no grant form exists in v1 (read + reload only)");
        assertEquals(AdminCommand.Action.USAGE,
                AdminCommand.route(new String[] {"delete", "Marvin"}).action(),
                "no delete form exists in v1 (read + reload only)");
    }

    @Test void usageTexts() {
        assertEquals("Usage: /excelshopadmin status <player> | /excelshopadmin reload",
                AdminCommand.USAGE);
        assertEquals("Usage: /excelshopadmin status <player>", AdminCommand.USAGE_STATUS);
    }

    // ---- status formatting

    @Test void statusForPlayerWithShops() {
        List<String> lines = AdminCommand.statusLines(data(List.of(shopOne(), shopTwo())));
        assertEquals("ExcelShops status for Marvin (" + OWNER + "):", lines.get(0));
        assertEquals("Online: yes", lines.get(1));
        assertEquals("Balance: $1,234.56", lines.get(2));
        assertEquals("Shops owned: 2 (of 5 registered).", lines.get(3));
        assertEquals("Shop 11111111 rev 3 at chest world 12345678 @ 100,64,200 "
                + "(sign world 12345678 @ 101,64,200): 2 offer(s)", lines.get(4));
        assertEquals("  - TORCH @ $6,000.00 each, stock cap 2304", lines.get(5));
        assertEquals("  - OAK_LOG @ $1.50 each, stock cap 64", lines.get(6));
        assertEquals("Shop 99999999 rev 1 at chest world 12345678 @ -40,70,90 "
                + "(sign world 12345678 @ -40,70,91): 1 offer(s)", lines.get(7));
        assertEquals("  - DIAMOND @ $2,500.50 each, stock cap 16", lines.get(8));
        assertEquals("Pending: none (no active quotes; checkout is running).", lines.get(9));
        assertEquals(10, lines.size());
    }

    @Test void statusForPlayerWithoutShops() {
        List<String> lines = AdminCommand.statusLines(data(List.of()));
        assertEquals("Shops owned: 0 (of 5 registered).", lines.get(3));
        assertEquals("No shops owned.", lines.get(4));
        assertEquals(6, lines.size(), "header, online, balance, owned, no-shops, pending");
    }

    @Test void statusForOfflineOwner() {
        AdminCommand.StatusData offline = new AdminCommand.StatusData(
                "Marvin", OWNER, false, "$0.00", true, 5, List.of(shopOne()), false, "", 0);
        List<String> lines = AdminCommand.statusLines(offline);
        assertEquals("Online: no", lines.get(1),
                "an offline owner resolved from the registry still gets a full status");
        assertEquals("Shops owned: 1 (of 5 registered).", lines.get(3));
    }

    @Test void statusWhenRegistryUnhealthy() {
        AdminCommand.StatusData down = new AdminCommand.StatusData(
                "Marvin", OWNER, true, "unavailable", false, 0, List.of(), false, "", 0);
        List<String> lines = AdminCommand.statusLines(down);
        assertEquals("Balance: unavailable", lines.get(2));
        assertEquals("Shops owned: unavailable (the shop registry is not healthy).", lines.get(3));
    }

    @Test void statusWhenCheckoutPaused() {
        AdminCommand.StatusData paused = new AdminCommand.StatusData(
                "Marvin", OWNER, true, "$5.00", true, 5, List.of(), true,
                "Journal failure at WITHDRAWN; inspect transaction x.", 0);
        List<String> lines = AdminCommand.statusLines(paused);
        assertEquals("Pending: CHECKOUT PAUSED for reconciliation: "
                + "Journal failure at WITHDRAWN; inspect transaction x.",
                lines.get(lines.size() - 1), "the pending state an operator must act on is spelled out");
    }

    @Test void statusWithPendingQuotes() {
        AdminCommand.StatusData pending = new AdminCommand.StatusData(
                "Marvin", OWNER, true, "$5.00", true, 5, List.of(), false, "", 2);
        List<String> lines = AdminCommand.statusLines(pending);
        assertEquals("Pending: 2 active quote(s) for this player; checkout is running.",
                lines.get(lines.size() - 1));
    }

    @Test void statusIsReadOnlyAndDeterministic() {
        List<AdminCommand.ShopInfo> shops = new ArrayList<>(List.of(shopOne()));
        AdminCommand.StatusData snapshot = data(shops);
        List<String> first = AdminCommand.statusLines(snapshot);
        List<String> second = AdminCommand.statusLines(snapshot);
        assertEquals(first, second, "same snapshot, same reply");
        assertEquals(1, shops.size(), "the caller's list is untouched by formatting");
        assertThrows(UnsupportedOperationException.class, () -> snapshot.shops().add(shopTwo()),
                "the snapshot itself is immutable");
    }

    @Test void rejectionText() {
        assertEquals("No online player named 'NobodyHereXYZ', no shop owner by that name, "
                + "and it is not a UUID. Nothing was looked up.",
                AdminCommand.rejection("NobodyHereXYZ"));
    }

    // ---- money formatting (must match BalanceDisplay exactly)

    @Test void moneyFormatting() {
        assertEquals("$0.00", AdminCommand.money(0));
        assertEquals("$0.05", AdminCommand.money(5));
        assertEquals("$1.50", AdminCommand.money(150));
        assertEquals("$6,000.00", AdminCommand.money(600_000));
        assertEquals("$2,500.50", AdminCommand.money(250_050));
        assertEquals("-$1.00", AdminCommand.money(-100));
        assertEquals(BalanceDisplay.money(600_000), AdminCommand.money(600_000),
                "admin money format is the display format");
    }

    // ---- reload replies

    @Test void reloadTexts() {
        assertEquals("ExcelShops reloaded: config, spawn catalog (18 offers) "
                + "and mobile catalog (21 products).", AdminCommand.reloadSuccess(18, 21));
        assertEquals("ExcelShops reload failed: bad yaml. Previous config and catalogs are unchanged.",
                AdminCommand.reloadFailure("bad yaml"));
        assertTrue(AdminCommand.reloadFailure(null).contains("unknown error"),
                "a null failure detail never throws");
    }

    // ---- the CheckoutService pending counters status reads

    private static final class StubJournal implements CheckoutService.Journal {
        @Override public boolean requiresRecovery() { return false; }
        @Override public String recoveryReason() { return ""; }
        @Override public void record(CheckoutService.Quote quote, CheckoutService.Stage stage,
                                     String detail) {}
        @Override public void close() {}
        @Override public void closeUnclean() {}
    }

    private static final class StubAdapter implements CheckoutService.Adapter {
        @Override public void assertMainThread() {}
        @Override public CheckoutService.Preparation prepare(CheckoutService.Quote quote) {
            return CheckoutService.Preparation.denied("stub");
        }
        @Override public CheckoutService.MoneyResult withdraw(UUID accountId, long amount) {
            return CheckoutService.MoneyResult.DECLINED;
        }
        @Override public CheckoutService.MoneyResult deposit(UUID accountId, long amount) {
            return CheckoutService.MoneyResult.DECLINED;
        }
    }

    @Test void checkoutPendingCounters() throws IOException {
        try (CheckoutService checkout = new CheckoutService(new StubAdapter(), new StubJournal(),
                Clock.systemUTC(), Duration.ofSeconds(60))) {
            assertEquals(0, checkout.activeQuoteCount());
            assertEquals(0, checkout.activeQuotesFor(OWNER));
            CheckoutService.Quote quote = checkout.quote(OWNER, OTHER, "offer-1", 1, 1, 100, "fp");
            assertEquals(1, checkout.activeQuoteCount());
            assertEquals(1, checkout.activeQuotesFor(OWNER), "the buyer counts as pending");
            assertEquals(1, checkout.activeQuotesFor(OTHER), "the seller counts as pending");
            assertEquals(0, checkout.activeQuotesFor(UUID.randomUUID()), "a bystander has none");
            assertEquals(0, checkout.activeQuotesFor(null), "null never throws");
            assertTrue(checkout.cancel(quote.id(), OWNER));
            assertEquals(0, checkout.activeQuoteCount(), "a cancelled quote is no longer pending");
            assertEquals(0, checkout.activeQuotesFor(OWNER));
        }
    }

    // ---- plugin.yml + source wiring (read off disk; the harness runs from the project dir)

    @Test void pluginYmlWiring() throws Exception {
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("version: '1.3.10'"), "plugin.yml: version is 1.3.10");
        assertTrue(yml.contains("excelshopadmin:"), "plugin.yml: the admin command is declared");
        assertTrue(yml.contains("/excelshopadmin status <player> | /excelshopadmin reload"),
                "plugin.yml: usage names both admin forms");
        assertTrue(yml.contains("excelshops.admin:"), "plugin.yml: admin node declared");
        assertTrue(yml.contains("default: op"), "plugin.yml: admin node defaults to op");
        assertTrue(yml.contains("\ndescription: \""), "plugin.yml: description is double-quoted");
        // The admin command block itself must carry NO command-level permission:
        // the gate is enforced in code so the denial text is ours and the console
        // path is explicit. (The /excelshop block above it keeps its own gate.)
        String adminBlock = yml.substring(yml.indexOf("excelshopadmin:"));
        adminBlock = adminBlock.substring(0, adminBlock.indexOf("permissions:"));
        assertFalse(adminBlock.contains("permission:"), "plugin.yml: no command-level permission "
                + "on excelshopadmin (the gate lives in ShopAdminCommand)");
    }

    @Test void excelshopPlayerGateUnchanged() throws Exception {
        String main = Files.readString(Path.of("src/com/winkreyes/shops/WRVShops.java"));
        assertTrue(main.contains("Use this in game. Recovery has no automatic replay command; "
                + "see WRVShops/README.md."), "/excelshop keeps its exact player-gate line "
                + "(unchanged since 1.3.4, verified byte-identical in the 1.3.7 reconstruction)");
        assertTrue(main.contains("if(!(sender instanceof Player p))"),
                "/excelshop still rejects non-players before anything else");
        assertTrue(main.contains("getCommand(\"excelshopadmin\")"),
                "the admin command is registered at enable");
        String glue = Files.readString(Path.of("src/com/winkreyes/shops/ShopAdminCommand.java"));
        assertTrue(glue.contains("AdminCommand.isAdmin"), "the glue enforces the admin gate");
        assertFalse(main.contains("excelshopadmin") && main.contains("args[0].equalsIgnoreCase(\"status\")"),
                "the admin surface is NOT folded into the /excelshop handler");
    }
}
