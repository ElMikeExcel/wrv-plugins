package com.winkreyes.exceltogether;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * 0.5.4 (BB-17) headless walk: every Back fix from the back-button audit, driven
 * screen by screen on both platforms with the same fake-forms harness as
 * PartyFeatureTest. Each check is one walk step: open a screen, tap Back (or
 * close the form), and assert the named parent is what shows next.
 * Rows: FIX-1 x8, FIX-2 x2, FIX-3 x1 = 11, plus the Java invites-panel parity
 * Back and the Server Menu failure line. Exit code 1 on any failure.
 */
public class BackNavigationTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    static final class FP implements PlayerView {
        final UUID id; final String real; final boolean bedrock;
        final List<String> said = new ArrayList<>(); final List<Panel> panels = new ArrayList<>();
        boolean menuOk = true; int menuCalls = 0;
        FP(String real, boolean bedrock) { this.id = UUID.randomUUID(); this.real = real; this.bedrock = bedrock; }
        public UUID id() { return id; } public String realName() { return real; } public String displayName() { return real; }
        public boolean bedrock() { return bedrock; }
        public void say(String l) { said.add(l); } public void say(Panel p) { panels.add(p); said.add(p.plain()); }
        public void actionBar(String t) { }
        @Override public boolean openServerMenu() { menuCalls++; return menuOk; }
        boolean heard(String s) { return String.join("\n", said).contains(s); }
        Panel lastPanel() { return panels.get(panels.size() - 1); }
        boolean panelHas(String label, String run) {
            for (Panel.Line l : lastPanel().lines) for (Panel.Seg s : l.segs)
                if (s.text.equals("[" + label + "]") && run.equals(s.run)) return true;
            return false;
        }
        boolean panelHasLabel(String label) {
            for (Panel.Line l : lastPanel().lines) for (Panel.Seg s : l.segs)
                if (s.text.equals("[" + label + "]")) return true;
            return false;
        }
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
        final List<String> titles = new ArrayList<>(); final List<List<String>> buttons = new ArrayList<>();
        IntConsumer lastSimple; Consumer<Boolean> lastModal;
        public boolean simple(PlayerView p, String t, String c, List<String> b, IntConsumer cb) {
            titles.add(t); buttons.add(new ArrayList<>(b)); lastSimple = cb; return true;
        }
        public boolean modal(PlayerView p, String t, String c, String y, String n, Consumer<Boolean> cb) {
            titles.add(t); buttons.add(List.of(y, n)); lastModal = cb; return true;
        }
        public boolean custom(PlayerView p, String t, List<Field> f, Consumer<List<Object>> cb) { return false; }
        String lastTitle() { return titles.get(titles.size() - 1); }
        List<String> lastButtons() { return buttons.get(buttons.size() - 1); }
        int countTitle(String t) { int n = 0; for (String x : titles) if (x.equals(t)) n++; return n; }
    }
    static final class Rig {
        final FEnv env = new FEnv(); final FForms forms = new FForms(); final PartyManager mgr; final Controller ctl;
        Rig() {
            mgr = new PartyManager(5, 60_000, 300_000, id -> env.on.containsKey(id));
            ctl = new Controller(mgr, env, forms, "Party bonus note.");
        }
        FP bedrock(String n) { return env.add(new FP(n, true)); }
        FP java(String n) { return env.add(new FP(n, false)); }
        /** Seed a real party of a+b through the manager (no popups). */
        void party(FP a, FP b) { mgr.invite(a.id(), b.id(), env.now()); mgr.accept(b.id(), a.id(), env.now()); }
    }

    public static void main(String[] args) {
        // ---- FIX-2 hub, Bedrock ----
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), b = r.java("Bob");
            r.ctl.open(a);
            check("W1 hub bedrock buttons end in Back", r.forms.lastTitle().equals("Party") && r.forms.lastButtons().equals(List.of("Invite to party", "My party", "Back")));
            r.forms.lastSimple.accept(2);
            check("W1 hub Back opens Server Menu once", a.menuCalls == 1);
            r.ctl.open(a); r.forms.lastSimple.accept(-1);
            check("W2 hub form-close opens Server Menu", a.menuCalls == 2);
            a.menuOk = false; r.ctl.open(a); r.forms.lastSimple.accept(2);
            check("W19 hub Back failure is spoken, not silent", a.heard("The main menu could not open."));
        }
        // ---- FIX-2 hub, Java ----
        {
            Rig r = new Rig(); FP a = r.java("Ann");
            r.ctl.open(a);
            check("W13 hub panel Back runs /menu", a.panelHas("Back", "/menu"));
            check("W13 hub panel keeps Invite/My party", a.panelHas("Invite to party", "/excelparty invite") && a.panelHas("My party", "/excelparty list"));
        }
        // ---- FIX-1 invite picker, Bedrock ----
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), c = r.java("Cal");
            r.ctl.invitePicker(a);
            check("W3 picker bedrock buttons = candidate + Back", r.forms.lastButtons().equals(List.of("Cal", "Back")));
            r.forms.lastSimple.accept(1);
            check("W3 picker Back reopens hub", r.forms.lastTitle().equals("Party"));
            r.ctl.invitePicker(a); r.forms.lastSimple.accept(-1);
            check("W4 picker form-close reopens hub", r.forms.lastTitle().equals("Party"));
            r.ctl.invitePicker(a); r.forms.lastSimple.accept(0);
            check("W5 picker tap still invites", r.mgr.invitesFor(c.id(), r.env.now()).size() == 1);
        }
        // ---- FIX-1 invite picker, Java ----
        {
            Rig r = new Rig(); FP a = r.java("Ann"), c = r.java("Cal");
            r.ctl.invitePicker(a);
            check("W14 picker panel Back reopens hub", a.panelHas("Back", "/excelparty"));
            check("W14 picker panel keeps candidate button", a.panelHas("Cal", "/excelparty invite Cal"));
        }
        // ---- FIX-1 invites list + answer, Bedrock ----
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), b = r.bedrock("Bob");
            r.ctl.open(a);   // remembers Ann's name, as any real command would
            r.mgr.invite(a.id(), b.id(), r.env.now());
            r.ctl.open(b);
            check("W6 hub shows Invites (1)", r.forms.lastButtons().contains("Invites (1)"));
            r.forms.lastSimple.accept(r.forms.lastButtons().indexOf("Invites (1)"));
            check("W6 invites list buttons = From + Back", r.forms.lastTitle().equals("Party invites") && r.forms.lastButtons().equals(List.of("From Ann", "Back")));
            r.forms.lastSimple.accept(1);
            check("W6 invites Back reopens hub", r.forms.lastTitle().equals("Party"));
            // answer flow: reopen list, tap the invite, accept in the modal
            r.ctl.command(b, new String[]{"invites"});
            r.forms.lastSimple.accept(0);
            check("W8 answer modal is Accept/Decline", r.forms.lastTitle().equals("Party invite") && r.forms.lastButtons().equals(List.of("Accept", "Decline")));
            r.forms.lastModal.accept(true);
            check("W8 accept joins the party", r.mgr.inParty(b.id()));
            check("W8 after accept, hub reopens (no invites left)", r.forms.lastTitle().equals("Party"));
        }
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), c = r.bedrock("Cid"), b = r.bedrock("Bob");
            r.mgr.invite(a.id(), b.id(), r.env.now()); r.mgr.invite(c.id(), b.id(), r.env.now());
            r.ctl.command(b, new String[]{"invites"});
            r.forms.lastSimple.accept(0);            // first invite -> modal
            r.forms.lastModal.accept(false);          // decline it
            check("W9 decline with one invite left returns to the list", r.forms.lastTitle().equals("Party invites") && r.forms.lastButtons().size() == 2);
            r.forms.lastSimple.accept(-1);
            check("W7 invites form-close reopens hub", r.forms.lastTitle().equals("Party"));
        }
        // ---- invites panel, Java (parity) ----
        {
            Rig r = new Rig(); FP a = r.java("Ann"), b = r.java("Bob");
            r.mgr.invite(a.id(), b.id(), r.env.now());
            r.ctl.command(b, new String[]{"invites"});
            check("W15 java invites panel Back reopens hub", b.panelHas("Back", "/excelparty"));
        }
        // ---- FIX-1 My party, Bedrock ----
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann");
            r.ctl.myParty(a);
            check("W10 my party (no party) buttons = Invite + Back", r.forms.lastButtons().equals(List.of("Invite to party", "Back")));
            r.forms.lastSimple.accept(1);
            check("W10 Back reopens hub", r.forms.lastTitle().equals("Party"));
            r.ctl.myParty(a); r.forms.lastSimple.accept(-1);
            check("W10 form-close reopens hub", r.forms.lastTitle().equals("Party"));
        }
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), b = r.bedrock("Bob");
            r.party(a, b);
            r.ctl.myParty(a);
            check("W11 my party (in party) has Back, no Close", r.forms.lastButtons().contains("Back") && !r.forms.lastButtons().contains("Close"));
            r.forms.lastSimple.accept(r.forms.lastButtons().indexOf("Back"));
            check("W11 Back reopens hub", r.forms.lastTitle().equals("Party"));
            r.ctl.myParty(a); r.forms.lastSimple.accept(-1);
            check("W11 form-close reopens hub", r.forms.lastTitle().equals("Party"));
        }
        // ---- FIX-1 My party, Java ----
        {
            Rig r = new Rig(); FP a = r.java("Ann"), b = r.java("Bob");
            r.ctl.myParty(a);
            check("W16 my party panel (no party) Back reopens hub", a.panelHas("Back", "/excelparty"));
            r.party(a, b);
            r.ctl.myParty(a);
            check("W17 my party panel (in party) Back reopens hub", a.panelHas("Back", "/excelparty"));
            check("W17 my party panel keeps Leave", a.panelHas("Leave party", "/excelparty leave"));
        }
        // ---- FIX-1 + FIX-3 leave confirm, Bedrock ----
        {
            Rig r = new Rig(); FP a = r.bedrock("Ann"), b = r.bedrock("Bob");
            r.party(a, b);
            r.ctl.leave(a, false);
            check("W12 leave modal buttons = Yes, leave / Back", r.forms.lastTitle().equals("Leave party?") && r.forms.lastButtons().equals(List.of("Yes, leave", "Back")));
            r.forms.lastModal.accept(false);
            check("W12 Back keeps party and reopens My party", r.mgr.inParty(a.id()) && r.forms.lastTitle().equals("My party"));
            r.ctl.leave(a, false); r.forms.lastModal.accept(true);
            check("W12 Yes still leaves", !r.mgr.inParty(a.id()));
        }
        // ---- FIX-3 leave confirm, Java ----
        {
            Rig r = new Rig(); FP a = r.java("Ann"), b = r.java("Bob");
            r.party(a, b);
            r.ctl.leave(a, false);
            check("W18 leave panel Back reopens My party", a.panelHas("Back", "/excelparty list"));
            check("W18 leave panel has no Stay button", !a.panelHasLabel("Stay"));
            check("W18 leave panel keeps Yes, leave", a.panelHas("Yes, leave", "/excelparty leave yes"));
        }
        System.out.println("BackNavigationTest: " + pass + " passed, " + fail + " failed");
        if (fail > 0) System.exit(1);
    }
}
