package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.SlotGuardPlan;
import com.winkreyes.excelwandbars.core.SlotGuardPlan.ClickKind;
/**
 * 0.3.12: the wand-bar slot guard (Mike's Nov 7 2026 fight intake:
 * arrows occupied a protected wand slot mid-combat). One case block per
 * entry path in the BUILD-RECORD audit table. Verdicts are computed
 * only for FOREIGN items — a real item that is not a spell icon, not a
 * pager button and not a wand; icon gestures belong to the drag-write
 * and Arrange interpreters and must never be decided here.
 */
public class SlotGuardPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // ------------------------------------------------ foreign predicate
  ok(SlotGuardPlan.foreign(false, false, false, false));        // a plain real item (the arrows)
  ok(!SlotGuardPlan.foreign(true, false, false, false));        // empty slot
  ok(!SlotGuardPlan.foreign(false, true, false, false));        // spell icon: the presentation itself
  ok(!SlotGuardPlan.foreign(false, false, true, false));        // pager button: PagerHook owns it
  ok(!SlotGuardPlan.foreign(false, false, false, true));        // a wand item
  ok(!SlotGuardPlan.foreign(true, true, true, true));           // empty trumps everything
  // ------------------------------------------------ guard context off: nothing is ever blocked or swept
  ok(!SlotGuardPlan.blocksClick(false, ClickKind.PLACE, true, false, true, false));
  ok(!SlotGuardPlan.blocksDrag(false, true, true));
  ok(!SlotGuardPlan.pickupSweeps(false));
  ok(!SlotGuardPlan.flipSweeps(false));
  // ------------------------------------------------ PLACE (cursor stack lands in the clicked slot)
  ok(SlotGuardPlan.blocksClick(true, ClickKind.PLACE, true, false, true, false));   // foreign cursor -> presented slot: BLOCK (the give/click entry)
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.PLACE, false, false, true, false)); // foreign cursor -> some other inventory: not the bar's business
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.PLACE, true, false, false, false)); // an icon on the cursor: the interpreters' case
  // ------------------------------------------------ SWAP_WITH_CURSOR
  ok(SlotGuardPlan.blocksClick(true, ClickKind.SWAP_CURSOR, true, false, true, false));  // foreign stack swaps INTO a presented slot: BLOCK
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.SWAP_CURSOR, true, true, false, false)); // icon cursor onto a foreign slot swaps the foreign item OUT: allowed (the sweep relocates it)
  // ------------------------------------------------ HOTBAR_SWAP (number key) — both directions enter a presented slot
  ok(SlotGuardPlan.blocksClick(true, ClickKind.HOTBAR_SWAP, true, false, true, true));   // foreign hotbar item -> presented grid slot: BLOCK
  ok(SlotGuardPlan.blocksClick(true, ClickKind.HOTBAR_SWAP, false, true, false, true));  // foreign grid item -> presented hotbar slot (clicked side outside): BLOCK
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.HOTBAR_SWAP, true, false, false, true)); // hotbar side is an icon: the interpreters already swallow it; never foreign here
  // ------------------------------------------------ SHIFT_MOVE (shift-click inside the bar's own screen)
  ok(SlotGuardPlan.blocksClick(true, ClickKind.SHIFT_MOVE, true, true, false, false));   // foreign stack in the grid shift-clicks toward the bar row: BLOCK
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.SHIFT_MOVE, true, false, false, false)); // shift-clicking an icon: the interpreters' case (icon != foreign)
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.SHIFT_MOVE, false, true, false, false)); // clicked outside the presentation: not this guard's context
  // ------------------------------------------------ OTHER actions never place into a slot
  ok(!SlotGuardPlan.blocksClick(true, ClickKind.OTHER, true, true, true, true)); // pickups/drops/collects only remove; exit invariant + interpreters keep their rules
  ok(!SlotGuardPlan.blocksClick(true, null, true, false, true, false));          // an unmapped action is never blocked on speculation
  // ------------------------------------------------ DRAG
  ok(SlotGuardPlan.blocksDrag(true, true, true));    // foreign stack dragged across a presented slot: BLOCK whole (the click-drag entry)
  ok(!SlotGuardPlan.blocksDrag(true, true, false));  // foreign drag that never touches the presentation: allowed
  ok(!SlotGuardPlan.blocksDrag(true, false, true));  // an icon drag: drag-write / Arrange own it, H1 untouched
  // ------------------------------------------------ PICKUP (the fight-intake path)
  ok(SlotGuardPlan.pickupSweeps(true));              // bar open: the pickup is kept, a presented sweep is scheduled
  // ------------------------------------------------ PAGE FLIP (the persistence path)
  ok(SlotGuardPlan.flipSweeps(true));                // before a WandBars-driven save, foreign items are swept out (never baked in)
  System.out.println("SlotGuardPlanTest: "+n+" checks passed");
 }
}
