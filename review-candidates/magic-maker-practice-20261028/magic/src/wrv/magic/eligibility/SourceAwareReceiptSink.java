package wrv.magic.eligibility;
import wrv.magic.evidence.EffectObserver;import java.util.*;
/** Decorates the existing shared fanout; never registers a second observer or awards SP itself. */
public final class SourceAwareReceiptSink implements EffectObserver.Sink,AutoCloseable {
 @FunctionalInterface public interface ExactHealthFrame{CombatDeficitLedger.Frame resolve(EffectObserver.Receipt receipt)throws Exception;}
 private final EffectObserver.Sink delegate;private final SelfHealReceiptGate gate;private final ExactHealthFrame frames;private boolean closed;
 public SourceAwareReceiptSink(EffectObserver.Sink existingFanout,SelfHealReceiptGate gate,ExactHealthFrame frames){this.delegate=Objects.requireNonNull(existingFanout);this.gate=Objects.requireNonNull(gate);this.frames=Objects.requireNonNull(frames);}
 @Override public void observe(EffectObserver.Receipt r){if(closed)return;if(r!=null&&r.kind()==EffectObserver.Kind.DIRECT_HEAL){CombatDeficitLedger.Frame f=null;try{f=frames.resolve(r);}catch(Exception unavailable){}gate.prepare(r,f);}delegate.observe(r);}
 @Override public void rootCaptured(EffectObserver.RootEvidence r){if(!closed)delegate.rootCaptured(r);}
 @Override public void castStarted(EffectObserver.RootEvidence r){if(!closed)delegate.castStarted(r);}
 @Override public void rootFinished(EffectObserver.RootEvidence r){if(closed)return;delegate.rootFinished(r);gate.retireRoot(r);}
 @Override public void damageStarted(EffectObserver.PendingDamage r){if(!closed)delegate.damageStarted(r);}
 @Override public void damageCompleted(EffectObserver.DamageCompletion r){if(!closed)delegate.damageCompleted(r);}
 @Override public void close(){if(closed)return;closed=true;gate.close();}
}
