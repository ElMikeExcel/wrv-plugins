package com.winkreyes.excelwaypoints;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * All menu and command logic. Java players get a clickable chat panel, Bedrock players get forms.
 * There is NO teleport call anywhere in this plugin. "Ask to visit" only runs the existing /tpa request
 * as the visitor; the owner must accept it through Essentials.
 */
public final class Controller {
    private final PlaceService svc;
    private final Env env;
    private final Forms forms;               // null = chat only
    private final Pending<String> delConfirm;   // value = place id
    private final Pending<Place.Type> nameInput; // waiting for a typed name

    public Controller(PlaceService svc, Env env, Forms forms) {
        this.svc = svc; this.env = env; this.forms = forms;
        this.delConfirm = new Pending<>(30_000, env::now);
        this.nameInput = new Pending<>(30_000, env::now);
    }

    // ---------- wording ----------
    String worldLabel(String world) {
        String e = env.worldEnvironment(world);
        if (e == null) return "a world that is missing right now";
        return switch (e) { case "NETHER" -> "the Nether"; case "THE_END" -> "the End"; default -> world.equalsIgnoreCase("world") ? "the Overworld" : "the world " + world; };
    }
    static String xyz(Place p) { return "x " + Math.round(p.x) + ", y " + Math.round(p.y) + ", z " + Math.round(p.z); }
    String howFar(PlayerView v, Place p) {
        if (env.worldEnvironment(p.world) == null) return "world missing";
        if (!v.worldName().equals(p.world)) return "other world";
        return Compass.describe(p.x - v.x(), p.z - v.z());
    }
    String brief(PlayerView v, Place p) {
        if (env.worldEnvironment(p.world) == null) return "world missing";
        if (!v.worldName().equals(p.world)) return "other world";
        return Compass.brief(p.x - v.x(), p.z - v.z());
    }
    static String q(String s) { return "\"" + s + "\""; }
    String ownerShown(Place p) { return Names.display(p.ownerName); }
    String saveMessage(PlaceService.Result r) {
        return switch (r.r) {
            case EMPTY_NAME -> "That name has nothing usable in it. Use letters and numbers, like My House.";
            case TOO_MANY -> "You already have " + svc.limit() + " saved places. Delete one first.";
            case DUPLICATE -> "You already have a place named " + q(r.place.name) + ". Pick a different name.";
            case BLOCKED_WORLD -> "Places can't be saved in this world.";
            case NOT_FOUND, NOT_OWNER -> "I can't find that place. It may have been deleted.";
            default -> "";
        };
    }

    // ---------- entry ----------
    public void open(PlayerView p) {
        if (p.bedrock() && forms != null && bedrockMain(p)) return;
        mainPanel(p);
    }
    public void openChat(PlayerView p) { mainPanel(p); }

