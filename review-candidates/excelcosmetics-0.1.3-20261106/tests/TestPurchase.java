import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PurchaseService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** The atomicity matrix: a failed charge never grants, a failed
 *  grant never keeps the money. */
public final class TestPurchase {

    static final class FakeEconomy implements PurchaseService.EconomyGateway {
        final Map<UUID, Double> balances = new HashMap<>();
        boolean available = true;
        boolean withdrawWorks = true;
        boolean depositWorks = true;
        int withdrawCalls = 0;
        int depositCalls = 0;
        public boolean available() { return available; }
        public double balance(UUID p) { return balances.getOrDefault(p, 0.0); }
        public boolean withdraw(UUID p, double amt) {
            withdrawCalls++;
            if (!withdrawWorks || balance(p) < amt) return false;
            balances.put(p, balance(p) - amt);
            return true;
        }
        public boolean deposit(UUID p, double amt) {
            depositCalls++;
            if (!depositWorks) return false;
            balances.put(p, balance(p) + amt);
            return true;
        }
    }

    static final class FakeApplier implements PurchaseService.GrantApplier {
        boolean works = true;
        int calls = 0;
        UUID lastPlayer; Kind lastKind; String lastId; int lastWeeks; boolean lastOwn;
        public boolean apply(UUID p, Kind k, String id, int w, boolean own) {
            calls++;
            lastPlayer = p; lastKind = k; lastId = id; lastWeeks = w; lastOwn = own;
            return works;
        }
    }

