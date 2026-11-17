package com.winkreyes.wanddiagnostic;
import java.util.*;
import java.nio.file.*;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;
/** Temporary diagnostic only; no gameplay API writes. Console-only and configured owner only. */
public final class WandDiagnostic extends JavaPlugin implements Listener {
 private final Window window=new Window(()->System.nanoTime()/1_000_000L);
 private Observe observer;
 public void onEnable(){saveDefaultConfig();Bukkit.getPluginManager().registerEvents(this,this);Bukkit.getScheduler().runTaskTimer(this,()->{if(observer!=null&&!window.running())finish();},1L,1L);}
 public void onDisable(){window.stop(Window.End.DISABLED);finish();}
 private void finish(){if(observer==null)return;List<String> saved=new ArrayList<>(window.snapshot());saved.addAll(observer.ownership());observer.restore();observer=null;try{Files.write(getDataFolder().toPath().resolve("last-snapshot.txt"),saved);}catch(Exception e){getLogger().warning("Could not save sanitized diagnostic snapshot.");}}
 public boolean onCommand(CommandSender sender,Command cmd,String label,String[] args){
  if(!(sender instanceof ConsoleCommandSender)){sender.sendMessage("Console only.");return true;}
  if(args.length==1&&args[0].equalsIgnoreCase("show")){for(String line:window.snapshot())sender.sendMessage(line);if(observer!=null)observer.ownership().forEach(sender::sendMessage);return true;}
  if(args.length==1&&args[0].equalsIgnoreCase("stop")){window.stop(Window.End.MANUAL);finish();sender.sendMessage("Diagnostic stopped.");return true;}
  if(args.length==1&&args[0].equalsIgnoreCase("start")){
   if(observer!=null){sender.sendMessage("Already active; stop first.");return true;}
   UUID owner;try{owner=UUID.fromString(getConfig().getString("owner-uuid",""));}catch(Exception e){sender.sendMessage("Disabled: explicitly configure owner's UUID first.");return true;}
   Player player=Bukkit.getPlayer(owner);if(player==null||!player.isOnline()){sender.sendMessage("Owner must be online.");return true;}
   window.start(owner);observer=new Observe(window);
   try{observer.install();}catch(Throwable e){window.stop(Window.End.MANUAL);finish();sender.sendMessage("Observer install failed; restored owned registrations.");return true;}
   sender.sendMessage("Owner-only diagnostic active for at most 60 seconds, 64 sanitized events. No control changes.");return true;
  }
  sender.sendMessage("wanddiag start|show|stop (console only, configured owner only)");return true;
 }
 private void state(Player p,Window.Kind kind,int a,int b,Enum<?> category){UUID id=p.getUniqueId();if(!window.active(id))return;window.note(id,kind,a,b,category,null,true);window.note(id,Window.Kind.STATE,p.getInventory().getHeldItemSlot(),p.isSneaking()?1:0,p.getOpenInventory().getType(),null,true);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void swing(PlayerAnimationEvent e){state(e.getPlayer(),Window.Kind.SWING,e.isCancelled()?1:0,0,e.getAnimationType());}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void interact(PlayerInteractEvent e){state(e.getPlayer(),Window.Kind.INTERACT,e.isCancelled()?1:0,0,e.getAction());}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void sneak(PlayerToggleSneakEvent e){state(e.getPlayer(),Window.Kind.SNEAK,e.isSneaking()?1:0,e.isCancelled()?1:0,null);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void held(PlayerItemHeldEvent e){state(e.getPlayer(),Window.Kind.HELD,e.getPreviousSlot(),e.getNewSlot(),null);}
 @EventHandler(priority=EventPriority.MONITOR) public void quit(PlayerQuitEvent e){if(window.active(e.getPlayer().getUniqueId())){window.logout(e.getPlayer().getUniqueId());finish();}}
}
