package wrv.magic.evidence;
import com.elmakers.mine.bukkit.api.action.CastContext;import org.bukkit.entity.*;import java.util.*;
/** Evidence-only observer. No progression, SP, policy defaults or activation. */
public final class EffectObserver {
 public enum Kind { DIRECT_HEAL,DIRECT_DAMAGE,DIRECT_CONTROL,DIRECT_CLEANSE }
 /** Null reference fields mean unknown; ownerKnown distinguishes observed empty owner from failed lookup. */
 public record SourceSnapshot(String wandId,String templateKey,UUID ownerId,boolean ownerKnown,Boolean bound,Boolean offhand,Double earns,long issuedAtMillis,UUID observedMakerItemId){
  public SourceSnapshot(String wandId,String templateKey,UUID ownerId,boolean ownerKnown,Boolean bound,Boolean offhand,Double earns,long issuedAtMillis){this(wandId,templateKey,ownerId,ownerKnown,bound,offhand,earns,issuedAtMillis,null);}
 }
 public record RootEvidence(UUID cast,UUID caster,String spell,SourceSnapshot source){}
 public record ControlChange(Integer beforeAmplifier,int beforeDuration,int afterAmplifier,int afterDuration){}
 public record CleanseChange(String effectKey,int beforeAmplifier,int beforeDuration,boolean ambient,boolean particles,boolean icon){}
 public record Receipt(UUID cast,UUID caster,String spell,UUID recipient,Kind kind,double effectiveAmount,SourceSnapshot source,ControlChange control,CleanseChange cleanse){
  public Receipt(UUID cast,UUID caster,String spell,UUID recipient,Kind kind,double effectiveAmount,SourceSnapshot source,ControlChange control){this(cast,caster,spell,recipient,kind,effectiveAmount,source,control,null);}
  public Receipt(UUID cast,UUID caster,String spell,UUID recipient,Kind kind,double effectiveAmount,SourceSnapshot source){this(cast,caster,spell,recipient,kind,effectiveAmount,source,null);}
  public Receipt(UUID cast,UUID caster,String spell,UUID recipient,Kind kind,double effectiveAmount){this(cast,caster,spell,recipient,kind,effectiveAmount,null,null);}
 }
 public record PendingDamage(UUID dispatch,UUID cast,UUID caster,String spell,UUID recipient,SourceSnapshot source){}
 public enum DamageVerdict { VERIFIED,REJECTED,AMBIGUOUS,OBSERVATION_FAILED }
 public record DamageCompletion(PendingDamage pending,DamageVerdict verdict,Receipt evidence,boolean deathObserved,org.bukkit.event.entity.EntityDamageEvent verifiedEvent,org.bukkit.event.entity.EntityDeathEvent deathEvent){public DamageCompletion(PendingDamage pending,DamageVerdict verdict,Receipt evidence,boolean deathObserved){this(pending,verdict,evidence,deathObserved,null,null);}}
 public interface Sink {void observe(Receipt receipt);default void rootCaptured(RootEvidence root){}default void castStarted(RootEvidence root){}default void rootFinished(RootEvidence root){}default void damageStarted(PendingDamage pending){}default void damageCompleted(DamageCompletion completion){}}
 public record RootAuthority(com.elmakers.mine.bukkit.api.magic.Mage mage,com.elmakers.mine.bukkit.api.wand.Wand wand){}
 public record ReceiptAuthority(com.elmakers.mine.bukkit.api.magic.Mage mage,com.elmakers.mine.bukkit.api.wand.Wand wand,LivingEntity target){}
 private static final class Origin {
  final UUID cast,caster;final String spell;final SourceSnapshot source;final Registration generation;
  final java.lang.ref.WeakReference<com.elmakers.mine.bukkit.api.magic.Mage> mage;
  final java.lang.ref.WeakReference<com.elmakers.mine.bukkit.api.wand.Wand> wand;
  Origin(UUID cast,UUID caster,String spell,SourceSnapshot source,Registration generation,RootAuthority original){this.cast=cast;this.caster=caster;this.spell=spell;this.source=source;this.generation=generation;this.mage=new java.lang.ref.WeakReference<>(original.mage());this.wand=new java.lang.ref.WeakReference<>(original.wand());}
  Optional<RootAuthority> authority(){var m=mage.get();var w=wand.get();return m!=null&&w!=null?Optional.of(new RootAuthority(m,w)):Optional.empty();}
 }
 private record ReceiptFrame(Receipt exact,Origin origin,LivingEntity target){}
 private static final ThreadLocal<ReceiptFrame> receiptFrame=new ThreadLocal<>();
 public static Optional<RootAuthority> currentRootAuthority(RootEvidence exact){var f=issuanceFrame.get();return Thread.currentThread()==ownerThread&&f!=null&&f.root==exact&&f.generation==registration?f.origin.authority():Optional.empty();}
 public static Optional<ReceiptAuthority> currentReceiptAuthority(Receipt exact){var f=receiptFrame.get();return Thread.currentThread()==ownerThread&&f!=null&&f.exact==exact&&f.origin.generation==registration?f.origin.authority().map(a->new ReceiptAuthority(a.mage(),a.wand(),f.target)):Optional.empty();}
 private static void deliver(Origin origin,Receipt receipt,LivingEntity target,Runnable callback){
  if(origin==null||origin.generation!=registration)return;
  var prior=receiptFrame.get();receiptFrame.set(new ReceiptFrame(receipt,origin,target));
  try{callback.run();}finally{if(prior==null)receiptFrame.remove();else receiptFrame.set(prior);}
 }
 static boolean hostileControlTarget(LivingEntity target){return target!=null&&!(target instanceof Player)&&!(target instanceof Tameable t&&t.isTamed())&&(target instanceof Enemy||target instanceof Monster||target instanceof EnderDragon);}

