package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.PagerPlan;
/** 0.3.8: pager buttons — slots, visibility, wraparound flips, page state line.
 * 0.3.12: the BAR state line is axis-honest — "Hotbar page X of Y" (was "Page X of Y"). */
public class PagerPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // The settled slots: the last two of the grid's bottom row, back left, forward right.
  ok(PagerPlan.BACK_SLOT == 34 && PagerPlan.FORWARD_SLOT == 35);
  ok(PagerPlan.buttonAt(34) == PagerPlan.Button.BACK);
  ok(PagerPlan.buttonAt(35) == PagerPlan.Button.FORWARD);
  ok(PagerPlan.buttonAt(33) == PagerPlan.Button.NONE);
  ok(PagerPlan.buttonAt(36) == PagerPlan.Button.NONE);
  ok(PagerPlan.buttonAt(0) == PagerPlan.Button.NONE);
  ok(PagerPlan.buttonAt(8) == PagerPlan.Button.NONE);
  ok(PagerPlan.isButtonSlot(34) && PagerPlan.isButtonSlot(35) && !PagerPlan.isButtonSlot(27));
  // Buttons only when there is somewhere to flip to.
  ok(!PagerPlan.shows(0) && !PagerPlan.shows(1));
  ok(PagerPlan.shows(2) && PagerPlan.shows(3) && PagerPlan.shows(14));
  // Clamp: a wand shrinking under the open bar never strands the state.
  ok(PagerPlan.clamp(-1, 3) == 0);
  ok(PagerPlan.clamp(0, 3) == 0 && PagerPlan.clamp(1, 3) == 1 && PagerPlan.clamp(2, 3) == 2);
  ok(PagerPlan.clamp(5, 3) == 2);
  ok(PagerPlan.clamp(3, 0) == 0);
  // Flips wrap at both ends, exactly like the wand-click cycle.
  ok(PagerPlan.target(0, 3, 1) == 1);
  ok(PagerPlan.target(1, 3, 1) == 2);
  ok(PagerPlan.target(2, 3, 1) == 0);
  ok(PagerPlan.target(0, 3, -1) == 2);
  ok(PagerPlan.target(2, 3, -1) == 1);
  ok(PagerPlan.target(1, 3, -1) == 0);
  ok(PagerPlan.target(0, 2, -1) == 1 && PagerPlan.target(1, 2, 1) == 0);
  ok(PagerPlan.target(0, 1, 1) == 0 && PagerPlan.target(0, 1, -1) == 0);
  ok(PagerPlan.target(9, 3, 1) == 0); // out-of-range current page clamps first
  // The state line the forward button carries.
  ok(PagerPlan.stateLine(0, 1).equals("Hotbar page 1 of 1"));
  ok(PagerPlan.stateLine(1, 3).equals("Hotbar page 2 of 3"));
  ok(PagerPlan.stateLine(2, 3).equals("Hotbar page 3 of 3"));
  ok(PagerPlan.stateLine(7, 3).equals("Hotbar page 3 of 3"));
  ok(PagerPlan.backName().equals("◀ Back page"));
  ok(PagerPlan.forwardName().equals("Next page ▶"));
  System.out.println("PagerPlanTest: "+n+" checks passed");
 }
}
