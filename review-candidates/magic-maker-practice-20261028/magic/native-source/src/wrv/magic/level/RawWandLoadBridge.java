package wrv.magic.level;
import java.util.*;import java.util.function.Predicate;import org.bukkit.configuration.ConfigurationSection;import com.elmakers.mine.bukkit.api.wand.Wand;
/** Disabled by default. Explicit pre-load durable confirmation; never grants ranks or edits raw configuration. */
public final class RawWandLoadBridge {
 public enum Origin { CONSTRUCTOR_RAW, ALREADY_LOADED_CURRENT_ITEM }
 public record Snapshot(Wand original,String id,String ownerId,String template,Boolean bound,Object spells,Map<String,Integer> spellLevels,boolean existingWand,boolean upgrade,Origin origin,boolean wellFormed){
  public Snapshot{Objects.requireNonNull(original);Objects.requireNonNull(origin);if(spells instanceof List<?> list)spells=List.copyOf(list);else if(spells!=null&&!(spells instanceof String))throw new IllegalArgumentException("raw spell shape");spellLevels=Map.copyOf(spellLevels);}
 }
 private record Registration(Thread thread,Predicate<Snapshot> confirm,Predicate<Snapshot> scope){}
 private static volatile Predicate<Snapshot> retiredScope;
 private static Predicate<Snapshot> union(Predicate<Snapshot> a,Predicate<Snapshot> b){return a==null?b:s->inScope(a,s)||inScope(b,s);}
 public static synchronized void requireManagedScope(Predicate<Snapshot> scope){if(registration!=null)throw new IllegalStateException("active raw load owner");retiredScope=union(retiredScope,Objects.requireNonNull(scope));}
 private static volatile Registration registration;
 private record Frame(Registration registration,Snapshot exact){}
 private static final ThreadLocal<Frame> frame=new ThreadLocal<>();
 public static boolean isCurrentSnapshot(Snapshot exact){var f=frame.get();return f!=null&&f.exact()==exact&&f.registration()==registration&&f.registration().thread()==Thread.currentThread();}
 public static synchronized AutoCloseable installOwned(Predicate<Snapshot> confirm){return installOwned(confirm,s->true);}
 /** Scope must be immutable and independent of the owner: it survives retirement as a safety fence. */
 public static synchronized AutoCloseable installOwned(Predicate<Snapshot> confirm,Predicate<Snapshot> scope){if(registration!=null)throw new IllegalStateException("raw load observer already owned");var mine=new Registration(Thread.currentThread(),Objects.requireNonNull(confirm),Objects.requireNonNull(scope));registration=mine;return ()->{if(mine.thread()!=Thread.currentThread())throw new IllegalStateException("owner thread required");synchronized(RawWandLoadBridge.class){if(registration==mine){retiredScope=union(retiredScope,mine.scope());registration=null;}}};}
 private static boolean inScope(Predicate<Snapshot> scope,Snapshot snapshot){try{return scope.test(snapshot);}catch(RuntimeException|LinkageError failed){throw new IllegalStateException("raw load scope unavailable",failed);}}
 /** Called ONLY at native existing-item constructor return from itemToConfig, before migration/load/save. */
 public static ConfigurationSection beforeLoad(ConfigurationSection raw,Wand original,boolean existingWand,boolean upgrade){if(!existingWand||upgrade)return raw;var r=registration;var retired=retiredScope;if(r==null&&retired==null)return raw;var captured=snapshot(raw,original,existingWand,upgrade,Origin.CONSTRUCTOR_RAW);if(r==null){if(inScope(retired,captured))throw new IllegalStateException("managed raw load owner unavailable");return raw;}if(inScope(r.scope(),captured))confirm(r,captured);else if(retired!=null&&inScope(retired,captured))throw new IllegalStateException("reserved raw load scope not owned");return raw;}
 /** Explicit bootstrap read of stored current item, never historical/pre-construction provenance. */
 public static void captureAlreadyLoaded(Wand original){var r=registration;if(r==null)throw new IllegalStateException("explicit observer required");if(original==null||original.getClass()!=com.elmakers.mine.bukkit.wand.Wand.class)throw new IllegalArgumentException("exact native wand required");var item=original.getItem();if(!com.elmakers.mine.bukkit.wand.Wand.isWand(item)||com.elmakers.mine.bukkit.wand.Wand.isUpgrade(item))return;var raw=com.elmakers.mine.bukkit.wand.Wand.itemToConfig(item,null);confirm(r,snapshot(raw,original,com.elmakers.mine.bukkit.wand.Wand.isWand(item),com.elmakers.mine.bukkit.wand.Wand.isUpgrade(item),Origin.ALREADY_LOADED_CURRENT_ITEM));}
 private static void confirm(Registration r,Snapshot snapshot){if(r.thread()!=Thread.currentThread()||r!=registration)throw new IllegalStateException("raw load owner unavailable");var prior=frame.get();frame.set(new Frame(r,snapshot));try{boolean accepted=r.confirm().test(snapshot);if(!accepted||r!=registration)throw new IllegalStateException("raw item baseline not durably confirmed");}finally{if(prior==null)frame.remove();else frame.set(prior);}}
 static Snapshot snapshot(ConfigurationSection raw,Wand original,boolean existing,boolean upgrade,Origin origin){
  if(raw==null)throw new IllegalStateException("raw item data unavailable");
  try{return strictSnapshot(raw,original,existing,upgrade,origin);}catch(IllegalArgumentException invalid){return new Snapshot(original,raw.getString("id"),raw.getString("owner_id"),raw.getString("template"),raw.isBoolean("bound")?raw.getBoolean("bound"):null,null,Map.of(),existing,upgrade,origin,false);}
 }
 private static Snapshot strictSnapshot(ConfigurationSection raw,Wand original,boolean existing,boolean upgrade,Origin origin){Object spells=raw.get("spells");if(spells instanceof Collection<?> c){if(c.size()>4096)throw new IllegalArgumentException("spell capacity");var copy=new ArrayList<String>();for(Object value:c){if(!(value instanceof String text)||text.length()>1024)throw new IllegalArgumentException("raw spell value");copy.add(text);}spells=List.copyOf(copy);}else if(spells!=null&&!(spells instanceof String))throw new IllegalArgumentException("raw spells unavailable");
  Object levels=raw.get("spell_levels");Map<?,?> map=levels instanceof ConfigurationSection section?section.getValues(false):levels instanceof Map<?,?> m?m:levels==null?Map.of():null;if(map==null||map.size()>4096)throw new IllegalArgumentException("raw levels unavailable");var copied=new TreeMap<String,Integer>();for(var entry:map.entrySet()){if(!(entry.getKey() instanceof String key)||!(entry.getValue() instanceof Integer value))throw new IllegalArgumentException("raw level value");copied.put(key,value);}
  return new Snapshot(original,raw.getString("id"),raw.getString("owner_id"),raw.getString("template"),raw.isBoolean("bound")?raw.getBoolean("bound"):null,spells,copied,existing,upgrade,origin,true);
 }
 private RawWandLoadBridge(){}
}
