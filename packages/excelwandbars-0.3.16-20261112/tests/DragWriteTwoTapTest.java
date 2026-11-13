package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ArrangePlan;
import com.winkreyes.excelwandbars.core.DragWritePlan;
import com.winkreyes.excelwandbars.core.DragWritePlan.Intent;
import com.winkreyes.excelwandbars.core.DragWritePlan.Result;
import java.util.List;
import java.util.UUID;
/**
 * 0.3.8: the two-tap write path, at plan level (the hook half — terminal
 * commits surviving a screen-state flicker, lifts surviving a failed
 * hold-read — is proven on the rig; the plan half pinned here is that the
 * gesture itself never expires and never double-writes):
 * Mike's live repro was tap-pickup, tap-place onto an OCCUPIED slot, where
 * the put intermittently failed while removes landed instantly.
 */
public class DragWriteTwoTapTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Result r, Intent i, String spell, int slot){
  ok(r != null && r.intent == i && (spell == null ? r.spell == null : spell.equals(r.spell)) && r.slot == slot);
 }
 public static void main(String[]args){
  UUID a=UUID.randomUUID(), b=UUID.randomUUID();
  DragWritePlan p=new DragWritePlan();
  long t=200_000L;
  // Two-tap put onto an occupied slot: wolf lifted off-bar, placed on the
  // slot heal occupies (slot 3). The plan's PUT is the write; BarLoadout.put
  // owns the swap (BarLoadoutNewTest pins the model side).
  p.pickup(a,"wolf",-1,t);
  is(p.place(a,ArrangePlan.Target.bar(3),t+120),Intent.PUT,"wolf",3);
  // The same gesture with a long pause between the taps (a Bedrock press
  // held past the 800 ms hold-read threshold, a player thinking): the lift
  // has no expiry — the second tap still writes, first try.
  p.pickup(a,"mend",0,t+10_000);
  is(p.place(a,ArrangePlan.Target.bar(3),t+11_500),Intent.PUT,"mend",3);
  p.pickup(a,"wolf",-1,t+20_000);
  is(p.place(a,ArrangePlan.Target.bar(5),t+29_000),Intent.PUT,"wolf",5);
  // The dialect that begins mid-carry (cursor already holds the icon) puts
  // onto an occupied slot the same way.
  is(p.placeWith(a,"cure",-1,ArrangePlan.Target.bar(3),t+40_000),Intent.PUT,"cure",3);
  // A duplicated terminal commit for the same put inside the dedupe window
  // counts once (the "swaps back" double-fire shape): the echo is NONE, so
  // nothing can undo the landed write.
  p.pickup(a,"boon",-1,t+50_000);
  is(p.place(a,ArrangePlan.Target.bar(2),t+50_100),Intent.PUT,"boon",2);
  is(p.placeWith(a,"boon",-1,ArrangePlan.Target.bar(2),t+50_300),Intent.NONE,null,-1);
  // ...but the same spell written again after the window is a real write.
  p.pickup(a,"boon",2,t+51_000);
  is(p.place(a,ArrangePlan.Target.bar(4),t+51_100),Intent.PUT,"boon",4);
  // Swap shape: the spell now occupying the target goes through the same
  // two taps back — each direction is one PUT, neither is swallowed.
  p.pickup(a,"heal",3,t+60_000);
  is(p.place(a,ArrangePlan.Target.bar(0),t+60_100),Intent.PUT,"heal",0);
  p.pickup(a,"mend",0,t+61_000);
  is(p.place(a,ArrangePlan.Target.bar(3),t+61_100),Intent.PUT,"mend",3);
  // Drag-off-to-remove still clears (layout only): lift the on-bar spell,
  // let go outside the bar row.
  p.pickup(a,"wolf",5,t+70_000);
  is(p.place(a,ArrangePlan.Target.outside(),t+70_100),Intent.REMOVE,"wolf",5);
  // Whole-drag dialect off the bar clears the same way.
  is(p.drag(a,"heal",0,List.of(ArrangePlan.Target.outside()),t+80_000),Intent.REMOVE,"heal",0);
  // One player's long pause never eats another player's gesture.
  p.pickup(a,"ward",-1,t+90_000);
  p.pickup(b,"ward",-1,t+90_050);
  is(p.place(b,ArrangePlan.Target.bar(1),t+90_100),Intent.PUT,"ward",1);
  is(p.place(a,ArrangePlan.Target.bar(2),t+95_000),Intent.PUT,"ward",2);
  System.out.println("DragWriteTwoTapTest: "+n+" checks passed");
 }
}
