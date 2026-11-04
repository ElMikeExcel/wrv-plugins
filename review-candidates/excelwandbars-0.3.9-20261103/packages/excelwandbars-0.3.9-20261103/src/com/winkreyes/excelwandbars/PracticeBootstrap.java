package com.winkreyes.excelwandbars;
import java.util.function.BiPredicate;import java.util.Set;import wrv.magic.level.LearnedHooks;import wrv.magic.level.RawWandLoadBridge;import wrv.magic.evidence.SharedHistoryProvider;import org.bukkit.Bukkit;import org.bukkit.entity.*;import org.bukkit.plugin.Plugin;import com.elmakers.mine.bukkit.api.magic.MagicAPI;import wrv.magic.evidence.EffectObserver;
/** Production lifecycle adapter. Constructed only for explicitly enabled validated configuration. */
final class PracticeBootstrap {
 static OwnedPracticeLifecycle start(ExcelWandBars plugin,Plugin magicPlugin,PracticeBootstrapConfig.Loaded config){
  if(!(magicPlugin instanceof MagicAPI magic)||plugin.rankUp()==null)throw new IllegalStateException("Magic and rank-up owner required");
  if(!java.util.Collections.disjoint(RankUpHook.parseStartupFence(plugin.getConfig().getConfigurationSection("rank-up-casting")),config.settings().spells().keySet()))throw new IllegalArgumentException("legacy SP mode cannot use managed-evidence-fenced-spells for practice keys");
  if(!(magic.getController() instanceof com.elmakers.mine.bukkit.magic.MagicController nativeController))throw new IllegalStateException("reviewed native Magic controller required");
  // Reserve fail-closed scope synchronously, before deferred lifecycle construction can fail.
  // Only immutable keys/templates are retained; no player, plugin or closed ledger references.
  Set<String> managed=Set.copyOf(config.settings().spells().keySet());
  Set<String> templates=Set.copyOf(plugin.rankUp().ladderOrder());
  LearnedHooks.requireManaged(managed);
  RawWandLoadBridge.requireManagedScope(raw->raw.existingWand()&&!raw.upgrade()&&templates.contains(raw.template())&&!Boolean.FALSE.equals(raw.bound()));
  MigrationTouchGuard.requireManaged();
  var factory=new PreparedPracticeFactory(plugin,magic,plugin.getDataFolder().toPath().resolve("spell-practice"),config.settings(),(player,spell)->false,
   targets(config.practiceTargets()),(receipt,target)->false,failure->plugin.getLogger().severe("Managed spell practice blocked: "+failure),config.migration(),true,()->AppliedMagicStage.active(config.minimumSeals(),AppliedMagicStage.read()));
  var owner=new OwnedPracticeLifecycle(plugin,magicPlugin,magic.getController(),factory,failure->{plugin.getLogger().severe("Managed spell practice unavailable; disabling plugin with managed scopes fenced: "+failure);plugin.getServer().getPluginManager().disablePlugin(plugin);},()->nativeController.isLoaded()&&AppliedMagicStage.dependencyEnabled()&&!Bukkit.getServicesManager().getRegistrations(SharedHistoryProvider.class).isEmpty(),Set.of("ExcelMagicGate"));owner.start();if(!plugin.isEnabled()){owner.close();throw new IllegalStateException("plugin disabled during bootstrap");}return owner;
 }
 static BiPredicate<EffectObserver.Receipt,LivingEntity> targets(String policy){
  if(!java.util.Set.of("any-effective-living","players-hostiles","players-and-pets-hostiles").contains(policy))throw new IllegalArgumentException("unknown target policy");
  return (receipt,target)->{
   if(receipt==null||receipt.kind()==null||target==null||receipt.recipient()==null||!receipt.recipient().equals(target.getUniqueId())||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0)return false;
   if(policy.equals("any-effective-living"))return receipt.kind()==EffectObserver.Kind.DIRECT_HEAL||receipt.kind()==EffectObserver.Kind.DIRECT_DAMAGE;
   boolean friendly=target instanceof Player p&&p.isOnline()||policy.equals("players-and-pets-hostiles")&&target instanceof Tameable pet&&pet.isTamed();
   return switch(receipt.kind()){
    case DIRECT_HEAL -> friendly&&!target.isDead();
    case DIRECT_CLEANSE -> friendly&&!target.isDead()&&receipt.cleanse()!=null;
    case DIRECT_DAMAGE,DIRECT_CONTROL -> !(target instanceof Player)&&!(target instanceof Tameable pet&&pet.isTamed())&&(target instanceof Enemy||target instanceof Monster||target instanceof EnderDragon);
   };
  };
 }
 private PracticeBootstrap(){}
}
