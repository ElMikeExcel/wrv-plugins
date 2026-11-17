// ExcelSilkWatch — an Excel plugin by WinkReyesValley. (Excel signature — MikeExcel)
package verification;

import com.winkreyes.silkstorage.StorageObserver;
import com.winkreyes.silkstorage.SilkContents;
import com.winkreyes.silkstorage.ExcelSilkWatch;
import com.winkreyes.silkstorage.PreferenceStore;
import org.bukkit.command.*;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.inventory.meta.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerQuitEvent;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

public class ObserverProbe extends JavaPlugin {
    int passed;
    final List<String> results = new ArrayList<>();
    @Override public void onEnable() { getServer().getScheduler().runTask(this, () -> {
        try { tests(); results.add("PASS all " + passed + " cases"); }
        catch (Throwable e) { results.add("FAIL " + e); e.printStackTrace(); }
        try { Files.write(Path.of("probe-results.txt"),results); } catch(Exception e) {throw new RuntimeException(e);}
        results.forEach(getLogger()::info); getServer().shutdown();
    }); }
    void check(String label, Runnable test) { try {test.run();} catch(Throwable failure){throw new AssertionError(label, failure);} passed++; results.add("PASS " + label); }
    static void eq(Object a,Object b) { if(!Objects.equals(a,b))throw new AssertionError(a+" != "+b); }
    static ItemStack silk() { ItemStack s=new ItemStack(Material.DIAMOND_PICKAXE);s.addUnsafeEnchantment(Enchantment.SILK_TOUCH,1);return s; }
    static <T> T proxy(Class<T> cls, InvocationHandler h) { return cls.cast(Proxy.newProxyInstance(cls.getClassLoader(),new Class[]{cls},h)); }
    static Object base(Object p, Method m,Object[] a) { return switch(m.getName()) {case "equals" -> p==a[0];case "hashCode" -> System.identityHashCode(p);case "toString" -> "ReadOnlyProbe";default -> throw new AssertionError("Unexpected/mutating call: "+m);}; }
    static class F {
        ItemStack[] topItems=new ItemStack[27], bottomItems=new ItemStack[41];
        ItemStack cursor=silk(); UUID id=UUID.randomUUID(); boolean online=true; boolean ownEnderChest=true;
        boolean permission=true; List<String> messages=new ArrayList<>(); List<String> permissionsChecked=new ArrayList<>();
        int warnings; long now; ArrayList<Runnable> queued=new ArrayList<>();
        InventoryType type=InventoryType.ENDER_CHEST;
        Inventory top=proxy(Inventory.class,(p,m,a)->switch(m.getName()) {
            case "getType" -> type; case "getSize" -> 27;case "getContents" -> topItems.clone();case "getItem" -> topItems[(Integer)a[0]];default -> base(p,m,a);});
        PlayerInventory bottom=proxy(PlayerInventory.class,(p,m,a)->switch(m.getName()) {
            case "getType" -> InventoryType.PLAYER; case "getSize" -> 41;case "getContents" -> bottomItems.clone();case "getItem" -> bottomItems[(Integer)a[0]];case "getItemInOffHand" -> bottomItems[40];default -> base(p,m,a);});
        Player player=proxy(Player.class,(p,m,a)->switch(m.getName()) {
            case "hasPermission" -> {permissionsChecked.add(String.valueOf(a[0]));yield permission;}case "sendMessage" -> {messages.add(Arrays.toString(a));yield null;}case "getName" -> "ProbePlayer";case "getUniqueId" -> id;case "getEnderChest" -> ownEnderChest?top:bottom;case "isOnline" -> online;case "getInventory" -> bottom;default -> base(p,m,a);});
        InventoryView view=proxy(InventoryView.class,(p,m,a)->switch(m.getName()) {
            case "getTopInventory" -> top;case "getBottomInventory" -> bottom;case "getPlayer" -> player;case "getCursor" -> cursor;
            case "convertSlot" -> (Integer)a[0]<27?(Integer)a[0]:(Integer)a[0]-27;
            case "getInventory" -> (Integer)a[0]<0?null:((Integer)a[0]<27?top:bottom);
            case "getItem" -> (Integer)a[0]<27?topItems[(Integer)a[0]]:bottomItems[(Integer)a[0]-27];
            default -> base(p,m,a);});
        StorageObserver observer=new StorageObserver(queued::add,p->warnings++,()->now,3000);
        InventoryClickEvent click(int slot,InventoryAction action,ClickType click,int hotbar) {
            return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER,slot,click,action,hotbar);
        }
        InventoryClickEvent place() {return click(0,InventoryAction.PLACE_ALL,ClickType.LEFT,-1);}
        void tick() { var tasks=new ArrayList<>(queued);queued.clear();tasks.forEach(Runnable::run); }
        void deposit() {observer.click(place());topItems[0]=cursor.clone();tick();}
    }
    static ItemStack book() {var s=new ItemStack(Material.ENCHANTED_BOOK);var m=(EnchantmentStorageMeta)s.getItemMeta();m.addStoredEnchant(Enchantment.SILK_TOUCH,1,true);s.setItemMeta(m);return s;}
    static ItemStack box(Material material, ItemStack... contents) {var s=new ItemStack(material);var m=(BlockStateMeta)s.getItemMeta();var b=(ShulkerBox)m.getBlockState();for(int i=0;i<contents.length;i++)b.getInventory().setItem(i,contents[i]);m.setBlockState(b);s.setItemMeta(m);return s;}
    static ItemStack wrap(ItemStack s,int depth) {for(int i=0;i<depth;i++)s=box(Material.SHULKER_BOX,s);return s;}
    static class BrokenStack extends ItemStack {BrokenStack(){super(Material.SHULKER_BOX);} @Override public ItemMeta getItemMeta(){throw new IllegalStateException("broken fixture");} @Override public boolean containsEnchantment(Enchantment e){return false;} }
    static class GuardedStack extends ItemStack {final ItemMeta guarded;GuardedStack(ItemMeta m){super(Material.SHULKER_BOX);guarded=m;} @Override public ItemMeta getItemMeta(){return guarded;} @Override public boolean containsEnchantment(Enchantment e){return false;} }
    static class NoScanStack extends ItemStack {NoScanStack(){super(Material.SHULKER_BOX);} @Override public int getAmount(){throw new AssertionError("OFF must not read amount");} @Override public ItemMeta getItemMeta(){throw new AssertionError("OFF must not scan metadata");} }
    void command(F f,String... args){ExcelSilkWatch p=(ExcelSilkWatch)getServer().getPluginManager().getPlugin("ExcelSilkWatch");eq(p.getCommand("silkwarnings").execute(f.player,"silkwarnings",args),true);}
    void tests() {
        for(InventoryAction a:List.of(InventoryAction.PLACE_ALL,InventoryAction.PLACE_ONE,InventoryAction.PLACE_SOME,InventoryAction.SWAP_WITH_CURSOR)) check("cursor "+a,()->{F f=new F();var e=f.click(0,a,ClickType.LEFT,-1); f.observer.click(e);eq(f.warnings,0);eq(f.topItems[0],null);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,1);eq(e.isCancelled(),false);eq(f.cursor,silk());});
        check("shift deposit",()->{F f=new F();f.bottomItems[0]=silk();var e=f.click(27,InventoryAction.MOVE_TO_OTHER_INVENTORY,ClickType.SHIFT_LEFT,-1);f.observer.click(e);f.topItems[0]=f.bottomItems[0];f.bottomItems[0]=null;f.tick();eq(f.warnings,1);});
        for(InventoryAction a:List.of(InventoryAction.HOTBAR_SWAP,InventoryAction.HOTBAR_MOVE_AND_READD))check("hotbar "+a,()->{F f=new F();f.bottomItems[3]=silk();f.observer.click(f.click(0,a,ClickType.NUMBER_KEY,3));f.topItems[0]=f.bottomItems[3];f.bottomItems[3]=null;f.tick();eq(f.warnings,1);});
        check("offhand deposit",()->{F f=new F();f.bottomItems[40]=silk();f.observer.click(f.click(0,InventoryAction.HOTBAR_SWAP,ClickType.SWAP_OFFHAND,-1));f.topItems[0]=f.bottomItems[40];f.bottomItems[40]=null;f.tick();eq(f.warnings,1);});
        check("drag deposit",()->{F f=new F();var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor));f.observer.drag(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,1);eq(e.isCancelled(),false);});
        check("bottom-only drag ignored",()->{F f=new F();var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(27,f.cursor));f.observer.drag(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);});
        check("cancelled drag ignored",()->{F f=new F();var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor));e.setCancelled(true);f.observer.drag(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);eq(e.isCancelled(),true);});
        check("cancelled click ignored",()->{F f=new F();var e=f.place();e.setCancelled(true);f.observer.click(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);eq(e.isCancelled(),true);});
        check("late cancellation ignored",()->{F f=new F();var e=f.place();f.observer.click(e);e.setCancelled(true);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);});
        check("failed deposit no-op",()->{F f=new F();f.observer.click(f.place());f.tick();eq(f.warnings,0);});
        check("shift withdrawal",()->{F f=new F();f.topItems[0]=silk();f.observer.click(f.click(0,InventoryAction.MOVE_TO_OTHER_INVENTORY,ClickType.SHIFT_LEFT,-1));f.topItems[0]=null;f.tick();eq(f.warnings,0);});
        check("cursor withdrawal",()->{F f=new F();f.topItems[0]=silk();f.observer.click(f.click(0,InventoryAction.PICKUP_ALL,ClickType.LEFT,-1));f.topItems[0]=null;f.tick();eq(f.warnings,0);});
        check("ordinary chest ignored",()->{F f=new F();f.type=InventoryType.CHEST;f.deposit();eq(f.warnings,0);});
        check("non-silk ignored",()->{F f=new F();f.cursor=new ItemStack(Material.DIAMOND_PICKAXE);f.deposit();eq(f.warnings,0);});
        check("same-tick deposit withdrawal",()->{F f=new F();f.observer.click(f.place());f.topItems[0]=silk();f.observer.click(f.click(0,InventoryAction.PICKUP_ALL,ClickType.LEFT,-1));eq(f.warnings,1);f.topItems[0]=null;f.tick();eq(f.warnings,1);});
        check("cooldown coalesces and expires",()->{F f=new F();f.deposit();eq(f.warnings,1);f.topItems[0]=null;f.now=2999;f.deposit();eq(f.warnings,1);f.topItems[0]=null;f.now=3000;f.deposit();eq(f.warnings,2);});
        check("close flush",()->{F f=new F();f.observer.click(f.place());f.topItems[0]=silk();f.observer.close(new InventoryCloseEvent(f.view));eq(f.warnings,1);f.tick();eq(f.warnings,1);});
        check("offline suppresses",()->{F f=new F();f.observer.click(f.place());f.topItems[0]=silk();f.online=false;f.tick();eq(f.warnings,0);});
        check("disable clears",()->{F f=new F();f.observer.click(f.place());f.topItems[0]=silk();f.observer.clear();f.tick();eq(f.warnings,0);});
        check("identical stack relocation is not deposit",()->{F f=new F();f.topItems[1]=silk();f.observer.click(f.place());f.topItems[0]=f.topItems[1];f.topItems[1]=null;f.tick();eq(f.warnings,0);});
        check("different silk swap counts new identity",()->{F f=new F();f.topItems[0]=new ItemStack(Material.IRON_PICKAXE);f.topItems[0].addUnsafeEnchantment(Enchantment.SILK_TOUCH,1);f.observer.click(f.click(0,InventoryAction.SWAP_WITH_CURSOR,ClickType.LEFT,-1));f.topItems[0]=silk();f.tick();eq(f.warnings,1);});
        check("Silk Touch stored enchantment book",()->{F f=new F();f.cursor=new ItemStack(Material.ENCHANTED_BOOK);var meta=(org.bukkit.inventory.meta.EnchantmentStorageMeta)f.cursor.getItemMeta();meta.addStoredEnchant(Enchantment.SILK_TOUCH,1,true);f.cursor.setItemMeta(meta);f.deposit();eq(f.warnings,1);});
        check("non-Silk stored enchantment book ignored",()->{F f=new F();f.cursor=new ItemStack(Material.ENCHANTED_BOOK);var meta=(org.bukkit.inventory.meta.EnchantmentStorageMeta)f.cursor.getItemMeta();meta.addStoredEnchant(Enchantment.FORTUNE,3,true);f.cursor.setItemMeta(meta);f.deposit();eq(f.warnings,0);});
        check("foreign/custom ender inventory ignored",()->{F f=new F();f.ownEnderChest=false;f.deposit();eq(f.warnings,0);});
        check("outside click ignored",()->{F f=new F();f.observer.click(f.click(-999,InventoryAction.PLACE_ALL,ClickType.LEFT,-1));f.topItems[0]=silk();f.tick();eq(f.warnings,0);});
        check("bottom cursor placement ignored",()->{F f=new F();f.observer.click(f.click(27,InventoryAction.PLACE_ALL,ClickType.LEFT,-1));f.topItems[0]=silk();f.tick();eq(f.warnings,0);});
        check("invalid hotbar ignored",()->{F f=new F();f.observer.click(f.click(0,InventoryAction.HOTBAR_SWAP,ClickType.NUMBER_KEY,12));f.topItems[0]=silk();f.tick();eq(f.warnings,0);});
        check("late drag cancellation",()->{F f=new F();var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor));f.observer.drag(e);e.setCancelled(true);f.topItems[0]=silk();f.tick();eq(f.warnings,0);});
        check("stale scheduled callback does not flush newer event",()->{F f=new F();f.observer.click(f.place());Runnable stale=f.queued.removeFirst();f.topItems[0]=silk();f.observer.click(f.click(1,InventoryAction.PLACE_ALL,ClickType.LEFT,-1));eq(f.warnings,1);f.now=3000;stale.run();f.topItems[1]=silk();f.tick();eq(f.warnings,2);});
        check("creative inventory event ignored",()->{F f=new F();var e=new InventoryCreativeEvent(f.view,InventoryType.SlotType.CONTAINER,0,silk());f.observer.click(e);f.topItems[0]=silk();f.tick();eq(f.warnings,0);});
        check("full chest rejects deposit without warning",()->{F f=new F();Arrays.fill(f.topItems,new ItemStack(Material.STONE,64));f.observer.click(f.place());f.tick();eq(f.warnings,0);});
        check("hotbar withdrawal not deposit",()->{F f=new F();f.topItems[0]=silk();f.bottomItems[2]=new ItemStack(Material.STONE);f.observer.click(f.click(0,InventoryAction.HOTBAR_SWAP,ClickType.NUMBER_KEY,2));f.bottomItems[2]=f.topItems[0];f.topItems[0]=new ItemStack(Material.STONE);f.tick();eq(f.warnings,0);});
        check("double-click collect is withdrawal not deposit",()->{F f=new F();f.topItems[0]=silk();f.observer.click(f.click(0,InventoryAction.COLLECT_TO_CURSOR,ClickType.DOUBLE_CLICK,-1));f.topItems[0]=null;f.tick();eq(f.warnings,0);});
        check("same-tick late cancellation suppresses earlier transaction",()->{F f=new F();var e=f.place();f.observer.click(e);e.setCancelled(true);f.topItems[0]=silk();f.observer.click(f.click(0,InventoryAction.PICKUP_ALL,ClickType.LEFT,-1));f.tick();eq(f.warnings,0);});
        check("quit discards pending transaction",()->{F f=new F();f.observer.click(f.place());f.topItems[0]=silk();f.observer.quit(new PlayerQuitEvent(f.player,net.kyori.adventure.text.Component.empty()));f.tick();eq(f.warnings,0);});
        check("zero cooldown warns every successful transaction",()->{F f=new F();f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0);f.deposit();f.topItems[0]=null;f.deposit();eq(f.warnings,2);});
        check("cooldown independent per player",()->{F a=new F(),b=new F();List<UUID> ids=new ArrayList<>();var observer=new StorageObserver(a.queued::add,p->ids.add(p.getUniqueId()),()->a.now,3000);observer.click(a.place());a.topItems[0]=silk();a.tick();observer.click(b.place());b.topItems[0]=silk();a.tick();eq(ids,List.of(a.id,b.id));});
        check("shulker tool metadata scanner",()->{var s=wrap(silk(),1);eq(SilkContents.containsSilkTouch(s),true);});
        check("shulker stored book scanner",()->eq(SilkContents.containsSilkTouch(wrap(book(),1)),true));
        check("colored shulker scanner",()->eq(SilkContents.containsSilkTouch(box(Material.RED_SHULKER_BOX,silk())),true));
        check("empty shulker scanner",()->eq(SilkContents.containsSilkTouch(box(Material.SHULKER_BOX)),false));
        check("non-Silk shulker scanner",()->eq(SilkContents.containsSilkTouch(box(Material.BLUE_SHULKER_BOX,new ItemStack(Material.DIAMOND_PICKAXE))),false));
        check("nested boundary depth 4 positive",()->eq(SilkContents.containsSilkTouch(wrap(silk(),4)),true));
        check("nested beyond depth 4 negative",()->eq(SilkContents.containsSilkTouch(wrap(silk(),5)),false));
        check("node budget last permitted node positive",()->{ItemStack[] root=new ItemStack[10];ItemStack[] stones=new ItemStack[27];Arrays.fill(stones,new ItemStack(Material.STONE));for(int i=0;i<9;i++)root[i]=box(Material.SHULKER_BOX,stones);root[9]=box(Material.SHULKER_BOX,new ItemStack(Material.STONE),silk());eq(SilkContents.containsSilkTouch(box(Material.SHULKER_BOX,root)),true);});
        check("node budget beyond limit negative",()->{ItemStack[] root=new ItemStack[10];ItemStack[] stones=new ItemStack[27];Arrays.fill(stones,new ItemStack(Material.STONE));for(int i=0;i<9;i++)root[i]=box(Material.SHULKER_BOX,stones);root[9]=box(Material.SHULKER_BOX,new ItemStack(Material.STONE),new ItemStack(Material.STONE),silk());eq(SilkContents.containsSilkTouch(box(Material.SHULKER_BOX,root)),false);});
        check("malformed metadata safe false",()->eq(SilkContents.containsSilkTouch(new BrokenStack()),false));
        check("metadata interfaces read-only guard",()->{Inventory inv=proxy(Inventory.class,(p,m,a)->m.getName().equals("getContents")?new ItemStack[]{silk()}:base(p,m,a));ShulkerBox state=proxy(ShulkerBox.class,(p,m,a)->m.getName().equals("getInventory")?inv:base(p,m,a));BlockStateMeta meta=proxy(BlockStateMeta.class,(p,m,a)->m.getName().equals("getBlockState")?state:base(p,m,a));eq(SilkContents.containsSilkTouch(new GuardedStack(meta)),true);});
        check("shulker metadata byte-identical after scanner",()->{var s=box(Material.PURPLE_SHULKER_BOX,silk(),book());var before=s.serializeAsBytes();eq(SilkContents.containsSilkTouch(s),true);eq(Arrays.equals(before,s.serializeAsBytes()),true);});
        check("shulker normal successful deposit",()->{F f=new F();f.cursor=wrap(silk(),1);var before=f.cursor.serializeAsBytes();f.deposit();eq(f.warnings,1);eq(Arrays.equals(before,f.cursor.serializeAsBytes()),true);eq(Arrays.equals(before,f.topItems[0].serializeAsBytes()),true);});
        check("shulker shift successful deposit",()->{F f=new F();f.bottomItems[0]=wrap(book(),1);f.observer.click(f.click(27,InventoryAction.MOVE_TO_OTHER_INVENTORY,ClickType.SHIFT_LEFT,-1));f.topItems[0]=f.bottomItems[0];f.bottomItems[0]=null;f.tick();eq(f.warnings,1);});
        check("shulker drag successful deposit",()->{F f=new F();f.cursor=wrap(silk(),1);f.observer.drag(new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor)));f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,1);});
        check("shulker hotbar successful deposit",()->{F f=new F();f.bottomItems[3]=wrap(silk(),1);f.observer.click(f.click(0,InventoryAction.HOTBAR_SWAP,ClickType.NUMBER_KEY,3));f.topItems[0]=f.bottomItems[3];f.bottomItems[3]=null;f.tick();eq(f.warnings,1);});
        check("shulker offhand successful deposit",()->{F f=new F();f.bottomItems[40]=wrap(silk(),1);f.observer.click(f.click(0,InventoryAction.HOTBAR_SWAP,ClickType.SWAP_OFFHAND,-1));f.topItems[0]=f.bottomItems[40];f.bottomItems[40]=null;f.tick();eq(f.warnings,1);});
        check("shulker canceled deposit",()->{F f=new F();f.cursor=wrap(silk(),1);var e=f.place();e.setCancelled(true);f.observer.click(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);eq(e.isCancelled(),true);});
        check("shulker late-canceled deposit",()->{F f=new F();f.cursor=wrap(silk(),1);var e=f.place();f.observer.click(e);e.setCancelled(true);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);});
        check("shulker failed no-op deposit",()->{F f=new F();f.cursor=wrap(silk(),1);f.observer.click(f.place());f.tick();eq(f.warnings,0);});
        check("shulker full chest deposit",()->{F f=new F();f.cursor=wrap(silk(),1);Arrays.fill(f.topItems,new ItemStack(Material.STONE,64));f.observer.click(f.place());f.tick();eq(f.warnings,0);});
        check("shulker withdrawal no warning",()->{F f=new F();f.topItems[0]=wrap(silk(),1);f.observer.click(f.click(0,InventoryAction.MOVE_TO_OTHER_INVENTORY,ClickType.SHIFT_LEFT,-1));f.topItems[0]=null;f.tick();eq(f.warnings,0);});
        check("shulker cooldown spam coalesced",()->{F f=new F();f.cursor=wrap(silk(),1);for(int i=0;i<10;i++){f.topItems[0]=null;f.deposit();}eq(f.warnings,1);f.now=3000;f.topItems[0]=null;f.deposit();eq(f.warnings,2);});
        check("empty shulker deposit no warning",()->{F f=new F();f.cursor=box(Material.SHULKER_BOX);f.deposit();eq(f.warnings,0);});
        check("non-Silk shulker deposit no warning",()->{F f=new F();f.cursor=box(Material.SHULKER_BOX,new ItemStack(Material.STONE));f.deposit();eq(f.warnings,0);});
        check("shulker same-tick deposit withdrawal",()->{F f=new F();f.cursor=wrap(silk(),1);f.observer.click(f.place());f.topItems[0]=f.cursor.clone();f.observer.click(f.click(0,InventoryAction.PICKUP_ALL,ClickType.LEFT,-1));f.topItems[0]=null;f.tick();eq(f.warnings,1);});
        check("shulker canceled drag",()->{F f=new F();f.cursor=wrap(silk(),1);var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor));e.setCancelled(true);f.observer.drag(e);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);});
        check("shulker late-canceled drag",()->{F f=new F();f.cursor=wrap(silk(),1);var e=new InventoryDragEvent(f.view,null,f.cursor,false,Map.of(0,f.cursor));f.observer.drag(e);e.setCancelled(true);f.topItems[0]=f.cursor.clone();f.tick();eq(f.warnings,0);});
        check("shulker ordinary chest excluded",()->{F f=new F();f.type=InventoryType.CHEST;f.cursor=wrap(silk(),1);f.deposit();eq(f.warnings,0);});
        check("shulker foreign ender inventory excluded",()->{F f=new F();f.ownEnderChest=false;f.cursor=wrap(silk(),1);f.deposit();eq(f.warnings,0);});
        check("malformed block state safe false",()->{BlockStateMeta m=proxy(BlockStateMeta.class,(p,method,a)->{if(method.getName().equals("getBlockState"))throw new IllegalArgumentException("bad state fixture");return base(p,method,a);});eq(SilkContents.containsSilkTouch(new GuardedStack(m)),false);});
        check("scanner null and air safe false",()->{eq(SilkContents.containsSilkTouch(null),false);eq(SilkContents.containsSilkTouch(new ItemStack(Material.AIR)),false);});
        check("OFF suppresses direct deposit",()->{F f=new F();f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0,id->false);f.deposit();eq(f.warnings,0);eq(f.queued.size(),0);});
        check("OFF suppresses shulker deposit",()->{F f=new F();f.cursor=wrap(silk(),1);f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0,id->false);f.deposit();eq(f.warnings,0);});
        check("OFF skips candidate scanning",()->{F f=new F();f.cursor=new NoScanStack();f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0,id->false);f.observer.click(f.place());eq(f.queued.size(),0);});
        check("pending ON to OFF suppresses warning",()->{F f=new F();boolean[] enabled={true};f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0,id->enabled[0]);f.observer.click(f.place());f.topItems[0]=silk();enabled[0]=false;f.tick();eq(f.warnings,0);});
        check("OFF to ON fresh deposit warns",()->{F f=new F();boolean[] enabled={false};f.observer=new StorageObserver(f.queued::add,p->f.warnings++,()->f.now,0,id->enabled[0]);f.deposit();eq(f.warnings,0);enabled[0]=true;f.topItems[0]=null;f.deposit();eq(f.warnings,1);});
        check("toggle predicate per-player independent",()->{F a=new F(),b=new F();List<UUID> warned=new ArrayList<>();var o=new StorageObserver(a.queued::add,p->warned.add(p.getUniqueId()),()->0,0,id->!id.equals(a.id));o.click(a.place());a.topItems[0]=silk();o.click(b.place());b.topItems[0]=silk();a.tick();eq(warned,List.of(b.id));});
        commandTests();
        check("observer never modifies source stack or contents",()->{F f=new F();f.cursor.setAmount(2);ItemStack original=f.cursor.clone();f.topItems[0]=silk();ItemStack before=f.topItems[0].clone();var e=f.place();f.observer.click(e);f.tick();eq(f.cursor,original);eq(f.topItems[0],before);eq(e.isCancelled(),false);eq(f.warnings,0);});
    }
    void commandTests() {
        ExcelSilkWatch plugin=(ExcelSilkWatch)getServer().getPluginManager().getPlugin("ExcelSilkWatch");
        check("Excel branding plugin identity author main version",()->{eq(plugin.getName(),"ExcelSilkWatch");eq(plugin.getClass().getName(),"com.winkreyes.silkstorage.ExcelSilkWatch");eq(plugin.getPluginMeta().getVersion(),"0.1.3-offline");eq(plugin.getPluginMeta().getAuthors(),List.of("WinkReyesValley"));eq(getServer().getPluginManager().getPlugin("SilkStorageWarning"),null);});
        check("Excel branding isolated data folder",()->{eq(plugin.getDataFolder().getName(),"ExcelSilkWatch");eq(Files.exists(plugin.getDataFolder().toPath().resolveSibling("SilkStorageWarning")),false);});
        check("Excel branding friendly and namespaced command owner",()->{var friendly=getServer().getPluginCommand("silkwarnings");var namespaced=getServer().getPluginCommand("excelsilkwatch:silkwarnings");eq(friendly.getPlugin(),plugin);eq(namespaced.getPlugin(),plugin);eq(namespaced.getPermission(),"excelsilkwatch.toggle");eq(getServer().getPluginCommand("silkstoragewarning:silkwarnings"),null);});
        check("Excel namespaced menu dispatch updates same store",()->{F f=new F();eq(getServer().getPluginCommand("excelsilkwatch:silkwarnings").execute(f.player,"excelsilkwatch:silkwarnings",new String[]{"off"}),true);eq(plugin.warningStatus(f.id),"OFF");if(!f.permissionsChecked.contains("excelsilkwatch.toggle"))throw new AssertionError(f.permissionsChecked);});
        Path file=plugin.getDataFolder().toPath().resolve("players.yml");
        check("actual plugin restart persistence sentinel",()->{F f=new F();f.id=new UUID(0xa55c257a1d1541dfL, 0xa6fa77c5ff982cb2L);Path marker=Path.of("persistence-sentinel-created.txt");try{if(Files.exists(marker)){eq(plugin.warningStatus(f.id),"OFF");results.add("EVIDENCE persisted OFF loaded on subsequent server boot");}else{eq(plugin.warningStatus(f.id),"ON");command(f,"off");Files.writeString(marker,"created by harness");results.add("EVIDENCE first boot seeded persistent OFF");}}catch(Exception e){throw new RuntimeException(e);}});
        check("loaded plugin default ON",()->{eq(plugin.isEnabled(),true);eq(plugin.warningStatus(UUID.randomUUID()),"ON");});
        check("actual command off persists and status accurate",()->{F f=new F();command(f,"off");eq(plugin.warningStatus(f.id),"OFF");if(!f.messages.getLast().contains("OFF"))throw new AssertionError(f.messages);try{eq(new PreferenceStore(file).enabled(f.id),false);}catch(Exception e){throw new RuntimeException(e);}});
        check("actual command on persists and status accurate",()->{F f=new F();command(f,"off");command(f,"on");eq(plugin.warningStatus(f.id),"ON");try{eq(new PreferenceStore(file).enabled(f.id),true);}catch(Exception e){throw new RuntimeException(e);}});
        check("default command toggles and explicit toggle restores",()->{F f=new F();command(f);eq(plugin.warningStatus(f.id),"OFF");command(f,"toggle");eq(plugin.warningStatus(f.id),"ON");});
        check("status command non-mutating",()->{F f=new F();try{byte[] before=Files.readAllBytes(file);command(f,"status");eq(plugin.warningStatus(f.id),"ON");eq(Arrays.equals(before,Files.readAllBytes(file)),true);}catch(Exception e){throw new RuntimeException(e);}});
        check("invalid command non-mutating",()->{F f=new F();command(f,"invalid");eq(plugin.warningStatus(f.id),"ON");if(!f.messages.getLast().contains("Use /silkwarnings"))throw new AssertionError(f.messages);});
        check("command self-only rejects target argument",()->{F f=new F(),other=new F();command(f,"off",other.id.toString());eq(plugin.warningStatus(f.id),"ON");eq(plugin.warningStatus(other.id),"ON");});
        check("command permission denied",()->{F f=new F();f.permission=false;command(f,"off");eq(plugin.warningStatus(f.id),"ON");});
        check("command console rejected",()->{List<String> messages=new ArrayList<>();CommandSender console=proxy(CommandSender.class,(p,m,a)->switch(m.getName()){case "hasPermission" -> true;case "sendMessage" -> {messages.add(Arrays.toString(a));yield null;}default -> base(p,m,a);});eq(plugin.onCommand(console,plugin.getCommand("silkwarnings"),"silkwarnings",new String[]{"off"}),true);if(!messages.getLast().contains("player-only"))throw new AssertionError(messages);});
        check("tab completion normal filtered permission no-extra-target",()->{F f=new F();var cmd=plugin.getCommand("silkwarnings");eq(plugin.onTabComplete(f.player,cmd,"silkwarnings",new String[]{""}),List.of("on","off","toggle","status"));eq(plugin.onTabComplete(f.player,cmd,"silkwarnings",new String[]{"O"}),List.of("on","off"));eq(plugin.onTabComplete(f.player,cmd,"silkwarnings",new String[]{"off",""}),List.of());f.permission=false;eq(plugin.onTabComplete(f.player,cmd,"silkwarnings",new String[]{""}),List.of());});
        check("command independent players",()->{F a=new F(),b=new F();command(a,"off");eq(plugin.warningStatus(a.id),"OFF");eq(plugin.warningStatus(b.id),"ON");});
        check("command disk failure preserves state and honest reply",()->{F f=new F();command(f,"on");Path backup=file.resolveSibling("test-players-backup.yml");try{Files.move(file,backup);Files.createDirectory(file);try{command(f,"off");eq(plugin.warningStatus(f.id),"ON");if(!f.messages.getLast().contains("Could not save your setting"))throw new AssertionError(f.messages);}finally{Files.delete(file);Files.move(backup,file);}}catch(Exception e){throw new RuntimeException(e);}});
        if(System.getProperty("probe.expectPapi","0").equals("1")) check("required real PAPI loaded",()->eq(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"),true));
        if(getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            check("Excel PAPI author identifier and version",()->{var expansion=me.clip.placeholderapi.PlaceholderAPIPlugin.getInstance().getLocalExpansionManager().getExpansion("silkwarnings");eq(expansion.getAuthor(),"WinkReyesValley");eq(expansion.getIdentifier(),"silkwarnings");eq(expansion.getVersion(),"0.1.3-offline");eq(expansion.persist(),true);});
            check("real PAPI hook shared ON OFF state",()->{F f=new F();eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_status%"),"ON");command(f,"off");eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_status%"),"OFF");command(f,"on");eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_status%"),"ON");});
            check("real PAPI unknown token untouched",()->{F f=new F();eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_unknown%"),"%silkwarnings_unknown%");});
            check("plugin disable unregisters real PAPI hook",()->{F f=new F();eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_status%"),"ON");getServer().getPluginManager().disablePlugin(plugin);eq(plugin.warningStatus(f.id),"UNAVAILABLE");eq(PlaceholderAPI.setPlaceholders(f.player,"%silkwarnings_status%"),"%silkwarnings_status%");});
        } else check("no-PAPI optional plugin enabled",()->eq(plugin.isEnabled(),true));
    }

}
