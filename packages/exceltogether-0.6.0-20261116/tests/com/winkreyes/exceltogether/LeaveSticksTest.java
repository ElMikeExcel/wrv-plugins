package com.winkreyes.exceltogether;

import java.util.*;

/**
 * 0.5.3 regression suite for the Nov 3, 2026 party-leave bug (BUG-VERDICT.md): a voluntary
 * leave at ANY door of the one party must stick. Scenarios L1-L5 exercise the convicted
 * paths - the EliteMobs door (EmSync Case C materialise resurrected the leaver), the mcMMO
 * door (a hand disband dissolved Together with mirror=false, so EmSync Case B rebuilt the
 * party from the untouched EliteMobs roster), the relog amplifier, and the partyKey-keyed
 * suppression a re-created EM party defeats. L6 is the no-regression guard: a disconnect is
 * NOT a leave and must keep materialising exactly as 0.5.2 did.
 *
 * Against the 0.5.2 tree L1-L5 fail and L6 passes; against 0.5.3 all pass.
 * 0.6.0 (U2-D1): L2/L4 are RE-PINNED - an mcMMO-door disband is no longer a door
 * act at all (mcMMO is a one-way mirror), so those scenarios now assert the
 * roster survives and the mirror is rebuilt. L1/L3/L6 are unchanged in
 * substance (the EM door and the Together door still end the party everywhere).
 * 0.6.0 (U2-D2, merged run 407): L5's tail is RE-PINNED - a fresh native EM
 * group no longer adopts (Case B deleted); it is reverted and dismantled.
 * L5's suppression/rotation head is unchanged in substance.
 * Run: java -cp <classes> com.winkreyes.exceltogether.LeaveSticksTest
 */
