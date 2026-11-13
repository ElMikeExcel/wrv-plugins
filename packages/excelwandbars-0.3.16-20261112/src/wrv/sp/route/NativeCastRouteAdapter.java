package wrv.sp.route;
import wrv.magic.evidence.EffectObserver;import com.elmakers.mine.bukkit.api.event.CastEvent;import java.util.*;import java.lang.ref.WeakReference;import java.util.function.*;
/** Inactive evidence-to-route adapter. No registration, SP writes, currency, persistence or historical replay. */
public final class NativeCastRouteAdapter implements EffectObserver.Sink{
 @FunctionalInterface public interface SnapshotProvider{SingleSpRoute.Event capture(CastEvent event);}
 private static final class Pending{final EffectObserver.RootEvidence root;EffectObserver.Receipt proof;WeakReference<Object> context;Pending(EffectObserver.RootEvidence r){root=r;}}
 private final Set<Object> seenFinishes=Collections.newSetFromMap(new WeakHashMap<>());private final Map<UUID,Pending> pending=new HashMap<>();private final int capacity;private final Set<String> managed;private final SingleSpRoute guard;private final SnapshotProvider snapshots;private final Predicate<EffectObserver.Receipt> eligibility;private final Consumer<SingleSpRoute.Decision> decisions;private final Consumer<Throwable> errors;private final Thread owner=Thread.currentThread();private boolean detached;
 public NativeCastRouteAdapter(Set<String> managed,SingleSpRoute.LateProof latePolicy,int capacity,SnapshotProvider snapshots,Predicate<EffectObserver.Receipt> eligibility,Consumer<SingleSpRoute.Decision> decisions,Consumer<Throwable> errors){if(capacity<1)throw new IllegalArgumentException("positive explicit capacity");this.managed=Set.copyOf(managed);this.guard=new SingleSpRoute(managed,latePolicy);this.capacity=capacity;this.snapshots=Objects.requireNonNull(snapshots);this.eligibility=Objects.requireNonNull(eligibility);this.decisions=Objects.requireNonNull(decisions);this.errors=Objects.requireNonNull(errors);}
 private boolean ready(){return !detached&&Thread.currentThread()==owner;}
 private void error(Throwable t){try{errors.accept(t);}catch(Throwable ignored){}}
 private void decision(SingleSpRoute.Decision d){if(d.route()!=SingleSpRoute.Route.NONE)try{decisions.accept(d);}catch(Throwable t){error(t);/* already claimed; never retry */}}
 @Override public void observe(EffectObserver.Receipt receipt){if(!ready()||receipt==null)return;try{
  Pending p=pending.get(receipt.cast());if(p==null||!p.root.equals(new EffectObserver.RootEvidence(receipt.cast(),receipt.caster(),receipt.spell(),receipt.source())))return;
  if(receipt.kind()!=EffectObserver.Kind.DIRECT_HEAL&&receipt.kind()!=EffectObserver.Kind.DIRECT_DAMAGE||receipt.recipient()==null||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0||!eligibility.test(receipt))return;
  if(p.proof!=null)return;p.proof=receipt;
  Object context=p.context==null?null:p.context.get();if(context!=null){
   var current=EffectObserver.originalRootEvidence((com.elmakers.mine.bukkit.api.action.CastContext)context);
   if(current.isPresent()&&current.get().equals(p.root))decision(guard.verified(context,receipt,true));
  }
 }catch(Throwable t){error(t);}}
 @Override public void castStarted(EffectObserver.RootEvidence root){if(!ready()||root==null||!managed.contains(root.spell()))return;try{
  if(root.cast()==null||root.caster()==null||root.source()==null)throw new IllegalArgumentException("source missing");Pending old=pending.get(root.cast());if(old!=null){if(!old.root.equals(root))throw new IllegalStateException("root conflict");return;}
  if(pending.size()>=capacity)throw new IllegalStateException("pending root capacity; no fallback");pending.put(root.cast(),new Pending(root));
 }catch(Throwable t){error(t);}}
 /** Invoke only from actual native CastEvent on the owning server thread, at the legacy listener point. */
 public void onCast(CastEvent event){if(!ready()||event==null)return;Object root=null;try{
  var context=event.getContext();if(context==null||context.getClass()!=com.elmakers.mine.bukkit.action.CastContext.class)return;root=context.getBaseContext();if(root==null||root.getClass()!=com.elmakers.mine.bukkit.action.CastContext.class)return;
  String spell=event.getSpell().getSpellKey().getBaseKey();if(managed.contains(spell)&&context!=root)return;
  if(!managed.contains(spell))root=context; // Unmanaged native event keeps its own context identity.
  if(!seenFinishes.add(root))return; // Failures cannot be retried as a new finish.
  var snapshot=Objects.requireNonNull(snapshots.capture(event));UUID caster=event.getMage().getPlayer().getUniqueId();boolean success=event.getSpellResult().isSuccess();
  if(!caster.equals(snapshot.caster())||!spell.equals(snapshot.spell())||success!=snapshot.success())throw new IllegalArgumentException("snapshot/native event mismatch");
  if(managed.contains(spell)){
   var evidence=EffectObserver.originalRootEvidence(context).orElse(null);Pending p=evidence==null?null:pending.get(evidence.cast());
   if(p!=null&&p.root.equals(evidence)){
    Object prior=p.context==null?null:p.context.get();if(prior!=null&&prior!=root)throw new IllegalStateException("root/context conflict");p.context=new WeakReference<>(root);guard.capture(root,evidence);if(p.proof!=null)decision(guard.verified(root,p.proof,true));
   }
  }
  decision(guard.event(root,snapshot));
 }catch(Throwable t){if(root!=null)try{guard.close(root);}catch(Throwable ignored){}error(t);}}
 @Override public void rootFinished(EffectObserver.RootEvidence root){if(!ready()||root==null)return;Pending p=pending.get(root.cast());if(p==null||!p.root.equals(root))return;Object context=p.context==null?null:p.context.get();if(context!=null)guard.close(context);pending.remove(root.cast());}
 /** Read-only current adapter-generation admission; does not create/backfill a root. */
 public boolean hasOriginalRoot(EffectObserver.RootEvidence root){if(!ready()||root==null)return false;Pending p=pending.get(root.cast());return p!=null&&p.root.equals(root);}
 public int pendingCount(){if(!ready())throw new IllegalStateException("wrong owner/detached");return pending.size();}
 public void detach(){if(!ready())throw new IllegalStateException("wrong owner/detached");detached=true;pending.clear();seenFinishes.clear();guard.detach();}
}
