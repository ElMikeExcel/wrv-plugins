package wrv.magic.eligibility;
import java.util.*;
import org.bukkit.*;import org.bukkit.entity.*;import org.bukkit.event.*;import org.bukkit.event.entity.*;import org.bukkit.event.player.*;import org.bukkit.plugin.Plugin;
/** Conservative owner-thread provenance. Colliding/unobserved transitions reset to unknown deficit. */
public final class BukkitCombatHealthTracker implements Listener,AutoCloseable {
 private record Session(UUID id,long sequence){}
 private record Pending(EntityDamageEvent event,double before,double maximum,CombatDeficitLedger.Origin origin){}
 private final Plugin plugin;private final CombatDeficitLedger ledger;private final Map<UUID,Session> sessions=new HashMap<>();private final Map<UUID,Pending> pending=new HashMap<>();private boolean closed,started;
 public BukkitCombatHealthTracker(Plugin plugin,CombatDeficitLedger ledger){this.plugin=Objects.requireNonNull(plugin);this.ledger=Objects.requireNonNull(ledger);}
 public void start(){check();if(started)throw new IllegalStateException("already started");started=true;Bukkit.getPluginManager().registerEvents(this,plugin);for(Player p:Bukkit.getOnlinePlayers())reset(p);}
 private void check(){if(closed||!Bukkit.isPrimaryThread())throw new IllegalStateException("active server owner required");}
 private double maximum(Player p){return p.getMaxHealth();}
 private void reset(Player p){UUID id=p.getUniqueId();pending.remove(id);ledger.invalidate(id);var s=new Session(UUID.randomUUID(),0);if(!p.isDead()&&ledger.begin(id,s.id(),0,p.getHealth(),maximum(p)))sessions.put(id,s);else sessions.remove(id);}
 private CombatDeficitLedger.Frame frame(Player p,double before,double after,double max){var s=sessions.get(p.getUniqueId());if(s==null)return null;var next=new Session(s.id(),s.sequence()+1);sessions.put(p.getUniqueId(),next);return new CombatDeficitLedger.Frame(p.getUniqueId(),s.id(),next.sequence(),before,after,max);}
 private static CombatDeficitLedger.Origin origin(EntityDamageEvent e){
  if(!(e instanceof EntityDamageByEntityEvent by))return CombatDeficitLedger.Origin.SELF_OR_UNKNOWN;
  Entity cause=by.getDamager();if(cause instanceof Projectile shot){var shooter=shot.getShooter();cause=shooter instanceof Entity entity?entity:null;}
  // Owned pets, players, environmental hazards and unattributed DoT never establish mob-combat authority.
  if(cause instanceof Player||cause instanceof Tameable pet&&pet.isTamed())return CombatDeficitLedger.Origin.SELF_OR_UNKNOWN;
  return cause instanceof Enemy||cause instanceof Monster||cause instanceof EnderDragon?CombatDeficitLedger.Origin.VERIFIED_HOSTILE_DIRECT:CombatDeficitLedger.Origin.SELF_OR_UNKNOWN;
 }
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void damage(EntityDamageEvent e){if(closed||!(e.getEntity() instanceof Player p))return;UUID id=p.getUniqueId();
  if(pending.containsKey(id)){reset(p);return;}if(!sessions.containsKey(id))reset(p);if(!sessions.containsKey(id))return;
  Pending observation=new Pending(e,p.getHealth(),maximum(p),origin(e));pending.put(id,observation);
  Bukkit.getScheduler().runTask(plugin,()->{if(!closed&&pending.get(id)==observation)settle(p,p.getHealth());});
 }
 private boolean settle(Player p,double actualAfter){var x=pending.remove(p.getUniqueId());if(x==null)return true;
  double finalDamage=x.event().isCancelled()?0:Math.max(0,x.event().getFinalDamage());double expected=Math.max(0,x.before()-finalDamage);
  if(!Double.isFinite(finalDamage)||Double.compare(expected,actualAfter)!=0||Double.compare(x.maximum(),maximum(p))!=0){reset(p);return false;}
  var f=frame(p,x.before(),actualAfter,x.maximum());if(f==null||!ledger.damage(p.getUniqueId(),f,x.origin(),x.event().isCancelled())){reset(p);return false;}return true;
 }
 /** Called at the native heal boundary BEFORE any shared receipt consumer, using captured actual HP. */
 public CombatDeficitLedger.Frame nativeHeal(Player p,double before,double after,double maximum){check();if(!settle(p,before))return null;if(!sessions.containsKey(p.getUniqueId())){reset(p);return null;}return frame(p,before,after,maximum);}
 @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=false) public void regained(EntityRegainHealthEvent e){if(closed||!(e.getEntity() instanceof Player p)||e.isCancelled())return;double before=p.getHealth(),max=maximum(p);if(!settle(p,before))return;var f=frame(p,before,Math.min(max,before+e.getAmount()),max);if(f==null)return;UUID id=p.getUniqueId();Session captured=sessions.get(id);
  Bukkit.getScheduler().runTask(plugin,()->{if(closed||sessions.get(id)!=captured)return;if(e.isCancelled()||Double.compare(p.getHealth(),f.after())!=0||Double.compare(maximum(p),max)!=0){reset(p);return;}if(!ledger.restore(id,f).accepted())reset(p);});
 }
 @EventHandler public void join(PlayerJoinEvent e){if(!closed)reset(e.getPlayer());}
 @EventHandler public void quit(PlayerQuitEvent e){remove(e.getPlayer());}
 @EventHandler public void death(PlayerDeathEvent e){remove(e.getEntity());}
 @EventHandler public void respawn(PlayerRespawnEvent e){if(!closed)Bukkit.getScheduler().runTask(plugin,()->{if(!closed)reset(e.getPlayer());});}
 private void remove(Player p){if(closed)return;pending.remove(p.getUniqueId());sessions.remove(p.getUniqueId());ledger.invalidate(p.getUniqueId());}
 @Override public void close(){if(closed)return;check();closed=true;HandlerList.unregisterAll(this);pending.clear();sessions.clear();ledger.clear();}
}
