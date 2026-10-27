import com.winkreyes.excelwandbars.core.TipGate;
import com.winkreyes.excelwandbars.core.WandStyle;

import java.util.List;
import java.util.UUID;

/** 0.2.1 wand tip gate + Sneak bars naming. Synthetic UUIDs only. */
public final class TipTest {
    public static void run() {
        System.out.println("TipTest");
        UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
        TipGate g = new TipGate();
        long t = 1_000_000;
        // 15 uses shown, 16th done
        int shows = 0;
        for (int i = 0; i < 20; i++) { if (g.onUse(a, t) == TipGate.Show.SHOW) shows++; t += 2500; }
        Check.eq(15, shows, "shown on exactly the first 15 uses");
        Check.eq(15, g.uses(a), "count stops at 15");
        Check.eq(TipGate.Show.DONE, g.onUse(a, t + 10_000), "after 15: DONE for good");
        // gap: rapid clicks are one use
        Check.eq(TipGate.Show.SHOW, g.onUse(b, 0 + 5_000_000), "first use shows");
        Check.eq(TipGate.Show.GAP, g.onUse(b, 5_000_500), "click 0.5 s later = same use");
        Check.eq(1, g.uses(b), "gap click not counted");
        Check.eq(TipGate.Show.SHOW, g.onUse(b, 5_002_100), "2.1 s later = next use");
        // ack stops it
        Check.ok(g.ack(b), "ack new");
        Check.ok(!g.ack(b), "ack twice = not new");
        Check.eq(TipGate.Show.ACKED, g.onUse(b, 6_000_000), "after OK: never again");
        // persistence round trip via put
        TipGate h = new TipGate();
        h.put(a, g.uses(a), g.acked(a));
        h.put(b, g.uses(b), g.acked(b));
        Check.eq(TipGate.Show.DONE, h.onUse(a, 1), "reloaded: 15 stays done");
        Check.eq(TipGate.Show.ACKED, h.onUse(b, 1), "reloaded: ack stays");
        h.reset(b);
        Check.eq(TipGate.Show.SHOW, h.onUse(b, 1), "admin reset starts over");
        Check.ok(h.dirty(), "changes mark dirty");
        // disabled / max 0
        TipGate off = new TipGate(); off.enabled = false;
        Check.eq(TipGate.Show.DISABLED, off.onUse(a, 1), "disabled");
        TipGate zero = new TipGate(); zero.maxUses = 0;
        Check.eq(TipGate.Show.DONE, zero.onUse(a, 1), "max-uses 0 = never");
        // Bedrock pop-up: once per session, not right after combat
        TipGate f = new TipGate();
        Check.ok(!f.takeForm(a, 100_000, 95_000), "no pop-up 5 s after a hit");
        Check.ok(f.takeForm(a, 100_000, 80_000), "pop-up 20 s after a hit");
        Check.ok(!f.takeForm(a, 200_000, 0), "second pop-up same session refused");
        f.endSession(a);
        Check.ok(f.takeForm(a, 300_000, 0), "new session: pop-up allowed again");
        Check.ok(f.takeForm(b, 1, 0), "never hit: pop-up allowed");
        // text
        List<String> j = TipGate.lines(false), br = TipGate.lines(true);
        Check.ok(String.join(" ", j).contains(TipGate.L_SNEAK_JAVA) && !String.join(" ", j).toLowerCase().contains("crouch"), "0.2.5 parity: Java tip has the Sneak bars line, Java wording");
        Check.ok(String.join(" ", br).contains("Sneak bars"), "Bedrock tip has the Sneak bars line");
        Check.ok(String.join(" ", j).contains("/wandstyle") && String.join(" ", br).contains("/wandstyle"), "both mention /wandstyle");
        Check.ok(String.join(" ", j).contains("compass") && String.join(" ", br).contains("compass"), "both mention the compass menu");
        Check.ok(String.join(" ", br).contains("/wandstyle ok"), "Bedrock tip says /wandstyle ok");
        String all = String.join(" ", j) + String.join(" ", br) + TipGate.formContent() + TipGate.DONE_ACK + TipGate.OK_BUTTON;
        Check.ok(all.indexOf('\u2014') < 0 && all.indexOf('\u2013') < 0, "no em or en dashes in tip text");
        for (String s : br) Check.ok(s.length() <= 120, "Bedrock line short: " + s.length());
        // 0.2.2: Bedrock wording says tap / hold, never right-click
        Check.ok(!String.join(" ", br).toLowerCase().contains("right-click") && !TipGate.formContent().toLowerCase().contains("right-click"), "0.2.2 Bedrock wand tip has no right-click");
        Check.ok(String.join(" ", br).contains("press and hold") && String.join(" ", br).contains("tap"), "0.2.2 Bedrock wand tip says tap and press and hold");
        Check.ok(String.join(" ", j).contains("right-click"), "Java wand tip unchanged (right-click)");
        // naming
        Check.eq("Sneak bars", WandStyle.BARS.label(), "label Sneak bars");
        Check.eq(WandStyle.BARS, WandStyle.parse("sneakbars"), "parse sneakbars");
        Check.eq(WandStyle.BARS, WandStyle.parse("sneak-bars"), "parse sneak-bars");
        Check.eq("bars", WandStyle.BARS.key(), "storage key still bars (0.2.0 files load)");
    }
}

