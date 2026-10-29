package com.winkreyes.makersmark;

import com.winkreyes.makersmark.core.TransferEvidence;
import com.winkreyes.makersmark.history.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;

/** Offline, UNREGISTERED listener. Observe committed result transfers; never mutate recipe previews. */
final class ObservedGearMergeHooks implements Listener {
 private final ExcelMakersMark plugin;private final HistoryLedger ledger;private final HistoryItemTokens codec;
 private final Map<UUID,Pending> pending=new HashMap<>();private final Set<UUID> ambiguous=new HashSet<>();private final Map<UUID,Integer> interactionTick=new HashMap<>();
 private record Pending(Player player,Inventory top,int resultSlot,List<Integer> sourceSlots,
   List<ItemStack> beforePlayer,Map<UUID,Integer> beforeCounts,List<HistoryLedger.Expected> inputs,
   ItemStack expected,InventoryClickEvent event,HistoryLedger.Plan plan,List<ItemStack> beforeSources,InventoryView view){}
 ObservedGearMergeHooks(ExcelMakersMark plugin,HistoryLedger ledger){this.plugin=plugin;this.ledger=ledger;codec=new HistoryItemTokens("excelmakersmark");}
 private boolean begin(Player p){Pending prior=pending.get(p.getUniqueId());if(prior!=null)ambiguous.add(prior.plan.receipt());Integer old=interactionTick.put(p.getUniqueId(),Bukkit.getCurrentTick());return old==null||old!=Bukkit.getCurrentTick();}
 private static boolean survival(Player p){return p.isOnline()&&(p.getGameMode()==GameMode.SURVIVAL||p.getGameMode()==GameMode.ADVENTURE);}
 private static ItemStack copy(ItemStack i){return i==null||i.getType().isAir()?null:i.clone();}
 private static List<ItemStack> snapshot(Player p){List<ItemStack> a=new ArrayList<>();for(ItemStack i:p.getInventory().getContents())a.add(copy(i));a.add(copy(p.getItemOnCursor()));return a;}
 private UUID identity(ItemStack i){if(i==null||!i.hasItemMeta())return null;String s=i.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING);try{return s==null?null:UUID.fromString(s);}catch(IllegalArgumentException e){return null;}}
 private Map<UUID,Integer> counts(List<ItemStack> owned,Inventory top,List<Integer> sources,int omitOwned){Map<UUID,Integer> c=new HashMap<>();for(int n=0;n<owned.size();n++)if(n!=omitOwned){UUID id=identity(owned.get(n));if(id!=null)c.merge(id,1,Integer::sum);}for(int slot:sources){UUID id=identity(top.getItem(slot));if(id!=null)c.merge(id,1,Integer::sum);}return c;}
 private static boolean supported(InventoryType type){return type==InventoryType.ANVIL||type==InventoryType.GRINDSTONE||type==InventoryType.SMITHING||type==InventoryType.WORKBENCH||type==InventoryType.CRAFTING;}
 @EventHandler(priority=EventPriority.HIGHEST)
 public void click(InventoryClickEvent e){
  if(!(e.getWhoClicked() instanceof Player p))return;
  if(pending.containsKey(p.getUniqueId())&&e.getSlotType()==InventoryType.SlotType.RESULT){e.setCancelled(true);p.sendMessage("Item history is finishing or needs reconciliation; this tracked result is held safely.");return;}
  boolean first=begin(p);
  if(e.isCancelled()||e.getSlotType()!=InventoryType.SlotType.RESULT)return;
  Inventory top=e.getView().getTopInventory();if(!supported(top.getType())||e.getClickedInventory()!=top)return;
  ItemStack result=e.getCurrentItem();
  // Only player-owned cursor/hotbar/inventory destinations; no throw/drop/collect or creative action.
  try{
   List<Integer> sources=new ArrayList<>();List<HistoryLedger.Expected> inputs=new ArrayList<>();
   for(int n=0;n<top.getSize();n++)if(n!=e.getSlot()){sources.add(n);ItemStack input=top.getItem(n);var token=codec.read(input);if(token.isPresent()){if(input.getAmount()!=1||!plugin.historyWorkstationEligible(input))throw new IllegalStateException("Unsupported tracked input");var t=token.get();inputs.add(new HistoryLedger.Expected(t.item(),t.revision()));}}
   List<ItemStack> magicInputs=new ArrayList<>();for(int source:sources){ItemStack input=top.getItem(source);if(input!=null&&plugin.isHistoryWand(input))magicInputs.add(input);}
   boolean trackedMagic=magicInputs.stream().anyMatch(item->plugin.marks().read(item).id!=null||plugin.marks().read(item).hasStats());
   if(trackedMagic&&(inputs.size()>1||magicInputs.size()!=1||inputs.stream().anyMatch(input->!input.item().toString().equals(plugin.marks().read(magicInputs.getFirst()).id))||!plugin.historyMagicEdit(magicInputs.getFirst(),result))){e.setCancelled(true);p.sendMessage("This wand edit cannot preserve its verified identity/history safely; nothing was consumed.");return;}
   if(inputs.isEmpty())return;
   if(e instanceof InventoryCreativeEvent||!survival(p)||result==null||result.getAmount()!=1||!plugin.historyWorkstationEligible(result)){
       e.setCancelled(true);p.sendMessage("This tracked-item conversion is not supported safely yet; its history was not consumed.");return;
   }
   if(!first||inputs.size()>2||!Set.of(InventoryAction.PICKUP_ALL,InventoryAction.PICKUP_HALF,InventoryAction.PICKUP_ONE,InventoryAction.PICKUP_SOME,InventoryAction.MOVE_TO_OTHER_INVENTORY,InventoryAction.HOTBAR_SWAP,InventoryAction.HOTBAR_MOVE_AND_READD).contains(e.getAction())){
       e.setCancelled(true);p.sendMessage("Use one normal result pickup so item history can be preserved safely.");return;
   }
   List<ItemStack> before=snapshot(p);Map<UUID,Integer> count=counts(before,top,sources,-1);
   for(int source:sources){var token=codec.read(top.getItem(source));if(token.isPresent()){var t=token.get();ledger.reconcile(t.item(),t.revision(),t.receipt(),count.getOrDefault(t.item(),0));}}
   UUID resultId=identity(result);if(resultId==null)resultId=UUID.randomUUID();
   HistoryLedger.Plan plan=ledger.preview(UUID.randomUUID(),inputs,resultId,null);
   ledger.prepareWithContext(plan,recoveryContext(p,top,sources,result)); // Actual pickup, NEVER preview.
   List<ItemStack> beforeSources=new ArrayList<>();for(int source:sources)beforeSources.add(copy(top.getItem(source)));
   Pending work=new Pending(p,top,e.getSlot(),List.copyOf(sources),before,count,List.copyOf(inputs),result.clone(),e,plan,Collections.unmodifiableList(beforeSources),e.getView());pending.put(p.getUniqueId(),work);
   Bukkit.getScheduler().runTask(plugin,()->finish(work));
  }catch(Exception failure){e.setCancelled(true);plugin.getLogger().warning("History merge pickup held before consumption: "+failure.getMessage());p.sendMessage("This item history could not be prepared safely; the merge was not taken.");}
 }
 private void finish(Pending work){
  Player p=work.player;if(pending.get(p.getUniqueId())!=work)return;
  if(!survival(p)){hold(work,"owner unavailable");return;}
  try{
   List<ItemStack> after=snapshot(p);
   if(ambiguous.contains(work.plan.receipt())||p.getOpenInventory()!=work.view){hold(work,"competing interaction or changed view");return;}
   boolean unchanged=work.beforePlayer.equals(after);
   for(int n=0;n<work.sourceSlots.size();n++)unchanged &= Objects.equals(work.beforeSources.get(n),copy(work.top.getItem(work.sourceSlots.get(n))));
   if(unchanged&&work.expected.equals(work.top.getItem(work.resultSlot))){ledger.abortUnchanged(work.plan.receipt(),true,true);pending.remove(p.getUniqueId(),work);ambiguous.remove(work.plan.receipt());return;}
   if(work.event.isCancelled()){hold(work,"cancelled after inputs or ownership changed");return;}
   int stillPreview=work.expected.equals(work.top.getItem(work.resultSlot))?1:0;
   var destination=TransferEvidence.uniqueNewSlot(work.beforePlayer,after,work.expected,1,stillPreview,false,true);
   if(destination.isEmpty()){hold(work,"result transfer not uniquely proved");return;}int selected=destination.getAsInt();
   HistoryLedger.Plan plan=work.plan;final UUID expectedId=plan.output().item();
   HistoryMergeTransaction.CommittedResult target=new HistoryMergeTransaction.CommittedResult(){
    public ItemStack snapshot(){return copy(selected==p.getInventory().getSize()?p.getItemOnCursor():p.getInventory().getItem(selected));}
    public boolean compareAndSet(ItemStack expected,ItemStack replacement){if(!Bukkit.isPrimaryThread()||!survival(p)||!Objects.equals(snapshot(),expected))return false;if(selected==p.getInventory().getSize()){if(plugin.isHistoryWand(expected)&&!plugin.historyWorkstationEligible(expected))return false;p.setItemOnCursor(replacement);}else plugin.historyReplace(p,selected,expected,replacement);return true;}
    public int observedCopies(UUID id){return counts(ObservedGearMergeHooks.snapshot(p),work.top,work.sourceSlots,-1).getOrDefault(id,0);}
    public boolean consumptionProven(HistoryLedger.Plan ignored){
     if(!survival(p)||work.event.isCancelled()||ambiguous.contains(work.plan.receipt())||p.getOpenInventory()!=work.view)return false;
     Map<UUID,Integer> remaining=counts(ObservedGearMergeHooks.snapshot(p),work.top,work.sourceSlots,selected);
     // Destination uniqueness was established by complete before/after snapshots; same-tick interactions invalidate Pending.
     return MergeEvidence.committed(work.inputs,work.beforeCounts,remaining,expectedId,expectedId,1,false,false,true);
    }
   };
   new HistoryMergeTransaction(ledger,codec).apply(target,work.expected,plan);if(plugin.historyRecorder()!=null)plugin.historyRecorder().refresh(plan.output().item());pending.remove(p.getUniqueId(),work);ambiguous.remove(work.plan.receipt());
  }catch(Exception failure){hold(work,failure.getMessage());}
 }
 private byte[] recoveryContext(Player p,Inventory top,List<Integer> slots,ItemStack result)throws Exception{
  List<RecoveryContext.Input> inputs=new ArrayList<>();
  for(int slot:slots){ItemStack input=top.getItem(slot);var token=codec.read(input);if(token.isPresent()){var t=token.get();inputs.add(new RecoveryContext.Input(slot,t.item(),t.revision(),RecoveryContext.fingerprint(input)));}}
  return new RecoveryContext(p.getUniqueId(),top.getType().name(),RecoveryContext.fingerprint(result),inputs).encode();
 }
 private void hold(Pending work,String reason){plugin.getLogger().warning("History merge receipt "+work.plan.receipt()+" remains pending: "+reason);if(work.player.isOnline())work.player.sendMessage("Item history is pending verification. Existing history is preserved; do not repeat this merge yet.");}
 @EventHandler(priority=EventPriority.MONITOR)public void close(InventoryCloseEvent e){if(e.getPlayer() instanceof Player p)begin(p);}
 @EventHandler(priority=EventPriority.MONITOR)public void drag(InventoryDragEvent e){if(e.getWhoClicked() instanceof Player p)begin(p);}
 @EventHandler(priority=EventPriority.MONITOR)public void drop(PlayerDropItemEvent e){begin(e.getPlayer());}
 @EventHandler(priority=EventPriority.MONITOR)public void swap(PlayerSwapHandItemsEvent e){begin(e.getPlayer());}
 @EventHandler public void quit(PlayerQuitEvent e){Pending work=pending.remove(e.getPlayer().getUniqueId());if(work!=null)hold(work,"owner quit; durable receipt retained");interactionTick.remove(e.getPlayer().getUniqueId());}
}
