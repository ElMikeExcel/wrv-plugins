/* Excel WRV maintained source; original attribution and license preserved. */ package wrv.makersmark.magic;
import java.util.*;import org.bukkit.Bukkit;import org.bukkit.entity.Player;import org.bukkit.entity.ArmorStand;import org.bukkit.event.entity.*;import wrv.magic.evidence.EffectObserver;
/** Read-only join. Output is evidence for later current-item reconciliation, NEVER permission to mutate cached gear. */
public final class MagicDeathJoin {
 public record Evidence(UUID dispatch,UUID cast,UUID caster,String spell,UUID victim,UUID observedItem,String magicWandId,String entityType,String observedName){}
 private static final class Observation {final EffectObserver.PendingDamage pending;final EntityDeathEvent death;final EntityDamageEvent cause;final String type,name;boolean ambiguous;final long capturedNanos;Observation(EffectObserver.PendingDamage p,EntityDeathEvent e,long now){capturedNanos=now;pending=p;death=e;cause=e.getEntity().getLastDamageCause();type=e.getEntityType().name();name=e.getEntity().customName()==null?null:net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(e.getEntity().customName());}}
 private static final long MAX_PENDING_NANOS=30_000_000_000L;
 private final java.util.function.LongSupplier nanoTime;
 public MagicDeathJoin(){this(System::nanoTime);}
 public MagicDeathJoin(java.util.function.LongSupplier monotonicTime){nanoTime=Objects.requireNonNull(monotonicTime);}
 private void expire(){long now=nanoTime.getAsLong();waiting.values().removeIf(o->now-o.capturedNanos>=MAX_PENDING_NANOS);}
 private final Map<UUID,Observation> waiting=new HashMap<>();private final LinkedHashSet<UUID> completed=new LinkedHashSet<>();
 public boolean capture(EffectObserver.PendingDamage pending,EntityDeathEvent death){
  if(!Bukkit.isPrimaryThread())return false;expire();
  if(pending==null||death==null||death.isCancelled()||death.getEntity() instanceof Player||death.getEntity() instanceof ArmorStand||pending.dispatch()==null||pending.cast()==null||pending.caster()==null||pending.spell()==null||pending.spell().isBlank()||!death.getEntity().getUniqueId().equals(pending.recipient())||completed.contains(pending.dispatch()))return false;
  var source=pending.source();if(source==null||source.observedMakerItemId()==null||source.wandId()==null||source.wandId().isBlank()||death.getEntity().getLastDamageCause()==null)return false;
  var old=waiting.get(pending.dispatch());if(old!=null){if(old.death!=death||!old.pending.equals(pending))old.ambiguous=true;return !old.ambiguous;}
  if(waiting.size()>=1024)return false;waiting.put(pending.dispatch(),new Observation(pending,death,nanoTime.getAsLong()));return true;
 }
 public Optional<Evidence> complete(EffectObserver.DamageCompletion completion){
  if(!Bukkit.isPrimaryThread())return Optional.empty();expire();
  if(completion==null||completion.pending()==null)return Optional.empty();var pending=completion.pending();Observation seen=waiting.remove(pending.dispatch());if(seen==null)return Optional.empty();
  completed.add(pending.dispatch());if(completed.size()>4096)completed.remove(completed.iterator().next());
  if(seen.ambiguous||!seen.pending.equals(pending)||completion.verdict()!=EffectObserver.DamageVerdict.VERIFIED||!completion.deathObserved()||completion.deathEvent()!=seen.death||completion.verifiedEvent()!=seen.cause||seen.death.isCancelled()||seen.cause.isCancelled()||!Double.isFinite(seen.cause.getFinalDamage())||seen.cause.getFinalDamage()<=0||seen.death.getEntity().getLastDamageCause()!=seen.cause)return Optional.empty();
  var receipt=completion.evidence();if(receipt==null||receipt.kind()!=EffectObserver.Kind.DIRECT_DAMAGE||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0||!Objects.equals(receipt.cast(),pending.cast())||!Objects.equals(receipt.caster(),pending.caster())||!Objects.equals(receipt.spell(),pending.spell())||!Objects.equals(receipt.recipient(),pending.recipient())||!Objects.equals(receipt.source(),pending.source()))return Optional.empty();
  var source=pending.source();return Optional.of(new Evidence(pending.dispatch(),pending.cast(),pending.caster(),pending.spell(),pending.recipient(),source.observedMakerItemId(),source.wandId(),seen.type,seen.name));
 }
 public int waiting(){return waiting.size();}
 public void clear(){waiting.clear();completed.clear();}
}
