package com.winkreyes.excelmenu.core.registry;

import com.winkreyes.excelmenu.core.platform.Platform;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EntryRegistryTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private static final String ENTRY = """
            id: %s
            menu: server_menu
            group: help
            order: 10
            icon: COMPASS
            label: 'Demo %s'
            action: '[player] demo'
            """;

    private File writeEntry(String id) throws Exception {
        File f = new File(tmp.getRoot(), id + ".yml");
        Files.writeString(f.toPath(), ENTRY.formatted(id, id), StandardCharsets.UTF_8);
        return f;
    }

    @Test
    public void directoryLoadMergesValidFiles() throws Exception {
        writeEntry("alpha");
        writeEntry("beta");
        EntryRegistry reg = new EntryRegistry();
        EntryRegistry.LoadOutcome out = reg.loadDirectory(tmp.getRoot().toPath());
        assertTrue(out.failed().toString(), out.failed().isEmpty());
        assertEquals(Set.of("alpha", "beta"), out.loaded().keySet());
        reg.installLoaded(out.loaded());
        assertNotNull(reg.entry("alpha"));
    }

    @Test
    public void duplicateIdsCannotEnterThroughFiles() throws Exception {
        // The loader binds id to filename, so two files in one folder
        // can never present the same id: a file claiming another
        // file's id is refused by the loader (idMustMatchFilename in
        // EntryLoaderTest), and the registry's own duplicate branch is
        // defense-in-depth behind it. The reachable duplicate paths —
        // API vs file and API vs API — are covered below.
        File a = new File(tmp.getRoot(), "same.yml");
        Files.writeString(a.toPath(), ENTRY.formatted("same", "same"), StandardCharsets.UTF_8);
        File b = new File(tmp.getRoot(), "two.yml");
        Files.writeString(b.toPath(), ENTRY.formatted("two", "two"), StandardCharsets.UTF_8);
        EntryRegistry reg = new EntryRegistry();
        EntryRegistry.LoadOutcome out = reg.loadDirectory(tmp.getRoot().toPath());
        assertEquals(Set.of("same", "two"), out.loaded().keySet());
        assertTrue(out.failed().isEmpty());
    }

    @Test
    public void badFileIsRefusedButTheRestLoad() throws Exception {
        writeEntry("good");
        File bad = new File(tmp.getRoot(), "bad.yml");
        Files.writeString(bad.toPath(), "id: bad\nmenu: server_menu\n", StandardCharsets.UTF_8);
        EntryRegistry reg = new EntryRegistry();
        EntryRegistry.LoadOutcome out = reg.loadDirectory(tmp.getRoot().toPath());
        assertEquals(Set.of("good"), out.loaded().keySet());
        assertTrue(out.failed().containsKey("bad"));
    }

    @Test
    public void apiRegistrationAndCollisionRules() throws Exception {
        writeEntry("from_file");
        EntryRegistry reg = new EntryRegistry();
        reg.installLoaded(reg.loadDirectory(tmp.getRoot().toPath()).loaded());

        EntryDef api = apiEntry("from_api");
        assertNull(reg.registerApi(api));
        assertEquals(EntryDef.Origin.API, reg.entry("from_api").origin());

        // Collision with the file entry: refused, file entry untouched.
        String refusal = reg.registerApi(apiEntry("from_file"));
        assertNotNull(refusal);
        assertTrue(refusal, refusal.contains("already registered"));
        assertEquals(EntryDef.Origin.FILE, reg.entry("from_file").origin());

        // Collision with another API entry: refused too.
        assertNotNull(reg.registerApi(apiEntry("from_api")));

        assertTrue(reg.unregisterApi("from_api"));
        assertNull(reg.entry("from_api"));
        assertFalse(reg.unregisterApi("from_api"));
        // File entries can never be unregistered through the API.
        assertFalse(reg.unregisterApi("from_file"));
        assertNotNull(reg.entry("from_file"));
    }

    @Test
    public void apiEntryCollidingWithFileOnReloadIsRefused() throws Exception {
        writeEntry("shared");
        EntryRegistry reg = new EntryRegistry();
        // Register the API entry BEFORE the file exists, then reload a
        // directory that now contains a file with the same id.
        assertNull(reg.registerApi(apiEntry("shared")));
        EntryRegistry.LoadOutcome out = reg.loadDirectory(tmp.getRoot().toPath());
        assertTrue(out.failed().toString(),
                out.failed().values().stream().flatMap(List::stream)
                        .anyMatch(e -> e.contains("API registration collides")));
        assertEquals(EntryDef.Origin.FILE, out.loaded().get("shared").origin());
    }

    @Test
    public void materialValidationFlagsBadIcons() {
        EntryDef bad = new EntryDef("bad", "server_menu", "help", 1, null,
                "NOT_A_MATERIAL", "X", List.of(), List.of("[close]"),
                Set.of(Platform.JAVA), null, null, null, Map.of(),
                null, null, EntryDef.Origin.FILE);
        List<String> errors = EntryRegistry.validateMaterials(
                Map.of("bad", bad), Set.of("COMPASS", "STONE"));
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("unknown material 'NOT_A_MATERIAL'"));
    }

    @Test
    public void reloadDiffNamesAddedRemovedChanged() {
        EntryDef a1 = apiEntry("a");
        EntryDef b = apiEntry("b");
        EntryDef a2 = new EntryDef("a", "server_menu", "help", 99, null,
                "COMPASS", "Demo a", List.of(), List.of("[player] demo"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null, Map.of(),
                null, null, EntryDef.Origin.API);
        RegistryDiff.Diff d = RegistryDiff.between(
                Map.of("a", a1, "b", b), Map.of("a", a2));
        assertEquals(List.of(), d.added());
        assertEquals(List.of("b"), d.removed());
        assertEquals(List.of("a"), d.changed());
        assertEquals(0, d.unchanged());
        assertTrue(d.summary().contains("~1 changed (a)"));
    }

    private static EntryDef apiEntry(String id) {
        return new EntryDef(id, "server_menu", "help", 10, null,
                "COMPASS", "Demo " + id, List.of(), List.of("[player] demo"),
                Set.of(Platform.JAVA, Platform.BEDROCK), null, null, null, Map.of(),
                null, null, EntryDef.Origin.API);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
