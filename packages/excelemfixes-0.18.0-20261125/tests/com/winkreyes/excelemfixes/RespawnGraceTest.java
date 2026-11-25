import excelemfixes.respawngrace.GraceLedger;
import excelemfixes.respawngrace.RespawnGracePolicy;

import java.util.List;
import java.util.UUID;

/**
 * Respawn-grace fuse model (D195 item 3, revised Nov 22, 2026):
 * protection after a death-return holds until the loot fuse
 * burns (first own-loot pickup starts a countdown; expiry ends
 * it), an attack ends it at any point, or the pre-loot cap runs
 * out. The death-linked half: no death, no grace; one ticket,
 * one grant.
 */
public final class RespawnGraceTest {

    private static final UUID A = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID B = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID DEATH1 = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID DEATH2 = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final long T0 = 1_000_000L;
    private static final long WINDOW = 600_000L;    // 600 s return window
    private static final long CAP = 60_000L;        // 60 s pre-loot cap
    private static final long COUNT = 10_000L;      // 10 s loot countdown

    public static void run() {
        System.out.println("RespawnGraceTest");
        ticketLifecycle();
        grantAndProtection();
        lootCountdown();
        attackEndsGrace();
        antiFarming();
        cleanup();
        policy();
    }

    private static void ticketLifecycle() {
        GraceLedger ledger = new GraceLedger();
        Check.ok(!ledger.hasTicket(A, T0), "no ticket before any death");
        Check.ok(!ledger.grantFromTicket(A, T0, CAP), "grant without a death fails (no ticket)");
        Check.ok(!ledger.isProtected(A, T0), "failed grant leaves the player unprotected");
        Check.ok(ledger.deathId(A) == null, "no death id before any death");

        ledger.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(ledger.hasTicket(A, T0), "death in scope arms the ticket");
        Check.ok(DEATH1.equals(ledger.deathId(A)), "ticket carries the death instance id");
        Check.ok(ledger.hasTicket(A, T0 + WINDOW - 1), "ticket live 1 ms before its window ends");
        Check.ok(!ledger.hasTicket(A, T0 + WINDOW), "ticket lapsed at exactly the window end (half-open)");
        Check.ok(ledger.deathId(A) == null, "lapsed ticket drops its death id");

        ledger.armTicket(A, DEATH1, T0, WINDOW);
        ledger.armTicket(A, DEATH2, T0 + 5_000, WINDOW);
        Check.ok(DEATH2.equals(ledger.deathId(A)), "a second death re-arms with the new death id");
        Check.ok(ledger.hasTicket(A, T0 + WINDOW + 4_999),
                "re-armed ticket runs from the second death, not the first");
        Check.ok(!ledger.hasTicket(A, T0 + WINDOW + 5_000), "re-armed ticket lapses on its own clock");
    }

    private static void grantAndProtection() {
        GraceLedger ledger = new GraceLedger();
        ledger.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(ledger.grantFromTicket(A, T0 + 1_000, CAP), "return with a live ticket grants");
        Check.eq(GraceLedger.Phase.PRELOOT, ledger.phase(A, T0 + 1_000),
                "grant enters the PRELOOT phase");
        Check.ok(ledger.isProtected(A, T0 + 1_000), "protected right after the grant");
        Check.ok(ledger.damageBlocked(A, T0 + 30_000), "damage blocked mid-preloot");
        Check.ok(ledger.isProtected(A, T0 + 1_000 + CAP - 1), "protected 1 ms before the cap");
        Check.ok(!ledger.isProtected(A, T0 + 1_000 + CAP), "cap ends protection at exactly cap (half-open)");
        Check.eq(CAP - 30_000, ledger.millisRemaining(A, T0 + 31_000),
                "remaining time tracks the cap deadline");
        Check.ok(!ledger.damageBlocked(B, T0 + 1_000), "another player is never protected by A's grace");
    }

