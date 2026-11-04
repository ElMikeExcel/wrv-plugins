package com.winkreyes.exceltogether;

import java.util.UUID;

/**
 * DESIGN §11B case B12: every PartyOperationResult maps per §1.4 (asserted on the name->EmMirror
 * table in ReflectEmLink, including the idempotent-OK trio and the conservative fallbacks for
 * combinations EM never returns), plus the driven-invite ledger's 60-second liveness edges.
 * Run: java -cp <classes> com.winkreyes.exceltogether.EmLinkMappingTest
 */
public class EmLinkMappingTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static void eq(String n, EmLink.EmMirror want, EmLink.EmMirror got) { check(n + " (want " + want + ", got " + got + ")", want == got); }

    public static void main(String[] args) {
        ReflectEmLink.Op C = ReflectEmLink.Op.CREATE, I = ReflectEmLink.Op.INVITE, A = ReflectEmLink.Op.ACCEPT, L = ReflectEmLink.Op.LEAVE;
        // the universal rows
        for (ReflectEmLink.Op op : ReflectEmLink.Op.values()) {
            eq("SUCCESS/" + op, EmLink.EmMirror.OK, ReflectEmLink.map("SUCCESS", op));
            eq("PARTY_FULL/" + op, EmLink.EmMirror.FULL, ReflectEmLink.map("PARTY_FULL", op));
            eq("TARGET_ALREADY_IN_PARTY/" + op, EmLink.EmMirror.TARGET_IN_OTHER_PARTY, ReflectEmLink.map("TARGET_ALREADY_IN_PARTY", op));
            eq("DISABLED/" + op, EmLink.EmMirror.DISABLED, ReflectEmLink.map("DISABLED", op));
            eq("NO_PERMISSION/" + op, EmLink.EmMirror.NO_PERMISSION, ReflectEmLink.map("NO_PERMISSION", op));
            eq("TARGET_NO_PERMISSION/" + op, EmLink.EmMirror.TARGET_NO_PERMISSION, ReflectEmLink.map("TARGET_NO_PERMISSION", op));
            eq("PLAYER_UNAVAILABLE/" + op, EmLink.EmMirror.NOT_READY, ReflectEmLink.map("PLAYER_UNAVAILABLE", op));
            eq("NO_PENDING_INVITE/" + op, EmLink.EmMirror.NOT_READY, ReflectEmLink.map("NO_PENDING_INVITE", op));
            eq("INVITE_EXPIRED/" + op, EmLink.EmMirror.NOT_READY, ReflectEmLink.map("INVITE_EXPIRED", op));
            eq("SELF_INVITE/" + op, EmLink.EmMirror.ERROR, ReflectEmLink.map("SELF_INVITE", op));
            eq("unknown result/" + op, EmLink.EmMirror.ERROR, ReflectEmLink.map("SOMETHING_NEW", op));
            eq("null result/" + op, EmLink.EmMirror.ERROR, ReflectEmLink.map(null, op));
        }
        // the idempotent-OK trio, context-sensitive (§1.4)
        eq("ALREADY_IN_PARTY on create is OK", EmLink.EmMirror.OK, ReflectEmLink.map("ALREADY_IN_PARTY", C));
        eq("ALREADY_IN_PARTY on accept is OK", EmLink.EmMirror.OK, ReflectEmLink.map("ALREADY_IN_PARTY", A));
        eq("INVITE_ALREADY_PENDING on invite is OK", EmLink.EmMirror.OK, ReflectEmLink.map("INVITE_ALREADY_PENDING", I));
        eq("NOT_IN_PARTY on leave is OK", EmLink.EmMirror.OK, ReflectEmLink.map("NOT_IN_PARTY", L));
        // unlisted combinations map conservatively (never OK, never a destructive reading)
        eq("ALREADY_IN_PARTY on invite is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("ALREADY_IN_PARTY", I));
        eq("ALREADY_IN_PARTY on leave is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("ALREADY_IN_PARTY", L));
        eq("INVITE_ALREADY_PENDING on accept is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("INVITE_ALREADY_PENDING", A));
        eq("NOT_IN_PARTY on create is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("NOT_IN_PARTY", C));
        eq("NOT_IN_PARTY on invite is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("NOT_IN_PARTY", I));
        eq("NOT_IN_PARTY on accept is not OK", EmLink.EmMirror.NOT_READY, ReflectEmLink.map("NOT_IN_PARTY", A));

        // the EmParty picture contract
        check("NONE is not real", !EmLink.EmParty.NONE.real());
        check("UNKNOWN is not real", !EmLink.EmParty.UNKNOWN.real());
        check("NONE and UNKNOWN are distinct sentinels", EmLink.EmParty.NONE != EmLink.EmParty.UNKNOWN);
        check("a keyed picture is real", new EmLink.EmParty(UUID.randomUUID(), java.util.List.of(), new Object()).real());

        // the driven-invite ledger: 60 s, inviter must match, clock skew is not live
        UUID inv = UUID.randomUUID(), other = UUID.randomUUID();
        ReflectEmLink.DrivenInvite d = new ReflectEmLink.DrivenInvite(inv, 10_000L);
        check("ledger live at once", ReflectEmLink.inviteLive(d, inv, 10_000L));
        check("ledger live at 59.999s", ReflectEmLink.inviteLive(d, inv, 69_999L));
        check("ledger dead at 60s", !ReflectEmLink.inviteLive(d, inv, 70_000L));
        check("ledger dead for another inviter", !ReflectEmLink.inviteLive(d, other, 10_000L));
        check("ledger dead on clock skew", !ReflectEmLink.inviteLive(d, inv, 9_999L));
        check("ledger dead when empty", !ReflectEmLink.inviteLive(null, inv, 10_000L));

        // EmLink.NONE contract
        check("NONE is unavailable", !EmLink.NONE.available());
        check("NONE partyOf is UNKNOWN", EmLink.NONE.partyOf(UUID.randomUUID()) == EmLink.EmParty.UNKNOWN);
        check("NONE drives are ABSENT", EmLink.NONE.createParty(UUID.randomUUID()) == EmLink.EmMirror.ABSENT
                && EmLink.NONE.invite(UUID.randomUUID(), UUID.randomUUID()) == EmLink.EmMirror.ABSENT
                && EmLink.NONE.accept(UUID.randomUUID()) == EmLink.EmMirror.ABSENT
                && EmLink.NONE.leave(UUID.randomUUID()) == EmLink.EmMirror.ABSENT);
        check("NONE is never dungeon-busy", !EmLink.NONE.dungeonBusy(java.util.List.of(UUID.randomUUID())));
        check("NONE has no driven invites", !EmLink.NONE.inviteDriven(inv, other));

        System.out.println("EmLinkMappingTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
