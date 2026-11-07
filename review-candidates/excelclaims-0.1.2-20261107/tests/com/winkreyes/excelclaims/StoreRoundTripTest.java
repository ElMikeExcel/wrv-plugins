package com.winkreyes.excelclaims;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

/** Our own store: save -> load -> every field survives, extras included. */
public class StoreRoundTripTest {

    @TempDir Path dir;

    @Test
    void claimsAndPlayersSurviveReload() throws Exception {
        ClaimsConfig cfg = new ClaimsConfig();
        cfg.worldModes.put("world", ClaimsConfig.WorldMode.SURVIVAL);
        ClaimStore store = new ClaimStore(Logger.getLogger("t"), cfg);
        store.load(dir.toFile());

        ClaimRecord r = new ClaimRecord();
        r.id = store.allocateId();
        r.world = "world";
        r.lx = -10; r.ly = 60; r.lz = -10; r.gx = 9; r.gy = 90; r.gz = 9;
        r.owner = UUID.randomUUID();
        r.builders.add(UUID.randomUUID().toString());
        r.containers.add("public");
        r.accessors.add("LegacyName");
        r.managers.add(UUID.randomUUID().toString());
        r.inventoryOnly.add("public");
        r.inheritNothing = true;
        r.subclaimRestricted = true;
        r.chestClaim = true;
        r.createdAt = 123456789L;
        r.lastModifiedAt = 123999999L;
        store.put(r);
        store.saveClaim(r);

        PlayerRecord pr = store.playerData(r.owner);
        pr.accrued = 4321;
        pr.bonus = 77;
        pr.accruedLimitOverride = 9000;
        pr.lastSeenAt = 555000111L;
        store.savePlayer(pr);

        ClaimStore reloaded = new ClaimStore(Logger.getLogger("t"), cfg);
        reloaded.load(dir.toFile());
        ClaimRecord back = reloaded.byId(r.id);
        assertNotNull(back);
        assertEquals(r.world, back.world);
        assertEquals(r.lx, back.lx); assertEquals(r.gy, back.gy);
        assertEquals(r.owner, back.owner);
        assertEquals(r.builders, back.builders);
        assertEquals(r.containers, back.containers);
        assertEquals(r.accessors, back.accessors);
        assertEquals(r.managers, back.managers);
        assertEquals(Set.of("public"), back.inventoryOnly);
        assertTrue(back.inheritNothing);
        assertTrue(back.subclaimRestricted);
        assertTrue(back.chestClaim);
        assertEquals(123456789L, back.createdAt);
        assertEquals(123999999L, back.lastModifiedAt);

        PlayerRecord prBack = reloaded.playerDataIfPresent(r.owner);
        assertNotNull(prBack);
        assertEquals(4321, prBack.accrued);
        assertEquals(77, prBack.bonus);
        assertEquals(9000, prBack.accruedLimitOverride);
        assertEquals(555000111L, prBack.lastSeenAt);

        // The id counter continues where it left off.
        assertEquals(r.id + 1, reloaded.allocateId());
    }

    @Test
    void playerFileKeepsGpByteShape() throws Exception {
        // Our PlayerData files must stay readable by the old store on a
        // rollback: blank line, accrued, bonus.
        ClaimsConfig cfg = new ClaimsConfig();
        ClaimStore store = new ClaimStore(Logger.getLogger("t"), cfg);
        store.load(dir.toFile());
        UUID id = UUID.randomUUID();
        PlayerRecord pr = store.playerData(id);
        pr.accrued = 1748;
        pr.bonus = 0;
        store.savePlayer(pr);
        String text = java.nio.file.Files.readString(dir.resolve("PlayerData").resolve(id.toString()));
        assertEquals("\n1748\n0\n", text);
    }
}

// ExcelClaims tests — Iris / Mike
