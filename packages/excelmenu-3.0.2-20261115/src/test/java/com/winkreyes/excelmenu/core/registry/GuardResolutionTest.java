package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;
import org.junit.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * The Amendment A guard contract, case by case: missing plugin,
 * missing permission, unknown state, unresolvable state — each one
 * resolves to the default variant with the standard message, and ONLY
 * an all-green guard reaches the authored variant.
 */
public class GuardResolutionTest {

    private static final Clock NOW = Clock.fixed(
            Instant.parse("2026-11-01T18:00:00Z"), ZoneOffset.UTC);
    private static final RegistryConfig CONFIG = new RegistryConfig(
            RegistryConfig.SERVER_ZONE, "&eNope, unavailable.",
            Map.of("server_menu", List.of("travel", "settings")), Map.of());

    private static EntryDef toggle() {
        return new EntryDef("demo_toggle", "server_menu", "settings", 10, null,
                "LIME_DYE", "Demo: ON", List.of("base lore"),
                List.of("[player] demo off"),
                Set.of(Platform.JAVA, Platform.BEDROCK),
                "ExcelDemo", "demo.toggle", "%demo_status%",
                Map.of(
                        "ON", new EntryDef.Variant("LIME_DYE", "Demo: ON", null,
                                List.of("[player] demo off")),
                        "OFF", new EntryDef.Variant("GRAY_DYE", "Demo: OFF", null,
                                List.of("[player] demo on")),
                        "default", new EntryDef.Variant("GRAY_DYE", "Demo: unavailable",
                                List.of("try later"), null)),
                null, null, EntryDef.Origin.FILE);
    }

    private static FakeViewer happyViewer() {
        return new FakeViewer().withPlugin("ExcelDemo").grant("demo.toggle")
                .placeholder("%demo_status%", "ON");
    }

    private static ResolvedEntry resolve(EntryDef e, ViewerContext v) {
        List<ResolvedEntry> out = EntryResolver.resolve(
                "server_menu", List.of(e), v, NOW, CONFIG);
        assertEquals(1, out.size());
        return out.get(0);
    }

    @Test
    public void allGreenResolvesTheAuthoredVariant() {
        ResolvedEntry r = resolve(toggle(), happyViewer());
        assertFalse(r.guarded());
        assertNull(r.guardFailure());
        assertEquals("ON", r.stateValue());
        assertEquals("Demo: ON", r.label());
        assertEquals(List.of("[player] demo off"), r.action());
        assertEquals(List.of("base lore"), r.lore()); // variant lore null -> base inherits
    }

    @Test
    public void offStateResolvesTheOffVariant() {
        ResolvedEntry r = resolve(toggle(),
                happyViewer().placeholder("%demo_status%", "OFF"));
        assertFalse(r.guarded());
        assertEquals("Demo: OFF", r.label());
        assertEquals(List.of("[player] demo on"), r.action());
    }

    @Test
    public void missingPluginFallsToDefaultWithStandardMessage() {
        FakeViewer v = new FakeViewer().grant("demo.toggle")
                .placeholder("%demo_status%", "ON"); // no plugin
        ResolvedEntry r = resolve(toggle(), v);
        assertTrue(r.guarded());
        assertEquals("plugin ExcelDemo absent", r.guardFailure());
        assertEquals("Demo: unavailable", r.label());      // default variant presentation
        assertEquals(List.of("try later"), r.lore());
        assertEquals(List.of("[message] &eNope, unavailable."), r.action()); // standard message
    }

    @Test
    public void missingPermissionFallsToDefaultWithStandardMessage() {
        FakeViewer v = new FakeViewer().withPlugin("ExcelDemo")
                .placeholder("%demo_status%", "ON"); // no permission
        ResolvedEntry r = resolve(toggle(), v);
        assertTrue(r.guarded());
        assertEquals("permission demo.toggle missing", r.guardFailure());
        assertEquals("Demo: unavailable", r.label());
        assertEquals(List.of("[message] &eNope, unavailable."), r.action());
    }

    @Test
    public void unknownStateFallsToDefault() {
        ResolvedEntry r = resolve(toggle(),
                happyViewer().placeholder("%demo_status%", "SIDEWAYS"));
        assertTrue(r.guarded());
        assertTrue(r.guardFailure(), r.guardFailure().contains("state 'SIDEWAYS' unknown"));
        assertEquals("Demo: unavailable", r.label());
    }

