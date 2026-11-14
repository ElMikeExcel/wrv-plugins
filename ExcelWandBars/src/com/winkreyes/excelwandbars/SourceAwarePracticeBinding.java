package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.*;import java.io.IOException;
import wrv.magic.evidence.*;import wrv.magic.retention.*;import wrv.magic.eligibility.*;
/** One durable practice consumer. The exact same cached gate is checked at issuance-use and acknowledgment. */
final class SourceAwarePracticeBinding {
 static final class Bound implements EffectObserver.Sink {
  private final IssuedUsefulSinkV2 delegate;private final SelfHealReceiptGate gate;
  private Bound(IssuedUsefulSinkV2 delegate,SelfHealReceiptGate gate){this.delegate=delegate;this.gate=gate;}
  boolean uses(SelfHealReceiptGate candidate){return gate==candidate;}
  public void observe(EffectObserver.Receipt r){delegate.observe(r);}
  public void castStarted(EffectObserver.RootEvidence r){delegate.castStarted(r);}
  public void rootCaptured(EffectObserver.RootEvidence r){delegate.rootCaptured(r);}
  public void rootFinished(EffectObserver.RootEvidence r){delegate.rootFinished(r);}
  public void damageStarted(EffectObserver.PendingDamage r){delegate.damageStarted(r);}
  public void damageCompleted(EffectObserver.DamageCompletion r){delegate.damageCompleted(r);}
 }
 static Bound build(AuthenticatedWindow issuer, Map<String,BoundedPractice> ledgers,
       Predicate<EffectObserver.RootEvidence> originEligibility,
       PracticeConsumerAckV2.AdmissionResolver admission, SelfHealReceiptGate gate,
       Consumer<Exception> errors) throws IOException {
  Objects.requireNonNull(gate);
  if(!issuer.consumerIds().equals(Set.of("practice")))throw new IllegalArgumentException("dedicated practice issuer required");
  var ack=new PracticeConsumerAckV2(issuer,ledgers,admission,PracticeConsumerAckV2.TimeBasis.CAST_ISSUANCE,gate);
  return new Bound(new IssuedUsefulSinkV2(issuer,originEligibility,gate,Map.of("practice",ack),errors),gate);
 }
 private SourceAwarePracticeBinding(){}
}
