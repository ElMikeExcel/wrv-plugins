package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.QuitClosePlan;
import com.winkreyes.excelwandbars.core.QuitClosePlan.Decision;
/** 0.3.11: the logout close — an open bar under the joins-on-items policy closes at exit; everything else is left alone. */
public class QuitClosePlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 static void is(Decision d, Decision want){ ok(d == want); }
 public static void main(String[]args){
  // The defect exactly: policy on, tap-bar style, wand present, bar open at logout -> CLOSE.
  is(QuitClosePlan.decide(true, true, true, true), Decision.CLOSE);
  // Bar already down: nothing to close, nothing changes (ordinary logouts are untouched).
  is(QuitClosePlan.decide(true, true, true, false), Decision.LEAVE);
  // No wand at all (never activated / already deactivated): leave.
  is(QuitClosePlan.decide(true, true, false, true), Decision.LEAVE);
  is(QuitClosePlan.decide(true, true, false, false), Decision.LEAVE);
  // Styles that never open a bar on hold are not this policy's business.
  is(QuitClosePlan.decide(true, false, true, true), Decision.LEAVE);
  is(QuitClosePlan.decide(true, false, true, false), Decision.LEAVE);
  // Policy off (join-starts-on-items: false): logouts behave exactly as before 0.3.11.
  is(QuitClosePlan.decide(false, true, true, true), Decision.LEAVE);
  is(QuitClosePlan.decide(false, false, false, false), Decision.LEAVE);
  System.out.println("QuitClosePlanTest: "+n+" checks passed");
 }
}
