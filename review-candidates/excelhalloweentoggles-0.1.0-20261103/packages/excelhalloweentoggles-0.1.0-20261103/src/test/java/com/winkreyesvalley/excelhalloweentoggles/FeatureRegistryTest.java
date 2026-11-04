package com.winkreyesvalley.excelhalloweentoggles;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * ExcelHalloweenToggles — registry completeness, checked two ways:
 *
 * 1. Against the design: exactly the seven gateable features exist
 *    (login pumpkin + six ambient scares) — no fake toggles for the
 *    features the design proved cannot be gated.
 * 2. Against the REAL Halloween 3.4.0 jar: every config path the
 *    registry points at (enabled / min / max / cooldown) exists in the
 *    jar's own bundled default config. The jar config is supplied by
 *    the build script via the halloween.config system property; the
 *    test FAILS (does not skip) if it is missing, so the check can
 *    never silently evaporate.
 */
public class FeatureRegistryTest {

    @Test
    public void shippedRegistryIsExactlyTheSevenGateableFeatures() throws Exception {
        FeatureRegistry r = FeatureRegistry.fromConfig(ToggleStoreTest.loadShippedConfig());
        assertEquals(7, r.all().size());
        assertEquals(6, r.ambient().size());
        assertNotNull(r.loginFeature());
        assertEquals("login_pumpkin", r.loginFeature().key());
        String[] expectedOrder = {"login_pumpkin", "pumpkin_curse", "slenderman",
                "darkness", "lightning", "fire", "herobrine"};
        for (int i = 0; i < expectedOrder.length; i++) {
            assertEquals(expectedOrder[i], r.all().get(i).key());
        }
    }

    @Test
    public void noFakeTogglesExist() throws Exception {
        FeatureRegistry r = FeatureRegistry.fromConfig(ToggleStoreTest.loadShippedConfig());
        for (String fake : new String[]{"ambient_sounds", "witching_hour", "pumpkin_head_mobs",
                "bat_witches", "pumpkin_buffs", "join_messages", "candy"}) {
            assertTrue("fake toggle must not exist: " + fake, !r.knows(fake));
        }
    }

    @Test
    public void eventNameAliasResolvesToTheToggle() throws Exception {
        FeatureRegistry r = FeatureRegistry.fromConfig(ToggleStoreTest.loadShippedConfig());
        assertEquals("pumpkin_curse", r.resolve("pumpkin").key());
        assertEquals("herobrine", r.resolve("HEROBRINE").key());
        assertEquals("login_pumpkin", r.resolve("login_pumpkin").key());
    }

    @Test
    public void everyRegistryPathExistsInTheRealHalloweenConfig() throws Exception {
        String path = System.getProperty("halloween.config");
        if (path == null) {
            fail("halloween.config system property not set — the build must supply the real jar's config");
        }
        File f = new File(path);
        assertTrue("real Halloween config not found at " + path, f.exists());
        YamlConfiguration hw = YamlConfiguration.loadConfiguration(f);

        FeatureRegistry r = FeatureRegistry.fromConfig(ToggleStoreTest.loadShippedConfig());
        for (FeatureDef d : r.all()) {
            assertTrue("enabled-path missing in Halloween config: " + d.enabledPath(),
                    hw.contains(d.enabledPath()));
            if (d.isLogin()) {
                assertTrue("cooldown-path missing in Halloween config: " + d.cooldownPath(),
                        hw.contains(d.cooldownPath()));
            } else {
                assertTrue("min-path missing in Halloween config: " + d.minPath(), hw.contains(d.minPath()));
                assertTrue("max-path missing in Halloween config: " + d.maxPath(), hw.contains(d.maxPath()));
                assertTrue("event not a known triggerable: " + d.halloweenEvent(),
                        FeatureRegistry.KNOWN_AMBIENT_EVENTS.contains(d.halloweenEvent()));
            }
        }
    }

    @Test
    public void registryRejectsAnUnknownHalloweenEvent() {
        YamlConfiguration c = new YamlConfiguration();
        c.set("features.bogus.display", "Bogus");
        c.set("features.bogus.kind", "ambient");
        c.set("features.bogus.event", "ambient_sounds");
        c.set("features.bogus.enabled-path", "events.bogus.enabled");
        c.set("features.bogus.min-path", "events.bogus.min");
        c.set("features.bogus.max-path", "events.bogus.max");
        try {
            FeatureRegistry.fromConfig(c);
            fail("an event the Halloween plugin cannot fire must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }

    @Test
    public void registryRejectsASecondLoginFeature() {
        YamlConfiguration c = new YamlConfiguration();
        for (String key : new String[]{"one", "two"}) {
            c.set("features." + key + ".display", key);
            c.set("features." + key + ".kind", "login");
            c.set("features." + key + ".enabled-path", "login_pumpkin.enabled");
            c.set("features." + key + ".cooldown-path", "login_pumpkin.cooldown_minutes");
        }
        try {
            FeatureRegistry.fromConfig(c);
            fail("two login features must be rejected");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
