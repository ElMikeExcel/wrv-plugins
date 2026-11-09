package com.winkreyes.exceldowned;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * State-machine proofs (DESIGN §1–§5): entry invariants, the two-step
 * give-up, bleed-out expiry, damage eating the window, TrainingWheels
 * composition, persistence round-trip, and exactly-once death.
 *
 * Excel — Wink Reyes Valley (Iris / Mike).
 */
public class DownedTest {

    private static final UUID ID = UUID.randomUUID();
    private static final long T0 = 1_000_000L;

    private DownedSession session(long bleedMs) {
        return new DownedSession(ID, "Patient", T0, bleedMs, "entity_attack",
                "world", 1.0, 64.0, 2.0);
    }

    @Test
    void deadlineIsDownedAtPlusBleedOut() {
        DownedSession s = session(300_000);
        assertEquals(T0 + 300_000, s.deadlineMs());
        assertFalse(s.isExpired(T0 + 299_999));
        assertTrue(s.isExpired(T0 + 300_000));
        assertEquals(150_000, s.remainingMs(T0 + 150_000));
    }

    @Test
    void damageEatsTheWindowScaled() {
        DownedSession s = session(300_000);
        // zombie hit 3.0 raw, downed factor 0.25 -> 0.75 scaled -> 750 ms eaten
        double scaled = DownedMath.scaleDamage(3.0, 0.25);
        assertEquals(0.75, scaled, 1e-9);
        long eaten = s.eatDamage(scaled, 1.0);
        assertEquals(750, eaten);
        assertEquals(T0 + 300_000 - 750, s.deadlineMs());
        assertEquals(0.75, s.totalWindowDamage(), 1e-9);
    }

    @Test
    void trainingWheelsComposesMultiplicatively() {
        // Rylie standing: TW 0.25. Rylie downed: another 0.25 on top.
        assertEquals(0.0625, DownedMath.composeFactor(0.25, 0.25), 1e-9);
        // order independence
        assertEquals(DownedMath.composeFactor(0.25, 0.5), DownedMath.composeFactor(0.5, 0.25), 1e-9);
    }