public class LeaveSticksTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    /** One rig, both mesh fakes (the EmSyncTest fake EliteMobs + the McmmoSyncTest fake mcMMO). */
    static final class W {
        final PartyFeatureTest.Rig r; final EmSyncTest.Em em = new EmSyncTest.Em(); final McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc();
        W(int cap) { r = new PartyFeatureTest.Rig(cap); r.ctl.setEmLink(em); r.ctl.setLink(mc); }
        PartyFeatureTest.FP j(String n) { PartyFeatureTest.FP p = r.java(n); em.names.put(p.id, n); mc.names.put(p.id, n); return p; }
        void tick(int n) { for (int i = 0; i < n; i++) { r.env.t += 2000; r.ctl.tick(); } }
        void emPolls(int n) { for (int i = 0; i < n; i++) { r.env.t += 2000; r.ctl.emSync().poll(); } }
        boolean ours(PartyFeatureTest.FP a, PartyFeatureTest.FP b) { return r.mgr.sameParty(a.id, b.id); }
        /** A fully meshed pair: Together party (a leads), mcMMO party, EliteMobs twin, both syncs settled. */
        void meshed(PartyFeatureTest.FP a, PartyFeatureTest.FP b) {
            r.mgr.adopt(b.id, a.id);
            mc.make("mesh", a.id); mc.join(b.id, "mesh");
            r.ctl.mcmmoChanged(List.of(a.id, b.id), List.of());
            tick(2); emPolls(1);
        }
        /** A fully meshed trio under leader a. */
        void meshed3(PartyFeatureTest.FP a, PartyFeatureTest.FP b, PartyFeatureTest.FP c) {
            r.mgr.adopt(b.id, a.id); r.mgr.adopt(c.id, a.id);
            mc.make("mesh", a.id); mc.join(b.id, "mesh"); mc.join(c.id, "mesh");
            r.ctl.mcmmoChanged(List.of(a.id, b.id, c.id), List.of());
            tick(2); emPolls(1);
        }
        void rest(String label, PartyFeatureTest.FP... ps) {
            // §11C convention (as in EmSyncTest): at rest the mesh drives nothing into EM and
            // says nothing. The mcMMO leg is excluded on purpose - McmmoSync re-marks its
            // mirrored bookkeeping on every reconcile in 0.5.2 and 0.5.3 alike.
            int d = em.drives.size();
            int[] counts = new int[ps.length];
            for (int i = 0; i < ps.length; i++) counts[i] = ps[i].said.size();
            tick(2); emPolls(2);
            check(label + " [rest: no EM drives]", em.drives.size() == d);
            boolean quiet = true;
            for (int i = 0; i < ps.length; i++) if (ps[i].said.size() != counts[i]) quiet = false;
            check(label + " [rest: silent]", quiet);
        }
    }

    public static void main(String[] args) { tests(); System.out.println("LeaveSticksTest: " + pass + " passed, " + fail + " failed"); if (fail > 0) System.exit(1); }

    static void tests() {
        // ---------- L1: the EM-door leave sticks (Jester's door) ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            check("L1 setup meshed everywhere", w.ours(marvin, jester) && w.em.inEm(marvin.id, jester.id) && w.mc.of(jester.id) != null);
            w.em.emLeaveRaw(jester.id);                       // Jester: /em party leave (EM state only)
            w.tick(4); w.emPolls(2);
            check("L1 Jester is out of the Together party", !w.r.mgr.inParty(jester.id));
            check("L1 the two-man party ended for Marvin too", w.r.mgr.partyOf(marvin.id) == null);
            check("L1 Jester was NOT driven back into EliteMobs", w.em.of(jester.id) == null);
            check("L1 the mcMMO mirror was told (leave cascade)", w.mc.of(jester.id) == null && w.mc.of(marvin.id) == null);
            check("L1 Jester heard he left", jester.heard("left the party"));
            w.rest("L1", marvin, jester);
        }
        // ---------- L2: an mcMMO-door disband is DRIFT since 0.6.0 (U2-D1 re-pin) ----------
        // The 0.5.3 version of this scenario pinned the opposite: a hand disband at the
        // mcMMO door dissolved Together (McmmoSync quietLeave) and tore down the EM face.
        // Under the one-door design mcMMO is never a door: the disband mutates only the
        // mirror, the roster of record is untouched, and the mirror is rebuilt from it.
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            check("L2 setup meshed everywhere", w.ours(marvin, jester) && w.em.inEm(marvin.id, jester.id));
            List<UUID> ids = w.r.ctl.mcmmoEventIds(marvin.id, "mesh", null);   // what the plugin collects on the event
            w.mc.quit(jester.id); w.mc.quit(marvin.id);                        // Marvin: /party DISBAND (mcMMO state only)
            w.r.ctl.mcmmoChanged(ids, List.of());
            w.tick(4); w.emPolls(2);
            check("L2 the Together party survives an mcMMO disband (drift, not a door)", w.ours(marvin, jester));
            check("L2 the mcMMO mirror is rebuilt from the roster", w.mc.of(jester.id) != null && w.mc.of(jester.id) == w.mc.of(marvin.id));
            check("L2 the EliteMobs face was left alone", w.em.inEm(marvin.id, jester.id));
            w.rest("L2", marvin, jester);
        }
        // ---------- L3: EM-door leave, then both relog - no resurrection ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            w.em.emLeaveRaw(jester.id);
            w.tick(4); w.emPolls(2);
            check("L3 leave landed before the relogs", !w.r.mgr.inParty(jester.id));
            // both disconnect and return (EM drops quitters itself; Together grace would hold members)
            w.r.env.on.remove(jester.id); w.r.env.on.remove(marvin.id);
            w.r.mgr.onQuit(jester.id, w.r.env.t); w.r.mgr.onQuit(marvin.id, w.r.env.t);
            w.em.emLeaveRaw(jester.id); w.em.emLeaveRaw(marvin.id);
            w.r.env.add(jester); w.r.env.add(marvin);
            w.r.mgr.onJoin(jester.id); w.r.mgr.onJoin(marvin.id);
            w.r.ctl.playerJoin(jester); w.r.ctl.playerJoin(marvin);
            w.tick(6); w.emPolls(2);
            check("L3 after relogs Jester is still out", !w.r.mgr.inParty(jester.id) && w.em.of(jester.id) == null);
            check("L3 after relogs no party re-formed", w.r.mgr.partyOf(marvin.id) == null);
            w.rest("L3", marvin, jester);
        }
        // ---------- L4: mcMMO disband, then both relog - the roster rebuilds the faces (U2-D1 re-pin) ----------
        // 0.5.3 pinned "stays disbanded everywhere" (the disband was a door act). Since
        // U2-D1 the disband is drift: the roster survives it, and the relogs' mirror
        // repair (playerJoin -> mcmmoWatch) rebuilds the mcMMO face from the roster.
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            List<UUID> ids = w.r.ctl.mcmmoEventIds(marvin.id, "mesh", null);
            w.mc.quit(jester.id); w.mc.quit(marvin.id);
            w.r.ctl.mcmmoChanged(ids, List.of());
            w.tick(4); w.emPolls(2);
            check("L4 the roster survived the disband before the relogs", w.ours(marvin, jester));
            w.r.env.on.remove(jester.id); w.r.env.on.remove(marvin.id);
            w.r.mgr.onQuit(jester.id, w.r.env.t); w.r.mgr.onQuit(marvin.id, w.r.env.t);
            w.em.emLeaveRaw(jester.id); w.em.emLeaveRaw(marvin.id);
            w.r.env.add(jester); w.r.env.add(marvin);
            w.r.mgr.onJoin(jester.id); w.r.mgr.onJoin(marvin.id);
            w.r.ctl.playerJoin(jester); w.r.ctl.playerJoin(marvin);
            w.tick(6); w.emPolls(2);
            check("L4 after relogs the party stands everywhere, rebuilt from the roster",
                    w.ours(marvin, jester) && w.em.inEm(marvin.id, jester.id) && w.mc.of(jester.id) != null && w.mc.of(jester.id) == w.mc.of(marvin.id));
            w.rest("L4", marvin, jester);
        }
        // ---------- L5: suppression survives an EM partyKey rotation (same roster); a fresh group is reverted, not adopted (U2-D2 tail) ----------
        {
            W w = new W(5);
            var liv = w.j("Liv"); var xio = w.j("Xio"); var mel = w.j("Mel"); var dee = w.j("Dee");
            w.meshed3(liv, xio, mel);
            w.r.ctl.command(liv, new String[]{"list"});
            check("L5 setup meshed trio", w.em.inEm(liv.id, xio.id) && w.em.inEm(liv.id, mel.id));
            EmSyncTest.Em.P twin = w.em.of(liv.id);
            w.r.ctl.command(liv, new String[]{"kick", "Xio"});
            check("L5 kick took Xio out", !w.ours(liv, xio) && w.ours(liv, mel));
            w.em.emJoinRaw(xio.id, twin);                                   // the EM leave "had not landed"
            EmSyncTest.Em.P twin2 = w.em.makeEm(liv.id);                    // EM re-creates the party: NEW partyKey,
            w.em.emJoinRaw(xio.id, twin2); w.em.emJoinRaw(mel.id, twin2);   // same roster (Liv, Xio, Mel)
            w.em.parties.remove(twin.key);
            w.emPolls(3); w.tick(2);
            check("L5 Xio is NOT re-adopted from the re-created twin", !w.ours(liv, xio));
            check("L5 the rest of the party is untouched", w.ours(liv, mel));
            EmSyncTest.Em.P fresh = w.em.makeEm(dee.id); w.em.emJoinRaw(xio.id, fresh);   // a genuinely fresh group
            w.emPolls(2); w.tick(2);
            // REWRITTEN (0.6.0, U2-D2): a fresh native EM group no longer adopts into Together -
            // Case B is deleted, so the fresh group is reverted like any non-door EM change.
            check("L5 a fresh EM group with a new person is reverted, not adopted (0.6.0)", !w.ours(xio, dee) && w.r.mgr.partyOf(dee.id) == null);
            check("L5 the fresh EM group was dismantled", w.em.of(xio.id) == null && w.em.of(dee.id) == null);
            w.rest("L5", liv, mel);
        }
        // ---------- L6: a disconnect is not a leave (0.5.2 behaviour preserved) ----------
        {
            W w = new W(5);
            var marvin = w.j("Marvin"); var jester = w.j("Jester");
            w.meshed(marvin, jester);
            w.r.env.on.remove(jester.id);
            w.r.ctl.playerQuit(jester.id);
            w.em.emLeaveRaw(jester.id);                       // EM drops quitters itself; the twin survives with Marvin
            w.emPolls(2); w.tick(2);
            check("L6 a quitter stays in the Together party (grace)", w.r.mgr.inParty(jester.id));
            w.r.env.add(jester);
            w.r.ctl.playerJoin(jester);
            w.tick(4); w.emPolls(2);
            check("L6 on return the EM face is rebuilt", w.em.inEm(marvin.id, jester.id) && w.ours(marvin, jester));
            w.rest("L6", marvin, jester);
        }
    }
}
