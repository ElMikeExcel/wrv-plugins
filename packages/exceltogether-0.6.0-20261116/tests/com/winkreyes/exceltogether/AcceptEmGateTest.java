package com.winkreyes.exceltogether;

import java.util.*;

/**
 * P-TOGETHER-PARTY-1 (D1 + D4 read-back guard) — the Together-door accept transition against a
 * conflicting EliteMobs party. This is the coverage the 0.5.3 audit found missing: before 0.5.5,
 * "noteJoined" occurred 0 times in tests/ and no shipped test drove Controller.acceptFrom with
 * the accepter pre-seeded in a different EM party.
 *
 * Harness = the rig report's Phase 0 desk harness (board-r364/together-rig/RESULT.md §5):
 * seed the §2.1 pre-state on the shipped fakes, drive invite -> acceptFrom -> noteJoined ->
 * poll. On 0.5.4 the G1/G4/G5 assertions FAIL (the defect reproduces headlessly: hub OK,
 * "You joined" sent, divergence parked); on 0.5.5 they pass with the refuse-with-reason shape.
 * 0.6.0 (U2-D2, SCOPING.md RT-2): G1 and G5 are RE-PINNED to hub behaviour - the 0.5.5
 * refuse-with-reason outcome is replaced by clear-and-join (the accept joins the roster of
 * record; EmSync clears the stale EM membership and re-materialises the face). The conflict
 * check itself, the invite-time disclosure, and the G4 read-back guard survive. Every changed
 * check is named in the U2-D2 build record (D2-RESULT.md).
 * Run: java -cp <classes> com.winkreyes.exceltogether.AcceptEmGateTest
 */
public class AcceptEmGateTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static long heardCount(PartyFeatureTest.FP p, String s) { return p.said.stream().filter(x -> x.contains(s)).count(); }

    public static void main(String[] args) { tests(); System.out.println("AcceptEmGateTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- G1 REWRITTEN (0.6.0, RT-2): accepter in a different EM party — disclose at invite, accept JOINS, stale EM cleared ----------
        // (0.5.5 pinned: refuse-with-reason, invite not consumed, recover after a manual EM leave.)
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess"); var xan = w.j("Xan"); var wes = w.j("Wes");
            EmSyncTest.Em.P p = w.em.makeEm(xan.id); w.em.emJoinRaw(tess.id, p); w.em.emJoinRaw(wes.id, p);
            w.r.ctl.inviteTarget(ivy, tess);
            check("G1 inviter told invite sent", ivy.heard("Party invite sent"));
            check("G1 target popup discloses the EM conflict before deciding", tess.heard("different EliteMobs party") && tess.heard("leaves that EliteMobs party"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("G1 accept joins the Together party (clear-and-join, no refusal)", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("G1 target told they left the other EM party to join", tess.heard("you left it to join this one"));
            check("G1 the invite IS consumed by the join", !w.r.mgr.hasInvite(ivy.id, tess.id, w.r.env.t));
            w.polls(2);
            check("G1 stale EM membership cleared", !w.em.inEm(xan.id, tess.id));
            check("G1 EM face re-materialised from the roster", w.em.inEm(ivy.id, tess.id));
            check("G1 the native party was reverted away", w.em.of(xan.id) == null && w.em.of(wes.id) == null);
        }
        // ---------- G2 (R1d control): inviter and target share the same EM party — no gate, clean join ----------
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess"); var xan = w.j("Xan");
            EmSyncTest.Em.P p = w.em.makeEm(xan.id); w.em.emJoinRaw(ivy.id, p); w.em.emJoinRaw(tess.id, p);
            w.r.ctl.inviteTarget(ivy, tess);
            check("G2 no conflict disclosure when the EM party is shared", !tess.heard("cannot accept this invite yet"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("G2 shared-EM accept joins", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("G2 EM membership untouched", w.em.inEm(ivy.id, tess.id));
        }
        // ---------- G3 (R3h control): inviter holds the EM party, target clean — the noteJoined drive still works ----------
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess"); var wes = w.j("Wes");
            EmSyncTest.Em.P p = w.em.makeEm(ivy.id); w.em.emJoinRaw(wes.id, p);
            w.r.ctl.inviteTarget(ivy, tess);
            check("G3 no conflict disclosure for a clean target", !tess.heard("cannot accept this invite yet"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("G3 accept joins Together", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("G3 noteJoined drove the EM join", w.em.inEm(ivy.id, tess.id));
        }
        // ---------- G4 (D4 read-back guard): EM accept claims OK but the read-back does not show the target ----------
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess");
            w.em.makeEm(ivy.id);
            w.r.ctl.inviteTarget(ivy, tess);
            w.em.acceptR = EmLink.EmMirror.OK;              // EM says yes and does nothing (the D4 shape)
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("G4 hub join stands (noteJoined is post-commit)", w.inOurs(ivy, tess));
            check("G4 OK-but-diverged read-back is messaged, not suppressed", tess.heard("did not line up"));
            int drives = w.em.drives.size();
            tess.clear();
            w.r.ctl.emSync().noteJoined(tess.id, ivy.id);   // a retry inside the quiet window must not re-drive
            check("G4 divergence arms the quiet window (no re-drive)", w.em.drives.size() == drives);
        }
        // ---------- G5 REWRITTEN (0.6.0): noteJoined with a divergent picture clears the stale face at once ----------
        // (0.5.5 pinned: note once, never yank either party, poll does not repeat the note.)
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess"); var xan = w.j("Xan"); var wes = w.j("Wes");
            w.together(ivy, tess);
            EmSyncTest.Em.P p = w.em.makeEm(xan.id); w.em.emJoinRaw(tess.id, p); w.em.emJoinRaw(wes.id, p);
            w.r.ctl.emSync().noteJoined(tess.id, ivy.id);
            check("G5 noteJoined cleared the stale EM membership at once", !w.em.inEm(xan.id, tess.id));
            check("G5 the Together party stands", w.inOurs(ivy, tess));
            check("G5 no kept-divergence note is sent", heardCount(tess, "different EliteMobs party") == 0);
            w.polls(2);
            check("G5 EM face re-materialised from the roster", w.em.inEm(ivy.id, tess.id));
            check("G5 the native party was reverted away", w.em.of(xan.id) == null);
        }
        // ---------- G6: a SOLO EM party is not a conflict — the target's own one-person EM party converges ----------
        {
            EmSyncTest.W w = new EmSyncTest.W(5);
            var ivy = w.j("Ivy"); var tess = w.j("Tess");
            w.em.makeEm(tess.id);
            w.r.ctl.inviteTarget(ivy, tess);
            check("G6 no conflict disclosure for a solo EM party", !tess.heard("cannot accept this invite yet"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("G6 solo-EM accept is not refused", w.inOurs(ivy, tess) && tess.heard("You joined"));
            w.poll();
            check("G6 the solo EM party becomes the party's EM face", w.em.inEm(ivy.id, tess.id));
        }
    }
}