 private record Registration(Set<String> spells,Sink sink){}
 private static final Map<CastContext,Origin> origins=Collections.synchronizedMap(new WeakHashMap<>());
 private static volatile Registration registration;
 private static volatile Thread ownerThread;
 /** Read-only source association, not issuance or payout authority. Unknown and stale contexts fail closed.
  * Only exact native contexts are supported; caller should retain context.getBaseContext() as route identity.
  * Repeated lookup of a finished/reused object returns the same original identity, never a new cast.
  */
 public static synchronized Optional<RootEvidence> originalRootEvidence(CastContext context){
  try{
   Registration r=registration;
   if(r==null||context==null||context.getClass()!=com.elmakers.mine.bukkit.action.CastContext.class)return Optional.empty();
   CastContext root=context.getBaseContext();
   if(root==null||root.getClass()!=com.elmakers.mine.bukkit.action.CastContext.class||root.getBaseContext()!=root)return Optional.empty();
   Origin origin=null;
   // Identity comparison is deliberate: no custom equals/hashCode may alias a root.
   synchronized(origins){for(var e:origins.entrySet())if(e.getKey()==root){origin=e.getValue();break;}}
   if(origin==null||origin.generation!=r||!r.spells.contains(origin.spell))return Optional.empty();
   boolean hasStarted=false;
   synchronized(started){for(CastContext candidate:started)if(candidate==root){hasStarted=true;break;}}
   if(!hasStarted||registration!=r)return Optional.empty();
   return Optional.of(new RootEvidence(origin.cast,origin.caster,origin.spell,origin.source));
  }catch(Throwable unavailable){return Optional.empty();}
 }
 private static final class IssuanceFrame {final RootEvidence root;final Registration generation;final Origin origin;boolean claimed;IssuanceFrame(RootEvidence r,Registration g,Origin o){root=r;generation=g;origin=o;}}
 private static final ThreadLocal<IssuanceFrame> issuanceFrame=new ThreadLocal<>();
 /** One dedicated useful-use issuer may claim only inside the authentic first-start callback. */
 public static boolean claimIssuance(RootEvidence root){var frame=issuanceFrame.get();if(frame==null||frame.root!=root||frame.generation!=registration||frame.claimed)return false;frame.claimed=true;return true;}

