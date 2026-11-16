package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.5.6 (P-TOGETHER-PARTY-1, D4 ledger fix) - the crossed pending invite, per the build-ready
 * spec in board-r365/together-em-rig/RESULT.md section 3. The rig settled the EliteMobs
 * semantics on real EM 10.9.14: a second invite over an existing pending slot returns
 * INVITE_ALREADY_PENDING and leaves the slot (partyId, inviter, expiry) byte-identical.
 * Before 0.5.6, ReflectEmLink mapped that code to OK and wrote a ledger entry naming the
 * second inviter; EmSync.noteJoined then drove a blind accept that consumed the FOREIGN
 * invite and mis-joined the target into the other party (R3e-prime, reproduced end-to-end).
 *
 * Two layers, matching the suite's existing split: the ReflectEmLink decision is factored
 * into the pure statics inviteOutcome/inviteWritesLedger and pinned directly here (the
 * reflective drive itself needs real EliteMobs classes - rig territory, like map() in
 * EmLinkMappingTest); the EmSync/Controller behaviour is pinned against Fx, a fake EmLink
 * that models the PROVEN EM pending-slot semantics below the link boundary and the FIXED
 * ReflectEmLink contract at it.
 * Run: java -cp <classes> com.winkreyes.exceltogether.ForeignPendingTest
 */
