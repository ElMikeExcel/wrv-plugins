package com.winkreyes.playermap;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LegacyStorageTest {
    @TempDir Path temp;
    private static final String CONFIG = "# Keep this comment and spacing.\r\nupdate-interval-ticks: 40\r\n";
    private static final String MAPS = """
            # Existing native identities must not be rewritten.
            format: 2
            maps:
            - id: 10
              world: 00000000-0000-0000-0000-000000000001
              scale: 0
              center-x: -128
              center-z: 256
              root: 10
            - id: 11
              world: 00000000-0000-0000-0000-000000000001
              scale: 1
              center-x: -192
              center-z: 320
              root: 10
            """;

    private Path legacy() { return temp.resolve("WRVPlayerMap"); }
    private Path current() { return temp.resolve("ExcelPlayerMap"); }
    private void writeLegacy() throws IOException {
        Files.createDirectory(legacy());
        Files.writeString(legacy().resolve("config.yml"), CONFIG);
        Files.writeString(legacy().resolve("maps.yml"), MAPS);
    }
    private void assertNoStaging() throws IOException {
        try (var children = Files.list(temp)) {
            assertFalse(children.anyMatch(path -> path.getFileName().toString().startsWith(".excelplayermap-migration-")));
        }
    }

    @Test void copiesExactBytesAndNativeIdentitiesWithoutChangingOrDeletingLegacy() throws Exception {
        writeLegacy();
        Files.writeString(legacy().resolve("operator-notes.txt"), "Keep my backup");
        assertEquals(LegacyStorage.Result.MIGRATED, LegacyStorage.prepare(current()));
        for (String filename : List.of("config.yml", "maps.yml")) {
            byte[] expected = (filename.equals("config.yml") ? CONFIG : MAPS).getBytes(java.nio.charset.StandardCharsets.UTF_8);
            assertArrayEquals(expected, Files.readAllBytes(legacy().resolve(filename)));
            assertArrayEquals(expected, Files.readAllBytes(current().resolve(filename)));
        }
        assertEquals("Keep my backup", Files.readString(legacy().resolve("operator-notes.txt")));
        assertFalse(Files.exists(current().resolve("operator-notes.txt")));
        assertTrue(Files.isRegularFile(current().resolve(LegacyStorage.MARKER)));
        MapRegistry registry = new MapRegistry(current().resolve("maps.yml")); registry.load();
        assertEquals(2, registry.entries().size());
        assertEquals(10, registry.entry(11).root());
        assertNoStaging();
    }

    @Test void acceptsAnExistingEmptyDestination() throws Exception {
        writeLegacy(); Files.createDirectory(current());
        assertEquals(LegacyStorage.Result.MIGRATED, LegacyStorage.prepare(current()));
        assertEquals(MAPS, Files.readString(current().resolve("maps.yml")));
        assertNoStaging();
    }

    @Test void ambiguousDestinationFailsClosedWithoutOverwritingEvenUnrelatedFiles() throws Exception {
        writeLegacy(); Files.createDirectory(current());
        Files.writeString(current().resolve("owner-notes.txt"), "Do not replace");
        assertThrows(IOException.class, () -> LegacyStorage.prepare(current()));
        assertEquals("Do not replace", Files.readString(current().resolve("owner-notes.txt")));
        assertFalse(Files.exists(current().resolve("maps.yml")));
        assertFalse(Files.exists(current().resolve(LegacyStorage.MARKER)));
        assertEquals(MAPS, Files.readString(legacy().resolve("maps.yml")));
        assertNoStaging();
    }

    @Test void malformedLegacyRegistriesNeverCreateADestinationOrLoseOriginals() throws Exception {
        writeLegacy();
        for (String invalid : List.of("format: 1\nmaps: []\n", "format: 2.5\nmaps: []\n", "format: 2\nmaps: invalid\n",
                "format: 2\nmaps: [", MAPS.replace("root: 10", "root: 999"))) {
            Files.writeString(legacy().resolve("maps.yml"), invalid);
            assertThrows(InvalidConfigurationException.class, () -> LegacyStorage.prepare(current()));
            assertFalse(Files.exists(current()));
            assertEquals(invalid, Files.readString(legacy().resolve("maps.yml")));
            assertEquals(CONFIG, Files.readString(legacy().resolve("config.yml")));
            assertNoStaging();
        }
    }

    @Test void invalidConfigLeavesEvenAnEmptyDestinationUntouched() throws Exception {
        writeLegacy(); Files.createDirectory(current());
        for (String invalid : List.of("update-interval-ticks: [", "update-interval-ticks: later\n",
                "update-interval-ticks: 1.5\n", "- not-a-config-mapping\n")) {
            Files.writeString(legacy().resolve("config.yml"), invalid);
            assertThrows(InvalidConfigurationException.class, () -> LegacyStorage.prepare(current()));
            try (var children = Files.list(current())) { assertEquals(0, children.count()); }
            assertEquals(invalid, Files.readString(legacy().resolve("config.yml")));
            assertEquals(MAPS, Files.readString(legacy().resolve("maps.yml")));
            assertNoStaging();
        }
    }

    @Test void repeatedStartsKeepUpdatedDestinationAndNeverRecopyStaleLegacy() throws Exception {
        writeLegacy(); LegacyStorage.prepare(current());
        String updatedMaps = MAPS.replace("id: 11", "id: 12");
        Files.writeString(current().resolve("maps.yml"), updatedMaps);
        Files.writeString(current().resolve("config.yml"), "update-interval-ticks: 60\n");
        assertEquals(LegacyStorage.Result.CURRENT, LegacyStorage.prepare(current()));
        assertEquals(updatedMaps, Files.readString(current().resolve("maps.yml")));
        assertEquals("update-interval-ticks: 60\n", Files.readString(current().resolve("config.yml")));
        assertEquals(MAPS, Files.readString(legacy().resolve("maps.yml")));
        // An intentionally absent current registry must never revive the legacy claims.
        Files.delete(current().resolve("maps.yml"));
        Files.writeString(legacy().resolve("maps.yml"), "invalid stale backup");
        assertEquals(LegacyStorage.Result.CURRENT, LegacyStorage.prepare(current()));
        assertFalse(Files.exists(current().resolve("maps.yml")));
        assertNoStaging();
    }

    @Test void freshInstallMarkerAlsoPreventsLaterAccidentalLegacyImport() throws Exception {
        assertEquals(LegacyStorage.Result.FRESH, LegacyStorage.prepare(current()));
        assertTrue(Files.exists(current().resolve(LegacyStorage.MARKER)));
        assertFalse(Files.exists(current().resolve("maps.yml")));
        assertFalse(Files.exists(current().resolve("config.yml"))); // JavaPlugin supplies the normal default.
        writeLegacy();
        assertEquals(LegacyStorage.Result.CURRENT, LegacyStorage.prepare(current()));
        assertFalse(Files.exists(current().resolve("maps.yml")));
    }

    @Test void legacyInstallWithoutEnrolledMapsCanMigrateConfig() throws Exception {
        Files.createDirectory(legacy()); Files.writeString(legacy().resolve("config.yml"), CONFIG);
        assertEquals(LegacyStorage.Result.MIGRATED, LegacyStorage.prepare(current()));
        assertEquals(CONFIG, Files.readString(current().resolve("config.yml")));
        assertFalse(Files.exists(current().resolve("maps.yml")));
    }

    @Test void existingCurrentStorageWithoutLegacyIsPreservedAndMarked() throws Exception {
        Files.createDirectory(current()); Files.writeString(current().resolve("config.yml"), CONFIG);
        Files.writeString(current().resolve("maps.yml"), MAPS);
        Files.writeString(current().resolve("notes.txt"), "Mine");
        assertEquals(LegacyStorage.Result.CURRENT, LegacyStorage.prepare(current()));
        assertEquals(CONFIG, Files.readString(current().resolve("config.yml")));
        assertEquals(MAPS, Files.readString(current().resolve("maps.yml")));
        assertEquals("Mine", Files.readString(current().resolve("notes.txt")));
        assertTrue(Files.exists(current().resolve(LegacyStorage.MARKER)));
    }

    @Test void unknownMarkerDoesNotAuthorizeAnImportOrOverwrite() throws Exception {
        writeLegacy(); Files.createDirectory(current());
        Files.writeString(current().resolve(LegacyStorage.MARKER), "Unknown version");
        assertThrows(IOException.class, () -> LegacyStorage.prepare(current()));
        assertEquals("Unknown version", Files.readString(current().resolve(LegacyStorage.MARKER)));
        assertFalse(Files.exists(current().resolve("maps.yml")));
    }

    @Test void symlinkedLegacyFilesAndDirectoriesAreRejectedWithoutFollowingThem() throws Exception {
        writeLegacy(); Files.delete(legacy().resolve("maps.yml"));
        Path outside = temp.resolve("external-maps.yml"); Files.writeString(outside, MAPS);
        Files.createSymbolicLink(legacy().resolve("maps.yml"), outside);
        assertThrows(IOException.class, () -> LegacyStorage.prepare(current()));
        assertEquals(MAPS, Files.readString(outside)); assertFalse(Files.exists(current()));
        Files.delete(legacy().resolve("maps.yml")); Files.delete(legacy().resolve("config.yml")); Files.delete(legacy());
        Path outsideDirectory = Files.createDirectory(temp.resolve("external-data"));
        Files.createSymbolicLink(legacy(), outsideDirectory);
        assertThrows(IOException.class, () -> LegacyStorage.prepare(current()));
        assertFalse(Files.exists(current()));
    }

    private PlayerMapPlugin plugin(PluginManager manager) {
        PlayerMapPlugin plugin = mock(PlayerMapPlugin.class, CALLS_REAL_METHODS);
        Server server = mock(Server.class);
        doReturn(server).when(plugin).getServer();
        when(server.getPluginManager()).thenReturn(manager);
        when(server.getScheduler()).thenReturn(mock(BukkitScheduler.class));
        doReturn(Logger.getAnonymousLogger()).when(plugin).getLogger();
        doReturn(current().toFile()).when(plugin).getDataFolder();
        doReturn(new YamlConfiguration()).when(plugin).getConfig();
        doReturn(mock(PluginCommand.class)).when(plugin).getCommand("playermap");
        doNothing().when(plugin).saveDefaultConfig();
        return plugin;
    }

    @Test void simultaneousLegacyPluginDisablesExcelBeforeAnyStorageOrDefaultWrites() {
        PluginManager manager = mock(PluginManager.class);
        when(manager.getPlugin("WRVPlayerMap")).thenReturn(mock(Plugin.class));
        PlayerMapPlugin plugin = plugin(manager);
        plugin.onEnable();
        verify(manager).disablePlugin(plugin);
        verify(plugin, never()).saveDefaultConfig();
        verify(plugin, never()).getDataFolder();
        verify(manager, never()).registerEvents(any(), any());
        assertFalse(Files.exists(current()));
    }

    @Test void storageFailureDisablesExcelBeforeDefaultConfigOrListenerRegistration() throws Exception {
        writeLegacy(); Files.writeString(legacy().resolve("maps.yml"), "format: 1\nmaps: []\n");
        PluginManager manager = mock(PluginManager.class); PlayerMapPlugin plugin = plugin(manager);
        plugin.onEnable();
        verify(manager).disablePlugin(plugin);
        verify(plugin, never()).saveDefaultConfig();
        verify(manager, never()).registerEvents(any(), any());
        assertFalse(Files.exists(current()));
    }

    @Test void selfAliasCannotHideAnActualLegacyPluginFromTheDuplicateGuard() {
        PluginManager manager = mock(PluginManager.class); PlayerMapPlugin plugin = plugin(manager);
        Plugin legacy = mock(Plugin.class); when(legacy.getName()).thenReturn("WRVPlayerMap");
        when(manager.getPlugin("WRVPlayerMap")).thenReturn(plugin);
        when(manager.getPlugins()).thenReturn(new Plugin[] {plugin, legacy});
        plugin.onEnable();
        verify(manager).disablePlugin(plugin);
        verify(plugin, never()).saveDefaultConfig();
        verify(plugin, never()).getDataFolder();
        verify(manager, never()).registerEvents(any(), any());
        assertFalse(Files.exists(current()));
    }

    @Test void enableMigratesBeforeDefaultsAndKeepsTheDeployedLineageNamespace() throws Exception {
        writeLegacy(); PluginManager manager = mock(PluginManager.class); PlayerMapPlugin plugin = plugin(manager);
        when(manager.getPlugin("WRVPlayerMap")).thenReturn(plugin); // Descriptor provides the old name as an alias.
        doAnswer(invocation -> {
            assertEquals(CONFIG, Files.readString(current().resolve("config.yml")));
            assertEquals(MAPS, Files.readString(current().resolve("maps.yml")));
            return null;
        }).when(plugin).saveDefaultConfig();
        plugin.onEnable();
        verify(plugin).saveDefaultConfig(); verify(manager, never()).disablePlugin(plugin);
        Field key = PlayerMapPlugin.class.getDeclaredField("rootKey"); key.setAccessible(true);
        assertEquals(new NamespacedKey("wrvplayermap", "lineage_root"), key.get(plugin));
        verify(plugin, atLeastOnce()).getCommand("playermap");
        verify(manager).registerEvents(plugin, plugin);
    }
}