 private static final Set<CastContext> started=Collections.newSetFromMap(Collections.synchronizedMap(new WeakHashMap<CastContext,Boolean>()));
 private static volatile boolean lifecycleProofEnabled;
 private static final Map<CastContext,StrictActionGraph.Proof> lifecycleProofs=Collections.synchronizedMap(new WeakHashMap<>());
 public static void enableLifecycleProof(boolean enabled){lifecycleProofEnabled=enabled;}
 private static final ThreadLocal<ArrayDeque<DamageFrame>> damageFrames=ThreadLocal.withInitial(ArrayDeque::new);
 private static final ThreadLocal<ArrayDeque<CleanseFrame>> cleanseFrames=ThreadLocal.withInitial(ArrayDeque::new);
 private static final Set<String> NEGATIVE=Set.of("minecraft:nausea","minecraft:poison","minecraft:slowness","minecraft:mining_fatigue","minecraft:weakness","minecraft:wither","minecraft:blindness","minecraft:hunger");
 private static final class CleanseFrame {final LivingEntity target;boolean nested;final List<org.bukkit.event.entity.EntityPotionEffectEvent> events=new ArrayList<>();CleanseFrame(LivingEntity t){target=t;}}
 private static org.bukkit.event.Listener damageListener;
 private static final class DamageFrame {final LivingEntity target; final double before; final CastContext context; PendingDamage pending;boolean deathObserved;org.bukkit.event.entity.EntityDeathEvent deathEvent;boolean nestedAmbiguous; final List<org.bukkit.event.entity.EntityDamageEvent> events=new ArrayList<>();DamageFrame(LivingEntity t,double b,CastContext c){target=t;before=b;context=c;}}

 public static synchronized void install(Set<String> spells,Sink sink){if(registration!=null)throw new IllegalStateException("already installed");if(spells==null||spells.isEmpty()||sink==null)throw new IllegalArgumentException();var plugin=org.bukkit.Bukkit.getPluginManager().getPlugin("Magic");if(plugin==null)throw new IllegalStateException("Magic unavailable");org.bukkit.event.Listener candidate=new org.bukkit.event.Listener(){};try{org.bukkit.Bukkit.getPluginManager().registerEvent(org.bukkit.event.entity.EntityDamageEvent.class,candidate,org.bukkit.event.EventPriority.MONITOR,(l,e)->{var f=damageFrames.get().peek();if(f!=null&&e instanceof org.bukkit.event.entity.EntityDamageEvent d&&d.getEntity().getUniqueId().equals(f.target.getUniqueId()))f.events.add(d);},plugin,false);org.bukkit.Bukkit.getPluginManager().registerEvent(org.bukkit.event.entity.EntityDeathEvent.class,candidate,org.bukkit.event.EventPriority.MONITOR,(l,e)->{var f=damageFrames.get().peek();if(f!=null&&e instanceof org.bukkit.event.entity.EntityDeathEvent d&&d.getEntity().getUniqueId().equals(f.target.getUniqueId())){f.deathObserved=true;f.deathEvent=d;}},plugin,false);org.bukkit.Bukkit.getPluginManager().registerEvent(org.bukkit.event.entity.EntityPotionEffectEvent.class,candidate,org.bukkit.event.EventPriority.MONITOR,(l,e)->{var f=cleanseFrames.get().peek();if(f!=null&&e instanceof org.bukkit.event.entity.EntityPotionEffectEvent p&&p.getEntity().getUniqueId().equals(f.target.getUniqueId()))f.events.add(p);},plugin,false);damageListener=candidate;ownerThread=Thread.currentThread();registration=new Registration(Set.copyOf(spells),sink);}catch(Throwable failure){org.bukkit.event.HandlerList.unregisterAll(candidate);throw failure;}}
 /** Identity-scoped owner; a stale close cannot detach a replacement registration. */
 public static synchronized AutoCloseable installOwned(Set<String> spells,Sink sink){
  install(spells,sink);Registration mine=registration;
  return ()->{synchronized(EffectObserver.class){if(registration==mine){lifecycleProofEnabled=false;detach();}}};
 }

 public static synchronized void detach(){registration=null;ownerThread=null;if(damageListener!=null){org.bukkit.event.HandlerList.unregisterAll(damageListener);damageListener=null;}}
 public static void captureRoot(CastContext context){try{Registration r=registration;if(r==null)return;String spell=context.getSpell().getSpellKey().getBaseKey();if(!r.spells.contains(spell))return;Entity caster=context.getMage().getEntity();if(caster instanceof Player){var mage=context.getMage();var wand=context.getWand();Origin origin=new Origin(UUID.randomUUID(),caster.getUniqueId(),spell,snapshot(context,wand),r,new RootAuthority(mage,wand));if(origins.putIfAbsent(context,origin)==null)try{r.sink.rootCaptured(new RootEvidence(origin.cast,origin.caster,origin.spell,origin.source));}catch(Throwable observerFailure){}}}catch(RuntimeException unavailable){/* Unknown origin never inferred later. */}}

