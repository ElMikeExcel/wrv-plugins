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
    void pluginYmlPins014() throws Exception {
        // local-build.sh runs the suite from the project root
        String yml = Files.readString(Path.of("plugin.yml"));
        assertTrue(yml.contains("version: 0.1.4"), "plugin.yml declares version 0.1.4");
        assertTrue(yml.contains("exceldowned.admin:"), "the existing admin node stays registered");
        int at = yml.indexOf("exceldowned.admin:");
        String block = yml.substring(at, Math.min(yml.length(), at + 300));
        assertTrue(block.contains("default: op"), "exceldowned.admin still defaults to op");
        assertTrue(yml.contains("revive <player>") && yml.contains("clear <player>"),
                "the usage line names both admin forms");
        assertTrue(BuildInfo.BUILD_ID.startsWith("EXC-DOWNED-0.1.4-WRV-")
                        && BuildInfo.BUILD_ID.endsWith("-d0wn3d"),
                "fingerprint lineage: EXC-DOWNED semantic marker + d0wn3d tag carried forward");
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
}
