package com.winkreyes.excelwaypoints;

import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Dependency-free test runner. Exit code 1 on any failure. Covers the edge-case list in design-WRV-017.md. */
public class WaypointsTest {
    static int fail = 0, pass = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }

    // ---------- fakes ----------
    static final class FP implements PlayerView {
        final UUID id = UUID.randomUUID(); final String name; final boolean bedrock; String world = "world"; double x, y = 64, z;
        final List<String> said = new ArrayList<>(); final List<Panel> panels = new ArrayList<>(); final List<String> bars = new ArrayList<>();
        final List<String> cmds = new ArrayList<>(); String compass;
        FP(String n, boolean b) { name = n; bedrock = b; }
        public UUID id() { return id; } public String realName() { return name; } public boolean bedrock() { return bedrock; }
        public String worldName() { return world; } public double x() { return x; } public double y() { return y; } public double z() { return z; }
        public void say(String l) { said.add(l); } public void say(Panel p) { panels.add(p); said.add(p.plain()); }
        public void actionBar(String t) { bars.add(t); }
        public boolean compassTo(Place p) { if (!p.world.equals(world)) return false; compass = p.x + "," + p.z; return true; }
        public void runCommand(String c) { cmds.add(c); }
        String last() { return said.isEmpty() ? "" : said.get(said.size() - 1); }
        boolean saidAny(String s) { for (String t : said) if (t.contains(s)) return true; return false; }
        FP at(String w, double x, double z) { world = w; this.x = x; this.z = z; return this; }
    }
    static final class FE implements Env {
        final Map<UUID, FP> online = new HashMap<>(); final Map<String, String> worlds = new HashMap<>(); boolean tpa = true; long now = 1_000_000;
        FE() { worlds.put("world", "NORMAL"); worlds.put("world_nether", "NETHER"); worlds.put("world_the_end", "THE_END"); worlds.put("em_adventurers_guild", "NORMAL"); }
        FE join(FP... ps) { for (FP p : ps) online.put(p.id, p); return this; }
        public PlayerView online(UUID id) { return online.get(id); }
        public String worldEnvironment(String w) { return worlds.get(w); }
        public boolean tpaAvailable() { return tpa; }
        public long now() { return now; }
    }
    static final class FF implements Forms {
        String title, content; List<String> buttons; IntConsumer simple; Consumer<Boolean> modal; Consumer<List<Object>> custom; List<Forms.Field> fields;
        boolean accept = true; int sent = 0; String kind;
        public boolean simple(PlayerView p, String t, String c, List<String> b, IntConsumer cb) { if (!accept) return false; sent++; kind = "simple"; title = t; content = c; buttons = b; simple = cb; return true; }
        public boolean modal(PlayerView p, String t, String c, String y, String n, Consumer<Boolean> cb) { if (!accept) return false; sent++; kind = "modal"; title = t; content = c; buttons = List.of(y, n); modal = cb; return true; }
        public boolean custom(PlayerView p, String t, List<Forms.Field> f, Consumer<List<Object>> cb) { if (!accept) return false; sent++; kind = "custom"; title = t; fields = f; custom = cb; return true; }
    }

    static File tmp() throws Exception { File d = Files.createTempDirectory("wpt").toFile(); d.deleteOnExit(); return d; }
    static List<String> warns;
    static PlaceService svc(File dir, FE env) {
        warns = new ArrayList<>();
        PlaceStore st = new PlaceStore(new File(dir, "places.yml"), warns::add); st.load();
        return new PlaceService(st, 5, List.of("em_adventurers_guild"), 30_000, env::now);
    }
    static Place find(PlaceService s, UUID o, String name) { for (Place p : s.mine(o)) if (p.name.equals(name)) return p; return null; }

    public static void main(String[] args) throws Exception {
        names(); compass(); pending(); service(); store(); controllerJava(); controllerBedrock(); visits(); formsReal(); noTeleport(); perf(); metadata();
        System.out.println(pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }

    // ---------- 3. names ----------
    static void names() {
        check("name plain", Names.clean("My House").equals("My House"));
        check("name color code section", Names.clean("\u00a7cRed\u00a7r Barn").equals("Red Barn"));
        check("name color code ampersand", Names.clean("&aGreen &lBold").equals("Green Bold"));
        check("name empty", Names.clean("").isEmpty());
        check("name null", Names.clean(null).isEmpty());
        check("name only spaces", Names.clean("    ").isEmpty());
        check("name only symbols", Names.clean("<>{}[]#$%^*").isEmpty());
        check("name newline tab", Names.clean("A\n\tB").equals("A B"));
        check("name too long cut", Names.clean("x".repeat(100)).length() == Names.MAX);
        check("name too long cut trims", !Names.clean("abcdefghijklmnopqrstuvw yz").endsWith(" "));
        check("name apostrophe ok", Names.clean("Mike's House").equals("Mike's House"));
        check("name emoji removed", Names.clean("Farm \uD83D\uDE00").equals("Farm"));
        check("name no crash on surrogate cut", Names.clean("a".repeat(23) + "\uD83D\uDE00\uD83D\uDE00").length() <= Names.MAX);
        check("name command injection chars", Names.clean("/op me; ${x}").equals("op me x"));
        check("name display floodgate dot", Names.display(".MikeExcel").equals("MikeExcel"));
        check("name display java", Names.display("Marvin").equals("Marvin"));
        check("name display just dot", Names.display(".").equals("Someone"));
        check("name display null", Names.display(null).equals("Someone"));
    }
    static void compass() {
        check("dir north", Compass.word(0, -10).equals("north"));
        check("dir south", Compass.word(0, 10).equals("south"));
        check("dir east", Compass.word(10, 0).equals("east"));
        check("dir west", Compass.word(-10, 0).equals("west"));
        check("dir north-east", Compass.word(10, -10).equals("north-east"));
        check("dir south-west", Compass.word(-10, 10).equals("south-west"));
        check("dir south-east", Compass.word(10, 10).equals("south-east"));
        check("dir north-west", Compass.word(-10, -10).equals("north-west"));
        check("describe 120 ne", Compass.describe(85, -85).equals("120 blocks north-east"));
        check("describe 1 block", Compass.describe(1, 0).equals("right here"));
        check("describe 3 blocks", Compass.describe(0, -3).equals("3 blocks north"));
        check("describe right here", Compass.describe(0.5, 0.5).equals("right here"));
        check("brief", Compass.brief(0, 120).equals("120 S"));
        check("arrow east", Compass.arrow(5, 0).equals("\u2192"));
        check("describe huge", Compass.describe(1e7, 0).startsWith("10000000 blocks"));
    }
    static void pending() {
        long[] t = {0}; Pending<String> p = new Pending<>(30_000, () -> t[0]); UUID u = UUID.randomUUID();
        String tok = p.put(u, "a");
        check("pending wrong token", p.take(u, "nope") == null);
        check("pending right token once", "a".equals(p.take(u, tok)));
        check("pending second take empty (double tap)", p.take(u, tok) == null);
        String t2 = p.put(u, "b"); t[0] = 30_001;
        check("pending expires", p.take(u, t2) == null);
        t[0] = 0; p.put(u, "c"); t[0] = 30_000;
        check("pending edge 30s still valid", p.has(u));
        p.clear(u);
        check("pending cleared", !p.has(u));
    }

    // ---------- 1, 2, 7, 11, 12, 15 service ----------
    static void service() throws Exception {
        FE env = new FE(); PlaceService s = svc(tmp(), env); UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        for (int i = 1; i <= 5; i++) check("save " + i, s.save(a, "Mike", "Place " + i, Place.Type.HOUSE, "world", i, 64, i, false, false).ok());
        check("6th refused", s.save(a, "Mike", "Place 6", Place.Type.HOUSE, "world", 0, 64, 0, false, false).r == PlaceService.R.TOO_MANY);
        check("limit is per player", s.save(b, "Kay", "Place 1", Place.Type.FARM, "world", 0, 64, 0, false, false).ok());
        check("delete frees slot", s.delete(a, find(s, a, "Place 5").id).ok());
        check("duplicate refused", s.save(a, "Mike", "place 3", Place.Type.MINE, "world", 0, 64, 0, false, false).r == PlaceService.R.DUPLICATE);
        check("save after delete", s.save(a, "Mike", "Place 6", Place.Type.MINE, "world", 0, 64, 0, false, false).ok());
        check("empty name refused", s.save(b, "Kay", "&c", Place.Type.OTHER, "world", 0, 64, 0, false, false).r == PlaceService.R.EMPTY_NAME);
        check("guild world blocked", s.save(b, "Kay", "Guild", Place.Type.OTHER, "em_adventurers_guild", 0, 64, 0, false, false).r == PlaceService.R.BLOCKED_WORLD);
        check("guild world blocked any case", s.save(b, "Kay", "Guild", Place.Type.OTHER, "EM_Adventurers_Guild", 0, 64, 0, false, false).r == PlaceService.R.BLOCKED_WORLD);
        check("nether allowed", s.save(b, "Kay", "Portal", Place.Type.OTHER, "world_nether", 0, 64, 0, false, false).ok());
        check("end allowed", s.save(b, "Kay", "End base", Place.Type.OTHER, "world_the_end", 0, 64, 0, false, false).ok());
        check("null world refused", s.save(b, "Kay", "Nope", Place.Type.OTHER, null, 0, 64, 0, false, false).r == PlaceService.R.BLOCKED_WORLD);
        // ownership
        Place pa = find(s, a, "Place 1");
        check("other cannot delete", s.delete(b, pa.id).r == PlaceService.R.NOT_FOUND && find(s, a, "Place 1") != null);
        check("other cannot rename", s.rename(b, pa.id, "Hack").r == PlaceService.R.NOT_FOUND && pa.name.equals("Place 1"));
        check("other cannot share", s.setShare(b, pa.id, true).r == PlaceService.R.NOT_FOUND && !pa.shareable);
        check("other cannot toggle visits", s.setVisits(b, pa.id, true).r == PlaceService.R.NOT_FOUND && !pa.allowVisit);
        check("rename ok", s.rename(a, pa.id, "Big House").ok() && pa.name.equals("Big House"));
        check("rename to own name ok", s.rename(a, pa.id, "big house").ok());
        check("rename duplicate refused", s.rename(a, pa.id, "Place 2").r == PlaceService.R.DUPLICATE);
        check("rename empty refused", s.rename(a, pa.id, "  ").r == PlaceService.R.EMPTY_NAME);
        check("bad id", s.delete(a, "zzzzzz").r == PlaceService.R.NOT_FOUND && s.delete(a, null).r == PlaceService.R.NOT_FOUND);
        // sharing
        check("private place not in family list", s.family(b).stream().noneMatch(p -> p.owner.equals(a)));
        s.setShare(a, pa.id, true);
        check("shared place in family list", s.family(b).stream().anyMatch(p -> p.id.equals(pa.id)));
        check("own place not in own family list", s.family(a).stream().noneMatch(p -> p.owner.equals(a)));
        check("viewable shared", s.viewable(b, pa.id) != null);
        s.setShare(a, pa.id, false);
        check("viewable after hide is null", s.viewable(b, pa.id) == null && s.viewable(a, pa.id) != null);
        s.setVisits(a, pa.id, true);
        check("visits on implies shared", pa.shareable && pa.allowVisit);
        s.setShare(a, pa.id, false);
        check("hide also turns visits off", !pa.allowVisit);
        // admin
        check("admin delete any owner", s.adminDelete(pa.id).ok() && s.store().get(pa.id) == null);
        check("admin delete missing", s.adminDelete("nope").r == PlaceService.R.NOT_FOUND);
        // 2 players save at the same instant
        FE e2 = new FE(); PlaceService s2 = svc(tmp(), e2); UUID c = UUID.randomUUID();
        Thread[] th = new Thread[8]; int[] okc = {0};
        for (int i = 0; i < th.length; i++) { final int k = i; th[i] = new Thread(() -> { if (s2.save(c, "C", "Same", Place.Type.OTHER, "world", k, 64, k, false, false).ok()) synchronized (okc) { okc[0]++; } }); }
        for (Thread t : th) t.start(); for (Thread t : th) t.join();
        check("simultaneous same-name save: exactly one wins", okc[0] == 1 && s2.mine(c).size() == 1);
        Thread[] th2 = new Thread[12]; int[] ok2 = {0};
        UUID d = UUID.randomUUID();
        for (int i = 0; i < th2.length; i++) { final int k = i; th2[i] = new Thread(() -> { if (s2.save(d, "D", "N" + k, Place.Type.OTHER, "world", 0, 64, 0, false, false).ok()) synchronized (ok2) { ok2[0]++; } }); }
        for (Thread t : th2) t.start(); for (Thread t : th2) t.join();
        check("simultaneous saves never pass the limit", ok2[0] == 5 && s2.mine(d).size() == 5);
        // refresh names
        s.refreshOwnerName(b, "KayNew");
        check("owner name refresh", s.mine(b).stream().allMatch(p -> p.ownerName.equals("KayNew")));
    }

    // ---------- 11 store ----------
    static void store() throws Exception {
        FE env = new FE(); File dir = tmp(); PlaceService s = svc(dir, env); UUID a = UUID.randomUUID();
        s.save(a, "Mike", "House", Place.Type.HOUSE, "world", 10.5, 70, -20.25, true, true);
        s.save(a, "Mike", "Gone World", Place.Type.FARM, "removed_world", 1, 2, 3, false, false);
        PlaceService again = svc(dir, env);
        Place h = find(again, a, "House");
        check("persist after restart", h != null && h.x == 10.5 && h.z == -20.25 && h.shareable && h.allowVisit && h.type == Place.Type.HOUSE && h.world.equals("world"));
        check("persist keeps world-missing place", find(again, a, "Gone World") != null);
        check("persist no warnings", warns.isEmpty());
        // missing file
        PlaceService empty = svc(tmp(), env);
        check("missing file starts empty", empty.store().all().isEmpty() && warns.isEmpty());
        // corrupt file
        File bad = tmp(); Files.writeString(new File(bad, "places.yml").toPath(), "places: [this is : not {valid yaml\n\t- ::");
        PlaceService c = svc(bad, env);
        check("corrupt file boots empty", c.store().all().isEmpty());
        check("corrupt file warned", warns.stream().anyMatch(w -> w.contains("Starting empty")));
        File[] kept = bad.listFiles((d, n) -> n.startsWith("places.yml.corrupt-"));
        check("corrupt file backup kept", kept != null && kept.length == 1);
        check("corrupt backup is the original", kept != null && kept.length == 1 && Files.readString(kept[0].toPath()).contains("not {valid yaml"));
        check("can save after corrupt", c.save(a, "Mike", "New", Place.Type.OTHER, "world", 0, 0, 0, false, false).ok());
        // partial garbage entries
        File part = tmp();
        Files.writeString(new File(part, "places.yml").toPath(), String.join("\n",
                "places:", "  good01:", "    owner: " + a, "    ownerName: Mike", "    name: Fine", "    type: FARM", "    world: world", "    x: 1.0", "    y: 2.0", "    z: 3.0",
                "  bad001:", "    owner: not-a-uuid", "    name: X", "  bad002: 5", "  bad003:", "    owner: " + a, "    name: NoCoords", "    world: world", ""));
        PlaceService pp = svc(part, env);
        check("bad entries skipped, good kept", pp.store().all().size() == 1 && find(pp, a, "Fine") != null);
        check("bad entries warned", warns.stream().anyMatch(w -> w.contains("Skipped 3")));
        File tmpLeft = new File(dir, "places.yml.tmp");
        check("no temp file left behind", !tmpLeft.exists());
        // read-only directory: save fails soft
        File ro = tmp(); PlaceService r = svc(ro, env); ro.setWritable(false);
        boolean root = new File("/proc/self").exists() && System.getProperty("user.name").equals("root");
        if (!root) { Place.Type t = Place.Type.OTHER; r.save(a, "Mike", "X", t, "world", 0, 0, 0, false, false); check("unwritable folder warns, no crash", !warns.isEmpty()); }
        else pass++;
        ro.setWritable(true);
    }

    // ---------- 4, 5, 8, 9(part), 12, 13 Java panel + chat ----------
    static Controller ctl(PlaceService s, FE e, Forms f) { return new Controller(s, e, f); }
    static void controllerJava() throws Exception {
        FE env = new FE(); PlaceService s = svc(tmp(), env); Controller c = ctl(s, env, new FF());
        FP mike = new FP("Marvin", false), kay = new FP(".KayBed", true);
        env.join(mike, kay);
        mike.at("world", 100, 100);
        c.command(mike, new String[]{});
        check("java opens chat panel", mike.panels.size() == 1 && mike.panels.get(0).plain().contains("My Places (0 of 5 saved)"));
        check("java main has save button text", mike.panels.get(0).plain().contains("[Save this spot here]"));
        c.command(mike, new String[]{"savetype", "house"});
        check("java asks for name", mike.saidAny("Type a name for this spot in chat within 30 seconds"));
        check("java waiting flag", c.waitingForName(mike.id));
        boolean handled = c.chatInput(mike, "My House");
        check("java chat input saved", handled && mike.saidAny("Saved \"My House\" here (x 100, y 64, z 100)"));
        check("java saved place is house type", find(s, mike.id, "My House").type == Place.Type.HOUSE);
        check("java saved is private by default", !find(s, mike.id, "My House").shareable);
        check("not waiting after save", !c.waitingForName(mike.id));
        check("chat when not waiting is ignored", !c.chatInput(mike, "hello everyone"));
        c.command(mike, new String[]{"save", "Farm", "North"});
        check("java /places save <name>", find(s, mike.id, "Farm North") != null);
        c.command(mike, new String[]{"save", "farm", "north"});
        check("java duplicate message", mike.last().contains("already have a place named"));
        c.command(mike, new String[]{"save", "&c"});
        check("java empty name message", mike.last().contains("nothing usable"));
        // typed 'cancel'
        c.command(mike, new String[]{"save"});
        check("save with no name starts chat input", c.waitingForName(mike.id));
        check("cancel typed", c.chatInput(mike, "cancel") && mike.last().contains("nothing saved") && !c.waitingForName(mike.id));
        // expiry of chat input
        c.command(mike, new String[]{"save"}); env.now += 31_000;
        check("chat input expires after 30s", !c.waitingForName(mike.id) && !c.chatInput(mike, "Late Name"));
        // mine list
        mike.panels.clear(); c.command(mike, new String[]{"mine"});
        String mine = mike.panels.get(0).plain();
        check("mine lists with phrase", mine.contains("House: My House (") && mine.contains("Other: Farm North ("));
        check("mine has plain-word buttons", mine.contains("[Compass]") && mine.contains("[Rename]") && mine.contains("[Delete]") && mine.contains("[Details]"));
        Place house = find(s, mike.id, "My House");
        // compass same world: 100,100 -> save at same spot, move away
        mike.at("world", 100, 300);
        c.command(mike, new String[]{"compass", house.id});
        check("compass sets target", "100.0,100.0".equals(mike.compass));
        check("compass says direction and no move", mike.last().contains("200 blocks north") && mike.last().contains("does not move you"));
        check("compass action bar arrow", mike.bars.size() == 1 && mike.bars.get(0).contains("\u2191") && mike.bars.get(0).contains("My House"));
        // 8: other world
        mike.compass = null; mike.at("world_nether", 0, 0);
        c.command(mike, new String[]{"compass", house.id});
        check("compass other world: no arrow", mike.compass == null && mike.last().contains("the Overworld") && mike.last().contains("same world"));
        // 7: world removed
        env.worlds.remove("world");
        c.command(mike, new String[]{"compass", house.id});
        check("world missing: compass explains", mike.last().contains("missing"));
        mike.panels.clear(); c.command(mike, new String[]{"view", house.id});
        check("world missing: view works", mike.panels.get(0).plain().contains("a world that is missing right now") && mike.panels.get(0).plain().contains("world missing"));
        mike.panels.clear(); c.command(mike, new String[]{"mine"});
        check("world missing: list works", mike.panels.get(0).plain().contains("(world missing)"));
        c.command(mike, new String[]{"delete", house.id});
        check("world missing: delete allowed (asks first)", mike.panels.get(mike.panels.size() - 1).plain().contains("Delete \"My House\"?"));
        env.worlds.put("world", "NORMAL");
        // delete confirm flow
        mike.panels.clear();
        c.command(mike, new String[]{"delete", house.id});
        Panel dp = mike.panels.get(0);
        check("delete asks first, place still there", find(s, mike.id, "My House") != null);
        check("delete buttons are explicit", dp.plain().contains("[Yes, delete My House]") && dp.plain().contains("[No, keep it]"));
        String yes = null; for (Panel.Line l : dp.lines) for (Panel.Seg sg : l.segs) if (sg.text.startsWith("[Yes")) yes = sg.run;
        String tok = yes.substring(yes.lastIndexOf(' ') + 1);
        c.command(mike, new String[]{"confirmdelete", tok});
        check("delete confirm deletes", find(s, mike.id, "My House") == null && mike.last().contains("Deleted \"My House\""));
        c.command(mike, new String[]{"confirmdelete", tok});
        check("double tap on Yes: second does nothing", mike.last().contains("expired or was already used"));
        // expiry
        Place farm = find(s, mike.id, "Farm North");
        mike.panels.clear(); c.command(mike, new String[]{"delete", farm.id});
        String y2 = null; for (Panel.Line l : mike.panels.get(0).lines) for (Panel.Seg sg : l.segs) if (sg.text.startsWith("[Yes")) y2 = sg.run;
        env.now += 31_000; c.command(mike, new String[]{"confirmdelete", y2.substring(y2.lastIndexOf(' ') + 1)});
        check("delete confirm expires at 30s", find(s, mike.id, "Farm North") != null);
        check("fake token refused", doDel(c, mike, farm, "BOGUS") && find(s, mike.id, "Farm North") != null);
        // someone else's place cannot be deleted via id
        kay.at("world", 0, 0);
        c.command(kay, new String[]{"delete", farm.id});
        check("delete someone else's place: not found", kay.last().contains("can't find that place") && find(s, mike.id, "Farm North") != null);
        c.command(kay, new String[]{"confirmdelete", "anything"});
        check("kay confirm without request does nothing", find(s, mike.id, "Farm North") != null);
        c.command(kay, new String[]{"rename", farm.id, "Hacked"});
        check("rename someone else's: refused", farm.name.equals("Farm North"));
        c.command(kay, new String[]{"view", farm.id});
        check("view someone else's private place: refused", kay.last().contains("can't find that place"));
        c.command(kay, new String[]{"compass", farm.id});
        check("compass to private place: refused", kay.compass == null);
        // 5: sharing and family list
        mike.panels.clear(); kay.panels.clear();
        c.command(mike, new String[]{"share", farm.id, "on"});
        check("share message", mike.last().contains("shown to family"));
        c.command(kay, new String[]{"family"});
        check("family sees shared place with owner name", kay.panels.get(0).plain().contains("Marvin: Farm North ("));
        check("family list no visit button until owner allows", !kay.panels.get(0).plain().contains("to visit"));
        c.command(mike, new String[]{"visits", farm.id, "on"});
        kay.panels.clear(); c.command(kay, new String[]{"family"});
        check("visit button names the owner", kay.panels.get(0).plain().contains("[Ask Marvin to visit]"));
        check("family compass works", doCompass(c, kay, farm));
        c.command(mike, new String[]{"share", farm.id, "off"});
        kay.panels.clear(); c.command(kay, new String[]{"family"});
        check("hide again: family list empty", kay.panels.get(0).plain().contains("No family places are shared yet"));
        // java views bedrock's shared place
        kay.at("world", 5, 5);
        c.command(kay, new String[]{"save", "Cabin"});
        Place cabin = find(s, kay.id, "Cabin");
        c.command(kay, new String[]{"share", cabin.id, "on"});
        mike.panels.clear(); c.command(mike, new String[]{"family"});
        check("java sees bedrock owner without dot prefix", mike.panels.get(0).plain().contains("KayBed: Cabin") && !mike.panels.get(0).plain().contains(".KayBed"));
        // rename via command
        c.command(mike, new String[]{"rename", farm.id, "South", "Barn"});
        check("rename via command", find(s, mike.id, "South Barn") != null);
        c.command(mike, new String[]{"rename", farm.id});
        check("rename without name explains", mike.last().contains("Type the new name"));
        // view panel for own
        mike.panels.clear(); c.command(mike, new String[]{"view", farm.id});
        String vp = mike.panels.get(0).plain();
        check("view own shows buttons", vp.contains("[Compass]") && vp.contains("[Show to family]") && vp.contains("[Let family ask to visit me here]") && vp.contains("[Back]"));
        check("view own shows where in plain words", vp.contains("Where: the Overworld, x 100, y 64, z 100"));
        // limit message in java chat flow
        for (int i = 1; i <= 5; i++) c.command(kay, new String[]{"save", "K" + i});
        c.command(kay, new String[]{"savetype", "farm"});
        check("at limit: chat flow refuses before asking", kay.last().contains("already have 5 saved places") && !c.waitingForName(kay.id));
        // quitting clears waiting
        c.command(mike, new String[]{"save"}); c.playerLeft(mike.id);
        check("quit clears waiting", !c.waitingForName(mike.id));
        // unknown subcommand
        c.command(mike, new String[]{"wat"});
        check("unknown subcommand shows help", mike.last().contains("/places"));
        // Java commands for a player not in a world with place from nether
        FP nk = new FP("Nether", false).at("world_nether", 10, 10); env.join(nk);
        c.command(nk, new String[]{"save", "Fortress"});
        check("save in nether", find(s, nk.id, "Fortress") != null && find(s, nk.id, "Fortress").world.equals("world_nether"));
        mike.panels.clear(); nk.compass = null; nk.at("world", 0, 0); c.command(nk, new String[]{"compass", find(s, nk.id, "Fortress").id});
        check("nether place from overworld: names the Nether", nk.last().contains("the Nether") && nk.compass == null);
        // guild world
        FP g = new FP("Guild", false).at("em_adventurers_guild", 1, 1); env.join(g);
        c.command(g, new String[]{"save", "Hall"});
        check("guild world refused with plain message", g.last().contains("can't be saved in this world") && s.mine(g.id).isEmpty());
        // Bedrock player using java chat fallback
        kay.panels.clear(); c.command(kay, new String[]{"chat"});
        check("bedrock player can force chat panel", kay.panels.size() == 1);
    }
    static boolean doDel(Controller c, FP p, Place pl, String token) { c.command(p, new String[]{"delete", pl.id}); c.command(p, new String[]{"confirmdelete", token}); return true; }
    static boolean doCompass(Controller c, FP p, Place pl) { p.compass = null; c.command(p, new String[]{"compass", pl.id}); return p.compass != null; }

    // ---------- 4, 14, 17 Bedrock ----------
    static void controllerBedrock() throws Exception {
        FE env = new FE(); PlaceService s = svc(tmp(), env); FF f = new FF(); Controller c = ctl(s, env, f);
        FP mike = new FP(".MikeExcel", true), marv = new FP("Marvin", false); env.join(mike, marv);
        mike.at("world", 0, 0);
        c.command(mike, new String[]{});
        check("bedrock opens form", f.kind.equals("simple") && f.title.equals("My Places") && f.sent == 1);
        check("bedrock empty main buttons", f.buttons.equals(List.of("Save this spot here", "Family places (0)", "Close menu")));
        f.simple.accept(0);
        check("bedrock save form", f.kind.equals("custom") && f.title.equals("Save this spot") && f.fields.size() == 4);
        check("bedrock save form labels explain", f.fields.get(0).label.contains("Name for this spot (example: My House)")
                && f.fields.get(1).label.contains("What kind of place") && f.fields.get(2).label.contains("Show this place to family") && f.fields.get(3).label.contains("ask to visit me here"));
        check("bedrock save defaults private", !f.fields.get(2).defBool && !f.fields.get(3).defBool);
        check("bedrock type choices", f.fields.get(1).options.equals(List.of("House", "Farm", "Mine", "Other")));
        f.custom.accept(Arrays.asList("My House", 0, false, false));
        check("bedrock saved", find(s, mike.id, "My House") != null && mike.last().contains("Saved \"My House\""));
        check("bedrock saved msg is plain text not panel", mike.panels.isEmpty());
        check("bedrock owner stored without dot", find(s, mike.id, "My House").ownerName.equals("MikeExcel"));
        // close form saves nothing
        c.command(mike, new String[]{}); f.simple.accept(0); f.custom.accept(null);
        check("closing save form saves nothing", s.mine(mike.id).size() == 1);
        // dropdown out of range clamps, odd name
        c.command(mike, new String[]{}); f.simple.accept(0); f.custom.accept(Arrays.asList("\u00a7cBarn\u00a7r", 99, true, true));
        Place barn = find(s, mike.id, "Barn");
        check("bedrock odd dropdown clamps, name sanitized", barn != null && barn.type == Place.Type.OTHER);
        check("bedrock toggles applied", barn.shareable && barn.allowVisit);
        c.command(mike, new String[]{}); f.simple.accept(0); f.custom.accept(Arrays.asList("", 0, false, false));
        check("bedrock empty name message", mike.last().contains("nothing usable"));
        c.command(mike, new String[]{}); f.simple.accept(0); f.custom.accept(Arrays.asList("my house", 1, false, false));
        check("bedrock duplicate message", mike.last().contains("already have a place named"));
        // main list shows places with short labels
        c.command(mike, new String[]{});
        check("bedrock main lists places", f.buttons.get(1).startsWith("House: My House (") && f.buttons.get(2).startsWith("Other: Barn ("));
        // 14 phone length: longest button with max-length name
        s.rename(mike.id, barn.id, "ABCDEFGHIJKLMNOPQRSTUVWX");
        mike.at("world", 9999, -9999); c.command(mike, new String[]{});
        int longest = 0; for (String b : f.buttons) longest = Math.max(longest, b.length());
        check("bedrock list buttons <= 42 chars (longest " + longest + ")", longest <= 42);
        System.out.println("INFO longest bedrock list button: " + longest + " chars");
        // detail
        f.simple.accept(2);
        check("bedrock place detail", f.title.equals("My House") || f.title.startsWith("ABCDEF"));
        int dl = 0; for (String b : f.buttons) dl = Math.max(dl, b.length());
        check("bedrock detail buttons <= 34 chars (longest " + dl + ")", dl <= 34);
        System.out.println("INFO longest bedrock detail button: " + dl + " chars");
        check("bedrock detail buttons are plain words", f.buttons.equals(List.of("Show direction (compass)", "Rename this place", "Hide from family", "Turn visit requests off", "Delete this place (asks first)", "Back to My Places")));
        // compass from detail
        f.simple.accept(0);
        check("bedrock compass", mike.compass != null && mike.last().contains("This does not move you"));
        // rename
        c.command(mike, new String[]{}); f.simple.accept(2); f.simple.accept(1);
        check("bedrock rename form prefilled", f.kind.equals("custom") && f.fields.get(0).def.equals("ABCDEFGHIJKLMNOPQRSTUVWX"));
        f.custom.accept(List.of("Lake House"));
        check("bedrock renamed", find(s, mike.id, "Lake House") != null);
        f.custom = null;
        // delete with modal
        c.command(mike, new String[]{}); f.simple.accept(1); f.simple.accept(4);
        check("bedrock delete is a 2-button confirm", f.kind.equals("modal") && f.buttons.get(0).startsWith("Yes, delete ") && f.buttons.get(1).equals("No, keep it"));
        int before = s.mine(mike.id).size(); f.modal.accept(false);
        check("bedrock delete No keeps it", s.mine(mike.id).size() == before);
        f.simple.accept(4); f.modal.accept(true);
        check("bedrock delete Yes deletes", s.mine(mike.id).size() == before - 1);
        // double tap Yes on the same modal: second is harmless
        f.modal.accept(true);
        check("bedrock double tap Yes harmless", s.mine(mike.id).size() == before - 1 && mike.last().contains("already gone"));
        // closing detail
        c.command(mike, new String[]{}); f.simple.accept(-1);
        // stale button: place was deleted by admin while form open
        c.command(mike, new String[]{}); String idStale = s.mine(mike.id).get(0).id; s.adminDelete(idStale); f.simple.accept(1);
        check("stale form tap on deleted place: friendly message", mike.last().contains("can't find that place"));
        // family forms
        Place lake = s.mine(mike.id).isEmpty() ? null : s.mine(mike.id).get(0);
        s.save(marv.id, "Marvin", "Mine Shaft", Place.Type.MINE, "world", 50, 12, 50, true, true);
        s.save(marv.id, "Marvin", "Secret", Place.Type.OTHER, "world", 5, 5, 5, false, false);
        c.command(mike, new String[]{});
        check("bedrock family count only shared", f.buttons.contains("Family places (1)"));
        f.simple.accept(f.buttons.size() - 2);
        check("bedrock family list", f.title.equals("Family places") && f.buttons.equals(List.of("Marvin: Mine Shaft", "Back to My Places")));
        f.simple.accept(0);
        check("bedrock family place buttons", f.title.equals("Marvin: Mine Shaft") && f.buttons.equals(List.of("Show direction (compass)", "Ask Marvin to visit (they must accept)", "Back")));
        f.simple.accept(1);
        check("bedrock ask visit runs only /tpa", marv.cmds.isEmpty() && mike.cmds.equals(List.of("tpa Marvin")));
        check("bedrock ask visit tells nothing happens until accept", mike.last().contains("Nothing happens unless Marvin accepts"));
        // Bedrock player's shared place seen by Java
        s.save(mike.id, "MikeExcel", "Mike Base", Place.Type.HOUSE, "world", 3, 64, 3, true, false);
        s.setShare(mike.id, find(s, mike.id, "Mike Base").id, true);
        marv.panels.clear(); c.command(marv, new String[]{"family"});
        check("java sees bedrock shared place", marv.panels.get(0).plain().contains("MikeExcel:"));
        // 17 forms refused / no Geyser: fall back to chat
        f.accept = false; mike.panels.clear(); c.command(mike, new String[]{});
        check("form failure falls back to chat panel", mike.panels.size() == 1);
        Controller noForms = ctl(s, env, null); mike.panels.clear(); noForms.command(mike, new String[]{});
        check("no Geyser (forms null): chat panel", mike.panels.size() == 1);
        f.accept = false; c.command(mike, new String[]{"savetype", "farm"});
        FP full = new FP(".Full", true); env.join(full); f.accept = true; for (int i = 1; i <= 5; i++) s.save(full.id, "Full", "F" + i, Place.Type.OTHER, "world", 0, 0, 0, false, false);
        int sent = f.sent; c.command(full, new String[]{}); f.simple.accept(0);
        check("bedrock at limit: plain message, no form", full.last().contains("already have 5 saved places") && f.sent == sent + 1);
    }

    // ---------- 9, 10 visits ----------
    static void visits() throws Exception {
        FE env = new FE(); PlaceService s = svc(tmp(), env); Controller c = ctl(s, env, new FF());
        FP owner = new FP("Kay", false), vis = new FP("Marvin", false); env.join(owner, vis);
        s.save(owner.id, "Kay", "Cabin", Place.Type.HOUSE, "world", 10, 64, 10, true, true);
        s.save(owner.id, "Kay", "Closed", Place.Type.HOUSE, "world", 20, 64, 20, true, false);
        s.save(owner.id, "Kay", "Private", Place.Type.HOUSE, "world", 30, 64, 30, false, false);
        Place cabin = find(s, owner.id, "Cabin"), closed = find(s, owner.id, "Closed"), priv = find(s, owner.id, "Private");
        c.command(vis, new String[]{"visit", closed.id});
        check("visits off: refused, no command", vis.cmds.isEmpty() && vis.last().contains("has not turned on visit requests"));
        c.command(vis, new String[]{"visit", priv.id});
        check("not shared: refused as not found", vis.cmds.isEmpty() && vis.last().contains("can't find that place"));
        c.command(vis, new String[]{"visit", "nope"});
        check("bad id: refused", vis.cmds.isEmpty());
        env.online.remove(owner.id);
        c.command(vis, new String[]{"visit", cabin.id});
        check("owner offline: refused", vis.cmds.isEmpty() && vis.last().contains("not online right now"));
        env.join(owner); env.tpa = false;
        c.command(vis, new String[]{"visit", cabin.id});
        check("no Essentials: refused plainly", vis.cmds.isEmpty() && vis.last().contains("not available"));
        env.tpa = true;
        c.command(owner, new String[]{"visit", cabin.id});
        check("visit own place: refused", owner.cmds.isEmpty() && owner.last().contains("your own place"));
        c.command(vis, new String[]{"visit", cabin.id});
        check("visit ok: exactly one /tpa to the owner", vis.cmds.equals(List.of("tpa Kay")));
        check("visit ok: owner not touched", owner.cmds.isEmpty());
        check("visit ok: no position change", vis.world.equals("world") && vis.x == 0 && vis.z == 0);
        c.command(vis, new String[]{"visit", cabin.id});
        check("visit cooldown", vis.cmds.size() == 1 && vis.last().contains("Wait a little"));
        env.now += 31_000;
        c.command(vis, new String[]{"visit", cabin.id});
        check("visit after cooldown", vis.cmds.size() == 2);
        // different worlds: allowed (Essentials handles) but request only
        vis.at("world_nether", 0, 0); env.now += 31_000;
        c.command(vis, new String[]{"visit", cabin.id});
        check("visit from other world still just a request", vis.cmds.size() == 3 && vis.cmds.get(2).equals("tpa Kay"));
        // owner hides place while request pending => later asks refused
        s.setShare(owner.id, cabin.id, false); env.now += 31_000;
        c.command(vis, new String[]{"visit", cabin.id});
        check("owner hid place: new request refused", vis.cmds.size() == 3);
        // floodgate owner name used for /tpa
        FP bk = new FP(".KayBed", true); env.join(bk);
        s.save(bk.id, "KayBed", "Bed House", Place.Type.HOUSE, "world", 1, 1, 1, true, true);
        env.now += 31_000; c.command(vis, new String[]{"visit", find(s, bk.id, "Bed House").id});
        check("bedrock owner: /tpa uses real login name", vis.cmds.get(vis.cmds.size() - 1).equals("tpa .KayBed"));
        check("bedrock owner: shown without dot", vis.last().contains("Asked KayBed"));
        // visitor dies/moves/quits: nothing queued on our side
        c.playerLeft(vis.id);
    }

    // ---------- Cumulus forms really build (test uses real floodgate.jar classes) ----------
    static void formsReal() throws Exception {
        File fg = new File("/workspace/mc/test26/plugins/floodgate.jar");
        if (!fg.exists()) { System.out.println("SKIP formsReal (floodgate jar missing)"); return; }
        ClassLoader l = new java.net.URLClassLoader(new java.net.URL[]{fg.toURI().toURL(),
                new File("/workspace/wrv015/scratch/libraries").toURI().toURL()}, WaypointsTest.class.getClassLoader());
        // gson is needed by Cumulus; find it
        List<java.net.URL> urls = new ArrayList<>(); urls.add(fg.toURI().toURL());
        try (var st = Files.walk(new File("/workspace/wrv015/scratch/libraries/com/google").toPath())) {
            st.filter(p -> p.toString().endsWith(".jar")).forEach(p -> { try { urls.add(p.toUri().toURL()); } catch (Exception ignored) { } });
        }
        l = new java.net.URLClassLoader(urls.toArray(new java.net.URL[0]), WaypointsTest.class.getClassLoader());
        Object simple = GeyserForms.buildSimple(l, "My Places", "body", List.of("A", "B", "C"), i -> { });
        check("cumulus simple form builds", simple != null && simple.getClass().getName().contains("SimpleForm"));
        Object modal = GeyserForms.buildModal(l, "Delete?", "sure", "Yes, delete X", "No, keep it", b -> { });
        check("cumulus modal form builds", modal != null && modal.getClass().getName().contains("ModalForm"));
        Object custom = GeyserForms.buildCustom(l, "Save this spot", List.of(Forms.Field.input("Name", "My House", ""),
                Forms.Field.dropdown("Kind", List.of("House", "Farm"), 0), Forms.Field.toggle("Share", false)), v -> { });
        check("cumulus custom form builds", custom != null && custom.getClass().getName().contains("CustomForm"));
        // Replay what the phone sends back, through Cumulus's own response parser and our handlers
        int[] got = {-5}; Boolean[] mod = {null}; List<Object>[] cus = new List[]{null}; boolean[] cusCalled = {false};
        Object sf = GeyserForms.buildSimple(l, "t", "c", List.of("x", "y", "z"), i -> got[0] = i);
        raw(sf, "2"); check("simple: tap button 3 gives index 2", got[0] == 2);
        raw(sf, "0"); check("simple: tap button 1 gives index 0", got[0] == 0);
        got[0] = -5; raw(sf, "null"); check("simple: closing form gives -1", got[0] == -1);
        Object mf = GeyserForms.buildModal(l, "t", "c", "Yes", "No", b -> mod[0] = b);
        raw(mf, "true"); check("modal: first button is true", Boolean.TRUE.equals(mod[0]));
        raw(mf, "false"); check("modal: second button is false", Boolean.FALSE.equals(mod[0]));
        mod[0] = null; raw(mf, "null"); check("modal: close counts as No", Boolean.FALSE.equals(mod[0]));
        Object cf = GeyserForms.buildCustom(l, "t", List.of(Forms.Field.input("Name", "h", ""), Forms.Field.dropdown("Kind", List.of("House", "Farm", "Mine", "Other"), 0),
                Forms.Field.toggle("Share", false), Forms.Field.toggle("Visit", false)), v -> { cus[0] = v; cusCalled[0] = true; });
        raw(cf, "[\"My House\",2,true,false]");
        check("custom: values arrive in order and typed", cus[0] != null && cus[0].equals(Arrays.asList("My House", 2, true, false)));
        raw(cf, "[null,0,false,false]");
        check("custom: empty input arrives as empty string or is handled", cus[0] == null || "".equals(cus[0].get(0)));
        cus[0] = List.of("stale"); cusCalled[0] = false; raw(cf, "null");
        check("custom: close gives null", cusCalled[0] && cus[0] == null);
        cus[0] = List.of("stale"); cusCalled[0] = false; raw(cf, "[1,2]");
        check("custom: garbage reply is treated as closed, no crash", cusCalled[0] && cus[0] == null);
    }
    /** Same path Geyser uses: Cumulus codec parses the client's JSON reply, then the form's result handler runs. */
    static void raw(Object form, String json) throws Exception {
        ClassLoader l = form.getClass().getClassLoader();
        Class<?> defs = Class.forName("org.geysermc.cumulus.form.impl.FormDefinitions", true, l);
        Class<?> formI = Class.forName("org.geysermc.cumulus.form.Form", true, l);
        Object inst = defs.getMethod("instance").invoke(null);
        Object codec = defs.getMethod("codecFor", formI).invoke(inst, form);
        Object result = null;
        for (var m : codec.getClass().getMethods()) if (m.getName().equals("deserializeFormResponse") && m.getParameterCount() == 2 && !m.isBridge()) { m.setAccessible(true); result = m.invoke(codec, form, json); }
        if (result == null) throw new IllegalStateException("no result");
        for (var m : form.getClass().getMethods()) if (m.getName().equals("callResultHandler")) { m.invoke(form, result); return; }
        throw new IllegalStateException("callResultHandler not found");
    }

    // ---------- 10: no teleport code path ----------
    static void noTeleport() throws Exception {
        File src = new File(System.getProperty("src", "src"));
        int[] files = {0};
        List<String> offenders = new ArrayList<>();
        try (var st = Files.walk(src.toPath())) {
            st.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                files[0]++;
                try {
                    String t = Files.readString(p).replaceAll("//.*", "").replaceAll("(?s)/\\*.*?\\*/", "");
                    for (String bad : new String[]{".teleport(", "teleportAsync(", "setLocation(", "dispatchCommand(", "\"tp ", "\"tpa ", "\"tpaccept", "\"home", "\"sethome", "\"spawn"}) {
                        if (t.contains(bad) && !(p.toString().endsWith("Controller.java") && bad.equals("\"tpa "))) offenders.add(p.getFileName() + ":" + bad);
                    }
                } catch (Exception e) { offenders.add("read " + p); }
            });
        }
        check("source scan: " + files[0] + " files, no teleport call (offenders " + offenders + ")", files[0] >= 10 && offenders.isEmpty());
        // the only command string the controller builds is "tpa <name>" and it only runs through PlayerView.runCommand
        String ctl = Files.readString(new File(src, "com/winkreyes/excelwaypoints/Controller.java").toPath());
        int count = ctl.split("runCommand\\(", -1).length - 1;
        check("controller has exactly one runCommand call", count == 1);
        check("that call is the tpa request", ctl.contains("p.runCommand(\"tpa \" + owner.realName())"));
        String ew = Files.readString(new File(src, "com/winkreyes/excelwaypoints/BukkitPlayer.java").toPath());
        check("runCommand only wraps performCommand (as the player, normal permissions)", ew.contains("p.performCommand(cmd)") && !ew.contains("dispatchCommand"));
        check("compass uses setCompassTarget only", ew.contains("setCompassTarget") && !ew.contains("teleport"));
    }

    // ---------- 18 perf ----------
    static void perf() throws Exception {
        FE env = new FE(); File dir = tmp(); PlaceStore st = new PlaceStore(new File(dir, "places.yml"), warns::add);
        PlaceService s = new PlaceService(st, 5, List.of(), 0, env::now);
        for (int i = 0; i < 100; i++) st.add(new Place(st.newId(), UUID.randomUUID(), "P" + i, "Place " + i, Place.Type.HOUSE, "world", i, 64, i, i % 2 == 0, false, i));
        long t0 = System.nanoTime(); st.save(); long saveMs = (System.nanoTime() - t0) / 1_000_000;
        PlaceStore st2 = new PlaceStore(new File(dir, "places.yml"), warns::add); t0 = System.nanoTime(); st2.load(); long loadMs = (System.nanoTime() - t0) / 1_000_000;
        check("100 places load back (" + loadMs + " ms)", st2.all().size() == 100);
        t0 = System.nanoTime(); for (int i = 0; i < 1000; i++) s.family(UUID.randomUUID()); long famMs = (System.nanoTime() - t0) / 1_000_000;
        System.out.println("INFO 100 places: save " + saveMs + " ms, load " + loadMs + " ms, 1000 family lookups " + famMs + " ms, file " + new File(dir, "places.yml").length() + " bytes");
        check("100 places: save under 1s", saveMs < 1000);
        check("1000 family lookups under 1s", famMs < 1000);
        Set<String> ids = new HashSet<>(); for (Place p : st2.all()) ids.add(p.id);
        check("ids unique and 6 chars", ids.size() == 100 && ids.stream().allMatch(x -> x.length() == 6));
        File srcDir = new File(System.getProperty("src", "src"));
        try (var w = Files.walk(srcDir.toPath())) {
            check("no per-tick tasks or chunk loading in source", w.filter(p -> p.toString().endsWith(".java")).noneMatch(p -> {
                try { String t = Files.readString(p); return t.contains("runTaskTimer") || t.contains("scheduleSyncRepeatingTask") || t.contains("getChunkAt") || t.contains("loadChunk"); } catch (Exception e) { return true; }
            }));
        }
    }

    // ---------- metadata ----------
    static void metadata() throws Exception {
        String y = Files.readString(new File(System.getProperty("plugin")).toPath());
        check("plugin.yml name and two-part-style version", y.contains("name: ExcelWaypoints") && y.contains("version: 0.1.0"));
        check("plugin.yml main", y.contains("main: com.winkreyes.excelwaypoints.ExcelWaypoints"));
        check("plugin.yml only soft dependencies", y.contains("softdepend: [Essentials, Geyser-Spigot, floodgate]") && !y.matches("(?s).*\\ndepend:.*"));
        check("plugin.yml plain-word dependency notes", y.contains("Plain-word dependencies") && y.contains("owner must accept"));
        check("plugin.yml use perm default true, admin op", y.contains("default: true") && y.contains("default: op"));
        String cfg = Files.readString(new File(System.getProperty("cfg")).toPath());
        check("config guild world blocked", cfg.contains("em_adventurers_guild") && cfg.contains("max-places-per-player: 5"));
        String all = ""; try (var w = Files.walk(new File(System.getProperty("src")).toPath())) { for (var p : (Iterable<java.nio.file.Path>) w::iterator) if (p.toString().endsWith(".java")) all += Files.readString(p); }
        check("no ip, password, token, key strings in source", !all.matches("(?s).*\\b\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\b.*") && !all.toLowerCase().contains("password") && !all.toLowerCase().contains("token=") && !all.toLowerCase().contains("api_key"));
        check("no em dashes in player-facing text", !all.contains("\u2014"));
    }
}
