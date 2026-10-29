package wrv.magic.evidence;

import wrv.magic.retention.BoundedPractice;
import wrv.magic.retention.AuthenticatedWindow;
import java.util.*;
import java.util.function.Predicate;

/** Inactive, result-bearing practice binding. No SP, rank, or inventory writes. */
public final class PracticeConsumerAckV2 implements IssuedUsefulSinkV2.ConsumerAck {
 public enum TimeBasis { CAST_ISSUANCE,EFFECT_OCCURRENCE }
 public record Admission(UUID cast,UUID caster,String spell,EffectObserver.SourceSnapshot source,String ownerScope,long trustedTimeMillis,TimeBasis timeBasis){}
 public interface AdmissionResolver {Admission resolve(EffectObserver.Receipt receipt)throws Exception;}
 private final AuthenticatedWindow issuer;
 private final Thread ownerThread=Thread.currentThread();
 private final Map<String, BoundedPractice> ledgers;
 private final AdmissionResolver resolver;
 private final TimeBasis timeBasis;
 private final Predicate<EffectObserver.Receipt> eligible;
 public PracticeConsumerAckV2(AuthenticatedWindow issuer, Map<String, BoundedPractice> ledgers,
   AdmissionResolver resolver, TimeBasis timeBasis,
   Predicate<EffectObserver.Receipt> eligible) {
  this.issuer=Objects.requireNonNull(issuer);this.ledgers=Map.copyOf(ledgers);
  this.resolver=Objects.requireNonNull(resolver);this.timeBasis=Objects.requireNonNull(timeBasis);
  this.eligible=Objects.requireNonNull(eligible);
 }
 @Override public AuthenticatedWindow.Result apply(AuthenticatedWindow.Token token, EffectObserver.Receipt r) throws Exception {
  if(Thread.currentThread()!=ownerThread)throw new IllegalStateException("practice binding requires its serialized owner thread");
  // Caller identity alone is not a grant. The exact token must still be live/useful in this issuer.
  if(token==null||r==null||r.source()==null||r.cast()==null||r.caster()==null||r.recipient()==null
    ||r.kind()==null||!validCleanseEvidence(r)
    ||!Double.isFinite(r.effectiveAmount())||r.effectiveAmount()<=0
    ||!Objects.equals(token.caster(),r.caster())||!Objects.equals(token.spell(),r.spell())
    ||!Objects.equals(token.sourceHash(),EvidenceFingerprint.root(new EffectObserver.RootEvidence(r.cast(),r.caster(),r.spell(),r.source())))||!issuer.isOpenUseful(token,"first-use",EvidenceFingerprint.receipt(r))||!eligible.test(r))
   return AuthenticatedWindow.Result.REVIEW;
  BoundedPractice ledger=ledgers.get(r.spell());
  if(ledger==null)return AuthenticatedWindow.Result.REVIEW;
  var a=resolver.resolve(r);
  if(a==null||!Objects.equals(a.cast(),r.cast())||!Objects.equals(a.caster(),r.caster())
    ||!Objects.equals(a.spell(),r.spell())||!Objects.equals(a.source(),r.source())
    ||a.ownerScope()==null||a.ownerScope().isBlank()||a.trustedTimeMillis()<0||a.timeBasis()!=timeBasis
    ||(timeBasis==TimeBasis.CAST_ISSUANCE&&a.trustedTimeMillis()!=r.source().issuedAtMillis()))
   return AuthenticatedWindow.Result.REVIEW;
  var kind=switch(r.kind()) {
   case DIRECT_HEAL -> BoundedPractice.EffectiveKind.DIRECT_HEAL;
   case DIRECT_DAMAGE -> BoundedPractice.EffectiveKind.HOSTILE_DAMAGE;
   case DIRECT_CONTROL -> BoundedPractice.EffectiveKind.HOSTILE_CONTROL;
   case DIRECT_CLEANSE -> BoundedPractice.EffectiveKind.DIRECT_CLEANSE;
  };
  synchronized(issuer) {
  // Resolver/eligibility callbacks can be reentrant. Revalidate after both, with issuer mutations excluded.
  if(!issuer.isOpenUseful(token,"first-use",EvidenceFingerprint.receipt(r)))return AuthenticatedWindow.Result.REVIEW;
  sweepRetired();
  var result=ledger.recordIssued(issuer,token,EvidenceFingerprint.receipt(r),new BoundedPractice.EffectiveUse(r.cast(),a.ownerScope(),r.spell(),
    r.recipient(),kind,false,true,true,a.trustedTimeMillis()));
  return switch(result) {
   case CREDITED -> AuthenticatedWindow.Result.SETTLED;
   case RATE_LIMIT -> AuthenticatedWindow.Result.RATE_LIMITED;
   case DAILY_CAP -> AuthenticatedWindow.Result.DAILY_CAPPED;
   // Exact replay returns its persisted result; unsupported/uncertain outcomes still fail closed.
   case CAPACITY -> throw new java.io.IOException("practice retained-receipt capacity exhausted; credit held for reconciliation, progress preserved");
   case DUPLICATE, INEFFECTIVE -> AuthenticatedWindow.Result.REVIEW;
  };
  }
 }
 @Override public void afterRetired(AuthenticatedWindow.Token token,EffectObserver.Receipt r)throws Exception {
  if(Thread.currentThread()!=ownerThread)throw new IllegalStateException("practice binding requires its serialized owner thread");
  synchronized(issuer) {
   if(!issuer.isRetired(token))return;
   sweepRetired();
  }
 }
 private static boolean validCleanseEvidence(EffectObserver.Receipt r) {
  if(r.kind()!=EffectObserver.Kind.DIRECT_CLEANSE)return true;
  var c=r.cleanse();
  // Native observer already proves one accepted removal of a negative effect.
  // Keep metadata required and leave target eligibility/accepted kinds explicit.
  return c!=null&&c.effectKey()!=null&&c.effectKey().matches("[a-z0-9_.-]+:[a-z0-9_/.-]+")
    &&c.beforeAmplifier()>=0&&c.beforeDuration()!=0&&r.effectiveAmount()==1d&&r.control()==null;
 }
 private void sweepRetired()throws Exception {
  // A previous sparse token can become retired only when a later close advances the watermark.
  // Every deletion still obtains exact issuer proof; absent records, age and epoch alone are not proof.
  for(var ledger:ledgers.values())for(var binding:ledger.retainedBindings())
   if(issuer.isRetired(binding.token()))ledger.prune(issuer,binding.token(),binding.cast());
 }
}