    @Test
    void giveUpIsTwoStepWithinWindow() {
        DownedSession s = session(300_000);
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 1_000, 10_000));
        assertFalse(s.isGiveUpConfirmed());
        assertEquals(DownedSession.GiveUpResult.DIED, s.giveUp(T0 + 5_000, 10_000));
        assertTrue(s.isGiveUpConfirmed());
        // confirmation is intent, not the death: the death path's latch
        // must still be available to consume exactly once (rig run-5 catch)
        assertFalse(s.deathConsumed());
        assertTrue(s.consumeDeath());
        assertFalse(s.consumeDeath());
    }

    @Test
    void giveUpAfterWindowRearmsInsteadOfDying() {
        DownedSession s = session(300_000);
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 1_000, 10_000));
        // 11 s later: the arm is stale — this must NOT kill
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 12_000, 10_000));
        assertFalse(s.deathConsumed());
        // but a prompt third call inside the new window does
        assertEquals(DownedSession.GiveUpResult.DIED, s.giveUp(T0 + 13_000, 10_000));
    }

    @Test
    void deathIsExactlyOnce() {
        DownedSession s = session(300_000);
        assertTrue(s.consumeDeath());
        assertFalse(s.consumeDeath());
        assertFalse(s.consumeDeath());
        // and a give-up after the death was consumed cannot re-die
        assertEquals(DownedSession.GiveUpResult.NOT_DOWNED, s.giveUp(T0 + 1, 10_000));
    }

    @Test
    void persistenceRoundTripPreservesDeadlineAndDamage() {
        DownedSession s = session(300_000);
        s.eatDamage(2.0, 1.0);
        Map<String, Object> m = s.toMap();
        DownedSession back = DownedSession.fromMap(m);
        assertEquals(ID, back.playerId());
        assertEquals(s.deadlineMs(), back.deadlineMs());
        assertEquals(2.0, back.totalWindowDamage(), 1e-9);
        assertEquals("entity_attack", back.cause());
    }

    @Test
    void zeroAndNegativeDamageEatNothing() {
        DownedSession s = session(300_000);
        assertEquals(0, s.eatDamage(0, 1.0));
        assertEquals(0, s.eatDamage(-5, 1.0));
        assertEquals(T0 + 300_000, s.deadlineMs());
    }

    // ---------- 0.1.1: the button confirm shares the typed arm state ----------

    @Test
    void uiOpenArmsWithoutConfirming() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000);
        assertTrue(s.isGiveUpArmed(T0 + 1_000, 10_000));
        assertFalse(s.isGiveUpConfirmed());
        assertFalse(s.deathConsumed());
    }

    @Test
    void uiLetGoWithinWindowConfirmsAndDeathStaysExactlyOnce() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000);
        assertTrue(s.confirmGiveUp(T0 + 5_000, 10_000));
        assertTrue(s.isGiveUpConfirmed());
        // confirming is intent, not the death: the latch is still the
        // single guard, consumed once by the death path (run-5 catch)
        assertFalse(s.deathConsumed());
        assertTrue(s.consumeDeath());
        assertFalse(s.consumeDeath());
        // a second confirm after the death is spent changes nothing
        assertFalse(s.confirmGiveUp(T0 + 6_000, 10_000));
    }

    @Test
    void uiLetGoAfterWindowLapsesIsStaleAndDoesNotRearm() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000);
        // 11 s later the arm is stale: a late "Let go" must do NOTHING
        assertFalse(s.confirmGiveUp(T0 + 12_000, 10_000));
        assertFalse(s.isGiveUpConfirmed());
        // and it must not have silently re-armed (a following typed
        // /giveup is a fresh first step, never a surprise death)
        assertFalse(s.isGiveUpArmed(T0 + 12_000, 10_000));
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 12_500, 10_000));
        assertFalse(s.deathConsumed());
    }

    @Test
    void uiLetGoWithoutArmIsStale() {
        DownedSession s = session(300_000);
        assertFalse(s.confirmGiveUp(T0 + 1_000, 10_000));
        assertFalse(s.isGiveUpConfirmed());
    }

    @Test
    void keepFightingDisarmCancels() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000);
        s.disarmGiveUp();
        assertFalse(s.isGiveUpArmed(T0 + 2_000, 10_000));
        // after a cancel, neither a UI confirm nor a single typed
        // /giveup can kill — the next /giveup is a fresh first step
        assertFalse(s.confirmGiveUp(T0 + 2_000, 10_000));
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 2_500, 10_000));
        assertFalse(s.deathConsumed());
    }

    @Test
    void menuArmThenTypedGiveUpConfirms() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000); // menu opened
        // typed /giveup while the menu stands armed = the confirm step
        assertEquals(DownedSession.GiveUpResult.DIED, s.giveUp(T0 + 4_000, 10_000));
        assertTrue(s.isGiveUpConfirmed());
    }

    @Test
    void typedArmThenUiConfirmConfirms() {
        DownedSession s = session(300_000);
        // chat-fallback arm (first typed /giveup), then a UI "Let go"
        assertEquals(DownedSession.GiveUpResult.ARMED, s.giveUp(T0 + 1_000, 10_000));
        assertTrue(s.confirmGiveUp(T0 + 4_000, 10_000));
        assertTrue(s.isGiveUpConfirmed());
    }

    @Test
    void disarmNeverUnconfirms() {
        DownedSession s = session(300_000);
        s.armGiveUp(T0 + 1_000);
        assertTrue(s.confirmGiveUp(T0 + 2_000, 10_000));
        s.disarmGiveUp(); // a stray close event after choosing Let go
        assertTrue(s.isGiveUpConfirmed());
        assertTrue(s.confirmGiveUp(T0 + 3_000, 10_000)); // intent stands
    }

    @Test
    void routingMatrix() {
        // Java always gets the inventory menu
        assertEquals(GiveUpRoute.Route.JAVA_MENU, GiveUpRoute.route(false, false));
        assertEquals(GiveUpRoute.Route.JAVA_MENU, GiveUpRoute.route(false, true));
        // Bedrock gets the form when the API is there…
        assertEquals(GiveUpRoute.Route.BEDROCK_FORM, GiveUpRoute.route(true, true));
        // …and the chat fallback when it is not — the typed path never breaks
        assertEquals(GiveUpRoute.Route.CHAT_FALLBACK, GiveUpRoute.route(true, false));
    }
}