    private static void lootCountdown() {
        GraceLedger ledger = new GraceLedger();
        Check.ok(!ledger.startCountdown(A, T0, COUNT), "countdown cannot start with no grace at all");

        ledger.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(!ledger.startCountdown(A, T0, COUNT),
                "countdown cannot start from a ticket (no grant yet)");
        ledger.grantFromTicket(A, T0, CAP);

        Check.ok(ledger.startCountdown(A, T0 + 40_000, COUNT),
                "first own-loot pickup starts the countdown");
        Check.eq(GraceLedger.Phase.COUNTDOWN, ledger.phase(A, T0 + 40_000),
                "pickup moves PRELOOT to COUNTDOWN");
        Check.ok(ledger.isProtected(A, T0 + 40_000), "still protected while the countdown runs");
        Check.ok(ledger.damageBlocked(A, T0 + 49_999), "damage blocked 1 ms before the countdown ends");
        Check.ok(!ledger.isProtected(A, T0 + 50_000), "countdown zero ends protection (half-open)");
        Check.eq(4_000L, ledger.millisRemaining(A, T0 + 46_000),
                "remaining time tracks the countdown deadline");

        // The countdown replaces the cap: starting it late in the
        // pre-loot window still buys the full countdown.
        GraceLedger late = new GraceLedger();
        late.armTicket(A, DEATH1, T0, WINDOW);
        late.grantFromTicket(A, T0, CAP);
        Check.ok(late.startCountdown(A, T0 + CAP - 1_000, COUNT),
                "pickup 1 s before the cap still starts the countdown");
        Check.ok(late.isProtected(A, T0 + CAP + 5_000),
                "countdown runs past the old cap deadline — the countdown is the fuse");
        Check.ok(!late.isProtected(A, T0 + CAP - 1_000 + COUNT),
                "and ends on the countdown's own clock");

        // Further pickups never restart it.
        GraceLedger again = new GraceLedger();
        again.armTicket(A, DEATH1, T0, WINDOW);
        again.grantFromTicket(A, T0, CAP);
        again.startCountdown(A, T0 + 10_000, COUNT);
        Check.ok(!again.startCountdown(A, T0 + 15_000, COUNT),
                "a second own-loot pickup does NOT restart the countdown");
        Check.ok(!again.isProtected(A, T0 + 20_000),
                "protection still ends on the FIRST pickup's clock");
        Check.ok(!again.startCountdown(A, T0 + 21_000, COUNT),
                "a pickup after protection ended starts nothing");
    }

    private static void attackEndsGrace() {
        GraceLedger preloot = new GraceLedger();
        preloot.armTicket(A, DEATH1, T0, WINDOW);
        preloot.grantFromTicket(A, T0, CAP);
        Check.eq(GraceLedger.Phase.PRELOOT, preloot.onAttack(A, T0 + 5_000),
                "attack during PRELOOT ends it and reports PRELOOT");
        Check.ok(!preloot.isProtected(A, T0 + 5_000), "damage flows right after the attack");
        Check.eq(GraceLedger.Phase.NONE, preloot.onAttack(A, T0 + 6_000),
                "a second attack ends nothing");

        GraceLedger countdown = new GraceLedger();
        countdown.armTicket(A, DEATH1, T0, WINDOW);
        countdown.grantFromTicket(A, T0, CAP);
        countdown.startCountdown(A, T0 + 10_000, COUNT);
        Check.eq(GraceLedger.Phase.COUNTDOWN, countdown.onAttack(A, T0 + 12_000),
                "attack during COUNTDOWN ends it and reports COUNTDOWN");
        Check.ok(!countdown.isProtected(A, T0 + 12_000),
                "the countdown is cancelled, not paused");

        GraceLedger ticketOnly = new GraceLedger();
        ticketOnly.armTicket(A, DEATH1, T0, WINDOW);
        Check.eq(GraceLedger.Phase.NONE, ticketOnly.onAttack(A, T0 + 1_000),
                "attacking while holding only a ticket ends nothing");
        Check.ok(ticketOnly.hasTicket(A, T0 + 1_000), "and the ticket survives it");

        GraceLedger expired = new GraceLedger();
        expired.armTicket(A, DEATH1, T0, WINDOW);
        expired.grantFromTicket(A, T0, CAP);
        Check.eq(GraceLedger.Phase.NONE, expired.onAttack(A, T0 + CAP + 1),
                "an attack after the cap ends nothing (already over)");
    }