 public static void castStarted(CastContext context){try{CastContext root=context.getBaseContext();var origin=origins.get(root);var r=registration;if(origin==null||r==null||origin.generation!=r||!r.spells.contains(origin.spell)||!started.add(root))return;if(lifecycleProofEnabled)StrictActionGraph.inspect(root.getSpell()).ifPresent(proof->lifecycleProofs.put(root,proof));var evidence=new RootEvidence(origin.cast,origin.caster,origin.spell,origin.source);var prior=issuanceFrame.get();issuanceFrame.set(new IssuanceFrame(evidence,r,origin));try{r.sink.castStarted(evidence);}finally{if(prior==null)issuanceFrame.remove();else issuanceFrame.set(prior);}}catch(Throwable observerFailure){/* Casting proceeds without an issuance token. */}}
 public static void finishSucceeded(CastContext context){try{if(!lifecycleProofEnabled||context.getBaseContext()!=context||context.hasHandlers())return;var proof=lifecycleProofs.remove(context);var origin=origins.get(context);var r=registration;if(proof==null||origin==null||r==null||origin.generation!=r||!r.spells.contains(origin.spell))return;if(!StrictActionGraph.inspect(context.getSpell()).filter(proof::equals).isPresent())return;r.sink.rootFinished(new RootEvidence(origin.cast,origin.caster,origin.spell,origin.source));}catch(Throwable observerFailure){/* Native finish already succeeded; evidence failure cannot alter it. */}}
 private static String knownText(String value){return value==null||value.isBlank()?null:value;}
 private static SourceSnapshot snapshot(CastContext context,com.elmakers.mine.bukkit.api.wand.Wand wand){
  long issuedAt=System.currentTimeMillis();String id=null,template=null;UUID owner=null;boolean ownerKnown=false;Boolean bound=null,offhand=null;Double earns=null;UUID makerItem=null;
  try{double value=context.getSpell().getEarns();if(Double.isFinite(value))earns=value;}catch(RuntimeException unknown){}
  if(wand!=null){
   try{id=knownText(wand.getId());}catch(RuntimeException unknown){}
   try{template=knownText(wand.getTemplateKey());}catch(RuntimeException unknown){}
   try{bound=wand.isBound();}catch(RuntimeException unknown){}
   try{offhand=wand.isInOffhand();}catch(RuntimeException unknown){}
   try{var item=wand.getItem();if(item!=null&&!item.getType().isAir()&&item.getAmount()==1&&item.hasItemMeta()){String value=item.getItemMeta().getPersistentDataContainer().get(new org.bukkit.NamespacedKey("excelmakersmark","item_id"),org.bukkit.persistence.PersistentDataType.STRING);if(value!=null){UUID parsed=UUID.fromString(value);if(parsed.toString().equals(value))makerItem=parsed;}}}catch(RuntimeException unknown){makerItem=null;}
   try{Object value=wand.getClass().getMethod("getOwnerId").invoke(wand);if(value==null||value instanceof String){String text=(String)value;if(text!=null&&!text.isBlank()){owner=UUID.fromString(text);if(!owner.toString().equalsIgnoreCase(text))throw new IllegalArgumentException("noncanonical owner");}ownerKnown=true;}}catch(ReflectiveOperationException|RuntimeException unknown){owner=null;ownerKnown=false;}
  }
  return new SourceSnapshot(id,template,owner,ownerKnown,bound,offhand,earns,issuedAt,makerItem);
 }
 public static void setHealth(LivingEntity target,double requestedHealth,CastContext context){
  if(registration==null){target.setHealth(requestedHealth);return;}
  double before;try{before=target.getHealth();}catch(RuntimeException unavailable){target.setHealth(requestedHealth);return;}
  double observedMaximum;try{observedMaximum=target.getMaxHealth();}catch(RuntimeException unknown){observedMaximum=Double.NaN;}
  final double maximum=observedMaximum;
  target.setHealth(requestedHealth); // preserve the original mutation and exception behavior exactly once
  try{Registration r=registration;if(r==null)return;CastContext root=context.getBaseContext();if(root==null)return;Origin origin=origins.get(root);if(origin==null||origin.generation!=r||!r.spells.contains(origin.spell))return;
   Entity caster=context.getMage().getEntity();if(!(caster instanceof Player)||!origin.caster.equals(caster.getUniqueId()))return;
   double after=target.getHealth(),delta=after-before;if(!Double.isFinite(delta)||delta<=0)return;
   Receipt receipt=new Receipt(origin.cast,origin.caster,origin.spell,target.getUniqueId(),Kind.DIRECT_HEAL,delta,origin.source);
   try{HealthTransitionBridge.deliver(receipt,target.getUniqueId(),before,after,maximum,()->deliver(origin,receipt,target,()->r.sink.observe(receipt)));}catch(Throwable observerFailure){/* Isolate only consumer callback, never the native mutation. */}
  }catch(RuntimeException unavailable){/* Evidence failure never changes successful healing. */}
 }

