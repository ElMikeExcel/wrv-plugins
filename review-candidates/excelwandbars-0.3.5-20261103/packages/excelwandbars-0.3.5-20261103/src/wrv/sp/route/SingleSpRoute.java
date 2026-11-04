package wrv.sp.route;
import wrv.magic.evidence.*;import java.util.*;import java.lang.ref.*;
/** Inactive one-session routing only. No currency API, payout arithmetic, persistence, backpay or native patch. */
public final class SingleSpRoute {
 public enum LateProof { REQUIRE_BEFORE_EVENT, USE_SAVED_EVENT_SNAPSHOT }
 public enum Route { NONE,LEGACY,VERIFIED }
 /** The trusted legacy-event adapter supplies these values at CastEvent dispatch, never at root capture. */
 public record Event(UUID caster,String spell,boolean success,long eventTime,String chicagoDay,int earns,long cooldown,double multiplier,String policyId){}
 public record Decision(Route route,Event event,EffectObserver.RootEvidence root,String reason){}
 private static final class Key extends WeakReference<Object>{final int hash;Key(Object value,ReferenceQueue<Object> queue){super(value,queue);hash=System.identityHashCode(value);}public int hashCode(){return hash;}public boolean equals(Object x){return this==x||x instanceof Key k&&get()!=null&&get()==k.get();}}
 private static final class State {EffectObserver.RootEvidence root;Event event;boolean useful,claimed,closed;String spell;State(String spell){this.spell=spell;}}
 private final Set<String> managed;private final LateProof lateProof;private final Thread owner=Thread.currentThread();private final Map<Key,State> states=new HashMap<>();private final ReferenceQueue<Object> queue=new ReferenceQueue<>();private boolean detached;
 public SingleSpRoute(Set<String> managed,LateProof lateProof){for(String k:managed)if(k==null||!k.matches("[a-z0-9_.-]+"))throw new IllegalArgumentException("base keys");this.managed=Set.copyOf(managed);this.lateProof=Objects.requireNonNull(lateProof);}
 private void ready(){if(Thread.currentThread()!=owner||detached)throw new IllegalStateException("wrong owner/detached generation");for(Reference<?> r;(r=queue.poll())!=null;)states.remove(r);}
 private State get(Object context){Objects.requireNonNull(context);return states.get(new Key(context,null));}
 private Decision none(State s,String why){return new Decision(Route.NONE,s==null?null:s.event,s==null?null:s.root,why);}
 /** Trusted current-generation observer adapter must pass the EXACT native root context. No time/caster matching. */
 public void capture(Object rootContext,EffectObserver.RootEvidence root){ready();Objects.requireNonNull(rootContext);Objects.requireNonNull(root);if(root.cast()==null||root.caster()==null||root.source()==null||root.spell()==null)throw new IllegalArgumentException("original source missing");
  State s=get(rootContext);if(s!=null){if(!root.equals(s.root))throw new IllegalStateException("context/source conflict");return;}
  s=new State(root.spell());s.root=root;states.put(new Key(rootContext,queue),s);
 }
 public Decision event(Object rootContext,Event event){ready();Objects.requireNonNull(event);Objects.requireNonNull(rootContext);valid(event);State s=get(rootContext);
  if(s==null){if(managed.contains(event.spell())){s=new State(event.spell());s.event=event;s.closed=true;states.put(new Key(rootContext,queue),s);return none(s,"managed source unavailable; never fall back or backfill");}s=new State(event.spell());states.put(new Key(rootContext,queue),s);}
  if(!s.spell.equals(event.spell())||s.root!=null&&!s.root.caster().equals(event.caster()))return none(s,"event/source mismatch");
  if(s.event!=null)return none(s,s.event.equals(event)?"duplicate event":"conflicting event");s.event=event;
  if(s.closed||s.claimed)return none(s,"closed/consumed");if(!event.success()){s.closed=true;return none(s,"native event unsuccessful");}
  if(!managed.contains(event.spell())){s.claimed=true;return new Decision(Route.LEGACY,event,s.root,"unchanged legacy route");}
  if(s.useful)return claim(s);
  if(lateProof==LateProof.REQUIRE_BEFORE_EVENT)s.closed=true;
  return none(s,"managed needs verified use; no legacy payout");
 }
 /** Eligibility is a mandatory trusted effect validator. Calling this method is not itself authentication. */
 public Decision verified(Object rootContext,EffectObserver.Receipt receipt,boolean eligible){ready();State s=get(rootContext);if(s==null||s.root==null||s.closed||s.claimed||receipt==null||!eligible)return none(s,"missing/ineligible/closed");
  if(!managed.contains(s.spell))return none(s,"unmanaged never takes verified route");
  // Shared SP currently authorizes direct useful healing/damage only; other evidence families remain separate policy.
  if(receipt.kind()!=EffectObserver.Kind.DIRECT_HEAL&&receipt.kind()!=EffectObserver.Kind.DIRECT_DAMAGE)return none(s,"SP evidence kind not enabled");
  if(receipt.recipient()==null||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0)return none(s,"no useful amount");
  if(!s.root.equals(new EffectObserver.RootEvidence(receipt.cast(),receipt.caster(),receipt.spell(),receipt.source())))return none(s,"exact root/source mismatch");
  s.useful=true;return s.event==null?none(s,"waiting for actual event-time policy snapshot"):claim(s);
 }
 private Decision claim(State s){if(s.claimed||s.closed||s.event==null||!s.event.success()||!s.useful)return none(s,"not ready");s.claimed=true;return new Decision(Route.VERIFIED,s.event,s.root,"one verified route; no automatic retry");}
 /** Only trusted lifecycle proof may close. Keep weak-key tombstone while the original context is reachable. */
 public void close(Object rootContext){ready();State s=get(rootContext);if(s!=null)s.closed=true;}
 public void detach(){ready();detached=true;states.clear();}
 private static void valid(Event e){if(e.caster()==null||e.spell()==null||!e.spell().matches("[a-z0-9_.-]+")||e.eventTime()<0||e.chicagoDay()==null||e.chicagoDay().isBlank()||e.earns()<0||e.cooldown()<0||!Double.isFinite(e.multiplier())||e.multiplier()<0||e.policyId()==null||e.policyId().isBlank())throw new IllegalArgumentException("explicit native event snapshot");}
}
