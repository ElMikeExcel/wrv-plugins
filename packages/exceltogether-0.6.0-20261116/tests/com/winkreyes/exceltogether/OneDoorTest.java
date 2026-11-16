package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.6.0 (P-PARTY-REBUILD-1 unit 2 / U2-D5): the OneDoorTest integration suite -
 * the CROSS-LEG unit layer of SCOPING.md SS3. The per-leg suites pin each
 * engine on its own (McmmoSyncTest: the one-way mcMMO mirror; EmSyncTest: the
 * materialise-only EM face; PartyCommandRouterTest: the door decision table;
 * LoginRebuildTest: seed + stale-clear + Q4). This suite runs ONE harness -
 * the real Controller with BOTH fakes wired at once, plus the real
 * PartyCommandRouter - and pins the matrix where the legs meet:
 *
 *  OD-1  RT-1 from each of the three doors in one suite (door-command leave,
 *        EM-door leave, and a native mcMMO "leave" that must NOT leave),
 *        plus a door disband - every outcome observed in hub + EM + mcMMO.
 *  OD-2  RT-2 / AT-1: one-tap EM popup accept with all three rosters in
 *        agreement, and a command accept out of disjoint EM *and* mcMMO
 *        native parties (clear-and-join, no kept divergence anywhere).
 *  OD-3  RT-3 crossed invites across both door names, with a foreign EM
 *        pending invite staged - the hub accept joins the winner only.
 *  OD-4  RT-4: decline at the door, then the native EM [ACCEPT] - reverted
 *        in EM, never mirrored into mcMMO, never joined in the hub.
 *  OD-5  RT-5 unit layer end-to-end: faces wiped, login rebuild restores
 *        both faces from the roster of record; a Q4 invite survives a
 *        relog and completes through the router into all three rosters.
 *  OD-6  AT-5 unit half: every non-door mutation surface the fakes expose
 *        is attempted; the RosterOrigin audit only ever shows DOOR,
 *        LOGIN_REBUILD (the armed seed) or ADMIN (the admin surface).
 *  OD-7  Q8 cross-leg: a hub accept out of a dungeon-frozen disjoint EM
 *        party lands in the hub and the mcMMO mirror at once, while the
 *        stale EM membership defers until unfreeze.
 *
 * The rig halves of RT-1..RT-5 / AT-1..AT-5 (real EM 10.9.14 + the live
 * mcMMO jar on the mirror rig) are U2-R1..R4 and are NOT claimed here.
 * Run: java -cp <classes> com.winkreyes.exceltogether.OneDoorTest
 */
