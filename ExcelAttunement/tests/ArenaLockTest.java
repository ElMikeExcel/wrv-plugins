import com.winkreyes.excelattunement.core.ArenaLock;
import com.winkreyes.excelattunement.core.ArenaLock.DragonState;
import com.winkreyes.excelattunement.core.ArenaLock.Rule;
import com.winkreyes.excelattunement.core.ArenaLock.Zone;
import com.winkreyes.excelattunement.core.WardNotice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** 0.1.6 arena locks: movement spells sealed in the End main island zone while a dragon lives. */
public class ArenaLockTest {
    static final Zone Z = Zone.defaults();
    static final List<Rule> D = ArenaLock.defaults();
    static final String END = "world_the_end", ENV = "THE_END";

    static void run() throws Exception {
        System.out.println("== ArenaLockTest ==");
        keys();
        zone();
        dragonState();
        casts();
        liftTargets();
        stopOnEntry();
        rulesAndMessages();
        notices();
        configAndSource();
    }

    static Rule rule(String id) {
        for (Rule r : D) if (r.id().equals(id)) return r;
        return null;
    }

    static void keys() {
        Check.eq("glide", ArenaLock.baseKey(" Glide|2 "), "base key trims, lower-cases, drops |level");
        Check.eq("", ArenaLock.baseKey(null), "base key null -> empty");
        Check.ok(ArenaLock.spellMatches(rule("fling"), "fling|4"), "fling|4 (the leap alias) matches fling");
        for (int lvl = 1; lvl <= 5; lvl++) Check.ok(ArenaLock.spellMatches(rule("fling"), "fling|" + lvl), "fling|" + lvl + " matches");
        Check.ok(ArenaLock.spellMatches(rule("hulkleap"), "HulkLeap|3"), "hulkleap any case/level");
        Check.ok(!ArenaLock.spellMatches(rule("glide"), "superglide"), "no substring match");
        Check.ok(!ArenaLock.spellMatches(null, "glide"), "null rule matches nothing");
    }

    static void zone() {
        Check.eq(200.0, ArenaLock.DEFAULT_RADIUS, "default radius 200");
        Check.ok(Z.contains(END, ENV, 0, 0), "centre in zone");
        Check.ok(Z.contains(END, ENV, 100, 0), "arrival platform 100,49,0 in zone");
        Check.ok(Z.contains(END, ENV, 42, 0) && Z.contains(END, ENV, 0, -42) && Z.contains(END, ENV, -29.7, 29.7), "pillar ring (r=42) in zone");
        Check.ok(Z.contains(END, ENV, 100, 100) && Z.contains(END, ENV, -100, -100), "island edge diagonals (r~141) in zone");
        Check.ok(Z.contains(END, ENV, 200, 0), "exactly on the radius = inside");
        Check.ok(Z.contains(END, ENV, 141, 141), "r~199.4 inside");
        Check.ok(!Z.contains(END, ENV, 142, 142), "r~200.8 outside");
        Check.ok(!Z.contains(END, ENV, 300, 0), "300,0 outside");
        Check.ok(!Z.contains(END, ENV, 1000, 0), "outer islands outside");
        Check.ok(Z.contains("my_custom_end", "the_end", 5, 5), "any End-environment world (env case-insensitive)");
        Check.ok(!Z.contains("world", "NORMAL", 0, 0), "overworld 0,0 not in zone");
        Check.ok(!Z.contains("world_nether", "NETHER", 0, 0), "nether 0,0 not in zone");
        Check.ok(!Z.contains(null, null, 0, 0), "null world not in zone");
        Zone named = new Zone(Set.of("Arena"), Set.of(), 10, -10, 5);
        Check.ok(named.contains("arena", "NORMAL", 13, -13), "named world, offset centre, r=5: 13,-13 inside");
        Check.ok(!named.contains("arena", "NORMAL", 16, -10), "named world r=5: 16,-10 outside");
        Check.ok(!named.contains("world_the_end", "THE_END", 10, -10), "named-only zone ignores env");
        Check.eq(200.0, new Zone(Set.of(), Set.of("THE_END"), 0, 0, -1).radius(), "negative radius -> default");
        Check.eq(200.0, new Zone(Set.of(), Set.of("THE_END"), 0, 0, Double.NaN).radius(), "NaN radius -> default");
        Check.ok(Z.contains(END, ENV, 0, 0) && Z.contains(END, ENV, 100, 0), "height ignored (cylinder): contains takes no y");
    }