 /** Only the exact PotionEffectAction.perform removal route calls this; finish cleanup is untouched. */
 public static void removeEffect(LivingEntity target,org.bukkit.potion.PotionEffectType type,CastContext context){
  Registration initial=registration;if(initial==null){target.removePotionEffect(type);return;}
  org.bukkit.potion.PotionEffect before;try{before=target.getPotionEffect(type);}catch(RuntimeException unknown){target.removePotionEffect(type);return;}
  var frame=new CleanseFrame(target);var frames=cleanseFrames.get();for(var prior:frames)if(prior.target.getUniqueId().equals(target.getUniqueId()))prior.nested=true;frames.push(frame);
  try{target.removePotionEffect(type);}finally{if(frames.peek()==frame)frames.pop();else{frames.clear();frame.nested=true;}if(frames.isEmpty())cleanseFrames.remove();}
  try{if(registration!=initial||frame.nested||before==null||before.getDuration()==0||!NEGATIVE.contains(type.getKey().toString())||target.isDead()||target.getPotionEffect(type)!=null||frame.events.size()!=1)return;
   var event=frame.events.get(0);if(event.isCancelled()||event.getOldEffect()==null||!event.getOldEffect().equals(before)||event.getNewEffect()!=null||event.getAction()!=org.bukkit.event.entity.EntityPotionEffectEvent.Action.REMOVED)return;
   var origin=origins.get(context.getBaseContext());if(origin==null||origin.generation!=initial||!initial.spells.contains(origin.spell))return;var caster=context.getMage().getEntity();if(!(caster instanceof Player)||!origin.caster.equals(caster.getUniqueId())||!caster.getUniqueId().equals(target.getUniqueId())&&!context.getController().isFriendly(caster,target))return;
   var change=new CleanseChange(type.getKey().toString(),before.getAmplifier(),before.getDuration(),before.isAmbient(),before.hasParticles(),before.hasIcon());try{var receipt=new Receipt(origin.cast,origin.caster,origin.spell,target.getUniqueId(),Kind.DIRECT_CLEANSE,1,origin.source,null,change);deliver(origin,receipt,target,()->initial.sink.observe(receipt));}catch(Throwable observerFailure){}
  }catch(RuntimeException unavailable){/* Evidence failure cannot change native removal. */}
 }

