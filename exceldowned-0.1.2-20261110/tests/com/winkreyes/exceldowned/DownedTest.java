package com.winkreyes.exceldowned;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
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

    // ---------- 0.1.2: console admin revive/clear (AdminPolicy) ----------

    @Test
    void adminGateMatrix() {
        assertTrue(AdminPolicy.mayUseAdmin(true, false), "console is explicitly allowed for the admin verbs");
        assertTrue(AdminPolicy.mayUseAdmin(true, true), "console with the node allowed");
        assertTrue(AdminPolicy.mayUseAdmin(false, true), "an operator holding exceldowned.admin is allowed");
        assertFalse(AdminPolicy.mayUseAdmin(false, false), "a player without the admin node is refused");
        assertEquals("exceldowned.admin", AdminPolicy.PERMISSION,
                "the gate is the EXISTING admin node (audit §4), not a new one");
    }

    @Test
    void adminRoutingBothVerbs() {
        var revive = AdminPolicy.route(new String[]{"revive", "Patient"}, true, false);
        assertEquals(AdminPolicy.Route.ADMIN_REVIVE, revive.route());
        assertEquals("Patient", revive.target(), "the target is carried verbatim");
        var clear = AdminPolicy.route(new String[]{"clear", "Patient"}, true, false);
        assertEquals(AdminPolicy.Route.ADMIN_CLEAR, clear.route());
        assertEquals("Patient", clear.target());
        // operator player with the node routes identically; case-insensitive
        assertEquals(AdminPolicy.Route.ADMIN_REVIVE,
                AdminPolicy.route(new String[]{"REVIVE", "Patient"}, false, true).route());
        assertEquals(AdminPolicy.Route.ADMIN_CLEAR,
                AdminPolicy.route(new String[]{"Clear", "Patient"}, false, true).route());
    }

    @Test
    void adminDenialPrecedesUsageAndTargets() {
        // a non-admin player is DENIED even for a well-formed call, and
        // even for a malformed one — the denial leaks no usage shape
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"revive", "Patient"}, false, false).route());
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"clear", "Patient"}, false, false).route());
        assertEquals(AdminPolicy.Route.ADMIN_DENIED,
                AdminPolicy.route(new String[]{"revive"}, false, false).route());
    }

    @Test
    void adminUsageErrors() {
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"revive"}, true, false).route(), "missing target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"clear"}, true, false).route(), "missing target");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"revive", "Patient", "extra"}, true, false).route(),
                "extra arguments are a usage error, never a partial act");
        assertEquals(AdminPolicy.Route.ADMIN_USAGE,
                AdminPolicy.route(new String[]{"clear", " "}, true, false).route(), "blank target");
    }

    @Test
    void legacySurfaceRoutesUnchanged() {
        // every pre-0.1.2 /downed shape stays LEGACY for every sender kind
        String[][] shapes = {{}, {"reload"}, {"Patient"}, {"someplayer"},
                {"revivex", "Patient"}, {"clearly"}, {"revive-me"}, {"RELOADED"}};
        for (String[] shape : shapes) {
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, true, false).route(),
                    "console keeps legacy for " + java.util.Arrays.toString(shape));
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, false, false).route(),
                    "plain player keeps legacy for " + java.util.Arrays.toString(shape));
            assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(shape, false, true).route(),
                    "operator keeps legacy for " + java.util.Arrays.toString(shape));
        }
        assertEquals(AdminPolicy.Route.LEGACY, AdminPolicy.route(null, true, false).route(),
                "a null arg array is legacy (defensive)");
    }

    @Test
    void adminTargetClassification() {
        assertEquals(AdminPolicy.TargetKind.ONLINE_DOWNED, AdminPolicy.targetKind(true, true, false));
        assertEquals(AdminPolicy.TargetKind.ONLINE_DOWNED, AdminPolicy.targetKind(true, true, true),
                "an online session wins over the durable scan");
        assertEquals(AdminPolicy.TargetKind.ONLINE_NOT_DOWNED, AdminPolicy.targetKind(true, false, false));
        assertEquals(AdminPolicy.TargetKind.OFFLINE_DOWNED, AdminPolicy.targetKind(false, false, true));
        assertEquals(AdminPolicy.TargetKind.UNKNOWN, AdminPolicy.targetKind(false, false, false));
    }

    @Test
    void adminReviveDecisions() {
        assertEquals(AdminPolicy.ReviveDecision.PROCEED,
                AdminPolicy.decideRevive(AdminPolicy.TargetKind.ONLINE_DOWNED));
        assertEquals(AdminPolicy.ReviveDecision.NOT_DOWNED,
                AdminPolicy.decideRevive(AdminPolicy.TargetKind.ONLINE_NOT_DOWNED));
        assertEquals(AdminPolicy.ReviveDecision.OFFLINE_ONLY,
                AdminPolicy.decideRevive(AdminPolicy.TargetKind.OFFLINE_DOWNED),
                "revive restores live health: an offline session is reported, never silently 'revived'");
        assertEquals(AdminPolicy.ReviveDecision.UNKNOWN,
                AdminPolicy.decideRevive(AdminPolicy.TargetKind.UNKNOWN));
    }

    @Test
    void adminClearDecisions() {
        assertEquals(AdminPolicy.ClearDecision.PROCEED,
                AdminPolicy.decideClear(AdminPolicy.TargetKind.ONLINE_DOWNED));
        assertEquals(AdminPolicy.ClearDecision.PROCEED,
                AdminPolicy.decideClear(AdminPolicy.TargetKind.OFFLINE_DOWNED),
                "clear is the offline tool: a durable record may be removed");
        assertEquals(AdminPolicy.ClearDecision.NOT_DOWNED,
                AdminPolicy.decideClear(AdminPolicy.TargetKind.ONLINE_NOT_DOWNED));
        assertEquals(AdminPolicy.ClearDecision.UNKNOWN,
                AdminPolicy.decideClear(AdminPolicy.TargetKind.UNKNOWN));
    }

    /**
     * The exercised state proof at the pure layer (the audit's
     * condition, BuildMode shape): the admin acts run against a real
     * registry of real DownedSessions — revive decision removes the
     * session exactly once and never consumes the death latch (a
     * revive is not a death); clear removes an OFFLINE session
     * restored from its persisted map; redundant acts are no-ops.
     * The live Bukkit halves (health write, presentation) are proven
     * by the 0.1.2 desk rig.
     */
    @Test
    void adminActsExercisedOnRealSessions() {
        Map<UUID, DownedSession> registry = new HashMap<>();
        DownedSession s = session(300_000);
        registry.put(ID, s);
        // revive: PROCEED -> the registry loses the session exactly once
        assertEquals(AdminPolicy.ReviveDecision.PROCEED,
                AdminPolicy.decideRevive(AdminPolicy.targetKind(true, registry.containsKey(ID), false)));
        assertSame(s, registry.remove(ID), "the revive act consumes the session");
        assertFalse(s.deathConsumed(), "a revive never touches the death latch");
        // redundant revive/clear on the now-standing player are no-ops
        assertEquals(AdminPolicy.ReviveDecision.NOT_DOWNED,
                AdminPolicy.decideRevive(AdminPolicy.targetKind(true, registry.containsKey(ID), false)));
        assertEquals(AdminPolicy.ClearDecision.NOT_DOWNED,
                AdminPolicy.decideClear(AdminPolicy.targetKind(true, registry.containsKey(ID), false)));
        // clear: an offline session restored from its persisted map
        DownedSession offline = DownedSession.fromMap(session(300_000).toMap());
        Map<UUID, DownedSession> durable = new HashMap<>();
        durable.put(ID, offline);
        assertEquals(AdminPolicy.ClearDecision.PROCEED,
                AdminPolicy.decideClear(AdminPolicy.targetKind(false, false, durable.containsKey(ID))));
        assertSame(offline, durable.remove(ID), "the clear act removes the durable record");
        assertTrue(durable.isEmpty(), "nothing lingers for the next login to resurrect");
        assertFalse(offline.deathConsumed(), "a clear never runs a death either");
    }

    @Test
    void pluginYmlPins012() throws Exception {
        // local-build.sh runs the suite from the project root
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("version: 0.1.2"), "plugin.yml declares version 0.1.2");
        assertTrue(yml.contains("exceldowned.admin:"), "the existing admin node stays registered");
        int at = yml.indexOf("exceldowned.admin:");
        String block = yml.substring(at, Math.min(yml.length(), at + 300));
        assertTrue(block.contains("default: op"), "exceldowned.admin still defaults to op");
        assertTrue(yml.contains("revive <player>") && yml.contains("clear <player>"),
                "the usage line names both admin forms");
        assertTrue(BuildInfo.BUILD_ID.startsWith("EXC-DOWNED-0.1.2-WRV-")
                        && BuildInfo.BUILD_ID.endsWith("-d0wn3d"),
                "fingerprint lineage: EXC-DOWNED semantic marker + d0wn3d tag carried forward");
    }
}
