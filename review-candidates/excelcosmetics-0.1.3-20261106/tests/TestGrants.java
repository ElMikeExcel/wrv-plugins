import com.winkreyes.excelcosmetics.core.Catalog;
import com.winkreyes.excelcosmetics.core.Grant;
import com.winkreyes.excelcosmetics.core.GrantStore;
import com.winkreyes.excelcosmetics.core.Kind;
import com.winkreyes.excelcosmetics.core.PlayerState;

import java.util.UUID;

/** The rental state machine: stacking, boundaries, own-tier,
 *  set-vs-single overlap — the rules players' money depends on. */
public final class TestGrants {

    private static final long T0 = 1_700_000_000_000L;
    private static final long WEEK = 7L * 24 * 3600 * 1000;

    public static void run() {
        Catalog catalog = TestCatalog.build();
        System.out.println("[grants]");

        // Basic grant + activity window
        PlayerState s = new PlayerState();
        Grant g = s.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Check.eq(T0, g.grantedAtMillis(), "grant starts at grant time");
        Check.eq(T0 + WEEK, g.expiresAtMillis(), "grant ends one week later");
        Check.ok(g.isActiveAt(T0), "active at grant instant");
        Check.ok(g.isActiveAt(T0 + WEEK - 1), "active one ms before expiry");
        Check.ok(!g.isActiveAt(T0 + WEEK), "INACTIVE at the exact expiry instant (boundary)");
        Check.ok(!g.isActiveAt(T0 + WEEK + 1), "inactive after expiry");
        Check.eq("pink", s.equippedColorId(), "first color auto-equips");

        // Renewal stacking: from current expiry, never from now
        PlayerState s2 = new PlayerState();
        s2.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Grant renewed = s2.applyGrant(Kind.COLOR, "pink", T0 + 3 * 24 * 3600 * 1000L, WEEK, false);
        Check.eq(T0 + 2 * WEEK, renewed.expiresAtMillis(), "renewal stacks from current expiry (+1 week)");
        Check.eq(T0, renewed.grantedAtMillis(), "renewal keeps the original grant start");

        // Renewal after lapse starts fresh from now
        PlayerState s3 = new PlayerState();
        s3.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Grant fresh = s3.applyGrant(Kind.COLOR, "pink", T0 + 2 * WEEK, WEEK, false);
        Check.eq(T0 + 3 * WEEK, fresh.expiresAtMillis(), "renewal after lapse starts from now");
        Check.eq(T0 + 2 * WEEK, fresh.grantedAtMillis(), "lapsed renewal resets grant start");

        // 4-week commitment tier: 28 days of duration, same stacking rules
        long fourWeeks = catalog.prices().rentalMillis(Catalog.Prices.COMMITMENT_WEEKS);
        Check.eq(4 * WEEK, fourWeeks, "commitment tier duration is 28 days");
        PlayerState s4w = new PlayerState();
        Grant fresh4 = s4w.applyGrant(Kind.COLOR, "pink", T0, fourWeeks, false);
        Check.eq(T0 + 4 * WEEK, fresh4.expiresAtMillis(), "fresh 4-week buy expires 28 days out");
        PlayerState s4w2 = new PlayerState();
        s4w2.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Grant stacked4 = s4w2.applyGrant(Kind.COLOR, "pink", T0 + 24 * 3600 * 1000L, fourWeeks, false);
        Check.eq(T0 + 5 * WEEK, stacked4.expiresAtMillis(), "4-week buy on an active week stacks from current expiry (+28 days)");
        Check.eq(T0, stacked4.grantedAtMillis(), "4-week stack keeps the original grant start");
        PlayerState s4w3 = new PlayerState();
        s4w3.applyGrant(Kind.COLOR, "pink", T0, fourWeeks, false);
        Grant restacked = s4w3.applyGrant(Kind.COLOR, "pink", T0 + WEEK, fourWeeks, false);
        Check.eq(T0 + 8 * WEEK, restacked.expiresAtMillis(), "4-week on 4-week stacks to 56 days");

        // Own tier: permanent, and a later rental never shortens it
        PlayerState s4 = new PlayerState();
        Grant own = s4.applyGrant(Kind.COLOR, "pink", T0, WEEK, true);
        Check.ok(own.isPermanent(), "own tier is permanent");
        Check.ok(own.isActiveAt(T0 + 520 * WEEK), "owned grant still active in ten years");
        Grant afterRent = s4.applyGrant(Kind.COLOR, "pink", T0 + WEEK, WEEK, false);
        Check.ok(afterRent.isPermanent(), "renting an owned color never shortens it");
        PlayerState s5 = new PlayerState();
        s5.applyGrant(Kind.COLOR, "sky", T0, WEEK, false);
        Grant upgraded = s5.applyGrant(Kind.COLOR, "sky", T0 + 1000, WEEK, true);
        Check.ok(upgraded.isPermanent(), "buying own upgrades a live rental to permanent");
        Check.eq(T0, upgraded.grantedAtMillis(), "upgrade keeps original grant start");

        // Independent grants don't interfere
        PlayerState s6 = new PlayerState();
        s6.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        s6.applyGrant(Kind.COLOR, "sky", T0, 2 * WEEK, false);
        Check.ok(s6.hasActive(Kind.COLOR, "pink", T0 + WEEK - 1), "pink active near its expiry");
        Check.ok(!s6.hasActive(Kind.COLOR, "pink", T0 + WEEK), "pink lapses on its own clock");
        Check.ok(s6.hasActive(Kind.COLOR, "sky", T0 + WEEK), "sky unaffected by pink lapsing");
        Check.eq(2, s6.grants().size(), "both grants held independently");

        // Auto-equip fills empty slots only, never overrides a choice
        PlayerState s7 = new PlayerState();
        s7.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        s7.applyGrant(Kind.COLOR, "sky", T0, WEEK, false);
        Check.eq("pink", s7.equippedColorId(), "second color does NOT override the auto-equipped first");
        s7.setEquipped(Kind.COLOR, "sky");
        s7.applyGrant(Kind.COLOR, "gold", T0, WEEK, false);
        Check.eq("sky", s7.equippedColorId(), "a purchase never overrides a manual equip choice");

        // Set-vs-single overlap: set shadows, then lapses, single resurfaces
        PlayerState s8 = new PlayerState();
        s8.applyGrant(Kind.COLOR, "pink", T0, 4 * WEEK, false);
        s8.applyGrant(Kind.EFFECT, "bold-name", T0, 4 * WEEK, false);
        s8.applyGrant(Kind.SET, "healer", T0, WEEK, false);
        PlayerState.Appearance during = s8.appearance(catalog, T0 + 1000);
        Check.eq("healer", during.setId(), "equipped set drives appearance while active");
        Check.eq(0xFF5555, during.rgb(), "set color paints the line while set is active");
        Check.eq("sparkle-accents", during.effectId(), "set effect replaces the single effect while active");
        PlayerState.Appearance after = s8.appearance(catalog, T0 + WEEK + 1);
        Check.eq(null, after.setId(), "lapsed set no longer drives appearance");
        Check.eq(0xFF55FF, after.rgb(), "individual color resurfaces after the set lapses");
        Check.eq("bold-name", after.effectId(), "individual effect resurfaces after the set lapses");

        // Equipping a second set switches the shadow
        PlayerState s9 = new PlayerState();
        s9.applyGrant(Kind.SET, "healer", T0, WEEK, false);
        s9.applyGrant(Kind.SET, "tank", T0, WEEK, false);
        Check.eq("healer", s9.equippedSetId(), "second set does not auto-override the first");
        s9.setEquipped(Kind.SET, "tank");
        Check.eq(0xCECACA, s9.appearance(catalog, T0).rgb(), "manual set switch repaints immediately");

        // Expiry of the equipped color: appearance falls back silently
        PlayerState s10 = new PlayerState();
        s10.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Check.ok(!s10.appearance(catalog, T0 + 1).isPlain(), "appearance painted while grant lives");
        Check.ok(s10.appearance(catalog, T0 + WEEK).isPlain(), "appearance is plain at expiry — silent fallback");
        int dropped = s10.normalize(T0 + WEEK);
        Check.eq(1, dropped, "normalize drops the lapsed grant");
        Check.eq(null, s10.equippedColorId(), "normalize clears the dead equip pointer");
        Check.eq(0, s10.normalize(T0 + WEEK), "normalize is idempotent");

        // Revoke
        PlayerState s11 = new PlayerState();
        s11.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Check.ok(s11.revoke(Kind.COLOR, "pink"), "revoke removes a held grant");
        Check.ok(!s11.revoke(Kind.COLOR, "pink"), "second revoke reports nothing removed");
        Check.eq(null, s11.equippedColorId(), "revoke clears the equip pointer");
        Check.ok(s11.appearance(catalog, T0).isPlain(), "revoked color no longer paints");

        // Store level: offline grants wait in state; prune counts
        GrantStore store = new GrantStore();
        UUID offline = UUID.randomUUID();
        store.grant(offline, Kind.COLOR, "pink", T0, WEEK, false);
        Check.ok(store.peek(offline).hasActive(Kind.COLOR, "pink", T0 + 1000),
                "grant to an offline (never-seen-online) UUID sits active in state");
        UUID other = UUID.randomUUID();
        store.grant(other, Kind.EFFECT, "bold-name", T0, WEEK, false);
        Check.eq(2, store.pruneAll(T0 + WEEK), "prune drops lapsed grants across players");
        Check.eq(0, store.pruneAll(T0 + WEEK), "second prune finds nothing");

        // setExpiry: admin/rig tool semantics
        PlayerState s13 = new PlayerState();
        s13.applyGrant(Kind.COLOR, "pink", T0, WEEK, false);
        Check.ok(s13.setExpiry(Kind.COLOR, "pink", T0 + 5000), "setExpiry moves a rental's expiry");
        Check.ok(s13.hasActive(Kind.COLOR, "pink", T0 + 4999), "grant alive just before the moved expiry");
        Check.ok(!s13.hasActive(Kind.COLOR, "pink", T0 + 5000), "grant over at the moved expiry");
        Check.ok(!s13.setExpiry(Kind.COLOR, "sky", T0 + 5000), "setExpiry on a missing grant refuses");
        PlayerState s14 = new PlayerState();
        s14.applyGrant(Kind.COLOR, "pink", T0, WEEK, true);
        Check.ok(!s14.setExpiry(Kind.COLOR, "pink", T0 + 5000), "setExpiry refuses a permanent grant — owned is owned");
        // Emote pack activity
        PlayerState s12 = new PlayerState();
        Check.ok(!s12.hasAnyActive(Kind.EMOTES, T0), "no emote pack by default");
        s12.applyGrant(Kind.EMOTES, "command-pack", T0, WEEK, false);
        Check.ok(s12.hasAnyActive(Kind.EMOTES, T0 + WEEK - 1), "emote pack active during its week");
        Check.ok(!s12.hasAnyActive(Kind.EMOTES, T0 + WEEK), "emote pack lapses silently at expiry");
    }
}

// ExcelCosmetics — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