 /** Observation-only control evidence. effectiveAmount=1 means one improved effect, never HP. */
 public static boolean applyControl(com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils api,LivingEntity target,org.bukkit.potion.PotionEffect effect,CastContext context){
  Registration initial=registration;if(initial==null)return api.applyPotionEffect(target,effect);
  org.bukkit.potion.PotionEffect before;try{before=target.getPotionEffect(effect.getType());}catch(RuntimeException unavailable){return api.applyPotionEffect(target,effect);}
  boolean applied=api.applyPotionEffect(target,effect); // original native application exactly once
  try{if(!applied||registration!=initial||!hostileControlTarget(target)||!effect.getType().equals(org.bukkit.potion.PotionEffectType.SLOWNESS))return applied;
   var after=target.getPotionEffect(effect.getType());if(after==null||after.getDuration()<=0||before!=null&&(after.getAmplifier()<before.getAmplifier()||after.getAmplifier()==before.getAmplifier()&&after.getDuration()<=before.getDuration()))return applied;
   var origin=origins.get(context.getBaseContext());if(origin==null||origin.generation!=initial||!initial.spells.contains(origin.spell))return applied;Entity caster=context.getMage().getEntity();if(!(caster instanceof Player)||!origin.caster.equals(caster.getUniqueId())||context.getController().isFriendly(caster,target))return applied;
   var detail=new ControlChange(before==null?null:before.getAmplifier(),before==null?0:before.getDuration(),after.getAmplifier(),after.getDuration());
   try{var receipt=new Receipt(origin.cast,origin.caster,origin.spell,target.getUniqueId(),Kind.DIRECT_CONTROL,1,origin.source,detail);deliver(origin,receipt,target,()->initial.sink.observe(receipt));}catch(Throwable observerFailure){}
  }catch(RuntimeException unavailable){}return applied;
 }
 public static void damage(com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils api,Damageable target,double amount,Entity source,CastContext context){observeDamage(target,context,()->api.damage(target,amount,source));}
 public static void typedDamage(com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils api,Damageable target,double amount,Entity source,String type,CastContext context){observeDamage(target,context,()->api.damage(target,amount,source,type));}
 public static void magicDamage(com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils api,Damageable target,double amount,Entity source,CastContext context){observeDamage(target,context,()->api.magicDamage(target,amount,source));}
 /** Pending only: caller MUST wait for matching VERIFIED completion before attributing any kill. */
 public static Optional<PendingDamage> currentDamage(UUID victim){DamageFrame frame=damageFrames.get().peek();return frame!=null&&frame.pending!=null&&frame.target.getUniqueId().equals(victim)?Optional.of(frame.pending):Optional.empty();}
 private static void notifyStart(Registration r,PendingDamage p){if(r!=null&&p!=null)try{r.sink.damageStarted(p);}catch(Throwable observerFailure){}}
 private static void notifyComplete(Registration r,DamageFrame f,DamageVerdict v,Receipt receipt){if(r!=null&&f.pending!=null)try{r.sink.damageCompleted(new DamageCompletion(f.pending,v,receipt,f.deathObserved&&f.deathEvent!=null&&!f.deathEvent.isCancelled(),v==DamageVerdict.VERIFIED&&f.events.size()==1?f.events.get(0):null,f.deathEvent));}catch(Throwable observerFailure){}}
 private static void observeDamage(Damageable target,CastContext context,Runnable original){
  Registration initial=registration;if(initial==null||!(target instanceof LivingEntity living)){original.run();return;}
  double before;try{before=living.getHealth()+living.getAbsorptionAmount();}catch(RuntimeException unavailable){original.run();return;}
  DamageFrame f=new DamageFrame(living,before,context);
  try{CastContext root=context.getBaseContext();Origin origin=root==null?null:origins.get(root);Entity caster=context.getMage().getEntity();if(origin!=null&&origin.generation==initial&&initial.spells.contains(origin.spell)&&caster instanceof Player&&origin.caster.equals(caster.getUniqueId()))f.pending=new PendingDamage(UUID.randomUUID(),origin.cast,origin.caster,origin.spell,living.getUniqueId(),origin.source);}catch(RuntimeException unknown){}
  var frames=damageFrames.get();for(DamageFrame parent:frames)if(parent.target.getUniqueId().equals(living.getUniqueId()))parent.nestedAmbiguous=true;frames.push(f);notifyStart(initial,f.pending);boolean finished=false;
  try{original.run();finished=true;}finally{if(frames.peek()==f)frames.pop();else{frames.clear();f.nestedAmbiguous=true;}if(frames.isEmpty())damageFrames.remove();if(!finished)notifyComplete(initial,f,DamageVerdict.OBSERVATION_FAILED,null);}
  DamageVerdict verdict=DamageVerdict.REJECTED;Receipt receipt=null;
  try{if(registration!=initial)verdict=DamageVerdict.OBSERVATION_FAILED;
   else if(f.nestedAmbiguous||f.events.size()!=1)verdict=DamageVerdict.AMBIGUOUS;
   else if(f.pending!=null){var event=f.events.get(0);if(!event.isCancelled()&&event.getDamage()>0){Entity caster=context.getMage().getEntity();if(caster instanceof Player&&f.pending.caster.equals(caster.getUniqueId())&&!context.getController().isFriendly(caster,living)){
    double delta=before-(living.getHealth()+living.getAbsorptionAmount());double budget=Math.max(0,event.getFinalDamage());if(event.isApplicable(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ABSORPTION))budget+=Math.max(0,-event.getDamage(org.bukkit.event.entity.EntityDamageEvent.DamageModifier.ABSORPTION));delta=Math.min(delta,budget);
    if(Double.isFinite(delta)&&delta>0){receipt=new Receipt(f.pending.cast,f.pending.caster,f.pending.spell,living.getUniqueId(),Kind.DIRECT_DAMAGE,delta,f.pending.source);verdict=DamageVerdict.VERIFIED;}
   }}}
  }catch(RuntimeException unknown){verdict=DamageVerdict.OBSERVATION_FAILED;receipt=null;}
  if(receipt!=null)try{var exact=receipt;deliver(origins.get(context.getBaseContext()),exact,living,()->initial.sink.observe(exact));}catch(Throwable observerFailure){}
  notifyComplete(initial,f,verdict,receipt);
 }
}
