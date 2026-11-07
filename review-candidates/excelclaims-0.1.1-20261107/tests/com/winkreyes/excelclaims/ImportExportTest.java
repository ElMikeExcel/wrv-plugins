package com.winkreyes.excelclaims;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Migration round-trip, both ways: synthetic GriefPrevention flat
 * files shaped like the live server's (real owners/corners from the
 * Oct 2026 pulls, plus trust lists, a subdivision, and an admin claim
 * the live set also carries) -> import -> verify every claim, owner,
 * subdivision and trust entry -> export back to GP format -> import
 * again -> verify the model is byte-for-byte the same in substance.
 */
public class ImportExportTest {

    static final String KAILEY = "00000000-0000-0000-0009-01f40ccd59de";
    static final String MARVIN = "8399c623-786f-448c-8b11-f54504721819";
    static final String JESTER = "e739fb4d-7cec-48f4-8a2b-7c25e27d8b9e";
    static final String BUYER = "11111111-2222-3333-4444-555555555555";

    @TempDir Path tmp;

    private void writeClaim(File dir, String name, String content) throws Exception {
        File claims = new File(dir, "ClaimData");
        claims.mkdirs();
        Files.writeString(new File(claims, name).toPath(), content, StandardCharsets.UTF_8);
    }

    private void writePlayer(File dir, String uuid, int accrued, int bonus) throws Exception {
        File players = new File(dir, "PlayerData");
        players.mkdirs();
        Files.writeString(new File(players, uuid).toPath(),
                "\n" + accrued + "\n" + bonus + "\n", StandardCharsets.UTF_8);
    }

    private File syntheticGpDir() throws Exception {
        File dir = tmp.resolve("gp").toFile();
        // Kailey's first-chest claim (live claim 0).
        writeClaim(dir, "0.yml", """
                Lesser Boundary Corner: world;-759;64;-308
                Greater Boundary Corner: world;-751;69;-300
                Owner: %s
                Builders: []
                Containers: []
                Accessors: []
                Managers: []
                Parent Claim ID: -1
                inheritNothing: false
                """.formatted(KAILEY));
        // Marvin's claim, carrying every trust flavor: a UUID builder,
        // public containers, a legacy NAME accessor, a manager.
        writeClaim(dir, "1.yml", """
                Lesser Boundary Corner: world;-1001;43;-672
                Greater Boundary Corner: world;-982;58;-646
                Owner: %s
                Builders:
                - %s
                Containers:
                - public
                Accessors:
                - LegacyNamePlayer
                Managers:
                - %s
                Parent Claim ID: -1
                inheritNothing: false
                """.formatted(MARVIN, KAILEY, JESTER));
        // The spawn admin claim (owner empty).
        writeClaim(dir, "2.yml", """
                Lesser Boundary Corner: world;-64;-64;-64
                Greater Boundary Corner: world;63;319;63
                Owner: ''
                Builders: []
                Containers: []
                Accessors: []
                Managers: []
                Parent Claim ID: -1
                inheritNothing: false
                """);
        // A Landmarks-style plot: subdivision of the admin claim, the
        // buyer holding Build + manager on the subdivision.
        writeClaim(dir, "3.yml", """
                Lesser Boundary Corner: world;-10;64;-10
                Greater Boundary Corner: world;-6;80;-6
                Owner: ''
                Builders:
                - %s
                Containers: []
                Accessors: []
                Managers:
                - %s
                Parent Claim ID: 2
                inheritNothing: false
                """.formatted(BUYER, BUYER));
        Files.writeString(new File(new File(dir, "ClaimData"), "_nextClaimID").toPath(), "4");
        writePlayer(dir, KAILEY, 3492, 0);
        writePlayer(dir, MARVIN, 2468, 250);
        writePlayer(dir, JESTER, 1092, 0);
        return dir;
    }

    private ClaimRecord byId(List<ClaimRecord> claims, long id) {
        return claims.stream().filter(c -> c.id == id).findFirst().orElseThrow();
    }

    @Test
    void importReadsTheWholeLiveShape() throws Exception {
        GpImporter.ImportResult result = new GpImporter().importFrom(syntheticGpDir());
        assertEquals(4, result.claims.size(), "claim count");
        assertEquals(3, result.players.size(), "player count");
        assertEquals(4, result.nextClaimId);
        assertEquals(0, result.unreadableFiles);
        assertEquals(0, result.orphanedSubdivisions);

        ClaimRecord c0 = byId(result.claims, 0);
        assertEquals(UUID.fromString(KAILEY), c0.owner);
        assertEquals(-759, c0.lx); assertEquals(-300, c0.gz);
        assertEquals(81, c0.area());

        ClaimRecord c1 = byId(result.claims, 1);
        assertEquals(Set.of(KAILEY), c1.builders);
        assertEquals(Set.of("public"), c1.containers);
        assertEquals(Set.of("LegacyNamePlayer"), c1.accessors, "legacy name entries survive verbatim");
        assertEquals(Set.of(JESTER), c1.managers);

        ClaimRecord admin = byId(result.claims, 2);
        assertNull(admin.owner, "empty owner = admin claim");
        assertTrue(admin.isAdmin());

        ClaimRecord plot = byId(result.claims, 3);
        assertEquals(2, plot.parentId);
        assertTrue(plot.isSubdivision());
        assertEquals(Set.of(BUYER), plot.builders);
        assertEquals(Set.of(BUYER), plot.managers);

        PlayerRecord kailey = result.players.stream()
                .filter(p -> p.id.toString().equals(KAILEY)).findFirst().orElseThrow();
        assertEquals(3492, kailey.accrued);
        assertEquals(0, kailey.bonus);
        PlayerRecord marvin = result.players.stream()
                .filter(p -> p.id.toString().equals(MARVIN)).findFirst().orElseThrow();
        assertEquals(2468, marvin.accrued);
        assertEquals(250, marvin.bonus);
    }

