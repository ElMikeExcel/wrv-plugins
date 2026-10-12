package com.winkreyes.nophantoms;

import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;

class NoPhantomsTest {
    @Test
    void phantomSpawnsAreCancelled() {
        NoPhantoms plugin = mock(NoPhantoms.class, CALLS_REAL_METHODS);
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getEntityType()).thenReturn(EntityType.PHANTOM);
        plugin.onCreatureSpawn(event);
        verify(event).setCancelled(true);
    }

    @Test
    void otherCreaturesAreUnaffected() {
        NoPhantoms plugin = mock(NoPhantoms.class, CALLS_REAL_METHODS);
        CreatureSpawnEvent event = mock(CreatureSpawnEvent.class);
        when(event.getEntityType()).thenReturn(EntityType.COW);
        plugin.onCreatureSpawn(event);
        verify(event, never()).setCancelled(anyBoolean());
    }
}
