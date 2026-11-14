package com.winkreyes.excelwandbars;
import java.util.*;import java.nio.file.*;import java.io.*;import java.util.function.*;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.wand.Wand;
import wrv.magic.level.*;import wrv.magic.retention.BoundedPractice;
/** Explicit offline/prepared owner. No onEnable registration or default policy activation. */
final class FirstObservationCoordinator implements AutoCloseable {
 private final MagicAPI magic;private final Set<String> templates;private final Map<String,Integer> maximum;private final Map<String,Integer> archiveMaximum;private final Function<String,String> canonical;private final FirstObservationFloor core;private AutoCloseable handle;private boolean closed;
 private final PracticeIdentityJournal identities;private List<String> templateOrder=List.of();private String lastReviewReason="none";private long lastReviewLog;private org.bukkit.event.Listener activationListener;
 private RawWandLoadBridge.Snapshot rawFrame;private LearnedLevelResolver.Request touchFrame;
 private record BoundFrame(Mage mage,Wand wand,boolean bootstrap){}
 private BoundFrame boundFrame;
 FirstObservationCoordinator(MagicAPI magic,Set<String> templates,Map<String,Integer> maximum,Function<String,String> canonical,
    Path journal,FirstObservationFloor.Policy policy,Map<String,BoundedPractice> ledgers)throws IOException{this(magic,templates,maximum,canonical,journal,policy,ledgers,false);}
 FirstObservationCoordinator(MagicAPI magic,Set<String> templates,Map<String,Integer> maximum,Function<String,String> canonical,Path journal,FirstObservationFloor.Policy policy,Map<String,BoundedPractice> ledgers,boolean compatibility)throws IOException{this(magic,templates,maximum,canonical,journal,policy,ledgers,compatibility,maximum);}
 FirstObservationCoordinator(MagicAPI magic,Set<String> templates,Map<String,Integer> maximum,Function<String,String> canonical,Path journal,FirstObservationFloor.Policy policy,Map<String,BoundedPractice> ledgers,boolean compatibility,Map<String,Integer> archiveMaximum)throws IOException{
  this.magic=Objects.requireNonNull(magic);this.templates=Set.copyOf(templates);this.maximum=Map.copyOf(maximum);this.archiveMaximum=Map.copyOf(archiveMaximum);for(var e:maximum.entrySet())if(!Objects.equals(e.getValue(),archiveMaximum.get(e.getKey())))throw new IllegalArgumentException("archive includes every active policy with same maximum");this.canonical=Objects.requireNonNull(canonical);
  if(this.templates.isEmpty()||!this.maximum.keySet().equals(ledgers.keySet()))throw new IllegalArgumentException("explicit ladder/managed ledger scope required");
  this.identities=compatibility?new PracticeIdentityJournal(journal.resolveSibling("original-wand-provenance.dat"),policy.capacity()):null;
  try{this.core=new FirstObservationFloor(journal,policy,ledgers,new FirstObservationFloor.Authority(){public boolean exactSavedCapture(FirstObservationFloor.Capture c){return rawAuthority(c);}public boolean exactBoundTouch(FirstObservationFloor.Capture c){return touchAuthority(c);}});}catch(IOException|RuntimeException failure){if(identities!=null)identities.close();throw failure;}
 }
 void start(){if(closed||handle!=null||!Bukkit.isPrimaryThread())throw new IllegalStateException("owner unavailable");var scopeTemplates=templates;handle=RawWandLoadBridge.installOwned(this::onRawSnapshot,raw->raw.existingWand()&&!raw.upgrade()&&scopeTemplates.contains(raw.template())&&!Boolean.FALSE.equals(raw.bound()));}
 private boolean active(){return !closed&&Bukkit.isPrimaryThread();}
 private UUID rawOwner(RawWandLoadBridge.Snapshot r){try{UUID u=UUID.fromString(r.ownerId());return u.toString().equalsIgnoreCase(r.ownerId())?u:null;}catch(RuntimeException bad){return null;}}
 private boolean rawAuthority(FirstObservationFloor.Capture c){var r=rawFrame;return active()&&r!=null&&c.exactAuthority()==r&&RawWandLoadBridge.isCurrentSnapshot(r)&&r.wellFormed()&&r.existingWand()&&!r.upgrade()&&!Boolean.FALSE.equals(r.bound())&&templates.contains(r.template())&&c.owner().equals(rawOwner(r))&&c.wand().equals(identity(r))&&archivedRank(c);}
 boolean onRawSnapshot(RawWandLoadBridge.Snapshot raw){
  if(!active()||raw==null||!RawWandLoadBridge.isCurrentSnapshot(raw))return false;
  if(!raw.existingWand()||raw.upgrade()||!templates.contains(raw.template())||Boolean.FALSE.equals(raw.bound()))return true;
  if(identities!=null)return onCompatibleRaw(raw);
  if(!raw.wellFormed()||rawOwner(raw)==null||raw.id()==null||raw.id().isBlank())return false;
  var previous=rawFrame;rawFrame=raw;try{
   var parsed=StoredNativeRanks.parse(raw.spells(),raw.spellLevels(),maximum,canonical);
   for(var rank:parsed.ranks().entrySet()){
    // A pending record keeps its original payload too: later current state cannot replace an uncertain first observation.
    if(core.observed(rawOwner(raw),raw.id(),rank.getKey()).isPresent())continue;
    var capture=new FirstObservationFloor.Capture(rawOwner(raw),raw.id(),rank.getKey(),rank.getValue(),FirstObservationFloor.Input.SAVED_ITEM,raw);
    if(core.prepare(capture).status()==FirstObservationFloor.Status.REVIEW)return false;
   }
   return active()&&RawWandLoadBridge.isCurrentSnapshot(raw);
  }catch(Exception unavailable){return false;}finally{rawFrame=previous;}
 }