    private static void antiFarming() {
        GraceLedger ledger = new GraceLedger();
        ledger.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(ledger.grantFromTicket(A, T0, CAP), "first return grants");
        Check.ok(!ledger.grantFromTicket(A, T0 + 1_000, CAP),
                "the same ticket cannot grant twice (one death, one grant)");
        Check.ok(ledger.endGrace(A) == GraceLedger.Phase.PRELOOT,
                "endGrace reports the phase it ended");
        Check.ok(!ledger.grantFromTicket(A, T0 + 2_000, CAP),
                "ending a grace does not resurrect its ticket");

        GraceLedger stale = new GraceLedger();
        stale.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(!stale.grantFromTicket(A, T0 + WINDOW, CAP),
                "a lapsed ticket grants nothing");

        GraceLedger cleared = new GraceLedger();
        cleared.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(cleared.clearTicket(A), "clearTicket drops a held ticket");
        Check.ok(!cleared.clearTicket(A), "clearTicket on nothing reports false");
        Check.ok(!cleared.grantFromTicket(A, T0, CAP), "a cleared ticket grants nothing");

        GraceLedger scope = new GraceLedger();
        scope.armTicket(A, DEATH1, T0, WINDOW);
        scope.grantFromTicket(A, T0, CAP);
        Check.ok(scope.clearTicket(A) == false,
                "clearTicket never touches an active grace");
        Check.ok(scope.isProtected(A, T0), "grace survives a clearTicket call");
    }

    private static void cleanup() {
        GraceLedger ledger = new GraceLedger();
        ledger.armTicket(A, DEATH1, T0, WINDOW);
        ledger.grantFromTicket(A, T0, CAP);
        ledger.armTicket(B, DEATH2, T0, WINDOW);
        List<GraceLedger.Expiry> first = ledger.purgeExpired(T0 + CAP);
        Check.eq(1, first.size(), "sweep reports exactly the ended protection");
        Check.eq(GraceLedger.EndKind.CAP_EXPIRED, first.get(0).kind(),
                "pre-loot deadline reports as CAP_EXPIRED (plain end)");
        Check.eq(A, first.get(0).playerId(), "expiry names the player");
        Check.ok(ledger.purgeExpired(T0 + CAP).isEmpty(),
                "an expiry is reported exactly once");
        Check.ok(ledger.hasTicket(B, T0 + CAP), "the other player's ticket is untouched by the sweep");

        GraceLedger cd = new GraceLedger();
        cd.armTicket(A, DEATH1, T0, WINDOW);
        cd.grantFromTicket(A, T0, CAP);
        cd.startCountdown(A, T0 + 10_000, COUNT);
        List<GraceLedger.Expiry> cdFirst = cd.purgeExpired(T0 + 20_000);
        Check.eq(1, cdFirst.size(), "sweep reports the finished countdown");
        Check.eq(GraceLedger.EndKind.COUNTDOWN_EXPIRED, cdFirst.get(0).kind(),
                "countdown deadline reports as COUNTDOWN_EXPIRED (flourish end)");

        GraceLedger quiet = new GraceLedger();
        quiet.armTicket(A, DEATH1, T0, WINDOW);
        Check.ok(quiet.purgeExpired(T0 + WINDOW).isEmpty(),
                "a lapsed ticket lapses silently (no expiry report)");
        Check.eq(0, quiet.ticketCount(), "lapsed ticket removed by the sweep");

        GraceLedger counts = new GraceLedger();
        counts.armTicket(A, DEATH1, T0, WINDOW);
        counts.armTicket(B, DEATH2, T0, WINDOW);
        Check.eq(2, counts.ticketCount(), "ticketCount counts held tickets");
        counts.grantFromTicket(A, T0, CAP);
        Check.eq(1, counts.ticketCount(), "grant consumes the ticket");
        Check.eq(1, counts.graceCount(), "graceCount counts active protections");
        counts.clear(A);
        Check.eq(0, counts.graceCount(), "clear (quit) drops the grace");
        Check.ok(!counts.isProtected(A, T0), "cleared player is unprotected");
    }

