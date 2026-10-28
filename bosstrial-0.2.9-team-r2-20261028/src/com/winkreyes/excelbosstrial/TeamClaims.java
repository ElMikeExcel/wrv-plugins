package com.winkreyes.excelbosstrial;

import com.winkreyes.teamrewards.encounter.TeamAdmission;
import com.winkreyes.teamrewards.ui.*;
import wrv.rewards.TeamRewardLedger;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.IOException;
import java.util.*;

/** Offline candidate adapter; personal claims only. Does not adjudicate fights or distribute shared drops. */
final class TeamClaims implements AutoCloseable {
 final TeamRewardLedger ledger;
 private final TeamAdmission admission;
 private final PersonalClaimsUi ui;
 private com.winkreyes.teamrewards.marker.MarkerRegistry markerRegistry;
 private com.winkreyes.teamrewards.marker.PersonalChestMarkers markers;
 TeamClaims(JavaPlugin plugin,Set<String> alreadyPaid)throws IOException{
  ledger=new TeamRewardLedger(plugin.getDataFolder().toPath().resolve("team-rewards.dat"));
  try{
   for(String seal:alreadyPaid)ledger.importConsumedScope("signature:"+seal);
   admission=new TeamAdmission(ledger::qualify);
   LedgerClaimGateway delivery = new LedgerClaimGateway(ledger,claim->{
    List<ItemStack> items=new ArrayList<>();
    for(var unit:claim.units()){
     if(!unit.payload().startsWith("paper-item-v1:"))throw new IllegalArgumentException("unknown reward encoding");
     byte[] bytes=Base64.getDecoder().decode(unit.payload().substring(14));
     if(bytes.length>14000)throw new IllegalArgumentException("oversized reward");
     ItemStack item=ItemStack.deserializeBytes(bytes);
     if(item.getType().isAir()||item.getAmount()<=0)throw new IllegalArgumentException("invalid reward");
     items.add(item);
    }
    return items;
   },TeamClaims::normalInventory);
   ui=new PersonalClaimsUi(plugin,new ClaimGateway(){
    public java.util.List<Pending> pending(UUID owner){return delivery.pending(owner);}
    public Result claim(UUID owner,String id){
     Result result=delivery.claim(owner,id);
     if(result==Result.DELIVERED)cleanupClaimedMarker(owner);
     return result;
    }
   });
   Bukkit.getPluginManager().registerEvents(ui,plugin);
   var command=plugin.getCommand("bossrewards");
   if(command==null)throw new IOException("missing owner reward command");
   command.setExecutor(ui);
   try {
    var resolved = com.winkreyes.teamrewards.marker.ReviewedWorlds.resolve(
      plugin.getConfig().get("team-rewards.markers.reviewed-world-uuids"),
      plugin.getConfig().get("team-rewards.markers.reviewed-world-names"),
      plugin.getServer().getWorlds().stream().map(w -> new com.winkreyes.teamrewards.marker.ReviewedWorlds.Loaded(w.getName(), w.getUID())).toList());
    Set<UUID> reviewed = resolved.ids();
    for(var world : resolved.worlds()) plugin.getLogger().info("Reviewed personal-marker world: " + world.name() + " -> " + world.id());
    markerRegistry = new com.winkreyes.teamrewards.marker.MarkerRegistry(plugin.getDataFolder().toPath().resolve("personal-markers.dat"));
    markers = new com.winkreyes.teamrewards.marker.PersonalChestMarkers(plugin,markerRegistry,
      new com.winkreyes.teamrewards.marker.GriefPreventionWildernessGuard(plugin.getServer(),reviewed),ui::open);
    Bukkit.getPluginManager().registerEvents(markers,plugin);
    if(reviewed.isEmpty()) plugin.getLogger().info("Personal chest markers use menu fallback until world protection policy is reviewed.");
   } catch(Exception markerFailure) {
    markers=null;
    if(markerRegistry!=null){try{markerRegistry.close();}catch(Exception ignored){}markerRegistry=null;}
    plugin.getLogger().warning("Personal marker unavailable; durable reward menu remains available: "+markerFailure.getMessage());
   }
  }catch(Exception e){ledger.close();throw new IOException("team rewards initialization failed",e);}
 }
 private static boolean normalInventory(Player player){
  if(player.isDead()||(player.getGameMode()!=GameMode.SURVIVAL&&player.getGameMode()!=GameMode.ADVENTURE))return false;
  var magic=Bukkit.getPluginManager().getPlugin("Magic");
  if(magic==null)return true;
  if(!magic.isEnabled())return false;
  try{
   Object mage=magic.getClass().getMethod("getMage",org.bukkit.command.CommandSender.class).invoke(magic,player);
   if(mage==null)return false;
   if((Boolean)mage.getClass().getMethod("hasStoredInventory").invoke(mage))return false;
   for(String method:List.of("getActiveWand","getOffhandWand")){
    Object wand=mage.getClass().getMethod(method).invoke(mage);
    if(wand!=null&&(Boolean)wand.getClass().getMethod("isInventoryOpen").invoke(wand))return false;
   }
   return true;
  }catch(ReflectiveOperationException|RuntimeException unavailable){return false;}
 }
 void observe(String run,TeamAdmission.Kind kind,LivingEntity boss,double range)throws IOException{
  if(boss.isDead()||!boss.isValid())return;
  ledger.openEncounter(run);
  var loc=boss.getLocation();var world=boss.getWorld();
  var center=new TeamAdmission.Point(world.getUID(),loc.getX(),loc.getY(),loc.getZ());
  List<TeamAdmission.PlayerView> players=new ArrayList<>();
  for(Player player:world.getPlayers()){
   var p=player.getLocation();
   players.add(new TeamAdmission.PlayerView(player.getUniqueId(),new TeamAdmission.Point(world.getUID(),p.getX(),p.getY(),p.getZ()),TeamAdmission.Mode.valueOf(player.getGameMode().name()),player.isOnline(),!player.isDead()));
  }
  admission.observe(new TeamAdmission.Encounter(run,kind,Set.of(boss.getUniqueId()),true,center,range),players);
 }
 static String encode(ItemStack item){
  byte[] bytes=item.serializeAsBytes();if(bytes.length>14000)throw new IllegalArgumentException("reward item too large");
  return "paper-item-v1:"+Base64.getEncoder().encodeToString(bytes);
 }
 private void cleanupClaimedMarker(UUID owner){
  if(markers==null||markerRegistry==null)return;
  try{
   boolean outstanding=ledger.claims(owner).stream().anyMatch(c->!c.units().isEmpty()&&c.state()!=TeamRewardLedger.State.DELIVERED);
   if(!outstanding)for(var marker:markerRegistry.all())if(marker.owner().equals(owner))markers.cleanup(marker.id());
  }catch(RuntimeException unavailable){/* Never infer safe cleanup from unavailable authoritative records. */}
 }
 void offerMarkers(Collection<UUID> owners,Location anchor){
  for(UUID owner:owners){
   boolean placed=false;
   if(markers!=null)try{placed=markers.placeNear(owner,anchor)==com.winkreyes.teamrewards.marker.PersonalChestMarkers.Placement.PLACED;}catch(RuntimeException unavailable){/* menu remains authoritative */}
   Player player=Bukkit.getPlayer(owner);
   if(player!=null&&player.isOnline())player.sendMessage(placed?"Your personal reward chest is nearby. Only you can open it; /bossrewards also works.":"Your boss reward is safely waiting in /bossrewards.");
  }
 }
 @Override public void close()throws IOException{
  try { ui.shutdown(); }
  finally { try{if(markerRegistry!=null)markerRegistry.close();}finally{ledger.close();} }
 }
}
