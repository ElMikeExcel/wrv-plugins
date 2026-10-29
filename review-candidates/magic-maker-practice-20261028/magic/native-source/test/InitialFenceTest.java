import wrv.magic.level.*;import java.util.*;import java.lang.reflect.*;import org.bukkit.configuration.MemoryConfiguration;import com.elmakers.mine.bukkit.api.wand.Wand;import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.spell.*;
public class InitialFenceTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 public static void main(String[] args)throws Exception {
 var raw=new MemoryConfiguration();raw.set("template","beginner");raw.set("spells",List.of("heal"));
 var wand=(Wand)Proxy.newProxyInstance(Wand.class.getClassLoader(),new Class[]{Wand.class},(p,m,a)->null);
 ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);ok(LearnedHooks.allowAuto(true,null));
 var keys=new HashSet<>(Set.of("heal"));LearnedHooks.requireManaged(keys);keys.clear();ok(!LearnedHooks.allowAuto(true,new SpellKey("heal")));ok(LearnedHooks.managesKey("heal"));ok(LearnedHooks.allowAuto(true,new SpellKey("feed")));
 RawWandLoadBridge.requireManagedScope(s->"beginner".equals(s.template()));RawWandLoadBridge.requireManagedScope(s->"master".equals(s.template()));
 for(String template:List.of("beginner","master")){raw.set("template",template);try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException e){ok(true);}}
 raw.set("template","unrelated");ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);
 try(var h=RawWandLoadBridge.installOwned(s->true,s->Set.of("beginner","master").contains(s.template()))){raw.set("template","beginner");ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);try{RawWandLoadBridge.requireManagedScope(s->true);throw new AssertionError();}catch(IllegalStateException e){ok(true);}}
 try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException e){ok(true);}
 var r=new LearnedLevelResolver(Map.of("heal",new LearnedLevelResolver.Binding("p",3)),q->null,q->null,q->null);try(var h=LearnedHooks.installOwned(r)){try{LearnedHooks.requireManaged(Set.of("feed"));throw new AssertionError();}catch(IllegalStateException e){ok(true);}}
 ok(!LearnedHooks.allowAuto(true,new SpellKey("heal")));System.out.println("PASS "+n+" initial reservation/failure/union/reattach checks; no runtime startup");
 }
}
