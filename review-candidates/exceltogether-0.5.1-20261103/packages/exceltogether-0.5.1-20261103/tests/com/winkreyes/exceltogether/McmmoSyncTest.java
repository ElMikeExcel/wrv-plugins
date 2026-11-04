package com.winkreyes.exceltogether;

import java.util.*;
import java.util.function.Predicate;

/** WRV-063 0.2.1: hand-made mcMMO party changes are mirrored into ExcelTogether parties (no mcMMO classes needed). */
public class McmmoSyncTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** An in-memory mcMMO: parties, a flag for profiles not loaded, and mirrors of the real link's calls. */
    static final class Mc implements McmmoLink {
        static final class P { String name; UUID leader; final LinkedHashMap<UUID, String> m = new LinkedHashMap<>(); }
        final Map<String, P> parties = new LinkedHashMap<>();
        final Map<UUID, String> linked = new HashMap<>();
        final Set<UUID> unknown = new HashSet<>();
        final List<String> calls = new ArrayList<>();
        final Map<UUID, String> names = new HashMap<>();
        boolean stuck = false, boom = false;
        public boolean available() { return true; }
        P of(UUID id) { for (P p : parties.values()) if (p.m.containsKey(id)) return p; return null; }
        McParty view(P p) { return p == null ? McParty.NONE : new McParty(p.name, p.leader, p.m); }
        public McParty partyOf(UUID id) { if (boom) throw new IllegalStateException("x"); return unknown.contains(id) ? McParty.UNKNOWN : view(of(id)); }
        public McParty partyByName(String n) { return view(parties.get(n)); }
        public void adopted(UUID id, String n) { linked.put(id, n); calls.add("adopted"); }
        public Mirror invite(UUID a, UUID b, String h) { calls.add("invite"); P p = of(a); if (p == null) p = make(h, a); return Mirror.OK; }
        public Mirror accept(UUID t, UUID i, String h) { calls.add("accept"); P p = of(i); if (p == null) p = make(h, i); join(t, p.name); linked.put(t, p.name); linked.put(i, p.name); return Mirror.OK; }
        public void cancel(UUID a, UUID b) { calls.add("cancel"); }
        public void leave(UUID id) {
            calls.add("leave"); String n = linked.remove(id); if (n == null || stuck) return;
            P p = of(id); if (p != null && p.name.equals(n)) quit(id);
        }
        public void cleanup(Predicate<UUID> x) { calls.add("cleanup"); }
        long count(String c) { return calls.stream().filter(c::equals).count(); }
        // raw mcMMO state edits (no events)
        P make(String name, UUID leader) { P p = new P(); p.name = name; p.leader = leader; p.m.put(leader, names.getOrDefault(leader, "x")); parties.put(name, p); return p; }
        void join(UUID id, String name) { P p = parties.get(name); p.m.put(id, names.getOrDefault(id, "x")); }
        void quit(UUID id) {
            P p = of(id); if (p == null) return; p.m.remove(id);
            if (p.m.isEmpty()) parties.remove(p.name); else if (p.leader.equals(id)) p.leader = p.m.keySet().iterator().next();
        }
    }

    static final class W {
        final PartyFeatureTest.Rig r; final Mc mc = new Mc();
        W(int cap) { r = new PartyFeatureTest.Rig(cap); r.ctl.setLink(mc); }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); mc.names.put(p.id, n); return p; }
        PartyFeatureTest.FP b(String n) { PartyFeatureTest.FP p = r.bedrock(n); mc.names.put(p.id, n); return p; }
        /** what the plugin does: collect before the change, apply, reconcile a tick later */
        void hand(UUID who, String oldP, String newP, Runnable change) {
            List<UUID> ids = r.ctl.mcmmoEventIds(who, oldP, newP);
            change.run();
            r.ctl.mcmmoChanged(ids, newP != null ? List.of(who) : List.of());
        }
        void handJoin(UUID who, String party) { hand(who, null, party, () -> mc.join(who, party)); }
        void handQuit(UUID who) { P0 p = new P0(mc.of(who)); hand(who, p.name, null, () -> mc.quit(who)); }
        void handKick(UUID who) { handQuit(who); }
        void handDisband(UUID leader) {
            Mc.P p = mc.of(leader); String n = p.name; List<UUID> all = new ArrayList<>(p.m.keySet());
            hand(leader, n, null, () -> { for (UUID u : all) mc.quit(u); });
        }
        /** ours party by hand: invite + accept through our own commands, mirrored to mc by the fake link */
        void ourParty(PartyFeatureTest.FP lead, PartyFeatureTest.FP... others) {
            for (PartyFeatureTest.FP o : others) { r.ctl.command(lead, new String[]{"invite", o.real}); r.ctl.command(o, new String[]{"accept"}); }
        }
        boolean inOurs(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        void polls(int n) { for (int i = 0; i < n; i++) { r.env.t += 1000; r.ctl.tick(); } }
    }
    record P0(String name) { P0(Mc.P p) { this(p == null ? null : p.name); } }

    static void tests() {
        // 1) hand join into a party that is already linked
        W w = new W(5); var a = w.j("Ann"); var b = w.j("Bob"); var c = w.j("Cy");
        w.ourParty(a, b);
        long inv = w.mc.count("invite"), acc = w.mc.count("accept");
        w.polls(6);
        check("our own mirrored party: sync adds nothing, calls nothing", w.r.mgr.partyOf(a.id).size() == 2 && w.mc.count("invite") == inv && w.mc.count("accept") == acc);
        String pn = w.mc.of(a.id).name;
        w.handJoin(c.id, pn);
        check("/party accept by hand: joiner lands in the ExcelTogether party", w.inOurs(a, c) && w.r.mgr.partyOf(a.id).size() == 3);
        check("joiner and party are told", c.heard("added to the ExcelTogether party") && a.heard("Cy joined the party (3/5)"));
        check("hand join never calls the link's invite/accept/leave", w.mc.count("invite") == inv && w.mc.count("accept") == acc && w.mc.count("leave") == 0);
        w.polls(10);
        check("polling after a hand join changes nothing more", w.r.mgr.partyOf(a.id).size() == 3);
        // our leave later also removes the hand joiner from mcMMO (link was told by adopted)
        w.r.ctl.command(c, new String[]{"leave", "yes"});
        check("our leave takes a hand joiner out of mcMMO too", w.mc.of(c.id) == null && !w.r.mgr.inParty(c.id));
        w.polls(10);
        check("no feedback: leaver is not pulled back", !w.r.mgr.inParty(c.id) && w.mc.of(c.id) == null);

        // 2) stuck mcMMO membership after OUR leave must not re-adopt
        W s = new W(5); PartyFeatureTest.FP s1 = s.j("Sia"), s2 = s.j("Tod"), s3 = s.j("Uli");
        s.ourParty(s1, s2, s3); s.mc.stuck = true;
        s.r.ctl.command(s3, new String[]{"leave", "yes"});
        s.polls(10); s.r.ctl.mcmmoChanged(List.of(s3.id), List.of());
        check("leave of ours while mcMMO keeps them: not re-adopted by poll or event", !s.r.mgr.inParty(s3.id));
        s.handQuit(s3.id);
        check("then they quit mcMMO: still out, suppression ends", !s.r.mgr.inParty(s3.id) && !s.r.mgr.partyOf(s1.id).members.contains(s3.id));
        s.handJoin(s3.id, s.mc.of(s1.id).name);
        check("a fresh hand join after that is adopted again", s.inOurs(s1, s3));

        // 3) quit / kick / disband by hand
        W q = new W(5); PartyFeatureTest.FP q1 = q.j("Quo"), q2 = q.j("Rex"), q3 = q.j("Sky");
        q.ourParty(q1, q2, q3);
        q.handQuit(q3.id);
        check("/party quit by hand: removed from ours, others stay", !q.r.mgr.inParty(q3.id) && q.inOurs(q1, q2) && q3.heard("left the ExcelTogether party"));
        check("others told", q1.heard("left the mcMMO party and so left the party"));
        q.handKick(q2.id);
        check("kick by hand of the second last: ours ends cleanly", !q.r.mgr.inParty(q1.id) && !q.r.mgr.inParty(q2.id));
        check("the one left over is told the party ended", q1.heard("only one member is left"));
        W d = new W(5); PartyFeatureTest.FP d1 = d.j("Dan"), d2 = d.j("Eda"), d3 = d.j("Fox");
        d.ourParty(d1, d2, d3); d.handDisband(d1.id);
        check("/party disband by hand dissolves ours", !d.r.mgr.inParty(d1.id) && !d.r.mgr.inParty(d2.id) && !d.r.mgr.inParty(d3.id));
        d.polls(5);
        check("and stays dissolved", d.r.mgr.allMembers().isEmpty());

        // 4) mcMMO-only party (nothing in ours): leader invite + accept all by hand
        W h = new W(5); PartyFeatureTest.FP h1 = h.j("Hal"), h2 = h.j("Ivy"), h3 = h.j("Jon");
        h.mc.make("Halls", h1.id);
        h.handJoin(h2.id, "Halls");
        check("mcMMO-only party: ours is created with the mcMMO leader as leader", h.inOurs(h1, h2) && h.r.mgr.partyOf(h1.id).leader.equals(h1.id));
        check("both told it was created/added", h1.heard("also made an ExcelTogether party") && h2.heard("added to the ExcelTogether party"));
        h.handJoin(h3.id, "Halls");
        check("third joins the same party", h.r.mgr.partyOf(h1.id).size() == 3 && h.inOurs(h1, h3));
        // existing 3-person mcMMO party with nobody in ours: one reconcile builds it for everyone online
        W t = new W(5); PartyFeatureTest.FP t1 = t.j("Kim"), t2 = t.j("Lou"), t3 = t.j("Max");
        t.mc.make("Trio", t1.id); t.mc.join(t2.id, "Trio"); t.mc.join(t3.id, "Trio");
        t.r.ctl.mcmmoChanged(List.of(t3.id), List.of(t3.id));
        check("whole mcMMO party mirrored in one go", t.r.mgr.partyOf(t1.id) != null && t.r.mgr.partyOf(t1.id).size() == 3);
        // a one-person mcMMO party: nothing to mirror
        W o = new W(5); var o1 = o.j("Neo"); o.mc.make("Solo", o1.id);
        o.r.ctl.mcmmoChanged(List.of(o1.id), List.of(o1.id));
        check("solo mcMMO party is not mirrored", !o.r.mgr.inParty(o1.id));

        // 5) full party (cap 5): extra mcMMO member stays in mcMMO, one note
        W f = new W(5); var fm = new ArrayList<PartyFeatureTest.FP>();
        for (int i = 0; i < 5; i++) fm.add(f.j("Mem" + i));
        f.ourParty(fm.get(0), fm.get(1), fm.get(2), fm.get(3), fm.get(4));
        var extra = f.j("Extra");
        String fp = f.mc.of(fm.get(0).id).name;
        f.handJoin(extra.id, fp);
        check("full: joiner is NOT in ours, still in mcMMO", !f.r.mgr.inParty(extra.id) && f.mc.of(extra.id) != null && f.r.mgr.partyOf(fm.get(0).id).size() == 5);
        check("full: exactly one plain note", extra.all().split("is full", -1).length - 1 == 1 && extra.heard("stay in mcMMO only"));
        f.r.ctl.mcmmoChanged(List.of(extra.id), List.of(extra.id)); f.polls(10);
        check("full: repeated passes do not repeat the note", extra.all().split("is full", -1).length - 1 == 1);
        check("full: our party unchanged at 5", f.r.mgr.partyOf(fm.get(0).id).size() == 5);
        // build from scratch with more mcMMO members online than the cap
        W big = new W(3); var g = new ArrayList<PartyFeatureTest.FP>();
        for (int i = 0; i < 5; i++) g.add(big.j("G" + i));
        big.mc.make("Big", g.get(0).id); for (int i = 1; i < 5; i++) big.mc.join(g.get(i).id, "Big");
        big.r.ctl.mcmmoChanged(List.of(g.get(4).id), List.of(g.get(4).id));
        check("cap 3: only 3 get in, mcMMO leader leads", big.r.mgr.partyOf(g.get(0).id).size() == 3 && big.r.mgr.partyOf(g.get(0).id).leader.equals(g.get(0).id));
        long noted = g.stream().filter(x -> x.heard("is full")).count();
        check("cap 3: the two who did not fit got a note and stay out", noted == 2 && g.stream().filter(x -> big.r.mgr.inParty(x.id)).count() == 3);

        // 6) leader changes
        W l = new W(5); PartyFeatureTest.FP l1 = l.j("Pax"), l2 = l.j("Quin"), l3 = l.j("Rue");
        l.ourParty(l1, l2, l3);
        l.mc.of(l1.id).leader = l2.id; l.polls(4);
        check("/party owner by hand: ours leader follows", l.r.mgr.partyOf(l1.id).leader.equals(l2.id) && l2.heard("You are now the party leader") && l1.heard("Quin is now the party leader"));
        l.polls(10);
        check("no flapping", l.r.mgr.partyOf(l1.id).leader.equals(l2.id));
        // our leader hands off when the leader leaves; mcMMO may pick someone else; mcMMO wins, no loop
        l.r.ctl.command(l2, new String[]{"leave", "yes"}); l.polls(10);
        check("after leader left ours: two left, leader is a current member", l.r.mgr.partyOf(l1.id).size() == 2 && l.r.mgr.partyOf(l1.id).members.contains(l.r.mgr.partyOf(l1.id).leader));
        // leader leaving mcMMO by hand
        W lq = new W(5); PartyFeatureTest.FP m1 = lq.j("Sal"), m2 = lq.j("Ted"), m3 = lq.j("Una");
        lq.ourParty(m1, m2, m3); lq.handQuit(m1.id); lq.polls(3);
        check("mcMMO leader quits by hand: gone from ours, remaining pair keeps a leader", !lq.r.mgr.inParty(m1.id) && lq.r.mgr.partyOf(m2.id).size() == 2 && lq.r.mgr.partyOf(m2.id).members.contains(lq.r.mgr.partyOf(m2.id).leader));
        // not-mirrored player never gets leader from mcMMO
        // 7) offline
        W off = new W(5); PartyFeatureTest.FP n1 = off.j("Vic"), n2 = off.j("Wes"), n3 = off.j("Xan");
        off.ourParty(n1, n2);
        off.r.env.on.remove(n2.id); off.r.ctl.playerQuit(n2.id);
        off.handJoin(n3.id, off.mc.of(n1.id).name);
        check("hand join while a member is offline (grace): joiner added", off.inOurs(n1, n3) && off.r.mgr.partyOf(n1.id).size() == 3);
        off.handKick(n2.id);        // an offline kick leaves no player online, but the event/poll still sees the uuid
        off.polls(3);
        check("offline member kicked in mcMMO is removed from ours", !off.r.mgr.inParty(n2.id) && off.r.mgr.inParty(n1.id) && off.r.mgr.inParty(n3.id));
        // offline mate does not stop the build; offline mate is skipped
        W of2 = new W(5); PartyFeatureTest.FP p1 = of2.j("Yan"), p2 = of2.j("Zed"), p3 = of2.j("Abe");
        of2.mc.make("Mix", p1.id); of2.mc.join(p2.id, "Mix"); of2.mc.join(p3.id, "Mix");
        of2.r.env.on.remove(p2.id);
        of2.r.ctl.mcmmoChanged(List.of(p3.id), List.of(p3.id));
        check("offline mcMMO mate is skipped, online ones are mirrored", of2.inOurs(p1, p3) && !of2.r.mgr.inParty(p2.id) && of2.r.mgr.partyOf(p1.id).size() == 2);
        // only one online: nothing yet
        W on1 = new W(5); PartyFeatureTest.FP q1b = on1.j("Bea"), q2b = on1.j("Cal"); on1.mc.make("Pair", q1b.id); on1.mc.join(q2b.id, "Pair");
        on1.r.env.on.remove(q1b.id);
        on1.r.ctl.mcmmoChanged(List.of(q2b.id), List.of(q2b.id));
        check("only one mcMMO member online: no party yet, no crash", !on1.r.mgr.inParty(q2b.id));

        // 8) Bedrock dot names
        W bd = new W(5); var k1 = bd.b(".MikeExcel"); var k2 = bd.b(".coolgirlkail"); var k3 = bd.j("Jay");
        bd.mc.make("Fam", k1.id); bd.handJoin(k2.id, "Fam");
        check("dot-name Bedrock pair mirrored", bd.inOurs(k1, k2));
        check("names shown without the dot", k1.heard("added") || k1.heard("also made"));
        bd.handJoin(k3.id, "Fam");
        check("Java joiner joins the dot-name party; others told without dots", bd.r.mgr.partyOf(k1.id).size() == 3 && k1.heard("Jay joined") && k3.heard("added") && !k1.all().contains(".coolgirlkail"));
        bd.handQuit(k2.id);
        check("dot-name member quits mcMMO: removed, told, others see no dot", !bd.r.mgr.inParty(k2.id) && k1.heard("coolgirlkail left the mcMMO party") && !k1.all().contains(".coolgirlkail"));

        // 9) unknown reads never remove anyone
        W u = new W(5); PartyFeatureTest.FP u1 = u.j("Ann2"), u2 = u.j("Bob2"); u.ourParty(u1, u2);
        u.mc.unknown.add(u1.id); u.mc.unknown.add(u2.id); u.mc.parties.clear();
        u.polls(10); u.r.ctl.mcmmoChanged(List.of(u1.id, u2.id), List.of());
        check("profile not loaded (UNKNOWN): ours is left alone", u.inOurs(u1, u2));
        u.mc.unknown.clear(); u.polls(2);
        check("mcMMO party gone for real: both leave ours", !u.r.mgr.inParty(u1.id));

        // 10) never-linked players are left alone
        W nl = new W(5); PartyFeatureTest.FP r1 = nl.j("Rob"), r2 = nl.j("Sue"), r3 = nl.j("Tim"), r4 = nl.j("Ula");
        nl.r.mgr.adopt(r2.id, r1.id);              // ours only, mcMMO knows nothing
        nl.mc.make("Other", r3.id); nl.mc.join(r4.id, "Other");
        nl.polls(10);
        check("ours-only party stays when mcMMO has no record of it", nl.inOurs(r1, r2));
        nl.handJoin(r2.id, "Other");
        check("unlinked ours member joins a different mcMMO party by hand: mcMMO wins, moves with a note", !nl.inOurs(r1, r2) && nl.inOurs(r2, r3) && nl.inOurs(r2, r4) && r2.heard("you left your old ExcelTogether party and joined theirs"));
        check("the old party's other member is told", r1.heard("joined a different mcMMO party and so left the party"));
        nl.polls(10);
        check("stable after the move", nl.inOurs(r2, r3) && nl.r.mgr.partyOf(r2.id).size() == 3);
        // unlinked member, target mcMMO party's ours party is full: stays, one note
        W kf = new W(2); PartyFeatureTest.FP kz1 = kf.j("Ka"), kz2 = kf.j("Kb"), kz3 = kf.j("Kc"), kz4 = kf.j("Kd"), kz5 = kf.j("Ke");
        kf.r.mgr.adopt(kz2.id, kz1.id);                       // ours-only pair (cap 2)
        kf.r.mgr.adopt(kz4.id, kz3.id);                       // another ours pair, full
        kf.mc.make("Cs", kz3.id); kf.mc.join(kz4.id, "Cs"); kf.r.ctl.mcmmoChanged(List.of(kz3.id, kz4.id), List.of());
        kf.handJoin(kz2.id, "Cs"); kf.handJoin(kz2.id, "Cs"); kf.polls(5);
        check("hand join into an mcMMO party whose ExcelTogether party is full: stays put, one note", kf.inOurs(kz1, kz2) && !kf.inOurs(kz2, kz3) && kz2.all().split("their ExcelTogether party is full", -1).length - 1 == 1);
        // nobody online to land with: stays, one note
        W kn = new W(5); PartyFeatureTest.FP nz1 = kn.j("Na"), nz2 = kn.j("Nb"), nz3 = kn.j("Nc");
        kn.r.mgr.adopt(nz2.id, nz1.id); kn.mc.make("Off", nz3.id); kn.r.env.on.remove(nz3.id);
        kn.handJoin(nz2.id, "Off");
        check("mcMMO mates all offline: stays in own party with a note", kn.inOurs(nz1, nz2) && nz2.heard("none of them is online"));
        // polling alone never moves an unlinked member
        W kp = new W(5); PartyFeatureTest.FP pz1 = kp.j("Pa"), pz2 = kp.j("Pb"), pz3 = kp.j("Pc"), pz4 = kp.j("Pd");
        kp.r.mgr.adopt(pz2.id, pz1.id); kp.mc.make("Q", pz3.id); kp.mc.join(pz2.id, "Q"); kp.mc.join(pz4.id, "Q");
        kp.polls(10);
        check("a poll never moves an unlinked member out of their party", kp.inOurs(pz1, pz2));
        // 11) switching mcMMO party moves mirrored players
        W sw = new W(5); PartyFeatureTest.FP w1 = sw.j("Al"), w2 = sw.j("Bo"), w3 = sw.j("Cat"), w4 = sw.j("Dot");
        sw.ourParty(w1, w2); sw.ourParty(w3, w4);
        String a1 = sw.mc.of(w1.id).name, a3 = sw.mc.of(w3.id).name;
        sw.handQuit(w2.id);
        check("mirrored player quits mcMMO: leaves ours (pair ends)", !sw.r.mgr.inParty(w1.id) && !sw.r.mgr.inParty(w2.id));
        sw.ourParty(w1, w2);
        sw.hand(w2.id, a1, a3, () -> { sw.mc.quit(w2.id); sw.mc.join(w2.id, a3); });
        check("mirrored player switches mcMMO party: follows into the new ExcelTogether party", sw.inOurs(w2, w3) && sw.r.mgr.partyOf(w3.id).size() == 3 && !sw.r.mgr.inParty(w1.id));
        // 12) rename keeps the link honest
        W rn = new W(5); PartyFeatureTest.FP x1 = rn.j("Eli"), x2 = rn.j("Fia"), x3 = rn.j("Gus");
        rn.ourParty(x1, x2);
        String old = rn.mc.of(x1.id).name;
        rn.hand(x1.id, old, "Renamed", () -> { Mc.P p = rn.mc.parties.remove(old); p.name = "Renamed"; rn.mc.parties.put("Renamed", p); });
        rn.polls(3);
        check("mcMMO rename: link bookkeeping follows the new name", "Renamed".equals(rn.mc.linked.get(x1.id)) && "Renamed".equals(rn.mc.linked.get(x2.id)));
        rn.handJoin(x3.id, "Renamed");
        rn.r.ctl.command(x3, new String[]{"leave", "yes"});
        check("after rename our leave still removes the hand joiner from mcMMO", rn.mc.of(x3.id) == null);
        // 13) accepting a stored invite of ours by typing /party accept
        W iv = new W(5); PartyFeatureTest.FP y1 = iv.j("Hux"), y2 = iv.j("Ida");
        iv.r.ctl.command(y1, new String[]{"invite", "Ida"});         // our invite pending, mcMMO invite set by link
        check("invite pending, nothing joined yet", !iv.r.mgr.inParty(y2.id));
        iv.handJoin(y2.id, iv.mc.of(y1.id).name);                     // she typed /party accept
        check("/party accept of a link-made invite: both in ours, our invite is consumed", iv.inOurs(y1, y2) && !iv.r.mgr.hasInvite(y1.id, y2.id, iv.r.env.now()));
        iv.r.ctl.command(y2, new String[]{"accept"});
        check("stale Accept tap afterwards is harmless", iv.inOurs(y1, y2) && iv.r.mgr.partyOf(y1.id).size() == 2);
        // 14) leader invite from mcMMO into an ours party led by someone else
        W li = new W(5); PartyFeatureTest.FP z1 = li.j("Jax"), z2 = li.j("Kai"), z3 = li.j("Lia");
        li.ourParty(z1, z2);
        li.mc.of(z1.id).leader = z2.id;       // mcMMO leader is Kai, not the ours leader
        li.handJoin(z3.id, li.mc.of(z1.id).name);
        check("mcMMO leader invite path: joiner in the party the leader is in", li.inOurs(z2, z3) && li.inOurs(z1, z3));
        // 15) failures
        W bm = new W(5); PartyFeatureTest.FP e1 = bm.j("Eve3"), e2 = bm.j("Fay3"); bm.ourParty(e1, e2);
        bm.mc.boom = true;
        bm.polls(5); bm.r.ctl.mcmmoChanged(List.of(e1.id), List.of(e1.id)); bm.r.ctl.mcmmoEventIds(e1.id, "x", "y");
        check("a link that throws on read never breaks or changes anything", bm.inOurs(e1, e2));
        PartyFeatureTest.Rig none = new PartyFeatureTest.Rig(5); var n9 = none.java("Moe9"); var n8 = none.java("Nia9");
        none.ctl.mcmmoChanged(List.of(n9.id, n8.id), List.of(n9.id)); none.ctl.tick(); none.ctl.tick(); none.ctl.tick();
        check("mcMMO absent (no link): sync calls are no-ops", !none.mgr.inParty(n9.id) && none.ctl.mcmmoEventIds(n9.id, "a", "b").contains(n9.id));
        none.ctl.command(n9, new String[]{"invite", "Nia9"}); none.ctl.command(n8, new String[]{"accept"}); none.ctl.tick(); none.ctl.tick();
        check("mcMMO absent: normal party flow unaffected by the poll", none.mgr.sameParty(n9.id, n8.id));
        // 16) event ids collect pre-change members
        W ev = new W(5); PartyFeatureTest.FP v1 = ev.j("Ota"), v2 = ev.j("Pru"), v3 = ev.j("Qin"); ev.ourParty(v1, v2, v3);
        List<UUID> ids = ev.r.ctl.mcmmoEventIds(v1.id, ev.mc.of(v1.id).name, null);
        check("pre-change event ids include every member of the old party", ids.containsAll(List.of(v1.id, v2.id, v3.id)));

        // 18) adopt on login (server restarted: mcMMO parties persist, ours is empty)
        W lg = new W(5); PartyFeatureTest.FP la = lg.j("Lga"), lb = lg.j("Lgb"), lc = lg.j("Lgc");
        lg.mc.make("Kin", la.id); lg.mc.join(lb.id, "Kin"); lg.mc.join(lc.id, "Kin");
        lg.r.env.on.remove(lb.id); lg.r.env.on.remove(lc.id);
        lg.r.ctl.playerJoin(la); lg.polls(4);
        check("login with every mcMMO mate offline: nothing yet, no crash", !lg.r.mgr.inParty(la.id));
        lg.r.env.add(lb); lg.r.ctl.playerJoin(lb); lg.polls(3);
        check("second mate logs in: party rebuilt, mcMMO leader leads", lg.inOurs(la, lb) && lg.r.mgr.partyOf(la.id).leader.equals(la.id) && lb.heard("added to the ExcelTogether party"));
        lg.r.env.add(lc); lg.r.ctl.playerJoin(lc); lg.polls(3);
        check("third mate logs in: joins the same party", lg.r.mgr.partyOf(la.id).size() == 3);
        lg.r.ctl.playerQuit(lc.id); lg.r.env.on.remove(lc.id); lg.r.env.add(lc); lg.r.ctl.playerJoin(lc); lg.polls(5);
        check("relogin does nothing new", lg.r.mgr.partyOf(la.id).size() == 3);
        lg.r.ctl.command(lc, new String[]{"leave", "yes"}); lg.polls(5);
        check("after leaving ours on purpose and a relog-less poll, not pulled back", !lg.r.mgr.inParty(lc.id));
        // profile not loaded for a while after login
        W lp = new W(5); PartyFeatureTest.FP pa = lp.j("Lpa"), pb = lp.j("Lpb");
        lp.mc.make("Slow", pa.id); lp.mc.join(pb.id, "Slow"); lp.mc.unknown.add(pa.id); lp.mc.unknown.add(pb.id);
        lp.r.ctl.playerJoin(pa); lp.r.ctl.playerJoin(pb); lp.polls(10);
        check("profiles still loading: nothing happens, nothing breaks", !lp.r.mgr.inParty(pa.id));
        lp.mc.unknown.clear(); lp.polls(3);
        check("profiles loaded later: both adopted", lp.inOurs(pa, pb));
        // login with no mcMMO party
        W ln = new W(5); PartyFeatureTest.FP na = ln.j("Lna"); ln.r.ctl.playerJoin(na); ln.polls(5);
        check("login with no mcMMO party: nothing", !ln.r.mgr.inParty(na.id));
        // reload / plugin enable with players already online
        W rl = new W(5); PartyFeatureTest.FP ra = rl.j("Rla"), rb = rl.j("Rlb"); rl.mc.make("Rel", ra.id); rl.mc.join(rb.id, "Rel");
        rl.r.ctl.mcmmoWatch(List.of(ra.id, rb.id)); rl.polls(3);
        check("plugin enable (reload): already-online players adopted", rl.inOurs(ra, rb));
        // dot-name Bedrock on login
        W bl = new W(5); PartyFeatureTest.FP ba = bl.b(".MikeExcel"), bb = bl.b(".coolgirlkail"); bl.mc.make("Fam", ba.id); bl.mc.join(bb.id, "Fam");
        bl.r.ctl.playerJoin(ba); bl.r.ctl.playerJoin(bb); bl.polls(3);
        check("dot-name Bedrock pair adopted on login, names without dots", bl.inOurs(ba, bb) && !ba.all().contains(".coolgirlkail"));
        // mcMMO absent: join hook is harmless
        PartyFeatureTest.Rig ab = new PartyFeatureTest.Rig(5); PartyFeatureTest.FP aa = ab.java("Abs"); ab.ctl.playerJoin(aa); ab.ctl.mcmmoWatch(List.of(aa.id)); ab.ctl.tick(); ab.ctl.tick();
        check("mcMMO absent: login hook does nothing", !ab.mgr.inParty(aa.id));

        // 19) full-party retry when a seat frees
        W fr = new W(3); PartyFeatureTest.FP[] ff = new PartyFeatureTest.FP[5];
        for (int i = 0; i < 5; i++) ff[i] = fr.j("Fr" + i);
        fr.mc.make("Five", ff[0].id); for (int i = 1; i < 5; i++) fr.mc.join(ff[i].id, "Five");
        for (PartyFeatureTest.FP x : ff) fr.r.ctl.playerJoin(x);
        fr.polls(4);
        check("login with 5 mcMMO mates, cap 3: three in, two waiting", fr.r.mgr.partyOf(ff[0].id).size() == 3 && !fr.r.mgr.inParty(ff[3].id) && !fr.r.mgr.inParty(ff[4].id));
        fr.polls(20);
        check("waiting players: exactly one note each, no spam over many polls", ff[3].all().split("is full", -1).length - 1 == 1 && ff[4].all().split("is full", -1).length - 1 == 1);
        fr.r.ctl.command(ff[2], new String[]{"leave", "yes"}); fr.polls(3);
        check("a member leaves ours: first waiting player takes the seat", fr.inOurs(ff[0], ff[3]) && !fr.r.mgr.inParty(ff[4].id) && fr.r.mgr.partyOf(ff[0].id).size() == 3);
        check("their leaving took them out of mcMMO too, no loop", fr.mc.of(ff[2].id) == null && !fr.r.mgr.inParty(ff[2].id));
        fr.r.ctl.command(ff[0], new String[]{"kick", "Fr1"}); fr.polls(3);
        check("kick frees a seat: last waiting player joins", fr.inOurs(ff[0], ff[4]) && fr.r.mgr.partyOf(ff[0].id).size() == 3);
        check("kicked player stays out", !fr.r.mgr.inParty(ff[1].id));
        // offline timeout frees a seat
        W ft = new W(3); PartyFeatureTest.FP[] gg = new PartyFeatureTest.FP[4];
        for (int i = 0; i < 4; i++) gg[i] = ft.j("Ft" + i);
        ft.mc.make("Quad", gg[0].id); for (int i = 1; i < 4; i++) ft.mc.join(gg[i].id, "Quad");
        for (PartyFeatureTest.FP x : gg) ft.r.ctl.playerJoin(x);
        ft.polls(3);
        check("cap 3 of 4: one waiting", ft.r.mgr.partyOf(gg[0].id).size() == 3 && !ft.r.mgr.inParty(gg[3].id));
        ft.r.env.on.remove(gg[2].id); ft.r.ctl.playerQuit(gg[2].id); ft.r.env.t += 301_000; ft.r.ctl.tick(); ft.polls(3);
        check("offline timeout frees a seat: waiting player joins", ft.inOurs(gg[0], gg[3]));
        // waiting player logs off: dropped, not adopted while away
        W fo = new W(2); PartyFeatureTest.FP[] hh = new PartyFeatureTest.FP[3];
        for (int i = 0; i < 3; i++) hh[i] = fo.j("Fo" + i);
        fo.mc.make("Tri", hh[0].id); fo.mc.join(hh[1].id, "Tri"); fo.mc.join(hh[2].id, "Tri");
        for (PartyFeatureTest.FP x : hh) fo.r.ctl.playerJoin(x);
        fo.polls(3);
        fo.r.env.on.remove(hh[2].id); fo.r.ctl.playerQuit(hh[2].id);
        fo.r.ctl.command(hh[1], new String[]{"leave", "yes"}); fo.polls(5);
        check("waiting player offline is not adopted", !fo.r.mgr.inParty(hh[2].id));

        // 17) manager adopt/setLeader rules
        PartyManager pm = new PartyManager(2, 60000, 300000, x -> true);
        UUID a0 = UUID.randomUUID(), b0 = UUID.randomUUID(), c0 = UUID.randomUUID();
        check("adopt self", pm.adopt(a0, a0).result == PartyManager.AdoptResult.SELF);
        check("adopt creates", pm.adopt(b0, a0).result == PartyManager.AdoptResult.CREATED && pm.partyOf(a0).leader.equals(a0));
        check("adopt full (cap 2)", pm.adopt(c0, a0).result == PartyManager.AdoptResult.FULL && !pm.inParty(c0));
        check("adopt already in party", pm.adopt(b0, a0).result == PartyManager.AdoptResult.ALREADY_IN_PARTY);
        check("setLeader switches once", pm.setLeader(b0) && !pm.setLeader(b0) && pm.partyOf(a0).leader.equals(b0) && !pm.setLeader(c0));
        check("allMembers lists both", pm.allMembers().size() == 2);
    }

    public static void main(String[] a) {
        tests();
        System.out.println("McmmoSyncTest: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
