package com.winkreyes.excelclaims;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 0.1.2 per-player expiration — the design matrix from
 * DESIGN-0.1.2-per-player-expiration.md (Mike's ruling, Nov 7,
 * 2026). ONE inactivity threshold per owner: their per-player
 * override when set, else the server default; `never` is infinite.
 * GriefPrevention could only run global clocks — per-player is the
 * upgrade Mike named. The 0.1.1 safety floor (unknown basis never
 * expires) is pinned in ExpirationSafetyTest; this file pins the
 * policy shape itself, the config seams, and the /excelclaims
 * expire write path's persistence.
 */
public class PerPlayerExpirationTest {

    static final long DAY = 86_400_000L;
    static final UUID A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final UUID B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    static final UUID C = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @TempDir Path tmp;

    private static void addClaim(Core core, long id, UUID owner, long stamp, long lastSeenAt) {
        ClaimRecord r = new ClaimRecord();
        r.id = id; r.world = "world";
        r.lx = (int) (id * 20); r.ly = 64; r.lz = 0;
        r.gx = (int) (id * 20) + 9; r.gy = 80; r.gz = 9;
        r.owner = owner; r.parentId = -1;
        r.createdAt = stamp; r.lastModifiedAt = stamp;
        core.store().put(r);
        core.store().saveClaim(r);
        core.store().playerData(owner).lastSeenAt = lastSeenAt;
    }

    // ------------------------------------------------------------ the policy matrix

    @Test
    void defaultNeverExpiresNothingEvenWithAKnownAncientAbsence() throws Exception {
        // The live state Mike chose: default `never`. The basis is
        // KNOWN here — 20,000 days of proven absence — and the
        // claim still survives. Only the setting saves it, which
        // is exactly the point: expiration is opt-in per setting,
        // never a background hazard.
        long now = System.currentTimeMillis();
        Core core = TestKit.newCore(tmp.resolve("store"));
        addClaim(core, 0, A, now - 20_000 * DAY, now - 20_000 * DAY);
        core.service().runExpirationIfDue(); // no config touched: field default is never
        assertNotNull(core.store().byId(0), "default never: a known 20,000-day absence expires nothing");
    }

    @Test
    void perPlayerThirtyExpiresAtThirtyOneSurvivesAtTwentyNine() throws Exception {
        long now = System.currentTimeMillis();
        Core core = TestKit.newCore(tmp.resolve("store"));
        addClaim(core, 0, A, now - 31 * DAY, now - 31 * DAY);
        addClaim(core, 1, B, now - 29 * DAY, now - 29 * DAY);
        core.config().setExpirationOverride(A, 30);
        core.config().setExpirationOverride(B, 30);
        core.service().runExpirationIfDue();
        assertNull(core.store().byId(0), "31 days away on a 30-day setting: expired");
        assertNotNull(core.store().byId(1), "29 days away on a 30-day setting: survives");
    }

    @Test
    void perPlayerOverrideBeatsAFiniteDefault() throws Exception {
        long now = System.currentTimeMillis();
        Core core = TestKit.newCore(tmp.resolve("store"));
        addClaim(core, 0, A, now - 45 * DAY, now - 45 * DAY);
        addClaim(core, 1, B, now - 45 * DAY, now - 45 * DAY);
        core.config().expirationDefaultDays = 60;
        core.config().setExpirationOverride(A, 30);
        core.service().runExpirationIfDue();
        assertNull(core.store().byId(0), "override 30 beats default 60: 45 days away expires");
        assertNotNull(core.store().byId(1), "no override: the 60-day default judges, 45 days survives");
    }

    @Test
    void perPlayerNeverBeatsAFiniteDefault() throws Exception {
        long now = System.currentTimeMillis();
        Core core = TestKit.newCore(tmp.resolve("store"));
        addClaim(core, 0, A, now - 5_000 * DAY, now - 5_000 * DAY);
        core.config().expirationDefaultDays = 30;
        core.config().setExpirationOverride(A, ClaimsConfig.EXPIRATION_NEVER);
        core.service().runExpirationIfDue();
        assertNotNull(core.store().byId(0), "per-player never beats a finite default at any absence");
    }

    // ------------------------------------------------------------ value parsing

