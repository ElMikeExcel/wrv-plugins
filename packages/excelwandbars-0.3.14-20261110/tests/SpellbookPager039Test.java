package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ArrangePlan;
import com.winkreyes.excelwandbars.core.BarLoadout;
import com.winkreyes.excelwandbars.core.DragWritePlan;
import com.winkreyes.excelwandbars.core.DragWritePlan.Intent;
import com.winkreyes.excelwandbars.core.DragWritePlan.Result;
import com.winkreyes.excelwandbars.core.PagerPlan;
import java.util.List;
import java.util.UUID;
/**
 * 0.3.9: the three spellbook fixes, at plan/model level.
 *
 * Defect #1 (pager everywhere): list-page math, the axis rule, and the
 * displace classification the hook renders by.
 * Defect #4 (Mike's exact live repro): lift a spell from the list, tap an
 * OCCUPIED bar slot — one flow, first try, slow tap included. In 0.3.8 the
 * duplicate fire of the pickup tap resolved as a place and ate the lift;
 * the hook now re-lifts on a duplicate pickup (its guard reads
 * liftedSpell), and the plan/model half pinned here is that a refreshed
 * lift still resolves to exactly one PUT with swap semantics, at any tap
 * speed, and that the model reflects a remove immediately (the list
 * refreshes in place — the layout is recomputed from the same model the
 * commit just changed, never from a cached copy).
 */
