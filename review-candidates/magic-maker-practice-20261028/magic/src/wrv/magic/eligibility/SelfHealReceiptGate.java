package wrv.magic.eligibility;
import wrv.magic.evidence.EffectObserver;import java.util.*;import java.util.function.Predicate;
/** Prepare once at the actual heal boundary; consumers only read the same immutable decision. */
public final class SelfHealReceiptGate implements Predicate<EffectObserver.Receipt> {
 private final CombatDeficitLedger ledger;private final int capacity;private final Predicate<EffectObserver.Receipt> otherEligibility;
 private final Map<EffectObserver.Receipt,Boolean> decisions=new IdentityHashMap<>();private final Thread owner=Thread.currentThread();private boolean closed;
 public SelfHealReceiptGate(CombatDeficitLedger ledger,int capacity,Predicate<EffectObserver.Receipt> otherEligibility){this.ledger=Objects.requireNonNull(ledger);if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;this.otherEligibility=Objects.requireNonNull(otherEligibility);}
 private void thread(){if(Thread.currentThread()!=owner)throw new IllegalStateException("serialized owner required");}
 /** Exact observer receipt and actual before/after HP are mandatory; identity replay cannot consume twice. */
 public boolean prepare(EffectObserver.Receipt r,CombatDeficitLedger.Frame f){thread();if(closed)return false;if(r==null||r.kind()!=EffectObserver.Kind.DIRECT_HEAL||r.recipient()==null||r.caster()==null)return false;if(decisions.containsKey(r))return decisions.get(r);
  if(decisions.size()>=capacity){ledger.invalidate(r.recipient());return false;}
  if(f==null||!Objects.equals(r.recipient(),f.player())||!Double.isFinite(r.effectiveAmount())||r.effectiveAmount()<=0||Double.compare(r.effectiveAmount(),f.after()-f.before())!=0){ledger.invalidate(r.recipient());decisions.put(r,false);return false;}
  var result=ledger.restore(r.recipient(),f);boolean allowed=result.accepted()&&result.eligibleRestored()>0;decisions.put(r,allowed);return allowed;
 }
 @Override public boolean test(EffectObserver.Receipt r){thread();if(closed)return false;if(r==null)return false;if(r.kind()==EffectObserver.Kind.DIRECT_HEAL&&Objects.equals(r.caster(),r.recipient()))return Boolean.TRUE.equals(decisions.get(r));return otherEligibility.test(r);}
 /** Retire only when the shared owner has finished every consumer for these exact receipts. */
 public void retire(EffectObserver.Receipt r){thread();decisions.remove(r);}
 public void retireRoot(EffectObserver.RootEvidence root){thread();if(closed||root==null)return;decisions.keySet().removeIf(r->Objects.equals(r.cast(),root.cast())&&Objects.equals(r.caster(),root.caster())&&Objects.equals(r.spell(),root.spell())&&Objects.equals(r.source(),root.source()));}
 public void close(){thread();if(closed)return;closed=true;decisions.clear();ledger.clear();}
}
