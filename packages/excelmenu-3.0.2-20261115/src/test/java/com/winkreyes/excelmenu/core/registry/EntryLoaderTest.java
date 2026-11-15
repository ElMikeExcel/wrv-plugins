package com.winkreyes.excelmenu.core.registry;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EntryLoaderTest {

    private static EntryLoader.Result load(String id, String yaml) {
        return EntryLoader.load(id,
                new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String VALID = """
            id: demo
            menu: server_menu
            group: help
            order: 15
            icon: COMPASS
            label: '&a&lDemo'
            action: '[player] demo'
            """;

    @Test
    public void validEntryLoadsWithDefaults() {
        EntryLoader.Result r = load("demo", VALID);
        assertTrue(r.errors().toString(), r.ok());
        assertEquals("demo", r.def().id());
        assertEquals(2, r.def().platforms().size()); // default: both
        assertEquals(List.of("[player] demo"), r.def().action());
        assertNull(r.def().orderBedrock());
        assertEquals(r.def().order(), r.def().bedrockOrder()); // fallback
    }

    @Test
    public void unknownKeyIsRefused() {
        EntryLoader.Result r = load("demo", VALID + "bogus-key: 1\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("unknown entry key 'bogus-key'")));
    }

    @Test
    public void malformedYamlIsRefused() {
        EntryLoader.Result r = load("demo", "id: demo\n  bad indent: [\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("YAML parse failed")));
    }

    @Test
    public void duplicateYamlKeyIsRefused() {
        EntryLoader.Result r = load("demo", VALID + "order: 99\n");
        assertFalse(r.ok());
    }

    @Test
    public void idMustMatchFilename() {
        EntryLoader.Result r = load("other", VALID);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("filename says 'other'")));
    }

    @Test
    public void missingGroupIsRefused() {
        EntryLoader.Result r = load("demo", VALID.replace("group: help\n", ""));
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("'group' is required")));
    }

    @Test
    public void missingMenuIsRefused() {
        EntryLoader.Result r = load("demo", VALID.replace("menu: server_menu\n", ""));
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("'menu' is required")));
    }

    @Test
    public void unknownPlatformIsRefused() {
        EntryLoader.Result r = load("demo", VALID + "platforms: [java, pocket]\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("unknown platform 'pocket'")));
    }

    @Test
    public void unknownActionTagIsRefused() {
        EntryLoader.Result r = load("demo",
                VALID.replace("'[player] demo'", "'[teleport] demo'"));
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("unknown action tag [teleport]")));
    }

    @Test
    public void actionMayBeAList() {
        EntryLoader.Result r = load("demo", VALID.replace(
                "action: '[player] demo'",
                "action:\n  - '[close]'\n  - '[player] demo'"));
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(List.of("[close]", "[player] demo"), r.def().action());
    }

    @Test
    public void badDateIsRefused() {
        EntryLoader.Result r = load("demo", VALID + "visible-until: next tuesday\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("must be a date")));
    }

    @Test
    public void untilBeforeFromIsRefused() {
        EntryLoader.Result r = load("demo",
                VALID + "visible-from: 2026-11-05\nvisible-until: 2026-10-31\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("is before 'visible-from'")));
    }

    @Test
    public void permissionAliasConflictIsRefused() {
        EntryLoader.Result r = load("demo",
                VALID + "permission: a.b\nrequires-permission: c.d\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("disagree")));
    }

    @Test
    public void permissionAliasAgreementMerges() {
        EntryLoader.Result r = load("demo",
                VALID + "permission: a.b\nrequires-permission: a.b\n");
        assertTrue(r.errors().toString(), r.ok());
        assertEquals("a.b", r.def().requiresPermission());
    }

    @Test
    public void statesWithoutSourceIsRefused() {
        EntryLoader.Result r = load("demo", VALID + """
                states:
                  'ON': { label: 'On' }
                """);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("needs a source")));
    }

    @Test
    public void statusPlaceholderWithoutStatesIsRefused() {
        EntryLoader.Result r = load("demo", VALID + "status-placeholder: '%demo_status%'\n");
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("'states' is empty")));
    }

    @Test
    public void badPlaceholderShapeIsRefused() {
        EntryLoader.Result r = load("demo", VALID + """
                status-placeholder: demo_status
                states:
                  ON: { label: 'On' }
                """);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("must be a %token%")));
    }

    @Test
    public void statesSourceConflictIsRefused() {
        EntryLoader.Result r = load("demo", VALID + """
                status-placeholder: '%a_status%'
                states:
                  source: '%b_status%'
                  ON: { label: 'On' }
                """);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("disagree")));
    }

    @Test
    public void variantUnknownKeyIsRefused() {
        EntryLoader.Result r = load("demo", VALID + """
                status-placeholder: '%demo_status%'
                states:
                  'ON': { label: 'On', sparkle: true }
                """);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("unknown variant key 'sparkle'")));
    }

    @Test
    public void unquotedBooleanStateKeyIsRefusedWithGuidance() {
        // YAML 1.1 reads unquoted ON as boolean true; the variant
        // would silently key as "true" and never match the "ON" the
        // placeholder returns. Refused, with the fix in the message.
        EntryLoader.Result r = load("demo", VALID + """
                status-placeholder: '%demo_status%'
                states:
                  ON: { label: 'On' }
                """);
        assertFalse(r.ok());
        assertTrue(r.errors().toString(),
                r.errors().stream().anyMatch(e -> e.contains("quote state keys")));
    }

    @Test
    public void quotedStateKeysLoad() {
        EntryLoader.Result r = load("demo", VALID + """
                status-placeholder: '%demo_status%'
                states:
                  'ON': { label: 'On' }
                  'false': { label: 'Sealed' }
                """);
        assertTrue(r.errors().toString(), r.ok());
        assertEquals(2, r.def().states().size());
    }

    @Test
    public void fullGuardedEntryLoads() {
        EntryLoader.Result r = load("demo", VALID + """
                order-bedrock: 42
                requires-plugin: ExcelDemo
                requires-permission: demo.toggle
                status-placeholder: '%demo_status%'
                states:
                  'ON': { icon: LIME_DYE, label: 'On', action: '[player] demo off' }
                  'OFF': { icon: GRAY_DYE, label: 'Off', action: '[player] demo on' }
                  'default': { icon: GRAY_DYE, label: 'Demo: unavailable' }
                visible-from: 2026-10-31
                visible-until: 2026-11-05
                """);
        assertTrue(r.errors().toString(), r.ok());
        assertNotNull(r.def());
        assertEquals(42, (int) r.def().orderBedrock());
        assertEquals(42, r.def().bedrockOrder());
        assertEquals("ExcelDemo", r.def().requiresPlugin());
        assertEquals("demo.toggle", r.def().requiresPermission());
        assertEquals(3, r.def().states().size());
        assertEquals("2026-10-31", r.def().visibleFrom().toString());
        assertEquals("2026-11-05", r.def().visibleUntil().toString());
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
