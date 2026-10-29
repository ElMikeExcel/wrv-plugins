import wrv.magic.level.RawWandLoadBridge;import com.elmakers.mine.bukkit.api.wand.Wand;import org.bukkit.configuration.MemoryConfiguration;import java.util.*;import java.lang.reflect.*;
public class RawWandLoadTest{
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 public static void main(String[]args)throws Exception{
 var raw=new MemoryConfiguration();raw.set("id","original");raw.set("owner_id","owner");raw.set("template","beginner");raw.set("bound",true);raw.set("spells",new ArrayList<>(List.of("heal|3")));raw.set("spell_levels",new LinkedHashMap<>(Map.of("heal",3)));
 Wand wand=(Wand)Proxy.newProxyInstance(Wand.class.getClassLoader(),new Class[]{Wand.class},(p,m,a)->{throw new AssertionError("must not inspect template/current rank "+m.getName());});
 ok(RawWandLoadBridge.beforeLoad(raw,null,true,false)==raw);ok(RawWandLoadBridge.beforeLoad(null,null,true,false)==null);
 var scope=Set.of("beginner","master");var captured=new ArrayList<RawWandLoadBridge.Snapshot>();try(var h=RawWandLoadBridge.installOwned(s->{captured.add(s);ok(RawWandLoadBridge.isCurrentSnapshot(s));return true;},s->!Boolean.FALSE.equals(s.bound())&&scope.contains(s.template()))){
 ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);var s=captured.get(0);ok(!RawWandLoadBridge.isCurrentSnapshot(s));ok(s.original()==wand);ok(s.spellLevels().get("heal")==3);ok(s.spells().equals(List.of("heal|3")));ok(Boolean.TRUE.equals(s.bound()));ok(s.wellFormed());ok(s.origin()==RawWandLoadBridge.Origin.CONSTRUCTOR_RAW);
 raw.set("template","master");raw.set("spell_levels",Map.of("heal",1));ok(s.template().equals("beginner"));ok(s.spellLevels().get("heal")==3);
 try{s.spellLevels().put("heal",5);throw new AssertionError();}catch(UnsupportedOperationException expected){ok(true);}
 int count=captured.size();ok(RawWandLoadBridge.beforeLoad(null,wand,true,true)==null);ok(RawWandLoadBridge.beforeLoad(null,wand,false,false)==null);ok(captured.size()==count);raw.set("spells",42);RawWandLoadBridge.beforeLoad(raw,wand,true,false);ok(!captured.get(captured.size()-1).wellFormed());raw.set("spells",List.of("heal|3"));
 }
 try(var h=RawWandLoadBridge.installOwned(s->false,s->!Boolean.FALSE.equals(s.bound())&&scope.contains(s.template()))){try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException expected){ok(true);}}
 try(var h=RawWandLoadBridge.installOwned(s->{throw new IllegalStateException("disk failure");},s->!Boolean.FALSE.equals(s.bound())&&scope.contains(s.template()))){try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException expected){ok(true);}}
 try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException expected){ok(true);}
 var h=RawWandLoadBridge.installOwned(s->true,s->s.existingWand()&&!s.upgrade()&&!Boolean.FALSE.equals(s.bound())&&scope.contains(s.template()));
 ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);h.close();
 try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException expected){ok(true);}
 raw.set("template","unrelated");ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);
 raw.set("template","beginner");raw.set("bound",false);ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);raw.set("bound",true);
 ok(RawWandLoadBridge.beforeLoad(null,null,false,false)==null);ok(RawWandLoadBridge.beforeLoad(null,null,true,true)==null);
 try(var fresh=RawWandLoadBridge.installOwned(s->true,s->!Boolean.FALSE.equals(s.bound())&&scope.contains(s.template()))){h.close();ok(RawWandLoadBridge.beforeLoad(raw,wand,true,false)==raw);}
 try{RawWandLoadBridge.beforeLoad(raw,wand,true,false);throw new AssertionError();}catch(IllegalStateException expected){ok(true);}
 System.out.println("PASS "+n+" raw-load immutable/default/deny/irrelevant-item checks; API proxy only");
 }
}
