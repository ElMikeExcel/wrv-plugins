package rig135;
import java.lang.reflect.*;import java.util.*;import org.bukkit.*;import org.bukkit.enchantments.Enchantment;import org.bukkit.inventory.ItemStack;import org.bukkit.inventory.meta.EnchantmentStorageMeta;import org.bukkit.plugin.Plugin;import org.bukkit.plugin.java.JavaPlugin;
public final class Rig135 extends JavaPlugin {
  @Override public void onLoad(){
    java.util.Map<String,Double> bal=new java.util.concurrent.ConcurrentHashMap<>();
    Object eco=java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{net.milkbowl.vault.economy.Economy.class},(px,m,a)->{
      switch(m.getName()){
        case "isEnabled": case "hasAccount": case "has": return m.getReturnType()==boolean.class?true:null;
        case "getName": return "RigEconomy";
        case "fractionalDigits": return 2;
        case "getBalance": return 5000.0;
        case "format": return String.valueOf(a[0]);
        case "currencyNamePlural": case "currencyNameSingular": return "$";
        default: if(m.getReturnType()==boolean.class)return false; if(m.getReturnType()==double.class)return 0.0; if(m.getReturnType()==int.class)return 0; return null;
      }});
    getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class,(net.milkbowl.vault.economy.Economy)eco,this,org.bukkit.plugin.ServicePriority.Highest);
  }
  @Override public void onEnable(){getServer().getScheduler().runTaskLater(this,this::go,60L);}
  private void go(){try{
    Plugin es=getServer().getPluginManager().getPlugin("ExcelShops");
    Object log=es.getClass().getMethod("getPurchaseLog").invoke(es);
    if(log==null){getLogger().severe("RIG135 FAIL purchaseLog null");return;}
    Method itemId=es.getClass().getDeclaredMethod("itemId",ItemStack.class);itemId.setAccessible(true);
    ItemStack book=new ItemStack(Material.ENCHANTED_BOOK);EnchantmentStorageMeta m=(EnchantmentStorageMeta)book.getItemMeta();m.addStoredEnchant(Enchantment.MENDING,1,true);book.setItemMeta(m);
    String id=(String)itemId.invoke(null,book);getLogger().info("RIG135 itemId mending="+id+" stone="+itemId.invoke(null,new ItemStack(Material.STONE)));
    Class<?> pc=Class.forName("com.winkreyes.shops.log.PurchaseLog$Purchase",true,es.getClass().getClassLoader());
    Constructor<?> c=pc.getConstructors()[0];
    Object p=c.newInstance(java.time.Instant.now(),"RigPlayer",UUID.randomUUID(),"java","spawn",id,1,1,200000L,200000L,Long.valueOf(500000L),"spawn:mending");
    Object f=log.getClass().getMethod("record",pc).invoke(log,p);((java.util.concurrent.CompletableFuture<?>)f).get();
    getLogger().info("RIG135 recorded");
    getServer().dispatchCommand(getServer().getConsoleSender(),"excelshop stats 7");
    getServer().getScheduler().runTaskLater(this,()->{getLogger().info("RIG135 DONE");},40L);
  }catch(Throwable t){getLogger().severe("RIG135 FAIL "+t);t.printStackTrace();}}
}
