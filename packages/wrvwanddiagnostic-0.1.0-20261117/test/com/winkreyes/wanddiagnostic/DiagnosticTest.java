package com.winkreyes.wanddiagnostic;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.PacketTranslator;
public class DiagnosticTest {
 static int n;
 static void check(boolean ok,String text){n++;if(!ok)throw new AssertionError(text);}
 public static void main(String[] args){
  AtomicLong t=new AtomicLong();Window w=new Window(t::get);UUID owner=UUID.randomUUID(),other=UUID.randomUUID();
  check(!w.running(),"off default");w.note(owner,Window.Kind.EQUIP,8,0,null,null,true);check(w.snapshot().get(1).equals("counts={}"),"off captures nothing");
  w.start(owner);w.note(other,Window.Kind.EQUIP,8,0,null,null,true);check(w.snapshot().get(1).equals("counts={}"),"other excluded");
  for(int i=0;i<1000;i++)w.note(owner,Window.Kind.AUTH,i,8,null,null,true);
  check(w.snapshot().size()==66,"64 events bounded");check(w.snapshot().get(1).contains("AUTH=1000"),"aggregate preserved");check(!String.join("",w.snapshot()).contains(owner.toString()),"owner identifier absent");
  t.set(59999);check(w.active(owner),"active before deadline");t.set(60000);check(!w.active(owner),"expires exactly at limit");w.note(owner,Window.Kind.EQUIP,8,0,null,null,true);check(!w.snapshot().get(1).contains("EQUIP"),"no capture after expiry");check(w.snapshot().get(0).contains("EXPIRED"),"expiry snapshot retained");
  w.start(owner);w.note(owner,Window.Kind.SWING,1,0,null,null,true);w.logout(other);check(w.running(),"other logout ignored");w.logout(owner);check(!w.running(),"owner logout stops");check(w.snapshot().get(1).contains("SWING=1"),"logout retains summary");check(w.snapshot().get(0).contains("LOGOUT"),"logout reason");
  w.start(owner);check(w.snapshot().get(1).equals("counts={}"),"restart clears old data");w.stop(Window.End.MANUAL);check(!w.running(),"manual stop");
  // A null session deliberately makes observer extraction throw. Original must still
  // receive the identical packet once, and event-loop choice must remain identical.
  w.start(owner);
  final Object packet=new Object();final int[] calls={0};
  PacketTranslator<Object> original=new PacketTranslator<>(){public void translate(GeyserSession s,Object p){check(p==packet,"same packet identity");calls[0]++;}public boolean shouldExecuteInEventLoop(){return false;}};
  Observe.Wrapper<Object> wrapper=new Observe.Wrapper<>(original,new Observe(w));wrapper.translate(null,packet);check(calls[0]==1,"observer failure forwards once");check(!wrapper.shouldExecuteInEventLoop(),"event loop delegation");
  PacketTranslator<Object> failing=new PacketTranslator<>(){public void translate(GeyserSession s,Object p){calls[0]++;throw new IllegalStateException("original");}};
  boolean threw=false;try{new Observe.Wrapper<>(failing,new Observe(w)).translate(null,packet);}catch(IllegalStateException e){threw=true;}check(threw,"original exception preserved");check(calls[0]==2,"original not retried");
  System.out.println(n+" diagnostic assertions passed");
 }
}
