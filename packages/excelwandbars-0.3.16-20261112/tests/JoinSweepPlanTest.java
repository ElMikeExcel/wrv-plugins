package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.JoinSweepPlan;
/** 0.3.11: the join sweep — only the final guard check of a join, only when the guard leaves an open bar, only once. */
public class JoinSweepPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // All favorable: final check, guard left, bar open, not yet swept -> sweep.
  ok(JoinSweepPlan.shouldSweep(true, true, true, true, true, false));
  // Early checks (1.5 s / 4 s) never sweep — the guard may still close the bar.
  ok(!JoinSweepPlan.shouldSweep(true, true, false, true, true, false));
  // Once per join: a second final-path call does not sweep again.
  ok(!JoinSweepPlan.shouldSweep(true, true, true, true, true, true));
  // Guard closing the bar: no sweep (the close restores items; CompassGuard tidies).
  ok(!JoinSweepPlan.shouldSweep(true, true, true, false, true, false));
  // Bar down: nothing in the pages is presented; nothing to evict at join.
  ok(!JoinSweepPlan.shouldSweep(true, true, true, true, false, false));
  // Either switch off disables it: guard policy off, or the exit sweep off.
  ok(!JoinSweepPlan.shouldSweep(false, true, true, true, true, false));
  ok(!JoinSweepPlan.shouldSweep(true, false, true, true, true, false));
  // Everything off / nothing favorable.
  ok(!JoinSweepPlan.shouldSweep(false, false, false, false, false, false));
  System.out.println("JoinSweepPlanTest: "+n+" checks passed");
 }
}
