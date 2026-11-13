// ExcelMenu — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package com.winkreyes.excelmenu.core.screens;

import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The confirmation gate's fail-closed matrix (DESIGN-2.0 §2.4): the
 * ported TeleportConfirmationGate must refuse every stale, foreign,
 * late, offline or "No" answer, consume tickets single-use, and let
 * exactly one shape through — the live ticket, same session, same
 * world, online, Yes, inside the TTL.
 */
public class TeleportConfirmationGateTest {

    private static final UUID PLAYER = UUID.randomUUID();
    private static final UUID WORLD = UUID.randomUUID();
    private static final Object SESSION = new Object();
    private static final long T0 = 1_000_000_000L;

    private TeleportConfirmationGate gate;
    private TeleportConfirmationGate.Ticket ticket;

    private void begin() {
        gate = new TeleportConfirmationGate();
        ticket = gate.begin(PLAYER, SESSION, WORLD, T0);
    }

    @Test
    public void theOneGoodAnswerTeleports() {
        begin();
        assertTrue(gate.answer(ticket, SESSION, WORLD, T0 + 1, true, true));
    }

    @Test
    public void noMeansNo() {
        begin();
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 1, true, false));
    }

    @Test
    public void nullChoiceNeverTeleports() {
        // A closed form reports no choice; only an explicit Yes runs.
        assertFalse(TeleportConfirmationGate.confirmChoiceTeleports(null));
        assertFalse(TeleportConfirmationGate.confirmChoiceTeleports(Boolean.FALSE));
        assertTrue(TeleportConfirmationGate.confirmChoiceTeleports(Boolean.TRUE));
    }

    @Test
    public void singleUseEvenOnSuccess() {
        begin();
        assertTrue(gate.answer(ticket, SESSION, WORLD, T0 + 1, true, true));
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 2, true, true));
    }

    @Test
    public void singleUseEvenOnFailure() {
        // The ported semantics: answer() consumes the ticket either
        // way — a wrong-world answer cannot be retried in-world.
        begin();
        assertFalse(gate.answer(ticket, SESSION, UUID.randomUUID(), T0 + 1, true, true));
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 2, true, true));
    }

    @Test
    public void wrongSessionRefused() {
        begin();
        assertFalse(gate.answer(ticket, new Object(), WORLD, T0 + 1, true, true));
    }

    @Test
    public void offlineRefused() {
        begin();
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 1, false, true));
    }

    @Test
    public void expiryRefused() {
        begin();
        long atTtl = T0 + TeleportConfirmationGate.TTL_NANOS;
        assertFalse(gate.answer(ticket, SESSION, WORLD, atTtl, true, true));
    }

    @Test
    public void justInsideTtlAccepted() {
        begin();
        long justInside = T0 + TeleportConfirmationGate.TTL_NANOS - 1;
        assertTrue(gate.answer(ticket, SESSION, WORLD, justInside, true, true));
    }

    @Test
    public void clockBackwardsRefused() {
        begin();
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 - 1, true, true));
    }

    @Test
    public void aNewerPromptReplacesTheTicket() {
        begin();
        TeleportConfirmationGate.Ticket newer =
                gate.begin(PLAYER, SESSION, WORLD, T0 + 10);
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 11, true, true));
        assertTrue(gate.answer(newer, SESSION, WORLD, T0 + 12, true, true));
    }

    @Test
    public void cancelDropsOnlyTheLiveTicket() {
        begin();
        TeleportConfirmationGate.Ticket newer =
                gate.begin(PLAYER, SESSION, WORLD, T0 + 10);
        gate.cancel(ticket); // stale ticket: must not drop the live one
        assertTrue(gate.answer(newer, SESSION, WORLD, T0 + 11, true, true));
    }

    @Test
    public void cancelThenAnswerRefused() {
        begin();
        gate.cancel(ticket);
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 1, true, true));
    }

    @Test
    public void invalidateDropsThePrompt() {
        begin();
        gate.invalidate(PLAYER);
        assertFalse(gate.answer(ticket, SESSION, WORLD, T0 + 1, true, true));
    }

    @Test
    public void unknownTicketRefused() {
        begin();
        TeleportConfirmationGate.Ticket foreign = new TeleportConfirmationGate.Ticket(
                PLAYER, SESSION, WORLD, T0);
        assertFalse(gate.answer(foreign, SESSION, WORLD, T0 + 1, true, true));
    }
}
