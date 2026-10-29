package com.winkreyes.makersmark;

import java.util.*;
import org.bukkit.Bukkit;
import org.bukkit.event.*;
import org.bukkit.event.server.PluginDisableEvent;
import com.elmakers.mine.bukkit.magic.MagicPlugin;
import com.elmakers.mine.bukkit.api.event.LoadEvent;
import com.elmakers.mine.bukkit.api.event.PreLoadEvent;
import wrv.magic.evidence.EffectObserver;
import wrv.magic.evidence.WandLoreBridge;

/** Optional sole history owner; never installs practice, learned levels, or SP hooks. */
final class HistoryOnlyMagicRegistration implements Listener,AutoCloseable {
 private final ExcelMakersMark plugin;
 private final MagicPlugin magic;
 private final MakerMagicBridge bridge;
 private AutoCloseable observer,lore;
 private boolean closed,loading;
 private long generation;
 HistoryOnlyMagicRegistration(ExcelMakersMark plugin,MagicPlugin magic,MakerMagicBridge bridge){this.plugin=plugin;this.magic=magic;this.bridge=bridge;}
 void start(){
  Bukkit.getPluginManager().registerEvents(this,plugin);
  scheduleAttach();
 }
 private void scheduleAttach(){long expected=generation;Bukkit.getScheduler().runTask(plugin,()->{if(expected==generation&&!loading)attach();});}
 private void attach(){
  if(closed||loading||!plugin.isEnabled()||!magic.isEnabled()||observer!=null||lore!=null)return;
  if(!Bukkit.isPrimaryThread()){plugin.getLogger().severe("History-only Magic registration refused off server thread");return;}
  AutoCloseable acquired=null;
  try{
   Set<String> keys=new TreeSet<>();
   for(var spell:magic.getAPI().getSpellTemplates(true))if(spell!=null&&spell.getSpellKey()!=null){String key=spell.getSpellKey().getBaseKey();if(key!=null&&!key.isBlank())keys.add(key);}
   if(keys.isEmpty())throw new IllegalStateException("Native spell registry is not ready; awaiting Magic LoadEvent");
   acquired=EffectObserver.installOwned(Set.copyOf(keys),bridge);
   AutoCloseable installedLore=WandLoreBridge.install(bridge);
   observer=acquired;lore=installedLore;
   plugin.getLogger().info("History-only Magic registration ACTIVE for "+keys.size()+" loaded spell base keys. Learned/practice/SP hooks not installed by this owner.");
  }catch(LinkageError|RuntimeException failure){
   if(acquired!=null)try{acquired.close();}catch(Exception closeFailure){plugin.getLogger().severe("History-only partial registration cleanup failed: "+closeFailure.getClass().getSimpleName());}
   plugin.getLogger().severe("History-only Magic registration INACTIVE: "+failure.getClass().getSimpleName()+": "+failure.getMessage()+". Existing observer owners are never replaced; ordinary combat remains unchanged.");
  }
 }
 @EventHandler(priority=EventPriority.MONITOR)public void preLoad(PreLoadEvent e){if(e.getController()==magic.getController()){loading=true;generation++;detach();}}
 @EventHandler(priority=EventPriority.MONITOR)public void loaded(LoadEvent e){if(e.getController()==magic.getController()&&!closed){loading=false;generation++;scheduleAttach();}}
 @EventHandler(priority=EventPriority.MONITOR)public void disabled(PluginDisableEvent e){if(e.getPlugin()==magic){loading=true;generation++;detach();}}
 private void detach(){
  AutoCloseable oldLore=lore,oldObserver=observer;lore=null;observer=null;
  for(AutoCloseable handle:Arrays.asList(oldLore,oldObserver))if(handle!=null)try{handle.close();}catch(Exception failure){plugin.getLogger().severe("History-only registration detach failed: "+failure.getClass().getSimpleName());}
 }
 @Override public void close(){closed=true;generation++;HandlerList.unregisterAll(this);detach();}
}
