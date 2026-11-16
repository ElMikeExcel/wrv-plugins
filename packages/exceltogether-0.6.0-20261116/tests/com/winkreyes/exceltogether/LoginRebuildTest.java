package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.6.0 (P-PARTY-REBUILD-1 unit 2 / U2-D4): the login-rebuild formalisation -
 * the one-shot EM-first first-boot seed (Q6), stale-face clearing at login,
 * and Q4 invite survival across a relog (the RT-5 unit layer's login half;
 * the store half is RosterStoreTest, the rig half is U2-R4). Runs the real
 * Controller + both syncs against the McmmoSyncTest / EmSyncTest fakes.
 * The seed is armed per scenario exactly as the plugin arms it: armed iff
 * roster.yml was absent at enable (Controller.setSeedArmed).
 */
public class LoginRebuildTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    static final class W {
        final PartyFeatureTest.Rig r; final McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc(); final EmSyncTest.Em em = new EmSyncTest.Em();
        final List<String> logs = new ArrayList<>();
        W(boolean seedArmed) {
            r = new PartyFeatureTest.Rig(5);
            r.ctl.setLink(mc); r.ctl.setEmLink(em); r.ctl.setLogSink(logs::add);
            r.ctl.setSeedArmed(seedArmed);
        }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); mc.names.put(p.id, n); em.names.put(p.id, n); return p; }
        void ticks(int n) { for (int i = 0; i < n; i++) { r.env.t += 1000; r.ctl.tick(); } }
        void quit(PartyFeatureTest.FP p) { r.env.on.remove(p.id); r.ctl.playerQuit(p.id); }
        void rejoin(PartyFeatureTest.FP p) { r.env.add(p); r.ctl.playerJoin(p); }
        boolean logged(String s) { for (String l : logs) if (l.contains(s)) return true; return false; }
        boolean inMc(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { McmmoSyncTest.Mc.P p = mc.of(a.id); return p != null && p.m.containsKey(b.id); }
        boolean inEm(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { EmSyncTest.Em.P p = em.of(a.id); return p != null && p.m.contains(b.id); }
    }

    public static void main(String[] args) { tests(); System.out.println("LoginRebuildTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- S1: EM-first seed - an EM party standing at first boot becomes the roster ----------
        W s1 = new W(true); var a1 = s1.j("Ana"); var b1 = s1.j("Ben"); var c1 = s1.j("Cid");
        EmSyncTest.Em.P ep = s1.em.makeEm(a1.id); s1.em.emJoinRaw(b1.id, ep); s1.em.emJoinRaw(c1.id, ep);
        s1.r.ctl.playerJoin(a1);
        check("S1 EM party seeded into the roster at login", s1.r.mgr.sameParty(a1.id, b1.id) && s1.r.mgr.sameParty(a1.id, c1.id));
        check("S1 the EM leader leads the seeded party", s1.r.mgr.partyOf(a1.id).leader.equals(a1.id));
        check("S1 seeded origin is LOGIN_REBUILD", s1.r.mgr.originOfPlayer(a1.id) == RosterOrigin.LOGIN_REBUILD && s1.r.mgr.lastOrigin() == RosterOrigin.LOGIN_REBUILD);
        check("S1 the seed is logged", s1.logged("first-boot seed: seeded a party of 3 from EliteMobs"));
        check("S1 the player is settled after seeding", s1.r.ctl.seedSettled(a1.id));
        s1.ticks(6);
        check("S1 the EM face is kept (materialised from the roster, not dismantled)", s1.inEm(a1, b1) && s1.inEm(a1, c1));
        check("S1 the mcMMO mirror is built from the seeded roster", s1.inMc(a1, b1) && s1.inMc(a1, c1));

        // ---------- S2: mcMMO-only seed - no EM party, the mcMMO grouping seeds ----------
        W s2 = new W(true); var d2 = s2.j("Dan"); var e2 = s2.j("Eli");
        s2.mc.make("Dan", d2.id); s2.mc.join(e2.id, "Dan");
        s2.r.ctl.playerJoin(d2);
        check("S2 mcMMO party seeded when EM holds nothing", s2.r.mgr.sameParty(d2.id, e2.id) && s2.r.mgr.partyOf(d2.id).leader.equals(d2.id));
        check("S2 seeded origin is LOGIN_REBUILD", s2.r.mgr.originOfPlayer(d2.id) == RosterOrigin.LOGIN_REBUILD);
        check("S2 the seed is logged as mcMMO-sourced", s2.logged("first-boot seed: seeded a party of 2 from mcMMO"));
        s2.ticks(6);
        check("S2 the EM face materialises from the seeded roster", s2.inEm(d2, e2));

        // ---------- S3: EM-first conflict - the faces disagree, EM wins, conflict logged, no merge ----------
        W s3 = new W(true); var a3 = s3.j("Amy"); var b3 = s3.j("Ben"); var c3 = s3.j("Cal");
        EmSyncTest.Em.P ep3 = s3.em.makeEm(a3.id); s3.em.emJoinRaw(b3.id, ep3);          // EM: Amy+Ben
        s3.mc.make("Crew", a3.id); s3.mc.join(c3.id, "Crew");                            // mcMMO: Amy+Cal
        s3.r.ctl.playerJoin(a3);
        check("S3 the EM grouping seeds (EM-first)", s3.r.mgr.sameParty(a3.id, b3.id) && !s3.r.mgr.inParty(c3.id));
        check("S3 the conflict is logged naming the player", s3.logged("first-boot seed: conflict for Amy") && s3.logged("EM wins"));
        s3.r.ctl.playerJoin(c3);
        check("S3 the mcMMO grouping is not merged over the EM seed", !s3.r.mgr.inParty(c3.id) && s3.logged("overlaps a roster party"));
        s3.ticks(6);
        check("S3 Cal's stale mcMMO membership is cleared at login", s3.mc.of(c3.id) == null && !s3.r.mgr.inParty(c3.id));
        check("S3 Amy+Ben end in their roster mirror, not the stale party", s3.inMc(a3, b3) && s3.mc.of(a3.id).name.equals("Amy"));
        check("S3 no DOOR mutation ever happened (seed + clears only)", s3.r.mgr.lastOrigin() == RosterOrigin.LOGIN_REBUILD);

        // ---------- S4: the seed runs ONCE per player per enable ----------
        W s4 = new W(true); var a4 = s4.j("Ana"); var b4 = s4.j("Ben");
        s4.r.ctl.playerJoin(a4); s4.r.ctl.playerJoin(b4); s4.ticks(4);                   // settled logins, nothing standing
        EmSyncTest.Em.P ep4 = s4.em.makeEm(a4.id); s4.em.emJoinRaw(b4.id, ep4);          // a native EM party forms later
        s4.r.ctl.playerJoin(a4); s4.ticks(6);
        check("S4 a later native EM party is NOT seeded (the one shot is spent)", s4.r.mgr.partyCount() == 0 && s4.r.mgr.lastOrigin() == null);
        check("S4 the unseeded native party is reverted as stale face state", s4.em.of(a4.id) == null && s4.em.of(b4.id) == null);

        // ---------- S5: second boot (store present, seed NOT armed) - stale faces cleared in BOTH links, never adopted ----------
        W s5 = new W(false); var a5 = s5.j("Ana"); var b5 = s5.j("Ben");
        EmSyncTest.Em.P ep5 = s5.em.makeEm(a5.id); s5.em.emJoinRaw(b5.id, ep5);
        s5.mc.make("Native", a5.id); s5.mc.join(b5.id, "Native");
        s5.r.ctl.playerJoin(a5); s5.r.ctl.playerJoin(b5); s5.ticks(8);
        check("S5 no roster is adopted on a stored boot", s5.r.mgr.partyCount() == 0 && s5.r.mgr.lastOrigin() == null);
        check("S5 the stale EM membership is cleared", s5.em.of(a5.id) == null && s5.em.of(b5.id) == null);
        check("S5 the stale mcMMO membership is cleared", s5.mc.of(a5.id) == null && s5.mc.of(b5.id) == null && s5.mc.parties.isEmpty());
        check("S5 the mcMMO clear is logged as stale face state", s5.logged("cleared from mcMMO party 'Native'"));

        // ---------- S6: Q4 - target relogs inside TTL: invite lives, both sides told on return, accept works ----------
        W s6 = new W(false); var a6 = s6.j("Ana"); var b6 = s6.j("Ben");
        s6.r.ctl.command(a6, new String[]{"invite", "Ben"});
        check("S6 invite created", s6.r.mgr.hasInvite(a6.id, b6.id, s6.r.env.now()));
        s6.quit(b6);
        check("S6 the invite survives the target's quit (Q4)", s6.r.mgr.hasInvite(a6.id, b6.id, s6.r.env.now()));
        s6.r.env.t += 30_000;
        a6.clear(); b6.clear();
        s6.rejoin(b6);
        check("S6 the invite is still live after the relog", s6.r.mgr.hasInvite(a6.id, b6.id, s6.r.env.now()));
        check("S6 the returning target is told the invite waits", b6.heard("still waiting for you"));
        check("S6 the inviter is told the target is back (Q4)", a6.heard("is back - your party invite to them is still open"));
        s6.r.ctl.command(b6, new String[]{"accept"});
        check("S6 the survived invite accepts into a party", s6.r.mgr.sameParty(a6.id, b6.id));

        // ---------- S7: Q4 - inviter relogs inside TTL: invite lives and accepts ----------
        W s7 = new W(false); var a7 = s7.j("Ana"); var b7 = s7.j("Ben");
        s7.r.ctl.command(a7, new String[]{"invite", "Ben"});
        s7.quit(a7);
        check("S7 the invite survives the inviter's quit (Q4)", s7.r.mgr.hasInvite(a7.id, b7.id, s7.r.env.now()));
        s7.r.env.t += 20_000;
        s7.rejoin(a7);
        s7.r.ctl.command(b7, new String[]{"accept"});
        check("S7 accept after the inviter's relog joins the party", s7.r.mgr.sameParty(a7.id, b7.id));

        // ---------- S8: Q4 - past TTL the purge still happens, and it is messaged to both sides ----------
        W s8 = new W(false); var a8 = s8.j("Ana"); var b8 = s8.j("Ben"); var c8 = s8.j("Cal");
        s8.r.ctl.command(a8, new String[]{"invite", "Ben"});
        a8.clear(); b8.clear();
        s8.r.env.t += 61_000; s8.r.ctl.tick();
        check("S8 an expired invite is gone", !s8.r.mgr.hasInvite(a8.id, b8.id, s8.r.env.now()));
        check("S8 expiry is messaged to BOTH sides", a8.heard("ran out of time") && b8.heard("ran out of time"));
        s8.r.ctl.command(a8, new String[]{"invite", "Cal"});
        s8.r.env.t += 10_000; s8.quit(c8);                                               // Cal logs off mid-TTL
        s8.r.env.t += 60_000;                                                            // and returns after it
        c8.clear(); s8.rejoin(c8);
        check("S8 a relog after the TTL does not resurrect the invite", !s8.r.mgr.hasInvite(a8.id, c8.id, s8.r.env.now()) && !c8.heard("still waiting"));

        // ---------- S9: Q4 at the PartyManager layer ----------
        Set<UUID> on = new HashSet<>(); UUID a9 = UUID.randomUUID(), b9 = UUID.randomUUID(); on.add(a9); on.add(b9);
        PartyManager m9 = new PartyManager(5, 60_000, 300_000, on::contains);
        check("S9 invite ok", m9.invite(a9, b9, 0) == PartyManager.InviteResult.OK);
        m9.onQuit(b9, 1000);
        check("S9 target quit keeps the invite", m9.invitesFor(b9, 2000).size() == 1);
        m9.onQuit(a9, 2000);
        check("S9 inviter quit keeps the invite", m9.hasInvite(a9, b9, 3000));
        check("S9 accept after both relogged inside TTL", m9.accept(b9, null, 4000).result == PartyManager.AcceptResult.OK);
        PartyManager m9b = new PartyManager(5, 60_000, 300_000, on::contains);
        m9b.invite(a9, b9, 0); m9b.onQuit(a9, 1000); on.remove(a9);                      // inviter stays offline
        check("S9 accept while the inviter is offline ends the invite (INVITER_OFFLINE)", m9b.accept(b9, null, 2000).result == PartyManager.AcceptResult.INVITER_OFFLINE && m9b.invitesFor(b9, 3000).isEmpty());

        // ---------- S10: UNKNOWN face reads defer the seed - the source is never reverted blind ----------
        W s10 = new W(true); var a10 = s10.j("Ana"); var b10 = s10.j("Ben");
        EmSyncTest.Em.P ep10 = s10.em.makeEm(a10.id); s10.em.emJoinRaw(b10.id, ep10);
        s10.em.unknown.add(a10.id); s10.em.unknown.add(b10.id);
        s10.r.ctl.playerJoin(a10); s10.ticks(4);
        check("S10 an unreadable EM picture seeds nothing yet", s10.r.mgr.partyCount() == 0);
        check("S10 the possible seed source is NOT reverted while unreadable", s10.inEm(a10, b10));
        s10.em.unknown.clear(); s10.ticks(4);
        check("S10 once readable, the seed lands EM-first", s10.r.mgr.sameParty(a10.id, b10.id) && s10.r.mgr.originOfPlayer(a10.id) == RosterOrigin.LOGIN_REBUILD);
        check("S10 the seeded face is still standing afterwards", s10.inEm(a10, b10));

        // ---------- S11: solo face listings seed nothing and are cleared as stale ----------
        W s11 = new W(true); var a11 = s11.j("Ana");
        s11.em.makeEm(a11.id);
        s11.mc.make("Solo", a11.id);
        s11.r.ctl.playerJoin(a11); s11.ticks(6);
        check("S11 a solo listing is not a party: nothing seeded", s11.r.mgr.partyCount() == 0 && s11.r.mgr.lastOrigin() == null);
        check("S11 the solo stale listings are cleared in both faces", s11.em.of(a11.id) == null && s11.mc.of(a11.id) == null);
    }
}