    private void assertSameModel(List<ClaimRecord> a, List<ClaimRecord> b) {
        assertEquals(a.size(), b.size());
        for (ClaimRecord x : a) {
            ClaimRecord y = byId(b, x.id);
            assertEquals(x.world, y.world);
            assertEquals(x.lx, y.lx); assertEquals(x.ly, y.ly); assertEquals(x.lz, y.lz);
            assertEquals(x.gx, y.gx); assertEquals(x.gy, y.gy); assertEquals(x.gz, y.gz);
            assertEquals(x.owner, y.owner);
            assertEquals(x.parentId, y.parentId);
            assertEquals(x.builders, y.builders);
            assertEquals(x.containers, y.containers);
            assertEquals(x.accessors, y.accessors);
            assertEquals(x.managers, y.managers);
            assertEquals(x.inheritNothing, y.inheritNothing);
        }
    }

    @Test
    void roundTripThroughGpFormatIsLossless() throws Exception {
        GpImporter.ImportResult first = new GpImporter().importFrom(syntheticGpDir());
        File outDir = tmp.resolve("gp-restore").toFile();
        new GpExporter().exportTo(first.claims, first.players, first.nextClaimId, outDir);
        GpImporter.ImportResult second = new GpImporter().importFrom(outDir);
        assertSameModel(first.claims, second.claims);
        assertEquals(first.players.size(), second.players.size());
        for (PlayerRecord p : first.players) {
            PlayerRecord q = second.players.stream().filter(r -> r.id.equals(p.id)).findFirst().orElseThrow();
            assertEquals(p.accrued, q.accrued);
            assertEquals(p.bonus, q.bonus);
        }
        assertEquals(first.nextClaimId, second.nextClaimId);
    }

    @Test
    void exportedFilesCarryExactlyTheGpKeys() throws Exception {
        GpImporter.ImportResult first = new GpImporter().importFrom(syntheticGpDir());
        File outDir = tmp.resolve("gp-keys").toFile();
        new GpExporter().exportTo(first.claims, first.players, first.nextClaimId, outDir);
        YamlConfiguration y = YamlConfiguration.loadConfiguration(new File(outDir, "ClaimData/1.yml"));
        assertEquals(Set.of("Lesser Boundary Corner", "Greater Boundary Corner", "Owner",
                "Builders", "Containers", "Accessors", "Managers", "Parent Claim ID", "inheritNothing"),
                y.getKeys(false), "the rollback files must be pure GP format, no foreign keys");
        String text = Files.readString(new File(outDir, "PlayerData/" + MARVIN).toPath());
        assertEquals("\n2468\n250\n", text, "player files keep the old byte shape");
    }

    @Test
    void orphanedSubdivisionIsRerootedAndReported() throws Exception {
        File dir = syntheticGpDir();
        // Point the plot at a parent that does not exist.
        File plot = new File(dir, "ClaimData/3.yml");
        String content = Files.readString(plot.toPath()).replace("Parent Claim ID: 2", "Parent Claim ID: 99");
        Files.writeString(plot.toPath(), content);
        GpImporter.ImportResult result = new GpImporter().importFrom(dir);
        assertEquals(1, result.orphanedSubdivisions);
        assertEquals(-1, byId(result.claims, 3).parentId);
        assertFalse(result.notes.isEmpty(), "the re-root is reported, never silent");
    }

    @Test
    void inventoryTrustDegradesToContainerOnExport_documented() throws Exception {
        // ExcelClaims' view-only tier has no field in the GP files: on
        // the rollback path it returns as ordinary Container trust.
        // This test pins that documented degradation so it can never
        // happen silently.
        ClaimRecord r = new ClaimRecord();
        r.id = 7; r.world = "world";
        r.lx = 0; r.ly = 64; r.lz = 0; r.gx = 9; r.gy = 80; r.gz = 9;
        r.owner = UUID.fromString(BUYER);
        r.containers.add(BUYER);
        r.inventoryOnly.add(BUYER);
        File outDir = tmp.resolve("gp-degrade").toFile();
        new GpExporter().exportTo(List.of(r), List.of(), 8, outDir);
        GpImporter.ImportResult back = new GpImporter().importFrom(outDir);
        ClaimRecord restored = byId(back.claims, 7);
        assertEquals(Set.of(BUYER), restored.containers, "the trust itself survives the rollback");
        assertTrue(restored.inventoryOnly.isEmpty(), "view-only nuance is the one documented loss");
    }
}

// ExcelClaims tests — Iris / Mike
