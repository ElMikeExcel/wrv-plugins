package com.winkreyes.exceltogether;

import com.gmail.nossr50.datatypes.party.Party;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.util.player.UserManager;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/** WRV-063 mcMMO party link tests: Controller wiring with a fake link, and the reflective link against a stub shaped like mcMMO. */
public class McmmoLinkTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static final Logger LOG = Logger.getLogger("mcmmolinktest");
    static { LOG.setLevel(java.util.logging.Level.OFF); }

    // ---------- fake link for Controller wiring ----------
    static final class FakeLink implements McmmoLink {
        final List<String> calls = new ArrayList<>(); McmmoLink.Mirror inviteRes = Mirror.OK, acceptRes = Mirror.OK; boolean boom = false;
        Predicate<UUID> lastCleanup;
        public boolean available() { return true; }
        public Mirror invite(UUID a, UUID b, String h) { calls.add("invite:" + h); if (boom) throw new IllegalStateException("x"); return inviteRes; }
        public Mirror accept(UUID a, UUID b, String h) { calls.add("accept:" + h); if (boom) throw new IllegalStateException("x"); return acceptRes; }
        public void cancel(UUID a, UUID b) { calls.add("cancel"); if (boom) throw new IllegalStateException("x"); }
        public void leave(UUID a) { calls.add("leave:" + a); if (boom) throw new IllegalStateException("x"); }
        public void cleanup(Predicate<UUID> p) { calls.add("cleanup"); lastCleanup = p; if (boom) throw new IllegalStateException("x"); }
        long count(String prefix) { return calls.stream().filter(c -> c.startsWith(prefix)).count(); }
    }

    static void wiringTests() {
        PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5); FakeLink fl = new FakeLink(); r.ctl.setLink(fl);
        PartyFeatureTest.FP a = r.java("Ann"), b = r.java("Bob"), c = r.java("Cy");
        r.ctl.command(a, new String[]{"invite", "Bob"});
        check("invite mirrors once", fl.count("invite:Ann") == 1);
        check("accept not mirrored before the invitee answers", fl.count("accept") == 0);
        r.ctl.command(b, new String[]{"accept"});
        check("accept mirrors after ours succeeds", fl.count("accept:Ann") == 1 && r.mgr.sameParty(a.id, b.id));
        r.ctl.command(a, new String[]{"invite", "Bob"});
        check("refused invite (already member) is not mirrored", fl.count("invite") == 1);
        r.ctl.command(a, new String[]{"invite", "Ann"});
        check("self invite not mirrored", fl.count("invite") == 1);
        r.ctl.command(a, new String[]{"invite", "Cy"}); r.ctl.command(a, new String[]{"invite", "Cy"});
        check("duplicate invite not mirrored twice", fl.count("invite") == 2);
        r.ctl.command(c, new String[]{"decline"});
        check("decline cancels the mcMMO invite and tidies", fl.count("cancel") == 1 && fl.count("cleanup") >= 1);
        // expiry
        r.ctl.command(b, new String[]{"invite", "Cy"});          // member (not leader) invites
        check("any member invite is mirrored", fl.count("invite:Bob") == 1);
        r.env.t += 61_000; r.ctl.tick();
        check("expiry cancels the mcMMO invite", fl.count("cancel") == 2);
        // accept after our expiry: ours refuses, link never called
        long acc = fl.count("accept");
        r.ctl.command(b, new String[]{"invite", "Cy"}); r.env.t += 61_000;
        r.ctl.command(c, new String[]{"accept"});
        check("expired accept never reaches mcMMO", fl.count("accept") == acc);
        // cleanup predicate follows open invites
        r.ctl.command(a, new String[]{"invite", "Cy"}); r.ctl.command(c, new String[]{"decline"});
        check("cleanup predicate: no open invites", fl.lastCleanup != null && !fl.lastCleanup.test(a.id));
        r.ctl.command(a, new String[]{"invite", "Cy"}); r.env.t += 61_000; r.ctl.tick();
        // leave and kick and disband
        PartyFeatureTest.Rig q = new PartyFeatureTest.Rig(5); FakeLink fq = new FakeLink(); q.ctl.setLink(fq);
        PartyFeatureTest.FP x = q.java("Xan"), y = q.java("Yui"), z = q.java("Zed");
        q.ctl.command(x, new String[]{"invite", "Yui"}); q.ctl.command(y, new String[]{"accept"});
        q.ctl.command(x, new String[]{"invite", "Zed"}); q.ctl.command(z, new String[]{"accept"});
        q.ctl.command(z, new String[]{"leave", "yes"});
        check("leave mirrors the leaver", fq.count("leave:" + z.id) == 1);
        q.ctl.command(x, new String[]{"invite", "Zed"}); q.ctl.command(z, new String[]{"accept"});
        q.ctl.command(x, new String[]{"kick", "Zed"});
        check("kick mirrors the kicked player", fq.count("leave:" + z.id) == 2);
        q.ctl.command(y, new String[]{"leave", "yes"});
        check("disband mirrors the leaver and the one left over", fq.count("leave:" + y.id) == 1 && fq.count("leave:" + x.id) == 1);
        // grace removal
        PartyFeatureTest.Rig g = new PartyFeatureTest.Rig(5); FakeLink fg = new FakeLink(); g.ctl.setLink(fg);
        PartyFeatureTest.FP g1 = g.java("Gus"), g2 = g.java("Hal"), g3 = g.java("Ivy");
        g.ctl.command(g1, new String[]{"invite", "Hal"}); g.ctl.command(g2, new String[]{"accept"});
        g.ctl.command(g1, new String[]{"invite", "Ivy"}); g.ctl.command(g3, new String[]{"accept"});
        g.env.on.remove(g3.id); g.ctl.playerQuit(g3.id); g.env.t += 301_000; g.ctl.tick();
        check("offline too long mirrors the removal", fg.count("leave:" + g3.id) == 1);
        // notes and failures
        for (Object[] row : new Object[][]{{McmmoLink.Mirror.TARGET_IN_OTHER_PARTY, "different mcMMO party"}, {McmmoLink.Mirror.FULL, "mcMMO party is full"}, {McmmoLink.Mirror.LOCKED, "locked"}}) {
            PartyFeatureTest.Rig n = new PartyFeatureTest.Rig(5); FakeLink fn = new FakeLink(); n.ctl.setLink(fn);
            fn.inviteRes = (McmmoLink.Mirror) row[0]; fn.acceptRes = (McmmoLink.Mirror) row[0];
            PartyFeatureTest.FP i1 = n.java("Ola"), i2 = n.java("Pia");
            n.ctl.command(i1, new String[]{"invite", "Pia"});
            check(row[0] + ": inviter told, invite still sent", i1.heard((String) row[1]) && n.mgr.hasInvite(i1.id, i2.id, n.env.now()) && i2.heard("Accept"));
            n.ctl.command(i2, new String[]{"accept"});
            check(row[0] + ": ExcelTogether party still made, invitee told", n.mgr.sameParty(i1.id, i2.id) && i2.heard((String) row[1]));
        }
        for (McmmoLink.Mirror quiet : new McmmoLink.Mirror[]{McmmoLink.Mirror.ABSENT, McmmoLink.Mirror.ERROR, McmmoLink.Mirror.NOT_READY}) {
            PartyFeatureTest.Rig n = new PartyFeatureTest.Rig(5); FakeLink fn = new FakeLink(); n.ctl.setLink(fn); fn.inviteRes = quiet; fn.acceptRes = quiet;
            PartyFeatureTest.FP i1 = n.java("Ola"), i2 = n.java("Pia");
            n.ctl.command(i1, new String[]{"invite", "Pia"}); n.ctl.command(i2, new String[]{"accept"});
            check(quiet + ": no mcMMO talk to players, party fine", !i1.heard("mcMMO") && !i2.heard("mcMMO") && n.mgr.sameParty(i1.id, i2.id));
        }
        PartyFeatureTest.Rig k = new PartyFeatureTest.Rig(5); FakeLink fk = new FakeLink(); fk.boom = true; k.ctl.setLink(fk);
        PartyFeatureTest.FP k1 = k.java("Ash"), k2 = k.java("Lee");
        k.ctl.command(k1, new String[]{"invite", "Lee"}); k.ctl.command(k2, new String[]{"accept"});
        k.ctl.command(k2, new String[]{"leave", "yes"}); k.env.t += 61_000; k.ctl.tick();
        check("a link that throws never breaks ExcelTogether", !k.mgr.inParty(k1.id) && k1.heard("left the party"));
        // absent link: whole flow with NONE
        PartyFeatureTest.Rig o = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP o1 = o.java("Moe"), o2 = o.java("Nia");
        o.ctl.setLink(null);
        o.ctl.command(o1, new String[]{"invite", "Nia"}); o.ctl.command(o2, new String[]{"accept"}); o.ctl.command(o2, new String[]{"leave", "yes"});
        check("no mcMMO link (null): full flow works", !o.mgr.inParty(o1.id));
        // compass click path mirrors only after Yes
        PartyFeatureTest.Rig cc = new PartyFeatureTest.Rig(5); FakeLink fc = new FakeLink(); cc.ctl.setLink(fc);
        PartyFeatureTest.FP c1 = cc.java("Sam"), c2 = cc.java("Tia");
        cc.ctl.compassClick(c1, c2);
        check("compass question alone does not touch mcMMO", fc.calls.isEmpty());
        cc.ctl.command(c1, new String[]{"cancel"}); cc.ctl.command(c1, new String[]{"confirm"});
        check("compass cancel does not touch mcMMO", fc.calls.isEmpty());
        cc.env.t += 2500; cc.ctl.compassClick(c1, c2); cc.ctl.command(c1, new String[]{"confirm"});
        check("compass Yes mirrors the invite", fc.count("invite:Sam") == 1);
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
        Player add(String n, Set<String> perms) { UUID id = UUID.randomUUID(); Player p = player(id, n, perms); on.put(id, p); known.put(id, p); UserManager.players.put(id, new McMMOPlayer(p)); return p; }
        McMMOPlayer mm(Player p) { return UserManager.players.get(p.getUniqueId()); }
        ReflectMcmmoLink link() { return new ReflectMcmmoLink(McmmoLinkTest.class.getClassLoader(), on::get, known::get, LOG); }
    }
    static World fresh() { mcMMO.p = new mcMMO(); UserManager.players.clear(); return new World(); }
    static com.gmail.nossr50.party.PartyManager pm() { return mcMMO.p.getPartyManager(); }
    static UUID id(Player p) { return p.getUniqueId(); }

    static void reflectTests() {
        World w = fresh(); ReflectMcmmoLink l = w.link();
        check("probe: nothing missing against the stub", l.available() && l.missing().isEmpty());
        Player a = w.add("Ann"), b = w.add("Bob"), c = w.add("Cy"), d = w.add("Dee");
        check("invite creates inviter's mcMMO party and sets invite", l.invite(id(a), id(b), "Ann") == McmmoLink.Mirror.OK && w.mm(a).getParty() != null && w.mm(a).getParty().getName().equals("Ann"));
        check("invitee has the invite but is not in the party yet", w.mm(b).getPartyInvite() == w.mm(a).getParty() && w.mm(b).getParty() == null);
        check("accept joins the mcMMO party", l.accept(id(b), id(a), "Ann") == McmmoLink.Mirror.OK && w.mm(b).getParty() == w.mm(a).getParty() && w.mm(a).getParty().getMembers().size() == 2);
        check("invite consumed on join", !w.mm(b).hasPartyInvite());
        check("member (not leader) can invite into the same mcMMO party", l.invite(id(b), id(c), "Bob") == McmmoLink.Mirror.OK && w.mm(c).getPartyInvite() == w.mm(a).getParty());
        check("no second party was created", pm().parties.size() == 1);
        l.cancel(id(c), id(b));
        check("cancel clears the invite we set", !w.mm(c).hasPartyInvite());
        // cancel must not remove an invite from a different party
        World w2 = fresh(); ReflectMcmmoLink l2 = w2.link();
        Player e = w2.add("Eve"), f = w2.add("Fay"), g = w2.add("Gil");
        l2.invite(id(e), id(g), "Eve"); l2.invite(id(f), id(g), "Fay");   // Fay's invite replaces Eve's (mcMMO keeps one)
        l2.cancel(id(g), id(e));
        check("cancel for the other inviter leaves the current invite alone", w2.mm(g).getPartyInvite() != null && w2.mm(g).getPartyInvite().getName().equals("Fay"));
        // solo party created for an invite that never got used is tidied up
        l2.cancel(id(g), id(f)); l2.cleanup(x -> false);
        check("unused invite: solo mcMMO parties we made are removed", pm().parties.isEmpty() && w2.mm(e).getParty() == null && w2.mm(f).getParty() == null);
        // keep while an invite is still open
        World w3 = fresh(); ReflectMcmmoLink l3 = w3.link(); Player h = w3.add("Hal"), i = w3.add("Ivy");
        l3.invite(id(h), id(i), "Hal"); l3.cleanup(x -> true);
        check("cleanup keeps the party while an invite is open", pm().parties.size() == 1);
        // pre-existing mcMMO party is never disbanded by us
        World w4 = fresh(); ReflectMcmmoLink l4 = w4.link(); Player j = w4.add("Jo"), k = w4.add("Kit");
        pm().createParty(w4.mm(j), "Jos", null);
        check("existing mcMMO party is reused", l4.invite(id(j), id(k), "Jo") == McmmoLink.Mirror.OK && w4.mm(k).getPartyInvite().getName().equals("Jos"));
        l4.cancel(id(k), id(j)); l4.cleanup(x -> false);
        check("existing mcMMO party is not removed by cleanup", pm().parties.size() == 1 && w4.mm(j).getParty() != null);
        l4.leave(id(j));
        check("leave for a player we did not put there does nothing", w4.mm(j).getParty() != null);
        // target already in another mcMMO party
        World w5 = fresh(); ReflectMcmmoLink l5 = w5.link(); Player m = w5.add("Moe"), n = w5.add("Nia"), o = w5.add("Oz");
        pm().createParty(w5.mm(n), "Nias", null);
        check("target in another mcMMO party: reported, no invite, no party made for inviter", l5.invite(id(m), id(n), "Moe") == McmmoLink.Mirror.TARGET_IN_OTHER_PARTY && w5.mm(m).getParty() == null && !w5.mm(n).hasPartyInvite());
        check("same on accept", l5.accept(id(n), id(m), "Moe") == McmmoLink.Mirror.TARGET_IN_OTHER_PARTY && w5.mm(n).getParty().getName().equals("Nias"));
        pm().createParty(w5.mm(m), "Moes", null); l5.invite(id(m), id(o), "Moe"); l5.accept(id(o), id(m), "Moe");
        check("target already in the inviter's mcMMO party counts as done", l5.invite(id(m), id(o), "Moe") == McmmoLink.Mirror.OK);
        // mcMMO size limit (smaller than our cap of 5)
        World w6 = fresh(); mcMMO.p.general.maxSize = 2; ReflectMcmmoLink l6 = w6.link();
        Player p1 = w6.add("Pat"), p2 = w6.add("Quin"), p3 = w6.add("Ray");
        check("limit 2: first invite fine", l6.invite(id(p1), id(p2), "Pat") == McmmoLink.Mirror.OK);
        check("limit 2: join fine", l6.accept(id(p2), id(p1), "Pat") == McmmoLink.Mirror.OK);
        check("limit 2: third player reported FULL on invite", l6.invite(id(p1), id(p3), "Pat") == McmmoLink.Mirror.FULL && !w6.mm(p3).hasPartyInvite());
        check("limit 2: third player not joined on accept either", l6.accept(id(p3), id(p1), "Pat") == McmmoLink.Mirror.FULL && w6.mm(p3).getParty() == null);
        // two invites out at one open seat, first fills it
        World w7 = fresh(); mcMMO.p.general.maxSize = 2; ReflectMcmmoLink l7 = w7.link();
        Player s1 = w7.add("Sol"), s2 = w7.add("Tom"), s3 = w7.add("Uma");
        l7.invite(id(s1), id(s2), "Sol"); l7.invite(id(s1), id(s3), "Sol");
        check("first accept fills mcMMO", l7.accept(id(s2), id(s1), "Sol") == McmmoLink.Mirror.OK);
        check("second accept finds mcMMO full", l7.accept(id(s3), id(s1), "Sol") == McmmoLink.Mirror.FULL && w7.mm(s3).getParty() == null && !w7.mm(s3).hasPartyInvite());
        // bypass permission
        World w8 = fresh(); mcMMO.p.general.maxSize = 2; ReflectMcmmoLink l8 = w8.link();
        Player v1 = w8.add("Vic"), v2 = w8.add("Wes"), v3 = w8.add("Xia", Set.of("mcmmo.bypass.partylimit"));
        l8.invite(id(v1), id(v2), "Vic"); l8.accept(id(v2), id(v1), "Vic");
        check("bypass permission joins past the limit", l8.invite(id(v1), id(v3), "Vic") == McmmoLink.Mirror.OK);
        // unlimited, and ours cap 5 with mcMMO unlimited / larger
        World w9 = fresh(); mcMMO.p.general.maxSize = -1; ReflectMcmmoLink l9 = w9.link();
        Player[] many = new Player[6]; for (int q = 0; q < 6; q++) many[q] = w9.add("Pl" + q);
        boolean allOk = true; for (int q = 1; q < 6; q++) { allOk &= l9.invite(id(many[0]), id(many[q]), "Pl0") == McmmoLink.Mirror.OK && l9.accept(id(many[q]), id(many[0]), "Pl0") == McmmoLink.Mirror.OK; }
        check("mcMMO unlimited: link never limits (our cap of 5 is ExcelTogether's job)", allOk && pm().parties.get(0).getMembers().size() == 6);
        // locked party, non-leader inviter
        World w10 = fresh(); ReflectMcmmoLink l10 = w10.link(); Player y1 = w10.add("Yan"), y2 = w10.add("Zoe"), y3 = w10.add("Abe");
        pm().createParty(w10.mm(y1), "Yans", null); l10.invite(id(y1), id(y2), "Yan"); l10.accept(id(y2), id(y1), "Yan");
        w10.mm(y1).getParty().setLocked(true);
        check("locked party: non-leader inviter gets LOCKED", l10.invite(id(y2), id(y3), "Zoe") == McmmoLink.Mirror.LOCKED && !w10.mm(y3).hasPartyInvite());
        check("locked party: leader can still invite", l10.invite(id(y1), id(y3), "Yan") == McmmoLink.Mirror.OK);
        // party system off, profile not loaded, unknown player
        World w11 = fresh(); ReflectMcmmoLink l11 = w11.link(); Player z1 = w11.add("Zed"), z2 = w11.add("Yui");
        mcMMO.p.partyConfig.enabled = false;
        check("mcMMO party system off: ABSENT, nothing changed", l11.invite(id(z1), id(z2), "Zed") == McmmoLink.Mirror.ABSENT && pm().parties.isEmpty() && l11.accept(id(z2), id(z1), "Zed") == McmmoLink.Mirror.ABSENT);
        mcMMO.p.partyConfig.enabled = true;
        UserManager.players.remove(id(z2));
        check("profile not loaded: NOT_READY, no party made", l11.invite(id(z1), id(z2), "Zed") == McmmoLink.Mirror.NOT_READY && pm().parties.isEmpty());
        check("offline player: NOT_READY", l11.invite(id(z1), UUID.randomUUID(), "Zed") == McmmoLink.Mirror.NOT_READY);
        // names
        World w12 = fresh(); ReflectMcmmoLink l12 = w12.link(); Player n1 = w12.add("Mike"), n2 = w12.add("Ash"), n3 = w12.add("Lou"), n4 = w12.add("Max");
        pm().createParty(w12.mm(n3), "Mike", null);
        l12.invite(id(n1), id(n2), "Mike");
        check("name taken: next free name", w12.mm(n1).getParty().getName().equals("Mike2"));
        l12.invite(id(n4), id(n2), ".Max party!");
        check("dot and symbols stripped from the party name", w12.mm(n4).getParty().getName().equals("Maxparty"));
        World w13 = fresh(); ReflectMcmmoLink l13 = w13.link(); Player r1 = w13.add("R1"), r2 = w13.add("R2");
        l13.invite(id(r1), id(r2), "...");
        check("empty name falls back to Family", w13.mm(r1).getParty().getName().equals("Family"));
        // mcMMO party disbanded between invite and accept: recreated
        World w14 = fresh(); ReflectMcmmoLink l14 = w14.link(); Player t1 = w14.add("Tia"), t2 = w14.add("Uri");
        l14.invite(id(t1), id(t2), "Tia"); pm().removeFromParty(w14.mm(t1)); 
        check("mcMMO party gone before accept: recreated and joined", l14.accept(id(t2), id(t1), "Tia") == McmmoLink.Mirror.OK && w14.mm(t2).getParty() != null && w14.mm(t2).getParty() == w14.mm(t1).getParty());
        // leave mirrors
        World w15 = fresh(); ReflectMcmmoLink l15 = w15.link(); Player u1 = w15.add("Una"), u2 = w15.add("Val"), u3 = w15.add("Wen");
        l15.invite(id(u1), id(u2), "Una"); l15.accept(id(u2), id(u1), "Una"); l15.invite(id(u1), id(u3), "Una"); l15.accept(id(u3), id(u1), "Una");
        l15.leave(id(u2));
        check("leave mirrors: member removed from mcMMO party", w15.mm(u2).getParty() == null && w15.mm(u1).getParty().getMembers().size() == 2);
        w15.on.remove(id(u3)); l15.leave(id(u3));
        check("leave mirrors for an offline player (kicked after grace)", !w15.mm(u1).getParty().getMembers().containsKey(id(u3)));
        l15.leave(id(u1));
        check("leader leave removes the party when empty", pm().parties.isEmpty());
        // someone who joined mcMMO on their own (not via us) is never removed by our leave
        World w16 = fresh(); ReflectMcmmoLink l16 = w16.link(); Player a1 = w16.add("Ada"), a2 = w16.add("Bev");
        pm().createParty(w16.mm(a1), "Adas", null); l16.leave(id(a2)); l16.leave(id(a1));
        check("leave ignores players we never linked", pm().parties.size() == 1 && w16.mm(a1).getParty() != null);
        // moved to a different mcMMO party after we linked them: leave must not touch the new one
        World w17 = fresh(); ReflectMcmmoLink l17 = w17.link(); Player c1 = w17.add("Cal"), c2 = w17.add("Dot");
        l17.invite(id(c1), id(c2), "Cal"); l17.accept(id(c2), id(c1), "Cal");
        pm().removeFromParty(w17.mm(c2)); pm().createParty(w17.mm(c2), "Dots", null);
        l17.leave(id(c2));
        check("leave does not remove them from a different mcMMO party they joined later", w17.mm(c2).getParty() != null && w17.mm(c2).getParty().getName().equals("Dots"));
        // read-only party views used by the hand-join sync
        World w20 = fresh(); ReflectMcmmoLink l20 = w20.link(); Player ra1 = w20.add("Rae"), ra2 = w20.add("Sid"), ra3 = w20.add("Tae");
        check("partyOf: no party reads as NONE", l20.partyOf(id(ra1)) == McmmoLink.McParty.NONE);
        l20.invite(id(ra1), id(ra2), "Rae"); l20.accept(id(ra2), id(ra1), "Rae");
        McmmoLink.McParty v = l20.partyOf(id(ra2));
        check("partyOf: name, leader and both members", v.real() && v.name.equals("Rae") && id(ra1).equals(v.leader) && v.members.size() == 2 && v.members.containsKey(id(ra2)));
        w20.on.remove(id(ra2));
        check("partyOf works for an offline player", l20.partyOf(id(ra2)).real() && l20.partyOf(id(ra2)).members.size() == 2);
        check("partyByName finds it, unknown name is NONE", l20.partyByName("Rae").real() && l20.partyByName("Nope") == McmmoLink.McParty.NONE);
        UserManager.players.remove(id(ra3));
        check("profile not loaded reads as UNKNOWN, never NONE", l20.partyOf(id(ra3)) == McmmoLink.McParty.UNKNOWN);
        l20.adopted(id(ra3), "Rae");
                // mcMMO throws
        World w18 = fresh(); ReflectMcmmoLink l18 = w18.link(); Player e1 = w18.add("Eli"), e2 = w18.add("Flo");
        mcMMO.p = null;
        check("mcMMO internals failing give ERROR, not an exception", l18.invite(id(e1), id(e2), "Eli") == McmmoLink.Mirror.ERROR);
        l18.cancel(id(e2), id(e1)); l18.leave(id(e1)); l18.cleanup(x -> false);
        check("other calls survive too", true);
    }

    static void absentTests() throws Exception {
        List<String> miss = ReflectMcmmoLink.probe(ClassLoader.getPlatformClassLoader());
        check("mcMMO absent: probe lists missing classes", !miss.isEmpty() && miss.get(0).startsWith("class "));
        ReflectMcmmoLink l = new ReflectMcmmoLink(ClassLoader.getPlatformClassLoader(), x -> null, x -> null, LOG);
        check("mcMMO absent: link unavailable and every call is a quiet no-op", !l.available() && l.invite(UUID.randomUUID(), UUID.randomUUID(), "x") == McmmoLink.Mirror.ABSENT
                && l.accept(UUID.randomUUID(), UUID.randomUUID(), "x") == McmmoLink.Mirror.ABSENT);
        l.cancel(UUID.randomUUID(), UUID.randomUUID()); l.leave(UUID.randomUUID()); l.cleanup(x -> false);
        check("mcMMO absent: NONE link", !McmmoLink.NONE.available() && McmmoLink.NONE.invite(null, null, null) == McmmoLink.Mirror.ABSENT);
        // real mcMMO jar signature check (names only; never run)
        String jar = System.getProperty("mcmmojar");
        if (jar != null && new java.io.File(jar).exists()) {
            ClassLoader app = McmmoLinkTest.class.getClassLoader();
            ClassLoader hide = new ClassLoader(app) {
                @Override protected Class<?> loadClass(String n, boolean r) throws ClassNotFoundException {
                    if (n.startsWith("com.gmail.nossr50.") ) throw new ClassNotFoundException(n);
                    return super.loadClass(n, r);
                }
            };
            ClassLoader real = new URLClassLoader(new URL[]{new java.io.File(jar).toURI().toURL()}, hide);
            List<String> m2 = ReflectMcmmoLink.probe(real);
            check("REAL mcMMO 2.3.003 jar: every method the link uses exists " + m2, m2.isEmpty());
        } else check("real mcMMO jar present for signature check (UNCONFIRMED if missing)", false);
    }

    public static void main(String[] a) throws Exception {
        wiringTests(); reflectTests(); absentTests();
        System.out.println("McmmoLinkTest: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
