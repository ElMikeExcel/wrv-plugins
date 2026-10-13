package com.winkreyes.bedrockmenu;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MenuEntriesTest {
    private static String[] entries(String name) throws ReflectiveOperationException {
        Field field = BedrockMenu.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String[]) field.get(null);
    }

    @Test
    void buttonsRemainPairedAfterRemovingRedundantJobsEntry() throws Exception {
        String[] buttons = entries("BUTTONS");
        String[] commands = entries("COMMANDS");
        assertArrayEquals(new String[] {
            "Spawn", "Teleport to a player", "Spawn Shop", "mcMMO Stats",
            "Leaderboard", "Server Info", "Auto-open on join"
        }, buttons);
        assertArrayEquals(new String[] {
            "spawn", "tpmenu", "shop", "mcstats", "leaderboard", "motd", "menujoin"
        }, commands);
        assertEquals(buttons.length, commands.length);
    }
}