    /** Handles "/places <args>". Java button clicks come through here too. */
    public void command(PlayerView p, String[] a) {
        if (a.length == 0) { open(p); return; }
        String sub = a[0].toLowerCase(Locale.ROOT);
        String id = a.length > 1 ? a[1] : null;
        String rest = a.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(a, 2, a.length)) : "";
        switch (sub) {
            case "chat" -> openChat(p);
            case "mine", "list" -> minePanel(p);
            case "family" -> familyPanel(p);
            case "save" -> { if (a.length < 2) startNameInput(p, Place.Type.OTHER); else saveHere(p, String.join(" ", java.util.Arrays.copyOfRange(a, 1, a.length)), Place.Type.OTHER, false, false); }
            case "savetype" -> startNameInput(p, Place.Type.parse(id));
            case "view" -> viewPanel(p, id);
            case "compass" -> compass(p, id);
            case "rename" -> { if (rest.isEmpty()) p.say("Type the new name after the command, like: /places rename " + id + " My Farm"); else rename(p, id, rest); }
            case "delete" -> askDelete(p, id);
            case "confirmdelete" -> confirmDelete(p, id);
            case "share" -> share(p, id, !"off".equalsIgnoreCase(a.length > 2 ? a[2] : "on"));
            case "visits" -> visits(p, id, !"off".equalsIgnoreCase(a.length > 2 ? a[2] : "on"));
            case "visit" -> askVisit(p, id);
            case "help" -> help(p);
            default -> help(p);
        }
    }
    void help(PlayerView p) {
        p.say("Places: /places opens the menu. /places save <name> saves this spot. Visits always need the owner to accept.");
    }

    // ---------- Java chat panels ----------
    void mainPanel(PlayerView p) {
        Panel pn = new Panel();
        int n = svc.mine(p.id()).size();
        pn.text("My Places (" + n + " of " + svc.limit() + " saved)");
        pn.line().button("Save this spot here", "/places savetype other").gap().button("My places", "/places mine").gap().button("Family places (" + svc.family(p.id()).size() + ")", "/places family");
        pn.text("Tip: a compass guide never moves you. Visits only happen if the owner accepts.");
        p.say(pn);
    }
    void startNameInput(PlayerView p, Place.Type t) {
        if (svc.mine(p.id()).size() >= svc.limit()) { p.say("You already have " + svc.limit() + " saved places. Delete one first."); return; }
        nameInput.put(p.id(), t);
        Panel pn = new Panel();
        pn.text("Type a name for this spot in chat within 30 seconds. Example: My House");
        pn.line().text("Pick what kind of place it is: ")
                .button("House", "/places savetype house").gap().button("Farm", "/places savetype farm").gap()
                .button("Mine", "/places savetype mine").gap().button("Other", "/places savetype other");
        pn.line().text("Or type it yourself: ").fill("Save this spot as ...", "/places save ");
        p.say(pn);
    }
    /** Called by the chat listener. Returns true if the message was a place name (message is then hidden). */
    public boolean chatInput(PlayerView p, String message) {
        Place.Type t = nameInput.take(p.id(), null);
        if (t == null) return false;
        if (message.trim().equalsIgnoreCase("cancel")) { p.say("Okay, nothing saved."); return true; }
        saveHere(p, message, t, false, false);
        return true;
    }
    public boolean waitingForName(UUID id) { return nameInput.has(id); }
    public void playerLeft(UUID id) { nameInput.clear(id); delConfirm.clear(id); }

    void saveHere(PlayerView p, String rawName, Place.Type t, boolean shareable, boolean allowVisit) {
        PlaceService.Result r = svc.save(p.id(), Names.display(p.realName()), rawName, t, p.worldName(), p.x(), p.y(), p.z(), shareable, allowVisit);
        if (r.ok()) {
            Panel pn = new Panel();
            pn.line().text("Saved " + q(r.place.name) + " here (" + xyz(r.place) + ").");
            pn.line().text("Only you can see it. ").button("Show it to family", "/places share " + r.place.id + " on").gap().button("My places", "/places mine");
            if (p.bedrock()) p.say("Saved " + q(r.place.name) + " here (" + xyz(r.place) + ")."); else p.say(pn);
        } else p.say(saveMessage(r));
    }

    void minePanel(PlayerView p) {
        List<Place> l = svc.mine(p.id());
        Panel pn = new Panel();
        if (l.isEmpty()) pn.text("You have no saved places yet.");
        for (Place pl : l) {
            pn.line().text(pl.type.label + ": " + pl.name + " (" + brief(p, pl) + (pl.shareable ? ", shared" : "") + ")");
            pn.line().text("   ").button("Compass", "/places compass " + pl.id).gap().fill("Rename", "/places rename " + pl.id + " ")
                    .gap().button("Delete", "/places delete " + pl.id).gap().button("Details", "/places view " + pl.id);
        }
        pn.line().button("Save this spot here", "/places savetype other").gap().button("Back", "/places chat");
        p.say(pn);
    }
    void familyPanel(PlayerView p) {
        List<Place> l = svc.family(p.id());
        Panel pn = new Panel();
        if (l.isEmpty()) pn.text("No family places are shared yet. Owners choose what to share.");
        for (Place pl : l) {
            String o = ownerShown(pl);
            pn.line().text(o + ": " + pl.name + " (" + brief(p, pl) + ")");
            Panel.Line b = pn.line().text("   ").button("Compass", "/places compass " + pl.id);
            if (pl.allowVisit) b.gap().button("Ask " + o + " to visit", "/places visit " + pl.id);
        }
        pn.line().button("Back", "/places chat");
        p.say(pn);
    }
    void viewPanel(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null) { p.say("I can't find that place. It may have been deleted or is no longer shared."); return; }
        boolean mine = pl.owner.equals(p.id());
        Panel pn = new Panel();
        pn.text((mine ? "" : ownerShown(pl) + ": ") + pl.type.label + " " + q(pl.name));
        pn.text("Where: " + worldLabel(pl.world) + ", " + xyz(pl));
        pn.text("From you: " + howFar(p, pl));
        if (mine) pn.text("Shown to family: " + (pl.shareable ? "yes" : "no") + (pl.shareable ? ", visit requests: " + (pl.allowVisit ? "on" : "off") : ""));
        Panel.Line b = pn.line().button("Compass", "/places compass " + pl.id);
        if (mine) {
            b.gap().button(pl.shareable ? "Hide from family" : "Show to family", "/places share " + pl.id + (pl.shareable ? " off" : " on"));
            b.gap().button(pl.allowVisit ? "Turn visit requests off" : "Let family ask to visit me here", "/places visits " + pl.id + (pl.allowVisit ? " off" : " on"));
        } else if (pl.allowVisit) b.gap().button("Ask " + ownerShown(pl) + " to visit", "/places visit " + pl.id);
        pn.line().button("Back", mine ? "/places mine" : "/places family");
        p.say(pn);
    }

    // ---------- actions shared by Java and Bedrock ----------
    boolean compass(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null) { p.say("I can't find that place. It may have been deleted or is no longer shared."); return false; }
        if (env.worldEnvironment(pl.world) == null) { p.say(q(pl.name) + " is in a world that is missing right now. Nothing to point at."); return false; }
        if (!p.worldName().equals(pl.world)) { p.say(q(pl.name) + " is in " + worldLabel(pl.world) + ". A compass only works in the same world, so go there first."); return false; }
        if (!p.compassTo(pl)) { p.say("The compass could not be set. Try again from the same world as the place."); return false; }
        double dx = pl.x - p.x(), dz = pl.z - p.z();
        p.say("Compass set to " + q(pl.name) + ": " + Compass.describe(dx, dz) + ". Hold a compass to follow it. This does not move you.");
        p.actionBar(Compass.arrow(dx, dz) + " " + pl.name + ": " + Compass.describe(dx, dz));
        return true;
    }
    void rename(PlayerView p, String id, String raw) {
        PlaceService.Result r = svc.rename(p.id(), id, raw);
        p.say(r.ok() ? "Renamed to " + q(r.place.name) + "." : saveMessage(r));
    }
    void askDelete(PlayerView p, String id) {
        Place pl = id == null ? null : svc.mine(p.id()).stream().filter(x -> x.id.equals(id.toLowerCase(Locale.ROOT))).findFirst().orElse(null);
        if (pl == null) { p.say("I can't find that place. It may already be deleted."); return; }
        String token = delConfirm.put(p.id(), pl.id);
        Panel pn = new Panel();
        pn.text("Delete " + q(pl.name) + "? This can't be undone. You have 30 seconds.");
        pn.line().button("Yes, delete " + pl.name, "/places confirmdelete " + token).gap().button("No, keep it", "/places mine");
        p.say(pn);
    }
    void confirmDelete(PlayerView p, String token) {
        String id = delConfirm.take(p.id(), token);
        if (id == null) { p.say("That delete request expired or was already used. Nothing was deleted."); return; }
        PlaceService.Result r = svc.delete(p.id(), id);
        p.say(r.ok() ? "Deleted " + q(r.place.name) + "." : "That place was already gone.");
    }
    void share(PlayerView p, String id, boolean on) {
        PlaceService.Result r = svc.setShare(p.id(), id, on);
        if (!r.ok()) { p.say(saveMessage(r)); return; }
        p.say(on ? q(r.place.name) + " is now shown to family. They can use a compass to it." : q(r.place.name) + " is hidden from family again.");
    }
    void visits(PlayerView p, String id, boolean on) {
        PlaceService.Result r = svc.setVisits(p.id(), id, on);
        if (!r.ok()) { p.say(saveMessage(r)); return; }
        p.say(on ? "Family can now ask to visit you at " + q(r.place.name) + ". You still accept each request." : "Visit requests are off for " + q(r.place.name) + ".");
    }
    /** The only teleport-related path: runs the existing /tpa request as the visitor. The owner must accept. */
    void askVisit(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null) { p.say("I can't find that place. It may have been deleted or is no longer shared."); return; }
        PlayerView owner = env.online(pl.owner);
        PlaceService.Visit v = svc.canAskVisit(p.id(), pl, owner != null, env.tpaAvailable());
        String o = ownerShown(pl);
        switch (v) {
            case IS_OWNER -> p.say("That's your own place. Use the compass to find it.");
            case NOT_SHARED -> p.say("I can't find that place. It may have been deleted or is no longer shared.");
            case VISITS_OFF -> p.say(o + " has not turned on visit requests for " + q(pl.name) + ".");
            case OWNER_OFFLINE -> p.say(o + " is not online right now, so you can't ask to visit.");
            case NO_ESSENTIALS -> p.say("Visit requests are not available on this server right now.");
            case COOLDOWN -> p.say("You just sent a visit request. Wait a little before asking again.");
            case OK -> {
                svc.markVisitSent(p.id());
                p.runCommand("tpa " + owner.realName());   // existing Essentials request, owner must /tpaccept
                p.say("Asked " + o + " to let you visit " + q(pl.name) + ". Nothing happens unless " + o + " accepts.");
            }
        }
    }

    // ---------- Bedrock forms ----------
    boolean bedrockMain(PlayerView p) {
        List<Place> mine = svc.mine(p.id());
        List<String> btn = new ArrayList<>();
        btn.add("Save this spot here");
        for (Place pl : mine) btn.add(pl.type.label + ": " + pl.name + " (" + brief(p, pl) + ")");
        int fam = svc.family(p.id()).size();
        btn.add("Family places (" + fam + ")");
        btn.add("Close menu");
        String content = "You have " + mine.size() + " of " + svc.limit() + " places saved. Compass guides never move you.";
        return forms.simple(p, "My Places", content, btn, i -> {
            if (i < 0 || i == btn.size() - 1) return;
            if (i == 0) bedrockSave(p);
            else if (i == btn.size() - 2) bedrockFamily(p);
            else bedrockPlace(p, mine.get(i - 1).id);
        });
    }
    void bedrockSave(PlayerView p) {
        if (svc.mine(p.id()).size() >= svc.limit()) { p.say("You already have " + svc.limit() + " saved places. Delete one first."); return; }
        List<String> types = List.of("House", "Farm", "Mine", "Other");
        boolean sent = forms.custom(p, "Save this spot", List.of(
                Forms.Field.input("Name for this spot (example: My House)", "My House", ""),
                Forms.Field.dropdown("What kind of place is it?", types, 0),
                Forms.Field.toggle("Show this place to family (they get a compass to it)", false),
                Forms.Field.toggle("Let family ask to visit me here (I still accept)", false)), v -> {
            if (v == null) return;
            saveHere(p, (String) v.get(0), Place.Type.values()[Math.max(0, Math.min(3, (Integer) v.get(1)))], (Boolean) v.get(2), (Boolean) v.get(3));
        });
        if (!sent) startNameInput(p, Place.Type.OTHER);
    }
    void bedrockPlace(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null || !pl.owner.equals(p.id())) { p.say("I can't find that place. It may have been deleted."); return; }
        List<String> btn = List.of("Show direction (compass)", "Rename this place",
                pl.shareable ? "Hide from family" : "Show to family",
                pl.allowVisit ? "Turn visit requests off" : "Let family ask to visit me here",
                "Delete this place (asks first)", "Back to My Places");
        String content = pl.type.label + " in " + worldLabel(pl.world) + "\n" + xyz(pl) + "\nFrom you: " + howFar(p, pl)
                + "\nShown to family: " + (pl.shareable ? "yes" : "no");
        boolean sent = forms.simple(p, pl.name, content, btn, i -> {
            switch (i) {
                case 0 -> compass(p, pl.id);
                case 1 -> bedrockRename(p, pl.id);
                case 2 -> { share(p, pl.id, !pl.shareable); }
                case 3 -> { visits(p, pl.id, !pl.allowVisit); }
                case 4 -> bedrockDelete(p, pl.id);
                case 5 -> bedrockMain(p);
                default -> { }
            }
        });
        if (!sent) viewPanel(p, id);
    }
    void bedrockRename(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null || !pl.owner.equals(p.id())) { p.say("I can't find that place."); return; }
        boolean sent = forms.custom(p, "Rename place", List.of(Forms.Field.input("New name", "My Farm", pl.name)), v -> {
            if (v != null) rename(p, id, (String) v.get(0));
        });
        if (!sent) p.say("Type: /places rename " + id + " New Name");
    }
    void bedrockDelete(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null || !pl.owner.equals(p.id())) { p.say("I can't find that place. It may already be deleted."); return; }
        boolean sent = forms.modal(p, "Delete place?", "Delete " + q(pl.name) + "? This can't be undone.", "Yes, delete " + pl.name, "No, keep it", yes -> {
            if (yes) { PlaceService.Result r = svc.delete(p.id(), id); p.say(r.ok() ? "Deleted " + q(r.place.name) + "." : "That place was already gone."); }
        });
        if (!sent) askDelete(p, id);
    }
    void bedrockFamily(PlayerView p) {
        List<Place> l = svc.family(p.id());
        if (l.isEmpty()) { p.say("No family places are shared yet. Owners choose what to share."); return; }
        List<String> btn = new ArrayList<>();
        for (Place pl : l) btn.add(ownerShown(pl) + ": " + pl.name);
        btn.add("Back to My Places");
        boolean sent = forms.simple(p, "Family places", "Only places the owner chose to share are listed.", btn, i -> {
            if (i < 0) return;
            if (i == btn.size() - 1) bedrockMain(p); else bedrockFamilyPlace(p, l.get(i).id);
        });
        if (!sent) familyPanel(p);
    }
    void bedrockFamilyPlace(PlayerView p, String id) {
        Place pl = svc.viewable(p.id(), id);
        if (pl == null) { p.say("I can't find that place. It may be no longer shared."); return; }
        String o = ownerShown(pl);
        List<String> btn = new ArrayList<>();
        btn.add("Show direction (compass)");
        if (pl.allowVisit) btn.add("Ask " + o + " to visit (they must accept)");
        btn.add("Back");
        boolean sent = forms.simple(p, o + ": " + pl.name, pl.type.label + " in " + worldLabel(pl.world) + "\nFrom you: " + howFar(p, pl), btn, i -> {
            if (i == 0) compass(p, pl.id);
            else if (i == btn.size() - 1) bedrockFamily(p);
            else if (i == 1) askVisit(p, pl.id);
        });
        if (!sent) viewPanel(p, id);
    }
}
