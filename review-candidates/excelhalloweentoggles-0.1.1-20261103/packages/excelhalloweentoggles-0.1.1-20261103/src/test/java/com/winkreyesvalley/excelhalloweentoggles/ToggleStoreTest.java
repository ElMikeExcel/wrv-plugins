package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ExcelHalloweenToggles — toggle-store behavior.
 *
 * Covers the persistence guarantees the design leans on: defaults are
 * ALL ON, a flip survives a save/reload cycle (the relog/restart case),
 * players are independent, and unknown feature keys are rejected rather
 * than silently stored.
 */
public class ToggleStoreTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private FeatureRegistry registry;

    @Before
    public void setUp() throws Exception {
        registry = FeatureRegistry.fromConfig(loadShippedConfig());
    }

    static YamlConfiguration loadShippedConfig() throws Exception {
        InputStream in = ToggleStoreTest.class.getResourceAsStream("/config.yml");
        if (in == null) {
            fail("shipped /config.yml not on the test classpath");
        }
        try (InputStream stream = in) {
            return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    private ToggleStore newStore(File file) {
        return new ToggleStore(file, registry, Logger.getLogger("test"));
    }

    @Test
    public void defaultsAreAllOnForAFreshPlayer() {
        ToggleStore store = newStore(new File(tmp.getRoot(), "players.yml"));
        UUID p = UUID.randomUUID();
        for (FeatureDef f : registry.all()) {
            assertTrue("default must be ON: " + f.key(), store.isEnabled(p, f.key()));
        }
    }

    @Test
    public void stateSurvivesSaveAndReload() {
        File file = new File(tmp.getRoot(), "players.yml");
        UUID p = UUID.randomUUID();
        ToggleStore first = newStore(file);
        first.setEnabled(p, "pumpkin_curse", false);
        first.setEnabled(p, "login_pumpkin", false);

        ToggleStore second = newStore(file); // constructor reloads from disk
        assertFalse(second.isEnabled(p, "pumpkin_curse"));
        assertFalse(second.isEnabled(p, "login_pumpkin"));
        assertTrue(second.isEnabled(p, "herobrine"));
    }

    @Test
    public void toggleFlipsAndReturnsTheNewState() {
        ToggleStore store = newStore(new File(tmp.getRoot(), "players.yml"));
        UUID p = UUID.randomUUID();
        assertFalse(store.toggle(p, "darkness"));
        assertFalse(store.isEnabled(p, "darkness"));
        assertTrue(store.toggle(p, "darkness"));
        assertTrue(store.isEnabled(p, "darkness"));
    }

    @Test
    public void playersAreIndependent() {
        ToggleStore store = newStore(new File(tmp.getRoot(), "players.yml"));
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        store.setEnabled(a, "fire", false);
        assertFalse(store.isEnabled(a, "fire"));
        assertTrue(store.isEnabled(b, "fire"));
    }

    @Test
    public void unknownFeatureKeyIsRejected() {
        ToggleStore store = newStore(new File(tmp.getRoot(), "players.yml"));
        UUID p = UUID.randomUUID();
        try {
            store.isEnabled(p, "ambient_sounds");
            fail("a feature the design proved cannot be gated must not be storable");
        } catch (IllegalArgumentException expected) {
            // The fake-toggle guard: only the seven real gates exist.
        }
        try {
            store.setEnabled(p, "witching_hour", false);
            fail("unknown key must be rejected on write too");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void corruptFileFallsBackToDefaults() throws Exception {
        File file = new File(tmp.getRoot(), "players.yml");
        java.nio.file.Files.writeString(file.toPath(), "players: [this is not a map");
        ToggleStore store = newStore(file);
        assertTrue(store.isEnabled(UUID.randomUUID(), "herobrine"));
    }
}