    static void dragonState() {
        // signals: entity, linked dragon, respawn ritual, boss bar visible, never won
        Check.eq(DragonState.NONE, ArenaLock.decide(false, false, false, false, false), "all false -> NONE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(true, false, false, false, false), "live dragon entity -> ALIVE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(false, true, false, false, false), "linked dragon -> ALIVE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(false, false, true, false, false), "respawn ritual running (gap) -> ALIVE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(false, false, false, true, false), "boss bar up (unloaded / dying) -> ALIVE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(false, false, false, false, true), "never won (first fight) -> ALIVE");
        Check.eq(DragonState.UNKNOWN, ArenaLock.decide(false, null, false, false, false), "unreadable signal, none live -> UNKNOWN");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(null, true, null, null, null), "a live signal beats unreadable ones");
        Check.eq(DragonState.UNKNOWN, ArenaLock.decide((Boolean[]) null), "null signals -> UNKNOWN");
        Check.eq(DragonState.NONE, ArenaLock.decide(), "no signals -> NONE");
        Check.ok(ArenaLock.dragonHolds(DragonState.ALIVE, false), "ALIVE holds");
        Check.ok(!ArenaLock.dragonHolds(DragonState.NONE, true), "NONE never holds");
        Check.ok(ArenaLock.dragonHolds(DragonState.UNKNOWN, true), "UNKNOWN holds when block-if-unknown (fail safe)");
        Check.ok(!ArenaLock.dragonHolds(DragonState.UNKNOWN, false), "UNKNOWN open when block-if-unknown false");
        Check.ok(ArenaLock.dragonHolds(null, true), "null state = unknown");
        // BossTrial phase flip: same entity, healed, never dead -> every tick stays ALIVE
        for (int t = 0; t < 5; t++) Check.ok(ArenaLock.decide(true, true, false, true, false) == DragonState.ALIVE, "phase flip tick " + t + " stays ALIVE");
        // respawn gap sequence: dead -> ritual -> dragon spawned
        Check.eq(DragonState.NONE, ArenaLock.decide(false, false, false, false, false), "after the kill (animation done) -> NONE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(false, false, true, false, false), "ritual START..SUMMONING_DRAGON -> ALIVE");
        Check.eq(DragonState.ALIVE, ArenaLock.decide(true, true, false, true, false), "new dragon (ritual END) -> ALIVE");
    }

    static void casts() {
        boolean in = true;
        for (String k : new String[]{"glide", "glide|2", "fling", "fling|4", "hulkleap", "hulkleap|2", "lift", "blink", "blink|3"}) {
            Check.ok(ArenaLock.firstBlocking(true, D, k, in, false) != null, "in active zone: '" + k + "' blocked");
            Check.ok(ArenaLock.firstBlocking(true, D, k, false, false) == null, "outside/no dragon: '" + k + "' allowed");
            Check.ok(ArenaLock.firstBlocking(true, D, k, in, true) == null, "bypass: '" + k + "' allowed");
            Check.ok(ArenaLock.firstBlocking(false, D, k, in, false) == null, "disabled: '" + k + "' allowed");
        }
        for (String k : new String[]{"backflip", "recall", "gather", "dash", "evade", "heal", "missile", "levitate", "fly", "", "superglide"}) {
            Check.ok(ArenaLock.firstBlocking(true, D, k, in, false) == null, "open by default in active zone: '" + k + "'");
        }
        Check.eq("glide", ArenaLock.firstBlocking(true, D, "glide", in, false).id(), "glide -> glide rule");
        Check.eq("fling", ArenaLock.firstBlocking(true, D, "fling|4", in, false).id(), "leap (fling|4) -> fling rule");
        Check.ok(ArenaLock.firstBlocking(true, null, "glide", in, false) == null, "null rules -> nothing");
        // spot check: active only with zone AND dragon
        Check.ok(ArenaLock.activeAt(true, Z, END, ENV, 100, 0, DragonState.ALIVE, true), "platform + dragon alive -> active");
        Check.ok(!ArenaLock.activeAt(true, Z, END, ENV, 100, 0, DragonState.NONE, true), "platform + no dragon -> open");
        Check.ok(ArenaLock.activeAt(true, Z, END, ENV, 100, 0, DragonState.UNKNOWN, true), "platform + unknown -> active (fail safe)");
        Check.ok(!ArenaLock.activeAt(true, Z, END, ENV, 300, 0, DragonState.ALIVE, true), "300,0 + dragon alive -> open");
        Check.ok(!ArenaLock.activeAt(true, Z, "world", "NORMAL", 0, 0, DragonState.ALIVE, true), "overworld -> open");
        Check.ok(!ArenaLock.activeAt(false, Z, END, ENV, 0, 0, DragonState.ALIVE, true), "disabled -> open");
        Check.ok(!ArenaLock.activeAt(true, null, END, ENV, 0, 0, DragonState.ALIVE, true), "no zone -> open");
    }

    static void liftTargets() {
        Rule lift = ArenaLock.targetRule(true, D, "lift", false);
        Check.ok(lift != null && lift.id().equals("lift"), "lift protects targets by default");
        Check.ok(ArenaLock.targetRule(true, D, "lift|2", false) != null, "lift levels too");
        for (String k : new String[]{"glide", "fling", "hulkleap", "blink", "heal", "backflip"}) {
            Check.ok(ArenaLock.targetRule(true, D, k, false) == null, "'" + k + "' has no target rule");
        }
        Check.ok(ArenaLock.targetRule(true, D, "lift", true) == null, "bypass caster: no target rule");
        Check.ok(ArenaLock.targetRule(false, D, "lift", false) == null, "disabled: no target rule");
        Check.ok(ArenaLock.blocksTarget(lift, true, false, true), "player target in active zone (caster anywhere) -> blocked");
        Check.ok(!ArenaLock.blocksTarget(lift, true, false, false), "player target outside / no dragon -> allowed");
        Check.ok(!ArenaLock.blocksTarget(lift, false, false, true), "mob target in zone -> allowed (players only)");
        Check.ok(!ArenaLock.blocksTarget(lift, true, true, true), "self -> not a target block");
        Check.ok(!ArenaLock.blocksTarget(null, true, false, true), "no rule -> allowed");
        Check.ok(!ArenaLock.blocksTarget(rule("blink"), true, false, true), "rule without protect-targets -> allowed");
    }

    static void stopOnEntry() {
        Check.eq(Set.of("glide"), ArenaLock.stopOnEntry(true, D, false), "only Glide is stopped on entry by default");
        Check.ok(ArenaLock.stopOnEntry(true, D, true).isEmpty(), "bypass: nothing stopped");
        Check.ok(ArenaLock.stopOnEntry(false, D, false).isEmpty(), "disabled: nothing stopped");
        Check.ok(ArenaLock.stopOnEntry(true, null, false).isEmpty(), "null rules: nothing stopped");
    }

    static void rulesAndMessages() {
        Check.eq(5, D.size(), "five default rules");
        Check.eq(List.of("glide", "fling", "hulkleap", "lift", "blink"), D.stream().map(Rule::id).toList(), "default rule ids, in order");
        for (Rule r : D) {
            String name = Character.toUpperCase(r.id().charAt(0)) + r.id().substring(1);
            Check.eq(name + " is sealed near the dragon while she lives", r.message(), r.id() + " message");
            Check.eq(Set.of(r.id()), r.spells(), r.id() + " spells = [" + r.id() + "]");
            Check.ok(r.usable(), r.id() + " usable");
        }
        Check.ok(rule("glide").stopRunning() && !rule("fling").stopRunning() && !rule("lift").stopRunning(), "stop-running: glide only");
        Check.ok(rule("lift").protectTargets() && !rule("glide").protectTargets(), "protect-targets: lift only");
        Check.eq(List.of("backflip", "recall", "gather", "dash", "evade"), ArenaLock.OPEN_BY_DEFAULT, "open-by-default list");
        Rule dash = ArenaLock.simple("dash");
        Check.eq("Dash is sealed near the dragon while she lives", dash.message(), "one-line add gets the default message");
        Check.ok(ArenaLock.firstBlocking(true, List.of(dash), "dash|2", true, false) != null, "one-line add locks the spell");
        Check.ok(!dash.stopRunning() && !dash.protectTargets(), "one-line add: no extras");
        Check.ok(!new Rule("x", Set.of(), "m", false, false).usable(), "no spells -> unusable");
        Check.ok(ArenaLock.firstBlocking(true, List.of(new Rule("x", Set.of(), "m", false, false)), "x", true, false) == null, "unusable rule never blocks");
        Check.eq(Set.of("leap", "fling"), new Rule("f", Set.of("Leap", "fling|4"), "m", false, false).spells(), "alias list normalized");
        Check.eq("", ArenaLock.defaultMessage(" "), "blank id -> blank message");
        Check.eq("", new Rule("x", Set.of("x"), null, false, false).message(), "null message -> blank (no line)");
    }

    static void notices() {
        WardNotice n = new WardNotice(3);
        Check.ok(n.take(0), "first notice shown");
        Check.ok(!n.take(2999), "within 3s suppressed");
        Check.ok(n.take(3000), "after 3s shown again");
    }

    static void configAndSource() throws Exception {
        String cfg = Files.readString(Path.of("config.yml"));
        Check.ok(cfg.contains("\narena-locks:\n"), "config has arena-locks section");
        Check.ok(!cfg.contains("seal-locks:") && !cfg.contains("until-seal"), "no seal-lock leftovers in config");
        Check.ok(cfg.contains("  enabled: true\n  # Dragon state unreadable"), "enabled toggle");
        Check.ok(cfg.contains("  block-if-dragon-unknown: true"), "unknown dragon = locked by default");
        Check.ok(cfg.contains("  notice-cooldown-seconds: 3"), "3s notice limit");
        Check.ok(cfg.contains("    environments:\n      - THE_END\n    worlds: []"), "zone: THE_END worlds");
        Check.ok(cfg.contains("    center-x: 0\n    center-z: 0") && cfg.contains("    radius: 200\n"), "zone: 0,0 r=200");
        int ri = cfg.indexOf("  rules:\n");
        String rules = cfg.substring(ri);
        for (String id : new String[]{"glide", "fling", "hulkleap", "lift", "blink"}) {
            String name = Character.toUpperCase(id.charAt(0)) + id.substring(1);
            Check.ok(rules.contains("\n    " + id + ":\n      message: \"" + name + " is sealed near the dragon while she lives\""), "config rule " + id + " + message");
        }
        Check.ok(rules.contains("    glide:\n      message: \"Glide is sealed near the dragon while she lives\"\n      stop-running: true"), "config: glide stop-running");
        Check.ok(rules.contains("    lift:\n      message: \"Lift is sealed near the dragon while she lives\"\n      protect-targets-in-zone: true"), "config: lift protects targets");
        Check.eq(1, rules.split("stop-running: true", -1).length - 1, "config: one stop-running rule");
        for (String id : ArenaLock.OPEN_BY_DEFAULT) Check.ok(rules.contains("\n    # " + id + ": {}\n"), "config: one-line commented add for " + id);
        String yml = Files.readString(Path.of("plugin.yml"));
        int i = yml.indexOf("excelattunement.seallock.bypass:");
        Check.ok(i >= 0 && yml.indexOf("default: false", i) > i, "bypass permission kept, not default for ops");
        String src = Files.readString(Path.of("src/com/winkreyes/excelattunement/ArenaLocks.java"));
        Check.ok(src.contains("rules.addAll(ArenaLock.defaults());"), "missing section/rules -> shipped defaults");
        Check.ok(src.contains("c == null || !c.isSet(\"rules\")"), "missing rules key -> defaults (isSet ignores jar defaults)");
        Check.ok(src.contains("getRespawnPhase()") && src.contains("hasBeenPreviouslyKilled()") && src.contains("getBossBar()"), "reads respawn phase, previously-killed, boss bar");
        Check.ok(!src.contains("GuildGate"), "no ExcelGuildGate dependency");
        String main = Files.readString(Path.of("src/com/winkreyes/excelattunement/ExcelAttunement.java"));
        Check.ok(main.contains("getConfig().isSet(\"arena-locks\")"), "section read with isSet (missing -> null -> defaults)");
        String mb = Files.readString(Path.of("src/com/winkreyes/excelattunement/MagicBridge.java"));
        Check.ok(mb.contains("@EventHandler(priority = EventPriority.LOW)\n    public void onArenaLockPreCast"), "PreCast LOW handler");
        Check.ok(mb.contains("event.isCancelled()"), "skips casts already cancelled (ward)");
        Check.ok(mb.contains("event.registerEntityTargetingManager(this)"), "targeting check re-registered on Magic reload");
    }
}
