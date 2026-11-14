package com.winkreyes.excelwandbars;
import com.winkreyes.excelwandbars.core.ApplyCoalescePlan;
/** 0.3.16: apply coalescing (F4) — fresh applies coalesce into a pending retry / in-flight apply without a guard token; our own apply's activation echo is skipped unless a real pick/tap landed in between. */
public class ApplyCoalescePlanTest {
 static int n; static void ok(boolean b){if(!b)throw new AssertionError("case "+n);n++;}
 public static void main(String[]args){
  // The spec's window.
  ok(ApplyCoalescePlan.SELF_APPLY_WINDOW_TICKS==2L);
  // Coalesce truth table: a fresh apply coalesces iff a retry is pending or an apply is in flight.
  ok(ApplyCoalescePlan.coalesce(false,true,false));
  ok(ApplyCoalescePlan.coalesce(false,false,true));
  ok(ApplyCoalescePlan.coalesce(false,true,true));
  ok(!ApplyCoalescePlan.coalesce(false,false,false));
  // A retry is never coalesced — it owns the pending flag and must run.
  ok(!ApplyCoalescePlan.coalesce(true,true,false));
  ok(!ApplyCoalescePlan.coalesce(true,false,true));
  ok(!ApplyCoalescePlan.coalesce(true,true,true));
  ok(!ApplyCoalescePlan.coalesce(true,false,false));
  // Echo: no prior apply -> never an echo.
  ok(!ApplyCoalescePlan.selfEcho(100,-1,0L,0L));
  // Echo at the apply tick and at the window edge (apply at tick 100, wall 5000, no input).
  ok(ApplyCoalescePlan.selfEcho(100,100,0L,5000L));
  ok(ApplyCoalescePlan.selfEcho(101,100,0L,5000L));
  ok(ApplyCoalescePlan.selfEcho(102,100,0L,5000L));
  // Past the window -> a real activation, admitted.
  ok(!ApplyCoalescePlan.selfEcho(103,100,0L,5000L));
  // An activation stamped before the apply is not its echo.
  ok(!ApplyCoalescePlan.selfEcho(99,100,0L,5000L));
  // Input from before the apply does not defeat the echo.
  ok(ApplyCoalescePlan.selfEcho(101,100,4999L,5000L));
  // Input exactly at the apply's stamp is the apply's own moment, not fresh input.
  ok(ApplyCoalescePlan.selfEcho(101,100,5000L,5000L));
  // A fresh pick/tap after the apply defeats the echo at every tick in the window.
  ok(!ApplyCoalescePlan.selfEcho(100,100,5001L,5000L));
  ok(!ApplyCoalescePlan.selfEcho(102,100,9000L,5000L));
  System.out.println("ApplyCoalescePlanTest: "+n+" checks passed");
 }
}
