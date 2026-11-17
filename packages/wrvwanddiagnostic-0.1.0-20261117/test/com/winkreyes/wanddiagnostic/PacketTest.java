package com.winkreyes.wanddiagnostic;
import java.lang.reflect.Field;
import java.util.*;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.entity.type.player.SessionPlayerEntity;
import org.geysermc.geyser.inventory.PlayerInventory;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
import org.cloudburstmc.protocol.bedrock.packet.*;
public class PacketTest {
 static sun.misc.Unsafe unsafe; static int n;
 static void check(boolean b,String m){n++;if(!b)throw new AssertionError(m);}
 static void set(Object o,String name,Object v)throws Exception{Class<?> c=o.getClass();while(c!=null){try{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,v);return;}catch(NoSuchFieldException e){c=c.getSuperclass();}}throw new NoSuchFieldException(name);}
 public static void main(String[] args)throws Exception{
  Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(sun.misc.Unsafe)f.get(null);
  GeyserSession s=(GeyserSession)unsafe.allocateInstance(GeyserSession.class);SessionPlayerEntity e=(SessionPlayerEntity)unsafe.allocateInstance(SessionPlayerEntity.class);PlayerInventory inv=(PlayerInventory)unsafe.allocateInstance(PlayerInventory.class);UUID id=UUID.randomUUID();set(e,"uuid",id);set(s,"playerEntity",e);set(s,"playerInventoryHolder",new org.geysermc.geyser.inventory.InventoryHolder(s,inv,null));set(inv,"heldItemSlot",8);
  Window w=new Window(()->0L);Observe o=new Observe(w);int[] forwarded={0};
  MobEquipmentPacket packet=new MobEquipmentPacket();packet.setHotbarSlot(8);packet.setContainerId(0);
  PacketTranslator<MobEquipmentPacket> original=new PacketTranslator<>(){public void translate(GeyserSession session,MobEquipmentPacket p){check(session==s&&p==packet,"identity preserved");check(p.getHotbarSlot()==8&&p.getContainerId()==0,"packet fields unchanged");forwarded[0]++;}public boolean shouldExecuteInEventLoop(){return true;}};
  Observe.Wrapper<MobEquipmentPacket> wrapper=new Observe.Wrapper<>(original,o);
  wrapper.translate(s,packet);check(w.snapshot().get(1).equals("counts={}"),"off real packet not recorded");
  w.start(UUID.randomUUID());wrapper.translate(s,packet);check(w.snapshot().get(1).equals("counts={}"),"other session real packet excluded");
  w.start(id);wrapper.translate(s,packet);check(w.snapshot().get(1).contains("EQUIP=1"),"owner equipment recorded");check(forwarded[0]==3,"once each off other and owner");check(wrapper.shouldExecuteInEventLoop(),"true event loop preserved");
  InteractPacket ip=new InteractPacket();ip.setAction(InteractPacket.Action.OPEN_INVENTORY);ip.setRuntimeEntityId(123456789L);o.capture(s,ip);String data=String.join("\n",w.snapshot());check(data.contains("OPEN_INVENTORY"),"inventory action captured");check(!data.contains("123456789"),"entity id omitted");check(!data.contains(id.toString()),"identity omitted");
  w.stop(Window.End.LOGOUT);String saved=String.join("\n",w.snapshot());o.capture(s,packet);check(saved.equals(String.join("\n",w.snapshot())),"logout snapshot frozen");
  o.restore();w.start(id);wrapper.translate(s,packet);check(w.snapshot().get(1).equals("counts={}"),"retired chained observer stays disabled after new window");check(forwarded[0]==4,"retired observer still passes original once");
  System.out.println(n+" real packet assertions passed (synthetic session, not device)");
 }
}
