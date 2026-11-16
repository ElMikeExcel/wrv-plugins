package com.winkreyes.exceltogether;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 0.6.0 (P-PARTY-REBUILD-1, U2-D3) - the door layer, unit layer.
 *
 * Pins, headless:
 *  - PartyCommandRouter's decision table over synthetic command strings:
 *    every candidate roster verb routes (+ would cancel the event), chat/ptp
 *    forms pass through uncancelled, namespaced /mcmmo:party roster forms
 *    route (the unit-layer half of RT-9/AT-5 - the live-registration half is
 *    rig sub-unit U2-R3 and is NOT claimed here), unknown subcommands pass.
 *  - The verb table is CONFIGURABLE: a configured pass-through verb wins over
 *    the routing sets, and a configured table replaces the candidate defaults
 *    with no code change (the authoritative split is rig enumeration U2-R2).
 *  - The routed args drive Controller.command end-to-end (invite -> accept),
 *    including the U2-D3 wording: the inviter's explicit "accepted your party
 *    invite" line (AT-1), the crossed-invite death notice to the losing
 *    inviter (RT-3 / audit D5), and the Q4 return messages (wording half;
 *    invite persistence across a quit is U2-D4).
 * Run: java -Dplugin=<plugin.yml> -Dcfg=<config.yml> -cp <classes> com.winkreyes.exceltogether.PartyCommandRouterTest
 */
