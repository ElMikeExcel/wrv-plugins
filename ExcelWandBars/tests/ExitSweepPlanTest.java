package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ExitSweepPlan;
import com.winkreyes.excelwandbars.core.LoadoutText;
import java.util.List;
/** 0.3.6: the exit-slot relocation sweep — what is foreign, what is left, what the player reads. */
public class ExitSweepPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // Foreign = a real item that is not a spell icon. Empty slots and icons are left alone.
  ok(!ExitSweepPlan.foreign(true,false));
  ok(!ExitSweepPlan.foreign(true,true));
  ok(!ExitSweepPlan.foreign(false,true));
  ok(ExitSweepPlan.foreign(false,false));
  // Accepted by the sink: nothing stays, whatever the clone reports.
  ok(ExitSweepPlan.leftover(10,true,10)==0);
  ok(ExitSweepPlan.leftover(10,true,0)==0);
  ok(ExitSweepPlan.leftover(1,true,1)==0);
  // Refused outright: the clone comes back whole, everything stays.
  ok(ExitSweepPlan.leftover(10,false,10)==10);
  ok(ExitSweepPlan.leftover(64,false,64)==64);
  // Partial take: the clone's leftover is what stays — items are conserved.
  ok(ExitSweepPlan.leftover(10,false,4)==4);
  ok(ExitSweepPlan.leftover(10,false,0)==0);
  ok(ExitSweepPlan.leftover(5,false,5-3)==2);
  // Defensive: a sink reporting nonsense never invents or destroys items.
  ok(ExitSweepPlan.leftover(10,false,99)==10);
  ok(ExitSweepPlan.leftover(10,false,-1)==10);
  ok(ExitSweepPlan.leftover(0,false,0)==0);
  // The one chat line: silent when nothing moved.
  ok(LoadoutText.sweptLine(List.of(),null,false)==null);
  ok(LoadoutText.sweptLine(List.of(),null,true)==null);
  ok(LoadoutText.sweptLine(null,null,false)==null);
  // One stack moved whole.
  ok("Moved Carved pumpkin out of your spell bar. It is in your inventory."
      .equals(LoadoutText.sweptLine(List.of("Carved pumpkin"),null,false)));
  // Several stacks: counted, not listed.
  ok("Moved 3 items out of your spell bar. They are in your inventory."
      .equals(LoadoutText.sweptLine(List.of("A","B","C"),null,false)));
  // Leftovers are admitted in the same line.
  ok("Moved Carved pumpkin out of your spell bar. It is in your inventory. Some would not fit and stayed in the bar."
      .equals(LoadoutText.sweptLine(List.of("Carved pumpkin"),null,true)));
  ok("Moved 2 items out of your spell bar. They are in your inventory. Some would not fit and stayed in the bar."
      .equals(LoadoutText.sweptLine(List.of("A","B"),"Candy",true)));
  // A partial-only move still gets its line, and it does not overclaim.
  ok("Moved part of the Candy out of your spell bar into your inventory. The rest stayed in the bar."
      .equals(LoadoutText.sweptLine(List.of(),"Candy",true)));
  System.out.println("ExitSweepPlanTest: "+n+" checks passed");
 }
}