    @Test
    public void unresolvableStateFallsToDefault() {
        FakeViewer v = new FakeViewer().withPlugin("ExcelDemo").grant("demo.toggle");
        ResolvedEntry r = resolve(toggle(), v); // placeholder never set -> null
        assertTrue(r.guarded());
        assertTrue(r.guardFailure(), r.guardFailure().contains("state unresolvable"));
        assertEquals("Demo: unavailable", r.label());
    }

    @Test
    public void guardedStatelessEntryKeepsBaseLabelButLosesItsAction() {
        EntryDef plain = new EntryDef("plain", "server_menu", "travel", 10, null,
                "COMPASS", "Plain door", List.of(), List.of("[player] plain"),
                Set.of(Platform.JAVA, Platform.BEDROCK),
                "ExcelGone", null, null, Map.of(), null, null, EntryDef.Origin.FILE);
        ResolvedEntry r = resolve(plain, new FakeViewer());
        assertTrue(r.guarded());
        assertEquals("Plain door", r.label());
        assertEquals(List.of("[message] &eNope, unavailable."), r.action());
    }

    @Test
    public void platformFilterExcludesTheOtherRenderer() {
        EntryDef javaOnly = new EntryDef("java_only", "server_menu", "travel", 10, null,
                "COMPASS", "Java only", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA), null, null, null, Map.of(),
                null, null, EntryDef.Origin.FILE);
        assertEquals(1, EntryResolver.resolve("server_menu", List.of(javaOnly),
                new FakeViewer().platform(Platform.JAVA), NOW, CONFIG).size());
        assertEquals(0, EntryResolver.resolve("server_menu", List.of(javaOnly),
                new FakeViewer().platform(Platform.BEDROCK), NOW, CONFIG).size());
    }

    @Test
    public void wrongMenuIsExcluded() {
        assertEquals(0, EntryResolver.resolve("other_menu", List.of(toggle()),
                happyViewer(), NOW, CONFIG).size());
    }

    @Test
    public void javaOrderingIsGroupThenOrderThenId() {
        EntryDef a = plainEntry("b_id", "settings", 10);
        EntryDef b = plainEntry("a_id", "settings", 10);
        EntryDef c = plainEntry("c_id", "travel", 99);
        List<ResolvedEntry> out = EntryResolver.resolve("server_menu",
                List.of(a, b, c), new FakeViewer(), NOW, CONFIG);
        assertEquals(List.of("c_id", "a_id", "b_id"),
                out.stream().map(ResolvedEntry::id).toList());
    }

    @Test
    public void bedrockOrderingIsTheFlatWeight() {
        EntryDef x = new EntryDef("x", "server_menu", "settings", 1, 300,
                "COMPASS", "X", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null, Map.of(),
                null, null, EntryDef.Origin.FILE);
        EntryDef y = new EntryDef("y", "server_menu", "travel", 99, 5,
                "COMPASS", "Y", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null, Map.of(),
                null, null, EntryDef.Origin.FILE);
        List<ResolvedEntry> out = EntryResolver.resolve("server_menu", List.of(x, y),
                new FakeViewer().platform(Platform.BEDROCK), NOW, CONFIG);
        assertEquals(List.of("y", "x"), out.stream().map(ResolvedEntry::id).toList());
    }

    @Test
    public void bedrockLabelCarriesGroupColourOnly() {
        RegistryConfig cfg = new RegistryConfig(RegistryConfig.SERVER_ZONE, "x",
                Map.of(), Map.of("settings", "&8"));
        EntryDef e = plainEntry("lbl", "settings", 1);
        ResolvedEntry r = EntryResolver.resolve("server_menu", List.of(e),
                new FakeViewer().platform(Platform.BEDROCK), NOW, cfg).get(0);
        assertEquals("§8Lbl", EntryResolver.bedrockButtonLabel(r, cfg));
    }

    private static EntryDef plainEntry(String id, String group, int order) {
        return new EntryDef(id, "server_menu", group, order, null,
                "COMPASS", "Lbl", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null, Map.of(),
                null, LocalDate.of(2027, 1, 1), EntryDef.Origin.FILE);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