public class PartyCommandRouterTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    // ---------- fakes (same shapes as PartyFeatureTest) ----------
    static final class FP implements PlayerView {
        final UUID id; final String real, nick; final boolean bedrock;
        final List<String> said = new ArrayList<>(); final List<Panel> panels = new ArrayList<>(); final List<String> bars = new ArrayList<>();
        FP(String real) { this(UUID.randomUUID(), real); }
        FP(UUID id, String real) { this.id = id; this.real = real; this.nick = real; this.bedrock = false; }
        public UUID id() { return id; } public String realName() { return real; } public String displayName() { return nick; }
        public boolean bedrock() { return bedrock; }
        public void say(String l) { said.add(l); } public void say(Panel p) { panels.add(p); said.add(p.plain()); }
        public void actionBar(String t) { bars.add(t); }
        String all() { return String.join("\n", said); }
        boolean heard(String s) { return all().contains(s); }
        void clear() { said.clear(); panels.clear(); bars.clear(); }
    }
    static final class FEnv implements Env {
        final Map<UUID, FP> on = new LinkedHashMap<>(); long t = 1_000_000L;
        FP add(FP p) { on.put(p.id, p); return p; }
        public PlayerView online(UUID id) { return on.get(id); }
        public List<PlayerView> visibleOthers(PlayerView v) {
            List<PlayerView> o = new ArrayList<>();
            for (FP p : on.values()) if (!p.id.equals(v.id())) o.add(p);
            return o;
        }
        public long now() { return t; }
    }
    static final class FForms implements Forms {
        public boolean simple(PlayerView p, String t, String c, List<String> b, IntConsumer cb) { return false; }
        public boolean modal(PlayerView p, String t, String c, String y, String n, Consumer<Boolean> cb) { return false; }
        public boolean custom(PlayerView p, String t, List<Field> f, Consumer<List<Object>> cb) { return false; }
    }
    static final class Rig {
        final FEnv env = new FEnv(); final PartyManager mgr; final Controller ctl;
        final PartyCommandRouter router = new PartyCommandRouter();
        Rig() {
            mgr = new PartyManager(5, 60_000, 300_000, id -> env.on.containsKey(id));
            ctl = new Controller(mgr, env, new FForms(), "Party bonus: +25% Jobs money and XP when a party member is within 50 blocks.");
        }
        FP java(String n) { return env.add(new FP(n)); }
        /** Route a raw command line exactly as ExcelTogether's preprocess handler does: ROUTE -> dispatch args, PASS -> nothing. */
        boolean type(FP p, String raw) {
            PartyCommandRouter.Result r = router.decide(raw);
            if (r.routed()) ctl.command(p, r.args);
            return r.routed();
        }
    }

    static void decisionTable() {
        PartyCommandRouter r = new PartyCommandRouter();
        // every candidate roster verb routes, plain and namespaced, with args preserved
        for (String v : PartyCommandRouter.DEFAULT_ROSTER_VERBS) {
            check("roster /party " + v + " routes", r.decide("/party " + v).routed());
            check("roster /mcmmo:party " + v + " routes", r.decide("/mcmmo:party " + v).routed());
        }
        check("roster verb with a name arg routes", r.decide("/party invite Bea").routed());
        check("args keep the name's case", Arrays.equals(r.decide("/party invite Bea").args, new String[]{"invite", "Bea"}));
        check("label and verb are case-insensitive", r.decide("/PARTY INVITE Bea").routed() && r.decide("/McMMO:Party Accept").routed());
        check("bare /party routes (opens the hub)", r.decide("/party").routed() && r.decide("/party").args.length == 0);
        check("bare /mcmmo:party routes", r.decide("/mcmmo:party").routed());
        check("join routes (Controller's accept alias)", r.decide("/party join").routed() && r.decide("/party join").sub.equals("join"));
        check("quit routes (Controller's leave alias)", r.decide("/party quit").routed());
        check("info routes (Controller's list alias)", r.decide("/party info").routed());
        // mcMMO-only mutation verbs are intercepted by the door, never passed to mcMMO
        for (String v : PartyCommandRouter.DEFAULT_INTERCEPT_VERBS)
            check("intercept /party " + v + " routes to the door", r.decide("/party " + v).routed());
        // chat / ptp forms pass through uncancelled (D74/Q7 - mcMMO keeps them)
        check("/party chat passes", !r.decide("/party chat").routed());
        check("/party chat <msg> passes", !r.decide("/party chat hello everyone").routed());
        check("/mcmmo:party chat passes", !r.decide("/mcmmo:party chat").routed());
        check("/party teleport passes (ptp-adjacent)", !r.decide("/party teleport Bea").routed());
        check("/party xpshare passes", !r.decide("/party xpshare").routed());
        check("/party itemshare passes", !r.decide("/party itemshare").routed());
        check("/ptp is not the door's label: passes", !r.decide("/ptp").routed() && !r.decide("/ptp accept").routed());
        check("/partychat passes", !r.decide("/partychat hi").routed());
        // unknown subcommands pass through - the router never intercepts what it does not know
        check("unknown subcommand passes", !r.decide("/party frobnicate").routed());
        check("unknown subcommand with args passes", !r.decide("/party xyz abc").routed());
        check("/party admin is not a router verb: passes to the Bukkit path", !r.decide("/party admin status Bea").routed());
        // our own Bukkit labels pass - onCommand dispatches them (one code path, no double-handling)
        check("/excelparty passes (Bukkit dispatch)", !r.decide("/excelparty invite Bea").routed());
        check("/eparty passes", !r.decide("/eparty").routed());
        check("/together passes", !r.decide("/together list").routed());
        check("/exceltogether:party passes", !r.decide("/exceltogether:party invite Bea").routed());
        // degenerate input never routes
        check("null passes", !r.decide(null).routed());
        check("empty passes", !r.decide("").routed() && !r.decide("/").routed());
        check("unrelated command passes", !r.decide("/gamemode creative").routed());
    }

    static void configurability() {
        // pass-through always wins over the routing sets (chat can be forced safe)
        PartyCommandRouter chatInRoster = new PartyCommandRouter(List.of("chat", "invite"), List.of(), List.of());
        check("chat listed as roster still passes (pass-through wins)", !chatInRoster.decide("/party chat").routed());
        check("invite still routes in that table", chatInRoster.decide("/party invite").routed());
        // a configured table replaces the candidate defaults with no code change (U2-R2's landing spot)
        PartyCommandRouter custom = PartyCommandRouter.fromStrings("invite,accept", "", "invite,shout");
        check("configured pass-through removes a default roster verb", !custom.decide("/party invite").routed());
        check("configured roster verb routes", custom.decide("/party accept").routed());
        check("verb dropped from the configured table passes (not hard-coded)", !custom.decide("/party leave").routed());
        // empty config falls back to the candidate defaults, never to an empty door
        PartyCommandRouter empty = PartyCommandRouter.fromStrings("", null, "");
        check("empty config falls back to candidate defaults", empty.decide("/party invite").routed() && !empty.decide("/party chat").routed());
        check("default sets equal the fork candidate sets",
                PartyCommandRouter.DEFAULT_ROSTER_VERBS.containsAll(List.of("invite", "accept", "decline", "leave", "kick", "list", "invites", "status", "help"))
                        && PartyCommandRouter.DEFAULT_PASS_VERBS.contains("chat"));
    }

    static void packaging() throws Exception {
        String plugin = Files.readString(Path.of(System.getProperty("plugin")));
        String cfg = Files.readString(Path.of(System.getProperty("cfg")));
        check("plugin.yml registers the party command", plugin.contains("\n  party:") && plugin.contains("The one party door"));
        check("plugin.yml keeps excelparty with its aliases (Q2)", plugin.contains("  excelparty:") && plugin.contains("aliases: [eparty, together]"));
        check("config.yml carries the candidate roster table", cfg.contains("roster-verbs: [invite, accept, decline, leave, kick, list, invites, status, help, join, quit, info]"));
        check("config.yml carries the candidate intercept table", cfg.contains("intercept-verbs: [create, disband, owner, password, rename, lock, unlock, alliance]"));
        check("config.yml carries the candidate pass-through table", cfg.contains("pass-through-verbs: [chat, teleport, xpshare, itemshare]"));
        check("config.yml names the table as candidate / rig-gated", cfg.contains("CANDIDATE split") && cfg.contains("U2-R2"));
    }

    static void integration() {
        // AT-1 unit layer: /party invite -> /party accept, inviter gets the explicit accepted line
        Rig g = new Rig(); FP ann = g.java("Ann"), bea = g.java("Bea");
        check("routed /party invite dispatches", g.type(ann, "/party invite Bea"));
        check("target got the invite popup", bea.heard("invited you to their party"));
        check("routed /party accept dispatches", g.type(bea, "/party accept"));
        check("accept formed the party", g.mgr.sameParty(ann.id, bea.id));
        check("AT-1: inviter told explicitly that the invite was accepted", ann.heard("Bea accepted your party invite"));
        check("joiner told they joined", bea.heard("You joined Ann's party"));

        // RT-3 unit layer (Controller half): crossed invites - accepting one tells the losing inviter it died
        Rig x = new Rig(); FP a2 = x.java("Ann"), c2 = x.java("Cat"), b2 = x.java("Bea");
        x.type(a2, "/party invite Bea"); x.type(c2, "/party invite Bea");
        check("crossed: target holds both invites", x.mgr.invitesFor(b2.id, x.env.now()).size() == 2);
        x.type(b2, "/party accept Ann");
        check("crossed: target joined the winner's party", x.mgr.sameParty(a2.id, b2.id) && !x.mgr.sameParty(c2.id, b2.id));
        check("crossed: losing inviter told their invite died (D5 fix)", c2.heard("Your party invite to Bea is gone") && c2.heard("accepted Ann's invite instead"));
        check("crossed: winner still gets the accepted line", a2.heard("Bea accepted your party invite"));

        // namespaced form + join alias end-to-end (unit half of RT-9/AT-5)
        Rig n = new Rig(); FP a3 = n.java("Ann"), b3 = n.java("Bea");
        n.type(a3, "/party invite Bea");
        check("routed /mcmmo:party join dispatches as accept", n.type(b3, "/mcmmo:party join") && n.mgr.sameParty(a3.id, b3.id));

        // leave through the door sticks at the hub (router args carry the confirmation)
        check("routed /party leave yes dispatches", n.type(b3, "/party leave yes"));
        check("leaver is out", !n.mgr.inParty(b3.id));

        // Q4 wording half: a still-live invite is announced to both sides when the target (re)joins
        Rig q = new Rig(); FP a4 = q.java("Ann"), b4 = q.java("Bea");
        q.type(a4, "/party invite Bea");
        a4.clear(); b4.clear();
        q.ctl.playerJoin(b4);   // wording only: invite persistence across the quit itself is U2-D4
        check("Q4: returning target told the invite is still waiting", b4.heard("still waiting for you"));
        check("Q4: inviter told the target is back and the invite is still open", a4.heard("is back") && a4.heard("still open"));

        // help carries the one-door wording
        Rig h = new Rig(); FP a5 = h.java("Ann");
        h.ctl.command(a5, new String[]{"help"});
        check("help names /party as the door and /excelparty as the same door", a5.heard("Parties here are run with /party"));
    }

    public static void main(String[] args) throws Exception {
        decisionTable();
        configurability();
        packaging();
        integration();
        System.out.println("PartyCommandRouterTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