public class ForeignPendingTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** Fake EliteMobs + fixed adapter contract: EM's pending slot is never overwritten by an invite. */
    static final class Fx implements EmLink {
        static final class P { final UUID key; UUID leader; final List<UUID> m = new ArrayList<>(); P(UUID k, UUID l) { key = k; leader = l; m.add(l); } }
        final Map<UUID, P> parties = new LinkedHashMap<>();
        final Map<UUID, UUID> emPending = new HashMap<>();   // EM's own slot: target -> inviter (raw EM state)
        final Map<UUID, UUID> ledger = new HashMap<>();      // Together's driven-invite ledger, as the FIXED ReflectEmLink keeps it
        final List<String> drives = new ArrayList<>();
        final Map<UUID, String> names = new HashMap<>();
        int acceptCalls = 0;
        String n(UUID id) { return names.getOrDefault(id, "?"); }
        public boolean available() { return true; }
        P of(UUID id) { for (P p : parties.values()) if (p.m.contains(id)) return p; return null; }
        public EmParty partyOf(UUID id) {
            P p = of(id);
            return p == null ? EmParty.NONE : new EmParty(p.leader, List.copyOf(p.m), p.key);
        }
        public List<EmParty> allParties() {
            List<EmParty> out = new ArrayList<>();
            for (P p : parties.values()) out.add(new EmParty(p.leader, List.copyOf(p.m), p.key));
            return out;
        }
        public EmMirror createParty(UUID leader) {
            drives.add("create:" + n(leader));
            if (of(leader) != null) return EmMirror.OK;
            P p = new P(UUID.randomUUID(), leader); parties.put(p.key, p);
            return EmMirror.OK;
        }
        public EmMirror invite(UUID inviter, UUID target) {
            drives.add("invite:" + n(inviter) + ">" + n(target));
            P mine = of(inviter); if (mine == null) return EmMirror.NOT_READY;
            P theirs = of(target);
            if (theirs != null && theirs != mine) return EmMirror.TARGET_IN_OTHER_PARTY;
            if (mine.m.size() >= 5) return EmMirror.FULL;
            UUID slot = emPending.get(target);
            if (slot != null) {
                // RAW EM result here is INVITE_ALREADY_PENDING and the slot stays untouched
                // (rig-proven). The fixed adapter decision, as pinned in T1/T2/T4:
                if (inviter.equals(ledger.get(target))) { ledger.put(target, inviter); return EmMirror.OK; }   // own re-drive: keep/refresh
                return EmMirror.FOREIGN_PENDING;                                                                // foreign: NO ledger write
            }
            emPending.put(target, inviter);
            ledger.put(target, inviter);                 // raw SUCCESS writes the ledger
            return EmMirror.OK;
        }
        public EmMirror accept(UUID target) {
            drives.add("accept:" + n(target)); acceptCalls++;
            UUID inv = emPending.remove(target);
            if (inv == null) return EmMirror.NOT_READY;
            P p = of(inv); if (p == null) return EmMirror.NOT_READY;
            if (p.m.size() >= 5) return EmMirror.FULL;
            if (!p.m.contains(target)) p.m.add(target);
            return EmMirror.OK;
        }
        public EmMirror leave(UUID id) { drives.add("leave:" + n(id)); emLeaveRaw(id); return EmMirror.OK; }
        public boolean inviteDriven(UUID inv, UUID tgt) { return inv.equals(ledger.get(tgt)); }
        // raw EM-door state edits / staging
        P makeEm(UUID leader) { P p = new P(UUID.randomUUID(), leader); parties.put(p.key, p); return p; }
        void emJoinRaw(UUID id, P p) { if (!p.m.contains(id)) p.m.add(id); }
        void emLeaveRaw(UUID id) {
            P p = of(id); if (p == null) return;
            p.m.remove(id); emPending.remove(id);
            if (p.m.isEmpty()) parties.remove(p.key);
            else if (p.leader.equals(id)) p.leader = p.m.get(0);
        }
        /** Stage a pending EM invite directly (X invited T at the EM door; T has not answered). */
        void emPendingRaw(UUID inviter, UUID target) { emPending.put(target, inviter); }
        boolean inEm(UUID a, UUID b) { P p = of(a); return p != null && p.m.contains(b); }
    }

    static final class W {
        final PartyFeatureTest.Rig r; final Fx fx = new Fx();
        final List<String> logs = new ArrayList<>();
        W(int cap) { r = new PartyFeatureTest.Rig(cap); r.ctl.setEmLink(fx); r.ctl.setLogSink(logs::add); }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); fx.names.put(p.id, n); return p; }
        void poll() { r.env.t += 2000; r.ctl.emSync().poll(); }
        void together(PartyFeatureTest.FP lead, PartyFeatureTest.FP... others) { for (PartyFeatureTest.FP o : others) r.mgr.adopt(o.id, lead.id); }
        boolean inOurs(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
    }

    public static void main(String[] args) { tests(); System.out.println("ForeignPendingTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- T1: foreign pending at the mirror drive -> FOREIGN_PENDING, zero ledger entries, both wordings ----------
        {
            // the ReflectEmLink decision itself (pure statics; map() untouched, pinned row intact)
            check("T1 decision: foreign INVITE_ALREADY_PENDING is FOREIGN_PENDING",
                    ReflectEmLink.inviteOutcome("INVITE_ALREADY_PENDING", EmLink.EmMirror.OK, false) == EmLink.EmMirror.FOREIGN_PENDING);
            check("T1 decision: a foreign pending writes NO ledger entry",
                    !ReflectEmLink.inviteWritesLedger("INVITE_ALREADY_PENDING", EmLink.EmMirror.OK, false));
            check("T1 decision: map() still collapses the code to OK (pinned row untouched)",
                    ReflectEmLink.map("INVITE_ALREADY_PENDING", ReflectEmLink.Op.INVITE) == EmLink.EmMirror.OK);

            W w = new W(5);
            var ivy = w.j("Ivy"); var vic = w.j("Vic"); var tess = w.j("Tess"); var xan = w.j("Xan"); var wes = w.j("Wes");
            Fx.P px = w.fx.makeEm(xan.id); w.fx.emJoinRaw(wes.id, px);
            Fx.P pi = w.fx.makeEm(ivy.id); w.fx.emJoinRaw(vic.id, pi);
            w.fx.emPendingRaw(xan.id, tess.id);                       // T holds X's waiting EM invite
            w.r.ctl.inviteTarget(ivy, tess);
            check("T1 mirror drive returns FOREIGN_PENDING (ledger holds nothing for T)", w.fx.ledger.get(tess.id) == null);
            check("T1 EM pending slot untouched (still X's)", xan.id.equals(w.fx.emPending.get(tess.id)));
            check("T1 inviter worded at invite time", ivy.heard("already has a party invite waiting in EliteMobs"));
            check("T1 target popup discloses the waiting invite before deciding", tess.heard("leaves that EliteMobs invite untouched"));
        }
        // ---------- T2: own-invite re-drive -> OK, ledger entry kept (the EmLinkMappingTest:36 behaviour, end-to-end) ----------
        {
            check("T2 decision: own INVITE_ALREADY_PENDING re-drive is OK",
                    ReflectEmLink.inviteOutcome("INVITE_ALREADY_PENDING", EmLink.EmMirror.OK, true) == EmLink.EmMirror.OK);
            check("T2 decision: the own re-drive keeps/refreshes the ledger entry",
                    ReflectEmLink.inviteWritesLedger("INVITE_ALREADY_PENDING", EmLink.EmMirror.OK, true));
            check("T2 decision: raw SUCCESS is OK and writes the ledger",
                    ReflectEmLink.inviteOutcome("SUCCESS", EmLink.EmMirror.OK, false) == EmLink.EmMirror.OK
                            && ReflectEmLink.inviteWritesLedger("SUCCESS", EmLink.EmMirror.OK, false));
            check("T2 decision: other results pass through untouched",
                    ReflectEmLink.inviteOutcome("PARTY_FULL", EmLink.EmMirror.FULL, false) == EmLink.EmMirror.FULL
                            && !ReflectEmLink.inviteWritesLedger("PARTY_FULL", EmLink.EmMirror.FULL, false));

            W w = new W(5);
            var ivy = w.j("Ivy"); var vic = w.j("Vic"); var tess = w.j("Tess");
            Fx.P pi = w.fx.makeEm(ivy.id); w.fx.emJoinRaw(vic.id, pi);
            w.r.ctl.inviteTarget(ivy, tess);
            check("T2 first drive wrote the ledger entry", ivy.id.equals(w.fx.ledger.get(tess.id)));
            EmLink.EmMirror again = w.fx.invite(ivy.id, tess.id);
            check("T2 re-drive over our own pending invite is OK", again == EmLink.EmMirror.OK);
            check("T2 ledger entry kept after the re-drive", ivy.id.equals(w.fx.ledger.get(tess.id)) && w.fx.inviteDriven(ivy.id, tess.id));
            check("T2 EM pending slot still ours", ivy.id.equals(w.fx.emPending.get(tess.id)));
        }
        // ---------- T3: noteJoined with a foreign pending -> zero accept drives, both sides worded, Together stands, quiet window armed ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy"); var vic = w.j("Vic"); var tess = w.j("Tess"); var xan = w.j("Xan"); var wes = w.j("Wes");
            Fx.P px = w.fx.makeEm(xan.id); w.fx.emJoinRaw(wes.id, px);
            Fx.P pi = w.fx.makeEm(ivy.id); w.fx.emJoinRaw(vic.id, pi);
            w.fx.emPendingRaw(xan.id, tess.id);
            w.together(ivy, vic, tess);                               // the Together-door accept already committed
            w.r.ctl.emSync().noteJoined(tess.id, ivy.id);
            check("T3 zero accept drives over the foreign pending", w.fx.acceptCalls == 0 && w.fx.drives.stream().noneMatch(d -> d.startsWith("accept:")));
            check("T3 target worded (join stood, EM side left untouched on purpose)", tess.heard("left untouched on purpose") && tess.heard("still waiting for you"));
            check("T3 inviter worded (EM side did not link, and why)", ivy.heard("did not link") && ivy.heard("EliteMobs invite waiting"));
            check("T3 Together party stands", w.inOurs(ivy, tess) && w.inOurs(ivy, vic));
            check("T3 foreign EM invite NOT consumed (T not mis-joined into X's party)", !w.fx.inEm(xan.id, tess.id) && xan.id.equals(w.fx.emPending.get(tess.id)));
            int drives = w.fx.drives.size();
            w.r.ctl.emSync().noteJoined(tess.id, ivy.id);             // a retry inside the quiet window must not re-drive
            check("T3 refusal arms the quiet window (no re-drive)", w.fx.drives.size() == drives);
        }
        // ---------- T4: a ledger entry naming a DIFFERENT inviter does not satisfy the own-invite check ----------
        {
            UUID zed = UUID.randomUUID(), ivyId = UUID.randomUUID();
            long now = System.currentTimeMillis();
            check("T4 ledger liveness is inviter-exact (an entry naming Zed is not Ivy's)",
                    !ReflectEmLink.inviteLive(new ReflectEmLink.DrivenInvite(zed, now), ivyId, now)
                            && ReflectEmLink.inviteLive(new ReflectEmLink.DrivenInvite(zed, now), zed, now));

            W w = new W(5);
            var ivy = w.j("Ivy"); var vic = w.j("Vic"); var tess = w.j("Tess"); var xan = w.j("Xan"); var wes = w.j("Wes"); var zedP = w.j("Zed");
            Fx.P px = w.fx.makeEm(xan.id); w.fx.emJoinRaw(wes.id, px);
            Fx.P pi = w.fx.makeEm(ivy.id); w.fx.emJoinRaw(vic.id, pi);
            w.fx.emPendingRaw(xan.id, tess.id);
            w.fx.ledger.put(tess.id, zedP.id);                        // a stale entry naming a different inviter
            check("T4 inviteDriven is false for this inviter", !w.fx.inviteDriven(ivy.id, tess.id) && w.fx.inviteDriven(zedP.id, tess.id));
            EmLink.EmMirror r = w.fx.invite(ivy.id, tess.id);
            check("T4 the drive is FOREIGN_PENDING, not an own re-drive OK", r == EmLink.EmMirror.FOREIGN_PENDING);
            check("T4 the other inviter's entry is left exactly as it was", zedP.id.equals(w.fx.ledger.get(tess.id)));
            w.together(ivy, vic, tess);
            w.r.ctl.emSync().noteJoined(tess.id, ivy.id);
            check("T4 no skip and no accept drive at noteJoined either", w.fx.acceptCalls == 0);
        }
        // ---------- T5: control - clean invite/accept convergence (R3h shape) unchanged ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy"); var wes = w.j("Wes"); var tess = w.j("Tess");
            Fx.P pi = w.fx.makeEm(ivy.id); w.fx.emJoinRaw(wes.id, pi);
            w.r.ctl.inviteTarget(ivy, tess);
            check("T5 no foreign-pending wording on a clean invite", !ivy.heard("already has a party invite waiting") && !tess.heard("leaves that EliteMobs invite untouched"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("T5 clean accept joins Together", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("T5 noteJoined drove the genuine EM accept exactly once", w.fx.acceptCalls == 1 && w.fx.inEm(ivy.id, tess.id));
            w.poll();
            check("T5 both faces converged after the poll", w.inOurs(ivy, tess) && w.fx.inEm(ivy.id, tess.id));
        }
    }
}
