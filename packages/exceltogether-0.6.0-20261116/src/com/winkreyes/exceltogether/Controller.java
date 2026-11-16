package com.winkreyes.exceltogether;

import java.util.*;

/**
 * All party menu and command logic. Java players get clickable chat panels, Bedrock players get forms.
 * Every state rule lives in PartyManager; this class only checks who is online, words the messages and
 * decides which form or panel to show.
 */
public final class Controller {
    public static final String CMD = "excelparty";      // the button/panel spelling; 0.6.0 U2-D3: /party is the same door (PartyCommandRouter + the party command registration) - one dispatch, two names (D74/Q2)
    private static final long COMPASS_COOLDOWN_MS = 2000L;
    private static final int PICKER_MAX = 20;

    private final PartyManager mgr;
    private final Env env;
    private final Forms forms;          // null = chat only
    private final String bonusNote;
    private McmmoLink link = McmmoLink.NONE;
    private EmLink emLink = EmLink.NONE;
    private int emPollEvery = 2;
    private boolean emMirrorInvites = true;
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

    private java.util.function.Supplier<List<PlayerView>> directory = List::of;
    /** Plugin supplies the online-player directory used to resolve names in admin commands (0.5.3). */
    public void setPlayerDirectory(java.util.function.Supplier<List<PlayerView>> d) { this.directory = d == null ? List::of : d; }

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
    /** Optional EliteMobs mesh leg (0.5.0). Never required; null means off. */
    public void setEmLink(EmLink l) { this.emLink = l == null ? EmLink.NONE : l; }
    EmLink emLink() { return emLink; }
    /** Mesh poll cadence in ticks of {@link #tick()} (about a second each). */
    public void setEmPollEvery(int ticks) { this.emPollEvery = Math.max(1, ticks); }
    /** Whether Together-door invites are also driven into EliteMobs (DESIGN §5). */
    public void setEmMirrorInvites(boolean on) { this.emMirrorInvites = on; }
    private java.util.function.Consumer<String> logSink = s -> { };
    /** Plugin wires its logger here so the mesh can leave its rare log lines (deferred-op drops, debug). */
    public void setLogSink(java.util.function.Consumer<String> s) { if (s != null) this.logSink = s; }
    void logInfo(String line) { try { logSink.accept(line); } catch (Throwable ignored) { } }
    PartyManager mgr() { return mgr; }
    Env env() { return env; }
    String bonusNote() { return bonusNote; }
    private final McmmoSync sync = new McmmoSync(this);
    /** mcMMO says these players' parties may have changed (a join, leave, kick or disband by hand). One-way since 0.6.0: this only reconciles the mirror against the roster of record - it never adopts an mcMMO change into the roster. Safe to call with any ids, any time. */
    public void mcmmoChanged(Collection<UUID> ids, Collection<UUID> joined) { safeRun(() -> sync.reconcile(ids, joined)); }
    /** Pre-change mcMMO event: who might be affected. Plugin reconciles them a tick later via mcmmoChanged. */
    public List<UUID> mcmmoEventIds(UUID player, String oldParty, String newParty) {
        try { return sync.collect(player, oldParty, newParty); } catch (Throwable t) { return player == null ? List.of() : List.of(player); }
    }
    /** Login or plugin enable: mirror-repair for these players (retries a short while until mcMMO has loaded their profile). Never adoption - the roster of record governs. */
    public void mcmmoWatch(Collection<UUID> ids) { safeRun(() -> { for (UUID id : ids) sync.watch(id); }); }
    private final EmSync emSync = new EmSync(this);
    EmSync emSync() { return emSync; }
    private final LoginRebuild loginRebuild = new LoginRebuild(this);
    LoginRebuild loginRebuild() { return loginRebuild; }
    /** 0.6.0 (U2-D4): the plugin arms the one-shot first-boot seed exactly when roster.yml was absent at enable (RosterStore filePresent == false). */
    public void setSeedArmed(boolean on) { loginRebuild.setArmed(on); }
    boolean seedArmed() { return loginRebuild.armed(); }
    /** True when no first-boot seed decision is outstanding for this player (the syncs' login paths consult this before clearing a face). */
    boolean seedSettled(UUID id) { return loginRebuild.settled(id); }
    /** Run this player's first-boot seed attempt if one is due (settled pictures only; UNKNOWN reads retry via the sync watches). */
    void seedLogin(UUID id) { safeRun(() -> loginRebuild.onLogin(id)); }
    /** The EliteMobs picture for these players may need reconciling now (the poll covers everything; this is the prompt path). */
    public void emChanged(Collection<UUID> ids) { safeRun(() -> { emSync.beginCycle(); emSync.reconcile(ids); }); }
    /** Login or plugin enable: reconcile these players' EliteMobs parties once their pictures settle. */
    public void emWatch(Collection<UUID> ids) { safeRun(() -> { for (UUID id : ids) emSync.watch(id); }); }
    private int pollTick = 0;

