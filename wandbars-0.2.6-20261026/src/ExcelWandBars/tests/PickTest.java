import com.winkreyes.excelwandbars.core.ControlsText;
import com.winkreyes.excelwandbars.core.StyleProfile;
import com.winkreyes.excelwandbars.core.TipGate;
import com.winkreyes.excelwandbars.core.WandStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.2.6: Tap to pick (original) picks, never casts, on a tap. Models Magic 11.2.5's
 * Mage.activateIcon + Wand quick_cast parsing (wand/Wand.java, magic/Mage.java) so the
 * root cause is reproduced without a server, then checks the new profile and that no
 * other style changed.
 */
public final class PickTest {
    /** Wand.loadProperties: quick_cast string -> {quickCast, quickCastDisabled} (inventory mode base resets both to false). */
    static boolean[] magicQuickCast(String v) {
        boolean qc = false, disabled = false;
        if (v != null) {
            if (v.equalsIgnoreCase("true")) { qc = true; disabled = true; }
            else if (v.equalsIgnoreCase("manual")) { qc = false; disabled = true; }
            else if (v.equalsIgnoreCase("disable")) { qc = false; disabled = true; }
            else { qc = false; disabled = false; }
        }
        return new boolean[] { qc, disabled };
    }

    /** What a hotbar tap does with the bar up: PlayerController.onPlayerItemHeld then Mage.activateIcon. */
    static String tap(String wandQuickCast, boolean spellQuickCast) {
        boolean[] w = magicQuickCast(wandQuickCast);
        if (w[0]) return "cast";                            // PlayerController: wand quick cast
        boolean isQuick = spellQuickCast && !w[1];         // Mage.activateIcon (inventory mode)
        return isQuick ? "cast" : "pick";
    }

