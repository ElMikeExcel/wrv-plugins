package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.VerifyRetryPlan;
import com.winkreyes.excelwandbars.core.VerifyRetryPlan.Branch;
import com.winkreyes.excelwandbars.core.VerifyRetryPlan.Verdict;
import java.util.Set;
/** 0.3.15: the wand-style verify race — a verify that only disagrees on what this apply just wrote retries (twice, delayed); anything else fails; retries and bar-open chains never close the bar. */
public class VerifyRetryPlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // The spec's numbers.
  ok(VerifyRetryPlan.RETRY_DELAY_TICKS==10L);
  ok(VerifyRetryPlan.MAX_RETRIES==2);
  // Mike's Nov 12 case exactly: the tapbar flip writes 4 keys and the rebuilt wand reads all 4 back stale.
  Set<String> mikeWritten=Set.of("left_click","right_click_sneak","drop","quick_cast");
  Set<String> mikeResidual=Set.of("left_click","right_click_sneak","drop","quick_cast");
  ok(VerifyRetryPlan.propsRetryable(mikeWritten,mikeResidual));
  ok(VerifyRetryPlan.verdict(mikeWritten,mikeResidual,false,false,0)==Verdict.RETRY);
  ok(VerifyRetryPlan.verdict(mikeWritten,mikeResidual,false,false,1)==Verdict.RETRY);
  ok(VerifyRetryPlan.verdict(mikeWritten,mikeResidual,false,false,2)==Verdict.FAILED); // capped: the chain terminates
  // A partial stale read (2 of the 4 keys caught up) is still only what this apply wrote.
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of("drop","quick_cast"),false,false,0)==Verdict.RETRY);
  // Nothing left differing = applied, at any point in a chain.
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of(),false,false,0)==Verdict.APPLIED);
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of(),false,false,2)==Verdict.APPLIED);
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of(),false,false,0)==Verdict.APPLIED);
  // A difference this apply did NOT write is a real mismatch: fail at once, no retry spent on it.
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of("left_click","mode"),false,false,0)==Verdict.FAILED);
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of("mode"),false,false,0)==Verdict.FAILED);
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of("drop"),false,false,0)==Verdict.FAILED);
  ok(!VerifyRetryPlan.propsRetryable(mikeWritten,Set.of()));
  ok(!VerifyRetryPlan.propsRetryable(mikeWritten,null));
  ok(!VerifyRetryPlan.propsRetryable(null,mikeResidual));
  // Layout branch: bad only retryable when this apply wrote the layout.
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of(),true,true,0)==Verdict.RETRY);
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of(),true,true,1)==Verdict.RETRY);
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of(),true,true,2)==Verdict.FAILED);
  ok(VerifyRetryPlan.verdict(Set.of(),Set.of(),false,true,0)==Verdict.FAILED);
  // Both branches: every residual must be just-written, or the whole verdict fails.
  ok(VerifyRetryPlan.verdict(mikeWritten,mikeResidual,true,true,0)==Verdict.RETRY);
  ok(VerifyRetryPlan.verdict(mikeWritten,Set.of("mode"),true,true,0)==Verdict.FAILED);
  ok(VerifyRetryPlan.verdict(mikeWritten,mikeResidual,false,true,0)==Verdict.FAILED);
  // Branch naming for the verdict log line.
  ok(VerifyRetryPlan.branch(false,false)==Branch.NONE);
  ok(VerifyRetryPlan.branch(true,false)==Branch.PROPS);
  ok(VerifyRetryPlan.branch(false,true)==Branch.LAYOUT);
  ok(VerifyRetryPlan.branch(true,true)==Branch.PROPS_AND_LAYOUT);
  // Bar-open triggers: activation, the bar-open repair, and any verify retry.
  ok(VerifyRetryPlan.isBarOpenTrigger("activate"));
  ok(VerifyRetryPlan.isBarOpenTrigger("ACTIVATE"));
  ok(VerifyRetryPlan.isBarOpenTrigger(" bar open "));
  ok(VerifyRetryPlan.isBarOpenTrigger("verify retry"));
  ok(!VerifyRetryPlan.isBarOpenTrigger("join"));
  ok(!VerifyRetryPlan.isBarOpenTrigger("magic load"));
  ok(!VerifyRetryPlan.isBarOpenTrigger("spell bar change"));
  ok(!VerifyRetryPlan.isBarOpenTrigger(null));
  ok(!VerifyRetryPlan.isBarOpenTrigger(""));
  // The close rule: deliberate, change-making, non-retry applies close (Magic's sequence);
  // bar-open chains and retries never do, and a no-change pass has nothing to close for.
  ok(VerifyRetryPlan.shouldCloseInventory(true,"join",false));
  ok(VerifyRetryPlan.shouldCloseInventory(true,"magic load",false));
  ok(VerifyRetryPlan.shouldCloseInventory(true,"respawn",false));
  ok(VerifyRetryPlan.shouldCloseInventory(true,"spell bar change",false));
  ok(VerifyRetryPlan.shouldCloseInventory(true,null,false));
  ok(!VerifyRetryPlan.shouldCloseInventory(true,"activate",false));
  ok(!VerifyRetryPlan.shouldCloseInventory(true,"bar open",false));
  ok(!VerifyRetryPlan.shouldCloseInventory(true,"verify retry",false));
  ok(!VerifyRetryPlan.shouldCloseInventory(true,"join",true));
  ok(!VerifyRetryPlan.shouldCloseInventory(true,"activate",true));
  ok(!VerifyRetryPlan.shouldCloseInventory(false,"join",false));
  System.out.println("VerifyRetryPlanTest: "+n+" checks passed");
 }
}
