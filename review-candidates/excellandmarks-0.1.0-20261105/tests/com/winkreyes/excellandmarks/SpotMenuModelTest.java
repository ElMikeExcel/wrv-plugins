package com.winkreyes.excellandmarks;

import com.winkreyes.excellandmarks.core.Spot;
import com.winkreyes.excellandmarks.core.SpotMenuModel;

import java.util.UUID;

/**
 * The spot menu model (SPEC §6 + Mike's Nov 5 clarification): only the
 * options available to this viewer for this spot appear — an available
 * spot offers Rent/Buy, the owner's rental offers Renew/Buy/Release,
 * grace emphasises Renew with its deadline, and SOMEONE ELSE'S SPOT IS
 * VIEW-ONLY with no action buttons at all.
 */
public final class SpotMenuModelTest {

    static final UUID ME = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID THEM = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    static final long NOW = 1_799_000_000_000L;
    static final long DAY = 86_400_000L;

    public static void main(String[] args) {
        Check ck = new Check("SpotMenuModelTest");
        available(ck);
        ownRented(ck);
        ownGrace(ck);
        ownOwned(ck);
        othersSpot(ck);
        confirms(ck);
        signLines(ck);
        ck.finish();
    }

    static Spot spot(Spot.Mode mode, UUID owner, long paidThrough) {
        Spot s = new Spot("lm-1", "world", 10, 64, 10, "Fountain Corner",
                Spot.Type.PLOT, Spot.Tier.SPOT, 50_000, 1_000_000, 3, NOW - DAY);
        s.mode = mode;
        s.ownerId = owner;
        s.ownerName = owner == null ? null : (owner.equals(ME) ? "Me" : "Them");
        s.paidThroughMs = paidThrough;
        return s;
    }

    static boolean has(SpotMenuModel.Screen s, SpotMenuModel.Btn b) { return s.has(b); }

    static void available(Check ck) {
        SpotMenuModel.Screen sc = SpotMenuModel.mainScreen(spot(Spot.Mode.NONE, null, 0), THEM, NOW, 7, null);
        ck.that(has(sc, SpotMenuModel.Btn.RENT), "available: Rent offered");
        ck.that(has(sc, SpotMenuModel.Btn.BUY), "available: Buy offered");
        ck.that(has(sc, SpotMenuModel.Btn.CLOSE), "available: Close present");
        ck.that(!has(sc, SpotMenuModel.Btn.RENEW) && !has(sc, SpotMenuModel.Btn.RELEASE),
                "available: no Renew/Release (no dead buttons)");
        ck.that(sc.lines().stream().anyMatch(l -> l.contains("$500")), "available: rent price shown");
        ck.that(sc.lines().stream().anyMatch(l -> l.contains("$10,000")), "available: buy price shown");
        ck.that(sc.buttons().get(sc.buttons().size() - 1).id() == SpotMenuModel.Btn.CLOSE, "Close is last");
    }

    static void ownRented(Check ck) {
        SpotMenuModel.Screen sc = SpotMenuModel.mainScreen(spot(Spot.Mode.RENTED, ME, NOW + 10 * DAY), ME, NOW, 7, null);
        ck.that(has(sc, SpotMenuModel.Btn.RENEW), "own rental: Renew offered");
        ck.that(has(sc, SpotMenuModel.Btn.BUY), "own rental: Buy Outright offered");
        ck.that(has(sc, SpotMenuModel.Btn.RELEASE), "own rental: Release offered");
        ck.that(!has(sc, SpotMenuModel.Btn.RENT), "own rental: no Rent (already renting)");
        ck.that(sc.lines().stream().anyMatch(l -> l.contains("Rented by you until")), "own rental: state line names the date");
    }

    static void ownGrace(Check ck) {
        SpotMenuModel.Screen sc = SpotMenuModel.mainScreen(spot(Spot.Mode.RENTED, ME, NOW - 2 * DAY), ME, NOW, 7, null);
        SpotMenuModel.Button first = sc.buttons().get(0);
        ck.that(first.id() == SpotMenuModel.Btn.RENEW, "grace: Renew is the FIRST button");
        ck.that(first.label().startsWith("Renew by"), "grace: Renew label carries the deadline");
        ck.that(sc.lines().stream().anyMatch(l -> l.toLowerCase().contains("grace")), "grace: state line says grace");
    }

