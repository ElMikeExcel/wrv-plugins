package verification;
import java.lang.reflect.Proxy;
import java.util.*;
import net.milkbowl.vault.economy.*;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
public final class FixtureVault extends JavaPlugin {
 public static double balance=1000000d,withdrawn,refunded;public static int withdrawals,deposits,economyCalls;public static boolean failWithdrawal,failRefund;
 public static void reset(){balance=1000000d;withdrawn=refunded=0;withdrawals=deposits=economyCalls=0;failWithdrawal=failRefund=false;}
 public void onEnable(){if(System.getProperty("mobveil.probe.mode").equals("no-economy"))return;
 Economy e=(Economy)Proxy.newProxyInstance(Economy.class.getClassLoader(),new Class[]{Economy.class},(p,m,a)->{economyCalls++;return switch(m.getName()){
 case "getName"->"SyntheticNoRealMoney";case "isEnabled","hasAccount"->true;case "has"->balance>=((Number)a[a.length-1]).doubleValue();case "format"->String.format(Locale.US,"$%,.2f",a[0]);case "getBalance"->balance;
 case "withdrawPlayer"->{double n=((Number)a[a.length-1]).doubleValue();withdrawals++;if(failWithdrawal)yield new EconomyResponse(0,balance,EconomyResponse.ResponseType.FAILURE,"Synthetic withdrawal failure");balance-=n;withdrawn+=n;yield new EconomyResponse(n,balance,EconomyResponse.ResponseType.SUCCESS,"");}
 case "depositPlayer"->{double n=((Number)a[a.length-1]).doubleValue();deposits++;if(failRefund)yield new EconomyResponse(0,balance,EconomyResponse.ResponseType.FAILURE,"Synthetic refund failure");balance+=n;refunded+=n;yield new EconomyResponse(n,balance,EconomyResponse.ResponseType.SUCCESS,"");}
 case "toString"->"SyntheticNoRealMoney";case "hashCode"->System.identityHashCode(p);case "equals"->p==a[0];default->defaultValue(m.getReturnType());};});
 getServer().getServicesManager().register(Economy.class,e,this,ServicePriority.Normal);}
 private static Object defaultValue(Class<?>t){if(t==boolean.class)return false;if(t==int.class)return 0;if(t==double.class)return 0d;if(t==long.class)return 0L;return null;}
}
