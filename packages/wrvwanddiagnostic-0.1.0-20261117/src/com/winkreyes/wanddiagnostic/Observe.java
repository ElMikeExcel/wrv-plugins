package com.winkreyes.wanddiagnostic;
import java.util.*;
import org.cloudburstmc.protocol.bedrock.packet.*;
import org.cloudburstmc.protocol.bedrock.data.PlayerAuthInputData;
import org.geysermc.geyser.registry.Registries;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
final class Observe {
 final Window window;
 private volatile boolean retired;
 final Map<Class<? extends BedrockPacket>,Wrapper<?>> installed=new LinkedHashMap<>();
 private int lastMask=Integer.MIN_VALUE,lastMode=-1,lastModel=-1;
 Observe(Window w){window=w;}
 @SuppressWarnings({"rawtypes","unchecked"}) void install(){
  for(Class c:List.of(MobEquipmentPacket.class,PlayerAuthInputPacket.class,InventoryTransactionPacket.class,AnimatePacket.class,PlayerActionPacket.class,ItemStackRequestPacket.class,PlayerHotbarPacket.class,InteractPacket.class)){
   PacketTranslator original=Registries.BEDROCK_PACKET_TRANSLATORS.get(c);
   // Never invent handling for an unhandled packet, including PlayerHotbar.
   if(original==null)continue;
   Wrapper next=new Wrapper(original,this);
   ((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(c,next);installed.put(c,next);
  }
 }
 @SuppressWarnings({"rawtypes","unchecked"}) void restore(){retired=true;for(var e:installed.entrySet())if(Registries.BEDROCK_PACKET_TRANSLATORS.get(e.getKey())==e.getValue())((org.geysermc.geyser.registry.AbstractMappedRegistry)Registries.BEDROCK_PACKET_TRANSLATORS).register(e.getKey(),e.getValue().original);installed.clear();}
 List<String> ownership(){List<String> out=new ArrayList<>();for(var e:installed.entrySet())out.add(e.getKey().getSimpleName()+": observer-owned="+(Registries.BEDROCK_PACKET_TRANSLATORS.get(e.getKey())==e.getValue())+", prior-wandbars-wrapper="+e.getValue().original.getClass().getName().startsWith("com.winkreyes.excelwandbars.Geyser"));return out;}
 void capture(GeyserSession session,Object packet){
  if(retired||!window.running())return;
  UUID id=session.javaUuid();if(!window.active(id))return;
  int slot=session.getPlayerInventory().getHeldItemSlot();
  if(packet instanceof MobEquipmentPacket p)window.note(id,Window.Kind.EQUIP,p.getHotbarSlot(),p.getContainerId(),null,null,true);
  else if(packet instanceof InventoryTransactionPacket p)window.note(id,Window.Kind.USE,p.getActionType(),p.getHotbarSlot(),p.getTransactionType(),p.getTriggerType(),true);
  else if(packet instanceof InteractPacket p)window.note(id,Window.Kind.INTERACT,slot,session.isSneaking()?1:0,p.getAction(),null,true);
  else if(packet instanceof AnimatePacket p)window.note(id,Window.Kind.ANIMATE,slot,session.isSneaking()?1:0,p.getAction(),p.getSwingSource(),true);
  else if(packet instanceof PlayerActionPacket p)window.note(id,Window.Kind.ACTION,slot,session.isSneaking()?1:0,p.getAction(),null,true);
  else if(packet instanceof ItemStackRequestPacket p)window.note(id,Window.Kind.STACK,p.getRequests()==null?0:p.getRequests().size(),slot,null,null,true);
  else if(packet instanceof PlayerHotbarPacket p)window.note(id,Window.Kind.HOTBAR,p.getSelectedHotbarSlot(),p.getContainerId(),null,null,true);
  else if(packet instanceof PlayerAuthInputPacket p){
   int mask=0,i=0;for(PlayerAuthInputData f:List.of(PlayerAuthInputData.SNEAKING,PlayerAuthInputData.SNEAK_DOWN,PlayerAuthInputData.START_SNEAKING,PlayerAuthInputData.STOP_SNEAKING,PlayerAuthInputData.MISSED_SWING,PlayerAuthInputData.START_USING_ITEM,PlayerAuthInputData.PERFORM_ITEM_INTERACTION,PlayerAuthInputData.PERFORM_ITEM_STACK_REQUEST,PlayerAuthInputData.PERFORM_BLOCK_ACTIONS,PlayerAuthInputData.HOTBAR_ONLY_TOUCH)){if(p.getInputData().contains(f))mask|=1<<i;i++;}
   int mode=p.getInputMode()==null?-1:p.getInputMode().ordinal(),model=p.getInputInteractionModel()==null?-1:p.getInputInteractionModel().ordinal();
   boolean detail=mask!=lastMask||mode!=lastMode||model!=lastModel;
   window.note(id,Window.Kind.AUTH,mask,slot,p.getInputMode(),p.getInputInteractionModel(),detail);lastMask=mask;lastMode=mode;lastModel=model;
   if(detail)window.note(id,Window.Kind.SESSION,session.getUpstream().getProtocolVersion(),session.isSneaking()?1:0,null,null,true);
   var t=p.getItemUseTransaction();if(t!=null)window.note(id,Window.Kind.USE,t.getActionType(),t.getHotbarSlot(),t.getTriggerType(),null,true);
   if(p.getPlayerActions()!=null)for(var a:p.getPlayerActions())window.note(id,Window.Kind.ACTION,slot,0,a.getAction(),null,true);
   if(p.getItemStackRequest()!=null)window.note(id,Window.Kind.STACK,p.getItemStackRequest().getActions().length,slot,null,null,true);
  }
 }
 static final class Wrapper<T> extends PacketTranslator<T>{
  final PacketTranslator<T> original;final Observe observer;
  Wrapper(PacketTranslator<T> o,Observe d){original=o;observer=d;}
  public void translate(GeyserSession s,T p){try{observer.capture(s,p);}catch(Throwable ignored){/* no packet text or exception details */}original.translate(s,p);}
  public boolean shouldExecuteInEventLoop(){return original.shouldExecuteInEventLoop();}
 }
}
