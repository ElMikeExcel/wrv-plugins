/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.history;
import java.util.*;
/** Pure post-event proof rule. Inventory/result preview slots must be excluded from ownership counts. */
public final class MergeEvidence {
 private MergeEvidence(){}
 public static boolean committed(List<HistoryLedger.Expected> inputs,Map<UUID,Integer> beforeOwnedInputs,
         Map<UUID,Integer> afterInputsExcludingResult,UUID expectedResult,UUID observedResult,
         int distinctNewResultSlots,boolean cancelled,boolean competingInteraction,boolean sourceContextValid){
  if(cancelled||competingInteraction||!sourceContextValid||distinctNewResultSlots!=1||!Objects.equals(expectedResult,observedResult)||inputs.isEmpty())return false;
  Set<UUID> seen=new HashSet<>();
  for(var input:inputs)if(!seen.add(input.item())||beforeOwnedInputs.getOrDefault(input.item(),0)!=1||afterInputsExcludingResult.getOrDefault(input.item(),0)!=0)return false;
  return true;
 }
}
