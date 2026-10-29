package wrv.magic.evidence;
import java.util.*;
/** Read-only exact heal observation, available only during delivery of that same receipt object. */
public final class HealthTransitionBridge {
 public record Observation(UUID recipient,double before,double after,double maximum){}
 private record Frame(EffectObserver.Receipt receipt,Observation health){}
 private static final ThreadLocal<Frame> current=new ThreadLocal<>();
 static void deliver(EffectObserver.Receipt receipt,UUID target,double before,double after,double maximum,Runnable consumer){Frame prior=current.get();current.set(new Frame(receipt,new Observation(target,before,after,maximum)));try{consumer.run();}finally{if(prior==null)current.remove();else current.set(prior);}}
 public static Optional<Observation> current(EffectObserver.Receipt receipt){var f=current.get();return f!=null&&f.receipt()==receipt?Optional.of(f.health()):Optional.empty();}
 private HealthTransitionBridge(){}
}