public class SpellbookPager039Test {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Result r, Intent i, String spell, int slot){
  ok(r != null && r.intent == i && (spell == null ? r.spell == null : spell.equals(r.spell)) && r.slot == slot);
 }
 public static void main(String[]args){
  // ---------------------------------------------------------- list pages
  ok(PagerPlan.listPages(0) == 1);
  ok(PagerPlan.listPages(1) == 1);
  ok(PagerPlan.listPages(27) == 1);
  ok(PagerPlan.listPages(28) == 2);
  ok(PagerPlan.listPages(54) == 2);
  ok(PagerPlan.listPages(55) == 3);
  // Mike's wand: 55 spells, 21 on the bar -> 34 parked -> 2 list pages.
  ok(PagerPlan.listPages(55 - 21) == 2);
  // Jester's wand: 95 spells -> wolf sits on a later list page either way.
  ok(PagerPlan.listPages(95 - 21) == 3);
  ok(PagerPlan.listStateLine(0, 3).equals("List page 1 of 3"));
  ok(PagerPlan.listStateLine(2, 3).equals("List page 3 of 3"));
  ok(PagerPlan.listStateLine(9, 3).equals("List page 3 of 3")); // clamped, never out of range
  // List flips wrap at both ends, exactly like the wand click.
  ok(PagerPlan.target(2, 3, 1) == 0);
  ok(PagerPlan.target(0, 3, -1) == 2);
  // Buttons render when EITHER axis has somewhere to go.
  ok(!PagerPlan.shows(1, 1));
  ok(PagerPlan.shows(2, 1));
  ok(PagerPlan.shows(1, 2));
  ok(PagerPlan.shows(3, 3));
  // The axis rule: "auto" follows the last paging used, and a
  // single-page axis never takes the buttons. 0.3.12: the untaught
  // default is the LIST (0.3.8/0.3.9 defaulted to the bar — the trap
  // Mike hit Nov 7, 2026; this assertion changed by design).
  ok(PagerPlan.autoAxis(null, 3, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.autoAxis(PagerPlan.Axis.LIST, 3, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.autoAxis(PagerPlan.Axis.BAR, 3, 2) == PagerPlan.Axis.BAR);
  ok(PagerPlan.autoAxis(PagerPlan.Axis.LIST, 3, 1) == PagerPlan.Axis.BAR);
  ok(PagerPlan.autoAxis(PagerPlan.Axis.BAR, 1, 2) == PagerPlan.Axis.LIST);
  // ---------------------------------------------------------- displace classification
  ok(PagerPlan.classify(true, false, false) == PagerPlan.SlotFate.FREE);
  ok(PagerPlan.classify(false, true, false) == PagerPlan.SlotFate.FREE);
  ok(PagerPlan.classify(false, false, true) == PagerPlan.SlotFate.DISPLACE);
  ok(PagerPlan.classify(false, false, false) == PagerPlan.SlotFate.BLOCKED);
  // ---------------------------------------------------------- defect #4: Mike's exact repro
  UUID a = UUID.randomUUID();
  DragWritePlan p = new DragWritePlan();
  long t = 500_000L;
  // The bar: 7 slots on this page, heal sits in slot 3. The wand knows
  // heal, mend and wolf; wolf is in the book (off-bar).
  BarLoadout l = new BarLoadout(7);
  l.put("mend", 0);
  l.put("heal", 3);
  ok(l.indexOf("wolf") == -1 && l.indexOf("heal") == 3);
  // (1) Lift wolf from the spell list (off-bar: from = -1).
  p.pickup(a, "wolf", -1, t);
  ok("wolf".equals(p.liftedSpell(a)));
  // (2) The pickup tap fires twice (the Bedrock duplicate). The hook's
  // re-lift guard refreshes the lift instead of resolving it as a place;
  // at plan level a refreshed pickup keeps the same lift, unconsumed.
  p.pickup(a, "wolf", -1, t + 150);
  ok("wolf".equals(p.liftedSpell(a)));
  // (3) The place tap, 2.4 s later (a slow second tap), onto the OCCUPIED
  // slot 3: exactly one PUT, first try — no remove first, no reopen.
  is(p.place(a, ArrangePlan.Target.bar(3), t + 2_400), Intent.PUT, "wolf", 3);
  ok(p.liftedSpell(a) == null);
  // Model side: wolf takes slot 3; heal is displaced back to the pool —
  // still known, now parked (the swap semantics of BarLoadout.put).
  BarLoadout.Change c = l.put("wolf", 3);
  ok(c.ok() && "put".equals(c.kind) && "heal".equals(c.other));
  ok(l.indexOf("wolf") == 3 && l.indexOf("heal") == -1);
  BarLoadout.Layout lay = l.layout(List.of("mend", "heal", "wolf"), 7);
  ok(lay.onBar == 2 && lay.inBook == 1);
  ok(lay.slots.get("heal") >= lay.pages * 8); // parked after the last bar page
  // On-bar swap in one flow: lift mend (slot 0), tap wolf's occupied slot.
  p.pickup(a, "mend", 0, t + 10_000);
  is(p.place(a, ArrangePlan.Target.bar(3), t + 10_300), Intent.PUT, "mend", 3);
  BarLoadout.Change sw = l.put("mend", 3);
  ok(sw.ok() && "swap".equals(sw.kind) && "wolf".equals(sw.other));
  ok(l.indexOf("mend") == 3 && l.indexOf("wolf") == 0);
  // An extreme slow tap (a full minute between lift and place) still
  // commits first try: lifts never expire.
  p.pickup(a, "heal", -1, t + 20_000);
  is(p.place(a, ArrangePlan.Target.bar(5), t + 80_000), Intent.PUT, "heal", 5);
  ok(l.put("heal", 5).ok() && l.indexOf("heal") == 5);
  // The list refreshes in place after a remove: the layout recomputed from
  // the changed model already shows slot 5 empty and heal parked — no
  // reopen, no stale copy anywhere in the chain.
  BarLoadout.Change rm = l.remove(5);
  ok(rm.ok() && "heal".equals(rm.spell));
  BarLoadout.Layout after = l.layout(List.of("mend", "heal", "wolf"), 7);
  ok(after.inBook == 1 && after.slots.get("heal") >= after.pages * 8);
  ok(!after.slots.containsValue(5)); // loadout slot 5's global position holds no spell anymore
  // ---------------------------------------------------------- arrange parity (re-lift guard reads)
  ArrangePlan ap = new ArrangePlan();
  ap.setArmed(a, true);
  ap.pickup(a, "wolf", 2, t);
  ok("wolf".equals(ap.liftedSpell(a)));
  ok(ap.liftedFrom(a) == 2);
  ap.place(a, ArrangePlan.Target.bar(4), t + 100);
  ok(ap.liftedSpell(a) == null);

  // ------------------------------------------- the duplicate-pickup signature (rig-caught W1/W3 pair)
  // Mike's W3 duplicate: an off-bar (list) lift whose tap fires twice on the
  // grid -> a re-lift, and the later bar-row tap is the real place.
  ok(DragWritePlan.isDuplicatePickup(-1, false, -1, "cure", "cure"));
  ok(!DragWritePlan.isDuplicatePickup(-1, true, 2, "cure", "cure")); // the place tap itself
  // An on-bar lift re-fired on its own slot -> a re-lift.
  ok(DragWritePlan.isDuplicatePickup(0, true, 0, "mend", "mend"));
  // The W1 flicker case: an on-bar lift, then a place onto a DIFFERENT slot
  // that happens to display the same spell after the restore -> NOT a
  // duplicate; the commit must run. (The first 0.3.9 rig pass failed here
  // when the guard trusted spell identity alone.)
  ok(!DragWritePlan.isDuplicatePickup(0, true, 1, "mend", "mend"));
  // A different spell under the finger is never a duplicate.
  ok(!DragWritePlan.isDuplicatePickup(-1, false, -1, "heal", "cure"));
  ok(!DragWritePlan.isDuplicatePickup(0, true, 0, "heal", "mend"));
  // liftedFrom tracks the origin through a drag-write lift.
  DragWritePlan dp = new DragWritePlan();
  dp.pickup(a, "cure", -1, t);
  ok(dp.liftedFrom(a) == -1 && "cure".equals(dp.liftedSpell(a)));
  dp.place(a, ArrangePlan.Target.bar(2), t + 100);
  ok(dp.liftedFrom(a) == Integer.MIN_VALUE);
  System.out.println("SpellbookPager039Test: " + n + " checks passed");
 }
}
