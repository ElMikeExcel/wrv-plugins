package com.winkreyes.excelwandbars;
import java.util.*;import java.util.function.BiPredicate;
import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.wand.Wand;
/** Disabled by default; an installed migration owner fences template mutation before any native change. */
final class MigrationTouchGuard {
 private record Owner(Thread thread,BiPredicate<Mage,Wand> confirm){}
 private record Frame(Owner owner,Mage mage,Wand wand){}
 private static Owner owner;private static boolean previouslyManaged;private static final ThreadLocal<Frame> frame=new ThreadLocal<>();
 static synchronized void requireManaged(){if(owner!=null)throw new IllegalStateException("migration touch already owned");previouslyManaged=true;}
 static synchronized AutoCloseable installOwned(BiPredicate<Mage,Wand> confirm){if(owner!=null)throw new IllegalStateException("migration touch already owned");var mine=new Owner(Thread.currentThread(),Objects.requireNonNull(confirm));owner=mine;previouslyManaged=true;return ()->{synchronized(MigrationTouchGuard.class){if(Thread.currentThread()!=mine.thread)throw new IllegalStateException("migration owner thread");if(owner==mine)owner=null;}};}
 static synchronized boolean allow(Mage mage,Wand wand){var current=owner;if(current==null)return !previouslyManaged;if(Thread.currentThread()!=current.thread||mage==null||wand==null)return false;var prior=frame.get();frame.set(new Frame(current,mage,wand));try{return current.confirm.test(mage,wand)&&owner==current;}catch(RuntimeException|LinkageError unavailable){return false;}finally{if(prior==null)frame.remove();else frame.set(prior);}}
 static synchronized boolean isCurrent(Mage mage,Wand wand){var current=frame.get();return current!=null&&current.owner==owner&&current.mage==mage&&current.wand==wand&&Thread.currentThread()==current.owner.thread;}
 private MigrationTouchGuard(){}
}