    private EmLink.EmMirror safeEm(java.util.function.Supplier<EmLink.EmMirror> f) {
        try { return f.get(); } catch (Throwable t) { return EmLink.EmMirror.ERROR; }
    }
    /** Plain-words note when the EliteMobs side could not be mirrored. ExcelTogether itself already worked. */
    private void emMirrorNote(PlayerView to, String other, EmLink.EmMirror m) {
        switch (m) {
            case TARGET_IN_OTHER_PARTY -> to.say("Note: " + other + " is in a different EliteMobs party, so only the ExcelTogether party links you. Your Jobs bonus still works.");
            case FOREIGN_PENDING -> to.say("Note: " + other + " already has a party invite waiting in EliteMobs from another party, so the EliteMobs side was not linked. If they accept here, only the ExcelTogether party links you. Your Jobs bonus still works.");
            case FULL -> to.say("Note: the EliteMobs party is full (5/5), so only the ExcelTogether party links " + other + ". Your Jobs bonus still works.");
            case NO_PERMISSION -> to.say("Note: EliteMobs party permissions are off for you, so this invite is ExcelTogether only. Your Jobs bonus still works.");
            case TARGET_NO_PERMISSION -> to.say("Note: " + other + " can't use EliteMobs parties, so this invite is ExcelTogether only. Your Jobs bonus still works.");
            default -> { }
        }
    }
    /**
     * 0.5.5 (P-TOGETHER-PARTY-1, D1): the accepter-side EliteMobs conflict shared by the invite
     * disclosure and the accept gate. Returns the target's EM party when joining the inviter
     * would split the target between two parties: the target sits in an EM party the inviter is
     * not part of, alongside someone who is not in the inviter's Together party either. A shared
     * EM party (the inviter is in it), a solo EM party, and an EM party made only of the
     * inviter's own Together members all converge on their own (noteJoined / the poll), so they
     * are NOT conflicts. Null when there is no conflict - including whenever EM cannot be read
     * (link unavailable or UNKNOWN): an unreadable picture never blocks a join; the poll stays
     * the backstop, exactly as before. PartyManager stays EM-agnostic by design; this check
     * lives here, where EmLink is already available.
     */
    private EmLink.EmParty emConflict(UUID target, UUID inviter) {
        if (target == null || inviter == null || !emLink.available()) return null;
        EmLink.EmParty theirs;
        try { theirs = emLink.partyOf(target); } catch (Throwable t) { return null; }
        if (theirs == null || !theirs.real() || theirs.members.contains(inviter)) return null;
        PartyManager.Party ours = mgr.partyOf(inviter);
        for (UUID m : theirs.members) {
            if (m.equals(target)) continue;
            if (ours == null || !ours.members.contains(m)) return theirs;
        }
        return null;
    }
    private void safeRun(Runnable r) { try { r.run(); } catch (Throwable ignored) { } }

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
        pn.text("Parties here are run with /party - /" + CMD + " is the same door, just another name for it.");
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

