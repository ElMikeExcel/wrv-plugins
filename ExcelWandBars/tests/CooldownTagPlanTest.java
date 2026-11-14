package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.CooldownTagPlan;
/** 0.3.10: the name-tag countdown — threshold edges, m:ss format, suffix strip/restore shape. */
public class CooldownTagPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // Threshold: annotate only while the true time-to-cast EXCEEDS the threshold.
  ok(!CooldownTagPlan.shouldTag(null, 60));            // castable / unknown: never
  ok(!CooldownTagPlan.shouldTag(0L, 60));
  ok(!CooldownTagPlan.shouldTag(-5L, 60));
  ok(!CooldownTagPlan.shouldTag(59_999L, 60));
  ok(!CooldownTagPlan.shouldTag(60_000L, 60));         // exactly 60s at a 60s threshold: NO tag
  ok(CooldownTagPlan.shouldTag(60_001L, 60));          // 1ms over: tag
  ok(CooldownTagPlan.shouldTag(61_000L, 60));          // 61s: tag
  ok(CooldownTagPlan.shouldTag(240_000L, 60));         // Familiar's 4 minutes: tag
  ok(CooldownTagPlan.shouldTag(1_800_000L, 60));       // Repair Item's 30 minutes: tag
  ok(!CooldownTagPlan.shouldTag(120_000L, 120));       // a raised threshold moves the edge with it
  ok(CooldownTagPlan.shouldTag(120_001L, 120));
  ok(!CooldownTagPlan.shouldTag(0L, 0));               // threshold 0 still needs time left
  ok(CooldownTagPlan.shouldTag(1L, 0));
  ok(!CooldownTagPlan.shouldTag(3_600_001L, 7200));    // clamped configs can't invert the rule
  // Seconds shown: ceil, exactly like Magic's own hotbar number.
  ok(CooldownTagPlan.secondsShown(0L) == 0);
  ok(CooldownTagPlan.secondsShown(1L) == 1);
  ok(CooldownTagPlan.secondsShown(999L) == 1);
  ok(CooldownTagPlan.secondsShown(1000L) == 1);
  ok(CooldownTagPlan.secondsShown(1001L) == 2);
  ok(CooldownTagPlan.secondsShown(60_000L) == 60);
  ok(CooldownTagPlan.secondsShown(60_001L) == 61);
  ok(CooldownTagPlan.secondsShown(240_000L) == 240);
  ok(CooldownTagPlan.secondsShown(3_599_000L) == 3599);
  // Format m:ss.
  ok(CooldownTagPlan.format(0).equals("0:00"));
  ok(CooldownTagPlan.format(7).equals("0:07"));        // under a minute still formats sanely
  ok(CooldownTagPlan.format(59).equals("0:59"));
  ok(CooldownTagPlan.format(60).equals("1:00"));
  ok(CooldownTagPlan.format(61).equals("1:01"));
  ok(CooldownTagPlan.format(204).equals("3:24"));
  ok(CooldownTagPlan.format(240).equals("4:00"));
  ok(CooldownTagPlan.format(599).equals("9:59"));
  ok(CooldownTagPlan.format(600).equals("10:00"));
  ok(CooldownTagPlan.format(1800).equals("30:00"));
  ok(CooldownTagPlan.format(3599).equals("59:59"));
  ok(CooldownTagPlan.format(3600).equals("60:00"));    // minutes are not wrapped at 60
  ok(CooldownTagPlan.format(-3).equals("0:00"));
  // The annotated name: the spell's own name, a middle dot, the countdown.
  ok(CooldownTagPlan.suffix(204).equals(" · 3:24"));
  ok(CooldownTagPlan.annotated("Familiar", 204).equals("Familiar · 3:24"));
  ok(CooldownTagPlan.annotated("Familiar", 240).equals("Familiar · 4:00"));
  ok(CooldownTagPlan.annotated("Repair Item", 1800).equals("Repair Item · 30:00"));
  ok(CooldownTagPlan.annotated("Eat Food", 61).equals("Eat Food · 1:01"));
  // Suffix shape recognition (the pattern fallback; the PDC marker is authoritative at runtime).
  ok(CooldownTagPlan.hasOurSuffix("Familiar · 3:24"));
  ok(CooldownTagPlan.hasOurSuffix("Familiar · 0:07"));
  ok(CooldownTagPlan.hasOurSuffix("Repair Item · 30:00"));
  ok(!CooldownTagPlan.hasOurSuffix("Familiar"));
  ok(!CooldownTagPlan.hasOurSuffix("Familiar · 3:60"));   // seconds must be a real clock value
  ok(!CooldownTagPlan.hasOurSuffix("Familiar · 3:4"));    // seconds are two digits
  ok(!CooldownTagPlan.hasOurSuffix("Familiar · 3:244"));
  ok(!CooldownTagPlan.hasOurSuffix("Familiar · x:24"));
  ok(!CooldownTagPlan.hasOurSuffix("Familiar 3:24"));     // no separator, no suffix
  ok(!CooldownTagPlan.hasOurSuffix("Familiar ·3:24"));    // the separator carries its spaces
  ok(!CooldownTagPlan.hasOurSuffix("Familiar · 3:24 "));  // trailing junk is not ours
  ok(!CooldownTagPlan.hasOurSuffix(""));
  ok(!CooldownTagPlan.hasOurSuffix(null));
  // Strip removes exactly our suffix and nothing else.
  ok(CooldownTagPlan.stripSuffix("Familiar · 3:24").equals("Familiar"));
  ok(CooldownTagPlan.stripSuffix("Familiar").equals("Familiar"));
  ok(CooldownTagPlan.stripSuffix("Familiar · 3:60").equals("Familiar · 3:60"));
  ok(CooldownTagPlan.stripSuffix("A · B · 1:01").equals("A · B"));            // a base name may itself contain a dot pair
  ok(CooldownTagPlan.stripSuffix("Familiar · 3:24 · 1:01").equals("Familiar · 3:24")); // one suffix only
  ok(CooldownTagPlan.stripSuffix(CooldownTagPlan.annotated("Wolf", 61)).equals("Wolf")); // round trip
  ok(CooldownTagPlan.stripSuffix(CooldownTagPlan.annotated("Repair Item", 1800)).equals("Repair Item"));
  System.out.println("CooldownTagPlanTest: "+n+" checks passed");
 }
}
