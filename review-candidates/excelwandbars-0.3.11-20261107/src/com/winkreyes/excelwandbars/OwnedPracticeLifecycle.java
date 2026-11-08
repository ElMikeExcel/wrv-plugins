package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.Consumer;import java.util.function.BooleanSupplier;
import org.bukkit.Bukkit;import org.bukkit.event.*;import org.bukkit.event.server.*;import org.bukkit.plugin.Plugin;import org.bukkit.scheduler.BukkitTask;
import com.elmakers.mine.bukkit.api.event.*;import wrv.magic.evidence.SharedHistoryProvider;
/** Explicit reviewed startup only. Each real lifecycle event permits one deferred attempt, never a retry loop. */
final class OwnedPracticeLifecycle implements Listener,AutoCloseable {
 @FunctionalInterface interface PreparedFactory {AutoCloseable open() throws Exception;}
 private final Plugin plugin,magic;private final Object controller;private final PreparedFactory factory;private final Consumer<Throwable> failures;private final BooleanSupplier ready;private final Set<String> readinessPlugins;
 private boolean started,closed,loading,cleanupFailed;private long generation;private BukkitTask pending;private AutoCloseable runtime;
 OwnedPracticeLifecycle(Plugin plugin,Plugin magic,Object controller,PreparedFactory factory,Consumer<Throwable> failures){
  this(plugin,magic,controller,factory,failures,()->true);
 }
 OwnedPracticeLifecycle(Plugin plugin,Plugin magic,Object controller,PreparedFactory factory,Consumer<Throwable> failures,BooleanSupplier ready){
  this(plugin,magic,controller,factory,failures,ready,Set.of());
 }
 OwnedPracticeLifecycle(Plugin plugin,Plugin magic,Object controller,PreparedFactory factory,Consumer<Throwable> failures,BooleanSupplier ready,Set<String> readinessPlugins){
  this.readinessPlugins=Set.copyOf(readinessPlugins);
  this.ready=Objects.requireNonNull(ready);
  this.plugin=Objects.requireNonNull(plugin);this.magic=Objects.requireNonNull(magic);this.controller=Objects.requireNonNull(controller);this.factory=Objects.requireNonNull(factory);this.failures=Objects.requireNonNull(failures);
 }
 private void thread(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("server owner thread required");}
 void start(){thread();if(started||closed)throw new IllegalStateException("already started/closed");started=true;try{Bukkit.getPluginManager().registerEvents(this,plugin);schedule();}catch(Throwable t){close();throw t;}}
 private void report(Throwable t){try{failures.accept(t);}catch(Throwable ignored){}}
 private void schedule(){
  thread();if(closed||loading||cleanupFailed||pending!=null||runtime!=null)return;try{if(!ready.getAsBoolean())return;}catch(Throwable unavailable){report(unavailable);return;}long expected=generation;
  pending=Bukkit.getScheduler().runTask(plugin,()->{
   pending=null;if(closed||loading||cleanupFailed||generation!=expected||!plugin.isEnabled()||!magic.isEnabled())return;
   AutoCloseable acquired=null;
   try{if(!ready.getAsBoolean())return;acquired=Objects.requireNonNull(factory.open());if(closed||loading||generation!=expected){var obsolete=acquired;acquired=null;try{obsolete.close();}catch(Throwable cleanup){cleanupFailed=true;throw cleanup;}return;}runtime=acquired;}
   catch(Throwable t){if(acquired!=null)try{acquired.close();}catch(Throwable cleanup){cleanupFailed=true;t.addSuppressed(cleanup);}if(t.getSuppressed().length>0)cleanupFailed=true;report(t);}
  });
 }
 private void retire(){
  thread();generation++;if(pending!=null){pending.cancel();pending=null;}var old=runtime;runtime=null;
  if(old!=null)try{old.close();}catch(Throwable failure){cleanupFailed=true;report(failure);}
 }
 @EventHandler(priority=EventPriority.MONITOR)public void preLoad(PreLoadEvent e){if(!closed&&e.getController()==controller){loading=true;retire();}}
 @EventHandler(priority=EventPriority.MONITOR)public void loaded(LoadEvent e){if(!closed&&e.getController()==controller){loading=false;retire();schedule();}}
 @EventHandler(priority=EventPriority.MONITOR)public void registered(ServiceRegisterEvent e){if(!closed&&e.getProvider().getService()==SharedHistoryProvider.class){retire();schedule();}}
 @EventHandler(priority=EventPriority.MONITOR)public void unregistered(ServiceUnregisterEvent e){if(!closed&&e.getProvider().getService()==SharedHistoryProvider.class)retire();}
 @EventHandler(priority=EventPriority.MONITOR)public void enabled(PluginEnableEvent e){if(!closed&&e.getPlugin().getName()!=null&&readinessPlugins.contains(e.getPlugin().getName()))schedule();}
 @EventHandler(priority=EventPriority.MONITOR)public void disabled(PluginDisableEvent e){if(closed)return;if(e.getPlugin()==plugin){close();return;}if(e.getPlugin()==magic){loading=true;retire();return;}if(e.getPlugin().getName()!=null&&readinessPlugins.contains(e.getPlugin().getName()))retire();}
 @Override public void close(){thread();if(closed)return;closed=true;retire();HandlerList.unregisterAll(this);}
}
