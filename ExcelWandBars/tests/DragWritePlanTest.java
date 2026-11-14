package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ArrangePlan;
import com.winkreyes.excelwandbars.core.DragWritePlan;
import com.winkreyes.excelwandbars.core.DragWritePlan.Intent;
import com.winkreyes.excelwandbars.core.DragWritePlan.Result;
import java.util.List;
import java.util.UUID;
/** 0.3.7: plain drags write — pickup/place/drop/drag intents for the unarmed screen. */
public class DragWritePlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Result r, Intent i, String spell, int slot){
  ok(r != null && r.intent == i && (spell == null ? r.spell == null : spell.equals(r.spell)) && r.slot == slot);
 }
 public static void main(String[]args){
  UUID a=UUID.randomUUID(), b=UUID.randomUUID();
  DragWritePlan p=new DragWritePlan();
  long t=100_000L;
  // Mike's Ward repro, click-carry dialect: a spell that is NOT on the bar is
  // lifted from the inventory grid (from = -1) and placed on a bar slot = PUT.
  p.pickup(a,"ward",-1,t);
  is(p.place(a,ArrangePlan.Target.bar(4),t+10),Intent.PUT,"ward",4);
  // The Wolf repro (all-spells view): same shape, another empty slot.
  p.pickup(a,"wolf",-1,t+1_000);
  is(p.place(a,ArrangePlan.Target.bar(6),t+1_010),Intent.PUT,"wolf",6);
  // An on-bar spell carried to another slot = PUT (the model swaps or moves).
  p.pickup(a,"mend",0,t+2_000);
  is(p.place(a,ArrangePlan.Target.bar(3),t+2_010),Intent.PUT,"mend",3);
  // Carried back onto its own slot: nothing to write.
  p.pickup(a,"mend",3,t+3_000);
  is(p.place(a,ArrangePlan.Target.bar(3),t+3_010),Intent.RETURN,"mend",3);
  // Dragged off the bar into the inventory: REMOVE (layout only).
  p.pickup(a,"mend",3,t+4_000);
  is(p.place(a,ArrangePlan.Target.outside(),t+4_010),Intent.REMOVE,"mend",3);
  // An off-bar spell "placed" outside never left the book: RETURN, no write.
  p.pickup(a,"ward",-1,t+5_000);
  is(p.place(a,ArrangePlan.Target.outside(),t+5_010),Intent.RETURN,"ward",-1);
  // The protected slot (wand / exit column) refuses every write.
  p.pickup(a,"mend",0,t+6_000);
  is(p.place(a,ArrangePlan.Target.protectedSlot(),t+6_010),Intent.RETURN,"mend",0);
  // Place with no lift recorded is inert at the plan level (the hook's
  // cursor path uses placeWith for that dialect instead).
  is(p.place(a,ArrangePlan.Target.bar(2),t+7_000),Intent.NONE,null,-1);
  // placeWith: the cursor already held the icon when the stream started.
  is(p.placeWith(a,"cure",-1,ArrangePlan.Target.bar(9),t+8_000),Intent.PUT,"cure",9);
  is(p.placeWith(a,"cure",9,ArrangePlan.Target.bar(9),t+9_000),Intent.RETURN,"cure",9);
  // Q-drop with a lift: on-bar removes, off-bar is inert.
  p.pickup(a,"heal",1,t+10_000);
  is(p.drop(a,t+10_010),Intent.REMOVE,"heal",1);
  p.pickup(a,"ward",-1,t+11_000);
  is(p.drop(a,t+11_010),Intent.RETURN,"ward",-1);
  // Q-drop hovering the icon (no lift): on-bar removes, off-bar is a no-op.
  is(p.dropSlot(a,"heal",1,t+12_000),Intent.REMOVE,"heal",1);
  is(p.dropSlot(a,"ward",-1,t+13_000),Intent.NONE,null,-1);
  // Whole-drag dialect: an on-bar drag lands on the FARTHEST bar target.
  is(p.drag(a,"mend",0,List.of(ArrangePlan.Target.bar(2),ArrangePlan.Target.bar(5)),t+14_000),Intent.PUT,"mend",5);
  // An off-bar drag lands on the FIRST bar target touched (no origin).
  is(p.drag(a,"wolf",-1,List.of(ArrangePlan.Target.bar(3),ArrangePlan.Target.bar(4)),t+15_000),Intent.PUT,"wolf",3);
  // A drag that touches the outside removes (on-bar) / returns (off-bar).
  is(p.drag(a,"mend",2,List.of(ArrangePlan.Target.bar(3),ArrangePlan.Target.outside()),t+16_000),Intent.REMOVE,"mend",2);
  is(p.drag(a,"wolf",-1,List.of(ArrangePlan.Target.outside()),t+17_000),Intent.RETURN,"wolf",-1);
  // A drag that only touches protected slots is inert; an empty drag is inert.
  is(p.drag(a,"mend",2,List.of(ArrangePlan.Target.protectedSlot()),t+18_000),Intent.RETURN,"mend",2);
  is(p.drag(a,"mend",2,List.of(),t+19_000),Intent.NONE,null,-1);
  // Dedupe: the same spell's terminal commit twice inside 500 ms counts once;
  // a different spell, or the same spell after the window, still writes.
  p.pickup(a,"boon",2,t+20_000);
  is(p.place(a,ArrangePlan.Target.bar(6),t+20_010),Intent.PUT,"boon",6);
  p.pickup(a,"boon",6,t+20_100);
  is(p.place(a,ArrangePlan.Target.bar(0),t+20_110),Intent.NONE,null,-1);
  p.pickup(a,"feed",3,t+20_200);
  is(p.place(a,ArrangePlan.Target.bar(0),t+20_210),Intent.PUT,"feed",0);
  p.pickup(a,"boon",0,t+21_000);
  is(p.place(a,ArrangePlan.Target.bar(6),t+21_010),Intent.PUT,"boon",6);
  // cancelLift (the read form opened) drops the pending gesture.
  p.pickup(a,"mend",0,t+22_000);
  p.cancelLift(a);
  is(p.place(a,ArrangePlan.Target.bar(4),t+22_010),Intent.NONE,null,-1);
  // Players are independent.
  p.pickup(b,"mend",0,t+23_000);
  is(p.place(b,ArrangePlan.Target.bar(1),t+23_010),Intent.PUT,"mend",1);
  ok(!p.hasLift(a));
  // forget + clear.
  p.pickup(a,"mend",0,t+24_000); p.forget(a);
  is(p.place(a,ArrangePlan.Target.bar(1),t+24_010),Intent.NONE,null,-1);
  p.pickup(a,"mend",0,t+25_000); p.clear();
  is(p.place(a,ArrangePlan.Target.bar(1),t+25_010),Intent.NONE,null,-1);
  // Confirmation throttle: first is due, an immediate second is not, one
  // after the gap is; a zero gap confirms every time.
  ok(p.confirmDue(a,t+30_000,1200));
  ok(!p.confirmDue(a,t+30_100,1200));
  ok(p.confirmDue(a,t+31_300,1200));
  ok(p.confirmDue(b,t+30_050,1200));
  ok(p.confirmDue(a,t+31_400,0));
  ok(p.confirmDue(a,t+31_401,0));
  // Recent-write window (the repaint restore in the hook).
  p.touch(a,t+40_000);
  ok(p.recentWrite(a,t+44_999,5000));
  ok(!p.recentWrite(a,t+45_001,5000));
  ok(!p.recentWrite(b,t+40_100,5000));
  System.out.println("DragWritePlanTest: "+n+" checks passed");
 }
}
