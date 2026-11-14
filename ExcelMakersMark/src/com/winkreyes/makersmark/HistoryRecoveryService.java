package com.winkreyes.makersmark;
import com.winkreyes.makersmark.history.*;
import java.util.*;import java.io.*;import java.nio.file.*;
import org.bukkit.Bukkit;import org.bukkit.command.CommandSender;import org.bukkit.entity.Player;
import org.bukkit.event.*;import org.bukkit.event.player.PlayerJoinEvent;import org.bukkit.inventory.ItemStack;
/** Experimental recovery module. No new archives are minted and no item snapshots are restored. */
final class HistoryRecoveryService implements Listener,AutoCloseable {
 private final ExcelMakersMark plugin;private final HistoryLedger ledger;private final PendingRecovery recovery;
 HistoryRecoveryService(ExcelMakersMark plugin)throws IOException{
  this.plugin=plugin;Path directory=plugin.getDataFolder().toPath().resolve("history");Files.createDirectories(directory);
  ledger=new HistoryLedger(directory.resolve("history.mmh2"));recovery=new PendingRecovery(ledger,"excelmakersmark");
 }
 HistoryLedger ledger(){return ledger;}
 void start(){plugin.getServer().getPluginManager().registerEvents(this,plugin);Bukkit.getScheduler().runTask(plugin,()->{for(Player p:Bukkit.getOnlinePlayers())check(p);});}
 @EventHandler(priority=EventPriority.MONITOR)public void join(PlayerJoinEvent e){Bukkit.getScheduler().runTask(plugin,()->check(e.getPlayer()));}
 private List<ItemStack> observed(){if(!Bukkit.isPrimaryThread())throw new IllegalStateException("Recovery requires server thread");List<ItemStack> out=new ArrayList<>();for(Player p:Bukkit.getOnlinePlayers()){
  for(ItemStack item:p.getInventory().getContents())add(out,item);add(out,p.getItemOnCursor());for(ItemStack item:p.getEnderChest().getContents())add(out,item);
 }return out;}
 private static void add(List<ItemStack> out,ItemStack item){if(item!=null)out.add(item.clone());}
 private void check(Player player){if(!player.isOnline())return;if(plugin.historyRecorder()!=null)plugin.historyRecorder().resume();int resolved=0,held=0;try{
  List<ItemStack> seen=observed();for(UUID receipt:ledger.unresolved()){
   var bytes=ledger.pendingContext(receipt);if(bytes.isEmpty())continue;RecoveryContext context;
   try{context=RecoveryContext.decode(bytes.get());}catch(IOException e){plugin.getLogger().warning("Malformed history recovery context: "+receipt);continue;}
   var priorPlan=ledger.pendingPlan(receipt);var answer=recovery.retry(receipt,seen);if(answer.outcome()==PendingRecovery.Outcome.COMMITTED){resolved++;if(plugin.historyRecorder()!=null&&priorPlan.isPresent())plugin.historyRecorder().refresh(priorPlan.get().output().item());plugin.getLogger().info("History receipt finalized: "+receipt);}
   else if(context.initiator().equals(player.getUniqueId()))held++;
  }
  if(resolved>0)player.sendMessage("Item history recovery finalized "+resolved+" verified receipt(s). No gear was reconstructed; its history tooltip may refresh.");
  if(held>0)player.sendMessage("You have "+held+" pending item-history receipt(s). Your history is retained; ask an admin to inspect before repeating the merge.");
 }catch(Exception e){plugin.getLogger().severe("History recovery stopped safely: "+e.getMessage());player.sendMessage("Item history needs administrator review; no automatic item changes were made.");}}
 boolean command(CommandSender sender,String[] args){
  if(!sender.hasPermission("excelmakersmark.admin")){sender.sendMessage("Administrator permission required for history recovery.");return true;}
  if(!Bukkit.isPrimaryThread()){sender.sendMessage("History recovery requires the server thread.");return true;}
  try{
   String action=args.length>1?args[1].toLowerCase(Locale.ROOT):"pending";
   if(action.equals("resume")){if(plugin.historyRecorder()!=null){plugin.historyRecorder().resume();sender.sendMessage("Durable ordinary-history inbox retry queued for currently observable weapons.");}else sender.sendMessage("Ordinary history is disabled.");return true;}
   if(action.equals("pending")){sender.sendMessage("Durable ordinary award inbox: "+ledger.pendingAwards().size());var pending=new TreeSet<>(ledger.unresolved());sender.sendMessage("Pending item-history receipts: "+pending.size()+". Scan scope: online inventories, cursors and ender chests only.");int n=0;for(UUID id:pending){if(n++==20){sender.sendMessage("More receipts exist. Inspect by full receipt UUID.");break;}sender.sendMessage(id+" | transfer proof: "+ledger.physicalProof(id).isPresent());}return true;}
   if(args.length!=3||!Set.of("inspect","retry").contains(action)){sender.sendMessage("/mark history pending | inspect <receipt UUID> | retry <receipt UUID>");return true;}
   UUID id=UUID.fromString(args[2]);var plan=ledger.pendingPlan(id);
   if(action.equals("inspect")){sender.sendMessage("Receipt "+id+": "+ledger.status(id));if(plan.isPresent()){sender.sendMessage("Output "+plan.get().output().item()+" revision "+plan.get().output().revision()+"; inputs "+plan.get().inputs().size()+"; durable transfer proof "+ledger.physicalProof(id).isPresent());var context=ledger.pendingContext(id);if(context.isPresent()){var c=RecoveryContext.decode(context.get());sender.sendMessage("Initiator "+c.initiator()+"; workstation "+c.workstation());}else sender.sendMessage("Original pickup context unavailable; automatic retry held.");}return true;}
   var result=recovery.retry(id,observed());if(result.outcome()==PendingRecovery.Outcome.COMMITTED&&plugin.historyRecorder()!=null&&plan.isPresent())plugin.historyRecorder().refresh(plan.get().output().item());sender.sendMessage(result.outcome()+": "+result.reason());sender.sendMessage("No gear was reconstructed or rolled back; its history tooltip may refresh. Offline/unloaded copies are outside this scan; no force merge or rollback is available.");return true;
  }catch(Exception e){sender.sendMessage("History recovery held: "+e.getMessage());return true;}
 }
 public void close()throws IOException{ledger.close();}
}
