package com.winkreyes.excelmenu.core.render;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** The §2.5 invariants at the token level: single-use, supersede,
 *  expiry, world binding, quit invalidation. */
public class FormSessionTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    public void answerOnceOkThenConsumed() {
        FormSession.Store store = new FormSession.Store();
        FormSession.Token t = store.issue(PLAYER, "server_menu", null, "world", 1_000, 60_000);
        assertEquals(FormSession.Verdict.OK,
                store.answer(PLAYER, t.id(), "world", 2_000));
        assertEquals(FormSession.Verdict.CONSUMED,
                store.answer(PLAYER, t.id(), "world", 3_000));
    }

    @Test
    public void newerFormSupersedesOlder() {
        FormSession.Store store = new FormSession.Store();
        FormSession.Token old = store.issue(PLAYER, "server_menu", null, "world", 0, 60_000);
        store.issue(PLAYER, "commands_menu", null, "world", 1, 60_000);
        assertEquals(FormSession.Verdict.SUPERSEDED,
                store.answer(PLAYER, old.id(), "world", 2));
    }

    @Test
    public void expiredAnswerRefused() {
        FormSession.Store store = new FormSession.Store();
        FormSession.Token t = store.issue(PLAYER, "server_menu", null, "world", 0, 1_000);
        assertEquals(FormSession.Verdict.EXPIRED,
                store.answer(PLAYER, t.id(), "world", 2_000));
        assertFalse(store.hasLive(PLAYER));
    }

    @Test
    public void worldChangeRefused() {
        FormSession.Store store = new FormSession.Store();
        FormSession.Token t = store.issue(PLAYER, "server_menu", null, "world", 0, 60_000);
        assertEquals(FormSession.Verdict.WORLD_CHANGED,
                store.answer(PLAYER, t.id(), "world_nether", 10));
    }

    @Test
    public void invalidateDropsToken() {
        FormSession.Store store = new FormSession.Store();
        FormSession.Token t = store.issue(PLAYER, "server_menu", null, "world", 0, 60_000);
        store.invalidate(PLAYER);
        assertEquals(FormSession.Verdict.NO_SESSION,
                store.answer(PLAYER, t.id(), "world", 10));
    }

    @Test
    public void unknownPlayerHasNoSession() {
        FormSession.Store store = new FormSession.Store();
        assertEquals(FormSession.Verdict.NO_SESSION,
                store.answer(UUID.randomUUID(), 42L, "world", 0));
        assertTrue(store.issue(PLAYER, "m", null, "world", 0, 1000) != null);
    }
}

// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
