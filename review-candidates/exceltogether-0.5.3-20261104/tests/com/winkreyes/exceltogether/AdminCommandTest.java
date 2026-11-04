package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.5.3: the /excelparty admin branch - the console admin surface Mike's standing rule
 * requires ("We should always have a way to help the players", Nov 3, 2026). The routing
 * (console accepted, player needs excelparty.admin) lives in ExcelTogether.onCommand and is
 * proven on the rig; what is proven HERE is Controller.adminCommand itself: status reports
 * the hub party and both mirrors accurately, remove lands through the same leave semantics
 * as a voluntary leave (mirror leave drives + suppression in both syncs, so it sticks),
 * disband cascades the same way, and bad input answers with usage instead of throwing.
 *
 * Run: java -cp <classes> com.winkreyes.exceltogether.AdminCommandTest
 */
public class AdminCommandTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    static final class W {
        final PartyFeatureTest.Rig r; final EmSyncTest.Em em = new EmSyncTest.Em(); final McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc();
        W(int cap) {
            r = new PartyFeatureTest.Rig(cap); r.ctl.setEmLink(em); r.ctl.setLink(mc);
            r.ctl.setPlayerDirectory(() -> { List<PlayerView> out = new ArrayList<>(); out.addAll(r.env.on.values()); return out; });
        }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); em.names.put(p.id, n); mc.names.put(p.id, n); return p; }
        void tick(int n) { for (int i = 0; i < n; i++) { r.env.t += 2000; r.ctl.tick(); } }
        void emPolls(int n) { for (int i = 0; i < n; i++) { r.env.t += 2000; r.ctl.emSync().poll(); } }
        boolean ours(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        void meshed(PartyFeatureTest.FP a, PartyFeatureTest.FP b) {
            r.mgr.adopt(b.id, a.id);
            mc.make("mesh", a.id); mc.join(b.id, "mesh");
            r.ctl.mcmmoChanged(List.of(a.id, b.id), List.of());
            tick(2); emPolls(1);
        }
        List<String> admin(String... a) { List<String> out = new ArrayList<>(); r.ctl.adminCommand(out::add, a); return out; }
        static boolean said(List<String> out, String sub) { for (String s : out) if (s.contains(sub)) return true; return false; }
    }

    public static void main(String[] args) { tests(); System.out.println("AdminCommandTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- A1: status reports the hub party and both mirrors ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester"); var solo = w.j("Solo");
            w.meshed(marvin, jester);
            List<String> out = w.admin("admin", "status", "Jester");
            check("A1 status names the player", W.said(out, "Party status for Jester"));
            check("A1 status reports the Together party and leader", W.said(out, "ExcelTogether: in a party of 2/5, leader Marvin"));
            check("A1 status lists both members", W.said(out, "Members: Marvin (leader), Jester"));
            check("A1 status reports the mcMMO mirror", W.said(out, "mcMMO: in party 'mesh' (leader Marvin, 2 members)"));
            check("A1 status reports the EliteMobs mirror", W.said(out, "EliteMobs: in a party (leader Marvin, 2 members)"));
            check("A1 status reports no suppression", W.said(out, "Suppressed (will not be auto-rejoined): mcMMO no, EliteMobs no"));
            List<String> soloOut = w.admin("admin", "status", "Solo");
            check("A1 status for a partyless player", W.said(soloOut, "ExcelTogether: not in a party."));
            List<String> none = w.admin("admin", "status", "Nobody");
            check("A1 status for an unknown name", W.said(none, "No online or partied player matches 'Nobody'."));
        }
        // ---------- A2: remove lands like a voluntary leave and sticks ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            List<String> out = w.admin("admin", "remove", "Jester");
            check("A2 remove replies with the removal", !out.isEmpty() && out.get(0).startsWith("Removed Jester from the party"));
            check("A2 Jester is out of the Together party", !w.r.mgr.inParty(jester.id));
            check("A2 the two-man party ended for Marvin too", w.r.mgr.partyOf(marvin.id) == null);
            check("A2 the mcMMO mirror was driven out", w.mc.of(jester.id) == null && w.mc.of(marvin.id) == null);
            check("A2 the EliteMobs mirror was driven out", w.em.of(jester.id) == null);
            check("A2 suppression recorded in both syncs at once", w.r.ctl.emSync().isSuppressed(jester.id)
                    && W.said(w.admin("admin", "status", "Jester"), "Suppressed (will not be auto-rejoined): mcMMO yes, EliteMobs yes"));
            check("A2 Jester was told", jester.heard("removed from the party by an admin"));
            check("A2 Marvin heard the admin wording", marvin.heard("Jester was removed from the party by an admin"));
            w.tick(4); w.emPolls(2);
            check("A2 polls do not resurrect the removed player", !w.r.mgr.inParty(jester.id) && w.em.of(jester.id) == null && w.mc.of(jester.id) == null);
        }
        // ---------- A3: disband cascades and sticks ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            List<String> out = w.admin("admin", "disband", "Marvin");
            check("A3 disband replies with the disband", !out.isEmpty() && out.get(0).startsWith("Disbanded the party of 2 (leader Marvin)"));
            check("A3 both are out of Together", w.r.mgr.partyOf(marvin.id) == null && w.r.mgr.partyOf(jester.id) == null);
            check("A3 both mirrors are torn down", w.mc.of(jester.id) == null && w.em.of(jester.id) == null && w.em.of(marvin.id) == null);
            check("A3 suppression recorded for both", w.r.ctl.emSync().isSuppressed(marvin.id) && w.r.ctl.emSync().isSuppressed(jester.id));
            check("A3 both were told", marvin.heard("Your party was disbanded by an admin") && jester.heard("Your party was disbanded by an admin"));
            w.tick(4); w.emPolls(2);
            check("A3 polls do not resurrect the disbanded party", w.r.mgr.partyOf(marvin.id) == null && w.em.of(jester.id) == null);
        }
        // ---------- A4: edges answer cleanly ----------
        {
            W w = new W(5);
            var solo = w.j("Solo"); var ari = w.j("Ari"); var aria = w.j("Aria");
            check("A4 remove a partyless player", W.said(w.admin("admin", "remove", "Solo"), "Solo is not in a party."));
            check("A4 disband a partyless player", W.said(w.admin("admin", "disband", "Solo"), "Solo is not in a party."));
            check("A4 bare admin shows usage", W.said(w.admin("admin"), "Usage: /excelparty admin status <player> | remove <player> | disband <player>"));
            check("A4 unknown subcommand shows usage", W.said(w.admin("admin", "bogus", "Solo"), "Usage: /excelparty admin"));
            check("A4 missing target shows usage", W.said(w.admin("admin", "status"), "Usage: /excelparty admin status <player>"));
            check("A4 ambiguous name is refused with the candidates", W.said(w.admin("admin", "status", "Ar"), "matches more than one player"));
        }
    }
}
