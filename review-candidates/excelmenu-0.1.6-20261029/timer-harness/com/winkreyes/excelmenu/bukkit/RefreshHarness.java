// Excel signature - WRV maintained source; existing authorship notices preserved.
package com.winkreyes.excelmenu.bukkit;
import java.util.*;import org.bukkit.inventory.*;import com.winkreyes.excelmenu.core.MenuDef;
class Player{} class BukkitContext { BukkitContext(Player p){} }
class Selector {static Map<Integer,MenuDef.ItemDef> visibleItems(MenuDef d,BukkitContext c){return d.visible;}}
public class RefreshHarness {
Object buildStack(Player p,MenuDef.ItemDef i){return i.id();}
    void fillInventory(Player player, MenuDef def, Inventory inv, MenuHolder holder,
                       boolean onlyUpdates) {
        BukkitContext ctx = new BukkitContext(player);
        Map<Integer, MenuDef.ItemDef> visible = Selector.visibleItems(def, ctx);
        for (Map.Entry<Integer, MenuDef.ItemDef> e : visible.entrySet()) {
            if (onlyUpdates && !e.getValue().update()) continue;
            inv.setItem(e.getKey(), buildStack(player, e.getValue()));
            if (onlyUpdates) holder.replaceSlotItem(e.getKey(), e.getValue().id());
        }
        if (!onlyUpdates) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (!visible.containsKey(slot)) inv.setItem(slot, null);
            }
            holder.replaceSlotItems(visible);
        }
        holder.markRendered();
    }


static class Inv implements Inventory {Map<Integer,Object> slots=new HashMap<>();public void setItem(int s,Object x){if(x==null)slots.remove(s);else slots.put(s,x);}public int getSize(){return 54;}}
static void check(boolean x,String why){if(!x)throw new AssertionError(why);}
public static void main(String[] args){
 boolean fixed=Boolean.parseBoolean(args[0]); RefreshHarness service=new RefreshHarness(); Player player=new Player();
 Inv inv=new Inv(); inv.setItem(42,"on"); inv.setItem(10,"static-old");
 MenuHolder holder=new MenuHolder("test",Map.of(42,"on",10,"static-old"));holder.bind(inv);
 MenuDef d=new MenuDef();d.visible.put(42,new MenuDef.ItemDef("off",true));d.visible.put(10,new MenuDef.ItemDef("static-new",false));
 service.fillInventory(player,d,inv,holder,true);
 check(inv.slots.get(42).equals("off"),"timer must render off");
 check(holder.itemAt(42).equals(fixed?"off":"on"),"timer mapping expected");
 check(inv.slots.get(10).equals("static-old") && holder.itemAt(10).equals("static-old"),"nonupdate slot unchanged");
 System.out.println("TIMER rendered="+inv.slots.get(42)+" click="+holder.itemAt(42)+(fixed?" MATCH":" MISMATCH REPRODUCED"));
 d.visible.put(42,new MenuDef.ItemDef("on",true));service.fillInventory(player,d,inv,holder,true);
 check(inv.slots.get(42).equals("on")&&holder.itemAt(42).equals("on"),"reverse state");
 d.visible.put(42,new MenuDef.ItemDef("off",false));service.fillInventory(player,d,inv,holder,false);
 check(inv.slots.get(42).equals("off")&&holder.itemAt(42).equals("off"),"full refresh including nonupdate");
 check(inv.slots.get(10).equals("static-new")&&holder.itemAt(10).equals("static-new"),"full refresh updates static");
 d.visible.remove(42);service.fillInventory(player,d,inv,holder,false);
 check(!inv.slots.containsKey(42)&&holder.itemAt(42)==null,"full refresh disappearance clears both");
 check(holder.lastRenderMillis()>0,"render time");
 d.visible.put(42,new MenuDef.ItemDef("off",true));d.visible.put(43,new MenuDef.ItemDef("new",true));service.fillInventory(player,d,inv,holder,true);
 check(inv.slots.get(42).equals("off") && (fixed?"off".equals(holder.itemAt(42)):holder.itemAt(42)==null),"timer newly appearing slot");
 check(inv.slots.get(43).equals("new") && (fixed?"new".equals(holder.itemAt(43)):holder.itemAt(43)==null),"multiple timer slots");
 d.visible.remove(43);service.fillInventory(player,d,inv,holder,true);
 check(inv.slots.get(43).equals("new") && (fixed?"new".equals(holder.itemAt(43)):holder.itemAt(43)==null),"timer disappearance preserves existing behavior");
 d.visible.put(42,new MenuDef.ItemDef("nonupdate-replacement",false));service.fillInventory(player,d,inv,holder,true);
 check(inv.slots.get(42).equals("off") && (fixed?"off".equals(holder.itemAt(42)):holder.itemAt(42)==null),"nonupdate replacement not rendered or remapped");
 service.fillInventory(player,d,inv,holder,false);check(holder.itemAt(43)==null && !inv.slots.containsKey(43),"subsequent full refresh clears stale timer slot");
 System.out.println("13 assertions PASS; extracted production method with test doubles, not Paper/client acceptance");
}}
