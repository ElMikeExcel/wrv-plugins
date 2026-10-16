package com.winkreyes.excelnophantoms;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class ExcelNoPhantomsTest {
    @Test
    void phantomSpawnsAreCancelled() {
        ExcelNoPhantoms plugin = mock(ExcelNoPhantoms.class, CALLS_REAL_METHODS);
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getEntityType()).thenReturn(EntityType.PHANTOM);
        plugin.onCreatureSpawn(event);
        verify(event).setCancelled(true);
    }

    @Test
    void otherCreaturesAreUnaffected() {
        ExcelNoPhantoms plugin = mock(ExcelNoPhantoms.class, CALLS_REAL_METHODS);
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getEntityType()).thenReturn(EntityType.COW);
        plugin.onCreatureSpawn(event);
        verify(event, never()).setCancelled(anyBoolean());
    }
}
