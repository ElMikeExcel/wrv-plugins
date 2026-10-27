import com.winkreyes.excelwandbars.core.*;
import java.util.*;

public final class WandSlotTest {
    static WandSlotPlan.State st(int wand, int held) { WandSlotPlan.State s = new WandSlotPlan.State(); s.wandSlot = wand; s.heldSlot = held; return s; }

    public static void run() {
        System.out.println("WandSlotTest");
        Check.eq(8, WandSlotPlan.targetIndex(9), "slot 9 -> index 8");
        Check.eq(0, WandSlotPlan.targetIndex(1), "slot 1 -> index 0");
        Check.eq(8, WandSlotPlan.targetIndex(0), "bad slot 0 -> 9");
        Check.eq(8, WandSlotPlan.targetIndex(12), "bad slot 12 -> 9");
        int T = 8;
        Check.eq(WandSlotPlan.Action.SWAP, WandSlotPlan.sweep(st(2, 0), T), "sweep: wand in 3, holding 1 -> move");
        Check.eq(WandSlotPlan.Action.SWAP, WandSlotPlan.sweep(st(2, 2), T), "sweep: holding the wand in 3 -> move (held path)");
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.sweep(st(8, 0), T), "sweep: already in 9");
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.sweep(st(-1, 0), T), "sweep: no wand in hotbar");
        Check.eq("holding the target slot", WandSlotPlan.why(st(2, 8), T), "sweep waits while holding slot 9's item");
        String[] blocks = { "barOpen", "otherScreenOpen", "cursorHasItem", "deadOrSpectator", "skipWorld", "recentInventoryClick", "targetIsRankWand" };
        for (String b : blocks) {
            WandSlotPlan.State s = st(2, 0);
            switch (b) {
                case "barOpen": s.barOpen = true; break;
                case "otherScreenOpen": s.otherScreenOpen = true; break;
                case "cursorHasItem": s.cursorHasItem = true; break;
                case "deadOrSpectator": s.deadOrSpectator = true; break;
                case "skipWorld": s.skipWorld = true; break;
                case "recentInventoryClick": s.recentInventoryClick = true; break;
                default: s.targetIsRankWand = true;
            }
            Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.sweep(s, T), "sweep blocked by " + b);
        }
        // pick
        Check.eq(WandSlotPlan.Action.SWAP, WandSlotPlan.onPick(st(2, 0), 2, T), "pick wand in 3 -> swap");
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.onPick(st(2, 0), 4, T), "pick another slot -> never touched");
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.onPick(st(8, 0), 8, T), "pick wand in 9 -> nothing");
        WandSlotPlan.State bo = st(2, 0); bo.barOpen = true;
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.onPick(bo, 2, T), "pick with bar up -> Magic's");
        WandSlotPlan.State rc = st(2, 0); rc.recentInventoryClick = true;
        Check.eq(WandSlotPlan.Action.SWAP, WandSlotPlan.onPick(rc, 2, T), "pick right after a click still moves (the pick is the player's own act)");
        WandSlotPlan.State cu = st(2, 0); cu.cursorHasItem = true;
        Check.eq(WandSlotPlan.Action.NONE, WandSlotPlan.onPick(cu, 2, T), "pick with cursor item -> no move");

        // Swap model: every item kept exactly once, counts unchanged (no dupe, no loss).
        Random r = new Random(27);
        boolean allOk = true;
        for (int n = 0; n < 2000; n++) {
            String[] bar = new String[9];
            int wand = r.nextInt(9);
            for (int i = 0; i < 9; i++) bar[i] = i == wand ? "WAND" : (r.nextInt(3) == 0 ? null : ("item" + i + "x" + (1 + r.nextInt(64))));
            List<String> before = new ArrayList<>(Arrays.asList(bar));
            if (wand != T) WandSlotPlan.swap(bar, wand, T);
            List<String> after = new ArrayList<>(Arrays.asList(bar));
            List<String> b2 = new ArrayList<>(before); b2.removeIf(Objects::isNull); Collections.sort(b2);
            List<String> a2 = new ArrayList<>(after); a2.removeIf(Objects::isNull); Collections.sort(a2);
            boolean ok = b2.equals(a2) && "WAND".equals(bar[T]) && Objects.equals(bar[wand], before.get(T));
            for (int i = 0; i < 9; i++) if (i != wand && i != T && !Objects.equals(bar[i], before.get(i))) ok = false;
            if (!ok) { allOk = false; System.out.println("    bad " + before + " -> " + after); break; }
        }
        Check.ok(allOk, "2000 random hotbars: one swap keeps every item exactly once, wand in 9, slot 9's item in the wand's old slot, others untouched");

        // wand tap guard
        Check.ok(WandSlotPlan.isWandTap(true, true, 8, 8, 10_000, 9_000, 700), "tap on the selected wand slot 1 s after a change -> tap");
        Check.ok(!WandSlotPlan.isWandTap(true, true, 8, 8, 10_000, 9_500, 700), "re-sent slot 0.5 s after a change -> not a tap");
        Check.ok(!WandSlotPlan.isWandTap(true, true, 3, 8, 10_000, 0, 700), "another slot -> not a wand tap");
        Check.ok(!WandSlotPlan.isWandTap(false, true, 8, 8, 10_000, 0, 700), "other style -> not a wand tap");
        Check.ok(!WandSlotPlan.isWandTap(true, false, 8, 8, 10_000, 0, 700), "not holding the wand -> not a wand tap");

        // Menu wording: Tap bar lines say what tap, hold and crouch do, fit a phone button, no long dash.
        String bt = ControlsText.button(WandStyle.TAPBAR, WandStyle.TAPBAR, true);
        String[] l = bt.split("\n");
        Check.ok(l[0].length() <= 38, "Bedrock Tap bar title line <= 38 with (now) (" + l[0].length() + "): " + l[0]);
        Check.ok(l[1].length() <= 38, "Bedrock Tap bar line 2 <= 38 (" + l[1].length() + "): " + l[1]);
        Check.ok(bt.contains("Tap:") && bt.contains("Menu:") && bt.contains("Crouch:") && bt.contains("tap wand: items back"), "Bedrock Tap bar button: tap, menu Book, crouch and tap wand");
        String jt = ControlsText.button(WandStyle.TAPBAR, WandStyle.CLASSIC, false);
        Check.ok(jt.contains("Q: items back") && jt.contains("sneak pages") && jt.contains("/spellbook reads"), "Java Tap bar button: key, left click, sneak, Q");
        Check.ok(!jt.contains("tap wand"), "Java never promises tap the wand (Java sends nothing for it)");
        String hb = String.join(" ", ControlsText.howTo(WandStyle.TAPBAR, true));
        Check.ok(hb.contains("last hotbar slot") && hb.contains("Tap the wand again"), "Bedrock Tap bar how-to: last slot + tap the wand");
        String hj = String.join(" ", ControlsText.howTo(WandStyle.TAPBAR, false));
        Check.ok(hj.contains("last hotbar slot (9)") && hj.contains("(1-8)") && !hj.contains("Tap the wand"), "Java Tap bar how-to: slot 9, keys 1-8");
        for (WandStyle s : WandStyle.values()) for (boolean b : new boolean[] { true, false }) {
            String all = ControlsText.button(s, s, b) + String.join(" ", ControlsText.howTo(s, b)) + ControlsText.chooserContent(s, b, true);
            Check.ok(!all.contains("\u2014") && !all.contains("\u2013"), "no long dash " + s + " " + b);
        }
        Check.ok(ControlsText.chooserContent(WandStyle.TAPBAR, true, true).equals("Now: Tap bar (best on phone). Pick one. Switch any time."), "form top line keeps the short name");
    }
}

