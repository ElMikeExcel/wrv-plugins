package com.winkreyesvalley.excelmagiclevel.store;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** ExcelMagicLevel — store round-trip, backup rotation, corruption paths (SPEC §7.1). */
public class PlayerStoreTest {
    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private final List<String> warnings = new ArrayList<>();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    private PlayerStore storeIn(File dir) {
        return new PlayerStore(new File(dir, "players.yml"), warnings::add);
    }

    @Test
    public void roundTrip() throws Exception {
        File dir = tmp.newFolder();
        PlayerStore store = storeIn(dir);
        store.load();
        PlayerRecord a = store.record(alice);
        a.addXp(1700);
        a.addCasts(42);
        a.setQuiet(true);
        store.record(bob).addXp(100);
        store.saveNow();
        assertTrue(new File(dir, "players.yml").exists());

        PlayerStore reloaded = storeIn(dir);
        reloaded.load();
        assertEquals(1700, reloaded.find(alice).totalXp());
        assertEquals(42, reloaded.find(alice).totalCasts());
        assertTrue(reloaded.find(alice).quiet());
        assertEquals(100, reloaded.find(bob).totalXp());
        assertFalse(reloaded.find(bob).quiet());
    }

    @Test
    public void backupRotatesThePreviousState() throws Exception {
        File dir = tmp.newFolder();
        PlayerStore store = storeIn(dir);
        store.load();
        store.record(alice).addXp(100);
        store.saveNow();
        store.record(alice).addXp(100);
        store.saveNow();
        assertTrue(store.backupFile().exists());
        PlayerStore fromBak = new PlayerStore(store.backupFile(), warnings::add);
        fromBak.load();
        assertEquals("bak holds the state before the last save", 100, fromBak.find(alice).totalXp());
    }

    @Test
    public void corruptFileIsPreservedAndNeverSilentlyZeroed() throws Exception {
        File dir = tmp.newFolder();
        File file = new File(dir, "players.yml");
        Files.writeString(file.toPath(), "players:\n  broken: [unclosed\n   :::not yaml at all:::\n", StandardCharsets.UTF_8);
        PlayerStore store = storeIn(dir);
        store.load(); // must not throw
        assertEquals(0, store.size());
        assertFalse("a loud warning is required", warnings.isEmpty());
        File[] preserved = dir.listFiles((d, name) -> name.contains(".corrupt-"));
        assertNotNull(preserved);
        assertEquals(1, preserved.length);
    }

    @Test
    public void missingFileWithBackupRecoversLoudly() throws Exception {
        File dir = tmp.newFolder();
        PlayerStore store = storeIn(dir);
        store.load();
        store.record(alice).addXp(500);
        store.saveNow();
        store.record(alice).addXp(1);
        store.saveNow(); // now players.yml AND players.yml.bak both exist
        assertTrue(new File(dir, "players.yml").delete());
        warnings.clear();
        PlayerStore recovered = storeIn(dir);
        recovered.load();
        assertNotNull(recovered.find(alice));
        assertEquals(500, recovered.find(alice).totalXp()); // the .bak state
        assertFalse(warnings.isEmpty());
    }

    @Test
    public void malformedEntriesAreSkippedNotFatal() throws Exception {
        File dir = tmp.newFolder();
        File file = new File(dir, "players.yml");
        Files.writeString(file.toPath(),
                "players:\n"
                        + "  not-a-uuid:\n    total-xp: 10\n"
                        + "  " + alice + ":\n    total-xp: -5\n"
                        + "  " + bob + ":\n    total-xp: 250\n    total-casts: 3\n    quiet: false\n",
                StandardCharsets.UTF_8);
        PlayerStore store = storeIn(dir);
        store.load();
        assertNull(store.find(alice));
        assertEquals(250, store.find(bob).totalXp());
        assertEquals(2, warnings.size());
    }

    @Test
    public void debouncedSaveRespectsTheQuietPeriod() throws Exception {
        File dir = tmp.newFolder();
        PlayerStore store = storeIn(dir);
        store.load();
        long t0 = System.currentTimeMillis();
        store.record(alice).addXp(10);
        store.markDirty();
        assertFalse(store.saveIfDue(t0, 5000));            // inside the debounce window
        assertFalse(new File(dir, "players.yml").exists());
        assertTrue(store.saveIfDue(t0 + 6000, 5000));      // window elapsed -> writes
        assertTrue(new File(dir, "players.yml").exists());
        assertFalse(store.saveIfDue(t0 + 12000, 5000));    // clean now, nothing to do
    }

    @Test
    public void findNeverCreates() throws Exception {
        File dir = tmp.newFolder();
        PlayerStore store = storeIn(dir);
        store.load();
        assertNull(store.find(alice));
        assertFalse(store.isDirty());
        assertEquals(0, store.size());
    }
}