    @Test
    void configValueParserRules() {
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, ClaimsConfig.tryParseExpirationDays("never"));
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, ClaimsConfig.tryParseExpirationDays("  NEVER "));
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, ClaimsConfig.tryParseExpirationDays(-1),
                "the design's -1 spelling of never");
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, ClaimsConfig.tryParseExpirationDays(-90),
                "any negative normalizes to never");
        assertEquals(60, ClaimsConfig.tryParseExpirationDays(60));
        assertEquals(45, ClaimsConfig.tryParseExpirationDays("45"));
        assertNull(ClaimsConfig.tryParseExpirationDays(null), "absent = not set");
        assertNull(ClaimsConfig.tryParseExpirationDays("junk"), "junk = not set");
        assertEquals("never", ClaimsConfig.formatExpirationDays(ClaimsConfig.EXPIRATION_NEVER));
        assertEquals("30", ClaimsConfig.formatExpirationDays(30));
    }

    @Test
    void commandValueParserRules() {
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, Commands.parseExpireValue("never"));
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, Commands.parseExpireValue("Never"));
        assertEquals(30, Commands.parseExpireValue("30"));
        assertEquals(7, Commands.parseExpireValue(" 7 "));
        assertNull(Commands.parseExpireValue("0"), "zero rejected: not a silent expire-now or never");
        assertNull(Commands.parseExpireValue("-1"), "negatives rejected at the command: spell never out");
        assertNull(Commands.parseExpireValue("abc"));
        assertNull(Commands.parseExpireValue(null));
    }

    // ------------------------------------------------------------ config load seams

    @Test
    void retiredKeyConfigLoadsCleanAndStaysInert() throws Exception {
        // The 0.1.1 recovery freeze, verbatim shape: the old keys
        // at 99999. 0.1.2 never reads them — the config loads
        // clean and nobody's claims expire behind them.
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("""
                expiration:
                  check-interval-minutes: 60
                  chest-claim-days: 99999
                  unused-claim-days: 99999
                  all-claims-days-inactive: 99999
                  except-when-owner-has-total-claim-blocks: 10000
                  except-when-owner-has-bonus-claim-blocks: 5000
                """);
        ClaimsConfig cc = new ClaimsConfig();
        cc.load(y);
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc.effectiveExpirationDays(A),
                "no default-days key: the never default stands, retired keys ignored");

        // And when a retired key carries a DANGEROUS value, it is
        // still ignored — retirement means retirement.
        YamlConfiguration y2 = new YamlConfiguration();
        y2.loadFromString("""
                expiration:
                  chest-claim-days: 1
                  unused-claim-days: 1
                  all-claims-days-inactive: 1
                """);
        ClaimsConfig cc2 = new ClaimsConfig();
        cc2.load(y2);
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc2.effectiveExpirationDays(A),
                "retired 1-day clocks must not reach the sweep");
    }

    @Test
    void playersSectionParsesFromConfig() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        y.loadFromString("""
                expiration:
                  default-days: never
                  players:
                    %s: never
                    %s: 30
                    not-a-uuid: 10
                    %s: junk
                """.formatted(A, B, C));
        ClaimsConfig cc = new ClaimsConfig();
        cc.load(y);
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc.effectiveExpirationDays(A));
        assertTrue(cc.hasExpirationOverride(A));
        assertEquals(30, cc.effectiveExpirationDays(B));
        assertTrue(cc.hasExpirationOverride(B));
        assertFalse(cc.hasExpirationOverride(C), "a junk value is no override — the default judges");
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc.effectiveExpirationDays(C));
        assertEquals(2, cc.expirationPlayerDays.size(), "bad UUID keys and junk values are skipped");
    }

    // ------------------------------------------------------------ the expire write path

    @Test
    void expireSetReportRoundTripPersistsToTheConfigFile() throws Exception {
        YamlConfiguration y = new YamlConfiguration();
        ClaimsConfig cc = new ClaimsConfig();
        cc.load(y);
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc.effectiveExpirationDays(A),
                "report before any set: the never default");
        assertFalse(cc.hasExpirationOverride(A));

        // The command's write seam, exactly as /excelclaims expire runs it.
        cc.applyExpirationOverride(y, A, 30);
        assertEquals(30, cc.effectiveExpirationDays(A), "the set applies live, in memory");
        assertTrue(cc.hasExpirationOverride(A));
        assertEquals("30", y.getString("expiration.players." + A), "and lands in the config values");

        File file = tmp.resolve("config.yml").toFile();
        y.save(file);
        YamlConfiguration y2 = YamlConfiguration.loadConfiguration(file);
        ClaimsConfig cc2 = new ClaimsConfig();
        cc2.load(y2);
        assertEquals(30, cc2.effectiveExpirationDays(A), "the set survives a save/reload round trip");
        assertTrue(cc2.hasExpirationOverride(A), "as an override, not a changed default");
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc2.effectiveExpirationDays(B),
                "other players still ride the default");

        cc2.applyExpirationOverride(y2, A, ClaimsConfig.EXPIRATION_NEVER);
        y2.save(file);
        ClaimsConfig cc3 = new ClaimsConfig();
        cc3.load(YamlConfiguration.loadConfiguration(file));
        assertEquals(ClaimsConfig.EXPIRATION_NEVER, cc3.effectiveExpirationDays(A));
        assertTrue(cc3.hasExpirationOverride(A), "an explicit never is an override, distinct from absent");
    }
}

// ExcelClaims tests — Iris / Mike
