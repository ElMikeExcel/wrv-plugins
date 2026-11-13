// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The four confirmation targets, pinned to the union of the retired
 * Java definitions and the Bedrock modal copy, with the Yes action
 * each column ran.
 */
public class ConfirmTargetsTest {

    @Test
    public void allFourTargetsExist() {
        assertEquals(4, ConfirmTargets.all().size());
        for (String id : new String[]{"teleport_confirm_spawn",
                "teleport_confirm_spawncmd", "teleport_confirm_guild",
                "teleport_confirm_home"}) {
            assertTrue(id, ConfirmTargets.isConfirmation(id));
        }
        assertNull(ConfirmTargets.byDefinitionId("server_menu"));
        assertNull(ConfirmTargets.byDefinitionId(null));
    }

    @Test
    public void spawnUsesTheFallbackPath() {
        ConfirmTargets.Target t = ConfirmTargets.byDefinitionId("teleport_confirm_spawn");
        assertEquals(ConfirmTargets.Kind.SPAWN_FALLBACK, t.kind());
        assertEquals("Teleport to spawn?", t.modalTitle());
        assertEquals("This takes you back to the village spawn right away.",
                t.modalContent());
        assertEquals("&6&lTeleport to Spawn?", t.chestTitle());
        assertEquals("COMPASS", t.icon());
    }

    @Test
    public void playerCommandTargets() {
        ConfirmTargets.Target spawncmd =
                ConfirmTargets.byDefinitionId("teleport_confirm_spawncmd");
        assertEquals(ConfirmTargets.Kind.PLAYER_COMMAND, spawncmd.kind());
        assertEquals("spawn", spawncmd.command());
        assertEquals("LODESTONE", spawncmd.icon());

        ConfirmTargets.Target guild =
                ConfirmTargets.byDefinitionId("teleport_confirm_guild");
        assertEquals("ag", guild.command());
        assertEquals("Teleport to the Adventurers Guild?", guild.modalTitle());
        assertEquals("&6&lTeleport to the Guild?", guild.chestTitle());
        assertEquals("BELL", guild.icon());

        ConfirmTargets.Target home =
                ConfirmTargets.byDefinitionId("teleport_confirm_home");
        assertEquals("home", home.command());
        assertEquals("Teleport home?", home.modalTitle());
        assertEquals("RED_BED", home.icon());
    }

    @Test
    public void settledButtonLabels() {
        assertEquals("Yes, teleport", ConfirmTargets.YES_LABEL);
        assertEquals("Cancel", ConfirmTargets.NO_LABEL);
    }

    @Test
    public void spawnFallbackRules() {
        assertTrue(SpawnFallback.needsSpawnFallback(false, "world"));
        assertTrue(SpawnFallback.needsSpawnFallback(true, "em_adventurers_guild"));
        assertEquals(false, SpawnFallback.needsSpawnFallback(true, "world"));
        assertTrue(SpawnFallback.isSpawnWorld(true, "world"));
        assertEquals(false, SpawnFallback.isSpawnWorld(true, "em_adventurers_guild"));
        assertEquals(false, SpawnFallback.isSpawnWorld(false, "world"));
        assertEquals(-784.0, SpawnFallback.DEFAULT_X, 0.0);
        assertEquals(-272.0, SpawnFallback.DEFAULT_Z, 0.0);
    }
}
