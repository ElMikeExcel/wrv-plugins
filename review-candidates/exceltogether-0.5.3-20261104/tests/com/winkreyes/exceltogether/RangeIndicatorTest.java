package com.winkreyes.exceltogether;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.*;

/**
 * WRV-068 tests: the range-state machine (debounce/hysteresis, cue toggle), the member count
 * behind the indicator (same near() test as the payout, so count > 0 exactly when the bonus is
 * on in PARTY mode), the line wording, and the /excelparty cue command. Dependency-free runner.
 */
public class RangeIndicatorTest {
    static int pass = 0, fail = 0;
    static void check(String n, boolean ok) { if (ok) pass++; else { fail++; System.out.println("FAIL " + n); } }
    static Object dflt(Class<?> r) {
        if (r == boolean.class) return false; if (r == int.class) return 0; if (r == long.class) return 0L;
        if (r == double.class) return 0.0; if (r == float.class) return 0f; if (r == short.class) return (short) 0;
        if (r == byte.class) return (byte) 0; if (r == char.class) return (char) 0;
        return null;
    }

    // ---------- Bukkit fakes (Proxy, same pattern as PartyTest/ShareTest) ----------
    static final class FW {
        final List<Player> players = new ArrayList<>();
        final World world;
        FW(String name) {
            world = (World) Proxy.newProxyInstance(RangeIndicatorTest.class.getClassLoader(), new Class<?>[]{World.class}, (p, m, a) -> {
                switch (m.getName()) {
                    case "getPlayers": return players;
                    case "getName": return name;
                    case "equals": return p == a[0];
                    case "hashCode": return System.identityHashCode(p);
                    case "toString": return "FW(" + name + ")";
                    default: return dflt(m.getReturnType());
                }
            });
        }
    }
    static final class FP2 implements InvocationHandler {
        final UUID id = UUID.randomUUID(); final String name;
        Location loc; boolean online = true; GameMode mode = GameMode.SURVIVAL;
        final Player player;
        FP2(FW w, String name, double x, double y, double z) {
            this.name = name; this.loc = new Location(w.world, x, y, z);
            this.player = (Player) Proxy.newProxyInstance(RangeIndicatorTest.class.getClassLoader(), new Class<?>[]{Player.class}, this);
        }
        void moveTo(FW w, double x, double y, double z) { loc = new Location(w.world, x, y, z); }
        public Object invoke(Object p, java.lang.reflect.Method m, Object[] a) {
            switch (m.getName()) {
                case "getUniqueId": return id;
                case "isOnline": return online;
                case "getGameMode": return mode;
                case "getLocation": return loc;
                case "getName": return name;
                case "getPlayer": return player;
                case "equals": return p == a[0];
                case "hashCode": return id.hashCode();
                case "toString": return "FP2(" + name + ")";
                default: return dflt(m.getReturnType());
            }
        }
    }

