package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.6.0 (P-PARTY-REBUILD-1 unit 2 / U2-D1): the one-way mcMMO mirror. The roster of
 * record is the only source: McmmoSync writes it into mcMMO and repairs native
 * mcMMO changes as drift. This suite REPLACES the 0.5.6 adoption suite wholesale -
 * every check in the old file pinned bidirectional adoption (hand joins/leaves/
 * disbands adopted into the hub, suppression holding a leave against a stuck
 * mcMMO membership, login adoption, waiting/full-party adoption retries); those
 * behaviours are deleted by design, and the checks here pin their replacements:
 * mirror construction, drift repair with a log line, RT-1 (a door leave/disband
 * is never re-adopted over >=30 poll cycles and the RosterOrigin audit shows no
 * post-leave mutation), RT-3's mcMMO layer (crossed hub invites: no mcMMO invite
 * state exists at all, exactly one mirror follows the winning accept), and
 * login mirror-repair (stale mirror membership cleared, never adopted).
 * The PartyManager adopt/setLeader origin rules from the old file's tail are
 * KEPT (adopt survives as the LOGIN_REBUILD entry point, tagged as such).
 */
public class McmmoSyncTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** An in-memory mcMMO implementing the one-way link: raw state edits (native changes) plus the recorded reconcile drives. */
    static final class Mc implements McmmoLink {
        static final class P { String name; UUID leader; final LinkedHashMap<UUID, String> m = new LinkedHashMap<>(); }
        final Map<String, P> parties = new LinkedHashMap<>();
        final Map<UUID, String> names = new HashMap<>();
        final List<String> drives = new ArrayList<>();
        final Set<UUID> unknown = new HashSet<>();
        boolean boom = false;
        public boolean available() { return true; }
        P of(UUID id) { for (P p : parties.values()) if (p.m.containsKey(id)) return p; return null; }
        McParty view(P p) { return p == null ? McParty.NONE : new McParty(p.name, p.leader, p.m); }
        public McParty partyOf(UUID id) { if (boom) throw new IllegalStateException("x"); return unknown.contains(id) ? McParty.UNKNOWN : view(of(id)); }
        public McParty partyByName(String n) { if (boom) throw new IllegalStateException("x"); return view(parties.get(n)); }
        private static String clean(String hint) {
            StringBuilder b = new StringBuilder();
            if (hint != null) for (char ch : hint.toCharArray()) if (Character.isLetterOrDigit(ch) || ch == '_') b.append(ch);
            return b.length() == 0 ? "Family" : b.length() > 16 ? b.substring(0, 16) : b.toString();
        }
        public String createMirrorParty(UUID leader, String hint, String fallbackHint) {
            P cur = of(leader); if (cur != null) quit(leader);
            String want = clean(hint);
            String name = parties.containsKey(want) ? (parties.containsKey(clean(fallbackHint)) ? clean(fallbackHint) + "2" : clean(fallbackHint)) : want;
            drives.add("create:" + name);
            make(name, leader);
            return name;
        }
        public boolean addToParty(UUID id, String partyName) {
            P p = parties.get(partyName); if (p == null) return false;
            if (p.m.containsKey(id)) return true;
            if (of(id) != null) quit(id);
            drives.add("add:" + name(id) + "->" + partyName);
            p.m.put(id, names.getOrDefault(id, "x"));
            return true;
        }
        public void removeFromParty(UUID id, String partyName) {
            P p = of(id); if (p == null || !p.name.equals(partyName)) return;
            drives.add("remove:" + name(id) + "<-" + partyName);
            quit(id);
        }
        public boolean setPartyLeader(String partyName, UUID leader, String leaderName) {
            P p = parties.get(partyName); if (p == null || !p.m.containsKey(leader)) return false;
            drives.add("leader:" + partyName + "->" + name(leader));
            p.leader = leader;
            return true;
        }
        String name(UUID id) { return names.getOrDefault(id, "?"); }
        // raw mcMMO state edits (native changes by hand - no drives recorded)
        P make(String name, UUID leader) { P p = new P(); p.name = name; p.leader = leader; p.m.put(leader, names.getOrDefault(leader, "x")); parties.put(name, p); return p; }
        void join(UUID id, String name) { P p = parties.get(name); if (p != null) p.m.put(id, names.getOrDefault(id, "x")); }
        void quit(UUID id) {
            P p = of(id); if (p == null) return; p.m.remove(id);
            if (p.m.isEmpty()) parties.remove(p.name); else if (p.leader.equals(id)) p.leader = p.m.keySet().iterator().next();
        }
    }

    static final class W {
        final PartyFeatureTest.Rig r; final Mc mc = new Mc();
        final List<String> logs = new ArrayList<>();
        W(int cap) { r = new PartyFeatureTest.Rig(cap); r.ctl.setLink(mc); r.ctl.setLogSink(logs::add); }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); mc.names.put(p.id, n); return p; }
        PartyFeatureTest.FP b(String n) { PartyFeatureTest.FP p = r.bedrock(n); mc.names.put(p.id, n); return p; }
        /** a party formed at the door (invite + accept through our own commands) */
        void doorParty(PartyFeatureTest.FP lead, PartyFeatureTest.FP... others) {
            for (PartyFeatureTest.FP o : others) { r.ctl.command(lead, new String[]{"invite", o.real}); r.ctl.command(o, new String[]{"accept"}); }
        }
        boolean inOurs(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        void polls(int n) { for (int i = 0; i < n; i++) { r.env.t += 1000; r.ctl.tick(); } }
        boolean driftLogged(String who) { for (String s : logs) if (s.contains("drift repaired for " + who)) return true; return false; }
        boolean sameMcParty(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { Mc.P p = mc.of(a.id); return p != null && p.m.containsKey(b.id); }
    }

    static void tests() {
        // 1) mirror construction: a door party is written into mcMMO, leader-named, hub leader leads
        W w = new W(5); var a = w.j("Ann"); var b = w.j("Bob"); var c = w.j("Cy");
        w.doorParty(a, b);
        check("door accept reconciles the mirror at once", w.sameMcParty(a, b) && w.mc.of(a.id).leader.equals(a.id));
        check("mirror party name is leader-name-based (Q5)", w.mc.of(a.id).name.equals("Ann"));
        w.doorParty(a, c);
        check("third door member joins the same mirror", w.mc.of(a.id).m.size() == 3 && w.sameMcParty(a, c));
        int drives = w.mc.drives.size();
        w.polls(10);
        check("polling a settled mirror drives nothing more", w.mc.drives.size() == drives && w.r.mgr.partyOf(a.id).size() == 3);

        // 2) RT-1 unit layer: a door leave sticks - never re-adopted over >=30 polls, origin audit clean
        W t1 = new W(5); var x = t1.j("Xan"); var y = t1.j("Yui"); var z = t1.j("Zed");
        t1.doorParty(x, y, z);
        t1.r.ctl.command(z, new String[]{"leave", "yes"});
        check("RT-1 door leave removes the leaver from the mirror at once", t1.mc.of(z.id) == null && !t1.r.mgr.inParty(z.id));
        RosterOrigin afterLeave = t1.r.mgr.originOfPlayer(x.id);
        t1.polls(35);
        check("RT-1 >=30 polls: the leaver is never re-adopted", !t1.r.mgr.inParty(z.id) && t1.mc.of(z.id) == null);
        check("RT-1 RosterOrigin shows no post-leave mutation", t1.r.mgr.originOfPlayer(x.id) == afterLeave && t1.r.mgr.lastOrigin() == RosterOrigin.DOOR);
        // leader disbands at the door: mirror empties, nothing resurrects
        t1.r.ctl.command(x, new String[]{"leave", "yes"});
        check("RT-1 door disband empties the mirror", t1.mc.of(x.id) == null && t1.mc.of(y.id) == null && t1.mc.parties.isEmpty());
        t1.polls(35);
        check("RT-1 disbanded party stays disbanded everywhere over >=30 polls", t1.r.mgr.allMembers().isEmpty() && t1.mc.parties.isEmpty());

        // 3) drift: a hand quit in mcMMO is NOT a roster leave - the mirror is restored, with a log line
        W d = new W(5); var d1 = d.j("Dan"); var d2 = d.j("Eda"); var d3 = d.j("Fox");
        d.doorParty(d1, d2, d3);
        d.mc.quit(d3.id);                                   // native /party quit (state only)
        d.r.ctl.mcmmoChanged(List.of(d3.id), List.of());
        check("drift quit: roster untouched (no adoption of the leave)", d.inOurs(d1, d3) && d.r.mgr.partyOf(d1.id).size() == 3);
        check("drift quit: mirror membership restored", d.sameMcParty(d1, d3));
        check("drift quit: repair logged naming the player", d.driftLogged("Fox"));
        // a whole native disband is drift too: the mirror party is recreated from the roster
        String mirrorName = d.mc.of(d1.id).name;
        d.mc.quit(d1.id); d.mc.quit(d2.id); d.mc.quit(d3.id);
        check("native disband removed the mcMMO party", d.mc.parties.isEmpty());
        d.polls(3);
        check("drift disband: mirror recreated, roster intact", d.sameMcParty(d1, d2) && d.sameMcParty(d1, d3) && d.inOurs(d1, d3) && d.mc.of(d1.id).name.equals(mirrorName));

        // 4) drift: a roster member who hand-joins a foreign mcMMO party is restored; the foreign party is otherwise untouched
        W f = new W(5); var f1 = f.j("Fay"); var f2 = f.j("Gil"); var f3 = f.j("Hal"); var f4 = f.j("Ida");
        f.doorParty(f1, f2);
        f.mc.make("Foreign", f3.id); f.mc.join(f4.id, "Foreign");
        f.mc.quit(f2.id); f.mc.join(f2.id, "Foreign");      // Gil hand-joins the foreign party
        f.polls(3);
        check("drift join: Gil is restored to the mirror", f.sameMcParty(f1, f2) && f.inOurs(f1, f2));
        check("drift join: the foreign party keeps its own members", f.sameMcParty(f3, f4) && f.mc.of(f3.id).name.equals("Foreign"));
        check("drift join: repair logged naming Gil", f.driftLogged("Gil"));

        // 5) drift: a non-roster player who hand-joins the MIRROR party is removed from it; the roster never changes
        W e = new W(5); var e1 = e.j("Eli"); var e2 = e.j("Fia"); var e3 = e.j("Gus");
        e.doorParty(e1, e2);
        e.mc.join(e3.id, e.mc.of(e1.id).name);
        e.polls(3);
        check("drift extra: Gus removed from the mirror", e.mc.of(e3.id) == null && e.sameMcParty(e1, e2));
        check("drift extra: Gus was never adopted into the roster", !e.r.mgr.inParty(e3.id));
        check("drift extra: repair logged naming Gus", e.driftLogged("Gus"));

        // 6) drift: a native leader change is restored to the hub leader
        W l = new W(5); var l1 = l.j("Leo"); var l2 = l.j("Mia"); var l3 = l.j("Ned");
        l.doorParty(l1, l2, l3);
        l.mc.of(l1.id).leader = l2.id;                      // native /party owner
        l.polls(3);
        check("drift leader: mirror leadership restored to the hub leader", l.mc.of(l1.id).leader.equals(l1.id) && l.r.mgr.partyOf(l1.id).leader.equals(l1.id));
        check("drift leader: repair logged naming the hub leader", l.driftLogged("Leo"));

        // 7) RT-3 unit layer (mcMMO half): crossed hub invites - no mcMMO invite state, one mirror for the winner
        W r3 = new W(5); var i1 = r3.j("Ivy"); var i2 = r3.j("Joe"); var tg = r3.j("Kim");
        r3.r.ctl.command(i1, new String[]{"invite", "Kim"});
        r3.r.ctl.command(i2, new String[]{"invite", "Kim"});
        check("RT-3 hub invites create no mcMMO state at all", r3.mc.parties.isEmpty() && r3.mc.drives.isEmpty());
        r3.r.ctl.command(tg, new String[]{"accept", "Ivy"});
        check("RT-3 accepting one invite joins only that party", r3.inOurs(i1, tg) && !r3.inOurs(i2, tg));
        check("RT-3 exactly one mirror party, for the winning inviter", r3.mc.parties.size() == 1 && r3.sameMcParty(i1, tg) && r3.mc.of(i2.id) == null);
        check("RT-3 the losing invite is dead hub-side too", !r3.r.mgr.hasInvite(i2.id, tg.id, r3.r.env.now()));

        // 8) login = mirror repair, never adoption
        W lg = new W(5); var g1 = lg.j("Gia"); var g2 = lg.j("Han"); var g3 = lg.j("Ike");
        lg.r.mgr.adopt(g2.id, g1.id);                       // the roster of record (restored): Gia leads Gia+Han
        lg.r.mgr.adopt(g3.id, g1.id);
        lg.mc.make("Stale", g3.id);                         // Ike sits in a foreign mcMMO party at login
        lg.r.ctl.playerJoin(g1); lg.r.ctl.playerJoin(g2); lg.r.ctl.playerJoin(g3);
        lg.polls(4);
        check("login repair: the restored roster is mirrored", lg.sameMcParty(g1, g2) && lg.sameMcParty(g1, g3));
        check("login repair: Ike was restored out of the foreign party", lg.mc.of(g3.id) != null && !lg.mc.of(g3.id).name.equals("Stale"));
        // a player on NO roster still sitting in our mirror party at login is cleared, not adopted
        W st = new W(5); var s1 = st.j("Sue"); var s2 = st.j("Tom"); var s3 = st.j("Uma");
        st.doorParty(s1, s2);
        st.r.mgr.leave(s2.id, new PartyManager.Change());   // roster changes elsewhere; Tom's mirror seat is now stale
        st.r.ctl.mcmmoWatch(List.of(s2.id));
        st.polls(3);
        check("login repair: stale mirror member cleared, never adopted", st.mc.of(s2.id) == null && !st.r.mgr.inParty(s2.id));
        check("login repair: the disbanded party's mirror is emptied, not rebuilt", st.mc.parties.isEmpty() && !st.r.mgr.inParty(s1.id));
        // login with no roster and only a native mcMMO party - RE-PINNED in U2-D4:
        // the pre-D4 pin ("the native party is left standing", seed pending) is
        // replaced by the formal stale-face rule (SCOPING §2.5): with the store
        // governing (seed not armed here), the stale membership is CLEARED at
        // login, never adopted. The armed-seed behaviour is LoginRebuildTest's.
        W nn = new W(5); var n1 = nn.j("Nia"); var n2 = nn.j("Ola");
        nn.mc.make("Native", n1.id); nn.mc.join(n2.id, "Native");
        nn.r.ctl.playerJoin(n1); nn.r.ctl.playerJoin(n2); nn.polls(5);
        check("login: a native mcMMO party with no roster is NOT adopted", !nn.r.mgr.inParty(n1.id) && !nn.r.mgr.inParty(n2.id));
        check("login: the stale native membership is cleared, not left standing (U2-D4)", nn.mc.of(n1.id) == null && nn.mc.of(n2.id) == null && nn.mc.parties.isEmpty());
        check("login: clearing created no roster mutation (no LOGIN_REBUILD adoption)", nn.r.mgr.lastOrigin() == null);

        // 9) naming: leader-name collision falls back to the hub-id name (Q5)
        W cn = new W(5); var c1 = cn.j("Mike"); var c2 = cn.j("Kay"); var c3 = cn.j("Lou");
        cn.mc.make("Mike", c3.id);                          // a foreign party already holds the leader's name
        cn.doorParty(c1, c2);
        check("collision: mirror uses the hub-id fallback name", cn.mc.of(c1.id) != null && cn.mc.of(c1.id).name.startsWith("party") && cn.sameMcParty(c1, c2));
        check("collision: the foreign party is untouched", cn.mc.of(c3.id).name.equals("Mike"));

        // 10) rename: the mirror is rediscovered under its new name and keeps reconciling
        W rn = new W(5); var q1 = rn.j("Rae"); var q2 = rn.j("Sid"); var q3 = rn.j("Tae");
        rn.doorParty(q1, q2);
        String old = rn.mc.of(q1.id).name;
        Mc.P rp = rn.mc.parties.remove(old); rp.name = "Renamed"; rn.mc.parties.put("Renamed", rp);
        rn.polls(3);
        check("rename: mirror rediscovered under the new name", "Renamed".equals(rn.mc.of(q1.id).name) && rn.sameMcParty(q1, q2));
        rn.doorParty(q1, q3);
        check("rename: later door joins land in the renamed mirror", rn.mc.of(q3.id) != null && rn.mc.of(q3.id).name.equals("Renamed"));

        // 11) UNKNOWN reads and failures never change anything; absent link is a no-op
        W u = new W(5); var u1 = u.j("Uma2"); var u2 = u.j("Val");
        u.doorParty(u1, u2);
        u.mc.unknown.add(u1.id); u.mc.unknown.add(u2.id); u.mc.parties.clear();
        u.polls(10); u.r.ctl.mcmmoChanged(List.of(u1.id, u2.id), List.of());
        check("UNKNOWN reads: roster left alone", u.inOurs(u1, u2));
        W bm = new W(5); var b1 = bm.j("Bea"); var b2 = bm.j("Cal");
        bm.doorParty(b1, b2); bm.mc.boom = true;
        bm.polls(5); bm.r.ctl.mcmmoChanged(List.of(b1.id), List.of(b1.id)); bm.r.ctl.mcmmoEventIds(b1.id, "x", "y");
        check("a link that throws on read never breaks or changes the roster", bm.inOurs(b1, b2));
        PartyFeatureTest.Rig none = new PartyFeatureTest.Rig(5); var n9 = none.java("Moe9"); var n8 = none.java("Nia9");
        none.ctl.mcmmoChanged(List.of(n9.id, n8.id), List.of(n9.id)); none.ctl.tick(); none.ctl.tick();
        check("mcMMO absent (no link): sync calls are no-ops", !none.mgr.inParty(n9.id) && none.ctl.mcmmoEventIds(n9.id, "a", "b").contains(n9.id));
        none.ctl.command(n9, new String[]{"invite", "Nia9"}); none.ctl.command(n8, new String[]{"accept"}); none.ctl.tick(); none.ctl.tick();
        check("mcMMO absent: normal party flow unaffected by the poll", none.mgr.sameParty(n9.id, n8.id));

        // 12) event ids collect pre-change members (unchanged contract)
        W ev = new W(5); var v1 = ev.j("Ota"); var v2 = ev.j("Pru"); var v3 = ev.j("Qin");
        ev.doorParty(v1, v2, v3);
        List<UUID> ids = ev.r.ctl.mcmmoEventIds(v1.id, ev.mc.of(v1.id).name, null);
        check("pre-change event ids include every member of the old party", ids.containsAll(List.of(v1.id, v2.id, v3.id)));

        // 13) Bedrock dot names mirror like anyone else, shown without the dot
        W bd = new W(5); var k1 = bd.b(".MikeExcel"); var k2 = bd.b(".coolgirlkail"); var k3 = bd.j("Jay");
        bd.doorParty(k1, k2);
        check("dot-name Bedrock pair mirrored", bd.sameMcParty(k1, k2) && bd.inOurs(k1, k2));
        bd.doorParty(k1, k3);
        check("Java joiner joins the dot-name mirror", bd.mc.of(k1.id).m.size() == 3 && !k1.all().contains(".coolgirlkail"));

        // 14) PartyManager adopt/setLeader origin rules (KEPT from the 0.5.6 suite: adopt is the LOGIN_REBUILD entry)
        PartyManager pm = new PartyManager(2, 60000, 300000, id -> true);
        UUID a0 = UUID.randomUUID(), b0 = UUID.randomUUID(), c0 = UUID.randomUUID();
        check("adopt self", pm.adopt(a0, a0).result == PartyManager.AdoptResult.SELF);
        check("adopt creates", pm.adopt(b0, a0).result == PartyManager.AdoptResult.CREATED && pm.partyOf(a0).leader.equals(a0));
        check("adopt tags LOGIN_REBUILD, never DOOR", pm.originOfPlayer(a0) == RosterOrigin.LOGIN_REBUILD);
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
