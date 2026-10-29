import wrv.magic.evidence.WandLoreBridge;
import com.elmakers.mine.bukkit.api.wand.Wand;
import com.elmakers.mine.bukkit.utility.platform.CompatibilityUtils;
import org.bukkit.inventory.ItemStack;
import java.lang.reflect.*;import java.util.*;import java.util.concurrent.atomic.*;
public class LoreBridgeTest {
 static int n;static void ok(boolean b){if(!b)throw new AssertionError(n);n++;}
 public static void main(String[]args)throws Exception {
  var f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(sun.misc.Unsafe)f.get(null);
  ItemStack item=(ItemStack)u.allocateInstance(ItemStack.class),other=(ItemStack)u.allocateInstance(ItemStack.class);AtomicReference<ItemStack> held=new AtomicReference<>(item);
  Wand w=(Wand)Proxy.newProxyInstance(Wand.class.getClassLoader(),new Class[]{Wand.class},(p,m,a)->m.getName().equals("getItem")?held.get():null);
  List<String> base=new ArrayList<>(List.of("native","foreign"));ok(WandLoreBridge.decorate(w,item,base)==base);
  AtomicInteger calls=new AtomicInteger();AtomicReference<List<String>> got=new AtomicReference<>();
  CompatibilityUtils utils=(CompatibilityUtils)Proxy.newProxyInstance(CompatibilityUtils.class.getClassLoader(),new Class[]{CompatibilityUtils.class},(p,m,a)->{if(m.getName().equals("setLore")){ok(a[0]==item);calls.incrementAndGet();got.set((List<String>)a[1]);return true;}throw new AssertionError(m);});
  try(var reg=WandLoreBridge.install((wand,target,lore)->{ok(wand==w&&target==item);try{lore.add("bad");throw new AssertionError();}catch(UnsupportedOperationException expected){n++;}var result=new ArrayList<>(lore);result.add("history");return result;})){
   ok(WandLoreBridge.setLore(utils,item,base,w));ok(calls.get()==1);ok(got.get().equals(List.of("native","foreign","history")));ok(base.size()==2);
   try{WandLoreBridge.install((a,b,c)->c);throw new AssertionError();}catch(IllegalStateException expected){n++;}
   held.set(other);ok(WandLoreBridge.decorate(w,item,base)==base);held.set(item);
  }
  ok(WandLoreBridge.decorate(w,item,base)==base);
  try(var reg=WandLoreBridge.install((a,b,c)->{throw new AssertionError("consumer");})){ok(WandLoreBridge.decorate(w,item,base)==base);}
  try(var reg=WandLoreBridge.install((a,b,c)->List.of("overwrite"))){ok(WandLoreBridge.decorate(w,item,base)==base);}
  try(var reg=WandLoreBridge.install((a,b,c)->{ok(WandLoreBridge.decorate(a,b,c)==c);return c;})){ok(WandLoreBridge.decorate(w,item,base).equals(base));}
  try(var reg=WandLoreBridge.install((a,b,c)->{held.set(other);return c;})){ok(WandLoreBridge.decorate(w,item,base)==base);held.set(item);}
  var r=WandLoreBridge.install((a,b,c)->c);r.close();try(var newer=WandLoreBridge.install((a,b,c)->{var x=new ArrayList<>(c);x.add("new");return x;})){r.close();ok(WandLoreBridge.decorate(w,item,base).size()==3);}
  CompatibilityUtils throwing=(CompatibilityUtils)Proxy.newProxyInstance(CompatibilityUtils.class.getClassLoader(),new Class[]{CompatibilityUtils.class},(p,m,a)->{calls.incrementAndGet();throw new IllegalArgumentException("native");});int before=calls.get();try{WandLoreBridge.setLore(throwing,item,base,w);throw new AssertionError();}catch(IllegalArgumentException expected){ok(calls.get()==before+1);}
  System.out.println("PASS "+n+" lore bridge boundary checks (proxy native API; no runtime inventory)");
 }
}
