import wrv.magic.evidence.EffectObserver;import com.elmakers.mine.bukkit.api.action.CastContext;import java.util.*;import java.lang.reflect.*;
/** Boundary tests seed private state reflectively; not a native cast/installation fixture. */
public class ContextLookupTest{
 static int n;static void check(boolean b){n++;if(!b)throw new AssertionError("check "+n);}
 static Object get(String f)throws Exception{Field x=EffectObserver.class.getDeclaredField(f);x.setAccessible(true);return x.get(null);}
 static void set(String f,Object v)throws Exception{Field x=EffectObserver.class.getDeclaredField(f);x.setAccessible(true);x.set(null,v);}
 static Object record(String name,Object...args)throws Exception{Class<?> c=Class.forName("wrv.magic.evidence.EffectObserver$"+name);Constructor<?> k=c.getDeclaredConstructors()[0];k.setAccessible(true);return k.newInstance(args);}
 static CastContext context(CastContext root)throws Exception{Field u=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");u.setAccessible(true);var unsafe=(sun.misc.Unsafe)u.get(null);var c=(CastContext)unsafe.allocateInstance(com.elmakers.mine.bukkit.action.CastContext.class);Field b=c.getClass().getDeclaredField("base");b.setAccessible(true);b.set(c,root==null?c:root);return c;}
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
 var origins=(Map<CastContext,Object>)get("origins");var started=(Set<CastContext>)get("started");
 var root=context(null);var child=context(root);var unknown=context(null);UUID id=UUID.randomUUID(),caster=UUID.randomUUID();var source=new EffectObserver.SourceSnapshot("original","beginner",null,true,false,false,1d,100L);
 EffectObserver.Sink sink=r->{};Object reg=record("Registration",Set.of("heal"),sink), next=record("Registration",Set.of("heal"),sink);
 check(EffectObserver.originalRootEvidence(root).isEmpty());set("registration",reg);check(EffectObserver.originalRootEvidence(null).isEmpty());check(EffectObserver.originalRootEvidence(root).isEmpty());
 origins.put(root,record("Origin",id,caster,"heal",source,reg,new EffectObserver.RootAuthority(null,null)));check(EffectObserver.originalRootEvidence(root).isEmpty());started.add(root);
 var evidence=EffectObserver.originalRootEvidence(root).orElseThrow();check(evidence.cast().equals(id));check(evidence.caster().equals(caster));check(evidence.source()==source);check(EffectObserver.originalRootEvidence(child).orElseThrow().equals(evidence));check(EffectObserver.originalRootEvidence(unknown).isEmpty());
 CastContext proxy=(CastContext)Proxy.newProxyInstance(ContextLookupTest.class.getClassLoader(),new Class[]{CastContext.class},(p,m,a)->m.getName().equals("getBaseContext")?root:null);check(EffectObserver.originalRootEvidence(proxy).isEmpty());
 check(!EffectObserver.claimIssuance(evidence));int before=origins.size();for(int i=0;i<10;i++)check(EffectObserver.originalRootEvidence(root).orElseThrow().equals(evidence));check(origins.size()==before);
 set("registration",null);check(EffectObserver.originalRootEvidence(root).isEmpty());set("registration",next);check(EffectObserver.originalRootEvidence(root).isEmpty());check(EffectObserver.originalRootEvidence(child).isEmpty());
 // A stale reused native object cannot acquire current-generation association via this API.
 check(origins.size()==before);var fresh=context(null);origins.put(fresh,record("Origin",UUID.randomUUID(),caster,"heal",source,next,new EffectObserver.RootAuthority(null,null)));started.add(fresh);check(EffectObserver.originalRootEvidence(fresh).isPresent());check(EffectObserver.originalRootEvidence(root).isEmpty());
 set("registration",record("Registration",Set.of("mend"),sink));check(EffectObserver.originalRootEvidence(fresh).isEmpty());set("registration",null);origins.clear();started.clear();System.out.println("PASS "+n+" read-only context identity/generation boundary checks");
 }
}
