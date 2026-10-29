/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark.history;
import java.util.*;import java.io.*;
import org.bukkit.NamespacedKey;import org.bukkit.inventory.ItemStack;import org.bukkit.persistence.PersistentDataType;
/** Archive-only finalization. Never writes an item or reconstructs consumed ingredients. */
public final class PendingRecovery {
 public enum Outcome { COMMITTED, ALREADY_COMMITTED, HELD }
 public record Result(Outcome outcome,String reason){}
 private final HistoryLedger ledger;private final HistoryItemTokens codec;private final NamespacedKey id;
 public PendingRecovery(HistoryLedger ledger,String namespace){this.ledger=ledger;codec=new HistoryItemTokens(namespace);id=new NamespacedKey(namespace,"item_id");}
 /** Caller supplies a fresh authoritative main-thread snapshot of observed owned slots, not recipe previews. */
 public Result retry(UUID receipt,List<ItemStack> observed)throws IOException{
  if(ledger.status(receipt)==HistoryLedger.Status.COMMITTED)return new Result(Outcome.ALREADY_COMMITTED,"Receipt already committed; no item changed");
  var pending=ledger.pendingPlan(receipt);if(pending.isEmpty())return held("No active pending receipt");var plan=pending.get();
  var bytes=ledger.pendingContext(receipt);if(bytes.isEmpty())return held("Missing original pickup context");
  RecoveryContext context;try{context=RecoveryContext.decode(bytes.get());}catch(IOException e){return held("Invalid original pickup context");}
  Set<HistoryLedger.Expected> expected=new HashSet<>(plan.inputs());Set<HistoryLedger.Expected> contextual=new HashSet<>();for(var i:context.inputs())contextual.add(new HistoryLedger.Expected(i.item(),i.revision()));
  if(!expected.equals(contextual))return held("Pickup context does not match archived inputs");
  if(ledger.physicalProof(receipt).isEmpty())return held("Result transfer was not durably verified before interruption");
  Map<UUID,Integer> counts=new HashMap<>();ItemStack result=null;boolean conflicting=false;
  boolean malformedIdentity=false;
  for(ItemStack item:observed){if(item==null||item.getType().isAir()||!item.hasItemMeta())continue;
   try{String raw=item.getItemMeta().getPersistentDataContainer().get(id,PersistentDataType.STRING);if(raw!=null)counts.merge(UUID.fromString(raw),item.getAmount(),Math::addExact);}catch(RuntimeException e){malformedIdentity=true;}
  }
  if(counts.getOrDefault(plan.output().item(),0)>1){ledger.recordObservedDuplicate(plan.output().item(),counts.get(plan.output().item()));return held("Duplicate result identity observed and durably quarantined");}
  for(var input:plan.inputs())if(counts.getOrDefault(input.item(),0)>1){ledger.recordObservedDuplicate(input.item(),counts.get(input.item()));return held("Duplicate input identity observed and durably quarantined");}
  if(malformedIdentity)return held("Malformed item identity in observed inventory");
  for(ItemStack item:observed){if(item==null||item.getType().isAir())continue;
   Optional<HistoryItemTokens.Token> token;try{token=codec.read(item);}catch(RuntimeException e){return held("Malformed history token in observed inventory");}
   if(token.isPresent()&&token.get().item().equals(plan.output().item())){
    var t=token.get();if(item.getAmount()!=1||t.revision()!=plan.output().revision()||!t.receipt().equals(receipt))conflicting=true;result=item;
   }
  }
  if(conflicting)return held("Conflicting output token");
  if(result==null)return held("Prepared result not found in observed owned slots");
  for(var input:plan.inputs())if(!input.item().equals(plan.output().item())&&counts.getOrDefault(input.item(),0)>0)return held("Consumed input identity is still present");
  // Token is authoritative; legitimate later name/enchantment edits are preserved byte-for-byte.
  try{ledger.commit(receipt,plan.output().item(),plan.output().revision(),true,true);}catch(IllegalStateException e){return held("Archive rejected finalization: " + e.getMessage());}
  return new Result(Outcome.COMMITTED,"Verified receipt finalized; current item metadata untouched");
 }
 private static Result held(String why){return new Result(Outcome.HELD,why);}
}
