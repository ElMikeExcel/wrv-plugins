package com.winkreyes.exceltogether;

import java.util.*;

/**
 * All party menu and command logic. Java players get clickable chat panels, Bedrock players get forms.
 * Every state rule lives in PartyManager; this class only checks who is online, words the messages and
 * decides which form or panel to show.
 */
public final class Controller {
    public static final String CMD = "excelparty";      // not "party": mcMMO owns /party on this server
    private static final long COMPASS_COOLDOWN_MS = 2000L;
    private static final int PICKER_MAX = 20;

    private final PartyManager mgr;
    private final Env env;
    private final Forms forms;          // null = chat only
    private final String bonusNote;
    private McmmoLink link = McmmoLink.NONE;
    private final Map<UUID, Long> compassAt = new HashMap<>();
    /** Compass-click invites waiting for the inviter's Yes tap: inviter -> target and deadline. */
    private final Map<UUID, Pending> confirms = new HashMap<>();
    public static final long CONFIRM_TTL_MS = 30_000L;
    private static final class Pending {
        final UUID target; final long until;
        Pending(UUID t, long u) { target = t; until = u; }
    }

    public Controller(PartyManager mgr, Env env, Forms forms, String bonusNote) {
        this.mgr = mgr; this.env = env; this.forms = forms; this.bonusNote = bonusNote;
    }

    private java.util.function.Function<PlayerView, List<String>> statusFn = v -> List.of();
    /** Plugin supplies the live bonus status lines (needs Bukkit). */
    public void setStatus(java.util.function.Function<PlayerView, List<String>> f) { this.statusFn = f == null ? v -> List.of() : f; }

    private RangeIndicator indicator = new RangeIndicator();
    /** Plugin supplies the shared WRV-068 range indicator, so the ticker and the cue command see the same per-player toggles. */
    public void setIndicator(RangeIndicator i) { if (i != null) this.indicator = i; }
    RangeIndicator indicator() { return indicator; }
    public void status(PlayerView p) {
        remember(p);
        List<String> ls; try { ls = statusFn.apply(p); } catch (Throwable t) { ls = List.of("Could not check the bonus right now."); }
        for (String l : ls) p.say(l);
    }

    /** Optional mcMMO party mirror. Never required; null means off. */
    public void setLink(McmmoLink l) { this.link = l == null ? McmmoLink.NONE : l; }
    McmmoLink link() { return link; }
    PartyManager mgr() { return mgr; }
    Env env() { return env; }
    String bonusNote() { return bonusNote; }
    private final McmmoSync sync = new McmmoSync(this);
    /** mcMMO says these players' parties may have changed (a join, leave, kick or disband by hand). Safe to call with any ids, any time. */
    public void mcmmoChanged(Collection<UUID> ids, Collection<UUID> joined) { safeRun(() -> sync.reconcile(ids, joined)); }
    /** Pre-change mcMMO event: who might be affected. Plugin reconciles them a tick later via mcmmoChanged. */
    public List<UUID> mcmmoEventIds(UUID player, String oldParty, String newParty) {
        try { return sync.collect(player, oldParty, newParty); } catch (Throwable t) { return player == null ? List.of() : List.of(player); }
    }
    /** Login or plugin enable: adopt these players' mcMMO parties (retries a short while until mcMMO has loaded their profile). */
    public void mcmmoWatch(Collection<UUID> ids) { safeRun(() -> { for (UUID id : ids) sync.watch(id); }); }
    private int pollTick = 0;

    private McmmoLink.Mirror safe(java.util.function.Supplier<McmmoLink.Mirror> f) {
        try { return f.get(); } catch (Throwable t) { return McmmoLink.Mirror.ERROR; }
    }
    private void safeRun(Runnable r) { try { r.run(); } catch (Throwable ignored) { } }
    /** Plain-words note when the mcMMO side could not be mirrored. ExcelTogether itself already worked. */
    private void mirrorNote(PlayerView to, String other, McmmoLink.Mirror m) {
        switch (m) {
            case TARGET_IN_OTHER_PARTY -> to.say("Note: " + other + " is in a different mcMMO party, so only the ExcelTogether party links you. Your Jobs bonus still works.");
            case FULL -> to.say("Note: the mcMMO party is full (server limit), so only the ExcelTogether party links " + other + ". Your Jobs bonus still works.");
            case LOCKED -> to.say("Note: the mcMMO party is locked by its leader, so only the ExcelTogether party links " + other + ". Your Jobs bonus still works.");
            default -> { }
        }
    }
    private void linkHousekeeping() { safeRun(() -> link.cleanup(id -> mgr.hasOutgoing(id, env.now()))); }

