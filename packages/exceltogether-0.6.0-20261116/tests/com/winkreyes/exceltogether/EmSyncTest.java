package com.winkreyes.exceltogether;

import java.util.*;

/**
 * DESIGN §11B/§11C/§11D for the party mesh: EmSync against a scripted fake EliteMobs (state +
 * drive-call recorder), McmmoSyncTest-style. §11C idempotence is woven in: after every scenario
 * converges, {@code rest(...)} runs two more polls and asserts zero drives and zero messages.
 * 0.6.0 (P-PARTY-REBUILD-1, U2-D2): rewritten for the materialise-only EmSync - Cases B/D/E
 * adoption are deleted, so the adoption-pinning scenarios (old B1, B3, B4, B8, B9-L1, D2, and
 * the tail of B5) are re-pinned to revert behaviour, and new RT-1/RT-2/RT-4 unit-layer
 * scenarios pin popup-answer matching, clear-and-join, frozen deferral and
 * decline-then-native-accept. Every rewritten/deleted check is named in the U2-D2 build
 * record (D2-RESULT.md).
 * Run: java -cp <classes> com.winkreyes.exceltogether.EmSyncTest
 */
public class EmSyncTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** An in-memory EliteMobs party system: session parties, pending invites, a busy (dungeon) set. */
    static final class Em implements EmLink {
        static final class P { final UUID key; UUID leader; final List<UUID> m = new ArrayList<>(); P(UUID k, UUID l) { key = k; leader = l; m.add(l); } }
        final Map<UUID, P> parties = new LinkedHashMap<>();
        final Map<UUID, UUID> pending = new HashMap<>();
        /** 0.6.0: Together's driven-invite ledger, modelled on ReflectEmLink: written by invite(), read by inviteDriven(), and NOT cleared by accept() (the EM pending slot is; the ledger lives for its TTL). */
        final Map<UUID, UUID> ledger = new HashMap<>();
        final List<String> drives = new ArrayList<>();
        final Map<UUID, String> names = new HashMap<>();
        final Set<UUID> unknown = new HashSet<>();
        final Set<UUID> busy = new HashSet<>();
        EmMirror createR, inviteR, acceptR, leaveR;
        String n(UUID id) { return names.getOrDefault(id, "?"); }
        public boolean available() { return true; }
        P of(UUID id) { for (P p : parties.values()) if (p.m.contains(id)) return p; return null; }
        public EmParty partyOf(UUID id) {
            if (unknown.contains(id)) return EmParty.UNKNOWN;
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
            if (createR != null) return createR;
            if (of(leader) != null) return EmMirror.OK;                       // ALREADY_IN_PARTY, mapped OK
            P p = new P(UUID.randomUUID(), leader); parties.put(p.key, p);
            return EmMirror.OK;
        }
        public EmMirror invite(UUID inviter, UUID target) {
            drives.add("invite:" + n(inviter) + ">" + n(target));
            if (inviteR != null) return inviteR;
            // Real EM 10.9.14 (rig run 416, AT-1): an invite from a party-less inviter
            // MATERIALISES the inviter's solo EM party to hold the pending invite - it
            // does not fail. The pre-AT-1 fake returned NOT_READY here and modelled only
            // the pending slot, which is exactly why the desk suite could not see AT-1:
            // no inviter membership change ever existed for the revert rule to meet.
            P mine = of(inviter); if (mine == null) mine = makeEm(inviter);
            P theirs = of(target);
            if (theirs != null && theirs != mine) return EmMirror.TARGET_IN_OTHER_PARTY;
            if (mine.m.size() >= 5) return EmMirror.FULL;
            ledger.put(target, inviter);
            if (pending.put(target, inviter) != null) return EmMirror.OK;     // INVITE_ALREADY_PENDING, mapped OK
            return EmMirror.OK;
        }
        public EmMirror accept(UUID target) {
            drives.add("accept:" + n(target));
            if (acceptR != null) return acceptR;
            UUID inv = pending.remove(target);
            if (inv == null) return EmMirror.NOT_READY;
            P p = of(inv); if (p == null) return EmMirror.NOT_READY;
            if (p.m.size() >= 5) return EmMirror.FULL;
            if (!p.m.contains(target)) p.m.add(target);
            return EmMirror.OK;
        }
        public EmMirror leave(UUID id) {
            drives.add("leave:" + n(id));
            if (leaveR != null) return leaveR;
            emLeaveRaw(id);
            return EmMirror.OK;
        }
        public boolean dungeonBusy(Collection<UUID> ms) { for (UUID u : ms) if (busy.contains(u)) return true; return false; }
        public boolean inviteDriven(UUID inv, UUID tgt) { return inv.equals(ledger.get(tgt)); }
        /** Run 426 part 1: the pending read (EmLink.hasPendingInvite) over the same pending slot invite()/accept() use. */
        public boolean hasPendingInvite(UUID target) { return pending.containsKey(target); }
        // raw EM-door state edits (what EM itself does when players use /em party)
        P makeEm(UUID leader) { P p = new P(UUID.randomUUID(), leader); parties.put(p.key, p); return p; }
        /**
         * Run 426 part 1: a NATIVE /em party invite, as EM itself performs it - the inviter's
         * solo party materialises to hold the pending slot and the pending slot is written,
         * but NO driven-invite ledger entry exists (the ledger is written only inside
         * ReflectEmLink.invite, which a native EM command bypasses entirely). This is the
         * raw native-pending state SCOPING.md §5 part 1 found missing from the fake: before
         * this helper the fake's pending map was written only by the driven invite() above.
         */
        void emInviteRaw(UUID inviter, UUID target) {
            P mine = of(inviter); if (mine == null) mine = makeEm(inviter);
            pending.put(target, inviter);
        }
        void emJoinRaw(UUID id, P p) { if (!p.m.contains(id)) p.m.add(id); }
        void emLeaveRaw(UUID id) {
            // Run 426 BP-6 (D93): real EM kills a pending invite when its INVITER's
            // materialised party is dismantled (Mirror Session 2: emPending(A)=false at
            // +3s after the revert dismantled B's party). The pending map is keyed by
            // TARGET, so the inviter's leave must remove every entry naming the inviter -
            // the pre-fix fake removed only the entry keyed by the leaver and left the
            // target's pending slot alive forever.
            pending.values().removeIf(inv -> inv.equals(id));
            P p = of(id); if (p == null) { pending.remove(id); return; }
            p.m.remove(id); pending.remove(id);
            if (p.m.isEmpty()) parties.remove(p.key);
            else if (p.leader.equals(id)) p.leader = p.m.get(0);
        }
        /** Run 426 parts 2+3: the target answers a native invite WITHOUT joining - a native decline, or EM's own 60s timeout: the pending slot dies, the inviter's materialised party is left standing for the mesh to judge. */
        void emPendingGoneRaw(UUID target) { pending.remove(target); }
        boolean inEm(UUID a, UUID b) { P p = of(a); return p != null && p.m.contains(b); }
    }

    static final class W {
        final PartyFeatureTest.Rig r; final Em em = new Em();
        final List<String> logs = new ArrayList<>();
        W(int cap) { r = new PartyFeatureTest.Rig(cap); r.ctl.setEmLink(em); r.ctl.setLogSink(logs::add); }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); em.names.put(p.id, n); return p; }
        PartyFeatureTest.FP b(String n) { PartyFeatureTest.FP p = r.bedrock(n); em.names.put(p.id, n); return p; }
        PartyFeatureTest.FP flood(String n, String uuid) {
            PartyFeatureTest.FP p = new PartyFeatureTest.FP(UUID.fromString(uuid), n, n, true);
            r.env.add(p); em.names.put(p.id, n); return p;
        }
        void poll() { r.env.t += 2000; r.ctl.emSync().poll(); }
        void polls(int n) { for (int i = 0; i < n; i++) poll(); }
        void together(PartyFeatureTest.FP lead, PartyFeatureTest.FP... others) { for (PartyFeatureTest.FP o : others) r.mgr.adopt(o.id, lead.id); }
        boolean inOurs(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        void rest(String label, PartyFeatureTest.FP... ps) {
            int d = em.drives.size();
            int[] counts = new int[ps.length];
            for (int i = 0; i < ps.length; i++) counts[i] = ps[i].said.size();
            polls(2);
            check(label + " [11C: rest drives nothing]", em.drives.size() == d);
            boolean quiet = true;
            for (int i = 0; i < ps.length; i++) if (ps[i].said.size() != counts[i]) quiet = false;
            check(label + " [11C: rest says nothing]", quiet);
        }
    }

    public static void main(String[] args) { tests(); System.out.println("EmSyncTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- B1 REWRITTEN (0.6.0): a native EM-only party of 3 with no hub invite is REVERTED, never adopted ----------
        // (0.5.x pinned: adopted into Together, zero drives, adoption tell. Case B is deleted.)
        {
            W w = new W(5);
            var a = w.j("Ann"); var b = w.j("Bob"); var c = w.j("Cy");
            Em.P p = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, p); w.em.emJoinRaw(c.id, p);
            w.poll();
            check("B1 no Together party was created from EM", w.r.mgr.partyOf(a.id) == null && w.r.mgr.partyOf(b.id) == null && w.r.mgr.partyOf(c.id) == null);
            check("B1 the native EM party was dismantled", w.em.parties.isEmpty());
            check("B1 the only drives are the revert leaves", w.em.drives.equals(List.of("leave:Ann", "leave:Bob", "leave:Cy")));
            check("B1 each player got the Q3 revert notice", a.heard("run with /party") && b.heard("run with /party") && c.heard("run with /party"));
            check("B1 every revert was logged", w.logs.stream().filter(s -> s.contains("reverted a non-door")).count() == 3);
            w.rest("B1", a, b, c);
        }
        // ---------- B2: Together-only party of 3 materialises with the exact drive sequence ----------
        {
            W w = new W(5);
            var l = w.j("Lea"); var a = w.j("Amy"); var b = w.j("Ben");
            w.together(l, a, b);
            w.poll();
            check("B2 drive sequence", w.em.drives.equals(List.of("create:Lea", "invite:Lea>Amy", "accept:Amy", "invite:Lea>Ben", "accept:Ben")));
            check("B2 EM party holds all three, leader first", w.em.inEm(l.id, a.id) && w.em.inEm(l.id, b.id) && w.em.of(l.id).leader.equals(l.id));
            check("B2 mirrored marked", w.r.ctl.emSync().isMirrored(a.id) && w.r.ctl.emSync().isMirrored(b.id));
            w.rest("B2", l, a, b);
        }
        // ---------- B3 REWRITTEN (0.6.0): a native move to another EM party is reverted; the roster never follows ----------
        // (0.5.x pinned: X leaves the old Together party and is adopted with the new EM group.)
        {
            W w = new W(5);
            var x = w.j("Xan"); var a = w.j("Ana"); var b = w.j("Bea"); var c = w.j("Cal");
            w.together(x, a);
            w.poll();                                   // twin formed, X mirrored to its key
            check("B3 setup converged", w.em.inEm(x.id, a.id));
            w.em.emLeaveRaw(x.id);                      // X leaves the twin at the EM door...
            Em.P p2 = w.em.makeEm(b.id); w.em.emJoinRaw(x.id, p2); w.em.emJoinRaw(c.id, p2);   // ...and joins a different EM party natively
            w.poll();
            check("B3 X stayed in the roster party", w.inOurs(x, a));
            check("B3 X was NOT adopted with the new EM group", !w.inOurs(x, b) && w.r.mgr.partyOf(b.id) == null);
            check("B3 the native EM party was dismantled", w.em.of(b.id) == null && w.em.of(c.id) == null);
            w.poll();
            check("B3 the roster face was re-materialised", w.em.inEm(x.id, a.id));
            check("B3 X got the Q3 revert notice, not a move tell", x.heard("run with /party") && !x.heard("moved to another EliteMobs party"));
            w.rest("B3", x, a, b, c);
        }
        // ---------- B4 REWRITTEN (0.6.0): static disjoint groups are cleared and re-materialised - no kept divergence ----------
        // (0.5.x Case E pinned: X stays in both, one kept-note, no leave driven. Case E is deleted.)
        {
            W w = new W(5);
            var x = w.j("Xiu"); var a = w.j("Ali"); var b = w.j("Bo"); var c = w.j("Cy2");
            w.together(x, a);
            Em.P p = w.em.makeEm(b.id); w.em.emJoinRaw(x.id, p); w.em.emJoinRaw(c.id, p);
            w.polls(2);
            check("B4 X stayed in the Together party", w.inOurs(x, a));
            check("B4 the stale EM membership was cleared and the roster face materialised", w.em.inEm(x.id, a.id));
            check("B4 the native-only members were reverted out of EM", w.em.of(b.id) == null && w.em.of(c.id) == null);
            long notes = x.said.stream().filter(s -> s.contains("different EliteMobs party")).count();
            check("B4 no kept-divergence note exists anymore", notes == 0);
            check("B4 X got the Q3 revert notice once", x.said.stream().filter(s -> s.contains("run with /party")).count() == 1);
            w.rest("B4", x, a);
        }
        // ---------- B5: a kicked player is never re-adopted while EM still lists them in the same party ----------
        {
            W w = new W(5);
            var l = w.j("Liv"); var x = w.j("Xio"); var d = w.j("Dee");
            w.together(l, x);
            w.r.ctl.command(l, new String[]{"list"});   // remember names for the kick query
            w.poll();
            check("B5 setup converged", w.em.inEm(l.id, x.id));
            Em.P twin = w.em.of(l.id);
            w.r.ctl.command(l, new String[]{"kick", "Xio"});
            check("B5 kick took X out of Together", !w.inOurs(l, x));
            check("B5 kick drove the EM leave", !w.em.inEm(l.id, x.id));
            w.em.emJoinRaw(x.id, twin);                 // the EM leg still lists X (its leave had not landed)
            w.polls(3);
            check("B5 suppressed X is not re-adopted", !w.inOurs(l, x));
            w.em.emLeaveRaw(x.id);                      // EM now agrees X is out: suppression spends itself
            w.poll();
            Em.P fresh = w.em.makeEm(d.id); w.em.emJoinRaw(x.id, fresh);   // X makes a fresh EM start with Dee
            w.poll();
            // REWRITTEN (0.6.0): a fresh native EM group no longer adopts - it is reverted like any non-door change.
            check("B5 a fresh EM join after suppression is reverted, not adopted", !w.inOurs(x, d) && w.r.mgr.partyOf(d.id) == null);
            check("B5 the fresh EM party was dismantled", w.em.of(x.id) == null && w.em.of(d.id) == null);
            check("B5 X got the Q3 revert notice", x.heard("run with /party"));
            w.rest("B5", l, x, d);
        }
        // ---------- B7: UNKNOWN reads mutate nothing, watch expiry is silent ----------
        {
            W w = new W(5);
            var a = w.j("Aya"); var b = w.j("Ben2");
            w.together(a, b);
            w.em.unknown.add(a.id); w.em.unknown.add(b.id);
            w.r.ctl.emWatch(List.of(a.id, b.id));
            w.polls(35);
            check("B7 UNKNOWN: no drives", w.em.drives.isEmpty());
            check("B7 UNKNOWN: Together party untouched", w.inOurs(a, b));
            check("B7 UNKNOWN: no messages", a.said.isEmpty() && b.said.isEmpty());
            w.em.unknown.clear();
            w.poll();
            check("B7 once readable, materialisation proceeds", w.em.inEm(a.id, b.id));
            w.rest("B7", a, b);
        }
        // ---------- B8 REWRITTEN (0.6.0): two full parties sharing one member - the roster wins, the native party is dismantled ----------
        // (0.5.x pinned: zero drives, both parties unchanged, kept-note, EM-only members wait-listed.)
        {
            W w = new W(5);
            var s = w.j("Sam");
            PartyFeatureTest.FP[] t = {w.j("T1"), w.j("T2"), w.j("T3"), w.j("T4")};
            PartyFeatureTest.FP[] e = {w.j("E1"), w.j("E2"), w.j("E3"), w.j("E4")};
            w.together(s, t[0], t[1], t[2], t[3]);
            Em.P p = w.em.makeEm(s.id);
            for (var m : e) w.em.emJoinRaw(m.id, p);
            w.polls(3);
            check("B8 Together party unchanged", w.r.mgr.partyOf(s.id).size() == 5 && w.inOurs(s, t[3]));
            check("B8 the EM face converged to the roster party", w.em.of(s.id) != null && w.em.of(s.id).m.size() == 5 && w.em.inEm(s.id, t[3].id) && !w.em.inEm(s.id, e[0].id));
            check("B8 the native-only members were reverted out of EM (no party left)", w.em.of(e[0].id) == null && w.em.of(e[3].id) == null);
            check("B8 nobody was wait-listed under one-door", !w.r.ctl.emSync().isWaiting(e[0].id));
            check("B8 Sam got the Q3 revert notice for the stale membership", s.heard("run with /party"));
            w.rest("B8", s, t[0], e[0]);
        }
        // ---------- B9: leader cases ----------
        {
            // L1 REWRITTEN (0.6.0): a party formed at the EM door creates no Together party at all - it is reverted.
            // (0.5.x pinned: the EM creator leads the adopted Together party.)
            W w = new W(5);
            var a = w.j("Ara"); var b = w.j("Bas");
            Em.P p = w.em.makeEm(b.id); w.em.emJoinRaw(a.id, p);
            w.poll();
            check("B9 L1 no Together party from the EM door", w.r.mgr.partyOf(a.id) == null && w.r.mgr.partyOf(b.id) == null);
            check("B9 L1 the native EM party was dismantled", w.em.parties.isEmpty());
            w.rest("B9a", a, b);
        }
        {
            // L4 + L3 (reform off) - REWRITTEN for 0.5.3 (BUG-VERDICT.md): the EM leader leaves
            // EM only. 0.5.2 expected the record to stand and L to be driven back in - that was
            // the convicted party-leave bug. A settled member's EM-door leave now ends the party
            // for them everywhere; the rest of the party and EM's promoted leader stand.
            W w = new W(5);
            var l = w.j("Lia"); var a = w.j("Ari"); var b = w.j("Bia");
            w.together(l, b, a);                        // Together join order: L, B, A (Together would promote B)
            // shape the EM twin's display order as L, A, B by materialising then reordering the fake's list
            w.poll();
            Em.P twin = w.em.of(l.id);
            twin.m.clear(); twin.m.add(l.id); twin.m.add(a.id); twin.m.add(b.id);
            w.em.emLeaveRaw(l.id);                      // EM promotes A (display order); L only left EM
            int drivesBefore = w.em.drives.size();
            w.poll();
            check("B9 L4 (0.5.3): an EM-door leave ends the Together membership too", !w.inOurs(l, a) && !w.inOurs(l, b));
            check("B9 L4 (0.5.3): the rest of the party stays together under the next leader", w.r.mgr.partyOf(a.id).leader.equals(b.id) && w.inOurs(a, b));
            check("B9 L4 (0.5.3): L is NOT driven back into EM - a voluntary leave sticks", !w.em.inEm(a.id, l.id));
            check("B9 L3 (0.5.3): EM keeps its promoted leader (no reform)", twin.leader.equals(a.id));
            check("B9 L4 (0.5.3): L heard he left", l.heard("You left the party"));
            w.rest("B9b", l, a, b);
        }
        {
            // L3 with leader-reform on: a leader-of-record change re-forms the EM party
            W w = new W(5);
            var l = w.j("Lem"); var a = w.j("Aza"); var b = w.j("Bel");
            w.together(l, a, b);
            w.poll();
            Em.P twin = w.em.of(l.id);
            twin.m.clear(); twin.m.add(l.id); twin.m.add(b.id); twin.m.add(a.id);   // EM display order L, B, A (EM would promote B)
            w.r.ctl.emSync().setLeaderReform(true);
            w.r.mgr.leave(l.id, new PartyManager.Change());   // Together promotes A (longest-standing)
            check("B9 setup: Together leader is now A", w.r.mgr.partyOf(a.id).leader.equals(a.id));
            w.em.emLeaveRaw(l.id);                      // EM's own copy of the departure
            w.poll();
            check("B9 reform: EM party re-formed under the leader of record", w.em.of(a.id) != null && w.em.of(a.id).leader.equals(a.id));
            check("B9 reform: EM holds the same members", w.em.inEm(a.id, b.id));
            w.rest("B9c", a, b);
        }
        // ---------- B10: dungeon freeze, quiet window, deferred-op expiry ----------
        {
            W w = new W(5);
            var l = w.j("Lux"); var a = w.j("Ana2"); var b = w.j("Bob2");
            w.together(l, a);
            w.em.busy.add(a.id);
            w.poll();
            check("B10 frozen: zero drives", w.em.drives.isEmpty());
            w.together(l, b);                           // a third member joins Together mid-freeze
            w.poll();
            check("B10 still frozen: zero drives", w.em.drives.isEmpty());
            w.em.busy.clear();
            w.poll();
            check("B10 freeze lifts: queued materialise runs FIFO",
                    w.em.drives.equals(List.of("create:Lux", "invite:Lux>Ana2", "accept:Ana2", "invite:Lux>Bob2", "accept:Bob2")));
            w.rest("B10a", l, a, b);
        }
        {
            W w = new W(5);
            var l = w.j("Liv2"); var a = w.j("Ari2");
            w.together(l, a);
            w.em.acceptR = EmLink.EmMirror.NOT_READY;   // a refusal: quiet window (F2)
            w.poll();
            int afterRefusal = w.em.drives.size();
            check("B10 refusal drove create+invite+accept once", afterRefusal == 3);
            w.em.acceptR = null;
            w.poll();                                   // ~2s later: inside the 45s quiet window
            check("B10 quiet window honoured: no retry inside it", w.em.drives.size() == afterRefusal);
            for (int i = 0; i < 24; i++) w.poll();      // ~48s pass
            check("B10 retry after the window converges", w.em.inEm(l.id, a.id));
            w.rest("B10b", l, a);
        }
        {
            W w = new W(5);
            var l = w.j("Lom"); var a = w.j("Asa");
            w.together(l, a);
            w.em.busy.add(l.id);                        // frozen for good
            w.poll();                                   // op queued
            for (int i = 0; i < 160; i++) w.poll();     // ~320s of polls
            long drops = w.logs.stream().filter(s -> s.startsWith("ExcelTogether mesh: dropped a deferred")).count();
            check("B10 op older than defer-max is dropped with one log line", drops == 1);
            check("B10 nothing was ever driven", w.em.drives.isEmpty());
        }
        // ---------- B11: adapter absent — EmSync inert, two-way behaviour untouched ----------
        {
            PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
            var a = r.java("Solo1"); var b = r.java("Solo2");
            r.ctl.emSync().poll();
            r.ctl.command(a, new String[]{"invite", "Solo2"});
            r.ctl.command(b, new String[]{"accept"});
            check("B11 parties work with EmLink.NONE", r.mgr.sameParty(a.id, b.id));
            r.ctl.emSync().poll();
            check("B11 still fine after polls", r.mgr.sameParty(a.id, b.id));
        }
        // ---------- B6 / §6 + §11D: a full roster of 5 materialises in EM; a freed seat admits the next member ----------
        // RE-PINNED by U2-D1 (0.6.0): the 0.5.x setup staged an mcMMO party of 7 and let
        // McmmoSync ADOPT five of them at login ("their mcMMO party is the record"), with
        // the other two waiting on the mcMMO side and a freed seat adopting the first
        // waiter. Adoption is deleted: the roster of record is the record. The scenario
        // now forms the roster hub-side and keeps its EM assertions - five materialise,
        // a freed seat admits the next member into EM too, the seventh stays out. The
        // MERGE (run 407, W15): this D1 re-pin is RETAINED in the merged tree and
        // D2's old B6 block is discarded - D2's version staged its roster through
        // mcMMO login adoption, the premise U2-D1 deleted, so it cannot pass
        // against the merged McmmoSync. B6's EM assertions are unchanged.
        {
            PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
            McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc();
            r.ctl.setLink(mc);
            W w = new W(5);
            // reuse the same rig for both fakes
            r.ctl.setEmLink(w.em);
            PartyFeatureTest.FP[] ps = new PartyFeatureTest.FP[7];
            for (int i = 0; i < 7; i++) { ps[i] = r.java("P" + i); mc.names.put(ps[i].id, "P" + i); w.em.names.put(ps[i].id, "P" + i); }
            for (int i = 1; i < 5; i++) r.mgr.adopt(ps[i].id, ps[0].id);   // the roster of record: five members
            for (int i = 0; i < 4; i++) { r.env.t += 2000; r.ctl.tick(); }
            check("B6 five are in the Together party (roster of record)", r.mgr.partyOf(ps[0].id).size() == 5);
            check("B6 the other two are not adopted from anywhere", !r.mgr.inParty(ps[5].id) && !r.mgr.inParty(ps[6].id));
            check("B6 the five are materialised in EM", w.em.of(ps[0].id) != null && w.em.of(ps[0].id).m.size() == 5);
            r.ctl.command(ps[0], new String[]{"list"});
            r.ctl.command(ps[4], new String[]{"leave", "yes"});
            r.mgr.adopt(ps[5].id, ps[0].id);                // the freed seat is filled at the roster, not by a face
            for (int i = 0; i < 4; i++) { r.env.t += 2000; r.ctl.tick(); }
            check("B6 freed seat: the next member is in the Together party", r.mgr.sameParty(ps[0].id, ps[5].id));
            check("B6 the new member joins EM too", w.em.inEm(ps[0].id, ps[5].id));
            check("B6 the seventh still stays out", !r.mgr.sameParty(ps[0].id, ps[6].id));
        }
        // ---------- §11D overshoot: two solo players stay solo; separate EM parties never merge ----------
        {
            W w = new W(5);
            var a = w.j("One"); var b = w.j("Two");
            w.polls(3);
            check("D1 two solo players stay solo everywhere", w.r.mgr.partyOf(a.id) == null && w.r.mgr.partyOf(b.id) == null && w.em.parties.isEmpty());
            check("D1 zero drives", w.em.drives.isEmpty());
        }
        {
            // REWRITTEN (0.6.0): two native EM parties adopt nothing - both are reverted.
            W w = new W(5);
            var a = w.j("A1"); var b = w.j("B1"); var c = w.j("C1"); var d = w.j("D1");
            Em.P p1 = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, p1);
            Em.P p2 = w.em.makeEm(c.id); w.em.emJoinRaw(d.id, p2);
            w.poll();
            check("D2 two native EM parties create no Together parties", w.r.mgr.partyOf(a.id) == null && w.r.mgr.partyOf(c.id) == null);
            check("D2 both native EM parties were dismantled", w.em.parties.isEmpty());
            w.rest("D2", a, b, c, d);
        }
        // ---------- §11D: Bedrock (floodgate) UUIDs round-trip ----------
        {
            W w = new W(5);
            var l = w.j("JavaLead");
            var f = w.flood(".BedMate", "00000000-0000-0000-0000-0000000000aa");
            w.together(l, f);
            w.poll();
            check("D3 floodgate UUID materialises in EM unchanged", w.em.inEm(l.id, f.id));
            check("D3 invite used the member slot, accept followed", w.em.drives.contains("accept:.BedMate"));
            w.rest("D3", l, f);
        }
        // ---------- §11D: leader in offline grace — materialise waits for a stable picture ----------
        {
            W w = new W(5);
            var l = w.j("Gone"); var a = w.j("Stay");
            w.together(l, a);
            w.r.env.on.remove(l.id); w.r.mgr.onQuit(l.id, w.r.env.t);
            w.polls(2);
            check("D4 no EM party while the leader is in grace", w.em.parties.isEmpty() && w.em.drives.isEmpty());
            w.r.env.add(l); w.r.mgr.onJoin(l.id);
            w.poll();
            check("D4 leader back: materialised under the leader", w.em.of(l.id) != null && w.em.of(l.id).leader.equals(l.id));
            w.rest("D4", l, a);
        }
        // ---------- §11D: suppression holds through a leader change in the same EM party (same key) ----------
        {
            W w = new W(5);
            var l = w.j("Kee"); var x = w.j("Ex"); var m = w.j("Mel");
            w.together(l, x, m);
            w.r.ctl.command(l, new String[]{"list"});
            w.poll();
            Em.P twin = w.em.of(l.id);
            w.r.ctl.command(l, new String[]{"kick", "Ex"});
            w.em.emJoinRaw(x.id, twin);                 // EM still lists X in the same party
            w.em.emLeaveRaw(l.id);                      // the EM party changes leader (same partyKey)
            w.polls(3);
            check("D5 kicked X stays out after an EM leader change", !w.inOurs(m, x));
            w.rest("D5", l, m);
        }
        // ---------- RT-1 unit layer (0.6.0): an EM-door leave sticks over >=30 poll cycles, no re-adoption, no re-materialise ----------
        {
            W w = new W(5);
            var lea = w.j("Lea6"); var mem = w.j("Mem6");
            w.together(lea, mem);
            w.poll();
            check("RT-1 setup converged", w.em.inEm(lea.id, mem.id) && w.inOurs(lea, mem));
            w.em.emLeaveRaw(mem.id);                    // the member steps out at the EM door
            w.polls(30);
            check("RT-1 EM-door leave sticks: leaver out of the roster", !w.r.mgr.inParty(mem.id));
            check("RT-1 the two-man party ended for the leader too", w.r.mgr.partyOf(lea.id) == null);
            check("RT-1 leaver was never re-materialised into EM", w.em.of(mem.id) == null);
            check("RT-1 last roster mutation is the DOOR-origin leave (no post-leave mutation)", w.r.mgr.lastOrigin() == RosterOrigin.DOOR);
            w.rest("RT-1", lea, mem);
        }
        // ---------- RT-2a unit layer (0.6.0): popup-answer matching - a native EM accept of a driven hub invite completes the hub accept ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy2"); var wes = w.j("Wes2"); var tess = w.j("Tess2");
            w.together(ivy, wes);
            w.poll();                                   // the roster face materialises first
            check("RT-2a setup face materialised", w.em.inEm(ivy.id, wes.id));
            w.r.ctl.inviteTarget(ivy, tess);            // hub invite + driven EM invite (ledger entry)
            check("RT-2a the driven invite is in the ledger", w.em.inviteDriven(ivy.id, tess.id));
            w.em.accept(tess.id);                       // the target answers at the EM popup (EM's own action)
            int drivesBefore = w.em.drives.size();
            w.poll();
            check("RT-2a popup answer completed the hub accept", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("RT-2a the completion carries DOOR origin", w.r.mgr.originOfPlayer(tess.id) == RosterOrigin.DOOR);
            check("RT-2a the poll itself drove nothing (the answer was already in EM)", w.em.drives.size() == drivesBefore);
            check("RT-2a both faces agree on the roster", w.em.inEm(ivy.id, tess.id) && w.em.inEm(ivy.id, wes.id));
            w.rest("RT-2a", ivy, wes, tess);
        }
        // ---------- RT-2b unit layer (0.6.0): command accept from a disjoint native EM party joins; the stale membership is cleared ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy3"); var tess = w.j("Tess3"); var xan = w.j("Xan3"); var wes = w.j("Wes3");
            Em.P p = w.em.makeEm(xan.id); w.em.emJoinRaw(tess.id, p); w.em.emJoinRaw(wes.id, p);
            w.r.ctl.inviteTarget(ivy, tess);
            check("RT-2b invite popup discloses the EM conflict in clear-and-join words", tess.heard("different EliteMobs party") && tess.heard("leaves that EliteMobs party"));
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("RT-2b command accept joins the roster (no refusal)", w.inOurs(ivy, tess) && tess.heard("You joined"));
            check("RT-2b the invite was consumed by the join", !w.r.mgr.hasInvite(ivy.id, tess.id, w.r.env.t));
            check("RT-2b target told they left the other EM party", tess.heard("you left it to join this one"));
            w.polls(2);
            check("RT-2b stale EM membership cleared", !w.em.inEm(xan.id, tess.id));
            check("RT-2b the native party was reverted away", w.em.of(xan.id) == null && w.em.of(wes.id) == null);
            check("RT-2b EM face re-materialised from the roster", w.em.inEm(ivy.id, tess.id));
            check("RT-2b the join carries DOOR origin", w.r.mgr.originOfPlayer(tess.id) == RosterOrigin.DOOR);
            w.rest("RT-2b", ivy, tess);
        }
        // ---------- RT-2c unit layer (0.6.0): the stale-face clear defers while the party is dungeon-frozen and applies on unfreeze (Q8) ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy4"); var tess = w.j("Tess4"); var xan = w.j("Xan4");
            Em.P p = w.em.makeEm(xan.id); w.em.emJoinRaw(tess.id, p);
            w.r.ctl.inviteTarget(ivy, tess);
            w.em.busy.add(tess.id);                     // Tess is inside the frozen instance
            w.r.ctl.acceptFrom(tess, ivy.id);
            check("RT-2c hub accept stands while frozen", w.inOurs(ivy, tess));
            w.poll();
            check("RT-2c stale membership NOT torn mid-freeze (deferred, Q8)", w.em.inEm(xan.id, tess.id));
            w.em.busy.clear();
            w.polls(2);
            check("RT-2c on unfreeze the stale membership is cleared and the face materialises", !w.em.inEm(xan.id, tess.id) && w.em.inEm(ivy.id, tess.id));
            w.rest("RT-2c", ivy, tess);
        }
        // ---------- RT-4 unit layer (0.6.0): decline at the door, then a native EM [ACCEPT] within 60s joins nothing and is reverted ----------
        {
            W w = new W(5);
            var ivy = w.j("Ivy5"); var wes = w.j("Wes5"); var tess = w.j("Tess5");
            w.together(ivy, wes);
            w.poll();
            w.r.ctl.inviteTarget(ivy, tess);            // hub invite + driven EM invite
            w.r.ctl.declineFrom(tess, ivy.id);
            check("RT-4 the decline removed the hub invite", !w.r.mgr.hasInvite(ivy.id, tess.id, w.r.env.t));
            w.em.accept(tess.id);                       // the still-live native EM [ACCEPT], clicked after the decline
            check("RT-4 setup: the native accept really landed in EM", w.em.inEm(ivy.id, tess.id));
            w.poll();
            check("RT-4 no hub join from the native accept", w.r.mgr.partyOf(tess.id) == null && !tess.heard("You joined"));
            check("RT-4 the native membership was reverted", !w.em.inEm(ivy.id, tess.id));
            check("RT-4 Q3 notice told exactly once", tess.said.stream().filter(s -> s.contains("run with /party")).count() == 1);
            check("RT-4 the revert was logged", w.logs.stream().anyMatch(s -> s.contains("reverted a non-door") && s.contains("Tess5")));
            w.em.emJoinRaw(tess.id, w.em.of(ivy.id));   // a second native join in the same session
            w.poll();
            check("RT-4 second native join also reverted", !w.em.inEm(ivy.id, tess.id));
            check("RT-4 second revert logged too (logged every time)", w.logs.stream().filter(s -> s.contains("reverted a non-door") && s.contains("Tess5")).count() == 2);
            check("RT-4 notice still told only once this session", tess.said.stream().filter(s -> s.contains("run with /party")).count() == 1);
            w.rest("RT-4", ivy, wes, tess);
        }
        // ---------- AT-1 regression (0.6.0, rig run 416): a party-less inviter's driven invite materialises their solo EM party; the poll must NOT revert it while the driven invite is pending ----------
        // (On real jars the first EmSync poll reverted the materialised party - "reverted a
        // non-door EliteMobs party change ... no matching /party invite" - and the pending
        // popup invite died with it, breaking one-tap accept. The command-accept path was
        // unaffected. The pre-fix fake could not express this: its invite drive never
        // changed the inviter's EM membership.)
        {
            W w = new W(5);
            var four = w.j("RigFour"); var five = w.j("RigFive");
            w.r.ctl.inviteTarget(four, five);           // party-less inviter: hub invite + driven EM invite
            check("AT-1 the hub invite is live", w.r.mgr.hasInvite(four.id, five.id, w.r.env.t));
            check("AT-1 the drive materialised the inviter's solo EM party", w.em.of(four.id) != null && w.em.of(four.id).m.equals(List.of(four.id)));
            check("AT-1 the driven invite is in the ledger", w.em.inviteDriven(four.id, five.id));
            check("AT-1 the EM pending invite is live for the target", four.id.equals(w.em.pending.get(five.id)));
            w.polls(3);                                 // three mesh polls inside the invite TTL
            check("AT-1 polls do NOT revert the materialised party while the invite is pending", w.em.of(four.id) != null && w.em.of(four.id).m.equals(List.of(four.id)));
            check("AT-1 no revert was logged for the inviter", w.logs.stream().noneMatch(s -> s.contains("reverted a non-door") && s.contains("RigFour")));
            check("AT-1 the inviter got no Q3 revert notice", !four.heard("run with /party"));
            check("AT-1 the pending invite survives the polls", four.id.equals(w.em.pending.get(five.id)));
            check("AT-1 no hub party exists yet (the invite is still just an invite)", w.r.mgr.partyOf(four.id) == null && w.r.mgr.partyOf(five.id) == null);
            w.em.accept(five.id);                       // the one-tap popup answer AT-1 protects
            w.poll();
            check("AT-1 the popup accept completes the hub party", w.inOurs(four, five) && five.heard("You joined"));
            check("AT-1 both faces agree after the accept", w.em.inEm(four.id, five.id));
            w.rest("AT-1", four, five);
        }
        // ---------- AT-1b: the spare dies with the hub invite - a decline re-arms the revert ----------
        {
            W w = new W(5);
            var four = w.j("RigFourB"); var five = w.j("RigFiveB");
            w.r.ctl.inviteTarget(four, five);
            check("AT-1b setup: the solo party materialised", w.em.of(four.id) != null);
            w.r.ctl.declineFrom(five, four.id);
            check("AT-1b the decline removed the hub invite", !w.r.mgr.hasInvite(four.id, five.id, w.r.env.t));
            w.poll();
            check("AT-1b after the decline the materialised party is reverted", w.em.of(four.id) == null);
            check("AT-1b the post-decline revert was logged", w.logs.stream().anyMatch(s -> s.contains("reverted a non-door") && s.contains("RigFourB")));
            w.rest("AT-1b", four, five);
        }
        // ---------- AT-1c: a hub invite alone spares nothing - with no driven-invite ledger entry the materialisation-shaped party still reverts ----------
        {
            W w = new W(5);
            var four = w.j("RigFourC"); var five = w.j("RigFiveC");
            w.r.ctl.inviteTarget(four, five);
            check("AT-1c setup: the solo party materialised", w.em.of(four.id) != null);
            w.em.ledger.remove(five.id);                // the EM drive's ledger half is gone (e.g. ledger TTL lapsed first)
            check("AT-1c setup: hub invite live but no ledger entry", w.r.mgr.hasInvite(four.id, five.id, w.r.env.t) && !w.em.inviteDriven(four.id, five.id));
            w.poll();
            check("AT-1c without the ledger entry the party is reverted", w.em.of(four.id) == null);
            w.rest("AT-1c", four, five);
        }
        // ---------- NP-1 (run 426, one-roster rebuild part 1 of 3): the pending read sees BOTH doors' pending invites, and tells them apart by the ledger ----------
        // (SCOPING.md §5 part 1: the mesh leg gains a pending-invite query + the fake gains the
        // raw native-pending state. Part 1 changes NO reconcile behaviour: NP-1f pins the M4
        // desk shape still reverting, which is exactly what part 2's native-pending spare flips.)
        {
            W w = new W(5);
            var ivy = w.j("IvyNP"); var tess = w.j("TessNP");
            check("NP-1a no invite anywhere: the pending read is false for both players", !w.em.hasPendingInvite(ivy.id) && !w.em.hasPendingInvite(tess.id));
            check("NP-1a the absent-EM link (EmLink.NONE) reads no pending invite", !EmLink.NONE.hasPendingInvite(tess.id));
            w.em.invite(ivy.id, tess.id);               // the driven (adapter) invite, /party door
            check("NP-1b a driven invite reads as pending for the target", w.em.hasPendingInvite(tess.id));
            check("NP-1b a driven invite is also in the ledger (the two halves agree)", w.em.inviteDriven(ivy.id, tess.id));
            w.em.accept(tess.id);
            check("NP-1c an accepted invite consumes the pending slot: the read is false again", !w.em.hasPendingInvite(tess.id));
        }
        {
            W w = new W(5);
            var nat = w.j("NatNP"); var uma = w.j("UmaNP");
            w.em.emInviteRaw(nat.id, uma.id);           // a native /em party invite: pending slot, NO ledger entry
            check("NP-1d a native invite reads as pending for the target (the read the mesh was missing)", w.em.hasPendingInvite(uma.id));
            check("NP-1d a native invite has NO ledger entry - pending + no-ledger is the native shape", !w.em.inviteDriven(nat.id, uma.id));
            check("NP-1d the native invite materialised the inviter's solo EM party, like the driven one", w.em.of(nat.id) != null && w.em.of(nat.id).m.equals(List.of(nat.id)));
            check("NP-1e the inviter themself holds no pending invite", !w.em.hasPendingInvite(nat.id));
            check("NP-1e no hub invite exists behind a native invite", !w.r.mgr.hasInvite(nat.id, uma.id, w.r.env.t) && w.r.mgr.invitesFor(uma.id, w.r.env.t).isEmpty());
            w.polls(2);
            // RE-PINNED by run 426 parts 2+3 (D88): part 1 alone pinned this baseline as
            // still-reverting; the part-2 native-pending spare now flips exactly this
            // shape - the materialisation is SPARED while the native pending lives, and
            // the spare records the pair (D91). The full lifecycle is NP-2 below.
            check("NP-1f parts 2+3: the native materialisation is spared while the native pending lives", w.em.of(nat.id) != null && w.em.of(nat.id).m.equals(List.of(nat.id)));
            check("NP-1f parts 2+3: no revert was logged for the native inviter", w.logs.stream().noneMatch(s -> s.contains("reverted a non-door") && s.contains("NatNP")));
            check("NP-1f parts 2+3: the spare recorded the native pair in the observation map", w.r.ctl.emSync().nativeObserved(uma.id));
        }
        // ---------- NP-2 (run 426, one-roster rebuild parts 2+3, D88-D93): the native-pending lifecycle ----------
        // (SCOPING.md L311-317 desk gate: spare survives >=3 polls; native accept completes
        // the roster and both faces agree; pending expiry/decline re-arms the revert;
        // crossed-pending does not complete - plus the D92 named scenarios: hub-refusal
        // fallback, suppression ordering, dungeon-busy deferral, and the BP-6 fake pin.)
        {
            // NP-2a: the spare survives >=3 polls; nothing is created, driven, or told.
            W w = new W(5);
            var nat = w.j("Nat2A"); var uma = w.j("Uma2A");
            w.em.emInviteRaw(nat.id, uma.id);
            w.polls(3);
            check("NP-2a spare survives >=3 polls: the inviter's solo party still stands", w.em.of(nat.id) != null && w.em.of(nat.id).m.equals(List.of(nat.id)));
            check("NP-2a the pending invite survives the polls", w.em.hasPendingInvite(uma.id));
            check("NP-2a no roster was created by the spare", w.r.mgr.partyOf(nat.id) == null && w.r.mgr.partyOf(uma.id) == null);
            check("NP-2a no revert was logged and no Q3 notice was told", w.logs.stream().noneMatch(s -> s.contains("reverted a non-door")) && !nat.heard("run with /party"));
            check("NP-2a the pair stands in the observation map", w.r.ctl.emSync().nativeObserved(uma.id));
            check("NP-2a the spare drove nothing", w.em.drives.isEmpty());
            w.rest("NP-2a", nat, uma);
        }
        {
            // NP-2b: the native accept completes the roster through the door path; both faces agree.
            W w = new W(5);
            var nat = w.j("Nat2B"); var uma = w.j("Uma2B");
            w.em.emInviteRaw(nat.id, uma.id);
            w.poll();                                   // the spare observes the live pair
            check("NP-2b setup: spared and observed", w.em.of(nat.id) != null && w.r.ctl.emSync().nativeObserved(uma.id));
            w.em.accept(uma.id);                        // the target answers at the EM door (EM's own action; the pending slot is consumed)
            check("NP-2b setup: the native accept landed in EM and consumed the pending slot", w.em.inEm(nat.id, uma.id) && !w.em.hasPendingInvite(uma.id));
            w.poll();
            check("NP-2b the native answer completed the roster", w.inOurs(nat, uma) && uma.heard("You joined"));
            check("NP-2b the completion carries DOOR origin (D90)", w.r.mgr.originOfPlayer(uma.id) == RosterOrigin.DOOR);
            check("NP-2b both faces agree on the roster", w.em.inEm(nat.id, uma.id) && w.em.of(nat.id).m.size() == 2);
            check("NP-2b the observation was spent by the completion", !w.r.ctl.emSync().nativeObserved(uma.id));
            check("NP-2b no revert was logged for either player", w.logs.stream().noneMatch(s -> s.contains("reverted a non-door")));
            w.rest("NP-2b", nat, uma);
        }
        {
            // NP-2c (expiry): the pending invite times out unanswered - the spare dies with it and the revert re-arms.
            W w = new W(5);
            var nat = w.j("Nat2C"); var uma = w.j("Uma2C");
            w.em.emInviteRaw(nat.id, uma.id);
            w.polls(2);
            check("NP-2c setup: spared while the pending lives", w.em.of(nat.id) != null);
            w.em.emPendingGoneRaw(uma.id);              // EM's own 60s invite timeout
            w.poll();
            check("NP-2c after expiry the materialised party is reverted", w.em.of(nat.id) == null);
            check("NP-2c the post-expiry revert was logged for the inviter", w.logs.stream().anyMatch(s -> s.contains("reverted a non-door") && s.contains("Nat2C")));
            check("NP-2c no roster was ever created", w.r.mgr.partyOf(nat.id) == null && w.r.mgr.partyOf(uma.id) == null);
            w.rest("NP-2c", nat, uma);
        }
        {
            // NP-2c2 (decline): the target declines natively - same re-arm as expiry.
            W w = new W(5);
            var nat = w.j("Nat2D"); var uma = w.j("Uma2D");
            w.em.emInviteRaw(nat.id, uma.id);
            w.poll();
            check("NP-2c2 setup: spared while the pending lives", w.em.of(nat.id) != null);
            w.em.emPendingGoneRaw(uma.id);              // a native decline: the slot dies, nobody joins
            w.poll();
            check("NP-2c2 after the native decline the materialised party is reverted", w.em.of(nat.id) == null);
            check("NP-2c2 no roster was created by the declined invite", w.r.mgr.partyOf(nat.id) == null && w.r.mgr.partyOf(uma.id) == null);
            w.rest("NP-2c2", nat, uma);
        }
        {
            // NP-2d (crossed): two native pairs stand at once - enumeration pairs NOTHING (D88), both revert as today.
            W w = new W(5);
            var n1 = w.j("Nat2E1"); var u1 = w.j("Uma2E1"); var n2 = w.j("Nat2E2"); var u2 = w.j("Uma2E2");
            w.em.emInviteRaw(n1.id, u1.id);
            w.em.emInviteRaw(n2.id, u2.id);
            w.polls(2);
            check("NP-2d crossed pendings pair nothing: no observation was recorded", !w.r.ctl.emSync().nativeObserved(u1.id) && !w.r.ctl.emSync().nativeObserved(u2.id));
            check("NP-2d crossed pendings complete nothing: no roster exists", w.r.mgr.partyOf(n1.id) == null && w.r.mgr.partyOf(u1.id) == null && w.r.mgr.partyOf(n2.id) == null && w.r.mgr.partyOf(u2.id) == null);
            check("NP-2d crossed materialisations revert as today", w.em.of(n1.id) == null && w.em.of(n2.id) == null);
            check("NP-2d both reverts were logged", w.logs.stream().filter(s -> s.contains("reverted a non-door")).count() == 2);
            w.rest("NP-2d", n1, u1, n2, u2);
        }
        {
            // NP-2e (D92(1)): a native pending crossed with a live HUB invite to the same target is never paired or completed blindly.
            W w = new W(5);
            var nat = w.j("Nat2F"); var uma = w.j("Uma2F"); var hub = w.j("Hub2F");
            w.em.emInviteRaw(nat.id, uma.id);           // native pending nat -> uma
            w.r.ctl.inviteTarget(hub, uma);             // hub invite hub -> uma (driven into EM too: the target slot now answers the drive)
            w.polls(2);
            check("NP-2e the crossed target was never observed as a native pair", !w.r.ctl.emSync().nativeObserved(uma.id));
            check("NP-2e the native inviter was not spared through the crossed picture", w.em.of(nat.id) == null);
            check("NP-2e the hub invite itself is untouched by the native side", w.r.mgr.hasInvite(hub.id, uma.id, w.r.env.t));
            check("NP-2e no roster was completed from the crossed picture", w.r.mgr.partyOf(uma.id) == null);
        }
        {
            // NP-2f (D92(2), named desk scenario): the hub side refuses the completion
            // (inviter offline at accept time) - the door wording fires, no join lands,
            // and the formed EM party falls back to the ordinary next-poll revert.
            W w = new W(5);
            var nat = w.j("Nat2G"); var uma = w.j("Uma2G");
            w.em.emInviteRaw(nat.id, uma.id);
            w.poll();
            check("NP-2f setup: spared and observed", w.r.ctl.emSync().nativeObserved(uma.id));
            w.em.accept(uma.id);                        // the native answer lands in EM
            w.r.env.on.remove(nat.id); w.r.mgr.onQuit(nat.id, w.r.env.t);   // the inviter goes offline before the completing poll
            w.poll();
            check("NP-2f the hub refusal worded itself (inviter offline)", uma.heard("not online anymore"));
            check("NP-2f no roster join landed from the refused completion", w.r.mgr.partyOf(uma.id) == null);
            check("NP-2f the observation was spent by the refused attempt", !w.r.ctl.emSync().nativeObserved(uma.id));
            w.poll();
            check("NP-2f fallback: the formed EM party is reverted on the next poll (B1 shape)", w.em.of(uma.id) == null);
            check("NP-2f the fallback revert was logged", w.logs.stream().anyMatch(s -> s.contains("reverted a non-door") && s.contains("Uma2G")));
        }
        {
            // NP-2g (D92(3)): a suppressed player must not re-enter via a native pending -
            // the completion check runs after the suppression hold.
            W w = new W(5);
            var nat = w.j("Nat2H"); var uma = w.j("Uma2H");
            w.em.emInviteRaw(nat.id, uma.id);
            w.poll();
            check("NP-2g setup: spared and observed", w.r.ctl.emSync().nativeObserved(uma.id));
            w.r.ctl.emSync().suppress(uma.id, Set.of(nat.id, uma.id));   // Together removed Uma on purpose (roster hint = the pair)
            w.em.accept(uma.id);                        // the native answer lands anyway
            w.polls(2);
            check("NP-2g the suppressed target never completed into the roster", w.r.mgr.partyOf(uma.id) == null && !uma.heard("You joined"));
            check("NP-2g the suppression was enforced on the EM face", !w.em.inEm(nat.id, uma.id));
        }
        {
            // NP-2h (D92(4)): a dungeon-busy completion defers through the deferred queue and lands on unfreeze.
            W w = new W(5);
            var nat = w.j("Nat2I"); var uma = w.j("Uma2I");
            w.em.emInviteRaw(nat.id, uma.id);
            w.poll();
            check("NP-2h setup: spared and observed", w.r.ctl.emSync().nativeObserved(uma.id));
            w.em.accept(uma.id);
            w.em.busy.add(uma.id);                      // the formed EM party is dungeon-frozen
            w.poll();
            check("NP-2h frozen: the completion did NOT land mid-freeze", w.r.mgr.partyOf(uma.id) == null);
            check("NP-2h frozen: the EM party was not torn either", w.em.inEm(nat.id, uma.id));
            w.em.busy.clear();
            w.polls(2);
            check("NP-2h on unfreeze the deferred completion lands", w.inOurs(nat, uma) && uma.heard("You joined"));
            check("NP-2h both faces agree after the deferred completion", w.em.inEm(nat.id, uma.id));
            w.rest("NP-2h", nat, uma);
        }
        {
            // NP-2i (BP-6 pin, D93): when the inviter's materialised party is dismantled,
            // the fake's target-keyed pending entry dies with it - real EM behaviour.
            W w = new W(5);
            var nat = w.j("Nat2J"); var uma = w.j("Uma2J");
            w.em.emInviteRaw(nat.id, uma.id);
            check("NP-2i setup: the native pending is live", w.em.hasPendingInvite(uma.id));
            w.em.emLeaveRaw(nat.id);                    // the inviter's party is dismantled (the revert's leave, raw)
            check("NP-2i the pending entry died with the inviter's party", !w.em.hasPendingInvite(uma.id));
        }
        // ---------- Converge drift (0.6.0): a stranger added to the EM twin natively is reverted out; the roster is untouched ----------
        {
            W w = new W(5);
            var l = w.j("Lou"); var a = w.j("Ada"); var s = w.j("Sid");
            w.together(l, a);
            w.poll();
            check("CD setup converged", w.em.inEm(l.id, a.id));
            w.em.emJoinRaw(s.id, w.em.of(l.id));        // Sid joins the twin at the EM door, no hub invite
            w.poll();
            check("CD stranger reverted out of the EM face", !w.em.inEm(l.id, s.id) && w.em.inEm(l.id, a.id));
            check("CD stranger never entered the roster", w.r.mgr.partyOf(s.id) == null && w.inOurs(l, a));
            check("CD stranger got the Q3 revert notice", s.heard("run with /party"));
            w.rest("CD", l, a, s);
        }
    }
}
