import com.winkreyes.excelwandbars.core.*;
import java.util.*;

/** 0.2.4: Tap to cycle (original) and Tap bar, the five-button chooser and rollback safety. */
public final class TapBarTest {
    public static void run() {
        System.out.println("TapBarTest");
        Check.eq(WandStyle.ORIGINAL, WandStyle.parse("original"), "parse original");
        Check.eq(WandStyle.ORIGINAL, WandStyle.parse("Tap-To-Cycle"), "parse tap-to-cycle");
        Check.eq(WandStyle.TAPBAR, WandStyle.parse("tapbar"), "parse tapbar");
        Check.eq(WandStyle.TAPBAR, WandStyle.parse(" phone "), "parse phone = Tap bar");
        Check.eq(WandStyle.BARS, WandStyle.parse("inventory"), "inventory still Sneak bars (0.2.0 alias)");
        Check.eq("original", WandStyle.ORIGINAL.key(), "storage key original");
        Check.eq("tapbar", WandStyle.TAPBAR.key(), "storage key tapbar");
        Check.eq("Tap to pick (original)", WandStyle.ORIGINAL.label(), "label original");
        Check.eq("Tap bar", WandStyle.TAPBAR.label(), "label Tap bar");
        Check.ok(WandStyle.ORIGINAL.inventory() && WandStyle.TAPBAR.inventory() && WandStyle.BARS.inventory(), "inventory(): bars, original, tap bar");
        Check.ok(!WandStyle.CLASSIC.inventory() && !WandStyle.CYCLE.inventory(), "Classic + Cycle are chest");
        Check.ok(WandStyle.ORIGINAL.opensOnHold() && WandStyle.TAPBAR.opensOnHold() && !WandStyle.BARS.opensOnHold(), "open on hold: original + tap bar only");
        Check.eq("sneakbars", WandStyle.BARS.typed(), "typed sneakbars");
        Check.eq("tapbar", WandStyle.TAPBAR.typed(), "typed tapbar");

        StyleProfile p = new StyleProfile();
        Map<String, String> o = p.wanted(WandStyle.ORIGINAL);
        // The first iteration (WRV-070 Phase 1 wands.yml, Oct 17 2026) base_wand keys, exactly.
        Check.eq("inventory", o.get("mode"), "original: inventory mode (Phase 1)");
        Check.eq("cast", o.get("left_click"), "original: left_click cast (Phase 1)");
        Check.eq("none", o.get("right_click"), "original 0.2.6: right_click none (ExcelWandBars: open / items back on Java, hold casts once on Bedrock)");
        Check.eq("toggle", o.get("drop"), "original: drop toggle (Phase 1)");
        Check.eq("cycle_hotbar", o.get("swap"), "original: swap cycle_hotbar (Phase 1)");
        Check.eq("cycle_hotbar_reverse", o.get("swap_sneak"), "original: swap_sneak cycle_hotbar_reverse (Phase 1)");
        Check.eq("manual", o.get("quick_cast"), "original 0.2.6: quick_cast manual (picking never casts, even quick_cast spells)");
        Map<String, String> t = p.wanted(WandStyle.TAPBAR);
        Check.eq("true", t.get("quick_cast"), "tap bar: quick cast (slot pick casts)");
        Check.eq("inventory", t.get("mode"), "tap bar: inventory mode");
        Check.eq("none", t.get("drop"), "tap bar 0.2.5: drop none (ExcelWandBars reads Q / a held slot, never casts)");
        Check.eq("none", t.get("left_click"), "tap bar 0.2.5: left_click none (press and hold never casts)");
        Check.eq("true", t.get("live_hotbar_cooldown"), "tap bar: cooldown shows on the icons");
        Check.eq("none", t.get("right_click"), "tap bar 0.2.5: right_click none (ExcelWandBars: Bedrock hold reads, Java right click opens / items back)");
        Check.eq("cycle_hotbar", t.get("right_click_sneak"), "tap bar: crouch + right click still flips the page (Magic)");
        for (String k : StyleProfile.KEYS) if (!k.equals("quick_cast") && !k.equals("left_click") && !k.equals("drop") && !k.equals("right_click")) Check.eq(o.get(k), t.get(k), "tap bar = original except quick_cast: " + k);
        // 0.2.5: every style's one-line menu text says what crouch / sneak does.
        for (boolean br : new boolean[] { true, false }) for (WandStyle st : WandStyle.values()) {
            String one = ControlsText.oneLine(st, br).toLowerCase();
            Check.ok(one.contains(br ? "crouch" : "sneak"), "menu line says what " + (br ? "crouch" : "sneak") + " does: " + st + ": " + one);
        }

        // classify tells the three inventory styles apart
        Check.eq(WandStyle.ORIGINAL, StyleProfile.classify(o), "classify original");
        Check.eq(WandStyle.TAPBAR, StyleProfile.classify(t), "classify tap bar");
        Check.eq(WandStyle.BARS, StyleProfile.classify(StyleTest.barsTemplate()), "classify Sneak bars unchanged");
        Check.eq(WandStyle.ORIGINAL, StyleProfile.classify(Map.of("mode", "inventory")), "inventory with no quick_cast = original (Phase 1 shape)");

        // apply loops: every pair of styles converges in one write
        for (WandStyle a : WandStyle.values()) for (WandStyle b : WandStyle.values()) {
            Map<String, String> w = new HashMap<>(p.wanted(a));
            w.putAll(p.changes(w, b));
            Check.ok(p.changes(w, b).isEmpty(), a + " -> " + b + ": one write, then matches");
        }
        Map<String, String> fromChest = p.changes(StyleTest.chestTemplate(), WandStyle.TAPBAR);
        Check.eq("mode", fromChest.keySet().iterator().next(), "tap bar from chest: mode written first");

        // rollback: a 0.2.3 build has no "original"/"tapbar"; its parse gives null and the default (Classic) is used.
        StylePrefs s = new StylePrefs();
        UUID a = UUID.fromString("00000000-0000-0000-0000-0000000000aa"), b = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
        s.set(a, WandStyle.TAPBAR); s.set(b, WandStyle.ORIGINAL);
        Map<String, String> saved = s.save();
        Check.eq("tapbar", saved.get(a.toString()), "styles.yml stores tapbar");
        Check.eq("original", saved.get(b.toString()), "styles.yml stores original");
        StylePrefs s2 = new StylePrefs(); Check.eq(0, s2.load(saved), "0.2.4 reload: 0 bad");
        Check.eq(WandStyle.TAPBAR, s2.get(a), "tap bar survives restart");
        Map<String, String> junk = new HashMap<>(); junk.put(a.toString(), "some-future-style");
        StylePrefs s3 = new StylePrefs(); s3.load(junk);
        Check.eq(WandStyle.CLASSIC, s3.get(a), "unknown style key -> Classic (what 0.2.3 does with tapbar)");

        // chooser: five buttons, name line + one-line description, short top text
        List<WandStyle> ord = ControlsText.chooserOrder(true);
        Check.eq(List.of(WandStyle.CLASSIC, WandStyle.BARS, WandStyle.CYCLE, WandStyle.ORIGINAL, WandStyle.TAPBAR), ord, "Bedrock order: Classic, Sneak bars, Cycle, original, Tap bar");
        Check.eq(4, ControlsText.chooserOrder(false).size(), "chooserOrder(false): four (only if sneak-bars-java is turned off)");
        for (boolean br : new boolean[] { true, false }) for (WandStyle st : WandStyle.values()) {
            String btn = ControlsText.button(st, WandStyle.TAPBAR, br);
            String[] lines = btn.split("\n");
            Check.eq(2, lines.length, "button is 2 lines: " + st + " " + br);
            Check.ok(lines[1].length() <= 42, "one-line description fits a phone button (" + lines[1].length() + "): " + lines[1]);
            Check.ok(!btn.contains("\u2014") && !btn.contains("\u2013"), "no long dash " + st);
            if (br) Check.ok(!btn.toLowerCase().contains("click"), "Bedrock button never says click: " + btn);
        }
        Check.ok(ControlsText.button(WandStyle.TAPBAR, WandStyle.TAPBAR, true).startsWith("Tap bar (best on phone) (now)\n"), "current style marked (now)");
        Check.eq("Cycle (next spell)", ControlsText.buttonName(WandStyle.CYCLE, true), "Cycle button name");
        String c = ControlsText.chooserContent(WandStyle.CLASSIC, true, true);
        Check.ok(c.length() <= 60 && !c.contains("\n"), "top text one short line (" + c.length() + "): " + c);

        // texts: Bedrock wording, tips <= 120, no click / em dash
        for (WandStyle st : new WandStyle[] { WandStyle.ORIGINAL, WandStyle.TAPBAR }) {
            String hb = String.join(" ", ControlsText.howTo(st, true));
            Check.ok(!hb.toLowerCase().contains("click"), "Bedrock how-to no click " + st);
            Check.ok(hb.contains("Tap a spell icon"), "Bedrock how-to: tap a spell icon " + st);
            Check.ok(hb.contains("items back"), "how-to says how to get items back " + st);
            Check.ok(!hb.contains("\u2014"), "no em dash " + st);
        }
        for (String l : List.of(TipGate.L_TAPBAR, TipGate.L_TAPBAR_BEDROCK, TipGate.L_ORIGINAL, TipGate.L_ORIGINAL_BEDROCK, TipGate.L_CLASSIC_BEDROCK)) {
            Check.ok(l.length() <= 120, "tip line <= 120 (" + l.length() + ")");
            Check.ok(!l.contains("\u2014"), "tip no em dash");
        }
        Check.ok(!TipGate.L_TAPBAR_BEDROCK.toLowerCase().contains("click") && !TipGate.L_ORIGINAL_BEDROCK.toLowerCase().contains("click"), "Bedrock tips no click");
        Check.ok(TipGate.lines(true).contains(TipGate.L_TAPBAR_BEDROCK) && TipGate.formContent().startsWith(TipGate.L_TAPBAR_BEDROCK), "Bedrock tip + pop-up lead with Tap bar");
        Check.ok(TipGate.lines(false).contains(TipGate.L_ORIGINAL), "Java tip lists the original");
    }
}
