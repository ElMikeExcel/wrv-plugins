package com.winkreyes.makersmark;
import com.winkreyes.makersmark.history.*;
import java.util.*;import java.util.concurrent.*;import java.nio.charset.StandardCharsets;
import org.bukkit.*;import org.bukkit.entity.Player;import org.bukkit.inventory.ItemStack;import org.bukkit.persistence.PersistentDataType;
/** Default-off strict alternative: qualifying death ingress fsyncs before acknowledgment; heavier processing is queued. No per-hit disk write. */
final class OrdinaryHistoryRecorder implements AutoCloseable {
 private final StrictIngress strictIngress;private final ExcelMakersMark plugin;private final HistoryLedger ledger;private final HistoryItemTokens codec=new HistoryItemTokens("excelmakersmark");
 private final java.util.function.Supplier<Collection<? extends Player>> players;
 private static final class Duplicate extends IllegalStateException { final UUID id;final int count;Duplicate(UUID id,int count){super("Observed duplicate weapon identity");this.id=id;this.count=count;} }
 private final ThreadPoolExecutor writer;private final Map<UUID,HistoryLedger.Award> queued=new ConcurrentHashMap<>();private final Map<UUID,Cached> cache=Collections.synchronizedMap(new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<UUID,Cached> e){return size()>1024;}});private volatile boolean closing;
 private record Located(ItemStack snapshot,HistoryItemTokens.Token token,int slot,Player owner,UUID ownerId,String fingerprint){}
 private record Cached(long revision,List<String> lines){}
 private final Set<UUID> localQuarantine=ConcurrentHashMap.newKeySet();
 private final Set<UUID> refreshing=ConcurrentHashMap.newKeySet();
 private record Proof(String fingerprint,int copies){}
 OrdinaryHistoryRecorder(ExcelMakersMark plugin,HistoryLedger ledger){this(plugin,ledger,()->Bukkit.getOnlinePlayers());}
 OrdinaryHistoryRecorder(ExcelMakersMark plugin,HistoryLedger ledger,java.util.function.Supplier<Collection<? extends Player>> players){this.plugin=plugin;this.ledger=ledger;this.players=players;this.strictIngress=new StrictIngress(ledger,java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(Math.max(1,plugin.getConfig().getLong("experimental.strict-ingress-pause-ms",50))));writer=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(256),r->{Thread t=new Thread(r,"MakerMark-history-writer");t.setDaemon(true);return t;},new ThreadPoolExecutor.AbortPolicy());}
 private static UUID nativeConflictKey(String nativeId){return UUID.nameUUIDFromBytes(("makersmark:native-conflict:v1:"+nativeId).getBytes(StandardCharsets.UTF_8));}
 boolean quarantined(UUID item){return localQuarantine.contains(item)||ledger.quarantined(item);}
 boolean nativeQuarantined(String nativeId){return nativeId!=null&&quarantined(nativeConflictKey(nativeId));}
 void observeAmbiguity(UUID item,int count){
  if(!Bukkit.isPrimaryThread()||count<2)throw new IllegalArgumentException("Duplicate observation requires main thread and copies > 1");
  if(localQuarantine.add(item))plugin.getLogger().warning("Observed duplicate identity quarantined; history/kill mutation held for administrator review: "+item);try{ledger.recordObservedDuplicate(item,count);}catch(Exception failure){plugin.getLogger().severe("Observed duplicate held locally; durable quarantine failed for "+item+": "+failure.getMessage());}
 }
 void observeNativeAmbiguity(String nativeId,int count){observeAmbiguity(nativeConflictKey(nativeId),count);}
 boolean record(UUID item,UUID victim,UUID actor,HistoryLedger.Key accomplishment){
  UUID receipt=UUID.nameUUIDFromBytes(("makersmark:death:v1:"+item+":"+victim).getBytes(StandardCharsets.UTF_8));
  HistoryLedger.Award incoming=new HistoryLedger.Award(receipt,item,victim,actor,accomplishment);
  if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Strict death ingress requires server thread");
  if(closing||quarantined(item))return false;
  if(queued.containsKey(receipt))return incoming.equals(queued.get(receipt));
  StrictIngress.Result admission=strictIngress.accept(incoming);
  if(admission.status()!=StrictIngress.Status.ACCEPTED){notice(actor,admission.reason());plugin.getLogger().severe("Strict history ingress "+admission.status()+" receipt "+receipt+" elapsed ns "+admission.elapsedNanos()+": "+admission.reason());}
  if(!admission.accepted())return false;
  if(queued.putIfAbsent(receipt,incoming)!=null)return true;
  try{writer.execute(()->{try{ledger.acceptAward(incoming);award(item,receipt,accomplishment);}catch(Exception e){if(e instanceof Duplicate d)try{ledger.recordObservedDuplicate(d.id,d.count);}catch(Exception failure){plugin.getLogger().severe("Could not persist observed duplicate quarantine: "+failure.getMessage());}notice(actor,"Weapon history is pending review; no guessed award was made.");plugin.getLogger().warning("History award held "+receipt+": "+e.getMessage());}finally{queued.remove(receipt);}});}
  catch(RejectedExecutionException e){queued.remove(receipt);notice(actor,"Weapon history is durably recorded, but its processing queue is full. It remains pending for rejoin or administrator resume.");plugin.getLogger().severe("Durable history deferred after full queue, receipt "+receipt+" item "+item+" victim "+victim);}  return true;
 }
 void resume(){
  if(closing||!Bukkit.isPrimaryThread())return;Set<UUID> available=new HashSet<>();
  for(Player p:players.get())for(ItemStack it:p.getInventory().getContents())if(it!=null&&it.hasItemMeta())try{String raw=it.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING);if(raw!=null)available.add(UUID.fromString(raw));}catch(RuntimeException ignored){}
  try{writer.execute(()->{try{resumeBatch(ledger.pendingAwards().stream().filter(a->available.contains(a.item())).toList(),0);}catch(Exception e){plugin.getLogger().severe("Pending history inbox unavailable: "+e.getMessage());}});}catch(RejectedExecutionException e){plugin.getLogger().warning("History resume queue busy; durable inbox retained");}
 }
 private void resumeBatch(List<HistoryLedger.Award> list,int from){
  if(closing||from>=list.size())return;
  try{writer.execute(()->{int end=Math.min(from+32,list.size());for(int i=from;i<end&&!closing;i++){var a=list.get(i);if(queued.putIfAbsent(a.receipt(),a)!=null)continue;
   try{award(a.item(),a.receipt(),a.key());}catch(Exception e){if(e instanceof Duplicate d)try{ledger.recordObservedDuplicate(d.id,d.count);}catch(Exception ignored){}plugin.getLogger().warning("Durable history inbox receipt remains held "+a.receipt()+": "+e.getMessage());}finally{queued.remove(a.receipt());}}
   resumeBatch(list,end);
  });}catch(RejectedExecutionException e){plugin.getLogger().warning("History resume paused; durable inbox retained for rejoin/retry");}
 }
 private void award(UUID item,UUID receipt,HistoryLedger.Key key)throws Exception{
  if(ledger.status(receipt)==HistoryLedger.Status.COMMITTED)return;
  var accepted=ledger.pendingAward(receipt).orElseThrow(()->new IllegalStateException("No durable award receipt"));
  UUID expectedReceipt=UUID.nameUUIDFromBytes(("makersmark:death:v1:"+accepted.item()+":"+accepted.victim()).getBytes(StandardCharsets.UTF_8));
  if(!accepted.item().equals(item)||!accepted.key().equals(key)||!receipt.equals(expectedReceipt)||ledger.consumed(item))throw new IllegalStateException("Durable award does not match current item lineage");
  if(ledger.state(item)==null){
   UUID seed=UUID.nameUUIDFromBytes(("makersmark:history-seed:v1:"+item).getBytes(StandardCharsets.UTF_8));
   var p=ledger.pendingPlan(seed).orElseGet(()->ledger.preview(seed,List.of(),item,null));ledger.prepare(p);
   Proof proof=main(()->apply(p,null));ledger.markPhysicalVerified(seed,item,1,proof.fingerprint,proof.copies,true);ledger.commit(seed,item,1,true,true);
  }
  HistoryLedger.State current=ledger.state(item);Located before=main(()->locate(item));
  if(before.token==null)throw new IllegalStateException("Archived identity has no item token");
  HistoryLedger.Plan pending=ledger.pendingPlan(receipt).orElse(null);
  if(pending!=null)verifyAwardPlan(pending,current,item,key);
  if(pending!=null && before.token.receipt().equals(receipt) && before.token.revision()==pending.output().revision()){
   var context=RecoveryContext.decode(ledger.pendingContext(receipt).orElseThrow(()->new IllegalStateException("Death update missing context")));
   if(!context.workstation().equals("DEATH")||pending.inputs().size()!=1||!pending.inputs().getFirst().equals(new HistoryLedger.Expected(item,current.revision()))||!pending.output().item().equals(item)||pending.output().revision()!=Math.addExact(current.revision(),1)||context.inputs().size()!=1||!context.inputs().getFirst().item().equals(item)||context.inputs().getFirst().revision()!=current.revision())throw new IllegalStateException("Not a provable same-item death update");
   Proof proof=main(()->apply(pending,before.token));ledger.markPhysicalVerified(receipt,item,pending.output().revision(),proof.fingerprint,proof.copies,true);ledger.commit(receipt,item,pending.output().revision(),true,true);cache.put(item,new Cached(pending.output().revision(),HistoryView.tooltip(pending.output())));main(()->{redraw(item);return true;});return;
  }
  ledger.reconcile(item,before.token.revision(),before.token.receipt(),1);
  HistoryLedger.Plan plan=pending!=null?pending:ledger.preview(receipt,List.of(new HistoryLedger.Expected(item,current.revision())),item,key);
  // A repeated event with changed name cannot alter an existing intent.
  verifyAwardPlan(plan,current,item,key);
  var context=new RecoveryContext(before.ownerId,"DEATH",before.fingerprint,List.of(new RecoveryContext.Input(before.slot,item,current.revision(),before.fingerprint)));
  if(ledger.pendingContext(receipt).isEmpty())ledger.prepareWithContext(plan,context.encode());else ledger.prepare(plan);
  Proof proof=main(()->apply(plan,before.token));ledger.markPhysicalVerified(receipt,item,plan.output().revision(),proof.fingerprint,proof.copies,true);ledger.commit(receipt,item,plan.output().revision(),true,true);
  cache.put(item,new Cached(plan.output().revision(),HistoryView.tooltip(plan.output())));main(()->{redraw(item);return true;});
 }
 private static void verifyAwardPlan(HistoryLedger.Plan plan,HistoryLedger.State current,UUID item,HistoryLedger.Key key){
  Map<HistoryLedger.Key,Long> names=new TreeMap<>(current.names());names.compute(key,(k,v)->v==null?1L:Math.addExact(v,1L));
  var expected=new HistoryLedger.State(item,Math.addExact(current.revision(),1),names,current.lineage());
  if(!plan.inputs().equals(List.of(new HistoryLedger.Expected(item,current.revision())))||!plan.output().equals(expected))throw new IllegalStateException("Pending plan does not match exact durable accomplishment delta");
 }
 private Located locate(UUID id){
  if(!Bukkit.isPrimaryThread()||closing)throw new IllegalStateException("History owner unavailable");Located found=null;int count=0;
  boolean malformed=false;
  for(Player p:players.get()){
   for(ItemStack observed:p.getInventory().getContents()){try{if(hasId(observed,id))count+=observed.getAmount();}catch(RuntimeException e){malformed=true;}}
   try{if(hasId(p.getItemOnCursor(),id))count+=p.getItemOnCursor().getAmount();}catch(RuntimeException e){malformed=true;}
   for(ItemStack observed:p.getEnderChest().getContents()){try{if(hasId(observed,id))count+=observed.getAmount();}catch(RuntimeException e){malformed=true;}}
  }
  if(count>1)throw new Duplicate(id,count);
  if(malformed)throw new IllegalStateException("Malformed identity in observed inventory");
  for(Player p:players.get())for(int slot=0;slot<p.getInventory().getSize();slot++){
   ItemStack live=p.getInventory().getItem(slot);if(hasId(live,id))found=new Located(live.clone(),codec.read(live).orElse(null),slot,p,p.getUniqueId(),RecoveryContext.fingerprint(live));
  }
  if(count!=1||found==null)throw new IllegalStateException("Weapon identity unavailable or duplicated in online inventories");
  if(!plugin.historyEligible(found.owner,found.slot,found.snapshot))throw new IllegalStateException("Unsupported or protected gear");return found;
 }
 private boolean hasId(ItemStack item,UUID id){return item!=null&&item.hasItemMeta()&&id.toString().equals(item.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING));}
 private Proof apply(HistoryLedger.Plan plan,HistoryItemTokens.Token expected){
  Located current=locate(plan.output().item());var token=current.token;
  if(token!=null&&token.receipt().equals(plan.receipt())&&token.revision()==plan.output().revision())return new Proof(RecoveryContext.fingerprint(current.snapshot),1);
  if(!Objects.equals(token,expected))throw new IllegalStateException("Item revision changed before history placement");
  ItemStack replacement=codec.resultCopy(current.snapshot,plan);
  if(!current.snapshot.equals(current.owner.getInventory().getItem(current.slot)))throw new IllegalStateException("Item moved before history placement");
  plugin.historyReplace(current.owner,current.slot,current.snapshot,replacement);
  Located after=locate(plan.output().item());if(!replacement.equals(after.snapshot))throw new IllegalStateException("History placement readback mismatch");return new Proof(RecoveryContext.fingerprint(after.snapshot),1);
 }
 private void redraw(UUID id){Located item=locate(id);if(plugin.historyMagicRedraw(item.owner,item.slot,item.snapshot))return;Marks.Data data=plugin.marks().read(item.snapshot);ItemStack replacement=item.snapshot.clone();plugin.marks().apply(replacement,data,plugin.showsStats(item.owner));if(item.snapshot.equals(item.owner.getInventory().getItem(item.slot)))item.owner.getInventory().setItem(item.slot,replacement);}
 List<String> lines(UUID id,long revision){if(revision<1)return List.of();var found=cache.get(id);if(found!=null&&found.revision==revision)return found.lines;refresh(id);return null;}
 void refresh(UUID item){if(closing||!refreshing.add(item))return;try{writer.execute(()->{try{var state=ledger.state(item);if(state!=null&&!ledger.consumed(item)){cache.put(item,new Cached(state.revision(),HistoryView.tooltip(state)));main(()->{redraw(item);return true;});}}catch(Exception e){plugin.getLogger().fine("Tooltip refresh held: "+e.getMessage());}finally{refreshing.remove(item);}});}catch(RejectedExecutionException e){refreshing.remove(item);}}
 void page(Player viewer,UUID item,int page){try{writer.execute(()->{try{var state=ledger.state(item);if(state==null){notice(viewer.getUniqueId(),"No archived named accomplishments yet.");return;}cache.put(item,new Cached(state.revision(),HistoryView.tooltip(state)));var view=HistoryView.page(state,page,8);main(()->{redraw(item);viewer.sendMessage("Weapon history "+(view.index()+1)+"/"+view.pages()+" ("+view.distinct()+" names)");for(var entry:view.entries())viewer.sendMessage(net.kyori.adventure.text.Component.text(entry.type()+": "+entry.fullName()+" ×"+entry.victories()));return true;});}catch(Exception e){notice(viewer.getUniqueId(),"History page unavailable: "+e.getMessage());}});}catch(RejectedExecutionException e){viewer.sendMessage("History is busy; please retry shortly.");}}
 private <T>T main(Callable<T> task)throws Exception{if(closing)throw new IllegalStateException("Recorder closing");Future<T> future=Bukkit.getScheduler().callSyncMethod(plugin,task);try{return future.get(5,TimeUnit.SECONDS);}catch(ExecutionException e){future.cancel(false);if(e.getCause() instanceof Exception cause)throw cause;throw e;}catch(Exception e){future.cancel(false);throw e;}}
 private void notice(UUID actor,String message){if(closing)return;try{Bukkit.getScheduler().runTask(plugin,()->{Player p=Bukkit.getPlayer(actor);if(p!=null)p.sendMessage(message);});}catch(IllegalStateException ignored){}}
 public void close(){
  closing=true;List<HistoryLedger.Award> remaining=List.copyOf(queued.values());writer.shutdownNow();
  for(UUID id:localQuarantine)try{ledger.recordObservedDuplicate(id,2);}catch(Exception failure){plugin.getLogger().severe("Quarantine shutdown persistence failed: "+id);}
  // Orderly shutdown may flush bounded ingress on the main thread; no per-hit disk writes occur.
  for(var award:remaining)try{ledger.acceptAward(award);}catch(Exception e){plugin.getLogger().severe("History shutdown could not persist receipt "+award.receipt()+" item "+award.item()+" victim "+award.victim()+" accomplishment "+award.key()+": "+e.getMessage());}
 }
}