public class OneDoorTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** One Controller, both face fakes, the real router. Ticks drive Controller.tick,
     *  so both syncs poll on their real cadence (every 2 ticks each):
     *  ticks(70) is 35 poll cycles of BOTH engines. */
    static final class W {
        final PartyFeatureTest.Rig r; final McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc(); final EmSyncTest.Em em = new EmSyncTest.Em();
        final PartyCommandRouter router = new PartyCommandRouter();
        final List<String> logs = new ArrayList<>();
        W(boolean seedArmed) {
            r = new PartyFeatureTest.Rig(5);
            r.ctl.setLink(mc); r.ctl.setEmLink(em); r.ctl.setLogSink(logs::add);
            r.ctl.setSeedArmed(seedArmed);
            r.ctl.setPlayerDirectory(() -> { List<PlayerView> out = new ArrayList<>(); out.addAll(r.env.on.values()); return out; });
        }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); mc.names.put(p.id, n); em.names.put(p.id, n); return p; }
        /** Type a raw command line exactly as the preprocess handler does: ROUTE -> dispatch, PASS -> nothing. */
        boolean type(PartyFeatureTest.FP p, String raw) {
            PartyCommandRouter.Result res = router.decide(raw);
            if (res.routed()) r.ctl.command(p, res.args);
            return res.routed();
        }
        /** Form a party entirely through the routed door, then let both faces settle. */
        void doorParty(PartyFeatureTest.FP lead, PartyFeatureTest.FP... others) {
            for (PartyFeatureTest.FP o : others) { type(lead, "/party invite " + o.real); type(o, "/party accept"); }
            ticks(8);
        }
        void ticks(int n) { for (int i = 0; i < n; i++) { r.env.t += 1000; r.ctl.tick(); } }
        void quit(PartyFeatureTest.FP p) { r.env.on.remove(p.id); r.ctl.playerQuit(p.id); }
        void rejoin(PartyFeatureTest.FP p) { r.env.add(p); r.ctl.playerJoin(p); }
        boolean inMc(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { McmmoSyncTest.Mc.P p = mc.of(a.id); return p != null && p.m.containsKey(b.id); }
        boolean inEm(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { EmSyncTest.Em.P p = em.of(a.id); return p != null && p.m.contains(b.id); }
        boolean ours(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        boolean logged(String s) { for (String l : logs) if (l.contains(s)) return true; return false; }
        List<String> admin(String... a) { List<String> out = new ArrayList<>(); r.ctl.adminCommand(out::add, a); return out; }
    }

    public static void main(String[] args) { tests(); System.out.println("OneDoorTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ================= OD-1: RT-1 from each of the three doors =================
        {
            // (a) the door command itself: /party leave
            W w = new W(false); var a = w.j("Ana"); var b = w.j("Ben"); var c = w.j("Cid");
            w.doorParty(a, b, c);
            check("OD-1a setup: hub, EM and mcMMO all hold the door party", w.ours(a, b) && w.ours(a, c) && w.inEm(a, b) && w.inEm(a, c) && w.inMc(a, b) && w.inMc(a, c));
            check("OD-1a routed /party leave dispatches", w.type(c, "/party leave yes"));
            check("OD-1a the leaver is out of all three at once", !w.r.mgr.inParty(c.id) && w.em.of(c.id) == null && w.mc.of(c.id) == null);
            RosterOrigin org = w.r.mgr.originOfPlayer(a.id);
            w.ticks(70);
            check("OD-1a >=30 polls of both engines: the leave sticks everywhere", !w.r.mgr.inParty(c.id) && w.em.of(c.id) == null && w.mc.of(c.id) == null);
            check("OD-1a the surviving pair still agrees in all three", w.ours(a, b) && w.inEm(a, b) && w.inMc(a, b));
            check("OD-1a RosterOrigin shows no post-leave mutation", w.r.mgr.originOfPlayer(a.id) == org && w.r.mgr.lastOrigin() == RosterOrigin.DOOR);
            int ed = w.em.drives.size(), md = w.mc.drives.size();
            w.ticks(4);
            check("OD-1a the settled state drives nothing on either face", w.em.drives.size() == ed && w.mc.drives.size() == md);
        }
        {
            // (b) the EM door: a settled member steps out at the EM face
            W w = new W(false); var a = w.j("Ara"); var b = w.j("Bas"); var c = w.j("Cal");
            w.doorParty(a, b, c);
            check("OD-1b setup converged in EM", w.inEm(a, c));
            w.em.emLeaveRaw(c.id);                                   // /em party leave, EM's own state edit
            w.ticks(8);
            check("OD-1b the EM-door leave ends the hub membership too", !w.r.mgr.inParty(c.id) && w.ours(a, b));
            check("OD-1b the mcMMO mirror drops the leaver as well", w.mc.of(c.id) == null && w.inMc(a, b));
            w.ticks(70);
            check("OD-1b >=30 polls: never re-adopted, never re-materialised", !w.r.mgr.inParty(c.id) && w.em.of(c.id) == null && w.mc.of(c.id) == null);
            check("OD-1b the honoured leave carries DOOR origin", w.r.mgr.lastOrigin() == RosterOrigin.DOOR);
        }
        {
            // (c) the mcMMO surface is NOT a door: a native quit is drift, not a leave
            W w = new W(false); var a = w.j("Dan"); var b = w.j("Eda"); var c = w.j("Fox");
            w.doorParty(a, b, c);
            w.mc.quit(c.id);                                         // native /party quit (mcMMO state only)
            w.ticks(6);
            check("OD-1c the roster does NOT follow the mcMMO quit", w.ours(a, c) && w.r.mgr.partyOf(a.id).size() == 3);
            check("OD-1c the mirror is restored from the roster", w.inMc(a, c));
            check("OD-1c the EM face never flickered", w.inEm(a, c));
            check("OD-1c the drift repair is logged naming the player", w.logged("drift repaired for Fox"));
            check("OD-1c no roster mutation happened (origin still the door's)", w.r.mgr.lastOrigin() == RosterOrigin.DOOR);
        }
        {
            // (d) leader disbands at the door: all three empty, and stay empty
            W w = new W(false); var a = w.j("Gia"); var b = w.j("Han");
            w.doorParty(a, b);
            w.type(a, "/party leave yes");
            check("OD-1d disband empties hub, EM and mcMMO", w.r.mgr.partyCount() == 0 && w.em.parties.isEmpty() && w.mc.parties.isEmpty());
            w.ticks(70);
            check("OD-1d >=30 polls: the disbanded party never resurrects anywhere", w.r.mgr.partyCount() == 0 && w.em.parties.isEmpty() && w.mc.parties.isEmpty());
        }

        // ================= OD-2: RT-2 / AT-1 =================
        {
            // (a) AT-1: /party invite -> ONE tap on the EM popup -> all three agree
            W w = new W(false); var a = w.j("Ivy"); var b = w.j("Wes"); var t = w.j("Tess");
            w.doorParty(a, b);
            check("OD-2a routed /party invite dispatches", w.type(a, "/party invite Tess"));
            check("OD-2a the EM popup was driven for the hub invite (ledger entry)", w.em.inviteDriven(a.id, t.id));
            w.em.accept(t.id);                                       // the one tap: EM's own [ACCEPT]
            w.ticks(6);
            check("OD-2a the popup answer completed the hub accept", w.ours(a, t) && t.heard("You joined"));
            check("OD-2a inviter got the explicit accepted line (AT-1)", a.heard("Tess accepted your party invite"));
            check("OD-2a EM face and mcMMO mirror both show the full roster", w.inEm(a, t) && w.inEm(a, b) && w.inMc(a, t) && w.inMc(a, b));
            check("OD-2a the completion carries DOOR origin", w.r.mgr.originOfPlayer(t.id) == RosterOrigin.DOOR);
        }
        {
            // (b) RT-2: command accept out of disjoint native parties on BOTH faces
            W w = new W(false); var a = w.j("Ana2"); var t = w.j("Tess2"); var s1 = w.j("Sid"); var s2 = w.j("Sue");
            EmSyncTest.Em.P ep = w.em.makeEm(s1.id); w.em.emJoinRaw(t.id, ep);       // Tess sits in Sid's native EM party
            w.mc.make("Native", s2.id); w.mc.join(t.id, "Native");                   // ...and in Sue's native mcMMO party
            w.r.ctl.command(a, new String[]{"invite", "Tess2"});                     // the /excelparty name of the same door
            check("OD-2b the invite disclosed the EM conflict in clear-and-join words", t.heard("different EliteMobs party") && t.heard("leaves that EliteMobs party"));
            check("OD-2b routed /mcmmo:party join dispatches as accept", w.type(t, "/mcmmo:party join"));
            check("OD-2b the accept joins the roster (no refusal)", w.ours(a, t) && t.heard("You joined"));
            w.ticks(8);
            check("OD-2b the stale EM membership is cleared and the face re-materialised", !w.inEm(s1, t) && w.inEm(a, t));
            check("OD-2b the native EM party was reverted away (no kept divergence)", w.em.of(s1.id) == null);
            check("OD-2b the stale mcMMO membership is cleared and the mirror built", !w.inMc(s2, t) && w.inMc(a, t));
            check("OD-2b the mcMMO stranger keeps her own party, untouched", w.mc.of(s2.id) != null && w.mc.of(s2.id).name.equals("Native"));
            check("OD-2b neither stranger ever entered the roster", !w.r.mgr.inParty(s1.id) && !w.r.mgr.inParty(s2.id));
            check("OD-2b the join carries DOOR origin", w.r.mgr.originOfPlayer(t.id) == RosterOrigin.DOOR);
        }

        // ================= OD-3: RT-3 crossed invites, mixed door names + a foreign EM pending =================
        {
            W w = new W(false); var a = w.j("Ann"); var b = w.j("Cat"); var t = w.j("Bea"); var x = w.j("Xan"); var y = w.j("Wes3");
            EmSyncTest.Em.P xp = w.em.makeEm(x.id); w.em.emJoinRaw(y.id, xp);        // a native EM party exists elsewhere
            w.type(a, "/party invite Bea");                                          // inviter A via /party
            w.r.ctl.command(b, new String[]{"invite", "Bea"});                       // inviter B via /excelparty (same dispatch)
            check("OD-3 the target holds both hub invites", w.r.mgr.invitesFor(t.id, w.r.env.now()).size() == 2);
            w.em.pending.put(t.id, x.id);                                            // a FOREIGN EM pending invite lands last (no ledger entry)
            check("OD-3 the foreign pending is staged with no driven-ledger entry", x.id.equals(w.em.pending.get(t.id)) && !w.em.inviteDriven(x.id, t.id));
            check("OD-3 routed accept of the winner dispatches", w.type(t, "/party accept Ann"));
            check("OD-3 the target joined the winner only", w.ours(a, t) && !w.ours(b, t));
            check("OD-3 the losing inviter was told their invite died (D5 fix)", b.heard("Your party invite to Bea is gone") && b.heard("accepted Ann's invite instead"));
            check("OD-3 the winner got the accepted line", a.heard("Bea accepted your party invite"));
            check("OD-3 every invite for the target is consumed", w.r.mgr.invitesFor(t.id, w.r.env.now()).isEmpty());
            w.ticks(8);
            check("OD-3 the foreign EM party never gained the target", !w.inEm(x, t));
            check("OD-3 exactly one face party and one mirror party, both the winner's", w.inEm(a, t) && w.inMc(a, t) && w.mc.parties.size() == 1);
        }

        // ================= OD-4: RT-4 decline, then the native EM [ACCEPT] =================
        {
            W w = new W(false); var a = w.j("Ivy4"); var b = w.j("Wes4"); var t = w.j("Tess4");
            w.doorParty(a, b);
            w.type(a, "/party invite Tess4");
            check("OD-4 routed /party decline dispatches", w.type(t, "/party decline"));
            check("OD-4 the decline removed the hub invite", !w.r.mgr.hasInvite(a.id, t.id, w.r.env.now()));
            w.em.accept(t.id);                                       // the still-live native EM [ACCEPT], clicked after the decline
            check("OD-4 setup: the native accept really landed in EM", w.inEm(a, t));
            w.ticks(8);
            check("OD-4 no hub join from the post-decline native accept", w.r.mgr.partyOf(t.id) == null && !t.heard("You joined"));
            check("OD-4 the native EM membership was reverted", !w.inEm(a, t) && w.inEm(a, b));
            check("OD-4 the mcMMO mirror never gained the target", w.mc.of(t.id) == null && w.inMc(a, b));
            check("OD-4 Q3 notice told exactly once, revert logged", t.said.stream().filter(s -> s.contains("run with /party")).count() == 1 && w.logged("reverted a non-door"));
            check("OD-4 the roster never mutated (party intact, origin still DOOR)", w.ours(a, b) && w.r.mgr.lastOrigin() == RosterOrigin.DOOR);
        }

        // ================= OD-5: RT-5 unit layer, end-to-end =================
        {
            // (a) faces wiped: login rebuild restores BOTH faces from the roster of record
            W w = new W(false); var a = w.j("Lea5"); var b = w.j("Ben5"); var c = w.j("Cid5");
            w.doorParty(a, b, c);
            w.em.parties.clear(); w.mc.parties.clear();              // the faces lose everything; the roster of record stands
            check("OD-5a setup: faces wiped, roster intact", w.em.parties.isEmpty() && w.mc.parties.isEmpty() && w.r.mgr.partyOf(a.id).size() == 3);
            w.rejoin(a); w.rejoin(b); w.rejoin(c);
            w.ticks(10);
            check("OD-5a the EM face is rebuilt from the roster", w.inEm(a, b) && w.inEm(a, c));
            check("OD-5a the mcMMO mirror is rebuilt from the roster", w.inMc(a, b) && w.inMc(a, c));
            check("OD-5a the rebuild mutated nothing (origin still DOOR)", w.r.mgr.lastOrigin() == RosterOrigin.DOOR && w.r.mgr.partyOf(a.id).size() == 3);
        }
        {
            // (b) Q4: an invite survives the target's relog and completes into all three rosters
            W w = new W(false); var a = w.j("Ana5"); var b = w.j("Ben6");
            w.type(a, "/party invite Ben6");
            w.quit(b);
            check("OD-5b the invite survives the target's quit (Q4)", w.r.mgr.hasInvite(a.id, b.id, w.r.env.now()));
            w.r.env.t += 30_000;
            a.clear(); b.clear();
            w.rejoin(b);
            check("OD-5b both sides are told on the return", b.heard("still waiting for you") && a.heard("is back - your party invite to them is still open"));
            check("OD-5b the survived invite accepts through the router", w.type(b, "/party accept"));
            w.ticks(8);
            check("OD-5b the party stands in hub, EM and mcMMO", w.ours(a, b) && w.inEm(a, b) && w.inMc(a, b));
        }

        // ================= OD-6: AT-5 unit half - the origin audit =================
        {
            // (a) native formations on stored boots create nothing and tag nothing
            W w = new W(false); var a = w.j("Aya6"); var b = w.j("Ben7"); var c = w.j("Cal6"); var d = w.j("Dee6");
            EmSyncTest.Em.P ep = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, ep);
            w.ticks(6);
            check("OD-6a a native EM party creates no roster and is dismantled", w.r.mgr.partyCount() == 0 && w.em.parties.isEmpty());
            w.mc.make("Native6", c.id); w.mc.join(d.id, "Native6");
            w.rejoin(c); w.rejoin(d);
            w.ticks(8);
            check("OD-6a a native mcMMO party at login is cleared, never adopted", w.r.mgr.partyCount() == 0 && w.mc.parties.isEmpty());
            check("OD-6a the audit is empty: no mutation of any origin happened", w.r.mgr.lastOrigin() == null);
        }
        {
            // (b) the drift battery against a settled door party: roster intact, origin unmoved
            W w = new W(false); var a = w.j("Lou6"); var b = w.j("Ada6"); var s = w.j("Sid6");
            w.doorParty(a, b);
            w.mc.quit(b.id); w.ticks(4);                             // native mcMMO quit
            check("OD-6b native mcMMO quit repaired, roster intact", w.ours(a, b) && w.inMc(a, b));
            w.mc.join(s.id, w.mc.of(a.id).name); w.ticks(4);         // stranger hand-joins the mirror
            check("OD-6b mirror stranger removed, never adopted", w.mc.of(s.id) == null && !w.r.mgr.inParty(s.id));
            w.em.emJoinRaw(s.id, w.em.of(a.id)); w.ticks(6);         // stranger hand-joins the EM twin
            check("OD-6b EM-twin stranger reverted out, never adopted", !w.inEm(a, s) && !w.r.mgr.inParty(s.id) && w.inEm(a, b));
            w.mc.of(a.id).leader = b.id; w.ticks(4);                 // native mcMMO leader change
            check("OD-6b native leader change restored to the hub leader", w.mc.of(a.id).leader.equals(a.id) && w.r.mgr.partyOf(a.id).leader.equals(a.id));
            check("OD-6b after the whole battery the only origin is DOOR", w.r.mgr.lastOrigin() == RosterOrigin.DOOR && w.r.mgr.originOfPlayer(a.id) == RosterOrigin.DOOR);
        }
        {
            // (c) router surfaces: chat/ptp pass, unknown passes, intercept verbs route but mutate nothing
            W w = new W(false); var a = w.j("Pam6");
            PartyCommandRouter r = w.router;
            check("OD-6c /party chat passes to mcMMO untouched", !r.decide("/party chat hello").routed());
            check("OD-6c /ptp passes", !r.decide("/ptp Ada6").routed());
            check("OD-6c an unknown /party verb passes", !r.decide("/party frobnicate").routed());
            check("OD-6c our own Bukkit labels pass (same dispatch, no double-handling)", !r.decide("/excelparty invite Ada6").routed());
            check("OD-6c an intercept verb routes to the door", r.decide("/party create").routed());
            w.type(a, "/party create");
            check("OD-6c the intercepted verb answers with help and mutates nothing", a.heard("Party commands") && w.r.mgr.partyCount() == 0 && w.r.mgr.lastOrigin() == null);
        }
        {
            // (d) the admin surface is the one allowed non-door origin
            W w = new W(false); var a = w.j("Rob6"); var b = w.j("Kim6"); var c = w.j("Tom6");
            w.doorParty(a, b, c);
            List<String> out = w.admin("admin", "remove", "Tom6");
            check("OD-6d the admin removal landed", !w.r.mgr.inParty(c.id) && w.ours(a, b));
            check("OD-6d the removal is tagged ADMIN in the audit", w.r.mgr.lastOrigin() == RosterOrigin.ADMIN && w.r.mgr.originOfPlayer(a.id) == RosterOrigin.ADMIN);
            w.ticks(8);
            check("OD-6d the removal sticks on both faces", w.em.of(c.id) == null && w.mc.of(c.id) == null && w.inEm(a, b) && w.inMc(a, b));
        }
        {
            // (e) the armed first-boot seed is the one allowed LOGIN_REBUILD origin
            W w = new W(true); var a = w.j("Seed1"); var b = w.j("Seed2");
            EmSyncTest.Em.P ep = w.em.makeEm(a.id); w.em.emJoinRaw(b.id, ep);
            w.rejoin(a);
            check("OD-6e the armed seed adopts the standing EM party as LOGIN_REBUILD", w.ours(a, b) && w.r.mgr.originOfPlayer(a.id) == RosterOrigin.LOGIN_REBUILD);
            w.ticks(8);
            check("OD-6e the seeded roster materialises on both faces", w.inEm(a, b) && w.inMc(a, b));
        }

        // ================= OD-7: Q8 cross-leg - frozen EM deferral, mcMMO immediate =================
        {
            W w = new W(false); var a = w.j("Ivy7"); var t = w.j("Tess7"); var x = w.j("Xan7");
            EmSyncTest.Em.P ep = w.em.makeEm(x.id); w.em.emJoinRaw(t.id, ep);
            w.r.ctl.command(a, new String[]{"invite", "Tess7"});
            w.em.busy.add(t.id);                                     // Tess is inside the frozen dungeon instance
            w.r.ctl.command(t, new String[]{"accept"});
            check("OD-7 the hub accept stands while the EM party is frozen", w.ours(a, t));
            check("OD-7 the mcMMO mirror takes the join at once (no freeze on that face)", w.inMc(a, t));
            w.ticks(6);
            check("OD-7 the stale EM membership is NOT torn mid-freeze (deferred, Q8)", w.inEm(x, t));
            w.em.busy.clear();
            w.ticks(8);
            check("OD-7 on unfreeze the stale membership clears and the face materialises", !w.inEm(x, t) && w.inEm(a, t) && w.em.of(x.id) == null);
        }
    }
}
