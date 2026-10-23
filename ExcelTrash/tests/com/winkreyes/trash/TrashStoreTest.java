package com.winkreyes.trash;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class TrashStoreTest {
    @TempDir Path dir;
    AtomicLong now = new AtomicLong(1_000_000);
    UUID a = UUID.randomUUID();
    TrashStore store() { return new TrashStore(dir.resolve("batches.dat"), now::get); }

    @Test void emptyBatchIgnoredAndKeepsPrevious() {
        TrashStore s = store(); s.put(a, List.of("x"));
        assertFalse(s.put(a, List.of()));
        assertEquals(List.of("x"), s.take(a).orElseThrow());
    }
    @Test void undoIsOneShot() {
        TrashStore s = store(); s.put(a, List.of("x", "y"));
        assertTrue(s.take(a).isPresent());
        assertTrue(s.take(a).isEmpty());
    }
    @Test void newTrashReplacesOld() {
        TrashStore s = store(); s.put(a, List.of("old")); s.put(a, List.of("new"));
        assertEquals(List.of("new"), s.take(a).orElseThrow());
    }
    @Test void expiresAfterFiveMinutes() {
        TrashStore s = store(); s.put(a, List.of("x"));
        now.addAndGet(TrashStore.TTL_MS - 1); assertTrue(s.has(a));
        now.addAndGet(1); assertFalse(s.has(a)); assertTrue(s.take(a).isEmpty());
    }
    @Test void survivesRestart() {
        TrashStore s = store(); s.put(a, List.of("QUJD", "REVG"));
        TrashStore r = store(); r.load();
        assertEquals(List.of("QUJD", "REVG"), r.take(a).orElseThrow());
        TrashStore r2 = store(); r2.load();
        assertTrue(r2.take(a).isEmpty(), "take must persist removal (no dupe after crash)");
    }
    @Test void expiredDroppedOnLoad() {
        TrashStore s = store(); s.put(a, List.of("x"));
        now.addAndGet(TrashStore.TTL_MS);
        TrashStore r = store(); r.load(); assertFalse(r.has(a));
    }
    @Test void purge() {
        TrashStore s = store(); s.put(a, List.of("x")); now.addAndGet(TrashStore.TTL_MS);
        assertEquals(1, s.purgeExpired());
    }
    @Test void slotPolicy() {
        for (int i = 0; i < 27; i++) { assertTrue(SlotPolicy.isStorage(i)); assertFalse(SlotPolicy.isButton(i)); }
        for (int i = 27; i < 36; i++) assertTrue(SlotPolicy.isButton(i));
        assertFalse(SlotPolicy.isButton(36)); assertTrue(SlotPolicy.isButton(SlotPolicy.UNDO_SLOT));
    }
}