    // ---------- wording ----------
    /** How a player is shown to others: nickname if set, else login name, without the Bedrock dot or color codes. */
    public static String shown(String displayName, String realName) {
        String n = NameMatch.norm(displayName).isEmpty() ? "" : stripColor(displayName).trim();
        while (n.startsWith(".") || n.startsWith("*") || n.startsWith("~")) n = n.substring(1);
        if (n.isEmpty()) { n = realName == null ? "" : realName; while (n.startsWith(".")) n = n.substring(1); }
        return n.isEmpty() ? "Someone" : n;
    }
    static String stripColor(String s) { return s == null ? "" : s.replaceAll("(?i)[\u00a7&][0-9a-fk-or]", ""); }
    String shown(PlayerView p) { return shown(p.displayName(), p.realName()); }
    private void remember(PlayerView p) { mgr.rememberName(p.id(), shown(p)); }
    String nameOf(UUID id) { PlayerView v = env.online(id); if (v != null) remember(v); return mgr.nameOf(id); }
    String sizeText(PartyManager.Party pt) { return pt.size() + "/" + mgr.maxSize(); }
    private long ttlSeconds() { return Math.max(1, mgr.ttlMs() / 1000); }

    void tell(UUID id, String line) { PlayerView v = env.online(id); if (v != null) v.say(line); }
    void tellParty(PartyManager.Party pt, UUID except, String line) {
        if (pt == null) return;
        for (UUID u : pt.members) if (!u.equals(except)) tell(u, line);
    }

    // ---------- entry points ----------
    /** Main menu: "Invite to party", "My party", pending invites. */
    public void open(PlayerView p) {
        remember(p);
        if (p.bedrock() && forms != null && bedrockHub(p)) return;
        hubPanel(p);
    }

    public void command(PlayerView p, String[] a) {
        remember(p);
        if (a.length == 0) { open(p); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        String rest = a.length > 1 ? String.join(" ", Arrays.copyOfRange(a, 1, a.length)) : "";
        switch (sub) {
            case "menu", "chat" -> { if (sub.equals("chat")) hubPanel(p); else open(p); }
            case "invite", "add" -> { if (rest.isEmpty()) invitePicker(p); else inviteByQuery(p, rest); }
            case "accept", "yes", "join" -> accept(p, rest);
            case "decline", "deny", "no" -> decline(p, rest);
            case "confirm", "sure" -> confirmInvite(p);
            case "cancel", "nevermind" -> cancelInvite(p);
            case "leave", "quit" -> leave(p, rest.equalsIgnoreCase("yes") || rest.equalsIgnoreCase("confirm"));
            case "kick", "remove" -> kick(p, rest);
            case "list", "info", "mine", "who" -> myParty(p);
            case "invites" -> invitesPanel(p);
            case "status", "bonus", "check" -> status(p);
            case "cue", "cues" -> cue(p, rest);
            case "help", "?" -> help(p);
            default -> help(p);
        }
    }

    private void help(PlayerView p) {
        Panel pn = new Panel();
        pn.text("Party commands (a party shares the +25% Jobs bonus when you are close):");
        pn.line().button("Invite someone", "/" + CMD + " invite").gap().button("My party", "/" + CMD + " list");
        pn.text("/" + CMD + " invite <name>   /" + CMD + " accept   /" + CMD + " decline");
        pn.text("/" + CMD + " leave   /" + CMD + " kick <name> (leader)");
        pn.text("/" + CMD + " cue on|off   (the Party bonus ON/OFF flash and sound)");
        p.say(pn);
    }

    /** WRV-068: per-player toggle for the range transition cues (title flash on any flip, soft sound on turning on). No argument flips it. */
    private void cue(PlayerView p, String rest) {
        String r = rest.trim().toLowerCase(Locale.ROOT);
        boolean on;
        if (r.equals("on")) on = indicator.setCues(p.id(), true);
        else if (r.equals("off")) on = indicator.setCues(p.id(), false);
        else if (r.isEmpty()) on = indicator.toggleCues(p.id());
        else { p.say("Usage: /" + CMD + " cue on|off (the Party bonus ON/OFF flash and sound)."); return; }
        p.say("Party range cues are " + (on ? "ON" : "OFF") + ": a title flash when the party bonus turns on or off, plus a soft sound when it turns on.");
    }

    // ---------- hub ----------
    private int pendingFor(PlayerView p) { return mgr.invitesFor(p.id(), env.now()).size(); }

    private boolean bedrockHub(PlayerView p) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        int n = pendingFor(p);
        List<String> b = new ArrayList<>(List.of("Invite to party", "My party"));
        if (n > 0) b.add("Invites (" + n + ")");
        b.add("Close");
        String body = pt == null ? "You are not in a party." : "You are in a party of " + sizeText(pt) + ".";
        return forms.simple(p, "Party", body + "\n" + bonusNote, b, i -> {
            if (i < 0 || i >= b.size()) return;
            String label = b.get(i);
            if (label.equals("Invite to party")) invitePicker(p);
            else if (label.equals("My party")) myParty(p);
            else if (label.startsWith("Invites")) invitesPanel(p);
        });
    }