    public static void run() {
        System.out.println("PickTest (0.2.6 Tap to pick)");
        // Reproduce the bug: 0.2.5 wrote quick_cast "false" for original.
        Check.eq("cast", tap("false", true), "0.2.5 bug reproduced: quick_cast false + a quick_cast spell (cure, boon, superheal, feed, recall) CASTS on tap");
        Check.eq("pick", tap("false", false), "0.2.5: quick_cast false + a normal spell (heal) picks");
        // The fix.
        Check.eq("pick", tap("manual", true), "0.2.6: quick_cast manual + a quick_cast spell picks");
        Check.eq("pick", tap("manual", false), "0.2.6: quick_cast manual + a normal spell picks");
        // Tap bar unchanged: still casts every spell on tap.
        Check.eq("cast", tap("true", true), "Tap bar: quick_cast true casts a quick_cast spell");
        Check.eq("cast", tap("true", false), "Tap bar: quick_cast true casts a normal spell");

        StyleProfile p = new StyleProfile();
        Map<String, String> o = p.wanted(WandStyle.ORIGINAL);
        Check.eq("manual", o.get("quick_cast"), "original wants quick_cast manual");
        Check.eq("none", o.get("right_click"), "original wants right_click none (handled by ExcelWandBars)");
        Check.eq("cast", o.get("left_click"), "original keeps left_click cast (tap the screen / RT / left click casts the picked spell)");
        Check.eq("toggle", o.get("drop"), "original keeps drop toggle (Q / hold the wand slot = items back)");
        Check.eq("cycle_hotbar", o.get("right_click_sneak"), "original keeps crouch + right click = next page");

        // Every other style is byte-for-byte what 0.2.5 wanted.
        Map<WandStyle, Map<String, String>> v025 = new LinkedHashMap<>();
        v025.put(WandStyle.CLASSIC, m("mode","chest","left_click","cast","right_click","cycle","right_click_sneak","toggle","drop","cycle_reverse","drop_sneak","toggle","swap","cycle","swap_sneak","cycle_reverse","quick_cast","false"));
        v025.put(WandStyle.CYCLE, m("mode","chest","left_click","cast","right_click","cycle","right_click_sneak","cycle_reverse","drop","cycle_reverse","drop_sneak","cycle_reverse","swap","cycle","swap_sneak","cycle_reverse","quick_cast","false"));
        v025.put(WandStyle.BARS, m("mode","inventory","left_click","cast","right_click","toggle","right_click_sneak","cycle","drop","cycle_hotbar","drop_sneak","cycle_hotbar_reverse","swap","cycle_hotbar","swap_sneak","cycle_reverse","quick_cast","true","live_hotbar_cooldown","true","live_hotbar_mana","true"));
        v025.put(WandStyle.TAPBAR, m("mode","inventory","left_click","none","right_click","none","right_click_sneak","cycle_hotbar","drop","none","drop_sneak","cycle_hotbar_reverse","swap","cycle_hotbar","swap_sneak","cycle_hotbar_reverse","quick_cast","true","live_hotbar_cooldown","true","live_hotbar_mana","true"));
        for (Map.Entry<WandStyle, Map<String, String>> e : v025.entrySet())
            Check.eq(e.getValue(), new HashMap<>(p.wanted(e.getKey())), "unchanged from 0.2.5: " + e.getKey());

        // A wand set up by 0.2.5 (original) is still recognised as original and gets exactly two keys.
        Map<String, String> old = m("mode","inventory","left_click","cast","right_click","toggle","right_click_sneak","cycle_hotbar","drop","toggle","drop_sneak","cycle_hotbar_reverse","swap","cycle_hotbar","swap_sneak","cycle_hotbar_reverse","quick_cast","false","live_hotbar_cooldown","true","live_hotbar_mana","true");
        Check.eq(WandStyle.ORIGINAL, StyleProfile.classify(old), "0.2.5 original wand classifies as original");
        Check.eq(m("right_click","none","quick_cast","manual"), new HashMap<>(p.changes(old, WandStyle.ORIGINAL)), "0.2.5 original wand -> 0.2.6: writes right_click none + quick_cast manual only");
        Check.eq(WandStyle.ORIGINAL, StyleProfile.classify(o), "0.2.6 original wand classifies as original (manual is not true)");
        Check.eq(WandStyle.TAPBAR, StyleProfile.classify(p.wanted(WandStyle.TAPBAR)), "Tap bar still classifies as Tap bar");
        Check.ok(p.changes(new HashMap<>(o), WandStyle.ORIGINAL).isEmpty(), "0.2.6 original wand: nothing more to write (no apply loop)");
        Map<String, String> upper = new HashMap<>(o); upper.put("quick_cast", "MANUAL");
        Check.ok(p.changes(upper, WandStyle.ORIGINAL).isEmpty(), "Magic reporting MANUAL in caps still matches (no rewrite loop)");

        // Wording matches what the style now does.
        List<String> br = ControlsText.howTo(WandStyle.ORIGINAL, true);
        Check.ok(br.get(1).contains("Tap a spell icon to pick it") && br.get(1).contains("press and hold the screen") && br.get(1).contains("to cast"), "Bedrock how-to: tap picks, press and hold casts");
        List<String> jv = ControlsText.howTo(WandStyle.ORIGINAL, false);
        Check.ok(jv.get(1).contains("to pick a spell") && jv.get(1).contains("Left click casts"), "Java how-to: key picks, left click casts");
        Check.ok(jv.get(3).contains("right click puts your items back") && jv.get(3).contains("Right click again brings the bar back"), "Java how-to: right click items back, again brings the bar back");
        Check.eq("Tap: pick. Hold: cast. Crouch: page", ControlsText.oneLine(WandStyle.ORIGINAL, true), "Bedrock menu line");
        Check.eq("Key picks, left click casts, sneak pages", ControlsText.oneLine(WandStyle.ORIGINAL, false), "Java menu line");
        Check.ok(TipGate.L_ORIGINAL_BEDROCK.contains("tap a spell icon to pick it") && TipGate.L_ORIGINAL_BEDROCK.contains("press and hold to cast"), "Bedrock wand tip: tap picks, hold casts");
        Check.ok(TipGate.L_ORIGINAL.contains("hotbar key picks a spell") && TipGate.L_ORIGINAL.contains("left-click casts"), "Java wand tip: key picks, left click casts");
        for (String l : List.of(String.join(" ", br), String.join(" ", jv), TipGate.L_ORIGINAL, TipGate.L_ORIGINAL_BEDROCK))
            Check.ok(!l.contains("\u2014") && !l.contains("\u2013"), "no em or en dashes: " + l.substring(0, Math.min(40, l.length())));
        Check.ok(!String.join(" ", br).toLowerCase().contains("click"), "Bedrock wording never says click");
    }

    static Map<String, String> m(String... kv) {
        Map<String, String> out = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2) out.put(kv[i], kv[i + 1]);
        return out;
    }
}
