package wrv.magic.evidence;
import java.util.*;import java.util.function.Consumer;
/** One immutable composition owned by the shared Magic adapter, never one root hook per consumer. */
public final class ConsumerFanout implements EffectObserver.Sink {
 public record Binding(String id,Set<String> spells,EffectObserver.Sink sink){public Binding{if(id==null||id.isBlank()||spells==null||spells.isEmpty()||sink==null)throw new IllegalArgumentException("binding");spells=Set.copyOf(spells);}}
 public record Failure(String consumer,String stage,Throwable error){}
 private final List<Binding> bindings;private final Set<String> spells;private final Consumer<Failure> failures;
 public ConsumerFanout(List<Binding> bindings,Consumer<Failure> failures){this.bindings=List.copyOf(bindings);this.failures=Objects.requireNonNull(failures);var ids=new HashSet<String>();var scope=new HashSet<String>();for(var b:this.bindings){if(!ids.add(b.id))throw new IllegalArgumentException("duplicate consumer");scope.addAll(b.spells);}if(scope.isEmpty())throw new IllegalArgumentException("empty composition");this.spells=Set.copyOf(scope);}
 public Set<String> spells(){return spells;}
 @Override public void castStarted(EffectObserver.RootEvidence root){if(root!=null)dispatch(root.spell(),"cast-start",b->b.sink.castStarted(root));}
 @Override public void rootFinished(EffectObserver.RootEvidence root){if(root!=null)dispatch(root.spell(),"finish",b->b.sink.rootFinished(root));}
 @Override public void rootCaptured(EffectObserver.RootEvidence root){if(root!=null)dispatch(root.spell(),"root",b->b.sink.rootCaptured(root));}
 @Override public void observe(EffectObserver.Receipt receipt){if(receipt==null)return;for(var b:bindings)if(b.spells.contains(receipt.spell()))try{b.sink.observe(receipt);}catch(Throwable failure){try{failures.accept(new Failure(b.id,"effect",failure));}catch(Throwable reportingFailure){/* Consumer diagnostics cannot change native combat. */}}}
 @Override public void damageStarted(EffectObserver.PendingDamage pending){if(pending==null)return;dispatch(pending.spell(),"damage-start",b->b.sink.damageStarted(pending));}
 @Override public void damageCompleted(EffectObserver.DamageCompletion completion){if(completion==null)return;dispatch(completion.pending().spell(),"damage-complete",b->b.sink.damageCompleted(completion));}
 private void dispatch(String spell,String stage,java.util.function.Consumer<Binding> call){for(var b:bindings)if(b.spells.contains(spell))try{call.accept(b);}catch(Throwable failure){try{failures.accept(new Failure(b.id,stage,failure));}catch(Throwable reportingFailure){}}}

}