    static void ownOwned(Check ck) {
        SpotMenuModel.Screen sc = SpotMenuModel.mainScreen(spot(Spot.Mode.OWNED, ME, 0), ME, NOW, 7, null);
        ck.that(has(sc, SpotMenuModel.Btn.RELEASE), "own owned: Release offered");
        ck.that(!has(sc, SpotMenuModel.Btn.RENEW) && !has(sc, SpotMenuModel.Btn.RENT) && !has(sc, SpotMenuModel.Btn.BUY),
                "own owned: no Renew/Rent/Buy");
    }

    static void othersSpot(Check ck) {
        for (Spot.Mode mode : new Spot.Mode[]{Spot.Mode.RENTED, Spot.Mode.OWNED}) {
            SpotMenuModel.Screen sc = SpotMenuModel.mainScreen(
                    spot(mode, THEM, NOW + 10 * DAY), ME, NOW, 7, null);
            ck.that(!has(sc, SpotMenuModel.Btn.RENT), "other's " + mode + ": no Rent");
            ck.that(!has(sc, SpotMenuModel.Btn.BUY), "other's " + mode + ": no Buy");
            ck.that(!has(sc, SpotMenuModel.Btn.RENEW), "other's " + mode + ": no Renew");
            ck.that(!has(sc, SpotMenuModel.Btn.RELEASE), "other's " + mode + ": no Release");
            ck.eq(1, sc.buttons().size(), "other's " + mode + ": Close is the ONLY button");
            ck.that(sc.lines().stream().anyMatch(l -> l.contains("Them")), "other's " + mode + ": owner name still shown (info view)");
        }
    }

    static void confirms(Check ck) {
        Spot avail = spot(Spot.Mode.NONE, null, 0);
        SpotMenuModel.Screen rent = SpotMenuModel.confirmScreen(avail, SpotMenuModel.Btn.RENT, NOW, 30);
        ck.that(has(rent, SpotMenuModel.Btn.CONFIRM) && has(rent, SpotMenuModel.Btn.BACK), "rent confirm: Confirm + Back");
        ck.that(rent.lines().stream().anyMatch(l -> l.contains("$500.00")), "rent confirm: exact charge shown");
        SpotMenuModel.Screen buyMine = SpotMenuModel.confirmScreen(spot(Spot.Mode.RENTED, ME, NOW + DAY),
                SpotMenuModel.Btn.BUY, NOW, 30);
        ck.that(buyMine.lines().stream().anyMatch(l -> l.toLowerCase().contains("not refunded")),
                "buy-out confirm: states the rental period is not refunded");
        SpotMenuModel.Screen buyFresh = SpotMenuModel.confirmScreen(avail, SpotMenuModel.Btn.BUY, NOW, 30);
        ck.that(buyFresh.lines().stream().noneMatch(l -> l.toLowerCase().contains("not refunded")),
                "fresh buy confirm: no refund note (nothing to refund)");
        SpotMenuModel.Screen rel = SpotMenuModel.confirmScreen(spot(Spot.Mode.RENTED, ME, NOW + DAY),
                SpotMenuModel.Btn.RELEASE, NOW, 30);
        ck.that(rel.lines().stream().anyMatch(l -> l.toLowerCase().contains("redemption box")),
                "plot release confirm: warns about the pack into the redemption box");
    }

    static void signLines(Check ck) {
        String[] a = SpotMenuModel.signLines(spot(Spot.Mode.NONE, null, 0), NOW, 7);
        ck.eqStr("[Landmark]", a[0], "sign L1 is the marker");
        ck.eqStr("Fountain Corner", a[1], "sign L2 is the name");
        ck.eqStr("Available", a[2], "sign L3: Available");
        ck.that(a[3].contains("$500") && a[3].contains("$10,000"), "available sign L4 carries both prices");
        String[] r = SpotMenuModel.signLines(spot(Spot.Mode.RENTED, THEM, NOW + 10 * DAY), NOW, 7);
        ck.eqStr("Them", r[2], "rented sign L3 is the owner name");
        ck.that(r[3].startsWith("Rented til"), "rented sign L4 shows the date");
        String[] g = SpotMenuModel.signLines(spot(Spot.Mode.RENTED, THEM, NOW - DAY), NOW, 7);
        ck.that(g[3].startsWith("Grace til"), "grace sign L4 shows the grace deadline");
        String[] o = SpotMenuModel.signLines(spot(Spot.Mode.OWNED, THEM, 0), NOW, 7);
        ck.eqStr("Owned", o[3], "owned sign L4");
    }
}
