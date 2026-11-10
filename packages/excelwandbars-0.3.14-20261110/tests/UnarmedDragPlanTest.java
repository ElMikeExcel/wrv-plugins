package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.UnarmedDragPlan;
import com.winkreyes.excelwandbars.core.UnarmedDragPlan.Verdict;
import java.util.UUID;
/** 0.3.6: the honest-drag gate — unarmed icon gestures cancel, the signpost is throttled. */
public class UnarmedDragPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  UUID a=UUID.randomUUID(), b=UUID.randomUUID();
  UnarmedDragPlan p=new UnarmedDragPlan();
  long t0=100_000L, gap=UnarmedDragPlan.MIN_SIGNPOST_GAP_MS;
  // Armed gestures belong to Arrange and always pass, icon or not.
  ok(p.judge(a,true,true,t0)==Verdict.PASS);
  ok(p.judge(a,true,false,t0)==Verdict.PASS);
  // Unarmed gestures that touch no icon pass.
  ok(p.judge(a,false,false,t0)==Verdict.PASS);
  // First unarmed icon gesture: cancel + signpost.
  ok(p.judge(a,false,true,t0)==Verdict.CANCEL_AND_SIGNPOST);
  // Same session: cancels stay quiet.
  ok(p.judge(a,false,true,t0+100)==Verdict.CANCEL);
  ok(p.judge(a,false,true,t0+5_000)==Verdict.CANCEL);
  // A new session inside the hard gap still stays quiet (repaint flicker).
  p.screenClosed(a);
  ok(p.judge(a,false,true,t0+gap-1)==Verdict.CANCEL);
  // That quiet cancel did not consume the next session's signpost: once the
  // gap has passed, a fresh session's first gesture signposts again...
  ok(p.judge(a,false,true,t0+gap+1)==Verdict.CANCEL_AND_SIGNPOST);
  ok(p.judge(a,false,true,t0+gap+2)==Verdict.CANCEL);
  // ...and the cycle repeats on the next session after the next gap.
  p.screenClosed(a);
  ok(p.judge(a,false,true,t0+2*gap+2)==Verdict.CANCEL_AND_SIGNPOST);
  // Players are independent: b's first gesture signposts while a is throttled.
  ok(p.judge(b,false,true,t0+10)==Verdict.CANCEL_AND_SIGNPOST);
  ok(p.judge(b,false,true,t0+20)==Verdict.CANCEL);
  // forget (quit) resets both the session flag and the time floor.
  p.forget(a);
  ok(p.judge(a,false,true,t0+30)==Verdict.CANCEL_AND_SIGNPOST);
  // clear resets everyone.
  p.clear();
  ok(p.judge(b,false,true,t0+40)==Verdict.CANCEL_AND_SIGNPOST);
  System.out.println("UnarmedDragPlanTest: "+n+" checks passed");
 }
}
