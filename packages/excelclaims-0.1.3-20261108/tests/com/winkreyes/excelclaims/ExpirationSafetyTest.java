package com.winkreyes.excelclaims;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Nov 7, 2026 expiration incident, pinned: 0.1.0 imported the
 * live GriefPrevention store with every date zero (GP files carry
 * none), and the expiration sweep read "days away" as ~20,000 and
 * expired all nine player claims at the first due tick after the
 * import. These tests pin the 0.1.1 fixes: (A) the importer seeds
 * the dates GP never wrote — claim created/modified from the claim
 * file's mtime, player last-seen from Bukkit's record or the player
 * file's mtime; (B) the sweep takes the newest evidence across all
 * sources and NEVER expires a claim whose last-seen basis is
 * unknown, while genuinely absent owners still expire on schedule.
 *
 * 0.1.2 (Mike's ruling, Nov 7, 2026): the three global clocks and
 * the block-count exemptions this file originally pinned are gone,
 * replaced by ONE per-player threshold (default + per-player
 * overrides, `never` = infinite). The safety floor is unchanged —
 * the incident shape below now runs under an explicitly FINITE
 * per-player setting, the strongest form of the pin — and the two
 * tests that pinned the retired trio/exemption semantics are
 * re-pinned here in their new shape: chest claims follow the same
 * single rule as every claim, and block counts exempt nobody
 * (a per-player `never` is the exemption now). The full design
 * matrix lives in PerPlayerExpirationTest.
 */
public class ExpirationSafetyTest {

    static final long DAY = 86_400_000L;
    static final UUID OWNER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    static final long MTIME = 1_700_000_000_000L; // a whole-second instant, 2023-11-14

    @TempDir Path tmp;

    // ------------------------------------------------------------ basis math (fix B)

    @Test
    void basisIsTheNewestEvidenceAcrossAllSources() {
        assertEquals(0, ClaimService.lastSeenBasis(0, 0, 0, 0), "all sources silent = unknown");
        assertEquals(123, ClaimService.lastSeenBasis(0, 123, 0, 0), "Bukkit alone rescues");
        assertEquals(100, ClaimService.lastSeenBasis(0, 0, 100, 50), "claim stamps alone");
        assertEquals(999, ClaimService.lastSeenBasis(999, 0, 0, 0), "stored stamp alone");
        assertEquals(900, ClaimService.lastSeenBasis(100, 900, 500, 0), "newest wins across sources");
        assertEquals(700, ClaimService.lastSeenBasis(700, 0, 650, 600),
                "a newer stored stamp is never dragged down by older claim stamps");
    }

    @Test
    void seedChoicePrefersBukkitThenFileMtimeThenUnknown() {
        assertEquals(5000, GpImporter.seedLastSeenAt(5000, 9000), "Bukkit record wins");
        assertEquals(9000, GpImporter.seedLastSeenAt(0, 9000), "file mtime is the fallback");
        assertEquals(0, GpImporter.seedLastSeenAt(0, 0), "both silent = unknown (safe: never expires)");
        assertEquals(0, GpImporter.seedLastSeenAt(-1, -1), "garbage reads as unknown, never as a date");
    }

    // ------------------------------------------------------------ importer seeding (fix A)

    private File gpDirWithOneClaim(String claimBody, long claimMtime,
                                   int accrued, long playerMtime) throws Exception {
        File dir = tmp.resolve("gp").toFile();
        File claims = new File(dir, "ClaimData");
        claims.mkdirs();
        File claimFile = new File(claims, "0.yml");
        Files.writeString(claimFile.toPath(), claimBody, StandardCharsets.UTF_8);
        Files.setLastModifiedTime(claimFile.toPath(), FileTime.fromMillis(claimMtime));
        Files.writeString(new File(claims, "_nextClaimID").toPath(), "1");
        File players = new File(dir, "PlayerData");
        players.mkdirs();
        File playerFile = new File(players, OWNER.toString());
        Files.writeString(playerFile.toPath(), "\n" + accrued + "\n0\n", StandardCharsets.UTF_8);
        Files.setLastModifiedTime(playerFile.toPath(), FileTime.fromMillis(playerMtime));
        return dir;
    }

    private static final String DATELESS_CLAIM = """
            Lesser Boundary Corner: world;0;64;0
            Greater Boundary Corner: world;9;80;9
            Owner: %s
            Builders: []
            Containers: []
            Accessors: []
            Managers: []
            Parent Claim ID: -1
            inheritNothing: false
            """.formatted(OWNER);

    @Test
    void importerSeedsDatelessClaimDatesFromTheClaimFileMtime() throws Exception {
        File dir = gpDirWithOneClaim(DATELESS_CLAIM, MTIME, 500, MTIME);
        GpImporter.ImportResult result = new GpImporter().importFrom(dir);
        assertEquals(1, result.claims.size());
        ClaimRecord r = result.claims.get(0);
        assertEquals(MTIME, r.createdAt, "created seeded from the GP file's mtime");
        assertEquals(MTIME, r.lastModifiedAt, "modified seeded from the GP file's mtime");
    }

    @Test
    void importerSeedsPlayerLastSeenFromThePlayerFileMtimeWhenHeadless() throws Exception {
        // Headless: no Bukkit server, so the Bukkit source is silent
        // and the player file's mtime is the seed. (On a live server
        // Bukkit's last-played wins — the seed-choice test above.)
        File dir = gpDirWithOneClaim(DATELESS_CLAIM, MTIME, 500, MTIME + 60_000L);
        GpImporter.ImportResult result = new GpImporter().importFrom(dir);
        assertEquals(1, result.players.size());
        assertEquals(MTIME + 60_000L, result.players.get(0).lastSeenAt);
    }

    @Test
    void importerNeverOverwritesDatesAFileActuallyCarries() throws Exception {
        String dated = DATELESS_CLAIM + "Created At: 12345\nLast Modified At: 23456\n";
        File dir = gpDirWithOneClaim(dated, MTIME, 500, MTIME);
        GpImporter.ImportResult result = new GpImporter().importFrom(dir);
        ClaimRecord r = result.claims.get(0);
        assertEquals(12345, r.createdAt, "a carried created date always wins over mtime");
        assertEquals(23456, r.lastModifiedAt, "a carried modified date always wins over mtime");
    }

    // ------------------------------------------------------------ the sweep itself

    private ClaimRecord claim(long id, long created, long modified) {
        ClaimRecord r = new ClaimRecord();
        r.id = id; r.world = "world";
        r.lx = 0; r.ly = 64; r.lz = 0; r.gx = 9; r.gy = 80; r.gz = 9;
        r.owner = OWNER; r.parentId = -1;
        r.createdAt = created; r.lastModifiedAt = modified;
        return r;
    }

    private Core coreWith(ClaimRecord r, int accrued, long lastSeenAt) throws Exception {
        Core core = TestKit.newCore(tmp.resolve("store"));
        ClaimStore store = core.store();
        store.put(r);
        store.saveClaim(r);
        PlayerRecord pr = store.playerData(OWNER);
        pr.accrued = accrued; // 0.1.2: block counts no longer affect expiration at all
        pr.bonus = 0;
        pr.lastSeenAt = lastSeenAt;
        return core;
    }

    @Test
    void incidentShapeNeverExpires() throws Exception {
        // THE incident shape, verbatim: an imported claim and owner
        // whose every date is zero and who has no Bukkit record.
        // 0.1.0 expired this claim; 0.1.1 must not, ever — and
        // 0.1.2 must not either, even under an explicitly finite
        // per-player setting (1 day: the harshest possible judge).
        // Unknown basis beats every setting.
        Core core = coreWith(claim(0, 0, 0), 100, 0);
        core.config().setExpirationOverride(OWNER, 1);
        core.service().runExpirationIfDue();
        assertNotNull(core.store().byId(0), "unknown last-seen basis: the claim survives");
        assertTrue(Files.exists(new File(core.store().claimDir(), "0.yml").toPath()),
                "and its file stays on disk");
    }

    @Test
    void genuinelyAbsentOwnerStillExpires() throws Exception {
        long now = System.currentTimeMillis();
        Core core = coreWith(claim(0, now - 100 * DAY, now - 100 * DAY), 100, now - 100 * DAY);
        core.config().expirationDefaultDays = 60; // a finite server default, set on purpose
        core.service().runExpirationIfDue();
        assertNull(core.store().byId(0), "100 days away on every source: expired");
        assertFalse(Files.exists(new File(core.store().claimDir(), "0.yml").toPath()),
                "and its file is removed");
    }

    @Test
    void activeOwnerSurvivesAncientClaimDates() throws Exception {
        // The case mtime-seeding alone cannot save: a claim nobody
        // has edited in 300 days whose owner played today. The
        // owner's stamp is the newest evidence and must rule — even
        // under a finite default.
        long now = System.currentTimeMillis();
        Core core = coreWith(claim(0, now - 300 * DAY, now - 300 * DAY), 100, now);
        core.config().expirationDefaultDays = 60;
        core.service().runExpirationIfDue();
        assertNotNull(core.store().byId(0));
    }

    @Test
    void chestClaimHasNoShortClockAnymore() throws Exception {
        // 0.1.0/0.1.1 shape: a chest claim whose owner is 30 days
        // away died on the 7-day chest clock. 0.1.2: there is no
        // chest clock — the owner's own setting (here the never
        // default) is the only judge, and it says survive.
        long now = System.currentTimeMillis();
        ClaimRecord r = claim(0, now - 30 * DAY, now - 30 * DAY + 1000);
        r.chestClaim = true;
        Core core = coreWith(r, 100, now - 30 * DAY);
        core.service().runExpirationIfDue();
        assertNotNull(core.store().byId(0), "chest claims follow the same single per-player rule");
    }

    @Test
    void chestClaimExpiresOnTheOwnersOwnThreshold() throws Exception {
        // The other half of the chest re-pin: give the owner a
        // 30-day threshold and a 31-day absence, and the chest
        // claim goes — on the owner's clock, like every claim.
        long now = System.currentTimeMillis();
        ClaimRecord r = claim(0, now - 31 * DAY, now - 31 * DAY + 1000);
        r.chestClaim = true;
        Core core = coreWith(r, 100, now - 31 * DAY);
        core.config().setExpirationOverride(OWNER, 30);
        core.service().runExpirationIfDue();
        assertNull(core.store().byId(0), "chest claim past the owner's own threshold: expired");
    }

    @Test
    void blockCountsNoLongerExemptAnyone() throws Exception {
        // 0.1.0/0.1.1 shape: an owner at the total-blocks exemption
        // (10,000 accrued) was untouchable at any absence. 0.1.2:
        // the exemption retired with the clocks — under a finite
        // setting, a genuinely absent owner expires, blocks or not.
        long now = System.currentTimeMillis();
        Core core = coreWith(claim(0, now - 300 * DAY, now - 300 * DAY), 10_000, now - 300 * DAY);
        core.config().expirationDefaultDays = 60;
        core.service().runExpirationIfDue();
        assertNull(core.store().byId(0), "10,000 blocks no longer exempt: expired on the finite default");
    }

    @Test
    void perPlayerNeverIsTheExemptionNow() throws Exception {
        // The exemption's successor: the same maxed-out owner under
        // a finite default survives untouched once their own entry
        // says never. Protection is a per-player choice now.
        long now = System.currentTimeMillis();
        Core core = coreWith(claim(0, now - 300 * DAY, now - 300 * DAY), 10_000, now - 300 * DAY);
        core.config().expirationDefaultDays = 30;
        core.config().setExpirationOverride(OWNER, ClaimsConfig.EXPIRATION_NEVER);
        core.service().runExpirationIfDue();
        assertNotNull(core.store().byId(0), "per-player never beats a finite default: untouchable");
    }
}

// ExcelClaims tests — Iris / Mike
