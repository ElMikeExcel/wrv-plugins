import com.winkreyes.excelcosmetics.core.AtomicWrite;
import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.Grant;
import com.winkreyes.excelcosmetics.core.GrantStore;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;
import com.winkreyes.excelcosmetics.core.PurchaseService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * 0.1.3 regression suite for the CONFIRMED "refund while retaining"
 * defect. The applier fake here MUTATES the real GrantStore before
 * its persist step fails — the exact blind spot of the shipped
 * 0.1.2 suite, whose FakeApplier mutated nothing on failure.
 * Production order is exercised through GrantStore.grantAndPersist,
 * the same method ExcelCosmetics.applyAndPersist delegates to.
 */
public final class TestCompensation {

    private static final long T0 = 1_700_000_000_000L;
    private static final long WEEK = 7L * 24 * 3600 * 1000;

    /** Production-order applier: grant in memory FIRST, persist SECOND. */
    static final class MutatingApplier implements PurchaseService.GrantApplier {
        final GrantStore store;
        final Catalog catalog;
        boolean persistWorks = true;
        MutatingApplier(GrantStore store, Catalog catalog) { this.store = store; this.catalog = catalog; }
        public boolean apply(UUID p, Kind k, String id, int w, boolean own) {
            long duration = own ? 0 : catalog.prices().rentalMillis(w);
            return store.grantAndPersist(p, k, id, System.currentTimeMillis(), duration, own, () -> persistWorks);
        }
    }

    public static void run() throws Exception {
        Catalog catalog = TestCatalog.build();
        System.out.println("[compensation]");

        // Failed-save initial purchase (existing player, other grants intact):
        // no new grant, no equip change, refund exact.
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            store.grant(buyer, Kind.COLOR, "sky", T0, WEEK, false); // pre-existing, auto-equips sky
            TestPurchase.FakeEconomy eco = new TestPurchase.FakeEconomy();
            eco.balances.put(buyer, 1000.0);
            MutatingApplier app = new MutatingApplier(store, catalog);
            app.persistWorks = false;
            PurchaseService.Result r = new PurchaseService(catalog, eco, app)
                    .buy(buyer, Kind.COLOR, "pink", 1, false);
            Check.eq(PurchaseService.Code.GRANT_FAILED_REFUNDED, r.code(), "failed-save purchase refunds");
            Check.close(1000.0, eco.balance(buyer), "refund exact after failed save");
            PlayerState s = store.peek(buyer);
            Check.ok(s != null, "pre-existing player state retained");
            Check.ok(s.findGrant(Kind.COLOR, "pink") == null, "failed-save purchase leaves NO grant");
            Check.eq(1, s.grants().size(), "only the pre-existing grant remains");
            Check.eq("sky", s.equippedColorId(), "equip unchanged after failed save");
        }

        // Failed-save first-ever purchase: no player state retained at all.
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            boolean ok = store.grantAndPersist(buyer, Kind.COLOR, "pink", T0, WEEK, false, () -> false);
            Check.ok(!ok, "first-ever failed save returns false");
            Check.ok(store.peek(buyer) == null, "first-ever failed save retains NO player state");
            Check.eq(0, store.playerCount(), "store empty after first-ever compensation");
        }

        // Failed-save active renewal: expiry unchanged.
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            store.grant(buyer, Kind.COLOR, "pink", T0, WEEK, false);
            boolean ok = store.grantAndPersist(buyer, Kind.COLOR, "pink", T0 + 1000, WEEK, false, () -> false);
            Check.ok(!ok, "failed renewal returns false");
            Grant g = store.peek(buyer).findGrant(Kind.COLOR, "pink");
            Check.eq(T0 + WEEK, g.expiresAtMillis(), "failed renewal: expiry unchanged");
            Check.eq(T0, g.grantedAtMillis(), "failed renewal: grant start unchanged");
        }

        // Failed-save lapsed renewal: the old lapsed grant is restored wholesale.
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            store.grant(buyer, Kind.COLOR, "pink", T0, WEEK, false);
            long later = T0 + 2 * WEEK;
            boolean ok = store.grantAndPersist(buyer, Kind.COLOR, "pink", later, WEEK, false, () -> false);
            Check.ok(!ok, "failed lapsed renewal returns false");
            Grant g = store.peek(buyer).findGrant(Kind.COLOR, "pink");
            Check.eq(T0, g.grantedAtMillis(), "failed lapsed renewal: original grant start restored");
            Check.eq(T0 + WEEK, g.expiresAtMillis(), "failed lapsed renewal: original expiry restored");
        }

        // Failed-save own-upgrade: still a rental, not permanent.
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            store.grant(buyer, Kind.COLOR, "pink", T0, WEEK, false);
            boolean ok = store.grantAndPersist(buyer, Kind.COLOR, "pink", T0 + 1000, 0, true, () -> false);
            Check.ok(!ok, "failed own-upgrade returns false");
            Grant g = store.peek(buyer).findGrant(Kind.COLOR, "pink");
            Check.ok(!g.isPermanent(), "failed own-upgrade: still a rental");
            Check.eq(T0 + WEEK, g.expiresAtMillis(), "failed own-upgrade: rental expiry unchanged");
        }

        // Success-after-failure: a later healthy persist must NOT resurrect
        // the compensated grant (the retained state is the restored state).
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            boolean ok1 = store.grantAndPersist(buyer, Kind.COLOR, "pink", T0, WEEK, false, () -> false);
            Check.ok(!ok1, "first attempt fails and compensates");
            boolean ok2 = store.grantAndPersist(buyer, Kind.COLOR, "sky", T0, WEEK, false, () -> true);
            Check.ok(ok2, "second (healthy) purchase succeeds");
            PlayerState s = store.peek(buyer);
            Check.ok(s.findGrant(Kind.COLOR, "pink") == null, "healthy save does NOT resurrect compensated grant");
            Check.ok(s.findGrant(Kind.COLOR, "sky") != null, "healthy purchase grant present");
            Check.eq("sky", s.equippedColorId(), "healthy purchase auto-equips");
        }

        // Successful grantAndPersist keeps the grant (control).
        {
            GrantStore store = new GrantStore();
            UUID buyer = UUID.randomUUID();
            boolean ok = store.grantAndPersist(buyer, Kind.COLOR, "pink", T0, WEEK, false, () -> true);
            Check.ok(ok, "healthy persist returns true");
            Check.ok(store.peek(buyer).findGrant(Kind.COLOR, "pink") != null, "healthy persist keeps grant");
        }

        // Atomic save: successful write replaces; a failed tmp write
        // (tmp path blocked by a directory) leaves the last good file intact.
        {
            Path dir = Files.createTempDirectory("ec013-atomic");
            Path target = dir.resolve("players.yml");
            Check.ok(AtomicWrite.writeAtomically(target, "good-v1"), "atomic write succeeds");
            Check.eq("good-v1", Files.readString(target), "atomic write content lands");
            Check.ok(AtomicWrite.writeAtomically(target, "good-v2"), "atomic replace succeeds");
            Check.eq("good-v2", Files.readString(target), "atomic replace content lands");
            Files.createDirectory(AtomicWrite.tmpPath(target)); // block the tmp write
            Check.ok(!AtomicWrite.writeAtomically(target, "corrupt-partial"), "blocked tmp write returns false");
            Check.eq("good-v2", Files.readString(target), "failed tmp write leaves last good file intact");
        }
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
