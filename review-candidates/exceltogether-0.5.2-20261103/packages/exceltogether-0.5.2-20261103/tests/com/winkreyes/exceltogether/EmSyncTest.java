package com.winkreyes.exceltogether;

import java.util.*;

/**
 * DESIGN §11B/§11C/§11D for the party mesh: EmSync against a scripted fake EliteMobs (state +
 * drive-call recorder), McmmoSyncTest-style. §11C idempotence is woven in: after every scenario
 * converges, {@code rest(...)} runs two more polls and asserts zero drives and zero messages.
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
            P mine = of(inviter); if (mine == null) return EmMirror.NOT_READY;
            P theirs = of(target);
            if (theirs != null && theirs != mine) return EmMirror.TARGET_IN_OTHER_PARTY;
            if (mine.m.size() >= 5) return EmMirror.FULL;
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
        public boolean inviteDriven(UUID inv, UUID tgt) { return inv.equals(pending.get(tgt)); }
        // raw EM-door state edits (what EM itself does when players use /em party)
        P makeEm(UUID leader) { P p = new P(UUID.randomUUID(), leader); parties.put(p.key, p); return p; }
        void emJoinRaw(UUID id, P p) { if (!p.m.contains(id)) p.m.add(id); }
        void emLeaveRaw(UUID id) {
            P p = of(id); if (p == null) return;
            p.m.remove(id); pending.remove(id);
            if (p.m.isEmpty()) parties.remove(p.key);
            else if (p.leader.equals(id)) p.leader = p.m.get(0);
        }
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
        // ---------- B1: EM-only party of 3 is adopted into Together, zero EM drives ----------
        {
            W w = new W(5);
            var a = w.j("Ann"); var b = w.j("Bob"); var c = w.j("Cy");
            Em.P p = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, p); w.em.emJoinRaw(c.id, p);
            w.poll();
            check("B1 EM group adopted into Together", w.inOurs(a, b) && w.inOurs(a, c));
            check("B1 EM leader is the Together leader", w.r.mgr.partyOf(b.id).leader.equals(a.id));
            check("B1 zero EmLink drives", w.em.drives.isEmpty());
            check("B1 adopted players marked mirrored", w.r.ctl.emSync().isMirrored(a.id) && w.r.ctl.emSync().isMirrored(c.id));
            check("B1 adoption tell names EliteMobs", b.heard("because you are in the same EliteMobs party"));
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
        // ---------- B3: EM partyKey changes under a meshed player: they move, old party hears it ----------
        {
            W w = new W(5);
            var x = w.j("Xan"); var a = w.j("Ana"); var b = w.j("Bea"); var c = w.j("Cal");
            w.together(x, a);
            w.poll();                                   // twin formed, X mirrored to its key
            check("B3 setup converged", w.em.inEm(x.id, a.id));
            w.em.emLeaveRaw(x.id);                      // X leaves the twin at the EM door...
            Em.P p2 = w.em.makeEm(b.id); w.em.emJoinRaw(x.id, p2); w.em.emJoinRaw(c.id, p2);   // ...and joins a different EM party
            int drivesBefore = w.em.drives.size();
            w.poll();
            check("B3 X left the old Together party", !w.inOurs(x, a));
            check("B3 X adopted with the new EM group", w.inOurs(x, b) && w.inOurs(x, c));
            check("B3 the move drove nothing into EM", w.em.drives.size() == drivesBefore);
            check("B3 X was told why", x.heard("moved to another EliteMobs party"));
            w.rest("B3", x, a, b, c);
        }
        // ---------- B4: static disjoint groups, never mirrored: no yank, one kept-note ----------
        {
            W w = new W(5);
            var x = w.j("Xiu"); var a = w.j("Ali"); var b = w.j("Bo"); var c = w.j("Cy2");
            w.together(x, a);
            Em.P p = w.em.makeEm(b.id); w.em.emJoinRaw(x.id, p); w.em.emJoinRaw(c.id, p);
            w.polls(2);
            check("B4 X was not yanked out of the Together party", w.inOurs(x, a));
            long notes = x.said.stream().filter(s -> s.contains("different EliteMobs party")).count();
            check("B4 kept-note exactly once", notes == 1);
            check("B4 no leave was driven for X", w.em.drives.stream().noneMatch(d -> d.equals("leave:Xiu")));
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
            check("B5 a fresh EM join after suppression adopts", w.inOurs(x, d));
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
        // ---------- B8: two full different parties sharing one member: no churn ----------
        {
            W w = new W(5);
            var s = w.j("Sam");
            PartyFeatureTest.FP[] t = {w.j("T1"), w.j("T2"), w.j("T3"), w.j("T4")};
            PartyFeatureTest.FP[] e = {w.j("E1"), w.j("E2"), w.j("E3"), w.j("E4")};
            w.together(s, t[0], t[1], t[2], t[3]);
            Em.P p = w.em.makeEm(s.id);
            for (var m : e) w.em.emJoinRaw(m.id, p);
            w.polls(3);
            check("B8 no drive calls at all", w.em.drives.isEmpty());
            check("B8 Together party unchanged", w.r.mgr.partyOf(s.id).size() == 5 && w.inOurs(s, t[3]));
            check("B8 EM party unchanged", w.em.of(s.id).m.size() == 5);
            long notes = s.said.stream().filter(x -> x.contains("different EliteMobs party")).count();
            check("B8 kept-note once for the shared member", notes == 1);
            check("B8 the EM-only members wait instead of churning", w.r.ctl.emSync().isWaiting(e[0].id));
            w.rest("B8", s, t[0], e[0]);
        }
        // ---------- B9: leader cases ----------
        {
            // L1 at the EM door: the EM creator leads in Together too
            W w = new W(5);
            var a = w.j("Ara"); var b = w.j("Bas");
            Em.P p = w.em.makeEm(b.id); w.em.emJoinRaw(a.id, p);
            w.poll();
            check("B9 L1 EM-door creator leads Together", w.r.mgr.partyOf(a.id).leader.equals(b.id));
            w.rest("B9a", a, b);
        }
        {
            // L4 + L3 (reform off): the EM leader leaves EM only; the record stands, EM keeps its new leader, member is driven back in
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
            check("B9 L4 Together record unchanged", w.r.mgr.partyOf(a.id).leader.equals(l.id) && w.inOurs(l, a) && w.inOurs(l, b));
            check("B9 L4 L is driven back into EM", w.em.inEm(a.id, l.id));
            check("B9 L3 EM keeps its promoted leader (no reform)", twin.leader.equals(a.id));
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
        // ---------- B6 / §6 + §11D: an mcMMO party of 7 — 5 mesh, 2 wait, seat frees in order ----------
        {
            PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
            McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc();
            r.ctl.setLink(mc);
            W w = new W(5);
            // reuse the same rig for both fakes
            r.ctl.setEmLink(w.em);
            PartyFeatureTest.FP[] ps = new PartyFeatureTest.FP[7];
            for (int i = 0; i < 7; i++) { ps[i] = r.java("P" + i); mc.names.put(ps[i].id, "P" + i); w.em.names.put(ps[i].id, "P" + i); }
            McmmoSyncTest.Mc.P mp = mc.make("seven", ps[0].id);
            for (int i = 1; i < 7; i++) mc.join(ps[i].id, "seven");
            List<UUID> all = new ArrayList<>(); for (PartyFeatureTest.FP p : ps) all.add(p.id);
            r.ctl.mcmmoWatch(all);                          // the login path: their mcMMO party is the record
            for (int i = 0; i < 4; i++) { r.env.t += 2000; r.ctl.tick(); }
            check("B6 five of seven are in the Together party", r.mgr.partyOf(ps[0].id).size() == 5);
            check("B6 the other two wait (mcMMO side)", ps[6].said.stream().anyMatch(s -> s.contains("ExcelTogether party is full")));
            check("B6 the five are materialised in EM", w.em.of(ps[0].id) != null && w.em.of(ps[0].id).m.size() == 5);
            r.ctl.command(ps[0], new String[]{"list"});
            r.ctl.command(ps[4], new String[]{"leave", "yes"});
            for (int i = 0; i < 4; i++) { r.env.t += 2000; r.ctl.tick(); }
            check("B6 freed seat adopts the first waiter", r.mgr.sameParty(ps[0].id, ps[5].id));
            check("B6 the adopted waiter joins EM too", w.em.inEm(ps[0].id, ps[5].id));
            check("B6 the last waiter still waits", !r.mgr.sameParty(ps[0].id, ps[6].id));
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
            W w = new W(5);
            var a = w.j("A1"); var b = w.j("B1"); var c = w.j("C1"); var d = w.j("D1");
            Em.P p1 = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, p1);
            Em.P p2 = w.em.makeEm(c.id); w.em.emJoinRaw(d.id, p2);
            w.poll();
            check("D2 two EM parties adopt into two Together parties", w.inOurs(a, b) && w.inOurs(c, d) && !w.inOurs(a, c));
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
    }
}
