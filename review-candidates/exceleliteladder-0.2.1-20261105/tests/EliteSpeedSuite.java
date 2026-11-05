import com.winkreyes.exceleliteladder.core.*;
import java.util.*;

/**
 * ExcelEliteLadder 0.2.1 suite for the elite-speed settings (core only):
 * parse/defaults, clamp + warning behaviour, the natural-only gate, and the
 * effect spec the Bukkit glue applies verbatim. The Bukkit-side construction
 * (PotionEffectType.SPEED with these exact parameters) cannot run on a plain
 * JVM — PotionEffectType needs a live registry — so the parameter VALUES are
 * pinned here and the construction itself is proven on the rig.
 */
public final class EliteSpeedSuite {

    static EliteSpeedSettings parse(Object e, Object a, Object d, List<String> w) {
        return EliteSpeedSettings.parse(e, a, d, w);
    }

    static void run() {
        System.out.println("0.2.1 elite speed: settings parse, clamp, gate, effect spec");

        List<String> w = new ArrayList<>();
        EliteSpeedSettings def = parse(null, null, null, w);
        Check.ok(def.enabled(), "absent keys -> enabled (default)");
        Check.eq(0, def.amplifier(), "absent keys -> amplifier 0 (Speed I)");
        Check.ok(def.permanent() && def.durationTicks() == Integer.MAX_VALUE,
                "absent keys -> permanent (Integer.MAX_VALUE ticks)");
        Check.eq(0, w.size(), "absent keys -> no warnings");
        Check.eq("on (Speed I, permanent)", def.describe(), "default describe: on (Speed I, permanent)");
        Check.eq("ON (amplifier 0, duration permanent)", def.bootText(), "default boot text");

        EliteSpeedSettings explicit = parse(Boolean.FALSE, 1, 100000, new ArrayList<>());
        Check.ok(!explicit.enabled(), "explicit enabled=false kept");
        Check.eq("off", explicit.describe(), "disabled describe: off");
        Check.eq("OFF", explicit.bootText(), "disabled boot text: OFF");
        EliteSpeedSettings emPower = parse(Boolean.TRUE, 1, 100000, new ArrayList<>());
        Check.eq(1, emPower.amplifier(), "amplifier 1 kept (EM's own power value)");
        Check.eq(100000, emPower.durationTicks(), "finite duration 100000 kept");
        Check.ok(!emPower.permanent(), "finite duration is not permanent");
        Check.eq("on (Speed II, 100000 ticks)", emPower.describe(), "describe: on (Speed II, 100000 ticks)");

        w.clear();
        EliteSpeedSettings clampLow = parse(null, -3, null, w);
        Check.eq(0, clampLow.amplifier(), "amplifier -3 clamps to 0");
        Check.eq(1, w.size(), "amplifier clamp warns once");
        w.clear();
        EliteSpeedSettings clampHigh = parse(null, 500, null, w);
        Check.eq(EliteSpeedSettings.AMPLIFIER_MAX, clampHigh.amplifier(), "amplifier 500 clamps to 127");
        Check.eq(1, w.size(), "amplifier high clamp warns once");
        w.clear();
        EliteSpeedSettings frac = parse(null, 1.5, null, w);
        Check.eq(0, frac.amplifier(), "amplifier 1.5 (not whole) falls back to 0");
        Check.eq(1, w.size(), "non-whole amplifier warns once");
        w.clear();
        EliteSpeedSettings text = parse(null, "fast", null, w);
        Check.eq(0, text.amplifier(), "amplifier as text falls back to 0");
        Check.eq(1, w.size(), "text amplifier warns once");

        w.clear();
        EliteSpeedSettings durZero = parse(null, null, 0, w);
        Check.ok(durZero.permanent(), "duration 0 falls back to permanent");
        Check.eq(1, w.size(), "duration 0 warns once");
        w.clear();
        EliteSpeedSettings durNeg = parse(null, null, -7, w);
        Check.ok(durNeg.permanent(), "duration -7 falls back to permanent");
        Check.eq(1, w.size(), "duration -7 warns once");
        EliteSpeedSettings durHuge = parse(null, null, 3_000_000_000L, new ArrayList<>());
        Check.ok(durHuge.permanent() && durHuge.durationTicks() == Integer.MAX_VALUE,
                "duration past int range clamps to permanent");
        EliteSpeedSettings durMinusOne = parse(null, null, -1, new ArrayList<>());
        Check.ok(durMinusOne.permanent(), "duration -1 is permanent (config idiom)");

        w.clear();
        EliteSpeedSettings enText = parse("yes", null, null, w);
        Check.ok(enText.enabled(), "enabled as text falls back to true");
        Check.eq(1, w.size(), "text enabled warns once");

        Check.eq("Speed I", parse(null, 0, null, new ArrayList<>()).amplifierLabel(), "amp 0 -> Speed I");
        Check.eq("Speed II", parse(null, 1, null, new ArrayList<>()).amplifierLabel(), "amp 1 -> Speed II");
        Check.eq("Speed V", parse(null, 4, null, new ArrayList<>()).amplifierLabel(), "amp 4 -> Speed V");
        Check.eq("Speed X", parse(null, 9, null, new ArrayList<>()).amplifierLabel(), "amp 9 -> Speed X");
        Check.eq("Speed 13", parse(null, 12, null, new ArrayList<>()).amplifierLabel(), "amp 12 -> Speed 13 (plain past X)");

        EliteSpeedSettings on = EliteSpeedSettings.defaults();
        Check.ok(on.shouldBuff(true, true), "gate: on + EM present + natural -> buff");
        Check.ok(!on.shouldBuff(true, false), "gate: custom boss -> NO buff");
        Check.ok(!on.shouldBuff(false, true), "gate: EM absent -> NO buff");
        Check.ok(!explicit.shouldBuff(true, true), "gate: feature off -> NO buff");

        Check.ok(!EliteSpeedSettings.EFFECT_AMBIENT && !EliteSpeedSettings.EFFECT_PARTICLES
                && !EliteSpeedSettings.EFFECT_ICON,
                "effect spec: ambient off, particles off, icon off (hidden buff)");
    }
}