    /**
     * 0.5.4 (BB-17, FIX-2): the hub's return to the ExcelMenu Server Menu, via the
     * plugin's existing opener (the "menu" command, run by the player view). If the
     * menu cannot open (ExcelMenu absent), the player hears so instead of a dead end.
     */
    private void backToServerMenu(PlayerView p) {
        if (!p.openServerMenu()) p.say("The main menu could not open.");
    }

    private boolean bedrockHub(PlayerView p) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        int n = pendingFor(p);
        List<String> b = new ArrayList<>(List.of("Invite to party", "My party"));
        if (n > 0) b.add("Invites (" + n + ")");
        b.add("Back");
        String body = pt == null ? PartyText.NOT_IN_PARTY : "You are in a party of " + sizeText(pt) + ".";
        return forms.simple(p, "Party", body + "\n" + bonusNote, b, i -> {
            // FIX-2: "Back" - and closing the form - return to the Server Menu.
            if (i < 0) { backToServerMenu(p); return; }
            if (i >= b.size()) return;
            String label = b.get(i);
            if (label.equals("Invite to party")) invitePicker(p);
            else if (label.equals("My party")) myParty(p);
            else if (label.startsWith("Invites")) invitesPanel(p);
            else if (label.equals("Back")) backToServerMenu(p);
        });
    }

    private void hubPanel(PlayerView p) {
        PartyManager.Party pt = mgr.partyOf(p.id());
        Panel pn = new Panel();
        pn.text(pt == null ? "Party: " + PartyText.NOT_IN_PARTY + " " + bonusNote : "Party: you are in a party of " + sizeText(pt) + ". " + bonusNote);
        pn.line().button("Invite to party", "/" + CMD + " invite").gap().button("My party", "/" + CMD + " list");
        int n = pendingFor(p);
        if (n > 0) pn.line().button("Invites (" + n + ")", "/" + CMD + " invites");
        // FIX-2: the panel's Back runs the Server Menu opener (/menu) as the player.
        pn.line().button("Back", "/menu");
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
            labels.add("Back");
            int backIdx = labels.size() - 1;
            if (forms.simple(p, "Invite to party", "Tap who you want to invite. They get a popup to accept." + (busy > 0 ? "\n" + busy + " player(s) already in a party are not shown." : ""), labels, i -> {
                // FIX-1: "Back" - and closing the form - return to the party hub.
                if (i < 0 || i == backIdx) { open(p); return; }
                if (i >= ids.size()) return;
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
        // FIX-1: the panel's Back reopens the party hub.
        pn.line().button("Back", "/" + CMD);
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
                // 0.6.0 (U2-D1): invites live hub-side only - nothing is mirrored into
                // mcMMO at invite time; the one-way mirror follows the roster on accept.
                EmLink.EmMirror emInvite = EmLink.EmMirror.ABSENT;
                if (emMirrorInvites) { emInvite = safeEm(() -> emLink.invite(p.id(), t.id())); emMirrorNote(p, tn, emInvite); }   // the EM face: its own invite popup too (§5.1)
                // 0.5.5 (D1): the TARGET hears about an EM conflict before they decide. Until
                // now only the inviter got the mirror note and the popup looked completely
                // normal, so the split-brain accept came as a surprise.
                // 0.5.6 (D4): the same disclosure for a FOREIGN pending EliteMobs invite - the
                // drive above just learned the target holds another party's waiting invite.
                popup(p, t, emConflict(t.id(), p.id()) != null, emInvite == EmLink.EmMirror.FOREIGN_PENDING);
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


    private void popup(PlayerView inviter, PlayerView target, boolean emConflict, boolean foreignPending) {
        PartyManager.Party pt = mgr.partyOf(inviter.id());
        String who = shown(inviter);
        int count = pt == null ? 1 : pt.size();
        String body = who + " invited you to their party (" + count + "/" + mgr.maxSize() + " members now). " + bonusNote
                + "\nThis invite lasts " + ttlSeconds() + " seconds.";
        if (emConflict) body += "\nNote: you are in a different EliteMobs party - accepting here leaves that EliteMobs party and joins this one instead. Parties here are run with /party.";
        if (foreignPending) body += "\nNote: you also have an EliteMobs party invite from another party still waiting. Accepting here joins the ExcelTogether party only and leaves that EliteMobs invite untouched.";
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
        // 0.5.5 (P-TOGETHER-PARTY-1, D1) made this accept read the accepter's EliteMobs party
        // and REFUSE with the reason when it was disjoint, so no permanent split-brain could
        // form. 0.6.0 (one-door rebuild, SCOPING.md RT-2) keeps the check and the protection
        // but changes the outcome to hub behaviour: the accept JOINS the roster of record and
        // the stale EM membership is cleared by EmSync (noteJoined clears it at once; the poll
        // re-materialises the face from the roster) - never kept, never refused. The check
        // runs only when a live invite from the effective inviter exists, so the NO_INVITE /
        // EXPIRED wordings below are untouched.
        PartyManager.Invite liveInvite = null;
        // 0.6.0 (U2-D3, RT-3 / audit D5): snapshot EVERY live inviter before the accept -
        // PartyManager.accept consumes all of the target's invites, so the losing
        // (crossed) inviters can only be told their invite died if they are named here.
        List<UUID> crossedInviters = new ArrayList<>();
        for (PartyManager.Invite i : mgr.invitesFor(p.id(), env.now())) {
            if (inviter == null || inviter.equals(i.inviter)) liveInvite = i;   // invitesFor is oldest first: the last match is the one accept would use
            if (!crossedInviters.contains(i.inviter)) crossedInviters.add(i.inviter);
        }
        boolean hadEmConflict = liveInvite != null && emConflict(p.id(), liveInvite.inviter) != null;
        PartyManager.AcceptOutcome o = mgr.accept(p.id(), inviter, env.now());
        switch (o.result) {
            case NO_INVITE -> p.say("You have no party invite right now.");
            case EXPIRED -> p.say("That invite ran out of time. Ask them to send a new one.");
            case ALREADY_IN_PARTY -> p.say("You are already in a party. Leave it first with /" + CMD + " leave.");
            case INVITER_OFFLINE -> p.say("They are not online anymore, so the invite is gone.");
            case PARTY_FULL -> { p.say("That party filled up (" + mgr.maxSize() + "/" + mgr.maxSize() + "). Ask them to make room."); if (o.invite != null) tell(o.invite.inviter, shown(p) + " tried to join but your party is full."); }
            case OK -> {
                PartyManager.Party pt = o.party;
                emSync.clear(p.id());
                p.say("You joined " + mgr.nameOf(pt.leader) + "'s party (" + sizeText(pt) + "). " + bonusNote);
                if (hadEmConflict) p.say("Note: you were in a different EliteMobs party, so you left it to join this one. Parties here are run with /party.");
                tellParty(pt, p.id(), shown(p) + " joined the party (" + sizeText(pt) + ").");
                if (o.invite != null) {
                    // 0.6.0 (U2-D3, AT-1 wording): the inviter gets an explicit accepted
                    // line, not only the generic joined line the whole party hears.
                    tell(o.invite.inviter, shown(p) + " accepted your party invite.");
                    // 0.6.0 (U2-D3, RT-3 / audit D5): every crossed (losing) inviter is
                    // told their invite died - 0.5.x consumed it silently.
                    for (UUID other : crossedInviters) {
                        if (other.equals(o.invite.inviter)) continue;
                        tell(other, "Your party invite to " + shown(p) + " is gone - they accepted " + mgr.nameOf(o.invite.inviter) + "'s invite instead.");
                    }
                }
                if (o.invite != null) { UUID inv = o.invite.inviter;
                    mcmmoChanged(List.of(p.id(), inv), List.of());   // one-way mirror: write the new roster into mcMMO at once (the poll is the backstop)
                    safeRun(() -> emSync.noteJoined(p.id(), inv)); }   // the EM join drive (§5.2): invite if needed, then accept
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
        // 0.6.0 (U2-D1): no mcMMO invite exists to cancel - invites are hub-side only.
        tell(i.inviter, shown(p) + " declined your party invite.");
    }

    private void invitesPanel(PlayerView p) {
        List<PartyManager.Invite> list = mgr.invitesFor(p.id(), env.now());
        if (list.isEmpty()) { p.say("You have no party invites right now."); return; }
        if (p.bedrock() && forms != null) {
            List<String> b = new ArrayList<>(); List<UUID> ids = new ArrayList<>();
            for (PartyManager.Invite i : list) { b.add("From " + mgr.nameOf(i.inviter)); ids.add(i.inviter); }
            b.add("Back");
            int backIdx = b.size() - 1;
            if (forms.simple(p, "Party invites", "Tap an invite to answer it.", b, k -> {
                // FIX-1: "Back" - and closing the form - return to the party hub.
                if (k < 0 || k == backIdx) { open(p); return; }
                if (k >= ids.size()) return;
                UUID id = ids.get(k);
                forms.modal(p, "Party invite", mgr.nameOf(id) + " invited you to their party.", "Accept", "Decline",
                        yes -> {
                            // FIX-1: a modal has no third button, so answering returns
                            // to the invites list (or the hub when none are left).
                            if (yes) acceptFrom(p, id); else declineFrom(p, id);
                            backFromInviteAnswer(p);
                        });
            })) return;
        }
        Panel pn = new Panel();
        for (PartyManager.Invite i : list) {
            PlayerView v = env.online(i.inviter);
            String real = v == null ? mgr.nameOf(i.inviter) : v.realName();
            pn.line().text("Invite from " + mgr.nameOf(i.inviter) + " ").button("Accept", "/" + CMD + " accept " + real).gap().button("Decline", "/" + CMD + " decline " + real);
        }
        // Parity with the Bedrock list (uncounted in the audit): Back to the hub.
        pn.line().button("Back", "/" + CMD);
        p.say(pn);
    }

    /** 0.5.4 (BB-17, FIX-1): after answering an invite from the list, land back on the invites list while invites remain, else on the party hub. */
    private void backFromInviteAnswer(PlayerView p) {
        if (!mgr.invitesFor(p.id(), env.now()).isEmpty()) invitesPanel(p);
        else open(p);
    }

    // ---------- my party / leave / kick ----------
    public void myParty(PlayerView p) {
        remember(p);
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt == null) {
            if (p.bedrock() && forms != null && forms.simple(p, "My party", "You are not in a party. Invite someone to start one.", List.of("Invite to party", "Back"),
                    i -> {
                        // FIX-1: "Back" - and closing the form - return to the party hub.
                        if (i == 0) invitePicker(p);
                        else open(p);
                    })) return;
            Panel pn = new Panel();
            pn.text("You are not in a party. Invite someone to start one.");
            pn.line().button("Invite to party", "/" + CMD + " invite");
            // FIX-1: the panel's Back reopens the party hub.
            pn.line().button("Back", "/" + CMD);
            p.say(pn);
            return;
        }
        List<String> lines = memberLines(pt);
        boolean full = pt.size() >= mgr.maxSize();
        if (p.bedrock() && forms != null) {
            List<String> b = new ArrayList<>();
            if (!full) b.add("Invite to party");
            b.add("Leave party");
            b.add("Back");
            String body = "Party " + sizeText(pt) + "\n" + String.join("\n", lines) + "\n" + bonusNote + statusBody(p);
            if (forms.simple(p, "My party", body, b, i -> {
                // FIX-1: "Back" - and closing the form - return to the party hub.
                if (i < 0) { open(p); return; }
                if (i >= b.size()) return;
                if (b.get(i).equals("Invite to party")) invitePicker(p);
                else if (b.get(i).equals("Leave party")) leave(p, false);
                else if (b.get(i).equals("Back")) open(p);
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
        // FIX-1: the panel's Back reopens the party hub.
        pn.line().button("Back", "/" + CMD);
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
            if (p.bedrock() && forms != null && forms.modal(p, "Leave party?", "Leave your party of " + sizeText(pt) + "? You can be invited back.", "Yes, leave", "Back",
                    yes -> {
                        // FIX-1 + FIX-3 relabel: "Back" (and closing the modal, which
                        // also reports false) reopens My party instead of dead-ending.
                        if (yes) leave(p, true); else myParty(p);
                    })) return;
            Panel pn = new Panel();
            pn.text("Leave your party of " + sizeText(pt) + "?");
            // FIX-3 relabel: "Stay" becomes a labelled Back; it still reopens My party.
            pn.line().button("Yes, leave", "/" + CMD + " leave yes").gap().button("Back", "/" + CMD + " list");
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
    /** mirror=false: the change came from a face sync, so the faces are not driven again here (no feedback loop); the mcMMO poll still repairs the mirror from the roster. */
    void announce(PartyManager.Change c, String who, String what, boolean mirror) {
        if (mirror) {
            // 0.6.0 (U2-D1): the mcMMO mirror follows the roster - reconcile it now
            // (removed players are taken out of the mirror; a disband empties it).
            // There is no suppression set anymore: with adoption deleted, a leftover
            // mcMMO membership cannot pull anyone back into the roster.
            List<UUID> affected = new ArrayList<>();
            if (c.removed != null) affected.add(c.removed);
            affected.addAll(c.remaining);
            safeRun(() -> sync.hubChanged(affected));
            safeRun(() -> { if (c.removed != null) emLink.leave(c.removed); if (c.disbanded) for (UUID u : c.remaining) emLink.leave(u); });
            // 0.5.3: the EmSync hold is roster-keyed - hand it the roster this change removed them from
            Set<UUID> roster = new HashSet<>(c.remaining); if (c.removed != null) roster.add(c.removed);
            safeRun(() -> { if (c.removed != null) emSync.suppress(c.removed, roster); if (c.disbanded) for (UUID u : c.remaining) emSync.suppress(u, roster); });
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

    // ---------- console / permission admin (0.5.3) ----------
    /**
     * The /excelparty admin branch: usable from the server console and by a player holding
     * excelparty.admin (Mike's standing rule, Nov 3, 2026: "We should always have a way to help
     * the players"). remove and disband go through the SAME removal semantics as a voluntary
     * leave - PartyManager.leave/announce with mirror=true: the EM leave drive plus the
     * EmSync hold and the one-way mcMMO mirror reconcile, so an admin removal sticks
     * exactly like a player's own leave (0.6.0: the mcMMO side needs no suppression -
     * adoption is deleted, so nothing can pull a removed player back).
     * a[0] is "admin"; replies go to the caller (the console log for a console sender).
     */
    public void adminCommand(java.util.function.Consumer<String> reply, String[] a) {
        String usage = "Usage: /" + CMD + " admin status <player> | remove <player> | disband <player>";
        if (a.length < 2) { reply.accept(usage); return; }
        switch (a[1].toLowerCase(Locale.ROOT)) {
            case "status" -> { if (a.length < 3) { reply.accept("Usage: /" + CMD + " admin status <player>"); return; } adminStatus(reply, String.join(" ", Arrays.copyOfRange(a, 2, a.length))); }
            case "remove" -> { if (a.length < 3) { reply.accept("Usage: /" + CMD + " admin remove <player>"); return; } adminRemove(reply, String.join(" ", Arrays.copyOfRange(a, 2, a.length))); }
            case "disband" -> { if (a.length < 3) { reply.accept("Usage: /" + CMD + " admin disband <player>"); return; } adminDisband(reply, String.join(" ", Arrays.copyOfRange(a, 2, a.length))); }
            default -> reply.accept(usage);
        }
    }

    /** Resolves a typed name over online players plus everyone in a Together party (offline members included). */
    private NameMatch.Result adminResolve(String query) {
        Map<UUID, NameMatch.Cand> byId = new LinkedHashMap<>();
        try { for (PlayerView v : directory.get()) byId.put(v.id(), new NameMatch.Cand(v.id(), v.realName(), v.displayName())); } catch (Throwable ignored) { }
        for (UUID u : mgr.allMembers()) if (!byId.containsKey(u)) byId.put(u, new NameMatch.Cand(u, nameOf(u), nameOf(u)));
        return NameMatch.find(query, new ArrayList<>(byId.values()));
    }

    /** @return the resolved id, or null after replying with the not-found / ambiguous message */
    private UUID adminTarget(java.util.function.Consumer<String> reply, String query) {
        NameMatch.Result r = adminResolve(query);
        if (r.isAmbiguous()) {
            StringBuilder sb = new StringBuilder("That name matches more than one player: ");
            List<String> ns = new ArrayList<>();
            for (NameMatch.Cand cand : r.ambiguous) ns.add(cand.real);
            sb.append(String.join(", ", ns)).append(". Use the full name.");
            reply.accept(sb.toString());
            return null;
        }
        if (!r.isFound()) { reply.accept("No online or partied player matches '" + query + "'."); return null; }
        return r.found.id;
    }

    void adminStatus(java.util.function.Consumer<String> reply, String query) {
        UUID id = adminTarget(reply, query);
        if (id == null) return;
        String name = nameOf(id);
        reply.accept("Party status for " + name + ":");
        PartyManager.Party pt = mgr.partyOf(id);
        if (pt == null) {
            reply.accept("  ExcelTogether: not in a party.");
        } else {
            List<String> names = new ArrayList<>();
            for (UUID u : pt.members) {
                String s = nameOf(u);
                if (u.equals(pt.leader)) s += " (leader)";
                if (env.online(u) == null) s += " (offline)";
                names.add(s);
            }
            reply.accept("  ExcelTogether: in a party of " + pt.size() + "/" + mgr.maxSize() + ", leader " + nameOf(pt.leader) + ". Members: " + String.join(", ", names) + ".");
            // 0.6.0: the roster-of-record origin tag - where this party's last change came from (one-door proof surface).
            RosterOrigin org = mgr.originOf(pt.id);
            reply.accept("  ExcelTogether: roster origin of last change: " + (org == null ? "unknown" : org.name()) + " (party id " + pt.id + ", persisted in roster.yml).");
        }
        if (!link.available()) reply.accept("  mcMMO: link off.");
        else {
            McmmoLink.McParty mc;
            try { mc = link.partyOf(id); } catch (Throwable t) { mc = McmmoLink.McParty.UNKNOWN; }
            if (mc == McmmoLink.McParty.UNKNOWN) reply.accept("  mcMMO: could not be read right now.");
            else if (!mc.real()) reply.accept("  mcMMO: not in a party.");
            else reply.accept("  mcMMO: in party '" + mc.name + "' (leader " + nameOf(mc.leader) + ", " + mc.members.size() + " members).");
        }
        if (!emLink.available()) reply.accept("  EliteMobs: link off.");
        else {
            EmLink.EmParty em;
            try { em = emLink.partyOf(id); } catch (Throwable t) { em = EmLink.EmParty.UNKNOWN; }
            if (em == EmLink.EmParty.UNKNOWN) reply.accept("  EliteMobs: could not be read right now.");
            else if (!em.real()) reply.accept("  EliteMobs: not in a party.");
            else reply.accept("  EliteMobs: in a party (leader " + nameOf(em.leader) + ", " + em.members.size() + " members).");
        }
        reply.accept("  mcMMO is a one-way mirror: native mcMMO changes are repaired as drift, never adopted. EliteMobs suppressed (will not be auto-rejoined): " + (emSync.isSuppressed(id) ? "yes" : "no") + ".");
    }

    void adminRemove(java.util.function.Consumer<String> reply, String query) {
        UUID id = adminTarget(reply, query);
        if (id == null) return;
        String name = nameOf(id);
        PartyManager.Party pt = mgr.partyOf(id);
        if (pt == null) { reply.accept(name + " is not in a party."); return; }
        int wasSize = pt.size();
        String wasLeader = nameOf(pt.leader);
        PartyManager.Change ch = new PartyManager.Change();
        PartyManager.LeaveResult r = mgr.leave(id, ch, RosterOrigin.ADMIN);
        if (r == PartyManager.LeaveResult.NOT_IN_PARTY) { reply.accept(name + " is not in a party."); return; }
        tell(id, "You were removed from the party by an admin.");
        announce(ch, name, " was removed from the party by an admin.");
        logInfo("ExcelTogether admin: removed " + name + " from the party (was " + wasSize + ", leader " + wasLeader + ").");
        reply.accept("Removed " + name + " from the party (was " + wasSize + "/" + mgr.maxSize() + ", leader " + wasLeader + "). The mirrors were told; " + name + " will not be auto-rejoined.");
    }

    void adminDisband(java.util.function.Consumer<String> reply, String query) {
        UUID id = adminTarget(reply, query);
        if (id == null) return;
        PartyManager.Party pt = mgr.partyOf(id);
        if (pt == null) { reply.accept(nameOf(id) + " is not in a party."); return; }
        List<UUID> members = new ArrayList<>(pt.members);
        List<String> names = new ArrayList<>();
        for (UUID u : members) names.add(nameOf(u));
        String leaderName = nameOf(pt.leader);
        for (UUID u : members) mgr.leave(u, new PartyManager.Change(), RosterOrigin.ADMIN);   // husk members return NOT_IN_PARTY: safe
        Set<UUID> roster = new HashSet<>(members);
        safeRun(() -> sync.hubChanged(members));   // 0.6.0: the one-way mirror is emptied for the disbanded party
        for (UUID u : members) {
            safeRun(() -> emLink.leave(u));
            safeRun(() -> emSync.suppress(u, roster));
            tell(u, "Your party was disbanded by an admin.");
        }
        logInfo("ExcelTogether admin: disbanded the party of " + members.size() + " (leader " + leaderName + "): " + String.join(", ", names) + ".");
        reply.accept("Disbanded the party of " + members.size() + " (leader " + leaderName + "): " + String.join(", ", names) + ". The mirrors were told; members will not be auto-rejoined.");
    }

    // ---------- time and connection events ----------
    /** About once a second: expire invites, drop members who stayed offline too long. */
    public void tick() {
        PartyManager.SweepResult r = mgr.sweep(env.now());
        if (++pollTick % McmmoSync.POLL_EVERY_TICKS == 0) safeRun(sync::poll);
        if (pollTick % emPollEvery == 0) safeRun(emSync::poll);   // the mesh reconciles against the settled roster (§2.2)
        for (PartyManager.Invite i : r.expired) {
            tell(i.inviter, "Your party invite to " + mgr.nameOf(i.target) + " ran out of time.");
            tell(i.target, "The party invite from " + mgr.nameOf(i.inviter) + " ran out of time.");
            // 0.6.0 (U2-D1): invites are hub-side only - no mcMMO invite to cancel or tidy.
        }
        for (PartyManager.Change c : r.removed) announce(c, mgr.nameOf(c.removed), " was away too long and left the party.");
    }
    public void playerQuit(UUID id) { confirms.remove(id); mgr.onQuit(id, env.now()); }
    public void playerJoin(PlayerView p) {
        mgr.onJoin(p.id()); remember(p);
        mcmmoWatch(List.of(p.id()));
        emWatch(List.of(p.id()));
        // 0.6.0 (U2-D4): the first-boot seed attempt for this login (a no-op unless
        // the seed is armed and this player's one attempt is still outstanding).
        seedLogin(p.id());
        // 0.6.0 (D74/Q4: wording half landed in U2-D3, persistence half in U2-D4 -
        // PartyManager.onQuit no longer purges invites, so a pending invite now
        // genuinely survives the relog): a pending invite that is still live when
        // its target returns is announced to BOTH sides, never silently kept or purged.
        for (PartyManager.Invite i : mgr.invitesFor(p.id(), env.now())) {
            p.say("A party invite from " + mgr.nameOf(i.inviter) + " is still waiting for you - /" + CMD + " accept to join them.");
            tell(i.inviter, shown(p) + " is back - your party invite to them is still open.");
        }
        PartyManager.Party pt = mgr.partyOf(p.id());
        if (pt != null) p.say("You are still in your party (" + sizeText(pt) + ").");
    }
}
