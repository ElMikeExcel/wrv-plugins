package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.BookHoldPlan;
import java.util.UUID;
/** 0.3.5 design 6.1/6.3: the crouch + hold book gate on Bukkit-shaped signals. */
public class BookHoldPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static final long H=400;
 public static void main(String[]args){
  UUID a=UUID.randomUUID(), b=UUID.randomUUID();
  BookHoldPlan g=new BookHoldPlan();
  // Cadence like a real touch client: fires on the signal past the threshold.
  ok(!g.signal(a,1000,BookHoldPlan.USE_AIR,true,true,H));
  ok(g.claimed(a));
  ok(!g.signal(a,1100,BookHoldPlan.USE_AIR,true,true,H));
  ok(g.signal(a,1450,BookHoldPlan.USE_AIR,true,true,H));
  // Fire-once: continued signals inside the burst never refire.
  ok(!g.signal(a,1500,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g.signal(a,1900,BookHoldPlan.USE_AIR,true,true,H));
  // Re-arm only after silence over 500 ms: this signal restarts, the next burst fires.
  ok(!g.signal(a,2500,BookHoldPlan.USE_AIR,true,true,H));
  ok(g.signal(a,2950,BookHoldPlan.USE_AIR,true,true,H));
  // Sneak up ends everything; a fresh crouch-first press fires again.
  g.sneakUp(a);
  ok(!g.claimed(a));
  ok(!g.signal(a,4000,BookHoldPlan.USE_AIR,true,true,H));
  ok(g.signal(a,4400,BookHoldPlan.USE_AIR,true,true,H));
  // Exactly at the threshold fires; 399 ms does not.
  BookHoldPlan g2=new BookHoldPlan();
  ok(!g2.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g2.signal(b,399,BookHoldPlan.USE_AIR,true,true,H));
  ok(g2.signal(b,400,BookHoldPlan.USE_AIR,true,true,H));
  // Press-first belongs to hold-to-read: no burst, no claim, ever, for that press.
  BookHoldPlan g3=new BookHoldPlan();
  ok(!g3.signal(b,0,BookHoldPlan.USE_AIR,true,false,H));
  ok(!g3.signal(b,500,BookHoldPlan.USE_AIR,true,false,H));
  ok(!g3.claimed(b));
  ok(g3.scheduledCheck(b,600,true,H)==BookHoldPlan.Check.DEAD);
  // Gap over 450 ms kills the burst; a sneak-first signal restarts it.
  BookHoldPlan g4=new BookHoldPlan();
  ok(!g4.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g4.signal(b,451,BookHoldPlan.USE_AIR,true,true,H));
  ok(g4.signal(b,851,BookHoldPlan.USE_AIR,true,true,H));
  // Gap death without sneak-first clears the state entirely.
  BookHoldPlan g5=new BookHoldPlan();
  ok(!g5.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g5.signal(b,451,BookHoldPlan.USE_AIR,true,false,H));
  ok(!g5.claimed(b));
  // Aiming at a block mid-hold latches the burst disqualified (the ice case);
  // tilting to the sky inside the same press does not revive it, a fresh press does.
  BookHoldPlan g6=new BookHoldPlan();
  ok(!g6.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g6.signal(b,100,BookHoldPlan.USE_BLOCK,true,true,H));
  ok(!g6.signal(b,500,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g6.signal(b,900,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g6.signal(b,1500,BookHoldPlan.USE_AIR,true,true,H)); // silence over 500: restart
  ok(g6.signal(b,1950,BookHoldPlan.USE_AIR,true,true,H));
  // Sparse cadence still qualifies through the scheduled check (design 6.3b).
  BookHoldPlan g7=new BookHoldPlan();
  ok(!g7.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g7.signal(b,250,BookHoldPlan.USE_AIR,true,true,H));
  ok(g7.scheduledCheck(b,400,true,H)==BookHoldPlan.Check.FIRE);
  // Sneak released before the check: no fire, state gone.
  BookHoldPlan g8=new BookHoldPlan();
  ok(!g8.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(g8.scheduledCheck(b,400,false,H)==BookHoldPlan.Check.DEAD);
  ok(!g8.claimed(b));
  // Freshness fails by a hair at the check: exactly one GRACE, then DEAD.
  BookHoldPlan g9=new BookHoldPlan();
  ok(!g9.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(!g9.signal(b,50,BookHoldPlan.USE_AIR,true,true,H));
  ok(g9.scheduledCheck(b,600,true,H)==BookHoldPlan.Check.GRACE);
  ok(g9.scheduledCheck(b,750,true,H)==BookHoldPlan.Check.DEAD);
  ok(!g9.claimed(b));
  // A check before the threshold is DEAD but harmless: the burst lives on.
  BookHoldPlan g10=new BookHoldPlan();
  ok(!g10.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  ok(g10.scheduledCheck(b,200,true,H)==BookHoldPlan.Check.DEAD);
  ok(g10.claimed(b));
  ok(g10.signal(b,400,BookHoldPlan.USE_AIR,true,true,H));
  // Cancel clears everything (wand tap, other inventory, quit).
  BookHoldPlan g11=new BookHoldPlan();
  ok(!g11.signal(b,0,BookHoldPlan.USE_AIR,true,true,H));
  g11.cancel(b);
  ok(!g11.claimed(b));
  ok(g11.burstStart(b)==0);
  ok(g11.scheduledCheck(b,500,true,H)==BookHoldPlan.Check.DEAD);
  // Not sneaking at all: no burst.
  BookHoldPlan g12=new BookHoldPlan();
  ok(!g12.signal(b,0,BookHoldPlan.USE_AIR,false,true,H));
  ok(!g12.claimed(b));
  System.out.println("PASS "+n+" book hold plan checks");
 }
}
