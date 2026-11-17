import java.lang.reflect.*;
import java.util.*;
import java.util.function.LongSupplier;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.cloudburstmc.protocol.bedrock.packet.MobEquipmentPacket;
public class DiagnosticProbe extends JavaPlugin {
 int n;void ok(boolean b,String s){if(!b)throw new AssertionError(s);n++;getLogger().info("PASS "+s);}
 static Object invoke(Object o,String name,Class<?>[] sig,Object...args)throws Exception{Method m=o.getClass().getDeclaredMethod(name,sig);m.setAccessible(true);return m.invoke(o,args);}
 @SuppressWarnings({"rawtypes","unchecked"}) public void onEnable(){Bukkit.getScheduler().runTaskLater(this,()->{try{
  JavaPlugin plugin=(JavaPlugin)Bukkit.getPluginManager().getPlugin("WRVWandDiagnostic");ok(plugin!=null&&plugin.isEnabled(),"plugin enabled");ok(plugin.getConfig().getString("owner-uuid","").isEmpty(),"owner blank");Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"wanddiag start");Bukkit.dispatchCommand(Bukkit.getConsoleSender(),"wanddiag show");
  ClassLoader cl=plugin.getClass().getClassLoader();Class<?> wc=Class.forName("com.winkreyes.wanddiagnostic.Window",true,cl),oc=Class.forName("com.winkreyes.wanddiagnostic.Observe",true,cl);
  Constructor<?> c=wc.getDeclaredConstructor(LongSupplier.class);c.setAccessible(true);Object w=c.newInstance((LongSupplier)()->0L);Constructor<?> d=oc.getDeclaredConstructor(wc);d.setAccessible(true);Object obs=d.newInstance(w);
  Object original=Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class);ok(original!=null,"actual Geyser translator exists");invoke(obs,"install",new Class<?>[]{});Object wrapped=Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class);ok(wrapped!=original,"actual registry wrapped");invoke(obs,"restore",new Class<?>[]{});ok(Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class)==original,"original restored by identity");
  Object obs2=d.newInstance(w);invoke(obs2,"install",new Class<?>[]{});PacketTranslator prior=Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class);PacketTranslator replacement=new PacketTranslator(){public void translate(GeyserSession s,Object p){prior.translate(s,p);}};
  ((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(MobEquipmentPacket.class,replacement);invoke(obs2,"restore",new Class<?>[]{});ok(Registries.BEDROCK_PACKET_TRANSLATORS.get(MobEquipmentPacket.class)==replacement,"later wrapper not overwritten");Field retired=oc.getDeclaredField("retired");retired.setAccessible(true);ok(retired.getBoolean(obs2),"chained observer retired");((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(MobEquipmentPacket.class,original);
  getLogger().info("DIAGNOSTIC_PROBE_PASS "+n);
 }catch(Throwable e){getLogger().severe("DIAGNOSTIC_PROBE_FAIL "+e);e.printStackTrace();}finally{Bukkit.getScheduler().runTaskLater(this,()->Bukkit.shutdown(),5L);}},100L);}
}
