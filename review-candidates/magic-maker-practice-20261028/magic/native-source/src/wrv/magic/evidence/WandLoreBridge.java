package wrv.magic.evidence;
import java.util.*;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils;
import org.bukkit.inventory.ItemStack;
/** Native fresh-generation seam only. No provisioning, item writes or stored-lore parsing. */
public final class WandLoreBridge {
 private WandLoreBridge(){}
 @FunctionalInterface public interface Decorator {List<String> decorate(Wand wand,ItemStack exactTarget,List<String> immutableNativeLore);}
 private static volatile Registration current;
 private static final ThreadLocal<Boolean> rendering=ThreadLocal.withInitial(()->false);
 public static final class Registration implements AutoCloseable {
  private final Decorator decorator;private Registration(Decorator d){decorator=d;}
  @Override public void close(){synchronized(WandLoreBridge.class){if(current==this)current=null;}}
 }
 public static synchronized Registration install(Decorator decorator){Objects.requireNonNull(decorator);if(current!=null)throw new IllegalStateException("lore consumer already registered");return current=new Registration(decorator);}
 public static List<String> decorate(Wand wand,ItemStack exactTarget,List<String> nativeLore){
  Registration r=current;if(r==null||rendering.get()||wand==null||exactTarget==null||nativeLore==null)return nativeLore;
  rendering.set(true);
  try {
   if(wand.getItem()!=exactTarget)return nativeLore;
   List<String> base=List.copyOf(nativeLore);List<String> supplied=r.decorator.decorate(wand,exactTarget,base);
   if(current!=r||wand.getItem()!=exactTarget||supplied==null)return nativeLore;
   List<String> result=List.copyOf(supplied);
   // Existing history contract: preserve the fresh native prefix and at most four appended lines.
   if(result.size()<base.size()||result.size()>base.size()+4||!result.subList(0,base.size()).equals(base))return nativeLore;
   return result;
  }catch(Throwable consumerFailure){return nativeLore;}finally{rendering.remove();}
 }
 public static boolean setLore(CompatibilityUtils utils,ItemStack exactTarget,List<String> nativeLore,Wand wand){
  // Native setter is outside observer failure handling: its return/exception must remain native.
  return utils.setLore(exactTarget,decorate(wand,exactTarget,nativeLore));
 }
}