    // ---------- state machine ----------
    static void machineTests() {
        RangeIndicator ind = new RangeIndicator();
        UUID u = UUID.randomUUID();
        // no party -> nothing, ever
        check("no party, in range numbers: nothing", ind.sample(u, false, true, 0) == RangeIndicator.Cue.NONE);
        check("no party again: nothing", ind.sample(u, false, false, 10_000) == RangeIndicator.Cue.NONE);
        // join out of range: silent first sample
        check("join out of range: silent init", ind.sample(u, true, false, 20_000) == RangeIndicator.Cue.NONE);
        // walk in: flip needs the full 3s hold
        check("step in: pending, no cue", ind.sample(u, true, true, 21_000) == RangeIndicator.Cue.NONE);
        check("in for 2.999s: still no cue", ind.sample(u, true, true, 23_999) == RangeIndicator.Cue.NONE);
        check("in for 3s: ON cue", ind.sample(u, true, true, 24_000) == RangeIndicator.Cue.TURNED_ON);
        check("stays in: no repeat cue", ind.sample(u, true, true, 25_000) == RangeIndicator.Cue.NONE);
        // brief dip out at the edge and back: no OFF strobe
        check("dip out: pending", ind.sample(u, true, false, 26_000) == RangeIndicator.Cue.NONE);
        check("back in within window: no OFF", ind.sample(u, true, true, 27_000) == RangeIndicator.Cue.NONE);
        check("still in later: no cue", ind.sample(u, true, true, 31_000) == RangeIndicator.Cue.NONE);
        // sustained out: OFF once
        check("out again: pending", ind.sample(u, true, false, 32_000) == RangeIndicator.Cue.NONE);
        check("out for 2.999s: no cue", ind.sample(u, true, false, 34_999) == RangeIndicator.Cue.NONE);
        check("out for 3s: OFF cue", ind.sample(u, true, false, 35_000) == RangeIndicator.Cue.TURNED_OFF);
        check("stays out: no repeat OFF", ind.sample(u, true, false, 36_000) == RangeIndicator.Cue.NONE);
        // a pending flip is forgotten when the state returns: the window restarts
        UUID w = UUID.randomUUID();
        check("w init out", ind.sample(w, true, false, 0) == RangeIndicator.Cue.NONE);
        check("w dips in: pending", ind.sample(w, true, true, 1_000) == RangeIndicator.Cue.NONE);
        check("w back out: pending cleared", ind.sample(w, true, false, 2_000) == RangeIndicator.Cue.NONE);
        check("w in again: new pending", ind.sample(w, true, true, 3_000) == RangeIndicator.Cue.NONE);
        check("w in 2.999s after restart: no cue", ind.sample(w, true, true, 5_999) == RangeIndicator.Cue.NONE);
        check("w in 3s after restart: ON cue", ind.sample(w, true, true, 6_000) == RangeIndicator.Cue.TURNED_ON);
        // first ever sample already in range (login next to the party): silent
        UUID v = UUID.randomUUID();
        check("first sample in range: silent", ind.sample(v, true, true, 0) == RangeIndicator.Cue.NONE);
        check("second sample in range: still silent", ind.sample(v, true, true, 9_000) == RangeIndicator.Cue.NONE);
        // leaving the party while the bonus is on: no OFF flash, and rejoining starts silent
        check("leave party while on: no OFF", ind.sample(v, false, false, 10_000) == RangeIndicator.Cue.NONE);
        check("rejoin in range: silent init", ind.sample(v, true, true, 11_000) == RangeIndicator.Cue.NONE);
        // hysteresis constant is the spec's ~3 seconds
        check("hysteresis is 3s", RangeIndicator.HYSTERESIS_MS == 3_000L);
    }

    // ---------- cue toggle store ----------
    static void toggleTests() {
        RangeIndicator ind = new RangeIndicator();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        check("cues default on", ind.cuesEnabled(a) && ind.cuesEnabled(b));
        check("setCues off returns off", !ind.setCues(a, false));
        check("off stored", !ind.cuesEnabled(a));
        check("toggle is per player", ind.cuesEnabled(b));
        check("toggle flips back on", ind.toggleCues(a) && ind.cuesEnabled(a));
        ind.setCues(a, false);
        ind.forget(a);
        check("forget resets toggle to default on", ind.cuesEnabled(a));
        check("forget resets state: next in-range sample is silent", ind.sample(a, true, true, 0) == RangeIndicator.Cue.NONE);
    }

    // ---------- line wording ----------
    static void lineTests() {
        check("active line, 1 member", RangeIndicator.activeLine(1, 1.25).equals("Party bonus ACTIVE: 1 in range (x1.25)"));
        check("active line, 3 members", RangeIndicator.activeLine(3, 1.25).equals("Party bonus ACTIVE: 3 in range (x1.25)"));
        check("active line, whole multiplier", RangeIndicator.activeLine(2, 2.0).equals("Party bonus ACTIVE: 2 in range (x2)"));
        check("active line, 1.5 multiplier", RangeIndicator.activeLine(2, 1.5).equals("Party bonus ACTIVE: 2 in range (x1.5)"));
        check("out line", RangeIndicator.OUT_LINE.equals("Party: nobody in range"));
        check("no em dash in indicator lines", !RangeIndicator.activeLine(1, 1.25).contains("\u2014") && !RangeIndicator.OUT_LINE.contains("\u2014"));
    }

