package wrv.harness;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.bukkit.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/** No ChestShop classes: prove legacy recovery closes native checkout when ChestShop is absent. */
public final class LegacyRecoveryProbe extends JavaPlugin {
    private int assertions,failed;
    private final List<String> results=new ArrayList<>();
    @Override public void onEnable(){Bukkit.getScheduler().runTaskLater(this,this::test,40);}
    private void test(){
        try{
            Plugin plugin=Bukkit.getPluginManager().getPlugin("ExcelShops");
            check(plugin!=null&&plugin.isEnabled(),"ExcelShops remains enabled");
            check(Bukkit.getPluginManager().getPlugin("ChestShop")==null,"ChestShop entirely absent");
            Object checkout=plugin.getClass().getMethod("getCheckout").invoke(plugin);
            check(checkout!=null,"native checkout loaded in recovery mode");
            check((boolean)checkout.getClass().getMethod("recoveryRequired").invoke(checkout),"dirty legacy evidence globally pauses native checkout");
            check(checkout.getClass().getMethod("tryExternalLease").invoke(checkout)==null,"paused checkout refuses external admission");
            boolean denied=false;
            try{checkout.getClass().getMethod("quote",UUID.class,UUID.class,String.class,long.class,int.class,long.class,String.class).invoke(checkout,UUID.fromString("d0523236-0000-4000-8000-000000000002"),null,"spawn:bread",0L,1,4000L,"probe");}
            catch(InvocationTargetException e){denied=e.getCause() instanceof IllegalStateException;}
            check(denied,"native quote API refuses purchase while legacy recovery is pending");
            FakePlayer buyer=new FakePlayer(UUID.fromString("d0523236-0000-4000-8000-000000000002"),"LegacyBuyer",Bukkit.getWorlds().getFirst());
            check(Bukkit.getPluginCommand("excelshop").execute(buyer.player,"excelshop",new String[]{"browse","bread"}),"native browse command remains registered");
            check(buyer.messages.stream().anyMatch(m->m.contains("paused")),"native command truthfully reports paused recovery");
            check(buyer.storage.isEmpty(),"native paused command gives no items");
            check(Files.exists(plugin.getDataFolder().toPath().resolve("legacy-checkout/active-session")),"legacy interrupted marker preserved before shutdown");
        }catch(Throwable e){failed++;results.add("FAIL "+e);e.printStackTrace();}
        try{Files.createDirectories(getDataFolder().toPath());Files.writeString(getDataFolder().toPath().resolve("missing-chestshop-recovery-results.txt"),String.join("\n",results)+"\nSUMMARY passed="+(failed==0?1:0)+" assertions="+assertions+" failed="+failed+"\n");}catch(Exception e){failed++;e.printStackTrace();}
        getLogger().info("LEGACY_FULL_SUMMARY passed="+(failed==0?1:0)+" assertions="+assertions+" failed="+failed);Bukkit.getScheduler().runTaskLater(this,Bukkit::shutdown,10);
    }
    private void check(boolean ok,String message){assertions++;if(!ok)throw new AssertionError(message);results.add("PASS "+message);}
}
