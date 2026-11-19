package com.winkreyes.exceldowned;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

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

    // ---------- 0.1.3: P-DOWNED-REVIVE-1 fix chain (FixPolicy) ----------

    @Test
    void totemInEitherHandPreventsDowning() {
        // D3-TOTEM-MAIN-1 / D3-TOTEM-OFF-1 (pure layer): a lethal,
        // non-void, uncancelled hit with a totem in either hand must
        // NOT enter DOWNED — vanilla consumes the totem instead.
        assertTrue(FixPolicy.holdsTotem(true, false));
        assertTrue(FixPolicy.holdsTotem(false, true));
        assertTrue(FixPolicy.holdsTotem(true, true));
        assertFalse(FixPolicy.entersDowned(true, false, false, true),
                "totem in a hand: totem first, no down");
    }

    @Test
    void noTotemLethalHitStillDowns() {
        // D3-NOTOTEM-1: same hit, no totem in either hand -> downed.
        assertFalse(FixPolicy.holdsTotem(false, false));
        assertTrue(FixPolicy.entersDowned(true, false, false, false));
        // D3-TOTEM-SECOND-1: after the totem is consumed the next
        // lethal hit is the no-totem case above — it downs.
        // Non-lethal, void, and already-cancelled hits never down:
        assertFalse(FixPolicy.entersDowned(false, false, false, false));
        assertFalse(FixPolicy.entersDowned(true, true, false, false),
                "D3-TOTEM-VOID-1: void stays a real death, never a down");
        assertFalse(FixPolicy.entersDowned(true, true, false, true),
                "void does not consult the totem for a down either");
        assertFalse(FixPolicy.entersDowned(true, false, true, false));
    }

    @Test
    void damageWhileDownedEatsNoWindowByDefault() {
        // Defect 1 window rule (run-363 evidence verdict): the incident
        // session's largest single eat was 28.95 s from one friendly
        // hit. Under the default rule that hit eats exactly zero, and
        // a session's deadline does not move when the listener applies
        // the policy (it only calls eatDamage when eaten > 0).
        assertEquals(0, FixPolicy.windowEatenMillis(false, 28.95, 1.0));
        assertEquals(0, FixPolicy.windowEatenMillis(false, 0.25, 1.0));
        assertEquals(0, FixPolicy.windowEatenMillis(false, 115.8, 1.0),
                "even the full Jester total (34.53 s at scale) eats nothing");
        DownedSession s = session(300_000);
        long eaten = FixPolicy.windowEatenMillis(false, 28.95, 1.0);
        if (eaten > 0) s.eatDamage(28.95, 1.0);
        assertEquals(T0 + 300_000, s.deadlineMs(), "deadline is pure wall-clock by default");
        assertEquals(300_000, s.remainingMs(T0));
    }

    @Test
    void windowEatingIsOptInOnly() {
        // The legacy scaled mechanism survives ONLY behind the explicit
        // operator opt-in (damage-eats-window: true).
        assertEquals(750, FixPolicy.windowEatenMillis(true, 0.75, 1.0));
        assertEquals(28_950, FixPolicy.windowEatenMillis(true, 28.95, 1.0));
        assertEquals(0, FixPolicy.windowEatenMillis(true, 0, 1.0));
    }

    @Test
    void reviveFallbackIsRangeAndWorldBounded() {
        // Defect 2 fix shape 5: the ResurrectAction fallback (and the
        // direct target) resolve only same-world and inside the one
        // aligned revive range (20).
        assertTrue(FixPolicy.withinReviveRange(true, 10 * 10, 20.0));
        assertTrue(FixPolicy.withinReviveRange(true, 20 * 20, 20.0),
                "the boundary itself is in range");
        assertFalse(FixPolicy.withinReviveRange(true, 21 * 21, 20.0),
                "D2-CAST-2: 21 blocks is out — no cross-map revive");
        assertFalse(FixPolicy.withinReviveRange(false, 1, 20.0),
                "another world is out at any distance");
    }

    @Test
    void grantIsVerifiedOnlyWhenTemplateResolves() {
        // Defect 2 grant honesty: on Nov 14 the fallback reported
        // grouprenewal + resurrection "verified" while Magic's
        // template was null (NPE in buildInventory). Presence in wand
        // data alone must never verify again.
        assertTrue(FixPolicy.grantVerified(true, true));
        assertFalse(FixPolicy.grantVerified(true, false),
                "wand-data presence with a dead template is NOT verified");
        assertFalse(FixPolicy.grantVerified(false, true));
        assertFalse(FixPolicy.grantVerified(false, false));
    }

    @Test
    void configPins013WindowRuleAndAlignedRanges() throws Exception {
        String cfg = Files.readString(Path.of("config.yml"));
        assertTrue(cfg.contains("damage-eats-window: false"),
                "the window rule ships OFF by default");
        assertTrue(cfg.contains("gate-range: 20.0") && cfg.contains("revive-range: 20.0"),
                "gate and revive ranges are the one aligned number: 20");
        assertTrue(cfg.contains("bleed-out-seconds: 300"), "bleed-out stays 300 s");
    }

    @Test
    void pluginYmlPins100() throws Exception {
        // local-build.sh runs the suite from the project root
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("version: 1.0.0"),
                "plugin.yml declares version 1.0.0 (Mike, Nov 17, 2026: the revive system ships as 1.0.0, not 0.1.6)");
        assertTrue(yml.contains("exceldowned.admin:"), "the existing admin node stays registered");
        int at = yml.indexOf("exceldowned.admin:");
        String block = yml.substring(at, Math.min(yml.length(), at + 300));
        assertTrue(block.contains("default: op"), "exceldowned.admin still defaults to op");
        assertTrue(yml.contains("revive <player>") && yml.contains("clear <player>"),
                "the usage line names both admin forms");
        assertTrue(yml.contains("revivepotion give <player>"),
                "the usage line names the revivepotion seam");
        assertTrue(BuildInfo.BUILD_ID.startsWith("EXC-DOWNED-1.0.0-WRV-")
                        && BuildInfo.BUILD_ID.endsWith("-d0wn3d"),
                "fingerprint lineage: EXC-DOWNED semantic marker + d0wn3d tag carried forward");
    }

    // ---------- 1.0.0: Revive Potion (RevivePotionPolicy) ----------
    // Spec A section 11.1. Mike's Nov 17, 2026 corrections apply:
    // potion cooldown is 10 minutes (600 s), separate from the
    // Cleric ability's; every tunable is config-fed (pins below).

    private static final PartyProvider.Membership PARTY = PartyProvider.Membership.PARTY;
    private static final PartyProvider.Membership NOT_PARTY = PartyProvider.Membership.NOT_PARTY;
    private static final PartyProvider.Membership UNKNOWN = PartyProvider.Membership.UNKNOWN;

    @Test
    void potionPromptMatrix() {
        // the happy path: standing carrier, downed party target,
        // same world, in prompt range (5.0), holding potions, off cooldown
        assertTrue(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, true, 4 * 4, 5.0, 2, false));
        assertTrue(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, true, 5 * 5, 5.0, 1, false), "the boundary itself prompts");
        // each single failing axis kills the prompt
        assertFalse(RevivePotionPolicy.promptEligible(false, false, false, true,
                PARTY, true, 1, 5.0, 2, false), "feature disabled");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, true, true,
                PARTY, true, 1, 5.0, 2, false), "a downed carrier is never prompted");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, false,
                PARTY, true, 1, 5.0, 2, false), "target not downed");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                NOT_PARTY, true, 1, 5.0, 2, false), "party-only (S-1)");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                UNKNOWN, true, 1, 5.0, 2, false), "UNKNOWN never counts as party");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, false, 1, 5.0, 2, false), "different world");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, true, 6 * 6, 5.0, 2, false), "outside prompt range");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, true, 1, 5.0, 0, false), "zero potions: no prompt (spec 5.4)");
        assertFalse(RevivePotionPolicy.promptEligible(true, false, false, true,
                PARTY, true, 1, 5.0, 2, true),
                "on the 10-minute potion cooldown: no prompt");
    }

    @Test
    void potionSelfExclusionPin() {
        // spec 5.3: C == T never prompts and never confirms, on any axis
        assertFalse(RevivePotionPolicy.promptEligible(true, true, false, true,
                PARTY, true, 0, 5.0, 3, false));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.SELF_TARGET,
                RevivePotionPolicy.confirmVerdict(true, true, true, false, false,
                        true, PARTY, true, 0, 6.0, 3));
    }

    @Test
    void potionConfirmMatrix() {
        // happy path at confirm range (6.0)
        assertEquals(RevivePotionPolicy.ConfirmVerdict.OK,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        true, PARTY, true, 6 * 6, 6.0, 1));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.STALE_TICKET,
                RevivePotionPolicy.confirmVerdict(true, false, false, false, false,
                        true, PARTY, true, 1, 6.0, 1),
                "a stale form answer is a silent no-op upstream");
        assertEquals(RevivePotionPolicy.ConfirmVerdict.CARRIER_DOWNED,
                RevivePotionPolicy.confirmVerdict(true, true, false, true, false,
                        true, PARTY, true, 1, 6.0, 1));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.CARRIER_ON_COOLDOWN,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, true,
                        true, PARTY, true, 1, 6.0, 1),
                "the 10-minute potion cooldown refuses a second use");
        assertEquals(RevivePotionPolicy.ConfirmVerdict.TARGET_NOT_DOWNED,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        false, PARTY, true, 1, 6.0, 1),
                "the race loser: target already resolved");
        assertEquals(RevivePotionPolicy.ConfirmVerdict.NOT_PARTY,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        true, UNKNOWN, true, 1, 6.0, 1));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.WRONG_WORLD,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        true, PARTY, false, 1, 6.0, 1));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.OUT_OF_RANGE,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        true, PARTY, true, 7 * 7, 6.0, 1));
        assertEquals(RevivePotionPolicy.ConfirmVerdict.NO_POTION,
                RevivePotionPolicy.confirmVerdict(true, true, false, false, false,
                        true, PARTY, true, 1, 6.0, 0),
                "the count is re-derived at click time, never trusted from the copy");
        assertEquals(RevivePotionPolicy.ConfirmVerdict.FEATURE_DISABLED,
                RevivePotionPolicy.confirmVerdict(false, true, false, false, false,
                        true, PARTY, true, 1, 6.0, 1));
    }

    @Test
    void potionOrderingInvariant() {
        // spec 3.5: consume if and only if the revive landed — every
        // race reason (spell won, bleed-out, give-up) spends nothing
        assertTrue(RevivePotionPolicy.shouldConsume(true));
        assertFalse(RevivePotionPolicy.shouldConsume(false));
    }

    @Test
    void potionCountAndConsumeOncePureCore() {
        // tagged stacks across hotbar + storage rows sum by amount;
        // untagged (vanilla) potions never count
        boolean[] tagged = {true, false, true, true, false};
        int[] amounts = {1, 1, 2, 1, 3};
        assertEquals(4, RevivePotionPolicy.countFromSlots(tagged, amounts));
        // consume picks the FIRST tagged slot, exactly once per call
        int idx = RevivePotionPolicy.consumeSlotIndex(tagged, amounts);
        assertEquals(0, idx);
        amounts[idx]--;
        assertEquals(3, RevivePotionPolicy.countFromSlots(tagged, amounts),
                "the count decrements by exactly 1");
        // a drained first stack is skipped; nothing tagged -> -1
        assertEquals(2, RevivePotionPolicy.consumeSlotIndex(tagged, amounts));
        assertEquals(-1, RevivePotionPolicy.consumeSlotIndex(
                new boolean[]{false, false}, new int[]{1, 1}));
        assertEquals(0, RevivePotionPolicy.countFromSlots(
                new boolean[]{}, new int[]{}), "an empty scan counts zero");
    }

    @Test
    void potionCooldownAndHintThrottle() {
        long cd = 600_000L; // Mike's 10 minutes, config-fed in production
        assertFalse(RevivePotionPolicy.cooldownActive(T0, 0, cd),
                "never used: no cooldown");
        assertTrue(RevivePotionPolicy.cooldownActive(T0 + 599_999, T0, cd));
        assertFalse(RevivePotionPolicy.cooldownActive(T0 + 600_000, T0, cd),
                "the cooldown lifts at exactly 10 minutes");
        assertFalse(RevivePotionPolicy.cooldownActive(T0, T0, 0),
                "a zero config cooldown disables the gate");
        // zero-hint throttle (5 s per pair)
        assertTrue(RevivePotionPolicy.hintAllowed(T0, 0, 5_000));
        assertFalse(RevivePotionPolicy.hintAllowed(T0 + 4_999, T0, 5_000));
        assertTrue(RevivePotionPolicy.hintAllowed(T0 + 5_000, T0, 5_000));
    }

    @Test
    void potionGiveRoutingAndGate() {
        var give = RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient"}, true, false);
        assertEquals(RevivePotionPolicy.GiveRoute.GIVE, give.route());
        assertEquals("Patient", give.target());
        assertEquals(1, give.amount(), "amount defaults to 1");
        var give3 = RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient", "3"}, false, true);
        assertEquals(RevivePotionPolicy.GiveRoute.GIVE, give3.route());
        assertEquals(3, give3.amount());
        var buy = RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "buy", "Patient"}, true, false);
        assertNotEquals(RevivePotionPolicy.GiveRoute.GIVE, buy.route(),
                "W6 charge leg: routeGive NEVER mints on a buy spelling — "
                        + "the paid verb belongs to routeBuy (free-buy regression pin)");
        // denial precedes usage and targets (0.1.2 discipline)
        assertEquals(RevivePotionPolicy.GiveRoute.DENIED, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient"}, false, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.DENIED, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give"}, false, false).route());
        // usage errors for the gated-in
        assertEquals(RevivePotionPolicy.GiveRoute.USAGE, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give"}, true, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.USAGE, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient", "0"}, true, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.USAGE, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient", "abc"}, true, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.USAGE, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient", "2", "extra"}, true, false).route());
        // the chat-fallback confirm verb is player-side, ungated
        assertEquals(RevivePotionPolicy.GiveRoute.CONFIRM, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "confirm"}, false, false).route());
        // everything else is not ours
        assertEquals(RevivePotionPolicy.GiveRoute.NOT_OURS, RevivePotionPolicy.routeGive(
                new String[]{"revive", "Patient"}, true, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.NOT_OURS, RevivePotionPolicy.routeGive(
                new String[]{}, true, false).route());
        assertEquals(-1, RevivePotionPolicy.parseGiveAmount("65"), "amount is capped at 64");
        assertEquals(64, RevivePotionPolicy.parseGiveAmount("64"));
    }

    @Test
    void potionShopBuyAliasRouting() {
        // W6 section 3.2 SUPERSEDES the piece-C free-buy alias: the
        // old [revive, buy, <player>] spelling (which minted free
        // through routeShopBuy) is retired — routeBuy claims ONLY
        // [revivepotion, buy, <player>], so the old spelling is
        // NOT_OURS on the buy route and can never reach a mint.
        assertEquals(RevivePotionPolicy.BuyRoute.NOT_OURS, RevivePotionPolicy.routeBuy(
                new String[]{"revive", "buy", "Patient"}, true).route(),
                "the retired 'revive buy' spelling never routes to a mint");
        assertEquals(RevivePotionPolicy.BuyRoute.NOT_OURS, RevivePotionPolicy.routeBuy(
                new String[]{"revive", "buy"}, true).route());
        assertNotEquals(RevivePotionPolicy.GiveRoute.GIVE, RevivePotionPolicy.routeGive(
                new String[]{"revive", "buy", "Patient"}, true, false).route(),
                "routeGive does not claim the retired spelling either");
        // and AdminPolicy still owns the plain admin revive
        assertEquals(AdminPolicy.Route.ADMIN_REVIVE,
                AdminPolicy.route(new String[]{"revive", "Patient"}, true, false).route());
    }

    @Test
    void configPins100ReviveSystem() throws Exception {
        // CONFIG RULE (Mike, Nov 17, 2026): every revive tunable is
        // config-editable with these defaults — pinned so a future
        // edit that hardcodes or drops one fails the suite.
        String cfg = Files.readString(Path.of("config.yml"));
        assertTrue(cfg.contains("revive-health-fraction: 0.5"), "revive HP 50% (existing key)");
        assertTrue(cfg.contains("revive-potion:"), "the revive-potion section exists");
        assertTrue(cfg.contains("enabled: true"), "master switch default on");
        assertTrue(cfg.contains("price: 5000"), "potion price $5,000 launch, config-editable");
        assertTrue(cfg.contains("use-cooldown-seconds: 600"),
                "potion cooldown is Mike's 10 minutes, NOT the spec's 2 s");
        assertTrue(cfg.contains("prompt-enabled: true"), "prompt behavior is a config switch");
        assertTrue(cfg.contains("prompt-range: 5.0") && cfg.contains("confirm-range: 6.0"),
                "prompt/confirm ranges are config values");
        assertTrue(cfg.contains("zero-hint-cooldown-seconds: 5"), "hint throttle is config");
        assertTrue(cfg.contains("cleric-revive:"), "the Cleric tunables share this config");
        assertTrue(cfg.contains("cooldown-seconds: 600"),
                "Cleric cooldown is Mike's 10 minutes, NOT the spec's 120 s");
        assertTrue(cfg.contains("unlock-em-level: 10"), "Cleric unlock point is config");
    }

    // ---------- 1.0.0: Cleric "Raise Ally" (ClericRevivePolicy) ----------
    // Design B (board-r506) sections 3 + 5; GRACE route D107 (real
    // 85.0 spend via trySpend behind the fail-soft probe); Mike's
    // amended baseline D108: cooldown 600 s success-only, unlock EM
    // level 10, range 20, every tunable config-fed. Desk-rig runtime
    // GRACE proof R1 is still owed pre-filing — these tests pin the
    // desk-side logic only and do not claim it.

    private static final ClericGate.Probe READY = ClericGate.Probe.READY;
    private static final ClericGate.Probe NOT_READY = ClericGate.Probe.NOT_INITIALIZED;
    private static final ClericGate.Probe UNAVAIL = ClericGate.Probe.UNAVAILABLE;
    private static final ClericRevivePolicy.Verdict C_OK = ClericRevivePolicy.Verdict.OK;

    /** The happy-path verdict inputs; matrix tests flip one axis. */
    private static ClericRevivePolicy.Verdict clericVerdict(boolean[] a) {
        return ClericRevivePolicy.verdict(a[0], READY, a[1], a[2], a[3], a[4],
                a[5], a[6], a[7], a[8], a[9], a[10], a[11]);
    }

    @Test
    void clericRootAndGraceIdentity() {
        assertTrue(ClericRevivePolicy.isClericRoot("cleric"));
        assertTrue(ClericRevivePolicy.isClericRoot(" Cleric "),
                "ids are read case/space-tolerantly");
        assertFalse(ClericRevivePolicy.isClericRoot("priest"),
                "a form id is not the root — the gate reads lineage.root()");
        assertFalse(ClericRevivePolicy.isClericRoot("paladin"));
        assertFalse(ClericRevivePolicy.isClericRoot(null));
        assertFalse(ClericRevivePolicy.isClericRoot(""));
        assertTrue(ClericRevivePolicy.isGrace("GRACE"));
        assertFalse(ClericRevivePolicy.isGrace("MANA"));
        assertFalse(ClericRevivePolicy.isGrace(null));
        assertEquals("excel.cleric.raise_ally", ClericRevivePolicy.ABILITY_ID,
                "house namespace — never an EM registry id (design section 3)");
    }

    @Test
    void clericLevelGateIsConfigFedAndFailsClosed() {
        assertTrue(ClericRevivePolicy.levelUnlocked(10, 10),
                "the Cleric root unlock itself qualifies (D-B1)");
        assertTrue(ClericRevivePolicy.levelUnlocked(31, 10),
                "a Priest-band Cleric keeps the root-kit companion");
        assertFalse(ClericRevivePolicy.levelUnlocked(9, 10));
        assertFalse(ClericRevivePolicy.levelUnlocked(-1, 10),
                "an unreported level never unlocks — a missing read refuses");
        assertTrue(ClericRevivePolicy.levelUnlocked(30, 30),
                "the unlock value is a parameter, not a constant");
        assertFalse(ClericRevivePolicy.levelUnlocked(10, 30));
    }

    @Test
    void clericGraceAffordabilityUsesEmsOwnEpsilon() {
        assertTrue(ClericRevivePolicy.graceAffordable(85.0, 85.0));
        assertTrue(ClericRevivePolicy.graceAffordable(100.0, 85.0));
        assertFalse(ClericRevivePolicy.graceAffordable(84.999, 85.0));
        assertTrue(ClericRevivePolicy.graceAffordable(85.0 - 5.0E-10, 85.0),
                "inside EM's 1e-9 tolerance (spike guard analysis)");
        assertFalse(ClericRevivePolicy.graceAffordable(null, 85.0),
                "an unreported balance never affords");
        assertFalse(ClericRevivePolicy.graceAffordable(Double.NaN, 85.0));
    }

    @Test
    void clericGateOrderMatrix() {
        // axes: enabled, hasClass, clericRoot, gracePool, levelOk,
        //       casterDowned, casterDead, cooling, targetInRange,
        //       requireParty, targetIsParty, graceOk
        boolean[] ok = {true, true, true, true, true,
                false, false, false, true, false, false, true};
        assertEquals(C_OK, clericVerdict(ok));
        // each single failing axis refuses with its own verdict
        boolean[] a;
        a = ok.clone(); a[0] = false;
        assertEquals(ClericRevivePolicy.Verdict.FEATURE_DISABLED, clericVerdict(a));
        a = ok.clone(); a[1] = false;
        assertEquals(ClericRevivePolicy.Verdict.NO_ACTIVE_CLASS, clericVerdict(a));
        a = ok.clone(); a[2] = false;
        assertEquals(ClericRevivePolicy.Verdict.NOT_CLERIC, clericVerdict(a),
                "a non-Cleric active class is refused at the class gate");
        a = ok.clone(); a[3] = false;
        assertEquals(ClericRevivePolicy.Verdict.NOT_CLERIC, clericVerdict(a),
                "a non-GRACE pool is not a Cleric, whatever the root says");
        a = ok.clone(); a[4] = false;
        assertEquals(ClericRevivePolicy.Verdict.LEVEL_TOO_LOW, clericVerdict(a));
        a = ok.clone(); a[5] = true;
        assertEquals(ClericRevivePolicy.Verdict.CASTER_DOWNED, clericVerdict(a));
        a = ok.clone(); a[6] = true;
        assertEquals(ClericRevivePolicy.Verdict.CASTER_DEAD, clericVerdict(a));
        a = ok.clone(); a[7] = true;
        assertEquals(ClericRevivePolicy.Verdict.ON_COOLDOWN, clericVerdict(a));
        a = ok.clone(); a[8] = false;
        assertEquals(ClericRevivePolicy.Verdict.NO_TARGET_IN_RANGE, clericVerdict(a));
        a = ok.clone(); a[11] = false;
        assertEquals(ClericRevivePolicy.Verdict.GRACE_SHORT, clericVerdict(a),
                "GRACE is the LAST pre-check — affordability never masks an earlier gate");
        // probe states precede every class question (design 5.1 step 1)
        assertEquals(ClericRevivePolicy.Verdict.MODULE_UNAVAILABLE,
                ClericRevivePolicy.verdict(true, UNAVAIL, false, false, false,
                        false, false, false, false, false, false, false, false));
        assertEquals(ClericRevivePolicy.Verdict.MODULE_NOT_READY,
                ClericRevivePolicy.verdict(true, NOT_READY, false, false, false,
                        false, false, false, false, false, false, false, false));
        assertEquals(ClericRevivePolicy.Verdict.MODULE_UNAVAILABLE,
                ClericRevivePolicy.verdict(true, null, true, true, true,
                        true, false, false, false, true, false, false, true),
                "a null probe answer is UNAVAILABLE, never a pass");
        // order: disabled beats unavailable; cooldown beats no-target
        assertEquals(ClericRevivePolicy.Verdict.FEATURE_DISABLED,
                ClericRevivePolicy.verdict(false, UNAVAIL, false, false, false,
                        false, false, false, false, false, false, false, false));
        a = ok.clone(); a[7] = true; a[8] = false;
        assertEquals(ClericRevivePolicy.Verdict.ON_COOLDOWN, clericVerdict(a));
        // party rule (D-B3): consulted only when the config requires it
        a = ok.clone(); a[9] = true; a[10] = false;
        assertEquals(ClericRevivePolicy.Verdict.NOT_PARTY, clericVerdict(a));
        a = ok.clone(); a[9] = true; a[10] = true;
        assertEquals(C_OK, clericVerdict(a));
        a = ok.clone(); a[9] = false; a[10] = false;
        assertEquals(C_OK, clericVerdict(a),
                "default require-party=false: a non-party ally is raisable");
    }

    @Test
    void clericTargetPickingAimedThenNearest() {
        boolean[] elig = {true, true, true};
        double[] dist = {100.0, 25.0, 64.0}; // 10, 5, 8 blocks
        assertEquals(2, ClericRevivePolicy.pickTarget(2, elig, dist),
                "the aimed-at ally wins over a nearer one (design section 3)");
        assertEquals(1, ClericRevivePolicy.pickTarget(-1, elig, dist),
                "no aim: the nearest is raised");
        assertEquals(1, ClericRevivePolicy.pickTarget(9, elig, dist),
                "an out-of-range aim index is ignored, nearest stands");
        boolean[] one = {false, true, false};
        assertEquals(1, ClericRevivePolicy.pickTarget(-1, one, dist),
                "the single downed ally in range is picked");
        assertEquals(1, ClericRevivePolicy.pickTarget(0, one, dist),
                "an ineligible aimed index falls back to nearest eligible");
        boolean[] none = {false, false, false};
        assertEquals(-1, ClericRevivePolicy.pickTarget(-1, none, dist));
        assertEquals(-1, ClericRevivePolicy.pickTarget(0, null, null));
    }

    @Test
    void clericChargeAndCooldownAreSuccessOnly() {
        // the no-burn invariant (design 5.1 step 7): charge + cooldown
        // if and only if the revive landed
        assertTrue(ClericRevivePolicy.shouldCharge(true));
        assertFalse(ClericRevivePolicy.shouldCharge(false),
                "a lost race charges no GRACE and starts no cooldown");
        long t0 = 2_000_000L;
        long cd = 600_000L; // D108: 600 s, config-fed into the policy
        assertFalse(ClericRevivePolicy.cooldownActive(t0, 0, cd),
                "no prior success = no cooldown");
        assertTrue(ClericRevivePolicy.cooldownActive(t0 + 599_999, t0, cd));
        assertFalse(ClericRevivePolicy.cooldownActive(t0 + 600_000, t0, cd),
                "the boundary itself is free again");
        assertEquals(1L, ClericRevivePolicy.cooldownRemainingMs(t0 + 599_999, t0, cd));
        assertEquals(0L, ClericRevivePolicy.cooldownRemainingMs(t0 + 600_000, t0, cd));
        assertFalse(ClericRevivePolicy.cooldownActive(t0 + 1, t0, 0),
                "a zero config cooldown disables the backstop, never wedges it");
    }

    @Test
    void clericProbeFailsSoftWithoutAServer() {
        // The harness has no EliteMobs module running: the gate must
        // answer NOT_INITIALIZED or UNAVAILABLE, never READY, and
        // never throw (the fail-soft probe discipline, D107).
        EmClericGate gate = new EmClericGate(Logger.getLogger("cleric-test"));
        ClericGate.Probe probe = gate.probe();
        assertNotNull(probe);
        assertNotEquals(READY, probe);
        assertFalse(gate.view(null).hasActiveClass(),
                "a null player reads as no class, without touching EM");
        assertFalse(gate.trySpend(null, 85.0),
                "a null player spends nothing and throws nothing");
    }

    @Test
    void clericConfigAndPluginPins() throws Exception {
        // CONFIG RULE (D108): every Raise Ally tunable is config-fed
        // with Mike's values — pinned so a future edit that hardcodes
        // or drops one fails the suite.
        String cfg = Files.readString(Path.of("config.yml"));
        assertTrue(cfg.contains("grace-cost: 85.0"),
                "GRACE cost 85.0 is config (the tree's Rewrite Fate price)");
        assertTrue(cfg.contains("range: 20.0"),
                "Cleric range 20 is config (the shared revive geometry)");
        assertTrue(cfg.contains("require-party: false"),
                "D-B3 ships OFF: no Together-party requirement in v1");
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("raiseally:"), "the /raiseally door is registered");
        assertTrue(yml.contains("aliases: [raise]"), "the /raise alias ships");
        assertTrue(yml.contains("EliteMobs"),
                "EliteMobs is named (softdepend) — absence stands the ability down");
        assertTrue(yml.contains("softdepend: [ExcelTogether, Geyser-Spigot, floodgate, EliteMobs, Vault]"),
                "EliteMobs stays a SOFT depend: Downed boots without it "
                        + "(Vault joined the same softdepend line with the W6 charge leg)");
    }

    // ---------- 0.1.5: deferred self-check policy (TemplateCheckPolicy) ----------
    // Diagnosis of record (board-r427 DIAGNOSIS.md §3d/§5): Magic's
    // spell registry loads ~7 s AFTER Downed's onEnable, so the old
    // synchronous probe read an empty registry and WARNed falsely on
    // every boot. The policy probes the builtin control key "heal":
    // control false = registry not loaded yet (defer, never WARN);
    // control true + target false = genuinely missing (WARN once).

    @Test
    void templateCheckControlKeyAndScheduleShape() {
        assertEquals("heal", TemplateCheckPolicy.CONTROL_KEY,
                "the control key is the known-good builtin spell");
        assertEquals(3, TemplateCheckPolicy.MAX_ATTEMPTS, "bounded: 3 attempts");
        // attempts land at +10 s, +20 s, +40 s after enable
        assertEquals(200L, TemplateCheckPolicy.delayTicksForAttempt(0));
        assertEquals(200L, TemplateCheckPolicy.delayTicksForAttempt(1));
        assertEquals(400L, TemplateCheckPolicy.delayTicksForAttempt(2));
        assertEquals(-1L, TemplateCheckPolicy.delayTicksForAttempt(3));
        assertFalse(TemplateCheckPolicy.isFinalAttempt(0));
        assertFalse(TemplateCheckPolicy.isFinalAttempt(1));
        assertTrue(TemplateCheckPolicy.isFinalAttempt(2));
    }

    @Test
    void templateCheckEmptyRegistryDefersAndNeverWarnsEarly() {
        // The live-boot shape: at probe time EVERY key answers false,
        // control included — that is an unloaded registry, not two
        // missing templates.
        var v = TemplateCheckPolicy.verdict(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
        assertEquals(TemplateCheckPolicy.Verdict.REGISTRY_NOT_LOADED, v);
        assertTrue(TemplateCheckPolicy.shouldRetry(v, 0));
        assertTrue(TemplateCheckPolicy.shouldRetry(v, 1));
        assertFalse(TemplateCheckPolicy.mayWarn(v, 0), "no template WARN on attempt 1");
        assertFalse(TemplateCheckPolicy.mayWarn(v, 1), "no template WARN on attempt 2");
        // exhausted with the registry STILL unloaded: the distinct
        // UNPROVEN warning may fire — never a per-key template WARN
        // (verdict stays REGISTRY_NOT_LOADED, not MISSING_KEYS).
        assertFalse(TemplateCheckPolicy.shouldRetry(v, 2));
        assertTrue(TemplateCheckPolicy.mayWarn(v, 2));
        assertNotEquals(TemplateCheckPolicy.Verdict.MISSING_KEYS, v);
    }

    @Test
    void templateCheckDeferredThenLoadedResolves() {
        // The R3 sequence: attempt 0 sees the empty registry, the
        // retry after Magic's deferred load sees every key resolve.
        var first = TemplateCheckPolicy.verdict(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE);
        assertEquals(TemplateCheckPolicy.Verdict.REGISTRY_NOT_LOADED, first);
        var second = TemplateCheckPolicy.verdict(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE);
        assertEquals(TemplateCheckPolicy.Verdict.ALL_RESOLVE, second);
        assertFalse(TemplateCheckPolicy.shouldRetry(second, 1), "settled: no further attempts");
        assertFalse(TemplateCheckPolicy.mayWarn(second, 1), "a resolving check never WARNs");
    }

    @Test
    void templateCheckMissingKeyInLoadedRegistryWarnsOncePerKey() {
        // The R2 negative-control shape: control + grouprenewal
        // resolve, resurrection absent from the POPULATED registry.
        var v = TemplateCheckPolicy.verdict(Boolean.TRUE, Boolean.FALSE, Boolean.TRUE);
        assertEquals(TemplateCheckPolicy.Verdict.MISSING_KEYS, v);
        assertTrue(TemplateCheckPolicy.mayWarn(v, 0),
                "a genuinely missing key fails loud immediately — no silent pass");
        assertFalse(TemplateCheckPolicy.shouldRetry(v, 0),
                "settled on the populated registry's answer: the WARN fires exactly once");
    }

    @Test
    void templateCheckProbeUnavailableStaysDistinct() {
        // Probe could not run (controller shape unavailable): the
        // UNPROVEN branch — never conflated with a missing template,
        // retried while attempts remain, warned only at the end.
        var v = TemplateCheckPolicy.verdict(null, Boolean.TRUE, Boolean.TRUE);
        assertEquals(TemplateCheckPolicy.Verdict.PROBE_UNAVAILABLE, v);
        assertTrue(TemplateCheckPolicy.shouldRetry(v, 0));
        assertFalse(TemplateCheckPolicy.mayWarn(v, 0));
        assertFalse(TemplateCheckPolicy.mayWarn(v, 1));
        assertTrue(TemplateCheckPolicy.mayWarn(v, 2), "final attempt: UNPROVEN warning");
        assertEquals(TemplateCheckPolicy.Verdict.PROBE_UNAVAILABLE,
                TemplateCheckPolicy.verdict(Boolean.TRUE, null, Boolean.TRUE),
                "a null target probe is also probe-unavailable, not missing");
    }

    // ---------- 0.1.4: P-DOWNED-PROVIDER-1 (S6 RC-1) ----------
    // ExcelTogether's PartyManager.Party exposes its members as a
    // public FIELD (public final List<UUID> members; sole method
    // size()). The 0.1.3 provider looked up accessor METHODS only,
    // so every real party read as [] / UNKNOWN. These tests fake
    // the FIELD shape, not an accessor shape (S6 §5.2).

    /** Mirrors PartyManager.Party exactly: field, no accessors. */
    public static final class RealShapeParty {
        public final List<UUID> members;
        RealShapeParty(List<UUID> members) { this.members = members; }
        public int size() { return members.size(); }
    }

    /** Legacy/other shape: accessor method, no field. */
    public static final class LegacyShapeParty {
        private final List<UUID> backing;
        LegacyShapeParty(List<UUID> backing) { this.backing = backing; }
        public List<UUID> getMembers() { return backing; }
    }

    /** Garbage shape: field of the wrong type, throwing accessor. */
    public static final class GarbageParty {
        public final String members = "not-a-collection";
        public List<UUID> getMembers() { throw new RuntimeException("boom"); }
    }

    /** Minimal PartyManager stand-in: partyOf(UUID) -> party/null. */
    public static final class FakeManager {
        private final Object party;
        FakeManager(Object party) { this.party = party; }
        public Object partyOf(UUID id) { return party; }
    }

    private static Collection<?> membersOfViaReflection(Object party) throws Exception {
        Method m = TogetherPartyProvider.class.getDeclaredMethod("membersOf", Object.class);
        m.setAccessible(true);
        return (Collection<?>) m.invoke(null, party);
    }

    private static TogetherPartyProvider providerWithParty(Object party) throws Exception {
        TogetherPartyProvider p = new TogetherPartyProvider(Logger.getLogger("downed-test"), false);
        Field fm = TogetherPartyProvider.class.getDeclaredField("manager");
        fm.setAccessible(true);
        fm.set(p, new FakeManager(party));
        Field fp = TogetherPartyProvider.class.getDeclaredField("partyOfMethod");
        fp.setAccessible(true);
        fp.set(p, FakeManager.class.getMethod("partyOf", UUID.class));
        return p;
    }

    @Test
    void realShapeFieldPartyMembersAreRead() throws Exception {
        // THE regression test (S6 §5.2 case 1): fails on 0.1.3 code.
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Collection<?> got = membersOfViaReflection(new RealShapeParty(List.of(a, b)));
        assertEquals(2, got.size(), "the members FIELD must be read");
        assertTrue(got.contains(a) && got.contains(b));
    }

    @Test
    void partyMembersOfRealShapeReturnsOtherMemberOnly() throws Exception {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        TogetherPartyProvider p = providerWithParty(new RealShapeParty(List.of(a, b)));
        Set<UUID> others = p.partyMembersOf(a);
        assertEquals(Set.of(b), others, "self excluded, party-mate returned");
    }

    @Test
    void membershipOfRealShapeTriState() throws Exception {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), stranger = UUID.randomUUID();
        TogetherPartyProvider inParty = providerWithParty(new RealShapeParty(List.of(a, b)));
        assertEquals(PartyProvider.Membership.PARTY, inParty.membershipOf(a, b),
                "same real-shape party -> PARTY");
        assertEquals(PartyProvider.Membership.NOT_PARTY, inParty.membershipOf(a, stranger),
                "party exists without the player -> NOT_PARTY");
        TogetherPartyProvider noParty = providerWithParty(null);
        assertEquals(PartyProvider.Membership.NOT_PARTY, noParty.membershipOf(a, b),
                "null party -> NOT_PARTY");
        TogetherPartyProvider emptyParty = providerWithParty(new RealShapeParty(List.of()));
        assertEquals(PartyProvider.Membership.UNKNOWN, emptyParty.membershipOf(a, b),
                "members read empty -> UNKNOWN (fail-soft rule pinned, never fail-open)");
    }

    @Test
    void legacyMethodShapeStillRead() throws Exception {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        Collection<?> got = membersOfViaReflection(new LegacyShapeParty(List.of(a, b)));
        assertEquals(2, got.size(), "the kept method fallback still works");
        assertTrue(got.contains(a) && got.contains(b));
    }

    @Test
    void garbagePartyFailsSoftNeverThrows() throws Exception {
        Collection<?> got = membersOfViaReflection(new GarbageParty());
        assertTrue(got.isEmpty(), "garbage shape degrades to the empty list");
        TogetherPartyProvider p = providerWithParty(new GarbageParty());
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertEquals(PartyProvider.Membership.UNKNOWN, p.membershipOf(a, b),
                "garbage shape -> UNKNOWN, no exception escapes");
        assertTrue(p.partyMembersOf(a).isEmpty());
    }

    // ---------- 1.0.0 charge leg: Revive Potion shop buy (W6 section 7.1) ----------
    // The 8 harness groups, all through fakes on the coordinator's
    // Ports seam (the DeathProtection pattern). Baseline note: the
    // single-product coordinator cannot produce a product-mismatched
    // token (REVIVE_POTION is the only product), so group 2 pins the
    // price-mismatch and expiry re-arms; product identity is pinned
    // by the route matrix and the journal's product field.

    private static final RevivePurchaseCoordinator.Product RP =
            RevivePurchaseCoordinator.Product.REVIVE_POTION;
    private static final double RP_PRICE = 5000.0;

    static final class FakePorts implements RevivePurchaseCoordinator.Ports {
        enum WithdrawMode { SUCCESS, DECLINE, NULL_RESPONSE, AMOUNT_MISMATCH, BALANCE_MISMATCH, THROW }
        enum DepositMode { SUCCESS, MISMATCH, THROW }
        double balance = 10_000.0;
        RevivePurchaseCoordinator.Eligibility eligibility = RevivePurchaseCoordinator.Eligibility.ELIGIBLE;
        WithdrawMode withdrawMode = WithdrawMode.SUCCESS;
        DepositMode depositMode = DepositMode.SUCCESS;
        RevivePurchaseCoordinator.DeliveryOutcome deliverOutcome =
                RevivePurchaseCoordinator.DeliveryOutcome.SUCCESS;
        int withdrawCalls, depositCalls, deliverCalls;
        double lastWithdrawAmount = -1, lastDepositAmount = -1;
        Runnable onWithdraw;
        @Override public RevivePurchaseCoordinator.Eligibility check(UUID id,
                RevivePurchaseCoordinator.Product product) { return eligibility; }
        @Override public double balance(UUID id) { return balance; }
        @Override public RevivePurchaseCoordinator.MoneyResult withdraw(UUID id, double price) {
            withdrawCalls++;
            lastWithdrawAmount = price;
            if (onWithdraw != null) { Runnable r = onWithdraw; onWithdraw = null; r.run(); }
            return switch (withdrawMode) {
                case SUCCESS -> { balance -= price; yield new RevivePurchaseCoordinator.MoneyResult(true, price, balance); }
                case DECLINE -> new RevivePurchaseCoordinator.MoneyResult(false, 0, balance);
                case NULL_RESPONSE -> null;
                case AMOUNT_MISMATCH -> { balance -= price; yield new RevivePurchaseCoordinator.MoneyResult(true, price - 1, balance); }
                case BALANCE_MISMATCH -> { balance -= price; yield new RevivePurchaseCoordinator.MoneyResult(true, price, balance + 1); }
                case THROW -> throw new RuntimeException("provider boom");
            };
        }
        @Override public RevivePurchaseCoordinator.MoneyResult deposit(UUID id, double price) {
            depositCalls++;
            lastDepositAmount = price;
            return switch (depositMode) {
                case SUCCESS -> { balance += price; yield new RevivePurchaseCoordinator.MoneyResult(true, price, balance); }
                case MISMATCH -> { balance += price; yield new RevivePurchaseCoordinator.MoneyResult(true, price, balance + 1); }
                case THROW -> throw new RuntimeException("provider boom");
            };
        }
        @Override public RevivePurchaseCoordinator.DeliveryOutcome deliver(UUID id,
                RevivePurchaseCoordinator.Product product) {
            deliverCalls++;
            return deliverOutcome;
        }
    }

    /** A journal whose appends / health can be failed on demand. */
    static final class FlakyJournal extends RevivePurchaseJournal {
        boolean failAppends;
        boolean forceUnhealthy;
        FlakyJournal(Path file) throws java.io.IOException { super(file); }
        @Override public synchronized void append(Entry entry) throws java.io.IOException {
            if (failAppends) throw new java.io.IOException("injected append failure");
            super.append(entry);
        }
        @Override public synchronized boolean healthy() {
            return !forceUnhealthy && super.healthy();
        }
    }

    private static RevivePurchaseJournal tempJournal() throws Exception {
        return new RevivePurchaseJournal(
                Files.createTempDirectory("revive-buy").resolve("purchases.journal"));
    }

    private static RevivePurchaseCoordinator coordinator(FakePorts ports,
            RevivePurchaseJournal journal, java.util.concurrent.atomic.AtomicLong clock) {
        return new RevivePurchaseCoordinator(ports, journal, clock::get, 20_000L, 1_000L);
    }

    private static RevivePurchaseJournal.Phase onlyPhase(RevivePurchaseJournal journal) {
        assertEquals(1, journal.entries().size(), "exactly one transaction journaled");
        return journal.entries().values().iterator().next().phase();
    }

    // Group 1: route matrix.
    @Test
    void buyRouteMatrix() {
        var buy = RevivePotionPolicy.routeBuy(
                new String[]{"revivepotion", "buy", "Patient"}, true);
        assertEquals(RevivePotionPolicy.BuyRoute.BUY, buy.route(),
                "console 'revivepotion buy <player>' is the paid verb");
        assertEquals("Patient", buy.target());
        assertEquals(RevivePotionPolicy.BuyRoute.DENIED, RevivePotionPolicy.routeBuy(
                new String[]{"revivepotion", "buy", "Patient"}, false).route(),
                "a player sender is refused (shop pointer at the command layer)");
        assertEquals(RevivePotionPolicy.BuyRoute.USAGE, RevivePotionPolicy.routeBuy(
                new String[]{"revivepotion", "buy"}, true).route(), "missing player");
        assertEquals(RevivePotionPolicy.BuyRoute.USAGE, RevivePotionPolicy.routeBuy(
                new String[]{"revivepotion", "buy", "Patient", "2"}, true).route(),
                "an amount arg is a usage error — quantity is fixed at 1");
        assertEquals(RevivePotionPolicy.BuyRoute.NOT_OURS, RevivePotionPolicy.routeBuy(
                new String[]{"revive", "buy", "Patient"}, true).route(),
                "the retired 'revive buy' spelling is never claimed");
        assertEquals(RevivePotionPolicy.BuyRoute.NOT_OURS, RevivePotionPolicy.routeBuy(
                new String[]{"revivepotion", "give", "Patient"}, true).route(),
                "give is not the buy route's business");
        // give routing + gate byte-identical in behavior to 1.0.0
        assertEquals(RevivePotionPolicy.GiveRoute.GIVE, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient"}, true, false).route());
        assertEquals(RevivePotionPolicy.GiveRoute.DENIED, RevivePotionPolicy.routeGive(
                new String[]{"revivepotion", "give", "Patient"}, false, false).route());
    }

    // Group 2: quote/confirm token discipline.
    @Test
    void buyQuoteArmsThenConfirmDebitsExactlyOnce() throws Exception {
        FakePorts ports = new FakePorts();
        RevivePurchaseJournal journal = tempJournal();
        var clock = new java.util.concurrent.atomic.AtomicLong(T0);
        RevivePurchaseCoordinator c = coordinator(ports, journal, clock);
        UUID buyer = UUID.randomUUID();
        // first confirm-path call (a bare confirm with no token) arms only
        var armed = c.confirm(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, armed.status());
        assertEquals(0, ports.withdrawCalls, "arming never withdraws");
        // the matching unexpired second call debits exactly once
        var bought = c.confirm(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.PURCHASED, bought.status());
        assertEquals(1, ports.withdrawCalls, "exactly one withdraw");
        assertEquals(RP_PRICE, ports.lastWithdrawAmount, 1e-9, "exactly the quoted price");
        assertEquals(1, ports.deliverCalls, "exactly one mint");
        assertEquals(5_000.0, ports.balance, 1e-9);
        assertEquals(RevivePurchaseJournal.Phase.COMPLETED, onlyPhase(journal));
        assertNotNull(bought.transactionId(), "the purchase carries its tx id");
        // a third call inside the success cooldown cannot debit again
        var third = c.confirm(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.COOLDOWN, third.status());
        assertEquals(1, ports.withdrawCalls);
    }

    @Test
    void buyExpiredOrPriceMismatchedTokenRearmsWithoutDebit() throws Exception {
        FakePorts ports = new FakePorts();
        ports.balance = 20_000.0;
        var clock = new java.util.concurrent.atomic.AtomicLong(T0);
        RevivePurchaseCoordinator c = coordinator(ports, tempJournal(), clock);
        UUID buyer = UUID.randomUUID();
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c.quote(buyer, RP, RP_PRICE).status());
        clock.addAndGet(20_000L); // the 20 s window lapses
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c.confirm(buyer, RP, RP_PRICE).status(),
                "an expired token re-arms instead of debiting");
        assertEquals(0, ports.withdrawCalls);
        // price moved since the quote: confirm at the new price re-arms
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c.confirm(buyer, RP, 6_000.0).status(),
                "a price-mismatched token re-arms at the new price, never debits stale");
        assertEquals(0, ports.withdrawCalls);
        // and the re-armed token is the one that can debit, at 6,000
        var bought = c.confirm(buyer, RP, 6_000.0);
        assertEquals(RevivePurchaseCoordinator.Status.PURCHASED, bought.status());
        assertEquals(1, ports.withdrawCalls);
        assertEquals(6_000.0, ports.lastWithdrawAmount, 1e-9);
    }

    // Group 3: prechecks refuse with no charge.
    @Test
    void buyPrechecksRefuseWithoutCharge() throws Exception {
        UUID buyer = UUID.randomUUID();
        // insufficient funds
        FakePorts poor = new FakePorts();
        poor.balance = 4_999.0;
        RevivePurchaseJournal poorJournal = tempJournal();
        RevivePurchaseCoordinator c1 = coordinator(poor, poorJournal,
                new java.util.concurrent.atomic.AtomicLong(T0));
        assertEquals(RevivePurchaseCoordinator.Status.INSUFFICIENT_FUNDS,
                c1.quote(buyer, RP, RP_PRICE).status());
        assertEquals(RevivePurchaseCoordinator.Status.INSUFFICIENT_FUNDS,
                c1.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(0, poor.withdrawCalls, "no debit attempted");
        assertEquals(0, poor.deliverCalls, "no mint");
        assertTrue(poorJournal.entries().isEmpty(), "no journal transaction opened");
        // no empty slot (buy-require-empty-slot shape at the port)
        FakePorts full = new FakePorts();
        full.eligibility = RevivePurchaseCoordinator.Eligibility.NO_SPACE;
        RevivePurchaseCoordinator c2 = coordinator(full, tempJournal(),
                new java.util.concurrent.atomic.AtomicLong(T0));
        var ineligible = c2.quote(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.INELIGIBLE, ineligible.status());
        assertEquals("NO_SPACE", ineligible.detail());
        assertEquals(0, full.withdrawCalls);
        // feature disabled / economy gone at the port: UNAVAILABLE eligibility
        FakePorts off = new FakePorts();
        off.eligibility = RevivePurchaseCoordinator.Eligibility.UNAVAILABLE;
        RevivePurchaseCoordinator c3 = coordinator(off, tempJournal(),
                new java.util.concurrent.atomic.AtomicLong(T0));
        assertEquals(RevivePurchaseCoordinator.Status.INELIGIBLE, c3.quote(buyer, RP, RP_PRICE).status());
        assertEquals(0, off.withdrawCalls);
        // journal unhealthy: UNAVAILABLE, nothing attempted
        FlakyJournal sick = new FlakyJournal(
                Files.createTempDirectory("revive-buy-sick").resolve("purchases.journal"));
        sick.forceUnhealthy = true;
        FakePorts ports4 = new FakePorts();
        RevivePurchaseCoordinator c4 = coordinator(ports4, sick,
                new java.util.concurrent.atomic.AtomicLong(T0));
        assertEquals(RevivePurchaseCoordinator.Status.UNAVAILABLE, c4.quote(buyer, RP, RP_PRICE).status());
        assertEquals(0, ports4.withdrawCalls);
        // invalid prices: 0, negative, NaN — never a charge
        FakePorts ports5 = new FakePorts();
        RevivePurchaseCoordinator c5 = coordinator(ports5, tempJournal(),
                new java.util.concurrent.atomic.AtomicLong(T0));
        assertEquals(RevivePurchaseCoordinator.Status.INVALID_PRICE, c5.quote(buyer, RP, 0).status());
        assertEquals(RevivePurchaseCoordinator.Status.INVALID_PRICE, c5.quote(buyer, RP, -5).status());
        assertEquals(RevivePurchaseCoordinator.Status.INVALID_PRICE, c5.quote(buyer, RP, Double.NaN).status());
        assertEquals(0, ports5.withdrawCalls);
    }

    // Group 4: strict money classification.
    @Test
    void buyMoneyClassificationStrict() throws Exception {
        UUID buyer = UUID.randomUUID();
        // definitive decline: DECLINED, delivery never runs, no lock
        FakePorts decline = new FakePorts();
        decline.withdrawMode = FakePorts.WithdrawMode.DECLINE;
        RevivePurchaseJournal declineJournal = tempJournal();
        RevivePurchaseCoordinator c1 = coordinator(decline, declineJournal,
                new java.util.concurrent.atomic.AtomicLong(T0));
        c1.quote(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.DECLINED, c1.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(0, decline.deliverCalls);
        assertEquals(0, decline.depositCalls);
        assertFalse(c1.isLocked(buyer), "a clean decline never locks");
        assertEquals(RevivePurchaseJournal.Phase.DECLINED, onlyPhase(declineJournal));
        // every ambiguity: LOCKED, delivery never runs, deposit never runs
        for (FakePorts.WithdrawMode mode : new FakePorts.WithdrawMode[]{
                FakePorts.WithdrawMode.NULL_RESPONSE, FakePorts.WithdrawMode.AMOUNT_MISMATCH,
                FakePorts.WithdrawMode.BALANCE_MISMATCH, FakePorts.WithdrawMode.THROW}) {
            FakePorts ports = new FakePorts();
            ports.withdrawMode = mode;
            RevivePurchaseCoordinator c = coordinator(ports, tempJournal(),
                    new java.util.concurrent.atomic.AtomicLong(T0));
            c.quote(buyer, RP, RP_PRICE);
            assertEquals(RevivePurchaseCoordinator.Status.LOCKED, c.confirm(buyer, RP, RP_PRICE).status(),
                    mode + " is ambiguous and must lock");
            assertEquals(0, ports.deliverCalls, mode + ": no mint on ambiguity");
            assertEquals(0, ports.depositCalls, mode + ": no refund on ambiguity");
            assertTrue(c.isLocked(buyer), mode + ": the buyer stays locked");
        }
    }

    // Group 5: delivery outcomes and the refund path.
    @Test
    void buyDeliveryOutcomesAndRefund() throws Exception {
        UUID buyer = UUID.randomUUID();
        // FAILED_ROLLED_BACK -> exactly one refund of the exact price -> REFUNDED
        FakePorts rolled = new FakePorts();
        rolled.deliverOutcome = RevivePurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        RevivePurchaseJournal rolledJournal = tempJournal();
        RevivePurchaseCoordinator c1 = coordinator(rolled, rolledJournal,
                new java.util.concurrent.atomic.AtomicLong(T0));
        c1.quote(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.REFUNDED, c1.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(1, rolled.depositCalls, "exactly one refund attempt");
        assertEquals(RP_PRICE, rolled.lastDepositAmount, 1e-9, "the refund is the exact price");
        assertEquals(10_000.0, rolled.balance, 1e-9, "the balance is restored");
        assertEquals(RevivePurchaseJournal.Phase.REFUNDED, onlyPhase(rolledJournal));
        assertFalse(c1.isLocked(buyer), "a verified refund does not lock");
        // refund ambiguity -> LOCKED with exactly one deposit attempt total
        FakePorts badRefund = new FakePorts();
        badRefund.deliverOutcome = RevivePurchaseCoordinator.DeliveryOutcome.FAILED_ROLLED_BACK;
        badRefund.depositMode = FakePorts.DepositMode.MISMATCH;
        RevivePurchaseCoordinator c2 = coordinator(badRefund, tempJournal(),
                new java.util.concurrent.atomic.AtomicLong(T0));
        c2.quote(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.LOCKED, c2.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(1, badRefund.depositCalls, "one deposit attempt, never a second");
        assertTrue(c2.isLocked(buyer));
        // delivery UNKNOWN -> LOCKED, refund withheld (zero deposit attempts)
        FakePorts unknown = new FakePorts();
        unknown.deliverOutcome = RevivePurchaseCoordinator.DeliveryOutcome.UNKNOWN;
        RevivePurchaseCoordinator c3 = coordinator(unknown, tempJournal(),
                new java.util.concurrent.atomic.AtomicLong(T0));
        c3.quote(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.LOCKED, c3.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(0, unknown.depositCalls,
                "refunding an item that may exist would print a free potion");
        assertTrue(c3.isLocked(buyer));
    }

    // Group 6: journal discipline and boot locks.
    @Test
    void buyJournalFailureAndBootLocks() throws Exception {
        UUID buyer = UUID.randomUUID();
        UUID other = UUID.randomUUID();
        // write failure BEFORE withdraw: no withdraw attempted + locked
        FlakyJournal flaky = new FlakyJournal(
                Files.createTempDirectory("revive-buy-flaky").resolve("purchases.journal"));
        FakePorts ports = new FakePorts();
        RevivePurchaseCoordinator c1 = coordinator(ports, flaky,
                new java.util.concurrent.atomic.AtomicLong(T0));
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c1.quote(buyer, RP, RP_PRICE).status());
        flaky.failAppends = true;
        assertEquals(RevivePurchaseCoordinator.Status.UNAVAILABLE,
                c1.confirm(buyer, RP, RP_PRICE).status());
        assertEquals(0, ports.withdrawCalls, "the journal leads: no write, no withdrawal");
        assertTrue(c1.isLocked(buyer), "a pre-withdraw journal failure locks the buyer");
        // boot with a non-terminal entry: that player locked, others unaffected
        Path file = Files.createTempDirectory("revive-buy-boot").resolve("purchases.journal");
        RevivePurchaseJournal boot1 = new RevivePurchaseJournal(file);
        boot1.append(new RevivePurchaseJournal.Entry(UUID.randomUUID(), buyer,
                RP_PRICE, RevivePurchaseJournal.Phase.WITHDRAW_INTENT, T0));
        RevivePurchaseCoordinator c2 = coordinator(new FakePorts(),
                new RevivePurchaseJournal(file), new java.util.concurrent.atomic.AtomicLong(T0));
        assertTrue(c2.isLocked(buyer), "an unresolved entry locks its buyer at boot");
        assertFalse(c2.isLocked(other), "other players are unaffected");
        assertEquals(RevivePurchaseCoordinator.Status.LOCKED, c2.quote(buyer, RP, RP_PRICE).status());
        // terminal entries do not lock
        Path file2 = Files.createTempDirectory("revive-buy-term").resolve("purchases.journal");
        RevivePurchaseJournal term = new RevivePurchaseJournal(file2);
        UUID tx = UUID.randomUUID();
        term.append(new RevivePurchaseJournal.Entry(tx, buyer, RP_PRICE,
                RevivePurchaseJournal.Phase.WITHDRAW_INTENT, T0));
        term.append(new RevivePurchaseJournal.Entry(tx, buyer, RP_PRICE,
                RevivePurchaseJournal.Phase.WITHDRAWN, T0));
        term.append(new RevivePurchaseJournal.Entry(tx, buyer, RP_PRICE,
                RevivePurchaseJournal.Phase.COMPLETED, T0));
        RevivePurchaseCoordinator c3 = coordinator(new FakePorts(),
                new RevivePurchaseJournal(file2), new java.util.concurrent.atomic.AtomicLong(T0));
        assertFalse(c3.isLocked(buyer), "a COMPLETED transaction never locks");
    }

    // Group 6b (F1, run 510): the journal's directory sync is best-effort.
    // Windows refuses directory channels (AccessDeniedException on open),
    // which on the unconditional directory force disabled purchases on the
    // first boot after journal creation (mirror rig W5, Boot A WARN).
    // Downed's build excludes Mockito, so the refusal is injected through
    // the journal's DirectoryChannelOpener seam instead of Shops'
    // MockedStatic<FileChannel> approach (DirectorySyncBestEffortTest).
    @Test
    void journalDirectorySyncBestEffort() throws Exception {
        Path dir = Files.createTempDirectory("revive-buy-dir-sync");
        // a platform refusal (either refusal type) must not fail the sync
        assertDoesNotThrow(() -> RevivePurchaseJournal.syncDirectoryBestEffort(dir,
                d -> { throw new java.nio.file.AccessDeniedException(d.toString()); }));
        assertDoesNotThrow(() -> RevivePurchaseJournal.syncDirectoryBestEffort(dir,
                d -> { throw new UnsupportedOperationException("no directory channels"); }));
        // any other IOException still propagates — only refusals are best-effort
        assertThrows(java.io.IOException.class, () -> RevivePurchaseJournal.syncDirectoryBestEffort(dir,
                d -> { throw new java.io.IOException("some other failure"); }));
        // the real opener syncs a real directory without error
        assertDoesNotThrow(() -> RevivePurchaseJournal.syncDirectoryBestEffort(dir));
        // end to end: a fresh journal in a fresh directory is created healthy
        // and usable (the creation path that carries the directory sync)
        Path file = dir.resolve("nested").resolve("purchases.journal");
        RevivePurchaseJournal fresh = new RevivePurchaseJournal(file);
        assertTrue(Files.isRegularFile(file), "journal file created");
        assertTrue(fresh.healthy(), "fresh journal is healthy");
        assertTrue(fresh.unresolved().isEmpty(), "fresh journal has no unresolved entries");
    }

    // Group 7: busy guard and success cooldown.
    @Test
    void buyBusyGuardAndSuccessCooldown() throws Exception {
        UUID buyer = UUID.randomUUID();
        // busy: a confirm re-entered from inside the withdraw sees BUSY;
        // exactly one debit lands in total
        FakePorts ports = new FakePorts();
        var clock = new java.util.concurrent.atomic.AtomicLong(T0);
        RevivePurchaseCoordinator c = coordinator(ports, tempJournal(), clock);
        var inner = new RevivePurchaseCoordinator.Result[1];
        ports.onWithdraw = () -> inner[0] = c.confirm(buyer, RP, RP_PRICE);
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c.quote(buyer, RP, RP_PRICE).status());
        assertEquals(RevivePurchaseCoordinator.Status.PURCHASED, c.confirm(buyer, RP, RP_PRICE).status());
        assertNotNull(inner[0]);
        assertEquals(RevivePurchaseCoordinator.Status.BUSY, inner[0].status(),
                "the concurrent confirm is BUSY, never a second debit");
        assertEquals(1, ports.withdrawCalls, "one debit total");
        // cooldown: an immediate second purchase is refused, then allowed
        assertEquals(RevivePurchaseCoordinator.Status.COOLDOWN, c.quote(buyer, RP, RP_PRICE).status(),
                "inside buy-success-cooldown-seconds the next buy cannot debit");
        assertEquals(1, ports.withdrawCalls);
        clock.addAndGet(1_001L);
        assertEquals(RevivePurchaseCoordinator.Status.ARMED, c.quote(buyer, RP, RP_PRICE).status(),
                "after the cooldown the buyer is purchasable again");
        assertEquals(RevivePurchaseCoordinator.Status.PURCHASED, c.confirm(buyer, RP, RP_PRICE).status(),
                "a second full purchase succeeds — qty 1 per transaction, no other cap");
        assertEquals(2, ports.withdrawCalls);
    }

    // Group 8: config + plugin pins for the charge leg.
    @Test
    void buyConfigAndPluginPins() throws Exception {
        String cfg = Files.readString(Path.of("config.yml"));
        assertTrue(cfg.contains("price: 5000"), "the price stays the config source of truth");
        assertTrue(cfg.contains("SOURCE OF TRUTH for the shop charge"),
                "the corrected price comment ships (W6 section 5.1)");
        assertFalse(cfg.contains("Downed takes no money"),
                "the superseded 'takes no money' comment is gone — it re-created this confusion");
        assertTrue(cfg.contains("buy-confirm-window-seconds: 20"), "confirm window 20 s (precedent)");
        assertTrue(cfg.contains("buy-success-cooldown-seconds: 1"), "success cooldown 1 s (precedent floor)");
        assertTrue(cfg.contains("buy-require-empty-slot: true"),
                "paid delivery refuses without an empty slot");
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("softdepend: [ExcelTogether, Geyser-Spigot, floodgate, EliteMobs, Vault]"),
                "Vault is a SOFT depend — the revive mechanic must not die with the economy");
        assertTrue(yml.contains("version: 1.0.0"),
                "D110: the charge leg amends 1.0.0 pre-install — the version stays 1.0.0");
        assertTrue(yml.contains("sold at the Excel Shop for $5,000 (launch)"),
                "the description names the real shop sale");
    }
}
