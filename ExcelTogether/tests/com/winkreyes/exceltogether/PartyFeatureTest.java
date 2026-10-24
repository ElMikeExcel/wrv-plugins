package com.winkreyes.exceltogether;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** WRV-063 tests: party rules, name matching, menus and popups, config. Exit code 1 on any failure. */
public class PartyFeatureTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    // ---------- fakes ----------
    static final class FP implements PlayerView {
        final UUID id; final String real, nick; final boolean bedrock;
        final List<String> said = new ArrayList<>(); final List<Panel> panels = new ArrayList<>(); final List<String> bars = new ArrayList<>();
        FP(String real, String nick, boolean bedrock) { this(UUID.randomUUID(), real, nick, bedrock); }
        FP(UUID id, String real, String nick, boolean bedrock) { this.id = id; this.real = real; this.nick = nick; this.bedrock = bedrock; }
        public UUID id() { return id; } public String realName() { return real; } public String displayName() { return nick; }
        public boolean bedrock() { return bedrock; }
        public void say(String l) { said.add(l); } public void say(Panel p) { panels.add(p); said.add(p.plain()); }
        public void actionBar(String t) { bars.add(t); }
        String all() { return String.join("\n", said); }
        boolean heard(String s) { return all().contains(s); }
        void clear() { said.clear(); panels.clear(); bars.clear(); }
    }
    static final class FEnv implements Env {
        final Map<UUID, FP> on = new LinkedHashMap<>(); long t = 1_000_000L; final Set<UUID> vanished = new HashSet<>();
        FP add(FP p) { on.put(p.id, p); return p; }
        public PlayerView online(UUID id) { return on.get(id); }
        public List<PlayerView> visibleOthers(PlayerView v) {
            List<PlayerView> o = new ArrayList<>();
            for (FP p : on.values()) if (!p.id.equals(v.id()) && !vanished.contains(p.id)) o.add(p);
            return o;
        }
        public long now() { return t; }
    }
    static final class FForms implements Forms {
        boolean works = true; final List<String> titles = new ArrayList<>(); final List<List<String>> buttons = new ArrayList<>(); final List<String> contents = new ArrayList<>();
        IntConsumer lastSimple; Consumer<Boolean> lastModal; PlayerView lastTo;
        public boolean simple(PlayerView p, String t, String c, List<String> b, IntConsumer cb) {
            if (!works) return false; titles.add(t); contents.add(c); buttons.add(b); lastSimple = cb; lastTo = p; return true;
        }
        public boolean modal(PlayerView p, String t, String c, String y, String n, Consumer<Boolean> cb) {
            if (!works) return false; titles.add(t); contents.add(c); buttons.add(List.of(y, n)); lastModal = cb; lastTo = p; return true;
        }
        public boolean custom(PlayerView p, String t, List<Field> f, Consumer<List<Object>> cb) { return false; }
        String lastTitle() { return titles.isEmpty() ? "" : titles.get(titles.size() - 1); }
        List<String> lastButtons() { return buttons.get(buttons.size() - 1); }
    }
    static final class Rig {
        final FEnv env = new FEnv(); final FForms forms = new FForms(); final PartyManager mgr; final Controller ctl;
        Rig(int max) {
            mgr = new PartyManager(max, 60_000, 300_000, id -> env.on.containsKey(id));
            ctl = new Controller(mgr, env, forms, "Party bonus: +25% Jobs money and XP when a party member is within 50 blocks.");
        }
        FP java(String n) { return env.add(new FP(n, n, false)); }
        FP bedrock(String n) { return env.add(new FP(n, n, true)); }
    }

    static UUID u() { return UUID.randomUUID(); }

    // ---------- PartyManager ----------
    static void managerTests() {
        Set<UUID> on = new HashSet<>();
        UUID a = u(), b = u(), c = u(), d = u(), e = u(), f = u(); on.addAll(List.of(a, b, c, d, e, f));
        PartyManager m = new PartyManager(5, 60_000, 300_000, on::contains);
        long t = 1000;
        check("invite self", m.invite(a, a, t) == PartyManager.InviteResult.SELF);
        check("invite ok", m.invite(a, b, t) == PartyManager.InviteResult.OK);
        check("invite duplicate", m.invite(a, b, t + 5) == PartyManager.InviteResult.DUPLICATE);
        check("no party yet (invite alone does not make one)", !m.inParty(a) && !m.inParty(b));
        PartyManager.AcceptOutcome o = m.accept(b, null, t + 10);
        check("accept ok", o.result == PartyManager.AcceptResult.OK && o.created);
        check("inviter is leader", m.partyOf(a).leader.equals(a) && m.partyOf(b).size() == 2);
        check("same party", m.sameParty(a, b) && !m.sameParty(a, c));
        check("invite already member", m.invite(a, b, t + 20) == PartyManager.InviteResult.ALREADY_MEMBER);
        check("member can invite too", m.invite(b, c, t + 20) == PartyManager.InviteResult.OK);
        check("accept again by same member after joined -> already in party", m.accept(b, null, t + 30).result == PartyManager.AcceptResult.NO_INVITE);
        // other-party
        PartyManager m2 = new PartyManager(5, 60_000, 300_000, on::contains);
        m2.invite(a, b, t); m2.accept(b, null, t);
        m2.invite(c, d, t); m2.accept(d, null, t);
        check("target in other party", m2.invite(a, c, t) == PartyManager.InviteResult.TARGET_IN_OTHER_PARTY);
        // full party: cap 5
        PartyManager m3 = new PartyManager(5, 60_000, 300_000, on::contains);
        UUID[] ids = new UUID[8]; for (int i = 0; i < 8; i++) { ids[i] = u(); on.add(ids[i]); }
        for (int i = 1; i < 5; i++) { m3.invite(ids[0], ids[i], t); check("join " + i, m3.accept(ids[i], null, t).result == PartyManager.AcceptResult.OK); }
        check("party at cap", m3.partyOf(ids[0]).size() == 5);
        check("invite when full", m3.invite(ids[0], ids[5], t) == PartyManager.InviteResult.PARTY_FULL);
        check("member invite when full", m3.invite(ids[2], ids[5], t) == PartyManager.InviteResult.PARTY_FULL);
        // full at accept time (two invites out at size 4)
        PartyManager m4 = new PartyManager(3, 60_000, 300_000, on::contains);
        m4.invite(ids[0], ids[1], t); m4.accept(ids[1], null, t);
        check("size 2 invite 1", m4.invite(ids[0], ids[2], t) == PartyManager.InviteResult.OK);
        check("size 2 invite 2", m4.invite(ids[0], ids[3], t) == PartyManager.InviteResult.OK);
        check("first fills", m4.accept(ids[2], null, t).result == PartyManager.AcceptResult.OK);
        check("second finds it full", m4.accept(ids[3], null, t).result == PartyManager.AcceptResult.PARTY_FULL);
        check("failed joiner not in party", !m4.inParty(ids[3]));
        // expiry
        PartyManager m5 = new PartyManager(5, 60_000, 300_000, on::contains);
        m5.invite(a, b, 0);
        check("valid at exactly ttl", m5.hasInvite(a, b, 60_000));
        check("expired 1ms later", !m5.hasInvite(a, b, 60_001));
        check("accept expired", m5.accept(b, null, 60_001).result == PartyManager.AcceptResult.EXPIRED);
        check("expired accept makes no party", !m5.inParty(a));
        m5.invite(a, c, 0);
        PartyManager.SweepResult sw = m5.sweep(70_000);
        check("sweep reports expired", sw.expired.size() >= 1);
        check("after sweep re-invite allowed", m5.invite(a, c, 70_001) == PartyManager.InviteResult.OK);
        check("expired duplicate can be re-sent without sweep", new PartyManager(5, 1000, 1, on::contains) != null);
        PartyManager m6 = new PartyManager(5, 1000, 300_000, on::contains);
        m6.invite(a, b, 0);
        check("re-invite after expiry (no sweep run)", m6.invite(a, b, 5000) == PartyManager.InviteResult.OK);
        // decline
        PartyManager m7 = new PartyManager(5, 60_000, 300_000, on::contains);
        m7.invite(a, b, 0);
        check("decline returns invite", m7.decline(b, null, 1) != null);
        check("decline again nothing", m7.decline(b, null, 2) == null);
        check("accept after decline", m7.accept(b, null, 3).result == PartyManager.AcceptResult.NO_INVITE);
        // inviter offline
        PartyManager m8 = new PartyManager(5, 60_000, 300_000, on::contains);
        UUID gone = u();
        m8.invite(gone, b, 0);
        check("inviter offline at accept", m8.accept(b, null, 1).result == PartyManager.AcceptResult.INVITER_OFFLINE);
        // quit clears invites
        PartyManager m9 = new PartyManager(5, 60_000, 300_000, on::contains);
        m9.invite(a, b, 0); m9.onQuit(a, 1);
        check("inviter quit removes invite", m9.invitesFor(b, 2).isEmpty());
        // newest invite default, specific inviter
        PartyManager m10 = new PartyManager(5, 60_000, 300_000, on::contains);
        m10.invite(a, c, 0); m10.invite(b, c, 1);
        check("two invites listed", m10.invitesFor(c, 2).size() == 2);
        PartyManager.AcceptOutcome o10 = m10.accept(c, a, 3);
        check("accept specific inviter", o10.result == PartyManager.AcceptResult.OK && m10.partyOf(c).leader.equals(a));
        check("other invites to joiner cleared", m10.invitesFor(c, 4).isEmpty());
        // cross invites
        PartyManager m11 = new PartyManager(5, 60_000, 300_000, on::contains);
        m11.invite(a, b, 0); m11.invite(b, a, 0);
        m11.accept(b, a, 1);
        check("cross invite cleaned after join", m11.accept(a, b, 2).result == PartyManager.AcceptResult.NO_INVITE && m11.partyOf(a).size() == 2);
        // leader leaves
        PartyManager m12 = new PartyManager(5, 60_000, 300_000, on::contains);
        m12.invite(a, b, 0); m12.accept(b, null, 0); m12.invite(a, c, 0); m12.accept(c, null, 0);
        PartyManager.Change ch = new PartyManager.Change();
        check("leader leave ok", m12.leave(a, ch) == PartyManager.LeaveResult.LEFT);
        check("new leader is oldest remaining", m12.partyOf(b).leader.equals(b) && ch.newLeader.equals(b) && !ch.disbanded);
        check("leaver is free", !m12.inParty(a));
        PartyManager.Change ch2 = new PartyManager.Change();
        check("member leave to one disbands", m12.leave(c, ch2) == PartyManager.LeaveResult.DISBANDED_BY_LEAVE && ch2.disbanded);
        check("remaining member freed", !m12.inParty(b) && !m12.inParty(c));
        check("leave when none", m12.leave(a, null) == PartyManager.LeaveResult.NOT_IN_PARTY);
        // leader prefers online successor
        PartyManager m13 = new PartyManager(5, 60_000, 300_000, on::contains);
        m13.invite(a, b, 0); m13.accept(b, null, 0); m13.invite(a, c, 0); m13.accept(c, null, 0);
        m13.onQuit(b, 5);
        PartyManager.Change ch3 = new PartyManager.Change(); m13.leave(a, ch3);
        check("leader passes to online member, not offline one", ch3.newLeader.equals(c));
        // kick
        PartyManager m14 = new PartyManager(5, 60_000, 300_000, on::contains);
        m14.invite(a, b, 0); m14.accept(b, null, 0); m14.invite(a, c, 0); m14.accept(c, null, 0);
        check("non leader cannot kick", m14.kick(b, c, null) == PartyManager.KickResult.NOT_LEADER);
        check("cannot kick self", m14.kick(a, a, null) == PartyManager.KickResult.SELF);
        check("cannot kick stranger", m14.kick(a, d, null) == PartyManager.KickResult.TARGET_NOT_MEMBER);
        check("kick ok", m14.kick(a, c, null) == PartyManager.KickResult.OK && !m14.inParty(c) && m14.partyOf(a).size() == 2);
        check("kick not in party", m14.kick(e, a, null) == PartyManager.KickResult.NOT_IN_PARTY);
        // grace on disconnect
        PartyManager m15 = new PartyManager(5, 60_000, 300_000, on::contains);
        m15.invite(a, b, 0); m15.accept(b, null, 0); m15.invite(a, c, 0); m15.accept(c, null, 0);
        m15.onQuit(c, 1000);
        check("still member during grace", m15.inParty(c) && m15.isOfflineGrace(c));
        m15.sweep(1000 + 299_999);
        check("still member just before grace ends", m15.inParty(c));
        PartyManager.SweepResult sr = m15.sweep(1000 + 300_000);
        check("removed after grace", !m15.inParty(c) && sr.removed.size() == 1 && sr.removed.get(0).removed.equals(c));
        m15.onQuit(b, 400_000); m15.onJoin(b);
        m15.sweep(400_000 + 400_000);
        check("rejoin cancels removal", m15.inParty(b));
        // last two both gone
        PartyManager m16 = new PartyManager(5, 60_000, 1000, on::contains);
        m16.invite(a, b, 0); m16.accept(b, null, 0);
        m16.onQuit(a, 10); m16.onQuit(b, 10);
        m16.sweep(5000);
        check("everyone gone, no party left", !m16.inParty(a) && !m16.inParty(b));
        // config clamps
        check("bad max falls back to 5", new PartyManager(1, 0, -1, on::contains).maxSize() == 5 && new PartyManager(99, 0, -1, on::contains).maxSize() == 5);
        check("bad ttl falls back to 60s", new PartyManager(5, 0, -1, on::contains).ttlMs() == 60_000);
        check("max 2 and 20 allowed", new PartyManager(2, 1, 1, on::contains).maxSize() == 2 && new PartyManager(20, 1, 1, on::contains).maxSize() == 20);
        check("party snapshot is immutable", throwsOnModify(m.partyOf(a).members));
    }
    static boolean throwsOnModify(List<UUID> l) { try { l.add(UUID.randomUUID()); return false; } catch (UnsupportedOperationException e) { return true; } }

    // ---------- NameMatch ----------
    static void nameTests() {
        UUID k = u(), m = u(), j = u(), j2 = u();
        List<NameMatch.Cand> cs = List.of(new NameMatch.Cand(k, ".brightfern", "Fern"), new NameMatch.Cand(m, ".AlphaPlayer", "\u00a7aMike"),
                new NameMatch.Cand(j, "Quill_Turtle", "Quill_Turtle"));
        check("dot typed", NameMatch.find(".brightfern", cs).found.id.equals(k));
        check("dot omitted", NameMatch.find("brightfern", cs).found.id.equals(k));
        check("case", NameMatch.find("BRIGHTFERN", cs).found.id.equals(k));
        check("nickname", NameMatch.find("fern", cs).found.id.equals(k));
        check("nickname with color code on target", NameMatch.find("mike", cs).found.id.equals(m));
        check("typed color code ignored", NameMatch.find("&aMike", cs).found.id.equals(m));
        check("unique prefix", NameMatch.find("quil", cs).found.id.equals(j));
        check("no match", !NameMatch.find("nobody", cs).isFound() && !NameMatch.find("nobody", cs).isAmbiguous());
        check("empty query", !NameMatch.find("  ", cs).isFound());
        check("only a dot", !NameMatch.find(".", cs).isFound());
        List<NameMatch.Cand> twins = List.of(new NameMatch.Cand(j, "Tam", "Tam"), new NameMatch.Cand(j2, ".Tam", "Tam"));
        check("java name wins exact", NameMatch.find("Tam", twins).found.id.equals(j));
        check("dotted name picks bedrock twin", NameMatch.find(".Tam", twins).found.id.equals(j2));
        List<NameMatch.Cand> amb = List.of(new NameMatch.Cand(j, "Steve1", "Steve1"), new NameMatch.Cand(j2, "Steve2", "Steve2"));
        check("ambiguous prefix", NameMatch.find("steve", amb).isAmbiguous() && NameMatch.find("steve", amb).ambiguous.size() == 2);
        check("exact beats prefix", NameMatch.find("steve1", amb).found.id.equals(j));
        check("shown strips dot and color", Controller.shown("\u00a7aFern", ".brightfern").equals("Fern") && Controller.shown(".brightfern", ".brightfern").equals("brightfern"));
        check("shown falls back to real name", Controller.shown("", ".brightfern").equals("brightfern") && Controller.shown(null, null).equals("Someone"));
    }

    // ---------- Controller ----------
    static void controllerTests() {
        // Java invite -> clickable popup -> accept
        Rig r = new Rig(5);
        FP mike = r.bedrock(".AlphaPlayer"), tam = r.bedrock(".brightfern"), cass = r.java("BuilderPlayer");
        r.ctl.command(cass, new String[]{"invite", "brightfern"});
        check("inviter told", cass.heard("Party invite sent to brightfern"));
        check("bedrock invitee got a form", r.forms.lastTitle().equals("Party invite") && r.forms.lastButtons().equals(List.of("Accept", "Decline")));
        check("form says who and expiry", r.forms.contents.get(0).contains("BuilderPlayer") && r.forms.contents.get(0).contains("60 seconds"));
        check("actionbar ping", tam.bars.size() == 1);
        r.forms.lastSimple.accept(0);
        check("accept joined", r.mgr.sameParty(cass.id, tam.id));
        check("joiner told", tam.heard("You joined BuilderPlayer's party (2/5)"));
        check("leader told", cass.heard("brightfern joined the party (2/5)"));

        // Java invitee gets Accept and Decline buttons running real-name commands (dot included)
        Rig r2 = new Rig(5);
        FP a = r2.java("Alex"), bk = r2.bedrock(".brightfern"); r2.forms.works = false;   // forms broken -> falls back to chat
        r2.ctl.command(bk, new String[]{"invite", "alex"});
        check("form failure falls back to chat panel", !a.panels.isEmpty());
        Panel pn = a.panels.get(0);
        boolean hasAcc = false, hasDec = false;
        for (Panel.Line l : pn.lines) for (Panel.Seg s : l.segs) { if ("/excelparty accept .brightfern".equals(s.run)) hasAcc = true; if ("/excelparty decline .brightfern".equals(s.run)) hasDec = true; }
        check("chat popup has Accept and Decline with dotted name", hasAcc && hasDec);
        r2.ctl.command(a, new String[]{"accept", ".brightfern"});
        check("dotted accept works", r2.mgr.sameParty(a.id, bk.id));

        // accept without a name, decline, nothing pending
        Rig r3 = new Rig(5);
        FP x = r3.java("X"), y = r3.java("Y");
        r3.ctl.command(x, new String[]{"accept"});
        check("accept with nothing", x.heard("no party invite"));
        r3.ctl.command(y, new String[]{"invite", "x"});
        r3.ctl.command(x, new String[]{"decline"});
        check("decline tells both", x.heard("Invite declined") && y.heard("X declined your party invite"));
        r3.ctl.command(x, new String[]{"accept"});
        check("accept after decline", x.heard("no party invite") && !r3.mgr.inParty(x.id));

        // self, unknown, offline, ambiguous, already member, already in other party, duplicate, full
        Rig r4 = new Rig(3);
        FP p1 = r4.java("Steve1"), p2 = r4.java("Steve2"), p3 = r4.java("Zed"), p4 = r4.java("Quin");
        r4.ctl.command(p1, new String[]{"invite", "Steve1"});
        check("invite self by name", p1.heard("can't invite yourself"));
        r4.ctl.command(p1, new String[]{"invite", "nobody"});
        check("unknown/offline target", p1.heard("can't find a player named nobody"));
        r4.ctl.command(p3, new String[]{"invite", "steve"});
        check("ambiguous names listed", p3.heard("More than one player") && p3.heard("Steve1") && p3.heard("Steve2"));
        p1.clear(); r4.ctl.command(p1, new String[]{"invite", "Steve2"});
        r4.ctl.command(p1, new String[]{"invite", "Steve2"});
        check("duplicate invite", p1.heard("already invited Steve2"));
        r4.ctl.command(p2, new String[]{"accept"});
        p1.clear(); r4.ctl.command(p1, new String[]{"invite", "Steve2"});
        check("already in your party", p1.heard("Steve2 is already in your party"));
        r4.ctl.command(p3, new String[]{"invite", "Quin"}); r4.ctl.command(p4, new String[]{"accept"});
        p1.clear(); r4.ctl.command(p1, new String[]{"invite", "Zed"});
        check("target in another party", p1.heard("Zed is already in a party"));
        // fill to cap 3
        FP p5 = r4.java("Rae");
        r4.ctl.command(p1, new String[]{"invite", "Rae"}); r4.ctl.command(p5, new String[]{"accept"});
        FP p6 = r4.java("Sol");
        p1.clear(); r4.ctl.command(p1, new String[]{"invite", "Sol"});
        check("full party message", p1.heard("Your party is full (3/3)"));
        r4.ctl.invitePicker(p1);
        check("picker also says full", p1.heard("Your party is full"));

        // expiry through tick + stale popup click
        Rig r5 = new Rig(5);
        FP e1 = r5.bedrock(".brightfern"), e2 = r5.java("Cass");
        r5.ctl.command(e2, new String[]{"invite", "brightfern"});
        IntConsumer stale = r5.forms.lastSimple;
        r5.env.t += 59_000; r5.ctl.tick();
        check("not expired at 59s", e2.heard("Party invite sent") && !e2.heard("ran out of time"));
        r5.env.t += 2_000; r5.ctl.tick();
        check("inviter told expiry", e2.heard("Your party invite to brightfern ran out of time"));
        check("invitee told expiry", e1.heard("party invite from Cass ran out of time"));
        stale.accept(0);
        check("stale Accept tap is refused", e1.heard("no party invite") && !r5.mgr.inParty(e1.id));
        r5.ctl.command(e2, new String[]{"invite", "brightfern"});
        check("re-invite after expiry works", r5.mgr.hasInvite(e2.id, e1.id, r5.env.now()));

        // closing the popup leaves the invite alive
        Rig r6 = new Rig(5);
        FP c1 = r6.bedrock(".brightfern"), c2 = r6.java("Cass");
        r6.ctl.command(c2, new String[]{"invite", "brightfern"});
        r6.forms.lastSimple.accept(-1);
        check("closing popup keeps invite", r6.mgr.hasInvite(c2.id, c1.id, r6.env.now()));
        r6.ctl.command(c1, new String[]{"invites"});
        check("invites list form", r6.forms.lastTitle().equals("Party invites") && r6.forms.lastButtons().get(0).equals("From Cass"));
        r6.forms.lastSimple.accept(0);
        r6.forms.lastModal.accept(true);
        check("accept from invites list", r6.mgr.sameParty(c1.id, c2.id));

        // inviter leaves before accept
        Rig r7 = new Rig(5);
        FP g1 = r7.java("Gus"), g2 = r7.java("Hal");
        r7.ctl.command(g1, new String[]{"invite", "Hal"});
        r7.env.on.remove(g1.id); r7.ctl.playerQuit(g1.id);
        r7.ctl.command(g2, new String[]{"accept"});
        check("inviter gone", g2.heard("no party invite") || g2.heard("not online"));
        check("no ghost party", !r7.mgr.inParty(g2.id));

        // target offline mid-invite (left between picker and tap)
        Rig r8 = new Rig(5);
        FP h1 = r8.bedrock(".AlphaPlayer"), h2 = r8.java("Ivy");
        r8.ctl.invitePicker(h1);
        IntConsumer pick = r8.forms.lastSimple;
        r8.env.on.remove(h2.id);
        pick.accept(0);
        check("target left after picking", h1.heard("just left the game"));

        // picker lists only invitable players, hides vanished and busy
        Rig r9 = new Rig(5);
        FP m1 = r9.bedrock(".AlphaPlayer"), m2 = r9.java("Alice"), m3 = r9.java("Bob"), m4 = r9.java("Cat"), m5 = r9.java("Dan");
        r9.mgr.invite(m3.id, m4.id, r9.env.now()); r9.mgr.accept(m4.id, null, r9.env.now());   // Bob+Cat are a party
        r9.env.vanished.add(m5.id);
        r9.ctl.invitePicker(m1);
        check("picker only Alice", r9.forms.lastTitle().equals("Invite to party") && r9.forms.lastButtons().equals(List.of("Alice")));
        check("picker notes busy players", r9.forms.contents.get(r9.forms.contents.size() - 1).contains("2 player(s) already in a party"));
        FP j1 = r9.java("Jay");
        r9.ctl.invitePicker(j1);
        boolean listsAlice = false, listsBob = false, listsDan = false;
        for (Panel.Line l : j1.panels.get(0).lines) for (Panel.Seg s : l.segs) { if ("/excelparty invite Alice".equals(s.run)) listsAlice = true; if ("/excelparty invite Bob".equals(s.run)) listsBob = true; if ("/excelparty invite Dan".equals(s.run)) listsDan = true; }
        check("java picker lists Alice, not busy Bob, not vanished Dan", listsAlice && !listsBob && !listsDan);
        // nobody else
        Rig ra = new Rig(5); FP lone = ra.java("Lone");
        ra.ctl.invitePicker(lone);
        check("nobody online message", lone.heard("Nobody else is online"));
        Rig rb = new Rig(5); FP l1 = rb.java("L1"), l2 = rb.java("L2"), l3 = rb.java("L3");
        rb.mgr.invite(l2.id, l3.id, rb.env.now()); rb.mgr.accept(l3.id, null, rb.env.now());
        rb.ctl.invitePicker(l1);
        check("everyone busy message", l1.heard("already in a party"));

        // nickname display + dot handling in picker and party list
        Rig rc = new Rig(5);
        FP n1 = rc.java("Cass"); FP n2 = rc.env.add(new FP(".brightfern", "\u00a7dFern", true));
        rc.ctl.invitePicker(n1);
        boolean nick = false; for (Panel.Line l : n1.panels.get(0).lines) for (Panel.Seg s : l.segs) if (s.text.equals("[Fern]") && ".brightfern".equals(s.run == null ? null : s.run.substring("/excelparty invite ".length()))) nick = true;
        check("picker shows nickname, runs real dotted name", nick);
        rc.ctl.command(n1, new String[]{"invite", "Fern"});
        check("invite by nickname", rc.mgr.hasInvite(n1.id, n2.id, rc.env.now()));

        // my party view, offline marker, leave confirm
        Rig rd = new Rig(5);
        FP o1 = rd.java("Owen"), o2 = rd.java("Pia"), o3 = rd.bedrock(".Quill");
        rd.ctl.command(o1, new String[]{"invite", "Pia"}); rd.ctl.command(o2, new String[]{"accept"});
        rd.ctl.command(o1, new String[]{"invite", "Quill"}); rd.forms.lastSimple.accept(0);
        o1.clear(); rd.ctl.command(o1, new String[]{"list"});
        String view = o1.all();
        check("party view lists all three", view.contains("(3/5)") && view.contains("Owen (leader)") && view.contains("Pia") && view.contains("Quill"));
        rd.env.on.remove(o2.id); rd.ctl.playerQuit(o2.id);
        o1.clear(); rd.ctl.command(o1, new String[]{"list"});
        check("offline member marked", o1.heard("Pia (offline)"));
        rd.ctl.command(o3, new String[]{"list"});
        check("bedrock my party form has Leave", rd.forms.lastTitle().equals("My party") && rd.forms.lastButtons().contains("Leave party"));
        o1.clear(); rd.ctl.command(o1, new String[]{"leave"});
        check("leave asks first (still in party)", rd.mgr.inParty(o1.id) && o1.heard("Leave your party of 3/5?"));
        boolean yes = false; for (Panel.Line l : o1.panels.get(0).lines) for (Panel.Seg s : l.segs) if ("/excelparty leave yes".equals(s.run)) yes = true;
        check("leave confirm button present", yes);
        o3.clear(); rd.ctl.command(o1, new String[]{"leave", "yes"});
        check("leader left", !rd.mgr.inParty(o1.id) && o1.heard("You left the party"));
        check("remaining told new leader", o3.heard("Owen left the party") && o3.heard("You are now the party leader"));
        check("party continues with 2", rd.mgr.partyOf(o3.id).size() == 2);
        rd.ctl.command(o1, new String[]{"leave", "yes"});
        check("leave when not in party", o1.heard("not in a party"));
        // bedrock leave uses a confirm modal
        rd.ctl.command(o3, new String[]{"leave"});
        check("bedrock leave confirm modal", rd.forms.lastTitle().equals("Leave party?") && rd.forms.lastButtons().equals(List.of("Yes, leave", "Stay")));
        rd.forms.lastModal.accept(false);
        check("Stay keeps party", rd.mgr.inParty(o3.id));
        rd.forms.lastModal.accept(true);
        check("Yes leaves; party of 2 disbands", !rd.mgr.inParty(o3.id));

        // kick
        Rig re = new Rig(5);
        FP k1 = re.java("Lead"), k2 = re.java("Mem"), k3 = re.java("Other");
        re.ctl.command(k1, new String[]{"invite", "Mem"}); re.ctl.command(k2, new String[]{"accept"});
        re.ctl.command(k1, new String[]{"invite", "Other"}); re.ctl.command(k3, new String[]{"accept"});
        k2.clear(); re.ctl.command(k2, new String[]{"kick", "Other"});
        check("member cannot kick", k2.heard("Only the party leader"));
        re.ctl.command(k1, new String[]{"kick", "Other"});
        check("leader kicks", !re.mgr.inParty(k3.id) && k3.heard("removed from the party"));

        // hub forms
        Rig rf = new Rig(5);
        FP q1 = rf.bedrock(".AlphaPlayer"), q2 = rf.java("Zoe");
        rf.ctl.command(q1, new String[]{});
        check("bedrock hub buttons", rf.forms.lastTitle().equals("Party") && rf.forms.lastButtons().equals(List.of("Invite to party", "My party", "Close")));
        check("hub mentions bonus", rf.forms.contents.get(0).contains("+25%"));
        rf.ctl.command(q2, new String[]{"invite", ".AlphaPlayer"});     // popup to Mike (bedrock)
        rf.ctl.command(q1, new String[]{});
        check("hub shows invite count", rf.forms.lastButtons().contains("Invites (1)"));
        rf.forms.lastSimple.accept(0);   // Invite to party -> picker
        check("hub tap opens picker", rf.forms.lastTitle().equals("Invite to party"));
        rf.ctl.command(q2, new String[]{});
        check("java hub panel has buttons", q2.panels.get(q2.panels.size() - 1).longestButton() > 0);
        rf.ctl.command(q2, new String[]{"help"});
        check("help lists commands", q2.heard("/excelparty invite <name>"));
        rf.ctl.command(q2, new String[]{"bogus"});
        check("unknown subcommand shows help", q2.panels.size() >= 3);

        // compass click: confirm first, cooldown, then the normal invite path
        Rig rg = new Rig(5);
        FP s1 = rg.java("Sam"), s2 = rg.java("Tia");
        rg.ctl.compassClick(s1, s2);
        check("java: compass click asks first, sends nothing", !rg.mgr.hasInvite(s1.id, s2.id, rg.env.now()) && s2.said.isEmpty() && s2.bars.isEmpty());
        check("java: question text", s1.heard("Invite Tia to your party?"));
        check("java: Yes and Cancel are clickable", s1.panels.size() == 1 && s1.panels.get(0).plain().contains("[Yes]") && s1.panels.get(0).plain().contains("[Cancel]"));
        rg.ctl.compassClick(s1, s2);
        check("double tap asks once", s1.said.size() == 1);
        rg.ctl.command(s1, new String[]{"confirm"});
        check("java: Yes sends the invite", rg.mgr.hasInvite(s1.id, s2.id, rg.env.now()) && s1.heard("Party invite sent to Tia") && s2.heard("Accept"));
        check("invitee still not in party until accept", !rg.mgr.inParty(s2.id));
        rg.ctl.command(s1, new String[]{"confirm"});
        check("second Yes tap does nothing more", s1.heard("Nothing to confirm"));
        rg.env.t += 2500;
        rg.ctl.compassClick(s1, s2); rg.ctl.command(s1, new String[]{"confirm"});
        check("after cooldown the duplicate rule answers", s1.heard("already invited Tia"));
        rg.ctl.compassClick(s1, s1);
        rg.env.t += 2500;
        rg.ctl.compassClick(s1, s1);
        check("compass click on self refused", s1.heard("can't invite yourself"));

        // long grace removal announces
        Rig rh = new Rig(5);
        FP w1 = rh.java("Wes"), w2 = rh.java("Xan"), w3 = rh.java("Yui");
        rh.ctl.command(w1, new String[]{"invite", "Xan"}); rh.ctl.command(w2, new String[]{"accept"});
        rh.ctl.command(w1, new String[]{"invite", "Yui"}); rh.ctl.command(w3, new String[]{"accept"});
        rh.env.on.remove(w3.id); rh.ctl.playerQuit(w3.id);
        rh.env.t += 301_000; rh.ctl.tick();
        check("away too long member removed and announced", !rh.mgr.inParty(w3.id) && w1.heard("Yui was away too long") && rh.mgr.partyOf(w1.id).size() == 2);
        FP w3b = rh.env.add(new FP(w3.id, "Yui", "Yui", false));
        rh.ctl.playerJoin(w3b);
        check("returning player not told they are still in party", !w3b.heard("still in your party"));
        rh.env.on.remove(w2.id); rh.ctl.playerQuit(w2.id);
        FP w2b = rh.env.add(new FP(w2.id, "Xan", "Xan", false)); rh.ctl.playerJoin(w2b);
        check("quick reconnect keeps seat and says so", rh.mgr.inParty(w2.id) && w2b.heard("still in your party"));
    }

    // ---------- bonus mode ----------
    static void bonusTests() {
        check("parse party", BonusMode.parse("party") == BonusMode.PARTY && BonusMode.parse(" PARTY ") == BonusMode.PARTY);
        check("parse nearby/either", BonusMode.parse("nearby") == BonusMode.NEARBY && BonusMode.parse("Either") == BonusMode.EITHER);
        check("junk/null falls back to party", BonusMode.parse("zzz") == BonusMode.PARTY && BonusMode.parse(null) == BonusMode.PARTY && BonusMode.parse("") == BonusMode.PARTY);
        check("party mode: stranger nearby does not count", !BonusMode.PARTY.counts(false, true));
        check("party mode: party member near counts", BonusMode.PARTY.counts(true, true));
        check("party mode: nobody", !BonusMode.PARTY.counts(false, false));
        check("nearby mode: stranger counts", BonusMode.NEARBY.counts(false, true) && !BonusMode.NEARBY.counts(false, false));
        check("either mode", BonusMode.EITHER.counts(true, true) && BonusMode.EITHER.counts(false, true) && !BonusMode.EITHER.counts(false, false));
    }

    // ---------- compass confirm ----------
    static void confirmTests() {
        // confirm: cancel sends nothing
        Rig rc = new Rig(5);
        FP c1 = rc.java("Cal"), c2 = rc.java("Dee");
        rc.ctl.compassClick(c1, c2);
        rc.ctl.command(c1, new String[]{"cancel"});
        check("java cancel: no invite, invitee told nothing", !rc.mgr.hasInvite(c1.id, c2.id, rc.env.now()) && c2.said.isEmpty() && c2.bars.isEmpty() && c2.panels.isEmpty());
        check("java cancel: inviter told", c1.heard("no invite sent"));
        rc.ctl.command(c1, new String[]{"confirm"});
        check("Yes after Cancel sends nothing", !rc.mgr.hasInvite(c1.id, c2.id, rc.env.now()) && c2.said.isEmpty());
        rc.ctl.command(c1, new String[]{"cancel"});
        check("cancel with nothing pending is harmless", c1.heard("Nothing to cancel"));
        // confirm with nothing pending
        rc.ctl.command(c2, new String[]{"confirm"});
        check("Yes with nothing pending sends nothing", c2.heard("Nothing to confirm") && !rc.mgr.hasInvite(c2.id, c1.id, rc.env.now()));

        // confirm expiry
        Rig re = new Rig(5);
        FP e1 = re.java("Eli"), e2 = re.java("Fay");
        re.ctl.compassClick(e1, e2);
        re.env.t += Controller.CONFIRM_TTL_MS;
        re.ctl.command(e1, new String[]{"confirm"});
        check("Yes exactly at 30s still works", re.mgr.hasInvite(e1.id, e2.id, re.env.now()));
        Rig rx = new Rig(5);
        FP x1 = rx.java("Gus"), x2 = rx.java("Hal");
        rx.ctl.compassClick(x1, x2);
        rx.env.t += Controller.CONFIRM_TTL_MS + 1;
        rx.ctl.command(x1, new String[]{"confirm"});
        check("Yes after 30s: timed out, nothing sent", x1.heard("timed out") && !rx.mgr.hasInvite(x1.id, x2.id, rx.env.now()) && x2.said.isEmpty());
        rx.ctl.command(x1, new String[]{"confirm"});
        check("expired confirm is cleared", x1.heard("Nothing to confirm"));

        // target logged off between click and Yes
        Rig rl = new Rig(5);
        FP l1 = rl.java("Ivy"), l2 = rl.java("Jon");
        rl.ctl.compassClick(l1, l2);
        rl.env.on.remove(l2.id); rl.ctl.playerQuit(l2.id);
        rl.ctl.command(l1, new String[]{"confirm"});
        check("target logged off: told, nothing sent", l1.heard("just left the game") && !rl.mgr.hasInvite(l1.id, l2.id, rl.env.now()));
        // inviter logs off, state cleared
        Rig rq = new Rig(5);
        FP q1b = rq.java("Kim"), q2b = rq.java("Lou");
        rq.ctl.compassClick(q1b, q2b); rq.ctl.playerQuit(q1b.id);
        rq.ctl.command(q1b, new String[]{"confirm"});
        check("inviter relog: old question gone", q1b.heard("Nothing to confirm") && !rq.mgr.hasInvite(q1b.id, q2b.id, rq.env.now()));
        // second click on another player replaces the first question
        Rig rr = new Rig(5);
        FP r1 = rr.java("Max"), r2 = rr.java("Ned"), r3 = rr.java("Oli");
        rr.ctl.compassClick(r1, r2); rr.env.t += 2500; rr.ctl.compassClick(r1, r3);
        rr.ctl.command(r1, new String[]{"confirm"});
        check("Yes applies to the latest player only", rr.mgr.hasInvite(r1.id, r3.id, rr.env.now()) && !rr.mgr.hasInvite(r1.id, r2.id, rr.env.now()));
        // full party: asked, then refused at Yes (rules unchanged)
        Rig rf2 = new Rig(2);
        FP f1 = rf2.java("Pam"), f2 = rf2.java("Quin"), f3 = rf2.java("Rex");
        rf2.ctl.command(f1, new String[]{"invite", "Quin"}); rf2.ctl.command(f2, new String[]{"accept"});
        rf2.ctl.compassClick(f1, f3); rf2.ctl.command(f1, new String[]{"confirm"});
        check("full party refused after Yes", f1.heard("party is full") && f3.said.isEmpty());
        // any member (not leader) can invite via compass
        rf2.ctl.compassClick(f2, f3); rf2.ctl.command(f2, new String[]{"confirm"});
        check("non-leader member compass invite reaches the same rules", f2.heard("party is full"));
        Rig rm = new Rig(5);
        FP m1 = rm.java("Sol"), m2 = rm.java("Tom"), m3 = rm.java("Uma");
        rm.ctl.command(m1, new String[]{"invite", "Tom"}); rm.ctl.command(m2, new String[]{"accept"});
        rm.ctl.compassClick(m2, m3); rm.ctl.command(m2, new String[]{"confirm"});
        check("non-leader member invite works, invitee must still accept", rm.mgr.hasInvite(m2.id, m3.id, rm.env.now()) && !rm.mgr.inParty(m3.id));
        rm.ctl.command(m3, new String[]{"accept"});
        check("invitee accepts into the party", rm.mgr.sameParty(m1.id, m3.id));

        // Bedrock: modal Yes / Cancel
        Rig rb = new Rig(5);
        FP b1 = rb.bedrock(".Ann"), b2 = rb.java("Bo");
        rb.ctl.compassClick(b1, b2);
        check("bedrock: confirm form shown with Yes/Cancel", rb.forms.lastTitle().equals("Party invite") && rb.forms.lastButtons().equals(List.of("Yes", "Cancel")) && rb.forms.contents.get(rb.forms.contents.size() - 1).contains("Invite Bo to your party?"));
        check("bedrock: nothing sent before the tap", !rb.mgr.hasInvite(b1.id, b2.id, rb.env.now()) && b2.said.isEmpty());
        rb.forms.lastModal.accept(false);
        check("bedrock Cancel sends nothing", !rb.mgr.hasInvite(b1.id, b2.id, rb.env.now()) && b2.said.isEmpty() && b2.bars.isEmpty());
        rb.forms.lastModal.accept(true);
        check("bedrock Yes after Cancel sends nothing", !rb.mgr.hasInvite(b1.id, b2.id, rb.env.now()));
        rb.env.t += 2500;
        rb.ctl.compassClick(b1, b2);
        rb.forms.lastModal.accept(true);
        check("bedrock Yes sends the invite", rb.mgr.hasInvite(b1.id, b2.id, rb.env.now()) && b2.panels.size() == 1);
        // bedrock stale form: asked about Bo, then clicked another player; old form must not invite Bo
        Rig rs = new Rig(5);
        FP t1 = rs.bedrock(".Zed"), t2 = rs.java("Amy"), t3 = rs.java("Ben");
        rs.ctl.compassClick(t1, t2);
        Consumer<Boolean> oldForm = rs.forms.lastModal;
        rs.env.t += 2500; rs.ctl.compassClick(t1, t3);
        oldForm.accept(true);
        check("bedrock stale form for another player is refused", !rs.mgr.hasInvite(t1.id, t2.id, rs.env.now()) && !rs.mgr.hasInvite(t1.id, t3.id, rs.env.now()));
        // bedrock expiry and logoff
        Rig rt = new Rig(5);
        FP u1 = rt.bedrock(".Cy"), u2 = rt.java("Di");
        rt.ctl.compassClick(u1, u2);
        rt.env.t += Controller.CONFIRM_TTL_MS + 1000;
        rt.forms.lastModal.accept(true);
        check("bedrock Yes after expiry sends nothing", u1.heard("timed out") && !rt.mgr.hasInvite(u1.id, u2.id, rt.env.now()));
        rt.env.t += 2500;
        rt.ctl.compassClick(u1, u2);
        rt.env.on.remove(u2.id); rt.ctl.playerQuit(u2.id);
        rt.forms.lastModal.accept(true);
        check("bedrock target logged off: told, nothing sent", u1.heard("just left the game") && !rt.mgr.hasInvite(u1.id, u2.id, rt.env.now()));
        // form could not be sent -> chat fallback
        Rig ru = new Rig(5);
        FP v1 = ru.bedrock(".Eve"), v2 = ru.java("Flo");
        ru.forms.works = false;
        ru.ctl.compassClick(v1, v2);
        check("bedrock form failure falls back to Yes/Cancel chat panel", v1.panels.size() == 1 && v1.panels.get(0).plain().contains("[Yes]"));
        ru.ctl.command(v1, new String[]{"confirm"});
        check("fallback Yes sends", ru.mgr.hasInvite(v1.id, v2.id, ru.env.now()));

    }

    // ---------- packaging and wiring ----------
    static void packagingTests() throws Exception {
        String plugin = Files.readString(Path.of(System.getProperty("plugin")));
        String cfg = Files.readString(Path.of(System.getProperty("cfg")));
        check("plugin version 0.4.0", plugin.contains("version: 0.4.0"));
        check("cue subcommand in usage", plugin.contains("cue"));
        check("command is excelparty", plugin.contains("  excelparty:") && plugin.contains("aliases: [eparty, together]"));
        check("does NOT claim bare /party (mcMMO owns it)", !plugin.contains("\n  party:") && !plugin.contains("[party") && !plugin.contains(", party"));
        check("Jobs still required", plugin.contains("depend: [Jobs]"));
        check("permission default true", plugin.contains("excelparty.use") && plugin.contains("default: true"));
        check("config keys", cfg.contains("multiplier: 1.25") && cfg.contains("radius: 50") && cfg.contains("bonus-mode: party") && cfg.contains("max-party-size: 5")
                && cfg.contains("invite-expiry-seconds: 60") && cfg.contains("compass-click-invite: true"));
        check("controller command constant matches plugin.yml", Controller.CMD.equals("excelparty"));
        Path src = Path.of(System.getProperty("src"));
        StringBuilder all = new StringBuilder();
        try (var w = Files.walk(src)) { for (Path p : (Iterable<Path>) w.filter(x -> x.toString().endsWith(".java") && !x.toString().endsWith("JobsShareSink.java"))::iterator) all.append(Files.readString(p)); }
        String code = all.toString();
        check("no teleport or economy calls in the plugin", !code.contains(".teleport(") && !code.contains("performCommand") && !code.contains("dispatchCommand") && !code.contains("Economy"));
        check("only JobsShareSink touches Jobs economy, never Vault", Files.readString(src.resolve("com/winkreyes/exceltogether/JobsShareSink.java")).contains("Jobs.getEconomy().pay") && !code.contains("net.milkbowl"));
        check("no em dashes in player-facing code", !code.contains("\u2014"));
        // real Cumulus form builders accept our forms
        java.io.File fg = new java.io.File(System.getProperty("fgjar", "/workspace/mc/test26/plugins/floodgate.jar"));   // -Dfgjar passed by build.sh (0.3.0 path retarget); the check still fails loudly if the jar is absent
        if (fg.exists()) {
            ClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{fg.toURI().toURL()}, ClassLoader.getPlatformClassLoader());
            try {
                check("real Cumulus SimpleForm builds (Accept/Decline)", GeyserForms.buildSimple(l, "Party invite", "Body", List.of("Accept", "Decline"), i -> {}) != null);
                check("real Cumulus ModalForm builds", GeyserForms.buildModal(l, "Leave party?", "Body", "Yes, leave", "Stay", b -> {}) != null);
                List<String> many = new ArrayList<>(); for (int i = 0; i < 20; i++) many.add("Player" + i);
                check("real Cumulus picker with 20 buttons builds", GeyserForms.buildSimple(l, "Invite to party", "Body", many, i -> {}) != null);
            } catch (Throwable t) { check("Cumulus build threw " + t, false); }
        } else check("floodgate jar present for form check (UNCONFIRMED if missing)", false);
    }

    public static void main(String[] a) throws Exception {
        managerTests(); nameTests(); controllerTests(); confirmTests(); bonusTests(); packagingTests();
        System.out.println("PartyFeatureTest: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
