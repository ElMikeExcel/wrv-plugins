import com.winkreyes.excelwandbars.core.*;
import java.util.*;

/** 0.2.3: the third wand style, Cycle (just cycling, no spell list from the wand). */
public final class CycleTest {
    public static void run() {
        System.out.println("CycleTest");
        Check.eq(5, WandStyle.values().length, "0.2.4: five styles: Classic, Sneak bars, Cycle, Tap to cycle (original), Tap bar");
        Check.eq(WandStyle.CYCLE, WandStyle.parse("cycle"), "parse cycle = Cycle (0.2.2 read it as Classic)");
        Check.eq(WandStyle.CYCLE, WandStyle.parse(" Next "), "parse next = Cycle");
        Check.eq(WandStyle.CLASSIC, WandStyle.parse("list"), "parse list = Classic");
        Check.eq(WandStyle.CLASSIC, WandStyle.parse("classic"), "classic still Classic");
        Check.eq(WandStyle.BARS, WandStyle.parse("sneakbars"), "sneakbars still Sneak bars");
        Check.eq("cycle", WandStyle.CYCLE.key(), "storage key cycle");
        Check.eq("Cycle", WandStyle.CYCLE.label(), "label Cycle");
        Check.eq(WandStyle.CLASSIC, WandStyle.CYCLE.other(), "switch from Cycle -> Classic");
        Check.eq(WandStyle.BARS, WandStyle.CLASSIC.other(), "switch Classic -> Sneak bars unchanged");
        Check.ok(WandStyle.CYCLE.chest() && WandStyle.CLASSIC.chest() && !WandStyle.BARS.chest(), "chest(): Classic + Cycle");

        StyleProfile p = new StyleProfile();
        Map<String, String> w = p.wanted(WandStyle.CYCLE);
        Check.eq("chest", w.get("mode"), "cycle: chest mode");
        Check.eq("cast", w.get("left_click"), "cycle: left click / RT casts");
        Check.eq("cycle", w.get("right_click"), "cycle: right click / press and hold / LT = next spell");
        Check.eq("cycle_reverse", w.get("right_click_sneak"), "cycle: sneak + right click / crouch + LT = back one");
        Check.eq("cycle_reverse", w.get("drop"), "cycle: Q = back one");
        Check.eq("cycle_reverse", w.get("drop_sneak"), "cycle: sneak + Q = back one");
        Check.eq("false", w.get("quick_cast"), "cycle: no quick cast");
        Check.ok(!w.containsValue("toggle"), "cycle: no key opens the spell list");
        Check.ok(!w.containsKey("live_hotbar_cooldown") && !w.containsKey("live_hotbar_mana"), "cycle never touches hotbar keys");

        // On today's live chest base_wand (Classic set), Cycle writes exactly the 3 list keys.
        Map<String, String> c = p.changes(StyleTest.chestTemplate(), WandStyle.CYCLE);
        Check.eq(new LinkedHashSet<>(List.of("right_click_sneak", "drop_sneak")), c.keySet(), "cycle on the live chest base_wand: only right_click_sneak + drop_sneak change");
        Map<String, String> after = new HashMap<>(StyleTest.chestTemplate()); after.putAll(c);
        Check.ok(p.changes(after, WandStyle.CYCLE).isEmpty(), "after cycle overrides: matches (no re-apply loop)");
        Check.eq(WandStyle.CYCLE, StyleProfile.classify(after), "classify cycle overrides = Cycle");
        Check.eq(WandStyle.CLASSIC, StyleProfile.classify(StyleTest.chestTemplate()), "classify live chest = Classic");
        Map<String, String> back = new HashMap<>(after); back.putAll(p.changes(after, WandStyle.CLASSIC));
        Check.ok(p.changes(back, WandStyle.CLASSIC).isEmpty(), "Cycle -> Classic puts the list back");
        Check.eq("toggle", back.get("right_click_sneak"), "Classic again: sneak + right click = list");
        Map<String, String> fromBars = p.changes(StyleTest.barsTemplate(), WandStyle.CYCLE);
        Check.eq("chest", fromBars.get("mode"), "cycle from Sneak bars: mode -> chest");
        Check.eq("false", fromBars.get("quick_cast"), "cycle from Sneak bars: quick_cast off");
        Check.eq("mode", fromBars.keySet().iterator().next(), "mode written first");
        Check.ok(p.set(WandStyle.CYCLE, "drop_sneak", "toggle"), "config override for cycle accepted");
        Check.eq("toggle", p.wanted(WandStyle.CYCLE).get("drop_sneak"), "cycle override stored");

        // prefs: save/load and what 0.2.2 makes of it
        StylePrefs s = new StylePrefs();
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000c");
        Check.ok(s.set(a, WandStyle.CYCLE), "pick cycle");
        Map<String, String> saved = s.save();
        Check.eq("cycle", saved.get(a.toString()), "styles.yml stores cycle");
        StylePrefs s2 = new StylePrefs();
        Check.eq(0, s2.load(saved), "reload: 0 bad lines");
        Check.eq(WandStyle.CYCLE, s2.get(a), "cycle survives restart");

        // texts
        String jb = String.join(" ", ControlsText.howTo(WandStyle.CYCLE, true));
        Check.ok(jb.contains("next spell") && jb.contains("go back one spell") && jb.contains("name shows on screen"), "Bedrock cycle how-to: next, back, name on screen");
        Check.ok(jb.contains("LT") && jb.contains("RT"), "Bedrock cycle how-to names the controller buttons");
        String jj = String.join(" ", ControlsText.howTo(WandStyle.CYCLE, false));
        Check.ok(jj.contains("Q goes back") && jj.contains("Left click casts."), "Java cycle how-to");
        String form = ControlsText.chooserContent(WandStyle.BARS, true, true);
        Check.ok(form.startsWith("Now: Sneak bars."), "chooser text starts with the current style (short, nothing cut off)");
        Check.ok(!form.contains("\n"), "0.2.4: chooser top text is one line (descriptions live on the buttons)");
        Check.ok(!form.toLowerCase().contains("click"), "Bedrock chooser text never says click");
        String javaForm = ControlsText.chooserContent(WandStyle.CLASSIC, false, false);
        Check.ok(!ControlsText.chooserOrder(false).contains(WandStyle.BARS) && ControlsText.chooserOrder(false).contains(WandStyle.CYCLE), "Java chooser: no Sneak bars, has Cycle");
        Check.eq(" [Wand controls: Cycle - tap to change]", ControlsText.controlsButton("Cycle", true), "Spell Book controls button shows Cycle");
        Check.eq(" [Wand controls: Classic - click to change]", ControlsText.controlsButton(false, false), "old controlsButton(bool) unchanged");
        String tipJ = String.join(" ", TipGate.lines(false));
        Check.ok(tipJ.contains(TipGate.L_CYCLE), "Java wand tip mentions Cycle");
        Check.ok(TipGate.formContent().contains(TipGate.L_CYCLE_BEDROCK), "Bedrock pop-up mentions Cycle");
        Check.ok(!TipGate.L_CYCLE_BEDROCK.toLowerCase().contains("click"), "Bedrock Cycle tip: no click");
        for (String t : List.of(jb, jj, form, TipGate.L_CYCLE, TipGate.L_CYCLE_BEDROCK)) Check.ok(!t.contains("\u2014"), "no long dash: " + t.substring(0, Math.min(30, t.length())));
    }
}
