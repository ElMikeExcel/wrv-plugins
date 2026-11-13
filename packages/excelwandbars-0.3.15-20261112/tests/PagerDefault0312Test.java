package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.PagerPlan;
/**
 * 0.3.12: the proper pager build (Mike, Nov 7 2026: "Config fix tonight,
 * proper build after"). The code default is the LIST axis; an explicit
 * config value still wins; the lore names the axis it turns.
 *
 * The regression this pins: on 0.3.11 the live config carried NO tap-bar
 * section at all, PagerHook defaulted page-target to "auto", and
 * PagerPlan.autoAxis defaulted an untaught session to BAR — so the
 * spell inventory's Next button paged the HOTBAR under a lore line
 * ("Page 1 of 3") that never said so (TAPBAR-PAGING-INVESTIGATION.md).
 */
public class PagerDefault0312Test {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // ---- resolveTarget: absent / unset / blank / junk all land on LIST.
  ok(PagerPlan.resolveTarget(null).equals("list"));      // section absent or key unset
  ok(PagerPlan.resolveTarget("").equals("list"));
  ok(PagerPlan.resolveTarget("   ").equals("list"));
  ok(PagerPlan.resolveTarget("bogus").equals("list"));
  // ---- resolveTarget: an explicitly set value still wins, verbatim.
  ok(PagerPlan.resolveTarget("list").equals("list"));
  ok(PagerPlan.resolveTarget("bar").equals("bar"));
  ok(PagerPlan.resolveTarget("auto").equals("auto"));
  ok(PagerPlan.resolveTarget(" BAR ").equals("bar"));    // case/space tolerant, like the old loader
  ok(PagerPlan.resolveTarget("List").equals("list"));
  // ---- axisForTarget under the code default ("list"): pages the list
  // whenever the list can page — even after the player paged the bar by
  // hand (the pin is the default's whole point), and from a cold start.
  ok(PagerPlan.axisForTarget("list", null, 3, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.axisForTarget("list", PagerPlan.Axis.BAR, 3, 2) == PagerPlan.Axis.LIST);
  // ---- axisForTarget under an explicit "bar" pin: pages the bar.
  ok(PagerPlan.axisForTarget("bar", null, 3, 2) == PagerPlan.Axis.BAR);
  ok(PagerPlan.axisForTarget("bar", PagerPlan.Axis.LIST, 3, 2) == PagerPlan.Axis.BAR);
  // ---- a pinned axis with nowhere to go never leaves inert buttons:
  // the other axis takes it (and its lore names that axis honestly).
  ok(PagerPlan.axisForTarget("list", null, 3, 1) == PagerPlan.Axis.BAR);
  ok(PagerPlan.axisForTarget("bar", null, 1, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.axisForTarget("list", null, 1, 1) == PagerPlan.Axis.LIST); // neither can page: pin stands
  ok(PagerPlan.axisForTarget("bar", null, 1, 1) == PagerPlan.Axis.BAR);
  // ---- "auto" still follows the last paging used, but STARTS on the
  // list (the retired trap was the BAR start, not the following).
  ok(PagerPlan.axisForTarget("auto", null, 3, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.axisForTarget("auto", PagerPlan.Axis.BAR, 3, 2) == PagerPlan.Axis.BAR);
  ok(PagerPlan.axisForTarget("auto", PagerPlan.Axis.LIST, 3, 2) == PagerPlan.Axis.LIST);
  ok(PagerPlan.axisForTarget("auto", null, 3, 1) == PagerPlan.Axis.BAR);  // single-page list never takes it
  ok(PagerPlan.axisForTarget("auto", null, 1, 2) == PagerPlan.Axis.LIST); // single-page bar never takes it
  // ---- axis-honest lore: the two lines name different axes, always.
  ok(PagerPlan.stateLine(0, 3).equals("Hotbar page 1 of 3"));
  ok(PagerPlan.stateLine(2, 3).equals("Hotbar page 3 of 3"));
  ok(PagerPlan.listStateLine(0, 3).equals("List page 1 of 3"));
  ok(PagerPlan.listStateLine(2, 3).equals("List page 3 of 3"));
  ok(!PagerPlan.stateLine(1, 3).equals(PagerPlan.listStateLine(1, 3)));
  System.out.println("PagerDefault0312Test: "+n+" checks passed");
 }
}
