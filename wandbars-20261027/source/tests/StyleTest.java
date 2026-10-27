import com.winkreyes.excelwandbars.core.ApplyGuard;
import com.winkreyes.excelwandbars.core.FlipGate;
import com.winkreyes.excelwandbars.core.StylePrefs;
import com.winkreyes.excelwandbars.core.StyleProfile;
import com.winkreyes.excelwandbars.core.WandStyle;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** 0.2.0 wand style opt-in: parse, control sets, change detection, prefs, loop guard, flip gating. */
public final class StyleTest {
    static Map<String, String> chestTemplate() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("mode", "chest"); m.put("left_click", "cast"); m.put("right_click", "cycle");
        m.put("right_click_sneak", "toggle"); m.put("drop", "cycle_reverse"); m.put("drop_sneak", "toggle");
        m.put("swap", "cycle"); m.put("swap_sneak", "cycle_reverse"); m.put("quick_cast", null);
        return m;
    }
    static Map<String, String> barsTemplate() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("mode", "inventory"); m.put("left_click", "cast"); m.put("right_click", "toggle");
        m.put("right_click_sneak", "cycle"); m.put("drop", "cycle_hotbar"); m.put("drop_sneak", "cycle_hotbar_reverse");
        m.put("swap", "cycle_hotbar"); m.put("swap_sneak", "cycle_reverse"); m.put("quick_cast", "true");
        m.put("live_hotbar_cooldown", "true"); m.put("live_hotbar_mana", "true");
        return m;
    }

    public static void run() {
        System.out.println("StyleTest");
        // parse
        Check.eq(WandStyle.CLASSIC, WandStyle.parse("classic"), "parse classic");
        Check.eq(WandStyle.CLASSIC, WandStyle.parse(" OLD "), "parse old (any case, spaces)");
        Check.eq(WandStyle.BARS, WandStyle.parse("bars"), "parse bars");
        Check.eq(WandStyle.BARS, WandStyle.parse("hotbar"), "parse hotbar");
        Check.eq(null, WandStyle.parse("banana"), "parse unknown = null");
        Check.eq(null, WandStyle.parse(null), "parse null = null");
        Check.eq(WandStyle.BARS, WandStyle.CLASSIC.other(), "switch classic -> bars");
        Check.eq("bars", WandStyle.BARS.key(), "storage key bars");

        StyleProfile p = new StyleProfile();
        // classic on a chest template (noab live): nothing to write
        Check.ok(p.changes(chestTemplate(), WandStyle.CLASSIC).equals(Map.of("xp_display", "none", "level_display", "none")), "classic on chest base_wand: only vanilla XP display change");
        // bars on a bars template: nothing to write
        Check.ok(p.changes(barsTemplate(), WandStyle.BARS).equals(Map.of("xp_display", "none", "level_display", "none")), "bars on inventory base_wand: only vanilla XP display change");
        // classic on bars template (overlay live): all 8 differing keys
        Map<String, String> c = p.changes(barsTemplate(), WandStyle.CLASSIC);
        Check.eq("chest", c.get("mode"), "classic on inventory base_wand: mode -> chest");
        Check.eq("cycle", c.get("right_click"), "classic: right_click -> cycle");
        Check.eq("toggle", c.get("right_click_sneak"), "classic: right_click_sneak -> toggle (spell list)");
        Check.eq("cycle_reverse", c.get("drop"), "classic: drop (Q) -> cycle_reverse");
        Check.eq("false", c.get("quick_cast"), "classic: quick_cast -> false");
        Check.ok(!c.containsKey("left_click") && !c.containsKey("swap_sneak"), "classic: equal keys not rewritten");
        Check.eq("mode", c.keySet().iterator().next(), "mode written first");
        // bars on chest template
        Map<String, String> b = p.changes(chestTemplate(), WandStyle.BARS);
        Check.eq("inventory", b.get("mode"), "bars on chest base_wand: mode -> inventory");
        Check.eq("true", b.get("quick_cast"), "bars: quick_cast -> true");
        Check.eq("toggle", b.get("right_click"), "bars: right_click -> toggle (opens bar)");
        Check.eq("true", b.get("live_hotbar_cooldown"), "bars: live hotbar cooldown on");
        Check.ok(!c.containsKey("live_hotbar_cooldown") && !c.containsKey("live_hotbar_mana"), "classic never touches the live hotbar keys");
        // round trip: apply the changes and recheck
        Map<String, String> after = new HashMap<>(barsTemplate()); after.putAll(c);
        Check.ok(p.changes(after, WandStyle.CLASSIC).isEmpty(), "after classic overrides: matches");
        Map<String, String> back = new HashMap<>(after); back.putAll(p.changes(after, WandStyle.BARS));
        Check.ok(p.changes(back, WandStyle.BARS).isEmpty(), "switch back to bars: matches");
        // case and boolean leniency
        Map<String, String> upper = chestTemplate(); upper.put("mode", "CHEST"); upper.put("quick_cast", "False");
        Check.ok(p.changes(upper, WandStyle.CLASSIC).equals(Map.of("xp_display", "none", "level_display", "none")), "upper-case CHEST / False count as equal");
        Check.ok(p.changes(null, WandStyle.CLASSIC).size() == 10, "unknown wand (all null) on classic: 10 keys written including vanilla XP display (missing quick_cast already means off)");
        Check.eq(WandStyle.CLASSIC, StyleProfile.classify(chestTemplate()), "classify chest = classic");
        Check.eq(WandStyle.BARS, StyleProfile.classify(barsTemplate()), "classify inventory = bars");
        // config override
        Check.ok(p.set(WandStyle.BARS, "right_click_sneak", "none"), "config override accepted");
        Check.eq("none", p.wanted(WandStyle.BARS).get("right_click_sneak"), "config override stored");
        Check.ok(!p.set(WandStyle.BARS, "spells", "x"), "non-control key refused");

        // prefs
        StylePrefs s = new StylePrefs();
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID z = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        Check.eq(WandStyle.CLASSIC, s.get(a), "default classic for a new player");
        Check.ok(s.set(a, WandStyle.BARS), "pick bars changes style");
        Check.eq(WandStyle.BARS, s.get(a), "bars stored");
        Check.eq(WandStyle.CLASSIC, s.get(z), "other players stay classic");
        Map<String, String> saved = s.save();
        StylePrefs s2 = new StylePrefs();
        Check.eq(0, s2.load(saved), "reload saved prefs, 0 bad lines");
        Check.eq(WandStyle.BARS, s2.get(a), "bars survives save + load (rejoin / restart)");
        Check.ok(!s2.set(a, WandStyle.BARS), "picking the same style again = no change");
        Check.ok(s2.set(a, WandStyle.CLASSIC), "switch back to classic");
        Check.eq(WandStyle.CLASSIC, s2.get(a), "classic again");
        Map<String, String> junk = new HashMap<>(); junk.put("not-a-uuid", "bars"); junk.put(z.toString(), "banana"); junk.put(a.toString(), "bars");
        Check.eq(2, s2.load(junk), "bad lines skipped and counted");
        Check.eq(WandStyle.BARS, s2.get(a), "good line still loaded");
        Check.ok(s2.clear(a) && s2.get(a) == WandStyle.CLASSIC, "clear = back to default");

        // loop guard
        ApplyGuard g = new ApplyGuard(4, 10_000);
        int ok = 0; for (int i = 0; i < 10; i++) if (g.allow(a, 1000 + i)) ok++;
        Check.eq(4, ok, "loop guard: 4 applies per 10 s");
        Check.ok(g.allow(a, 12_000), "loop guard: allowed again after the window");
        Check.ok(g.allow(z, 1005), "loop guard is per player");

        // flip only for bars players
        FlipGate gate = new FlipGate();
        FlipGate.State st = new FlipGate.State();
        st.sneakStart = true; st.hasActiveWand = true; st.inventoryMode = true; st.barOpen = true; st.barCount = 3;
        st.barsStyle = false;
        Check.eq(FlipGate.Result.CLASSIC_STYLE, gate.decide(a, st, new FlipGate.Options(), 5000), "classic player: sneak never flips, even on an inventory wand");
        st.barsStyle = true;
        Check.eq(FlipGate.Result.FLIP, gate.decide(a, st, new FlipGate.Options(), 6000), "bars player: sneak flips");
    }
}