    private void hubPanel(PlayerView p) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        Panel pn = new Panel();
        pn.text(pt == null ? "Party: you are not in a party. " + bonusNote : "Party: you are in a party of " + sizeText(pt) + ". " + bonusNote);
        pn.line().button("Invite to party", "/" + CMD + " invite").gap().button("My party", "/" + CMD + " list");
        int n = pendingFor(p);
        if (n > 0) pn.line().button("Invites (" + n + ")", "/" + CMD + " invites");
        p.say(pn);
    }

    // ---------- invite picker ----------
    public void invitePicker(PlayerView p) {
        remember(p);
        PartyManager.Party mine = mgr.partyOf(p.id());
        if (mine != null && mine.size() >= mgr.maxSize()) { p.say("Your party is full (" + sizeText(mine) + ")."); return; }
        List<PlayerView> ok = new ArrayList<>(); int busy = 0;
        for (PlayerView o : env.visibleOthers(p)) {
            if (o.id().equals(p.id())) continue;
            if (mgr.inParty(o.id())) { busy++; continue; }
            ok.add(o);
        }
        ok.sort(Comparator.comparing(o -> shown(o).toLowerCase(Locale.ROOT)));
        if (ok.isEmpty()) {
            p.say(busy > 0 ? "Everyone else online is already in a party." : "Nobody else is online right now. Try again when family joins!");
            return;
        }
        if (p.bedrock() && forms != null) {
            List<String> labels = new ArrayList<>(); List<UUID> ids = new ArrayList<>();
            for (PlayerView o : ok) { if (labels.size() >= PICKER_MAX) break; labels.add(shown(o)); ids.add(o.id()); }
            if (forms.simple(p, "Invite to party", "Tap who you want to invite. They get a popup to accept." + (busy > 0 ? "\n" + busy + " player(s) already in a party are not shown." : ""), labels, i -> {
                if (i < 0 || i >= ids.size()) return;
                PlayerView t = env.online(ids.get(i));
                if (t == null) { p.say("Looks like they just left the game. Try someone else!"); return; }
                inviteTarget(p, t);
            })) return;
        }
        Panel pn = new Panel();
        pn.text("Invite to party: click a player. They get a popup and must accept.");
        int shownCount = 0;
        for (PlayerView o : ok) {
            if (shownCount >= PICKER_MAX) break;
            pn.line().button(shown(o), "/" + CMD + " invite " + o.realName());
            shownCount++;
        }
        if (ok.size() > PICKER_MAX) pn.text("More players are online. Type /" + CMD + " invite <name> for anyone not listed.");
        if (busy > 0) pn.text(busy + " player(s) already in a party are not listed.");
        p.say(pn);
    }

    // ---------- invite ----------
    private List<NameMatch.Cand> candidates(PlayerView viewer) {
        List<NameMatch.Cand> c = new ArrayList<>();
        for (PlayerView o : env.visibleOthers(viewer)) c.add(new NameMatch.Cand(o.id(), o.realName(), o.displayName()));
        return c;
    }

    public void inviteByQuery(PlayerView p, String query) {
        remember(p);
        NameMatch.Result r = NameMatch.find(query, candidates(p));
        if (r.isAmbiguous()) {
            StringBuilder b = new StringBuilder("More than one player matches. Type more of the name: ");
            for (int i = 0; i < r.ambiguous.size(); i++) { if (i > 0) b.append(", "); b.append(r.ambiguous.get(i).real); }
            p.say(b.toString());
            return;
        }
        if (!r.isFound()) {
            // maybe it is the player themselves (visibleOthers hides self)
            if (NameMatch.find(query, List.of(new NameMatch.Cand(p.id(), p.realName(), p.displayName()))).isFound()) { p.say("You can't invite yourself."); return; }
            p.say("I can't find a player named " + query + " online right now.");
            return;
        }
        PlayerView t = env.online(r.found.id);
        if (t == null) { p.say("Looks like they just left the game. Try someone else!"); return; }
        inviteTarget(p, t);
    }

    /** Shared by the picker, the command and the compass click. */
    public void inviteTarget(PlayerView p, PlayerView t) {
        remember(p); remember(t);
        PartyManager.InviteResult r = mgr.invite(p.id(), t.id(), env.now());
        String tn = shown(t);
        switch (r) {
            case SELF -> p.say("You can't invite yourself.");
            case ALREADY_MEMBER -> p.say(tn + " is already in your party.");
            case TARGET_IN_OTHER_PARTY -> p.say(tn + " is already in a party. They need to leave it first.");
            case PARTY_FULL -> p.say("Your party is full (" + sizeText(mgr.partyOf(p.id())) + ").");
            case DUPLICATE -> p.say("You already invited " + tn + ". Give them a minute to answer.");
            case OK -> {
                p.say("Party invite sent to " + tn + ". It lasts " + ttlSeconds() + " seconds.");
                mirrorNote(p, tn, safe(() -> link.invite(p.id(), t.id(), shown(p))));
                popup(p, t);
            }
        }
    }

    /** Called by the compass-click listener. Quiet cooldown so a double tap never spams. */
    public void compassClick(PlayerView p, PlayerView t) {
        long now = env.now();
        Long last = compassAt.get(p.id());
        if (last != null && now - last < COMPASS_COOLDOWN_MS) return;
        compassAt.put(p.id(), now);
        if (p.id().equals(t.id())) { p.say("You can't invite yourself."); return; }
        remember(p); remember(t);
        confirms.put(p.id(), new Pending(t.id(), now + CONFIRM_TTL_MS));
        String tn = shown(t);
        String q = "Invite " + tn + " to your party?";
        if (p.bedrock() && forms != null) {
            UUID tid = t.id();
            boolean sent = forms.modal(p, "Party invite", q + "\nThey still have to accept.", "Yes", "Cancel", yes -> {
                if (yes) confirmInvite(p, tid); else cancelInvite(p);
            });
            if (sent) return;
        }
        Panel pn = new Panel();
        pn.text(q);
        pn.line().button("Yes", "/" + CMD + " confirm").gap().button("Cancel", "/" + CMD + " cancel");
        p.say(pn);
    }

    /** Yes tap from the Java panel. */
    public void confirmInvite(PlayerView p) { confirmInvite(p, null); }

    /** expect (Bedrock form) must match the pending target, so an old form can never invite someone else. */
    void confirmInvite(PlayerView p, UUID expect) {
        Pending pe = confirms.get(p.id());
        if (pe == null || (expect != null && !expect.equals(pe.target))) { p.say("Nothing to confirm. Click the player again with your compass."); return; }
        confirms.remove(p.id());
        if (env.now() > pe.until) { p.say("That invite question timed out. Click the player again with your compass."); return; }
        PlayerView t = env.online(pe.target);
        if (t == null) { p.say("Looks like they just left the game. Try someone else!"); return; }
        inviteTarget(p, t);
    }

    public void cancelInvite(PlayerView p) {
        if (confirms.remove(p.id()) != null) p.say("Okay, no invite sent.");
        else p.say("Nothing to cancel.");
    }


    private void popup(PlayerView inviter, PlayerView target) {
        PartyManager.Party pt = mgr.partyOf(inviter.id());
        String who = shown(inviter);
        int count = pt == null ? 1 : pt.size();
        String body = who + " invited you to their party (" + count + "/" + mgr.maxSize() + " members now). " + bonusNote
                + "\nThis invite lasts " + ttlSeconds() + " seconds.";
        target.actionBar("Party invite from " + who);
        UUID inviterId = inviter.id();
        if (target.bedrock() && forms != null) {
            boolean sent = forms.simple(target, "Party invite", body, List.of("Accept", "Decline"), i -> {
                if (i == 0) acceptFrom(target, inviterId);
                else if (i == 1) declineFrom(target, inviterId);
                // closed (-1): leave the invite pending, it still expires on its own
            });
            if (sent) { target.say(who + " invited you to a party. Answer the popup, or type /" + CMD + " accept."); return; }
        }
        Panel pn = new Panel();
        pn.text(body);
        pn.line().button("Accept", "/" + CMD + " accept " + inviter.realName()).gap().button("Decline", "/" + CMD + " decline " + inviter.realName());
        target.say(pn);
    }

    // ---------- accept / decline ----------
    private UUID resolveInviter(PlayerView p, String query, boolean[] problem) {
        if (query == null || query.isEmpty()) return null;
        List<NameMatch.Cand> c = new ArrayList<>();
        for (PartyManager.Invite i : mgr.invitesFor(p.id(), env.now())) {
            PlayerView v = env.online(i.inviter);
            c.add(new NameMatch.Cand(i.inviter, v == null ? mgr.nameOf(i.inviter) : v.realName(), v == null ? mgr.nameOf(i.inviter) : v.displayName()));
        }
        NameMatch.Result r = NameMatch.find(query, c);
        if (r.isFound()) return r.found.id;
        problem[0] = true;
        if (r.isAmbiguous()) p.say("More than one invite matches. Type more of the name.");
        else p.say("You have no party invite from " + query + ".");
        return null;
    }

    public void accept(PlayerView p, String query) {
        boolean[] bad = {false};
        UUID from = resolveInviter(p, query, bad);
        if (bad[0]) return;
        acceptFrom(p, from);
    }

    void acceptFrom(PlayerView p, UUID inviter) {
        remember(p);
        PartyManager.AcceptOutcome o = mgr.accept(p.id(), inviter, env.now());
        switch (o.result) {
            case NO_INVITE -> p.say("You have no party invite right now.");
            case EXPIRED -> p.say("That invite ran out of time. Ask them to send a new one.");
            case ALREADY_IN_PARTY -> p.say("You are already in a party. Leave it first with /" + CMD + " leave.");
            case INVITER_OFFLINE -> p.say("They are not online anymore, so the invite is gone.");
            case PARTY_FULL -> { p.say("That party filled up (" + mgr.maxSize() + "/" + mgr.maxSize() + "). Ask them to make room."); if (o.invite != null) tell(o.invite.inviter, shown(p) + " tried to join but your party is full."); }
            case OK -> {
                PartyManager.Party pt = o.party;
                sync.clear(p.id());
                p.say("You joined " + mgr.nameOf(pt.leader) + "'s party (" + sizeText(pt) + "). " + bonusNote);
                tellParty(pt, p.id(), shown(p) + " joined the party (" + sizeText(pt) + ").");
                if (o.invite != null) { UUID inv = o.invite.inviter; PlayerView iv = env.online(inv);
                    mirrorNote(p, mgr.nameOf(inv), safe(() -> link.accept(p.id(), inv, iv == null ? mgr.nameOf(inv) : shown(iv))));
                    mcmmoChanged(List.of(p.id(), inv), List.of()); }   // record both as in both systems, so a later hand quit is mirrored at once
            }
        }
    }

    public void decline(PlayerView p, String query) {
        boolean[] bad = {false};
        UUID from = resolveInviter(p, query, bad);
        if (bad[0]) return;
        declineFrom(p, from);
    }

    void declineFrom(PlayerView p, UUID inviter) {
        PartyManager.Invite i = mgr.decline(p.id(), inviter, env.now());
        if (i == null) { p.say("You have no party invite right now."); return; }
        p.say("Invite declined.");
        safeRun(() -> link.cancel(p.id(), i.inviter)); linkHousekeeping();
        tell(i.inviter, shown(p) + " declined your party invite.");
    }

    private void invitesPanel(PlayerView p) {
        List<PartyManager.Invite> list = mgr.invitesFor(p.id(), env.now());
        if (list.isEmpty()) { p.say("You have no party invites right now."); return; }
        if (p.bedrock() && forms != null) {
            List<String> b = new ArrayList<>(); List<UUID> ids = new ArrayList<>();
            for (PartyManager.Invite i : list) { b.add("From " + mgr.nameOf(i.inviter)); ids.add(i.inviter); }
            if (forms.simple(p, "Party invites", "Tap an invite to answer it.", b, k -> {
                if (k < 0 || k >= ids.size()) return;
                UUID id = ids.get(k);
                forms.modal(p, "Party invite", mgr.nameOf(id) + " invited you to their party.", "Accept", "Decline",
                        yes -> { if (yes) acceptFrom(p, id); else declineFrom(p, id); });
            })) return;
        }
        Panel pn = new Panel();
        for (PartyManager.Invite i : list) {
            PlayerView v = env.online(i.inviter);
            String real = v == null ? mgr.nameOf(i.inviter) : v.realName();
            pn.line().text("Invite from " + mgr.nameOf(i.inviter) + " ").button("Accept", "/" + CMD + " accept " + real).gap().button("Decline", "/" + CMD + " decline " + real);
        }
        p.say(pn);
    }

    // ---------- my party / leave / kick ----------
    public void myParty(PlayerView p) {
        remember(p);
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt == null) {
            if (p.bedrock() && forms != null && forms.simple(p, "My party", "You are not in a party. Invite someone to start one.", List.of("Invite to party", "Close"),
                    i -> { if (i == 0) invitePicker(p); })) return;
            Panel pn = new Panel();
            pn.text("You are not in a party. Invite someone to start one.");
            pn.line().button("Invite to party", "/" + CMD + " invite");
            p.say(pn);
            return;
        }
        List<String> lines = memberLines(pt);
        boolean full = pt.size() >= mgr.maxSize();
        if (p.bedrock() && forms != null) {
            List<String> b = new ArrayList<>();
            if (!full) b.add("Invite to party");
            b.add("Leave party");
            b.add("Close");
            String body = "Party " + sizeText(pt) + "\n" + String.join("\n", lines) + "\n" + bonusNote + statusBody(p);
            if (forms.simple(p, "My party", body, b, i -> {
                if (i < 0 || i >= b.size()) return;
                if (b.get(i).equals("Invite to party")) invitePicker(p);
                else if (b.get(i).equals("Leave party")) leave(p, false);
            })) return;
        }
        Panel pn = new Panel();
        pn.text("Your party (" + sizeText(pt) + "):");
        for (String l : lines) pn.text(l);
        pn.text(bonusNote);
        try { for (String l : statusFn.apply(p)) pn.text(l); } catch (Throwable ignored) { }
        Panel.Line act = pn.line();
        if (!full) act.button("Invite more", "/" + CMD + " invite").gap();
        act.button("Leave party", "/" + CMD + " leave");
        p.say(pn);
    }

    private String statusBody(PlayerView p) {
        try { StringBuilder sb = new StringBuilder(); for (String l : statusFn.apply(p)) sb.append("\n").append(l); return sb.toString(); } catch (Throwable t) { return ""; }
    }
    private List<String> memberLines(PartyManager.Party pt) {
        List<String> out = new ArrayList<>();
        for (UUID u : pt.members) {
            PlayerView v = env.online(u);
            String n = v != null ? shown(v) : mgr.nameOf(u);
            String tag = (u.equals(pt.leader) ? " (leader)" : "") + (v == null ? " (offline)" : "");
            out.add("- " + n + tag);
        }
        return out;
    }

    /** confirmed=false asks first (fat-finger safe), confirmed=true leaves. */
    public void leave(PlayerView p, boolean confirmed) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt == null) { p.say("You are not in a party."); return; }
        if (!confirmed) {
            if (p.bedrock() && forms != null && forms.modal(p, "Leave party?", "Leave your party of " + sizeText(pt) + "? You can be invited back.", "Yes, leave", "Stay",
                    yes -> { if (yes) leave(p, true); })) return;
            Panel pn = new Panel();
            pn.text("Leave your party of " + sizeText(pt) + "?");
            pn.line().button("Yes, leave", "/" + CMD + " leave yes").gap().button("Stay", "/" + CMD + " list");
            p.say(pn);
            return;
        }
        PartyManager.Change c = new PartyManager.Change();
        PartyManager.LeaveResult r = mgr.leave(p.id(), c);
        if (r == PartyManager.LeaveResult.NOT_IN_PARTY) { p.say("You are not in a party."); return; }
        p.say("You left the party.");
        announce(c, shown(p), " left the party.");
    }

    public void kick(PlayerView p, String query) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt == null) { p.say("You are not in a party."); return; }
        if (!pt.leader.equals(p.id())) { p.say("Only the party leader can remove someone."); return; }
        if (query.isEmpty()) { p.say("Type who to remove, like /" + CMD + " kick Name"); return; }
        List<NameMatch.Cand> c = new ArrayList<>();
        for (UUID u : pt.members) {
            if (u.equals(p.id())) continue;
            PlayerView v = env.online(u);
            c.add(new NameMatch.Cand(u, v == null ? mgr.nameOf(u) : v.realName(), v == null ? mgr.nameOf(u) : v.displayName()));
        }
        NameMatch.Result r = NameMatch.find(query, c);
        if (r.isAmbiguous()) { p.say("More than one member matches. Type more of the name."); return; }
        if (!r.isFound()) { p.say(query + " is not in your party."); return; }
        PartyManager.Change ch = new PartyManager.Change();
        PartyManager.KickResult k = mgr.kick(p.id(), r.found.id, ch);
        if (k != PartyManager.KickResult.OK) { p.say("That didn't work."); return; }
        String nm = mgr.nameOf(r.found.id);
        p.say("Removed " + nm + " from the party.");
        tell(r.found.id, "You were removed from the party.");
        announce(ch, nm, " was removed from the party.");
    }

    void announce(PartyManager.Change c, String who, String what) {
        announce(c, who, what, true);
    }
    /** mirror=false: the change itself came from mcMMO, so never push it back (no feedback loop). */
    void announce(PartyManager.Change c, String who, String what, boolean mirror) {
        if (mirror) {
            safeRun(() -> { if (c.removed != null) link.leave(c.removed); if (c.disbanded) for (UUID u : c.remaining) link.leave(u); });
            // we removed them from ours on purpose: a leftover mcMMO membership must not pull them back in
            safeRun(() -> { if (c.removed != null) sync.suppress(c.removed); if (c.disbanded) for (UUID u : c.remaining) sync.suppress(u); });
        }
        if (c.disbanded) {
            for (UUID u : c.remaining) tell(u, who + what + " The party ended because only one member is left.");
            return;
        }
        for (UUID u : c.remaining) {
            tell(u, who + what);
            if (u.equals(c.newLeader)) tell(u, "You are now the party leader.");
            else if (c.newLeader != null) tell(u, mgr.nameOf(c.newLeader) + " is now the party leader.");
        }
    }

    // ---------- time and connection events ----------
    /** About once a second: expire invites, drop members who stayed offline too long. */
    public void tick() {
        PartyManager.SweepResult r = mgr.sweep(env.now());
        if (++pollTick % McmmoSync.POLL_EVERY_TICKS == 0) safeRun(sync::poll);
        for (PartyManager.Invite i : r.expired) {
            tell(i.inviter, "Your party invite to " + mgr.nameOf(i.target) + " ran out of time.");
            tell(i.target, "The party invite from " + mgr.nameOf(i.inviter) + " ran out of time.");
            safeRun(() -> link.cancel(i.target, i.inviter));
        }
        if (!r.expired.isEmpty()) linkHousekeeping();
        for (PartyManager.Change c : r.removed) announce(c, mgr.nameOf(c.removed), " was away too long and left the party.");
    }
    public void playerQuit(UUID id) { confirms.remove(id); mgr.onQuit(id, env.now()); }
    public void playerJoin(PlayerView p) {
        mgr.onJoin(p.id()); remember(p);
        mcmmoWatch(List.of(p.id()));
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt != null) p.say("You are still in your party (" + sizeText(pt) + ").");
    }
}
