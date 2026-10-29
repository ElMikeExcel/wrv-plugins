/* Excel WRV maintained source; original attribution and license preserved. */ package com.winkreyes.makersmark;
// Excel WRV: preserve complete identity scans; snapshot only potential Maker-ID matches.
import java.util.*;import org.bukkit.*;import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.entity.*;import org.bukkit.event.player.*;import org.bukkit.inventory.ItemStack;import org.bukkit.persistence.PersistentDataType;
import wrv.magic.evidence.*;import wrv.makersmark.magic.*;import com.elmakers.mine.bukkit.wand.Wand;
/** One downstream consumer. Registration belongs to the coordinated Magic bootstrap, never this class. */
public final class MakerMagicBridge implements EffectObserver.Sink,WandLoreBridge.Decorator,Listener,AutoCloseable {
 private final java.util.function.Supplier<? extends Collection<? extends Player>> players;
 private final ExcelMakersMark plugin;private final FarmCredit farm;private final MagicDeathJoin deaths=new MagicDeathJoin();private final MagicBossContributors bosses;
 private final Map<UUID,Boolean> reduced=bounded();private final Map<UUID,com.winkreyes.makersmark.history.HistoryLedger.Key> names=bounded();private boolean closed;
 private static final class PendingKill {
  final MagicDeathJoin.Evidence evidence;final boolean farmed;final com.winkreyes.makersmark.history.HistoryLedger.Key key;boolean rawApplied;
  PendingKill(MagicDeathJoin.Evidence e,boolean f,com.winkreyes.makersmark.history.HistoryLedger.Key k){evidence=e;farmed=f;key=k;}
  MagicDeathJoin.Evidence evidence(){return evidence;}boolean farmed(){return farmed;}com.winkreyes.makersmark.history.HistoryLedger.Key key(){return key;}
 }
 private final Map<UUID,PendingKill> pendingKills=new LinkedHashMap<>();private record PendingBoss(EntityDeathEvent event,com.winkreyes.makersmark.history.HistoryLedger.Key key){}
 private final Map<UUID,PendingBoss> pendingBosses=new LinkedHashMap<>();
 private final org.bukkit.scheduler.BukkitTask retryTask;
 private static <V> Map<UUID,V> bounded(){return new LinkedHashMap<>(){protected boolean removeEldestEntry(Map.Entry<UUID,V> entry){return size()>1024;}};}
 private record Slot(Player owner,int index,ItemStack item){}
 MakerMagicBridge(ExcelMakersMark plugin){this(plugin,()->Bukkit.getOnlinePlayers());}
 MakerMagicBridge(ExcelMakersMark plugin,java.util.function.Supplier<? extends Collection<? extends Player>> players){this.players=players;this.plugin=plugin;farm=new FarmCredit(plugin);bosses=new MagicBossContributors(source->plugin.getConfig().getBoolean("stats.trophies",true)&&actorCounts(source.actor())&&find(source.item(),source.nativeWand(),true)!=null,message->plugin.getLogger().warning(message));retryTask=Bukkit.getScheduler().runTaskTimer(plugin,this::retryPending,20,20);}
 private boolean actorCounts(UUID actor){return players.get().stream().filter(p->p.getUniqueId().equals(actor)).anyMatch(plugin::counts);}
 private Slot find(UUID maker,String nativeId){return find(maker,nativeId,false);}
 private Slot find(UUID maker,String nativeId,boolean mutationBoundary){
  if(closed||!Bukkit.isPrimaryThread()||maker==null||nativeId==null)return null;if(plugin.historyRecorder().quarantined(maker)||plugin.historyRecorder().nativeQuarantined(nativeId))return null;String makerToken=null;Slot found=null;int count=0,nativeCopies=0;Set<UUID> nativeItems=new HashSet<>();
  boolean malformed=false;List<Slot> physical=new ArrayList<>();
  for(Player p:players.get()){
   if(makerToken==null)makerToken=maker.toString(); // Excel: lazy once per lookup, including zero-player calls.
   List<ItemStack> seen=new ArrayList<>(Arrays.asList(p.getInventory().getContents()));seen.add(p.getItemOnCursor());seen.addAll(Arrays.asList(p.getEnderChest().getContents()));
   for(ItemStack item:seen){
    if(item==null)continue;
    try{if(nativeId.equals(Wand.getWandId(item))){nativeCopies+=item.getAmount();try{String id=plugin.marks().read(item).id;if(id!=null)nativeItems.add(UUID.fromString(id));}catch(RuntimeException bad){malformed=true;}}}catch(RuntimeException bad){malformed=true;}
    try{if(item.hasItemMeta()&&makerToken.equals(item.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING)))count+=item.getAmount();}catch(RuntimeException bad){malformed=true;}
   }
   for(int i=0;i<p.getInventory().getSize();i++){ItemStack item=p.getInventory().getItem(i);if(item!=null){
    // Excel: eligibility still uses a snapshot, taken at the same point as before.
    // Keep a conservative snapshot on an unexpected metadata-read failure; the
    // original post-scan selection remains responsible for refusal/exception.
    boolean potential=true;
    try{potential=item.hasItemMeta()&&makerToken.equals(item.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING));}catch(RuntimeException ignored){}
    if(potential)physical.add(new Slot(p,i,item.clone()));
   }}
  }
  // Finish all identity counting before malformed-token or eligibility refusal can hide a known duplicate.
  if(count>1||nativeCopies>1){if(mutationBoundary){plugin.historyRecorder().observeAmbiguity(maker,Math.max(count,nativeCopies));if(nativeCopies>1){plugin.historyRecorder().observeNativeAmbiguity(nativeId,nativeCopies);for(UUID id:nativeItems)plugin.historyRecorder().observeAmbiguity(id,nativeCopies);}}return null;}
  if(malformed)return null;
  for(Slot slot:physical){ItemStack item=slot.item;if(!item.hasItemMeta())continue;
   if(makerToken.equals(item.getItemMeta().getPersistentDataContainer().get(plugin.marks().kId,PersistentDataType.STRING))&&nativeId.equals(Wand.getWandId(item))&&plugin.historyEligible(slot.owner,slot.index,item))found=slot;
  }
  return count==1&&nativeCopies==1?found:null;
 }
 @Override public void observe(EffectObserver.Receipt ignored){}
 @EventHandler(priority=EventPriority.LOWEST) public void death(EntityDeathEvent event){
  if(closed)return;var pending=EffectObserver.currentDamage(event.getEntity().getUniqueId()).orElse(null);
  if(pending!=null&&deaths.capture(pending,event)){reduced.put(pending.dispatch(),farm.reduced(event.getEntity()));var key=Accomplishments.observed(event.getEntity());if(key!=null)names.put(pending.dispatch(),key);}
  if(plugin.getConfig().getBoolean("stats.trophies",true)&&bosses.captureDeath(event)){
   if(pendingBosses.size()<256)pendingBosses.putIfAbsent(event.getEntity().getUniqueId(),new PendingBoss(event,Accomplishments.observed(event.getEntity())));
   else plugin.getLogger().severe("Wand boss history pre-admission capacity reached; event was not saved");
   Bukkit.getScheduler().runTask(plugin,()->finishBoss(event));
  }
 }
 @Override public void damageCompleted(EffectObserver.DamageCompletion completion){
  if(closed||!Bukkit.isPrimaryThread())return;bosses.damageCompleted(completion);
  if(completion==null||completion.pending()==null)return;UUID dispatch=completion.pending().dispatch();boolean farmed=Boolean.TRUE.equals(reduced.remove(dispatch));var accomplishment=names.remove(dispatch);
  deaths.complete(completion).ifPresent(e->{
   if(!plugin.getConfig().getBoolean("stats.kills",true))return;
   if(pendingKills.size()>=1024){plugin.getLogger().severe("Wand kill pre-admission capacity reached; event was not saved: "+e.dispatch());return;}
   pendingKills.putIfAbsent(e.dispatch(),new PendingKill(e,farmed,accomplishment));
  });
  retryKills();
  if(completion.deathEvent()!=null)finishBoss(completion.deathEvent());
 }
 private void retryKills(){
  Set<UUID> blocked=new HashSet<>();
  for(var entry:new ArrayList<>(pendingKills.entrySet())){
   var pending=entry.getValue();var e=pending.evidence();if(blocked.contains(e.observedItem()))continue;
   Player caster=players.get().stream().filter(p->p.getUniqueId().equals(e.caster())).findFirst().orElse(null);
   if(caster==null){blocked.add(e.observedItem());continue;}
   if(!plugin.getConfig().getBoolean("stats.kills",true)||!plugin.counts(caster)){pendingKills.remove(entry.getKey());continue;}
   try{
    Slot slot=find(e.observedItem(),e.magicWandId(),true);if(slot==null){blocked.add(e.observedItem());continue;}
    boolean admitted=pending.key()==null||plugin.historyRecorder().record(e.observedItem(),e.victim(),e.caster(),pending.key());
    var receiptKey=new NamespacedKey(plugin,"magic_kill_receipt_v1");String last=slot.item.getItemMeta().getPersistentDataContainer().get(receiptKey,PersistentDataType.STRING);
    if(!pending.rawApplied&&!e.dispatch().toString().equals(last)){
     ItemStack replacement=slot.item.clone();Marks.Data data=plugin.marks().read(replacement);data.mobKill(pending.farmed());var meta=replacement.getItemMeta();plugin.marks().write(meta,data);meta.getPersistentDataContainer().set(receiptKey,PersistentDataType.STRING,e.dispatch().toString());replacement.setItemMeta(meta);
     plugin.historyReplace(slot.owner,slot.index,slot.item,replacement);
    }
    pending.rawApplied=true;
    Slot refreshed=find(e.observedItem(),e.magicWandId());if(refreshed==null)throw new IllegalStateException("Wand moved before display refresh");plugin.historyMagicRedraw(refreshed.owner,refreshed.index,refreshed.item);
    if(admitted)pendingKills.remove(entry.getKey());
   }catch(RuntimeException failure){blocked.add(e.observedItem());plugin.getLogger().warning("Unconfirmed wand event retained for retry "+e.dispatch()+": "+failure.getMessage());}
  }
 }
 private void retryPending(){if(closed)return;retryKills();for(var pending:new ArrayList<>(pendingBosses.values()))finishBoss(pending.event());}

 private void finishBoss(EntityDeathEvent event){
  if(closed)return;var pending=pendingBosses.get(event.getEntity().getUniqueId());if(pending==null||pending.event()!=event)return;if(event.isCancelled()){bosses.finishDeath(event);pendingBosses.remove(event.getEntity().getUniqueId());return;}
  bosses.discardOptedOut(event.getEntity().getUniqueId(),actor->players.get().stream().filter(p->p.getUniqueId().equals(actor)).anyMatch(p->!plugin.counts(p)));
  var list=bosses.finishDeath(event);if(list.isEmpty()){if(!bosses.hasContributors(event.getEntity().getUniqueId())){bosses.retireEmptyDeath(event);pendingBosses.remove(event.getEntity().getUniqueId());}return;}var key=pending.key();if(key==null)return;
  boolean admitted=true;for(var a:list)admitted&=plugin.historyRecorder().record(a.item(),a.victim(),a.actor(),key);
  if(admitted&&bosses.acknowledge(event,list))pendingBosses.remove(event.getEntity().getUniqueId()); // No raw kills, milestone credit, SP, or rewards for assists.
 }
 @Override public List<String> decorate(com.elmakers.mine.bukkit.api.wand.Wand wand,ItemStack exact,List<String> nativeLore){
  if(closed||!Bukkit.isPrimaryThread()||wand.getItem()!=exact)return nativeLore;var data=plugin.marks().read(exact);if(data.id==null)return nativeLore;
  UUID id;try{id=UUID.fromString(data.id);}catch(IllegalArgumentException e){return nativeLore;}Slot slot=find(id,wand.getId());if(slot==null||!slot.item.equals(exact)||!plugin.showsStats(slot.owner))return nativeLore;
  long revision=exact.getItemMeta().getPersistentDataContainer().getOrDefault(new NamespacedKey(plugin,"history_revision_v2"),PersistentDataType.LONG,0L);
  List<String> summary=new ArrayList<>();summary.add("Kills: "+((long)data.mob+data.pvp)+(data.pvp>0?" (Players: "+data.pvp+")":""));List<String> history=plugin.historyRecorder().lines(id,revision);if(history!=null)for(String line:history){if(summary.size()==4)break;summary.add(line);}
  List<String> rendered=new ArrayList<>(FreshMagicLore.fromNativeGetLore(nativeLore).render(summary).lines());
  if(rendered.size()>nativeLore.size())rendered.set(nativeLore.size(),net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacySection().serialize(net.kyori.adventure.text.Component.text(summary.getFirst(),com.winkreyes.makersmark.core.MilestoneColors.weapon(data.creditUnits(),data.pvp))));
  return rendered;
 }
 @EventHandler(priority=EventPriority.MONITOR)public void activated(com.elmakers.mine.bukkit.api.event.WandActivatedEvent e){Player player=e.getMage().getPlayer();if(player!=null)provision(player);}
 @EventHandler(priority=EventPriority.MONITOR)public void deactivated(com.elmakers.mine.bukkit.api.event.WandDeactivatedEvent e){Player player=e.getMage().getPlayer();if(player!=null)Bukkit.getScheduler().runTask(plugin,()->{provision(player);plugin.historyRecorder().resume();});}
 @EventHandler(priority=EventPriority.MONITOR)public void held(PlayerItemHeldEvent e){if(!e.isCancelled())Bukkit.getScheduler().runTask(plugin,()->provision(e.getPlayer()));}
 @EventHandler(priority=EventPriority.MONITOR)public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->provision(e.getPlayer()));}
 void provision(Player player){
  if(closed||!plugin.counts(player))return;
  for(int i=0;i<player.getInventory().getSize();i++){ItemStack item=player.getInventory().getItem(i);if(!MagicPhysicalItems.wand(item)||!plugin.historyEligible(player,i,item))continue;var meta=item.getItemMeta();var pdc=meta.getPersistentDataContainer();if(pdc.has(plugin.marks().kId))continue;
   if(pdc.has(new NamespacedKey(plugin,"history_revision_v2"))||pdc.has(new NamespacedKey(plugin,"history_receipt_v2")))continue;
   ItemStack copy=item.clone();var cm=copy.getItemMeta();cm.getPersistentDataContainer().set(plugin.marks().kId,PersistentDataType.STRING,UUID.randomUUID().toString());copy.setItemMeta(cm);plugin.historyReplace(player,i,item.clone(),copy);
  }
 }
 @Override public void close(){closed=true;retryTask.cancel();if(!pendingKills.isEmpty()||!pendingBosses.isEmpty())plugin.getLogger().severe("Unconfirmed pre-admission wand evidence at shutdown: kills="+pendingKills.keySet()+" bosses="+pendingBosses.keySet()+". These were not confirmed durable.");deaths.clear();bosses.clear();reduced.clear();names.clear();pendingKills.clear();pendingBosses.clear();}
}
