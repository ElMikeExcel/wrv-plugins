package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.HoldReadPlan;
import java.util.UUID;
/** 0.3.5 design 3.3: in-screen hold-to-read disambiguation. */
public class HoldReadPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static final long H=800;
 public static void main(String[]args){
  UUID a=UUID.randomUUID();
  HoldReadPlan r=new HoldReadPlan();
  // Silence for the full window reads the lifted spell, at exactly the deadline too.
  r.lift(a,"mend",1000);
  ok(r.decide(a,1799,H)==null);
  ok("mend".equals(r.decide(a,1800,H)));
  // It fires exactly once.
  ok(r.decide(a,1900,H)==null);
  // A commit inside the window cancels the read (the tap-tap dialect).
  HoldReadPlan r2=new HoldReadPlan();
  r2.lift(a,"mend",1000);
  r2.commit(a);
  ok(r2.decide(a,2000,H)==null);
  ok(!r2.pending(a));
  // Screen close cancels. Bar close cancels.
  HoldReadPlan r3=new HoldReadPlan();
  r3.lift(a,"mend",1000);
  r3.screenClosed(a);
  ok(r3.decide(a,2000,H)==null);
  r3.lift(a,"mend",3000);
  r3.barClosed(a);
  ok(r3.decide(a,4000,H)==null);
  // A second lift replaces the first: the new spell reads, timed from the new lift.
  HoldReadPlan r4=new HoldReadPlan();
  r4.lift(a,"mend",1000);
  r4.lift(a,"heal",1500);
  ok(r4.decide(a,2000,H)==null);
  ok("heal".equals(r4.decide(a,2300,H)));
  // No lift: nothing to read.
  HoldReadPlan r5=new HoldReadPlan();
  ok(r5.decide(a,99999,H)==null);
  // A lift after a fired read starts fresh.
  r.lift(a,"boon",5000);
  ok("boon".equals(r.decide(a,5800,H)));
  System.out.println("PASS "+n+" hold read plan checks");
 }
}
