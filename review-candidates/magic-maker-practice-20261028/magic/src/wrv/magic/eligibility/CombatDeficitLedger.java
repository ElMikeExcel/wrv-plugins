package wrv.magic.eligibility;
import java.util.*;
/** Serialized, session-local HP provenance. Inputs must be exact observed transitions, never estimates. */
public final class CombatDeficitLedger {
 public enum Origin { VERIFIED_HOSTILE_DIRECT, VERIFIED_HOSTILE_DOT, SELF_OR_UNKNOWN }
 public record Frame(UUID player,UUID session,long sequence,double before,double after,double maximum){}
 public record HealResult(double effective,double eligibleRestored,boolean accepted){}
 private static final class State{UUID session;long seq;double hp,max,eligible;State(UUID s,long q,double h,double m){session=s;seq=q;hp=h;max=m;}}
 private final Map<UUID,State> states=new HashMap<>();private final Thread owner=Thread.currentThread();private final int capacity;
 public CombatDeficitLedger(int capacity){if(capacity<1)throw new IllegalArgumentException();this.capacity=capacity;}
 private void thread(){if(Thread.currentThread()!=owner)throw new IllegalStateException("serialized owner required");}
 private static boolean value(double h,double m){return Double.isFinite(h)&&Double.isFinite(m)&&m>0&&h>=0&&h<=m;}
 public boolean begin(UUID player,UUID session,long sequence,double health,double maximum){thread();if(player==null||session==null||sequence<0||!value(health,maximum))return false;if(!states.containsKey(player)&&states.size()>=capacity)return false;states.put(player,new State(session,sequence,health,maximum));return true;}
 public void invalidate(UUID player){thread();states.remove(player);}
 public void clear(){thread();states.clear();}
 private State valid(UUID p,Frame f){thread();State s=states.get(p);if(f==null||s==null)return null;if(!Objects.equals(p,f.player())||!Objects.equals(s.session,f.session())||f.sequence()<=s.seq||!value(f.before(),f.maximum())||!value(f.after(),f.maximum())||Double.compare(s.hp,f.before())!=0||Double.compare(s.max,f.maximum())!=0){states.remove(p);return null;}return s;}
 public boolean damage(UUID player,Frame f,Origin origin,boolean cancelled){State s=valid(player,f);if(s==null)return false;if(f.after()>f.before()||cancelled&&f.after()!=f.before()){states.remove(player);return false;}s.seq=f.sequence();s.hp=f.after();if(!cancelled&&origin!=null&&origin!=Origin.SELF_OR_UNKNOWN)s.eligible+=f.before()-f.after();s.eligible=Math.min(s.eligible,s.max-s.hp);if(s.hp==0)states.remove(player);return true;}
 /** All restorations consume deficit, including food, natural regen, other healers and admin restoration. */
 public HealResult restore(UUID player,Frame f){State s=valid(player,f);if(s==null)return new HealResult(0,0,false);if(f.after()<f.before()){states.remove(player);return new HealResult(0,0,false);}double amount=f.after()-f.before();double unknown=Math.max(0,s.max-s.hp-s.eligible);double eligible=Math.min(s.eligible,Math.max(0,amount-unknown));s.eligible-=eligible;s.hp=f.after();s.seq=f.sequence();return new HealResult(amount,eligible,true);}
 public double remaining(UUID player){thread();var s=states.get(player);return s==null?0:s.eligible;}
}
