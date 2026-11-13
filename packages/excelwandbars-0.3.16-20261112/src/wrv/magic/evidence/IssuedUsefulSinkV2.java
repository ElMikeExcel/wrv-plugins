package wrv.magic.evidence;
import wrv.magic.retention.AuthenticatedWindow;import java.util.*;import java.util.function.*;import java.security.*;import java.nio.charset.StandardCharsets;import java.io.*;
/** Dedicated one-use credit window; never a whole-Magic-cast or per-victim kill lifecycle. */
public final class IssuedUsefulSinkV2 implements EffectObserver.Sink {
 public interface ConsumerAck {AuthenticatedWindow.Result apply(AuthenticatedWindow.Token token,EffectObserver.Receipt evidence)throws Exception;default void afterRetired(AuthenticatedWindow.Token token,EffectObserver.Receipt evidence)throws Exception{}}
 private record Issued(EffectObserver.RootEvidence origin,AuthenticatedWindow.Token token){}
 private final AuthenticatedWindow issuer;private final Predicate<EffectObserver.RootEvidence> originEligible;private final Predicate<EffectObserver.Receipt> effectEligible;private final Map<String,ConsumerAck> consumers;private final Consumer<Exception> errors;
 private final Map<UUID,Issued> active=new HashMap<>();
 public IssuedUsefulSinkV2(AuthenticatedWindow issuer,Predicate<EffectObserver.RootEvidence> originEligible,Predicate<EffectObserver.Receipt> effectEligible,Map<String,ConsumerAck> consumers,Consumer<Exception> errors)throws IOException{this.issuer=Objects.requireNonNull(issuer);this.originEligible=Objects.requireNonNull(originEligible);this.effectEligible=Objects.requireNonNull(effectEligible);this.consumers=Map.copyOf(consumers);if(consumers.isEmpty()||!issuer.consumerIds().equals(consumers.keySet()))throw new IllegalArgumentException("consumer set must exactly match dedicated issuer");this.errors=Objects.requireNonNull(errors);}
 @Override public synchronized void castStarted(EffectObserver.RootEvidence root){try{if(root==null||root.source()==null||active.containsKey(root.cast())||!originEligible.test(root)||!EffectObserver.claimIssuance(root))return;var token=issuer.issue(root.caster(),root.spell(),EvidenceFingerprint.root(root));active.put(root.cast(),new Issued(root,token));}catch(Exception e){report(e);}}
 @Override public synchronized void observe(EffectObserver.Receipt receipt){
  Issued issued=receipt==null?null:active.get(receipt.cast());if(issued==null)return;
  try{if(!issued.origin.caster().equals(receipt.caster())||!issued.origin.spell().equals(receipt.spell())||!Objects.equals(issued.origin.source(),receipt.source())||!Double.isFinite(receipt.effectiveAmount())||receipt.effectiveAmount()<=0||!effectEligible.test(receipt))return;
   // Remove before invoking external consumers: reentrant/multiple effects cannot settle twice.
   active.remove(receipt.cast());issuer.useful(issued.token,"first-use",EvidenceFingerprint.receipt(receipt));
   for(var entry:consumers.entrySet()){
    AuthenticatedWindow.Result result;
    try{result=entry.getValue().apply(issued.token,receipt);if(result==null||result==AuthenticatedWindow.Result.CLOSED_NO_EFFECT)throw new IOException("invalid durable consumer acknowledgment");}
    catch(Throwable failure){result=AuthenticatedWindow.Result.REVIEW;report(new IOException("consumer result uncertain: "+entry.getKey(),failure));}
    issuer.settle(issued.token,entry.getKey(),result);
   }
   // Every configured one-use consumer has acknowledged or entered REVIEW. This does not close Magic children.
   issuer.closeCast(issued.token);
   if(issuer.isRetired(issued.token))for(var entry:consumers.entrySet())try{entry.getValue().afterRetired(issued.token,receipt);}catch(Throwable failure){report(new IOException("retirement callback failed: "+entry.getKey(),failure));}
  }catch(Exception e){report(e);}
 }
 @Override public synchronized void rootFinished(EffectObserver.RootEvidence root){var issued=active.get(root.cast());if(issued!=null&&issued.origin.equals(root))closeWithProof(root.cast());}
 /** Explicit authoritative lifecycle proof only, never inferred from root.finish(). */
 public synchronized void closeWithProof(UUID cast){var issued=active.remove(cast);if(issued!=null)try{issuer.closeCast(issued.token);}catch(Exception e){report(e);}}
 public synchronized int pending(){return active.size();}
 private void report(Exception e){try{errors.accept(e);}catch(Throwable ignored){}}
}
