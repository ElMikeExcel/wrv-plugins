package com.winkreyes.wanddiagnostic;
import java.util.*;
import java.util.function.LongSupplier;
/** Owner filter never appears in rendered output; fixed enums and numbers only. */
final class Window {
 enum Kind { SESSION, AUTH, USE, EQUIP, ANIMATE, ACTION, STACK, HOTBAR, SWING, INTERACT, SNEAK, HELD, STATE, ERROR }
 enum End { MANUAL, EXPIRED, LOGOUT, DISABLED }
 private final LongSupplier clock;
 private UUID owner;
 private long start;
 private End ended;
 private final EnumMap<Kind,Long> counts=new EnumMap<>(Kind.class);
 private final ArrayDeque<String> events=new ArrayDeque<>();
 Window(LongSupplier clock){this.clock=clock;}
 synchronized void start(UUID id){if(id==null)throw new IllegalArgumentException();owner=id;start=clock.getAsLong();ended=null;counts.clear();events.clear();}
 synchronized boolean active(UUID id){expire();return owner!=null&&owner.equals(id);}
 synchronized boolean running(){expire();return owner!=null;}
 private void expire(){if(owner!=null&&clock.getAsLong()-start>=60000)stop(End.EXPIRED);}
 synchronized void stop(End reason){if(owner!=null){owner=null;ended=reason;}}
 synchronized void logout(UUID id){if(active(id))stop(End.LOGOUT);}
 synchronized void note(UUID id,Kind kind,int a,int b,Enum<?> first,Enum<?> second,boolean detail){
  if(!active(id))return;
  counts.merge(kind,1L,(x,y)->x==Long.MAX_VALUE?x:x+1);
  if(detail){if(events.size()==64)events.removeFirst();events.addLast((clock.getAsLong()-start)+"ms "+kind+" a="+a+" b="+b+" "+(first==null?"-":first.name())+" "+(second==null?"-":second.name()));}
 }
 synchronized List<String> snapshot(){expire();List<String> out=new ArrayList<>();out.add("Wand diagnostic: "+(owner!=null?"ACTIVE":ended==null?"OFF":ended)+"; events="+events.size()+"/64; max=60s");out.add("counts="+counts);out.addAll(events);return out;}
}