    // ---------- member count behind the indicator ----------
    static void countTests() {
        FW w = new FW("world"), w2 = new FW("nether");
        FP2 a = new FP2(w, "Ann", 0, 64, 0), b = new FP2(w, "Bob", 30, 64, 0), c = new FP2(w, "Cat", 200, 64, 0), d = new FP2(w, "Dan", 10, 64, 0);
        w.players.addAll(List.of(a.player, b.player, c.player, d.player));
        Set<UUID> on = new HashSet<>(List.of(a.id, b.id, c.id, d.id));
        PartyManager mgr = new PartyManager(5, 60_000, 300_000, on::contains);
        long t = 1_000;
        mgr.invite(a.id, b.id, t); mgr.accept(b.id, null, t);
        mgr.invite(a.id, c.id, t); mgr.accept(c.id, null, t);
        PartyMemberFinder f = new PartyMemberFinder(new PartyBonus(1.25, 50), mgr, BonusMode.PARTY);
        check("count: one member in range (stranger next door does not count)", f.partyMembersInRange(a.player) == 1);
        check("count: symmetric for the near member", f.partyMembersInRange(b.player) == 1);
        check("count: far member sees nobody", f.partyMembersInRange(c.player) == 0);
        check("count agrees with hasPartner (party mode)", (f.partyMembersInRange(a.player) > 0) == f.hasPartner(a.player)
                && (f.partyMembersInRange(c.player) > 0) == f.hasPartner(c.player));
        // exactly 50 blocks counts (inclusive, same as the payout)
        b.moveTo(w, 30, 104, 0);
        check("count: exactly 50 blocks counts", f.partyMembersInRange(a.player) == 1);
        b.moveTo(w, 50.1, 64, 0);
        check("count: 50.1 blocks does not count", f.partyMembersInRange(a.player) == 0 && !f.hasPartner(a.player));
        // another world never counts
        b.moveTo(w2, 5, 64, 0);
        check("count: member in another world does not count", f.partyMembersInRange(a.player) == 0);
        // spectator member does not count
        b.moveTo(w, 20, 64, 0); b.mode = GameMode.SPECTATOR;
        check("count: spectator member does not count", f.partyMembersInRange(a.player) == 0);
        b.mode = GameMode.SURVIVAL;
        check("count: back from spectator counts again", f.partyMembersInRange(a.player) == 1);
        // offline member does not count
        b.online = false;
        check("count: offline member does not count", f.partyMembersInRange(a.player) == 0);
        b.online = true;
        // member quits (gone from the world entirely)
        w.players.remove(b.player);
        check("count: member who quit does not count", f.partyMembersInRange(a.player) == 0);
        w.players.add(b.player);
        // two members in range
        c.moveTo(w, -20, 64, 0);
        check("count: two members in range", f.partyMembersInRange(a.player) == 2);
        // member leaves the party: standing right there no longer counts
        PartyManager.Change ch = new PartyManager.Change();
        mgr.leave(b.id, ch);
        check("count: ex-member in range does not count", f.partyMembersInRange(a.player) == 1);
        check("count still agrees with hasPartner after leave", (f.partyMembersInRange(a.player) > 0) == f.hasPartner(a.player));
        // stranger joins the party: now counts
        mgr.invite(a.id, d.id, t); mgr.accept(d.id, null, t);
        check("count: new member in range counts", f.partyMembersInRange(a.player) == 2);
        // viewer not in any party: zero, and the stranger danger check (no party -> ticker shows nothing)
        FP2 solo = new FP2(w, "Solo", 1, 64, 0);
        w.players.add(solo.player);
        check("count: solo viewer counts nobody", f.partyMembersInRange(solo.player) == 0);
    }

    // ---------- /excelparty cue command ----------
    static void commandTests() {
        PartyFeatureTest.Rig r = new PartyFeatureTest.Rig(5);
        PartyFeatureTest.FP p = r.java("Ann");
        r.ctl.command(p, new String[]{"cue", "off"});
        check("cue off: told", p.heard("cues are OFF"));
        check("cue off: stored on the shared indicator", !r.ctl.indicator().cuesEnabled(p.id));
        r.ctl.command(p, new String[]{"cue", "on"});
        check("cue on: told and stored", p.heard("cues are ON") && r.ctl.indicator().cuesEnabled(p.id));
        r.ctl.command(p, new String[]{"cue"});
        check("cue with no argument flips", !r.ctl.indicator().cuesEnabled(p.id));
        r.ctl.command(p, new String[]{"cue", "bogus"});
        check("cue junk: usage, state untouched", p.heard("Usage:") && !r.ctl.indicator().cuesEnabled(p.id));
        PartyFeatureTest.FP q = r.java("Ben");
        check("cue setting is per player", r.ctl.indicator().cuesEnabled(q.id));
        r.ctl.command(q, new String[]{"help"});
        check("help mentions cue", q.heard("cue on|off"));
    }

    public static void main(String[] a) {
        machineTests(); toggleTests(); lineTests(); countTests(); commandTests();
        System.out.println("RangeIndicatorTest: " + pass + " passed, " + fail + " failed");
        System.exit(fail == 0 ? 0 : 1);
    }
}
