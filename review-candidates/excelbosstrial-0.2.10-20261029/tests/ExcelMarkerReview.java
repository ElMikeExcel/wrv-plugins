// Excel independent regression review. Actual Bukkit API classes; synthetic proxies, no server/world mutations.
import com.winkreyes.teamrewards.marker.*;
import org.bukkit.*;import org.bukkit.block.*;import org.bukkit.inventory.*;import org.bukkit.persistence.*;import org.bukkit.plugin.*;import org.bukkit.event.inventory.*;import java.lang.reflect.*;import java.nio.file.*;import java.util.*;
public class ExcelMarkerReview {
 static class SyntheticStack extends ItemStack {SyntheticStack(){super();}}
 static int checks, sideInventoryCalls; static boolean baseline;
 interface Fn {Object call(Method m,Object[] a)throws Throwable;}
 static <T>T proxy(Class<T> t, Fn fn){return t.cast(Proxy.newProxyInstance(t.getClassLoader(),new Class[]{t},(o,m,a)->{if(m.getName().equals("toString"))return "synthetic";if(m.getName().equals("hashCode"))return System.identityHashCode(o);if(m.getName().equals("equals"))return o==a[0];Object v=fn.call(m,a);if(v!=null)return v;if(m.getReturnType()==boolean.class)return false;if(m.getReturnType()==int.class)return 0;return null;}));}
 static Chest chest(boolean mark, Inventory[] combined){
  PersistentDataContainer p=proxy(PersistentDataContainer.class,(m,a)->m.getName().equals("has")?mark:null);
  return proxy(Chest.class,(m,a)->{if(m.getName().equals("getPersistentDataContainer"))return p;if(m.getName().equals("getInventory")){sideInventoryCalls++;return combined[0];}return null;});
 }
 static Inventory inv(InventoryHolder h){return proxy(Inventory.class,(m,a)->m.getName().equals("getHolder")?h:null);}
 static Inventory doubleInv(boolean left,boolean right){Inventory[] combined=new Inventory[1];Chest l=chest(left,combined),r=chest(right,combined);Inventory li=inv(l),ri=inv(r);DoubleChestInventory d=proxy(DoubleChestInventory.class,(m,a)->switch(m.getName()){case "getLeftSide"->li;case "getRightSide"->ri;case "getHolder"->new DoubleChest((DoubleChestInventory)combined[0]);default->null;});combined[0]=d;return d;}
 static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
 static void events(PersonalChestMarkers p,Inventory top,boolean expected){
  Inventory normal=inv(null); InventoryView v=proxy(InventoryView.class,(m,a)->switch(m.getName()){case "getTopInventory"->top;case "getBottomInventory"->normal;case "getType"->InventoryType.CHEST;case "convertSlot"->a[0];default->null;});
  InventoryOpenEvent o=new InventoryOpenEvent(v);p.inventoryOpen(o);ok(o.isCancelled()==expected,"open");
  InventoryClickEvent c=new InventoryClickEvent(v,InventoryType.SlotType.CONTAINER,0,ClickType.LEFT,InventoryAction.PICKUP_ALL);p.click(c);ok(c.isCancelled()==expected,"click");
  InventoryDragEvent d=new InventoryDragEvent(v,new SyntheticStack(),new SyntheticStack(),false,Map.of());p.drag(d);ok(d.isCancelled()==expected,"drag");
  InventoryMoveItemEvent ms=new InventoryMoveItemEvent(top,new SyntheticStack(),normal,true);p.move(ms);ok(ms.isCancelled()==expected,"move source");
  InventoryMoveItemEvent md=new InventoryMoveItemEvent(normal,new SyntheticStack(),top,true);p.move(md);ok(md.isCancelled()==expected,"move destination");
  InventoryPickupItemEvent pi=new InventoryPickupItemEvent(top,null);p.pickup(pi);ok(pi.isCancelled()==expected,"pickup");
 }
 public static void main(String[] args)throws Exception{
  baseline=args.length>0&&args[0].equals("baseline");
  Plugin plug=proxy(Plugin.class,(m,a)->m.getName().equals("getName")?"ExcelBossTrial":m.getName().equals("namespace")?"excelbosstrial":null);
  try(MarkerRegistry registry=new MarkerRegistry(Files.createTempDirectory("excel-marker-review-").resolve("markers.bin"))){
   PersonalChestMarkers p=new PersonalChestMarkers(plug,registry,new PersonalChestMarkers.Authorization(){public boolean mayPlace(UUID o,Block b){return false;}public boolean mayRemove(UUID o,Block b){return false;}},player->{throw new AssertionError("unexpected claim opening");});
   Method tagged=PersonalChestMarkers.class.getDeclaredMethod("tagged",Inventory.class);tagged.setAccessible(true);
   if(baseline){try{tagged.invoke(p,doubleInv(false,false));throw new AssertionError("baseline unexpectedly terminates");}catch(InvocationTargetException e){if(!(e.getCause() instanceof StackOverflowError))throw e;System.out.println("BASELINE StackOverflow reproduced using actual DoubleChest class with combined side inventory behavior");}return;}
   for(boolean l:new boolean[]{false,true})for(boolean r:new boolean[]{false,true}){Inventory d=doubleInv(l,r);ok((boolean)tagged.invoke(p,d)==(l||r),"both sides detection");events(p,d,l||r);}
   Inventory[] empty={null}; events(p,inv(chest(true,empty)),true);events(p,inv(chest(false,empty)),false);events(p,inv(null),false);
   ok(!(boolean)tagged.invoke(p,(Object)null),"null inventory");
   DoubleChestInventory nul=proxy(DoubleChestInventory.class,(m,a)->m.getName().equals("getLeftSide")||m.getName().equals("getRightSide")?inv(null):null);ok(!(boolean)tagged.invoke(p,inv(new DoubleChest(nul))),"null sides");
   ok(sideInventoryCalls==0,"never follows Chest.getInventory cycle");
   System.out.println("PASS "+checks+" actual API holder/event checks; no side getInventory calls");
  }
 }
}
