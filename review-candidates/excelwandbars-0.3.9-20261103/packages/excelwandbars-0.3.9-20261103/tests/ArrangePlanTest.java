package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ArrangePlan;
import com.winkreyes.excelwandbars.core.ArrangePlan.Intent;
import com.winkreyes.excelwandbars.core.ArrangePlan.Result;
import com.winkreyes.excelwandbars.core.ArrangePlan.Target;
import java.util.List;
import java.util.UUID;
/** 0.3.5 design 2.3: the arrange intent interpreter (both input dialects). */
public class ArrangePlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Result r, Intent i, String spell, int slot){ok(r.intent==i&&java.util.Objects.equals(r.spell,spell)&&r.slot==slot);}
 public static void main(String[]args){
  UUID a=UUID.randomUUID(), b=UUID.randomUUID();
  ArrangePlan p=new ArrangePlan();
  // Not armed: every input is NONE.
  p.pickup(a,"mend",3,1000);
  is(p.place(a,Target.outside(),1100),Intent.NONE,null,-1);
  is(p.dropSlot(a,"mend",3,1200),Intent.NONE,null,-1);
  is(p.drag(a,"mend",3,List.of(Target.outside()),1300),Intent.NONE,null,-1);
  p.setArmed(a,true);
  // Pickup then place outside the bar row: REMOVE for the lifted spell and its slot.
  p.pickup(a,"mend",3,2000);
  is(p.place(a,Target.outside(),2100),Intent.REMOVE,"mend",3);
  // Dedupe: the protocol twin of the same commit inside 500 ms is swallowed,
  // and the same commit after the window is honored.
  p.pickup(a,"mend",3,2200);
  is(p.place(a,Target.outside(),2300),Intent.NONE,null,-1);
  p.pickup(a,"mend",3,3000);
  is(p.place(a,Target.outside(),3100),Intent.REMOVE,"mend",3);
  // A different spell inside the window is NOT deduped.
  p.pickup(a,"heal",1,3200);
  is(p.place(a,Target.outside(),3300),Intent.REMOVE,"heal",1);
  // Place back on its own slot: RETURN. Place on another bar slot: MOVE (page 2 col 0 = slot 7).
  p.pickup(a,"boon",2,5000);
  is(p.place(a,Target.bar(2),5100),Intent.RETURN,"boon",2);
  p.pickup(a,"boon",2,6000);
  is(p.place(a,Target.bar(7),6100),Intent.MOVE,"boon",7);
  // Protected target (wand slot, exit column): RETURN, lift forgotten.
  p.pickup(a,"boon",2,7000);
  is(p.place(a,Target.protectedSlot(),7100),Intent.RETURN,"boon",2);
  ok(!p.hasLift(a));
  // Place with nothing lifted: NONE.
  is(p.place(a,Target.outside(),8000),Intent.NONE,null,-1);
  // Drop with a lift: REMOVE. Drop with no lift: NONE.
  p.pickup(a,"mend",0,10000);
  is(p.drop(a,10100),Intent.REMOVE,"mend",0);
  is(p.drop(a,10200),Intent.NONE,null,-1);
  // Drop straight off the icon's slot (Q over the slot): REMOVE.
  is(p.dropSlot(a,"shackle",5,12000),Intent.REMOVE,"shackle",5);
  // A second lift replaces the first.
  p.pickup(a,"mend",1,14000);
  p.pickup(a,"heal",4,14100);
  is(p.place(a,Target.outside(),14200),Intent.REMOVE,"heal",4);
  // Drag: any outside target in the set means REMOVE.
  is(p.drag(a,"mend",3,List.of(Target.bar(4),Target.outside()),20000),Intent.REMOVE,"mend",3);
  // Drag across bar slots only: MOVE to the farthest bar target.
  is(p.drag(a,"mend",1,List.of(Target.bar(2),Target.bar(6),Target.bar(3)),22000),Intent.MOVE,"mend",6);
  // Drag that ends where it started: RETURN. Drag over protected slots only: RETURN.
  is(p.drag(a,"mend",3,List.of(Target.bar(3)),24000),Intent.RETURN,"mend",3);
  is(p.drag(a,"mend",3,List.of(Target.protectedSlot()),26000),Intent.RETURN,"mend",3);
  // Drag with an empty target set: NONE.
  is(p.drag(a,"mend",3,List.of(),28000),Intent.NONE,null,-1);
  // Disarming forgets the lift; the other player is unaffected.
  p.setArmed(b,true);
  p.pickup(a,"mend",2,30000);
  p.setArmed(a,false);
  ok(!p.hasLift(a));
  is(p.place(a,Target.outside(),30100),Intent.NONE,null,-1);
  p.pickup(b,"mend",2,30200);
  is(p.place(b,Target.outside(),30300),Intent.REMOVE,"mend",2);
  // cancelLift (the read form opened) forgets the lift without an intent.
  p.setArmed(a,true);
  p.pickup(a,"mend",2,40000);
  p.cancelLift(a);
  is(p.place(a,Target.outside(),40100),Intent.NONE,null,-1);
  System.out.println("PASS "+n+" arrange plan checks");
 }
}