    private static void policy() {
        Check.ok(RespawnGracePolicy.inScope(true, false), "scope: registry world is in scope");
        Check.ok(RespawnGracePolicy.inScope(false, true), "scope: listed world is in scope");
        Check.ok(!RespawnGracePolicy.inScope(false, false), "scope: neither signal, out of scope");

        Check.ok(RespawnGracePolicy.deathArmsTicket(true), "death in scope arms the ticket");
        Check.ok(!RespawnGracePolicy.deathArmsTicket(false), "death out of scope arms nothing");

        Check.ok(RespawnGracePolicy.grantOnRespawn(true), "respawn landing in scope may grant");
        Check.ok(!RespawnGracePolicy.grantOnRespawn(false), "respawn out of scope never grants");

        Check.ok(RespawnGracePolicy.grantOnWorldChange(false, true),
                "crossing INTO scope may grant");
        Check.ok(!RespawnGracePolicy.grantOnWorldChange(true, true),
                "moving between EM worlds does not grant");
        Check.ok(!RespawnGracePolicy.grantOnWorldChange(false, false),
                "moving outside scope does not grant");

        Check.ok(RespawnGracePolicy.endOnWorldChange(true, false),
                "leaving scope ends protection");
        Check.ok(!RespawnGracePolicy.endOnWorldChange(true, true),
                "moving within scope keeps protection");
        Check.ok(!RespawnGracePolicy.endOnWorldChange(false, true),
                "entering scope is not an end");

        Check.ok(RespawnGracePolicy.damageBlocked(true, true),
                "damage blocked: protected and in scope");
        Check.ok(!RespawnGracePolicy.damageBlocked(true, false),
                "damage NOT blocked out of scope even if the ledger says protected");
        Check.ok(!RespawnGracePolicy.damageBlocked(false, true),
                "damage NOT blocked when unprotected");

        Check.ok(RespawnGracePolicy.isOwnDeathLoot(A, DEATH1, A, DEATH1),
                "own loot: owner and death id both match");
        Check.ok(!RespawnGracePolicy.isOwnDeathLoot(B, DEATH1, A, DEATH1),
                "another player's drops are not own loot");
        Check.ok(!RespawnGracePolicy.isOwnDeathLoot(A, DEATH2, A, DEATH1),
                "loot from an EARLIER death is not this death's loot");
        Check.ok(!RespawnGracePolicy.isOwnDeathLoot(A, DEATH1, A, DEATH2),
                "loot from a LATER death does not belong to the running grace");
        Check.ok(!RespawnGracePolicy.isOwnDeathLoot(null, null, A, DEATH1),
                "untagged ground loot is never own loot");
        Check.ok(!RespawnGracePolicy.isOwnDeathLoot(A, DEATH1, A, null),
                "no running grace, no own loot");

        Check.eq(60, RespawnGracePolicy.clampMaxSeconds(60), "cap: shipped default passes through");
        Check.eq(1, RespawnGracePolicy.clampMaxSeconds(-5), "cap: non-positive clamps to minimum");
        Check.eq(60, RespawnGracePolicy.clampMaxSeconds(0), "cap: zero falls back to the default");
        Check.eq(600, RespawnGracePolicy.clampMaxSeconds(99999), "cap: huge clamps to maximum");
        Check.eq(600, RespawnGracePolicy.clampReturnWindowSeconds(600),
                "window: shipped default passes through");
        Check.eq(30, RespawnGracePolicy.clampReturnWindowSeconds(5),
                "window: below minimum clamps up");
        Check.eq(3600, RespawnGracePolicy.clampReturnWindowSeconds(99999),
                "window: huge clamps to maximum");
        Check.eq(10, RespawnGracePolicy.clampCountdownSeconds(10),
                "countdown: shipped default (Mike's 10 s) passes through");
        Check.eq(10, RespawnGracePolicy.clampCountdownSeconds(0),
                "countdown: zero falls back to the default");
        Check.eq(1, RespawnGracePolicy.clampCountdownSeconds(-3),
                "countdown: negative clamps to minimum");
        Check.eq(60, RespawnGracePolicy.clampCountdownSeconds(999),
                "countdown: huge clamps to maximum");
    }
}