 private boolean archivedRank(FirstObservationFloor.Capture c){if(identities==null)return true;try{var e=identities.get(c.wand());return e!=null&&c.owner().toString().equalsIgnoreCase(e.owner())&&Objects.equals(e.baseline().get(c.spell()),c.nativeRank());}catch(IOException failed){return false;}}
 /** Excel: compatibility path commits immutable original provenance before a PDC write. */
 private String identity(RawWandLoadBridge.Snapshot raw){if(identities==null)return raw.id();var item=raw.original().getItem();return PracticeWandIdentity.hasToken(item)?PracticeWandIdentity.get(raw.original()):raw.id();}
 private boolean onCompatibleRaw(RawWandLoadBridge.Snapshot raw){
  if(!raw.wellFormed()||raw.original().getClass()!=com.elmakers.mine.bukkit.wand.Wand.class)return false;
  var prior=rawFrame;rawFrame=raw;
  try{
   String owner=raw.ownerId();if(owner!=null&&owner.isBlank())owner=null;
   if(owner!=null&&rawOwner(raw)==null)return false;
   var item=raw.original().getItem();if(item==null||item.getAmount()!=1)return false;String id=identity(raw);
   if(id!=null){try{if(!UUID.fromString(id).toString().equals(id))return review("noncanonical-native-id");}catch(IllegalArgumentException invalid){return review("noncanonical-native-id");}}
   if(PracticeWandIdentity.hasToken(item)&&id==null)return review("malformed-practice-token");
   var entry=id==null?null:identities.get(id);
   if(entry!=null&&((entry.owner()!=null&&!entry.owner().equals(owner))||!entry.allowedTemplates().contains(raw.template())))return review("original-owner-or-template-mismatch");
   if(entry==null){
    // A present Excel token without its original journal is never silently reconstructed.
    if(PracticeWandIdentity.hasToken(item))return review("original-archive-missing");
    String text=RawWandLoadBridge.rawText(raw);if(text==null)return false;
    var parsed=StoredNativeRanks.parse(raw.spells(),raw.spellLevels(),archiveMaximum,canonical);
    var levels=LegacyWandBaselines.resolve(raw,text,archiveMaximum,parsed.ranks());
    entry=identities.prepare(id,owner,raw.template(),text,owner==null?"excel-new-unowned-v7":LegacyWandBaselines.PROVENANCE,levels);
    if(!PracticeWandIdentity.hasToken(item)){
     if(!PracticeWandIdentity.attach(item,UUID.fromString(entry.identity())))return false;
     if(!entry.identity().equals(PracticeWandIdentity.get(raw.original())))return false;
    }
   }
   if(!PracticeWandIdentity.hasToken(item)){if(!PracticeWandIdentity.attach(item,UUID.fromString(entry.identity()))||!entry.identity().equals(PracticeWandIdentity.get(raw.original())))return false;}
   if(!entry.allowedTemplates().contains(raw.template()))return review("template-transition-not-reviewed");
   if(owner==null){return entry.owner()==null&&active()&&RawWandLoadBridge.isCurrentSnapshot(raw);}
   entry=identities.claimOwner(entry.identity(),owner);
   for(var rank:entry.baseline().entrySet()){
    if(!maximum.containsKey(rank.getKey()))continue;
    if(core.observed(rawOwner(raw),entry.identity(),rank.getKey()).isPresent())continue;
    var capture=new FirstObservationFloor.Capture(rawOwner(raw),entry.identity(),rank.getKey(),rank.getValue(),FirstObservationFloor.Input.SAVED_ITEM,raw);
    if(core.prepare(capture).status()==FirstObservationFloor.Status.REVIEW)return false;
   }
   return active()&&RawWandLoadBridge.isCurrentSnapshot(raw);
  }catch(Exception|LinkageError unavailable){return review("original-provenance-unavailable");}finally{rawFrame=prior;}
 }
 private boolean review(String category){lastReviewReason=category;long now=System.currentTimeMillis();if(now-lastReviewLog>=60000){lastReviewLog=now;try{magic.getController().getLogger().warning("Excel wand compatibility requires review: "+category+"; original data retained, no progression grant");}catch(RuntimeException ignored){}}return false;}
 String lastReviewReason(){return lastReviewReason;}
 /** Exact native activation supplies identity for normal API-created, still-unowned wands. */
 void registerActivation(org.bukkit.plugin.Plugin plugin){
  if(identities==null||activationListener!=null)return;
  if(!(plugin instanceof ExcelWandBars exact))throw new IllegalArgumentException("exact Excel owner required");templateOrder=List.copyOf(exact.rankUp().ladderOrder());
  activationListener=new org.bukkit.event.Listener(){
   @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.LOWEST,ignoreCancelled=true)
   public void activate(com.elmakers.mine.bukkit.api.event.WandPreActivateEvent event){
    var wand=event.getWand();if(wand==null||!templates.contains(wand.getTemplateKey()))return;
    try{RawWandLoadBridge.captureAlreadyLoaded(wand);}catch(Exception|LinkageError unavailable){event.setCancelled(true);}
   }
  };
  Bukkit.getPluginManager().registerEvents(activationListener,plugin);
 }
 /** No blanket resolver exception: original archive + committed floor + exact bound subject required. */
 boolean reviewedNativeDefault(LearnedLevelResolver.Request request){
  if(identities==null||!active()||request==null)return false;
  try{
   var original=OriginalWandContext.current(request).orElse(null);if(original==null||!boundOwner(original.mage(),original.wand(),request.player())||!request.wandId().equals(PracticeWandIdentity.get(original.wand()))||request.storedNativeLevel()!=original.wand().getSpellLevel(request.spell()))return false;
   var entry=identities.get(request.wandId());if(entry==null||!request.player().toString().equalsIgnoreCase(entry.owner())||!entry.allowedTemplates().contains(original.wand().getTemplateKey())||!entry.baseline().containsKey(request.spell()))return false;
   var observation=core.observed(request.player(),request.wandId(),request.spell()).orElse(null);
   return observation!=null&&observation.committed()&&observation.capturedRank()==entry.baseline().get(request.spell())&&core.covered(request.player(),request.spell());
  }catch(Exception unavailable){return false;}
 }

 private boolean touchAuthority(FirstObservationFloor.Capture c){
  if(boundFrame!=null&&c.exactAuthority()==boundFrame){var f=boundFrame;return active()&&(f.bootstrap()||MigrationTouchGuard.isCurrent(f.mage(),f.wand()))&&boundOwner(f.mage(),f.wand(),c.owner())&&c.wand().equals(PracticeWandIdentity.get(f.wand()))&&c.nativeRank()==f.wand().getSpellLevel(c.spell());}
  if(!active()||touchFrame==null||c.exactAuthority()!=touchFrame||!c.owner().equals(touchFrame.player())||!c.wand().equals(touchFrame.wandId())||!c.spell().equals(touchFrame.spell()))return false;
  try{var a=OriginalWandContext.current(touchFrame).orElse(null);if(a==null||!(a.mage().getEntity() instanceof Player p)||!p.isOnline()||!p.getUniqueId().equals(c.owner())||magic.getController().getRegisteredMage(p)!=a.mage())return false;
   var w=a.wand();Object owner=w.getClass().getMethod("getOwnerId").invoke(w);return w.isBound()&&w.canUse(p)&&c.wand().equals(PracticeWandIdentity.get(w))&&templates.contains(w.getTemplateKey())&&owner instanceof String id&&c.owner().toString().equalsIgnoreCase(id)&&w.getSpellLevel(c.spell())==c.nativeRank();
  }catch(ReflectiveOperationException|RuntimeException unavailable){return false;}
 }
 boolean beforeUse(LearnedLevelResolver.Request request){
  if(!active()||handle==null||request==null||!maximum.containsKey(request.spell()))return false;
  var prior=touchFrame;touchFrame=request;try{
   var capture=new FirstObservationFloor.Capture(request.player(),request.wandId(),request.spell(),request.storedNativeLevel(),FirstObservationFloor.Input.SAVED_ITEM,request);
   if(!touchAuthority(capture))return false;
   if(core.observed(request.player(),request.wandId(),request.spell()).isEmpty()){
    var original=OriginalWandContext.current(request).orElseThrow();RawWandLoadBridge.captureAlreadyLoaded(original.wand());
   }
   return touchAuthority(capture)&&core.beforeTouch(capture).status()==FirstObservationFloor.Status.CONFIRMED;
  }catch(Exception unavailable){return false;}finally{touchFrame=prior;}
 }
 private boolean boundOwner(Mage mage,Wand wand,UUID owner){
  try{return active()&&mage!=null&&wand!=null&&mage.getEntity() instanceof Player p&&p.isOnline()&&p.getUniqueId().equals(owner)&&magic.getController().getRegisteredMage(p)==mage&&wand.isBound()&&wand.canUse(p)&&templates.contains(wand.getTemplateKey())&&owner.toString().equalsIgnoreCase((String)wand.getClass().getMethod("getOwnerId").invoke(wand));}catch(ReflectiveOperationException|RuntimeException e){return false;}
 }
 private boolean captureAndCommit(Mage mage,Wand wand,boolean bootstrap){
  if(!active()||handle==null)return false;
  if(wand==null||!templates.contains(wand.getTemplateKey()))return true;
  if(!(mage.getEntity() instanceof Player p)||!boundOwner(mage,wand,p.getUniqueId()))return false;
  var prior=boundFrame;boundFrame=new BoundFrame(mage,wand,bootstrap);try{
   // Native bridge reads the actual saved item, never inherited template defaults.
   RawWandLoadBridge.captureAlreadyLoaded(wand);
   for(String spell:new TreeSet<>(maximum.keySet())){
    if(core.observed(p.getUniqueId(),PracticeWandIdentity.get(wand),spell).isEmpty())continue;
    var capture=new FirstObservationFloor.Capture(p.getUniqueId(),PracticeWandIdentity.get(wand),spell,wand.getSpellLevel(spell),FirstObservationFloor.Input.SAVED_ITEM,boundFrame);
    if(!touchAuthority(capture)||core.beforeTouch(capture).status()!=FirstObservationFloor.Status.CONFIRMED)return false;
   }
   return boundOwner(mage,wand,p.getUniqueId())&&(bootstrap||MigrationTouchGuard.isCurrent(mage,wand));
  }catch(Exception unavailable){return false;}finally{boundFrame=prior;}
 }
 /** Only callable from the exact rank-up guard before any template or SP mutation. */
 boolean beforeTemplateTouch(Mage mage,Wand wand){
  if(!MigrationTouchGuard.isCurrent(mage,wand)||!captureAndCommit(mage,wand,false))return false;
  if(identities==null)return true;
  try{int index=templateOrder.indexOf(wand.getTemplateKey());if(index<0||index+1>=templateOrder.size())return false;identities.authorizeTemplate(PracticeWandIdentity.get(wand),wand.getTemplateKey(),templateOrder.get(index+1));return MigrationTouchGuard.isCurrent(mage,wand);}catch(IOException unavailable){return false;}
 }
 /** Snapshot loaded player references, not inventories/chests. Every original identity is visited once. */
 int bootstrapLoaded(){
  if(!active()||handle==null)throw new IllegalStateException("migration owner unavailable");
  int visited=0;Set<Wand> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  for(Mage mage:new ArrayList<>(magic.getMages())){
   if(mage==null||!(mage.getEntity() instanceof Player p)||!p.isOnline())continue;
   List<Wand> originals=new ArrayList<>();originals.addAll(mage.getBoundWands());originals.add(mage.getActiveWand());originals.add(mage.getOffhandWand());
   for(Wand wand:originals){if(wand==null||!seen.add(wand)||!templates.contains(wand.getTemplateKey()))continue;
    if(!captureAndCommit(mage,wand,true))throw new IllegalStateException("loaded wand migration requires reconciliation");visited++;
   }
  }
  return visited;
 }
 boolean covered(UUID player,String spell){if(!active()||handle==null)return false;try{return core.covered(player,spell);}catch(Exception unavailable){return false;}}
 /** Excel: normal native inventory restoration must finish before its raw observer retires. */
 private void flushShutdownWands(){
  if(identities==null||!Bukkit.isStopping())return;
  Set<Wand> seen=Collections.newSetFromMap(new IdentityHashMap<>());
  for(Mage mage:new ArrayList<>(magic.getMages())){
   if(mage==null||!(mage.getEntity() instanceof Player player)||magic.getController().getRegisteredMage(player)!=mage)continue;
   Wand active=mage.getActiveWand(),offhand=mage.getOffhandWand();
   for(Wand wand:new Wand[]{active,offhand}){
    if(wand==null||!seen.add(wand)||!templates.contains(wand.getTemplateKey()))continue;
    if(wand.getClass()!=com.elmakers.mine.bukkit.wand.Wand.class)throw new IllegalStateException("Excel shutdown wand authority unavailable");
    wand.deactivate();
    if(mage.getActiveWand()==wand||mage.getOffhandWand()==wand)throw new IllegalStateException("Excel shutdown wand did not deactivate");
   }
  }
 }
 @Override public void close()throws Exception{if(!Bukkit.isPrimaryThread())throw new IllegalStateException("owner thread required");if(closed)return;flushShutdownWands();closed=true;try{if(handle!=null)handle.close();}finally{handle=null;if(activationListener!=null)org.bukkit.event.HandlerList.unregisterAll(activationListener);core.close();if(identities!=null)identities.close();}}
}
