package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.JoinGuardPlan;
import com.winkreyes.excelwandbars.core.JoinGuardPlan.Decision;
/** 0.3.8: the login wand-mode trap — a restored bar inside the grace window closes; engagement exempts. */
public class JoinGuardPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Decision d, Decision want){ ok(d == want); }
 public static void main(String[]args){
  long j = 100_000L, grace = 8_000L;
  // Mike's trap exactly: bar restored open at join, no engagement -> CLOSE.
  is(JoinGuardPlan.decide(true, true, true, j, j + 1_500, 0, grace), Decision.CLOSE);
  is(JoinGuardPlan.decide(true, true, true, j, j + 4_000, 0, grace), Decision.CLOSE);
  is(JoinGuardPlan.decide(true, true, true, j, j + 8_000, 0, grace), Decision.CLOSE); // edge: still inside
  // Server churn stamps inside the first second prove nothing -> still CLOSE.
  is(JoinGuardPlan.decide(true, true, true, j, j + 3_000, j + 500, grace), Decision.CLOSE);
  is(JoinGuardPlan.decide(true, true, true, j, j + 3_000, j + JoinGuardPlan.CHURN_MS, grace), Decision.CLOSE);
  // A deliberate pick / open after the churn window exempts the session.
  is(JoinGuardPlan.decide(true, true, true, j, j + 3_000, j + JoinGuardPlan.CHURN_MS + 1, grace), Decision.LEAVE);
  is(JoinGuardPlan.decide(true, true, true, j, j + 5_000, j + 2_500, grace), Decision.LEAVE);
  // Past the grace window the guard stops policing entirely.
  is(JoinGuardPlan.decide(true, true, true, j, j + 8_001, 0, grace), Decision.LEAVE);
  is(JoinGuardPlan.decide(true, true, true, j, j + 60_000, 0, grace), Decision.LEAVE);
  // Nothing to fix: bar down, other styles, feature off, no join stamp (reload).
  is(JoinGuardPlan.decide(true, true, false, j, j + 1_500, 0, grace), Decision.LEAVE);
  is(JoinGuardPlan.decide(true, false, true, j, j + 1_500, 0, grace), Decision.LEAVE);
  is(JoinGuardPlan.decide(false, true, true, j, j + 1_500, 0, grace), Decision.LEAVE);
  is(JoinGuardPlan.decide(true, true, true, 0, j + 1_500, 0, grace), Decision.LEAVE);
  // A pre-join activity stamp (previous session) is not engagement.
  is(JoinGuardPlan.decide(true, true, true, j, j + 2_000, j - 5_000, grace), Decision.CLOSE);
  System.out.println("JoinGuardPlanTest: "+n+" checks passed");
 }
}
