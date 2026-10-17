package com.winkreyes.excelweekendxp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VanillaXpBridgeTest {
    public static class CompatiblePlugin {
        boolean supported = true;
        double multiplier = 1.0;
        int writes;
        boolean rejectWrite;
        boolean throwOnRead;
        boolean throwOnSupport;
        Double overrideReadback;

        public boolean isVanillaXpBonusSupported() {
            if (throwOnSupport) throw new IllegalStateException("support check failed");
            return supported;
        }

        public void setVanillaXpEventMultiplier(double value) {
            if (rejectWrite) throw new IllegalStateException("setter failed");
            multiplier = value;
            writes++;
        }

        public double getVanillaXpEventMultiplier() {
            if (throwOnRead) throw new IllegalStateException("readback failed");
            return overrideReadback == null ? multiplier : overrideReadback;
        }
    }

    public static class WrongSignaturePlugin {
        public boolean isVanillaXpBonusSupported() { return true; }
        public boolean setVanillaXpEventMultiplier(double value) { return true; }
        public double getVanillaXpEventMultiplier() { return 1.2; }
    }

    public static class StaticApiPlugin {
        public static boolean isVanillaXpBonusSupported() { return true; }
        public void setVanillaXpEventMultiplier(double value) { }
        public double getVanillaXpEventMultiplier() { return 1.2; }
    }

    @Test
    void compatiblePluginSupportsWeekendAndWeekdayReadback() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        assertTrue(bridge.isSupported());
        assertEquals(0, plugin.writes, "Startup probe must not change a rate");
        assertTrue(bridge.applyMultiplier(1.2));
        assertEquals(1.2, plugin.multiplier);
        assertTrue(bridge.applyMultiplier(1.0));
        assertEquals(1.0, plugin.multiplier);
        assertEquals(2, plugin.writes);
        assertTrue(warnings.isEmpty());
    }

    @Test
    void legacyPluginWarnsAndNeverReportsSuccess() {
        List<String> warnings = new ArrayList<>();
        Object legacy = new Object();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> legacy, warnings::add);
        assertFalse(bridge.isSupported());
        assertFalse(bridge.applyMultiplier(1.2));
        assertEquals(1, warnings.size(), "Repeated identical incompatibility messages are deduplicated");
        assertTrue(warnings.getFirst().contains("custom mcMMO vanilla XP bridge"));
        assertTrue(warnings.getFirst().contains("greenbar XP schedule is not confirmed"));
        assertTrue(warnings.getFirst().contains("NoSuchMethodException"));
    }

    @Test
    void unsupportedCapabilitySkipsSetterAndCanRecover() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        plugin.supported = false;
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        assertFalse(bridge.applyMultiplier(1.2));
        assertEquals(0, plugin.writes);
        assertTrue(warnings.getFirst().contains("unsupported"));
        plugin.supported = true;
        assertTrue(bridge.applyMultiplier(1.2));
        assertEquals(1, plugin.writes);
    }

    @Test
    void wrongSignatureAndStaticMethodsAreRejected() {
        for (Object plugin : List.of(new WrongSignaturePlugin(), new StaticApiPlugin())) {
            List<String> warnings = new ArrayList<>();
            VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
            assertFalse(bridge.applyMultiplier(1.2));
            assertTrue(warnings.getFirst().contains("incompatible signature"));
        }
    }

    @Test
    void failedSupportCheckIsRetryable() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        plugin.throwOnSupport = true;
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        assertFalse(bridge.applyMultiplier(1.2));
        assertEquals(0, plugin.writes);
        assertTrue(warnings.getFirst().contains("support check failed"));
        plugin.throwOnSupport = false;
        assertTrue(bridge.applyMultiplier(1.2));
    }

    @Test
    void failedSetterIsRetryableAndReportsItsCause() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        plugin.rejectWrite = true;
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        assertFalse(bridge.applyMultiplier(1.2));
        assertTrue(warnings.getFirst().contains("IllegalStateException: setter failed"));
        plugin.rejectWrite = false;
        assertTrue(bridge.applyMultiplier(1.2));
    }

    @Test
    void failedReadbackIsNotSuccessEvenWhenSetterApplied() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        plugin.throwOnRead = true;
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        assertFalse(bridge.applyMultiplier(1.2));
        assertEquals(1.2, plugin.multiplier);
        assertTrue(warnings.getFirst().contains("readback failed"));
        plugin.throwOnRead = false;
        assertTrue(bridge.applyMultiplier(1.2));
    }

    @Test
    void incorrectAndNonFiniteReadbackNeverReportSuccess() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, warnings::add);
        for (double incorrect : new double[] { 1.0, 1.25, Double.NaN, Double.POSITIVE_INFINITY }) {
            plugin.overrideReadback = incorrect;
            assertFalse(bridge.applyMultiplier(1.2));
        }
        assertEquals(4, warnings.size());
        assertTrue(warnings.stream().allMatch(message -> message.contains("expected 1.2")));
        plugin.overrideReadback = null;
        assertTrue(bridge.applyMultiplier(1.2));
    }

    @Test
    void missingOrReplacedPluginCanRecoverAndDoesNotUseAStaleInstance() {
        AtomicReference<Object> current = new AtomicReference<>();
        List<String> warnings = new ArrayList<>();
        VanillaXpBridge bridge = new VanillaXpBridge(current::get, warnings::add);
        assertFalse(bridge.isSupported());
        current.set(new Object());
        assertFalse(bridge.applyMultiplier(1.2));
        CompatiblePlugin first = new CompatiblePlugin();
        current.set(first);
        assertTrue(bridge.applyMultiplier(1.2));
        current.set(null);
        assertFalse(bridge.applyMultiplier(1.0));
        CompatiblePlugin replacement = new CompatiblePlugin();
        current.set(replacement);
        assertTrue(bridge.applyMultiplier(1.0));
        assertEquals(1.2, first.multiplier);
        assertEquals(1.0, replacement.multiplier);
        assertEquals(1, replacement.writes);
    }

    @Test
    void invalidInputsAreRejectedBeforeThePluginIsCalled() {
        CompatiblePlugin plugin = new CompatiblePlugin();
        VanillaXpBridge bridge = new VanillaXpBridge(() -> plugin, message -> fail(message));
        for (double invalid : new double[] { 0.0, 0.99, -1.0, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY }) {
            assertThrows(IllegalArgumentException.class, () -> bridge.applyMultiplier(invalid));
        }
        assertEquals(0, plugin.writes);
    }
}
