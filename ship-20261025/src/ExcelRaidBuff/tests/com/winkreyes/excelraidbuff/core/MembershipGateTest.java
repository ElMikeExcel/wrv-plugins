package com.winkreyes.excelraidbuff.core;

import static com.winkreyes.excelraidbuff.core.MembershipGate.BuffDecision;
import static com.winkreyes.excelraidbuff.core.MembershipGate.StripDecision;

/**
 * The membership gate: the raid object's own claim is the ONLY
 * criterion. A buff goes on a live member of a running raid, once;
 * a strip comes off the moment the marked raid ends or the game
 * stops claiming the raider for it — so a survivor of a finished
 * raid can never stay buffed, and a patrol (never marked, never a
 * member) is never in either decision's way.
 */
public final class MembershipGateTest {

    private static int passed;
    private static int failed;

    private static void check(boolean cond, String label) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + label);
        }
    }

    public static void main(String[] args) {
        // Buff decisions.
        check(MembershipGate.decideBuff(true, true, false) == BuffDecision.BUFF,
                "member of a running raid, unbuffed: BUFF");
        check(MembershipGate.decideBuff(false, true, false) == BuffDecision.SKIP,
                "not a raid member (patrol/outpost/mansion): SKIP");
        check(MembershipGate.decideBuff(true, false, false) == BuffDecision.SKIP,
                "member of a finished raid: SKIP");
        check(MembershipGate.decideBuff(true, true, true) == BuffDecision.SKIP,
                "already buffed: SKIP (repair is the sweep's separate path)");
        check(MembershipGate.decideBuff(false, false, false) == BuffDecision.SKIP,
                "non-member of a dead raid: SKIP");
        check(MembershipGate.decideBuff(false, false, true) == BuffDecision.SKIP,
                "marked non-member: not a buff decision (the strip gate owns it)");

        // Strip decisions.
        check(MembershipGate.decideStrip(false, 7, 7, true) == StripDecision.KEEP,
                "unmarked entity: KEEP (nothing of ours on it)");
        check(MembershipGate.decideStrip(true, 7, 7, true) == StripDecision.KEEP,
                "marked, still in its live raid: KEEP");
        check(MembershipGate.decideStrip(true, 7, 7, false) == StripDecision.STRIP,
                "marked, its raid ended: STRIP");
        check(MembershipGate.decideStrip(true, -1, 7, true) == StripDecision.STRIP,
                "marked, game assigns it no raid (left / distance-removed): STRIP");
        check(MembershipGate.decideStrip(true, -1, 7, false) == StripDecision.STRIP,
                "marked, no raid and raid over: STRIP");
        check(MembershipGate.decideStrip(true, 9, 7, true) == StripDecision.STRIP,
                "marked, now claimed by a DIFFERENT raid: STRIP (the new raid re-buffs on its own terms)");
        check(MembershipGate.decideStrip(true, 9, 7, false) == StripDecision.STRIP,
                "marked, different raid and marked raid over: STRIP");

        System.out.println("MembershipGateTest: " + passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }
}
