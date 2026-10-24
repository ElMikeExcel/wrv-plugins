package com.winkreyes.trash;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class TrashStoreTest {
    @TempDir Path dir;
    AtomicLong now = new AtomicLong(1_000_000);
    UUID a = UUID.randomUUID();
    TrashStore store() { return new TrashStore(dir.resolve("batches.dat"), now::get); }
    static List<String> raw(TrashStore.Batch b) { return b.entries().stream().map(TrashStore::data).toList(); }

    // ---- 0.1.0 behavior kept
    @Test void emptyBatchIgnoredAndKeepsPrevious() {
        TrashStore s = store(); s.put(a, List.of("x"));
        assertFalse(s.put(a, List.of()));
        assertEquals(List.of("x"), raw(s.take(a).orElseThrow()));
    }
    @Test void undoIsOneShot() {
        TrashStore s = store(); s.put(a, List.of("x", "y"));
        assertTrue(s.take(a).isPresent());
        assertTrue(s.take(a).isEmpty(), "second undo does nothing");
    }
    @Test void newTrashReplacesOld() {
        TrashStore s = store(); s.put(a, List.of("old")); s.put(a, List.of("new"));
        assertEquals(List.of("new"), raw(s.take(a).orElseThrow()));
    }
    @Test void expiresAfterFiveMinutes() {
        TrashStore s = store(); s.put(a, List.of("x"));
        now.addAndGet(TrashStore.TTL_MS - 1); assertTrue(s.has(a));
        now.addAndGet(1); assertFalse(s.has(a)); assertTrue(s.take(a).isEmpty());
    }
    @Test void survivesRestartAndTakePersists() {
        TrashStore s = store(); s.put(a, List.of("QUJD", "REVG"));
        TrashStore r = store(); r.load();
        assertEquals(List.of("QUJD", "REVG"), raw(r.take(a).orElseThrow()));
        TrashStore r2 = store(); r2.load();
        assertTrue(r2.take(a).isEmpty(), "take must persist removal (no dupe after crash)");
    }
    @Test void purge() {
        TrashStore s = store(); s.put(a, List.of("x")); now.addAndGet(TrashStore.TTL_MS);
        assertEquals(1, s.purgeExpired());
    }
    // ---- 0.2.0
    @Test void okPressesInOneScreenAreOneBatch() {
        TrashStore s = store();
        long id = s.add(a, 0, "w;1;2;3", List.of(TrashStore.chest("A")));
        long id2 = s.add(a, id, "w;1;2;3", List.of(TrashStore.player("B")));
        assertEquals(id, id2);
        TrashStore.Batch b = s.take(a).orElseThrow();
        assertEquals(List.of("C:A", "P:B"), b.entries()); assertTrue(b.fromChest());
        assertTrue(s.take(a).isEmpty());
    }
    @Test void newScreenReplacesLastBatch() {
        TrashStore s = store();
        long id = s.add(a, 0, TrashStore.INV, List.of(TrashStore.player("A")));
        long id2 = s.add(a, 0, TrashStore.INV, List.of(TrashStore.player("B")));
        assertNotEquals(id, id2);
        assertEquals(List.of("P:B"), s.take(a).orElseThrow().entries());
    }
    @Test void afterUndoSameScreenStartsNewBatch() {
        TrashStore s = store();
        long id = s.add(a, 0, TrashStore.INV, List.of(TrashStore.player("A")));
        s.take(a);
        long id2 = s.add(a, id, TrashStore.INV, List.of(TrashStore.player("B")));
        assertNotEquals(id, id2);
        assertEquals(List.of("P:B"), s.take(a).orElseThrow().entries(), "the undone A must not come back");
    }
    @Test void migratesV010File() throws Exception {
        Files.writeString(dir.resolve("batches.dat"), a + "\t" + now.get() + "\tQUJD\tREVG\n");
        TrashStore s = store(); s.load();
        TrashStore.Batch b = s.take(a).orElseThrow();
        assertEquals(List.of("P:QUJD", "P:REVG"), b.entries()); assertFalse(b.fromChest());
        assertTrue(Files.readString(dir.resolve("batches.dat")).startsWith("#v2"));
    }
    @Test void v010ReadingV2SeesExpired() throws Exception {
        TrashStore s = store(); s.add(a, 0, "w;1;2;3", List.of(TrashStore.chest("A")));
        for (String line : Files.readAllLines(dir.resolve("batches.dat"))) {
            String[] p = line.split("\t");
            if (p.length < 3) continue; // 0.1.0 skips the header
            long created = Long.parseLong(p[1]);       // 0.1.0 reads the id here
            assertTrue(now.get() - created >= TrashStore.TTL_MS, "0.1.0 must treat v2 batches as expired (no dupe on rollback)");
        }
    }
    @Test void downtimeDoesNotEatTheWindow() {
        TrashStore s = store(); s.put(a, List.of("x"));
        now.addAndGet(60_000); s.save();          // stop 1 min after OK
        now.addAndGet(10 * 60_000);               // 10 min down
        TrashStore r = store(); r.load();
        assertTrue(r.has(a)); assertEquals(240, r.secondsLeft(a));
    }
    @Test void layout() {
        for (int i = 0; i < 27; i++) { assertTrue(Layout.aBag(i)); assertFalse(Layout.aButtons(i)); assertTrue(Layout.bMirror(i)); }
        for (int i = 27; i < 36; i++) { assertTrue(Layout.aButtons(i)); assertTrue(Layout.bButtons(i)); }
        for (int i = 36; i < 54; i++) assertTrue(Layout.bBag(i));
        assertEquals(31, Layout.UNDO_SLOT);
        assertEquals(2, Layout.pages(54)); assertEquals(1, Layout.pages(5));
        assertEquals(30, Layout.real(1, 3, 54)); assertEquals(-1, Layout.real(0, 5, 5)); assertEquals(4, Layout.real(0, 4, 5));
    }
}
