/* Excel WRV maintenance; existing authorship retained. */ package com.winkreyes.excelbosstrial;

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
final class TeamClaims implements AutoCloseable, org.bukkit.event.Listener {
 final TeamRewardLedger ledger;
 private final TeamAdmission admission;
 private final PersonalClaimsUi ui;
 private final JavaPlugin plugin;
 private final ClaimGateway gateway;
 private final ReviewPresentation presentation;
 private final ReviewFrames frames;
 private final long verdictDelayTicks;
 private final long joinDelayTicks;
 private final int frameMaxTicks;
 private final boolean popInvisibilityPotion;
 // 0.3.0 (SPEC section 5): outcome logging + the retry queue.
 private final long presentRetryTicks;
 private final long presentRetryMaxMs;
 private final boolean presentOutcomeLogging;
 private final String msgRewardsWaiting;
 /** One owner's retry state: when this batch first failed, and
  *  which batch (pending claim ids) the failures belong to. */
 private record PresentRetry(long firstFailMs,java.util.Set<String> batchIds,boolean hinted){}
 private final java.util.Map<UUID,PresentRetry> presentRetries=new java.util.HashMap<>();
 private com.winkreyes.teamrewards.marker.MarkerRegistry markerRegistry;
 private com.winkreyes.teamrewards.marker.PersonalChestMarkers markers;
 TeamClaims(JavaPlugin plugin,Set<String> alreadyPaid)throws IOException{
  this.plugin=plugin;
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
   gateway=new ClaimGateway(){
    public java.util.List<Pending> pending(UUID owner){return delivery.pending(owner);}
    public Result claim(UUID owner,String id){
     Result result=delivery.claim(owner,id);
     if(result==Result.DELIVERED)cleanupClaimedMarker(owner);
     return result;
    }
   };
   ui=new PersonalClaimsUi(plugin,gateway);
   Bukkit.getPluginManager().registerEvents(ui,plugin);
   // 0.2.11 (B-2): auto-present + AUTO-session immunity frames. The
   // delays and frame lengths are balance values, so they are config
   // keys (team-rewards.*) whose defaults are the settled numbers.
   presentation=loadPresentation(plugin);
   ui.setOpenObserver((owner,ids)->{presentation.markPresented(owner,ids);persistPresentation();});
   int frameSeconds=Math.max(0,plugin.getConfig().getInt("team-rewards.review-frames.max-seconds",ReviewFramesPolicy.DEFAULT_MAX_TICKS/20));
   int graceSeconds=Math.max(0,plugin.getConfig().getInt("team-rewards.review-frames.close-grace-seconds",ReviewFramesPolicy.DEFAULT_GRACE_TICKS/20));
   frames=new ReviewFrames(ui,frameSeconds*20,graceSeconds*20);
   ui.setSessionListener(frames);
   Bukkit.getPluginManager().registerEvents(frames,plugin);
   Bukkit.getPluginManager().registerEvents(this,plugin);
   verdictDelayTicks=Math.max(0,plugin.getConfig().getInt("team-rewards.presentation.verdict-delay-seconds",2))*20L;
   joinDelayTicks=Math.max(0,plugin.getConfig().getInt("team-rewards.presentation.join-delay-seconds",3))*20L;
   presentRetryTicks=Math.max(1,plugin.getConfig().getInt("team-rewards.presentation.retry-seconds",30))*20L;
   presentRetryMaxMs=Math.max(1,plugin.getConfig().getInt("team-rewards.presentation.retry-max-minutes",10))*60_000L;
   presentOutcomeLogging=plugin.getConfig().getBoolean("team-rewards.presentation.outcome-logging",true);
   msgRewardsWaiting=plugin.getConfig().getString("messages.rewards-waiting",
    "&eYour boss rewards are waiting in &f/bossrewards&e.");
   frameMaxTicks=frameSeconds*20;
   popInvisibilityPotion=plugin.getConfig().getBoolean("team-rewards.presentation.excel-invisibility-potion",true);
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
 private static java.nio.file.Path presentedFile(JavaPlugin plugin){return plugin.getDataFolder().toPath().resolve("team-rewards-presented.dat");}
 private static ReviewPresentation loadPresentation(JavaPlugin plugin){
  try{var file=presentedFile(plugin);if(java.nio.file.Files.exists(file))return ReviewPresentation.decode(java.nio.file.Files.readString(file));}
  catch(Exception failure){plugin.getLogger().warning("Presented-rewards record unreadable; starting empty (one auto-present may repeat; no reward is affected): "+failure.getMessage());}
  return new ReviewPresentation();
 }
 private void persistPresentation(){
  try{com.winkreyes.excelbosstrial.core.AtomicStateFile.write(presentedFile(plugin),presentation.encode());}
  catch(Exception failure){plugin.getLogger().warning("Could not persist the presented-rewards record: "+failure.getMessage());}
 }
 /** 0.2.11 (B-2): after a verdict stores claims, present each owner's
  *  menu automatically, briefly delayed so the kill moment lands first. */
 void presentToOwners(Collection<UUID> owners){
  for(UUID owner:owners)Bukkit.getScheduler().runTaskLater(plugin,()->attemptPresent(owner),verdictDelayTicks);
 }
 /** A join presents rewards that were earned while the player was away
  *  (or never presented), once per batch, after a short settle delay. */
 @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.MONITOR,ignoreCancelled=true)
 public void onJoin(org.bukkit.event.player.PlayerJoinEvent event){
  UUID owner=event.getPlayer().getUniqueId();
  Bukkit.getScheduler().runTaskLater(plugin,()->attemptPresent(owner),joinDelayTicks);
 }
 /** 0.4.0 (PM-11): the moment a foreign inventory closes, a
  *  waiting batch gets its re-attempt on the next tick - a
  *  player who just closed the menu that blocked presentation
  *  no longer waits out the retry interval behind it. Only
  *  owners with a live retry state are attempted, so ordinary
  *  container closes cost nothing. */
 @org.bukkit.event.EventHandler(priority=org.bukkit.event.EventPriority.MONITOR)
 public void onInventoryClose(org.bukkit.event.inventory.InventoryCloseEvent event){
  if(!(event.getPlayer() instanceof Player player))return;
  UUID owner=player.getUniqueId();
  if(presentRetries.containsKey(owner))Bukkit.getScheduler().runTask(plugin,()->attemptPresent(owner));
 }
 /** One presentation attempt. 0.3.0 (SPEC section 5): every exit
  *  goes through the {@link com.winkreyes.excelbosstrial.core.PresentOutcome}
  *  classifier, so a skipped presentation is LOGGED with its named
  *  outcome (the Elder-night defect was a skip with no trace), and
  *  transient outcomes enter the retry queue instead of silently
  *  waiting for the next verdict or join. Facts are gathered in
  *  the classifier's priority order. */
 private void attemptPresent(UUID owner){
  Player player=Bukkit.getPlayer(owner);
  boolean online=player!=null&&player.isOnline()&&!player.isDead();
  String who=player!=null?player.getName():owner.toString();
  boolean modeOk=online&&(player.getGameMode()==GameMode.SURVIVAL||player.getGameMode()==GameMode.ADVENTURE);
  boolean foreignInventory=false;
  boolean magicBlocked=false;
  java.util.List<ClaimGateway.Pending> pending=java.util.List.of();
  boolean pendingReadable=true;
  if(online){
   var top=player.getOpenInventory().getTopInventory();
   foreignInventory=top!=null&&!(top.getHolder() instanceof Player)&&!ui.isClaimsViewOpen(player); // another menu or container is up (our own rewards view re-presents)
   magicBlocked=!normalInventory(player);
   try{pending=gateway.pending(owner);}catch(RuntimeException unavailable){pendingReadable=false;}
  }
  java.util.List<String> ids=pending.stream().map(ClaimGateway.Pending::id).toList();
  boolean hasUnpresented=pendingReadable&&!pending.isEmpty()&&presentation.hasUnpresented(owner,ids);
  boolean alreadyPresented=pendingReadable&&!pending.isEmpty()&&!hasUnpresented;
  com.winkreyes.excelbosstrial.core.PresentOutcome outcome;
  if(!pendingReadable){
   outcome=com.winkreyes.excelbosstrial.core.PresentOutcome.MAGIC_INVENTORY_STATE; // ledger unreadable mid-attempt: transient, retried below
   logPresentOutcome(who,outcome," (pending claims unreadable this attempt)");
  }else{
   outcome=com.winkreyes.excelbosstrial.core.PresentOutcome.classify(
    new com.winkreyes.excelbosstrial.core.PresentOutcome.Facts(
     online,modeOk,foreignInventory,magicBlocked,hasUnpresented,alreadyPresented));
   if(outcome!=com.winkreyes.excelbosstrial.core.PresentOutcome.PRESENTED)logPresentOutcome(who,outcome,"");
  }
  java.util.Set<String> batch=new java.util.HashSet<>(ids);
  if(outcome==com.winkreyes.excelbosstrial.core.PresentOutcome.PRESENTED){
   if(!ui.openAuto(player)){ // a successful open marks the batch presented via the open observer
    logPresentOutcome(who,com.winkreyes.excelbosstrial.core.PresentOutcome.FOREIGN_INVENTORY," (menu open refused)");
    queuePresentRetry(owner,batch,player);
    return;
   }
   presentRetries.remove(owner);
   // The Excel invisibility potion rides the presentation itself:
   // this point is reached at most once per batch (the presented-set
   // gate above), so a batch can never apply it twice, and manual
   // opens never reach it at all.
   if(popInvisibilityPotion&&frameMaxTicks>0)applyReviewInvisibility(player);
   return;
  }
  if(outcome.transientFailure())queuePresentRetry(owner,batch,player);
  else presentRetries.remove(owner);
 }
 private void logPresentOutcome(String who,com.winkreyes.excelbosstrial.core.PresentOutcome outcome,String note){
  if(presentOutcomeLogging)plugin.getLogger().info("BossRewards auto-present: "+who+" -> "+outcome+note+".");
 }
 /** Queue (or continue) the retry loop for one owner's batch:
  *  re-attempt every retry interval until the batch presents or
  *  the retry window closes. 0.4.0 (PM-11): a skip is SILENT
  *  while retries remain - the 0.3.0 first-skip hint read as an
  *  error ("the menu could not open") for a normal, will-retry
  *  state. The waiting line is pure information and is sent
  *  exactly once, when the window closes unpresented; a Bedrock
  *  player is then offered the Floodgate form fallback too. */
 private void queuePresentRetry(UUID owner,java.util.Set<String> batch,Player player){
  long now=System.currentTimeMillis();
  PresentRetry state=presentRetries.get(owner);
  if(state==null||!state.batchIds().equals(batch)){
   state=new PresentRetry(now,batch,false);
   presentRetries.put(owner,state);
  }
  if(now-state.firstFailMs()>presentRetryMaxMs){
   presentRetries.remove(owner);
   plugin.getLogger().info("BossRewards auto-present: gave up retrying "+owner
    +" after "+(presentRetryMaxMs/60_000L)+" minutes; the batch waits in /bossrewards.");
   Player waiting=Bukkit.getPlayer(owner);
   if(waiting!=null&&waiting.isOnline()&&!batch.isEmpty()){
    waiting.sendMessage(net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
     .legacyAmpersand().deserialize(msgRewardsWaiting));
    offerBedrockForm(waiting,batch.size());
   }
   return;
  }
  Bukkit.getScheduler().runTaskLater(plugin,()->{
   if(presentRetries.containsKey(owner))attemptPresent(owner);
  },presentRetryTicks);
 }
 /** 0.4.0 (PM-11): the Bedrock form fallback. When the chest
  *  menu's whole retry window closes unpresented, a Bedrock
  *  player is offered a Floodgate (Cumulus) form - one button
  *  that opens the real claims menu. The form is a doorbell,
  *  never a second claims implementation: the claim itself
  *  still happens in the chest UI. Floodgate is an optional
  *  dependency, so every call is reflective and fail-soft: no
  *  Floodgate, a Java player, or any failure along the seam
  *  leaves the chat line as the whole fallback. The decision
  *  itself is the unit-tested PresentFallback policy; this
  *  method only executes it. */
 private void offerBedrockForm(Player player,int batchSize){
  try{
   org.bukkit.plugin.Plugin floodgate=Bukkit.getPluginManager().getPlugin("floodgate");
   if(floodgate==null||!floodgate.isEnabled())return;
   Class<?> apiClass=Class.forName("org.geysermc.floodgate.api.FloodgateApi");
   Object api=apiClass.getMethod("getInstance").invoke(null);
   boolean bedrock=(Boolean)apiClass.getMethod("isFloodgatePlayer",java.util.UUID.class)
    .invoke(api,player.getUniqueId());
   if(!com.winkreyes.excelbosstrial.core.PresentFallback.offerForm(bedrock,true,batchSize>0))return;
   Class<?> builderClass=Class.forName("org.geysermc.cumulus.form.SimpleForm");
   Object builder=builderClass.getMethod("builder").invoke(null);
   builderClass.getMethod("title",String.class).invoke(builder,"Boss rewards");
   builderClass.getMethod("content",String.class).invoke(builder,
    "Your boss rewards are waiting. Open the rewards menu to collect them.");
   builderClass.getMethod("button",String.class).invoke(builder,"Open rewards menu");
   for(java.lang.reflect.Method m:builderClass.getMethods()){
    if(m.getName().equals("validResultHandler")&&m.getParameterCount()==1){
     Class<?> handlerType=m.getParameterTypes()[0];
     if(handlerType.isInterface()){
      Object handler=java.lang.reflect.Proxy.newProxyInstance(
       TeamClaims.class.getClassLoader(),new Class<?>[]{handlerType},
       (proxy,method,args)->{
        if(method.getName().equals("accept")||method.getName().equals("handle")){
         Bukkit.getScheduler().runTask(plugin,()->{if(player.isOnline())ui.openAuto(player);});
        }
        return null;
       });
      m.invoke(builder,handler);
     }
     break;
    }
   }
   Object form=builderClass.getMethod("build").invoke(builder);
   apiClass.getMethod("sendForm",java.util.UUID.class,Class.forName("org.geysermc.cumulus.form.Form"))
    .invoke(api,player.getUniqueId(),form);
   plugin.getLogger().info("BossRewards auto-present: offered the Bedrock form fallback to "
    +player.getName()+" (the chest menu never presented inside its window).");
  }catch(ReflectiveOperationException|RuntimeException unavailable){
   plugin.getLogger().fine("BossRewards form fallback unavailable for "+player.getName()
    +": "+unavailable);
  }
 }
 /** Whether the owner is inside a reward review frame right now
  *  (framed or in the close grace) - the attrition pulse skips
  *  these players exactly like the review design intends. */
 boolean inReviewFrame(UUID owner){
  Player player=Bukkit.getPlayer(owner);
  return player!=null&&(frames.framed(player)||frames.inGrace(player));
 }
 /** 0.2.11 (B-2 addition, as corrected by Mike the same night: "Not
  *  one of their own. It'll use an Excel invisibility potion on the
  *  player."): the plugin applies its OWN invisibility for the
  *  review window - server-side, nothing consumed, nothing required
  *  from the player's inventory, which is never touched. Duration
  *  is the review-frames cap (the same config value, no second
  *  literal); the effect shape follows the Excel house style set by
  *  ExcelMobVeil's veil (amplifier 0, not ambient, no particles,
  *  icon shown). No sibling plugin defines a reusable invisibility
  *  potion for this - DeathProtection's premium potion is a
  *  protection marker, and MobVeil's veil is its own item mechanic
  *  with a fixed 480 s - so the effect is defined here and recorded
  *  in SOURCE.md as the Excel invisibility potion. */
 private void applyReviewInvisibility(Player player){
  player.addPotionEffect(new org.bukkit.potion.PotionEffect(
   org.bukkit.potion.PotionEffectType.INVISIBILITY,frameMaxTicks,0,false,false,true));
  plugin.getLogger().info("BossRewards auto-present applied the Excel invisibility potion to "
   +player.getName()+" ("+(frameMaxTicks/20)+"s, the review window).");
 }
 private long grantSequence;
 /** 0.2.11: store one console grant as a normal personal claim,
  *  through the ledger's own fight-shaped path (open encounter,
  *  qualify the owner, win it) - no raw store edits, so the claim
  *  persists, presents and delivers exactly like an earned batch,
  *  and waits out offline players and full inventories by design.
  *  Each grant is its own encounter, so two grants stack as two
  *  claim entries. Returns the grant's encounter id. */
 String grantClaim(UUID owner,List<ItemStack> stacks)throws IOException{
  String run="console-grant:"+java.time.LocalDateTime.now()
   .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+(++grantSequence);
  ledger.openEncounter(run);
  ledger.qualify(run,owner);
  List<TeamRewardLedger.RewardUnit> units=new ArrayList<>();
  for(int i=0;i<stacks.size();i++)units.add(new TeamRewardLedger.RewardUnit("grant:"+owner+":"+i,encode(stacks.get(i))));
  ledger.win(run,"console-grant-v1",Map.of(owner,List.copyOf(units)));
  presentToOwners(List.of(owner)); // online targets present now; offline ones present at their next join
  return run;
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
