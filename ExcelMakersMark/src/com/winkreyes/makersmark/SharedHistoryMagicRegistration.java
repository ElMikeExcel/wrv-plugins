package com.winkreyes.makersmark;
import java.util.*;import java.util.function.Supplier;
import org.bukkit.Bukkit;import org.bukkit.event.*;import org.bukkit.event.server.PluginDisableEvent;import org.bukkit.plugin.*;
import com.elmakers.mine.bukkit.magic.MagicPlugin;import com.elmakers.mine.bukkit.api.event.*;
import wrv.magic.evidence.*;
/** Explicit shared mode. Publishes a typed binding; never installs or replaces EffectObserver. */
final class SharedHistoryMagicRegistration implements Listener,AutoCloseable,SharedHistoryProvider {
 private final Plugin plugin,magic;private final Object controller;private final Supplier<Set<String>> spellKeys;
 private final EffectObserver.Sink sink;private final WandLoreBridge.Decorator decorator;
 private boolean started,closed,loading;private long generation;private OwnedLease current;
 SharedHistoryMagicRegistration(ExcelMakersMark plugin,MagicPlugin magic,MakerMagicBridge bridge){
  this(plugin,magic,magic.getController(),()->{Set<String> keys=new TreeSet<>();for(var s:magic.getAPI().getSpellTemplates(true))if(s!=null&&s.getSpellKey()!=null)keys.add(s.getSpellKey().getBaseKey());return Set.copyOf(keys);},bridge,bridge);
 }
 SharedHistoryMagicRegistration(Plugin plugin,Plugin magic,Object controller,Supplier<Set<String>> keys,EffectObserver.Sink sink,WandLoreBridge.Decorator decorator){
  this.plugin=Objects.requireNonNull(plugin);this.magic=Objects.requireNonNull(magic);this.controller=Objects.requireNonNull(controller);spellKeys=Objects.requireNonNull(keys);this.sink=Objects.requireNonNull(sink);this.decorator=Objects.requireNonNull(decorator);
 }
 private void thread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner required");}
 void start(){thread();if(started||closed)throw new IllegalStateException("registration already started/closed");
  // Refuse ambiguity instead of silently winning service priority over another provider.
  if(!Bukkit.getServicesManager().getRegistrations(SharedHistoryProvider.class).isEmpty())throw new IllegalStateException("history provider already present");
  started=true;
  try{Bukkit.getPluginManager().registerEvents(this,plugin);Bukkit.getServicesManager().register(SharedHistoryProvider.class,this,plugin,ServicePriority.Normal);}
  catch(RuntimeException|Error failure){HandlerList.unregisterAll(this);closed=true;throw failure;}
 }
 @Override public SharedHistoryProvider.Lease acquire(Runnable invalidated){
  thread();if(!started||closed||loading||!plugin.isEnabled()||!magic.isEnabled()||current!=null)throw new IllegalStateException("history generation unavailable");
  Objects.requireNonNull(invalidated);Set<String> keys=Set.copyOf(spellKeys.get());if(keys.isEmpty()||keys.stream().anyMatch(k->!k.matches("[a-z0-9_.-]+")))throw new IllegalStateException("native registry unavailable");
  // WandLoreBridge refuses another owner; no takeover or detach is attempted.
  var lore=WandLoreBridge.install(decorator);var lease=new OwnedLease(generation,keys,Objects.requireNonNull(invalidated),lore);current=lease;return lease;
 }
 private final class OwnedLease implements SharedHistoryProvider.Lease {
  private final long epoch;private final Runnable invalidated;private final AutoCloseable lore;private boolean ended;
  private final ConsumerFanout.Binding binding;
  OwnedLease(long epoch,Set<String> keys,Runnable invalidated,AutoCloseable lore){this.epoch=epoch;this.invalidated=invalidated;this.lore=lore;
   binding=new ConsumerFanout.Binding("makersmark-history",keys,new EffectObserver.Sink(){
    public void observe(EffectObserver.Receipt r){if(active())sink.observe(r);}
    public void rootCaptured(EffectObserver.RootEvidence r){if(active())sink.rootCaptured(r);}
    public void castStarted(EffectObserver.RootEvidence r){if(active())sink.castStarted(r);}
    public void rootFinished(EffectObserver.RootEvidence r){if(active())sink.rootFinished(r);}
    public void damageStarted(EffectObserver.PendingDamage r){if(active())sink.damageStarted(r);}
    public void damageCompleted(EffectObserver.DamageCompletion r){if(active())sink.damageCompleted(r);}
   });
  }
  public boolean active(){return Bukkit.isPrimaryThread()&&!ended&&!closed&&!loading&&epoch==generation&&current==this&&plugin.isEnabled()&&magic.isEnabled();}
  public ConsumerFanout.Binding binding(){thread();if(!active())throw new IllegalStateException("stale history lease");return binding;}
  public void close(){thread();if(ended)return;ended=true;if(current==this)current=null;try{lore.close();}catch(Exception e){throw new IllegalStateException("history lore cleanup failed",e);}}
  void invalidate(){try{close();}finally{invalidated.run();}}
 }
 private void invalidate(){generation++;var old=current;if(old!=null)try{old.invalidate();}catch(Throwable e){plugin.getLogger().severe("Shared history owner cleanup failed: "+e);}}
 @EventHandler(priority=EventPriority.MONITOR)public void preLoad(PreLoadEvent e){if(!closed&&e.getController()==controller){loading=true;invalidate();}}
 @EventHandler(priority=EventPriority.MONITOR)public void loaded(LoadEvent e){if(!closed&&e.getController()==controller){loading=false;invalidate();}}
 @EventHandler(priority=EventPriority.MONITOR)public void disabled(PluginDisableEvent e){if(!closed&&e.getPlugin()==magic){loading=true;invalidate();}}
 @Override public void close(){thread();if(closed)return;closed=true;invalidate();Bukkit.getServicesManager().unregister(SharedHistoryProvider.class,this);HandlerList.unregisterAll(this);}
}
