package wrv.magic.level;
import java.util.*;import java.util.function.Supplier;
import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.wand.Wand;
/** Exact original native/display request scope, never resolves an item from a mutable held slot or template lookup. */
public final class OriginalWandContext {
 public record Authority(Mage mage,Wand wand){}
 private record Frame(LearnedLevelResolver.Request request,Authority authority){}
 private static final ThreadLocal<Frame> current=new ThreadLocal<>();
 public static <T>T withOriginal(LearnedLevelResolver.Request request,Mage mage,Wand wand,Supplier<T> operation){
  Objects.requireNonNull(request);Objects.requireNonNull(mage);Objects.requireNonNull(wand);Objects.requireNonNull(operation);
  Frame prior=current.get();current.set(new Frame(request,new Authority(mage,wand)));
  try{return operation.get();}finally{if(prior==null)current.remove();else current.set(prior);}
 }
 public static Optional<Authority> current(LearnedLevelResolver.Request request){Frame f=current.get();return f!=null&&f.request()==request?Optional.of(f.authority()):Optional.empty();}
 private OriginalWandContext(){}
}
