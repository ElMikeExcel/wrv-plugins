import wrv.magic.level.*;import com.elmakers.mine.bukkit.api.magic.Mage;import com.elmakers.mine.bukkit.api.spell.*;import java.util.*;import java.lang.reflect.*;
public class LearnedRetirementTest {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 static MageSpell spell(String key){return (MageSpell)Proxy.newProxyInstance(MageSpell.class.getClassLoader(),new Class[]{MageSpell.class},(p,m,a)->m.getName().equals("getSpellKey")?new SpellKey(key):null);}
 public static void main(String[] args)throws Exception {
 MageSpell heal=spell("heal"),feed=spell("feed");Mage mage=(Mage)Proxy.newProxyInstance(Mage.class.getClassLoader(),new Class[]{Mage.class},(p,m,a)->m.getName().equals("getSpell")?(a[0].equals("heal")?heal:feed):null);
 ok(LearnedHooks.resolve(mage,"heal",null)==heal);ok(LearnedHooks.allowCast(heal,null));ok(LearnedHooks.allowAuto(true,new SpellKey("heal")));ok(!LearnedHooks.managesKey("heal"));
 var r=new LearnedLevelResolver(Map.of("heal",new LearnedLevelResolver.Binding("policy",3)),q->null,q->null,q->null);
 var first=LearnedHooks.installOwned(r);ok(!LearnedHooks.allowAuto(true,new SpellKey("heal")));first.close();
 ok(LearnedHooks.resolve(mage,"heal",null)==null);ok(!LearnedHooks.allowCast(heal,null));ok(!LearnedHooks.allowAuto(true,new SpellKey("heal")));ok(LearnedHooks.managesKey("heal"));ok(LearnedHooks.managed(heal));
 ok(LearnedHooks.resolve(mage,"feed",null)==feed);ok(LearnedHooks.allowCast(feed,null));ok(LearnedHooks.allowAuto(true,new SpellKey("feed")));ok(!LearnedHooks.managesKey("feed"));
 try(var fresh=LearnedHooks.installOwned(r)){first.close();ok(LearnedHooks.managesKey("heal"));ok(LearnedHooks.resolve(mage,"heal",null)==null);}
 LearnedHooks.detach();ok(!LearnedHooks.allowCast(heal,null));ok(!LearnedHooks.allowAuto(true,new SpellKey("heal")));
 System.out.println("PASS "+n+" learned retirement/default/unrelated/stale-handle checks; API proxies only");
 }
}