    public static void run() {
        Catalog catalog = TestCatalog.build();
        System.out.println("[purchase]");
        UUID buyer = UUID.randomUUID();

        // Happy path: charged once, granted once, exact price
        FakeEconomy eco = new FakeEconomy();
        eco.balances.put(buyer, 1000.0);
        FakeApplier app = new FakeApplier();
        PurchaseService svc = new PurchaseService(catalog, eco, app);
        PurchaseService.Result r = svc.buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.ok(r.success(), "happy path succeeds");
        Check.close(800.0, eco.balance(buyer), "charged exactly 200 for a color week");
        Check.eq(1, app.calls, "grant applied exactly once");
        Check.eq(0, eco.depositCalls, "no refund on success");

        // Insufficient funds: NO withdraw attempt, NO grant
        FakeEconomy eco2 = new FakeEconomy();
        eco2.balances.put(buyer, 199.0);
        FakeApplier app2 = new FakeApplier();
        PurchaseService.Result r2 = new PurchaseService(catalog, eco2, app2).buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.eq(PurchaseService.Code.INSUFFICIENT_FUNDS, r2.code(), "short balance = INSUFFICIENT_FUNDS");
        Check.eq(0, eco2.withdrawCalls, "no withdraw attempted when short");
        Check.eq(0, app2.calls, "no grant when short — a failed charge never grants");
        Check.close(199.0, eco2.balance(buyer), "balance untouched when short");

        // Withdraw declines: no grant
        FakeEconomy eco3 = new FakeEconomy();
        eco3.balances.put(buyer, 1000.0);
        eco3.withdrawWorks = false;
        FakeApplier app3 = new FakeApplier();
        PurchaseService.Result r3 = new PurchaseService(catalog, eco3, app3).buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.eq(PurchaseService.Code.CHARGE_FAILED, r3.code(), "declined withdraw = CHARGE_FAILED");
        Check.eq(0, app3.calls, "no grant when the charge fails");

        // Grant fails after charge: refund restores the balance exactly
        FakeEconomy eco4 = new FakeEconomy();
        eco4.balances.put(buyer, 1000.0);
        FakeApplier app4 = new FakeApplier();
        app4.works = false;
        PurchaseService.Result r4 = new PurchaseService(catalog, eco4, app4).buy(buyer, Kind.SET, "healer", 1, false);
        Check.eq(PurchaseService.Code.GRANT_FAILED_REFUNDED, r4.code(), "grant failure = refunded");
        Check.close(1000.0, eco4.balance(buyer), "balance fully restored after refund — a failed grant never charges");
        Check.eq(1, eco4.depositCalls, "exactly one refund deposit");

        // Grant fails AND refund fails: surfaced loudly, never silent
        FakeEconomy eco5 = new FakeEconomy();
        eco5.balances.put(buyer, 1000.0);
        eco5.depositWorks = false;
        FakeApplier app5 = new FakeApplier();
        app5.works = false;
        PurchaseService.Result r5 = new PurchaseService(catalog, eco5, app5).buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.eq(PurchaseService.Code.GRANT_FAILED_REFUND_LOST, r5.code(), "lost refund is its own loud code");

        // No economy: no charge, no grant
        FakeEconomy eco6 = new FakeEconomy();
        eco6.available = false;
        eco6.balances.put(buyer, 1000.0);
        FakeApplier app6 = new FakeApplier();
        PurchaseService.Result r6 = new PurchaseService(catalog, eco6, app6).buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.eq(PurchaseService.Code.NO_ECONOMY, r6.code(), "no economy = NO_ECONOMY");
        Check.eq(0, app6.calls, "no grant without an economy");

        // Unknown SKU: rejected before any money moves
        FakeEconomy eco7 = new FakeEconomy();
        eco7.balances.put(buyer, 1000.0);
        FakeApplier app7 = new FakeApplier();
        PurchaseService.Result r7 = new PurchaseService(catalog, eco7, app7).buy(buyer, Kind.COLOR, "ultraviolet", 1, false);
        Check.eq(PurchaseService.Code.UNKNOWN_SKU, r7.code(), "unknown sku rejected");
        Check.eq(0, eco7.withdrawCalls, "unknown sku never touches money");

        // Weeks + own pricing flow through to the applier
        FakeEconomy eco8 = new FakeEconomy();
        eco8.balances.put(buyer, 100000.0);
        FakeApplier app8 = new FakeApplier();
        PurchaseService svc8 = new PurchaseService(catalog, eco8, app8);
        svc8.buy(buyer, Kind.COLOR, "pink", 3, false);
        Check.close(100000.0 - 600.0, eco8.balance(buyer), "3 weeks charged at 3x the week price");
        Check.eq(3, app8.lastWeeks, "weeks flow to the grant");
        svc8.buy(buyer, Kind.COLOR, "pink", 1, true);
        Check.close(100000.0 - 600.0 - 15000.0, eco8.balance(buyer), "own tier charged at the flat 15000");
        Check.ok(app8.lastOwn, "own flag flows to the grant");

        // 4-week commitment tier: a SINGLE charge at 3.5x, 4 weeks to the grant
        FakeEconomy eco10 = new FakeEconomy();
        eco10.balances.put(buyer, 10000.0);
        FakeApplier app10 = new FakeApplier();
        PurchaseService svc10 = new PurchaseService(catalog, eco10, app10);
        PurchaseService.Result r10 = svc10.buy(buyer, Kind.COLOR, "pink", 4, false);
        Check.ok(r10.success(), "4-week buy succeeds");
        Check.close(700.0, r10.price(), "4-week color priced at 3.5x = 700");
        Check.eq(1, eco10.withdrawCalls, "4-week tier is one single charge, not four");
        Check.close(10000.0 - 700.0, eco10.balance(buyer), "balance after the single 700 charge");
        Check.eq(4, app10.lastWeeks, "4 weeks flow to the grant");
        svc10.buy(buyer, Kind.SET, "tank", 4, false);
        Check.close(10000.0 - 700.0 - 2100.0, eco10.balance(buyer), "4-week set charged 2100 in one charge");
        Check.eq(2, eco10.withdrawCalls, "two buys = two charges total");

        // Own tier: the flat whale price per kind, charged once each
        FakeEconomy eco11 = new FakeEconomy();
        eco11.balances.put(buyer, 100000.0);
        FakeApplier app11 = new FakeApplier();
        PurchaseService svc11 = new PurchaseService(catalog, eco11, app11);
        svc11.buy(buyer, Kind.COLOR, "pink", 1, true);
        Check.close(85000.0, eco11.balance(buyer), "own a color charges flat 15000");
        svc11.buy(buyer, Kind.EMOTES, "command-pack", 1, true);
        Check.close(65000.0, eco11.balance(buyer), "own the emote pack charges flat 20000");
        svc11.buy(buyer, Kind.EFFECT, "bold-name", 1, true);
        Check.close(40000.0, eco11.balance(buyer), "own an effect charges flat 25000");
        svc11.buy(buyer, Kind.SET, "tank", 1, true);
        Check.close(10000.0, eco11.balance(buyer), "own a set charges flat 30000");
        Check.eq(4, eco11.withdrawCalls, "four own buys = four charges");

        // Applier throwing counts as grant failure (refund still happens)
        FakeEconomy eco9 = new FakeEconomy();
        eco9.balances.put(buyer, 1000.0);
        PurchaseService.GrantApplier throwing = (p, k, id, w, own) -> { throw new RuntimeException("disk full"); };
        PurchaseService.Result r9 = new PurchaseService(catalog, eco9, throwing).buy(buyer, Kind.COLOR, "pink", 1, false);
        Check.eq(PurchaseService.Code.GRANT_FAILED_REFUNDED, r9.code(), "throwing applier = refunded failure");
        Check.close(1000.0, eco9.balance(buyer), "balance restored after applier exception");

        // 0.1.2: already owned forever -> refused BEFORE any charge (rent and own)
        FakeEconomy ecoOwned = new FakeEconomy();
        ecoOwned.balances.put(buyer, 100000.0);
        FakeApplier appOwned = new FakeApplier();
        PurchaseService owned = new PurchaseService(catalog, ecoOwned, appOwned,
                (p, k, id) -> k == Kind.COLOR && id.equals("pink"));
        PurchaseService.Result o1 = owned.buy(buyer, Kind.COLOR, "pink", 1, true);
        Check.eq(PurchaseService.Code.ALREADY_OWNED, o1.code(), "re-buying an owned color is refused");
        PurchaseService.Result o2 = owned.buy(buyer, Kind.COLOR, "pink", 4, false);
        Check.eq(PurchaseService.Code.ALREADY_OWNED, o2.code(), "renting an owned color is refused");
        Check.eq(0, ecoOwned.withdrawCalls, "owned refusal never touches the wallet");
        Check.eq(0, appOwned.calls, "owned refusal never grants");
        Check.close(100000.0, ecoOwned.balance(buyer), "balance untouched on owned refusal");
        PurchaseService.Result o3 = owned.buy(buyer, Kind.COLOR, "amethyst", 1, true);
        Check.ok(o3.success(), "owning one color does not block buying another");

        // 0.1.2: retired ids cannot be bought
        FakeEconomy ecoRetired = new FakeEconomy();
        ecoRetired.balances.put(buyer, 100000.0);
        FakeApplier appRetired = new FakeApplier();
        PurchaseService retired = new PurchaseService(catalog, ecoRetired, appRetired, (p, k, id) -> false);
        PurchaseService.Result n1 = retired.buy(buyer, Kind.COLOR, "bubblegum", 1, false);
        Check.eq(PurchaseService.Code.NOT_FOR_SALE, n1.code(), "retired color is not for sale");
        Check.eq(0, ecoRetired.withdrawCalls, "retired refusal never touches the wallet");
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
