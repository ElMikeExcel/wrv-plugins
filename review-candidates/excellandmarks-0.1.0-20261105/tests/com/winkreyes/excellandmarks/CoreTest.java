package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.Money;
import com.winkreyes.excellandmarks.core.Registry;
import com.winkreyes.excellandmarks.core.Settings;
import com.winkreyes.excellandmarks.core.Spot;

import java.util.UUID;

/**
 * Core plumbing: the adopted price defaults (Mike, Nov 5, 2026 — these
 * are the config defaults the tests pin so a code edit can't silently
 * change live prices), money formatting, the region test, and the
 * registry's sign index + ownership queries.
 */
public final class CoreTest {

    public static void main(String[] args) {
        Check ck = new Check("CoreTest");
        adoptedPrices(ck);
        money(ck);
        region(ck);
        registry(ck);
        ck.finish();
    }

    static void adoptedPrices(Check ck) {
        Settings s = new Settings();
        ck.eq(50_000, s.rentFor(Spot.Tier.SPOT), "minor spot rent $500");
        ck.eq(150_000, s.rentFor(Spot.Tier.LANDMARK), "landmark rent $1,500");
        ck.eq(300_000, s.rentFor(Spot.Tier.HEART), "heart piece rent $3,000");
        ck.eq(1_000_000, s.buyFor(Spot.Tier.SPOT), "minor spot buy $10,000");
        ck.eq(2_500_000, s.buyFor(Spot.Tier.LANDMARK), "landmark buy $25,000");
        ck.eq(5_000_000, s.buyFor(Spot.Tier.HEART), "heart piece buy $50,000");
        ck.eq(30, s.rentPeriodDays, "rent period 30 days");
        ck.eq(7, s.graceDays, "grace 7 days");
        ck.that(s.sizeFor(Spot.Tier.SPOT) < s.sizeFor(Spot.Tier.LANDMARK)
                && s.sizeFor(Spot.Tier.LANDMARK) < s.sizeFor(Spot.Tier.HEART), "plot sizes grow by tier");
    }

    static void money(Check ck) {
        ck.eqStr("$500.00", Money.fmt(50_000), "fmt whole dollars");
        ck.eqStr("$1,500.50", Money.fmt(150_050), "fmt with cents + grouping");
        ck.eqStr("$10,000.00", Money.fmt(1_000_000), "fmt buy price");
        ck.eqStr("$500", Money.fmtShort(50_000), "fmtShort drops .00");
        ck.eqStr("$1,500.50", Money.fmtShort(150_050), "fmtShort keeps real cents");
        ck.eqStr("$0.05", Money.fmt(5), "fmt sub-dollar");
    }

    static void region(Check ck) {
        Spot s = new Spot("lm-1", "world", 100, 64, 100, "R", Spot.Type.PLOT, Spot.Tier.SPOT,
                1, 1, 3, 0L);
        ck.that(s.inRegion("world", 100, 64, 100, 3, 12), "centre inside");
        ck.that(s.inRegion("world", 103, 61, 97, 3, 12), "corners at the boundary inside");
        ck.that(s.inRegion("world", 100, 76, 100, 3, 12), "top boundary inside");
        ck.that(!s.inRegion("world", 104, 64, 100, 3, 12), "x beyond size outside");
        ck.that(!s.inRegion("world", 100, 60, 100, 3, 12), "below y-below outside");
        ck.that(!s.inRegion("world", 100, 77, 100, 3, 12), "above y-above outside");
        ck.that(!s.inRegion("world_nether", 100, 64, 100, 3, 12), "other world outside");
        Spot lm = new Spot("lm-2", "world", 100, 64, 100, "L", Spot.Type.LANDMARK, Spot.Tier.SPOT,
                1, 1, 0, 0L);
        ck.that(!lm.isPlot() && lm.size == 0, "landmarks carry no region");
    }

    static void registry(Check ck) {
        Registry r = new Registry();
        Spot a = new Spot(r.nextId(), "world", 1, 64, 1, "A", Spot.Type.LANDMARK, Spot.Tier.SPOT, 1, 1, 0, 0L);
        Spot b = new Spot(r.nextId(), "world", 2, 64, 2, "B", Spot.Type.PLOT, Spot.Tier.HEART, 1, 1, 8, 0L);
        ck.eqStr("lm-1", a.id, "first id");
        ck.eqStr("lm-2", b.id, "second id");
        r.add(a);
        r.add(b);
        ck.that(r.bySign("world", 1, 64, 1) == a, "sign index hit");
        ck.that(r.bySign("world", 9, 9, 9) == null, "sign index miss");
        ck.that(r.byId("lm-2") == b, "id index hit");
        UUID u = UUID.randomUUID();
        b.ownerId = u;
        b.mode = Spot.Mode.RENTED;
        ck.eq(1, r.ownedBy(u).size(), "ownedBy finds the rental");
        ck.eq(0, r.ownedBy(UUID.randomUUID()).size(), "ownedBy empty for strangers");
        r.remove("lm-1");
        ck.that(r.bySign("world", 1, 64, 1) == null, "remove clears the sign index");
        Registry restored = new Registry();
        restored.restoreSeq(7);
        ck.eqStr("lm-8", restored.nextId(), "sequence restores past loaded ids");
    }
}
