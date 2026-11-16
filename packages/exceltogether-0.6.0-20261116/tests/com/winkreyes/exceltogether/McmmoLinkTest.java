package com.winkreyes.exceltogether;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;

/**
 * 0.6.0 (P-PARTY-REBUILD-1 unit 2 / U2-D1): the one-way mcMMO link. This suite
 * REPLACES the 0.5.6 link suite: the old wiring checks pinned invite/accept/
 * cancel mirroring into mcMMO and the old reflect checks exercised the link's
 * invite machinery (invite sets an mcMMO invite, accept joins via the invite,
 * cancel tidies solo parties, FULL/LOCKED invite outcomes) - all deleted by
 * design, because invites live hub-side only now. The replacements pin:
 * (wiring) a hub invite touches mcMMO NOT AT ALL, an accept reconciles the
 * mirror, leave/kick/disband/grace removals empty the mirror, and a throwing
 * or absent link never breaks the hub flow; (reflect) the reconcile bindings
 * against the mcMMO-shaped stub - createMirrorParty with Q5 naming,
 * addToParty (including the drift move), removeFromParty scoped to the named
 * party, and the NEW setPartyLeader binding (PartyManager.setPartyLeader,
 * the call PartyAPI.setPartyLeader delegates to in the fork).
 */
public class McmmoLinkTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static final Logger LOG = Logger.getLogger("mcmmolinktest");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    // ---------- Controller wiring against the one-way fake (McmmoSyncTest.Mc) ----------
    static McmmoSyncTest.Mc linkFor(PartyFeatureTest.Rig r, PartyFeatureTest.FP... ps) {
        McmmoSyncTest.Mc mc = new McmmoSyncTest.Mc();
        for (PartyFeatureTest.FP p : ps) mc.names.put(p.id, p.real);
        r.ctl.setLink(mc);
        return mc;
    }

    static void wiringTests() {
        PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP a = r.java("Ann"), b = r.java("Bob"), c = r.java("Cy");
        McmmoSyncTest.Mc mc = linkFor(r, a, b, c);
        r.ctl.command(a, new String[]{"invite", "Bob"});
        check("invite is hub-side only: mcMMO untouched", mc.drives.isEmpty() && mc.parties.isEmpty());
        r.ctl.command(b, new String[]{"accept"});
        check("accept reconciles the mirror: both in one mcMMO party, inviter leads", mc.of(a.id) != null && mc.of(a.id) == mc.of(b.id) && mc.of(a.id).leader.equals(a.id) && r.mgr.sameParty(a.id, b.id));
        int drivesAfterAccept = mc.drives.size();
        r.ctl.command(a, new String[]{"invite", "Bob"});
        check("refused invite (already member) drives nothing", mc.drives.size() == drivesAfterAccept);
        r.ctl.command(a, new String[]{"invite", "Ann"});
        check("self invite drives nothing", mc.drives.size() == drivesAfterAccept);
        r.ctl.command(a, new String[]{"invite", "Cy"}); r.ctl.command(a, new String[]{"invite", "Cy"});
        check("duplicate invite drives nothing", mc.drives.size() == drivesAfterAccept);
        r.ctl.command(c, new String[]{"decline"});
        check("decline has no mcMMO invite to cancel: mirror unchanged", mc.of(a.id) != null && mc.of(a.id).m.size() == 2);
        r.ctl.command(b, new String[]{"invite", "Cy"});
        r.env.t += 61_000; r.ctl.tick();
        check("invite expiry touches no mcMMO state", mc.of(c.id) == null && mc.of(a.id).m.size() == 2);
        // leave / kick / disband empty the mirror for the removed players
        PartyFeatureTest.Rig q = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP x = q.java("Xan"), y = q.java("Yui"), z = q.java("Zed");
        McmmoSyncTest.Mc mq = linkFor(q, x, y, z);
        q.ctl.command(x, new String[]{"invite", "Yui"}); q.ctl.command(y, new String[]{"accept"});
        q.ctl.command(x, new String[]{"invite", "Zed"}); q.ctl.command(z, new String[]{"accept"});
        check("trio mirrored", mq.of(x.id).m.size() == 3);
        q.ctl.command(z, new String[]{"leave", "yes"});
        check("leave removes the leaver from the mirror", mq.of(z.id) == null && mq.of(x.id).m.size() == 2);
        q.ctl.command(x, new String[]{"invite", "Zed"}); q.ctl.command(z, new String[]{"accept"});
        q.ctl.command(x, new String[]{"kick", "Zed"});
        check("kick removes the kicked player from the mirror", mq.of(z.id) == null && mq.of(x.id).m.size() == 2);
        q.ctl.command(y, new String[]{"leave", "yes"});
        check("disband empties the mirror", mq.parties.isEmpty());
        // grace removal
        PartyFeatureTest.Rig g = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP g1 = g.java("Gus"), g2 = g.java("Hal"), g3 = g.java("Ivy");
        McmmoSyncTest.Mc mg = linkFor(g, g1, g2, g3);
        g.ctl.command(g1, new String[]{"invite", "Hal"}); g.ctl.command(g2, new String[]{"accept"});
        g.ctl.command(g1, new String[]{"invite", "Ivy"}); g.ctl.command(g3, new String[]{"accept"});
        g.env.on.remove(g3.id); g.ctl.playerQuit(g3.id); g.env.t += 301_000; g.ctl.tick();
        check("offline too long removes them from the mirror", mg.of(g3.id) == null && mg.of(g1.id).m.size() == 2);
        // a throwing link never breaks ExcelTogether
        PartyFeatureTest.Rig k = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP k1 = k.java("Kay"), k2 = k.java("Lee");
        McmmoSyncTest.Mc mk = linkFor(k, k1, k2); mk.boom = true;
        k.ctl.command(k1, new String[]{"invite", "Lee"}); k.ctl.command(k2, new String[]{"accept"});
        k.ctl.command(k2, new String[]{"leave", "yes"}); k.env.t += 61_000; k.ctl.tick();
        check("a link that throws never breaks ExcelTogether", !k.mgr.inParty(k1.id) && k1.heard("left the party"));
        // absent link: whole flow with NONE
        PartyFeatureTest.Rig o = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP o1 = o.java("Moe"), o2 = o.java("Nia");
        o.ctl.setLink(null);
        o.ctl.command(o1, new String[]{"invite", "Nia"}); o.ctl.command(o2, new String[]{"accept"}); o.ctl.command(o2, new String[]{"leave", "yes"});
        check("no mcMMO link (null): full flow works", !o.mgr.inParty(o1.id));
        // compass click path: the question alone touches nothing; the invite is hub-side too
        PartyFeatureTest.Rig cc = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP c1 = cc.java("Sam"), c2 = cc.java("Tia");
        McmmoSyncTest.Mc mcc = linkFor(cc, c1, c2);
        cc.ctl.compassClick(c1, c2);
        check("compass question alone does not touch mcMMO", mcc.drives.isEmpty() && mcc.parties.isEmpty());
        cc.ctl.command(c1, new String[]{"cancel"}); cc.ctl.command(c1, new String[]{"confirm"});
        check("compass cancel does not touch mcMMO", mcc.drives.isEmpty());
        cc.env.t += 2500; cc.ctl.compassClick(c1, c2); cc.ctl.command(c1, new String[]{"confirm"});
        check("compass Yes sends a hub invite only", mcc.drives.isEmpty() && cc.mgr.hasInvite(c1.id, c2.id, cc.env.now()));
        cc.ctl.command(c2, new String[]{"accept"});
        check("compass invite accept reconciles the mirror", mcc.of(c1.id) != null && mcc.of(c1.id) == mcc.of(c2.id));
    }

    // ---------- reflective link against the mcMMO-shaped stub ----------
    static Player player(UUID id, String name, Set<String> perms) {
        return (Player) Proxy.newProxyInstance(McmmoLinkTest.class.getClassLoader(), new Class<?>[]{Player.class}, (p, m, a) -> switch (m.getName()) {
            case "getUniqueId" -> id; case "getName" -> name;
            case "hasPermission" -> perms.contains((String) a[0]);
            case "hashCode" -> id.hashCode(); case "equals" -> p == a[0]; case "toString" -> name;
            case "isOnline" -> true;
            default -> m.getReturnType() == boolean.class ? (Object) false : m.getReturnType() == int.class ? (Object) 0 : null;
        });
    }
    static final class World {
        final Map<UUID, Player> on = new HashMap<>(); final Map<UUID, Player> known = new HashMap<>();
        Player add(String n) { return add(n, Set.of()); }
        Player add(String n, Set<String> perms) { UUID id = UUID.randomUUID(); Player p = player(id, n, perms); on.put(id, p); known.put(id, p); UserManager.players.put(id, new com.gmail.nossr50.datatypes.player.McMMOPlayer(p)); return p; }
        com.gmail.nossr50.datatypes.player.McMMOPlayer mm(Player p) { return UserManager.players.get(p.getUniqueId()); }
        ReflectMcmmoLink link() { return new ReflectMcmmoLink(McmmoLinkTest.class.getClassLoader(), on::get, known::get, LOG); }
    }
    static World fresh() { mcMMO.p = new mcMMO(); UserManager.players.clear(); return new World(); }
    static com.gmail.nossr50.party.PartyManager pm() { return mcMMO.p.getPartyManager(); }
    static UUID id(Player p) { return p.getUniqueId(); }

    static void reflectTests() {
        World w = fresh(); ReflectMcmmoLink l = w.link();
        check("probe: nothing missing against the stub (addToParty + setPartyLeader bindings resolve)", l.available() && l.missing().isEmpty());
        Player a = w.add("Ann"), b = w.add("Bob"), c = w.add("Cy"), d = w.add("Dee");
        // createMirrorParty: leader-named, leader is the sole first member
        String name = l.createMirrorParty(id(a), "Ann", "party1");
        check("createMirrorParty returns the leader-based name", "Ann".equals(name) && w.mm(a).getParty() != null && w.mm(a).getParty().getName().equals("Ann"));
        check("the creator leads the mirror party", w.mm(a).getParty().getLeader().getUniqueId().equals(id(a)));
        // addToParty joins the rest of the roster
        check("addToParty joins a roster member", l.addToParty(id(b), "Ann") && w.mm(b).getParty() == w.mm(a).getParty() && w.mm(a).getParty().getMembers().size() == 2);
        check("addToParty is idempotent for a member already in", l.addToParty(id(b), "Ann") && w.mm(a).getParty().getMembers().size() == 2);
        check("addToParty to a missing party fails soft", !l.addToParty(id(c), "Nope") && w.mm(c).getParty() == null);
        // setPartyLeader: the new binding (U2-D1)
        check("setPartyLeader moves leadership to a member", l.setPartyLeader("Ann", id(b), "Bob") && w.mm(a).getParty().getLeader().getUniqueId().equals(id(b)));
        check("setPartyLeader to the current leader is a quiet success", l.setPartyLeader("Ann", id(b), "Bob"));
        check("setPartyLeader refuses a non-member", !l.setPartyLeader("Ann", id(c), "Cy") && w.mm(a).getParty().getLeader().getUniqueId().equals(id(b)));
        check("setPartyLeader on a missing party fails soft", !l.setPartyLeader("Nope", id(a), "Ann"));
        check("leadership restored to the hub leader", l.setPartyLeader("Ann", id(a), "Ann") && w.mm(a).getParty().getLeader().getUniqueId().equals(id(a)));
        // addToParty as the drift move: a member of another party is moved, the other party keeps its own
        pm().createParty(w.mm(c), "Cys", null); pm().addToParty(w.mm(d), pm().getParty("Cys"));
        check("addToParty moves a player out of a foreign party into the mirror", l.addToParty(id(c), "Ann") && w.mm(c).getParty().getName().equals("Ann") && w.mm(d).getParty().getName().equals("Cys"));
        // removeFromParty is scoped to the named party
        l.removeFromParty(id(c), "Cys");
        check("removeFromParty with the wrong name touches nothing", w.mm(c).getParty() != null && w.mm(c).getParty().getName().equals("Ann"));
        l.removeFromParty(id(c), "Ann");
        check("removeFromParty removes from the named party only", w.mm(c).getParty() == null && w.mm(a).getParty().getMembers().size() == 2 && w.mm(d).getParty() != null);
        // offline removal (the kicked-after-grace path)
        w.on.remove(id(b)); l.removeFromParty(id(b), "Ann");
        check("removeFromParty works for an offline player", !w.mm(a).getParty().getMembers().containsKey(id(b)));
        l.removeFromParty(id(a), "Ann");
        check("removing the last member dissolves the mirror party", pm().getParty("Ann") == null);
        // Q5 naming: leader-name collision falls back to the hub-id name; symbols stripped
        World w2 = fresh(); ReflectMcmmoLink l2 = w2.link();
        Player m1 = w2.add("Mike"), m2 = w2.add("Kay"), lou = w2.add("Lou");
        pm().createParty(w2.mm(lou), "Mike", null);
        String fb = l2.createMirrorParty(id(m1), "Mike", "party42");
        check("name taken: hub-id fallback name used", "party42".equals(fb) && w2.mm(m1).getParty().getName().equals("party42") && w2.mm(lou).getParty().getName().equals("Mike"));
        World w3 = fresh(); ReflectMcmmoLink l3 = w3.link(); Player n1 = w3.add("Max"), n2 = w3.add("Ned");
        String cleaned = l3.createMirrorParty(id(n1), ".Max party!", "party9");
        check("dot and symbols stripped from the mirror name", "Maxparty".equals(cleaned));
        World w4 = fresh(); ReflectMcmmoLink l4 = w4.link(); Player r1 = w4.add("R1"), r2 = w4.add("R2");
        check("empty name falls back to Family", "Family".equals(l4.createMirrorParty(id(r1), "...", "...")));
        // a leader already in an mcMMO party is moved out before the mirror is created
        World w5 = fresh(); ReflectMcmmoLink l5 = w5.link(); Player s1 = w5.add("Sol"), s2 = w5.add("Tom");
        pm().createParty(w5.mm(s1), "Sols", null); pm().addToParty(w5.mm(s2), pm().getParty("Sols"));
        String made = l5.createMirrorParty(id(s1), "Sol", "party5");
        check("createMirrorParty moves the leader out of their old party first", "Sol".equals(made) && w5.mm(s1).getParty().getName().equals("Sol") && w5.mm(s2).getParty().getName().equals("Sols"));
        // read-only views used by drift detection
        World w6 = fresh(); ReflectMcmmoLink l6 = w6.link(); Player t1 = w6.add("Tia"), t2 = w6.add("Uri"), t3 = w6.add("Val");
        check("partyOf: no party reads as NONE", l6.partyOf(id(t1)) == McmmoLink.McParty.NONE);
        l6.createMirrorParty(id(t1), "Tia", "party6"); l6.addToParty(id(t2), "Tia");
        McmmoLink.McParty v = l6.partyOf(id(t2));
        check("partyOf: name, leader and both members", v.real() && v.name.equals("Tia") && id(t1).equals(v.leader) && v.members.size() == 2 && v.members.containsKey(id(t2)));
        w6.on.remove(id(t2));
        check("partyOf works for an offline player", l6.partyOf(id(t2)).real() && l6.partyOf(id(t2)).members.size() == 2);
        check("partyByName finds it, unknown name is NONE", l6.partyByName("Tia").real() && l6.partyByName("Nope") == McmmoLink.McParty.NONE);
        UserManager.players.remove(id(t3));
        check("profile not loaded reads as UNKNOWN, never NONE", l6.partyOf(id(t3)) == McmmoLink.McParty.UNKNOWN);
        check("createMirrorParty with an unloaded profile fails soft (retry later)", l6.createMirrorParty(id(t3), "Val", "party7") == null);
        check("addToParty with an unloaded profile fails soft", !l6.addToParty(id(t3), "Tia"));
        // party system off / internals failing: quiet, never an exception
        World w7 = fresh(); ReflectMcmmoLink l7 = w7.link(); Player z1 = w7.add("Zed"), z2 = w7.add("Yui");
        mcMMO.p.partyConfig.enabled = false;
        check("mcMMO party system off: create fails soft, nothing made", l7.createMirrorParty(id(z1), "Zed", "party8") == null && pm().parties.isEmpty());
        mcMMO.p.partyConfig.enabled = true;
        mcMMO.p = null;
        check("mcMMO internals failing: reconcile calls fail soft, not an exception", l7.createMirrorParty(id(z1), "Zed", "party8") == null && !l7.addToParty(id(z2), "Zed") && !l7.setPartyLeader("Zed", id(z1), "Zed"));
        l7.removeFromParty(id(z1), "Zed");
        check("remove survives too", true);
    }

    static void absentTests() throws Exception {
        List<String> miss = ReflectMcmmoLink.probe(ClassLoader.getPlatformClassLoader());
        check("mcMMO absent: probe lists missing classes", !miss.isEmpty() && miss.get(0).startsWith("class "));
        ReflectMcmmoLink l = new ReflectMcmmoLink(ClassLoader.getPlatformClassLoader(), x -> null, x -> null, LOG);
        check("mcMMO absent: link unavailable and every reconcile call is a quiet no-op", !l.available()
                && l.createMirrorParty(UUID.randomUUID(), "x", "y") == null
                && !l.addToParty(UUID.randomUUID(), "x") && !l.setPartyLeader("x", UUID.randomUUID(), "y"));
        l.removeFromParty(UUID.randomUUID(), "x");
        check("mcMMO absent: NONE link", !McmmoLink.NONE.available() && McmmoLink.NONE.createMirrorParty(null, null, null) == null);
        // real mcMMO jar signature check (names only; never run)
        String jar = System.getProperty("mcmmojar");
        if (jar != null && new java.io.File(jar).exists()) {
            ClassLoader app = McmmoLinkTest.class.getClassLoader();
            ClassLoader hide = new ClassLoader(app) {
                @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
                    if (n.startsWith("com.gmail.nossr50.")) throw new ClassNotFoundException(n);
                    return super.loadClass(n, r);
                }
            };
            ClassLoader real = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL()}, hide);
            List<String> m2 = ReflectMcmmoLink.probe(real);
            check("REAL mcMMO jar: every method the link uses exists (incl. addToParty + setPartyLeader) " + m2, m2.isEmpty());
        } else check("real mcMMO jar present for signature check (UNCONFIRMED if missing)", false);
    }

    public static void main(String[] a) throws Exception {
        wiringTests(); reflectTests(); absentTests();
        System.out.println("McmmoLinkTest: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
