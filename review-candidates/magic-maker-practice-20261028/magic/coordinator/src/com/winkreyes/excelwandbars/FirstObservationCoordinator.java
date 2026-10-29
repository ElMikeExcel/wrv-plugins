package com.winkreyes.excelwandbars;
import java.util.*;import java.nio.file.*;import java.io.*;import java.util.function.*;
import org.bukkit.Bukkit;import org.bukkit.entity.Player;
import com.elmakers.mine.bukkit.api.magic.MagicAPI;
import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.wand.Wand;
import wrv.magic.level.*;import wrv.magic.retention.BoundedPractice;
/** Explicit offline/prepared owner. No onEnable registration or default policy activation. */
final class FirstObservationCoordinator implements AutoCloseable {
 private final MagicAPI magic;private final Set<String> templates;private final Map<String,Integer> maximum;private final Function<String,String> canonical;private final FirstObservationFloor core;private AutoCloseable handle;private boolean closed;
 private RawWandLoadBridge.Snapshot rawFrame;private LearnedLevelResolver.Request touchFrame;
 private record BoundFrame(Mage mage,Wand wand,boolean bootstrap){}
 private BoundFrame boundFrame;
 FirstObservationCoordinator(MagicAPI magic,Set<String> templates,Map<String,Integer> maximum,Function<String,String> canonical,
    Path journal,FirstObservationFloor.Policy policy,Map<String,BoundedPractice> ledgers)throws IOException{
  this.magic=Objects.requireNonNull(magic);this.templates=Set.copyOf(templates);this.maximum=Map.copyOf(maximum);this.canonical=Objects.requireNonNull(canonical);
  if(this.templates.isEmpty()||!this.maximum.keySet().equals(ledgers.keySet()))throw new IllegalArgumentException("explicit ladder/managed ledger scope required");
  this.core=new FirstObservationFloor(journal,policy,ledgers,new FirstObservationFloor.Authority(){public boolean exactSavedCapture(FirstObservationFloor.Capture c){return rawAuthority(c);}public boolean exactBoundTouch(FirstObservationFloor.Capture c){return touchAuthority(c);}});
 }
 void start(){if(closed||handle!=null||!Bukkit.isPrimaryThread())throw new IllegalStateException("owner unavailable");var scopeTemplates=templates;handle=RawWandLoadBridge.installOwned(this::onRawSnapshot,raw->raw.existingWand()&&!raw.upgrade()&&scopeTemplates.contains(raw.template())&&!Boolean.FALSE.equals(raw.bound()));}
 private boolean active(){return !closed&&Bukkit.isPrimaryThread();}
 private UUID rawOwner(RawWandLoadBridge.Snapshot r){try{UUID u=UUID.fromString(r.ownerId());return u.toString().equalsIgnoreCase(r.ownerId())?u:null;}catch(RuntimeException bad){return null;}}
 private boolean rawAuthority(FirstObservationFloor.Capture c){var r=rawFrame;return active()&&r!=null&&c.exactAuthority()==r&&RawWandLoadBridge.isCurrentSnapshot(r)&&r.wellFormed()&&r.existingWand()&&!r.upgrade()&&!Boolean.FALSE.equals(r.bound())&&templates.contains(r.template())&&c.owner().equals(rawOwner(r))&&c.wand().equals(r.id());}
 boolean onRawSnapshot(RawWandLoadBridge.Snapshot raw){
  if(!active()||raw==null||!RawWandLoadBridge.isCurrentSnapshot(raw))return false;
  if(!raw.existingWand()||raw.upgrade()||!templates.contains(raw.template())||Boolean.FALSE.equals(raw.bound()))return true;
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
 private boolean touchAuthority(FirstObservationFloor.Capture c){
  if(boundFrame!=null&&c.exactAuthority()==boundFrame){var f=boundFrame;return active()&&(f.bootstrap()||MigrationTouchGuard.isCurrent(f.mage(),f.wand()))&&boundOwner(f.mage(),f.wand(),c.owner())&&c.wand().equals(f.wand().getId())&&c.nativeRank()==f.wand().getSpellLevel(c.spell());}
  if(!active()||touchFrame==null||c.exactAuthority()!=touchFrame||!c.owner().equals(touchFrame.player())||!c.wand().equals(touchFrame.wandId())||!c.spell().equals(touchFrame.spell()))return false;
  try{var a=OriginalWandContext.current(touchFrame).orElse(null);if(a==null||!(a.mage().getEntity() instanceof Player p)||!p.isOnline()||!p.getUniqueId().equals(c.owner())||magic.getController().getRegisteredMage(p)!=a.mage())return false;
   var w=a.wand();Object owner=w.getClass().getMethod("getOwnerId").invoke(w);return w.isBound()&&w.canUse(p)&&c.wand().equals(w.getId())&&templates.contains(w.getTemplateKey())&&owner instanceof String id&&c.owner().toString().equalsIgnoreCase(id)&&w.getSpellLevel(c.spell())==c.nativeRank();
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
    if(core.observed(p.getUniqueId(),wand.getId(),spell).isEmpty())continue;
    var capture=new FirstObservationFloor.Capture(p.getUniqueId(),wand.getId(),spell,wand.getSpellLevel(spell),FirstObservationFloor.Input.SAVED_ITEM,boundFrame);
    if(!touchAuthority(capture)||core.beforeTouch(capture).status()!=FirstObservationFloor.Status.CONFIRMED)return false;
   }
   return boundOwner(mage,wand,p.getUniqueId())&&(bootstrap||MigrationTouchGuard.isCurrent(mage,wand));
  }catch(Exception unavailable){return false;}finally{boundFrame=prior;}
 }
 /** Only callable from the exact rank-up guard before any template or SP mutation. */
 boolean beforeTemplateTouch(Mage mage,Wand wand){return MigrationTouchGuard.isCurrent(mage,wand)&&captureAndCommit(mage,wand,false);}
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
 @Override public void close()throws Exception{if(!Bukkit.isPrimaryThread())throw new IllegalStateException("owner thread required");if(closed)return;closed=true;try{if(handle!=null)handle.close();}finally{handle=null;core.close();}}
}
